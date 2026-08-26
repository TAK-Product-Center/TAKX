package gov.takx.api.milsym;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.symbology.Affiliation;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.utils.AffiliationUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

// TODO TAKX-2515: Unit tests for class

/**
 * Provides conversion utilities between 2525C symbol identifiers and CoT types.
 *
 * @since 5.1
 */
public class CotSidcAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Mil2525ServiceAccessor MIL_2525_SERVICE_ACCESSOR = new Mil2525ServiceAccessor();

    /** Unknown ground unit */
    public static final String DEFAULT_COT_UNKNOWN = getCotAtomPrefix(Affiliation.Unknown) + "G";
    /** Friendly ground unit */
    public static final String DEFAULT_COT_FRIEND = getCotAtomPrefix(Affiliation.Friend) + "G";
    /** Neutral ground unit */
    public static final String DEFAULT_COT_NEUTRAL = getCotAtomPrefix(Affiliation.Neutral) + "G";
    /** Hostile ground unit */
    public static final String DEFAULT_COT_HOSTILE = getCotAtomPrefix(Affiliation.Hostile) + "G";
    /**
     * Joker ground unit
     * @since 5.2
     */
    public static final String DEFAULT_COT_JOKER = getCotAtomPrefix(Affiliation.Joker) + "G";
    /**
     * Suspect ground unit
     * @since 5.2
     */
    public static final String DEFAULT_COT_SUSPECT = getCotAtomPrefix(Affiliation.Suspect) + "G";
    /**
     * Faker ground unit
     * @since 5.2
     */
    public static final String DEFAULT_COT_FAKER = getCotAtomPrefix(Affiliation.Faker) + "G";
    /**
     * Pending ground unit
     * @since 5.2
     */
    public static final String DEFAULT_COT_PENDING = getCotAtomPrefix(Affiliation.Pending) + "G";
    /**
     * Assumed friend ground unit
     * @since 5.2
     */
    public static final String DEFAULT_COT_ASSUMED_FRIEND = getCotAtomPrefix(Affiliation.AssumedFriend) + "G";
    /**
     * Exercise assumed friend ground unit
     * @since 5.2
     */
    public static final String DEFAULT_COT_EXERCISE_ASSUMED_FRIEND = getCotAtomPrefix(Affiliation.ExerciseAssumedFriend) + "G";
    /**
     * Exercise friend ground unit
     * @since 5.2
     */
    public static final String DEFAULT_COT_EXERCISE_FRIEND = getCotAtomPrefix(Affiliation.ExerciseFriend) + "G";
    /**
     * Exercise neutral ground unit
     * @since 5.2
     */
    public static final String DEFAULT_COT_EXERCISE_NEUTRAL = getCotAtomPrefix(Affiliation.ExerciseNeutral) + "G";
    /**
     * Exercise unknown ground unit
     * @since 5.2
     */
    public static final String DEFAULT_COT_EXERCISE_UNKNOWN = getCotAtomPrefix(Affiliation.ExerciseUnknown) + "G";
    /**
     * Exercise unknown ground unit
     * @since 5.5
     */
    public static final String DEFAULT_COT_EXERCISE_PENDING = getCotAtomPrefix(Affiliation.ExercisePending) + "G";

    private static final String ALL_COT_AFFILIATION_CODES = "ADFGHJKLMNPSW";

    /**
     * @param affiliation Affiliation of the atom type.
     * @return Four character CoT atom prefix for the given affiliation.
     * @since 5.5
     */
    @Nonnull
    public static String getCotAtomPrefix(@Nonnull Affiliation affiliation)
    {
        return CotTypes.PREFIX_ATOM + AffiliationUtils.getCotAffiliationCode(affiliation) + "-";
    }

    /**
     * Convert the given MIL-STD-2525C symbol identifier (SIDC) to a CoT type.
     *
     * @param symbolCode The 2525C symbol identifier (SIDC code) to translate.
     * @return CoT type, or {@code null} if the symbol code cannot be converted to a CoT type.
     */
    @Nullable
    public static String cotTypeFromMil2525C(@Nullable String symbolCode)
    {
        // Can't be null or too short.
        if (symbolCode == null || symbolCode.length() <= 2) return null;

        symbolCode = symbolCode.toUpperCase();

        // We cannot ensure that symbol subtree outside the "S" group can be properly converted.
        // If there are others found later we can add those to the check here.
        if (symbolCode.charAt(0) != 'S') return null;

        final var affiliation = MIL_2525_SERVICE_ACCESSOR.milStd2525Service.getAffiliation(symbolCode);

        // Build the CoT type from the base affiliation, removing content that cannot be represented in
        // the output such as presence amplifiers.
        final var baseType = CotTypes.PREFIX_ATOM + AffiliationUtils.getCotAffiliationCode(affiliation);

        final var sb = new StringBuilder(baseType);
        for (int i = 2; i < symbolCode.length(); ++i)
        {
            if (i == 3) continue; // Skip presence amplifier
            char c = symbolCode.charAt(i);
            if (c == '-') break;
            sb.append('-').append(c);
        }
        return sb.toString();
    }

    /**
     * Convert the given CoT type to a MIL-STD-2525C symbol identifier (SIDC).
     *
     * @param cotType CoT type to translate.
     * @return 2525C SIDC, or {@code null} if the given CoT type can't be converted to an SIDC.
     */
    @Nullable
    public static String mil2525CFromCotType(@Nonnull String cotType)
    {
        // Adapt to upper-case
        cotType = cotType.toUpperCase(Locale.ROOT);

        // Skip invalid types
        final int typeLength = cotType.length();
        if (typeLength < 3 || cotType.charAt(0) != 'A' || CotTypes.EMPTY.equals(cotType)) return null;

        final var sb = new StringBuilder("S"); // Battle Dimension: War fighting

        // Add affiliation, default to unknown if CoT affiliation code is not a known affiliation code
        final char cotAffiliation = cotType.charAt(2);
        final char sidcAffiliation = ALL_COT_AFFILIATION_CODES.indexOf(cotAffiliation) >= 0 ? cotAffiliation : 'U';
        sb.append(sidcAffiliation);

        // Adapt the rest of the type.
        for (int i = 4; i < typeLength; i += 2)
        {
            // If we encounter any part that is longer than a single character, we bail.
            // We do this by checking if the next char is not a separator.
            // This handles CoT "suffixes" that don't map to 2525 codes, like 'a-.-G-I-c-bar'
            if (i < typeLength - 1)
            {
                char next = cotType.charAt(i + 1);
                if (next != '-' && next != '*') break;
            }

            sb.append(cotType.charAt(i));
            if (i == 4) sb.append('P'); // Default status to "Present"
        }

        // Fill in any remaining length.
        for (int i = sb.length(); i < 15; i++)
        {
            // If the SIDC indicates that it is an installation (has 'I' as the first function ID code)
            // then we need to set the first modifier (index == 10) to 'H' to indicate that.
            sb.append(i == 10 && sb.charAt(4) == 'I' ? 'H' : '-');
        }

        return sb.toString();
    }

    /**
     * Find a valid 2525-C SIDC starting with the given {@code cotType} converted directly to an SIDC.
     * <p/>
     * This loop will iteratively try to find a valid SIDC given the CoT type, successively chopping off the
     * last component until a valid SIDC is generated, or we run out of components.
     * <p/>
     * E.g., A CoT type of {@code a-.-G-I-i-e-icc} will be shortened to {@code a-.-G-I} before yielding a valid SIDC
     *
     * @param cotType Starting CoT type code
     * @return Revised SIDC that can be rendered, {@code null} if no renderable symbol could be found
     * @see #mil2525CFromCotType(String)
     * @see IMilStd2525Service#isUnrenderableSymbol(String)
     * @since 5.7
     */
    @Nullable
    public static String findRenderable2525CSymbol(@Nonnull String cotType)
    {
        final var charlieService = MIL_2525_SERVICE_ACCESSOR.milStd2525Service;

        String sidc = mil2525CFromCotType(cotType);
        while (!StringUtils.isEmpty(sidc))
        {
            if (!charlieService.isUnrenderableSymbol(sidc))
            {
                logger.debug("For '{}' found '{}'", cotType, sidc);
                return sidc;
            }

            // Either the SIDC is invalid or we didn't get a usable icon, so chop a component and try again.
            final int index = cotType.lastIndexOf('-');
            if (index != -1)
            {
                cotType = cotType.substring(0, index);
                sidc = mil2525CFromCotType(cotType);
            } else
            {
                break; // Nothing left, so bail
            }
        }
        return null;
    }

    /**
     * Check if the given symbol code can be converted into a CoT type.
     *
     * @param symbolCode SIDC to check.
     * @return {@code true} when the SIDC is known to be convertible to a CoT type. {@code false} otherwise.
     * @since 5.2
     */
    public static boolean isCotCompatibleMil2525C(@Nonnull String symbolCode)
    {
        return cotTypeFromMil2525C(symbolCode) != null;
    }

    /**
     * @since 5.7
     */
    @PseudoBean
    private static class Mil2525ServiceAccessor
    {
        private IMilStd2525Service milStd2525Service;

        @Inject
        private IMilStd2525ServiceProvider milStd2525ServiceProvider;

        @PostConstruct
        private void postConstruct()
        {
            milStd2525Service = milStd2525ServiceProvider.get2525Service(MilStd2525Variant.CHARLIE);
        }
    }
}
