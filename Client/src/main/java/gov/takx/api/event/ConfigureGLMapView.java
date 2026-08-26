package gov.takx.api.event;

import com.atakmap.map.RenderContext;
import com.atakmap.map.layer.control.RenderContextControl;
import com.atakmap.map.opengl.GLMapView;

/**
 * This event is fired when a {@link GLMapView} is (re-)configured. This is sent on initial GLMapView creation and after calling {@link RenderContextControl#setRenderContext(RenderContext)}.
 *
 * @since 5.6
 */
public record ConfigureGLMapView(GLMapView glMapView)
{
}
