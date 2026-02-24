package mage.player.ai.selection;

import mage.game.Game;
import mage.player.ai.MCTSReduxNode;
import mage.util.RandomUtil;

/**
 * Thompson Sampling selection strategy for MCTS.
 * Approximates Bayesian posterior sampling using Beta distribution heuristics.
 * 
 * @author vcommero
 */
public class ThompsonSamplingSelectionStrategy implements SelectionStrategy {

    @Override
    public MCTSReduxNode selectChild(MCTSReduxNode node, Game game) {
        if (node == null || node.getChildren().isEmpty()) {
            return null;
        }

        // For now: use weighted random selection based on average scores
        // This is a simplified approximation of Beta sampling.
        // Future enhancement: Use actual Beta distribution with success/failure counts.

        double totalScore = 0.0;
        for (MCTSReduxNode child : node.getChildren()) {
            // Add small epsilon to avoid zero-weight issues
            totalScore += child.getAverageScore() + 1e-6;
        }

        if (totalScore <= 0) {
            return node.getChildren().get(RandomUtil.nextInt(node.getChildren().size()));
        }

        double random = RandomUtil.nextDouble() * totalScore;
        double cumulative = 0.0;

        for (MCTSReduxNode child : node.getChildren()) {
            cumulative += child.getAverageScore() + 1e-6;
            if (random <= cumulative) {
                return child;
            }
        }

        // Fallback: last child
        return node.getChildren().get(node.getChildren().size() - 1);
    }
}
