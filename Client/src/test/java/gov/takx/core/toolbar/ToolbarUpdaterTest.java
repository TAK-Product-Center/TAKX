package gov.takx.core.toolbar;

import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the methods of {@link ToolbarUpdater}.
 *
 * @since 2.1 (Renamed in 3.4)
 */
public class ToolbarUpdaterTest extends AContainerTest
{
    /**
     * Verifies that getTabEffectiveIndex is returning the correct index value
     */
    @Test
    public void testGetTabEffectiveIndex()
    {
        ToolbarUpdater updater = getBean(ToolbarUpdater.class);
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        ToolbarTab tab1 = ToolbarTab.createUserTab("Test");
        ToolbarTab tab2 = ToolbarTab.createUserTab("Test");
        ToolbarTab tab3 = ToolbarTab.createUserTab("Test");

        tab1.setValid(false);
        tab2.setVisible(false, false);

        configuration.addTab(tab1, false);
        configuration.addTab(tab2, false);
        configuration.addTab(tab3, false);

        List<ToolbarTab> siblingTabs = new ArrayList<>();
        siblingTabs.add(tab1);
        siblingTabs.add(tab2);

        int index = CdiTestUtils.invoke(updater, "getTabEffectiveIndex", siblingTabs, tab3);
        assertThat(index).isEqualTo(0);
    }
}
