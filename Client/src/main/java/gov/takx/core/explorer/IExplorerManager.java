package gov.takx.core.explorer;

import gov.takx.api.explorer.IExplorerManagerDelegate;
import gov.takx.core.view.gui.ATitledPanel;

/**
 * Interface for the explorer manager.  The explorer manager is responsible for tracking and managing explorable
 * objects via its controllers.  Its view provides the user-facing front end.
 *
 * @since 1.48
 */
public interface IExplorerManager extends IExplorerManagerDelegate
{
    /**
     * Get the view component for this explorer manager.
     *
     * @return the view component for this explorer manager.
     */
    ATitledPanel getViewComponent();

    @Override
    IExplorer getDefaultExplorerDelegate();
}
