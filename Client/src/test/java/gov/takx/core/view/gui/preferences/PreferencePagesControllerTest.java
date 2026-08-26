package gov.takx.core.view.gui.preferences;

import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.test.AContainerTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @since 5.8
 */
public class PreferencePagesControllerTest extends AContainerTest
{
    @Mocked
    IUISpaceManager uiSpaceManager;

    private PreferencePagesController controller;
    private APreferencesPage mainPage;

    @BeforeEach
    public void beforeEach()
    {
        stopService(PreferencePagesController.class); // Clear all pages

        controller = getBean(PreferencePagesController.class);
        controller.getPreferencesPage(); // Force creation of main page
        mainPage = controller.getCurrentPage();
    }

    @Test
    void getCurrentPage_returnsCorrectPage()
    {
        final var p1_1 = new PrefPage1("p1_1");
        controller.push(p1_1);
        assertThat(controller.getCurrentPage()).isSameAs(p1_1);

        final var p2_1 = new PrefPage2("p2_1");
        controller.push(p2_1);
        assertThat(controller.getCurrentPage()).isSameAs(p2_1);

        controller.goBackward();
        assertThat(controller.getCurrentPage()).isSameAs(p1_1);

        controller.goBackward();
        assertThat(controller.getCurrentPage()).isSameAs(mainPage);
    }

    @Test
    void verifyNavigationWorksCorrectly()
    {
        assertThat(controller.getCurrentPage()).isSameAs(mainPage);

        assertThat(controller.canGoBackward()).isFalse();
        assertThatIllegalStateException().isThrownBy(() -> controller.goBackward());
        assertThat(controller.canGoForward()).isFalse();
        assertThatIllegalStateException().isThrownBy(() -> controller.goForward());

        final PrefPage1 p1_1 = new PrefPage1("p1_1");
        controller.push(p1_1);
        assertThat(controller.getCurrentPage()).isSameAs(p1_1);
        assertThat(controller.canGoBackward()).isTrue();
        assertThat(controller.canGoForward()).isFalse();

        final PrefPage1 p1_2 = new PrefPage1("p1_2");
        controller.push(p1_2);
        assertThat(controller.getCurrentPage()).isSameAs(p1_2);
        assertThat(controller.canGoBackward()).isTrue();
        assertThat(controller.canGoForward()).isFalse();

        controller.goBackward();
        assertThat(controller.getCurrentPage()).isSameAs(p1_1);
        assertThat(controller.canGoBackward()).isTrue();
        assertThat(controller.canGoForward()).isTrue();

        controller.goForward();
        assertThat(controller.getCurrentPage()).isSameAs(p1_2);
        assertThat(controller.canGoBackward()).isTrue();
        assertThat(controller.canGoForward()).isFalse();

        controller.goBackward();
        controller.goBackward(); // Back to main page
        assertThat(controller.canGoBackward()).isFalse();
        assertThat(controller.canGoForward()).isTrue();
        assertThat(controller.getCurrentPage()).isSameAs(mainPage);
        assertThatIllegalStateException().isThrownBy(() -> controller.goBackward());
    }

    @Test
    void removePages_removesExpectedPages()
    {
        final var p1_1 = new PrefPage1("p1_1");
        final var p1_2 = new PrefPage1("p1_2");
        final var p1_3 = new PrefPage1("p1_3");
        final var p2_1 = new PrefPage2("p2_1");
        final var p2_2 = new PrefPage2("p2_2");

        controller.push(p1_1);
        controller.push(p2_1);
        controller.push(p1_2);
        controller.push(p1_2); // Multiple pages of the same type can be present in the page stack
        controller.push(p2_2);
        controller.push(p1_3);

        controller.removePages(page -> page instanceof PrefPage1);

        assertThat(controller.canGoForward()).isFalse();

        assertThat(controller.getCurrentPage()).isSameAs(p2_2);
        controller.goBackward();
        assertThat(controller.getCurrentPage()).isSameAs(p2_1);
        controller.goBackward();
        assertThat(controller.getCurrentPage()).isSameAs(mainPage);
    }

    private static class PrefPage1 extends APreferencesPage
    {
        PrefPage1(@Nonnull String title)
        {
            super(title);
        }
    }

    private static class PrefPage2 extends APreferencesPage
    {
        PrefPage2(@Nonnull String title)
        {
            super(title);
        }
    }
}
