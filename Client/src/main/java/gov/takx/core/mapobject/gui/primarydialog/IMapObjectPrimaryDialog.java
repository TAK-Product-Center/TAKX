package gov.takx.core.mapobject.gui.primarydialog;

import gov.takx.core.mapobject.gui.IMapObjectDialog;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.IMapObjectPanel;

/**
 * Contract for MO primary dialogs.
 *
 * @since 2.37
 */
public interface IMapObjectPrimaryDialog<T extends IMapObjectPanel> extends IMapObjectDialog
{
    /**
     * Show the dialog.
     */
    void openDialog();
}
