
package mage.cards.c;

import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.common.AsEntersBattlefieldAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.common.SkipUntapOptionalAbility;
import mage.abilities.condition.common.SourceTappedCondition;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.decorator.ConditionalContinuousEffect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.continuous.GainControlTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.choices.Choice;
import mage.choices.ChoiceCreatureType;
import mage.constants.*;
import mage.filter.common.FilterCreaturePermanent;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;
import mage.target.common.TargetCreaturePermanent;
import mage.target.common.TargetOpponent;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author L_J
 */
public final class CallousOppressor extends CardImpl {

    public CallousOppressor(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{U}{U}");
        this.subtype.add(SubType.OCTOPUS);
        this.power = new MageInt(1);
        this.toughness = new MageInt(2);

        // You may choose not to untap Callous Oppressor during your untap step.
        this.addAbility(new SkipUntapOptionalAbility());

        // As Callous Oppressor enters the battlefield, an opponent chooses a creature type.
        this.addAbility(new AsEntersBattlefieldAbility(new CallousOppressorChooseCreatureTypeEffect()));

        // {T}: Gain control of target creature that isn't of the chosen type for as long as Callous Oppressor remains tapped.
        ConditionalContinuousEffect effect = new ConditionalContinuousEffect(
                new GainControlTargetEffect(Duration.OneUse),
                SourceTappedCondition.TAPPED,
                "Gain control of target creature that isn't of the chosen type for as long as {this} remains tapped");
        Ability ability = new SimpleActivatedAbility(effect, new TapSourceCost());
        ability.addTarget(new TargetPermanent(new CallousOppressorFilter()));
        this.addAbility(ability);
    }

    private CallousOppressor(final CallousOppressor card) {
        super(card);
    }

    @Override
    public CallousOppressor copy() {
        return new CallousOppressor(this);
    }
}

class CallousOppressorFilter extends FilterCreaturePermanent {

    public CallousOppressorFilter() {
        super("creature that isn't of the chosen type");
    }

    private CallousOppressorFilter(final CallousOppressorFilter filter) {
        super(filter);
    }

    @Override
    public CallousOppressorFilter copy() {
        return new CallousOppressorFilter(this);
    }

    @Override
    public boolean match(Permanent permanent, UUID playerId, Ability source, Game game) {
        if (super.match(permanent, playerId, source, game)) {
            SubType subtype = (SubType) game.getState().getValue(source.getSourceId() + "_type");
            if (subtype != null && permanent.hasSubtype(subtype, game)) {
                return false;
            }
            return true;
        }
        return false;
    }

}

class CallousOppressorChooseCreatureTypeEffect extends OneShotEffect {

    CallousOppressorChooseCreatureTypeEffect() {
        super(Outcome.Benefit);
        staticText = "an opponent chooses a creature type";
    }

    private CallousOppressorChooseCreatureTypeEffect(final CallousOppressorChooseCreatureTypeEffect effect) {
        super(effect);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        MageObject mageObject = game.getPermanentEntering(source.getSourceId());
        if (mageObject == null) {
            mageObject = game.getObject(source);
        }
        if (controller != null) {
            TargetOpponent target = new TargetOpponent(true);
            if (target.canChoose(controller.getId(), source, game)) {
                while (!target.isChosen(game) && target.canChoose(controller.getId(), source, game) && controller.canRespond()) {
                    controller.chooseTarget(outcome, target, source, game);
                }
            } else {
                return false;
            }
            Player opponent = game.getPlayer(target.getFirstTarget());
            if (opponent != null && mageObject != null) {
                Choice typeChoice = new ChoiceCreatureType(game, source);
                if (!opponent.choose(outcome, typeChoice, game)) {
                    return false;
                }
                if (typeChoice.getChoiceKey() == null) {
                    return false;
                }
                if (!game.isSimulation()) {
                    game.informPlayers(mageObject.getName() + ": " + opponent.getLogName() + " has chosen " + typeChoice.getChoiceKey());
                }
                game.getState().setValue(mageObject.getId() + "_type", SubType.byDescription(typeChoice.getChoiceKey()));
                if (mageObject instanceof Permanent) {
                    ((Permanent) mageObject).addInfo("chosen type", CardUtil.addToolTipMarkTags("Chosen type: " + typeChoice.getChoiceKey()), game);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public CallousOppressorChooseCreatureTypeEffect copy() {
        return new CallousOppressorChooseCreatureTypeEffect(this);
    }

}
