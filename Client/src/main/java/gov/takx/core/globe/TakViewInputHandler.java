package gov.takx.core.globe;

import com.atakmap.map.CameraController;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Vector3D;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.animation.AnimationController;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.ViewInputAttributes;
import gov.nasa.worldwind.awt.ViewInputAttributes.ActionAttributes;
import gov.nasa.worldwind.awt.ViewInputAttributes.ActionAttributes.ActionTrigger;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.IMapRendererEnums.InverseResult;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.math.PointD;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.core.globe.tak.ww.ApplicationOrbitView;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import static gov.nasa.worldwind.awt.ViewInputAttributes.DEVICE_KEYBOARD;
import static gov.nasa.worldwind.awt.ViewInputAttributes.DEVICE_MOUSE;
import static gov.nasa.worldwind.awt.ViewInputAttributes.DEVICE_MOUSE_WHEEL;
import static gov.nasa.worldwind.awt.ViewInputAttributes.VIEW_HORIZONTAL_TRANSLATE;
import static gov.nasa.worldwind.awt.ViewInputAttributes.VIEW_HORIZONTAL_TRANS_KEYS;
import static gov.nasa.worldwind.awt.ViewInputAttributes.VIEW_VERTICAL_TRANSLATE;
import static gov.nasa.worldwind.awt.ViewInputAttributes.VIEW_VERTICAL_TRANS_KEYS_CTRL;
import static gov.tak.api.engine.map.IMapRendererEnums.InverseMode.RayCast;

/**
 * Custom input handler for TAK globe.
 *
 * @since 3.4
 */
public class TakViewInputHandler extends GlobeViewInputHandler
{
    private static final int INPUT_SCALE = 3;

    /**
     * Relative density threshold between the current camera focus and the
     * desired pan-to location. Values numerically greater than this value may
     * use pan-to; values numerically less than use pan-by. This limits
     * velocity of movement of the foreground.
     */
    private static final double PAN_TO_RELATIVE_DENSITY_LIMIT = 0.75d;

    private final GlobeViewController viewController;
    private final MapRenderer<?> mapRenderer;

    private GeoPoint mouseDownLocation;
    private float panScrollSpeed = 1f;
    private boolean convertMouseButtonZoomToTilt;
    private boolean convertMouseTiltToRotate;
    private boolean convertKeyRotateToZoom;
    private boolean convertKeyZoomToCenter;

    TakViewInputHandler(GlobeViewController viewController, MapRenderer<?> mapRenderer)
    {
        this.viewController = viewController;
        this.mapRenderer = mapRenderer;

        // Scale up the input sensitivity
        scaleValues(DEVICE_KEYBOARD, VIEW_HORIZONTAL_TRANS_KEYS);
        scaleValues(DEVICE_KEYBOARD, VIEW_VERTICAL_TRANS_KEYS_CTRL);

        scaleValues(DEVICE_MOUSE, VIEW_HORIZONTAL_TRANSLATE);
        scaleValues(DEVICE_MOUSE, VIEW_VERTICAL_TRANSLATE);

        scaleValues(DEVICE_MOUSE_WHEEL, VIEW_VERTICAL_TRANSLATE);
    }

    /**
     * Scale the input handing values to make the adjustment to the view larger/faster.
     *
     * @param device Input device type to update
     * @param action Input action to update
     * @since 3.5
     */
    private void scaleValues(String device, String action)
    {
        final ActionAttributes attributes = getAttributes().getActionMap(device).getActionAttributes(action);
        final double[] values = attributes.getValues();
        attributes.setValues(values[0] * INPUT_SCALE, values[1] * INPUT_SCALE);
    }

    @Override
    protected void setMouseDownView(View mouseDownView)
    {
        super.setMouseDownView(mouseDownView);
        if (mouseDownView != null)
        {
            GeoPoint location = GeoPoint.createMutable();
            InverseResult result = mapRenderer.inverse(new PointD(mouseDownPoint.x, mouseDownPoint.y, 0), location, RayCast, 0, DisplayOrigin.UpperLeft);
            mouseDownLocation = result != InverseResult.None ? location : null;
        } else
        {
            mouseDownLocation = null;
        }
    }

    @Override
    protected void onMoveTo(Position focalPosition, ViewInputAttributes.DeviceAttributes ignored, ActionAttributes actionAttribs)
    {
        if (actionAttribs.getActionTrigger() == ActionTrigger.ON_DRAG)
        {
            if (panScrollSpeed == 1f && mouseDownLocation != null && useCameraPanTo(mapRenderer, mouseDownLocation, mousePoint))
            {
                // This offers pixel-perfect precision dragging even at high zoom levels
                viewController.panTo(mouseDownLocation, mousePoint.x, mousePoint.y);
            } else if (mousePoint != null && lastMousePoint != null)
            {
                // This offers the ability to scale back the pan speed (with a slight loss of precision when zoomed in)
                float deltaX = lastMousePoint.x - mousePoint.x;
                float deltaY = lastMousePoint.y - mousePoint.y;
                viewController.panBy(deltaX * panScrollSpeed, deltaY * panScrollSpeed);
            }
        } else
        {
            viewController.centerOn(focalPosition.getLatitude().degrees, focalPosition.getLongitude().degrees, focalPosition.getAltitude(), false);
        }
    }

    @Override
    protected void onMoveTo2D(Position focalPosition, ViewInputAttributes.DeviceAttributes deviceAttributes, ActionAttributes actionAttribs)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void setEyePoint(Vec4 eyePoint, ActionAttributes actionAttribs)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onResetHeading(ActionAttributes actionAttribs)
    {
        viewController.setHeading(0, true);
    }

    @Override
    protected void onResetRoll(ActionAttributes actionAttribs)
    {
        // No roll for TAK globe
    }

    @Override
    protected void onResetHeadingPitchRoll(ActionAttributes actionAttribs)
    {
        viewController.resetHeadingAndTilt();
    }

    @Override
    protected void setCenterPosition(BasicOrbitView view, AnimationController animControl, Position position, ActionAttributes actionAttribs)
    {
        onMoveTo(position, null, actionAttribs);
        view.firePropertyChange(AVKey.VIEW, null, view);
        getView().updateModelViewStateID();
    }

    @Override
    protected void changeHeading(BasicOrbitView view, AnimationController animControl, Angle change, ActionAttributes actionAttribs)
    {
        boolean animate = isAnimating(actionAttribs);
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, DisplayOrigin.UpperLeft);
        viewController.setHeading(mapSceneModel.camera.azimuth + change.degrees, animate);
        view.firePropertyChange(AVKey.VIEW, null, view);
        getView().updateModelViewStateID();
    }

    @Override
    protected void changePitch(BasicOrbitView view, AnimationController animControl, Angle change, ActionAttributes actionAttribs)
    {
        boolean animate = isAnimating(actionAttribs);
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, DisplayOrigin.UpperLeft);
        viewController.setTilt(90d + mapSceneModel.camera.elevation + change.degrees, animate);
        view.firePropertyChange(AVKey.VIEW, null, view);
        getView().updateModelViewStateID();
    }

    /**
     * TAKX implementation applies our globe navigation style changes before delegating to super.
     * {@inheritDoc}
     *
     * @param headingChange the heading change
     * @param pitchChange the pitch change
     * @param actionAttribs the action attributes
     * @since 5.6
     */
    @Override
    protected void onRotateView(Angle headingChange, Angle pitchChange, ViewInputAttributes.ActionAttributes actionAttribs)
    {
        if (actionAttribs.getKeyActions() != null && convertKeyRotateToZoom)
        {
            changeZoom(getView(), uiAnimControl, pitchChange.radians * 10, actionAttribs);
        } else
        {
            if (actionAttribs.getMouseActions() != null && convertMouseTiltToRotate && getWorldWindow() instanceof Component ww)
            {
                headingChange = getMousePoint().x > ww.getWidth() / 2 ? headingChange.subtract(pitchChange) : headingChange.add(pitchChange);
                pitchChange = Angle.fromDegrees(0);
            }
            super.onRotateView(headingChange, pitchChange, actionAttribs);
        }
    }

    /**
     * Changes the zoom while maintaining cursor position, if cursor position is available.
     * <br>
     * NOTE: Technically this could be made more accurate by using Ellipsoid math, but the inaccuracies are only apparent
     * at the far longitudinal edges of the screen when zoomed out wide enough to see the globe edges.
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected void changeZoom(BasicOrbitView view, AnimationController animControl, double change, ActionAttributes actionAttribs)
    {
        boolean animate = isAnimating(actionAttribs);
        if (keyEventState.isKeyDown(KeyEvent.VK_CONTROL)) change = change * 3;
        double scale = Math.pow(2.0, change);
        Position cursorPosition = getWorldWindow().getCurrentPosition();
        boolean mouseAction = actionAttribs.getMouseActions() != null;
        if (mouseAction && convertMouseButtonZoomToTilt && Arrays.stream(ViewInputAttributes.verticalTransMouseEvents).anyMatch(e -> e == actionAttribs.getMouseActions().get(0)))
        {
            change = change / getScaleValueRotate(actionAttribs); // Undo the scale done by WW
            ActionAttributes aa = attributes.getActionMap("gov.nasa.worldwind.DeviceMouse").getActionAttributes("gov.nasa.worldwind.ViewRotate");
            Angle pitchChange = Angle.fromDegrees(-(change * getScaleValueRotate(aa))); // Apply the rotation scale that WW normally does
            changePitch(view, animControl, pitchChange, aa);
        } else if (cursorPosition != null && (mouseAction || !convertKeyZoomToCenter))
        {
            Position centerPosition = view.getCenterPosition();
            Angle cursorDistance = Position.greatCircleDistance(centerPosition, cursorPosition);
            Angle cursorBearing = Position.greatCircleAzimuth(centerPosition, cursorPosition);
            Angle distanceDelta = cursorDistance.subtract(cursorDistance.multiply(scale));
            LatLon newCenter = Position.greatCircleEndPosition(centerPosition, cursorBearing, distanceDelta);
            viewController.zoomTo(view.getZoom() * scale, newCenter.latitude.degrees, newCenter.longitude.degrees, animate);
        } else
        {
            viewController.setEyeAltitude(view.getZoom() * scale, animate);
        }
        view.firePropertyChange(AVKey.VIEW, null, view);
        getView().updateModelViewStateID();
    }

    @Override
    public void addPanToAnimator(Position beginCenterPos, Position endCenterPos, Angle beginHeading, Angle endHeading, Angle beginPitch, Angle endPitch, double beginZoom, double endZoom, long timeToMove, boolean endCenterOnSurface)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPanToAnimator(Position beginCenterPos, Position endCenterPos, Angle beginHeading, Angle endHeading, Angle beginPitch, Angle endPitch, double beginZoom, double endZoom, boolean endCenterOnSurface)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addEyePositionAnimator(long timeToIterate, Position beginPosition, Position endPosition)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addHeadingAnimator(Angle begin, Angle end)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPitchAnimator(Angle begin, Angle end)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRollAnimator(Angle begin, Angle end)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addHeadingPitchRollAnimator(Angle beginHeading, Angle endHeading, Angle beginPitch, Angle endPitch, Angle beginRoll, Angle endRoll)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addZoomAnimator(double zoomStart, double zoomEnd)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFlyToZoomAnimator(Angle heading, Angle pitch, double zoom)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addCenterAnimator(Position begin, Position end, long lengthMillis, boolean smoothed)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void goTo(Position lookAtPos, double distance)
    {
        viewController.goTo(lookAtPos.getLatitude().degrees, lookAtPos.getLongitude().degrees, distance);
        final var view = getView();
        view.firePropertyChange(AVKey.VIEW, null, view);
        view.updateModelViewStateID();
    }

    private boolean isAnimating(ActionAttributes actionAttribs)
    {
        boolean isAnimating = actionAttribs.getSmoothingValue() > 0.0;
        if (!actionAttribs.isEnableSmoothing() || !isEnableSmoothing())
        {
            isAnimating = false;
        }
        return isAnimating;
    }

    // The default implementation of these two methods is ineffective, so lets force a redraw ourselves
    @Override
    protected void handleKeyPressed(KeyEvent e)
    {
        super.handleKeyPressed(e);
        // Determine whether the current key state would have generated a view change event.
        // If so, issue a repaint event to give the per-frame input a chance to run.
        if (handlePerFrameKeyState(keyEventState, QUERY_EVENTS)) getWorldWindow().redraw();
    }

    @Override
    protected void handleMousePressed(MouseEvent e)
    {
        super.handleMousePressed(e);
        // Determine whether the current key state would have generated a view change event.
        // If so, issue a repaint event to give the per-frame input a chance to run.
        if (handlePerFrameMouseState(keyEventState, QUERY_EVENTS)) getWorldWindow().redraw();
    }

    /**
     * Overridden for covariant return.
     * @since 3.4
     */
    @Override
    protected ApplicationOrbitView getView()
    {
        return (ApplicationOrbitView) super.getView();
    }

    /**
     * Get the pan scroll speed. This value acts as a multiplier to the standard normal pan speed.
     * @return the pan scroll speed
     * @since 5.1
     */
    public float getPanScrollSpeed()
    {
        return panScrollSpeed;
    }

    /**
     * Set the pan scroll speed. This value acts as a multiplier to the standard normal pan speed.
     * @param panScrollSpeed the pan scroll speed
     * @since 5.1
     */
    public void setPanScrollSpeed(float panScrollSpeed)
    {
        this.panScrollSpeed = panScrollSpeed;
    }

    /**
     * Tells the input handler to convert the mouse button zoom operation to a tilt operation. Note this does not affect
     * mouse wheel zoom, only the mouse button zoom.
     *
     * @param convertMouseButtonZoomToTilt {@code true} to convert the zoom values, otherwise {@code false}
     * @since 5.6
     */
    public void setConvertMouseButtonZoomToTilt(boolean convertMouseButtonZoomToTilt)
    {
        this.convertMouseButtonZoomToTilt = convertMouseButtonZoomToTilt;
    }

    /**
     * Tells the input handler to convert tilt-rotate motions generated by mouse action to rotate only values.
     *
     * @param convertMouseTiltToRotate {@code true} to convert the tilt values, otherwise {@code false}
     * @since 5.6
     */
    public void setConvertMouseTiltToRotate(boolean convertMouseTiltToRotate)
    {
        this.convertMouseTiltToRotate = convertMouseTiltToRotate;
    }

    /**
     * Tells the input handler to convert keyboard rotate operations to zoom operations.
     *
     * @param convertKeyRotateToZoom {@code true} to convert the rotate operation, otherwise {@code false}
     * @since 5.6
     */
    public void setConvertKeyRotateToZoom(boolean convertKeyRotateToZoom)
    {
        this.convertKeyRotateToZoom = convertKeyRotateToZoom;
    }

    /**
     * Tells the input handler to make keyboard zoom operations use the view center point instead of the cursor point.
     *
     * @param convertKeyZoomToCenter {@code true} to user center point, or {@code false} to use cursor point
     * @since 5.6
     */
    public void setConvertKeyZoomToCenter(boolean convertKeyZoomToCenter)
    {
        this.convertKeyZoomToCenter = convertKeyZoomToCenter;
    }

    /**
     * Determine what side of the plane the camera is on. If the camera is below the plane, we should
     * do an ortho style drag using panBy(), otherwise we should do a real drag using panTo().
     */
    private static boolean useCameraPanTo(MapRenderer<?> mapRenderer, GeoPoint mouseDownLocation, Point mousePoint)
    {
        MapSceneModel sm = mapRenderer.getMapSceneModel(false, DisplayOrigin.UpperLeft);
        // create plane at press location
        PointD startProj = new PointD();
        PointD startProjUp = new PointD();
        sm.mapProjection.forward(mouseDownLocation, startProj);
        sm.mapProjection.forward(new GeoPoint(mouseDownLocation.getLatitude(), mouseDownLocation.getLongitude(), mouseDownLocation.getAltitude() + 100d), startProjUp);

        // compute the normal at the start point
        final Vector3D startNormal = new Vector3D(startProjUp.x - startProj.x, startProjUp.y - startProj.y, startProjUp.z - startProj.z);
        startNormal.X *= sm.displayModel.projectionXToNominalMeters;
        startNormal.Y *= sm.displayModel.projectionYToNominalMeters;
        startNormal.Z *= sm.displayModel.projectionZToNominalMeters;
        final double startNormalLen = MathUtils.distance(startNormal.X, startNormal.Y, startNormal.Z, 0d, 0d, 0d);
        startNormal.X /= startNormalLen;
        startNormal.Y /= startNormalLen;
        startNormal.Z /= startNormalLen;

        final double d = -startNormal.dot(startProj.x, startProj.y, startProj.z);
        final double above = startNormal.dot(startProjUp.x, startProjUp.y, startProjUp.z) + d;
        final double camera = startNormal.dot(sm.camera.location.x, sm.camera.location.y, sm.camera.location.z) + d;

        // determine what side of the plane the camera is on; if the camera is below the plane,
        // we'll do an ortho style drag (false), otherwise we'll do a real drag (true).
        return sm.camera.perspective && (above * camera > 0d) &&
                (CameraController.Util.computeRelativeDensityRatio(MarshalManager.marshal(sm, MapSceneModel.class, com.atakmap.map.MapSceneModel.class), mousePoint.x, mousePoint.y)
                        > (PAN_TO_RELATIVE_DENSITY_LIMIT * Math.abs(Math.cos(Math.toRadians(sm.camera.elevation)))));
    }
}
