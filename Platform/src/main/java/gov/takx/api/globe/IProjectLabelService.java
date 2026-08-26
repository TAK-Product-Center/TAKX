package gov.takx.api.globe;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Displays static, floating text labels at 6 predefined locations on the screen.
 *
 * @since 1.4
 */
@InjectableService
@NoPluginImplementations
public interface IProjectLabelService
{
    /**
     * Identifies the current visibility of the project label layer, as defined by the user.
     *
     * @return true, if the project label layer is presently visible
     */
    boolean isProjectLabelLayerVisible();

    /**
     * Set the visibility of the project labels layer.
     *
     * @param visible New visibility setting
     */
    void setProjectLabelLayerVisible(boolean visible);

    /**
     * Lookup the project label for the given location.
     *
     * @param location Label Location
     * @return Project Label with its current values/settings
     */
    @Nonnull
    IProjectLabel getLabel(@Nonnull ProjectLabelLocation location);

    /**
     * Redefines the project label at the given location.  Even if the project label's visibility is set to true, if
     * the user has hidden the Project Label globe layer, the label will not show up on the globe.
     *
     * @param location Location to define
     * @param label    Label to use at the defined location
     */
    void setLabel(@Nonnull ProjectLabelLocation location, @Nonnull IProjectLabel label);
}
