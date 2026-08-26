package gov.takx.test.mock.plugin.io;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.plugin.io.ACancellableWizard;
import gov.takx.api.plugin.io.IExportWizard;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.platform.io.panels.CommonWizardSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;

/**
 * Test exporter wizard for handling files of type "fakefile".
 *
 * @since 2.35
 */
public class FakeExportWizard extends ACancellableWizard implements IExportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Performs an export of map objects to a fake file format, which simply includes the latitude, longitude, and
     * elevation separated by a comma. It expects the {@link CommonWizardSettings#SELECTED_MAP_OBJECTS} and
     * {@link CommonWizardSettings#FILE} values to be populated in the preconfiguration settings (the passed in
     * properties are ignored).
     *
     * @param props           Wizard properties.
     * @param progressMonitor Progress monitor that can be used to update the GUI with Import/Export progress.
     */
    @SuppressWarnings("unchecked")
    @Override
    public ImportExportResult doExport(IWizardProperties props, IImportExportProgressMonitor progressMonitor)
    {
        ImportExportResult importExportResult = new ImportExportResult();

        String filename = (String) preconfSettings.get(CommonWizardSettings.FILE);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename)))
        {
            Collection<IMapObject> mapObjects = (Collection<IMapObject>) preconfSettings.get(CommonWizardSettings.SELECTED_MAP_OBJECTS);
            if (mapObjects != null)
            {
                for (IMapObject mapObject : mapObjects)
                {
                    if (mapObject.hasLocation())
                    {
                        ILocation location = mapObject.getLocation();
                        String line = String.join(",",
                                String.valueOf(location.getLatitude()),
                                String.valueOf(location.getLongitude()),
                                String.valueOf(location.getElevation()));
                        writer.write(line);
                        writer.newLine();
                    }

                    importExportResult.incrementMapObjectCount();
                }
            }
            writer.flush();
        } catch (IOException e)
        {
            logger.warn("Writing to file failed", e);
        }

        importExportResult.setSuccess(true);

        return importExportResult;
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
}
