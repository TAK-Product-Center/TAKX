package gov.takx.core.view.gui.preferences;

import gov.takx.core.preferences.ToolPreferenceManager.ToolDefinition;
import gov.takx.core.preferences.tool.ToolPreferenceRemoved;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.function.Predicate;

/**
 * Controller for Tool Preferences pages.  Supports creation and display of a preferences page for Tools defined
 * with {@link gov.takx.api.preferences.IToolPreferenceManager}.
 * 
 * @since 5.8
 * @see gov.takx.api.preferences.IToolPreferenceManager
 */
@ApplicationScoped
public class ToolPreferencesPagesController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static Constructor<ToolPreferencesPage> PAGE_CONSTRUCTOR;
    static
    {
        // Find the ToolPreferencesPage constructor
        try
        {
            PAGE_CONSTRUCTOR = ToolPreferencesPage.class.getDeclaredConstructor(ToolDefinition.class);
            PAGE_CONSTRUCTOR.setAccessible(true);
        } catch (NoSuchMethodException e)
        {
            logger.error("Failed to find constructor for ToolPreferencesPage", e);
        }
    }

    private final PreferencePagesController preferencePagesController;
    private final IExtendedUISpaceManager uiSpaceManager;

    @Inject
    ToolPreferencesPagesController(PreferencePagesController preferencePagesController, IExtendedUISpaceManager uiSpaceManager)
    {
        this.preferencePagesController = preferencePagesController;
        this.uiSpaceManager = uiSpaceManager;
    }

    /**
     * Construct a {@link ToolPreferencesPage} for the given tool definition and push it onto the page stack.
     * The Settings panel is made visible, if it is not already showing.
     *
     * @param toolDefinition Definition of the tool preferences page to display
     */
    public void showToolPage(@Nonnull ToolDefinition toolDefinition)
    {
        uiSpaceManager.showComponent(CoreComponentType.PREFERENCES_PANEL);

        // Don't push a new instance if the desired page is already showing
        if (getPageMatcher(toolDefinition).test(preferencePagesController.getCurrentPage())) return;

        try
        {
            preferencePagesController.push(PAGE_CONSTRUCTOR.newInstance(toolDefinition));
        } catch (ReflectiveOperationException e)
        {
            logger.error("Failed to create preferences page for tool '{}'", toolDefinition.title(), e);
        }
    }

    /**
     * Remove a tool definition.
     */
    private void onToolPreferenceRemoved(@Observes ToolPreferenceRemoved event)
    {
        // Remove any instances of the tool's preferences page
        preferencePagesController.removePages(getPageMatcher(event.toolDefinition()));
    }

    /**
     * @return a predicate that matches our page type and the given tool definition
     */
    @Nonnull
    private static Predicate<APreferencesPage> getPageMatcher(@Nonnull ToolDefinition toolDefinition)
    {
        return page -> page instanceof ToolPreferencesPage toolPreferencesPage
                && toolPreferencesPage.getToolDefinition() == toolDefinition;
    }
}
