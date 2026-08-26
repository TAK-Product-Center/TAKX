package gov.takx.api.mapobject;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.tracks.ISmartTrack;
import gov.takx.commons.constants.State;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 */
@NoPluginImplementations
public interface IIconMapObject extends IMarkerMapObject, ISmartTrack
{
    /**
     * Protocol prefix used to indicate that the imagePath is representative of an actual file's icon, as
     * extracted from the Win32 ShellAPI.
     */
    String PROTOCOL_SCHEME_SHELL = "shell";

    /**
     * Protocol prefix used to indicate that the imagePath is located within the VFS.
     */
    String PROTOCOL_SCHEME_VFS = "vfs";

    /**
     * Protocol prefix used to indicate that the imagePath is located within the actual file system.
     */
    String PROTOCOL_SCHEME_FILE = "file";

    /**
     * Protocol prefix used to indicate that the imagePath is located within a resource bundle specified by the jar.
     */
    String PROTOCOL_SCHEME_JAR = "jar";

    String ICON_TYPE = "Icons";

    String CONTAINED = "CONTAINED";
    String CONTAINED_ADDED = "CONTAINED_ADDED";
    String CONTAINED_REMOVED = "CONTAINED_REMOVED";

    boolean isContained();

    /**
     * Add the given map object to this map object.  The given map object will be "contained" in this
     * map object after this call.  If the given map object is already contained by this map object, then
     * the operation is ignored.
     *
     * @param imo Map object to add to contained list
     */
    void addMapObjectToContainer(@Nonnull IIconMapObject imo);

    /**
     * Remove the given map object from this map objects contained list.  If the map object is not currently
     * contained, then the operation is ignored.
     *
     * @param imo The map object to remove from our containment
     */
    void removeContainedMapObject(@Nonnull IIconMapObject imo);

    List<IIconMapObject> getContainedMapObjects();

    IIconMapObject getParentContainer();

    @Internal
    void setParentContainer(IIconMapObject parent);

    State calculateContainedHighestState();

    /**
     * Display the device log.
     *
     * @since 2.37, moved here from IClientMapObject
     */
    void showDeviceLog();
}
