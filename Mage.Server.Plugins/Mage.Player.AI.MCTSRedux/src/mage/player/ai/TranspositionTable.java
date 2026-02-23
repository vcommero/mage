package mage.player.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple wrapper around a transposition table (state → node mapping).
 * Enables reuse and future enhancements (e.g., pruning, stats).
 */
public class TranspositionTable {
    private final Map<Long, MCTSReduxNode> table;

    public TranspositionTable() {
        this.table = new ConcurrentHashMap<>();
    }

    public MCTSReduxNode get(long stateHash) {
        return table.get(stateHash);
    }

    public void put(long stateHash, MCTSReduxNode node) {
        table.put(stateHash, node);
    }

    public boolean containsKey(long stateHash) {
        return table.containsKey(stateHash);
    }

    public void clear() {
        table.clear();
    }

    public int size() {
        return table.size();
    }
}