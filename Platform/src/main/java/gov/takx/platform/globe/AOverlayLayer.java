package gov.takx.platform.globe;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.OGLTextRenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;

/**
 * Base class for layers the display text over the globe view.
 *
 * @since 3.1 (moved from Client)
 */
public abstract class AOverlayLayer extends AbstractLayer
{
    /**
     * The default color to use when drawing filled rectangles, white 60% opaque.
     */
    protected static final Color DEFAULT_BACKGROUND_COLOR = new Color(0f, 0f, 0f, 0.6f);

    /**
     * The default font to use when drawing text labels.
     */
    protected static final Font DEFAULT_TEXT_FONT = GlobeTextUtils.DEFAULT_MONOSPACE_GLOBE_FONT;

    private static final int GL_ATTRIBUTES = GL2.GL_DEPTH_BUFFER_BIT
            | GL2.GL_COLOR_BUFFER_BIT
            | GL2.GL_ENABLE_BIT
            | GL2.GL_TRANSFORM_BIT
            | GL2.GL_VIEWPORT_BIT
            | GL2.GL_CURRENT_BIT;

    private final OrderedIcon orderedImage = new OrderedIcon();

    protected AOverlayLayer()
    {
        setPickEnabled(false);
    }

    @Override
    public void doRender(DrawContext dc)
    {
        dc.addOrderedRenderable(orderedImage);
    }

    @Override
    public void doPick(DrawContext dc, Point pickPoint)
    {
        dc.addOrderedRenderable(orderedImage);
    }

    private void draw(DrawContext dc)
    {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        boolean attrsPushed = false;
        boolean modelviewPushed = false;
        boolean projectionPushed = false;
        try
        {
            gl.glPushAttrib(GL_ATTRIBUTES);
            attrsPushed = true;

            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDisable(GL.GL_DEPTH_TEST);

            // Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
            // into the GL projection matrix.
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glPushMatrix();
            projectionPushed = true;
            gl.glLoadIdentity();

            modelviewPushed = drawOverlay(dc, gl);
        } finally
        {
            if (projectionPushed)
            {
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glPopMatrix();
            }
            if (modelviewPushed)
            {
                gl.glMatrixMode(GL2.GL_MODELVIEW);
                gl.glPopMatrix();
            }
            if (attrsPushed)
            {
                gl.glPopAttrib();
            }
        }
    }

    /**
     * Draw the actual overlay content (such as any labels or shapes).
     *
     * @param dc The draw context
     * @param gl The OpenGL interface
     * @return Boolean to indicate if {@link GL2#GL_MODELVIEW} matrix was pushed.
     * @since 5.2
     */
    protected abstract boolean drawOverlay(DrawContext dc, GL2 gl);

    /**
     * @return the dimension of a text string using the given font and draw context
     */
    protected Dimension getTextRenderSize(DrawContext dc, String text, Font font)
    {
        return OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), font)
                .getBounds(text)
                .getBounds().getSize();
    }

    /**
     * Draw a label at the given x, y coordinates, using the given text, font, and text color.
     *
     * @param text Text to render
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     * @param textColor Color to use to render text
     */
    protected void drawLabel(DrawContext dc, String text, int x, int y, Color textColor)
    {
        drawLabel(dc, text, x, y, textColor, DEFAULT_TEXT_FONT);
    }

    /**
     * Draw a label at the given x, y coordinates, using the given text, font, and text color.
     *
     * @param text Text to render
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     * @param textColor Color to use to render text
     * @param font Font to use to render text
     */
    protected void drawLabel(DrawContext dc, String text, int x, int y, Color textColor, Font font)
    {
        TextRenderer textRenderer = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), font);
        textRenderer.begin3DRendering();
        textRenderer.setColor(textColor);
        textRenderer.draw(text, x, y);
        textRenderer.end3DRendering();
    }

    /**
     * Draw a rectangle at the given x, y coordinates, filled with the {@link #DEFAULT_BACKGROUND_COLOR}.
     *
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     * @param dimension Size of rectangle to draw
     */
    protected void drawFilledRectangle(DrawContext dc, double x, double y, Dimension dimension)
    {
        drawFilledRectangle(dc, x, y, dimension, DEFAULT_BACKGROUND_COLOR);
    }

    /**
     * Draw a rectangle at the given x, y coordinates, filled with the specified color.
     *
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     * @param dimension Size of rectangle to draw
     * @param color Fill color
     */
    protected void drawFilledRectangle(DrawContext dc, double x, double y, Dimension dimension, Color color)
    {
        final double width = dimension.getWidth();
        final double height = dimension.getHeight();

        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
        gl.glBegin(GL2.GL_POLYGON);
        gl.glVertex3d(x, y, 0);
        gl.glVertex3d(x + width, y, 0);
        gl.glVertex3d(x + width, y + height, 0);
        gl.glVertex3d(x, y + height, 0);
        gl.glVertex3d(x, y, 0);
        gl.glEnd();
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }

    /**
     * Ordered icon renderable. Draw with an eye distance of 0 so that it shows up in front of most other things.
     */
    protected final class OrderedIcon implements OrderedRenderable
    {
        @Override
        public double getDistanceFromEye()
        {
            return 0;
        }

        @Override
        public void pick(DrawContext dc, Point pickPoint)
        {
            draw(dc);
        }

        @Override
        public void render(DrawContext dc)
        {
            draw(dc);
        }
    }
}
