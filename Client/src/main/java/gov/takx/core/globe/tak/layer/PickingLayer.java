package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.tak.PickedObjectManager;
import gov.takx.core.globe.tak.layer.opengl.GLPickingLayer;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * {@link com.atakmap.map.layer.Layer} implementation for {@link GLPickingLayer}.
 *
 * @since 5.2
 */
@PseudoBean
public class PickingLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLPickingLayer.SPI);
    }

    @Inject
    private PickedObjectManager pickedObjectManager;

    public PickingLayer()
    {
        super("Picking Layer");
    }

    /**
     * @return the picked object manager
     */
    @Nonnull
    public PickedObjectManager getPickedObjectManager()
    {
        return pickedObjectManager;
    }
}
