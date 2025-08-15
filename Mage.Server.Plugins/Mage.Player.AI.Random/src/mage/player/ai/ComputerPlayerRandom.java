package mage.player.ai;

import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.abilities.PlayLandAbility;
import mage.abilities.SpellAbility;
import mage.abilities.common.PlayLandAsCommanderAbility;
import mage.cards.Card;
import mage.constants.RangeOfInfluence;
import mage.filter.common.FilterCreatureForCombat;
import mage.filter.common.FilterCreaturePermanent;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.Target;
import mage.util.RandomUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Random AI: A computer player that just makes completely random decisions.
 * When given priority, it randomly decides whether to take an action or pass.
 * If it decides to take an action, it randomly selects from all available actions.
 * 
 * @author vcommero
 */
public class ComputerPlayerRandom extends ComputerPlayer {

    private static final Logger logger = Logger.getLogger(ComputerPlayerRandom.class);

    private boolean allowBadMoves;
    
    // Probability of taking an action when actions are available (0.0 to 1.0)
    private static final double ACTION_PROBABILITY = 0.3;
    
    // Probability of a creature attacking when it can (0.0 to 1.0)
    private static final double ATTACK_PROBABILITY = 0.6;
    
    // Probability of a creature blocking when it can (0.0 to 1.0)
    private static final double BLOCK_PROBABILITY = 0.4;

    public ComputerPlayerRandom(String name, RangeOfInfluence range, int skill) {
        super(name, range);
    }

    protected ComputerPlayerRandom(UUID id) {
        super(id);
    }

    public ComputerPlayerRandom(final ComputerPlayerRandom player) {
        super(player);
        this.allowBadMoves = player.allowBadMoves;
    }

    @Override
    public boolean priority(Game game) {
        logger.warn("Random player - has priority");
        game.resumeTimer(getTurnControlledBy());
        boolean result = priorityHelper(game);
        game.pauseTimer(getTurnControlledBy());
        return result;
    }

    private boolean priorityHelper(Game game) {
        logger.warn("Random player - priorityHelper");
        game.getState().setPriorityPlayerId(playerId);
        game.firePriorityEvent(playerId);
        ActivatedAbility nextAction;
        switch (game.getTurnStepType()) {
            case UNTAP:
            case UPKEEP:
            case DRAW:
                pass(game);
                return false;
            case PRECOMBAT_MAIN:
                //printBattlefieldScore(game, "Sim PRIORITY on MAIN 1");
                logger.warn("Random player - priorityHelper main1");
                nextAction = calculateNextAction(game);
                performAction(game, nextAction);
                return true;
            case BEGIN_COMBAT:
                logger.warn("Random player - priorityHelper begin-combat");
                pass(game);
                return false;
            case DECLARE_ATTACKERS:
                //printBattlefieldScore(game, "Sim PRIORITY on DECLARE ATTACKERS");
                logger.warn("Random player - priorityHelper declare-attk");
                nextAction = calculateNextAction(game);
                performAction(game, nextAction);
                return true;
            case DECLARE_BLOCKERS:
                //printBattlefieldScore(game, "Sim PRIORITY on DECLARE BLOCKERS");
                logger.warn("Random player - priorityHelper delcare-blk");
                nextAction = calculateNextAction(game);
                performAction(game, nextAction);
                return true;
            case FIRST_COMBAT_DAMAGE:
            case COMBAT_DAMAGE:
            case END_COMBAT:
                pass(game);
                return false;
            case POSTCOMBAT_MAIN:
                //printBattlefieldScore(game, "Sim PRIORITY on MAIN 2");
                logger.warn("Random player - priorityHelper main");
                nextAction = calculateNextAction(game);
                performAction(game, nextAction);
                return true;
            case END_TURN:
            case CLEANUP:
                pass(game);
                return false;
        }
        return false;
    }

    /**
     * calculateNextAction - Determines the next move the computer player should make, or if they should pass (returns null).
     */
    private ActivatedAbility calculateNextAction(Game game) {
        logger.warn("Random player - calculateNextAction");
        // Get all available actions (non-mana abilities and spells)
        List<ActivatedAbility> availableActions = getAvailableActions(game).stream()
            .filter(a -> !a.isManaAbility())
            .collect(Collectors.toList());
        logger.warn("Random player - calculateNextAction - available actions: " + availableActions.size());
        
        if (availableActions.isEmpty()) {
            // No actions available, must pass
            logger.warn("Random player - No actions available, must pass");
            return null;
        }

        // Check for available land plays. Play a random land if able.
        List<ActivatedAbility> landPlays = availableActions.stream()
            .filter(a -> a instanceof PlayLandAbility || a instanceof PlayLandAsCommanderAbility)
            .collect(Collectors.toList());
        logger.warn(String.format("Random - player: Found %d land plays", landPlays.size()));
        if (!landPlays.isEmpty()) {
            ActivatedAbility landPlay = landPlays.get(RandomUtil.nextInt(landPlays.size()));
            Card card = game.getCard(landPlay.getSourceId());
            if (card != null) {
                logger.info(String.format("%s plays land: " + card.getName(), getName()));
                return landPlay;
            }
        }
        
        // Randomly decide whether to take an action or pass
        if (RandomUtil.nextDouble() < ACTION_PROBABILITY) {
            // Take a random action
            ActivatedAbility randomAction = availableActions.get(RandomUtil.nextInt(availableActions.size()));
            
            try {
                if (randomAction instanceof SpellAbility) {
                    // Cast a spell
                    Card card = game.getCard(randomAction.getSourceId());
                    if (card != null) {
                        logger.info(String.format("%s casting: " + card.getName(), getName()));
                        return randomAction;
                    }
                } else if (randomAction instanceof ActivatedAbility) {
                    // Activate an ability
                    logger.info(String.format("%s activating ability: %s", getName(), randomAction.getRule()));
                    return randomAction;
                }
                else {
                    throw new Exception("Action isn't a spell or activated ability.");
                }
            } catch (Exception e) {
                logger.warn(String.format("%s failed to execute action, passing instead", getName()), e);
                return null;
            }
        }
        
        // Pass priority
        return null;
    }

    /**
     * performAction - Executes the passed game action(s).
     * @param game
     * @param actions
     */
    private void performAction(Game game, ActivatedAbility action) {
        logger.warn("Random player - performAction");
        if (action == null) {
            pass(game);
        } else {
            boolean usedStack = false;
            
            ActivatedAbility ability = action;
            // example: ===> SELECTED ACTION for PlayerA: Play Swamp
            /*logger.info(String.format("===> SELECTED ACTION for %s: %s",
                    getName(),
                    getAbilityAndSourceInfo(game, ability, true)
            ));*/
            if (!ability.getTargets().isEmpty()) {
                for (Target target : ability.getTargets()) {
                    for (UUID id : target.getTargets()) {
                        target.updateTarget(id, game);
                        if (!target.isNotTarget()) {
                            game.addSimultaneousEvent(GameEvent.getEvent(GameEvent.EventType.TARGETED, id, ability, ability.getControllerId()));
                        }
                    }
                }
            }
            this.activateAbility(ability, game);
            if (ability.isUsesStack()) {
                usedStack = true;
            }

            if (usedStack) {
                pass(game);
            }
        }
    
    }

    /**
     * Get all available non-mana abilities and spells that can be activated/cast
     */
    private List<ActivatedAbility> getAvailableActions(Game game) {
        logger.warn("Random player - getAvailableActions");
        List<ActivatedAbility> actions = new ArrayList<>();
        Player player = game.getPlayer(playerId);
        
        // Add castable spells, activated abilities, and lands from all zones
        actions = player.getPlayable(game, true);
        
        return actions;
    }

    @Override
    public void selectAttackers(Game game, UUID attackingPlayerId) {
        logger.warn("Random player - selectAttackers");
        if (!attackingPlayerId.equals(getId())) {
            return; // Not our turn to attack
        }
        
        // Get all creatures that can attack
        List<Permanent> availableAttackers = new ArrayList<>();
        FilterCreatureForCombat filter = new FilterCreatureForCombat();
        
        for (Permanent permanent : game.getBattlefield().getActivePermanents(filter, getId(), game)) {
            if (permanent.isControlledBy(getId()) && permanent.canAttack(null, game)) {
                availableAttackers.add(permanent);
            }
        }

        logger.warn("Random player - selectAttackers - creatures that can attack: " + availableAttackers.size());
        
        if (availableAttackers.isEmpty()) {
            return; // No creatures can attack
        }
        
        // Get all possible defenders (opponents, their planeswalkers, battles)
        List<UUID> possibleDefenders = new ArrayList<>();
        
        // Add opponent players
        for (UUID opponentId : game.getOpponents(getId())) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent != null && opponent.canBeTargetedBy(null, getId(), null, game)) {
                possibleDefenders.add(opponentId);
            }
        }
        
        // Add planeswalkers and battles controlled by opponents
        for (Permanent permanent : game.getBattlefield().getActivePermanents(new FilterCreaturePermanent(), getId(), game)) {
            if (game.getOpponents(getId()).contains(permanent.getControllerId())) {
                if (permanent.isPlaneswalker(game) || permanent.isBattle(game)) {
                    if (permanent.canBeTargetedBy(null, getId(), null, game)) {
                        possibleDefenders.add(permanent.getId());
                    }
                }
            }
        }

        logger.warn("Random player - selectAttackers - valid defenders: " + possibleDefenders.size());
        
        if (possibleDefenders.isEmpty()) {
            return; // No valid defenders
        }
        
        // Randomly decide which creatures attack and who they attack
        for (Permanent attacker : availableAttackers) {
            if (RandomUtil.nextDouble() < ATTACK_PROBABILITY) {
                // This creature will attack - pick a random defender
                UUID randomDefender = possibleDefenders.get(RandomUtil.nextInt(possibleDefenders.size()));
                
                // Verify the creature can attack this specific defender
                if (attacker.canAttack(randomDefender, game)) {
                    this.declareAttacker(attacker.getId(), randomDefender, game, false);
                    logger.warn("Random AI attacking " + getDefenderName(randomDefender, game) + 
                               " with " + attacker.getName());
                }
            }
        }
    }

    @Override
    public void selectBlockers(Ability source, Game game, UUID defendingPlayerId) {
        if (!defendingPlayerId.equals(getId())) {
            return; // Not our turn to block
        }
        
        // Get all creatures that can block
        List<Permanent> availableBlockers = new ArrayList<>();
        FilterCreatureForCombat filter = new FilterCreatureForCombat();
        
        for (Permanent permanent : game.getBattlefield().getActivePermanents(filter, getId(), game)) {
            if (permanent.canBlock(null, game)) {
                availableBlockers.add(permanent);
            }
        }
        
        if (availableBlockers.isEmpty()) {
            return; // No creatures can block
        }
        
        // Get all attacking creatures that can be blocked
        List<Permanent> attackers = new ArrayList<>();
        for (UUID attackerId : game.getCombat().getAttackers()) {
            Permanent attacker = game.getPermanent(attackerId);
            if (attacker != null) {
                // Check if any of our creatures can block this attacker
                boolean canBlock = false;
                for (Permanent blocker : availableBlockers) {
                    if (blocker.canBlock(attackerId, game)) {
                        canBlock = true;
                        break;
                    }
                }
                if (canBlock) {
                    attackers.add(attacker);
                }
            }
        }
        
        if (attackers.isEmpty()) {
            return; // No attackers can be blocked
        }
        
        // Randomly assign blockers
        List<Permanent> unusedBlockers = new ArrayList<>(availableBlockers);
        
        for (Permanent attacker : attackers) {
            // Find blockers that can block this specific attacker
            List<Permanent> validBlockers = new ArrayList<>();
            for (Permanent blocker : unusedBlockers) {
                if (blocker.canBlock(attacker.getId(), game)) {
                    validBlockers.add(blocker);
                }
            }
            
            if (!validBlockers.isEmpty() && RandomUtil.nextDouble() < BLOCK_PROBABILITY) {
                // Randomly select a blocker for this attacker
                Permanent randomBlocker = validBlockers.get(RandomUtil.nextInt(validBlockers.size()));
                
                this.declareBlocker(this.getId(), randomBlocker.getId(), attacker.getId(), game);
                unusedBlockers.remove(randomBlocker);
                
                logger.info("Random AI blocking " + attacker.getName() + 
                           " with " + randomBlocker.getName());
            }
        }
    }
    
    /**
     * Helper method to get a readable name for a defender (player, planeswalker, or battle)
     */
    private String getDefenderName(UUID defenderId, Game game) {
        Player player = game.getPlayer(defenderId);
        if (player != null) {
            return player.getName();
        }
        
        Permanent permanent = game.getPermanent(defenderId);
        if (permanent != null) {
            return permanent.getName();
        }
        
        return "Unknown defender";
    }

    @Override
    public ComputerPlayerRandom copy() {
        return new ComputerPlayerRandom(this);
    }

    @Override
    public void setAllowBadMoves(boolean allowBadMoves) {
        this.allowBadMoves = allowBadMoves;
    }

    @Override
    public String toString() {
        return "ComputerPlayerRandom AI - " + getName();
    }
}