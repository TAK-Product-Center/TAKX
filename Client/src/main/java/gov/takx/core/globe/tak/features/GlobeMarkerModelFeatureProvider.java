package gov.takx.core.globe.tak.features;

import android.graphics.PointF;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.BasicPointStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle.ScrollMode;
import com.atakmap.map.layer.feature.style.LevelOfDetailStyle;
import com.atakmap.map.layer.feature.style.MeshPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.model.ModelInfo;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.api.vehicle.VehicleModelCache;
import gov.tak.api.vehicle.VehicleModelInfo;
import gov.tak.platform.commons.opengl.Matrix;
import gov.tak.platform.graphics.Color;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link IFeatureProvider} for {@link IGlobeMarkerMapObject IGlobeMarkerMapObjects} that represent 3D models.
 *
 * @since 5.8
 */
@PseudoBean
public final class GlobeMarkerModelFeatureProvider implements IFeatureProvider<IGlobeMarkerMapObject, GlobeMarkerModelFeatureProvider.GlobeMarkerModelState>
{
    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(GlobeMarkerFeatureProvider.SPI.getPriority() + 1, IGlobeMarkerMapObject.class, GlobeMarkerModelFeatureProvider::new)
    {
        @Nullable
        @Override
        protected IFeatureProvider<?, ?> createFeatureProvider(@Nonnull Object object)
        {
            final IGlobeMarkerMapObject globeMarkerMapObject = (IGlobeMarkerMapObject) object;
            if (globeMarkerMapObject.getMapObject() == null ||
                    !globeMarkerMapObject.getMapObject().getAttributeSet().containsAttribute(StandardAttributeNames.MODEL_NAME))
                return null;

            return new GlobeMarkerModelFeatureProvider();
        }
    };

    @Inject
    private VehicleModelCache vehicleModelCache;

    @Nonnull
    @Override
    public GlobeMarkerModelState createState(IGlobeMarkerMapObject globeMarkerMapObject)
    {
        IMapObject mapObject = globeMarkerMapObject.getMapObject();

        boolean isClampToGround = isClampToGround(globeMarkerMapObject);
        double scale = mapObject.getIconScale();
        float iconOffsetX = 0;
        float iconOffsetY = 0;

        boolean hasHeading = mapObject.hasHeading();
        double heading = hasHeading ? mapObject.getHeading() : 0;
        boolean visible = globeMarkerMapObject.isViewable();

        AttributeSet attributeSet = mapObject.getAttributeSet();
        String modelName = attributeSet.getStringAttribute(StandardAttributeNames.MODEL_NAME);
        String modelGroup = attributeSet.getStringAttribute(StandardAttributeNames.MODEL_GROUP, "");
        int color = attributeSet.getIntAttribute(StandardAttributeNames.MODEL_COLOR, 0xFFFFFFFF);
        boolean showOutline = attributeSet.getBooleanAttribute(StandardAttributeNames.MODEL_OUTLINE_ENABLED, false);

        return new GlobeMarkerModelState(globeMarkerMapObject.getLocation(), modelName, modelGroup, mapObject.getName(), showOutline, color, isClampToGround, visible,
                scale, iconOffsetX, iconOffsetY, hasHeading, heading);
    }

    @Override
    public int getFeatureCount(@Nonnull GlobeMarkerModelState state)
    {
        return state.showOutline ? 2 : 1;
    }

    @Override
    public boolean isVisible(@Nonnull GlobeMarkerModelState state)
    {
        return state.isVisible;
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull GlobeMarkerModelState state)
    {
        final ILocation location = state.location;
        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();
        final double hae = state.isClampToGround
                ? -1000 // Arbitrary value to ensure the points snap to the surface regardless of the terrain used/not used
                : location.getElevation() + ElevationManager.getGeoidHeight(latitude, longitude);
        return state.showOutline ? new Geometry[]{new Point(longitude, latitude, hae), getOutlineGeometry(state)} : new Geometry[]{new Point(longitude, latitude, hae)};
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull GlobeMarkerModelState state)
    {
        List<Style> styles = new ArrayList<>();
        float[] transformF = new float[16];
        Matrix.setIdentityM(transformF, 0);
        Matrix.scaleM(transformF, 0, 2, 2, 2); // see note on `MeshPointStyle`
        Matrix.scaleM(transformF, 0, (float) state.scale, (float) state.scale, (float) state.scale);
        Matrix.rotateM(transformF, 0, 270, 1, 0, 0);
        if (!state.showOutline)
        {
            VehicleModelInfo vehicleModelInfo = vehicleModelCache.get(state.modelGroup, state.modelName);
            if (vehicleModelInfo != null)
            {
                ModelInfo info = vehicleModelInfo.getInfo();
                if (info != null) styles.add(new MeshPointStyle(info.uri, state.color, transformF));
            }
        }
        styles.add(new LevelOfDetailStyle(new LabelPointStyle(state.name, Color.WHITE, Color.BLACK, ScrollMode.DEFAULT, 14, 0, 0, 0, false), 16, 21));
        styles.add(new BasicPointStyle(-1, 24));

        var compositePointStyle = new CompositeStyle(styles.toArray(new Style[0]));
        return state.showOutline ? new Style[]{compositePointStyle, new BasicStrokeStyle(state.color, 3)} : new Style[]{compositePointStyle};
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull GlobeMarkerModelState state)
    {
        AltitudeMode modelAltitudeMode = state.isClampToGround ? AltitudeMode.ClampToGround : AltitudeMode.Absolute;
        return state.showOutline ? new AltitudeMode[]{modelAltitudeMode, AltitudeMode.ClampToGround} : new AltitudeMode[]{modelAltitudeMode};
    }

    private boolean isClampToGround(IGlobeMarkerMapObject globeMarkerMapObject)
    {
        IMapObject mapObject = globeMarkerMapObject.getMapObject();
        boolean snapIconToSurface = mapObject.supportsSnapIconToSurface() && mapObject.isSnapIconToSurface();
        return globeMarkerMapObject.isRenderingOnSurface() || snapIconToSurface;
    }

    /**
     * Get an outline {@link LineString} geometry for the vehicle model.
     * @param state the state object
     * @return the linestring geometry
     */
    @Nonnull
    private LineString getOutlineGeometry(@Nonnull GlobeMarkerModelState state)
    {
        double heading = state.heading;
        ILocation center = state.location;

        List<PointF> outlinePoints = getOutline(state);
        LineString ls = new LineString(3);
        // Calculate geodetic outline
        if (outlinePoints != null)
        {
            for (PointF outlinePoint : outlinePoints)
            {
                double x = outlinePoint.x;
                double y = outlinePoint.y;
                double angle = Math.toDegrees(Math.atan2(x, y)); // Purposefully swapping x,y params to atan2 to account for mesh orientation
                double dist = Math.hypot(x, y);
                angle += heading;
                IGeoPoint gp = GeoCalculations.pointAtDistance(center, angle, dist);
                ls.addPoint(gp.getLongitude(), gp.getLatitude(), Double.isNaN(gp.getAltitude()) ? 0 : gp.getAltitude());
            }
        }
        return ls;
    }

    /**
     * Synchronously obtain the vehicle model outline points.
     * @param state the state object
     * @return a list of vehicle model outline points, or {@code null} if not available
     */
    @Nullable
    private List<PointF> getOutline(@Nonnull GlobeMarkerModelState state)
    {
        VehicleModelInfo info = vehicleModelCache.get(state.modelGroup, state.modelName);
        if (info == null) return null;

        AtomicBoolean outlineGenerated = new AtomicBoolean();
        info.setVehicleOutlineLoadedCallback(points -> {
            synchronized (GlobeMarkerModelFeatureProvider.this)
            {
                outlineGenerated.set(true);
                GlobeMarkerModelFeatureProvider.this.notify();
            }
        });

        List<PointF> points = info.getOutline();
        if (points != null) return points;

        // Wait for points generation to finish if we need
        synchronized (this)
        {
            if (!outlineGenerated.get())
            {
                try
                {
                    this.wait();
                } catch (Exception ignored)
                {
                }
            }
        }

        return info.getOutline();
    }

    /**
     * State object for {@link GlobeMarkerModelFeatureProvider}.
     */
    public record GlobeMarkerModelState(ILocation location, String modelName, String modelGroup, String name, boolean showOutline, int color, boolean isClampToGround, boolean isVisible, double scale,
            float offsetX, float offsetY, boolean hasHeading, double heading)
    {
    }
}
