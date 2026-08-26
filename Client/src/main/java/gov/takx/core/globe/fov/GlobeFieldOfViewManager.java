package gov.takx.core.globe.fov;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.AngleUtilities;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.Marker;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.layer.feature.ISharedFeatureDataStore;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.commons.lang.ErrorThrowingRunnable;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.shapes.FieldOfView;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.platform.globe.GlobeTextUtils.DEFAULT_GLOBE_FONT;

/**
 * Manages the field of view renderables associated with {@link FieldOfView} instances.
 *
 * @since 5.8
 */
@ApplicationScoped
public class GlobeFieldOfViewManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String FEATURE_SET_PROVIDER_NAME = MethodHandles.lookup().lookupClass().getName();
    private static final List<String> MO_PROPERTIES = List.of(IMapObject.FIELD_OF_VIEW, IMapObject.VISIBILITY);
    private static final List<String> FOV_PROPERTIES = List.of(FieldOfView.ALPHA, FieldOfView.AZIMUTH, FieldOfView.ELEVATION,
            FieldOfView.FOV_DEGREES, FieldOfView.RANGE_LINES_COLOR, FieldOfView.RANGE_LINES_WEIGHT, FieldOfView.SHOW_FOV_LABELS,
            FieldOfView.MEDIA, FieldOfView.VISIBILITY, ShapeProperties.FILL_COLOR, ShapeProperties.LINE_COLOR, ShapeProperties.LINE_WIDTH,
            ShapeProperties.LOCATION, ShapeProperties.RANGE, ShapeProperties.RANGE_DISTANCES, ShapeProperties.VISIBILITY);
    private static final int SLICES_PER_90 = 8;
    private static final int STROKE_WEIGHT_SCALAR = 8; // value picked to mimic ATAK scaling

    private final FieldOfViewPropertyChangeListener fovPropertyChangeListener = new FieldOfViewPropertyChangeListener();
    private final MapObjectPropertyChangeListener moPropertyChangeListener = new MapObjectPropertyChangeListener();

    private final Map<FieldOfView, GlobeFieldOfView> fovToGlobeFov = new ConcurrentHashMap<>();
    private final FeatureDataStore4 dataStore;

    @Inject
    GlobeFieldOfViewManager(ISharedFeatureDataStore dataStore)
    {
        this.dataStore = dataStore;
    }

    @SuppressWarnings("unused")
    private void onMapObjectCreated(@Observes MapObjectAdded mapObjectAdded)
    {
        final var mapObject = mapObjectAdded.getMapObject();
        if (!(mapObject instanceof Marker)) return;
        final var fieldOfView = mapObject.getFieldOfView();
        if (fieldOfView != null) createGlobeFieldOfView(mapObject, fieldOfView);

        mapObject.addPropertyChangeListener(MO_PROPERTIES, moPropertyChangeListener);
        mapObject.addDeletionHandler(this::removeFieldOfView);
    }

    /**
     * Creates a new {@link GlobeFieldOfView}, including all its feature sets and features.
     * 
     * @param mapObject The map object owner
     * @param fieldOfView The map object's field of view definition.
     */
    private void createGlobeFieldOfView(@Nonnull IMapObject mapObject, @Nonnull FieldOfView fieldOfView)
    {
        lockAndExecute(() -> {
            final var visible = mapObject.isVisible() && fieldOfView.isVisible();
            final var labelsAndRangeLinesVisible = visible & fieldOfView.isShowLabels();
            final var uid = mapObject.getUid();

            // Holds polygon, arrow and labels
            final var featureSet = new FeatureSet(FEATURE_SET_PROVIDER_NAME, ISharedFeatureDataStore.ALL_GLOBES_FEATURE_SET_TYPE, uid, Double.MAX_VALUE, 0);
            final long fsid = dataStore.insertFeatureSet(featureSet);
            dataStore.setFeatureSetVisible(fsid, visible);

            final var polygonFeature = new Feature(fsid, null, createPolygonGeometry(fieldOfView), createPolygonStyle(fieldOfView), null);
            final var polygonFid = dataStore.insertFeature(polygonFeature);

            final var arrowFeature = new Feature(fsid, null, createArrowGeometry(fieldOfView), createArrowStyle(fieldOfView), null);
            final var arrowFid = dataStore.insertFeature(arrowFeature);

            final var leftLabelFeature = new Feature(fsid, null, createLabelGeometry(fieldOfView, true), createLabelStyle(fieldOfView, true), null);
            final var leftLabelFid = dataStore.insertFeature(leftLabelFeature);
            dataStore.setFeatureVisible(leftLabelFid, labelsAndRangeLinesVisible);

            final var rightLabelFeature = new Feature(fsid, null, createLabelGeometry(fieldOfView, false), createLabelStyle(fieldOfView, false), null);
            final var rightLabelFid = dataStore.insertFeature(rightLabelFeature);
            dataStore.setFeatureVisible(rightLabelFid, labelsAndRangeLinesVisible);

            // Holds multiple range line features; even though labels share the same visibility, did not include them
            // so we can easily clear/rebuilt the entire set when the lines change.
            final var rangeLinesFeatureSet = new FeatureSet(FEATURE_SET_PROVIDER_NAME, ISharedFeatureDataStore.ALL_GLOBES_FEATURE_SET_TYPE, null, Double.MAX_VALUE, 0);
            final long rangeLinesFsid = dataStore.insertFeatureSet(rangeLinesFeatureSet);
            dataStore.setFeatureSetVisible(rangeLinesFsid, labelsAndRangeLinesVisible);

            createAndAddRangeLines(fieldOfView, rangeLinesFsid);

            final var globeFieldOfView = new GlobeFieldOfView(mapObject, fsid, polygonFid, arrowFid, leftLabelFid, rightLabelFid, rangeLinesFsid);
            fovToGlobeFov.put(fieldOfView, globeFieldOfView);

            fieldOfView.addPropertyChangeListener(FOV_PROPERTIES, fovPropertyChangeListener);
        }, "create FOV");
    }

    /**
     * @param fieldOfView The FOV definition
     * @return The FOV polygon geometry
     */
    @Nullable
    private Geometry createPolygonGeometry(@Nonnull FieldOfView fieldOfView)
    {
        final var point = fieldOfView.getPoint();
        final var lat = point.getLatitude();
        final var lon = point.getLongitude();
        final var alt = point.getAltitude();
        if (!GeoPoint.isValid(lat, lon)) return null;

        final var lineString = new LineString(3);
        final var fov = fieldOfView.getFovDegrees();
        final var elevation = fieldOfView.getElevation();
        final var range = fieldOfView.getRange();
        final var numSlices = SLICES_PER_90 * (int) Math.ceil(fov / 90);
        final var azimuth = fieldOfView.getAzimuthTrueNorth();

        lineString.addPoint(lon, lat, alt);

        for (int i = 0; i <= numSlices; i++)
        {
            final var nextPoint = GeoCalculations.pointAtDistance(point, azimuth - (fov / 2.0f) + ((fov / numSlices) * i), range, elevation);
            lineString.addPoint(nextPoint.getLongitude(), nextPoint.getLatitude(), nextPoint.getAltitude());
        }

        lineString.addPoint(lon, lat, alt);

        return new Polygon(lineString);
    }

    /**
     * @param fieldOfView The FOV definition
     * @return The FOV polygon style
     */
    @Nonnull
    private Style createPolygonStyle(@Nonnull FieldOfView fieldOfView)
    {
        final List<Style> composite = new ArrayList<>();

        composite.add(new BasicFillStyle(fieldOfView.getFillColor().getRGB()));

        var strokeWeight = (float) (fieldOfView.getStrokeWeight() * STROKE_WEIGHT_SCALAR);
        if (strokeWeight > 0) composite.add(new BasicStrokeStyle(fieldOfView.getStrokeColor().getRGB(), strokeWeight));

        return new CompositeStyle(composite.toArray(new Style[0]));
    }

    /**
     * @param fieldOfView The FOV definition
     * @param left {@code true} for the left label, {@code false} for the right label
     * @return The label geometry that corresponds to the left or right edge of the FOV polygon
     */
    @Nullable
    private Geometry createLabelGeometry(@Nonnull FieldOfView fieldOfView, boolean left)
    {
        final var point = fieldOfView.getPoint();
        final var lat = point.getLatitude();
        final var lon = point.getLongitude();
        if (!GeoPoint.isValid(lat, lon)) return null;

        final var lineString = new LineString(3);
        final var fov = fieldOfView.getFovDegrees();
        final var azimuth = fieldOfView.getAzimuthTrueNorth();
        final var elevation = fieldOfView.getElevation();
        final var range = fieldOfView.getRange();

        // Initial point
        lineString.addPoint(lat, lon, point.getAltitude());

        // Left or right edge endpoint
        final IGeoPoint endpoint = GeoCalculations.pointAtDistance(point, azimuth - (fov / 2.0f) + (left ? 0 : fov), range / 2, elevation);
        lineString.addPoint(endpoint.getLongitude(), endpoint.getLatitude(), endpoint.getAltitude());

        return lineString;
    }

    /**
     * @param fieldOfView The FOV definition
     * @param left {@code true} for the left label, {@code false} for the right label
     * @return The {@link LabelPointStyle} for the left or right edge of the FOV polygon; may be {@code null} if labels should not be displayed
     */
    @Nullable
    private Style createLabelStyle(@Nonnull FieldOfView fieldOfView, boolean left)
    {
        if (!fieldOfView.isShowLabels()) return null;

        final var azimuth = fieldOfView.getAzimuth();
        final var fov = fieldOfView.getFovDegrees();
        final var useMagneticNorth = fieldOfView.getNorthReference() == NorthReference.MAGNETIC;
        final double angle = azimuth - (fov / 2.0f) + (left ? 0 : fov);

        String label;
        if (useMagneticNorth)
        {
            final double bearingMag = GeoCalculations.convertFromTrueToMagnetic(fieldOfView.getPoint(), angle);
            label = AngleUtilities.format(bearingMag, Angle.DEGREE) + "M";
        } else
        {
            label = AngleUtilities.format(angle, Angle.DEGREE) + "T";
        }

        return new LabelPointStyle(label, Color.white.getRGB(), Color.black.getRGB(),
                LabelPointStyle.ScrollMode.DEFAULT, DEFAULT_GLOBE_FONT.getName(), DEFAULT_GLOBE_FONT.getSize() * 1.25f,
                EnumSet.of(LabelPointStyle.Style.BOLD), 0, 0, 0, 0, 0, false,
                14d, 1);
    }

    /**
     * @param fieldOfView The FOV definition
     * @return The arrow geometry that points from the FOV vertex to its extent
     */
    @Nonnull
    private Geometry createArrowGeometry(@Nonnull FieldOfView fieldOfView)
    {
        final var lineString = new LineString(3);

        final var start = fieldOfView.getPoint();
        lineString.addPoint(start.getLongitude(), start.getLatitude(), start.getAltitude());

        final var end = fieldOfView.getExtentPoint();
        lineString.addPoint(end.getLongitude(), end.getLatitude(), end.getAltitude());

        return lineString;
    }

    /**
     * @param fieldOfView The FOV definition
     * @return The arrow style
     */
    @Nonnull
    private Style createArrowStyle(@Nonnull FieldOfView fieldOfView)
    {
        var strokeWeight = (float) (fieldOfView.getStrokeWeight() * STROKE_WEIGHT_SCALAR);
        // factor = 2, pattern = -1 => solid line
        final var strokeStyle = new ArrowStrokeStyle(20, 2, (short) -1,
                fieldOfView.getStrokeColor().getRGB(), strokeWeight, BasicStrokeStyle.EXTRUDE_NONE,
                ArrowStrokeStyle.ArrowHeadMode.PerVertex);

        return FeatureUtility.makeNoLabelStyle(strokeStyle);
    }

    /**
     * @param fieldOfView The FOV definition
     * @param rangeLinesFsid The range lines feature set id
     * @throws DataStoreException thrown on errors adding the range line geometries to the data store
     */
    private void createAndAddRangeLines(@Nonnull FieldOfView fieldOfView, long rangeLinesFsid) throws DataStoreException
    {
        final double rangeLineSpacingMeters = fieldOfView.getRangeLineSpacing();
        if (rangeLineSpacingMeters == 0 || fieldOfView.getRangeLineStrokeWeight() == 0.0) return;

        final var count = (int) Math.floor(fieldOfView.getRange() / rangeLineSpacingMeters);
        final var fovDegrees = fieldOfView.getFovDegrees();
        final var point = fieldOfView.getPoint();
        final var azimuth = fieldOfView.getAzimuthTrueNorth();
        final var elevation = fieldOfView.getElevation();

        for (int i = 1; i <= count; i++)
        {
            final var geometry = createRangeLineGeometry(fovDegrees, point, azimuth, elevation, rangeLineSpacingMeters * i);
            final var feature = new Feature(rangeLinesFsid, null, geometry, createRangeLineStyle(fieldOfView), null);
            dataStore.insertFeature(feature);
        }
    }

    /**
     * @param fovDegrees The FOV angular degrees
     * @param origin The origin point
     * @param azimuth The azimuth, in degrees from true north
     * @param elevation The elevation
     * @param distance The distance of the range line from the origin point
     * @return The range line geometry
     */
    @Nonnull
    private Geometry createRangeLineGeometry(double fovDegrees, @Nonnull IGeoPoint origin, double azimuth, double elevation, double distance)
    {
        final var lineString = new LineString(3);
        final int numSlices = SLICES_PER_90 * (int) Math.ceil(fovDegrees / 90);

        for (int i = 0; i <= numSlices; i++)
        {
            final var point = GeoCalculations.pointAtDistance(origin, azimuth - (fovDegrees / 2.0f) + ((fovDegrees / numSlices) * i), distance, elevation);
            lineString.addPoint(point.getLongitude(), point.getLatitude(), point.getAltitude());
        }

        return lineString;
    }

    /**
     * @param fieldOfView The FOV definition
     * @return The range line style
     */
    @Nonnull
    private Style createRangeLineStyle(@Nonnull FieldOfView fieldOfView)
    {
        return new BasicStrokeStyle(fieldOfView.getRangeLinesStrokeColor().getRGB(), (float) (fieldOfView.getRangeLineStrokeWeight() * STROKE_WEIGHT_SCALAR));
    }

    /**
     * Updates the FOV's polygon and arrow geometry, which requires full update of the range lines and labels.
     *
     * @param fieldOfView The FOV definition
     * @param globeFieldOfView The globe FOV info
     */
    private void updatePolygonGeometry(@Nonnull FieldOfView fieldOfView, @Nonnull GlobeFieldOfView globeFieldOfView)
    {
        lockAndExecute(() -> {
            dataStore.updateFeature(globeFieldOfView.polygonFid(), FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY,
                    null, createPolygonGeometry(fieldOfView), null, null, 0);

            dataStore.updateFeature(globeFieldOfView.arrowFid(), FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY,
                    null, createArrowGeometry(fieldOfView), null, null, 0);

            // Range lines and labels (geometry and style) need to be updated whenever the polygon is updated
            replaceRangeLines(fieldOfView, globeFieldOfView);
            updateLabelGeometry(fieldOfView, globeFieldOfView);
            updateLabelStyle(fieldOfView, globeFieldOfView);
        }, "update polygon geometry");
    }

    /**
     * Updates the FOV's polygon and arrow styles.
     *
     * @param fieldOfView The FOV definition
     * @param globeFieldOfView The globe FOV info
     */
    private void updatePolygonStyle(@Nonnull FieldOfView fieldOfView, @Nonnull GlobeFieldOfView globeFieldOfView)
    {
        lockAndExecute(() -> {
            dataStore.updateFeature(globeFieldOfView.polygonFid(), FeatureDataStore4.PROPERTY_FEATURE_STYLE,
                    null, null, createPolygonStyle(fieldOfView), null, 0);

            dataStore.updateFeature(globeFieldOfView.arrowFid(), FeatureDataStore4.PROPERTY_FEATURE_STYLE,
                    null, null, createArrowStyle(fieldOfView), null, 0);
        }, "update polygon style");
    }

    /**
     * Updates the FOV edge geometries that display the left and right labels.
     * 
     * @param fieldOfView The FOV definition
     * @param globeFieldOfView The globe FOV info
     */
    private void updateLabelGeometry(@Nonnull FieldOfView fieldOfView, @Nonnull GlobeFieldOfView globeFieldOfView)
    {
        lockAndExecute(() -> {
            dataStore.updateFeature(globeFieldOfView.leftLabelFid(), FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY,
                    null, createLabelGeometry(fieldOfView, true), null, null, 0);

            dataStore.updateFeature(globeFieldOfView.rightLabelFid(), FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY,
                    null, createLabelGeometry(fieldOfView, false), null, null, 0);
        }, "update label geometry");
    }

    /**
     * Updates the left and right label styles; required to update the labels' text.
     * 
     * @param fieldOfView The FOV definition
     * @param globeFieldOfView The globe FOV info
     */
    private void updateLabelStyle(@Nonnull FieldOfView fieldOfView, @Nonnull GlobeFieldOfView globeFieldOfView)
    {
        lockAndExecute(() -> {
            dataStore.updateFeature(globeFieldOfView.leftLabelFid(), FeatureDataStore4.PROPERTY_FEATURE_STYLE,
                    null, null, createLabelStyle(fieldOfView, true), null, 0);

            dataStore.updateFeature(globeFieldOfView.rightLabelFid(), FeatureDataStore4.PROPERTY_FEATURE_STYLE,
                    null, null, createLabelStyle(fieldOfView, false), null, 0);
        }, "update label style");
    }

    /**
     * Updates the visibility of the range lines and FOV edge labels. The visibility of both features are controlled by
     * the {@link FieldOfView#isShowLabels()} value.
     * 
     * @param fieldOfView The FOV definition      
     * @param globeFieldOfView The globe FOV info
     */
    private void updateLabelAndRangeLinesVisibility(@Nonnull FieldOfView fieldOfView, @Nonnull GlobeFieldOfView globeFieldOfView)
    {
        lockAndExecute(() -> {
            final var visible = globeFieldOfView.mapObject().isVisible() && fieldOfView.isVisible() && fieldOfView.isShowLabels();
            dataStore.setFeatureSetVisible(globeFieldOfView.rangeLinesFsid(), visible);
            dataStore.setFeatureVisible(globeFieldOfView.leftLabelFid(), visible);
            dataStore.setFeatureVisible(globeFieldOfView.rightLabelFid(), visible);
        }, "update label and range lines visibility");
    }

    /**
     * Removes the current range lines and adds new lines according to the {@link FieldOfView#getRangeLineSpacing()} 
     * value.
     * 
     * @param fieldOfView The FOV definition
     * @param globeFieldOfView The globe FOV info
     */
    private void replaceRangeLines(@Nonnull FieldOfView fieldOfView, @Nonnull GlobeFieldOfView globeFieldOfView)
    {
        lockAndExecute(() -> {
            final var rangeLinesFsid = globeFieldOfView.rangeLinesFsid();

            // Remove the old
            final var params = new FeatureDataStore4.FeatureQueryParameters();
            params.featureSetFilter = new FeatureDataStore4.FeatureSetQueryParameters();
            params.featureSetFilter.ids = Set.of(rangeLinesFsid);
            dataStore.deleteFeatures(params);

            // Add the new
            createAndAddRangeLines(fieldOfView, rangeLinesFsid);
        }, "replace range lines");
    }

    /**
     * Updates the visibility of all FOV {@link Feature features} by updating the visibility of the {@link FeatureSet feature sets}.
     *
     * @param fieldOfView The FOV definition 
     * @param globeFieldOfView The globe FOV info
     */
    private void updateVisibility(@Nonnull FieldOfView fieldOfView, @Nonnull GlobeFieldOfView globeFieldOfView)
    {
        lockAndExecute(() -> {
            final var visible = globeFieldOfView.mapObject().isVisible() && fieldOfView.isVisible();
            dataStore.setFeatureSetVisible(globeFieldOfView.fsid(), visible);

            final var showLabels = visible && fieldOfView.isShowLabels();
            dataStore.setFeatureSetVisible(globeFieldOfView.rangeLinesFsid(), showLabels);
            dataStore.setFeatureVisible(globeFieldOfView.leftLabelFid(), showLabels);
            dataStore.setFeatureVisible(globeFieldOfView.rightLabelFid(), showLabels);
        }, "update visibility");
    }

    /**
     * Removes the globe FOV for the specified item.
     * 
     * @param mapItem The map item whose globe FOV should be removed
     */
    private void removeFieldOfView(@Nonnull MapItem mapItem)
    {
        final var fieldOfView = ((MapObject) mapItem).getFieldOfView();
        if (fieldOfView == null) return;
        fieldOfView.removePropertyChangeListener(FOV_PROPERTIES, fovPropertyChangeListener);
        final var globeFieldOfView = fovToGlobeFov.remove(fieldOfView);

        if (globeFieldOfView != null)
        {
            lockAndExecute(() -> {
                dataStore.deleteFeatureSet(globeFieldOfView.fsid());
                dataStore.deleteFeatureSet(globeFieldOfView.rangeLinesFsid());
            }, "delete FOV");
        }
    }

    /**
     * Handles feature data store locking and exception handling for a runnable.
     *
     * @param runnable The runnable
     * @param actionName The name of the action to log if errors occur
     */
    private void lockAndExecute(@Nonnull ErrorThrowingRunnable<DataStoreException> runnable, @Nonnull String actionName)
    {
        try
        {
            dataStore.acquireModifyLock(false);
            runnable.run();
        } catch (DataStoreException | InterruptedException e)
        {
            logger.error("Failed to update FieldOfView datastore while attempting to {}", actionName, e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * @param mapObject The owner 
     * @param fsid The main {@link FeatureSet} id that holds the polygon, arrow and labels
     * @param polygonFid The polygon {@link Feature} id
     * @param arrowFid The extent arrow {@link Feature} id
     * @param leftLabelFid The left label {@link Feature} id
     * @param rightLabelFid The right label {@link Feature} id
     * @param rangeLinesFsid The {@link FeatureSet} id that hold the range {@link Feature features}
     */
    private record GlobeFieldOfView(IMapObject mapObject, long fsid, long polygonFid, long arrowFid,
            long leftLabelFid, long rightLabelFid, long rangeLinesFsid)
    {
    }

    /**
     * Listens for relevant {@link FieldOfView} {@link PropertyChangeEvent events}.
     */
    private class FieldOfViewPropertyChangeListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            final var fieldOfView = (FieldOfView) evt.getSource();
            final var globeFieldOfView = fovToGlobeFov.get(fieldOfView);
            if (globeFieldOfView == null) return;

            switch (evt.getPropertyName())
            {
                case FieldOfView.ALPHA, FieldOfView.FOV_NORTH_REFERENCE, ShapeProperties.LINE_COLOR,
                        ShapeProperties.LINE_WIDTH, ShapeProperties.FILL_COLOR ->
                    updatePolygonStyle(fieldOfView, globeFieldOfView);
                case FieldOfView.LABELS, FieldOfView.SHOW_FOV_LABELS ->
                {
                    updateLabelGeometry(fieldOfView, globeFieldOfView);
                    updateLabelStyle(fieldOfView, globeFieldOfView);
                    updateLabelAndRangeLinesVisibility(fieldOfView, globeFieldOfView);
                    replaceRangeLines(fieldOfView, globeFieldOfView);
                }
                case FieldOfView.RANGE_LINES_COLOR, FieldOfView.RANGE_LINES_WEIGHT, ShapeProperties.RANGE_DISTANCES ->
                    replaceRangeLines(fieldOfView, globeFieldOfView);
                case FieldOfView.AZIMUTH, FieldOfView.ELEVATION, FieldOfView.FOV_DEGREES, ShapeProperties.LOCATION, ShapeProperties.RANGE ->
                    updatePolygonGeometry(fieldOfView, globeFieldOfView);
                case ShapeProperties.VISIBILITY, FieldOfView.VISIBILITY -> updateVisibility(fieldOfView, globeFieldOfView);
            }
        }
    }

    /**
     * Listens for relevant {@link IMapObject} {@link PropertyChangeEvent events}.
     */
    private class MapObjectPropertyChangeListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            final var mapObject = (MapObject) evt.getSource();
            final var fieldOfView = evt.getNewValue() instanceof FieldOfView fov ? fov : null;
            final var globeFieldOfView = fieldOfView != null ? fovToGlobeFov.get(fieldOfView) : null;

            switch (evt.getPropertyName())
            {
                case IMapObject.FIELD_OF_VIEW:
                    if (fieldOfView == null)
                    {
                        removeFieldOfView(mapObject);
                    } else
                    {
                        createGlobeFieldOfView(mapObject, fieldOfView);
                    }
                    break;
                case ShapeProperties.VISIBILITY:
                    if (globeFieldOfView != null) updateVisibility(fieldOfView, globeFieldOfView);
                    break;
            }
        }
    }
}
