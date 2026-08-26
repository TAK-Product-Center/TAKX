package gov.takx.core.globe;

import gov.takx.api.globe.IProjectLabel;
import gov.takx.api.globe.IProjectLabelService;
import gov.takx.api.globe.ProjectLabelLocation;
import gov.takx.core.globe.widgets.ProjectLabelWidgetManager;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Manages the project label display. Delegates all operations to {@link ProjectLabelWidgetManager}.
 *
 * @since 1.4
 */
@ApplicationScoped
public class ProjectLabelService implements IProjectLabelService
{
    private final ProjectLabelWidgetManager projectLabelWidgetManager;

    @Inject
    public ProjectLabelService(ProjectLabelWidgetManager projectLabelWidgetManager)
    {
        this.projectLabelWidgetManager = projectLabelWidgetManager;
    }

    @Nonnull
    @Override
    public IProjectLabel getLabel(@Nonnull ProjectLabelLocation location)
    {
        return projectLabelWidgetManager.getProjectLabel(location);
    }

    @Override
    public boolean isProjectLabelLayerVisible()
    {
        return projectLabelWidgetManager.isVisible();
    }

    @Override
    public void setProjectLabelLayerVisible(boolean visible)
    {
        projectLabelWidgetManager.setVisible(visible);
    }

    @Override
    public void setLabel(@Nonnull ProjectLabelLocation location, @Nonnull IProjectLabel label)
    {
        projectLabelWidgetManager.setProjectLabel(location, label);
    }
}
