package gov.takx.api.event.tak;

import com.jogamp.opengl.GLAutoDrawable;

/**
 * Event signaling that the OpenGL context has been initialized.
 *
 * @since 5.6
 */
public record GLEventInitialized(GLAutoDrawable drawable)
{
}
