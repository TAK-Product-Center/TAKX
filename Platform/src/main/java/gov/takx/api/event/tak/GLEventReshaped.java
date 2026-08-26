package gov.takx.api.event.tak;

import com.jogamp.opengl.GLAutoDrawable;

/**
 * Event signaling that the OpenGL context has been reshaped.
 *
 * @since 5.6
 */
public record GLEventReshaped(GLAutoDrawable drawable, int x, int y, int width, int height)
{
}
