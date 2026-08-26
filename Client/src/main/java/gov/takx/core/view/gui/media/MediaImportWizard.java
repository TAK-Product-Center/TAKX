package gov.takx.core.view.gui.media;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.MediaType;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.maps.importers.wizards.AFileImportWizard;
import gov.takx.platform.ui.UIResources;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Import wizard to handle supported media types.
 *
 * @see MediaType
 * @since 3.5
 */
@PseudoBean
public class MediaImportWizard extends AFileImportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    IMediaManager mediaManager;

    @Inject
    IExtendedUISpaceManager uiSpaceManager;

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "media.png");
    }

    @Override
    public String getDisplayName()
    {
        return "Media";
    }

    @Override
    public String getHelpMessage()
    {
        return "Import the file as a Media object to play back as audio/video";
    }

    @Override
    public String getType()
    {
        return "Media File Importer";
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        return MediaType.getMediaTypeForExtension(fileExtension) != MediaType.Unknown;
    }

    @Override
    public void runOperation(Map<String, Object> ignored)
    {
        logger.debug("Importing: {}", file);

        try
        {
            mediaManager.addMediaFromLocalFile(file, "user");

            // Open the Media panel for the user
            uiSpaceManager.showComponent(CoreComponentType.MEDIA_PANEL);

        } catch (IllegalArgumentException e)
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Unsupported media extension for file: " + file, "Unsupported Media Type");
        }
    }

    @Override
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }
}
