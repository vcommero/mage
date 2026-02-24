package mage.player.ai;

import mage.abilities.Ability;
import mage.abilities.keyword.*;
import mage.counters.CounterType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.player.ai.util.CombatInfo;
import mage.player.ai.util.CombatUtil;
import mage.players.Player;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Static utility class that handles combat declaration logic for AI players.
 * Encapsulates attacker/blocker selection heuristics so they can be shared
 * across different AI player implementations.
 * 
 * This was adapted from the combat logic used in the MAD AI player by 
 * nantuko and JayDi85.
 *
 * Strategy overview:
 *  - Attacking: alpha-strike check first, then static P/T safety analysis
 *               with keyword awareness (Vigilance, Lifelink, Deathtouch, etc.)
 *  - Blocking:  CombatUtil.blockWithGoodTrade2 handles trade analysis;
 *               unblockable and non-blocking creatures are filtered out first.
 * 
 * @author vcommero
 */
public class CombatExecutor {

    private static final Logger logger = Logger.getLogger(CombatExecutor.class);

    private CombatExecutor() {}

    // =========================================================================
    // Public Entry Points
    // =========================================================================

    /**
     * Selects and declares attackers for the given AI player.
     *
     * @param aiPlayer          The AI player declaring attackers.
     * @param game              The current game state.
     * @param attackingPlayerId UUID of the attacking player (should equal aiPlayer.getId()).
     */
    public static void declareAttackers(Player aiPlayer, Game game, UUID attackingPlayerId) {
        Player attackingPlayer = game.getPlayer(attackingPlayerId);
        if (attackingPlayer == null) {
            return;
        }

        // Respect game events that may prevent or replace attacking
        // (e.g. "creatures can't attack this turn" effects).
        game.fireEvent(new GameEvent(GameEvent.EventType.DECLARE_ATTACKERS_STEP_PRE,
                null, null, attackingPlayerId));
        if (game.replaceEvent(GameEvent.getEvent(
                GameEvent.EventType.DECLARING_ATTACKERS, attackingPlayerId, attackingPlayerId))) {
            return;
        }

        UUID playerId = aiPlayer.getId();

        // --- Phase 1: Alpha Strike ---
        // If the full attacker pool can push through lethal damage right now, commit
        // everything and return immediately — no need for further analysis.
        for (UUID defenderId : game.getOpponents(playerId, true)) {
            Player defender = game.getPlayer(defenderId);
            if (defender == null || !defender.isInGame()) {
                continue;
            }
            List<Permanent> availableAttackers = aiPlayer.getAvailableAttackers(defenderId, game);
            if (availableAttackers.isEmpty()) {
                continue;
            }
            List<Permanent> possibleBlockers = defender.getAvailableBlockers(game);
            List<Permanent> killers = CombatUtil.canKillOpponent(
                    game, availableAttackers, possibleBlockers, defender);
            if (!killers.isEmpty()) {
                logger.info("CombatExecutor - Alpha strike against " + defender.getName());
                for (Permanent attacker : killers) {
                    attackingPlayer.declareAttacker(attacker.getId(), defenderId, game, false);
                }
                return;
            }
        }

        // --- Phase 2: Safe Attacker Analysis ---
        // For each opponent, evaluate which of our creatures can safely attack
        // without being profitably blocked, then assign them to targets.
        List<Permanent> attackersToCheck = new ArrayList<>();

        for (UUID defenderId : game.getOpponents(playerId, true)) {
            Player defender = game.getPlayer(defenderId);
            if (defender == null || !defender.isInGame()) {
                continue;
            }
            List<Permanent> availableAttackers = aiPlayer.getAvailableAttackers(defenderId, game);
            if (availableAttackers.isEmpty()) {
                continue;
            }
            List<Permanent> possibleBlockers = defender.getAvailableBlockers(game);
            CombatEvaluator eval = new CombatEvaluator();
            attackersToCheck.clear();

            for (Permanent attacker : availableAttackers) {
                // No power means no damage — attacking is pointless.
                if (attacker.getPower().getValue() == 0) {
                    continue;
                }

                // Vigilance: attacker doesn't tap when attacking, so it can still block
                // afterwards. Always worth sending in regardless of blockers.
                if (attacker.getAbilities().containsKey(VigilanceAbility.getInstance().getId())) {
                    attackersToCheck.add(attacker);
                    continue;
                }

                boolean safeToAttack = true;
                int attackerValue = eval.evaluate(attacker, game);

                for (Permanent blocker : possibleBlockers) {
                    int blockerValue = eval.evaluate(blocker, game);

                    // Blocker kills the attacker outright (attacker cannot kill it back).
                    if (attacker.getToughness().getValue() <= blocker.getPower().getValue()
                            && attacker.getPower().getValue() <= blocker.getToughness().getValue()) {
                        safeToAttack = false;
                    }

                    // Equal P/T trade: determine who actually wins the exchange.
                    if (attacker.getToughness().getValue() == blocker.getPower().getValue()
                            && attacker.getPower().getValue() == blocker.getToughness().getValue()) {
                        boolean blockerWins = false;

                        // Blocker has a damage-ordering advantage the attacker lacks.
                        if ((blocker.getAbilities().containsKey(FirstStrikeAbility.getInstance().getId())
                                || blocker.getAbilities().containsKey(DoubleStrikeAbility.getInstance().getId()))
                                && !attacker.getAbilities().containsKey(FirstStrikeAbility.getInstance().getId())
                                && !attacker.getAbilities().containsKey(DoubleStrikeAbility.getInstance().getId())) {
                            blockerWins = true;
                        }
                        // Blocker's Deathtouch kills the attacker regardless of toughness.
                        if (blocker.getAbilities().containsKey(DeathtouchAbility.getInstance().getId())) {
                            blockerWins = true;
                        }
                        // Blocker survives the trade.
                        if (blocker.getAbilities().containsKey(IndestructibleAbility.getInstance().getId())) {
                            blockerWins = true;
                        }
                        // Blocker is a more valuable permanent overall.
                        if (blockerValue > attackerValue) {
                            blockerWins = true;
                        }

                        // Lifelink override: even a dead-even trade is net positive
                        // because we gain life equal to the damage dealt before dying.
                        if (attacker.getAbilities().containsKey(LifelinkAbility.getInstance().getId())) {
                            blockerWins = false;
                        }

                        if (blockerWins) {
                            safeToAttack = false;
                        }
                    }

                    // Attacker has Deathtouch or Indestructible: it wins or survives
                    // any blocking scenario, so override any earlier unsafe flag.
                    if (attacker.getAbilities().containsKey(DeathtouchAbility.getInstance().getId())
                            || attacker.getAbilities().containsKey(IndestructibleAbility.getInstance().getId())) {
                        safeToAttack = true;
                    }

                    // Attacker has Flying and the blocker cannot reach it —
                    // the block simply won't happen.
                    if (attacker.getAbilities().containsKey(FlyingAbility.getInstance().getId())
                            && !blocker.getAbilities().containsKey(FlyingAbility.getInstance().getId())
                            && !blocker.getAbilities().containsKey(ReachAbility.getInstance().getId())) {
                        safeToAttack = true;
                    }

                    // No point checking more blockers once we've confirmed unsafe.
                    if (!safeToAttack) {
                        break;
                    }
                }

                if (safeToAttack) {
                    attackersToCheck.add(attacker);
                }
            }

            // --- Phase 3: Assign Safe Attackers to Targets ---
            int usedPower = 0;
            int totalPower = attackersToCheck.stream()
                    .mapToInt(a -> a.getPower().getValue())
                    .sum();

            // Priority 1: Planeswalkers (drain loyalty before they ultimate).
            List<Permanent> permanentDefenders = new ArrayList<>();
            game.getBattlefield()
                    .getActivePermanents(StaticFilters.FILTER_PERMANENT_PLANESWALKER, attackingPlayerId, game)
                    .stream()
                    .filter(p -> p.canBeAttacked(null, defenderId, game))
                    .forEach(permanentDefenders::add);

            // Priority 2: Battles.
            game.getBattlefield()
                    .getActivePermanents(StaticFilters.FILTER_PERMANENT_BATTLE, attackingPlayerId, game)
                    .stream()
                    .filter(p -> p.canBeAttacked(null, defenderId, game))
                    .forEach(permanentDefenders::add);

            for (Permanent permanentDefender : permanentDefenders) {
                if (usedPower >= totalPower) {
                    break;
                }
                int counters;
                if (permanentDefender.isPlaneswalker(game)) {
                    counters = permanentDefender.getCounters(game).getCount(CounterType.LOYALTY);
                } else if (permanentDefender.isBattle(game)) {
                    counters = permanentDefender.getCounters(game).getCount(CounterType.DEFENSE);
                } else {
                    continue;
                }
                for (Permanent attackingPermanent : attackersToCheck) {
                    if (attackingPermanent.isAttacking()) {
                        continue;
                    }
                    attackingPlayer.declareAttacker(
                            attackingPermanent.getId(), permanentDefender.getId(), game, true);
                    counters -= attackingPermanent.getPower().getValue();
                    usedPower += attackingPermanent.getPower().getValue();
                    if (counters <= 0) {
                        break;
                    }
                }
            }

            // Priority 3: Any remaining safe attackers go straight for the player.
            for (Permanent attackingPermanent : attackersToCheck) {
                if (!attackingPermanent.isAttacking()) {
                    attackingPlayer.declareAttacker(
                            attackingPermanent.getId(), defenderId, game, true);
                }
            }
        }
    }

    /**
     * Selects and declares blockers for the given AI player.
     *
     * @param aiPlayer          The AI player declaring blockers.
     * @param source            The ability source passed through from the override.
     * @param game              The current game state.
     * @param defendingPlayerId UUID of the defending player (should equal aiPlayer.getId()).
     */
    public static void declareBlockers(Player aiPlayer, Ability source, Game game, UUID defendingPlayerId) {
        UUID playerId = aiPlayer.getId();

        // Respect game events that may prevent or replace blocking.
        game.fireEvent(new GameEvent(GameEvent.EventType.DECLARE_BLOCKERS_STEP_PRE,
                null, null, defendingPlayerId));
        if (game.replaceEvent(GameEvent.getEvent(
                GameEvent.EventType.DECLARING_BLOCKERS, defendingPlayerId, defendingPlayerId))) {
            return;
        }

        List<Permanent> attackers = getAttackersFromCombat(game);
        if (attackers == null || attackers.isEmpty()) {
            return;
        }

        // Remove blockers that can't legally block any current attacker.
        List<Permanent> possibleBlockers = aiPlayer.getAvailableBlockers(game);
        possibleBlockers = filterOutNonblocking(game, attackers, possibleBlockers);
        if (possibleBlockers.isEmpty()) {
            return;
        }

        // Remove attackers that can't be blocked at all (e.g. unblockable, shadow).
        attackers = filterOutUnblockable(game, attackers, possibleBlockers);
        if (attackers.isEmpty()) {
            return;
        }

        // Sort by power descending so CombatUtil prioritizes the largest threats.
        CombatUtil.sortByPower(attackers, false);
        CombatInfo combatInfo = CombatUtil.blockWithGoodTrade2(game, attackers, possibleBlockers);

        Player player = game.getPlayer(playerId);
        if (player == null) {
            return;
        }

        boolean blocked = false;
        for (Map.Entry<Permanent, List<Permanent>> entry : combatInfo.getCombat().entrySet()) {
            UUID attackerId = entry.getKey().getId();
            List<Permanent> blockers = entry.getValue();
            if (blockers != null) {
                for (Permanent blocker : blockers) {
                    player.declareBlocker(player.getId(), blocker.getId(), attackerId, game);
                    blocked = true;
                }
            }
        }
        if (blocked) {
            game.getPlayers().resetPassed();
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Collects all permanents currently declared as attackers in the active combat.
     */
    private static List<Permanent> getAttackersFromCombat(Game game) {
        Set<UUID> attackerUUIDs = game.getCombat().getAttackers();
        if (attackerUUIDs.isEmpty()) {
            return null;
        }
        List<Permanent> attackers = new ArrayList<>();
        for (UUID attackerId : attackerUUIDs) {
            Permanent permanent = game.getPermanent(attackerId);
            if (permanent != null) {
                attackers.add(permanent);
            }
        }
        return attackers;
    }

    /**
     * Returns only the blockers that can legally block at least one attacker.
     */
    private static List<Permanent> filterOutNonblocking(Game game,
                                                         List<Permanent> attackers,
                                                         List<Permanent> blockers) {
        List<Permanent> blockersLeft = new ArrayList<>();
        for (Permanent blocker : blockers) {
            for (Permanent attacker : attackers) {
                if (blocker.canBlock(attacker.getId(), game)) {
                    blockersLeft.add(blocker);
                    break;
                }
            }
        }
        return blockersLeft;
    }

    /**
     * Returns only the attackers that can actually be blocked by at least one available blocker.
     */
    private static List<Permanent> filterOutUnblockable(Game game,
                                                          List<Permanent> attackers,
                                                          List<Permanent> blockers) {
        List<Permanent> attackersLeft = new ArrayList<>();
        for (Permanent attacker : attackers) {
            if (CombatUtil.canBeBlocked(game, attacker, blockers)) {
                attackersLeft.add(attacker);
            }
        }
        return attackersLeft;
    }
}
