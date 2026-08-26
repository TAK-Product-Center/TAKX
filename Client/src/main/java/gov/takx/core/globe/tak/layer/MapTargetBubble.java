package gov.takx.core.globe.tak.layer;

import com.atakmap.map.Globe;
import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.math.PointD;
import gov.tak.platform.graphics.Rect;
import gov.tak.platform.layers.CrosshairLayer;
import gov.tak.platform.layers.MagnifierLayer;
import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.core.globe.tak.layer.opengl.GLMapTargetBubble;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * {@link com.atakmap.map.layer.Layer} implementation for {@link GLMapTargetBubble}.
 *
 * @since 5.1
 */
public class MapTargetBubble extends AbstractLayer
{
    static
    {
        MarshalManager.registerMarshal(new AbstractMarshal(MapTargetBubble.class, MagnifierLayer.class)
        {
            @Override
            protected <T, V> T marshalImpl(V in)
            {
                return (T) ((MapTargetBubble) in).impl;
            }
        }, MapTargetBubble.class, MagnifierLayer.class);
        GLLayerFactory.register(GLMapTargetBubble.SPI2);
    }

    private final Queue<OnLocationChangedListener> onLocationChangedListener = new ConcurrentLinkedQueue<>();
    private final Queue<OnScaleChangedListener> onScaleChangedListener = new ConcurrentLinkedQueue<>();
    private final Queue<OnCrosshairColorChangedListener> onCrosshairColorChangedListener = new ConcurrentLinkedQueue<>();

    private final CrosshairLayer crosshair;
    private final Polygon viewport;
    public final MagnifierLayer impl;

    public MapTargetBubble(MapRenderer mapRenderer, List<? extends Layer> layers, int x, int y,
                           int width, int height, double mapScale)
    {
        this(mapRenderer, layers, createRectangle(x, y, width, height), mapScale);
    }

    public MapTargetBubble(MapRenderer mapRenderer, List<? extends Layer> layers, Polygon viewport, double mapScale)
    {
        super("Target Bubble");

        crosshair = new CrosshairLayer("Target Bubble Crosshair");
        crosshair.setColor(0xFF000000);

        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, DisplayOrigin.UpperLeft);
        GeoPoint center = GeoPoint.createMutable();
        mapRenderer.inverse(new PointD(mapSceneModel.width / 2.0, mapSceneModel.height / 2.0, 0), center, IMapRendererEnums.InverseMode.RayCast, 0, DisplayOrigin.UpperLeft);
        this.viewport = viewport;
        Rect implViewport;
        if (this.viewport != null)
        {
            Envelope bounds = this.viewport.getEnvelope();
            implViewport = new Rect(
                    (int) bounds.minX,
                    (int) bounds.minY,
                    (int) bounds.maxX,
                    (int) bounds.maxY);
        } else
        {
            implViewport = new Rect(
                    0,
                    0,
                    mapSceneModel.width,
                    mapSceneModel.height);
        }

        ArrayList<Layer> newLayers = new ArrayList<>(layers);
        newLayers.add(crosshair);
        impl = new MagnifierLayer(getName(), newLayers, implViewport, center, mapScale, false);
        impl.addOnScaleChangedListener((bubble, scale, scaleIsRelative) -> onScaleChanged());
        impl.addOnLocationChangedListener((bubble, latitude, longitude) -> onLocationChanged());
    }

    public Polygon getViewport()
    {
        return viewport;
    }

    public void shiftLocation(final double latShift, final double lngShift)
    {
        setLocation(getLatitude() + latShift, getLongitude() + lngShift);
    }

    public void setLocation(final double latitude, final double longitude)
    {
        impl.setLocation(latitude, longitude);
    }

    public List<Layer> getLayers()
    {
        return impl.getLayers();
    }

    public Globe getGlobe()
    {
        return impl.getGlobe();
    }

    public CrosshairLayer getCrosshair()
    {
        return crosshair;
    }

    public double getLatitude()
    {
        return impl.getLatitude();
    }

    public double getLongitude()
    {
        return impl.getLongitude();
    }

    public int getX()
    {
        return impl.getViewport().left;
    }

    public int getY()
    {
        return impl.getViewport().top;
    }

    public int getWidth()
    {
        return impl.getViewport().width();
    }

    public int getHeight()
    {
        return impl.getViewport().height();
    }

    /**
     * Returns an appropriate map scale for displaying the content in the bubble based on the
     * available layers.
     *
     * @return the map scale.
     */
    public double getMapScale()
    {
        return impl.getMagnifierScale();
    }

    /**
     * Sets the map scale for displaying the content of the bubble based on the available layers
     * @param scale the scale to set
     */
    public void setMapScale(double scale)
    {
        impl.setMagnifierScale(scale, false);
    }

    public int getCrosshairColor()
    {
        return crosshair.getCrosshairColor();
    }

    /**
     * Sets the  color of the crosshair.
     * @param color the crosshair changed
     */
    public void setCrosshairColor(int color)
    {
        if (getCrosshairColor() != color)
        {
            crosshair.setColor(color);

            onCrosshairColorChanged();
        }
    }

    /**
     * A callback to signal that the {@link MapTargetBubble} location changed.
     */
    public interface OnLocationChangedListener
    {
        /**
         * Called when the {@link MapTargetBubble} location changed.
         */
        void onMapTargetBubbleLocationChanged(MapTargetBubble bubble);
    }

    public void addOnLocationChangedListener(OnLocationChangedListener l)
    {
        onLocationChangedListener.add(l);
    }

    public void removeOnLocationChangedListener(OnLocationChangedListener l)
    {
        onLocationChangedListener.remove(l);
    }

    protected void onLocationChanged()
    {
        for (OnLocationChangedListener l : onLocationChangedListener)
        {
            l.onMapTargetBubbleLocationChanged(this);
        }
    }

    /**
     * A callback to signal that the {@link MapTargetBubble} scale changed.
     */
    public interface OnScaleChangedListener
    {
        /**
         * Called when the {@link MapTargetBubble} scale changed.
         */
        void onMapTargetBubbleScaleChanged(MapTargetBubble bubble);
    }

    public void addOnScaleChangedListener(OnScaleChangedListener l)
    {
        onScaleChangedListener.add(l);
    }

    public void removeOnScaleChangedListener(OnScaleChangedListener l)
    {
        onScaleChangedListener.remove(l);
    }

    protected void onScaleChanged()
    {
        for (OnScaleChangedListener l : onScaleChangedListener)
        {
            l.onMapTargetBubbleScaleChanged(this);
        }
    }

    /**
     * A callback to signal that the {@link MapTargetBubble} crosshair color changed.
     */
    public interface OnCrosshairColorChangedListener
    {
        /**
         * Called when the {@link MapTargetBubble} crosshair color changed.
         */
        void onMapTargetBubbleCrosshairColorChanged(MapTargetBubble bubble);
    }

    public void addOnCrosshairColorChangedListener(OnCrosshairColorChangedListener l)
    {
        onCrosshairColorChangedListener.add(l);
    }

    public void removeOnCrosshairColorChangedListener(OnCrosshairColorChangedListener l)
    {
        onCrosshairColorChangedListener.remove(l);
    }

    protected void onCrosshairColorChanged()
    {
        for (OnCrosshairColorChangedListener l : onCrosshairColorChangedListener)
        {
            l.onMapTargetBubbleCrosshairColorChanged(this);
        }
    }

    private static Polygon createRectangle(int x, int y, int w, int h)
    {
        LineString rect = new LineString(2);
        rect.addPoint(x, y);
        rect.addPoint(x + w, y);
        rect.addPoint(x + w, y + h);
        rect.addPoint(x, y + h);
        rect.addPoint(x, y);

        Polygon polygon = new Polygon(rect.getDimension());
        polygon.addRing(rect);
        return polygon;
    }
}
