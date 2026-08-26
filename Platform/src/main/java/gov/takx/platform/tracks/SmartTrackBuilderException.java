package gov.takx.platform.tracks;

/**
 * Indicates an exception occurred within a SmartTrack builder class that prevented the build operation from completing.
 *
 * @see TrackStyleDefinitionBuilder
 * @see ConnectingLineDefinitionBuilder
 * @since 1.12
 */
public class SmartTrackBuilderException extends RuntimeException
{
    public SmartTrackBuilderException(String message)
    {
        super(message);
    }
}
