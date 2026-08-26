package gov.takx.core.view.gui.maps.importers;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.core.globe.layers.LayerImportParameters;
import gov.takx.core.globe.layers.mapsforge.MapsForgeLayerFactory;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.io.File;

/**
 * Importer that handles mapsforge ".map" files.  Copy the file into VFS and then construct a layer out of it.
 *
 * @since 1.35
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapsforgeMapFileImporter implements IImporter
{
    private final File mapFile;
    private final LayerImportParameters importParameters;

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    private MapsForgeLayerFactory layerFactory;

    public MapsforgeMapFileImporter(@Nonnull File mapFile, LayerImportParameters importParameters)
    {
        this.mapFile = mapFile;
        this.importParameters = importParameters;
    }

    /**
     * Import a surface overlay image
     */
    @Override
    public void beginImport()
    {
        executorService.execute(() -> layerFactory.makeLayerFromMapFile(mapFile, importParameters));
    }
}
