package org.mage.test.cards.asthough;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.player.ai.ComputerPlayer7;
import org.junit.Ignore;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBaseWithAIHelps;

/**
 * @author JayDi85
 */
public class PlayTopCardFromLibraryTest extends CardTestPlayerBaseWithAIHelps {

    /*
    Bolas's Citadel
    {3}{B}{B}{B}
    You may look at the top card of your library any time.
    You may play the top card of your library. If you cast a spell this way, pay life equal to its converted mana cost rather than pay its mana cost.
    {T}, Sacrifice ten nonland permanents: Each opponent loses 10 life.
     */

    @Test
    public void test_BolassCitadel_CreaturePlay() {
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Balduvian Bears", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Bolas's Citadel", 1);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Balduvian Bears"); // 2 CMC

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Balduvian Bears", 1);
        assertLife(playerA, 20 - 2);
    }

    @Test
    public void test_Vizier_CreaturePlay() {
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Balduvian Bears", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Vizier of the Menagerie", 1);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Balduvian Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Balduvian Bears", 1);
    }

    @Test
    public void test_ManaCostmodifications() {
        //
        // {5}{B}{B}
        // You may cast Scourge of Nel Toth from your graveyard by paying {B}{B} and sacrificing two creatures rather than paying its mana cost.
        addCard(Zone.GRAVEYARD, playerA, "Scourge of Nel Toth", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Kitesail Corsair", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Scourge of Nel Toth");
        setChoice(playerA, "Kitesail Corsair");
        setChoice(playerA, "Kitesail Corsair");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Scourge of Nel Toth", 1);
        assertLife(playerA, 20);
    }

    @Test
    public void test_BolassCitadel_SplitRightPlay() {
        // https://github.com/magefree/mage/issues/5912
        // Bolas's citadel requires you to pay mana instead of life for a split card on top of library.
        //
        // Steps to reproduce:
        //
        //    Bolas's Citadel in play, Revival//Revenge on top of library.
        //    Cast Revenge, choose target
        //    receive prompt to pay 4WB.
        //
        // Expected outcome
        //
        //    No prompt for mana payment, payment of six life instead.

        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Revival // Revenge", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Bolas's Citadel", 1);

        // Double your life total. Target opponent loses half their life, rounded up.
        checkPlayableAbility("playable", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Revenge", true);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Revenge", playerB); // {4}{W}{B} = 6 life

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, (20 - 6) * 2);
        assertLife(playerB, 20 / 2);
    }

    @Test
    public void test_BolassCitadel_SplitLeftPlay() {
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Revival // Revenge", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Bolas's Citadel", 1);
        addCard(Zone.GRAVEYARD, playerA, "Balduvian Bears", 1);

        // Return target creature card with converted mana cost 3 or less from your graveyard to the battlefield.
        checkPlayableAbility("playable", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Revival", true);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Revival", "Balduvian Bears"); // {W/B}{W/B} = 2 life

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 - 2);
        assertLife(playerB, 20);
        assertGraveyardCount(playerA, "Balduvian Bears", 0);
        assertPermanentCount(playerA, "Balduvian Bears", 1);
    }

    @Test
    public void test_MindsDesire_CreaturePlay() {
        removeAllCardsFromLibrary(playerA);

        addCard(Zone.HAND, playerA, "Mind's Desire", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 6);
        //
        addCard(Zone.LIBRARY, playerA, "Balduvian Bears", 1);

        // prepare mind
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mind's Desire");

        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Balduvian Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Balduvian Bears", 1);
        assertLife(playerA, 20);
    }

    @Test
    public void test_MindsDesire_LandPlay() {
        removeAllCardsFromLibrary(playerA);
        removeAllCardsFromHand(playerA);

        addCard(Zone.HAND, playerA, "Mind's Desire", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 6);
        //
        addCard(Zone.LIBRARY, playerA, "Swamp", 1);

        // prepare mind
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mind's Desire");

        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Swamp");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Swamp", 1);
        assertLife(playerA, 20);
    }

    @Test
    public void test_MindsDesire_SplitRightPlay() {
        removeAllCardsFromLibrary(playerA);

        // Shuffle your library. Then exile the top card of your library. Until end of turn, you may play that card without paying its mana cost.
        // Storm (When you cast this spell, copy it for each spell cast before it this turn.)
        addCard(Zone.HAND, playerA, "Mind's Desire", 1); // {4}{U}{U}
        addCard(Zone.BATTLEFIELD, playerA, "Island", 6);
        //
        addCard(Zone.LIBRARY, playerA, "Revival // Revenge", 1);

        // prepare mind
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mind's Desire");

        // Double your life total. Target opponent loses half their life, rounded up.
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Revenge", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 * 2);
        assertLife(playerB, 20 / 2);
    }

    @Test
    public void test_MindsDesire_SplitLeftPlay() {
        removeAllCardsFromLibrary(playerA);

        addCard(Zone.HAND, playerA, "Mind's Desire", 1); // {4}{U}{U}
        addCard(Zone.BATTLEFIELD, playerA, "Island", 6);
        //
        addCard(Zone.LIBRARY, playerA, "Revival // Revenge", 1);
        addCard(Zone.GRAVEYARD, playerA, "Balduvian Bears", 1);

        // prepare mind
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mind's Desire", true);

        // Return target creature card with converted mana cost 3 or less from your graveyard to the battlefield.
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Revival", "Balduvian Bears"); // {W/B}{W/B} = 2 life

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20);
        assertLife(playerB, 20);
        assertGraveyardCount(playerA, "Balduvian Bears", 0);
        assertPermanentCount(playerA, "Balduvian Bears", 1);
    }

    @Test
    public void test_EtaliPrimalStorm_NoCards_Manual() {
        removeAllCardsFromLibrary(playerA);
        removeAllCardsFromLibrary(playerB);

        // Whenever Etali, Primal Storm attacks, exile the top card of each player's library,
        // then you may cast any number of nonland cards exiled this way without paying their mana costs.
        addCard(Zone.BATTLEFIELD, playerA, "Etali, Primal Storm", 1); // 6/6
        //
        addCard(Zone.LIBRARY, playerA, "Forest", 1);
        addCard(Zone.LIBRARY, playerB, "Forest", 1);

        // nothing to free cast
        attack(1, playerA, "Etali, Primal Storm");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 6);
    }

    @Test
    public void test_EtaliPrimalStorm_NoCards_AI() {
        removeAllCardsFromLibrary(playerA);
        removeAllCardsFromLibrary(playerB);

        // Whenever Etali, Primal Storm attacks, exile the top card of each player's library,
        // then you may cast any number of nonland cards exiled this way without paying their mana costs.
        addCard(Zone.BATTLEFIELD, playerA, "Etali, Primal Storm", 1); // 6/6
        //
        addCard(Zone.LIBRARY, playerA, "Forest", 1);
        addCard(Zone.LIBRARY, playerB, "Forest", 1);

        // ai must attack and nothing to free cast
        attack(1, playerA, "Etali, Primal Storm");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 6);
    }

    @Test
    public void test_EtaliPrimalStorm_OneCard_Manual() {
        removeAllCardsFromLibrary(playerA);
        removeAllCardsFromLibrary(playerB);

        // Whenever Etali, Primal Storm attacks, exile the top card of each player's library,
        // then you may cast any number of nonland cards exiled this way without paying their mana costs.
        addCard(Zone.BATTLEFIELD, playerA, "Etali, Primal Storm", 1); // 6/6
        //
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt", 1);
        addCard(Zone.LIBRARY, playerB, "Forest", 1);

        attack(1, playerA, "Etali, Primal Storm");
        setChoice(playerA, true); // use free cast
        addTarget(playerA, playerB); // to damage

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 6 - 3);
    }

    @Test
    public void test_EtaliPrimalStorm_OneCard_AI() {
        removeAllCardsFromLibrary(playerA);
        removeAllCardsFromLibrary(playerB);

        // Whenever Etali, Primal Storm attacks, exile the top card of each player's library,
        // then you may cast any number of nonland cards exiled this way without paying their mana costs.
        addCard(Zone.BATTLEFIELD, playerA, "Etali, Primal Storm", 1); // 6/6
        //
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt", 1);
        addCard(Zone.LIBRARY, playerB, "Forest", 1);

        // ai must attack and free cast bolt to opponent's
        aiPlayStep(1, PhaseStep.DECLARE_ATTACKERS, playerA);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 6 - 3);
    }

    @Test
    public void test_EtaliPrimalStorm_MultipleCards_Manual() {
        removeAllCardsFromLibrary(playerA);
        removeAllCardsFromLibrary(playerB);

        // Whenever Etali, Primal Storm attacks, exile the top card of each player's library,
        // then you may cast any number of nonland cards exiled this way without paying their mana costs.
        addCard(Zone.BATTLEFIELD, playerA, "Etali, Primal Storm", 1); // 6/6
        //
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt", 1); // 3 damage
        addCard(Zone.LIBRARY, playerB, "Cleansing Screech", 1); // 4 damage

        // choose cards one by one
        attack(1, playerA, "Etali, Primal Storm");
        // first card
        setChoice(playerA, "Cleansing Screech");
        setChoice(playerA, true); // use free
        addTarget(playerA, playerB);
        // last card (auto-chosen)
        setChoice(playerA, true); // use free
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 6 - 4 - 3);
    }

    @Test
    public void test_EtaliPrimalStorm_MultipleCards_AI() {
        removeAllCardsFromLibrary(playerA);
        removeAllCardsFromLibrary(playerB);

        // Whenever Etali, Primal Storm attacks, exile the top card of each player's library,
        // then you may cast any number of nonland cards exiled this way without paying their mana costs.
        addCard(Zone.BATTLEFIELD, playerA, "Etali, Primal Storm", 1); // 6/6
        //
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt", 1); // 3 damage
        addCard(Zone.LIBRARY, playerB, "Cleansing Screech", 1); // 4 damage

        // ai must attack and free cast two cards
        // possible bug 1: game freeze due wrong dialog/selection logic
        // possible bug 2: TargetCard can't find ALL zone
        aiPlayStep(1, PhaseStep.DECLARE_ATTACKERS, playerA);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 6 - 4 - 3);
    }
}
