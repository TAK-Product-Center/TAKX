package gov.takx.core.messagequery;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.query.AggregateFieldConstraint;
import gov.takx.api.query.AggregateFieldSelector;
import gov.takx.api.query.AggregateType;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.CoreFieldSelector;
import gov.takx.api.query.FieldConstraint;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.api.query.InvalidFieldSelectorException;
import gov.takx.api.query.LogicConstraint;
import gov.takx.api.query.LogicType;
import gov.takx.api.query.NoSuchFieldException;
import gov.takx.api.query.QueryExecutionException;
import gov.takx.api.query.ShapeConstraint;
import gov.takx.api.query.StructureFieldSelector;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.dataservices.persist.ShapeRangeFanMapObject;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AQueryServiceTest;
import gov.takx.test.PersistTestSetup;
import gov.takx.test.datasim.ADeviceSim;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;

/**
 * Test cases for {@link PluginQueryService} implementation.
 *
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class QueryServiceTest extends AQueryServiceTest
{
    private static final int ONE_DEGREE_METERS = 112_000; // Slightly larger than 1 degree longitude at the equator

    /**
     * Verify that adding two Device date constraints throws an exception.
     *
     * @throws ConflictingConstraintException expected
     */
    @Test
    public void testDeviceDeviceDateConflicts() throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService(KeyUtils.EMPTY_FAMILY, "Device", "unit", null);
        IQueryConstraints qc = qs.createEmptyConstraints();
        qc.addDeviceDateRange(0, 1);
        assertThatExceptionOfType(ConflictingConstraintException.class)
                .isThrownBy(() -> qc.addDeviceDateRange(0, 1));
    }

    /**
     * Verify that adding two Receive date constraints throws an exception.
     *
     * @throws ConflictingConstraintException expected
     */
    @Test
    public void testReceiveReceiveDateConflicts() throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService(KeyUtils.EMPTY_FAMILY, "Device", "unit", null);
        IQueryConstraints qc = qs.createEmptyConstraints();
        qc.addReceiveDateRange(0, 1);
        assertThatExceptionOfType(ConflictingConstraintException.class)
                .isThrownBy(() -> qc.addReceiveDateRange(0, 1));
    }

    /**
     * Verify that adding a Device and a Receive date constraint throws an exception.
     *
     * @throws ConflictingConstraintException expected
     */
    @Test
    public void testDeviceReceiveDateConflicts() throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService(KeyUtils.EMPTY_FAMILY, "Device", "unit", null);
        IQueryConstraints qc = qs.createEmptyConstraints();
        qc.addDeviceDateRange(0, 1);
        assertThatExceptionOfType(ConflictingConstraintException.class)
                .isThrownBy(() -> qc.addReceiveDateRange(0, 1));
    }

    /**
     * Verify that adding two location constraints throws an exception.
     *
     * @throws ConflictingConstraintException expected
     */
    @Test
    public void testLocationConflicts() throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService(KeyUtils.EMPTY_FAMILY, "Device", "unit", null);
        IQueryConstraints qc = qs.createEmptyConstraints();

        final RaptorLocation location = new RaptorLocation(1.0, 2.0);
        qc.addLocationRange(location, location);
        assertThatExceptionOfType(ConflictingConstraintException.class)
                .isThrownBy(() -> qc.addLocationRange(location, location));
    }

    /**
     * Verify that clearing the constraints between calls to normally conflicting constraints doesn't
     * throw an exception.
     *
     * @throws ConflictingConstraintException unexpected
     */
    @Test
    public void testClearAvoidsConflicts() throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService(KeyUtils.EMPTY_FAMILY, "Device", "unit", null);
        IQueryConstraints qc = qs.createEmptyConstraints();

        final RaptorLocation location = new RaptorLocation(1.0, 2.0);

        qc.addDeviceDateRange(0, 1);
        qc.clear();
        qc.addDeviceDateRange(0, 1);
        qc.clear();
        qc.addReceiveDateRange(0, 1);
        qc.clear();
        qc.addReceiveDateRange(0, 1);
        qc.clear();
        qc.addLocationRange(location, location);
        qc.clear();
        qc.addLocationRange(location, location);
    }

    /**
     * Test that adding legal message structure names don't throw an exception
     *
     * @throws NoSuchStructureException unexpected
     */
    @Test
    public void testLegalMessageStructures() throws NoSuchStructureException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();
        qc.addMessageStructureConstraint(bh_struct.getMessageType());

        IQueryService qs2 = createPluginQueryService(BJ_FAMILY, BJ_DEV_TYPE, "0", bj_struct);
        IQueryConstraints qc2 = qs2.createEmptyConstraints();
        final String bjMessageStructureName = bj_struct.getMessageType();
        qc2.addMessageStructureConstraint(bjMessageStructureName);

        Set<String> structs = qc2.getMessageStructureConstraint();
        assertThat(structs).withFailMessage("structure count").hasSize(1);
        assertThat(structs).withFailMessage("Bluejay structure in set").contains(bjMessageStructureName);
    }

    @Test
    public void testDiscreteMessageStructures() throws NoSuchStructureException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();
        qc.addMessageStructureConstraint(bh_struct);

        IQueryService qs2 = createPluginQueryService(BJ_FAMILY, BJ_DEV_TYPE, "0", bj_struct);
        IQueryConstraints qc2 = qs2.createEmptyConstraints();
        qc2.addMessageStructureConstraint(bj_struct);

        Set<String> structs = qc2.getMessageStructureHashConstraints();
        assertThat(structs).withFailMessage("structure hash count").hasSize(1);
        assertThat(structs).withFailMessage("Bluejay structure hash in set").contains(bj_struct.getStructure().getHashValue());

        structs = qc2.getMessageStructureConstraint();
        assertThat(structs).withFailMessage("structure count").hasSize(1);
        assertThat(structs).withFailMessage("Bluejay structure in set").contains(bj_struct.getMessageType());

        structs = qc.getMessageStructureHashConstraints();
        assertThat(structs).withFailMessage("structure hash count").hasSize(1);
        assertThat(structs).withFailMessage("Bluejay structure hash in set").contains(bh_struct.getStructure().getHashValue());

        structs = qc.getMessageStructureConstraint();
        assertThat(structs).withFailMessage("structure count").hasSize(1);
        assertThat(structs).withFailMessage("Bluejay structure in set").contains(bh_struct.getMessageType());
    }

    /**
     * Validate that the Query Service properly handles message structure ids.
     *
     * @since 2.38
     */
    @Test
    public void testMessageStructureIds()
    {
        final IRaptorDataStructure structOther = new DataStructureBuilder(ORIGIN, ORIGIN, "0.0.1", "structOther")
                .addInt("int").build();
        final IRaptorDataStructure struct1 = new DataStructureBuilder(ORIGIN, ORIGIN, "0.0.1", "s1")
                .addInt("int").build();
        final IRaptorDataStructure struct2 = new DataStructureBuilder(ORIGIN, ORIGIN, "0.0.1", "s1")
                .addInt("int").addInt("int2").build();
        final IRaptorDataStructure struct3 = new DataStructureBuilder(ORIGIN, ORIGIN, "0.0.1", "s1")
                .addInt("int").addInt("int2").addInt("int3").build();

        // Now construct some RDMs to query against
        final IPersistService service = test.getService();
        service.persistRDM(struct1.createRaptorDataMessage(originMo));
        service.persistRDM(struct2.createRaptorDataMessage(originMo));
        service.persistRDM(struct3.createRaptorDataMessage(originMo));
        service.persistRDM(structOther.createRaptorDataMessage(originMo));

        final IQueryService qs = new ExtendedQueryService();
        final IQueryConstraints qc = qs.createEmptyConstraints();

        qc.addKnownMessageStructures(new HashSet<>(Arrays.asList(struct1, struct2, struct3, structOther)));

        qc.addMessageStructureConstraint("s1", "structOther");

        assertThat(qc.getMessageStructureIds())
                .hasSize(4) // 3 named "s1" + "structOther"
                .contains(struct1.getId(), struct2.getId(), struct3.getId(), structOther.getId());

        assertThat(qs.count(qc)).isEqualTo(4); // 1 RDM per structure

        // This will override the initial structure constraint on "s1" and limit it to this specific s1 (struct3)
        qc.addMessageStructureConstraint(struct3);

        assertThat(qc.getMessageStructureIds())
                .hasSize(2)
                .contains(struct3.getId(), structOther.getId());

        assertThat(qs.count(qc)).isEqualTo(2);
    }

    /**
     * Test that adding an illegal message structure name throws an exception
     */
    @Test
    public void testIllegalMessageStructures()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();
        assertThatExceptionOfType(NoSuchStructureException.class)
                .isThrownBy(() -> qc.addMessageStructureConstraint("BOGUS_STRUCTURE_NAME"));
    }

    /**
     * Test that an empty constraints object selects all the messages for a given unit ID.
     *
     * @param unitID Unit ID to test
     */
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4"})
    public void testBHWithEmptyConstraints(String unitID)
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, unitID, bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        long count = qs.count(qc);
        assertThat(count).withFailMessage("for BH " + unitID).isEqualTo(10);

        List<IRaptorDataMessage> rdms = qs.query(qc);
        assertThat(rdms).withFailMessage("for BH " + unitID).hasSize(10);

        // Ensure that all the returned messages have the correct type and id
        for (IRaptorDataMessage rdm : rdms)
        {
            assertThat(rdm.getFamily()).withFailMessage("for BH family").isEqualTo(BH_FAMILY);
            assertThat(rdm.getType()).withFailMessage("for BH " + BH_DEV_TYPE).isEqualTo(BH_DEV_TYPE);
            assertThat(rdm.getUnitID()).withFailMessage("for BH " + unitID).isEqualTo(unitID);
        }
    }

    /**
     * Test that an empty constraints object selects all the messages for a given unit ID.
     * Test using iterator return instead of message collection.
     *
     * @param unitID Unit ID to test
     */
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4"})
    public void testBHWithEmptyConstraintsUsingIterator(String unitID)
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, unitID, bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        Iterator<IRaptorDataMessage> iterator = qs.iterator(qc);
        assertThat(countMessages(iterator)).withFailMessage("for BH " + unitID).isEqualTo(10);

        qs.closeIterator(iterator);
    }

    /**
     * Test that specifying a message limit works properly.
     */
    @Test
    public void testMessageCountLimit()
    {
        IQueryService qs = createPluginQueryService(BJ_FAMILY, BJ_DEV_TYPE, "0", bj_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Set the max message count
        qc.setMaxMessageCount(5);
        List<IRaptorDataMessage> rdms = qs.query(qc);  // Without limit, returns 10 messages
        assertThat(rdms).hasSize(5);

        // Set the max message count to -1, which is unlimited
        qc.setMaxMessageCount(-1);
        rdms = qs.query(qc);
        assertThat(rdms).hasSize(10);
    }

    /**
     * Test that specifying a message limit works properly.
     */
    @Test
    public void testMessageCountLimitUsingIterator()
    {
        IQueryService qs = createPluginQueryService(BJ_FAMILY, BJ_DEV_TYPE, "0", bj_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Set the max message count
        qc.setMaxMessageCount(5);
        Iterator<IRaptorDataMessage> iterator = qs.iterator(qc);  // Without limit, returns 10 messages
        assertThat(countMessages(iterator)).isEqualTo(5);
        qs.closeIterator(iterator);

        // Set the max message count to -1, which is unlimited
        qc.setMaxMessageCount(-1);
        iterator = qs.iterator(qc);
        assertThat(countMessages(iterator)).isEqualTo(10);
        qs.closeIterator(iterator);

        // set the offset to 5; should return items 6--10.
        qc.setMaxMessageCount(-1);
        qc.setStartMessageCount(5);
        iterator = qs.iterator(qc);
        assertThat(countMessages(iterator)).isEqualTo(5);
        qs.closeIterator(iterator);

        // Make sure it works with an offset and limit set
        qc.setMaxMessageCount(4);
        qc.setStartMessageCount(5);
        iterator = qs.iterator(qc);
        assertThat(countMessages(iterator)).isEqualTo(4);
        qs.closeIterator(iterator);

        // Make sure it works with an offset and limit set, where the number of results will be less than
        // the limit
        qc.setMaxMessageCount(10);
        qc.setStartMessageCount(5);
        iterator = qs.iterator(qc);
        assertThat(countMessages(iterator)).isEqualTo(5);
        qs.closeIterator(iterator);
    }

    @Test
    public void testSetSortingOptionsThrowsErrorOnNoSortOrder()
    {
        IQueryService qs = createPluginQueryService(BJ_FAMILY, BJ_DEV_TYPE, "0", bj_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> qc.setSortingOptions(IQueryConstraints.SortOn.LOCATION, null));
    }

    /**
     * Test message ID constraints.
     */
    @Test
    public void testMessageIdConstraints()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // The first set of Bloodhound messages will have message ids 1 - 10
        qc.addMessageIds(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<IRaptorDataMessage> rdms = qs.query(qc);  // should return all 10 messages
        assertThat(rdms).hasSize(10);

        qc.clear();
        List<Integer> ids = new ArrayList<>();
        for (int i = 1; i < 10; i += 2) ids.add(i);

        qc.addMessageIds(ids);
        rdms = qs.query(qc);  // should return just 5 messages
        assertThat(rdms).hasSize(ids.size());

        // Ensure that we only got the ids we requested
        Set<Integer> idSet = new HashSet<>(ids);

        for (IRaptorDataMessage rdm : rdms)
        {
            idSet.remove(rdm.getId());
        }

        assertThat(idSet).withFailMessage("Not all ids found: " + idSet).isEmpty();
    }

    @Test
    public void testPrefetch()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Can't really test that the prefetch occurred, but we can verify that no exceptions get thrown

        List<IRaptorDataMessage> rdms = qs.query(qc, true);  // should return all 10 messages and prefetch
        assertThat(rdms).hasSize(10);
    }

    /**
     * Test that an empty constraints object selects all the BlueJay messages.
     */
    @Test
    public void testBJWithEmptyConstraints()
    {
        // The test setup creates 1 BlueJay device with a unit ID of 0
        IQueryService qs = createPluginQueryService(BJ_FAMILY, BJ_DEV_TYPE, "0", bj_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        long count = qs.count(qc);
        assertThat(count).isEqualTo(10);

        List<IRaptorDataMessage> rdms = qs.query(qc);
        assertThat(rdms).hasSize(10);

        // Ensure that all the returned messages have the correct type and id
        for (IRaptorDataMessage rdm : rdms)
        {
            assertThat(rdm.getType()).isEqualTo(BJ_DEV_TYPE);
            assertThat(rdm.getUnitID()).isEqualTo("0");
        }
    }

    /**
     * Test that message structure constraint selects all the BlueJay messages.
     */
    @Test
    public void testBJWithStructureConstraints()
    {
        // The test setup creates 1 BlueJay device with a unit ID of 0
        IQueryService qs = createPluginQueryService(BJ_FAMILY, BJ_DEV_TYPE, "0", bj_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();
        qc.addMessageStructureConstraint(bj_struct.getMessageType());

        assertThat(qs.count(qc)).isEqualTo(10);
        assertThat(qs.query(qc)).hasSize(10);
    }

    /**
     * Test that adding a constraints properly augments the constraint and changes the query results
     */
    @Test
    public void testBJWithAddedConstraints()
    {
        // The test setup creates 1 BlueJay device with a unit ID of 0
        IQueryService qs = createPluginQueryService(BJ_FAMILY, BJ_DEV_TYPE, "0", bj_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        assertThat(qs.count(qc)).isEqualTo(10);
        assertThat(qs.query(qc)).hasSize(10);

        final RaptorLocation minLoc = new RaptorLocation(ADeviceSim.STARTING_LATITUDE - 10, ADeviceSim.STARTING_LONGITUDE - 10);
        final RaptorLocation maxLoc = new RaptorLocation(ADeviceSim.STARTING_LATITUDE - 5.01, ADeviceSim.STARTING_LONGITUDE - 5.01);

        assertThatNoException().isThrownBy(() -> qc.addLocationRange(minLoc, maxLoc));
        assertThat(qs.count(qc)).isEqualTo(5);
        assertThat(qs.query(qc)).hasSize(5);
    }

    public Iterable<Object[]> provideBHDeviceDateData()
    {
        // The test setup has created 5 Bloodhound devices with ids 0 - 4
        return Arrays.asList(
                new Object[]{"0", 0, 100, 9},   // First one has no device time
                new Object[]{"1", PersistTestSetup.FIXED_TIME_1 - 10, PersistTestSetup.FIXED_TIME_1 + 100, 10},
                new Object[]{"2", PersistTestSetup.FIXED_TIME_2 - 10, PersistTestSetup.FIXED_TIME_2 + 100, 10},
                new Object[]{"3", PersistTestSetup.FIXED_TIME_3 - 10, PersistTestSetup.FIXED_TIME_3 + 100, 10},
                new Object[]{"4", PersistTestSetup.FIXED_TIME_4 - 10, PersistTestSetup.FIXED_TIME_4 + 100, 10});
    }

    /**
     * Test Device Date ranges.
     *
     * @param unitID   Unit ID to test
     * @param minDate  min Date
     * @param maxDate  max Date
     * @param expected expected message count
     * @throws ConflictingConstraintException unexpected
     */
    @ParameterizedTest
    @MethodSource("provideBHDeviceDateData")
    public void testBHDeviceDateRanges(String unitID, long minDate, long maxDate, long expected) throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, unitID, bh_struct);

        IQueryConstraints qc = qs.createEmptyConstraints();
        qc.addDeviceDateRange(minDate, maxDate);
        assertThat(qs.count(qc)).isEqualTo(expected);
        assertThat(qs.query(qc)).hasSize((int) expected);
    }

    /**
     * Test Device Date ordering.
     */
    @Test
    public void testBHDeviceOrderByDeviceDate()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        qc.setSortingOptions(IQueryConstraints.SortOn.DEVICE_DATE, IQueryConstraints.SortOrder.ASCENDING);
        List<IRaptorDataMessage> rdms = qs.query(qc);

        long time = 0;
        for (IRaptorDataMessage rdm : rdms)
        {
            final long deviceTime = rdm.getTime();
            assertThat(deviceTime).withFailMessage("Device time not in ascending order").isGreaterThanOrEqualTo(time);
            time = deviceTime;
        }

        qc.setSortingOptions(IQueryConstraints.SortOn.DEVICE_DATE, IQueryConstraints.SortOrder.DESCENDING);
        rdms = qs.query(qc);

        time = Long.MAX_VALUE;
        for (IRaptorDataMessage rdm : rdms)
        {
            final long deviceTime = rdm.getTime();
            assertThat(deviceTime).withFailMessage("Device time not in descending order").isLessThanOrEqualTo(time);
            time = deviceTime;
        }

        // Confirm that counting works
        long cnt = qs.count(qc);
        assertThat(cnt).withFailMessage("Count feature is broken").isEqualTo(10);
    }

    /**
     * Test Device ordering by an arbitrary column -- in this case, longitude.
     */
    @Test
    public void testBHDeviceOrderByLongitude()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();
        CoreFieldSelector selector = new CoreFieldSelector(CoreField.longitude);

        qc.setSortingOptions(selector, IQueryConstraints.SortOrder.ASCENDING);
        List<IRaptorDataMessage> rdms = qs.query(qc);

        double previousLongitude = -999;
        for (IRaptorDataMessage rdm : rdms)
        {
            final double thisLongitude = rdm.getLongitude();
            assertThat(thisLongitude).withFailMessage("Results not in ascending order by longitude; " + thisLongitude + " is not >= " + previousLongitude).isGreaterThanOrEqualTo(previousLongitude);
            previousLongitude = thisLongitude;
        }

        qc.setSortingOptions(selector, IQueryConstraints.SortOrder.DESCENDING);
        rdms = qs.query(qc);

        previousLongitude = 999;
        for (IRaptorDataMessage rdm : rdms)
        {
            final double thisLongitude = rdm.getLongitude();
            assertThat(thisLongitude).withFailMessage("Results not in descending order by longitude").isLessThanOrEqualTo(previousLongitude);
            previousLongitude = thisLongitude;
        }
    }

    /**
     * Test Device ordering by an extended field -- in this case, TEMPERATURE.
     *
     * @throws ConflictingConstraintException unexpected
     */
    @Test
    public void testBHDeviceOrderByTemperature() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        FieldSelector fieldSelector = new FieldSelector();
        fieldSelector.setStructure(bh_struct.getMessageType());
        String[] fields = {"TEMPERATURE"};
        fieldSelector.addExtendedFields(fields);

        qc.addMessageStructureConstraint(bh_struct.getMessageType());
        StructureFieldSelector selector = new StructureFieldSelector(bh_struct.getMessageType(), "TEMPERATURE");

        qc.setSortingOptions(selector, IQueryConstraints.SortOrder.ASCENDING);

        List<Object[]> rdms = qs.queryForFields(qc, fieldSelector);

        double previousTemperature = -999;
        for (Object[] rdm : rdms)
        {
            final double thisTemperature = (double) rdm[0];
            assertThat(thisTemperature).withFailMessage("Results not in ascending order by temperature; " + thisTemperature + " is not >= " + previousTemperature + " " + rdm.length).isGreaterThanOrEqualTo(previousTemperature);
            previousTemperature = thisTemperature;
        }

        previousTemperature = 999;
        qc.setSortingOptions(selector, IQueryConstraints.SortOrder.DESCENDING);

        for (Object[] rdm : rdms)
        {
            final double thisTemperature = (double) rdm[0];
            assertThat(thisTemperature).withFailMessage("Results not in descending order by temperature; " + thisTemperature + " is not <= " + previousTemperature).isLessThanOrEqualTo(previousTemperature);
            previousTemperature = thisTemperature;
        }
    }

    public Iterable<Object[]> provideBHReceiveDateData()
    {
        // The test setup has created 5 Bloodhound devices with ids 0 - 4
        return Arrays.asList(
                new Object[]{"0", 0, 100, 9},   // First one uses real time
                new Object[]{"0", startTime, finishTime, 1},   // First one uses real time
                new Object[]{"1", PersistTestSetup.FIXED_TIME_1, PersistTestSetup.FIXED_TIME_1 + 100, 10},
                new Object[]{"1", PersistTestSetup.FIXED_TIME_1, PersistTestSetup.FIXED_TIME_1 + 49, 5},
                new Object[]{"2", PersistTestSetup.FIXED_TIME_2, PersistTestSetup.FIXED_TIME_2 + 100, 10},
                new Object[]{"2", PersistTestSetup.FIXED_TIME_2, PersistTestSetup.FIXED_TIME_2 + 49, 5},
                new Object[]{"3", PersistTestSetup.FIXED_TIME_3, PersistTestSetup.FIXED_TIME_3 + 100, 10},
                new Object[]{"3", PersistTestSetup.FIXED_TIME_3, PersistTestSetup.FIXED_TIME_3 + 49, 5},
                new Object[]{"4", PersistTestSetup.FIXED_TIME_4, PersistTestSetup.FIXED_TIME_4 + 100, 10},
                new Object[]{"4", PersistTestSetup.FIXED_TIME_4, PersistTestSetup.FIXED_TIME_4 + 49, 5});
    }

    /**
     * Test Receive Date ranges.
     *
     * @param unitID   Unit ID to test
     * @param minDate  min Date
     * @param maxDate  max Date
     * @param expected expected message count
     * @throws ConflictingConstraintException unexpected
     */
    @ParameterizedTest
    @MethodSource("provideBHReceiveDateData")
    public void testBHReceiveDateRanges(String unitID, long minDate, long maxDate, long expected) throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, unitID, bh_struct);

        IQueryConstraints qc = qs.createEmptyConstraints();
        qc.addReceiveDateRange(minDate, maxDate);
        assertThat(qs.count(qc)).isEqualTo(expected);
        assertThat(qs.query(qc)).hasSize((int) expected);
    }

    /**
     * Test Receive Date ordering.
     */
    @Test
    public void testBHDeviceOrderByReceiveDate()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        qc.setSortingOptions(IQueryConstraints.SortOn.RECEIVE_DATE, IQueryConstraints.SortOrder.ASCENDING);
        List<IRaptorDataMessage> rdms = qs.query(qc);

        long time = 0;
        for (IRaptorDataMessage rdm : rdms)
        {
            final long receiveTime = rdm.getReceiveTime();
            assertThat(receiveTime).withFailMessage("Receive time not in ascending order").isGreaterThanOrEqualTo(time);
            time = receiveTime;
        }

        qc.setSortingOptions(IQueryConstraints.SortOn.RECEIVE_DATE, IQueryConstraints.SortOrder.DESCENDING);
        rdms = qs.query(qc);

        time = Long.MAX_VALUE;
        for (IRaptorDataMessage rdm : rdms)
        {
            final long receiveTime = rdm.getReceiveTime();
            assertThat(receiveTime).withFailMessage("Receive time not in descending order").isLessThanOrEqualTo(time);
            time = receiveTime;
        }

        // Confirm that counting works
        long cnt = qs.count(qc);
        assertThat(cnt).withFailMessage("Count feature is broken").isEqualTo(10);
    }

    public Iterable<Object[]> provideByTimeBucketData()
    {
        // We need to round the starttime and finishtime to even "seconds" for hypersonic
        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(startTime);
        int mS = c.get(Calendar.MILLISECOND);
        c.setTimeInMillis(finishTime);
        int mF = c.get(Calendar.MILLISECOND);

        // Device time is always 10 ms behind receive time, so account for that in relevant time
        // The very first RDM uses "current" time - reference start and finish time for that one
        return Arrays.asList(
                new Object[]{true, new Time(startTime - mS), new Time(finishTime - mF + 1000), 21},
                new Object[]{false, new Time(PersistTestSetup.FIXED_TIME_1), new Time(PersistTestSetup.FIXED_TIME_1 + 1000), 9},
                new Object[]{false, new Time(PersistTestSetup.FIXED_TIME_1), new Time(PersistTestSetup.FIXED_TIME_1 + 49), 9},
                new Object[]{false, new Time(PersistTestSetup.FIXED_TIME_1), new Time(PersistTestSetup.FIXED_TIME_3 + 1000), 39});
    }

    /**
     * Test relevant time buckets.
     *
     * @param thisCentury Pass {@code true} to restrict values to this century
     * @param minTime     min Time
     * @param maxTime     max Time
     * @param expected    expected message count
     * @throws ConflictingConstraintException unexpected
     */
    @ParameterizedTest
    @MethodSource("provideByTimeBucketData")
    public void testBHTimeBuckets(boolean thisCentury, Time minTime, Time maxTime, long expected) throws ConflictingConstraintException
    {
        IQueryService qs = new ExtendedQueryService();

        IQueryConstraints qc = qs.createEmptyConstraints();
        if (thisCentury)
        {
            qc.addReceiveDateRange(PersistTestSetup.FIXED_TIME_4 + 1000, finishTime + 1000);
        } else
        {
            qc.addReceiveDateRange(0, PersistTestSetup.FIXED_TIME_4 + 1000);
        }
        qc.addTimeBucketRange(minTime, maxTime);
        List<IRaptorDataMessage> rdms = qs.query(qc);
        assertThat(qs.count(qc)).isEqualTo(expected);
        assertThat(rdms).hasSize((int) expected);
    }

    //@Test TAKX-447
    public void testBHDayOfWeek() throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Unit 0 has it's first RDM using the current time, so use today
        //   Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // OK, so I like the attempt to use UTC here, but if we do then since the RDM's inserted are using the local
        // clock they will naturally be off by the difference between the local clock and UTC. In AZ this is 7 hours.
        // So If I run this test after 5 in the afternoon then UTC says it is the following day and the test breaks.
        // Ouch! Of course, this can only be seen when the test is run against postgres.

        // Standardize on 0 - 6 (which is what Postgres uses for day numbers)
        // Calendar uses 1 - 7, so shift by one
        final int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;

        qc.addDayOfWeekConstraint(new int[]{today});
        qc.addFieldConstraint(CoreField.receiveTime, ComparisonType.GT, 1000L);

        assertThat(qs.count(qc)).isEqualTo(1);
    }

    public static Iterable<Object[]> provideBHLocationData()
    {
        // The test setup has created 5 Bloodhound devices with ids 0 - 4

        // They have lat/lon boundaries from (24.437,-129.82) - (33.347,-120.82)
        // These are based on ADeviceSim.STARTING_LATITUDE and ADeviceSim.STARTING_LONGITUDE and
        // decreasing the location by 1 degree in each of lat/lon on creation
        final RaptorLocation minLoc1 = new RaptorLocation(ADeviceSim.STARTING_LATITUDE - 10, ADeviceSim.STARTING_LONGITUDE - 10);
        final RaptorLocation maxLoc1 = new RaptorLocation(ADeviceSim.STARTING_LATITUDE - 1, ADeviceSim.STARTING_LONGITUDE - 1);
        final RaptorLocation minLoc2 = new RaptorLocation(ADeviceSim.STARTING_LATITUDE - 10, ADeviceSim.STARTING_LONGITUDE - 10);
        final RaptorLocation maxLoc2 = new RaptorLocation(ADeviceSim.STARTING_LATITUDE - 5, ADeviceSim.STARTING_LONGITUDE - 5);
        final RaptorLocation minLoc3 = new RaptorLocation(ADeviceSim.STARTING_LATITUDE - 10, ADeviceSim.STARTING_LONGITUDE - 10);
        final RaptorLocation maxLoc3 = new RaptorLocation(ADeviceSim.STARTING_LATITUDE - 5.01, ADeviceSim.STARTING_LONGITUDE - 5.01);

        return Arrays.asList(
                new Object[]{"0", minLoc1, maxLoc1, 10},
                new Object[]{"0", minLoc2, maxLoc2, 6},
                new Object[]{"0", minLoc3, maxLoc3, 5},
                new Object[]{"1", minLoc1, maxLoc1, 10},
                new Object[]{"1", minLoc2, maxLoc2, 6},
                new Object[]{"1", minLoc3, maxLoc3, 5},
                new Object[]{"2", minLoc1, maxLoc1, 10},
                new Object[]{"2", minLoc2, maxLoc2, 6},
                new Object[]{"2", minLoc3, maxLoc3, 5},
                new Object[]{"3", minLoc1, maxLoc1, 10},
                new Object[]{"3", minLoc2, maxLoc2, 6},
                new Object[]{"3", minLoc3, maxLoc3, 5},
                new Object[]{"4", minLoc1, maxLoc1, 10},
                new Object[]{"4", minLoc2, maxLoc2, 6},
                new Object[]{"4", minLoc3, maxLoc3, 5});
    }

    /**
     * Test Location ranges.
     *
     * @param unitID   Unit ID to test
     * @param minLoc   min location
     * @param maxLoc   max location
     * @param expected expected message count
     * @throws ConflictingConstraintException unexpected
     */
    @ParameterizedTest
    @MethodSource("provideBHLocationData")
    public void testBHLocationRanges(String unitID, ILocation minLoc, ILocation maxLoc, long expected) throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, unitID, bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        qc.addLocationRange(minLoc, maxLoc);
        assertThat(qs.count(qc)).isEqualTo(expected);
        assertThat(qs.query(qc)).hasSize((int) expected);

        // Swapping the coords should have no effect
        qc.clear();
        qc.addLocationRange(maxLoc, minLoc);
        assertThat(qs.count(qc)).isEqualTo(expected);
        assertThat(qs.query(qc)).hasSize((int) expected);

        // Now test strict interpretation
        qc.clear();
        qc.addLocationRange(minLoc, maxLoc, true);
        assertThat(qs.count(qc)).isEqualTo(expected);
        assertThat(qs.query(qc)).hasSize((int) expected);

        // Swapped should yield no results
        qc.clear();
        qc.addLocationRange(maxLoc, minLoc, true);
        assertThat(qs.count(qc)).isEqualTo(0);
        assertThat(qs.query(qc)).isEmpty();
    }

    /**
     * Test location ordering.
     */
    @Test
    public void testBHDeviceOrderByLocation()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        qc.setSortingOptions(IQueryConstraints.SortOn.LOCATION, IQueryConstraints.SortOrder.ASCENDING);
        List<IRaptorDataMessage> rdms = qs.query(qc);

        double lat = -180.0;
        for (IRaptorDataMessage rdm : rdms)
        {
            final double msgLat = rdm.getLatitude();
            assertThat(msgLat).withFailMessage("Latitude not in ascending order").isGreaterThanOrEqualTo(lat);
            lat = msgLat;
        }

        qc.setSortingOptions(IQueryConstraints.SortOn.LOCATION, IQueryConstraints.SortOrder.DESCENDING);
        rdms = qs.query(qc);

        lat = 180.0;
        for (IRaptorDataMessage rdm : rdms)
        {
            final double msgLat = rdm.getLatitude();
            assertThat(msgLat).withFailMessage("Latitude not in descending order").isLessThanOrEqualTo(lat);
            lat = msgLat;
        }

        // Confirm that counting works
        long cnt = qs.count(qc);
        assertThat(cnt).withFailMessage("Count feature is broken").isEqualTo(10);
    }

    @Test
    public void testConstraintRemoval() throws NoSuchFieldException, ConflictingConstraintException
    {
        final IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        final IQueryConstraints qc = qs.createEmptyConstraints();

        final StructureFieldSelector panicField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Panic");
        final StructureFieldSelector voltageField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Voltage");

        final FieldConstraint fc1 = new FieldConstraint(CoreField.id.selector, ComparisonType.EQ, 1);
        final FieldConstraint fc1a = new FieldConstraint(CoreField.id.selector, ComparisonType.EQ, 1);
        final FieldConstraint fc2 = new FieldConstraint(CoreField.sourceName.selector, ComparisonType.EQ, "foo");
        final FieldConstraint fc4 = new FieldConstraint(panicField, ComparisonType.NEQ, true);
        final FieldConstraint fc5 = new FieldConstraint(voltageField, ComparisonType.EQ, 7.0);

        qc.addFieldConstraints(fc1);
        assertThat(qc.removeFieldConstraints(fc2)).isFalse();
        assertThat(qc.removeFieldConstraints(fc1)).isTrue();
        assertThat(qc.removeFieldConstraints(fc1)).isFalse();

        qc.addFieldConstraints(fc1);
        assertThat(qc.removeFieldConstraints(fc1a)).isTrue();
        assertThat(qc.removeFieldConstraints(fc1a)).isFalse();

        qc.clear();
        qc.addFieldConstraints(fc1, fc2);
        assertThat(qc.removeFieldConstraints(fc1)).isTrue();
        assertThat(qc.removeFieldConstraints(fc1)).isFalse();
        qc.addFieldConstraints(fc1);
        assertThat(qc.removeFieldConstraints(fc1, fc2)).isTrue();
        assertThat(qc.removeFieldConstraints(fc1, fc2)).isFalse();

        qc.clear();
        qc.addFieldConstraints(fc1, fc2);
        assertThat(qc.removeFieldConstraints(fc1, fc4)).isFalse();

        // Now structure fields

        qc.clear();
        qc.addFieldConstraints(fc4);
        assertThat(qc.removeFieldConstraints(fc5)).isFalse();
        assertThat(qc.removeFieldConstraints(fc4)).isTrue();
        assertThat(qc.removeFieldConstraints(fc4)).isFalse();

        qc.clear();
        qc.addFieldConstraints(fc4, fc5);
        assertThat(qc.removeFieldConstraints(fc4)).isTrue();
        assertThat(qc.removeFieldConstraints(fc4)).isFalse();
        qc.addFieldConstraints(fc4);
        assertThat(qc.removeFieldConstraints(fc4, fc5)).isTrue();
        assertThat(qc.removeFieldConstraints(fc4, fc5)).isFalse();

        qc.clear();
        qc.addFieldConstraints(fc4, fc5);
        assertThat(qc.removeFieldConstraints(fc1, fc4)).isFalse();

        // Now mixed core and structure fields
        qc.clear();
        qc.addFieldConstraints(fc1, fc4);
        assertThat(qc.removeFieldConstraints(fc1)).isTrue();
        assertThat(qc.removeFieldConstraints(fc1)).isFalse();
        qc.addFieldConstraints(fc1);
        assertThat(qc.removeFieldConstraints(fc1, fc4)).isTrue();
        assertThat(qc.removeFieldConstraints(fc1, fc4)).isFalse();

        // Now group constraints
        final AggregateFieldSelector countHeading = new AggregateFieldSelector(AggregateType.COUNT, false, CoreField.heading.selector);
        final AggregateFieldConstraint gc1 = new AggregateFieldConstraint(countHeading, ComparisonType.GT, 5);
        final AggregateFieldConstraint gc1a = new AggregateFieldConstraint(countHeading, ComparisonType.GT, 5);
        final AggregateFieldConstraint gc2 = new AggregateFieldConstraint(countHeading, ComparisonType.LT, 5);
        final AggregateFieldSelector avgHeading = new AggregateFieldSelector(AggregateType.AVG, false, CoreField.heading.selector);
        final AggregateFieldConstraint gc3 = new AggregateFieldConstraint(avgHeading, ComparisonType.LT, 5);

        qc.clear();
        qc.setGrouping(true);
        qc.addGroupConstraints(gc1);
        assertThat(qc.removeGroupConstraints(gc2)).isFalse();
        assertThat(qc.removeGroupConstraints(gc1)).isTrue();
        assertThat(qc.removeGroupConstraints(gc1)).isFalse();

        qc.addGroupConstraints(gc1);
        assertThat(qc.removeGroupConstraints(gc1a)).isTrue();
        assertThat(qc.removeGroupConstraints(gc1a)).isFalse();

        qc.clear();
        qc.setGrouping(true);
        qc.addGroupConstraints(gc1, gc2);
        assertThat(qc.removeGroupConstraints(gc1)).isTrue();
        assertThat(qc.removeGroupConstraints(gc1)).isFalse();
        qc.addGroupConstraints(gc1);
        assertThat(qc.removeGroupConstraints(gc1, gc2)).isTrue();
        assertThat(qc.removeGroupConstraints(gc1, gc2)).isFalse();

        qc.clear();
        qc.setGrouping(true);
        qc.addGroupConstraints(gc1, gc2);
        assertThat(qc.removeGroupConstraints(gc1, gc3)).isFalse();

        // Now logic constraints
        final LogicConstraint lc1 = new LogicConstraint(LogicType.or,
                new FieldConstraint(CoreField.heading.selector, ComparisonType.EQ, 50.),
                new FieldConstraint(CoreField.heading.selector, ComparisonType.EQ, 60.));
        final LogicConstraint lc1a = new LogicConstraint(LogicType.or,
                new FieldConstraint(CoreField.heading.selector, ComparisonType.EQ, 50.),
                new FieldConstraint(CoreField.heading.selector, ComparisonType.EQ, 60.));
        final LogicConstraint lc2 = new LogicConstraint(LogicType.and,
                new LogicConstraint(LogicType.not,
                        new FieldConstraint(panicField, ComparisonType.NEQ, true)),
                new FieldConstraint(voltageField, ComparisonType.GT, 7.55),
                new FieldConstraint(voltageField, ComparisonType.LT, 7.85));
        final LogicConstraint lc3 = new LogicConstraint(LogicType.and,
                new FieldConstraint(CoreField.longitude.selector, ComparisonType.GT, 50.),
                new FieldConstraint(CoreField.longitude.selector, ComparisonType.LT, 60.));

        qc.clear();
        qc.addLogicConstraints(lc1);
        assertThat(qc.removeLogicConstraints(lc2)).isFalse();
        assertThat(qc.removeLogicConstraints(lc1)).isTrue();
        assertThat(qc.removeLogicConstraints(lc1)).isFalse();

        qc.addLogicConstraints(lc1);
        assertThat(qc.removeLogicConstraints(lc1a)).isTrue();
        assertThat(qc.removeLogicConstraints(lc1a)).isFalse();

        qc.clear();
        qc.addLogicConstraints(lc1, lc2);
        assertThat(qc.removeLogicConstraints(lc1)).isTrue();
        assertThat(qc.removeLogicConstraints(lc1)).isFalse();
        qc.addLogicConstraints(lc1);
        assertThat(qc.removeLogicConstraints(lc1, lc2)).isTrue();
        assertThat(qc.removeLogicConstraints(lc1, lc2)).isFalse();

        qc.clear();
        qc.addLogicConstraints(lc1, lc2);
        assertThat(qc.removeLogicConstraints(lc1, lc3)).isFalse();

        // Now shape constraints
        final RaptorLocation l1 = new RaptorLocation(1, 2);
        final RaptorLocation l2 = new RaptorLocation(3, 4);
        final RaptorLocation l3 = new RaptorLocation(3, 4);
        final ShapeConstraint sc1 = new ShapeConstraint(l1, l2);
        final ShapeConstraint sc1a = new ShapeConstraint(l1, l2);
        final ShapeConstraint sc2 = new ShapeConstraint(l1, 20);
        final ShapeConstraint sc3 = new ShapeConstraint(Arrays.asList(l1, l2, l3));

        qc.clear();
        qc.addShapeConstraint(sc1);
        assertThat(qc.removeShapeConstraints(sc2)).isFalse();
        assertThat(qc.removeShapeConstraints(sc1)).isTrue();
        assertThat(qc.removeShapeConstraints(sc1)).isFalse();

        qc.addShapeConstraint(sc1);
        assertThat(qc.removeShapeConstraints(sc1a)).isTrue();
        assertThat(qc.removeShapeConstraints(sc1a)).isFalse();

        qc.clear();
        qc.addShapeConstraint(sc1);
        qc.addShapeConstraint(sc2);
        assertThat(qc.removeShapeConstraints(sc1)).isTrue();
        assertThat(qc.removeShapeConstraints(sc1)).isFalse();
        qc.addShapeConstraint(sc1);
        assertThat(qc.removeShapeConstraints(sc1, sc2)).isTrue();
        assertThat(qc.removeShapeConstraints(sc1, sc2)).isFalse();

        qc.clear();
        qc.addShapeConstraint(sc1);
        qc.addShapeConstraint(sc2);
        assertThat(qc.removeShapeConstraints(sc1, sc3)).isFalse();
    }

    /**
     * Test that adding a field constraint on an invalid structure throws an exception.
     */
    @Test
    public void testBogusStructureOnFieldConstraint()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();
        assertThatExceptionOfType(NoSuchStructureException.class)
                .isThrownBy(() -> qc.addFieldConstraint("BOGUS", "field", ComparisonType.EQ, "some value"));
    }

    /**
     * Test that adding a field constraint on an invalid field name throws an exception.
     */
    @Test
    public void testBogusFieldOnFieldConstraint()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();
        assertThatExceptionOfType(NoSuchFieldException.class)
                .isThrownBy(() -> qc.addFieldConstraint("BH_STATUS_MESSAGE", "BOGUS", ComparisonType.EQ, "some value"));
    }

    /**
     * Test a single field constraint on the Bloodhound messages
     *
     * @throws NoSuchStructureException       unexpected
     * @throws ConflictingConstraintException unexpected
     * @throws NoSuchFieldException           unexpected
     */
    @Test
    public void testPanicBHFieldConstraint() throws NoSuchStructureException, ConflictingConstraintException, NoSuchFieldException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Panic", ComparisonType.EQ, true);
        assertThat(qs.count(qc)).isEqualTo(5);
        List<IRaptorDataMessage> rdms = qs.query(qc);
        assertThat(rdms).hasSize(5);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Panic", ComparisonType.EQ, false);
        assertThat(qs.count(qc)).isEqualTo(5);
        rdms = qs.query(qc);
        assertThat(rdms).hasSize(5);
    }

    /**
     * Test multiple field constraints on the Bloodhound messages
     *
     * @throws NoSuchStructureException       unexpected
     * @throws ConflictingConstraintException unexpected
     * @throws NoSuchFieldException           unexpected
     */
    @Test
    public void testPanicBHMultiFieldConstraints() throws NoSuchStructureException, ConflictingConstraintException, NoSuchFieldException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Test for RPTR-1372: ensure that a range constraint works if it is the only/first field constraint

        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GTE, 7.0); // All messages
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.LT, 8.0); // All messages
        assertThat(qs.count(qc)).isEqualTo(10);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "batt", ComparisonType.EQ, 42.0f); // All messages
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "devType", ComparisonType.EQ, 42); // All messages
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GTE, 7.0); // All messages
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.LT, 8.0); // All messages
        assertThat(qs.count(qc)).isEqualTo(10);

        List<IRaptorDataMessage> rdms = qs.query(qc);
        assertThat(rdms).hasSize(10);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "batt", ComparisonType.EQ, 42.0f); // All messages
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "devType", ComparisonType.EQ, 42); // All messages
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GT, 7.05); // 9 messages
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.LT, 7.4); // 3 messages
        assertThat(qs.count(qc)).isEqualTo(3);
        rdms = qs.query(qc);
        assertThat(rdms).hasSize(3);

        // Add in one more constraint and requery
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Panic", ComparisonType.EQ, false);
        assertThat(qs.count(qc)).isEqualTo(1);
        rdms = qs.query(qc);
        assertThat(rdms).hasSize(1);
    }

    /**
     * Test the operation of core field constraints on the heading
     */
    @Test
    public void testHeadingCoreFieldConstraint()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Headings start at 10 and increment by 10

        qc.addFieldConstraint(CoreField.heading, ComparisonType.GTE, 50.0);
        assertThat(qs.count(qc)).isEqualTo(6);

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.GTE, 50.0);
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LTE, 70.0);
        assertThat(qs.count(qc)).isEqualTo(3);
    }

    /**
     * Test the operation of core field constraints on multiple fields
     */
    @Test
    public void testMultipleCoreFieldConstraint()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // heading runs like this:  10,  20,  30,  40,  50,  60,  70,  80,  90,  100
        // pitch runs like this  :  40,  60,  80,  90,  70,  50,  30,  10, -10,  -30
        // roll runs like this   :  10,  20,  30,  40,  50,  60,  70,  80,  90,  100
        // speed runs like this :  100, 200, 300, 400, 500, 600, 700, 800, 900, 1000
        // test 1                                       ^--------------------^
        // test 2                                            ^----------^
        // test 3                                                 ^^

        qc.addFieldConstraint(CoreField.heading, ComparisonType.GTE, 50.0);
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LTE, 100.0); // 6 match this

        qc.addFieldConstraint(CoreField.pitch, ComparisonType.GTE, -10.0);
        qc.addFieldConstraint(CoreField.pitch, ComparisonType.LTE, 70.0); // 5 match this

        assertThat(qs.count(qc)).isEqualTo(5); // Overlap should be 5

        qc.addFieldConstraint(CoreField.roll, ComparisonType.GTE, 60.0);
        qc.addFieldConstraint(CoreField.roll, ComparisonType.LTE, 80.0); // 3 match this

        assertThat(qs.count(qc)).isEqualTo(3); // Overlap should be 3

        qc.addFieldConstraint(CoreField.speed, ComparisonType.EQ, 700.0);

        assertThat(qs.count(qc)).isEqualTo(1); // Overlap should be 1
    }

    /**
     * Test the operation of core field constraints on the message source type
     */
    @Test
    public void testSourceTypeCoreFieldConstraint()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Source type is always FILE

        qc.addFieldConstraint(CoreField.sourceType, ComparisonType.EQ, CommServiceType.FILE);
        assertThat(qs.count(qc)).isEqualTo(10);

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceType, ComparisonType.EQ, CommServiceType.SERIAL);
        assertThat(qs.count(qc)).isEqualTo(0);
    }

    /**
     * Test the operation of core field constraints on the message source name
     */
    @Test
    public void testSourceNameCoreFieldConstraint()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Source name is always TESTING

        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.EQ, "TESTING");
        assertThat(qs.count(qc)).isEqualTo(10);

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.EQ, "SOMETHING ELSE");
        assertThat(qs.count(qc)).isEqualTo(0);
    }

    /**
     * Test the operation of core field constraints/selectors on the mapObject_id
     */
    @Test
    public void testMoIdCoreField() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        final IQueryService qs = new ExtendedQueryService();
        final IQueryConstraints qc = qs.createEmptyConstraints();

        qc.addFieldConstraint(CoreField.mapObject_id, ComparisonType.LT, 3);

        final List<IRaptorDataMessage> list = qs.query(qc);
        assertThat(list).hasSize(20);

        final List<Object[]> results = qs.queryForFields(qc, new FieldSelector().addCoreFields(CoreField.mapObject_id));
        final List<Integer> ids = results.stream()
                .map(a -> (Integer) a[0])
                .toList();
        assertThat(ids).containsOnly(1, 2);
        assertThat(ids).hasSize(20);
    }

    /**
     * Test that proper SQL is generated when both a structure constraint and field-level constraints are in place.
     * See CORE-2212.
     *
     * @throws NoSuchStructureException       unexpected
     * @throws ConflictingConstraintException unexpected
     * @throws NoSuchFieldException           unexpected
     */
    @Test
    public void testStructureAndFieldConstraints() throws NoSuchStructureException, ConflictingConstraintException, NoSuchFieldException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        qc.addMessageStructureConstraint("BH_STATUS_MESSAGE");
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "batt", ComparisonType.EQ, 42.0f); // All messages
        assertThat(qs.count(qc)).isEqualTo(10);
    }

    /**
     * Test the operation of core field constraints on the message source name
     *
     * @throws ConflictingConstraintException Should never be thrown
     * @throws NoSuchFieldException           Should never be thrown
     */
    @Test
    public void testAllComparitors() throws ConflictingConstraintException, NoSuchFieldException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Core fields

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.EQ, "TESTING");
        assertThat(qs.count(qc)).isEqualTo(10);

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.NEQ, "TESTING");
        assertThat(qs.count(qc)).isEqualTo(0);

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.GT, 50.0);
        assertThat(qs.count(qc)).isEqualTo(5);

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);
        assertThat(qs.count(qc)).isEqualTo(4);

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.GTE, 50.0);
        assertThat(qs.count(qc)).isEqualTo(6);

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LTE, 50.0);
        assertThat(qs.count(qc)).isEqualTo(5);

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.ISNULL, null);
        assertThat(qs.count(qc)).isEqualTo(0);

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.ISNOTNULL, null);
        assertThat(qs.count(qc)).isEqualTo(10);

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.LIKE, "%TING");
        assertThat(qs.count(qc)).isEqualTo(10);

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.NOTLIKE, "%TING");
        assertThat(qs.count(qc)).isEqualTo(0);

        // Structure fields

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Height", ComparisonType.EQ, 42);
        assertThat(qs.count(qc)).isEqualTo(10);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Height", ComparisonType.NEQ, 42);
        assertThat(qs.count(qc)).isEqualTo(0);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GT, 7.35);
        assertThat(qs.count(qc)).isEqualTo(6);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.LT, 7.35);
        assertThat(qs.count(qc)).isEqualTo(4);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GTE, 7.35);
        assertThat(qs.count(qc)).isEqualTo(6);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.LTE, 7.35);
        assertThat(qs.count(qc)).isEqualTo(4);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.ISNULL, null);
        assertThat(qs.count(qc)).isEqualTo(0);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.ISNOTNULL, null);
        assertThat(qs.count(qc)).isEqualTo(10);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "MessageType", ComparisonType.LIKE, "%STATUS%");
        assertThat(qs.count(qc)).isEqualTo(10);

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "MessageType", ComparisonType.NOTLIKE, "%STATUS%");
        assertThat(qs.count(qc)).isEqualTo(0);
    }

    /**
     * Test the field-level query method that returns results as a Map.
     *
     * @throws ConflictingConstraintException shouldn't
     * @throws InvalidFieldSelectorException  shouldn't
     * @throws NoSuchFieldException           shouldn't
     */
    @Test
    public void testFieldQueryWithMap() throws ConflictingConstraintException, InvalidFieldSelectorException, NoSuchFieldException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Start with a simple single core field query
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0); // Should be 4 matching RDMs
        FieldSelector fs = new FieldSelector().addCoreFields(CoreField.heading);
        List<Map<String, Object>> results = qs.queryForFieldsAsMap(qc, fs);
        assertThat(results).hasSize(4);

        // The result will be a list of Maps
        for (Map<String, Object> valueMap : results)
        {
            Double value = (Double) valueMap.get("heading");
            assertThat(value).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GT, 2.0);

        fs = new FieldSelector()
                .addCoreFields(CoreField.heading, CoreField.id, CoreField.receiveTime)
                .setStructure("BH_STATUS_MESSAGE").addExtendedFields("key", "Voltage");
        results = qs.queryForFieldsAsMap(qc, fs);
        assertThat(results).hasSize(4);

        // The results will be a list of maps, each map has these entries:
        //    heading:     Double
        //    id:          Integer
        //    receiveTime: Long/BigInteger
        //    key:         String
        //    voltage:     Double

        for (Map<String, Object> valueMap : results)
        {
            assertThat(valueMap).withFailMessage("Has 'heading' entry").containsKey("heading");
            assertThat(valueMap).withFailMessage("Has 'id' entry").containsKey("id");
            assertThat(valueMap).withFailMessage("Has 'receiveTime' entry").containsKey("receiveTime");
            assertThat(valueMap).withFailMessage("Has 'key' entry").containsKey("key");
            assertThat(valueMap).withFailMessage("Has 'voltage' entry").containsKey("Voltage");

            Double heading = (Double) valueMap.get("heading");
            String key = (String) valueMap.get("key");
            Double voltage = (Double) valueMap.get("Voltage");

            assertThat(voltage).withFailMessage("Selected field value doesn't match constraint").isGreaterThan(2.0);
            assertThat(key).withFailMessage("Selected field value doesn't match constraint").isEqualTo("mykey");
            assertThat(heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
            assertThat(valueMap.get("receiveTime")).withFailMessage("'receiveTime' is type Long").isInstanceOf(Long.class);
        }

        // Now every core field
        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);

        fs = new FieldSelector().addCoreFields(CoreField.ALL_SELECTABLE_FIELDS);
        results = qs.queryForFieldsAsMap(qc, fs);
        assertThat(results).hasSize(4);

        // The results will be a list of Maps
        for (Map<String, Object> valueMap : results)
        {
            assertThat(valueMap).withFailMessage("All fields returned").hasSize(CoreField.ALL_SELECTABLE_FIELDS.length);
            assertThat((Double) valueMap.get(CoreField.heading.toString())).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        // Add in max message count
        qc.setMaxMessageCount(1);
        results = qs.queryForFieldsAsMap(qc, fs);
        assertThat(results).hasSize(1);

        // Add in start message count
        qc.setMaxMessageCount(IQueryConstraints.DEFAULT_MAX_MESSAGES);
        qc.setStartMessageCount(3);
        results = qs.queryForFieldsAsMap(qc, fs);
        assertThat(results).hasSize(1);
    }

    /**
     * Test the field-level query method that returns results as a DTO bean.
     *
     * @throws ConflictingConstraintException shouldn't
     * @throws InvalidFieldSelectorException  shouldn't
     * @throws NoSuchFieldException           shouldn't
     */
    @Test
    public void testFieldQueryWithDTO() throws ConflictingConstraintException, InvalidFieldSelectorException, NoSuchFieldException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Start with a simple single core field query
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0); // Should be 4 matching RDMs
        FieldSelector fs = new FieldSelector().addCoreFields(CoreField.heading);

        List<DtoResult> results = qs.queryForFieldsAsBean(qc, fs, DtoResult.class);
        assertThat(results).hasSize(4);

        // The result will be a list of Objects
        for (DtoResult dto : results)
        {
            assertThat(dto.heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GT, 2.0);

        fs = new FieldSelector()
                .addCoreFields(CoreField.heading, CoreField.id, CoreField.receiveTime)
                .setStructure("BH_STATUS_MESSAGE").addExtendedFields("key", "Voltage");

        results = qs.queryForFieldsAsBean(qc, fs, DtoResult.class);
        assertThat(results).hasSize(4);

        // The results will be a list of DtoResults, the following fields will have been set:
        //    heading:     Double
        //    id:          Integer
        //    receiveTime: Long/BigInteger
        //    key:         String
        //    Voltage:     Double

        for (DtoResult dto : results)
        {
            Double heading = dto.heading;
            Long receiveTime = dto.receiveTime;
            String key = dto.key;
            Double voltage = dto.Voltage;
            Integer id = dto.id;

            assertThat(heading).withFailMessage("Has 'heading' value").isNotNull();
            assertThat(id).withFailMessage("Has 'id' value").isNotNull();
            assertThat(receiveTime).withFailMessage("Has 'receiveTime' value").isNotNull();
            assertThat(key).withFailMessage("Has 'key' value").isNotNull();
            assertThat(voltage).withFailMessage("Has 'voltage' value").isNotNull();

            assertThat(voltage).withFailMessage("Selected field value doesn't match constraint").isGreaterThan(2.0);
            assertThat(key).withFailMessage("Selected field value doesn't match constraint").isEqualTo("mykey");
            assertThat(heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        // Now every core field
        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);

        fs = new FieldSelector().addCoreFields(CoreField.ALL_SELECTABLE_FIELDS);
        results = qs.queryForFieldsAsBean(qc, fs, DtoResult.class);
        assertThat(results).hasSize(4);

        // The results will be a list of DtoResults
        for (DtoResult dto : results)
        {
            assertThat(dto.allCoreFieldsSet()).withFailMessage("All fields returned").isTrue();
            assertThat(dto.heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        // Add in max message count
        qc.setMaxMessageCount(1);
        results = qs.queryForFieldsAsBean(qc, fs, DtoResult.class);
        assertThat(results).hasSize(1);

        // Add in start message count
        qc.setMaxMessageCount(IQueryConstraints.DEFAULT_MAX_MESSAGES);
        qc.setStartMessageCount(3);
        results = qs.queryForFieldsAsBean(qc, fs, DtoResult.class);
        assertThat(results).hasSize(1);
    }

    /**
     * Test the field-level query against core fields only.
     *
     * @throws ConflictingConstraintException shouldn't
     * @throws InvalidFieldSelectorException  shouldn't
     */
    @Test
    public void testFieldQueryOnCoreFields() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // First, no constraints, just field selection
        FieldSelector fs = new FieldSelector().addCoreFields(CoreField.id);
        List<Object[]> results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(10);

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0); // Should be 4 matching RDMs

        // Just do a simple single field query
        fs = new FieldSelector().addCoreFields(CoreField.heading);
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(4);

        // The result will be a list of Object arrays containing a single value (doubles in this case)
        for (Object[] values : results)
        {
            assertThat(values).withFailMessage("Result array is of correct length").hasSize(1);
            assertThat((Double) values[0]).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        // Now a couple of fields
        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);

        fs = new FieldSelector().addCoreFields(CoreField.heading, CoreField.id);
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(4);

        // The results will be a list of arrays, each array is a Double and an Integer
        for (Object[] values : results)
        {
            assertThat(values).withFailMessage("Result array is of correct length").hasSize(2);
            assertThat((Double) values[0]).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        // Now every core field
        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);

        fs = new FieldSelector().addCoreFields(CoreField.ALL_SELECTABLE_FIELDS);
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(4);

        // The results will be a list of arrays
        for (Object[] values : results)
        {
            assertThat(values).withFailMessage("Result array is of correct length").hasSize(CoreField.ALL_SELECTABLE_FIELDS.length);
            assertThat((Double) values[0]).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        // Add in max message count
        qc.setMaxMessageCount(1);
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(1);

        // Add in start message count
        qc.setMaxMessageCount(IQueryConstraints.DEFAULT_MAX_MESSAGES);
        qc.setStartMessageCount(3);
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(1);

        // Add in some ordering
        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);
        qc.setSortingOptions(IQueryConstraints.SortOn.RECEIVE_DATE, IQueryConstraints.SortOrder.ASCENDING);
        fs = new FieldSelector().addCoreFields(CoreField.heading, CoreField.receiveTime, CoreField.id);
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(4);

        // The results will be a list of arrays, each array is a Double, a Long, and an Integer
        // I believe that under Postgres, receiveTime will be a Long, but under HSQL it is a BigInteger.

        long lastReceiveTime = -1;

        for (Object[] values : results)
        {
            assertThat(values).withFailMessage("Result array is of correct length").hasSize(3);

            double heading = (Double) values[0];
            long receiveTime = (Long) values[1];

            assertThat(heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
            assertThat(lastReceiveTime).withFailMessage("Not ordered by ascending receive time").isLessThan(receiveTime);
            lastReceiveTime = receiveTime;
        }
    }

    /**
     * Test the field-level query against core fields but with extended field constraints.
     *
     * @throws ConflictingConstraintException shouldn't
     * @throws InvalidFieldSelectorException  shouldn't
     * @throws NoSuchFieldException           shouldn't
     */
    @Test
    public void testFieldQueryOnCoreFieldsWithExtendedConstraints()
            throws ConflictingConstraintException, InvalidFieldSelectorException, NoSuchFieldException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.LT, 7.35); // 4 messages
        FieldSelector fs = new FieldSelector().addCoreFields(CoreField.id);
        List<?> results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(4);
    }

    /**
     * Test the field-level query against extended fields with only core field constraints.
     *
     * @throws ConflictingConstraintException shouldn't
     * @throws InvalidFieldSelectorException  shouldn't
     * @throws NoSuchFieldException           shouldn't
     */
    @Test
    public void testFieldQueryOnExtendedFields()
            throws ConflictingConstraintException, InvalidFieldSelectorException, NoSuchFieldException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // First, no constraints, just field selection
        FieldSelector fs = new FieldSelector().setStructure("BH_STATUS_MESSAGE").addExtendedFields("key");
        List<Object[]> results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(10);

        // The result will be a list of Object arrays containing a single value (strings in this case)
        for (Object[] values : results)
        {
            assertThat(values).withFailMessage("Result array is of correct length").hasSize(1);
            assertThat(values[0]).withFailMessage("Selected field value doesn't match constraint").isEqualTo("mykey");
        }

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0); // Should be 4 matching RDMs

        // Just do a simple single field query
        fs = new FieldSelector().setStructure("BH_STATUS_MESSAGE").addExtendedFields("key");
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(4);

        // The result will be a list of Object arrays containing a single value (strings in this case)
        for (Object[] values : results)
        {
            assertThat(values).withFailMessage("Result array is of correct length").hasSize(1);
            assertThat(values[0]).withFailMessage("Selected field value doesn't match constraint").isEqualTo("mykey");
        }

        // Now a couple of fields
        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);

        fs = new FieldSelector().setStructure("BH_STATUS_MESSAGE").addExtendedFields("key", "Voltage");
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(4);

        // The results will be a list of arrays, each array is a Double and an Integer
        for (Object[] values : results)
        {
            assertThat(values).withFailMessage("Result array is of correct length").hasSize(2);
            assertThat(values[0]).withFailMessage("Selected field value doesn't match constraint").isEqualTo("mykey");
            Double voltage = (Double) values[1];
            assertThat(voltage).withFailMessage("Selected field value doesn't match constraint").isBetween(0.0, 8.0);
        }

        // And finally select both core and extended fields with constraints on both core and extended fields
        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GT, 2.0);

        fs = new FieldSelector()
                .addCoreFields(CoreField.heading, CoreField.id)
                .setStructure("BH_STATUS_MESSAGE").addExtendedFields("key", "Voltage");
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(4);

        // The results will be a list of arrays, each array is:
        //    heading: Double
        //    id:      Integer
        //    key:     String
        //    voltage: Double

        for (Object[] values : results)
        {
            assertThat(values).withFailMessage("Result array is of correct length").hasSize(4);
            Double heading = (Double) values[0];
            String key = (String) values[2];
            Double voltage = (Double) values[3];

            assertThat(voltage).withFailMessage("Selected field value doesn't match constraint").isGreaterThan(2.0);
            assertThat(key).withFailMessage("Selected field value doesn't match constraint").isEqualTo("mykey");
            assertThat(heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        // And really finally, toss in a sort order
        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GT, 2.0);

        qc.setSortingOptions(IQueryConstraints.SortOn.RECEIVE_DATE, IQueryConstraints.SortOrder.DESCENDING);
        fs = new FieldSelector()
                .addCoreFields(CoreField.heading, CoreField.receiveTime)
                .setStructure("BH_STATUS_MESSAGE").addExtendedFields("Voltage");
        results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(4);

        // The results will be a list of arrays, each array is:
        //    heading: Double
        //    receiveTime: Long/BigInt
        //    voltage: Double

        // I believe that under Postgres, receiveTime will be a Long, but under HSQL it is a BigInteger.
        long lastReceiveTime = Long.MAX_VALUE;

        for (Object[] values : results)
        {
            assertThat(values).withFailMessage("Result array is of correct length").hasSize(3);

            Double heading = (Double) values[0];
            Long receiveTime = (Long) values[1];
            Double voltage = (Double) values[2];

            assertThat(voltage).withFailMessage("Selected field value doesn't match constraint").isGreaterThan(2.0);
            assertThat(heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
            assertThat(lastReceiveTime).withFailMessage("Not ordered by descending receive time").isGreaterThan(receiveTime);
            lastReceiveTime = receiveTime;
        }
    }

    /**
     * Test that calling count prior to calling a field level query operation works properly.  Regression test, this
     * was causing an exception in 1.25.
     *
     * @throws ConflictingConstraintException Shouldn't
     * @throws InvalidFieldSelectorException  Shouldn't
     */
    @Test
    public void testFieldQueryAfterCount() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        // Start with a simple single core field query
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0); // Should be 4 matching RDMs

        assertThat(qs.count(qc)).withFailMessage("Count is correct").isEqualTo(4);

        FieldSelector fs = new FieldSelector().addCoreFields(CoreField.heading);
        List<Map<String, Object>> results = qs.queryForFieldsAsMap(qc, fs);
        assertThat(results).hasSize(4);
    }

    @Test
    public void testSelectorProperties()
    {
        final CoreFieldSelector cf = new CoreFieldSelector(CoreField.elevation);
        assertThat(cf.isCoreField()).isTrue();
        assertThat(cf.getCoreField()).isEqualTo(CoreField.elevation);
        assertThat(cf.getStructureName()).isNull();

        final StructureFieldSelector sf = new StructureFieldSelector("SOME_STRUCT", "afield");
        assertThat(sf.isCoreField()).isFalse();
        assertThat(sf.getStructureName()).isEqualTo("SOME_STRUCT");
        assertThat(sf.getFieldName()).isEqualTo("afield");

        final StructureFieldSelector sf2 = new StructureFieldSelector("SOME_STRUCT", "a space");
        assertThat(sf2.getFieldName()).isEqualTo("a space");
        assertThat(sf2.getAlias()).isEqualTo("aspace");

        final AggregateFieldSelector as1 = new AggregateFieldSelector(AggregateType.AVG, cf);
        assertThat(as1.isCoreField()).isTrue();
        assertThat(as1.getFieldName()).isEqualTo(CoreField.elevation.name());
        assertThat(as1.getStructureName()).isNull();
        assertThat(as1.getField()).isSameAs(cf);
        assertThat(as1.getType()).isEqualTo(AggregateType.AVG);
        assertThat(as1.getAlias()).isEqualTo("avgOfElevation");

        final AggregateFieldSelector as2 = new AggregateFieldSelector(AggregateType.AVG, sf);
        assertThat(as2.isCoreField()).isFalse();
        assertThat(as2.getFieldName()).isEqualTo("afield");
        assertThat(as2.getStructureName()).isEqualTo("SOME_STRUCT");
        assertThat(as2.getField()).isSameAs(sf);
        assertThat(as2.getType()).isEqualTo(AggregateType.AVG);
        assertThat(as2.getAlias()).isEqualTo("avgOfAfield");

        final AggregateFieldSelector as3 = new AggregateFieldSelector(AggregateType.AVG, true, cf, "myAlias");
        assertThat(as3.isCoreField()).isTrue();
        assertThat(as3.getFieldName()).isEqualTo(CoreField.elevation.name());
        assertThat(as3.getStructureName()).isNull();
        assertThat(as3.getField()).isSameAs(cf);
        assertThat(as3.getType()).isEqualTo(AggregateType.AVG);
        assertThat(as3.getAlias()).isEqualTo("myAlias");
    }

    @Test
    public void testFieldSelector() throws ConflictingConstraintException
    {
        // Test FieldSelector to ensure all the new *Selector methods work properly and interact nicely with
        // the old string (field name) based methods.

        final String s1 = "S1";
        final String f1 = "f1";
        final CoreFieldSelector cf1 = new CoreFieldSelector(CoreField.sourceName);
        final StructureFieldSelector sf1 = new StructureFieldSelector(s1, f1);
        final AggregateFieldSelector af1 = new AggregateFieldSelector(AggregateType.COUNT, cf1);
        final AggregateFieldSelector af2 = new AggregateFieldSelector(AggregateType.COUNT, sf1);

        FieldSelector fs = new FieldSelector();
        assertThat(fs.isEmpty()).isTrue();

        fs.addCoreFields(CoreField.sourceName);
        assertThat(fs.isEmpty()).isFalse();
        assertThat(fs.usesExtendedFields()).isFalse();
        assertThat(fs.hasCoreFields()).isTrue();
        assertThat(fs.hasExtendedFields()).isFalse();
        assertThat(fs.hasAggregateFields()).isFalse();
        assertThat(fs.getCoreFields()).containsExactly(CoreField.sourceName);
        assertThat(fs.getCoreFieldSelectors()).containsExactly(cf1);
        assertThat(fs.getExtendedFields()).isEmpty();
        assertThat(fs.getExtendedFieldSelectors()).isEmpty();
        assertThat(fs.getAggregateFieldSelectors()).isEmpty();

        fs.setStructure(s1).addExtendedFields(f1);
        assertThat(fs.usesExtendedFields()).isTrue();
        assertThat(fs.hasCoreFields()).isTrue();
        assertThat(fs.hasExtendedFields()).isTrue();
        assertThat(fs.hasAggregateFields()).isFalse();
        assertThat(fs.getStructure()).isEqualTo(s1);
        assertThat(fs.getExtendedFields()).containsExactly(f1);
        assertThat(fs.getExtendedFieldSelectors()).containsExactly(sf1);

        fs = new FieldSelector();

        fs.addSelectors(cf1, sf1, af1);
        assertThat(fs.isEmpty()).isFalse();
        assertThat(fs.usesExtendedFields()).isTrue();
        assertThat(fs.hasCoreFields()).isTrue();
        assertThat(fs.hasExtendedFields()).isTrue();
        assertThat(fs.hasAggregateFields()).isTrue();
        assertThat(fs.getStructure()).isEqualTo(s1);
        assertThat(fs.getCoreFields()).containsExactly(CoreField.sourceName);
        assertThat(fs.getCoreFieldSelectors()).containsExactly(cf1);
        assertThat(fs.getExtendedFields()).containsExactly(f1);
        assertThat(fs.getExtendedFieldSelectors()).containsExactly(sf1);
        assertThat(fs.getAggregateFieldSelectors()).containsExactly(af1);

        fs = new FieldSelector();

        fs.addSelectors(af1);
        assertThat(fs.isEmpty()).isFalse();
        assertThat(fs.usesExtendedFields()).isFalse();
        assertThat(fs.hasCoreFields()).isFalse();
        assertThat(fs.hasExtendedFields()).isFalse();
        assertThat(fs.hasAggregateFields()).isTrue();
        assertThat(fs.getCoreFields()).isEmpty();
        assertThat(fs.getCoreFieldSelectors()).isEmpty();
        assertThat(fs.getExtendedFields()).isEmpty();
        assertThat(fs.getExtendedFieldSelectors()).isEmpty();
        assertThat(fs.getAggregateFieldSelectors()).containsExactly(af1);

        fs = new FieldSelector();

        fs.addSelectors(af2);
        assertThat(fs.isEmpty()).isFalse();
        assertThat(fs.usesExtendedFields()).isTrue();
        assertThat(fs.hasCoreFields()).isFalse();
        assertThat(fs.hasExtendedFields()).isFalse();
        assertThat(fs.hasAggregateFields()).isTrue();
        assertThat(fs.getStructure()).isEqualTo(s1);
        assertThat(fs.getAggregateFieldSelectors()).containsExactly(af2);

        fs = new FieldSelector();

        fs.addAggregateField(AggregateType.COUNT, CoreField.sourceName);
        fs.setStructure(s1).addAggregateField(AggregateType.COUNT, f1);
        assertThat(fs.usesExtendedFields()).isTrue();
        assertThat(fs.hasCoreFields()).isFalse();
        assertThat(fs.hasExtendedFields()).isFalse();
        assertThat(fs.hasAggregateFields()).isTrue();
        assertThat(fs.getStructure()).isEqualTo(s1);
        assertThat(fs.getAggregateFieldSelectors()).containsExactly(af1, af2);
    }

    @Test
    public void testFieldSelectorWithAlias() throws ConflictingConstraintException
    {
        // Test FieldSelector to ensure all the new *Selector methods work properly and interact nicely with
        // the old string (field name) based methods.

        final String s1 = "S1";
        final String f1 = "f1";
        final CoreFieldSelector cf1 = new CoreFieldSelector(CoreField.sourceName);
        final StructureFieldSelector sf1 = new StructureFieldSelector(s1, f1, "aliased_" + f1);
        final AggregateFieldSelector af1 = new AggregateFieldSelector(AggregateType.COUNT, cf1);
        final AggregateFieldSelector af2 = new AggregateFieldSelector(AggregateType.COUNT, sf1);

        FieldSelector fs = new FieldSelector();
        assertThat(fs.isEmpty()).isTrue();

        fs.addCoreFields(CoreField.sourceName);
        assertThat(fs.isEmpty()).isFalse();
        assertThat(fs.usesExtendedFields()).isFalse();
        assertThat(fs.hasCoreFields()).isTrue();
        assertThat(fs.hasExtendedFields()).isFalse();
        assertThat(fs.hasAggregateFields()).isFalse();
        assertThat(fs.getCoreFields()).containsExactly(CoreField.sourceName);
        assertThat(fs.getCoreFieldSelectors()).containsExactly(cf1);
        assertThat(fs.getExtendedFields()).isEmpty();
        assertThat(fs.getExtendedFieldSelectors()).isEmpty();
        assertThat(fs.getAggregateFieldSelectors()).isEmpty();

        fs.setStructure(s1).addExtendedFields(sf1);
        assertThat(fs.usesExtendedFields()).isTrue();
        assertThat(fs.hasCoreFields()).isTrue();
        assertThat(fs.hasExtendedFields()).isTrue();
        assertThat(fs.hasAggregateFields()).isFalse();
        assertThat(fs.getStructure()).isEqualTo(s1);
        assertThat(fs.getExtendedFields()).containsExactly(f1);
        assertThat(fs.getExtendedFieldSelectors()).containsExactly(sf1);

        fs = new FieldSelector();

        fs.addSelectors(cf1, sf1, af1);
        assertThat(fs.isEmpty()).isFalse();
        assertThat(fs.usesExtendedFields()).isTrue();
        assertThat(fs.hasCoreFields()).isTrue();
        assertThat(fs.hasExtendedFields()).isTrue();
        assertThat(fs.hasAggregateFields()).isTrue();
        assertThat(fs.getStructure()).isEqualTo(s1);
        assertThat(fs.getCoreFields()).containsExactly(CoreField.sourceName);
        assertThat(fs.getCoreFieldSelectors()).containsExactly(cf1);
        assertThat(fs.getExtendedFields()).containsExactly(f1);
        assertThat(fs.getExtendedFieldSelectors()).containsExactly(sf1);
        assertThat(fs.getAggregateFieldSelectors()).containsExactly(af1);

        fs = new FieldSelector();

        fs.addSelectors(af1);
        assertThat(fs.isEmpty()).isFalse();
        assertThat(fs.usesExtendedFields()).isFalse();
        assertThat(fs.hasCoreFields()).isFalse();
        assertThat(fs.hasExtendedFields()).isFalse();
        assertThat(fs.hasAggregateFields()).isTrue();
        assertThat(fs.getCoreFields()).isEmpty();
        assertThat(fs.getCoreFieldSelectors()).isEmpty();
        assertThat(fs.getExtendedFields()).isEmpty();
        assertThat(fs.getExtendedFieldSelectors()).isEmpty();
        assertThat(fs.getAggregateFieldSelectors()).containsExactly(af1);

        fs = new FieldSelector();

        fs.addSelectors(af2);
        assertThat(fs.isEmpty()).isFalse();
        assertThat(fs.usesExtendedFields()).isTrue();
        assertThat(fs.hasCoreFields()).isFalse();
        assertThat(fs.hasExtendedFields()).isFalse();
        assertThat(fs.hasAggregateFields()).isTrue();
        assertThat(fs.getStructure()).isEqualTo(s1);
        assertThat(fs.getAggregateFieldSelectors()).containsExactly(af2);

        fs = new FieldSelector();

        fs.addAggregateField(AggregateType.COUNT, CoreField.sourceName);
        fs.setStructure(s1).addAggregateField(AggregateType.COUNT, sf1);
        assertThat(fs.usesExtendedFields()).isTrue();
        assertThat(fs.hasCoreFields()).isFalse();
        assertThat(fs.hasExtendedFields()).isFalse();
        assertThat(fs.hasAggregateFields()).isTrue();
        assertThat(fs.getStructure()).isEqualTo(s1);
        assertThat(fs.getAggregateFieldSelectors()).containsExactly(af1, af2);
    }

    /**
     * Test Query Grouping
     */
    @Test
    public void testGroupingQuery() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);

        IQueryConstraints qc = qs.createEmptyConstraints();

        FieldSelector qfs = new FieldSelector()
                .addSelectors(new AggregateFieldSelector(AggregateType.COUNT, true, CoreField.longitude.selector));

        List<Map<String, Object>> results = qs.queryForFieldsAsMap(qc, qfs);
        assertThat(1).withFailMessage("count(*) single result").isEqualTo(results.size());
        assertThat(10).isEqualTo((long) results.get(0).get("countOfDistinctLongitude"));

        qc.addFieldConstraint(CoreField.longitude, ComparisonType.GT, -125.0);

        List<IRaptorDataMessage> rdms = qs.query(qc);
        assertThat(rdms).hasSize(5);
        assertThat(qs.count(qc)).isEqualTo(5);

        qfs = new FieldSelector()
                .addCoreFields(CoreField.time, CoreField.sourceName)
                .addSelectors(new AggregateFieldSelector(AggregateType.COUNT, false, CoreField.sourceName.selector));

        qc.setGrouping(true)
                .addGroupConstraint(new FieldConstraint(CoreField.time.selector, ComparisonType.LT, 25));

        results = qs.queryForFieldsAsMap(qc, qfs);

        assertThat(3).isEqualTo(results.size());
        Comparator<Map<String, Object>> resultComparator = Comparator.comparing(o -> ((Long) o.get("time")));

        results.sort(resultComparator);
        assertThat((long) results.get(0).get("time")).isEqualTo(0);
        assertThat((long) results.get(0).get("countOfSourceName")).isEqualTo(2);
        assertThat(results.get(0).get("sourceName")).isEqualTo("TESTING");

        assertThat((long) results.get(1).get("time")).isEqualTo(10);
        assertThat((long) results.get(1).get("countOfSourceName")).isEqualTo(1);
        assertThat(results.get(1).get("sourceName")).isEqualTo("TESTING");

        assertThat((long) results.get(2).get("time")).isEqualTo(20);
        assertThat((long) results.get(2).get("countOfSourceName")).isEqualTo(1);
        assertThat(results.get(2).get("sourceName")).isEqualTo("TESTING");

        //
        //
        //
        final AggregateFieldSelector countSourceName = new AggregateFieldSelector(AggregateType.COUNT, false, CoreField.sourceName.selector);
        qc.addGroupConstraint(new AggregateFieldConstraint(countSourceName, ComparisonType.GT, 1));

        results = qs.queryForFieldsAsMap(qc, qfs);

        assertThat(results).hasSize(1);
    }

    @Test
    public void testAggregateTypes() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        final IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        final IQueryConstraints qc = qs.createEmptyConstraints();

        final StructureFieldSelector voltageField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Voltage");
        final StructureFieldSelector accelxField = new StructureFieldSelector("BH_STATUS_MESSAGE", "ACCEL_X");

        FieldSelector fs = new FieldSelector()
                .setStructure("BH_STATUS_MESSAGE").addSelectors(
                        new AggregateFieldSelector(AggregateType.COUNT, false, voltageField),
                        new AggregateFieldSelector(AggregateType.COUNT, true, voltageField),
                        new AggregateFieldSelector(AggregateType.COUNT, false, accelxField),
                        new AggregateFieldSelector(AggregateType.COUNT, true, accelxField),
                        new AggregateFieldSelector(AggregateType.SUM, false, accelxField),
                        new AggregateFieldSelector(AggregateType.SUM, true, accelxField),
                        new AggregateFieldSelector(AggregateType.MIN, false, CoreField.speed.selector),
                        new AggregateFieldSelector(AggregateType.MAX, false, CoreField.speed.selector),
                        new AggregateFieldSelector(AggregateType.AVG, false, CoreField.speed.selector));

        final List<Map<String, Object>> rmaps = qs.queryForFieldsAsMap(qc, fs);
        assertThat(rmaps).hasSize(1);

        final Map<String, Object> map = rmaps.get(0);
        assertThat(map.get("countOfVoltage")).isEqualTo(10L);
        assertThat(map.get("countOfDistinctVoltage")).isEqualTo(10L);
        assertThat(map.get("countOfACCEL_X")).isEqualTo(10L);
        assertThat(map.get("countOfDistinctACCEL_X")).isEqualTo(1L);
        assertThat(map.get("sumOfACCEL_X")).isEqualTo(42.0 * 10);
        assertThat(map.get("sumOfDistinctACCEL_X")).isEqualTo(42.0);
        assertThat(map.get("minOfSpeed")).isEqualTo(100.0);
        assertThat(map.get("maxOfSpeed")).isEqualTo(1000.0);
        assertThat(map.get("avgOfSpeed")).isEqualTo((100.0 + 1000.0) / 2);
    }

    @Test
    public void testAggregateTypesWithAlias() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        final IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        final IQueryConstraints qc = qs.createEmptyConstraints();

        final StructureFieldSelector voltageField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Voltage", "alias_Voltage");
        final StructureFieldSelector accelxField = new StructureFieldSelector("BH_STATUS_MESSAGE", "ACCEL_X", "alias_ACCEL_X");

        FieldSelector fs = new FieldSelector()
                .setStructure("BH_STATUS_MESSAGE").addSelectors(
                        new AggregateFieldSelector(AggregateType.COUNT, false, voltageField),
                        new AggregateFieldSelector(AggregateType.COUNT, true, voltageField),
                        new AggregateFieldSelector(AggregateType.COUNT, false, accelxField),
                        new AggregateFieldSelector(AggregateType.COUNT, true, accelxField),
                        new AggregateFieldSelector(AggregateType.SUM, false, accelxField),
                        new AggregateFieldSelector(AggregateType.SUM, true, accelxField),
                        new AggregateFieldSelector(AggregateType.MIN, false, CoreField.speed.selector),
                        new AggregateFieldSelector(AggregateType.MAX, false, CoreField.speed.selector),
                        new AggregateFieldSelector(AggregateType.AVG, false, CoreField.speed.selector));

        final List<Map<String, Object>> rmaps = qs.queryForFieldsAsMap(qc, fs);
        assertThat(rmaps).hasSize(1);

        final Map<String, Object> map = rmaps.get(0);
        assertThat(map.get("countOfAlias_Voltage")).isEqualTo(10L);
        assertThat(map.get("countOfDistinctAlias_Voltage")).isEqualTo(10L);
        assertThat(map.get("countOfAlias_ACCEL_X")).isEqualTo(10L);
        assertThat(map.get("countOfDistinctAlias_ACCEL_X")).isEqualTo(1L);
        assertThat(map.get("sumOfAlias_ACCEL_X")).isEqualTo(42.0 * 10);
        assertThat(map.get("sumOfDistinctAlias_ACCEL_X")).isEqualTo(42.0);
        assertThat(map.get("minOfSpeed")).isEqualTo(100.0);
        assertThat(map.get("maxOfSpeed")).isEqualTo(1000.0);
        assertThat(map.get("avgOfSpeed")).isEqualTo((100.0 + 1000.0) / 2);
    }

    @Test
    public void testGroupingQueryExtended()
            throws ConflictingConstraintException, InvalidFieldSelectorException, NoSuchFieldException
    {

        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        FieldSelector fs = new FieldSelector()
                .setStructure("BH_STATUS_MESSAGE").addExtendedFields("Panic", "Voltage")
                .addCoreFields(CoreField.time, CoreField.heading);

        // Just do a simple single field query
        List<Object[]> results = qs.queryForFields(qc, fs);
        assertThat(results).hasSize(10);

        //
        // Group
        //
        fs = new FieldSelector()
                .setStructure("BH_STATUS_MESSAGE").addExtendedFields("Panic")
                .addAggregateField(AggregateType.COUNT, CoreField.heading)
                .addAggregateField(AggregateType.COUNT, "Voltage");

        // Group by panic; no grouping constraints
        qc.setGrouping(true);

        List<Map<String, Object>> rmaps = qs.queryForFieldsAsMap(qc, fs);
        assertThat(rmaps).hasSize(2);  // Panic is boolean

        final Map<String, Object> result0 = rmaps.get(0);
        final Map<String, Object> result1 = rmaps.get(1);

        assertThat(result0).containsKey("Panic");
        assertThat(result0).containsKey("countOfHeading");
        assertThat(result0).containsKey("countOfVoltage");
        // 5 and 5
        assertThat((long) result0.get("countOfHeading")).isEqualTo(5);
        assertThat((long) result1.get("countOfHeading")).isEqualTo(5);

        assertThat(((Boolean) result0.get("Panic")) || ((Boolean) result1.get("Panic"))).isTrue();

        final AggregateFieldSelector countHeading = new AggregateFieldSelector(AggregateType.COUNT, false, CoreField.heading.selector);
        qc.addGroupConstraint(new AggregateFieldConstraint(countHeading, ComparisonType.GT, 5));

        rmaps = qs.queryForFieldsAsMap(qc, fs);
        assertThat(rmaps).isEmpty(); // Panic is boolean; max heading is 90, 100 with true,false

        qc.clear();
        final StructureFieldSelector panicField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Panic");

        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GT, -1e6);
        qc.setGrouping(true)
                .addGroupConstraint(new FieldConstraint(panicField, ComparisonType.EQ, true));

        rmaps = qs.queryForFieldsAsMap(qc, fs);
        assertThat(rmaps).hasSize(1);
        assertThat((Boolean) result0.get("Panic")).isTrue();
    }

    @Test
    public void testGroupingQueryWithExtendedAggregate() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        final IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        final IQueryConstraints qc = qs.createEmptyConstraints();

        FieldSelector qfs = new FieldSelector()
                .setStructure("BH_STATUS_MESSAGE")
                .addAggregateField(AggregateType.COUNT, true, "Panic");

        List<Map<String, Object>> results = qs.queryForFieldsAsMap(qc, qfs);
        assertThat(results).withFailMessage("count(*) single result").hasSize(1);
        assertThat((long) results.get(0).get("countOfDistinctPanic")).isEqualTo(2);

        // Test basic grouping on core field and select an aggregate on an extended
        qc.clear();
        final StructureFieldSelector panicField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Panic");
        final AggregateFieldSelector countPanic = new AggregateFieldSelector(AggregateType.COUNT, panicField);

        qc.setGrouping(true);

        qfs = new FieldSelector().addCoreFields(CoreField.sourceName).addSelectors(countPanic);
        results = qs.queryForFieldsAsMap(qc, qfs);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("countOfPanic")).isEqualTo(10L);
        assertThat(results.get(0).get(countPanic.getAlias())).isEqualTo(10L);

        // Ensure that things work if the only reference to an extended field is in a group constraint
        qc.clear();
        qc.setGrouping(true).addGroupConstraint(new AggregateFieldConstraint(countPanic, ComparisonType.EQ, 10));

        qfs = new FieldSelector().addCoreFields(CoreField.sourceName);
        results = qs.queryForFieldsAsMap(qc, qfs);
        assertThat(results).hasSize(1);
    }

    @Test
    public void testMissingFieldInAggregateFails()
            throws ConflictingConstraintException, IllegalStateException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);

        IQueryConstraints qc = qs.createEmptyConstraints();

        // No such extended field "Panic B" nor "Panic B"
        FieldSelector qfs = new FieldSelector()
                .setStructure("BH_STATUS_MESSAGE")
                .addExtendedFields("Panic B")
                .addAggregateField(AggregateType.COUNT, true, "Panic A");

        assertThatExceptionOfType(QueryExecutionException.class)
                .isThrownBy(() -> qs.queryForFieldsAsMap(qc, qfs));
    }

    @Test
    public void testLogicConstraintOnCoreFields()
            throws ConflictingConstraintException, InvalidFieldSelectorException, IllegalStateException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);

        IQueryConstraints qc = qs.createEmptyConstraints();

        //
        // Subcase 1
        //

        // NOT (heading < 50)
        qc.addLogicConstraint(LogicType.not, new FieldConstraint(CoreField.heading.selector, ComparisonType.LT, 50.));

        FieldSelector qfs = new FieldSelector()
                .setStructure("BH_STATUS_MESSAGE")
                .addCoreFields(CoreField.heading);

        List<Map<String, Object>> results = qs.queryForFieldsAsMap(qc, qfs);

        // ==> heading >= 50
        for (Map<String, Object> oneMap : results)
        {
            assertThat(((Double) oneMap.get("heading"))).withFailMessage("heading").isGreaterThanOrEqualTo(50);
        }
        assertThat(results).withFailMessage("heading 6").hasSize(6);

        //
        // Subcase 2
        //
        qc.clear();
        qc.addLogicConstraints(new LogicConstraint(LogicType.or,
                new FieldConstraint(CoreField.heading.selector, ComparisonType.EQ, 50.),
                new FieldConstraint(CoreField.heading.selector, ComparisonType.EQ, 60.)));

        results = qs.queryForFieldsAsMap(qc, qfs);
        assertThat(results).withFailMessage("heading = {50,60}").hasSize(2);
        Double heading0 = (Double) results.get(0).get("heading");
        Double heading1 = (Double) results.get(1).get("heading");
        assertThat(List.of(heading0, heading1)).containsOnly(50.0, 60.0);

        //
        // Subcase 3
        //
        qc.clear();
        qc.addLogicConstraint(LogicType.not,
                new LogicConstraint(LogicType.or,
                        new FieldConstraint(CoreField.heading.selector, ComparisonType.LT, 50.),
                        new FieldConstraint(CoreField.heading.selector, ComparisonType.GT, 60.)));

        results = qs.queryForFieldsAsMap(qc, qfs);
        assertThat(results).withFailMessage("heading = {50,60}").hasSize(2);
        heading0 = (Double) results.get(0).get("heading");
        heading1 = (Double) results.get(1).get("heading");
        assertThat(List.of(heading0, heading1)).containsOnly(50.0, 60.0);

        //
        // Subcase 4
        //
        qc.addLogicConstraints(
                new LogicConstraint(LogicType.not, new FieldConstraint(CoreField.heading.selector, ComparisonType.LT, 50.)),
                new LogicConstraint(LogicType.not, new FieldConstraint(CoreField.heading.selector, ComparisonType.GT, 60.)));
        results = qs.queryForFieldsAsMap(qc, qfs);
        assertThat(results).withFailMessage("heading = {50,60}").hasSize(2);
        heading0 = (Double) results.get(0).get("heading");
        heading1 = (Double) results.get(1).get("heading");
        assertThat(List.of(heading0, heading1)).containsOnly(50.0, 60.0);
    }

    @Test
    public void testLogicConstraintOnExtendedFields()
            throws ConflictingConstraintException, InvalidFieldSelectorException, IllegalStateException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);

        FieldSelector qfs = new FieldSelector()
                .setStructure("BH_STATUS_MESSAGE").addExtendedFields("Panic", "Voltage")
                .addCoreFields(CoreField.time, CoreField.heading);

        IQueryConstraints qc = qs.createEmptyConstraints();

        final StructureFieldSelector panicField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Panic");
        final StructureFieldSelector voltageField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Voltage");

        qc.addLogicConstraint(LogicType.or,
                new LogicConstraint(LogicType.and,
                        new LogicConstraint(LogicType.not,
                                new FieldConstraint(panicField, ComparisonType.NEQ, true)),
                        new FieldConstraint(voltageField, ComparisonType.GT, 7.55),
                        new FieldConstraint(voltageField, ComparisonType.LT, 7.85)),

                new LogicConstraint(LogicType.and,
                        new LogicConstraint(LogicType.not,
                                new FieldConstraint(panicField, ComparisonType.NEQ, false)),
                        new FieldConstraint(voltageField, ComparisonType.GT, 7.65),
                        new FieldConstraint(voltageField, ComparisonType.LT, 7.95)));
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 35.0);
        qc.setSortingOptions(IQueryConstraints.SortOn.DEVICE_DATE, IQueryConstraints.SortOrder.DESCENDING);

        List<Map<String, Object>> results = qs.queryForFieldsAsMap(qc, qfs);
        assertThat(results).withFailMessage("complex").hasSize(2);
        assertThat(results.get(0).get("heading")).withFailMessage("missed 30 == heading @t=10").isEqualTo(30.0);
        assertThat(results.get(1).get("heading")).withFailMessage("missed 30 == heading @t= 0").isEqualTo(20.0);
    }

    @Test
    public void testLogicConstraintOnGrouping()
            throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);

        IQueryConstraints qc = qs.createEmptyConstraints();

        qc.addFieldConstraint(CoreField.longitude, ComparisonType.GT, -125.0);

        FieldSelector qfs = new FieldSelector()
                .addCoreFields(CoreField.time, CoreField.sourceName)
                .addAggregateField(AggregateType.COUNT, CoreField.sourceName);

        qc.setGrouping(true)
                .addGroupConstraint(new LogicConstraint(LogicType.not,
                        new FieldConstraint(CoreField.time.selector, ComparisonType.GTE, 25)));

        List<Map<String, Object>> results = qs.queryForFieldsAsMap(qc, qfs);
        assertThat(results).hasSize(3);
    }

    @Test
    public void testMultiVersionQueries()
            throws NoSuchFieldException, ConflictingConstraintException, InvalidFieldSelectorException
    {
        final PluginQueryService qs = createMultiVersionQueryService();
        final IQueryConstraints qc = qs.createEmptyConstraints();

        // Validate core fields
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.EQ, "source2");
        long count = qs.count(qc);
        assertThat(count).isEqualTo(2);
        List<IRaptorDataMessage> results = qs.query(qc);
        assertThat(results).hasSize(2); // 1 from each version

        // Validate extended fields
        qc.clear();
        qc.addFieldConstraint(MULTI_VERSION_MESSAGE, "int", ComparisonType.GTE, 2);
        count = qs.count(qc);
        assertThat(count).isEqualTo(6);
        results = qs.query(qc);
        assertThat(results).hasSize(6); // 2,3,4 for each version, 6 total

        // Now some logic constraints

        final StructureFieldSelector intField = new StructureFieldSelector(MULTI_VERSION_MESSAGE, "int");

        qc.clear();
        qc.addLogicConstraints(
                new LogicConstraint(LogicType.or,
                        new FieldConstraint(intField, ComparisonType.LT, 2),
                        new FieldConstraint(intField, ComparisonType.GT, 3)));

        count = qs.count(qc);
        assertThat(count).isEqualTo(6);
        results = qs.query(qc);
        assertThat(results).hasSize(6); // 0, 1, 4 for each version, 6 total

        // Same constraints, but select a field instead of RDMs
        // Same constraints, but select a CORE FIELD instead of RDMs
        // TODO: uncomment this when structure field constraints are worked out
//        qc.clear();
//        qc.addLogicConstraints(
//                new LogicConstraint(LogicType.or,
//                        new FieldConstraint("int", ComparisonType.LT, 2),
//                        new FieldConstraint("int", ComparisonType.GT, 3)));
//
//        FieldSelector fs = new FieldSelector()
//                .addCoreFields(CoreField.id);
//
//        List<Object[]> fieldsList = qs.queryForFields(qc, fs);
//        assertThat(fieldsList).hasSize(6);

        // Same constraints, but select an EXTENDED FIELD instead of RDMs
        qc.clear();
        qc.addLogicConstraints(
                new LogicConstraint(LogicType.or,
                        new FieldConstraint(intField, ComparisonType.LT, 2),
                        new FieldConstraint(intField, ComparisonType.GT, 3)));

        FieldSelector fs = new FieldSelector()
                .setStructure(MULTI_VERSION_MESSAGE)
                .addExtendedFields("int");

        List<Object[]> fieldsList = qs.queryForFields(qc, fs);
        assertThat(fieldsList).hasSize(6);

        for (Object[] fields : fieldsList)
        {
            assertThat(fields).hasSize(1);
            int intValue = (Integer) fields[0];
            assertThat(intValue < 2 || intValue > 3).isTrue(); // Validate the constraints
        }

        // Invert it
        qc.clear();
        qc.addLogicConstraints(
                new LogicConstraint(LogicType.not,
                        new LogicConstraint(LogicType.or,
                                new FieldConstraint(intField, ComparisonType.LT, 2),
                                new FieldConstraint(intField, ComparisonType.GT, 3))));

        fs = new FieldSelector()
                .setStructure(MULTI_VERSION_MESSAGE)
                .addExtendedFields("int");

        fieldsList = qs.queryForFields(qc, fs);
        assertThat(fieldsList).hasSize(4);

        for (Object[] fields : fieldsList)
        {
            assertThat(fields).hasSize(1);
            int intValue = (Integer) fields[0];
            assertThat(intValue < 2 || intValue > 3).isFalse(); // Validate the constraints
        }
    }

    @Test
    public void testMultiVersionGrouping() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        final PluginQueryService qs = createMultiVersionQueryService();
        final IQueryConstraints qc = qs.createEmptyConstraints();

        // With aggregates/grouping, the unions aren't coalesced, so we get one result per version of the structure.

        // Test an aggregate
        qc.setGrouping(true);
        FieldSelector fs = new FieldSelector()
                .setStructure(MULTI_VERSION_MESSAGE)
                .addAggregateField(AggregateType.COUNT, true, "string")
                .addAggregateField(AggregateType.SUM, "int")
                .addAggregateField(AggregateType.MIN, "int")
                .addAggregateField(AggregateType.MAX, "int");

        List<Map<String, Object>> rmaps = qs.queryForFieldsAsMap(qc, fs);
        assertThat(rmaps).hasSize(2);

        for (Map<String, Object> rmap : rmaps)
        {
            assertThat(rmap).hasSize(4);

            long sumOfInt = (Long) rmap.get("sumOfInt");
            long minOfInt = (Integer) rmap.get("minOfInt");
            long maxOfInt = (Integer) rmap.get("maxOfInt");
            long countOfDistinctString = (Long) rmap.get("countOfDistinctString");

            assertThat(sumOfInt).isEqualTo(10);
            assertThat(minOfInt).isEqualTo(0);
            assertThat(maxOfInt).isEqualTo(4);
            assertThat(countOfDistinctString).isEqualTo(5);
        }

        // Grouping on int2 will yield 3 distinct groups for each version.

        qc.clear();
        qc.setGrouping(true);
        fs = new FieldSelector()
                .setStructure(MULTI_VERSION_MESSAGE)
                .addExtendedFields("int2")
                .addAggregateField(AggregateType.COUNT, true, "string")
                .addAggregateField(AggregateType.SUM, "int");

        rmaps = qs.queryForFieldsAsMap(qc, fs);

        assertThat(rmaps).hasSize(6);

        int int2is1 = 0;
        int int2is2 = 0;
        int int2is3 = 0;

        for (Map<String, Object> rmap : rmaps)
        {
            assertThat(rmap).hasSize(3);

            int int2 = (Integer) rmap.get("int2");
            long sumOfInt = (Long) rmap.get("sumOfInt");
            long countOfDistinctString = (Long) rmap.get("countOfDistinctString");

            switch (int2)
            {
                case 1:
                    int2is1 += 1;
                    assertThat(sumOfInt).isEqualTo(0);
                    assertThat(countOfDistinctString).isEqualTo(1);
                    break;
                case 2:
                    int2is2 += 1;
                    assertThat(sumOfInt).isEqualTo(3); // 1 + 2
                    assertThat(countOfDistinctString).isEqualTo(2);
                    break;
                case 3:
                    int2is3 += 1;
                    assertThat(sumOfInt).isEqualTo(7); // 3 + 4
                    assertThat(countOfDistinctString).isEqualTo(2);
                    break;
                default:
                    fail("Unexpected value for int2: " + int2);
            }
        }

        assertThat(int2is1).isEqualTo(2);
        assertThat(int2is2).isEqualTo(2);
        assertThat(int2is3).isEqualTo(2);

        qc.clear();

        // Add a constraint on the aggregate.  Grouping on int2 will yield 3 distinct groups for each version.
        // The group constraint will reduce that to 1 match, so we should get 2 results, one per structure version.

        final StructureFieldSelector intField = new StructureFieldSelector(MULTI_VERSION_MESSAGE, "int");
        final AggregateFieldSelector sumInt = new AggregateFieldSelector(AggregateType.SUM, false, intField);

        qc.setGrouping(true)
                .addGroupConstraint(new AggregateFieldConstraint(sumInt, ComparisonType.GT, 3));

        fs = new FieldSelector()
                .setStructure(MULTI_VERSION_MESSAGE)
                .addExtendedFields("int2")
                .addAggregateField(AggregateType.COUNT, true, "string")
                .addAggregateField(AggregateType.SUM, "int");

        rmaps = qs.queryForFieldsAsMap(qc, fs);

        assertThat(rmaps).hasSize(2);

        for (Map<String, Object> rmap : rmaps)
        {
            assertThat(rmap).hasSize(3);

            int int2 = (Integer) rmap.get("int2");
            long sumOfInt = (Long) rmap.get("sumOfInt");
            long countOfDistinctString = (Long) rmap.get("countOfDistinctString");

            assertThat(int2).isEqualTo(3);
            assertThat(sumOfInt).isEqualTo(7); // 3 + 4
            assertThat(countOfDistinctString).isEqualTo(2);
        }
    }

    @Test
    public void testShapeConstraints()
    {
        final PluginQueryService qs = createMultiVersionQueryService();
        final IQueryConstraints qc = qs.createEmptyConstraints();

        // Square centered at (1,1) with a size of .5 should yield 2 RDMs (both at 1,1)
        qc.addShapeConstraint(new Square("square", new RaptorLocation(.5, .5), new RaptorLocation(1.5, 1.5)));
        List<IRaptorDataMessage> results = qs.query(qc);

        assertThat(results).hasSize(2);
        for (IRaptorDataMessage rdm : results)
        {
            assertThat(rdm.getLatitude()).isEqualTo(1);
            assertThat(rdm.getLongitude()).isEqualTo(1);
        }

        qc.clear();
        qc.addShapeConstraint(new RaptorLocation(.5, .5), new RaptorLocation(1.5, 1.5));
        List<IRaptorDataMessage> results2 = qs.query(qc);

        assertThat(results).isEqualTo(results2);

        // Square centered at (1,1) with a size of 1 should yield 6 RDMs (0,0, 1,1, and 2,2)
        qc.clear();
        qc.addShapeConstraint(new Square("square", new RaptorLocation(0, 0), new RaptorLocation(2, 2)));
        results = qs.query(qc);

        assertThat(results).hasSize(6);
        for (IRaptorDataMessage rdm : results)
        {
            assertThat(rdm.getLatitude()).isBetween(0.0, 2.0);
            assertThat(rdm.getLongitude()).isBetween(0.0, 2.0);
        }

        qc.clear();
        qc.addShapeConstraint(new ShapeConstraint(new RaptorLocation(0, 0), new RaptorLocation(2, 2)));
        results2 = qs.query(qc);

        assertThat(results).isEqualTo(results2);

        // Rectangle from (.5,0) to (2.5,3.5) should yield 4 RDMs (1,1, and 2,2)
        qc.clear();
        qc.addShapeConstraint(new Rectangle("rectangle", new RaptorLocation(0.5, 0), new RaptorLocation(2.5, 3.5)));
        results = qs.query(qc);

        assertThat(results).hasSize(4);
        for (IRaptorDataMessage rdm : results)
        {
            assertThat(rdm.getLatitude()).isBetween(1.0, 2.0);
            assertThat(rdm.getLongitude()).isBetween(1.0, 2.0);
        }

        qc.clear();
        qc.addShapeConstraint(new RaptorLocation(0.5, 0), new RaptorLocation(2.5, 3.5));
        results2 = qs.query(qc);

        assertThat(results).isEqualTo(results2);

        // Circle centered at (1,1) with a radius of .5 should yield 2 RDMs (at 1,1)
        qc.clear();
        qc.addShapeConstraint(new Circle("circle", new RaptorLocation(1, 1), .5 * ONE_DEGREE_METERS));
        results = qs.query(qc);

        assertThat(results).hasSize(2);
        for (IRaptorDataMessage rdm : results)
        {
            assertThat(rdm.getLatitude()).isEqualTo(1);
            assertThat(rdm.getLongitude()).isEqualTo(1);
        }

        qc.clear();
        qc.addShapeConstraint(new RaptorLocation(1, 1), .5 * ONE_DEGREE_METERS);
        results2 = qs.query(qc);

        assertThat(results).isEqualTo(results2);

        // Polygon that should encompass only (2,2) and (4,4) (an L shape going up from 2 and over to 4, skipping 3)
        qc.clear();

        final List<ILocation> vertices = Arrays.asList(
                new RaptorLocation(1.5, 1.5), new RaptorLocation(4.5, 1.5), new RaptorLocation(4.5, 4.5),
                new RaptorLocation(3.5, 4.5), new RaptorLocation(3.5, 2.5), new RaptorLocation(1.5, 2.5));
        final Polygon poly = new Polygon("poly", vertices);

        qc.addShapeConstraint(poly);
        results = qs.query(qc);

        assertThat(results).hasSize(4);
        for (IRaptorDataMessage rdm : results)
        {
            assertThat(rdm.getLatitude() == 2 || rdm.getLatitude() == 4).isTrue();
            assertThat(rdm.getLongitude() == 2 || rdm.getLongitude() == 4).isTrue();
        }

        qc.clear();
        qc.addShapeConstraint(vertices);
        results2 = qs.query(qc);

        assertThat(results).isEqualTo(results2);
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @Test
    public void testShapeConstraintsLegalShapeTypes()
    {
        // Test that the shape constraints allows all the supported types

        new ShapeConstraint(new Square("square", new RaptorLocation(1, 1), 0, 1));
        new ShapeConstraint(new Rectangle("rectangle", new RaptorLocation(1, 1), 0, 1, 1));
        new ShapeConstraint(new Circle("circle", new RaptorLocation(1, 1), 1));
        new ShapeConstraint(new Ellipse("ellipse", new RaptorLocation(1, 1), 0, 1, 2));
        new ShapeConstraint(new Polygon("polygon", Arrays.asList(new RaptorLocation(1, 1), new RaptorLocation(2, 1), new RaptorLocation(1, 2))));
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @Test
    public void testShapeConstraintsIllegalShapeType()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ShapeConstraint(CdiTestUtils.newInstance(ShapeRangeFanMapObject.class)));
    }

    /**
     * Count the number of messages in an iterator.
     *
     * @param iterator Iterator to traverse
     * @return count of messages provided by the iterator
     */
    private int countMessages(Iterator<?> iterator)
    {
        int count = 0;
        while (iterator.hasNext())
        {
            count += 1;
            Object o = iterator.next();
            assertThat(o).withFailMessage("Unexpected type in iterator").isInstanceOf(IPrePersistRaptorDataMessage.class);
        }
        return count;
    }

    /**
     * DTO class used to test field-level queries returning Objects.
     */
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    public static class DtoResult
    {
        // Core fields
        private Integer id;
        private Double latitude;
        private Double longitude;
        private Double elevation;
        private Long time;
        private Long receiveTime;
        private Long relevantTime;
        private Double heading;
        private Double pitch;
        private Double roll;
        private Double speed;
        private String sourceName;
        private Integer sourceType;
        private Boolean hasMotion;
        private Boolean hasLocation;
        private Boolean hasElevation;
        private Boolean hasHeading;
        private Boolean has3dOrientation;
        private Boolean hasSpeed;
        private Boolean hasTime;
        private Boolean isManualLocation;
        private Boolean isRenderable;
        private Integer trackStyleId;
        private Integer overrideTrackStyleId;
        private UUID uuid;
        private Integer originId;
        private UUID originUuid;
        private Integer mapObject_id;
        private Boolean isImported;

        // Extended fields
        private String key;
        private Double Voltage; // Note capital V to match field name in results

        public boolean allCoreFieldsSet()
        {
            // Not all our fields will actually get set, the test RDMs don't include:
            //   trackStyleId
            //   overrideTrackStyleId
            //   originId
            //   originUuid

            return id != null && latitude != null && longitude != null && elevation != null &&
                    time != null && receiveTime != null && relevantTime != null && heading != null && pitch != null &&
                    roll != null && speed != null && sourceName != null && sourceType != null &&
                    hasMotion != null && hasLocation != null && hasElevation != null && hasHeading != null
                    && has3dOrientation != null && hasSpeed != null && hasTime != null && isManualLocation != null
                    && isRenderable != null && uuid != null && mapObject_id != null && isImported != null;
        }
    }
}
