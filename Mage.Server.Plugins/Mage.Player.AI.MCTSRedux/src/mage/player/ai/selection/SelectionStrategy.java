package mage.player.ai.selection;

import mage.game.Game;
import mage.player.ai.MCTSReduxNode;
/**
 * Strategy interface for selecting the best child node during MCTS selection phase.
 * 
 * @author vcommero
 */
public interface SelectionStrategy {
    /**
     * Selects a child node from the given parent using this strategy's algorithm.
     *
     * @param node The parent node
     * @param game Current game state (may be used for context)
     * @return The selected child node, or null if no valid selection exists
     */
    MCTSReduxNode selectChild(MCTSReduxNode node, Game game);
}
