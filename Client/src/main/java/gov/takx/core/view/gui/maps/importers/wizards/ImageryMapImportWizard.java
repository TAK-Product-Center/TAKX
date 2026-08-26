package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.core.globe.imagerywizard.ImagerySetFilter;
import gov.takx.core.view.gui.maps.LoadImageryFilesDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Wizard for importing imagery data.
 *
 */
public class ImageryMapImportWizard extends AMapImportWizard
{
    public static final String TYPE = "maps_imagery";

    /** @since 5.2 **/
    private static final ImagerySetFilter IMAGERY_SET_FILTER = new ImagerySetFilter();

    public ImageryMapImportWizard()
    {
        super(IMAGERY_SET_FILTER::accept);
    }

    @Override
    public String getHelpMessage()
    {
        return "Imports imagery that will be displayed as new layer(s) on the globe.";
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        new LoadImageryFilesDialog(files.toArray(new File[0])).setVisible(true);
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
        return "Imagery Import";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "imagery_overlay.png");
    }
}
