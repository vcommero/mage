
package mage.cards.r;

import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DamageEverythingEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.filter.StaticFilters;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.Predicates;
import mage.filter.predicate.mageobject.AbilityPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetSacrifice;

import java.util.UUID;

/**
 * @author L_J
 */
public final class Rupture extends CardImpl {

    public Rupture(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{2}{R}");

        // Sacrifice a creature. Rupture deals damage equal to that creature's power to each creature without flying and each player.
        this.getSpellAbility().addEffect(new RuptureEffect());
    }

    private Rupture(final Rupture card) {
        super(card);
    }

    @Override
    public Rupture copy() {
        return new Rupture(this);
    }
}

class RuptureEffect extends OneShotEffect {

    private static final FilterCreaturePermanent filter = new FilterCreaturePermanent("creature without flying");

    static {
        filter.add(Predicates.not(new AbilityPredicate(FlyingAbility.class)));
    }

    public RuptureEffect() {
        super(Outcome.Damage);
        staticText = "Sacrifice a creature. {this} deals damage equal to that creature's power to each creature without flying and each player";
    }

    private RuptureEffect(final RuptureEffect effect) {
        super(effect);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player != null) {
            int power = 0;
            TargetSacrifice target = new TargetSacrifice(StaticFilters.FILTER_PERMANENT_CREATURE);
            if (player.choose(Outcome.Sacrifice, target, source, game)){
                Permanent permanent = game.getPermanent(target.getFirstTarget());
                if (permanent != null) {
                    power = permanent.getPower().getValue();
                    permanent.sacrifice(source, game);
                }
            }
            if (power > 0) {
                new DamageEverythingEffect(power, filter).apply(game, source);
            }
            return true;
        }
        return false;
    }

    @Override
    public RuptureEffect copy() {
        return new RuptureEffect(this);
    }
}
