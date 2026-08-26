package gov.takx.core.view.gui.docking;

import bibliothek.gui.DockStation;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.ScreenDockStation;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CExternalizeArea;
import bibliothek.gui.dock.common.theme.eclipse.CommonEclipseThemeConnector;

/**
 * Eclipse theme connector to disable the display of tabs when dockables are externalized
 *
 * @since 2.15
 */
public class ExternalizedTitleBarThemeConnector extends CommonEclipseThemeConnector
{
    /**
     * Creates a new connector.
     *
     * @param control the control in whose realm this connector works, not {@code null}
     */
    public ExternalizedTitleBarThemeConnector(CControl control)
    {
        super(control);
    }

    @Override
    public TitleBar getTitleBarKind(DockStation parent, Dockable dockable)
    {
        if (parent instanceof ScreenDockStation || parent instanceof CExternalizeArea)
        {
            // For externalized windows, showing the tab is stupid
            return TitleBar.BASIC_BORDERED;
        }

        return super.getTitleBarKind(parent, dockable);
    }
}
