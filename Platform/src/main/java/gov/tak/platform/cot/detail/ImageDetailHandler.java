package gov.tak.platform.cot.detail;

import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;

/**
 * Handle the "image" detail.  Decode the content, as embedded base64 encoded bytes, or from a URL, and add as an
 * attachment to the Map Object.
 *
 * @since 5.2
 */
public class ImageDetailHandler extends CotDetailHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String URL_ATTR = "url";

    // Image file magic headers, represented as unsigned bytes (thus mapped to int space and not as byte)
    private static final int[] PNG = {0x89, 0x50, 0x4E, 0x47};
    private static final int[] JPG = {0xFF, 0xD8, 0xFF};
    private static final int[] GIF = {0x47, 0x49, 0x46, 0x38};
    private static final int[] BMP = {0x42, 0x4D};

    ImageDetailHandler()
    {
        super("image");
    }

    @Override
    public ImportResult toItemMetadata(MapItem mapItem, CotEvent event, CotDetail detail)
    {
        if (detail.hasAttribute(URL_ATTR))
        {
            final String url = detail.getAttribute(URL_ATTR, "");
            if (!StringUtils.isBlank(url))
            {
                try (var in = new URL(url).openStream())
                {
                    addAttachment(mapItem, detail, in.readAllBytes());
                } catch (IOException e)
                {
                    logger.warn("Failed to read image from {}; error {}", url, e.getMessage());
                }
            }
        } else
        {
            addAttachment(mapItem, detail, Base64.getDecoder().decode(detail.getInnerText().getBytes(StandardCharsets.UTF_8)));
        }
        return ImportResult.SUCCESS;
    }

    /**
     * Add the given bytes as an attachment on the mapItem.
     * @since 5.5.1
     */
    private static void addAttachment(@Nonnull MapItem mapItem, @Nonnull CotDetail detail, @Nonnull byte[] bytes)
    {
        // Construct the attachment name
        final String mimeType = detail.getAttribute("mime", "image/jpg");
        final String attachmentName = getDateTimeString() + '.' + getExtensionForImage(bytes, mimeType);

        mapItem.addAttachment(attachmentName, new ByteArrayInputStream(bytes));
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail)
    {
        return detail.hasAttribute(URL_ATTR) || detail.getInnerText() != null;
    }

    /**
     * Returns the date string in a format that mimics what Android does when saving a new file.
     */
    private static String getDateTimeString()
    {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", LocaleUtil.getCurrent())
                .format(CoordinatedTime.currentDate());
    }

    /**
     * Determine an appropriate file extension for the given bytes and mime type.
     */
    private static String getExtensionForImage(byte[] bytes, String mimeType)
    {
        // First, try to identify the type from the actual bytes
        if (bytes.length < 4) return "dat";

        if (bytesMatch(bytes, PNG)) return "png";
        if (bytesMatch(bytes, JPG)) return "jpg";
        if (bytesMatch(bytes, GIF)) return "gif";
        if (bytesMatch(bytes, BMP)) return "bmp";

        // Otherwise, pull it from the given mime type
        final var parts = mimeType.toLowerCase().split("/");
        final var subType = parts.length > 1 ? parts[parts.length - 1] : parts[0];

        return switch (subType)
        {
            case "jpeg", "jpg", "png", "gif", "bmp" -> subType;
            default -> "dat";
        };
    }

    /**
     * Compare integer values against the beginning of the given {@code bytes}.
     * @return {@code true} if all given values match, {@code false} otherwise
     */
    private static boolean bytesMatch(byte[] bytes, int... values)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (((int) bytes[i]) != values[i]) return false;
        }
        return true;
    }

    @Override
    public boolean toCotDetail(MapItem mapItem, CotEvent event, CotDetail detail)
    {
        return false; // Not exported, to match ATAK handling
    }
}
