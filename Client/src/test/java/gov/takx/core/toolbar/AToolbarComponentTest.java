package gov.takx.core.toolbar;

import gov.takx.api.plugin.IPlugin;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AToolbarComponent}.
 *
 * @since 3.4
 */
public class AToolbarComponentTest
{
    private final TestDevicePlugin testDevicePlugin = new TestDevicePlugin();

    @Test
    public void testGetParentConfiguration()
    {
        final TestToolbarComponent component = new TestToolbarComponent();
        assertThat(component.getParentConfiguration()).isNull();

        final ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");
        component.setTab(tab);
        assertThat(component.getParentConfiguration()).isNull();

        final ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        tab.setConfiguration(configuration);
        assertThat(component.getParentConfiguration()).isNotNull().isEqualTo(configuration);
    }

    @Test
    public void testIsParentVisible()
    {
        final TestToolbarComponent component = new TestToolbarComponent();
        assertThat(component.isParentVisible()).isFalse();

        final ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");
        component.setTab(tab);
        assertThat(component.isParentVisible()).isTrue();

        tab.setValid(false);
        assertThat(component.isParentVisible()).isFalse();

        tab.setValid(true);
        tab.setVisible(false, false);
        assertThat(component.isParentVisible()).isFalse();
    }

    @Test
    public void testGetId()
    {
        final TestToolbarComponent component = new TestToolbarComponent();
        final UUID type3UUID = UUIDUtils.generateType3UUID("test");
        final TestToolbarComponent component2 = new TestToolbarComponent(type3UUID);
        final TestToolbarComponent component3 = new TestToolbarComponent(type3UUID);

        // If no id provided, one will not be created (JAXB will set it in practice)
        assertThat(component.id).isNull();

        // If an id is provided, it should be returned
        assertThat(component2.getId()).isEqualTo(component3.getId()).isEqualTo(type3UUID);
    }

    @Test
    public void testGetPluginUniqueKey()
    {
        final TestToolbarComponent component = new TestToolbarComponent(testDevicePlugin);
        assertThat(component.getPluginUniqueKey()).isEqualTo(testDevicePlugin.getUniqueKey());
    }

    /**
     * Concrete implementation to support testing the abstract base class.
     */
    private static class TestToolbarComponent extends AToolbarComponent
    {
        public TestToolbarComponent()
        {
        }

        public TestToolbarComponent(@Nonnull UUID id)
        {
            super(id, null);
        }

        public TestToolbarComponent(@Nonnull IPlugin plugin)
        {
            super(UUIDUtils.generateType1UUID(), plugin);
        }

        public TestToolbarComponent(@Nonnull AToolbarComponent origComponent)
        {
            super(origComponent);
        }

        @Override
        public boolean isValid()
        {
            return false;
        }

        @Nonnull
        @Override
        public AToolbarComponent copy()
        {
            return new TestToolbarComponent(this);
        }
    }
}