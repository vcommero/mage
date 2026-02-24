package mage.player.ai.util;

import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;

/**
 * Utility class for computing state hashes for MCTS transposition detection.
 * 
 * @author vcommero
 */
public class StateHasher {

    /**
     * Compute a hash representing the current game state.
     * Based on: battlefield permanents (ID + tapped state), life totals, turn/phase.
     */
    public static long computeStateHash(Game game) {
        long hash = 17;
        
        // Hash battlefield
        for (Permanent permanent : game.getBattlefield().getAllActivePermanents()) {
            hash = 31 * hash + permanent.getId().hashCode();
            hash = 31 * hash + (permanent.isTapped() ? 1 : 0);
        }
        
        // Hash life totals
        for (Player player : game.getPlayers().values()) {
            hash = 31 * hash + player.getLife();
        }
        
        // Hash turn/phase
        hash = 31 * hash + game.getTurnNum();
        hash = 31 * hash + game.getStep().getType().hashCode();
        
        return hash;
    }
}