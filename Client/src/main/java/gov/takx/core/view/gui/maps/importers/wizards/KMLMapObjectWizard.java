package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.kml.KmlMapObjectImporter;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.io.File;
import java.util.Map;

/**
 * For import of KML files which were exported as MapObjects.
 */
@PseudoBean
public class KMLMapObjectWizard extends AFileImportWizard
{
    public static final String TYPE = "kml_map_objects";

    @Inject
    private KmlMapObjectImporter kmlMapObjectImporter;

    @Override
    public String getDisplayName()
    {
        return "KML/Z Tracks";
    }

    @Override
    public String getHelpMessage()
    {
        return "Import device tracks from a file created by " +
                PreferenceEntry.NAME.getStringValue() +
                " into a new or existing device icon as track data points.";
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        return fileExtension.equalsIgnoreCase("kml") || fileExtension.equalsIgnoreCase("kmz");
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> preconfigProps)
    {
        kmlMapObjectImporter.importKML(file);
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
}
