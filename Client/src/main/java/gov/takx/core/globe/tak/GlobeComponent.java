package gov.takx.core.globe.tak;

import com.atakmap.map.RenderSurface;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * A renderable map component. Implementations <B>MUST</B> derive from {@link java.awt.Component}.
 *
 * @since 3.4
 */
interface GlobeComponent extends GLAutoDrawable, RenderSurface
{
}
