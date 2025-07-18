package mage.cards.c;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.ActivateIfConditionActivatedAbility;
import mage.abilities.condition.common.MyTurnBeforeAttackersDeclaredCondition;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.effects.common.ReturnToHandSourceEffect;
import mage.abilities.effects.common.ReturnToHandTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.filter.StaticFilters;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author fireshoes
 */
public final class CoastalWizard extends CardImpl {

    public CoastalWizard(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{U}{U}");
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(1);
        this.toughness = new MageInt(1);

        // {tap}: Return Coastal Wizard and another target creature to their owners' hands. Activate this ability only during your turn, before attackers are declared.
        Ability ability = new ActivateIfConditionActivatedAbility(
                new ReturnToHandSourceEffect(true).setText("return {this}"),
                new TapSourceCost(), MyTurnBeforeAttackersDeclaredCondition.instance
        );
        ability.addEffect(new ReturnToHandTargetEffect().setText("and another target creature to their owners' hands"));
        ability.addTarget(new TargetPermanent(StaticFilters.FILTER_ANOTHER_TARGET_CREATURE));
        this.addAbility(ability);
    }

    private CoastalWizard(final CoastalWizard card) {
        super(card);
    }

    @Override
    public CoastalWizard copy() {
        return new CoastalWizard(this);
    }
}
