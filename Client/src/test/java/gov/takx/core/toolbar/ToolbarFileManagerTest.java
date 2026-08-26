package gov.takx.core.toolbar;

import gov.takx.test.AContainerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the methods of {@link ToolbarFileManager}.
 *
 * @since 2.1 (Renamed in 3.4)
 */
public class ToolbarFileManagerTest extends AContainerTest
{
    String filename;

    /**
     * Initialize filename before test is run
     */
    @BeforeEach
    public void setup()
    {
        filename = "Config";
    }

    /**
     * This will clean up the new file that is created by writeToConfigurationFile
     */
    @AfterEach
    public void cleanUp()
    {
        try
        {
            Files.delete(Paths.get(new File("../bin/" + filename).getCanonicalPath()));
        } catch (IOException ignored)
        {
        }
    }

    /**
     * Verifies that configuration is written correctly to a given filename
     */
    @Test
    public void testWriteConfigurationToFile()
    {
        ToolbarFileManager configurationFileManager = new ToolbarFileManager();
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        configuration.setName(filename);
        assertThat(configurationFileManager.writeConfigurationToFile(configuration.getName(), configuration)).isEqualTo(true);
    }
}
