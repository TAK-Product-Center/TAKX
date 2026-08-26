package gov.takx.api.globe;

import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.platform.IRaptorShutdownListener;
import gov.takx.api.shapes.IShape;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeListener;
import java.util.Collection;

/**
 */
public interface IGlobeMapObjectManager extends PropertyChangeListener, IRaptorShutdownListener
{
    /**
     * @param key key of object to obtain
     * @param forPlayback Pass {@code true} to get the playback globe map object
     * @return The globe map object with the given key, {@code null} if not found
     * @since 5.7
     */
    @Nullable
    IGlobeMapObject getGlobeMapObject(@Nonnull String key, boolean forPlayback);

    /**
     * @param key key of object to obtain
     * @return The globe map object with the given key, {@code null} if not found
     */
    @Nullable
    default IGlobeMapObject getGlobeMapObject(@Nonnull String key)
    {
        return getGlobeMapObject(key, false);
    }

    /**
     * @return The current editor on this globe
     * @since 3.3
     */
    @Nullable
    IGlobeMapObjectEditor getCurrentEditor();

    /**
     * Called when the World Wind globe is loaded and initialized.
     */
    void worldWindowLoaded();

    /**
     * @return The list of all shape map objects
     * @since 2.20
     */
    @Nonnull
    Collection<IGlobeShapeMapObject> getShapeMapObjects();

    /**
     * Activate the given map tool on the given globe. This allows the user to interactively draw one of our
     * supported tool types.  This will result in the creation of a Map Object.
     *
     * @param mapToolType Tool to activate
     * @return Globe map object for the installed tool, {@code null} if {@code mapToolType} is {@link MapToolType#NONE}
     * @since 3.5
     */
    @Nullable
    default IGlobeMapObject setMapTool(@Nonnull MapToolType mapToolType)
    {
        return setMapTool(mapToolType, false);
    }

    /**
     * Activate the given map tool on the given globe. This allows the user to interactively draw one of our
     * supported tool types.  This may result in the creation of a Map Object depending on the value of {@code drawOnly}.
     *
     * @param mapToolType Tool to activate
     * @param drawOnly Pass {@code true} if the shape should be drawn only, and not create a Map Object
     * @return Globe shape map object for the installed tool, {@code null} if {@code mapToolType} is {@link MapToolType#NONE}
     * @since 3.5
     */
    @Nullable
    IGlobeMapObject setMapTool(@Nonnull MapToolType mapToolType, boolean drawOnly);

    /**
     * Cancel any map tool (editor) that may be active.  This will NOT apply any updates from the current editor
     * to the edited object.
     *
     * @since 5.6
     */
    void cancelMapTool();

    /**
     * Remove the globe map object for the given shape.
     * This method is only exposed for use by {@link gov.takx.api.shapes.IShapeDrawingService}.
     *
     * @param shape The shape for which to remove the globe map object
     * @since 3.5
     */
    void removeGlobeMapObjectForShape(@Nonnull IShape shape);
}
