package gov.tak.platform.cot.detail;

import com.atakmap.coremap.maps.coords.NorthReference;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.dataservices.shapes.FieldOfView;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;

/**
 * Handles the {@code <sensor/> detail}.
 *
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class SensorDetailHandler extends CotDetailHandler
{
    private static final String DETAIL_NAME = "sensor";
    private static final String SENSOR_LOCATION_TYPE = "b-m-p-s-p-loc";
    private static final String ROLL_ATTRIBUTE = "roll";
    private static final String VFOV_ATTRIBUTE = "vfov";
    private static final String MODEL_ATTRIBUTE = "model";
    private static final String RANGE_ATTRIBUTE = "range";
    private static final String RANGE_LINES_ATTRIBUTE = "rangeLines";
    private static final String AZIMUTH_ATTRIBUTE = "azimuth";
    private static final String FOV_ATTRIBUTE = "fov";
    private static final String MAG_REF_ATTRIBUTE = "displayMagneticReference";
    private static final String HIDE_FOV = "hideFov";
    private static final String DISPLAY_LABELS = "fovLabels";
    private static final String FOV_ALPHA = "fovAlpha";
    private static final String FOV_RED = "fovRed";
    private static final String FOV_GREEN = "fovGreen";
    private static final String FOV_BLUE = "fovBlue";
    private static final String STROKE_COLOR = "strokeColor";
    private static final String STROKE_WEIGHT = "strokeWeight";
    private static final String RANGE_LINES_STROKE_COLOR = "rangeLineStrokeColor";
    private static final String RANGE_LINES_STROKE_WEIGHT = "rangeLineStrokeWeight";

    private static final String ELEVATION_ATTRIBUTE = "elevation";

    public SensorDetailHandler()
    {
        super(DETAIL_NAME);
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final var azimuth = parseDouble(detail.getAttribute(AZIMUTH_ATTRIBUTE), FieldOfView.DEFAULT_AZIMUTH);
        final var fovDegrees = parseDouble(detail.getAttribute(FOV_ATTRIBUTE), FieldOfView.DEFAULT_FOV);
        final var elevation = parseDouble(detail.getAttribute(ELEVATION_ATTRIBUTE), 0);
        final var linearRange = parseDouble(detail.getAttribute(RANGE_ATTRIBUTE), FieldOfView.DEFAULT_RANGE);
        final var range = !Double.isNaN(elevation) ? linearRange * Math.cos(Math.toRadians(elevation)) : linearRange;
        final var rangeLineSpacing = parseDouble(detail.getAttribute(RANGE_LINES_ATTRIBUTE), FieldOfView.DEFAULT_RANGE_LINE_SPACING);
        final var strokeWeight = parseDouble(detail.getAttribute(STROKE_WEIGHT), 0);
        final var rangeLineStrokeWeight = parseDouble(detail.getAttribute(RANGE_LINES_STROKE_WEIGHT), 0);
        final var magneticRef = parseInt(detail.getAttribute(MAG_REF_ATTRIBUTE), 0);
        final var vfov = parseInt(detail.getAttribute(VFOV_ATTRIBUTE), FieldOfView.DEFAULT_VFOV);
        final var roll = parseInt(detail.getAttribute(ROLL_ATTRIBUTE), FieldOfView.DEFAULT_ROLL);

        final var fillRed = parseDouble(detail.getAttribute(FOV_RED), 1);
        final var fillGreen = parseDouble(detail.getAttribute(FOV_GREEN), 1);
        final var fillBlue = parseDouble(detail.getAttribute(FOV_BLUE), 1);
        final var fillAlpha = parseDouble(detail.getAttribute(FOV_ALPHA), .3);
        final var strokeColor = new Color(parseInt(detail.getAttribute(STROKE_COLOR), Color.LIGHT_GRAY.getRGB()));
        final var rangeLineStrokeColor = new Color(parseInt(detail.getAttribute(RANGE_LINES_STROKE_COLOR), Color.LIGHT_GRAY.getRGB()));

        final var model = detail.hasAttribute(MODEL_ATTRIBUTE) ? detail.getAttribute(MODEL_ATTRIBUTE) : null;
        item.setMetaBoolean(StandardAttributeNames.SENSOR_FOV, true);

        final var hideFov = Boolean.parseBoolean(detail.getAttribute(HIDE_FOV)); // null = show the fov (hideFov = false)
        final var displayLabels = Boolean.parseBoolean(detail.getAttribute(DISPLAY_LABELS)); // null = hide the labels (displayLabels = false)

        final var marker = (Marker) item;
        final var visible = marker.isVisible() && !hideFov;
        final var fillColor = new Color((int) (fillRed * 255), (int) (fillGreen * 255), (int) (fillBlue * 255), (int) (fillAlpha * 255));

        // From ATAK: in the past we just failed, we should probably instead fill in the fields since
        // according to the sensor xsd most are optional and just not plot the field of view cone.
        if (Double.isNaN(azimuth) || Double.isNaN(fovDegrees) || Double.isNaN(range)) return ImportResult.SUCCESS;

        createOrUpdateFov(marker, azimuth, fovDegrees, range, fillColor, visible, displayLabels, rangeLineSpacing,
                elevation, strokeColor, strokeWeight, rangeLineStrokeColor, rangeLineStrokeWeight, magneticRef, vfov, roll, model);

        return ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail root)
    {
        if (!hasFoV(item)) return false;
        IMapObject mo = (IMapObject) item;
        FieldOfView fov = mo.getFieldOfView();
        if (fov == null) return false;
        final var sensor = new CotDetail(DETAIL_NAME);

        final var range = fov.getRange();
        final var elevation = fov.getElevation();
        final var slantRange = range / Math.cos(Math.toRadians(elevation));
        final var fillColor = fov.getFillColor();

        setDoubleAttribute(sensor, ELEVATION_ATTRIBUTE, elevation);
        setDoubleAttribute(sensor, RANGE_ATTRIBUTE, Math.round(slantRange));
        setDoubleAttribute(sensor, RANGE_LINES_ATTRIBUTE, fov.getRangeLineSpacing());
        setDoubleAttribute(sensor, AZIMUTH_ATTRIBUTE, fov.getAzimuth());
        setDoubleAttribute(sensor, FOV_ATTRIBUTE, fov.getFovDegrees());
        setDoubleAttribute(sensor, VFOV_ATTRIBUTE, fov.getVerticalFieldOfView());
        setDoubleAttribute(sensor, ROLL_ATTRIBUTE, fov.getRoll());
        setDoubleAttribute(sensor, FOV_ALPHA, fillColor.getAlpha() / 255d);
        setDoubleAttribute(sensor, FOV_RED, fillColor.getRed() / 255d);
        setDoubleAttribute(sensor, FOV_GREEN, fillColor.getGreen() / 255d);
        setDoubleAttribute(sensor, FOV_BLUE, fillColor.getBlue() / 255d);

        sensor.setAttribute(STROKE_COLOR, Integer.toString(fov.getStrokeColor().getRGB()));
        setDoubleAttribute(sensor, STROKE_WEIGHT, fov.getStrokeWeight());

        sensor.setAttribute(RANGE_LINES_STROKE_COLOR, Integer.toString(fov.getRangeLinesStrokeColor().getRGB()));
        setDoubleAttribute(sensor, RANGE_LINES_STROKE_WEIGHT, fov.getRangeLineStrokeWeight());

        sensor.setAttribute(MAG_REF_ATTRIBUTE, String.valueOf(fov.getNorthReference() == NorthReference.TRUE ? 0 : 1));
        sensor.setAttribute(HIDE_FOV, Boolean.toString(!fov.isVisible()));
        sensor.setAttribute(DISPLAY_LABELS, Boolean.toString(fov.isShowLabels()));

        final var model = fov.getModel();
        if (model != null) sensor.setAttribute(MODEL_ATTRIBUTE, model);
        root.addChild(sensor);
        return true;
    }

    private void setDoubleAttribute(CotDetail detail, String attributeName, double value)
    {
        detail.setAttribute(attributeName, Double.toString(value));
    }

    /**
     * @param item The map item to check
     * @return {@code true} if the item should have FOV data, {@code false} otherwise
     */
    public boolean hasFoV(@Nullable MapItem item)
    {
        // ATAK parity check - the SENSOR should have an FOV, but maintain this to ensure no broken legacy behavior.
        return item != null && (item.getCotType().equals(SENSOR_LOCATION_TYPE) || item.hasMetaValue(StandardAttributeNames.SENSOR_FOV));
    }

    /**
     * Creates or updates a {@link FieldOfView} for a {@link Marker} with the specified FOV values.
     *
     * @param marker The marker to update
     * @param azimuth The azimuth, in degrees from true north
     * @param fovDegrees The field of view, in angular degrees
     * @param range The field of view range
     * @param fillColor The value of each primary color, r-g-b, between 0 and 1.
     * @param visible The field of view visibility
     * @param displayLabels The label visibility
     * @param rangeLineSpacing The range line spacing
     * @param elevation The field of view elevation
     * @param strokeColor The field of view border and arrow stroke color
     * @param strokeWeight The field of view border and arrow stroke weight
     * @param rangeLineStrokeColor The range lines color
     * @param rangeLineStrokeWeight The range lines stroke weight
     */
    private void createOrUpdateFov(@Nonnull Marker marker, double azimuth, double fovDegrees, double range,
                                   @Nonnull Color fillColor, boolean visible, boolean displayLabels,
                                   double rangeLineSpacing, double elevation, @Nonnull Color strokeColor, double strokeWeight,
                                   @Nonnull Color rangeLineStrokeColor, double rangeLineStrokeWeight, int magneticRef,
                                   int vfov, int roll, @Nullable String model)
    {
        var fieldOfView = marker.getFieldOfView();
        final var newFieldOfView = fieldOfView == null;

        if (newFieldOfView) fieldOfView = new FieldOfView();

        fieldOfView.setPoint(marker.getLocation());
        fieldOfView.setAzimuth((float) azimuth);
        fieldOfView.setFovDegrees((float) fovDegrees);
        fieldOfView.setRange((float) range);
        fieldOfView.setFillColor(fillColor); // don't need to set alpha, it's set within the color
        fieldOfView.setVisible(visible);
        fieldOfView.setShowLabels(displayLabels);
        fieldOfView.setRangeLineSpacing((float) rangeLineSpacing);
        fieldOfView.setElevation((float) elevation);
        fieldOfView.setStrokeColor(strokeColor);
        fieldOfView.setStrokeWeight((float) strokeWeight);
        fieldOfView.setRangeLinesStrokeColor(rangeLineStrokeColor);
        fieldOfView.setRangeLineStrokeWeight((float) rangeLineStrokeWeight);
        fieldOfView.setNorthReference(magneticRef == 0 ? NorthReference.TRUE : NorthReference.MAGNETIC);
        fieldOfView.setVerticalFieldOfView(vfov);
        fieldOfView.setRoll(roll);
        fieldOfView.setModel(model);

        // Wait so listeners aren't added before initialization
        if (newFieldOfView) marker.setFieldOfView(fieldOfView);
    }
}
