package gov.takx.api.globe.spiderfy;

/**
 * Constants defining the properties that can be used within {@code IconStack} property change events.
 *
 * @since 2.26
 */
public final class IconStackPropertyConstants
{
    public static final String EXPANSION_SHAPE = "globe.spiderfy.expansion.shape";
    public static final String CHILD_ADDED = "globe.spiderfy.child.added";
    public static final String CHILD_REMOVED = "globe.spiderfy.child.removed";
    /**
     * Property change type fired when an {@code IconStack}'s position has changed.
     */
    public static final String POSITION = "globe.spiderfy.position";
    /**
     * Property change type fired when an {@code IconStack} and all of its child {@code IconStack}'s positions are
     * updated. Additional {@link #POSITION} events will be fired for each child {@code IconStack} whose position
     * is updated.
     */
    public static final String ALL_POSITIONS = "globe.spiderfy.all.position";

    /**
     * Private constructor to avoid instantiation.
     */
    private IconStackPropertyConstants()
    {
    }
}
