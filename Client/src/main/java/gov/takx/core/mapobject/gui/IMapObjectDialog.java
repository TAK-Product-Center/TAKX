package gov.takx.core.mapobject.gui;

import gov.takx.api.mapobject.IMapObjectController;

import javax.swing.JDialog;

import java.util.function.Consumer;

/**
 * Contract for all dialogs that handle a Map Object.
 *
 * @since 2.37 (not really, but the hierarchy was changed and classes were moved)
 */
public interface IMapObjectDialog
{
    /**
     * Add a handler tpo be invoked when this dialog is closing.
     *
     * @param handler Handler to invoke
     */
    void addCloseHandler(Consumer<JDialog> handler);

    /**
     * @return MO Controller being handled by this dialog
     */
    IMapObjectController getMapObjectController();
}
