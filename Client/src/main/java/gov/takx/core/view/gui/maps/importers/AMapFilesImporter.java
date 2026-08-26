package gov.takx.core.view.gui.maps.importers;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.ILayerService;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.globe.tak.CacheConflictException;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 3.4.1 (renamed from AConvertMapsImporter)
 */
@PseudoBean
public abstract class AMapFilesImporter implements IImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    protected IRaptorLayerManager raptorLayerManager;
    @Inject
    private ILayerService layerService;

    private final Map<String, Object> dataMap;
    private final List<LayerConfiguration> layersWithNameConflicts = new ArrayList<>();
    private final Map<LayerConfiguration, CacheConflictException> layersWithCacheConflicts = new HashMap<>();

    protected AMapFilesImporter(Map<String, Object> dataMap)
    {
        this.dataMap = dataMap;
    }

    @Override
    public abstract void beginImport();

    protected Map<String, Object> getDataMap()
    {
        return dataMap;
    }

    /**
     * Create a layer under the given {@code name} according to {@code configuration}. Record any name and cache
     * location conflicts to be reported later by {@link #reportConflicts()}.
     *
     * @since 5.5
     */
    protected void createLayer(LayerConfiguration configuration, String name)
    {
        if (raptorLayerManager.getLayer(name) == null)
        {
            try
            {
                layerService.addLayer(configuration);
            } catch (CacheConflictException e)
            {
                layersWithCacheConflicts.put(configuration, e);
            }
        } else
        {
            layersWithNameConflicts.add(configuration);
        }
    }

    /**
     * Show the user any conflicts that occurred during the import.
     * @since 5.5
     */
    protected void reportConflicts()
    {
        if (layersWithCacheConflicts.isEmpty() && layersWithNameConflicts.isEmpty()) return;

        final var sb = new StringBuilder();

        if (!layersWithNameConflicts.isEmpty())
        {
            logger.warn("Failed to create layers due to name conflicts: {}", layersWithNameConflicts);
            sb.append("The following layer(s) conflict with existing layers with the same name:\n");
            layersWithNameConflicts.forEach(layer -> sb.append(layer.name()).append("\n"));
        }

        if (!layersWithCacheConflicts.isEmpty())
        {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("The following layer(s) conflict with existing layer cache locations:\n");
            layersWithCacheConflicts.forEach((layer, cce) -> {
                logger.warn("Layer cache conflict: {} : {}", layer, cce.getMessage());
                sb.append(layer.name()).append(" ").append(cce.getMessage()).append("\n");
            });
        }

        AlwaysOnTopJOptionPane.showErrorDialog(sb.toString(), "Layer Creation Failures");
    }
}
