package gov.takx.core.globe;

import gov.nasa.worldwind.animation.AnimationSupport;
import gov.nasa.worldwind.awt.AbstractViewInputHandler;
import gov.nasa.worldwind.awt.ViewInputActionHandler;
import gov.nasa.worldwind.awt.ViewInputAttributes;
import gov.nasa.worldwind.awt.ViewInputAttributes.ActionAttributes;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.view.ViewUtil;
import gov.nasa.worldwind.view.orbit.OrbitViewInputHandler;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.inject.Inject;

import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Custom Input Handler for a globe orbit view.
 * <p>
 * This class overrides a few methods in WW OrbitViewInputHandler in order to fix bugs in map translation (panning)
 * which would occur when the mouse is dragged over non-terrain globe objects or non-WW Swing dialogs.
 * <p>
 * It also overrides the input handler for a left mouse click in order to add on our "nav lock" and click count
 * handling for mouse navigation (re-center view).
 *
 * @since 3.0 - moved out of RaptorOrbitView (which has been deleted)
 */
@PseudoBean
class GlobeViewInputHandler extends OrbitViewInputHandler
{
    // min/max times for pan animation
    private static final long MIN_PAN_LENGTH_MS = 1_000;
    private static final long MAX_PAN_LENGTH_MS = 2_500;

    @Inject
    @ProgramPreference(PreferenceEntry.GLOBE_CENTER_GLOBE_ON_CLICK_COUNT)
    private int centerOnClickCount; // How many clicks to invoke "center on"

    private boolean centerOnClickNavLock = false;

    protected GlobeViewInputHandler()
    {
        // Install our "Move to" mouse handler
        getAttributes().getActionMap(ViewInputAttributes.DEVICE_MOUSE)
                .getActionAttributes(ViewInputAttributes.VIEW_MOVE_TO)
                .setMouseActionListener(new MoveToMouseActionListener());
    }

    /**
     * Set the nav lock.
     *
     * @param centerOnClickNavLock Pass {@code true} to lock mouse click navigation
     */
    public void setCenterOnClickNavLock(boolean centerOnClickNavLock)
    {
        this.centerOnClickNavLock = centerOnClickNavLock;
    }

    /**
     * Overridden to speed up the animation.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void addPanToAnimator(Position beginCenterPos, Position endCenterPos,
                                 Angle beginHeading, Angle endHeading,
                                 Angle beginPitch, Angle endPitch,
                                 double beginZoom, double endZoom, boolean endCenterOnSurface)
    {
        final long timeToMove = AnimationSupport.getScaledTimeMillisecs(beginCenterPos, endCenterPos,
                MIN_PAN_LENGTH_MS, MAX_PAN_LENGTH_MS);

        addPanToAnimator(beginCenterPos, endCenterPos, beginHeading, endHeading, beginPitch, endPitch,
                beginZoom, endZoom, timeToMove, endCenterOnSurface);
    }

    @Override
    protected void onHorizontalTranslateRel(double forwardInput, double sideInput,
                                            double totalForwardInput, double totalSideInput,
                                            ViewInputAttributes.DeviceAttributes deviceAttributes,
                                            ActionAttributes actionAttributes)
    {
        stopGoToAnimators();
        stopUserInputAnimators(VIEW_ANIM_HEADING, VIEW_ANIM_PITCH, VIEW_ANIM_ZOOM);

        if (actionAttributes.getMouseActions() != null)
        {
            Point point = constrainToSourceBounds(getMousePoint(), getWorldWindow());
            Point lastPoint = constrainToSourceBounds(getLastMousePoint(), getWorldWindow());
            if (getSelectedPosition() == null)
            {
                // Compute the current selected position if none exists. This happens if the user starts dragging when
                // the cursor is off the globe, then drags the cursor onto the globe.

                // call the custom compute position method
                setSelectedPosition(computePositionOfTopPickedObject());
            }

            Vec4 vec = computeSelectedPointAt(point);
            Vec4 lastVec = computeSelectedPointAt(lastPoint);

            // Cursor is on the globe, pan between the two positions.
            if (vec != null && lastVec != null)
            {
                // Compute the change in view location given two screen points and corresponding world vectors.
                LatLon latlon = getChangeInLocation(lastPoint, point, lastVec, vec);
                onHorizontalTranslateAbs(latlon.getLatitude(), latlon.getLongitude(), actionAttributes);
                return;
            }

            // This section of code should not be reachable under normal circumstances, but
            Point movement = ViewUtil.subtract(point, lastPoint);
            forwardInput = movement.y;
            sideInput = -movement.x;
        }

        // Cursor is off the globe, we potentially want to simulate globe dragging.
        // or this is a keyboard event.
        double scaleValueHorizTransRel = getScaleValueHorizTransRel(deviceAttributes, actionAttributes);
        Angle forwardChange = Angle.fromDegrees(forwardInput * scaleValueHorizTransRel);
        Angle sideChange = Angle.fromDegrees(sideInput * scaleValueHorizTransRel);
        onHorizontalTranslateRel(forwardChange, sideChange, actionAttributes);
    }

    /**
     * Custom compute selected position method which returns a position from the top picked object regardless of
     * whether it is terrain.  This solves the bug where drags over non terrain objects (i.e. shapes, tracks)
     * were causing panning issues.
     *
     * @return the position of the top picked object
     */
    protected Position computePositionOfTopPickedObject()
    {
        final PickedObjectList pickedObjects = wwd.getObjectsAtCurrentPosition();
        final Object topObject = pickedObjects != null ? pickedObjects.getTopObject() : null;

        // If it's a MapPoint, use the reference position rather than the real one, to account for snapping to surface.
        return topObject instanceof MapPoint mapPoint
                ? mapPoint.getReferencePosition()
                : pickedObjects == null || pickedObjects.isEmpty() ? null : pickedObjects.getTopPickedObject().getPosition();
    }

    /**
     * Action listener to handle our custom logic for mouse click globe navigation.
     *
     * @since 1.49
     */
    protected class MoveToMouseActionListener extends ViewInputActionHandler
    {
        @Override
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler, MouseEvent mouseEvent, ActionAttributes viewAction)
        {
            // We only operate upon button 1 clicks on terrain

            if (mouseEvent.getButton() != MouseEvent.BUTTON1) return false; // Not button 1

            final Position pos = ((GlobeViewInputHandler) inputHandler).computeSelectedPosition();
            if (pos == null) return false; // Not on terrain

            // Nav lock must be disabled and the click count must match the configuration

            if (!centerOnClickNavLock && mouseEvent.getClickCount() == centerOnClickCount)
            {
                onMoveTo(pos, getAttributes().getDeviceAttributes(ViewInputAttributes.DEVICE_MOUSE), viewAction);
            }
            return true;
        }
    }
}
