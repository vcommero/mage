
package mage.cards.a;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.common.SacrificeSourceCost;
import mage.abilities.dynamicvalue.common.SourcePermanentPowerValue;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.PersistAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.mageobject.AbilityPredicate;
import mage.target.TargetPermanent;
import mage.target.common.TargetCreaturePermanent;

import java.util.UUID;

/**
 *
 * @author jeffwadsworth
 */
public final class AerieOuphes extends CardImpl {

    private static final FilterCreaturePermanent filter = new FilterCreaturePermanent("creature with flying");

    static {
        filter.add(new AbilityPredicate(FlyingAbility.class));
    }

    public AerieOuphes(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId,setInfo,new CardType[]{CardType.CREATURE},"{4}{G}");
        this.subtype.add(SubType.OUPHE);

        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Sacrifice Aerie Ouphes: Aerie Ouphes deals damage equal to its power to target creature with flying.
        Ability ability = new SimpleActivatedAbility(new DamageTargetEffect(SourcePermanentPowerValue.NOT_NEGATIVE)
                .setText("it deals damage equal to its power to target creature with flying"), new SacrificeSourceCost());
        ability.addTarget(new TargetPermanent(filter));
        this.addAbility(ability);

        // Persist
        this.addAbility(new PersistAbility());
    }

    private AerieOuphes(final AerieOuphes card) {
        super(card);
    }

    @Override
    public AerieOuphes copy() {
        return new AerieOuphes(this);
    }
}
