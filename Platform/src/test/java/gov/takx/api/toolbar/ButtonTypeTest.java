package gov.takx.api.toolbar;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ButtonType}. Each test does a spot check of literals that have different behavior.
 *
 * @since 3.4
 */
public class ButtonTypeTest
{
    @Test
    public void testGetDisplayText()
    {
        assertThat(ButtonType.OVERLAYS.getDisplayText()).isEqualTo("Overlays");
        assertThat(ButtonType.POLYLINE.getDisplayText()).isEqualTo("Polyline");
        assertThat(ButtonType.PROJECT_FAVORITE.getDisplayText()).isNull();
        assertThat(ButtonType.PLUGIN_CREATED.getDisplayText()).isNull();
    }

    @Test
    public void testIsSingular()
    {
        assertThat(ButtonType.OVERLAYS.isSingular()).isTrue();
        assertThat(ButtonType.EVENT_LOG.isSingular()).isTrue();
        assertThat(ButtonType.PROJECT_FAVORITE.isSingular()).isFalse();
        assertThat(ButtonType.CONNECTION_PLUGIN.isSingular()).isFalse();
        assertThat(ButtonType.PLUGIN_CREATED.isSingular()).isFalse();
    }

    @Test
    public void testGetCategory()
    {
        assertThat(ButtonType.OVERLAYS.getCategory()).isNull();
        assertThat(ButtonType.PROJECT_FAVORITE.getCategory()).isEqualTo("Project Favorite");
        assertThat(ButtonType.PLUGIN_CREATED.getCategory()).isNull();
    }

    @Test
    public void testGetImageUrl()
    {
        URL imageUrl = ButtonType.OVERLAYS.getImageUrl();
        assertThat(imageUrl).isNotNull();
        assertThat(imageUrl.getPath()).contains("Overlays_32x32.png");

        imageUrl = ButtonType.EVENT_LOG.getImageUrl();
        assertThat(imageUrl).isNotNull();
        assertThat(imageUrl.getPath()).contains("event_log.png");

        imageUrl = ButtonType.PLUGIN_CREATED.getImageUrl();
        assertThat(imageUrl).isNotNull();
        assertThat(imageUrl.getPath()).contains("plugin.png");
    }

    @Test
    public void testGetImage()
    {
        assertThat(ButtonType.OVERLAYS.getImage()).isNotNull();
        assertThat(ButtonType.EVENT_LOG.getImage()).isNotNull();
        assertThat(ButtonType.PLUGIN_CREATED.getImage()).isNotNull();
    }

    @Test
    public void testGetSingularId()
    {
        assertThat(ButtonType.OVERLAYS.getSingularId()).isNotNull().isEqualTo(ButtonType.OVERLAYS.getSingularId());
        assertThat(ButtonType.PROJECT_FAVORITE.getSingularId()).isNull();
        assertThat(ButtonType.PLUGIN_CREATED.getSingularId()).isNull();
    }
}