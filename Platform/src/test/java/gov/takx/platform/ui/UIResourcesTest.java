package gov.takx.platform.ui;

import gov.takx.api.annotation.SuppressLogging;
import gov.takx.test.AContainerTest;
import gov.takx.url.UiResourceUrlConnection;
import org.junit.jupiter.api.Test;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static org.assertj.core.api.Assertions.assertThat;

public class UIResourcesTest extends AContainerTest
{
    @SuppressLogging(UiResourceUrlConnection.class)
    @Test
    public void getFeatureIcon_withMissingResource_doesNotThrow()
    {
        assertThat(UIResources.getFeatureIcon(PLATFORM_ICONS, "sir_not_appearing_in_this_film.png"))
                .isNotNull();
    }
}
