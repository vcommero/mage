package mage.player.ai;

import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.abilities.PlayLandAbility;
import mage.abilities.SpellAbility;
import mage.abilities.common.PlayLandAsCommanderAbility;
import mage.cards.Card;
import mage.constants.RangeOfInfluence;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.player.ai.score.GameStateEvaluator2;
import mage.player.ai.selection.SelectionStrategy;
import mage.player.ai.selection.Ucb1SelectionStrategy;
import mage.player.ai.util.StateHasher;
import mage.players.Player;
import mage.target.Target;
import mage.util.RandomUtil;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Monte Carlo Tree Search AI: Modular implementation with pluggable strategies
 * 
 * This implementation abstracts key components for easy experimentation:
 * - Selection strategy (UCB1, Thompson Sampling, etc.)
 * - Simulation strategy (random rollout, evaluation function, neural network)
 * - Expansion strategy (progressive widening, full expansion, etc.)
 * - State evaluation (heuristic, learned, hybrid)
 * 
 * @author vcommero
 */
public class ComputerPlayerMCTSRedux extends ComputerPlayer {

    private static final Logger logger = Logger.getLogger(ComputerPlayerMCTSRedux.class);

    // Configuration Parameters
    private static final long THINK_TIME_MS = 2000;
    private static final int MAX_ROLLOUT_DEPTH = 20;
    private Integer skillLevelDepth;
    
    // Strategy Configuration Enums
    public enum SimulationStrategy {
        RANDOM_ROLLOUT,  // Random moves until terminal
        EVALUATION,      // Direct evaluation function
        HYBRID,          // Short rollout + evaluation
        HEAVY_ROLLOUT    // Smart rollout with heuristics
    }
    
    public enum ExpansionStrategy {
        FULL,              // Expand all children immediately
        PROGRESSIVE,       // Progressive widening
        SINGLE,           // One child at a time
        THRESHOLD_BASED   // Expand based on visit count
    }
    
    // Active strategies (can be configured)
    private SelectionStrategy selectionStrategy = new Ucb1SelectionStrategy();
    private SimulationStrategy simulationStrategy = SimulationStrategy.RANDOM_ROLLOUT;
    private ExpansionStrategy expansionStrategy = ExpansionStrategy.PROGRESSIVE;
    
    // Performance tracking
    private final TranspositionTable transpositionTable = new TranspositionTable();
    private long nodesExplored = 0;
    private long simulationsRun = 0;

    public ComputerPlayerMCTSRedux(String name, RangeOfInfluence range, int skill) {
        super(name, range);
        this.skillLevelDepth = MAX_ROLLOUT_DEPTH / 10 * skill;
    }

    protected ComputerPlayerMCTSRedux(UUID id) {
        super(id);
    }

    public ComputerPlayerMCTSRedux(final ComputerPlayerMCTSRedux player) {
        super(player);
    }

    @Override
    public boolean priority(Game game) {
        logger.info("MCTS AI - Priority pass received");
        game.resumeTimer(getTurnControlledBy());
        boolean result = priorityHelper(game);
        game.pauseTimer(getTurnControlledBy());
        logPerformanceMetrics();
        return result;
    }

    private boolean priorityHelper(Game game) {
        game.getState().setPriorityPlayerId(playerId);
        game.firePriorityEvent(playerId);
        
        ActivatedAbility nextAction = null;
        
        switch (game.getTurnStepType()) {
            case UNTAP:
            case UPKEEP:
            case DRAW:
            case BEGIN_COMBAT:
            case FIRST_COMBAT_DAMAGE:
            case COMBAT_DAMAGE:
            case END_COMBAT:
            case END_TURN:
            case CLEANUP:
                pass(game);
                return false;
                
            case PRECOMBAT_MAIN:
            case POSTCOMBAT_MAIN:
            case DECLARE_ATTACKERS:
            case DECLARE_BLOCKERS:
                nextAction = findBestActionMCTS(game);
                performAction(game, nextAction);
                return true;
        }
        
        return false;
    }

    /**
     * Main MCTS algorithm with modular components
     */
    private ActivatedAbility findBestActionMCTS(Game game) {
        logger.info("MCTS - Starting tree search");
        
        List<ActivatedAbility> availableActions = getFilteredActions(game);
        
        if (availableActions.isEmpty()) {
            logger.info("MCTS - No actions available");
            return null;
        }
        
        // Check for mandatory good moves (like land plays)
        Optional<ActivatedAbility> mandatoryAction = selectMandatoryAction(availableActions, game);
        if (mandatoryAction.isPresent()) {
            return mandatoryAction.get();
        }
        
        if (availableActions.size() == 1) {
            return availableActions.get(0);
        }

        // Create game copy for MCTS search
        Game mctsGame = game.createSimulationForAI();
        
        // Initialize MCTS
        resetMetrics();
        MCTSReduxNode root = createRootNode(mctsGame, availableActions);
        
        // Main MCTS loop
        long startTime = System.currentTimeMillis();
        long endTime = startTime + THINK_TIME_MS;
        int iterations = 0;
        
        while (System.currentTimeMillis() < endTime) {
            runMCTSIteration(root, mctsGame);
            iterations++;
            
            // Periodic maintenance
            if (iterations % 100 == 0) {
                performTreeMaintenance(root);
            }
        }
        
        logger.info(String.format("MCTS - Completed %d iterations in %d ms", 
            iterations, System.currentTimeMillis() - startTime));
        
        // Select best action
        return selectFinalAction(root, mctsGame);
    }

    /**
     * Run a single MCTS iteration through all four phases
     */
    private void runMCTSIteration(MCTSReduxNode root, Game simGame) {
        
        // Phase 1: Selection
        MCTSReduxNode selectedNode = performSelection(root, simGame);
        
        // Phase 2: Expansion
        MCTSReduxNode expandedNode = performExpansion(selectedNode, simGame);
        
        // Phase 3: Simulation
        double score = performSimulation(expandedNode, simGame);
        
        // Phase 4: Backpropagation
        performBackpropagation(expandedNode, score);
    }

    /**
     * ABSTRACT: Selection phase - choose which node to expand
     * This method implements the tree policy for navigating to a leaf node
     * 
     * @param root The root node to start selection from
     * @param game The game state (will be modified to match selected node)
     * @return The selected leaf node for expansion
     */
    protected MCTSReduxNode performSelection(MCTSReduxNode root, Game game) {
        MCTSReduxNode current = root;
        
        while (!current.isLeaf()) {
            // Check if we should expand or continue selection
            if (shouldExpand(current)) {
                break;
            }
            
            // Select best child according to strategy
            MCTSReduxNode selected = selectChild(current, game);
            if (selected == null) {
                break;
            }
            
            // Apply action to game state
            if (selected.getAction() != null) {
                applyActionToGame(selected.getAction(), game);
            }
            
            current = selected;
            nodesExplored++;
        }
        
        return current;
    }

    /**
     * ABSTRACT: Child selection strategy
     * Implement different selection algorithms (UCB1, Thompson Sampling, etc.)
     * 
     * @param node Parent node
     * @param game Current game state
     * @return Selected child node
     */
    protected MCTSReduxNode selectChild(MCTSReduxNode node, Game game) {
        return selectionStrategy.selectChild(node, game);
    }

    /**
     * ABSTRACT: Expansion phase - add new node(s) to tree
     * 
     * @param node Node to expand from
     * @param game Current game state
     * @return The newly expanded node (or original if no expansion)
     */
    protected MCTSReduxNode performExpansion(MCTSReduxNode node, Game game) {
        if (!shouldExpand(node) || isTerminalState(game)) {
            return node;
        }
        
        switch (expansionStrategy) {
            case FULL:
                return expandAllChildren(node, game);
            case PROGRESSIVE:
                return expandProgressiveWidening(node, game);
            case SINGLE:
                return expandSingleChild(node, game);
            case THRESHOLD_BASED:
                return expandThresholdBased(node, game);
            default:
                return expandSingleChild(node, game);
        }
    }

    /**
     * SKELETON: Expand all children at once
     */
    private MCTSReduxNode expandAllChildren(MCTSReduxNode node, Game game) {
        // TODO: Implement full expansion
        // Create all child nodes immediately
        
        // Placeholder: Expand single child
        return expandSingleChild(node, game);
    }

    /**
     * SKELETON: Progressive widening expansion
     */
    private MCTSReduxNode expandProgressiveWidening(MCTSReduxNode node, Game game) {
        // TODO: Implement progressive widening
        // Limit branching based on visit count: k * N^alpha
        
        int maxChildren = (int)(10 * Math.pow(node.getVisits() + 1, 0.5));
        if (node.getChildren().size() >= maxChildren) {
            return node;
        }
        
        return expandSingleChild(node, game);
    }

    /**
     * SKELETON: Single child expansion
     */
    private MCTSReduxNode expandSingleChild(MCTSReduxNode node, Game game) {
        if (node.getUnexploredActions().isEmpty()) {
            return node;
        }
        
        // Select action to expand (can use heuristics)
        if (node.getUnexploredActions().isEmpty())
            return node;
        ActivatedAbility actionToExpand = selectActionWithHeuristic(node.getUnexploredActions(), game);
        
        // Apply action and create child
        Game childGame = game.createSimulationForAI();
        applyActionToGame(actionToExpand, childGame);
        
        List<ActivatedAbility> childActions = getFilteredActions(childGame);
        long stateHash = StateHasher.computeStateHash(childGame);
        
        // Check transposition table
        MCTSReduxNode cachedNode = transpositionTable.get(stateHash);
        if (cachedNode != null) {
            return cachedNode;
        }
        
        MCTSReduxNode child = new MCTSReduxNode(playerId, actionToExpand, node, childActions, stateHash);
        node.getChildren().add(child);
        node.getUnexploredActions().remove(actionToExpand);
        
        transpositionTable.put(stateHash, child);
        
        return child;
    }

    /**
     * SKELETON: Threshold-based expansion
     */
    private MCTSReduxNode expandThresholdBased(MCTSReduxNode node, Game game) {
        // TODO: Implement threshold-based expansion
        // Expand when visits exceed certain threshold
        
        int threshold = 10;
        if (node.getVisits() < threshold) {
            return node;
        }
        
        return expandSingleChild(node, game);
    }

    /**
     * ABSTRACT: Simulation phase - evaluate node through rollout or evaluation
     * 
     * @param node Node to simulate from
     * @param game Game state at this node
     * @return Score for the simulation (normalized to [0,1])
     */
    protected double performSimulation(MCTSReduxNode node, Game game) {
        simulationsRun++;
        
        switch (simulationStrategy) {
            case RANDOM_ROLLOUT:
                return simulateRandomRollout(node, game);
            case EVALUATION:
                return simulateWithEvaluation(node, game);
            case HYBRID:
                return simulateHybrid(node, game);
            case HEAVY_ROLLOUT:
                return simulateHeavyRollout(node, game);
            default:
                return simulateWithEvaluation(node, game);
        }
    }

    /**
     * SKELETON: Random rollout simulation
     */
    private double simulateRandomRollout(MCTSReduxNode node, Game game) {
        // TODO: Implement random rollout
        // Play random moves until terminal state or depth limit
        
        Game simGame = game.createSimulationForAI();
        int depth = 0;
        
        int rolloutDepth = skillLevelDepth != null ? skillLevelDepth : MAX_ROLLOUT_DEPTH;
        while (!isTerminalState(simGame) && depth < rolloutDepth) {
            List<ActivatedAbility> actions = getFilteredActions(simGame);
            if (actions.isEmpty()) {
                break;
            }
            
            // Random action
            ActivatedAbility randomAction = actions.get(RandomUtil.nextInt(actions.size()));
            applyActionToGame(randomAction, simGame);
            depth++;
        }
        
        return evaluateGameState(simGame, node.getPlayerId());
    }

    /**
     * SKELETON: Direct evaluation simulation
     */
    private double simulateWithEvaluation(MCTSReduxNode node, Game game) {
        // Use heuristic or other advanced evaluation to evaluate position
        throw new UnsupportedOperationException("Evaluation Simulation not yet implemented. Do not use.");
        
        //return evaluateGameState(game, node.getPlayerId());
    }

    /**
     * SKELETON: Hybrid simulation (short rollout + evaluation)
     */
    private double simulateHybrid(MCTSReduxNode node, Game game) {
        // TODO: Implement hybrid approach
        // Do short rollout (3-5 moves) then evaluate
        
        Game simGame = game.createSimulationForAI();
        int shortRolloutDepth = 3;
        
        for (int i = 0; i < shortRolloutDepth; i++) {
            List<ActivatedAbility> actions = getFilteredActions(simGame);
            if (actions.isEmpty() || isTerminalState(simGame)) {
                break;
            }
            
            // Use heuristic to select action
            ActivatedAbility action = selectActionWithHeuristic(actions, simGame);
            applyActionToGame(action, simGame);
        }
        
        return evaluateGameState(simGame, node.getPlayerId());
    }

    /**
     * SKELETON: Heavy rollout with smart action selection
     */
    private double simulateHeavyRollout(MCTSReduxNode node, Game game) {
        // TODO: Implement heavy rollout
        // Use domain knowledge to guide rollout
        
        Game simGame = game.createSimulationForAI();
        int depth = 0;
        
        int rolloutDepth = skillLevelDepth != null ? skillLevelDepth : MAX_ROLLOUT_DEPTH;
        while (!isTerminalState(simGame) && depth < rolloutDepth) {
            List<ActivatedAbility> actions = getFilteredActions(simGame);
            if (actions.isEmpty()) {
                break;
            }
            
            // Smart action selection
            ActivatedAbility action = selectActionWithHeuristic(actions, simGame);
            applyActionToGame(action, simGame);
            depth++;
        }
        
        return evaluateGameState(simGame, node.getPlayerId());
    }

    /**
     * ABSTRACT: State evaluation function
     * Core evaluation logic for scoring game states
     * 
     * @param game Game state to evaluate
     * @param playerId Player perspective for evaluation
     * @return Normalized score [0,1] where 1 is winning
     */
    protected double evaluateGameState(Game game, UUID playerId) {
        Player player = game.getPlayer(playerId);
        if (player == null || player.hasLost()) return 0.0;
        if (player.hasWon()) return 1.0;

        int myScore = GameStateEvaluator2.evaluate(playerId, game).getTotalScore();
        int opponentScore = game.getOpponents(playerId).stream()
            .mapToInt(id -> GameStateEvaluator2.evaluate(id, game).getTotalScore())
            .sum();

        int total = myScore + opponentScore;
        return total == 0 ? 0.5 : Math.max(0.0, Math.min(1.0, (double) myScore / total));
    }

    /**
     * ABSTRACT: Backpropagation phase
     * Update tree statistics with simulation result
     * 
     * @param node Leaf node where simulation started
     * @param score Simulation result
     */
    protected void performBackpropagation(MCTSReduxNode node, double score) {
        MCTSReduxNode current = node;
        
        while (current != null) {
            // Adjust score based on player perspective
            double adjustedScore = current.getPlayerId().equals(playerId) ? score : 1 - score;
            
            // Update node statistics
            current.updateStats(adjustedScore);
            
            // Additional updates for advanced strategies
            updateNodeMetadata(current, adjustedScore);
            
            current = current.getParent();
        }
    }

    /**
     * SKELETON: Update additional node metadata for advanced strategies
     */
    protected void updateNodeMetadata(MCTSReduxNode node, double score) {
        // TODO: Implement metadata updates
        // AMAF/RAVE updates
        // Confidence bounds
        // Variance tracking
        
        // Track variance for uncertainty estimation
        if (node.getMetadata().containsKey("sumSquares")) {
            double sumSquares = node.getMetadata().get("sumSquares");
            node.getMetadata().put("sumSquares", sumSquares + score * score);
        } else {
            node.getMetadata().put("sumSquares", score * score);
        }
    }

    /**
     * Helper: Should expand from this node?
     */
    private boolean shouldExpand(MCTSReduxNode node) {
        return !node.getUnexploredActions().isEmpty() && node.getVisits() > 0;
    }

    /**
     * Helper: Select action using heuristics
     */
    private ActivatedAbility selectActionWithHeuristic(List<ActivatedAbility> actions, Game game) {
        // Sort by priority
        actions.sort((a1, a2) -> {
            int priority1 = getActionPriority(a1, game);
            int priority2 = getActionPriority(a2, game);
            return Integer.compare(priority2, priority1);
        });
        
        // Take best with some randomness
        if (RandomUtil.nextDouble() < 0.7) {
            return actions.get(0);
        }
        return actions.get(RandomUtil.nextInt(Math.min(3, actions.size())));
    }

    /**
     * Helper: Get action priority for move ordering
     */
    private int getActionPriority(ActivatedAbility action, Game game) {
        int priority = 0;
        
        // Land plays have highest priority
        if (action instanceof PlayLandAbility || action instanceof PlayLandAsCommanderAbility) {
            priority += 1000;
        }
        
        // Cheaper spells have higher priority
        if (action instanceof SpellAbility) {
            Card card = game.getCard(action.getSourceId());
            if (card != null) {
                priority += 100 - card.getManaValue() * 10;
                
                // Instant speed is valuable
                if (card.isInstant(game)) {
                    priority += 50;
                }
            }
        }
        
        return priority;
    }

    /**
     * Helper: Select final action after MCTS search
     */
    private ActivatedAbility selectFinalAction(MCTSReduxNode root, Game game) {
        if (root.getChildren().isEmpty()) {
            return null;
        }
        
        // Select most visited child (robust)
        MCTSReduxNode bestChild = root.getChildren().stream()
            .max(Comparator.comparing(n -> n.getVisits()))
            .orElse(null);
        
        if (bestChild != null) {
            logDecision(game, bestChild);
            return bestChild.getAction();
        }
        
        return null;
    }

    /**
     * Helper: Apply action to game state
     */
    private void applyActionToGame(ActivatedAbility action, Game game) {
        if (action == null) return;
        
        try {
            // Handle targeting
            if (!action.getTargets().isEmpty()) {
                for (Target target : action.getTargets()) {
                    for (UUID id : target.getTargets()) {
                        target.updateTarget(id, game);
                        if (!target.isNotTarget()) {
                            game.addSimultaneousEvent(GameEvent.getEvent(
                                GameEvent.EventType.TARGETED, id, action, action.getControllerId()));
                        }
                    }
                }
            }
            
            this.activateAbility(action, game);
            
        } catch (Exception e) {
            logger.warn("MCTS - Failed to apply action: " + e.getMessage());
        }
    }

    /**
     * Helper: Tree maintenance operations
     */
    private void performTreeMaintenance(MCTSReduxNode root) {
        // Prune poor branches
        if (root.getChildren().size() > 5) {
            int totalVisits = root.getChildren().stream().mapToInt(c -> c.getVisits()).sum();
            double threshold = totalVisits * 0.05;
            root.getChildren().removeIf(child -> child.getVisits() < threshold);
        }
        
        // Clear old transposition entries
        if (transpositionTable.size() > 10000) {
            transpositionTable.clear();
        }
    }

    /**
     * Helper: Create root node for MCTS
     */
    private MCTSReduxNode createRootNode(Game game, List<ActivatedAbility> actions) {
        long stateHash = StateHasher.computeStateHash(game);
        return new MCTSReduxNode(playerId, null, null, actions, stateHash);
    }

    /**
     * Helper: Get filtered actions (non-mana abilities)
     */
    private List<ActivatedAbility> getFilteredActions(Game game) {
        Player player = game.getPlayer(playerId);
        if (player == null) {
            return new ArrayList<>();
        }
        
        return player.getPlayable(game, true).stream()
            .filter(a -> !a.isManaAbility())
            .collect(Collectors.toList());
    }

    /**
     * Helper: Select mandatory actions (like land plays)
     */
    private Optional<ActivatedAbility> selectMandatoryAction(List<ActivatedAbility> actions, Game game) {
        // Always play lands when possible
        Optional<ActivatedAbility> landPlay = actions.stream()
            .filter(a -> a instanceof PlayLandAbility || a instanceof PlayLandAsCommanderAbility)
            .findFirst();
        
        if (landPlay.isPresent()) {
            Card card = game.getCard(landPlay.get().getSourceId());
            if (card != null) {
                logger.info("MCTS - Playing mandatory land: " + card.getName());
            }
            return landPlay;
        }
        
        return Optional.empty();
    }

    /**
     * Helper: Check if state is terminal
     */
    private boolean isTerminalState(Game game) {
        return game.checkIfGameIsOver() || 
               game.getPlayer(playerId) == null ||
               game.getPlayer(playerId).hasLost();
    }

    /**
     * Helper: Log decision information
     */
    private void logDecision(Game game, MCTSReduxNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("MCTS Decision: visits=%d, avg=%.3f", 
            node.getVisits(), node.getAverageScore()));
        
        if (node.getAction() instanceof SpellAbility) {
            Card card = game.getCard(node.getAction().getSourceId());
            if (card != null) {
                sb.append(" - Cast ").append(card.getName());
            }
        } else if (node.getAction() != null) {
            sb.append(" - ").append(node.getAction().getRule());
        }
        
        logger.info(sb.toString());
    }

    /**
     * Helper: Log performance metrics
     */
    private void logPerformanceMetrics() {
        logger.info(String.format("MCTS Performance: Nodes=%d, Simulations=%d", 
            nodesExplored, simulationsRun));
    }

    /**
     * Helper: Reset metrics
     */
    private void resetMetrics() {
        nodesExplored = 0;
        simulationsRun = 0;
    }

    // Combat Methods

    @Override
    public void selectAttackers(Game game, UUID attackingPlayerId) {
        if (!attackingPlayerId.equals(getId())) {
            return;
        }
        logger.info("MCTS - Selecting attackers");
        CombatExecutor.declareAttackers(this, game, attackingPlayerId);
    }

    @Override
    public void selectBlockers(Ability source, Game game, UUID defendingPlayerId) {
        if (!defendingPlayerId.equals(getId())) {
            return;
        }
        logger.info("MCTS - Selecting blockers");
        CombatExecutor.declareBlockers(this, source, game, defendingPlayerId);
    }

    /**
     * Helper: Perform action
     */
    private void performAction(Game game, ActivatedAbility action) {
        if (action == null) {
            pass(game);
            return;
        }
        
        if (!action.getTargets().isEmpty()) {
            for (Target target : action.getTargets()) {
                for (UUID id : target.getTargets()) {
                    target.updateTarget(id, game);
                    if (!target.isNotTarget()) {
                        game.addSimultaneousEvent(GameEvent.getEvent(
                            GameEvent.EventType.TARGETED, id, action, action.getControllerId()));
                    }
                }
            }
        }
        
        this.activateAbility(action, game);
        
        if (action.isUsesStack()) {
            pass(game);
        }
    }

    @Override
    public ComputerPlayerMCTSRedux copy() {
        return new ComputerPlayerMCTSRedux(this);
    }

    @Override
    public void setAllowBadMoves(boolean allowBadMoves) {
        // Not used in MCTS
    }

    @Override
    public String toString() {
        return "ComputerPlayerMCTSRedux AI (Modular MCTS) - " + getName();
    }
}
