package gov.takx.core.mapobject.gui;

import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JFrame;

import java.awt.Window;

/**
 * Abstract base for dialogs associated with a Map Object.  The dialog will automatically close if the
 * map object is deleted/suppressed, becomes non-displayable, or if the project closes.
 *
 * @since 2.37
 */
public abstract class AMapObjectDialog extends AStandardDialog implements IMapObjectDialog
{
    protected final IMapObjectController mapObjectController;

    /**
     * Construct with a default owner.
     *
     * @param mapObjectController MO Controller
     */
    protected AMapObjectDialog(IMapObjectController mapObjectController)
    {
        this(JFrame.getFrames()[0], mapObjectController);
    }

    /**
     * Construct with the given owner.
     *
     * @param owner               Owning window
     * @param mapObjectController MO Controller
     */
    protected AMapObjectDialog(Window owner, IMapObjectController mapObjectController)
    {
        super(owner);
        this.mapObjectController = mapObjectController;
        mapObjectController.getMapObject().addDeletionHandler(mo -> dispose());
    }

    @Override
    public IMapObjectController getMapObjectController()
    {
        return mapObjectController;
    }
}
