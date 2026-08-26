package gov.takx.api.messages;

import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.IExtendedQueryConstraints;
import gov.takx.api.query.InvalidFieldSelectorException;
import gov.takx.api.query.NoSuchFieldException;
import gov.takx.core.messagequery.ExtendedQueryService;
import gov.takx.dataservices.persist.RaptorDataStructure;
import gov.takx.dataservices.utils.DynamicStructureDBUtils;
import gov.takx.dataservices.utils.DynamicStructures;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test cases for {@link DataStructureBuilder} implementation.
 */
public class DataStructureBuilderTest extends AContainerTest
{
    private static final String FAMILY = "gov.takx";
    private static final String TYPE = "Test";
    private static final String VERSION = "1.0.0";
    private static final String NAME = "NAME";
    private static final String MSG_TYPE = "Test";
    private static final String DESCRIPTION = "DESCRIPTION";
    private static final String ONE_THING = "ONE_THING";
    private static final String ONE_ARRAY_THING = "ONE_ARRAY_THING";
    private static final String ONE_DATE_ARRAY_THING = "ONE_DATE_ARRAY_THING";
    private static final String ANOTHER_THING = "ANOTHER_THING";
    private IRaptorDataStructure testStruct;
    private PersistTestSetup test;

    @BeforeAll
    public void setup() throws JAXBException, IOException, NoSuchFieldException, InvalidFieldProcessorException
    {
        test = new PersistTestSetup();
        test.init();

        URL url = getClass().getClassLoader().getResource("schemas/sample1.xml");
        DataStructureBuilder.buildFromXML("gov.takx", "Acme", url);

        testStruct = new DataStructureBuilder(FAMILY, TYPE, VERSION, MSG_TYPE)
                .addInt("X(){System.exit(0);return\\u0020null;}//")
                // This field name would previously cause a failure to compile the generated structure methods.
                .addString("HELLOTHERE=\": java.lang.ProcessBuilder(\\\"notepad.exe\\\").start();\n" +
                        "default: java.lang.ProcessBuilder(\\\"notepad.exe\\\").start(); ////\"")
                .addString(NAME)
                .addString(DESCRIPTION)
                .setFieldLength(DESCRIPTION, 2048)
                .addInt(ONE_THING)
                .addIntArray(ONE_ARRAY_THING)
                .addDateArray(ONE_DATE_ARRAY_THING)
                .addDate("OneDate")
                .addStringArray("stringArray")
                .addShortArray("shortArray")
                .addInt(ANOTHER_THING).setFieldDisplayProcessor(ANOTHER_THING, TestFieldDisplayProcessor.class.getName())
                .addIndex(true, NAME)
                .addIndex(false, ONE_THING, ANOTHER_THING)
                .build();

        // Force the generation of the dynamic structure class to exercise the field name sanitization
        getBean(DynamicStructures.class).getOrGenerateClass(testStruct.getStructure());
    }

    @AfterAll
    public void close()
    {
        test.shutdown();
    }

    /**
     * Verify that the random-data structure definition can be parsed and turned into an IRaptorDataStructure.
     */
    @Test
    public void testSample1Schema()
    {
        // Query one structure back to ensure that the metadata was handled properly
        final IStructure structure = test.getService().queryUnique("from Structure where messageType = 'MessageType0'");
        final Set<String> structureMetadataKeys = structure.getMetadata().getMetadataKeys();
        assertThat(structureMetadataKeys).containsExactly("Key25", "Key26", "Key27", "Key28", "Key29");

        // Five fields each with five metadata entries
        for (int i = 0; i < 5; i++)
        {
            final String fieldName = "name" + i;
            final Set<String> metadataKeys = structure.getField(fieldName).getMetadataKeys();
            assertThat(metadataKeys).hasSize(5);
        }
    }

    /**
     * Verify that a query against a structure that hasn't had any RDMs persisted for it can be used in a query.
     * This validates that the dynamic mapping of DB tables occurs as part of query processing.
     *
     * @since 2.37
     */
    @Test
    public void testQueryBeforeData() throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        final String messageType = "before_query";
        new DataStructureBuilder("test", "type", "1", messageType)
                .addString(NAME)
                .addString(DESCRIPTION)
                .addInt(ONE_THING)
                .build();

        final IStructure structure = test.getService().queryUnique("from Structure where messageType = '" + messageType + "'");

        // First, ensure that the table doesn't exist already
        final String tableName = structure.getExtendedDataClassName();
        assertThat(test.getService().tableExists(tableName)).isFalse();

        // Then perform a query on it
        final ExtendedQueryService queryService = new ExtendedQueryService();
        final IExtendedQueryConstraints qc = queryService.createEmptyConstraints();

        final RaptorDataStructure rds = new RaptorDataStructure("ignore", "ignore", structure);
        qc.addKnownMessageStructures(Collections.singleton(rds));

        // If this query doesn't throw an exception, then we've verified table creation on query
        final List<Object[]> fields = queryService.queryForFields(qc, new FieldSelector().setStructure(messageType).addExtendedFields(NAME, DESCRIPTION));
        assertThat(fields).isEmpty();
        assertThat(test.getService().tableExists(tableName)).isTrue();
    }

    /**
     * Verify that the random-data structure definition can be parsed and turned into an IRaptorDataStructure.
     */
    @Test
    public void testSample2Schema() throws IOException, JAXBException
    {
        URL url = getClass().getClassLoader().getResource("schemas/sample2.xml");
        DataStructureBuilder.buildFromXML("gov.takx", "Acme", url);
    }

    /**
     * Verify that the random-data structure definition can be parsed and turned into an IRaptorDataStructure.
     */
    @Test
    public void testSample3Schema() throws IOException, JAXBException
    {
        URL url = getClass().getClassLoader().getResource("schemas/sample3.xml");

        final Map<String, IRaptorDataStructure> structureMap = DataStructureBuilder.buildFromXML("gov.takx", "Acme", url);
        assertThat(structureMap).hasSize(5);

        final IRaptorDataStructure mt0 = structureMap.get("MessageType0");
        assertThat(mt0).isNotNull();

        final List<? extends IIndexDefinition> indices = mt0.getStructure().getIndexes();
        assertThat(indices).hasSize(3);

        assertThat(mt0.getField("makeMeProcess").getFieldDisplayProcessor().getDisplayString(new Object())).isEqualTo("SugarMagnolia");

        IIndexDefinition definition = indices.get(0);
        List<String> columnNames = definition.getColumnNames();

        assertThat(definition.isUnique()).isFalse();
        assertThat(columnNames).hasSize(1).contains("name2");

        definition = indices.get(1);
        columnNames = definition.getColumnNames();

        assertThat(definition.isUnique()).isTrue();
        assertThat(columnNames).hasSize(2).contains("name0", "name1");

        definition = indices.get(2);
        columnNames = definition.getColumnNames();

        assertThat(definition.isUnique()).isFalse();
        assertThat(columnNames).hasSize(2).contains("name0", "name2");
    }

    @Test
    public void testStructureCreation()
    {
        final List<? extends IIndexDefinition> indices = testStruct.getStructure().getIndexes();
        assertThat(indices).hasSize(2);

        IIndexDefinition definition = indices.get(0);
        assertThat(definition.isUnique()).isTrue();
        List<String> columnNames = definition.getColumnNames();
        assertThat(columnNames).hasSize(1).contains(NAME);

        definition = indices.get(1);
        assertThat(definition.isUnique()).isFalse();
        columnNames = definition.getColumnNames();
        assertThat(columnNames).hasSize(2).contains(ONE_THING, ANOTHER_THING);

        // Force the DB table creation to occur
        getBean(DynamicStructureDBUtils.class).ensureMappedInDb(testStruct.getStructure());

        // Validate that both indices were created.  We don't know the generated index name, so we just validate
        // that there are 4 indices:
        //    The primary key
        //    The index on NAME
        //    The index on ONE_THING, ANOTHER_THING - which results in 2 indices

        test.getService().doWork(connection -> {
            final ResultSet indexInfo = connection.getMetaData().getIndexInfo(null, null, testStruct.getStructure().getExtendedDataClassName(), false, true);
            int indexCount = 0;

            while (indexInfo.next()) indexCount++;

            assertThat(indexCount).isEqualTo(4);
        });
    }

    /**
     * Test both structure definition contributed field display processors and ones that are constructed by default
     * for array fields.
     *
     * @since 2.29
     */
    @Test
    public void testFieldDisplayProcessor()
    {
        IStructure structure = testStruct.getStructure();
        assertThat(structure.getField(ANOTHER_THING)).isNotNull();
        assertThat(structure.getField(ANOTHER_THING).getFieldDisplayProcessor().getDisplayString(new Object())).isEqualTo("SugarMagnolia");
        assertThat(structure.getField(ONE_THING).getFieldDisplayProcessor()).isNull();
        assertThat(structure.getField(ONE_ARRAY_THING).getFieldDisplayProcessor()).isNotNull();
        int[] testArray = {1, 2};
        assertThat(structure.getField(ONE_ARRAY_THING).getFieldDisplayProcessor().getDisplayString(testArray)).isEqualTo("1|*|2");
    }

    /**
     * Specifying a bad class name for the field processor should result in an exception.
     *
     * @since 2.29
     */
    @Test
    public void testBadFieldProcessor()
    {
        assertThatExceptionOfType(InvalidFieldProcessorException.class)
                .isThrownBy(() -> new DataStructureBuilder(FAMILY, TYPE, VERSION, MSG_TYPE)
                        .addString(NAME).setFieldDisplayProcessor(NAME, "badclass"))
                .withMessageContaining("Error trying to find field display processor class by name: badclass ");
    }
}
