package gov.takx.core.media;

import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * An SVG-specific {@link ImageReaderSpi} which allows the {@link SvgImageReader} to be registered and determine if it
 * should be used for a provided image source.
 *
 * @since 5.4
 */
public class SvgImageReaderSpi extends ImageReaderSpi
{
    public SvgImageReaderSpi()
    {
        super(
                "TAKX",
                "TAKX",
                new String[]{"svg", "SVG"},
                new String[]{"svg"},
                new String[]{"image/svg", "image/x-svg", "image/svg+xml", "image/svg-xml"},
                SvgImageReaderSpi.class.getName(),
                new Class[]{ImageInputStream.class},
                new String[0],
                false, null, null, null, null,
                true, null, null, null, null);
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException
    {
        if (source instanceof ImageInputStream is)
        {
            try
            {
                // ImageInputStream != InputStream, so much of the below is to deal with that difference
                is.mark();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = is.read(buffer);
                if (len <= 0) return false;
                while (len != -1)
                {
                    out.write(buffer, 0, len);
                    for (int i = 0; i < len; i++)
                    {
                        char c = (char) (buffer[i] & 0xFF); // ensure byte data is within char range
                        if (c == '\n' || c == '\r') continue;
                        if (c < 32 || c > 126) // only allow printable base ASCII: https://www.asciitable.com/
                        {
                            is.reset();
                            return false;
                        }
                    }
                    len = is.read(buffer);
                }
                return new SVGLoader().load(new ByteArrayInputStream(out.toByteArray()), null, LoaderContext.createDefault()) != null;
            } finally
            {
                is.reset();
            }
        }
        return false;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException
    {
        return new SvgImageReader(this);
    }

    @Override
    public String getDescription(Locale locale)
    {
        return "SVG Reader";
    }
}
