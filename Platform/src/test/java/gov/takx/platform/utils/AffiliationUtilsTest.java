package gov.takx.platform.utils;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.MapItem;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.milsym.CotSidcAdapter;
import gov.takx.platform.utils.AffiliationUtils.AffiliationFilter;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.7
 */
public class AffiliationUtilsTest extends AIntegrationTest
{
    private IMapObjectManager mapObjectManager;

    @AfterEach
    public void afterEach()
    {
        deleteAllMapObjects();
    }

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager = getBean((IMapObjectManager.class));
    }

    @Test
    void mapItemWithNoCotAndNoSidc_hasNoAffiliation()
    {
        final var mapObject = makeMapObject(CotTypes.EMPTY, null);
        assertThat(AffiliationUtils.getAffiliation(mapObject)).isNull();

        List.of(AffiliationFilter.FRIENDLY, AffiliationFilter.HOSTILE, AffiliationFilter.NEUTRAL, AffiliationFilter.UNKNOWN,
                AffiliationFilter.PENDING, AffiliationFilter.SUSPECT)
                .forEach(f -> assertThat(f.test(mapObject)).isFalse());
    }

    @ParameterizedTest
    @MethodSource("provideCotTypesAndAffiliations")
    void mapItemWithCotTypeAndNoSidc_onTest_returnsCorrectValue(String cotType, Affiliation expectedAffiliation, Predicate<MapItem> filter)
    {
        final var mapObject = makeMapObject(cotType, null);
        assertThat(AffiliationUtils.getAffiliation(mapObject)).isEqualTo(expectedAffiliation);
        assertThat(filter.test(mapObject)).isTrue();
    }

    private static Stream<Arguments> provideCotTypesAndAffiliations()
    {
        return Stream.of(
                Arguments.of(CotSidcAdapter.DEFAULT_COT_FRIEND, Affiliation.Friend, AffiliationFilter.FRIENDLY),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_EXERCISE_FRIEND, Affiliation.ExerciseFriend, AffiliationFilter.FRIENDLY),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_ASSUMED_FRIEND, Affiliation.AssumedFriend, AffiliationFilter.FRIENDLY),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_EXERCISE_ASSUMED_FRIEND, Affiliation.ExerciseAssumedFriend, AffiliationFilter.FRIENDLY),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_HOSTILE, Affiliation.Hostile, AffiliationFilter.HOSTILE),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_FAKER, Affiliation.Faker, AffiliationFilter.HOSTILE),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_JOKER, Affiliation.Joker, AffiliationFilter.HOSTILE),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_NEUTRAL, Affiliation.Neutral, AffiliationFilter.NEUTRAL),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_EXERCISE_NEUTRAL, Affiliation.ExerciseNeutral, AffiliationFilter.NEUTRAL),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_UNKNOWN, Affiliation.Unknown, AffiliationFilter.UNKNOWN),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_EXERCISE_UNKNOWN, Affiliation.ExerciseUnknown, AffiliationFilter.UNKNOWN),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_PENDING, Affiliation.Pending, AffiliationFilter.PENDING),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_EXERCISE_PENDING, Affiliation.ExercisePending, AffiliationFilter.PENDING),
                Arguments.of(CotSidcAdapter.DEFAULT_COT_SUSPECT, Affiliation.Suspect, AffiliationFilter.SUSPECT),
                Arguments.of(CotTypes.SELF, Affiliation.Friend, AffiliationFilter.FRIENDLY)); // Self is defined to be friendly
    }

    @ParameterizedTest
    @MethodSource("provideSidcAndAffiliations")
    void mapItemWithSidc_onTest_returnsCorrectValue(String sidc, Affiliation expectedAffiliation, Predicate<MapItem> filter)
    {
        final var mapObject = makeMapObject(CotTypes.EMPTY, sidc);
        assertThat(AffiliationUtils.getAffiliation(mapObject)).isEqualTo(expectedAffiliation);
        assertThat(filter.test(mapObject)).isTrue();
    }

    private static Stream<Arguments> provideSidcAndAffiliations()
    {
        return Stream.of(
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_FRIEND), Affiliation.Friend, AffiliationFilter.FRIENDLY),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_ASSUMED_FRIEND), Affiliation.AssumedFriend, AffiliationFilter.FRIENDLY),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_EXERCISE_ASSUMED_FRIEND), Affiliation.ExerciseAssumedFriend, AffiliationFilter.FRIENDLY),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_HOSTILE), Affiliation.Hostile, AffiliationFilter.HOSTILE),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_JOKER), Affiliation.Joker, AffiliationFilter.HOSTILE),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_FAKER), Affiliation.Faker, AffiliationFilter.HOSTILE),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_NEUTRAL), Affiliation.Neutral, AffiliationFilter.NEUTRAL),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_EXERCISE_NEUTRAL), Affiliation.ExerciseNeutral, AffiliationFilter.NEUTRAL),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_UNKNOWN), Affiliation.Unknown, AffiliationFilter.UNKNOWN),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_EXERCISE_UNKNOWN), Affiliation.ExerciseUnknown, AffiliationFilter.UNKNOWN),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_PENDING), Affiliation.Pending, AffiliationFilter.PENDING),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_EXERCISE_PENDING), Affiliation.ExercisePending, AffiliationFilter.PENDING),
                Arguments.of(CotSidcAdapter.mil2525CFromCotType(CotSidcAdapter.DEFAULT_COT_SUSPECT), Affiliation.Suspect, AffiliationFilter.SUSPECT));
    }

    @ParameterizedTest
    @MethodSource("provideCotAffiliations")
    void getCotAffiliationCode_returnsCorrectValue(Affiliation affiliation, char code)
    {
        assertThat(AffiliationUtils.getCotAffiliationCode(affiliation)).isEqualTo(code);
    }

    private static Stream<Arguments> provideCotAffiliations()
    {
        return Stream.of(
                Arguments.of(Affiliation.Friend, 'f'),
                Arguments.of(Affiliation.SimulatedFriend, 'f'),
                Arguments.of(Affiliation.ExerciseFriend, 'd'),
                Arguments.of(Affiliation.AssumedFriend, 'a'),
                Arguments.of(Affiliation.SimulatedAssumedFriend, 'a'),
                Arguments.of(Affiliation.ExerciseAssumedFriend, 'm'),
                Arguments.of(Affiliation.Hostile, 'h'),
                Arguments.of(Affiliation.SimulatedHostile, 'h'),
                Arguments.of(Affiliation.Joker, 'j'),
                Arguments.of(Affiliation.Faker, 'k'),
                Arguments.of(Affiliation.Neutral, 'n'),
                Arguments.of(Affiliation.SimulatedNeutral, 'n'),
                Arguments.of(Affiliation.ExerciseNeutral, 'l'),
                Arguments.of(Affiliation.Unknown, 'u'),
                Arguments.of(Affiliation.SimulatedUnknown, 'u'),
                Arguments.of(Affiliation.ExerciseUnknown, 'w'),
                Arguments.of(Affiliation.Pending, 'p'),
                Arguments.of(Affiliation.ExercisePending, 'g'),
                Arguments.of(Affiliation.SimulatedPending, 'p'),
                Arguments.of(Affiliation.Suspect, 's'),
                Arguments.of(Affiliation.SimulatedSuspect, 's'));
    }

    private IMapObject makeMapObject(String cotType, String sidc)
    {
        final var spec = MapObjectSpec.builder("TEST", "TEST")
                .cotType(cotType)
                .initializer(mo -> {
                    if (sidc != null) mo.setMetaString(StandardAttributeNames.MIL_SYM, sidc);
                }).build();

        return mapObjectManager.getOrCreateMapObject(spec);
    }
}
