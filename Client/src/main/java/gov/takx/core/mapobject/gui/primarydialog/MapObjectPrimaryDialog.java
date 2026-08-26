package gov.takx.core.mapobject.gui.primarydialog;

import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.AMapObjectPanel;

/**
 * A dialog that can be used to display custom MapObjectPanels.
 *
 * @since 3.0.1
 */
public class MapObjectPrimaryDialog<T extends AMapObjectPanel> extends AMapObjectPrimaryDialog<T>
{
    public MapObjectPrimaryDialog(IMapObjectController controller, T primaryPanel)
    {
        super(controller, primaryPanel);
    }
}
