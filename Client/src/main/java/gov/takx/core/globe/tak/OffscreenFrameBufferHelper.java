package gov.takx.core.globe.tak;

import com.atakmap.opengl.OffscreenFrameBuffer;
import gov.tak.platform.commons.opengl.GLES30;

/**
 * A helper class to manage an {@link OffscreenFrameBuffer}. Includes support for automatically
 * recreating offscreen buffer if the size changes and restoring the original framebuffer.
 * @since 5.1
 */
public class OffscreenFrameBufferHelper
{
    private OffscreenFrameBuffer ofb;

    /**
     * Bind to the offscreen buffer and execute the provided {@link Runnable}. The original framebuffer will be restored upon returning.
     * @param r the runnable
     * @param width the offscreen buffer width
     * @param height the offscreen buffer height
     */
    public void useOffscreenBuffer(Runnable r, int width, int height)
    {
        int[] boundFbo = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo, 0);
        if (ofb == null || ofb.width != width || ofb.height != height)
        {
            if (ofb != null)
            {
                ofb.release();
                ofb = null;
            }
            OffscreenFrameBuffer.Options opts = new OffscreenFrameBuffer.Options();
            opts.colorFormat = GLES30.GL_RGBA;
            opts.colorInternalFormat = GLES30.GL_RGBA8;
            opts.colorType = GLES30.GL_UNSIGNED_BYTE;
            ofb = OffscreenFrameBuffer.create(width, height, opts);
        }
        ofb.bind();
        r.run();
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, boundFbo[0]);
    }

    /**
     * Release the offscreen buffer.
     */
    public void release()
    {
        if (ofb != null)
        {
            ofb.release();
            ofb = null;
        }
    }
}
