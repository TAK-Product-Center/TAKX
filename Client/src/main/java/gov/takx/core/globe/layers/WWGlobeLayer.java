package gov.takx.core.globe.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRestorable;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorShutdownListener;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Globe wrapper for a World Wind Layer.
 */
@Deprecated
public class WWGlobeLayer implements IGlobeLayer, IRestorable, IRaptorShutdownListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IRaptorLayer raptorLayer;
    protected Layer layer;
    protected boolean sharable = false; // Default to false

    public WWGlobeLayer(IRaptorLayer raptorLayer, Layer layer)
    {
        this.raptorLayer = raptorLayer;
        raptorLayer.setRestorableState(this);
        this.layer = layer;
        layer.setEnabled(this.raptorLayer.isVisible());
        setOpacity(this.raptorLayer.getOpacity());
        setPickEnabled(this.raptorLayer.isPickable());
        setName(this.raptorLayer.getName());
        setMinActiveAltitude(this.raptorLayer.getMinActiveAltitude());
        setMaxActiveAltitude(this.raptorLayer.getMaxActiveAltitude());

        double minAlt = layer.getMinActiveAltitude();
        if (minAlt < IRaptorLayer.DEFAULT_MIN_ACTIVE_ALTITUDE) minAlt = IRaptorLayer.DEFAULT_MIN_ACTIVE_ALTITUDE;
        raptorLayer.setMinActiveAltitude(minAlt);

        double maxAlt = layer.getMaxActiveAltitude();
        if (maxAlt > IRaptorLayer.DEFAULT_MAX_ACTIVE_ALTITUDE) maxAlt = IRaptorLayer.DEFAULT_MAX_ACTIVE_ALTITUDE;
        raptorLayer.setMaxActiveAltitude(maxAlt);

        //set the splitScale for tiled image layers
        if (layer instanceof TiledImageLayer)
        {
            setSplitScale(raptorLayer.getSplitScale());
        }

        raptorLayer.addPropertyChangeListener(new RaptorLayerPropertyChangeHandler());
    }

    @Override
    public IRaptorLayer getRaptorLayer()
    {
        return raptorLayer;
    }

    @Override
    public Layer getLayer()
    {
        return layer;
    }

    @Override
    public void setLayer(Layer layer)
    {
        this.layer = layer;
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
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        // Do nothing
    }

    // LAYER DELEGATE METHODS

    @Override
    public boolean isNetworkRetrievalEnabled()
    {
        return layer.isNetworkRetrievalEnabled();
    }

    @Override
    public void setNetworkRetrievalEnabled(boolean b)
    {
        layer.setNetworkRetrievalEnabled(b);
    }

    @Override
    public void setExpiryTime(long expiryTime)
    {
        layer.setExpiryTime(expiryTime);
    }

    @Override
    public long getExpiryTime()
    {
        return layer.getExpiryTime();
    }

    @Override
    public boolean isEnabled()
    {
        return layer.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        layer.setEnabled(enabled);
    }

    @Override
    public String getName()
    {
        return layer.getName();
    }

    @Override
    public void setName(String name)
    {
        layer.setName(name);
    }

    @Override
    public double getScale()
    {
        return layer.getScale();
    }

    @Override
    public double getOpacity()
    {
        return layer.getOpacity();
    }

    @Override
    public void setOpacity(double opacity)
    {
        layer.setOpacity(opacity);
    }

    @Override
    public boolean isPickEnabled()
    {
        return layer.isPickEnabled();
    }

    @Override
    public void setPickEnabled(boolean isPickable)
    {
        layer.setPickEnabled(isPickable);
    }

    @Override
    public void setEditable(boolean editable)
    {
        throw new UnsupportedOperationException("Layer not editable");
    }

    @Override
    public void render(DrawContext dc)
    {
        layer.render(dc);
    }

    @Override
    public void preRender(DrawContext drawContext)
    {
        layer.preRender(drawContext);
    }

    @Override
    public void pick(DrawContext dc, Point pickPoint)
    {
        layer.pick(dc, pickPoint);
    }

    @Override
    public boolean isAtMaxResolution()
    {
        return layer.isAtMaxResolution();
    }

    @Override
    public boolean isMultiResolution()
    {
        return layer.isMultiResolution();
    }

    @Override
    public Object setValue(String key, Object value)
    {
        return layer.setValue(key, value);
    }

    @Override
    public AVList setValues(AVList avList)
    {
        return layer.setValues(avList);
    }

    @Override
    public Object getValue(String key)
    {
        return layer.getValue(key);
    }

    @Override
    public Collection<Object> getValues()
    {
        return layer.getValues();
    }

    @Override
    public String getStringValue(String key)
    {
        return layer.getStringValue(key);
    }

    @Override
    public Set<Map.Entry<String, Object>> getEntries()
    {
        return layer.getEntries();
    }

    @Override
    public boolean hasKey(String key)
    {
        return layer.hasKey(key);
    }

    @Override
    public Object removeKey(String key)
    {
        return layer.removeKey(key);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
        layer.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
        layer.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        layer.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        layer.removePropertyChangeListener(listener);
    }

    @Override
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        layer.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void firePropertyChange(PropertyChangeEvent propertyChangeEvent)
    {
        layer.firePropertyChange(propertyChangeEvent);
    }

    @Override
    public AVList copy()
    {
        return layer.copy();
    }

    @Override
    public AVList clearList()
    {
        return layer.clearList();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        layer.propertyChange(evt);
    }

    @Override
    public void dispose()
    {
        layer.dispose();
    }

    @Override
    public String getRestorableState()
    {
        if (layer != null)
        {
            return layer.getRestorableState();
        }
        return null;
    }

    @Override
    public void restoreState(String stateInXml)
    {
        layer.restoreState(stateInXml);
    }

    @Override
    public double getMinActiveAltitude()
    {
        return layer.getMinActiveAltitude();
    }

    @Override
    public void setMinActiveAltitude(double v)
    {
        layer.setMinActiveAltitude(v);
        // All our updates would come through IRaptorLayer, not this method on Layer, so we don't update iRaptorLayer
    }

    @Override
    public double getMaxActiveAltitude()
    {
        return layer.getMaxActiveAltitude();
    }

    @Override
    public void setMaxActiveAltitude(double v)
    {
        layer.setMaxActiveAltitude(v);
        // All our updates would come through IRaptorLayer, not this method on Layer, so we don't update iRaptorLayer
    }

    @Override
    public boolean isLayerInView(DrawContext drawContext)
    {
        return layer.isLayerInView(drawContext);
    }

    @Override
    public boolean isLayerActive(DrawContext drawContext)
    {
        return layer.isLayerActive(drawContext);
    }

    @Override
    public Double getMaxEffectiveAltitude(Double radius)
    {
        return layer.getMaxEffectiveAltitude(radius);
    }

    @Override
    public Double getMinEffectiveAltitude(Double radius)
    {
        return layer.getMinEffectiveAltitude(radius);
    }

    /**
     * Update the bounding sector to the Layer's AVList, adding it if necessary.
     *
     * @param layer  The layer to set the AVList parameter on.
     * @param sector The bounding sector for the layer.
     * @since 1.14.13
     */
    protected void setSectorLayerConstructionParameter(Layer layer, Sector sector)
    {
        if (sector != null)
        {
            AVList constParams = (AVList) layer.getValue(AVKey.CONSTRUCTION_PARAMETERS);

            // If the entry doesn't exist yet, create it.
            if (constParams == null)
            {
                constParams = new AVListImpl();

                // Don't ask my why this needs to be wrapped in another AVList...
                AVList avlist = new AVListImpl();
                avlist.setValue(AVKey.CONSTRUCTION_PARAMETERS, constParams);
                layer.setValues(avlist);
            }

            // Set the new sector value.
            constParams.setValue(AVKey.SECTOR, sector);
        }
    }

    private void raptorLayerMinActiveAltitudeChanged(IRaptorLayer raptorLayer)
    {
        if (raptorLayer.isMinActiveAltitudeEnabled())
        {
            if (Double.compare(raptorLayer.getMinActiveAltitude(), layer.getMinActiveAltitude()) != 0)
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("changing altitude on layer: {}, to: {}", layer.getName(), raptorLayer.getMinActiveAltitude());
                }

                layer.setMinActiveAltitude(raptorLayer.getMinActiveAltitude());
            }
        }
    }

    private void raptorLayerMaxActiveAltitudeChanged(IRaptorLayer raptorLayer)
    {
        if (raptorLayer.isMaxActiveAltitudeEnabled())
        {
            if (Double.compare(raptorLayer.getMaxActiveAltitude(), layer.getMaxActiveAltitude()) != 0)
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("changing altitude on layer: {}, to: {}", layer.getName(), raptorLayer.getMaxActiveAltitude());
                }

                layer.setMaxActiveAltitude(raptorLayer.getMaxActiveAltitude());
            }
        }
    }

    private void setSplitScale(double splitScale)
    {
        if (layer instanceof TiledImageLayer tiledImageLayer)
        {
            tiledImageLayer.setDetailHint(splitScale);
            logger.debug("setting detailHint: {}", splitScale);
        }
    }

    /**
     * @return The name of this layer
     */
    @Override
    public String toString()
    {
        return raptorLayer.getName();
    }

    @Override
    public void onMessage(Message message)
    {
        layer.onMessage(message);
    }

    /**
     * Class to handle changes on the {@link IRaptorLayer} that this {@link WWGlobeLayer} wraps by propagating the changes
     * into the Globe layer.
     */
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
                case IRaptorLayer.MIN_ACTIVE_ALTITUDE -> raptorLayerMinActiveAltitudeChanged(raptorLayer);
                case IRaptorLayer.MAX_ACTIVE_ALTITUDE -> raptorLayerMaxActiveAltitudeChanged(raptorLayer);
                case IRaptorLayer.SPLIT_SCALE -> setSplitScale(raptorLayer.getSplitScale());
                default ->
                {
                    // Ignore
                }
            }
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WWGlobeLayer that = (WWGlobeLayer) o;

        if (that.layer == layer && that.raptorLayer == raptorLayer) return true;

        String thatState = that.layer.getRestorableState();
        String thisState = layer.getRestorableState();
        if (thisState == null || thatState == null) return false;

        return Objects.equals(thisState, thatState);
    }

    @Override
    public int hashCode()
    {
        int result = raptorLayer.hashCode();
        result = 31 * result + layer.hashCode();
        return result;
    }
}
