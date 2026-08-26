package gov.takx.core.view.gui.maps.importers;

import gov.nasa.worldwind.layers.RenderableLayer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.core.globe.layers.LayerImportParameters;
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
public class OverlayImageImporter implements IImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IRaptorLayerManager layerManager;

    private final File imageFile;
    private final ImportThread importThread;
    private final LayerImportParameters importParameters;

    public OverlayImageImporter(@Nonnull File imageFile, @Nonnull LayerImportParameters importParameters)
    {
        this.imageFile = imageFile;
        this.importParameters = importParameters;

        importThread = new ImportThread();
    }

    /**
     * Import a surface overlay image
     */
    @Override
    public void beginImport()
    {
        if (!importThread.isAlive())
        {
            importThread.start();
        }
    }

    private class ImportThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                String restorableState = new SurfaceImageryFileImporter(importParameters).importSurfaceImageryFile(imageFile);

                IRaptorLayer newLayer = layerManager.addLayer(
                        imageFile.getName(),
                        imageFile.getName(),
                        RenderableLayer.class.getName(),
                        true,
                        true,
                        null,
                        LayerGroup.SURFACE_IMAGERY,
                        null,
                        restorableState,
                        false);
                newLayer.setEditable(true);
            } catch (Exception e)
            {
                // Drill down to the original exception to get its message.
                Throwable cause = e;
                while (cause.getCause() != null)
                {
                    cause = cause.getCause();
                }
                logger.error("Unable to import {}:\n {}", imageFile, cause.getMessage(), e);
            }
        }
    }
}
