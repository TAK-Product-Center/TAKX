package gov.takx.platform.utils;

import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class TableLayoutManagerTest
{
    /**
     * Verify that the random-data layout definition can be parsed and turned into a LayoutsDefinitionsCollection.
     *
     * @throws IOException Unexpected
     * @throws JAXBException Unexpected
     */
    @Test
    public void testLoadSchema() throws IOException, JAXBException
    {
        URL url = getClass().getClassLoader().getResource("schemas/layout1.xml");
        var ignored = TableLayoutManager.loadLayoutDefinitions(url);
    }

    /**
     * Verify that the random-data layout definition can be parsed and turned into a LayoutsDefinitionsCollection.
     *
     * @throws IOException Unexpected
     * @throws JAXBException Unexpected
     */
    @Test
    public void testSchemaResults() throws IOException, JAXBException
    {
        URL url = getClass().getClassLoader().getResource("schemas/layout1.xml");
        var collection = TableLayoutManager.loadLayoutDefinitions(url);
        assertThat(collection.getLayoutDefinition()).hasSize(5);
    }
}
