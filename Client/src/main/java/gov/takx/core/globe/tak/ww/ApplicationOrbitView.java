package gov.takx.core.globe.tak.ww;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.opengl.GLMapView;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.tak.api.engine.map.IMapRendererEnums.CameraCollision;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.math.IMatrix;
import gov.takx.api.event.tak.WWViewInitialized;
import gov.takx.api.globe.IApplicationGlobe;
import org.greenrobot.eventbus.EventBus;

import java.awt.Rectangle;

/**
 * Implementation of {@link BasicOrbitView} for TAK globe.
 *
 * @since 3.4
 */
public class ApplicationOrbitView extends BasicOrbitView
{
    private final double[] takModelView = new double[16];
    private final double[] takProjection = new double[16];
    private final GeoPoint geoPoint = GeoPoint.createMutable();
    private final gov.tak.api.engine.map.coords.GeoPoint takGeoPoint = gov.tak.api.engine.map.coords.GeoPoint.createMutable();
    private long lastViewId = 0;
    private long lastDrawVersion = 0;
    private long viewId = 0;
    private final MapRenderer<?> renderer;
    private final EventBus eventBus;

    private boolean viewInitialized;

    public ApplicationOrbitView(MapRenderer<?> renderer, EventBus eventBus)
    {
        this.renderer = renderer;
        this.eventBus = eventBus;
    }

    @Override
    protected void loadConfigurationValues()
    {
        // Skip loading configuration values from WW preferences
        //super.loadConfigurationValues();
    }

    @Override
    protected void doApply(DrawContext dc)
    {
        IApplicationGlobe applicationGlobe = (IApplicationGlobe) dc.getValue(ApplicationSceneController.GLOBE_DELEGATE);
        if (applicationGlobe == null)
        {
            super.doApply(dc);
            return;
        }
        GLMapView glMapView = applicationGlobe.getGLMapView();
        GLMapView.State state = glMapView.currentPass;

        if (dc.getModel().getGlobe() instanceof TakEarthGlobe takEarthGlobe) takEarthGlobe.setGlMapView(glMapView);

        // Did the View state change in a way that we need to apply it to the TAK globe?
        if (lastViewId != viewId)
        {
            applyViewToTak();
            lastViewId = viewId;
        }

        GeoPoint inverse = state.scene.mapProjection.inverse(state.scene.camera.location, geoPoint);
        eyePosition = new Position(LatLon.fromDegrees(inverse.getLatitude(), inverse.getLongitude()), inverse.getAltitude());
        inverse = state.scene.mapProjection.inverse(state.scene.camera.target, geoPoint);
        center = new Position(LatLon.fromDegrees(inverse.getLatitude(), inverse.getLongitude()), inverse.getAltitude());
        dc.setViewportCenterPosition(center);
        Sector visibleSector = Sector.fromDegrees(state.southBound, state.northBound, state.crossesIDL ? -180 : state.westBound, state.crossesIDL ? 180 : state.eastBound);
        dc.setVisibleSector(visibleSector);

        state.scene.camera.modelView.get(takModelView, IMatrix.MatrixOrder.ROW_MAJOR);
        state.scene.camera.projection.get(takProjection, IMatrix.MatrixOrder.ROW_MAJOR);

        this.dc = dc;
        globe = this.dc.getGlobe();

        heading = Angle.fromDegrees(state.drawRotation).normalize();
        pitch = Angle.fromDegrees(90d + state.scene.camera.elevation);
        roll = Angle.fromDegrees(state.scene.camera.roll);
        zoom = com.atakmap.map.MapSceneModel.range(state.scene.gsd, state.scene.camera.fov, state.scene.height);

        // TAK field of view is vertical. WW uses horizontal.
        double aspectRatio = ((double) state.scene.width / state.scene.height);
        Angle vFov = Angle.fromDegrees(state.scene.camera.fov);
        double hFov = 2 * Math.atan(Math.tan(vFov.getRadians() / 2.0) * aspectRatio);
        fieldOfView = Angle.fromRadians(hFov);

        modelview = Matrix.fromArray(takModelView, 0, true);
        modelview = modelview.multiply(Matrix.fromRotationXYZ(Angle.POS90, Angle.POS90, Angle.ZERO));
        modelviewInv = modelview.getInverse();
        if (modelviewInv == null) modelviewInv = Matrix.IDENTITY;

        int[] viewportArray = new int[4];
        this.dc.getGL().glGetIntegerv(2978, viewportArray, 0);
        viewport = new Rectangle(viewportArray[0], viewportArray[1], viewportArray[2], viewportArray[3]);
        farClipDistance = state.scene.camera.farMeters;
        nearClipDistance = state.scene.camera.nearMeters;
        projection = Matrix.fromArray(takProjection, 0, true);
        frustum = Frustum.fromProjectionMatrix(projection);
        loadGLViewState(dc, modelview, projection);
        afterDoApply();

        if (!viewInitialized)
        {
            eventBus.post(new WWViewInitialized(this));
            viewInitialized = true;
        }

        if (lastDrawVersion != state.drawVersion)
        {
            firePropertyChange(AVKey.VIEW, null, this);
            lastDrawVersion = state.drawVersion;
        }
    }

    private void applyViewToTak()
    {
        takGeoPoint.set(center.getLatitude().degrees, center.getLongitude().degrees, 0.0);
        MapSceneModel mapSceneModel = renderer.getMapSceneModel(false, DisplayOrigin.UpperLeft);
        double gsd = com.atakmap.map.MapSceneModel.gsd(getZoom(), mapSceneModel.camera.fov, mapSceneModel.height);
        renderer.lookAt(takGeoPoint, gsd, getHeading().degrees, getPitch().degrees, CameraCollision.AdjustCamera, false);
    }

    /**
     * Overridden to make public.
     * @since 3.4
     */
    @Override
    public void updateModelViewStateID()
    {
        super.updateModelViewStateID();
    }

    @Override
    public void setCenterPosition(Position center)
    {
        super.setCenterPosition(center);
        viewId++;
    }

    @Override
    public void setHeading(Angle heading)
    {
        super.setHeading(heading);
        viewId++;
    }

    @Override
    public void setPitch(Angle pitch)
    {
        super.setPitch(pitch);
        viewId++;
    }

    @Override
    public void setZoom(double zoom)
    {
        super.setZoom(zoom);
        viewId++;
    }

    @Override
    public void setEyePosition(Position eyePosition)
    {
        super.setEyePosition(eyePosition);
        viewId++;
    }
}
