package gov.takx.api.structures;

/**
 * Surface structure service event type.
 *
 * @see SurfaceStructureServiceEvent
 * @since 1.42
 */
public enum SurfaceStructureServiceEventType
{
    /**
     * Surface structures have been added.
     * <p>
     * For this event type, the source will be the surface structure service, and the new value will be a collection
     * of {@link ISurfaceStructure}.
     */
    STRUCTURES_ADDED,

    /**
     * Surface structures have been removed.
     * <p>
     * For this event type, the source will be the surface structure service, and the new value will be a collection
     * of {@link ISurfaceStructure}.
     */
    STRUCTURES_REMOVED,

    /**
     * A change to a selected state of a surface structure.
     * <p>
     * For this event type, the source will be the {@link ISurfaceStructure} whose
     * selection state changed, and the new value will be the current selection state.
     */
    SELECTED_CHANGE,

    /**
     * A change to a property value on a surface structure backed by a Shape MapObject.
     * <p>
     * For this event type, the source will be the Shape MapObject whose property changed.
     */
    PROPERTY_CHANGE,

    /**
     * A change to a metadata value associated with a structure (from both shape file objects and Shape MapObjects).
     * <p>
     * For this event type, the source will be the {@link IIntersectableShape} or {@link ISurfaceStructure} whose
     * metadata changed.
     */
    METADATA_CHANGE
}
