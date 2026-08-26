package gov.takx.core.globe.tak.features;

import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle.ScrollMode;
import com.atakmap.map.layer.feature.style.Style;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.dataservices.shapes.Arrow;
import gov.takx.platform.globe.GlobeTextUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * A {@link IFeatureProvider} for {@link IGlobeShapeMapObject} that represents an {@link Arrow} shape.
 *
 * @since 5.5
 */
public final class GlobeArrowFeatureProvider implements IFeatureProvider<IGlobeShapeMapObject, GlobeArrowFeatureProvider.GlobeArrowState>
{
    // Default maximum screen size of the arrowheads, in pixels taken from DirectedPath
    private static final float DEFAULT_MAX_SCREEN_SIZE = 20f;

    // Instances of our containing class are created quite frequently, so we need to keep the overhead of that creation
    // as low as possible.  Further, there is no "cleanup" model for provider instances, so no way to dispose of
    // resources like preference monitors. For those reasons, all our required monitors are kept in a single static instance.
    private static final Monitors MONITORS = new Monitors();

    @Nonnull
    @Override
    public GlobeArrowState createState(IGlobeShapeMapObject object)
    {
        Arrow arrow = object.requireShapeModel();
        AltitudeMode altitudeMode = AltitudeMode.ClampToGround;
        double extrusion = arrow.isExtruded() ? arrow.getVerticalHeight() : 0;
        int stippleFactor = arrow.getLineStippleFactor();
        short pattern = arrow.getLineStipplePattern();
        // GlobeArrowState requires a stipple value, so describe a solid line here
        if (stippleFactor == 0)
        {
            stippleFactor = 2;
            pattern = -1;
        }
        double arrowRadius = DEFAULT_MAX_SCREEN_SIZE / 1.5f;
        double bearing = arrow.getBearing();

        final String directionArrow = bearing > 180 && bearing < 360 ? "←" : "→";
        final String rangeLabelText = MONITORS.rangeUnitsMonitor.getCurrentValue().format(arrow.getRange(), 1);
        final String bearingLabelText = MONITORS.bearingUnits.getCurrentValue().format(arrow.getLocation(), arrow.getRange(), bearing);
        final String labelText = directionArrow + " " + rangeLabelText + "  " + bearingLabelText;

        return new GlobeArrowState(arrow.getVertices(), arrow.isVisible(), arrow.getLineColor().getRGB(), arrow.getLineWidth(),
                stippleFactor, pattern, arrowRadius, arrow.getLocation(), labelText, altitudeMode, extrusion);
    }

    @Override
    public int getFeatureCount(@Nonnull GlobeArrowState state)
    {
        return 1;
    }

    @Override
    public boolean isVisible(@Nonnull GlobeArrowState state)
    {
        return state.isVisible;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull GlobeArrowState state)
    {
        // Using the lineString as the geometry tells the kernel to render the text "somewhere along that line",
        // and it will automatically move the annotation if the view would clip it.  Using this instead of a
        // Point geometry brings feature parity with WinTAK and ATAK.

        final LineString lineString = new LineString(3);
        final boolean clampToGround = state.altitudeMode == AltitudeMode.ClampToGround;

        final double[] prev = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        state.locations.forEach(position -> {
            final double latitude = position.getLatitude();
            final double longitude = position.getLongitude();
            double hae = clampToGround ? 0 : position.getAltitude();
            if (latitude != prev[0] || longitude != prev[1] || hae != prev[2]) lineString.addPoint(longitude, latitude, hae);
            prev[0] = latitude;
            prev[1] = longitude;
            prev[2] = hae;
        });

        return lineString.getNumPoints() == 0 ? EMPTY_GEOMETRY : new Geometry[]{lineString};
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull GlobeArrowState state)
    {
        int stippleFactor = state.stippleFactor == 0 ? 2 : state.stippleFactor;
        Style outlineStyle = new ArrowStrokeStyle((float) state.arrowRadius, stippleFactor, state.pattern, state.diffuseColor, (float) state.outlineWidth, BasicStrokeStyle.EXTRUDE_NONE, ArrowStrokeStyle.ArrowHeadMode.PerVertex);
        outlineStyle = FeatureUtility.makeNoLabelStyle(outlineStyle);

        final Style style;
        if (!StringUtils.isEmpty(state.labelText))
        {
            Style arrowLabelStyle = new LabelPointStyle(state.labelText, -1, 0, ScrollMode.DEFAULT,
                    GlobeTextUtils.SHAPE_LABEL_FONT.getName(), GlobeTextUtils.SHAPE_LABEL_FONT.getSize(), EnumSet.noneOf(LabelPointStyle.Style.class),
                    0, 0, 0, 0, 0, false, 14d, 1);
            style = new CompositeStyle(new Style[]{outlineStyle, arrowLabelStyle});
        } else
        {
            style = outlineStyle;
        }
        return new Style[]{style};
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull GlobeArrowState state)
    {
        boolean isExtruded = state.extrusion != 0;
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(isExtruded ? AltitudeMode.Relative : AltitudeMode.ClampToGround, featureCount);
    }

    @Nonnull
    @Override
    public double[] createExtrusions(@Nonnull GlobeArrowState state)
    {
        return new double[]{state.extrusion};
    }

    /**
     * Holds our preference monitors.
     */
    private static class Monitors
    {
        private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, v -> {
        });
        private final IPreferenceMonitor<NorthReference> northReference = UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, v -> {
        });
        private final IPreferenceMonitor<BearingUnits> bearingUnits = UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, v -> {
        });
    }

    /**
     * State object for {@link GlobeArrowFeatureProvider}.
     */
    public record GlobeArrowState(List<ILocation> locations, boolean isVisible, int diffuseColor, double outlineWidth, int stippleFactor, short pattern, double arrowRadius, @Nullable ILocation labelLocation, @Nullable String labelText, AltitudeMode altitudeMode, double extrusion)
    {
    }
}
