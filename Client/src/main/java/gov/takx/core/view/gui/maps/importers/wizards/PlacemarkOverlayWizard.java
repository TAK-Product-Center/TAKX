package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayManager;
import gov.takx.core.placemarkoverlay.gui.AddNewPlacemarkOverlayDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Internal point placemark import wizard.
 *
 */
@PseudoBean
public class PlacemarkOverlayWizard extends AFileImportWizard
{
    public static final String TYPE = "Placemark Overlay Importer";

    @Inject
    private PlacemarkOverlayManager placemarkOverlayManager;

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return "Placemark Overlay";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "placemark.png");
    }

    @Override
    public String getHelpMessage()
    {
        return "Creates icons on the map for each geo-referenced data point (\"placemark\") defined in the file. " +
                "You may customize the data displayed for each placemark and link them to images.";
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        return fileExtension.equals("csv") || fileExtension.equals("txt");
    }

    @Override
    @InvokeOnEDT
    public void runOperation(final @Nullable Map<String, Object> preconfigProps)
    {
        AddNewPlacemarkOverlayDialog dialog = placemarkOverlayManager.getAddNewPlacemarkOverlayDialog();
        dialog.setSelectedFile(file);
        dialog.setVisible(true);
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }
}
