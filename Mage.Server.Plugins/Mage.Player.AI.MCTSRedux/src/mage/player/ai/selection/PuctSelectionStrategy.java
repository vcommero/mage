package mage.player.ai.selection;

import mage.game.Game;
import mage.player.ai.MCTSReduxNode;
/**
 * PUCT (Predictor + UCT) selection strategy for MCTS.
 * Formula: argmax(Q(s,a) + C * P(s,a) * sqrt(N(s)) / (1 + N(s,a)))
 *
 * @author vcommero
 */
public class PuctSelectionStrategy implements SelectionStrategy {

    private final double explorationConstant;

    public PuctSelectionStrategy() {
        this(1.0); // Typical C value in AlphaZero
    }

    public PuctSelectionStrategy(double explorationConstant) {
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

        double totalVisits = Math.max(1, node.getVisits());
        MCTSReduxNode bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (MCTSReduxNode child : node.getChildren()) {
            // Q-value (exploitation)
            double qValue = child.getAverageScore();

            // Prior probability — use stored priorScore as placeholder
            double pPrior = Math.max(1e-6, child.getPriorScore());

            // Exploration term: C * P(s,a) * sqrt(N(s)) / (1 + N(s,a))
            double exploration = explorationConstant 
                * pPrior 
                * Math.sqrt(totalVisits) / (1.0 + child.getVisits());

            double value = qValue + exploration;

            if (value > bestValue) {
                bestValue = value;
                bestChild = child;
            }
        }

        return bestChild;
    }
}
