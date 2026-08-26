package gov.takx.core.toolbar;

import gov.takx.test.AContainerTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the methods of {@link ToolbarConfiguration}.
 *
 * @since 2.1 (Renamed in 3.4)
 */
public class ToolbarConfigurationTest extends AContainerTest
{
    /**
     * Verifies that tab can be added correctly
     */
    @Test
    public void testAddTab()
    {
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        configuration.addTab(tab, false);
        assertThat(configuration.getTabIndex(tab)).isEqualTo(0);
    }

    /**
     * Verifies that tab can be added at an index
     */
    @Test
    public void testAddTabIndex()
    {
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        configuration.insertTab(tab, 0, false);
        assertThat(configuration.getTabIndex(tab)).isEqualTo(0);
    }

    /**
     * Verifies that tab can be removed correctly
     */
    @Test
    public void testRemoveTab()
    {
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        ToolbarTab tab1 = ToolbarTab.createUserTab("Test");
        ToolbarTab tab2 = ToolbarTab.createUserTab("Test");

        configuration.addTab(tab1, false);
        configuration.addTab(tab2, false);

        configuration.removeTab(tab1);
        assertThat(configuration.getTabIndex(tab2)).isEqualTo(0);
    }

    @Test
    public void testGetTabIndex()
    {
        ToolbarTab tab1 = ToolbarTab.createUserTab("Test");
        ToolbarTab tab2 = ToolbarTab.createUserTab("Test");

        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        configuration.addTab(tab1, false);
        configuration.addTab(tab2, false);
        assertThat(configuration.getTabIndex(tab2)).isEqualTo(1);
    }

    /**
     * Verifies that tabs are returned in the order added.
     */
    @Test
    public void testSetTab()
    {
        ToolbarTab tab1 = ToolbarTab.createUserTab("Test");
        ToolbarTab tab2 = ToolbarTab.createUserTab("Test");

        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        configuration.addTab(tab1, false);
        configuration.addTab(tab2, false);
        assertThat(configuration.getTabs()).containsExactly(tab1, tab2);
    }
}
