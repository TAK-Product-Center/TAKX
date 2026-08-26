package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import gov.takx.api.ui.IResourceCleaner;

import javax.swing.JPanel;

/**
 */
public interface IMapObjectPanel extends IResourceCleaner
{
    JPanel getPanel();

    /**
     * Register a close handler.  The panel will invoke this when it wants to "close", typically in response
     * to a close/cancel button being clicked.
     *
     * @param closeHandler Handler to invoke when the panel wants to close
     * @since 2.37
     */
    void setCloseHandler(Runnable closeHandler);
}
