package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.control.StarsControl;
import gov.tak.api.engine.map.MapRenderer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.tak.GLEventInitialized;
import gov.takx.core.globe.ApplicationGlobeManager;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * A Layer that controls the visibility of the stars rendering.
 *
 * @since 5.2
 */
@PseudoBean
public class StarsLayer extends AbstractLayer
{
    public final static String NAME = "Stars";

    @Inject
    private ApplicationGlobeManager globeManager;

    public StarsLayer()
    {
        super(NAME);
    }

    private void onGLEventInitialized(@Observes GLEventInitialized event)
    {
        globeManager.getAllGlobeDelegates().forEach(g -> setVisible(g.getPortableMapRenderer(), isVisible()));
    }

    @Override
    public synchronized void setVisible(boolean visible)
    {
        globeManager.getAllGlobeDelegates().forEach(g -> setVisible(g.getPortableMapRenderer(), visible));
        super.setVisible(visible);
    }

    private void setVisible(MapRenderer<?> mapRenderer, boolean visible)
    {
        StarsControl starsControl = mapRenderer.getControl(StarsControl.class);
        if (starsControl != null) starsControl.setStarsEnabled(visible);
    }
}