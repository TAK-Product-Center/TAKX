package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.core.view.gui.maps.AddKMLOverlayController;
import gov.takx.core.view.gui.maps.AddOverlayController;
import gov.takx.platform.ui.UIResources;

import java.io.File;
import java.net.URL;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Import wizard for KML layer files.
 *
 */
public class KMLOverlayWizard extends AOverlayWizard
{
    public static final String TYPE = "kml_overlay";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    protected AddOverlayController getController(File file)
    {
        return new AddKMLOverlayController(file);
    }

    @Override
    public String getDisplayName()
    {
        return "KML/Z Overlay";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "kml_overlay.png");
    }

    @Override
    public String getHelpMessage()
    {
        return "Import a KML or KMZ file as a new map overlay layer on the globe, including images, shapes and 3D buildings.";
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        return fileExtension.equalsIgnoreCase("kml") || fileExtension.equalsIgnoreCase("kmz");
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
}
