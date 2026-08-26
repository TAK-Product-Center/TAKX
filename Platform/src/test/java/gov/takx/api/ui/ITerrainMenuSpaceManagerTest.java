package gov.takx.api.ui;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.test.AContainerTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify injectability of {@link ITerrainMenuSpaceManager}.
 *
 * @since 2.38
 */
public class ITerrainMenuSpaceManagerTest extends AContainerTest
{
    @Test
    public void testInjection()
    {
        TestPseudoBean testPseudoBean = new TestPseudoBean();
        ITerrainMenuSpaceManager terrainMenuSpaceManager = testPseudoBean.getTerrainMenuSpaceManager();
        assertThat(terrainMenuSpaceManager).isNotNull();
    }

    /**
     * PseudoBean class for verifying menu space manager could be injected correctly.
     *
     * @since 2.38
     */
    @PseudoBean
    private static class TestPseudoBean
    {
        @Inject
        private ITerrainMenuSpaceManager terrainMenuSpaceManager;

        public ITerrainMenuSpaceManager getTerrainMenuSpaceManager()
        {
            return terrainMenuSpaceManager;
        }
    }
}
