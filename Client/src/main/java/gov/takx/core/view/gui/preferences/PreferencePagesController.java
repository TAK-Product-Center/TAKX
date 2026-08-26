package gov.takx.core.view.gui.preferences;

import gov.takx.api.event.StopProjectServices;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.commons.swing.MouseButtonUtils;
import gov.takx.core.view.gui.ATitledPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.SwingUtilities;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Controller for preference pages and their navigation stack.
 * 
 * @since 5.4
 */
@ApplicationScoped
public class PreferencePagesController
{
    private final IUISpaceManager uiSpaceManager;

    private final PageHolder pageHolder = new PageHolder();
    private final List<APreferencesPage> pageList = new ArrayList<>();

    private MainPreferencesPage mainPreferencesPage;
    private int pageIndex = -1;

    @Inject
    PreferencePagesController(IUISpaceManager uiSpaceManager)
    {
        this.uiSpaceManager = uiSpaceManager;

        Toolkit.getDefaultToolkit().addAWTEventListener(new NavigationAndHighlightHandler(),
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    /**
     * @since 5.8
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices ignored)
    {
        if (mainPreferencesPage != null)
        {
            releasePages(1);
            pageIndex = 0;
            showPage(mainPreferencesPage);
        }
    }

    /**
     * Remove all pages in the list after {@code start}, and release their resources.
     * @since 5.8
     */
    private void releasePages(int start)
    {
        while (start < pageList.size()) pageList.remove(start).releaseResources();
    }

    /**
     * @return The preferences page holder with the main preferences page initially showing
     */
    public ATitledPanel getPreferencesPage()
    {
        if (mainPreferencesPage == null)
        {
            mainPreferencesPage = new MainPreferencesPage();
            push(mainPreferencesPage);
        }
        return pageHolder;
    }

    /**
     * @return The current preferences page
     * @since 5.7
     */
    public APreferencesPage getCurrentPage()
    {
        return pageHolder.page;
    }

    /**
     * Push the given page onto the stack and show it as the current page.
     *
     * @param page Page to push
     */
    public void push(@Nonnull APreferencesPage page)
    {
        pageIndex += 1;

        releasePages(pageIndex); // trim previous forward pages

        pageList.add(page);
        showPage(page);
    }

    /**
     * Display the given page, replacing any page currently being shown.
     */
    private void showPage(@Nonnull APreferencesPage page)
    {
        // Automatically adjust our width as we change pages.
        final int preferredWidth = page.getPreferredSize().width;
        final int currentWidth = pageHolder.getSize().width;

        if (preferredWidth > currentWidth && currentWidth > 0)
        {
            uiSpaceManager.setComponentSize(pageHolder, new Dimension(preferredWidth, 0));
        }

        page.makeCurrent();
        pageHolder.setPage(page);

        SwingUtilities.invokeLater(() -> {
            pageHolder.invalidate();
            pageHolder.revalidate();
            pageHolder.repaint();
        });
    }

    /**
     * Remove any instances of preference pages that are matched by the given predicate.
     *
     * @param pageMatcher Predicate to match
     * @since 5.8
     */
    public void removePages(@Nonnull Predicate<APreferencesPage> pageMatcher)
    {
        final var currentPage = pageHolder.page;

        int i = 0;
        while (i < pageList.size() - 1)
        {
            final var page = pageList.get(i);

            // Skip the currently showing page, we'll handle that after this loop
            if (page != currentPage && pageMatcher.test(page))
            {
                pageList.remove(i).releaseResources();
                if (i <= pageIndex) pageIndex--;
            } else
            {
                i++;
            }
        }

        if (pageMatcher.test(currentPage))
        {
            pageList.remove(pageIndex).releaseResources();
            goBackward();
        }
    }

    /**
     * @return {@code true} if the user can navigate backward
     */
    public boolean canGoBackward()
    {
        return pageIndex > 0;
    }

    /**
     * Go backward in the page history.
     * @throws IllegalStateException if there is no "backward" page
     */
    public void goBackward()
    {
        if (!canGoBackward()) throw new IllegalStateException("can't go backward");
        showPage(pageList.get(--pageIndex));
    }

    /**
     * @return {@code true} if the user can navigate forward
     */
    public boolean canGoForward()
    {
        return pageIndex < pageList.size() - 1;
    }

    /**
     * Go forward in the page history.
     * @throws IllegalStateException if there is no "forward" page
     */
    public void goForward()
    {
        if (!canGoForward()) throw new IllegalStateException("can't go forward");
        showPage(pageList.get(++pageIndex));
    }

    /**
     * Side panel that holds the current preference page.
     */
    private static class PageHolder extends ATitledPanel
    {
        private APreferencesPage page;

        PageHolder()
        {
            super("Settings");
            setLayout(new BorderLayout());
        }

        void setPage(@Nonnull APreferencesPage page)
        {
            this.page = page;
            removeAll();
            add(page, BorderLayout.CENTER);
        }
    }

    /**
     * Listener to add mouse tracking for tile highlighting and generic back/forward mouse button handling across all
     * preferences pages, no matter which one is showing.
     */
    private class NavigationAndHighlightHandler implements AWTEventListener
    {
        @Override
        public void eventDispatched(AWTEvent event)
        {
            final int eventID = event.getID();

            if (!pageHolder.isShowing() || (eventID != MouseEvent.MOUSE_CLICKED && eventID != MouseEvent.MOUSE_MOVED)) return;

            final var mouseEvent = (MouseEvent) event;
            final Point point = mouseEvent.getLocationOnScreen();
            SwingUtilities.convertPointFromScreen(point, pageHolder);

            // Only respond if the mouse is within the bounds of the preferences page holder
            if (!pageHolder.contains(point))
            {
                // Clear any highlighting when the mouse moves out of the page holder
                if (eventID == MouseEvent.MOUSE_MOVED) pageHolder.page.mouseExited();
                return;
            }

            if (eventID == MouseEvent.MOUSE_CLICKED)
            {
                handleMouseClick(mouseEvent);
            } else
            {
                pageHolder.page.mouseOver(mouseEvent);
            }
        }

        private void handleMouseClick(MouseEvent mouseEvent)
        {
            final boolean isBackButton = MouseButtonUtils.isBackButton(mouseEvent);
            final boolean isForwardButton = MouseButtonUtils.isForwardButton(mouseEvent);
            if (!isBackButton && !isForwardButton) return;

            mouseEvent.consume();

            if (isBackButton && canGoBackward())
            {
                goBackward();
            } else if (isForwardButton && canGoForward())
            {
                goForward();
            }
        }
    }
}
