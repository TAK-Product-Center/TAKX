package gov.takx.core.globe.tak.features;

import android.graphics.Color;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.PatternStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.globe.tak.FeatureUtility;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.function.Function;

/**
 * A utility class for feature providers.
 *
 * @since 3.5
 */
final class FeatureProviderUtility
{
    // A cache of AltitudeMode arrays of array sizes 0..7 for each AltitudeMode
    @SuppressWarnings("unchecked")
    private static final EnumMap<AltitudeMode, AltitudeMode[]>[] altitudeModeCache = new EnumMap[8];

    /**
     * Create a stroke style according to the given attributes.  If the attributes configure a stipple pattern,
     * then a {@link PatternStrokeStyle} will created, otherwise a {@link BasicStrokeStyle} will be created.
     *
     * @param lineWidth Width of line
     * @param lineColorArgb Color of line
     * @param stippleFactor the stipple factor
     * @param stipplePattern the stipple pattern
     * @return Appropriate stroke style
     * @see ShapeAttributes#getOutlineStippleFactor()
     * @see ShapeAttributes#getOutlineStipplePattern()
     * @since 5.0
     */
    static Style createStrokeStyle(double lineWidth, int lineColorArgb, int stippleFactor, short stipplePattern)
    {
        if (stippleFactor > 0)
        {
            final float r = Color.red(lineColorArgb) / 255f;
            final float g = Color.green(lineColorArgb) / 255f;
            final float b = Color.blue(lineColorArgb) / 255f;
            final float a = Color.alpha(lineColorArgb) / 255f;
            return new PatternStrokeStyle(stippleFactor, stipplePattern, r, g, b, a, (float) lineWidth, BasicStrokeStyle.EXTRUDE_NONE);
        } else
        {
            return new BasicStrokeStyle(lineColorArgb, (float) lineWidth, BasicStrokeStyle.EXTRUDE_NONE);
        }
    }

    /**
     * Convert a {@link WorldWind} altitude constant to an array of {@link AltitudeMode} of size {@code count}.
     * @param wwMode the WorldWind altitude mode
     * @param count the size of the returned array
     * @return an array of AltitudeMode elements
     */
    static AltitudeMode[] getAltitudeModeArrayForWWMode(int wwMode, int count)
    {
        return switch (wwMode)
        {
            case WorldWind.CLAMP_TO_GROUND -> getAltitudeModeArray(AltitudeMode.ClampToGround, count);
            case WorldWind.RELATIVE_TO_GROUND -> getAltitudeModeArray(AltitudeMode.Relative, count);
            default /*WorldWind.ABSOLUTE*/ -> getAltitudeModeArray(AltitudeMode.Absolute, count);
        };
    }

    /**
     * Create an array of a {@link AltitudeMode} of size {@code featureCount}.
     * @param altitudeMode the altitude mode
     * @param count the size of the returned array
     * @return an array of AltitudeMode elements
     */
    synchronized static AltitudeMode[] getAltitudeModeArray(AltitudeMode altitudeMode, int count)
    {
        Function<AltitudeMode, AltitudeMode[]> allocAltitudeModeArray = a -> {
            AltitudeMode[] altitudeModes = new AltitudeMode[count];
            Arrays.fill(altitudeModes, altitudeMode);
            return altitudeModes;
        };

        if (count < altitudeModeCache.length)
        {
            if (altitudeModeCache[count] == null) altitudeModeCache[count] = new EnumMap<>(AltitudeMode.class);
            return altitudeModeCache[count].computeIfAbsent(altitudeMode, allocAltitudeModeArray);
        } else
        {
            return allocAltitudeModeArray.apply(altitudeMode);
        }
    }

    /**
     * Sanitizes URIs from WW for use with the TAK feature renderer.
     * <ul>
     * <li>Windows paths can produce URIs with "%5C" in place of backslashes.</li>
     * <li>Windows file URIs are formatted either "file:path" or "file:/path" and TAK feature renderer expects "file://path".</li>
     * </ul>
     * @param uri the uri
     * @return the sanitized uri
     */
    static String sanitizeUri(String uri)
    {
        return FeatureUtility.sanitizeUri(uri);
    }

    /**
     * Persist a {@link BufferedImage} in the Virtual File System and return its URI.
     * @param image the image
     * @param projectVFSManager the project VFS manager
     * @return a URI that refers to the persisted image
     */
    static String persistImageInVFS(BufferedImage image, IVirtualFileSystemManager projectVFSManager)
    {
        // Get the image persisted in VFS
        final IVirtualFileSystem vfs = projectVFSManager.getVirtualFileSystem(IMapObject.MAP_OBJECT_ICONS_DIR);
        return ImageUtils.writeImageToCanonicalFile(vfs.getRootDir(), image).toString();
    }
}
