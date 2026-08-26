package gov.takx.core.milsym;

import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Modifier;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.milsym.Echelon;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.Presence;
import gov.takx.api.milsym.SymbolWrapper;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.core.persist.MapObjectPersist;
import gov.takx.dataservices.persist.MarkerMapObject;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests the {@link MilStd2525Service}.
 *
 * @since 5.1
 */
@SuppressWarnings("removal")
class MilStd2525ServiceTest extends AContainerTest
{
    private static final String CODE_BOGUS = "***************";
    private static final String CODE1 = "G*F*ACVC--****X";
    private static final String CODE2 = "G*F*ACNC--****X";
    private static final String CODE3 = "G*FAACNC--AA**X";
    private static final String CODE_METOC = "WO-DL-SA----A--";

    private final IMilStd2525Service milSym2525C;

    private IMapObject mapObject;

    @SuppressWarnings("unused")
    @Mocked(parentTypes = IMapObjectPersist.class)
    private MapObjectPersist mapObjectPersist;

    public MilStd2525ServiceTest()
    {
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(getBean(MapObjectManager.class)));

        IMilStd2525ServiceProvider serviceProvider = getBean(IMilStd2525ServiceProvider.class);
        milSym2525C = serviceProvider.get2525cService();
    }

    @BeforeEach
    void setup()
    {
        final MapObjectSpec spec = MapObjectSpec.builder("test", "test", "1").build();
        mapObject = new MarkerMapObject(spec, null);
    }

    @AfterEach
    void cleanup()
    {
        getBean(IMapObjectManager.class).deleteMapObject(mapObject);
    }

    @Test
    void affiliation_updates_SIDC()
    {
        String codeFriend = milSym2525C.setAffiliation(CODE2, Affiliation.Friend);
        assertThat(CODE2)
                .withFailMessage("Assigning affiliation did not modify SIDC")
                .isNotEqualTo(codeFriend);
        assertThat(CODE2.replaceFirst("\\*", "F"))
                .withFailMessage("Assigning affiliation did not yield expected SIDC")
                .isEqualTo(codeFriend);
    }

    @Test
    void affiliations_does_not_affect_symbol_modifiers()
    {
        String codeFriend = milSym2525C.setAffiliation(CODE2, Affiliation.Friend);
        Collection<Modifier> modifiers = milSym2525C.getModifiers(CODE2);
        Collection<Modifier> modifiersFriend = milSym2525C.getModifiers(codeFriend);

        assertThat(modifiers)
                .withFailMessage("Assigning affiliation resulted in different modifiers for same code")
                .usingRecursiveComparison() // Modifier does not implement equals(Object)
                .isEqualTo(modifiersFriend);
    }

    @Test
    void amplifiers_do_not_affect_symbol_lookup()
    {
        String codeFriend = milSym2525C.setAffiliation(CODE2, Affiliation.Friend);
        SymbolWrapper symbolA = milSym2525C.getSymbol(CODE2);
        SymbolWrapper symbolB = milSym2525C.getSymbol(CODE3);
        SymbolWrapper symbolC = milSym2525C.getSymbol(codeFriend);
        assertThat(symbolA)
                .withFailMessage("Symbol lookup for base code, and echelon/presence-modified code did not yield the same symbol wrapper")
                .isEqualTo(symbolB);
        assertThat(symbolA)
                .withFailMessage("Symbol lookup for base code, and affiliation-modified code did not yield the same symbol wrapper")
                .isEqualTo(symbolC);
    }

    @Test
    void amplifiers_do_not_apply_to_metoc()
    {
        String metoc = CODE_METOC;
        assertThat(milSym2525C.setAffiliation(metoc, Affiliation.Friend))
                .withFailMessage("Setting affiliation on METOC symbol should not modify the symbol code")
                .isEqualTo(metoc);
        //noinspection removal
        assertThat(milSym2525C.setEchelon(metoc, Echelon.BRIGADE))
                .withFailMessage("Setting echelon on METOC symbol should not modify the symbol code")
                .isEqualTo(metoc);
        //noinspection removal
        assertThat(milSym2525C.setPresence(metoc, Presence.PRESENT_DAMAGED))
                .withFailMessage("Setting presence on METOC symbol should not modify the symbol code")
                .isEqualTo(metoc);
    }

    @Test
    void service_populates_all_symbols()
    {
        Set<SymbolWrapper> traversed = new HashSet<>();
        milSym2525C.traverse(traversed::add);

        Collection<SymbolWrapper> allSymbols = milSym2525C.getAllSymbols();
        assertThat(allSymbols)
                .withFailMessage("Collection of all-symbols cannot be null/empty")
                .isNotNull()
                .isNotEmpty();
        HashSet<SymbolWrapper> setA = new HashSet<>(traversed);
        HashSet<SymbolWrapper> setB = new HashSet<>(allSymbols);
        assertThat(setA)
                .withFailMessage("Collection of all-symbols not the same as full traversal of symbol tree")
                .isEqualTo(setB);
    }

    @Test
    void bogus_code_has_no_symbol()
    {
        assertThat(milSym2525C.getSymbol(CODE_BOGUS))
                .withFailMessage("Bogus code yielded symbol result")
                .isNull();
    }

    @Test
    void render_for_bogus_fails()
    {
        assertThat(milSym2525C.isUnrenderableSymbol(CODE_BOGUS))
                .withFailMessage("A bogus symbol should not yield a valid render")
                .isTrue();
    }

    @Test
    void getBaseSymbolCode_clears_amplifiers()
    {
        StringBuilder sb = new StringBuilder(CODE1);
        sb.setCharAt(1, 'F');  // Affiliation
        sb.setCharAt(3, 'P');  // Presence
        sb.setCharAt(11, 'A'); // Echelon
        String modifiedCode = sb.toString();

        String baseSymbolCode = milSym2525C.getBaseSymbolCode(modifiedCode);
        assertThat(baseSymbolCode)
                .withFailMessage("Getting base symbol did not strip expected characters of modified SIDC")
                .isEqualTo(CODE1);
    }

    @SuppressWarnings("removal")
    @Test
    void setEchelon_works()
    {
        String modifiedCode = milSym2525C.setEchelon(CODE1, Echelon.BRIGADE);
        assertThat(milSym2525C.getEchelon(CODE1)).isEqualTo(Echelon.UNSPECIFIED);
        assertThat(milSym2525C.getEchelon(modifiedCode)).isEqualTo(Echelon.BRIGADE);
    }

    @SuppressWarnings("removal")
    @Test
    void setPresence_works()
    {
        String modifiedCode = milSym2525C.setPresence(CODE1, Presence.PRESENT_DAMAGED);
        assertThat(milSym2525C.getPresence(CODE1)).isEqualTo(Presence.PRESENT);
        assertThat(milSym2525C.getPresence(modifiedCode)).isEqualTo(Presence.PRESENT_DAMAGED);
    }

    @Test
    void setSymbolCode_works()
    {
        assertThat(milSym2525C.hasSymbolCode(mapObject)).isFalse();

        milSym2525C.setSymbolCode(mapObject, CODE1);

        assertThat(milSym2525C.hasSymbolCode(mapObject)).isTrue();
        assertThat(milSym2525C.getSymbolCode(mapObject)).isEqualTo(CODE1);
    }

    @Test
    void support_setSymbolCodeAndModifiers_works()
    {
        milSym2525C.setSymbolCode(mapObject, CODE1, java.util.Map.of(
                "T", "T value",
                "W", "W value"));

        assertThat(milSym2525C.hasSymbolCode(mapObject)).isTrue();
        assertThat(milSym2525C.getSymbolCode(mapObject)).isEqualTo(CODE1);

        assertThat(milSym2525C.getModifier(mapObject, "T")).isEqualTo("T value");
        assertThat(milSym2525C.getModifier(mapObject, "W")).isEqualTo("W value");
        assertThat(milSym2525C.getModifier(mapObject, "X")).isNull();
    }

    @Test
    void support_addModifier_works()
    {
        milSym2525C.setSymbolCode(mapObject, CODE1);

        assertThat(milSym2525C.getModifier(mapObject, "T")).isNull();
        assertThat(milSym2525C.getModifier(mapObject, "W")).isNull();

        milSym2525C.setModifier(mapObject, "T", "T value");
        assertThat(milSym2525C.getModifier(mapObject, "T")).isEqualTo("T value");
        assertThat(milSym2525C.getModifier(mapObject, "W")).isNull();

        milSym2525C.setModifier(mapObject, "W", "W value");
        assertThat(milSym2525C.getModifier(mapObject, "T")).isEqualTo("T value");
        assertThat(milSym2525C.getModifier(mapObject, "W")).isEqualTo("W value");
    }

    @Test
    void support_setUnsupportedModifier_throws()
    {
        milSym2525C.setSymbolCode(mapObject, CODE1);

        /// "XXX" is not a legal modifier id
        assertThatIllegalArgumentException().isThrownBy(() -> milSym2525C.setModifier(mapObject, "XXX", "value"));

        /// "X" is a legal modifier id, but not for this code
        assertThatIllegalArgumentException().isThrownBy(() -> milSym2525C.setModifier(mapObject, "X", "value"));
    }

    @Test
    void convert_2525E_to_2525C()
    {
        // 2525E "Field Artillery" ---> 2525C
        // 1: Base symbol
        // 2: Amplifier + Status set
        String convertedSymbol1 = milSym2525C.convertFromOther2525Variant("130610000013030000000000000000");
        String convertedSymbol2 = milSym2525C.convertFromOther2525Variant("130610402313030000000000000000");

        // The converted symbols should be the expected 2525C "Field Artillery" with matching amplifier/status values.
        assertThat(convertedSymbol1).isEqualTo("SHGPUCF---*-***");
        assertThat(convertedSymbol2).isEqualTo("SHGXUCF---*K***");
    }
}
