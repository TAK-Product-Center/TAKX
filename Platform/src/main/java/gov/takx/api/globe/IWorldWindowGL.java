package gov.takx.api.globe;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.awt.AWTGLAutoDrawable;
import gov.nasa.worldwind.WorldWindow;

import java.awt.Component;

/**
 * This interface provides the intersection of the functionality of {@link gov.nasa.worldwind.awt.WorldWindowGLCanvas}
 * and {@link gov.nasa.worldwind.awt.WorldWindowGLJPanel}.
 */
public interface IWorldWindowGL extends WorldWindow, GLAutoDrawable, AWTGLAutoDrawable
{
    /**
     * Return the AWT {@link Component} responsible for drawing this globe on the screen.
     */
    Component getComponent();
}
