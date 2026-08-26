package gov.tak.platform.contour;

import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.contour.ContourLine;
import gov.tak.api.contour.ContourStyle;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.layer.IRaptorLayer;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Data wrapper of contour line information.
 *
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
@NoPluginImplementations
public interface ContourLineGroup
{
    /**
     * @return Contour line models.
     */
    @Nonnull
    List<ContourLine> getContourLines();

    /**
     * @return Contour line layer.
     */
    @Nonnull
    IRaptorLayer getLayer();

    /**
     * @return The contour style, allowing for dynamic control of the layer's visuals.
     */
    @NonNull
    ContourStyle getStyle();

    /**
     * @return Bounds of this layer's contours.
     */
    @NonNull
    Envelope getBoundingBox();

    /**
     * @return Lowest elevation observed in the contour layer.
     */
    double getMinElevationMeters();

    /**
     * @return Highest elevation observed in the contour layer.
     */
    double getMaxElevationMeters();
}
