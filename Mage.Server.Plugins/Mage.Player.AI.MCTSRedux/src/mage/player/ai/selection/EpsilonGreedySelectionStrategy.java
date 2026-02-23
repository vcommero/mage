package mage.player.ai.selection;

import mage.game.Game;
import mage.player.ai.MCTSReduxNode;
import mage.util.RandomUtil;

/**
 * Epsilon-greedy selection strategy for MCTS.
 * With probability epsilon: random child; otherwise: best average score.
 */
public class EpsilonGreedySelectionStrategy implements SelectionStrategy {

    private final double epsilon;

    public EpsilonGreedySelectionStrategy() {
        this(0.1); // Default 10% exploration
    }

    public EpsilonGreedySelectionStrategy(double epsilon) {
        if (epsilon < 0 || epsilon > 1) {
            throw new IllegalArgumentException("Epsilon must be in [0, 1]");
        }
        this.epsilon = epsilon;
    }

    @Override
    public MCTSReduxNode selectChild(MCTSReduxNode node, Game game) {
        if (node == null || node.getChildren().isEmpty()) {
            return null;
        }

        if (RandomUtil.nextDouble() < epsilon) {
            // Explore: random selection
            int index = RandomUtil.nextInt(node.getChildren().size());
            return node.getChildren().get(index);
        } else {
            // Exploit: best average score
            return node.getChildren().stream()
                    .max((c1, c2) -> Double.compare(c1.getAverageScore(), c2.getAverageScore()))
                    .orElse(null);
        }
    }
}
