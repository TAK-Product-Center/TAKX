package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.core.globe.layers.LayerImportParameters;
import gov.takx.core.view.gui.maps.importers.MapsforgeMapFileImporter;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Map;

import static gov.takx.platform.io.panels.CommonWizardSettings.CANCEL_IMPORT;
import static gov.takx.platform.io.panels.CommonWizardSettings.COPY_TO_VFS;

/**
 * For import of Mapsforge .map files, they turn into a layer showing the map data.
 */
public class MapsforgeFileWizard extends AFileImportWizard
{
    public static final String TYPE = "Mapsforge Map File Importer";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return "MapsForge";
    }

    @Override
    public String getHelpMessage()
    {
        return "Imports MapsForge files and adds the imagery data they contain to the globe.";
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        return fileExtension.equalsIgnoreCase("map");
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

        new MapsforgeMapFileImporter(file, new LayerImportParameters(copyToVfs)).beginImport();
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return TYPE;
    }
}
