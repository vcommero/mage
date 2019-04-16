package org.mage.test.cards.mana;

import mage.constants.ManaType;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author JayDi85
 */
public class TappedForManaTest extends CardTestPlayerBase {

    @Test
    public void test_SimpleManaProduce() {
        // Whenever a player taps a land for mana, that player adds one mana of any type that land produced.
        //addCard(Zone.BATTLEFIELD, playerA, "Mana Flare", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        // Mana pools don't empty as steps and phases end.
        addCard(Zone.BATTLEFIELD, playerA, "Upwelling", 1);

        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertAllCommandsUsed();

        assertManaPool(playerA, ManaType.RED, 1);
        assertTappedCount("Mountain", true, 1);
        assertTappedCount("Mountain", false, 2);
    }

    @Test
    public void test_DoubleManaProduce() {
        // Whenever a player taps a land for mana, that player adds one mana of any type that land produced.
        addCard(Zone.BATTLEFIELD, playerA, "Mana Flare", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        // Mana pools don't empty as steps and phases end.
        addCard(Zone.BATTLEFIELD, playerA, "Upwelling", 1);

        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertAllCommandsUsed();

        assertManaPool(playerA, ManaType.RED, 2);
        assertTappedCount("Mountain", true, 1);
        assertTappedCount("Mountain", false, 2);
    }
}
