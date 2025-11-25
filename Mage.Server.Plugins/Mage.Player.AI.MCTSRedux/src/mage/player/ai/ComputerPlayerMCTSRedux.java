package mage.player.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.constants.RangeOfInfluence;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.players.Player;
import mage.target.Target;

public class ComputerPlayerMCTSRedux extends ComputerPlayer {

    private static final Logger logger = Logger.getLogger(ComputerPlayerMCTSRedux.class);

    public ComputerPlayerMCTSRedux(String name, RangeOfInfluence range) {
        super(name, range);
        //TODO Auto-generated constructor stub
    }
    protected ComputerPlayerMCTSRedux(UUID id) {
        super(id);
    }

    public ComputerPlayerMCTSRedux(final ComputerPlayerMCTSRedux player) {
        super(player);
    }

    @Override
    public ComputerPlayerMCTSRedux copy() {
        return new ComputerPlayerMCTSRedux(this);
    }


    @Override
    public boolean priority(Game game) {
        logger.warn("Random player - has priority");
        game.resumeTimer(getTurnControlledBy());
        boolean result = priorityHelper(game);
        game.pauseTimer(getTurnControlledBy());
        return result;
    }

    @Override
    public void selectAttackers(Game game, UUID attackingPlayerId) {
        // TODO
    }

    @Override
    public void selectBlockers(Ability source, Game game, UUID defendingPlayerId) {
        // TODO
    }
    
    private boolean priorityHelper(Game game) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'priorityHelper'");
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
}