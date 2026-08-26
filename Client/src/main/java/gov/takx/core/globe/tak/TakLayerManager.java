package gov.takx.core.globe.tak;

import com.atakmap.map.Globe;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.MultiLayer;
import gov.tak.api.engine.map.ILayer;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.layer.ITakLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.ui.event.GlobeCreatedEvent;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * An implementation of {@link ITakLayerManager} that allows adding TAK layers to the running globes.
 * @since 5.1
 */
@ApplicationScoped
public class TakLayerManager implements ITakLayerManager
{
    private final ApplicationGlobeManager globeManager;
    private final ImageryLayerManager imageryLayerManager;
    private final ElevationLayerManager elevationLayerManager;
    private final EventBus uiEventBus;

    private final MultiLayer mapDataLayers = new MultiLayer("Map Data Layers");
    private final MultiLayer shapeFileLayers = new MultiLayer("Shape File Layers");
    private final MultiLayer kmlOverlayLayers = new MultiLayer("KML Overlay Layers");
    private final MultiLayer uncategorizedLayers = new MultiLayer("Uncategorized Layers");
    private final MultiLayer pluginLayers = new MultiLayer("Plugin Layers");

    private final List<LayerFactory> layerFactories = new LinkedList<>();
    private final Map<Layer, LayerFactory> layerToLayerFactoriesMap = new HashMap<>();
    private final Map<ILayer, LayerFactory> iLayerToLayerFactoriesMap = new HashMap<>();

    @Inject
    public TakLayerManager(ApplicationGlobeManager applicationGlobeManager, ImageryLayerManager imageryLayerManager,
                           ElevationLayerManager elevationLayerManager, @UiEventBus EventBus uiEventBus)
    {
        globeManager = applicationGlobeManager;
        this.imageryLayerManager = imageryLayerManager;
        this.elevationLayerManager = elevationLayerManager;
        this.uiEventBus = uiEventBus;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices event)
    {
        uiEventBus.register(this);
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices event)
    {
        mapDataLayers.removeAllLayers();
        shapeFileLayers.removeAllLayers();
        kmlOverlayLayers.removeAllLayers();
        uncategorizedLayers.removeAllLayers();
        pluginLayers.removeAllLayers();
        layerFactories.clear();
        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);
    }

    @Subscribe
    public void onGlobeCreated(GlobeCreatedEvent event)
    {
        layerFactories.forEach(f -> f.addLayer(event.getGlobe(), event.getGlobe().getNativeGlobe()));
    }

    @Override
    public void addLayer(IGlobeDelegate globe, Layer layer)
    {
        ((IApplicationGlobe) globe).getNativeGlobe().addLayer(layer);
    }

    @Override
    public void addLayer(IGlobeDelegate globe, ILayer layer)
    {
        ((IApplicationGlobe) globe).getNativeGlobe().addLayer(layer);
    }

    @Override
    public void addLayer(Layer layer)
    {
        LayerFactory layerFactory = (appGlobe, globe) -> globe.addLayer(layer);
        LayerFactory oldFactory = layerToLayerFactoriesMap.put(layer, layerFactory);
        if (oldFactory != null) layerFactories.remove(oldFactory);
        addLayerFactory(layerFactory);
    }

    @Override
    public void addLayer(ILayer layer)
    {
        LayerFactory layerFactory = (appGlobe, globe) -> globe.addLayer(layer);
        LayerFactory oldFactory = iLayerToLayerFactoriesMap.put(layer, layerFactory);
        if (oldFactory != null) layerFactories.remove(oldFactory);
        addLayerFactory(layerFactory);
    }

    /**
     * Add a layer factory to be applied to all globes.
     * @param factory the factory
     * @since 5.3
     */
    public void addLayerFactory(LayerFactory factory)
    {
        layerFactories.add(factory);
        globeManager.getAllGlobeDelegates().forEach(g -> factory.addLayer(g, g.getNativeGlobe()));
    }

    /**
     * Add a shared {@link Layer} to all maps to the specified {@link LayerGroup}.
     * @param layer the layer to add
     * @param layerGroup the layer group
     * @since 5.2
     */
    public void addLayer(Layer layer, LayerGroup layerGroup)
    {
        MultiLayer parentLayer = getParentLayer(layerGroup);
        if (!parentLayer.getLayers().contains(layer))
        {
            parentLayer.addLayer(layer);
        }
    }

    /**
     * Add a shared {@link Layer} to all maps to the specified {@link LayerGroup} in the specified {@code position}.
     * @param layer the layer to add
     * @param layerGroup the layer group
     * @param position the index position
     * @since 5.2
     */
    public void addLayer(Layer layer, LayerGroup layerGroup, int position)
    {
        MultiLayer parentLayer = getParentLayer(layerGroup);
        if (!parentLayer.getLayers().contains(layer))
        {
            parentLayer.addLayer(position, layer);
        }
    }

    @Override
    public void removeLayer(IGlobeDelegate globe, Layer layer)
    {
        ((IApplicationGlobe) globe).getNativeGlobe().removeLayer(layer);
    }

    @Override
    public void removeLayer(IGlobeDelegate globe, ILayer layer)
    {
        ((IApplicationGlobe) globe).getNativeGlobe().removeLayer(layer);
    }

    @Override
    public void removeLayer(Layer layer)
    {
        LayerFactory factory = layerToLayerFactoriesMap.remove(layer);
        if (factory != null) removeLayerFactory(factory, (a, g) -> g.removeLayer(layer));
    }

    @Override
    public void removeLayer(ILayer layer)
    {
        LayerFactory factory = iLayerToLayerFactoriesMap.remove(layer);
        if (factory != null) removeLayerFactory(factory, (a, g) -> g.removeLayer(layer));
    }

    /**
     * Remove a {@link Layer} in the specified {@link LayerGroup} from all maps.
     * @param layer the layer to remove
     * @param layerGroup the layer group
     * @since 5.2
     */
    public void removeLayer(Layer layer, LayerGroup layerGroup)
    {
        getParentLayer(layerGroup).removeLayer(layer);
    }

    /**
     * Remove an installed layer factory. An optional {@link BiConsumer function} can be provided to clean up any {@link Layer layers} that may have been added by the factory.
     * @param factory the factory
     * @param removeLayerFunc a function to clean up layers created by the factory, or {@code null}
     * @since 5.7
     */
    public void removeLayerFactory(LayerFactory factory, @Nullable BiConsumer<IApplicationGlobe, Globe> removeLayerFunc)
    {
        layerFactories.remove(factory);
        if (removeLayerFunc != null) globeManager.getAllGlobeDelegates().forEach(g -> removeLayerFunc.accept(g, g.getNativeGlobe()));
    }

    /**
     * Set the position for the specified {@link Layer} in the specified {@link LayerGroup} for all maps.
     * @param layer the layer to move
     * @param layerGroup the layer group
     * @param position the index position
     * @since 5.2
     */
    public void setLayerPosition(Layer layer, LayerGroup layerGroup, int position)
    {
        getParentLayer(layerGroup).setLayerPosition(layer, position);
    }

    /**
     * Get the parent {@link MultiLayer} that contains all layers for the specified {@link LayerGroup}.
     * Parent layers are always shared with all globes.
     * @param layerGroup the layer group
     * @return the parent layer
     * @since 5.2
     */
    @Nonnull
    private MultiLayer getParentLayer(LayerGroup layerGroup)
    {
        return switch (layerGroup)
        {
            case IMAGERY -> imageryLayerManager.getLayer();
            case ELEVATION -> elevationLayerManager.getLayer();
            case MAP_DATA -> mapDataLayers;
            case SHAPE_FILES -> shapeFileLayers;
            case KML_OVERLAY -> kmlOverlayLayers;
            default -> uncategorizedLayers;
        };
    }

    /**
     * @return a {@link MultiLayer} containing the {@link LayerGroup#MAP_DATA} layers.
     */
    @Nonnull
    public MultiLayer getMapDataLayers()
    {
        return mapDataLayers;
    }

    /**
     * @return a {@link MultiLayer} containing the shapefile layers.
     */
    @Nonnull
    public MultiLayer getShapeFileLayers()
    {
        return shapeFileLayers;
    }

    /**
     * @return a {@link MultiLayer} containing the KML overlay layers.
     * @since 5.7
     */
    @Nonnull
    public MultiLayer getKmlOverlayLayers()
    {
        return kmlOverlayLayers;
    }

    /**
     * @return a {@link MultiLayer} containing the uncategorized layers.
     */
    @Nonnull
    public MultiLayer getUncategorizedLayers()
    {
        return uncategorizedLayers;
    }

    /**
     * @return a {@link MultiLayer} containing the plugin layers.
     */
    @Nonnull
    public MultiLayer getPluginLayers()
    {
        return pluginLayers;
    }

    /**
     * A factory to add a layer to the globe
     * @since 5.3
     */
    @FunctionalInterface
    public interface LayerFactory
    {
        /**
         * Add a layer to the provided {@code globe}.
         * @param appGlobe the application globe
         * @param globe the native globe
         */
        void addLayer(IApplicationGlobe appGlobe, Globe globe);
    }
}
