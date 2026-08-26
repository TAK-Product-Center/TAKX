package gov.takx.core.mapobject;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.plugin.device.IDevicePlugin;

/**
 */
@PseudoBean
public class FileMapObjectController extends MapObjectController
{
    /**
     * @param mapObjectPlugin      Plugin defining the map object
     * @param clientMapObject      map object we will control
     * @param dataPortalController data portal controller
     */
    public FileMapObjectController(IDevicePlugin mapObjectPlugin, IFileMapObject clientMapObject,
                                   IRaptorDataPortalController dataPortalController)
    {
        super(mapObjectPlugin, clientMapObject, dataPortalController);
    }

    @Override
    protected void deleteSupplementalData()
    {
        final var fmo = (IFileMapObject) mapObject;

        fmo.getVfs().deleteFile(fmo.getVfsFile());

        super.deleteSupplementalData();
    }
}
