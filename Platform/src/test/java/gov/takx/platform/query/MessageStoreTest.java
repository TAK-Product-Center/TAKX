package gov.takx.platform.query;

import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.query.ASimpleFieldSelector;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.CoreFieldSelector;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.api.query.NoSuchFieldException;
import gov.takx.core.messagequery.ExtendedQueryService;
import gov.takx.core.messagequery.PluginQueryService;
import gov.takx.test.AQueryServiceTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of {@link MessageStore}
 */
@SuppressWarnings("rawtypes")
public class MessageStoreTest extends AQueryServiceTest
{
    @Test
    public void testMessageStore()
    {
        final PluginQueryService qs = createMultiVersionQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();
        List<IRaptorDataMessage> rdmList = qs.query(qc);

        MessageStore<MessageStore, IRaptorDataMessage> ms = new MessageStore<>();
        ms.loadMessages(rdmList);

        assertThat(rdmList).withFailMessage("MessageStore should contain all added RDMs").hasSize(ms.size());
        assertThat(rdmList).withFailMessage("An empty query should return all records").hasSize(ms.query(qc).size());
    }

    @Test
    void testMessageStoreSortLongitude()
    {
        final PluginQueryService qs = createMultiVersionQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        MessageStore<MessageStore, IRaptorDataMessage> ms = createMessageStore(qs, qc);

        ASimpleFieldSelector longitudeSelector = new CoreFieldSelector(CoreField.longitude);
        qc.setSortingOptions(longitudeSelector, IQueryConstraints.SortOrder.ASCENDING);

        List<IRaptorDataMessage> rdmsLonAsc = ms.query(qc);

        double previousLongitude = rdmsLonAsc.get(0).getLongitude();
        for (int i = 1; i < rdmsLonAsc.size(); i++)
        {
            final double thisLongitude = rdmsLonAsc.get(i).getLongitude();
            assertThat(thisLongitude).withFailMessage("Results not in ascending order by longitude; " + thisLongitude + " is not >= " + previousLongitude).isGreaterThanOrEqualTo(previousLongitude);
            previousLongitude = thisLongitude;
        }

        qc.setSortingOptions(longitudeSelector, IQueryConstraints.SortOrder.DESCENDING);
        List<IRaptorDataMessage> rdmsLonDesc = ms.query(qc);

        previousLongitude = rdmsLonDesc.get(0).getLongitude();
        for (int i = 1; i < rdmsLonDesc.size(); i++)
        {
            final double thisLongitude = rdmsLonDesc.get(i).getLongitude();
            assertThat(thisLongitude).withFailMessage("Results not in descending order by longitude; " + thisLongitude + " is not <= " + previousLongitude).isLessThanOrEqualTo(previousLongitude);
            previousLongitude = thisLongitude;
        }
    }

    @Test
    void testMessageStoreSortReceiveTime()
    {
        final PluginQueryService qs = createMultiVersionQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        MessageStore<MessageStore, IRaptorDataMessage> ms = createMessageStore(qs, qc);

        ASimpleFieldSelector longitudeSelector = new CoreFieldSelector(CoreField.receiveTime);
        qc.setSortingOptions(longitudeSelector, IQueryConstraints.SortOrder.ASCENDING);

        List<IRaptorDataMessage> rdmsRtAsc = ms.query(qc);

        double previousReceiveTime = rdmsRtAsc.get(0).getReceiveTime();
        for (int i = 1; i < rdmsRtAsc.size(); i++)
        {
            final double thisReceiveTime = rdmsRtAsc.get(i).getReceiveTime();
            assertThat(thisReceiveTime).withFailMessage("Results not in ascending order by receive time; " + thisReceiveTime + " is not >= " + previousReceiveTime).isGreaterThanOrEqualTo(previousReceiveTime);
            previousReceiveTime = thisReceiveTime;
        }

        qc.setSortingOptions(longitudeSelector, IQueryConstraints.SortOrder.DESCENDING);
        List<IRaptorDataMessage> rdmsRtDesc = ms.query(qc);

        previousReceiveTime = rdmsRtDesc.get(0).getReceiveTime();
        for (int i = 1; i < rdmsRtDesc.size(); i++)
        {
            final double thisReceiveTime = rdmsRtDesc.get(i).getReceiveTime();
            assertThat(thisReceiveTime).withFailMessage("Results not in descending order by receive time; " + thisReceiveTime + " is not <= " + previousReceiveTime).isLessThanOrEqualTo(previousReceiveTime);
            previousReceiveTime = thisReceiveTime;
        }
    }

    /**
     * Validate that the MessageStore properly handles message structure ids.
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
        makeRdmWithLocation(struct1);
        makeRdmWithLocation(struct2);
        makeRdmWithLocation(struct3);
        makeRdmWithLocation(structOther);

        final IQueryService qs = new ExtendedQueryService();
        final IQueryConstraints qc = qs.createEmptyConstraints();

        // Pull ALL messages into the store (empty constraints == all)
        final MessageStore<MessageStore, IRaptorDataMessage> ms = createMessageStore(qs, qc);

        qc.addKnownMessageStructures(new HashSet<>(Arrays.asList(struct1, struct2, struct3, structOther)));

        qc.addMessageStructureConstraint("s1", "structOther");
        assertThat(ms.count(qc)).isEqualTo(4); // 3 named "s1" + "structOther"

        // This will override the initial structure constraint on "s1" and limit it to this specific s1 (struct3)
        qc.addMessageStructureConstraint(struct3);
        assertThat(ms.count(qc)).isEqualTo(2);
    }

    /**
     * Construct and persist an RDM with a location.  The location is required so that it will be included in
     * the message store.
     *
     * @param structure Structure from which to construct the RDM
     * @since 2.38
     */
    private void makeRdmWithLocation(IRaptorDataStructure structure)
    {
        final IPrePersistRaptorDataMessage rdm1 = structure.createRaptorDataMessage(originMo);
        rdm1.setLocation(new RaptorLocation(1, 1));
        test.getService().persistRDM(rdm1);
    }

    /**
     * MessageStore version of the {@link QueryServiceTest#testHeadingCoreFieldConstraint()}
     */
    @Test
    public void testMessageStoreHeadingCoreFieldConstraint()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        MessageStore<MessageStore, IRaptorDataMessage> ms = createMessageStore(qs, qc);

        // Headings start at 10 and increment by 10
        qc.addFieldConstraint(CoreField.heading, ComparisonType.GTE, 50.0);
        assertThat(ms.count(qc)).isEqualTo(6);

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.GTE, 50.0);
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LTE, 70.0);
        assertThat(ms.count(qc)).isEqualTo(3);
    }

    /**
     * MessageStore version of the {@link QueryServiceTest#testMultipleCoreFieldConstraint()} test
     */
    @Test
    public void testMessageStoreQuerySpeed()
    {
        IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        IQueryConstraints qc = qs.createEmptyConstraints();

        MessageStore<MessageStore, IRaptorDataMessage> ms = createMessageStore(qs, qc);

        qc.addFieldConstraint(CoreField.heading, ComparisonType.GTE, 50.0);
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LTE, 100.0); // 6 match this

        qc.addFieldConstraint(CoreField.pitch, ComparisonType.GTE, -10.0);
        qc.addFieldConstraint(CoreField.pitch, ComparisonType.LTE, 70.0); // 5 match this

        assertThat(ms.count(qc)).isEqualTo(5); // Overlap should be 5

        qc.addFieldConstraint(CoreField.roll, ComparisonType.GTE, 60.0);
        qc.addFieldConstraint(CoreField.roll, ComparisonType.LTE, 80.0); // 3 match this

        assertThat(ms.count(qc)).isEqualTo(3); // Overlap should be 3

        qc.addFieldConstraint(CoreField.speed, ComparisonType.EQ, 700.0);

        assertThat(ms.count(qc)).isEqualTo(1); // Overlap should be 1
    }

    @Test
    public void testGetFieldsAsMap() throws NoSuchFieldException, ConflictingConstraintException
    {
        final IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        final IQueryConstraints qc = qs.createEmptyConstraints();
        final MessageStore<MessageStore, IRaptorDataMessage> ms = createMessageStore(qs, qc);

        // Start with a simple single core field query
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0); // Should be 4 matching RDMs
        FieldSelector fs = new FieldSelector().addCoreFields(CoreField.heading);

        List<Map<String, Object>> results = ms.queryForFieldsAsMap(qc, fs);
        assertThat(results).hasSize(4);

        // The result will be a list of Maps
        for (Map<String, Object> map : results)
        {
            assertThat((Double) map.get("heading")).isLessThan(50.0);
        }

        qc.addFieldConstraint(bh_struct.getMessageType(), "Voltage", ComparisonType.GT, 2.0);

        fs = new FieldSelector()
                .addCoreFields(CoreField.heading, CoreField.id, CoreField.receiveTime)
                .setStructure(bh_struct.getMessageType())
                .addExtendedFields("key", "Voltage");

        results = ms.queryForFieldsAsMap(qc, fs);
        assertThat(results).hasSize(4);

        // The results will be a list of Maps, the following keys will have been set in each:
        //    heading:     Double
        //    id:          Integer
        //    receiveTime: Long/BigInteger
        //    key:         String
        //    Voltage:     Double

        for (Map<String, Object> map : results)
        {
            Double heading = (Double) map.get("heading");
            Long receiveTime = (Long) map.get("receiveTime");
            String key = (String) map.get("key");
            Double voltage = (Double) map.get("Voltage");
            Integer id = (Integer) map.get("id");

            assertThat(heading).withFailMessage("Has 'heading' value").isNotNull();
            assertThat(id).withFailMessage("Has 'id' value").isNotNull();
            assertThat(receiveTime).withFailMessage("Has 'receiveTime' value").isNotNull();
            assertThat(key).withFailMessage("Has 'key' value").isNotNull();
            assertThat(voltage).withFailMessage("Has 'voltage' value").isNotNull();

            assertThat(voltage).withFailMessage("Selected field value doesn't match constraint").isGreaterThan(2.0);
            assertThat(key).withFailMessage("Selected field value doesn't match constraint").isEqualTo("mykey");
            assertThat(heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }
    }

    @Test
    public void testGetFieldsAsBean() throws NoSuchFieldException, ConflictingConstraintException
    {
        final IQueryService qs = createPluginQueryService(BH_FAMILY, BH_DEV_TYPE, "0", bh_struct);
        final IQueryConstraints qc = qs.createEmptyConstraints();
        final MessageStore<MessageStore, IRaptorDataMessage> ms = createMessageStore(qs, qc);

        // Start with a simple single core field query
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0); // Should be 4 matching RDMs
        FieldSelector fs = new FieldSelector().addCoreFields(CoreField.heading);

        //noinspection unchecked
        List<Bean> results = ms.queryForFieldsAsBean(qc, fs, Bean.class);
        assertThat(results).hasSize(4);

        // The result will be a list of Bean objects
        for (Bean bean : results)
        {
            assertThat(bean.heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }

        qc.addFieldConstraint(bh_struct.getMessageType(), "Voltage", ComparisonType.GT, 2.0);

        fs = new FieldSelector()
                .addCoreFields(CoreField.heading, CoreField.id, CoreField.receiveTime)
                .setStructure(bh_struct.getMessageType())
                .addExtendedFields("key", "Voltage");

        //noinspection unchecked
        results = ms.queryForFieldsAsBean(qc, fs, Bean.class);
        assertThat(results).hasSize(4);

        // The results will be a list of Beans, the following fields will have been set:
        //    heading:     Double
        //    id:          Integer
        //    receiveTime: Long/BigInteger
        //    key:         String
        //    Voltage:     Double

        for (Bean bean : results)
        {
            Double heading = bean.heading;
            Long receiveTime = bean.receiveTime;
            String key = bean.key;
            Double voltage = bean.Voltage;
            Integer id = bean.id;

            assertThat(heading).withFailMessage("Has 'heading' value").isNotNull();
            assertThat(id).withFailMessage("Has 'id' value").isNotNull();
            assertThat(receiveTime).withFailMessage("Has 'receiveTime' value").isNotNull();
            assertThat(key).withFailMessage("Has 'key' value").isNotNull();
            assertThat(voltage).withFailMessage("Has 'voltage' value").isNotNull();

            assertThat(voltage).withFailMessage("Selected field value doesn't match constraint").isGreaterThan(2.0);
            assertThat(key).withFailMessage("Selected field value doesn't match constraint").isEqualTo("mykey");
            assertThat(heading).withFailMessage("Selected field value doesn't match constraint").isLessThan(50.0);
        }
    }

    private MessageStore<MessageStore, IRaptorDataMessage> createMessageStore(IQueryService qs, IQueryConstraints qc)
    {
        final MessageStore<MessageStore, IRaptorDataMessage> messageStore = new MessageStore<>();
        messageStore.loadMessages(qs.query(qc));

        return messageStore;
    }

    @SuppressWarnings("FieldMayBeFinal")
    public static class Bean
    {
        private Integer id = null;
        private Double heading = null;
        private Long receiveTime = null;

        // Extended fields
        private String key = null;
        private Double Voltage = null; // Note capital V to match field name in results
    }
}
