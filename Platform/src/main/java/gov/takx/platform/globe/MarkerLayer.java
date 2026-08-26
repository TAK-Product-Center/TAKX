package gov.takx.platform.globe;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.markers.Marker;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * A WorldWind {@link gov.nasa.worldwind.layers.Layer} to render a collection of {@link Marker} using TAK. Existing markers should be reused
 * and updated when editing instead of being recreated to prevent the markers from flickering on the maps as they update.
 * @since 5.1
 */
public class MarkerLayer extends AbstractLayer
{
    private Iterable<Marker> markers;
    private final HashMap<Marker, MarkerRenderable> markerRenderableCache = new HashMap<>();

    public MarkerLayer()
    {
    }

    /**
     * @param markers the initial markers for this layer
     */
    public MarkerLayer(Iterable<Marker> markers)
    {
        this.markers = markers;
    }

    /**
     * Get the markers in this layer.
     * @return the markers for this layer
     */
    @Nullable
    public Iterable<Marker> getMarkers()
    {
        return markers;
    }

    /**
     * Set the markers for this layer.
     * @param markers the markers for this layer
     */
    public void setMarkers(@Nullable Iterable<Marker> markers)
    {
        this.markers = markers;
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        if (markers == null) return;
        Collection<Marker> markerCollection;
        if (markers instanceof Collection<Marker> collection)
        {
            markerCollection = collection;
        } else
        {
            markerCollection = new ArrayList<>();
            markers.forEach(markerCollection::add);
        }
        markerRenderableCache.keySet().retainAll(markerCollection);
        markers.forEach(m -> {
            MarkerRenderable markerRenderable = markerRenderableCache.computeIfAbsent(m, MarkerRenderable::new);
            doRender(dc, markerRenderable);
        });
    }

    protected void doRender(DrawContext dc, MarkerRenderable markerRenderable)
    {
        dc.addOrderedRenderable(markerRenderable);
    }
}
