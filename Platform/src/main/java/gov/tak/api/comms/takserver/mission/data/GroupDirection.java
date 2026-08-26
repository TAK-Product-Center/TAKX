package gov.tak.api.comms.takserver.mission.data;

/**
 * Group direction for modeling a group as being either read or write.
 *
 * @see Group
 * @since 5.0
 */
public enum GroupDirection
{
    /**
     * Alias for {@code WRITE}.
     */
    IN,
    /**
     * Alias for {@code READ}.
     */
    OUT,
    /**
     * Alias for {@code READ} and {@code WRITE}; combination of {@link GroupDirection#IN} and {@link GroupDirection#OUT}
     * @since 5.2
     */
    BOTH
}
