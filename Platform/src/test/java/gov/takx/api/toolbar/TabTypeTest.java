package gov.takx.api.toolbar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TabType}.
 *
 * @since 3.4
 */
public class TabTypeTest
{
    @Test
    public void testIsUpdatesDynamically()
    {
        // Test a subset of the literals that have different behavior
        assertThat(TabType.HOME.isUpdatesDynamically()).isFalse();
        assertThat(TabType.CONNECT.isUpdatesDynamically()).isTrue();
        assertThat(TabType.PLUGIN_CREATED.isUpdatesDynamically()).isFalse();
        assertThat(TabType.USER_CREATED.isUpdatesDynamically()).isFalse();
    }

    @Test
    public void testGetDynamicButtonTypes()
    {
        // Test a subset of the literals that have different behavior
        assertThat(TabType.HOME.getDynamicButtonTypes()).isEmpty();
        assertThat(TabType.CONNECT.getDynamicButtonTypes()).containsOnly(ButtonType.CONNECTION_PLUGIN);
        assertThat(TabType.DATA.getDynamicButtonTypes()).containsOnly(ButtonType.IMPORT_PLUGIN, ButtonType.EXPORT_PLUGIN);
        assertThat(TabType.PLUGIN_CREATED.getDynamicButtonTypes()).isEmpty();
        assertThat(TabType.USER_CREATED.getDynamicButtonTypes()).isEmpty();
    }

    @Test
    public void testGetDefaultTabTypes()
    {
        // Make sure all the tabs are returned in the expected order
        assertThat(TabType.getDefaultTabTypes()).containsExactly(TabType.HOME, TabType.CONNECT, TabType.DEVICES,
                TabType.MAPS, TabType.ATTACHMENTS, TabType.DATA, TabType.DRAW, TabType.VIEW, TabType.PLUGINS,
                TabType.RANGE_AND_BEARING, TabType.HELP);
    }
}