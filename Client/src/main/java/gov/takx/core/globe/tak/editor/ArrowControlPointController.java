package gov.takx.core.globe.tak.editor;

/**
 * Implementation of {@link ARubberBandControlPointController} for two-point arrow objects.
 *
 * @since 5.4
 */
public class ArrowControlPointController extends ARubberBandControlPointController
{
    @Override
    String getInitialControlPointType()
    {
        return "1";
    }
}
