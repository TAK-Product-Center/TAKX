package gov.takx.core.persist;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IRDMPersist;
import gov.takx.dataservices.persist.IconMapObject;
import gov.takx.platform.utils.UnitsOfMeasureUtils;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import gov.takx.test.datasim.ADeviceSim;
import gov.takx.test.datasim.BloodHoundSim;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;

import static gov.takx.api.messages.IDataType.BOOLEAN;
import static gov.takx.api.messages.IDataType.BOOLEAN_ARRAY;
import static gov.takx.api.messages.IDataType.BYTE_ARRAY;
import static gov.takx.api.messages.IDataType.DATE;
import static gov.takx.api.messages.IDataType.DATE_ARRAY;
import static gov.takx.api.messages.IDataType.DOUBLE;
import static gov.takx.api.messages.IDataType.DOUBLE_ARRAY;
import static gov.takx.api.messages.IDataType.FLOAT;
import static gov.takx.api.messages.IDataType.FLOAT_ARRAY;
import static gov.takx.api.messages.IDataType.INT;
import static gov.takx.api.messages.IDataType.INT_ARRAY;
import static gov.takx.api.messages.IDataType.LONG;
import static gov.takx.api.messages.IDataType.LONG_ARRAY;
import static gov.takx.api.messages.IDataType.SHORT;
import static gov.takx.api.messages.IDataType.SHORT_ARRAY;
import static gov.takx.api.messages.IDataType.STRING;
import static gov.takx.api.messages.IDataType.STRING_ARRAY;
import static gov.takx.test.PersistTestSetup.FIXED_TIME_1;
import static gov.takx.test.PersistTestSetup.FIXED_TIME_2;
import static gov.takx.test.PersistTestSetup.FIXED_TIME_3;
import static gov.takx.test.PersistTestSetup.FIXED_TIME_4;
import static gov.takx.test.PersistTestSetup.OFFSET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

/**
 * Test cases for the RDMPersist service
 *
 */
@TestMethodOrder(DependsOnOrdering.class)
@SuppressWarnings("JavaUtilDate")
public class RDMPersistTest extends AContainerTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String VOLTAGE = "Voltage";
    private static final String PANIC = "Panic";

    private static final String WIDEN_FAMILY = "widen";
    private static final String WIDEN_TYPE = "widen";

    private static final double EPSILON = 1e-8; // Comparison tolerance for speeds

    private PersistTestSetup test;
    private IRDMPersist rdmPersist;
    private long startTime;
    private long finishTime;
    private long minTime;
    private long maxTime;
    private long minReceiveTime;
    private long maxReceiveTime;
    private IMapObject bh1_mapObject;
    private IRaptorDataStructure bh_struct;
    private IRaptorDataStructure widenTestStructure;

    @BeforeAll
    public void startHere()
    {
        // TODO: Test if 'before-all' in super is called first
        if (logger.isDebugEnabled()) logger.debug("{} is starting", getClass().getCanonicalName());
        test = new PersistTestSetup();
        test.setup();

        rdmPersist = getBean(IRDMPersist.class);
        startTime = test.getStartTime();
        finishTime = test.getFinishTime();
        minTime = test.getMinTime();
        maxTime = test.getMaxTime();
        minReceiveTime = test.getMinReceiveTime();
        maxReceiveTime = test.getMaxReceiveTime();

        String moKey = BloodHoundSim.makeUnitIdKey("1");
        bh1_mapObject = test.getMapObject(moKey);
        bh_struct = test.getBloodhoundStruct();

        final DataStructureBuilder builder = new DataStructureBuilder(WIDEN_FAMILY, WIDEN_TYPE, "0.0.0", "TestWidening");

        // Add numeric types for widening tests
        builder.addShort("Short");
        builder.addInt("Int");
        builder.addLong("Long");
        builder.addFloat("Float");
        builder.addDouble("Double");

        widenTestStructure = builder.build();
    }

    @AfterAll
    public void close()
    {
        test.shutdown();
        destroyProject();
    }

    @Test
    public void testNonExistentFieldReturnsNull()
    {
        // Validate that calling getField with a field name that doesn't exist on the structure returns null, no NPE
        // Same for isNull
        final IPrePersistRaptorDataMessage rdm = bh_struct.createRaptorDataMessage(bh1_mapObject);

        assertThat(rdm.getField("IDontExistInThisStructure")).isNull();
        assertThat(rdm.isNull("IDontExistInThisStructure")).isTrue();
    }

    @Test
    public void testNullPrimitive()
    {
        // Ensure that a "null" primitive field returns null when getField is called
        final IPrePersistRaptorDataMessage rdm = bh_struct.createRaptorDataMessage(bh1_mapObject);

        // Should start out null
        assertThat(rdm.isNull(VOLTAGE)).isTrue();
        assertThat(rdm.isNull(PANIC)).isTrue();

        assertThat(rdm.getField(VOLTAGE)).isNull();
        assertThat(rdm.getField(PANIC)).isNull();

        // Set values
        rdm.setDouble(VOLTAGE, 10.0);
        rdm.setBoolean(PANIC, true);

        assertThat(rdm.isNull(VOLTAGE)).isFalse();
        assertThat(rdm.isNull(PANIC)).isFalse();

        assertThat(rdm.getField(VOLTAGE)).isNotNull();
        assertThat(rdm.getDouble(VOLTAGE)).isEqualTo(10.0);
        assertThat(rdm.getField(PANIC)).isNotNull();
        assertThat(rdm.getBoolean(PANIC)).isTrue();

        // Set back to null
        rdm.removeField(VOLTAGE);
        rdm.removeField(PANIC);

        assertThat(rdm.isNull(VOLTAGE)).isTrue();
        assertThat(rdm.isNull(PANIC)).isTrue();

        assertThat(rdm.getField(VOLTAGE)).isNull();
        assertThat(rdm.getField(PANIC)).isNull();
    }

    @Test
    public void searchLatLong()
    {
        Sector region = getTestRegion();

        List<IRaptorDataMessage> rs = rdmPersist.searchLatLong(null, region);
        assertThat(rs).hasSize(18);

        rs = rdmPersist.searchLatLong(bh1_mapObject, region);
        assertThat(rs).hasSize(3);

        rs = rdmPersist.searchLatLong(bh1_mapObject, region, 2);
        assertThat(rs).hasSize(2);

        rs = rdmPersist.searchLatLong(bh1_mapObject, region, 2);
        assertThat(rs).hasSize(2);
    }

    @Test
    public void findMostRecentWithLocation()
    {
        IRaptorDataMessage rdm = rdmPersist.findMostRecentWithLocation(bh1_mapObject);
        assertThat(rdm).isNotNull();
    }

    @Test
    public void hasUUID()
    {
        boolean hasUUID = rdmPersist.hasUUID(test.getUUID());
        assertThat(hasUUID).isTrue();
    }

    @Test
    public void searchUUID()
    {
        IRaptorDataMessage rdm = rdmPersist.searchUUID(test.getUUID());
        assertThat(rdm).isNotNull();
    }

    @Test
    public void searchById()
    {
        IRaptorDataMessage rdm = rdmPersist.searchById(1);
        assertThat(rdm).isNotNull();
    }

    @Test
    public void searchReceiveDate()
    {
        List<IRaptorDataMessage> rs = rdmPersist.searchReceiveDate(null, FIXED_TIME_2, FIXED_TIME_3);
        assertThat(rs).hasSize(11);

        rs = rdmPersist.searchReceiveDate(null, startTime, finishTime);
        assertThat(rs).hasSize(11);

        rs = rdmPersist.searchReceiveDate(bh1_mapObject, FIXED_TIME_1, FIXED_TIME_2);
        assertThat(rs).hasSize(10);

        rs = rdmPersist.searchReceiveDate(null, FIXED_TIME_2, FIXED_TIME_3, 10, null);
        assertThat(rs).hasSize(10);
    }

    @Test
    public void searchDeviceDate()
    {
        List<IRaptorDataMessage> rs = rdmPersist.searchDeviceDate(null, FIXED_TIME_2, FIXED_TIME_3);
        assertThat(rs).hasSize(11);

        rs = rdmPersist.searchDeviceDate(null, startTime, finishTime);
        assertThat(rs).hasSize(11);

        rs = rdmPersist.searchDeviceDate(bh1_mapObject, FIXED_TIME_1, FIXED_TIME_2);
        assertThat(rs).hasSize(9);
    }

    @Test
    public void searchLatLongDate()
    {
        Sector region = getTestRegion();

        List<IRaptorDataMessage> rs = rdmPersist.searchLatLongDate(null, region, FIXED_TIME_2, FIXED_TIME_3);
        assertThat(rs).hasSize(3);

        rs = rdmPersist.searchLatLongDate(bh1_mapObject, region, FIXED_TIME_1, FIXED_TIME_2);
        assertThat(rs).hasSize(3);

        rs = rdmPersist.searchLatLongDate(bh1_mapObject, region, FIXED_TIME_1, FIXED_TIME_2, 2);
        assertThat(rs).hasSize(2);

        rs = rdmPersist.searchLatLongDate(bh1_mapObject, region, FIXED_TIME_1, FIXED_TIME_2, 2);
        assertThat(rs).hasSize(2);
    }

    @Test
    public void searchLatLongTimeThreshold()
    {
        Sector region = getTestRegion();

        List<IRaptorDataMessage> rs = rdmPersist.searchLatLongTimeThreshold(bh1_mapObject, region, FIXED_TIME_1, 2, IRDMPersist.SortOrder.ASCENDING);
        assertThat(rs).hasSize(2);

        rs = rdmPersist.searchLatLongTimeThreshold(bh1_mapObject, region, FIXED_TIME_1, null, IRDMPersist.SortOrder.NONE);
        assertThat(rs).hasSize(3);

        rs = rdmPersist.searchLatLongTimeThreshold(bh1_mapObject, region, FIXED_TIME_1, 2, IRDMPersist.SortOrder.ASCENDING);
        assertThat(rs).hasSize(2);
    }

    @Test
    public void searchMapObject()
    {
        List<IRaptorDataMessage> rs = rdmPersist.searchMapObject(bh1_mapObject);
        assertThat(rs).hasSize(10);

        rs = rdmPersist.searchMapObject(bh1_mapObject, 2);
        assertThat(rs).hasSize(2);
    }

    @Test
    public void countDate()
    {
        long rs = rdmPersist.countDate(null, FIXED_TIME_3, FIXED_TIME_4);
        assertThat(rs).isEqualTo(11);

        rs = rdmPersist.countDate(bh1_mapObject, FIXED_TIME_1, FIXED_TIME_4);
        assertThat(rs).isEqualTo(10);
    }

    @Test
    public void countLatLong()
    {
        Sector region = getTestRegion();

        long rs = rdmPersist.countLatLong(null, region);
        assertThat(rs).isEqualTo(18);

        rs = rdmPersist.countLatLong(bh1_mapObject, region);
        assertThat(rs).isEqualTo(3);
    }

    @Test
    public void countLatLongDate()
    {
        Sector region = getTestRegion();

        long rs = rdmPersist.countLatLongDate(null, region, FIXED_TIME_3, FIXED_TIME_4);
        assertThat(rs).isEqualTo(3);

        rs = rdmPersist.countLatLongDate(bh1_mapObject, region, FIXED_TIME_1, FIXED_TIME_4);
        assertThat(rs).isEqualTo(3);
    }

    @Test
    public void countLatLongTimeThreshold()
    {
        Sector region = getTestRegion();

        long rs = rdmPersist.countLatLongTimeThreshold(bh1_mapObject, region, FIXED_TIME_1);
        assertThat(rs).isEqualTo(3);
    }

    @Test
    public void queryReceiveDateRange()
    {
        long[] arr = rdmPersist.queryReceiveDateRange();
        assertThat(arr[0]).isEqualTo(minReceiveTime);
        assertThat(arr[1]).isEqualTo(maxReceiveTime);
    }

    @Test
    public void queryDeviceDateRange()
    {
        long[] arr = rdmPersist.queryDeviceDateRange();
        assertThat(arr[0]).withFailMessage("minTime is wrong").isEqualTo(minTime);
        assertThat(arr[1]).withFailMessage("maxTime is wrong").isEqualTo(maxTime);
    }

    @Test
    public void immutable()
    {
        IPrePersistRaptorDataMessage rdm = (IPrePersistRaptorDataMessage) rdmPersist.searchById(1);
        String origSourceName = rdm.getSourceName();
        rdm.setSourceName("something different");
        rdmPersist.persistRDM(rdm);

        rdm = (IPrePersistRaptorDataMessage) rdmPersist.searchById(1);
        assertThat(rdm.getSourceName()).isEqualTo(origSourceName);
    }

    /**
     * Tests the getField method to make sure it is returning the correct Object type as the DataType defined.
     */
    @Test
    public void getFieldsTest()
    {
        List<IRaptorDataMessage> rdms = rdmPersist.searchMapObject(bh1_mapObject);
        for (IRaptorDataMessage rdm : rdms)
        {
            testRdmFields(rdm);
        }
    }

    /**
     * Test all the fields in the given RDM to ensure they are of the proper type.
     *
     * @param rdm RDM to test
     */
    private void testRdmFields(IRaptorDataMessage rdm)
    {
        for (String fieldName : rdm.getFieldNames())
        {
            int fieldType = rdm.getStructure().getField(fieldName).getFieldType();
            Object fieldValue = rdm.getField(fieldName);

            if (fieldType == STRING)
            {
                assertThat(fieldValue).isInstanceOf(String.class);
            } else if (fieldType == STRING_ARRAY)
            {
                assertThat(fieldValue).isInstanceOf(String[].class);
            } else if (fieldType == BOOLEAN)
            {
                assertThat(fieldValue).isInstanceOf(Boolean.class);
            } else if (fieldType == BOOLEAN_ARRAY)
            {
                assertThat(fieldValue).isInstanceOf(boolean[].class);
            } else if (fieldType == INT)
            {
                assertThat(fieldValue).isInstanceOf(Integer.class);
            } else if (fieldType == INT_ARRAY)
            {
                assertThat(fieldValue).isInstanceOf(int[].class);
            } else if (fieldType == FLOAT)
            {
                assertThat(fieldValue).isInstanceOf(Float.class);
            } else if (fieldType == FLOAT_ARRAY)
            {
                assertThat(fieldValue).isInstanceOf(float[].class);
            } else if (fieldType == SHORT)
            {
                assertThat(fieldValue).isInstanceOf(Short.class);
            } else if (fieldType == SHORT_ARRAY)
            {
                assertThat(fieldValue).isInstanceOf(short[].class);
            } else if (fieldType == LONG)
            {
                assertThat(fieldValue).isInstanceOf(Long.class);
            } else if (fieldType == LONG_ARRAY)
            {
                assertThat(fieldValue).isInstanceOf(long[].class);
            } else if (fieldType == DOUBLE)
            {
                assertThat(fieldValue).isInstanceOf(Double.class);
            } else if (fieldType == DOUBLE_ARRAY)
            {
                assertThat(fieldValue).isInstanceOf(double[].class);
            } else if (fieldType == DATE)
            {
                assertThat(fieldValue).isInstanceOf(Date.class);
            } else if (fieldType == DATE_ARRAY)
            {
                assertThat(fieldValue).isInstanceOf(Date[].class);
            } else if (fieldType == BYTE_ARRAY)
            {
                assertThat(fieldValue).isInstanceOf(byte[].class);
            }
        }
    }

    /**
     * Test that narrowing conversions (as defined by the JLS) via primitive getters throw a ClassCastException.
     */
    @Test
    public void testPrimitiveNarrowingFails()
    {
        final IPrePersistRaptorDataMessage rdm = createWideningTestRdm();

        expectNarrowingException(() -> rdm.getShort("Int"));
        expectNarrowingException(() -> rdm.getShort("Long"));
        expectNarrowingException(() -> rdm.getShort("Float"));
        expectNarrowingException(() -> rdm.getShort("Double"));

        expectNarrowingException(() -> rdm.getInt("Long"));
        expectNarrowingException(() -> rdm.getInt("Float"));
        expectNarrowingException(() -> rdm.getInt("Double"));

        expectNarrowingException(() -> rdm.getLong("Float"));
        expectNarrowingException(() -> rdm.getLong("Double"));

        expectNarrowingException(() -> rdm.getFloat("Double"));
    }

    /**
     * Perform an operation that is expected to throw a ClassCastException, fail if it doesn't.
     */
    private void expectNarrowingException(Runnable operation)
    {
        assertThatExceptionOfType(ClassCastException.class).isThrownBy(operation::run);
    }

    /**
     * Test that widening conversions (as defined by the JLS) via primitive getters work properly.
     */
    @Test
    public void testPrimitiveWidening()
    {
        final IPrePersistRaptorDataMessage rdm = createWideningTestRdm();

        assertThat(rdm.getShort("Short")).isEqualTo((short) 1234);
        assertThat(rdm.getInt("Short")).isEqualTo(1234);
        assertThat(rdm.getLong("Short")).isEqualTo(1234L);
        assertThat(rdm.getFloat("Short")).isEqualTo(1234.0f);
        assertThat(rdm.getDouble("Short")).isEqualTo(1234.0d);

        assertThat(rdm.getInt("Int")).isEqualTo(1234);
        assertThat(rdm.getLong("Int")).isEqualTo(1234L);
        assertThat(rdm.getFloat("Int")).isEqualTo(1234.0f);
        assertThat(rdm.getDouble("Int")).isEqualTo(1234.0d);

        assertThat(rdm.getLong("Long")).isEqualTo(1234L);
        assertThat(rdm.getFloat("Long")).isEqualTo(1234.0f);
        assertThat(rdm.getDouble("Long")).isEqualTo(1234.0d);

        assertThat(rdm.getFloat("Float")).isEqualTo(1234.0f);
        assertThat(rdm.getDouble("Float")).isEqualTo(1234.0d);

        assertThat(rdm.getDouble("Double")).isEqualTo(1234.0d);
    }

    /**
     * @return An RDM for use in testing primitive conversions (widening and narrowing).
     */
    private IPrePersistRaptorDataMessage createWideningTestRdm()
    {
        final IPrePersistRaptorDataMessage rdm = widenTestStructure.createRaptorDataMessage(WIDEN_FAMILY, WIDEN_TYPE, "unitID");

        rdm.setShort("Short", (short) 1234);
        rdm.setInt("Int", 1234);
        rdm.setLong("Long", 1234);
        rdm.setFloat("Float", 1234);
        rdm.setDouble("Double", 1234);
        return rdm;
    }

    /**
     * Create a structure with every known field type, make an RDM from that, fill it with data, persist it, pull
     * it back out, and check all the fields.
     */
    @Test
    public void testAllFieldTypesInPersistence()
    {
        final String family = "AllFields";
        final String type = "test";
        final String unitID = "unitID";

        final DataStructureBuilder builder = new DataStructureBuilder(family, type, "0.0.0", "allFieldsType");

        // Add in all scalar types
        builder.addBoolean("Boolean");
        builder.addShort("Short");
        builder.addInt("Int");
        builder.addLong("Long");
        builder.addFloat("Float");
        builder.addDouble("Double");
        builder.addString("String");
        builder.addDate("Date");

        // Add in all array field types
        builder.addByteArray("ByteArray");
        builder.addBooleanArray("BooleanArray");
        builder.addStringArray("StringArray");
        builder.addShortArray("ShortArray");
        builder.addIntArray("IntArray");
        builder.addLongArray("LongArray");
        builder.addDoubleArray("DoubleArray");
        builder.addFloatArray("FloatArray");
        builder.addDateArray("DateArray");

        final IRaptorDataStructure rds = builder.build();

        // Construct a MapEntity and MO to keep this RDM out of the other tests query results
        final IMapObjectPersist mapObjectPersist = getBean(IMapObjectPersist.class);

        IMapEntity me = mapObjectPersist.getOrCreateMapEntity(family, type);
        me.addStructure(rds.getStructure());
        assertThat(me.getId()).withFailMessage("ME ID must not be 0").isGreaterThan(0);

        final MapObjectSpec spec = MapObjectSpec.builder(me, unitID).build();
        final IIconMapObject mapObject = new IconMapObject(spec, null);
        mapObjectPersist.persistMapObject(mapObject);

        // Now construct an RDM and fill it with data
        IPrePersistRaptorDataMessage rdm = rds.createRaptorDataMessage(family, type, unitID);

        rdm.setBoolean("Boolean", true);
        rdm.setShort("Short", (short) 1234);
        rdm.setInt("Int", 1234);
        rdm.setLong("Long", 1234);
        rdm.setFloat("Float", 1234);
        rdm.setDouble("Double", 1234);
        rdm.setString("String", "1234");
        Date theDate = new Date(System.currentTimeMillis());
        rdm.setDate("Date", theDate);

        // Add in array field types
        byte[] bytes = {1, 2, 3, 4};
        rdm.setByteArray("ByteArray", bytes);

        boolean[] booleans = {true, false, true, false};
        rdm.setBooleanArray("BooleanArray", booleans);

        String[] strings = {"One", "Two", "Three", "Four"};
        rdm.setStringArray("StringArray", strings);

        short[] shorts = {1, 2, 3, 4};
        rdm.setShortArray("ShortArray", shorts);

        int[] ints = {1, 2, 3, 4};
        rdm.setIntArray("IntArray", ints);

        long[] longs = {1, 2, 3, 4};
        rdm.setLongArray("LongArray", longs);

        float[] floats = {1, 2, 3, 4};
        rdm.setFloatArray("FloatArray", floats);

        double[] doubles = {1, 2, 3, 4};
        rdm.setDoubleArray("DoubleArray", doubles);

        Date[] dates = {new Date(1), new Date(2), new Date(3), new Date(4)};
        rdm.setDateArray("DateArray", dates);

        rdm.setMapObject(mapObject);

        rdm.setReceiveTime(minReceiveTime); // So queryReceiveDateRange won't fail

        rdmPersist.persistRDM(rdm);

        int rdmId = rdm.getId();
        assertThat(rdmId).withFailMessage("RDM ID must not be 0").isGreaterThan(0);

        testRdmFields(rdm); // Test the source RDM

        // Now validate that all the fields have the proper value

        assertThat(rdm.getBoolean("Boolean")).isTrue();
        assertThat(rdm.getShort("Short")).isEqualTo((short) 1234);
        assertThat(rdm.getInt("Int")).isEqualTo(1234);
        assertThat(rdm.getLong("Long")).isEqualTo(1234L);
        assertThat(rdm.getFloat("Float")).isEqualTo(1234.0f);
        assertThat(rdm.getDouble("Double")).isEqualTo(1234.0);
        assertThat(rdm.getString("String")).isEqualTo("1234");
        assertThat(rdm.getDate("Date")).isEqualTo(theDate);

        assertThat(rdm.getBooleanArray("BooleanArray")).isEqualTo(booleans);
        assertThat(rdm.getByteArray("ByteArray")).isEqualTo(bytes);

        assertThat(rdm.getStringArray("StringArray")).isEqualTo(strings);
        assertThat(rdm.getShortArray("ShortArray")).isEqualTo(shorts);
        assertThat(rdm.getIntArray("IntArray")).isEqualTo(ints);
        assertThat(rdm.getLongArray("LongArray")).isEqualTo(longs);
        assertThat(rdm.getFloatArray("FloatArray")).isEqualTo(floats);
        assertThat(rdm.getDoubleArray("DoubleArray")).isEqualTo(doubles);
        assertThat(rdm.getDateArray("DateArray")).isEqualTo(dates);

        // Now pull it back out
        IRaptorDataMessage newRDM = rdmPersist.searchById(rdmId);
        testRdmFields(newRDM); // Test the reified RDM

        // And validate that all the fields again have the proper value

        assertThat(newRDM.getBoolean("Boolean")).isTrue();
        assertThat(newRDM.getShort("Short")).isEqualTo((short) 1234);
        assertThat(newRDM.getInt("Int")).isEqualTo(1234);
        assertThat(newRDM.getLong("Long")).isEqualTo(1234L);
        assertThat(newRDM.getFloat("Float")).isEqualTo(1234.0f);
        assertThat(newRDM.getDouble("Double")).isEqualTo(1234.0);
        assertThat(newRDM.getString("String")).isEqualTo("1234");
        assertThat(newRDM.getDate("Date")).isEqualTo(theDate);

        assertThat(newRDM.getStringArray("StringArray")).isEqualTo(strings);
        assertThat(newRDM.getShortArray("ShortArray")).isEqualTo(shorts);
        assertThat(newRDM.getIntArray("IntArray")).isEqualTo(ints);
        assertThat(newRDM.getLongArray("LongArray")).isEqualTo(longs);
        assertThat(newRDM.getFloatArray("FloatArray")).isEqualTo(floats);
        assertThat(newRDM.getDoubleArray("DoubleArray")).isEqualTo(doubles);
        assertThat(newRDM.getDateArray("DateArray")).isEqualTo(dates);

        // Ensure that nulls will work as well
        // Now construct an RDM and fill it with data
        rdm = rds.createRaptorDataMessage(family, type, unitID);

        rdm.setField("Boolean", null);
        rdm.setField("Short", null);
        rdm.setField("Int", null);
        rdm.setField("Long", null);
        rdm.setField("Float", null);
        rdm.setField("Double", null);
        rdm.setField("String", null);
        rdm.setField("Date", null);

        // Add in array field types
        rdm.setByteArray("ByteArray", null);
        rdm.setBooleanArray("BooleanArray", null);
        rdm.setStringArray("StringArray", null);
        rdm.setShortArray("ShortArray", null);
        rdm.setIntArray("IntArray", null);
        rdm.setLongArray("LongArray", null);
        rdm.setFloatArray("FloatArray", null);
        rdm.setDoubleArray("DoubleArray", null);
        rdm.setDateArray("DateArray", null);

        rdm.setMapObject(mapObject);

        rdm.setReceiveTime(minReceiveTime); // So queryReceiveDateRange won't fail

        rdmPersist.persistRDM(rdm);

        rdmId = rdm.getId();
        assertThat(rdmId).withFailMessage("RDM ID must not be 0").isGreaterThan(0);

        // Now validate that all the fields have the proper value

        assertThat(rdm.isNull("Boolean")).isTrue();
        assertThat(rdm.isNull("Short")).isTrue();
        assertThat(rdm.isNull("Int")).isTrue();
        assertThat(rdm.isNull("Long")).isTrue();
        assertThat(rdm.isNull("Float")).isTrue();
        assertThat(rdm.isNull("Double")).isTrue();
        assertThat(rdm.isNull("String")).isTrue();
        assertThat(rdm.isNull("Date")).isTrue();

        assertThat(rdm.isNull("BooleanArray")).isTrue();
        assertThat(rdm.isNull("ByteArray")).isTrue();
        assertThat(rdm.isNull("StringArray")).isTrue();
        assertThat(rdm.isNull("ShortArray")).isTrue();
        assertThat(rdm.isNull("IntArray")).isTrue();
        assertThat(rdm.isNull("LongArray")).isTrue();
        assertThat(rdm.isNull("FloatArray")).isTrue();
        assertThat(rdm.isNull("DoubleArray")).isTrue();
        assertThat(rdm.isNull("DateArray")).isTrue();

        // Now pull it back out
        newRDM = rdmPersist.searchById(rdmId);

        // And validate that all the fields again have the proper value

        assertThat(newRDM.isNull("Boolean")).isTrue();
        assertThat(newRDM.isNull("Short")).isTrue();
        assertThat(newRDM.isNull("Int")).isTrue();
        assertThat(newRDM.isNull("Long")).isTrue();
        assertThat(newRDM.isNull("Float")).isTrue();
        assertThat(newRDM.isNull("Double")).isTrue();
        assertThat(newRDM.isNull("String")).isTrue();
        assertThat(newRDM.isNull("Date")).isTrue();

        assertThat(newRDM.isNull("BooleanArray")).isTrue();
        assertThat(newRDM.isNull("ByteArray")).isTrue();
        assertThat(newRDM.isNull("StringArray")).isTrue();
        assertThat(newRDM.isNull("ShortArray")).isTrue();
        assertThat(newRDM.isNull("IntArray")).isTrue();
        assertThat(newRDM.isNull("LongArray")).isTrue();
        assertThat(newRDM.isNull("FloatArray")).isTrue();
        assertThat(newRDM.isNull("DoubleArray")).isTrue();
        assertThat(newRDM.isNull("DateArray")).isTrue();
    }

    @Test
    @DependsOn("testAllFieldTypesInPersistence")
    public void testNewObject()
    {
        final String hql = "select new " + CustomClass.class.getName() + "(rdm.id, rdm.latitude, rdm.longitude, rdm.trackStyleId)" +
                " from RaptorDataMessage rdm";

        final List<Object> objects = test.getService().queryFromRdm(hql);
        assertThat(objects).hasSize(62);
    }

    /**
     * Validate the various setSpeed methods to ensure the translations are correct.
     *
     * @since 3.0
     */
    @Test
    public void testSetSpeed()
    {
        final IPrePersistRaptorDataMessage rdm = bh_struct.createRaptorDataMessage(bh1_mapObject);

        assertThat(rdm.getSpeedInMps()).isEqualTo(0d); // Should start at 0

        final int speed = 42;

        rdm.setSpeedInMps(speed);
        assertThat(rdm.getSpeedInMps()).isEqualTo(42d);

        rdm.setSpeedInKph(speed);
        assertThat(rdm.getSpeedInMps()).isCloseTo(UnitsOfMeasureUtils.convertKphToMps(speed), within(EPSILON));

        rdm.setSpeedInKnots(speed);
        double kph = UnitsOfMeasureUtils.convertNauticalMilesToKilometers(speed);
        assertThat(rdm.getSpeedInMps()).isCloseTo(UnitsOfMeasureUtils.convertKphToMps(kph), within(EPSILON));

        rdm.setSpeedInMph(speed);
        kph = UnitsOfMeasureUtils.convertMilesToKilometers(speed);
        assertThat(rdm.getSpeedInMps()).isCloseTo(UnitsOfMeasureUtils.convertKphToMps(kph), within(EPSILON));
    }

    /**
     * @return The geographic bounds for the message searches
     */
    private Sector getTestRegion()
    {
        return Sector.fromDegrees(
                ADeviceSim.STARTING_LATITUDE + OFFSET / 2,
                ADeviceSim.STARTING_LATITUDE + OFFSET / 4,
                ADeviceSim.STARTING_LONGITUDE + OFFSET / 2,
                ADeviceSim.STARTING_LONGITUDE + OFFSET / 4);
    }

    @SuppressWarnings("unused")
    public static class CustomClass
    {
        private final int id;

        // Include a wrapper class to test the constructor matching logic in DatabaseUtils
        public CustomClass(int id, double latitude, double longitude, Integer rdmTsdId)
        {
            this.id = id;
        }

        @Override
        public String toString()
        {
            return "CustomClass[" + id + "]";
        }
    }
}
