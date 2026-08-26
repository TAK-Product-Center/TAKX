package gov.takx.api.mapobject;

import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.plugin.datamonitor.DataMonitorBuilder;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.plugin.datamonitor.IDataMonitorDelegate;
import gov.takx.api.plugin.datamonitor.TestDataMonitorDelegate;
import gov.takx.commons.constants.State;
import gov.takx.commons.utils.Threshold;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class DataMonitorTest extends AContainerTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BH0_KEY = BloodHoundSim.makeUnitIdKey("0");
    private static final String BJ0_KEY = BlueJaySim.makeUnitIdKey("0");

    private PersistTestSetup test;
    private IPrePersistRaptorDataMessage bhRdm;
    private IPrePersistRaptorDataMessage bjRdm;

    private IDataMonitorDelegate delegate;

    @BeforeAll
    public void setup()
    {
        if (logger.isDebugEnabled()) logger.debug("{} is starting", getClass().getCanonicalName());

        test = new PersistTestSetup();
        test.setup();

        IMapObject bhMo0 = test.getMapObject(BH0_KEY);
        BloodHoundSim bhSim = new BloodHoundSim();
        bhRdm = bhSim.makeDataMessage(bhMo0);

        IMapObject bjMo0 = test.getMapObject(BJ0_KEY);
        BlueJaySim bjSim = new BlueJaySim();
        bjRdm = bjSim.makeDataMessage(bjMo0);

        delegate = new TestDataMonitorDelegate();
    }

    @AfterAll
    public void close()
    {
        test.shutdown();
    }

    public static Iterable<Object[]> makeNumericTestValues()
    {
        return Arrays.asList(
                new Object[]{(short) 9, State.Active},
                new Object[]{(short) 10, State.Active},
                new Object[]{(short) 11, State.Warning},
                new Object[]{(short) 20, State.Warning},
                new Object[]{(short) 21, State.Critical},
                new Object[]{(short) 31, State.Inactive});
    }

    @Test
    public void testBooleanThresholds()
    {
        final IDataMonitor dm = createDataMonitor("visible", FieldType.BOOLEAN, true, false,
                new Threshold(true, State.Critical, IDataMonitor.INCLUSIVE));

        bhRdm.setBoolean("visible", true);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Critical);

        // Seems wrong, but you have to clear the alarm before it will come back down
        dm.onDataMonitorEvent(new DataMonitorEvent(this, DataMonitorEventType.AlarmClear));

        bhRdm.setBoolean("visible", false);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Inactive);
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testShortThresholds(short value, State expected)
    {
        final IDataMonitor dm = createDataMonitor("shortVal", FieldType.SHORT, true, false,
                new Threshold((short) 10, State.Active, IDataMonitor.INCLUSIVE),
                new Threshold((short) 20, State.Warning, IDataMonitor.INCLUSIVE),
                new Threshold((short) 30, State.Critical, IDataMonitor.INCLUSIVE));

        bjRdm.setField("shortVal", value);
        dm.analyze(bjRdm);
        assertThat(dm.getCurrentState()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testIntThresholds(int value, State expected)
    {
        final IDataMonitor dm = createDataMonitor("Height", FieldType.INTEGER, true, false,
                new Threshold(10, State.Active, IDataMonitor.INCLUSIVE),
                new Threshold(20, State.Warning, IDataMonitor.INCLUSIVE),
                new Threshold(30, State.Critical, IDataMonitor.INCLUSIVE));

        bhRdm.setField("Height", value);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testLongThresholds(long value, State expected)
    {
        final IDataMonitor dm = createDataMonitor("longVal", FieldType.LONG, true, false,
                new Threshold(10L, State.Active, IDataMonitor.INCLUSIVE),
                new Threshold(20L, State.Warning, IDataMonitor.INCLUSIVE),
                new Threshold(30L, State.Critical, IDataMonitor.INCLUSIVE));

        bjRdm.setField("longVal", value);
        dm.analyze(bjRdm);
        assertThat(dm.getCurrentState()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testFloatThresholds(float value, State expected)
    {
        final IDataMonitor dm = createDataMonitor("batt", FieldType.FLOAT, true, false,
                new Threshold(10f, State.Active, IDataMonitor.INCLUSIVE),
                new Threshold(20f, State.Warning, IDataMonitor.INCLUSIVE),
                new Threshold(30f, State.Critical, IDataMonitor.INCLUSIVE));

        bhRdm.setField("batt", value);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("makeNumericTestValues")
    public void testDoubleThresholds(double value, State expected)
    {
        final IDataMonitor dm = createDataMonitor("Voltage", FieldType.DOUBLE, true, false,
                new Threshold(10.0, State.Active, IDataMonitor.INCLUSIVE),
                new Threshold(20.0, State.Warning, IDataMonitor.INCLUSIVE),
                new Threshold(30.0, State.Critical, IDataMonitor.INCLUSIVE));

        bhRdm.setField("Voltage", value);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(expected);
    }

    @Test
    public void testEvents()
    {
        final IDataMonitor dm = createDataMonitor("Voltage", FieldType.DOUBLE, true, true,
                new Threshold(7.0, State.Critical, IDataMonitor.EXCLUSIVE),
                new Threshold(7.5, State.Warning, IDataMonitor.EXCLUSIVE),
                new Threshold(12.3, State.Active, IDataMonitor.EXCLUSIVE));

        assertThat(dm.getCurrentState()).isEqualTo(State.Unknown);

        bhRdm.setDouble("Voltage", 6.0);
        dm.analyze(bhRdm);

        assertThat(dm.getCurrentState()).isEqualTo(State.Critical);

        bhRdm.setDouble("Voltage", 7.4);
        dm.analyze(bhRdm);

        assertThat(dm.getCurrentState()).isEqualTo(State.Warning);
        assertThat(dm.getUnclearedAlarmState()).isEqualTo(State.Critical);

        dm.onDataMonitorEvent(new DataMonitorEvent(this, DataMonitorEventType.AlarmAcknowledge));
        assertThat(dm.getCurrentState()).isEqualTo(State.Warning);
        assertThat(dm.getUnclearedAlarmState()).isEqualTo(State.Warning);

        bhRdm.setDouble("Voltage", 13.0);
        dm.analyze(bhRdm);

        assertThat(dm.getCurrentState()).isEqualTo(State.Inactive);
        assertThat(dm.getUnclearedAlarmState()).isEqualTo(State.Warning);

        assertThat(dm.isClearable()).isTrue();
        dm.onDataMonitorEvent(new DataMonitorEvent(this, DataMonitorEventType.AlarmClear));
        assertThat(dm.getCurrentState()).isEqualTo(State.Inactive);
        assertThat(dm.getUnclearedAlarmState()).isEqualTo(State.Inactive);

        dm.onDataMonitorEvent(new DataMonitorEvent(this, DataMonitorEventType.Deactivate));
        assertThat(dm.getCurrentState()).isEqualTo(State.Unknown);
        assertThat(dm.getUnclearedAlarmState()).isEqualTo(State.Unknown);
    }

    @Test
    public void testThresholdSorting()
    {
        final Threshold t1 = new Threshold(10.0, State.Active, IDataMonitor.INCLUSIVE);
        final Threshold t2 = new Threshold(20.0, State.Warning, IDataMonitor.INCLUSIVE);
        final Threshold t3 = new Threshold(30.0, State.Serious, IDataMonitor.INCLUSIVE);
        final Threshold t4 = new Threshold(40.0, State.Critical, IDataMonitor.INCLUSIVE);

        IDataMonitor dm = createDataMonitor("Voltage", FieldType.DOUBLE, true, true, t4, t1, t3, t2);

        assertThat(dm.thresholds()).containsExactly(t1, t2, t3, t4);

        bhRdm.setDouble("Voltage", 6.0);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Active);

        bhRdm.setDouble("Voltage", 15.0);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Warning);

        bhRdm.setDouble("Voltage", 25.0);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Serious);

        bhRdm.setDouble("Voltage", 35.0);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Critical);

        bhRdm.setDouble("Voltage", 45.0);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Inactive);

        dm = createDataMonitor("Voltage", FieldType.DOUBLE, true, true, t4, t3, t2, t1);
        assertThat(dm.thresholds()).containsExactly(t1, t2, t3, t4);

        dm = createDataMonitor("Voltage", FieldType.DOUBLE, true, true, t1, t2, t3, t4);
        assertThat(dm.thresholds()).containsExactly(t1, t2, t3, t4);
    }

    @Test
    public void testValueVsState()
    {
        final DataMonitorBuilder builder = createBuilder("Height", FieldType.INTEGER, true, false);
        builder.setValueFieldName("style")
                .addThreshold(10, State.Active, IDataMonitor.INCLUSIVE)
                .addThreshold(20, State.Warning, IDataMonitor.INCLUSIVE)
                .addThreshold(30, State.Critical, IDataMonitor.INCLUSIVE);
        final IDataMonitor dm = builder.build();
        dm.setDelegate(delegate);

        bhRdm.setInt("Height", 20);
        bhRdm.setInt("style", 100);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Warning);
        assertThat(dm.getValue()).isEqualTo(100);

        bhRdm.setInt("Height", 30);
        bhRdm.setInt("style", 200);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Critical);
        assertThat(dm.getValue()).isEqualTo(200);

        bhRdm.setInt("style", 0);
        dm.analyze(bhRdm);
        assertThat(dm.getCurrentState()).isEqualTo(State.Critical);
        assertThat(dm.getValue()).isEqualTo(0);
    }

    @Test
    public void testHierarchy()
    {
        final IDataMonitor parent = createDataMonitor("Height", FieldType.INTEGER, true, false);

        final IDataMonitor dm1 = createBuilder("Height", FieldType.INTEGER, true, false)
                .setParent(parent).build();

        final IDataMonitor dm2 = createBuilder("Height", FieldType.INTEGER, true, false)
                .setParent(parent).build();

        assertThat(parent.getChildren()).containsExactly(dm1, dm2);
    }

    /**
     * Construct a DataMonitor on the given field with the given thresholds.
     */
    private IDataMonitor createDataMonitor(String fieldName, FieldType fieldType, boolean clearable, boolean mustAck, Threshold... thresholds)
    {
        final DataMonitorBuilder builder = createBuilder(fieldName, fieldType, clearable, mustAck);

        for (Threshold threshold : thresholds)
        {
            builder.addThreshold(threshold);
        }

        final IDataMonitor dm = builder.build();

        dm.setDelegate(delegate);

        return dm;
    }

    /**
     * Create a DataMonitorBuilder initialized with the given data.  The caller must later call build() to get
     * the DataMonitor.
     */
    private DataMonitorBuilder createBuilder(String fieldName, FieldType fieldType, boolean clearable, boolean mustAck)
    {
        return new DataMonitorBuilder("family", "type")
                .setDisplayName(fieldName)
                .setDefaultState(State.Inactive)
                .setFieldName(fieldName)
                .setFieldType(fieldType)
                .setClearable(clearable)
                .setTrackHighestAlarmState(mustAck);
    }
}
