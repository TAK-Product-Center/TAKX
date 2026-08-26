package gov.takx.core.view.gui.maps.importers.wizards;

import gov.tak.api.util.TakPreferenceKeys;
import gov.tak.platform.datapackage.PackageImporter;
import gov.tak.platform.datapackage.PackageReader;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Import a zip file containing preferences and certificates for a TAK Server connection.
 * <p>
 * This is a fallback importer that handles zip files that don't contain a proper Data Package manifest, but
 * do contain a preferences file, which is typically used to import TAK Server connections.  Any certificates,
 * {@code .p12} files, will also be imported.
 *
 * @since 3.2
 */
@PseudoBean
public class TakServerImportWizard extends AFileImportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TYPE = "TAK Server Connection Importer";
    private static final URL ICON_URL = UIResources.getUrl(PLATFORM_ICONS, "cloud_connected.png");

    @Inject
    private IUserProxy userProxy;

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return "TAK Server";
    }

    @Override
    public String getHelpMessage()
    {
        return "Imports TAK Server connection preferences";
    }

    @Override
    public URL getIcon()
    {
        return ICON_URL;
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }

    @Override
    public boolean supportsMultipleFiles()
    {
        return true;
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        // Must be a zip file and contain a preferences file
        if (!StringUtils.endsWithIgnoreCase(file.getName(), ".zip")) return false;

        try (PackageReader packageReader = new PackageReader(file))
        {
            final var preferences = packageReader.getPreferences();
            return preferences != null && preferences.findByName(TakPreferenceKeys.COT_STREAMS) != null;
        } catch (IOException e)
        {
            return false;
        }
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> configuration)
    {
        final StringBuilder sb = new StringBuilder();

        for (File file : files)
        {
            try
            {
                importConnections(file);
            } catch (Exception e)
            {
                logger.error("Import of file '{}' failed", file, e);
                sb.append('\n').append(file.getName()).append(": ").append(e.getMessage());
            }
        }

        if (!sb.isEmpty())
        {
            final String message = "TAK Server connection import failed with the following errors:\n" + sb;
            userProxy.reportError("TAKServer Connection Import Failed", message, null);
            throw new RuntimeException(message);
        }
    }

    @UseClientClassLoader // We can be invoked on a random executor thread
    private void importConnections(File packageFile) throws Exception
    {
        new PackageImporter(packageFile).importTakServerConnection();
    }
}
