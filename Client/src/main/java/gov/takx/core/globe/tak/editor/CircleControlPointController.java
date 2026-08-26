package gov.takx.core.globe.tak.editor;

/**
 * Implementation of {@link ARubberBandControlPointController} for circular objects.
 *
 * @since 5.4
 */
public class CircleControlPointController extends ARubberBandControlPointController
{
    @Override
    String getInitialControlPointType()
    {
        return IControlPoint.CONTROL_POINT_EAST;
    }
}
