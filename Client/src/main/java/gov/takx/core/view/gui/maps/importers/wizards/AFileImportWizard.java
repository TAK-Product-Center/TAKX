package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.AImportExportWizard;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.io.panels.CommonWizardSettings;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Abstract class representing any importer that is not an Import/Export plugin. A no-argument constructor is
 * required, as we may instantiate these wizards via reflection.
 */
public abstract class AFileImportWizard extends AImportExportWizard implements IImportWizard
{
    public static final String FAMILY = "gov.takx.FileImporter";

    /**
     * The file to import.
     */
    protected File file;

    /**
     * The files to import, if this wizard supports multiple files.
     */
    protected List<File> files;

    @Override
    public ImportExportResult doImport(IWizardProperties props, IImportExportProgressMonitor progressMonitor)
            throws Exception
    {
        return null;
    }

    @Override
    public void preconfigureWizard(Map<String, Object> configuration)
    {
        if (supportsMultipleFiles())
        {
            //noinspection unchecked
            files = (List<File>) configuration.get(CommonWizardSettings.FILES);
        } else
        {
            file = (File) configuration.get(CommonWizardSettings.FILE);
        }
    }

    @Override
    public void setFormat(Format format)
    {
    }

    @Override
    public String getFirstWizardPanel()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return getType();
    }

    /**
     * @return Unique key under which to register this importer
     * @since 3.2
     */
    public String getKey()
    {
        return KeyUtils.getUniqueKey(FAMILY, getType());
    }

    /**
     * The importer type.
     *
     * @return The importer type.
     */
    public abstract String getType();

    /**
     * Perform the import.
     * todo: Why aren't we using doImport() like with the other importers?
     *
     * @param preconfigProps The configuration properties.
     * @since 3.0 (parameter type changed)
     */
    public abstract void runOperation(@Nullable Map<String, Object> preconfigProps);

    /**
     * Display the dialog.
     *
     * @return The results to process, if any. May be {@code null}.
     * @throws ImportCancelledException if the user cancelled the import
     * @since 3.0 (return value changed)
     */
    @Nullable
    public abstract Map<String, Object> showImporterWizardDialog() throws ImportCancelledException;
}
