
package mage.cards.n;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.AttacksAllTriggeredAbility;
import mage.abilities.condition.Condition;
import mage.abilities.condition.common.IsPhaseCondition;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.common.ActivateIfConditionActivatedAbility;
import mage.abilities.dynamicvalue.common.StaticValue;
import mage.abilities.effects.common.AdditionalCombatPhaseEffect;
import mage.abilities.effects.common.CreateTokenTargetEffect;
import mage.abilities.effects.common.UntapAllEffect;
import mage.abilities.effects.common.continuous.GainAbilityAllEffect;
import mage.abilities.keyword.HasteAbility;
import mage.abilities.keyword.LifelinkAbility;
import mage.abilities.keyword.TrampleAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.*;
import mage.filter.StaticFilters;
import mage.filter.common.FilterCreaturePermanent;
import mage.game.permanent.token.WarriorToken;

import java.util.UUID;

/**
 * @author TheElk801
 */
public final class NajeelaTheBladeBlossom extends CardImpl {

    private static final FilterCreaturePermanent filter = new FilterCreaturePermanent(SubType.WARRIOR, "Warrior");
    private static final Condition condition = new IsPhaseCondition(TurnPhase.COMBAT);

    public NajeelaTheBladeBlossom(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WARRIOR);
        this.power = new MageInt(3);
        this.toughness = new MageInt(2);

        // Whenever a Warrior attacks, you may have its controller create a 1/1 white Warrior creature token that's tapped and attacking.
        this.addAbility(new AttacksAllTriggeredAbility(
                new CreateTokenTargetEffect(new WarriorToken(), StaticValue.get(1), true, true)
                        .setText("you may have its controller create a 1/1 white Warrior creature token that's tapped and attacking"),
                true, filter, SetTargetPointer.PLAYER, false, true
        ));

        // {W}{U}{B}{R}{G}: Untap all attacking creatures. They gain trample, lifelink, and haste until end of turn. After this phase, there is an additional combat phase. Activate this ability only during combat.
        Ability ability = new ActivateIfConditionActivatedAbility(
                new UntapAllEffect(StaticFilters.FILTER_ATTACKING_CREATURES),
                new ManaCostsImpl<>("{W}{U}{B}{R}{G}"), condition
        );
        ability.addEffect(new GainAbilityAllEffect(
                TrampleAbility.getInstance(), Duration.EndOfTurn,
                StaticFilters.FILTER_ATTACKING_CREATURES
        ).setText("They gain trample"));
        ability.addEffect(new GainAbilityAllEffect(
                LifelinkAbility.getInstance(), Duration.EndOfTurn,
                StaticFilters.FILTER_ATTACKING_CREATURES
        ).setText(", lifelink"));
        ability.addEffect(new GainAbilityAllEffect(
                HasteAbility.getInstance(), Duration.EndOfTurn,
                StaticFilters.FILTER_ATTACKING_CREATURES
        ).setText(", and haste until end of turn"));
        ability.addEffect(new AdditionalCombatPhaseEffect());
        this.addAbility(ability);
    }

    private NajeelaTheBladeBlossom(final NajeelaTheBladeBlossom card) {
        super(card);
    }

    @Override
    public NajeelaTheBladeBlossom copy() {
        return new NajeelaTheBladeBlossom(this);
    }
}
