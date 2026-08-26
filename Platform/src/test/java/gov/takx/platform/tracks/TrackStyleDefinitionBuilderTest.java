package gov.takx.platform.tracks;

import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.test.AContainerTest;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link TrackStyleDefinitionBuilder} implementation.
 *
 */
public class TrackStyleDefinitionBuilderTest extends AContainerTest
{
    /**
     * Verify that the random-data TSD can be parsed and turned into an ITrackStyleDefinition.
     *
     * @throws IOException Unexpected
     * @throws JAXBException Unexpected
     */
    @Test
    public void testTrackStyle1Schema() throws IOException, JAXBException
    {
        URL url = getClass().getClassLoader().getResource("schemas/trackStyle1.xml");
        Map<String, ITrackStyleDefinition> map = TrackStyleDefinitionBuilder.buildFromXML(url);

        assertThat(map).withFailMessage("Correct number of TSDs from XML").hasSize(2);
    }
}
