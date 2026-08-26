package gov.takx.dataservices.persist;

import gov.takx.api.mapobject.FieldType;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.ICustomDataMonitorPersist;
import gov.takx.api.plugin.datamonitor.IDataMonitorDelegate;
import gov.takx.api.plugin.datamonitor.TestDataMonitorDelegate;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.CoreField;
import gov.takx.commons.constants.State;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import gov.takx.test.datasim.BloodHoundSim;
import gov.takx.test.datasim.BlueJaySim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class CustomDataMonitorTest extends AContainerTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BH0_KEY = BloodHoundSim.makeUnitIdKey("0");
    private static final String BJ0_KEY = BlueJaySim.makeUnitIdKey("0");

    private PersistTestSetup test;

    private IMapObject bhMo0;
    private IRaptorDataStructure bhRds;
    private IPrePersistRaptorDataMessage bhRdm;

    private IMapObject bjMo0;
    private IRaptorDataStructure bjRds;
    private IPrePersistRaptorDataMessage bjRdm;

    private IDataMonitorDelegate delegate;

    @BeforeAll
    public void setup()
    {
        logger.debug("{} is starting", getClass().getCanonicalName());

        test = new PersistTestSetup();
        test.setup();

        bhMo0 = test.getMapObject(BH0_KEY);
        bhRds = test.getBloodhoundStruct();
        BloodHoundSim bhSim = new BloodHoundSim();
        bhRdm = bhSim.makeDataMessage(bhMo0);

        bjMo0 = test.getMapObject(BJ0_KEY);
        bjRds = test.getBlueJayStruct();
        BlueJaySim bjSim = new BlueJaySim();
        bjRdm = bjSim.makeDataMessage(bjMo0);

        delegate = new TestDataMonitorDelegate();
    }

    @AfterAll
    public void close()
    {
        test.shutdown();
    }

    public Iterable<Object[]> makeNumericTestValues()
    {
        return Arrays.asList(
                new Object[]{ComparisonType.EQ, "10", (short) 10, State.Critical},
                new Object[]{ComparisonType.EQ, "10", (short) 11, State.Active},
                new Object[]{ComparisonType.NEQ, "10", (short) 10, State.Active},
                new Object[]{ComparisonType.NEQ, "10", (short) 11, State.Critical},
                new Object[]{ComparisonType.LT, "20", (short) 10, State.Critical},
                new Object[]{ComparisonType.LT, "20", (short) 20, State.Active},
                new Object[]{ComparisonType.LT, "20", (short) 21, State.Active},
                new Object[]{ComparisonType.LTE, "20", (short) 10, State.Critical},
                new Object[]{ComparisonType.LTE, "20", (short) 20, State.Critical},
                new Object[]{ComparisonType.LTE, "20", (short) 21, State.Active},
                new Object[]{ComparisonType.GT, "42", (short) 43, State.Critical},
                new Object[]{ComparisonType.GT, "42", (short) 42, State.Active},
                new Object[]{ComparisonType.GT, "42", (short) 41, State.Active},
                new Object[]{ComparisonType.GTE, "42", (short) 42, State.Critical},
                new Object[]{ComparisonType.GTE, "42", (short) 43, State.Critical},
                new Object[]{ComparisonType.GTE, "42", (short) 41, State.Active});
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testShortThresholds(ComparisonType comparisonType, String threshold, short value, State expected)
    {
        final CustomDataMonitor cdm = makeCDM(bjMo0, bjRds.getMessageType(), "shortVal", comparisonType, threshold, State.Critical, FieldType.SHORT);
        final IDataMonitor dataMonitor = cdm.getDataMonitor();

        bjRdm.setShort("shortVal", value);
        dataMonitor.analyze(bjRdm);
        assertThat(dataMonitor.getCurrentState()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testIntThresholds(ComparisonType comparisonType, String threshold, int value, State expected)
    {
        final CustomDataMonitor cdm = makeCDM(bhMo0, bhRds.getMessageType(), "Height", comparisonType, threshold, State.Critical, FieldType.INTEGER);
        final IDataMonitor dataMonitor = cdm.getDataMonitor();

        bhRdm.setInt("Height", value);
        dataMonitor.analyze(bhRdm);
        assertThat(dataMonitor.getCurrentState()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testLongThresholds(ComparisonType comparisonType, String threshold, long value, State expected)
    {
        final CustomDataMonitor cdm = makeCDM(bjMo0, bjRds.getMessageType(), "longVal", comparisonType, threshold, State.Critical, FieldType.LONG);
        final IDataMonitor dataMonitor = cdm.getDataMonitor();

        bjRdm.setLong("longVal", value);
        dataMonitor.analyze(bjRdm);
        assertThat(dataMonitor.getCurrentState()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testDoubleThresholds(ComparisonType comparisonType, String threshold, double value, State expected)
    {
        final CustomDataMonitor cdm = makeCDM(bhMo0, bhRds.getMessageType(), "CURRENT", comparisonType, threshold, State.Critical, FieldType.DOUBLE);
        final IDataMonitor dataMonitor = cdm.getDataMonitor();

        bhRdm.setDouble("CURRENT", value);
        dataMonitor.analyze(bhRdm);
        assertThat(dataMonitor.getCurrentState()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testFloatThresholds(ComparisonType comparisonType, String threshold, float value, State expected)
    {
        final CustomDataMonitor cdm = makeCDM(bjMo0, bjRds.getMessageType(), "battery", comparisonType, threshold, State.Critical, FieldType.FLOAT);
        final IDataMonitor dataMonitor = cdm.getDataMonitor();

        bjRdm.setFloat("battery", value);
        dataMonitor.analyze(bjRdm);
        assertThat(dataMonitor.getCurrentState()).isEqualTo(expected);
    }

    public Iterable<Object[]> makeBoolTestValues()
    {
        return Arrays.asList(
                new Object[]{ComparisonType.EQ, "true", true, State.Critical},
                new Object[]{ComparisonType.EQ, "true", false, State.Active},
                new Object[]{ComparisonType.NEQ, "true", true, State.Active},
                new Object[]{ComparisonType.NEQ, "true", false, State.Critical},
                new Object[]{ComparisonType.LT, "true", true, State.Active},
                new Object[]{ComparisonType.LT, "true", false, State.Critical},
                new Object[]{ComparisonType.LT, "false", true, State.Active},
                new Object[]{ComparisonType.LT, "false", false, State.Active},
                new Object[]{ComparisonType.LTE, "true", true, State.Critical},
                new Object[]{ComparisonType.LTE, "true", false, State.Critical},
                new Object[]{ComparisonType.LTE, "false", true, State.Active},
                new Object[]{ComparisonType.LTE, "false", false, State.Critical},
                new Object[]{ComparisonType.GT, "true", true, State.Active},
                new Object[]{ComparisonType.GT, "true", false, State.Active},
                new Object[]{ComparisonType.GT, "false", false, State.Active},
                new Object[]{ComparisonType.GT, "false", true, State.Critical},
                new Object[]{ComparisonType.GTE, "true", true, State.Critical},
                new Object[]{ComparisonType.GTE, "true", false, State.Active},
                new Object[]{ComparisonType.GTE, "false", true, State.Critical},
                new Object[]{ComparisonType.GTE, "false", false, State.Critical});
    }

    @ParameterizedTest
    @MethodSource("makeBoolTestValues")
    public void testBoolThresholds(ComparisonType comparisonType, String threshold, boolean value, State expected)
    {
        final CustomDataMonitor cdm = makeCDM(bhMo0, bhRds.getMessageType(), "visible", comparisonType, threshold, State.Critical, FieldType.BOOLEAN);
        final IDataMonitor dataMonitor = cdm.getDataMonitor();

        bhRdm.setBoolean("visible", value);
        dataMonitor.analyze(bhRdm);
        assertThat(dataMonitor.getCurrentState()).isEqualTo(expected);
    }

    public Iterable<Object[]> makeStringTestValues()
    {
        return Arrays.asList(
                new Object[]{ComparisonType.EQ, "mykey", "mykey", State.Critical},
                new Object[]{ComparisonType.EQ, "mykey", "MYKEY", State.Critical},
                new Object[]{ComparisonType.EQ, "mykey", "xx", State.Active},
                new Object[]{ComparisonType.NEQ, "mykey", "mykey", State.Active},
                new Object[]{ComparisonType.NEQ, "mykey", "MYKEY", State.Active},
                new Object[]{ComparisonType.NEQ, "mykey", "xx", State.Critical});
    }

    @ParameterizedTest
    @MethodSource("makeStringTestValues")
    public void testStringThresholds(ComparisonType comparisonType, String threshold, String value, State expected)
    {
        final CustomDataMonitor cdm = makeCDM(bhMo0, bhRds.getMessageType(), "key", comparisonType, threshold, State.Critical, FieldType.STRING);
        final IDataMonitor dataMonitor = cdm.getDataMonitor();

        bhRdm.setString("key", value);
        dataMonitor.analyze(bhRdm);
        assertThat(dataMonitor.getCurrentState()).isEqualTo(expected);
    }

    public Iterable<Object[]> makeCoreTestValues()
    {
        bhRdm.setLocation(20, 30, 40);
        bhRdm.setSpeedInMps(100);
        bhRdm.setOrientation(50, 60, 70);
        bhRdm.setHasMotion(true);

        return Arrays.asList(
                new Object[]{CoreField.latitude.name(), FieldType.DOUBLE, ComparisonType.EQ, "20", State.Critical},
                new Object[]{CoreField.latitude.name(), FieldType.DOUBLE, ComparisonType.EQ, "21", State.Active},
                new Object[]{CoreField.longitude.name(), FieldType.DOUBLE, ComparisonType.EQ, "30", State.Critical},
                new Object[]{CoreField.longitude.name(), FieldType.DOUBLE, ComparisonType.EQ, "31", State.Active},
                new Object[]{CoreField.elevation.name(), FieldType.DOUBLE, ComparisonType.EQ, "40", State.Critical},
                new Object[]{CoreField.elevation.name(), FieldType.DOUBLE, ComparisonType.EQ, "41", State.Active},
                new Object[]{CoreField.speed.name(), FieldType.DOUBLE, ComparisonType.EQ, "100", State.Critical},
                new Object[]{CoreField.speed.name(), FieldType.DOUBLE, ComparisonType.EQ, "101", State.Active},
                new Object[]{CoreField.heading.name(), FieldType.DOUBLE, ComparisonType.EQ, "50", State.Critical},
                new Object[]{CoreField.heading.name(), FieldType.DOUBLE, ComparisonType.EQ, "51", State.Active},
                new Object[]{CoreField.pitch.name(), FieldType.DOUBLE, ComparisonType.EQ, "60", State.Critical},
                new Object[]{CoreField.pitch.name(), FieldType.DOUBLE, ComparisonType.EQ, "61", State.Active},
                new Object[]{CoreField.roll.name(), FieldType.DOUBLE, ComparisonType.EQ, "70", State.Critical},
                new Object[]{CoreField.roll.name(), FieldType.DOUBLE, ComparisonType.EQ, "71", State.Active},
                new Object[]{CoreField.hasMotion.name(), FieldType.BOOLEAN, ComparisonType.EQ, "true", State.Critical},
                new Object[]{CoreField.hasMotion.name(), FieldType.BOOLEAN, ComparisonType.EQ, "false", State.Active});
    }

    @ParameterizedTest
    @MethodSource("makeCoreTestValues")
    public void testCoreFields(String fieldName, FieldType fieldType, ComparisonType comparisonType, String threshold, State expected)
    {
        final CustomDataMonitor cdm = makeCDM(bhMo0, bhRds.getMessageType(), fieldName, comparisonType, threshold, State.Critical, fieldType);
        final IDataMonitor dataMonitor = cdm.getDataMonitor();

        dataMonitor.analyze(bhRdm);
        assertThat(dataMonitor.getCurrentState()).isEqualTo(expected);
    }

    /**
     * Verify that a CDM comes out of persistence equal to its original form.
     */
    @Test
    public void testPersistence()
    {
        final ICustomDataMonitorPersist persist = getBean(ICustomDataMonitorPersist.class);
        final CustomDataMonitor cdm = makeCDM(bhMo0, bhRds.getMessageType(), "key", ComparisonType.NEQ, "foo", State.Critical, FieldType.STRING);
        final IDataMonitor dm = cdm.getDataMonitor();

        persist.persistDataMonitor(cdm);

        final List<CustomDataMonitor> results = persist.query(cdm.getKey());
        assertThat(results).hasSize(1);

        final CustomDataMonitor cdm2 = results.get(0);

        assertThat(cdm2.getKey()).isEqualTo(cdm.getKey());
        assertThat(cdm2.getDisplayName()).isEqualTo(cdm.getDisplayName());
        assertThat(cdm2.getStructureName()).isEqualTo(cdm.getStructureName());
        assertThat(cdm2.getFieldName()).isEqualTo(cdm.getFieldName());
        assertThat(cdm2.getFieldType()).isEqualTo(cdm.getFieldType());
        assertThat(cdm2.getComparisonType()).isEqualTo(cdm.getComparisonType());
        assertThat(cdm2.getThreshold()).isEqualTo(cdm.getThreshold());
        assertThat(cdm2.getAlarmState()).isEqualTo(cdm.getAlarmState());
        assertThat(cdm2.getDefaultState()).isEqualTo(cdm.getDefaultState());
        assertThat(cdm2.isAckReq()).isEqualTo(cdm.isAckReq());
        assertThat(cdm2.isClearable()).isEqualTo(cdm.isClearable());
        assertThat(cdm2.isHoldState()).isEqualTo(cdm.isHoldState());
        assertThat(cdm2.isShowValue()).isEqualTo(cdm.isShowValue());

        // Now dive in and validate the underlying DataMonitor

        final IDataMonitor dm2 = cdm2.getDataMonitor();
        assertThat(dm2).isNotNull();

        dm2.setDelegate(delegate);

        assertThat(dm2.getFieldName()).isEqualTo(dm.getFieldName());
        assertThat(dm2.getFamily()).isEqualTo(dm.getFamily());
        assertThat(dm2.getDisplayName()).isEqualTo(dm.getDisplayName());
        assertThat(dm2.getValue()).isEqualTo(dm.getValue());
        assertThat(dm2.getCurrentState()).isEqualTo(dm.getCurrentState());
        assertThat(dm2.getUnclearedAlarmState()).isEqualTo(dm.getUnclearedAlarmState());
        assertThat(dm2.getCustomIcon()).isEqualTo(dm.getCustomIcon());
        assertThat(dm2.getShowValueOnDisplay()).isEqualTo(dm.getShowValueOnDisplay());
        assertThat(dm2.isTrackHighestAlarmState()).isEqualTo(dm.isTrackHighestAlarmState());
        assertThat(dm2.isClearable()).isEqualTo(dm.isClearable());

        // And test that they work equivalently
        bhRdm.setString("key", "bar");
        dm.analyze(bhRdm);
        dm2.analyze(bhRdm);

        assertThat(dm2.getCurrentState()).isEqualTo(State.Critical);
        assertThat(dm2.getValue()).isEqualTo(dm.getValue());
        assertThat(dm2.getCurrentState()).isEqualTo(dm.getCurrentState());
        assertThat(dm2.getUnclearedAlarmState()).isEqualTo(dm.getUnclearedAlarmState());
    }

    /**
     * Construct a custom data monitor.
     */
    private CustomDataMonitor makeCDM(IMapObject mapObject, String structure, String fieldName,
                                      ComparisonType comparisonType, String threshold,
                                      State alarmState, FieldType fieldType)
    {
        final CustomDataMonitor cdm = new CustomDataMonitor(mapObject,
                "displayName", structure, fieldName, comparisonType, threshold, alarmState, fieldType,
                State.Active, true, false, true, false);
        cdm.getDataMonitor().setDelegate(delegate);

        return cdm;
    }
}
