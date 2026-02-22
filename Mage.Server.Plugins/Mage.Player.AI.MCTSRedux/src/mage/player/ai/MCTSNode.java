package mage.player.ai;

import mage.abilities.ActivatedAbility;
import java.util.*;

/**
 * MCTS Node with modular design support.
 * Used by ComputerPlayerMCTSRedux for Monte Carlo Tree Search decisions.
 */
class MCTSNode {
    private final UUID playerId;
    private final ActivatedAbility action;
    private final MCTSNode parent;
    private final List<MCTSNode> children;
    private final List<ActivatedAbility> unexploredActions;
    private final long stateHash;

    private int visits;
    private double totalScore;
    private double priorScore; // For PUCT and other advanced strategies
    private Map<String, Double> metadata; // Extensible metadata for different algorithms

    public MCTSNode(UUID playerId, ActivatedAbility action, MCTSNode parent,
                    List<ActivatedAbility> availableActions, long stateHash) {
        this.playerId = playerId;
        this.action = action;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.unexploredActions = new ArrayList<>(availableActions);
        this.stateHash = stateHash;
        this.visits = 0;
        this.totalScore = 0.0;
        this.priorScore = 0.5; // Default prior
        this.metadata = new HashMap<>();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public double getAverageScore() {
        return visits > 0 ? totalScore / visits : 0.0;
    }

    public void updateStats(double score) {
        visits++;
        totalScore += score;
    }

    public MCTSNode getChildForAction(ActivatedAbility action) {
        return children.stream()
                .filter(child -> child.action == action)
                .findFirst()
                .orElse(null);
    }

    // Getters and setters
    public UUID getPlayerId() { return playerId; }
    public ActivatedAbility getAction() { return action; }
    public MCTSNode getParent() { return parent; }
    public List<MCTSNode> getChildren() { return children; }
    public List<ActivatedAbility> getUnexploredActions() { return unexploredActions; }
    public long getStateHash() { return stateHash; }

    public int getVisits() { return visits; }
    public void setVisits(int visits) { this.visits = visits; }

    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

    public double getPriorScore() { return priorScore; }
    public void setPriorScore(double priorScore) { this.priorScore = priorScore; }

    public Map<String, Double> getMetadata() { return metadata; }
}
