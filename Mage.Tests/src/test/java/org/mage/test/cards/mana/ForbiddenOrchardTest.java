package org.mage.test.cards.mana;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 *
 * @author LevelX2
 */
public class ForbiddenOrchardTest extends CardTestPlayerBase {

    @Test
    public void testSpiritCreation() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);

        // {T}: Add one mana of any color.
        // Whenever you tap Forbidden Orchard for mana, create a 1/1 colorless Spirit creature token under target opponent's control.
        addCard(Zone.BATTLEFIELD, playerA, "Forbidden Orchard", 1);
        addCard(Zone.HAND, playerA, "Silvercoat Lion", 1);

        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add one mana of any color");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Silvercoat Lion");
        setChoice(playerA, "White");
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();
        assertAllCommandsUsed();

        assertPermanentCount(playerA, "Silvercoat Lion", 1);

        assertPermanentCount(playerB, "Spirit", 1);
        assertPermanentCount(playerA, "Spirit", 0);

    }

}
