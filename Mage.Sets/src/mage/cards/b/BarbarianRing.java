package mage.cards.b;

import mage.abilities.Ability;
import mage.abilities.condition.common.ThresholdCondition;
import mage.abilities.costs.common.SacrificeSourceCost;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.common.ActivateIfConditionActivatedAbility;
import mage.abilities.effects.common.DamageControllerEffect;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.abilities.mana.RedManaAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.AbilityWord;
import mage.constants.CardType;
import mage.target.common.TargetAnyTarget;

import java.util.UUID;

/**
 * @author LevelX2
 */
public final class BarbarianRing extends CardImpl {

    public BarbarianRing(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.LAND}, "");

        // {T}: Add {R}. Barbarian Ring deals 1 damage to you.
        Ability redManaAbility = new RedManaAbility();
        redManaAbility.addEffect(new DamageControllerEffect(1));
        this.addAbility(redManaAbility);

        // Threshold - {R}, {T}, Sacrifice Barbarian Ring: Barbarian Ring deals 2 damage to any target. Activate this ability only if seven or more cards are in your graveyard.
        Ability thresholdAbility = new ActivateIfConditionActivatedAbility(
                new DamageTargetEffect(2, "it"),
                new ManaCostsImpl<>("{R}"), ThresholdCondition.instance
        );
        thresholdAbility.addCost(new TapSourceCost());
        thresholdAbility.addCost(new SacrificeSourceCost());
        thresholdAbility.addTarget(new TargetAnyTarget());
        thresholdAbility.setAbilityWord(AbilityWord.THRESHOLD);
        this.addAbility(thresholdAbility);
    }

    private BarbarianRing(final BarbarianRing card) {
        super(card);
    }

    @Override
    public BarbarianRing copy() {
        return new BarbarianRing(this);
    }
}
