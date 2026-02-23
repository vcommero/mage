package mage.player.ai.selection;

import mage.game.Game;
import mage.player.ai.MCTSReduxNode;
/**
 * UCB1 (Upper Confidence Bound 1) selection strategy for MCTS.
 * Formula: argmax(Q(s,a) + C * sqrt(ln(N(s)) / N(s,a)))
 */
public class Ucb1SelectionStrategy implements SelectionStrategy {
    
    private final double explorationConstant;

    public Ucb1SelectionStrategy() {
        this(1.41); // Default exploration constant (sqrt(2))
    }

    public Ucb1SelectionStrategy(double explorationConstant) {
        if (explorationConstant <= 0) {
            throw new IllegalArgumentException("Exploration constant must be positive");
        }
        this.explorationConstant = explorationConstant;
    }

    @Override
    public MCTSReduxNode selectChild(MCTSReduxNode node, Game game) {
        if (node == null || node.getChildren().isEmpty()) {
            return null;
        }

        MCTSReduxNode bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (MCTSReduxNode child : node.getChildren()) {
            double exploitation = child.getAverageScore();
            
            double exploration = 0.0;
            if (child.getVisits() == 0) {
                // Unvisited nodes get infinite exploration bonus
                exploration = Double.POSITIVE_INFINITY;
            } else {
                exploration = explorationConstant 
                    * Math.sqrt(Math.log(node.getVisits()) / child.getVisits());
            }
            
            double ucb1Value = exploitation + exploration;

            if (ucb1Value > bestValue) {
                bestValue = ucb1Value;
                bestChild = child;
            }
        }

        return bestChild;
    }
}
