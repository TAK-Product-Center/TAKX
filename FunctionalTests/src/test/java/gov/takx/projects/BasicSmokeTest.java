package gov.takx.projects;

import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Perform basic smoke tests of the whole TAKX platform. Open a project and ensure that the basics are all working.
 *
 */
public class BasicSmokeTest extends AIntegrationTest
{
    @Test
    public void testInitialState()
    {
        // Test initial state
        assertThat(projectManager.getProjectName()).withFailMessage("Project name").isEqualTo(getClass().getSimpleName());
        assertThat(getBean(IMapObjectManager.class).getMapObjectControllers().size()).withFailMessage("MOC count").isEqualTo(0);
        checkEventLogEntryCount(0);
        assertThat(getBean(IRaptorLayerManager.class).getLayers().size()).withFailMessage("Layer count").isGreaterThan(10); // Can't use exact count (too limiting)
    }
}
