package gov.takx.api.globe;

import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nullable;

/**
 * IGlobeMapObjectEditor provides a generic interface for controlling a user interface based editor on globe map
 * objects. The standard usage pattern is to assign the editor an IGlobeMapObject to edit by calling
 * {@link #setGlobeMapObject(IGlobeMapObject, boolean)}, then call {@link #arm()} to activate the editor. A call to
 * {@link #disarm()} deactivates the editor, but does not change the IGlobeMapObject assigned to it.
 * <p/>
 * The editor's IGlobeMapObject may be set at any time, regardless of whether the editor is currently armed.
 * Implementations of IGlobeMapObjectEditor may restrict which types of IGlobeMapObject are accepted for editing. For
 * example, an implementation may only accept references to IGlobeShapeMapObject.
 */
public interface IGlobeMapObjectEditor
{
    /**
     * Returns true if the editor is ready for editing, and false otherwise.
     *
     * @return true if the editor is ready for editing; false otherwise.
     */
    boolean isArmed();

    /**
     * Sets the editor in a state where it's IGlobeMapObject ready for editing.
     */
    void arm();

    /**
     * Sets the editor in a state where it's IGlobeMapObject cannot be edited.
     */
    void disarm();

    /**
     * Sets the editor in a state where it's IGlobeMapObject cannot be edited.
     *
     * @param noUpdate Pass {@code true} to suppress updates to the editor source
     */
    void disarm(boolean noUpdate);

    /**
     * Returns true if the IGlobeMapObject assigned to this editor has the specified key.
     *
     * @param key the IMapObject key to test.
     * @return true if this editor's IGlobeMapObject is non-null and the specified IMapObject key corresponds to this
     * editor's IGlobeMapObject; false otherwise.
     */
    boolean isGlobeMapObject(String key);

    /**
     * Returns the IGlobeMapObject assigned this editor. May return {@code null}.
     *
     * @return the editor's IGlobeMapObject.
     */
    IGlobeMapObject getGlobeMapObject();

    /**
     * @return The Map Object associated with the current GlobeMapObject, may be {@code null} if the GMO is {@code null}.
     * @see #getGlobeMapObject()
     * @since 3.3
     */
    @Nullable
    default IMapObject getMapObject()
    {
        final IGlobeMapObject globeMapObject = getGlobeMapObject();
        return globeMapObject != null ? globeMapObject.getMapObject() : null;
    }

    /**
     * Sets the IGlobeMapObject assigned to this editor. May be {@code null}.
     *
     * @param mapObject  the editor's IGlobeMapObject. A {@code null} value is permitted.
     * @param isNewShape Pass {@code true} if this is a newly created map object
     * @since 3.3 (renamed from setMapObject)
     */
    void setGlobeMapObject(IGlobeMapObject mapObject, boolean isNewShape);
}
