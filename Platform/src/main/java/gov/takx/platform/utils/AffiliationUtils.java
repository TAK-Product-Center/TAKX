package gov.takx.platform.utils;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.MapItem;
import gov.tak.api.symbology.Affiliation;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Utility methods to determine and test the {@link Affiliation} of a map item, based on either an attached MIl_SYM SIDC,
 * or falling back to the CoT type.
 *
 * @since 5.7
 */
public final class AffiliationUtils
{
    private static final SidcAffiliationExtractor AFFILIATION_EXTRACTOR = new SidcAffiliationExtractor();

    /**
     * Determine the affiliation of the given {@code mapItem}.  If the map item has an attached MIL_SYM SIDC, then
     * the {@link Affiliation} is extracted from that SIDC.  If not, then the CoT type of the map item is used to
     * determine the affiliation.  If neither of those produce an affiliation mapping, then {@code null} is returned.
     *
     * @param mapItem Map item of interest
     * @return The Map item's affiliation, or {@code null} if one could not be determined
     * @apiNote The "Self Marker" is defined to have an affiliation of {@link Affiliation#Friend}
     */
    @Nullable
    public static Affiliation getAffiliation(@Nonnull MapItem mapItem)
    {
        final var affiliation = AFFILIATION_EXTRACTOR.getAffiliation(mapItem);
        if (affiliation != null) return affiliation;

        final var cotType = mapItem.getCotType();
        final boolean validCotType = !CotTypes.EMPTY.equals(cotType) && cotType.length() > 2
                && (CotTypes.SELF.equals(cotType) || cotType.charAt(1) == '-');

        return validCotType ? getAffiliationFromCotType(cotType.charAt(2)) : null;
    }

    /**
     * Determine the {@link Affiliation} specified by the given {@code cotAffiliationCode}, which is the character
     * at position 2 in the overall CoT type.
     * 
     * @param cotAffiliationCode The CoT code to inspect
     * @return Affiliation specified in the given CoT code, or {@code null} if no known affiliation is found
     */
    @Nullable
    public static Affiliation getAffiliationFromCotType(char cotAffiliationCode)
    {
        return switch (Character.toLowerCase(cotAffiliationCode))
        {
            case 'f' -> Affiliation.Friend;
            case 'a' -> Affiliation.AssumedFriend;
            case 'd' -> Affiliation.ExerciseFriend;
            case 'm' -> Affiliation.ExerciseAssumedFriend;
            case 'n' -> Affiliation.Neutral;
            case 'l' -> Affiliation.ExerciseNeutral;
            case 'h' -> Affiliation.Hostile;
            case 'j' -> Affiliation.Joker;
            case 'k' -> Affiliation.Faker;
            case 's' -> Affiliation.Suspect;
            case 'p' -> Affiliation.Pending;
            case 'g' -> Affiliation.ExercisePending;
            case 'u' -> Affiliation.Unknown;
            case 'w' -> Affiliation.ExerciseUnknown;
            default -> null;
        };
    }

    /**
     * Get the one character CoT affiliation code for the given affiliation.
     * E.g., {@link Affiliation#Friend} yields {@code "f"} and {@link Affiliation#ExerciseNeutral} yields {@code "l"}.
     *
     * @param affiliation Affiliation ro process
     * @return One character CoT code for the given affiliation
     */
    public static char getCotAffiliationCode(@Nonnull Affiliation affiliation)
    {
        return switch (affiliation)
        {
            case Friend, SimulatedFriend -> 'f';
            case ExerciseFriend -> 'd';
            case AssumedFriend, SimulatedAssumedFriend -> 'a';
            case ExerciseAssumedFriend -> 'm';
            case Neutral, SimulatedNeutral -> 'n';
            case ExerciseNeutral -> 'l';
            case Hostile, SimulatedHostile -> 'h';
            case Joker -> 'j';
            case Faker -> 'k';
            case Unknown, SimulatedUnknown -> 'u';
            case ExerciseUnknown -> 'w';
            case Pending, SimulatedPending -> 'p';
            case ExercisePending -> 'g';
            case Suspect, SimulatedSuspect -> 's';
        };
    }

    private AffiliationUtils() // no instances
    {
    }

    /**
     * A predicate to test the {@link Affiliation} of a map item.  The affiliation is obtained using {@link #getAffiliation(MapItem)}.
     */
    public static final class AffiliationFilter implements Predicate<MapItem>
    {
        /**
         * A filter testing for a Friendly affiliation, this includes {@link Affiliation#Friend}, {@link Affiliation#SimulatedFriend},
         * {@link Affiliation#ExerciseFriend}, {@link Affiliation#AssumedFriend}, {@link Affiliation#SimulatedAssumedFriend},
         * and {@link Affiliation#ExerciseAssumedFriend}.
         */
        public static final AffiliationFilter FRIENDLY = new AffiliationFilter(
                Set.of(Affiliation.Friend, Affiliation.ExerciseFriend, Affiliation.SimulatedFriend,
                        Affiliation.AssumedFriend, Affiliation.SimulatedAssumedFriend, Affiliation.ExerciseAssumedFriend));

        /**
         * A filter testing for a Hostile affiliation, this includes {@link Affiliation#Hostile}, {@link Affiliation#SimulatedHostile},
         * {@link Affiliation#Joker}, {@link Affiliation#Faker}, {@link Affiliation#Suspect}, and {@link Affiliation#SimulatedSuspect}.
         */
        public static final AffiliationFilter HOSTILE = new AffiliationFilter(
                Set.of(Affiliation.Hostile, Affiliation.SimulatedHostile, Affiliation.Joker, Affiliation.Faker,
                        Affiliation.Suspect, Affiliation.SimulatedSuspect));

        /**
         * A filter testing for an Unknown affiliation, this includes {@link Affiliation#Unknown}, {@link Affiliation#SimulatedUnknown},
         * and {@link Affiliation#ExerciseUnknown}.
         */
        public static final AffiliationFilter UNKNOWN = new AffiliationFilter(
                Set.of(Affiliation.Unknown, Affiliation.SimulatedUnknown, Affiliation.ExerciseUnknown));

        /**
         * A filter testing for a Neutral affiliation, this includes {@link Affiliation#Neutral}, {@link Affiliation#SimulatedNeutral},
         * and {@link Affiliation#ExerciseNeutral}.
         */
        public static final AffiliationFilter NEUTRAL = new AffiliationFilter(
                Set.of(Affiliation.Neutral, Affiliation.SimulatedNeutral, Affiliation.ExerciseNeutral));

        /**
         * A filter testing for a Pending affiliation, this includes {@link Affiliation#Pending}, {@link Affiliation#SimulatedPending},
         * and {@link Affiliation#ExercisePending}.
         */
        public static final AffiliationFilter PENDING = new AffiliationFilter(
                Set.of(Affiliation.Pending, Affiliation.SimulatedPending, Affiliation.ExercisePending));

        /**
         * A filter testing for a Suspect affiliation, this includes {@link Affiliation#Suspect} and, {@link Affiliation#SimulatedSuspect}.
         */
        public static final AffiliationFilter SUSPECT = new AffiliationFilter(Set.of(Affiliation.Suspect, Affiliation.SimulatedSuspect));

        private final Set<Affiliation> affiliations;

        /**
         * @param affiliations Collection of affiliations that should pass the predicate test
         */
        public AffiliationFilter(@Nonnull Set<Affiliation> affiliations)
        {
            this.affiliations = affiliations;
        }

        @Override
        public boolean test(MapItem mapItem)
        {
            final var affiliation = getAffiliation(mapItem);
            return affiliation != null && affiliations.contains(affiliation);
        }
    }

    @PseudoBean
    private static final class SidcAffiliationExtractor
    {
        @Inject
        private IMilStd2525ServiceProvider milStd2525ServiceProvider;

        /**
         * @return The {@link Affiliation} extracted from the map item's attached SIDC (if it has one), or else {@code null}
         * @apiNote The "Self Marker" is defined to have an affiliation of {@link Affiliation#Friend}
         */
        @Nullable
        public Affiliation getAffiliation(MapItem mapItem)
        {
            if (CotTypes.SELF.equals(mapItem.getCotType())) return Affiliation.Friend;

            if (mapItem instanceof IMapObject mapObject)
            {
                final var service = milStd2525ServiceProvider.get2525ServiceForMapObject(mapObject);
                final var sidc = service.getSymbolCode(mapObject);

                return sidc != null ? service.getAffiliation(sidc) : null;
            }
            return null;
        }
    }
}
