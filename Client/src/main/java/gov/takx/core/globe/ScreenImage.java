package gov.takx.core.globe;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.WWIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;

/**
 * TODO: get rid of this class.  Need to add a method to PowerOfTwoPaddedImage to create a centered image from the
 * image source and that will fix the reason for this class existing (instead of using the one from WW).
 *
 */
public class ScreenImage implements Renderable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Object imageSource;
    private OrderedImage orderedImage = new OrderedImage();
    private PickSupport pickSupport = new PickSupport();
    private int imageWidth;
    private int imageHeight;
    private Point screenLocation;
    private double opacity = 1d;

    private class OrderedImage implements OrderedRenderable
    {
        public double getDistanceFromEye()
        {
            return 0;
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            ScreenImage.this.draw(dc);
        }

        public void render(DrawContext dc)
        {
            ScreenImage.this.draw(dc);
        }
    }

    public Point getScreenLocation()
    {
        return this.screenLocation;
    }

    public void setScreenLocation(Point screenLocation)
    {
        this.screenLocation = screenLocation;
    }

    public Object getImageSource()
    {
        return this.imageSource;
    }

    public void setImageSource(Object imageSource)
    {
        this.imageSource = imageSource;
    }

    public double getOpacity()
    {
        return opacity;
    }

    public void setOpacity(double opacity)
    {
        this.opacity = opacity;
    }

    public void render(DrawContext dc)
    {
        this.doRender(dc);
    }

    protected void doRender(DrawContext dc)
    {
        dc.addOrderedRenderable(this.orderedImage);
    }

    private void draw(DrawContext dc)
    {
        if (this.screenLocation == null || this.imageSource == null)
        {
            return;
        }

        GL2 gl = (GL2) dc.getGL();

        boolean attribsPushed = false;
        boolean modelviewPushed = false;
        boolean projectionPushed = false;

        try
        {
            gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT
                    | GL.GL_COLOR_BUFFER_BIT
                    | GL2.GL_ENABLE_BIT
                    | GL2.GL_TEXTURE_BIT
                    | GL2.GL_TRANSFORM_BIT
                    | GL2.GL_VIEWPORT_BIT
                    | GL2.GL_CURRENT_BIT);
            attribsPushed = true;

            // Don't depth buffer.
            gl.glDisable(GL.GL_DEPTH_TEST);

            // Suppress any fully transparent image pixels
            gl.glEnable(GL2ES1.GL_ALPHA_TEST);
            gl.glAlphaFunc(GL.GL_GREATER, 0.001f);

            Texture texture = null;
            if (!dc.isPickingMode())
            {
                texture = (Texture) dc.getTextureCache().get(this.imageSource);
                if (texture == null)
                {
                    this.initializeTexture(dc);
                    texture = (Texture) dc.getTextureCache().get(this.imageSource);
                    if (texture == null)
                    {
                        return;
                    }
                }
            }

            java.awt.Rectangle viewport = dc.getView().getViewport();
            gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.glPushMatrix();
            projectionPushed = true;
            gl.glLoadIdentity();
            gl.glOrtho(0d, viewport.width, 0d, viewport.height, -1, 1);

            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glPushMatrix();
            modelviewPushed = true;
            gl.glLoadIdentity();
            gl.glTranslated(this.screenLocation.x - this.imageWidth / 2,
                    viewport.height - (this.screenLocation.y + this.imageHeight / 2), 0d);

            if (!dc.isPickingMode())
            {
                if (texture != null)
                {
                    gl.glEnable(GL.GL_TEXTURE_2D);
                    texture.bind(gl);

                    gl.glColor4d(1d, 1d, 1d, this.opacity);
                    gl.glEnable(GL.GL_BLEND);
                    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                    TextureCoords texCoords = texture.getImageTexCoords();
                    gl.glScaled(this.imageWidth, this.imageHeight, 1d);
                    dc.drawUnitQuad(texCoords);
                }
            } else
            {
                this.pickSupport.clearPickList();
                this.pickSupport.beginPicking(dc);
                Color color = dc.getUniquePickColor();
                int colorCode = color.getRGB();
                this.pickSupport.addPickableObject(colorCode, this, null, false);
                gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                gl.glScaled(this.imageWidth, this.imageHeight, 1d);
                dc.drawUnitQuad();
                this.pickSupport.endPicking(dc);
                this.pickSupport.resolvePick(dc, dc.getPickPoint(), dc.getCurrentLayer());
            }
        } finally
        {
            if (projectionPushed)
            {
                gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
                gl.glPopMatrix();
            }
            if (modelviewPushed)
            {
                gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
                gl.glPopMatrix();
            }
            if (attribsPushed)
            {
                gl.glPopAttrib();
            }
        }
    }

    private void initializeTexture(DrawContext dc)
    {
        Texture iconTexture = (Texture) dc.getTextureCache().get(this.imageSource);
        if (iconTexture != null)
        {
            return;
        }

        try
        {
            if (this.imageSource instanceof BufferedImage)
            {
                iconTexture = AWTTextureIO.newTexture(GLProfile.getDefault(), (BufferedImage) this.imageSource, true);
                iconTexture.bind(dc.getGL());
//                this.imageWidth = iconTexture.getWidth();
//                this.imageHeight = iconTexture.getHeight();
                imageWidth = ((BufferedImage) imageSource).getWidth();
                imageHeight = ((BufferedImage) imageSource).getHeight();
                dc.getTextureCache().put(this.imageSource, iconTexture);
            } else if (this.imageSource instanceof String)
            {
                // Handle local file or resource
                String imagePath = (String) this.imageSource;
                Object streamOrException = WWIO.getFileOrResourceAsStream(imagePath, this.getClass());
                if (streamOrException == null || streamOrException instanceof Exception)
                {
//                    Logging.logger().log(Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile",
//                        streamOrException != null ? streamOrException : "");
                    return;
                }
                iconTexture = TextureIO.newTexture((InputStream) streamOrException, true, null);
                iconTexture.bind(dc.getGL());
                this.imageWidth = iconTexture.getWidth();
                this.imageHeight = iconTexture.getHeight();
                dc.getTextureCache().put(imagePath, iconTexture);
            } else
            {
                logger.error("Unrecognized image source type {}", imageSource.getClass().getName());
                return;
            }
        } catch (IOException e)
        {
            String msg = "IOException during layer initialization";
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }

        final GL2 gl = (GL2) dc.getGL();
        gl.glTexEnvf(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_MODE, GL2ES1.GL_MODULATE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);//_MIPMAP_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
    }
}
