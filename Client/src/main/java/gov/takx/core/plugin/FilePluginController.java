package gov.takx.core.plugin;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.FileMapObjectPrimaryPanel;
import gov.takx.platform.KeyUtils;
import jakarta.inject.Inject;

/**
 * Plugin controller for File Map Objects.  Provides the custom primary panel.
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class FilePluginController extends DefaultDevicePluginController
{
    @Inject
    private IMapObjectManager mapObjectManager;

    /**
     * @param unitId device unit id
     */
    public FilePluginController(String unitId)
    {
        super(KeyUtils.FILE_FAMILY, IFileMapObject.FILE_TYPE, unitId);
    }

    @Override
    public IRaptorPanel getPrimaryPanel()
    {
        return new FileMapObjectPrimaryPanel(mapObjectManager.getMapObjectController(getUniqueKey()));
    }
}
