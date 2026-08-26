package gov.takx.core.globe.tak.editor;

/**
 * Implementation of {@link ARubberBandControlPointController} for rectangular objects.
 *
 * @since 5.4
 */
public class RectangleControlPointController extends ARubberBandControlPointController
{
    @Override
    String getInitialControlPointType()
    {
        return IControlPoint.CONTROL_POINT_NORTHEAST;
    }
}
