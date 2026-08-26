package gov.takx.core.view.gui.maps.importers.wizards;

import gov.tak.platform.datapackage.PackageImporter;
import gov.tak.platform.datapackage.PackageReader;
import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Import a zip file containing a TAK Data Package.
 *
 * @since 3.0
 */
public class DataPackageImportWizard extends AFileImportWizard
{
    public static final String TYPE = "TAK Data Package Importer";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return "Data Package";
    }

    @Override
    public String getHelpMessage()
    {
        return "Imports TAK Data Packages";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "package.png");
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
        // Must be a zip file and contain a Data Package manifest
        if (!StringUtils.endsWithIgnoreCase(file.getName(), ".zip")) return false;

        try (PackageReader packageReader = new PackageReader(file))
        {
            return packageReader.hasPackageManifest();
        } catch (IOException e)
        {
            return false;
        }
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> configuration)
    {
        files.forEach(this::importPackage);
    }

    @UseClientClassLoader // We can be invoked on a random executor thread
    private void importPackage(File packageFile)
    {
        try
        {
            new PackageImporter(packageFile).importPackage();
        } catch (Exception e)
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Import failed: " + e.getMessage(), "Import failed");
            throw new RuntimeException("Import failed", e);
        }
    }
}
