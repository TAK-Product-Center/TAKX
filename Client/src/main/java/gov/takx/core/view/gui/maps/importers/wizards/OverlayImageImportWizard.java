package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.core.globe.layers.LayerImportParameters;
import gov.takx.core.view.gui.maps.importers.OverlayImageImporter;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;

import javax.imageio.ImageIO;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.io.panels.CommonWizardSettings.CANCEL_IMPORT;
import static gov.takx.platform.io.panels.CommonWizardSettings.COPY_TO_VFS;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Wizard used to import imagery as a stretchable layer on the globe.
 */
public class OverlayImageImportWizard extends AFileImportWizard
{

    public static final String TYPE = "Overlay Image Importer";

    /**
     * The formats supported by this wizard.
     */
    private static final String[] FORMATS = ImageIO.getReaderFileSuffixes();

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return "Overlay Image";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "imagery_overlay.png");
    }

    @Override
    public String getHelpMessage()
    {
        return "Creates a stretchable canvas from an image and places it on the globe.";
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        for (String format : FORMATS)
        {
            if (format.equalsIgnoreCase(fileExtension))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> preconfigProps)
    {
        boolean cancelled = preconfigProps != null
                ? (Boolean) preconfigProps.getOrDefault(CANCEL_IMPORT, false)
                : false;
        boolean copyToVfs = preconfigProps != null
                ? (Boolean) preconfigProps.getOrDefault(COPY_TO_VFS, true)
                : true;

        if (cancelled) return;

        new OverlayImageImporter(file, new LayerImportParameters(copyToVfs)).beginImport();
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }
}
