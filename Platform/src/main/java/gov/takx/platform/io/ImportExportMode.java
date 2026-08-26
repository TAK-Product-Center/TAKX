package gov.takx.platform.io;

import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportExportWizard;

import java.util.function.Function;

/**
 * Specifies the operation being performed, import or export.
 *
 */
public enum ImportExportMode
{
    IMPORT("Import", IImportExportPlugin::createImporter),
    EXPORT("Export", IImportExportPlugin::createExporter);

    private final String displayName;
    private final Function<IImportExportPlugin, IImportExportWizard> wizardCreator;

    /**
     * @param displayName   Display name
     * @param wizardCreator Function to create the appropriate wizard from a plugin instance
     * @since 2.37
     */
    ImportExportMode(String displayName, Function<IImportExportPlugin, IImportExportWizard> wizardCreator)
    {
        this.displayName = displayName;
        this.wizardCreator = wizardCreator;
    }

    /**
     * @return The capitalized name of this mode
     * @since 2.37
     */
    public String displayName()
    {
        return displayName;
    }

    /**
     * @return Function to create the appropriate wizard from a plugin instance
     * @since 2.37
     */
    public Function<IImportExportPlugin, IImportExportWizard> getWizardCreator()
    {
        return wizardCreator;
    }
}
