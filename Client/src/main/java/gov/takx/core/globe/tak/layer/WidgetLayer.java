package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.tak.WidgetLayerManager;
import gov.takx.core.globe.tak.layer.opengl.GLWidgetLayer;
import jakarta.inject.Inject;

/**
 * A layer that represents and controls the {@link WidgetLayerManager}.
 *
 * @since 3.5
 */
@PseudoBean
public class WidgetLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLWidgetLayer.SPI);
    }

    @Inject
    private WidgetLayerManager widgetLayerManager;

    public WidgetLayer()
    {
        super("Widget Layer");
    }

    /**
     * Get the widget layer manager.
     * @return the widget layer manager.
     */
    public WidgetLayerManager getWidgetLayerManager()
    {
        return widgetLayerManager;
    }
}
