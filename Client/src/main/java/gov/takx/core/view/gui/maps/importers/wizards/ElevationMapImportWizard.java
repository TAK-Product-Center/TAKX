package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.core.globe.imagerywizard.ElevationFileSetFilter;
import gov.takx.core.view.gui.maps.LoadElevationFilesDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Wizard for importing elevation data.
 *
 */
public class ElevationMapImportWizard extends AMapImportWizard
{
    public static final String TYPE = "maps_elevation";

    /** @since 5.2 **/
    private static final ElevationFileSetFilter ELEVATION_FILE_SET_FILTER = new ElevationFileSetFilter();

    public ElevationMapImportWizard()
    {
        super(ELEVATION_FILE_SET_FILTER::accept);
    }

    @Override
    public String getHelpMessage()
    {
        return "Imports elevation data that will affect the 3D display of terrain and calculations involving elevation.";
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        new LoadElevationFilesDialog(files.toArray(new File[0])).setVisible(true);

        return null;
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return "Elevation Import";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "elevation_server.png");
    }
}
