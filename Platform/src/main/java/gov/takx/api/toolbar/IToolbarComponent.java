package gov.takx.api.toolbar;

import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.UUID;

/**
 * Interface representing a component that can be added to an {@link IToolbarTab} for display on the application
 * toolbar. To create a toolbar component, use the creation methods on {@link IToolbarManager}.
 *
 * @since 3.4
 */
@NoPluginImplementations
public interface IToolbarComponent
{
    /**
     * @return The unique id for the component.
     */
    @Nonnull
    UUID getId();

    /**
     * @return The unique key of the plugin that created this component, or {@code null} if created by a user or TAKX.
     */
    @Nullable
    String getPluginUniqueKey();

    /**
     * @return True if the component is in a valid state to be added to the Swing toolbar. This may be false if the
     * toolbar component was created by a plugin that is not currently installed in the application. When a component is
     * not valid, it will remain in the toolbar configuration (so any customization by the user/plugins can be retained)
     * but will not be present in the toolbar user interface.
     */
    boolean isValid();

    /**
     * Creates a copy of itself. This should be used to create a copy of a component before attempting to add it to
     * additional tabs, since a component instance can only be added to a single tab.
     *
     * @return The new component that is a copy of the instance on which the call was made.
     */
    @Nonnull
    IToolbarComponent copy();
}
