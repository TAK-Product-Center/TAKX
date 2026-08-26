package gov.takx.core.view.gui.maps.importers;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.core.globe.layers.ConfigFileSupport;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class WWXMLImporter implements IImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IRaptorLayerManager layerManager;

    private final ConfigFileProducer producer;

    /**
     * @param layerGroup Layer group in which to create the new layer
     * @param fileName WW XML source file name
     * @throws IllegalArgumentException If {@code fileName} is empty
     */
    public WWXMLImporter(@Nonnull LayerGroup layerGroup, @Nonnull String fileName)
    {
        if (fileName.isEmpty()) throw new IllegalArgumentException("'fileName' must not be empty");

        producer = new ConfigFileProducer(layerGroup, fileName);
    }

    @Override
    public void beginImport()
    {
        if (!producer.isAlive())
        {
            producer.start();
        }
    }

    /**
     * This class handles the actual import of the configuration file into VFS and then generating the layer
     * off of that config file.  The layer group is provided so this class can produce both imagery layers and
     * elevation models. This is all done on a background thread with a progress display.
     */
    private class ConfigFileProducer extends Thread
    {
        private final LayerGroup layerGroup;
        private final String fileName;

        /**
         * Construct the deferred result with the given layer group.
         *
         * @param layerGroup The layer group to use for creating the raptor layer.
         * @param fileName   The name of the config file to import
         */
        public ConfigFileProducer(@Nonnull LayerGroup layerGroup, @Nonnull String fileName)
        {
            this.layerGroup = layerGroup;
            this.fileName = fileName;
        }

        @Override
        public void run()
        {
            try
            {
                File file = new File(fileName);
                String restorableState = new ConfigFileSupport().importXMLConfigFile(file);

                layerManager.addLayerFromXML(file.getName(), restorableState, layerGroup);
            } catch (Exception e)
            {
                logger.error("Failed to import XML configuration file", e);

                // Drill down to the original exception to get its message.
                Throwable cause = e;
                while (cause.getCause() != null)
                {
                    cause = cause.getCause();
                }

                logger.error("Unable to import {}:\n {}", fileName, cause.getMessage());
                return;
            }
            logger.debug("completed XML layer import");
        }
    }
}
