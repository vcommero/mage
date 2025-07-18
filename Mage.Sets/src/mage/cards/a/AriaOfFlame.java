package mage.cards.a;

import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.common.SpellCastControllerTriggeredAbility;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.dynamicvalue.common.CountersSourceCount;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.abilities.effects.common.GainLifeAllEffect;
import mage.abilities.effects.common.counter.AddCountersSourceEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.TargetController;
import mage.counters.CounterType;
import mage.filter.StaticFilters;
import mage.target.common.TargetPlayerOrPlaneswalker;

import java.util.UUID;

/**
 * @author TheElk801
 */
public final class AriaOfFlame extends CardImpl {

    private static final DynamicValue xValue = new CountersSourceCount(CounterType.VERSE);

    public AriaOfFlame(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{2}{R}");

        // When Aria of Flame enters the battlefield, each opponent gains 10 life.
        this.addAbility(new EntersBattlefieldTriggeredAbility(new GainLifeAllEffect(10, TargetController.OPPONENT)));

        // Whenever you cast an instant or sorcery spell, put a verse counter on Aria of Flame, then it deals damage equal to the number of verse counters on it to target player or planeswalker.
        Ability ability = new SpellCastControllerTriggeredAbility(
                new AddCountersSourceEffect(CounterType.VERSE.createInstance()),
                StaticFilters.FILTER_SPELL_AN_INSTANT_OR_SORCERY,
                false
        );
        ability.addEffect(new DamageTargetEffect(xValue)
                .setText(", then it deals damage equal to the number of verse counters on it to target player or planeswalker"));
        ability.addTarget(new TargetPlayerOrPlaneswalker());
        this.addAbility(ability);
    }

    private AriaOfFlame(final AriaOfFlame card) {
        super(card);
    }

    @Override
    public AriaOfFlame copy() {
        return new AriaOfFlame(this);
    }
}
