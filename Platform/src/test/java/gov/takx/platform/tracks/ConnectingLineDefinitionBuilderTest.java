package gov.takx.platform.tracks;

import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.test.AContainerTest;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link ConnectingLineDefinitionBuilder} implementation.
 *
 */
public class ConnectingLineDefinitionBuilderTest extends AContainerTest
{
    /**
     * Verify that the random-data structure definition can be parsed and turned into an IConnectingLineDefinition.
     */
    @Test
    public void testConnectingLine1Schema() throws IOException, JAXBException
    {
        URL url = getClass().getClassLoader().getResource("schemas/connectingLine1.xml");
        Map<String, IConnectingLineDefinition> map = ConnectingLineDefinitionBuilder.buildFromXML(url);

        assertThat(map).withFailMessage("Correct number of CLDs from XML").hasSize(2);
    }
}
