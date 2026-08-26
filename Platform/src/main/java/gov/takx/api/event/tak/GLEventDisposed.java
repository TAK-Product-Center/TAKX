package gov.takx.api.event.tak;

import com.jogamp.opengl.GLAutoDrawable;

/**
 * Event signaling that the OpenGL context has been disposed.
 *
 * @since 5.6
 */
public record GLEventDisposed(GLAutoDrawable drawable)
{
}
