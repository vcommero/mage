package mage.cards.t;

import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.common.SpellCastControllerTriggeredAbility;
import mage.abilities.effects.common.search.SearchLibraryPutInHandEffect;
import mage.abilities.keyword.EnchantAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.filter.FilterCard;
import mage.filter.StaticFilters;
import mage.filter.predicate.Predicate;
import mage.game.Game;
import mage.target.common.TargetCardInLibrary;

import java.util.UUID;

/**
 *
 * @author LevelX2
 */
public final class Tallowisp extends CardImpl {

    private static final FilterCard filterAura = new FilterCard("Aura card with enchant creature");

    static {
        filterAura.add(CardType.ENCHANTMENT.getPredicate());
        filterAura.add(SubType.AURA.getPredicate());
        filterAura.add(TallowispAbilityPredicate.instance);
    }

    public Tallowisp(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId,setInfo,new CardType[]{CardType.CREATURE},"{1}{W}");
        this.subtype.add(SubType.SPIRIT);

        this.power = new MageInt(1);
        this.toughness = new MageInt(3);

        // Whenever you cast a Spirit or Arcane spell, you may search your library for an Aura card with enchant creature, reveal it, and put it into your hand. If you do, shuffle your library.
        this.addAbility(new SpellCastControllerTriggeredAbility(new SearchLibraryPutInHandEffect(new TargetCardInLibrary(filterAura), true), StaticFilters.FILTER_SPELL_SPIRIT_OR_ARCANE, true));
    }

    private Tallowisp(final Tallowisp card) {
        super(card);
    }

    @Override
    public Tallowisp copy() {
        return new Tallowisp(this);
    }
}

enum TallowispAbilityPredicate implements Predicate<MageObject> {
    instance;

    @Override
    public boolean apply(MageObject input, Game game) {
        for (Ability ability : input.getAbilities()) {
            if (ability instanceof EnchantAbility && ability.getRule().contentEquals("Enchant creature")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Aura card with enchant creature";
    }
}
