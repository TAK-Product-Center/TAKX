package gov.takx.core.globe.layers;

import gov.nasa.worldwind.layers.Layer;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.layer.IRaptorLayer;

/**
 * Wrapper around a WW layer.
 *
 */
@Internal
@NoPluginImplementations
public interface IGlobeLayer extends Layer
{
    /**
     * @since 3.4 (renamed from getIRaptorLayer)
     */
    IRaptorLayer getRaptorLayer();

    Layer getLayer();

    void setLayer(Layer layer);

    void setEditable(boolean editable);

    /**
     * Determine if this layer is sharable across multiple globe instances.
     *
     * @return {@code true} if this is a sharable layer
     * @since 1.26
     */
    boolean isSharable();

    /**
     * Set the sharable-ness of this layer.  If {@code false} then unique globe layers will be instantiated
     * per globe.
     *
     * @param sharable {@code true} if this globe layer can be shared across multiple globes
     * @since 1.26
     */
    void setSharable(boolean sharable);
}
