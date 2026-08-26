package gov.takx.platform.ui;

import gov.takx.api.ui.ComponentPosition;
import gov.takx.commons.swing.ImageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;

import java.awt.Rectangle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link ComponentDescriptorBuilder} and {@link ComponentDescriptor}.
 *
 * @since 3.4
 */
public class ComponentDescriptorBuilderTest
{
    protected static final String TEST_NAME = "test name";
    private ComponentDescriptorBuilder componentDescriptorBuilder;

    @BeforeEach
    public void setUp()
    {
        componentDescriptorBuilder = new ComponentDescriptorBuilder(JPanel.class, TEST_NAME);
    }

    @Test
    public void testConstruct_withComponentClassAndName_hasUniqueIdAndName()
    {
        final ComponentDescriptor descriptor = componentDescriptorBuilder.build();
        assertThat(descriptor.getId()).contains(JPanel.class.getCanonicalName(), TEST_NAME);
        assertThat(descriptor.getName()).isEqualTo(TEST_NAME);
    }

    @Test
    public void testConstruct_withComponent_hasUniqueIdAndName()
    {
        final JPanel testPanel = new JPanel();

        final ComponentDescriptor descriptor = new ComponentDescriptorBuilder(testPanel, TEST_NAME).build();
        assertThat(descriptor.getId()).contains(testPanel.getClass().getCanonicalName(), TEST_NAME);
        assertThat(descriptor.getName()).isEqualTo(TEST_NAME);
    }

    @Test
    public void testSetIcon()
    {
        //noinspection ConstantConditions
        assertThatNullPointerException().isThrownBy(() -> componentDescriptorBuilder.setIcon(null)
                .build());

        final ComponentDescriptor descriptor = componentDescriptorBuilder.setIcon(ImageUtils.DEFAULT_IMAGE_URL)
                .build();
        assertThat(descriptor.getIconUrl()).isEqualTo(ImageUtils.DEFAULT_IMAGE_URL);
    }

    @Test
    public void testSetTooltipText()
    {
        String tooltip = "tooltip text";
        final ComponentDescriptor descriptor = componentDescriptorBuilder.setTooltipText(tooltip)
                .build();
        assertThat(descriptor.getTooltipText()).isEqualTo(tooltip);
    }

    @Test
    public void testSetComponentPosition()
    {
        ComponentPosition componentPosition = ComponentPosition.BOTTOM;
        final ComponentDescriptor descriptor = componentDescriptorBuilder.setComponentPosition(componentPosition)
                .build();
        assertThat(descriptor.getComponentPosition()).isEqualTo(componentPosition);
    }

    @Test
    public void testSetToolbarButtonVisible()
    {
        final ComponentDescriptor descriptor = componentDescriptorBuilder.setToolbarButtonVisible(false)
                .build();
        assertThat(descriptor.isToolbarButtonVisible()).isFalse();
    }

    @Test
    public void testSetScrollbarsEnabled()
    {
        final ComponentDescriptor descriptor = componentDescriptorBuilder.setParentScrollingEnabled(true)
                .build();
        assertThat(descriptor.isParentScrollingEnabled()).isTrue();
    }

    @Test
    public void testSetCloseable()
    {
        final ComponentDescriptor descriptor = componentDescriptorBuilder.setCloseable(false)
                .build();
        assertThat(descriptor.isCloseable()).isFalse();
    }

    @Test
    public void testSetExternalizedRect()
    {
        final Rectangle rect = new Rectangle(1, 1);
        final ComponentDescriptor descriptor = componentDescriptorBuilder.setExternalizedRect(rect)
                .build();
        assertThat(descriptor.getExternalizedRect()).isEqualTo(rect);
    }

    @Test
    public void testSetPinned()
    {
        ComponentDescriptor descriptor = componentDescriptorBuilder.build();
        assertThat(descriptor.isPinned()).isNull();

        descriptor = componentDescriptorBuilder.setPinned(false).build();
        assertThat(descriptor.isPinned()).isFalse();

        descriptor = componentDescriptorBuilder.setPinned(true).build();
        assertThat(descriptor.isPinned()).isTrue();

        descriptor = componentDescriptorBuilder.setPinned(null).build();
        assertThat(descriptor.isPinned()).isNull();
    }

    @Test
    public void testSetHidePin()
    {
        final ComponentDescriptor descriptor = componentDescriptorBuilder.setHidePin(true)
                .build();
        assertThat(descriptor.isHidePin()).isTrue();
    }

    @Test
    public void testSetPlaybackAware()
    {
        final ComponentDescriptor descriptor = componentDescriptorBuilder.setPlaybackAware(true)
                .build();
        assertThat(descriptor.isPlaybackAware()).isTrue();
    }

    @Test
    public void testSetExternalizable()
    {
        final ComponentDescriptor descriptor = componentDescriptorBuilder.setExternalizable(false)
                .build();
        assertThat(descriptor.isExternalizable()).isFalse();
    }
}