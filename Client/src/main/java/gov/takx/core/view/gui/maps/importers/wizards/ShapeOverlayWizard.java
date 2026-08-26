package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.commons.file.FileUtils;
import gov.takx.core.globe.layers.ShapefileImportHandler;
import gov.takx.core.view.gui.maps.AddOverlayController;
import gov.takx.core.view.gui.maps.AddShapeOverlayController;
import gov.takx.platform.ui.UIResources;

import java.io.File;
import java.net.URL;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Importer for shape file layers.
 *
 */
public class ShapeOverlayWizard extends AOverlayWizard
{
    public static final String TYPE = "shp_overlay";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    protected AddOverlayController getController(File file)
    {
        // If the user dropped a related file, but not the SHP file, then fixup the name
        final File shpFile = "shp".equalsIgnoreCase(FileUtils.getExtension(file))
                ? file
                : new File(file.getParentFile(), FileUtils.getNameWithoutExtension(file) + ".shp");

        return new AddShapeOverlayController(shpFile);
    }

    @Override
    public String getDisplayName()
    {
        return "SHP Overlay";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "shp_overlay.png");
    }

    @Override
    public String getHelpMessage()
    {
        return "Processes shape files that contain 3D shape data and draws those shapes on the map.";
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        return ShapefileImportHandler.SHAPE_FILE_EXTENSIONS.contains(fileExtension.toLowerCase());
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
}
