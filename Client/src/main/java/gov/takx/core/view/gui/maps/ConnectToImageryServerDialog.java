package gov.takx.core.view.gui.maps;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.LayerGroup;
import gov.takx.core.globe.imagerywizard.FileStoreUtil;
import gov.takx.core.globe.imagerywizard.LayerInfo;
import gov.takx.core.globe.imagerywizard.WMSValidator;

/**
 */
@PseudoBean
public class ConnectToImageryServerDialog extends ConnectToWMSServerDialog
{
    private static final WMSValidator WMS_VALIDATOR = new WMSValidator(new String[]{"image/dds", "image/png", "image/jpeg"});

    public ConnectToImageryServerDialog()
    {
        super("Choose an existing Web Map Service (WMS) server containing layers of imagery or maps to be displayed on the globe.",
                "imagery_server.png",
                LayerGroup.IMAGERY, FileStoreUtil.WMSType.IMAGERY_WMS_SERVERS, WMS_VALIDATOR);

        setTitle("Connect to Imagery Server");
        refreshComponents();
    }

    @Override
    protected void createAndAddLayer(LayerInfo layerInfo) throws Exception
    {
        globeManager.getPrimaryGlobeDelegate().addImageryLayer(layerInfo.createWWLayer());
    }
}
