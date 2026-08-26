package gov.takx.dataservices.shapes;

import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.math.MathUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.IPropertyChangeDelegateSupport;
import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.awt.Color;

/**
 * Defines a field of view shape.
 *
 * @since 5.8
 */
@PseudoBean
@IncubatingApi(since = "5.8")
public class FieldOfView implements IPropertyChangeDelegateSupport, IResourceCleaner
{
    public static final String ALPHA = "ALPHA";
    public static final String AZIMUTH = "AZIMUTH";
    public static final String ELEVATION = "ELEVATION";
    public static final String VISIBILITY = "VISIBILITY";
    public static final String FOV_DEGREES = "FOV_DEGREES";
    public static final String FOV_NORTH_REFERENCE = "FOV_NORTH_REFERENCE";
    public static final String LABELS = "LABELS";
    public static final String RANGE_LINES_COLOR = "RANGE_LINES_COLOR";
    public static final String RANGE_LINES_WEIGHT = "RANGE_LINES_WEIGHT";
    public static final String SHOW_FOV_LABELS = "SHOW_FOV_LABELS";
    public static final String MEDIA = "MEDIA";

    // Default values in line with WinTAK. Used to keep imports and default 
    public static final double DEFAULT_AZIMUTH = 0;
    public static final double DEFAULT_FOV = 10;
    public static final int DEFAULT_VFOV = 45;
    public static final int DEFAULT_ROLL = 0;
    public static final double DEFAULT_RANGE = 500;
    public static final double DEFAULT_RANGE_LINE_SPACING = 100;

    private static final int MAX_SENSOR_RANGE = 60000; // ATAK & WinTAK parity
    private static final double MAX_ANGLE = 359.9999;

    private static final String INVALID_ANGLE = "%s is out of range [0, " + MAX_ANGLE + "\u00b0], clamping to " + MAX_ANGLE;
    private static final String INVALID_RANGE = "%s is out of range [0, " + MAX_SENSOR_RANGE + "m]";

    @JsonIgnore
    private final PrioritizedPropertyChangeSupport propertyChangeSupport = new PrioritizedPropertyChangeSupport(this);

    @JsonIgnore
    @Inject
    private INotificationService notificationService;

    @JsonIgnore
    @Inject
    private IMediaManager mediaManager;

    private IGeoPoint point = GeoPoint.UNKNOWN_POINT;
    private IGeoPoint extentPoint = GeoPoint.UNKNOWN_POINT;
    private boolean visible = true;
    private double azimuth = DEFAULT_AZIMUTH;
    private NorthReference northReference = NorthReference.TRUE;
    private double fovDegrees = DEFAULT_FOV;
    private int verticalFieldOfView = DEFAULT_VFOV; // view up and down
    private int roll = DEFAULT_ROLL;
    private double range = DEFAULT_RANGE;
    private double elevation = 0;
    private Color fillColor = new Color(255, 255, 255, 77);
    private Color strokeColor = Color.LIGHT_GRAY;
    private double strokeWeight = 0;
    private boolean showLabels;
    private double rangeLineSpacing = DEFAULT_RANGE_LINE_SPACING;
    private Color rangeLinesStrokeColor = Color.LIGHT_GRAY;
    private double rangeLineStrokeWeight = 1;
    private String mediaSourceKey;
    private String mediaName;
    private String model;

    @Override
    public void releaseResources()
    {
        propertyChangeSupport.removeAllListeners();
    }

    @Nonnull
    @Override
    public IPropertyChangeListenerSupport getPropertyChangeListenerSupport()
    {
        return propertyChangeSupport;
    }

    /**
     * @return The field of view origin point; will be {@link GeoPoint#UNKNOWN_POINT} until initialized
     */
    @Nonnull
    public IGeoPoint getPoint()
    {
        return point;
    }

    /**
     * @param point The field of view origin point
     */
    public void setPoint(@Nonnull IGeoPoint point)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.LOCATION, this.point, point, () -> {
            this.point = new GeoPoint(point);
            updateExtentPoint();
            updateLabels();
        });
    }

    /**
     * @param extentPoint the Field of View end point, where the arrow will point to.
     */
    public void setExtentPoint(@Nonnull IGeoPoint extentPoint) throws IllegalArgumentException
    {
        final var range = GeoCalculations.distance(point, extentPoint);

        if (range > MAX_SENSOR_RANGE || range < 0)
        {
            var message = INVALID_RANGE.formatted("Extent Point");
            notificationService.show(Notification.builder(message, Notification.Type.Warning).build());
            throw new IllegalArgumentException(message);
        }

        setRange(range);
        setAzimuth(GeoCalculations.bearing(point, extentPoint));
        setNorthReference(this.northReference); // need to reset the north reference to ensure extent point is calculated appropriately
        updateExtentPoint();
    }

    /**
     * @return The field of view extent point; will be {@link GeoPoint#UNKNOWN_POINT} until initialized
     */
    @Nonnull
    public IGeoPoint getExtentPoint()
    {
        return extentPoint;
    }

    /**
     * @return the azimuth, in degrees reference {@link #getNorthReference()}
     */
    public double getAzimuth()
    {
        return azimuth;
    }

    /**
     * @return the azimuth, in degrees {@link NorthReference#TRUE}
     */
    public double getAzimuthTrueNorth()
    {
        return getNorthReference() == NorthReference.TRUE ? azimuth : GeoCalculations.convertFromMagneticToTrue(point, azimuth);
    }

    /**
     * @param azimuth The azimuth, in degrees reference {@link #getNorthReference()}. The value will be clamped [0, {@link #MAX_ANGLE}].
     */
    public void setAzimuth(double azimuth)
    {
        if (azimuth > MAX_ANGLE || azimuth < 0) notificationService.show(Notification.builder(INVALID_ANGLE.formatted("Azimuth"), Notification.Type.Warning).build());
        final var clampedAzimuth = MathUtils.clamp(azimuth, 0, MAX_ANGLE);

        propertyChangeSupport.updateAndFire(AZIMUTH, this.azimuth, clampedAzimuth, () -> {
            this.azimuth = clampedAzimuth;
            updateExtentPoint();
            updateLabels();
        });
    }

    /**
     * @return the {@link NorthReference} of the FOV currently.
     */
    @Nonnull
    public NorthReference getNorthReference()
    {
        return northReference;
    }

    /**
     * Set the {@link NorthReference} of the FOV. Only accepts {@link NorthReference#TRUE TRUE}
     * and {@link NorthReference#MAGNETIC MAGNETIC} - {@link NorthReference#GRID GRID} is ignored.
     * @param northReference the new reference to use.
     */
    public void setNorthReference(@Nonnull NorthReference northReference) throws IllegalArgumentException
    {
        if (this.northReference == northReference) return;
        if (northReference == NorthReference.GRID) throw new IllegalArgumentException("NorthReference GRID is unsupported, use either TRUE or MAGNETIC");
        propertyChangeSupport.updateAndFire(FOV_NORTH_REFERENCE, this.northReference, northReference, () -> {
            this.northReference = northReference;
            var azimuth = this.azimuth;
            if (northReference == NorthReference.MAGNETIC)
            {
                azimuth = GeoCalculations.convertFromTrueToMagnetic(point, azimuth);
            } else
            {
                azimuth = GeoCalculations.convertFromMagneticToTrue(point, azimuth);
            }
            setAzimuth(azimuth);
            updateExtentPoint();
            updateLabels();
        });
    }

    /**
     * @return The field of view, in angular degrees
     */
    public double getFovDegrees()
    {
        return fovDegrees;
    }

    /**
     * @param fovDegrees The field of view, in angular degrees. The value will be clamped [0, {@link #MAX_ANGLE}].
     */
    public void setFovDegrees(double fovDegrees)
    {
        if (fovDegrees > MAX_ANGLE || fovDegrees < 0) notificationService.show(Notification.builder(INVALID_ANGLE.formatted("FOV Degrees"), Notification.Type.Warning).build());
        final var clampedFovDegrees = MathUtils.clamp(fovDegrees, 0, MAX_ANGLE);

        propertyChangeSupport.updateAndFire(FOV_DEGREES, this.fovDegrees, clampedFovDegrees, () -> {
            this.fovDegrees = clampedFovDegrees;
            updateLabels();
        });
    }

    /**
     * @return The field of view range, in meters. Note that the extent is the ground range and not the slant range.
     */
    public double getRange()
    {
        return range;
    }

    /**
     * @param range The field of view range, in meters. The value will be clamped between 0 and {@link #MAX_SENSOR_RANGE}.
     *              Note that the extent is the ground range and not the slant range.
     */
    public void setRange(double range) throws IllegalArgumentException
    {
        if (range > MAX_SENSOR_RANGE || range < 0)
        {
            var message = INVALID_RANGE.formatted("Range value");
            notificationService.show(Notification.builder(message, Notification.Type.Warning).build());
            throw new IllegalArgumentException(message);
        }
        final var clampedRange = MathUtils.clamp(range, 0, MAX_SENSOR_RANGE);

        propertyChangeSupport.updateAndFire(ShapeProperties.RANGE, this.range, clampedRange, () -> {
            this.range = clampedRange;
            updateExtentPoint();
            updateLabels();
        });
    }

    /**
     * Sets the value of the fill alpha, between 0 and 255. The value will be clamped to stay in range.
     *
     * @param alpha The fill alpha
     */
    public void setFillAlpha(int alpha)
    {
        setFillColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), MathUtils.clamp(alpha, 0, 255)));
    }

    /**
     * @return The fill {@link Color}
     */
    public Color getFillColor()
    {
        return fillColor;
    }

    /**
     * @param fillColor the new fill color
     */
    public void setFillColor(@Nonnull Color fillColor)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.FILL_COLOR, this.fillColor, fillColor, () -> this.fillColor = fillColor);
    }

    /**
     * @return {@code true} if the field of view should be visible
     */
    public boolean isVisible()
    {
        return visible;
    }

    /**
     * @param visible {@code true} if the field of view should be visible
     */
    public void setVisible(boolean visible)
    {
        propertyChangeSupport.updateAndFire(FieldOfView.VISIBILITY, this.visible, visible, () -> this.visible = visible);
    }

    /**
     * @return {@code true} if labels displaying the field of view edge angles should be visible
     */
    public boolean isShowLabels()
    {
        return showLabels;
    }

    /**
     * @param showLabels {@code true} to show labels that display the field of view edge angles
     */
    public void setShowLabels(boolean showLabels)
    {
        propertyChangeSupport.updateAndFire(SHOW_FOV_LABELS, this.showLabels, showLabels, () -> {
            this.showLabels = showLabels;
            updateLabels();
        });
    }

    /**
     * @return The spacing between range lines, in meters
     */
    public double getRangeLineSpacing()
    {
        return rangeLineSpacing;
    }

    /**
     * @param rangeLineSpacing The spacing between range lines, in meters. The value will be clamped between 0 and {@link #MAX_SENSOR_RANGE}.
     */
    public void setRangeLineSpacing(double rangeLineSpacing)
    {
        final var clampedRangeLines = MathUtils.clamp(rangeLineSpacing, 0, MAX_SENSOR_RANGE);

        propertyChangeSupport.updateAndFire(ShapeProperties.RANGE_DISTANCES, this.rangeLineSpacing, clampedRangeLines, () -> this.rangeLineSpacing = clampedRangeLines);
    }

    /**
     * @return The field of view elevation
     */
    public double getElevation()
    {
        return elevation;
    }

    /**
     * @param elevation The field of view elevation. The value will be clamped between {@link Integer#MIN_VALUE} and {@link Integer#MAX_VALUE}.
     */
    public void setElevation(double elevation)
    {
        final var clampedElevation = Double.isNaN(elevation) ? elevation : MathUtils.clamp(elevation, Integer.MIN_VALUE, Integer.MAX_VALUE);

        propertyChangeSupport.updateAndFire(ELEVATION, this.elevation, clampedElevation, () -> {
            this.elevation = clampedElevation;
            updateExtentPoint();
            updateLabels();
        });
    }

    /**
     * @return the field of view stroke color
     */
    public Color getStrokeColor()
    {
        return strokeColor;
    }

    /**
     * @param strokeColor The field of view stroke color
     */
    public void setStrokeColor(@Nonnull Color strokeColor)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.LINE_COLOR, this.strokeColor, strokeColor, () -> this.strokeColor = strokeColor);
    }

    /**
     * @return the field of view stroke weight
     */
    public double getStrokeWeight()
    {
        return strokeWeight;
    }

    /**
     * @param strokeWeight The field of view stroke weight
     */
    public void setStrokeWeight(double strokeWeight)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.LINE_WIDTH, this.strokeWeight, strokeWeight, () -> this.strokeWeight = strokeWeight);
    }

    /**
     * @return the range lines stroke color
     */
    public Color getRangeLinesStrokeColor()
    {
        return rangeLinesStrokeColor;
    }

    /**
     * @param rangeLinesStrokeColor The range lines stroke color
     */
    public void setRangeLinesStrokeColor(@Nonnull Color rangeLinesStrokeColor)
    {
        propertyChangeSupport.updateAndFire(RANGE_LINES_COLOR, this.rangeLinesStrokeColor, rangeLinesStrokeColor,
                () -> this.rangeLinesStrokeColor = rangeLinesStrokeColor);
    }

    /**
     * @return the range lines stroke weight
     */
    public double getRangeLineStrokeWeight()
    {
        return rangeLineStrokeWeight;
    }

    /**
     * @param rangeLineStrokeWeight The range lines stroke weight
     */
    public void setRangeLineStrokeWeight(double rangeLineStrokeWeight)
    {
        propertyChangeSupport.updateAndFire(RANGE_LINES_WEIGHT, this.rangeLineStrokeWeight, rangeLineStrokeWeight,
                () -> this.rangeLineStrokeWeight = rangeLineStrokeWeight);
    }

    /**
     * @param media the media to associate with this FOV
     */
    @JsonIgnore
    public void setMedia(@Nullable IMedia media)
    {
        IMedia oldMedia = (this.mediaSourceKey == null || this.mediaName == null) ? null : mediaManager.getMedia(this.mediaSourceKey, this.mediaName);
        propertyChangeSupport.updateAndFire(MEDIA, oldMedia, media, () -> {
            this.mediaSourceKey = media == null ? null : media.getKeyOfSource();
            this.mediaName = media == null ? null : media.getName();
        });
    }

    /**
     * @return the media associated with this FOV
     */
    @JsonIgnore
    @Nullable
    public IMedia getMedia()
    {
        if (mediaSourceKey == null || mediaName == null) return null;
        return mediaManager.getMedia(mediaSourceKey, mediaName);
    }

    /**
     * Updates the {@link #extentPoint}. Should be called whenever the field of view geometry changes.
     */
    private void updateExtentPoint()
    {
        var azimuth = northReference == NorthReference.TRUE ? this.azimuth : GeoCalculations.convertFromMagneticToTrue(point, this.azimuth);
        extentPoint = GeoCalculations.pointAtDistance(point, azimuth, range, elevation);
    }

    /**
     * @param verticalFieldOfView the new vertical field of view, clamped between [0, {@link #MAX_ANGLE}].
     */
    public void setVerticalFieldOfView(int verticalFieldOfView)
    {
        this.verticalFieldOfView = (int) MathUtils.clamp(verticalFieldOfView, 0, MAX_ANGLE);
    }

    /**
     * @return the vertical field of view, between [0, {@link #MAX_ANGLE}].
     */
    public int getVerticalFieldOfView()
    {
        return verticalFieldOfView;
    }

    /**
     * @param roll the new roll of the field of view, clamped between [0, {@link #MAX_ANGLE}].
     */
    public void setRoll(int roll)
    {
        this.roll = (int) MathUtils.clamp(roll, 0, MAX_ANGLE);
    }

    /**
     * @return the roll of the field of view, between [0, {@link #MAX_ANGLE}].
     */
    public int getRoll()
    {
        return roll;
    }

    public void setModel(@Nullable String model)
    {
        this.model = model;
    }

    @Nullable
    public String getModel()
    {
        return model;
    }

    /**
     * Fire a PCE to update the labels on the globe.
     */
    private void updateLabels()
    {
        propertyChangeSupport.firePropertyChange(LABELS, null, null);
    }
}
