package gov.takx.core.view.gui.maptools.elevationtools.contour;

import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.contour.ContourLayerParameters;
import gov.tak.api.contour.ContourLine;
import gov.tak.api.contour.ContourLineFactory;
import gov.tak.api.contour.DefaultContourLineFactory;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.math.PointD;
import gov.tak.platform.graphics.PointF;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.terrain.experimental.ElevationSourceUtils;
import gov.takx.core.contour.ContourLinesService;
import gov.takx.core.globe.tak.elevation.ElevationDownloaderManager;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Dialog for updating {@link ContourLineFactory.Generation} parameters for newly created contour groups.
 *
 * @since 5.8
 */
@PseudoBean
public class GenerationDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ContourLineFactory lineFactory = new DefaultContourLineFactory();

    private static final double LAT_METERS_PER_DEGREE = 111139.0;
    private static final double MAX_SIZE_METERS = 100_000.0; // 100 km
    private static final double MAX_VISIBLE_ANGLE_DEGREES = 10.0;
    private static final double MAX_DEG_METERS = Math.toRadians(MAX_VISIBLE_ANGLE_DEGREES) * 6371000; // ~10 degrees in meters
    private static final Set<String> ALLOWED_ELEV_SOURCES = Set.of("DTED0"); // The upstream contour-generation system only allows DTED
    private static final ElevationSource.QueryParameters ELEVATION_SRC_FILTER = new ElevationSource.QueryParameters();
    private static final ElevationManager.QueryParameters ELEVATION_MAN_FILTER = new ElevationManager.QueryParameters();
    static
    {
        ELEVATION_SRC_FILTER.types = ALLOWED_ELEV_SOURCES;
        ELEVATION_MAN_FILTER.types = ALLOWED_ELEV_SOURCES;
    }

    @Inject
    private IExtendedGlobeManager globeManager;
    @Inject
    private ElevationDownloaderManager elevationDownloaderManager;
    @Inject
    private ICoreExecutorService executorService;

    private final ContourLineFactory.Generation generationParams;
    private final ContourLayerParameters layerParams;

    private int currentRenderId;

    /**
     * @param generationParams Target generation parameters to modify.
     * @param layerParams Layer parameters to use for styling / visualization.
     */
    public GenerationDialog(
                            @Nonnull ContourLineFactory.Generation generationParams,
                            @Nonnull ContourLayerParameters layerParams)
    {
        super("Contour generation options");

        this.generationParams = generationParams;
        this.layerParams = layerParams;

        initComponents();

        // Update to display current values.
        intervalSpinner.setValue(generationParams.getInterval());
        majorIntervalSpinner.setValue(generationParams.getNumMinorIntervalsPerMajor());
        intervalUnitsSpinner.setSelectedIndex(ContourUnits.toIndex(generationParams.getIntervalUnits()));
    }

    @PostConstruct
    private void postConstruct()
    {
        // Show initial display.
        updateDisplay();
    }

    /**
     * Update the {@link #contourPreviewLabel} to show a 2D visualization of the contour line generation parameters.
     * This shows a sample area with a valley and some mountains for some good variation. Users can interatively
     * tweak the parameters to see how that reflects in this area.
     */
    private void updateDisplay()
    {
        updateParametersLabel();

        // Grab the elevation query params used by the shared generation params.
        // We will be replacing these, and need to hold on to the original reference for restoration later.
        ElevationSource.QueryParameters restorableParams = Objects.requireNonNullElseGet(generationParams.getElevationQueryParams(), ElevationSource.QueryParameters::new);

        // We just want low resolution data for the preview, even if the user happens to have high resolution data available.
        // Since this is the shared params object, we need to clear the filter later. This gets done further below after displays are updated.
        generationParams.setElevationQueryParams(ELEVATION_SRC_FILTER);

        // Get the last globe the user interacted with. Should never be null in practice.
        IGlobeDelegate globe = globeManager.getLastFocusedGlobe();
        if (globe == null) return;

        // Get the visible bounds from the current globe/screen view
        GeoBounds visibleBounds = getVisibleTargetArea(globe);
        if (visibleBounds == null)
        {
            areaDescriptionLabel.setText("<html><font color=\"red\">Could not determine visible area</font></html>");
            return;
        }

        // Map the visible bounds to a rectangle.
        double south = visibleBounds.getSouth();
        double west = visibleBounds.getWest();
        double north = visibleBounds.getNorth();
        double east = visibleBounds.getEast();
        RaptorLocation topLeft = new RaptorLocation(north, west);
        RaptorLocation bottomRight = new RaptorLocation(south, east);
        Rectangle previewArea = new Rectangle("Preview Area", topLeft, bottomRight);

        // We can mutate the area on the generation parameters since
        // the contour lines service will mutate it later based on user area selection.
        ContourLinesService.fitArea(generationParams, previewArea);

        // Extracting the bounds from the spatial filter (matching the preview area)
        // so that we match the same 'strict mode' filtering logic of the underlying contour line factory.
        Envelope bounds = generationParams.getElevationQueryParams().spatialFilter.getEnvelope();

        // Extract coordinates, X=Lon, Y=Lat.
        double minLon = bounds.minX;
        double minLat = bounds.minY;
        double maxLon = bounds.maxX;
        double maxLat = bounds.maxY;
        double rangeX = maxLon - minLon;
        double rangeY = maxLat - minLat;

        // Check if we have elevation data for the location before generating a preview.
        double minCornerElev = ElevationManager.getElevation(new GeoPoint(minLat, minLon), ELEVATION_MAN_FILTER);
        double maxCornerElev = ElevationManager.getElevation(new GeoPoint(maxLat, maxLon), ELEVATION_MAN_FILTER);
        if (Double.isNaN(minCornerElev) || Double.isNaN(maxCornerElev))
        {
            // Data is missing. Schedule download, then generate preview upon completion.
            File cacheDir = elevationDownloaderManager.getDtedDir();
            Runnable onDownloadComplete = () -> {
                logger.debug("Elevation download complete. Attempting preview generation.");
                generateContourPreview(bounds, minLat, minLon, rangeX, rangeY, restorableParams);
            };
            SwingUtilities.invokeLater(() -> areaDescriptionLabel.setText("<html>Downloading elevation data...</html>"));

            try
            {
                ElevationSourceUtils.fillDtedCache(cacheDir,
                        new GeoBounds[]{EnvelopeUtils.toGeoBounds(bounds)},
                        onDownloadComplete,
                        executorService,
                        null);
            } catch (Exception e)
            {
                logger.error("Failed to schedule elevation download", e);
                SwingUtilities.invokeLater(() -> areaDescriptionLabel.setText("<html><font color=\"red\">Failed to download elevation data</font></html>"));
            }
        } else
        {
            // Data is present. Generate preview immediately.
            generateContourPreview(bounds, minLat, minLon, rangeX, rangeY, restorableParams);
        }
    }

    /**
     * Generate a preview of the given area and update the {@link #contourPreviewLabel} + {@link #contourFormatLabel}.
     *
     * @param bounds The bounds of the area to render.
     * @param minLat Min latitude for context.
     * @param minLon Min longitude for context.
     * @param rangeX The width of the area in degrees.
     * @param rangeY The height of the area in degrees.
     * @param restorableParams Elevation source parameters on the shared {@link #generationParams} to restore after preview completion.
     */
    private void generateContourPreview(@Nonnull Envelope bounds, double minLat, double minLon, double rangeX, double rangeY,
                                        @Nonnull ElevationSource.QueryParameters restorableParams)
    {
        // Queue up rendering the preview area with our current parameters.
        long renderId = ++currentRenderId;
        CompletableFuture.runAsync(() -> {
            Thread.currentThread().setName("Contour Preview Generation id=" + renderId);

            try
            {
                // Build the actual contours. Largely IO bound with GDAL.
                List<ContourLine> contourLines = lineFactory.buildContours(generationParams, null, null);

                // Check if this request is still valid (user may have changed params while waiting)
                if (currentRenderId != renderId) return;

                // If something went horribly wrong, we should bail.
                if (contourLines == null || contourLines.isEmpty())
                {
                    throw new IllegalStateException("Preview area failed to generate contour lines. Check elevation data availability.");
                }

                // Same matter as with mutating the shared generation parameter area.
                // The contour lines services will mutate it later based on user area selection.
                layerParams.setContourLines(contourLines);

                // Provide some context about the actual area being shown.
                Span intervalUnits = generationParams.getIntervalUnits();
                double minElevation = contourLines.get(0).getElevationMeters();
                double maxElevation = contourLines.get(contourLines.size() - 1).getElevationMeters();
                double areaMetersSquared = EnvelopeUtils.areaMetersSquared(bounds);
                double areaUnitsSquared = SpanUtilities.convert(areaMetersSquared, Span.METER, intervalUnits);
                String areaText = String.format("Sample area is %,d %s² — Elevation ranging from: %,d to %,d %s",
                        (long) areaUnitsSquared, intervalUnits.getPlural(),
                        (int) minElevation, (int) maxElevation, intervalUnits.getPlural());

                // Render the preview area.
                BufferedImage render = ContourMapRenderer.renderContourMap(
                        contourLines,
                        layerParams.getStyle(),
                        minLon, minLat,
                        rangeX, rangeY,
                        // Size offset by 2px to accommodate for 1px line border.
                        contourPreviewLabel.getWidth() - 2,
                        contourPreviewLabel.getHeight() - 2);

                // Update preview labels.
                SwingUtilities.invokeLater(() -> {
                    try
                    {
                        contourPreviewLabel.setIcon(new ImageIcon(render));
                        areaDescriptionLabel.setText(areaText);

                        // Minor adjustments in case output image bumps size up slightly.
                        pack();
                    } catch (Exception e)
                    {
                        logger.error("Error during UI update for contour preview", e);
                        areaDescriptionLabel.setText("<html><font color=\"red\">Error rendering preview</font></html>");
                    }
                });

            } catch (Throwable t)
            {
                logger.error("Failed to generate contour preview", t);

                // Make error apparent by updating the labels.
                SwingUtilities.invokeLater(() -> areaDescriptionLabel.setText("<html><font color=\"red\">Failed to generate preview: " + t.getMessage() + "</font></html>"));

                // Generally this should never occur so long as the bundled low-res elevation data can be queried.
                throw new CompletionException("Failed to generate contour preview", t);
            } finally
            {
                // Reset the elevation query parameters so usage outside the preview isn't limited to low-res elevation data.
                generationParams.setElevationQueryParams(restorableParams);
            }
        }, executorService);
    }

    /**
     * Create bounds for the generation area based on what is visible.
     *
     * @param globe Globe to pull positions from.
     *
     * @return Bounds in globe-space of what is visible, with some downscaling to keep final resulting area reasonably sized.
     *
     * @see #createBoundsFromCorners(List, MapSceneModel)
     * @see #createMaxSizeBoxCenteredOnCamera(MapSceneModel, GeoPoint)
     */
    @Nullable
    public static GeoBounds getVisibleTargetArea(@Nonnull IGlobeDelegate globe)
    {
        MapRenderer<?> renderer = globe.getPortableMapRenderer();
        if (renderer == null) return null;

        MapSceneModel mapSceneModel = renderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        if (mapSceneModel == null || mapSceneModel.camera == null) return null;

        // Raycast camera screen-space position to globe-space point.
        PointD centerPoint = mapSceneModel.camera.location;
        GeoPoint cameraCenter;
        if (centerPoint != null)
        {
            PointF centerScreen = new PointF(mapSceneModel.width / 2.0f, mapSceneModel.height / 2.0f);
            cameraCenter = inverse(mapSceneModel, centerScreen);
            if (cameraCenter == null) return null;
        } else
        {
            return null;
        }

        // Define the four corners of the screen.
        int width = mapSceneModel.width;
        int height = mapSceneModel.height;
        List<PointF> corners = new ArrayList<>(4);
        corners.add(new PointF(0, 0));          // Top-Left
        corners.add(new PointF(width, 0));      // Top-Right
        corners.add(new PointF(width, height)); // Bottom-Right
        corners.add(new PointF(0, height));     // Bottom-Left

        // Convert screen corners to GeoPoints.
        boolean allValid = true;
        List<GeoPoint> validCorners = new ArrayList<>();
        for (PointF corner : corners)
        {
            // Raycast the corner screen-space coordinate to globe-space.
            GeoPoint result = inverse(mapSceneModel, corner);
            if (result != null)
            {
                // Even if we get a result, it may be skewed if the camera is too heavily tilted.
                // We don't want to make an area that has some stupid aspect ratio.
                if (cameraCenter.distanceTo(result) > MAX_DEG_METERS)
                {
                    // This corner is too far away (likely tilted, and we hit some horizon point)
                    allValid = false;
                } else
                {
                    // This corner is reasonably close.
                    validCorners.add(result);
                }
            } else
            {
                allValid = false;
            }
        }

        // If all the corners have been validated we can create the bounds from those.
        // Otherwise, fall back to centered box strategy.
        if (allValid && validCorners.size() == 4)
        {
            return createBoundsFromCorners(validCorners, mapSceneModel);
        } else
        {
            return createMaxSizeBoxCenteredOnCamera(mapSceneModel, cameraCenter);
        }
    }

    /**
     * Create bounds for the generation area based on resolved points from the four visible corners of the screen.
     *
     * @param corners Points projected from the globe rendering scene/camera's corners.
     * @param mapSceneModel Globe rendering camera/scene model.
     *
     * @return Bounds in globe-space of what is visible, with some downscaling to keep final resulting area reasonably sized.
     */
    @Nonnull
    private static GeoBounds createBoundsFromCorners(@Nonnull List<GeoPoint> corners, @Nonnull MapSceneModel mapSceneModel)
    {
        // Get positions of where the scene's corners are.
        double minLat = 90.0;
        double maxLat = -90.0;
        double minLon = 180.0;
        double maxLon = -180.0;
        for (GeoPoint pt : corners)
        {
            double lat = pt.getLatitude();
            double lon = pt.getLongitude();

            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);

            minLon = Math.min(minLon, lon);
            maxLon = Math.max(maxLon, lon);
        }
        minLat = Math.max(-90, Math.min(90, minLat));
        maxLat = Math.max(-90, Math.min(90, maxLat));

        // Check if the box is too big, scaling it down if needed.
        GeoPoint minPoint = new GeoPoint(minLon, minLat);
        double latDist = minPoint.distanceTo(new GeoPoint(minLon, maxLat));
        double lonDist = minPoint.distanceTo(new GeoPoint(maxLon, minLat));
        double longestSide = Math.max(latDist, lonDist);
        if (longestSide > MAX_SIZE_METERS)
        {
            // If too big, scale down around the center.
            double centerLat = (minLat + maxLat) / 2.0;
            double centerLon = (minLon + maxLon) / 2.0;

            // Handle IDL wrapping.
            if (Math.abs(maxLon - minLon) > 180)
            {
                centerLon = (minLon + maxLon) / 2.0;
                if (centerLon >= 180) centerLon -= 360;
                if (centerLon < -180) centerLon += 360;
            }

            // Calculate half spans for 100km total size.
            double lonMetersPerDegree = LAT_METERS_PER_DEGREE * Math.cos(Math.toRadians(centerLat));
            if (Math.abs(lonMetersPerDegree) < 1.0) lonMetersPerDegree = 1.0; // Avoid division by zero at poles
            double aspectRatio = (double) mapSceneModel.width / mapSceneModel.height;
            double halfLatSpan;
            double halfLonSpan;
            if (aspectRatio >= 1.0)
            {
                // Landscape: Lon is longer. Cap Lon.
                halfLonSpan = MAX_SIZE_METERS / (2.0 * lonMetersPerDegree);
                halfLatSpan = halfLonSpan / aspectRatio;
            } else
            {
                // Portrait: Lat is longer. Cap Lat.
                halfLatSpan = MAX_SIZE_METERS / (2.0 * LAT_METERS_PER_DEGREE);
                halfLonSpan = halfLatSpan * aspectRatio;
            }
            minLat = centerLat - halfLatSpan;
            maxLat = centerLat + halfLatSpan;
            minLon = centerLon - halfLonSpan;
            maxLon = centerLon + halfLonSpan;

            // Normalize
            minLat = Math.max(-90, Math.min(90, minLat));
            maxLat = Math.max(-90, Math.min(90, maxLat));
            minLon = normalizeLon(minLon);
            maxLon = normalizeLon(maxLon);
        }

        return new GeoBounds(minLat, minLon, maxLat, maxLon);
    }

    /**
     * Creates bounds for the generation area based on expanding space from the given center point.
     * Used as a fallback when the corners cannot be resolved for use in {@link #createBoundsFromCorners(List, MapSceneModel)}.
     *
     * @param mapSceneModel Globe rendering camera/scene model.
     * @param cameraCenter Globe position at the center of the scene.
     *
     * @return Bounds in globe-space of what is visible, with some downscaling to keep final resulting area reasonably sized.
     */
    @Nullable
    private static GeoBounds createMaxSizeBoxCenteredOnCamera(@Nonnull MapSceneModel mapSceneModel, @Nullable GeoPoint cameraCenter)
    {
        if (cameraCenter == null) return null;
        double centerLat = cameraCenter.getLatitude();
        double centerLon = cameraCenter.getLongitude();

        // Calculate half spans for 100km total size.
        double lonMetersPerDegree = LAT_METERS_PER_DEGREE * Math.cos(Math.toRadians(centerLat));
        if (Math.abs(lonMetersPerDegree) < 1.0) lonMetersPerDegree = 1.0; // Avoid division by zero at poles
        double halfLatSpanDegrees = MAX_SIZE_METERS / (2.0 * LAT_METERS_PER_DEGREE);
        double halfLonSpanDegrees = MAX_SIZE_METERS / (2.0 * lonMetersPerDegree);

        // Normalize spans to the current viewport aspect ratio.
        double aspectRatio = (double) mapSceneModel.width / mapSceneModel.height;
        double targetHalfLatSpan;
        double targetHalfLonSpan;
        if (aspectRatio >= 1.0)
        {
            targetHalfLonSpan = halfLonSpanDegrees;
            targetHalfLatSpan = halfLonSpanDegrees / aspectRatio;
        } else
        {
            targetHalfLatSpan = halfLatSpanDegrees;
            targetHalfLonSpan = halfLatSpanDegrees * aspectRatio;
        }

        // Apply the 100km cap to the longest side.
        // Ensures the output doesn't get too big, which would cause noticeable performance issues.
        double actualLatSpanMeters = targetHalfLatSpan * 2.0 * LAT_METERS_PER_DEGREE;
        double actualLonSpanMeters = targetHalfLonSpan * 2.0 * lonMetersPerDegree;
        double dominantSpanMeters = Math.max(actualLatSpanMeters, actualLonSpanMeters);

        if (dominantSpanMeters > MAX_SIZE_METERS)
        {
            double scale = MAX_SIZE_METERS / dominantSpanMeters;
            targetHalfLatSpan *= scale;
            targetHalfLonSpan *= scale;
        }

        double minLat = centerLat - targetHalfLatSpan;
        double maxLat = centerLat + targetHalfLatSpan;
        double minLon = centerLon - targetHalfLonSpan;
        double maxLon = centerLon + targetHalfLonSpan;

        // Clamp Latitudes.
        minLat = Math.max(-90, Math.min(90, minLat));
        maxLat = Math.max(-90, Math.min(90, maxLat));

        // Normalize Longitudes.
        minLon = normalizeLon(minLon);
        maxLon = normalizeLon(maxLon);

        return new GeoBounds(minLat, minLon, maxLat, maxLon);
    }

    /**
     * @param lon Longitude.
     * @return Normalized [-180, 180] longitude.
     */
    private static double normalizeLon(double lon)
    {
        lon = lon % 360;
        if (lon < -180)
        {
            lon += 360;
        } else if (lon > 180)
        {
            lon -= 360;
        }
        return lon;
    }

    @Nullable
    private static GeoPoint inverse(@Nonnull MapSceneModel mapSceneModel, @Nonnull PointF point)
    {
        // Ugh...
        var ret = gov.tak.api.engine.map.coords.GeoPoint.createMutable();
        if (mapSceneModel.inverse(point, ret))
        {
            return MarshalManager.marshal(ret, gov.tak.api.engine.map.coords.GeoPoint.class, GeoPoint.class);
        }
        return null;
    }

    /**
     * Update the {@link #contourFormatLabel} with a simple presentation of contour line parameters.
     */
    private void updateParametersLabel()
    {
        // Rather than just doing 'N' items is major we can make it more clear for baseline cases like:
        //  1 -> every line is major
        //  2 -> every other line
        //  3 -> every 3rd line (and the pattern from here is consistent so we can just do Nth)
        int numMinorIntervalsPerMajor = generationParams.getNumMinorIntervalsPerMajor();
        String nd = switch (numMinorIntervalsPerMajor)
        {
            case 1 -> "";
            case 2 -> " other";
            case 3 -> " 3rd";
            default -> " " + numMinorIntervalsPerMajor + "th";
        };
        contourFormatLabel.setText(String.format(
                "Contour every %.1f %s, every%s line (%.1f %s) is major",
                generationParams.getInterval(),
                generationParams.getIntervalUnits().getPlural(),
                nd,
                numMinorIntervalsPerMajor * generationParams.getInterval(),
                generationParams.getIntervalUnits().getPlural()));
    }

    private void onIntervalChanged()
    {
        generationParams.setInterval((Double) intervalSpinner.getValue());
        updateDisplay();
    }

    private void onMajorIntervalChanged()
    {
        generationParams.setNumMinorIntervalsPerMajor((Integer) majorIntervalSpinner.getValue());
        updateDisplay();
    }

    private void onIntervalUnitsChanged()
    {
        generationParams.setIntervalUnits(ContourUnits.fromSelectionIndex(intervalUnitsSpinner.getSelectedIndex()));
        updateDisplay();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var wrapperPanel = new JPanel();
        contourPreviewLabel = new JLabel();
        areaDescriptionLabel = new JLabel();
        contourFormatLabel = new JLabel();
        separator1 = new JSeparator();
        var intervalLabel = new JLabel();
        intervalSpinner = new JSpinner();
        majorIntervalSpinner = new JSpinner();
        var majorIntervalLabel = new JLabel();
        var intervalUnitsLabel = new JLabel();
        intervalUnitsSpinner = new JComboBox<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setResizable(false);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== wrapperPanel ========
        {
            wrapperPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            wrapperPanel.setName("wrapperPanel");
            wrapperPanel.setLayout(new FormLayout(
                    "200dlu:grow, $lcgap, default",
                    "fill:200dlu:grow, 6*($lgap, default)"));

            //---- contourPreviewLabel ----
            contourPreviewLabel.setBorder(new LineBorder(UIManager.getColor("Group.separatorColor")));
            contourPreviewLabel.setName("contourPreviewLabel");
            wrapperPanel.add(contourPreviewLabel, cc.xywh(1, 1, 3, 1));

            //---- areaDescriptionLabel ----
            areaDescriptionLabel.setText("Area description");
            areaDescriptionLabel.setFont(areaDescriptionLabel.getFont().deriveFont(areaDescriptionLabel.getFont().getStyle() | Font.ITALIC));
            areaDescriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
            areaDescriptionLabel.setName("areaDescriptionLabel");
            wrapperPanel.add(areaDescriptionLabel, cc.xywh(1, 3, 3, 1));

            //---- contourFormatLabel ----
            contourFormatLabel.setHorizontalAlignment(SwingConstants.CENTER);
            contourFormatLabel.setText("Format");
            contourFormatLabel.setFont(contourFormatLabel.getFont().deriveFont(contourFormatLabel.getFont().getStyle() | Font.ITALIC));
            contourFormatLabel.setName("contourFormatLabel");
            wrapperPanel.add(contourFormatLabel, cc.xywh(1, 5, 3, 1));

            //---- separator1 ----
            separator1.setName("separator1");
            wrapperPanel.add(separator1, cc.xywh(1, 7, 3, 1));

            //---- intervalLabel ----
            intervalLabel.setText("Line every N units");
            intervalLabel.setName("intervalLabel");
            wrapperPanel.add(intervalLabel, cc.xy(1, 9));

            //---- intervalSpinner ----
            intervalSpinner.setModel(new SpinnerNumberModel(50.0, 1.0, null, 1.0));
            intervalSpinner.setName("intervalSpinner");
            intervalSpinner.addChangeListener(e -> onIntervalChanged());
            wrapperPanel.add(intervalSpinner, cc.xy(3, 9));

            //---- majorIntervalSpinner ----
            majorIntervalSpinner.setModel(new SpinnerNumberModel(2, 1, null, 1));
            majorIntervalSpinner.setName("majorIntervalSpinner");
            majorIntervalSpinner.addChangeListener(e -> onMajorIntervalChanged());
            wrapperPanel.add(majorIntervalSpinner, cc.xy(3, 11));

            //---- majorIntervalLabel ----
            majorIntervalLabel.setText("Major every N lines");
            majorIntervalLabel.setName("majorIntervalLabel");
            wrapperPanel.add(majorIntervalLabel, cc.xy(1, 11));

            //---- intervalUnitsLabel ----
            intervalUnitsLabel.setText("Interval units");
            intervalUnitsLabel.setName("intervalUnitsLabel");
            wrapperPanel.add(intervalUnitsLabel, cc.xy(1, 13));

            //---- intervalUnitsSpinner ----
            intervalUnitsSpinner.setModel(new DefaultComboBoxModel<>(new String[]{
                    "Meters",
                    "Feet",
                    "Yards"
            }));
            intervalUnitsSpinner.setName("intervalUnitsSpinner");
            intervalUnitsSpinner.addItemListener(e -> onIntervalUnitsChanged());
            wrapperPanel.add(intervalUnitsSpinner, cc.xy(3, 13));
        }
        contentPane.add(wrapperPanel, cc.xy(1, 1));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel contourPreviewLabel;
    private JLabel areaDescriptionLabel;
    private JLabel contourFormatLabel;
    private JSeparator separator1;
    private JSpinner intervalSpinner;
    private JSpinner majorIntervalSpinner;
    private JComboBox<String> intervalUnitsSpinner;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
