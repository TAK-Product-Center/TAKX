package gov.takx.core.persist;

import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.IMapPointPersist;
import gov.takx.api.persist.IRDMPersist;
import gov.takx.api.plugin.util.MessageSerializer;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.dataservices.persist.AExtendedDataStructure;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.io.csv.CsvEmitter;
import gov.takx.platform.io.csv.CsvMapping;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import gov.takx.test.datasim.ADeviceSim;
import gov.takx.test.datasim.BloodHoundSim;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for the MapPointPersist class.
 *
 */
@SuppressWarnings("JavaUtilDate")
public class MapPointPersistTest extends AContainerTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BH0_KEY = KeyUtils.getUniqueKey(BloodHoundSim.BLOODHOUND_FAMILY, BloodHoundSim.BLOODHOUND_TYPE, "0");
    private static final String BH1_KEY = KeyUtils.getUniqueKey(BloodHoundSim.BLOODHOUND_FAMILY, BloodHoundSim.BLOODHOUND_TYPE, "1");
    private static final String BH2_KEY = KeyUtils.getUniqueKey(BloodHoundSim.BLOODHOUND_FAMILY, BloodHoundSim.BLOODHOUND_TYPE, "2");
    private static final String BH3_KEY = KeyUtils.getUniqueKey(BloodHoundSim.BLOODHOUND_FAMILY, BloodHoundSim.BLOODHOUND_TYPE, "3");
    private static final String BH4_KEY = KeyUtils.getUniqueKey(BloodHoundSim.BLOODHOUND_FAMILY, BloodHoundSim.BLOODHOUND_TYPE, "4");

    private PersistTestSetup test;
    private IMapPointPersist mpPersist;
    private IRDMPersist rdmPersist;

    @BeforeAll
    public void startHere()
    {
        if (logger.isDebugEnabled()) logger.debug("{} is starting", getClass().getCanonicalName());
        test = new PersistTestSetup();
        test.setup();
        mpPersist = getBean(IMapPointPersist.class);
        rdmPersist = getBean(IRDMPersist.class);
    }

    @AfterAll
    public void close()
    {
        test.shutdown();
    }

    public Iterable<Object[]> provideBHLocationData()
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
                new Object[]{BH0_KEY, minLoc1, maxLoc1, 10},
                new Object[]{BH0_KEY, minLoc2, maxLoc2, 6},
                new Object[]{BH0_KEY, minLoc3, maxLoc3, 5},
                new Object[]{BH1_KEY, minLoc1, maxLoc1, 10},
                new Object[]{BH1_KEY, minLoc2, maxLoc2, 6},
                new Object[]{BH1_KEY, minLoc3, maxLoc3, 5},
                new Object[]{BH2_KEY, minLoc1, maxLoc1, 10},
                new Object[]{BH2_KEY, minLoc2, maxLoc2, 6},
                new Object[]{BH2_KEY, minLoc3, maxLoc3, 5},
                new Object[]{BH3_KEY, minLoc1, maxLoc1, 10},
                new Object[]{BH3_KEY, minLoc2, maxLoc2, 6},
                new Object[]{BH3_KEY, minLoc3, maxLoc3, 5},
                new Object[]{BH4_KEY, minLoc1, maxLoc1, 10},
                new Object[]{BH4_KEY, minLoc2, maxLoc2, 6},
                new Object[]{BH4_KEY, minLoc3, maxLoc3, 5});
    }

    /**
     * Test Location ranges.
     *
     * @param mapObjectKey Key of map object to query on
     * @param minLoc       min location
     * @param maxLoc       max location
     * @param expected     expected message count
     */
    @ParameterizedTest
    @MethodSource("provideBHLocationData")
    public void testBHLocationRanges(String mapObjectKey, ILocation minLoc, ILocation maxLoc, long expected)
    {
        final IMapObject mapObject = test.getMapObject(mapObjectKey);
        List<MapPoint> rs = mpPersist.searchLatLong(mapObject, minLoc, maxLoc);
        assertThat(rs).hasSize((int) expected);

        // Swapping the coords should have no effect
        rs = mpPersist.searchLatLong(mapObject, minLoc, maxLoc);
        assertThat(rs).hasSize((int) expected);
    }

    @Test
    public void searchLatLong()
    {
        RaptorLocation minLoc = new RaptorLocation(
                ADeviceSim.STARTING_LATITUDE + PersistTestSetup.OFFSET / 2,
                ADeviceSim.STARTING_LONGITUDE + PersistTestSetup.OFFSET / 2);
        RaptorLocation maxLoc = new RaptorLocation(
                ADeviceSim.STARTING_LATITUDE + PersistTestSetup.OFFSET / 4,
                ADeviceSim.STARTING_LONGITUDE + PersistTestSetup.OFFSET / 4);

        List<MapPoint> rs = mpPersist.searchLatLong(test.getMapObject(BH1_KEY), minLoc, maxLoc);
        assertThat(rs).hasSize(3);
    }

    @Test
    public void searchLatLongDate()
    {
        RaptorLocation minLoc = new RaptorLocation(
                ADeviceSim.STARTING_LATITUDE + PersistTestSetup.OFFSET / 2,
                ADeviceSim.STARTING_LONGITUDE + PersistTestSetup.OFFSET / 2);
        RaptorLocation maxLoc = new RaptorLocation(
                ADeviceSim.STARTING_LATITUDE + PersistTestSetup.OFFSET / 4,
                ADeviceSim.STARTING_LONGITUDE + PersistTestSetup.OFFSET / 4);

        List<MapPoint> rs = mpPersist.searchLatLongDate(test.getMapObject(BH1_KEY), minLoc, maxLoc, PersistTestSetup.FIXED_TIME_1, PersistTestSetup.FIXED_TIME_2);
        assertThat(rs).hasSize(3);
    }

    @Test
    public void searchLatLongTime()
    {
        RaptorLocation minLoc = new RaptorLocation(
                ADeviceSim.STARTING_LATITUDE + PersistTestSetup.OFFSET / 2,
                ADeviceSim.STARTING_LONGITUDE + PersistTestSetup.OFFSET / 2);
        RaptorLocation maxLoc = new RaptorLocation(
                ADeviceSim.STARTING_LATITUDE + PersistTestSetup.OFFSET / 4,
                ADeviceSim.STARTING_LONGITUDE + PersistTestSetup.OFFSET / 4);

        List<MapPoint> rs = mpPersist.searchLatLongTimeThreshold(test.getMapObject(BH1_KEY), minLoc, maxLoc, PersistTestSetup.FIXED_TIME_1);
        assertThat(rs).hasSize(3);
    }

    @Test
    public void searchMapObject()
    {
        List<MapPoint> rs = mpPersist.searchMapObject(test.getMapObject(BH1_KEY));
        assertThat(rs).hasSize(10);
    }

    @Test
    public void searchReceiveDate()
    {
        List<MapPoint> rs = mpPersist.searchReceiveDate(test.getMapObject(BH1_KEY), PersistTestSetup.FIXED_TIME_1, PersistTestSetup.FIXED_TIME_2);
        assertThat(rs).hasSize(10);
    }

    @Test
    public void searchTimeThreshold()
    {
        List<MapPoint> rs = mpPersist.searchTimeThreshold(test.getMapObject(BH1_KEY), PersistTestSetup.FIXED_TIME_1);
        assertThat(rs).hasSize(9);
    }

    @Test
    @SuppressLogging(AExtendedDataStructure.class)
    public void testBadMapPointData()
    {
        final IRaptorDataStructure rds = new DataStructureBuilder(BloodHoundSim.BLOODHOUND_FAMILY, BloodHoundSim.BLOODHOUND_TYPE, "1.0.0", "BAD_MESSAGE")
                .addString("field1").build();
        final IMapObject mapObject = test.getMapObject(BH0_KEY);

        // Test an unknown data type
        assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> {
            class SomeGoofyDataType
            {
            }
            IPrePersistRaptorDataMessage rdm = rds.createRaptorDataMessage(mapObject);
            SomeGoofyDataType g = new SomeGoofyDataType();

            rdm.setField("field1", g);
            rdmPersist.persistRDM(rdm);
        });

        // Test an out-of-bounds location
        assertThatIllegalArgumentException().isThrownBy(() -> {
            IPrePersistRaptorDataMessage rdm = rds.createRaptorDataMessage(mapObject);
            rdm.setLocation(99.0, 190.0);
        });

        // Test another out-of-bounds location
        assertThatIllegalArgumentException().isThrownBy(() -> {
            IPrePersistRaptorDataMessage rdm = rds.createRaptorDataMessage(mapObject);
            rdm.setLocation(-99.0, -190.0);
        });
    }

    @Test
    public void testMapPointDataArray()
    {
        final IMapObject mapObject = test.getMapObject(BH0_KEY);

        DataStructureBuilder builder = new DataStructureBuilder(BloodHoundSim.BLOODHOUND_FAMILY, BloodHoundSim.BLOODHOUND_TYPE, "1.0.0", "ARRAY_MESSAGE");
        builder.addIntArray("ia2");
        builder.addShortArray("sa2");
        builder.addLongArray("la2");
        builder.addFloatArray("fa2");
        builder.addDoubleArray("da2");
        builder.addBooleanArray("ba2");
        builder.addDateArray("datea2");
        builder.addStringArray("stra2");
        IRaptorDataStructure rds = builder.build();

        IPrePersistRaptorDataMessage rdm = rds.createRaptorDataMessage(mapObject);

        int[] ia2 = {3, 4};
        rdm.setField("ia2", ia2);

        float[] fa2 = {3.3F, 4.4F};
        rdm.setField("fa2", fa2);

        double[] da2 = {3.3, 4.4};
        rdm.setField("da2", da2);

        short[] sa2 = {4, 5, 6};
        rdm.setField("sa2", sa2);

        boolean[] ba2 = {false, true, false};
        rdm.setField("ba2", ba2);

        long now = 1281564692000L;
        Date now1 = new Date(now);
        Date now2 = new Date(now);
        Date now3 = new Date(now);

        Date[] datea2 = {now3, now2, now1};
        rdm.setField("datea2", datea2);

        String a = "A";
        String b = "B";
        String c = "C";
        String[] stra2 = {c, b, a};
        rdm.setField("stra2", stra2);

        long[] la2 = {4, 5, 6};
        rdm.setField("la2", la2);

        rdm.setMapObject(mapObject);

        rdmPersist.persistRDM(rdm);

        Writer writer = new StringWriter();
        CsvEmitter emitter = new CsvEmitter();

        List<CsvMapping> mapping = new ArrayList<>();
        mapping.add(new CsvMapping("stra2", "stra2"));
        mapping.add(new CsvMapping("ba2", "ba2"));
        mapping.add(new CsvMapping("ia2", "ia2"));
        mapping.add(new CsvMapping("sa2", "sa2"));
        mapping.add(new CsvMapping("la2", "la2"));
        mapping.add(new CsvMapping("fa2", "fa2"));
        mapping.add(new CsvMapping("da2", "da2"));
        mapping.add(new CsvMapping("datea2", "datea2"));

        emitter.setMapping(mapping);

        try
        {
            emitter.write(rdm, writer);
            final String result = writer.toString();

            final String expected = "\"C|*|B|*|A\",\"false|*|true|*|false\",\"3|*|4\",\"4|*|5|*|6\",\"4|*|5|*|6\",\"3.3|*|4.4\",\"3.3|*|4.4\",\"2010-08-11T17:11:32-0500|*|2010-08-11T17:11:32-0500|*|2010-08-11T17:11:32-0500\"\n";

            assertThat(StringUtils.countMatches(expected, '|')).isEqualTo(StringUtils.countMatches(result, '|'));
        } catch (IOException e)
        {
            fail("Emitter was unable to emit array data types");
        }

        try
        {
            String xml = MessageSerializer.toXml(rdm);

            IPrePersistRaptorDataMessage xmlRdm = MessageSerializer.fromXml(xml, rds);

            assertThat(rdm.getBooleanArray("ba2")[1]).isEqualTo(xmlRdm.getBooleanArray("ba2")[1]);
            assertThat(rdm.getDateArray("datea2")[1].getTime()).isEqualTo(xmlRdm.getDateArray("datea2")[1].getTime());
            assertThat(rdm.getStringArray("stra2")[1]).isEqualTo(xmlRdm.getStringArray("stra2")[1]);
        } catch (Exception e)
        {
            fail("Message Serializer was unable to emit array data types");
        }
    }
}
