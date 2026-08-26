package gov.takx.core.globe.layers;

import android.util.Pair;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.layer.raster.RasterLayer2;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.layer.IRaptorLayer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Custom {@link IGlobeLayer} for rendering TAK globe layers.
 *
 * @since 3.4
 */
public class GlobeLayer extends AbstractLayer implements IGlobeLayer, Layer
{
    public final static GLLayerSpi2 SPI = new GLLayerSpi2()
    {
        @Override
        public int getPriority()
        {
            return 1;
        }

        @SuppressWarnings("deprecation")
        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> arg)
        {
            final MapRenderer surface = arg.first;
            final Layer layer = arg.second;
            if (layer instanceof GlobeLayer globeLayer) return GLLayerFactory.create3(surface, globeLayer.layer);
            return null;
        }
    };

    static
    {
        GLLayerFactory.register(SPI);
    }

    private final IRaptorLayer raptorLayer;
    private final Layer layer;
    protected boolean sharable;

    /**
     * @param raptorLayer the raptor layer
     * @param layer the TAK layer
     */
    public GlobeLayer(IRaptorLayer raptorLayer, Layer layer)
    {
        this(raptorLayer, layer, null);
    }

    /**
     * @param raptorLayer the raptor layer
     * @param layer the TAK layer
     * @param envelope the area covered by the layer, or null
     */
    public GlobeLayer(IRaptorLayer raptorLayer, Layer layer, @Nullable Envelope envelope)
    {
        this.raptorLayer = raptorLayer;
        // TODO TAKX-2649 - provide IRestoreable to raptorLayer
        //raptorLayer.setRestorableState(this);
        this.layer = layer;
        layer.setVisible(this.raptorLayer.isVisible());
        setSharable(raptorLayer.isSharable());
        setValue(AVKey.CONSTRUCTION_PARAMETERS, new AVListImpl());
        if (envelope != null) setSector(envelope);

        raptorLayer.addPropertyChangeListener(new RaptorLayerPropertyChangeHandler());
    }

    /**
     * Set the layer's coverage area.
     * @param envelope the layer's coverage area
     * @since 3.4.1
     */
    public void setSector(@Nonnull Envelope envelope)
    {
        setSector(Sector.fromDegrees(envelope.minY, envelope.maxY, envelope.minX, envelope.maxX));
    }

    /**
     * Set the layer's coverage area.
     * @param sector the layer's coverage area
     * @since 5.4
     */
    public void setSector(@Nonnull Sector sector)
    {
        ((AVList) getValue(AVKey.CONSTRUCTION_PARAMETERS)).setValue(AVKey.SECTOR, sector);
    }

    @Override
    protected void doRender(DrawContext drawContext)
    {
        // do nothing - rendering happens in TAK, not WW
    }

    @Override
    public String getName()
    {
        return layer.getName();
    }

    @Override
    public IRaptorLayer getRaptorLayer()
    {
        return raptorLayer;
    }

    /**
     * Get the TAK {@link Layer} that this GlobeLayer wraps
     * @return the TAK layer
     * @since 5.2
     */
    public Layer getTakLayer()
    {
        return layer;
    }

    @Override
    public gov.nasa.worldwind.layers.Layer getLayer()
    {
        return this;
    }

    @Override
    public void setLayer(gov.nasa.worldwind.layers.Layer layer)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEditable(boolean editable)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSharable()
    {
        return sharable;
    }

    @Override
    public void setSharable(boolean sharable)
    {
        this.sharable = sharable;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        layer.setVisible(enabled);
        super.setEnabled(enabled);
    }

    /**
     * Overridden to return the Raptor layers restorable state.
     * <p/>
     * {@inheritDoc}
     * @see #getRaptorLayer()
     * @since 3.5
     */
    @Override
    public String getRestorableState()
    {
        return raptorLayer.getRestorableState();
    }

    @Override
    public void setOpacity(double opacity)
    {
        super.setOpacity(opacity);
        if (layer instanceof RasterLayer2 rasterLayer)
        {
            rasterLayer.getSelectionOptions().forEach(so -> rasterLayer.setTransparency(so, (float) opacity));
        }
    }

    private class RaptorLayerPropertyChangeHandler implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            final String propName = evt.getPropertyName();
            final IRaptorLayer raptorLayer = (IRaptorLayer) evt.getSource();

            switch (propName)
            {
                case IRaptorLayer.NAME -> setName(raptorLayer.getName());
                case IRaptorLayer.VISIBLE -> setEnabled(raptorLayer.isVisible());
                case IRaptorLayer.OPACITY -> setOpacity(raptorLayer.getOpacity());
                case IRaptorLayer.EDITABLE -> setEditable(raptorLayer.isEditable());
                case IRaptorLayer.PICKABLE -> setPickEnabled(raptorLayer.isPickable());
                case IRaptorLayer.NETWORK_RETRIEVABLE -> setNetworkRetrievalEnabled(raptorLayer.isNetworkRetrievalEnabled());
                case IRaptorLayer.MIN_ACTIVE_ALTITUDE ->
                {
                    // TODO TAKX-2649 - restore MIN_ACTIVE_ALTITUDE functionality
                    //raptorLayerMinActiveAltitudeChanged(raptorLayer);
                }
                case IRaptorLayer.MAX_ACTIVE_ALTITUDE ->
                {
                    // TODO TAKX-2649 - restore MAX_ACTIVE_ALTITUDE functionality
                    //raptorLayerMaxActiveAltitudeChanged(raptorLayer);
                }
                case IRaptorLayer.SPLIT_SCALE ->
                {
                    // TODO TAKX-2649 - restore SPLIT_SCALE functionality
                    //setSplitScale(raptorLayer.getSplitScale());
                }
                default ->
                {
                    // Ignore
                }
            }
        }
    }

    @Override
    public void setVisible(boolean visible)
    {
        layer.setVisible(visible);
    }

    @Override
    public boolean isVisible()
    {
        return layer.isVisible();
    }

    @Override
    public void addOnLayerVisibleChangedListener(OnLayerVisibleChangedListener l)
    {
        layer.addOnLayerVisibleChangedListener(l);
    }

    @Override
    public void removeOnLayerVisibleChangedListener(OnLayerVisibleChangedListener l)
    {
        layer.removeOnLayerVisibleChangedListener(l);
    }
}
