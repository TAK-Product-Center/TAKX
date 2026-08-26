package gov.takx.core.media;

import com.github.weisj.jsvg.parser.SVGLoader;
import gov.takx.platform.ui.UIResources;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * An SVG-specific {@link ImageReader} to provide image decoding and rendering functionality.
 * 
 * @since 5.4
 */
public class SvgImageReader extends ImageReader
{
    SvgImageReader(ImageReaderSpi originatingProvider)
    {
        super(originatingProvider);
    }

    @Override
    public int getNumImages(boolean allowSearch)
    {
        return 1;
    }

    @Override
    public int getWidth(int imageIndex)
    {
        return 0;
    }

    @Override
    public int getHeight(int imageIndex)
    {
        return 0;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex)
    {
        return Collections.singletonList(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)).iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata()
    {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex)
    {
        return null;
    }

    @Override
    public ImageReadParam getDefaultReadParam()
    {
        return new SvgImageReadParam();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException
    {
        if (input instanceof ImageInputStream is)
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = is.read(buffer);
            while (len != -1)
            {
                out.write(buffer, 0, len);
                len = is.read(buffer);
            }
            Dimension imageSize = param.getSourceRenderSize();
            if (imageSize == null) imageSize = new Dimension(-1, -1);
            return UIResources.getSvg(new SVGLoader(), new ByteArrayInputStream(out.toByteArray()), imageSize.width, imageSize.height, BufferedImage.TYPE_INT_ARGB, false);
        }
        throw new IOException("SVG input cannot be decoded");
    }

    /**
     * An SVG-specific {@link ImageReadParam} to allow the reader to read in the image at a set size. This extension is required as
     * the ImageReadParam class does not allow this by default.
     */
    private static class SvgImageReadParam extends ImageReadParam
    {
        private SvgImageReadParam()
        {
            canSetSourceRenderSize = true;
        }
    }
}