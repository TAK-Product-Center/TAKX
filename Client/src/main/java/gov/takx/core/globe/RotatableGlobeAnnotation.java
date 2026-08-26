package gov.takx.core.globe;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;

import java.awt.Color;
import java.awt.Font;

/**
 * A globe annotation that provides controls to display a label rotated around the z-axis.
 *
 * @since 3.0.1
 */
public class RotatableGlobeAnnotation extends GlobeAnnotation
{

    private double rotationAngleDegrees;
    private boolean wrapText = true;

    public RotatableGlobeAnnotation(String text, Position position)
    {
        super(text, position);
    }

    public RotatableGlobeAnnotation(String text, Position position, Font font)
    {
        super(text, position, font);
    }

    public RotatableGlobeAnnotation(String text, Position position, Font font, Color textColor)
    {
        super(text, position, font, textColor);
    }

    public RotatableGlobeAnnotation(String text, Position position, AnnotationAttributes defaults)
    {
        super(text, position, defaults);
    }

    /**
     * @return the counterclockwise degree offset from due west that the label will be displayed at.
     */
    public double getTextRotationAngle()
    {
        return rotationAngleDegrees;
    }

    /**
     * Set the counterclockwise degree offset from due west.
     *
     * @param rotationAngleDegrees The number of degrees to rotate the text
     */
    public void setTextRotationAngle(double rotationAngleDegrees)
    {
        this.rotationAngleDegrees = rotationAngleDegrees;
    }

    /**
     * @return true if long labels should have line breaks automatically inserted into them or false if only explicit
     * line breaks will be displayed within the label.
     */
    public boolean isWrapLabel()
    {
        return wrapText;
    }

    /**
     * Configures whether long blocks of text should have line breaks automatically inserted. By default, line automatic
     * line wrapping is enabled.
     *
     * @param wrapText false to disable automatic text wrapping.
     */
    public void setWrapLabel(boolean wrapText)
    {
        this.wrapText = wrapText;
    }

    @Override
    protected String getWrappedText(DrawContext dc, int width, int height, String text, Font font, String align)
    {
        if (wrapText)
        {
            return super.getWrappedText(dc, width, height, text, font, align);
        }

        return text;
    }

    @Override
    protected void applyScreenTransform(DrawContext dc, int x, int y, int width, int height, double scale)
    {
        super.applyScreenTransform(dc, x, y, width, height, scale);

        GL2 gl = dc.getGL().getGL2();

        // Rotate the text in place
        gl.glTranslated(width / 2.0, height / 2.0, 0.0D);
        gl.glRotated(rotationAngleDegrees, 0, 0, 1);
        gl.glTranslated(-width / 2.0, -height / 2.0, 0.0D);
    }
}
