package gov.tak.api.maps;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.api.mapobject.MapObjectOption;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.platform.utils.UnitsOfMeasureUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Contract for an item on or referencing the map.  This is a bridge interface to attempt to harmonize the
 * TAKX code with the ATAK code (where MapItem is a concrete class).
 *
 * @since 3.0
 */
@SuppressWarnings("NewClassNamingConvention")
@NoPluginImplementations
public interface MapItem
{
    /**
     * @return The attribute set for this Map Item
     */
    @Nonnull
    AttributeSet getAttributeSet();

    /**
     * Get the UID of the Map Item.  It can only be set during construction.  This UID is a String,
     * as compared to other UUID identifiers on other API objects.  If this is not set, then a UID will be
     * generated as the string form of a random UUID.
     * <p>
     * A typical case for this is when processing the import of a CoT event, which specifies a UID.
     * That UID will then be used for the Map Item constructed from the event.
     *
     * @return The UID received in a Cursor-on-Target message that created this Map Item
     * @see MapObjectSpec#uid()
     */
    @Nonnull
    String getUid();

    /**
     * @return the CoT type of this Map Item
     */
    @Nonnull
    String getCotType();

    /**
     * Set the CoT type of this Map Item.  This should only be used on "point" types (typically to change the
     * affiliation).
     *
     * @param cotType New CoT type
     */
    void setCotType(@Nonnull String cotType);

    /**
     * @return The latest CoT event received for this map item. May be {@code null} if the map item was not created from
     * a CoT event and a CoT event has never been received from the map item.
     * @since 3.2
     */
    @Nullable
    CotEvent getLatestCotEvent();

    /**
     * Set the latest CoT event received for this map item. For cases where the map item was created from a CoT
     * event, this will be the most recent CoT event received for that map item. For cases where the map item was
     * created locally, this may be {@code null} if no CoT events have been received for the map item.
     * @param cotEvent The most recently received CoT event
     * @since 3.2
     */
    void setLatestCotEvent(@Nonnull CotEvent cotEvent);

    /**
     * Get the title of this map item (display name)
     *
     * @return Item title or {@code null} if N/A
     */
    default String getTitle()
    {
        final AttributeSet attributeSet = getAttributeSet();

        return attributeSet.containsAttribute(StandardAttributeNames.TITLE)
                ? attributeSet.getStringAttribute(StandardAttributeNames.TITLE, "")
                : null;
    }

    /**
     * Set the title of this map item (display name)
     *
     * @param title Item title
     */
    default void setTitle(String title)
    {
        setMetaString(StandardAttributeNames.TITLE, title);
    }

    /**
     * Set speed in meters/sec.
     * <p>
     * Each time an RDM is processed for this Map Item, this method will automatically be called if the RDM
     * has a speed set.
     *
     * @param speed The speed in meters/sec
     * @see IPrePersistRaptorDataMessage#hasSpeed()
     */
    void setSpeedInMps(double speed);

    /**
     * @return Speed of the map item in meters/sec, or {@link Double#NaN} if no speed has been set on this map item
     * @see #hasSpeed()
     * @see UnitsOfMeasureUtils
     */
    double getSpeedInMps();

    /**
     * @return {@code true} if a valid speed (not {@link Double#NaN}) has been set on this map item.
     */
    boolean hasSpeed();

    /**
     * Set the heading of this Map Item, normalized to the range [0..359], with 0 indicating true North.
     * <p>
     * Each time an RDM is processed for this Map Item, this method will automatically be called if the RDM
     * has a heading set.
     *
     * @param heading Heading of Map Item
     * @see IPrePersistRaptorDataMessage#hasHeading()
     */
    void setHeading(double heading);

    /**
     * @return Heading in range [0..359], with 0 indicating true North, or {@link Double#NaN} if no heading has been
     * set on this map item
     * @see #hasHeading()
     */
    double getHeading();

    /**
     * @return {@code true} if a valid heading (not {@link Double#NaN}) has been set on this map item.
     */
    boolean hasHeading();

    /**
     * Sets the heading and speed for this map item.
     *
     * @param heading the track heading in degrees True, will be normalized to [0..359]
     * @param speed   the track speed in meters/sec
     */
    void setTrack(double heading, double speed);

    /**
     * Check if an attribute with the given key is present in our attribute set.
     *
     * @param key Key of metadata value to test
     * @return {@code true} if the attribute is present, {@code false} otherwise
     */
    default boolean hasMetaValue(@Nonnull String key)
    {
        return getAttributeSet().containsAttribute(key);
    }

    /**
     * Returns the value associated with the given key, or {@code defaultValue} if no value has been set for that key.
     * <p>
     * This is a convenience method, equivalent to {@code getAttributeSet().getStringAttribute(key, defaultValue)}
     *
     * @param key          Key of metadata value to return
     * @param defaultValue Value to return if key is not present
     * @return String value, or {@code defaultValue} if not present
     * @see AttributeSet#getStringAttribute(String, String)
     */
    default String getMetaString(@Nonnull String key, @Nonnull String defaultValue)
    {
        return switch (key)
        {
            case StandardAttributeNames.UID -> getUid();
            case StandardAttributeNames.COT_TYPE -> getCotType();
            default -> getAttributeSet().getStringAttribute(key, defaultValue);
        };
    }

    /**
     * Set an attribute (metadata) value.  As a special case, when {@code value} is {@code null}, the named attribute will
     * be removed, since {@code null} values are not allowed in an {@link AttributeSet}.
     *
     * @param key   Key of attribute to set
     * @param value Value to set
     * @see #getAttributeSet()
     * @see AttributeSet#setAttribute(String, String)
     */
    void setMetaString(@Nonnull String key, @Nullable String value);

    /**
     * Returns the value associated with the given key, or {@code defaultValue} if no value has been set for that key.
     * <p>
     * This is a convenience method, equivalent to {@code getAttributeSet().getBooleanAttribute(key, defaultValue)}
     *
     * @param key          Key of metadata value to return
     * @param defaultValue Value to return if key is not present
     * @return boolean value, or {@code defaultValue} if not present
     * @see #getAttributeSet()
     * @see AttributeSet#getBooleanAttribute(String, boolean)
     */
    default boolean getMetaBoolean(@Nonnull String key, boolean defaultValue)
    {
        return getAttributeSet().getBooleanAttribute(key, defaultValue);
    }

    /**
     * Set an attribute (metadata) value.
     *
     * @param key   Key of attribute to set
     * @param value Value to set
     * @see #getAttributeSet()
     * @see AttributeSet#setAttribute(String, boolean)
     */
    default void setMetaBoolean(@Nonnull String key, boolean value)
    {
        getAttributeSet().setAttribute(key, value);
    }

    /**
     * Returns the value associated with the given key, or {@code defaultValue} if no value has been set for that key.
     * <p>
     * This is a convenience method, equivalent to {@code getAttributeSet().getDoubleAttribute(key, defaultValue)}
     *
     * @param key          Key of metadata value to return
     * @param defaultValue Value to return if key is not present
     * @return double value, or {@code defaultValue} if not present
     * @see #getAttributeSet()
     * @see AttributeSet#getDoubleAttribute(String, double)
     */
    default double getMetaDouble(@Nonnull String key, double defaultValue)
    {
        return getAttributeSet().getDoubleAttribute(key, defaultValue);
    }

    /**
     * Set an attribute (metadata) value.
     *
     * @param key   Key of attribute to set
     * @param value Value to set
     * @see #getAttributeSet()
     * @see AttributeSet#setAttribute(String, double)
     */
    default void setMetaDouble(@Nonnull String key, double value)
    {
        getAttributeSet().setAttribute(key, value);
    }

    /**
     * Returns the value associated with the given key, or {@code defaultValue} if no value has been set for that key.
     * <p>
     * This is a convenience method, equivalent to {@code getAttributeSet().getIntAttribute(key, defaultValue)}
     *
     * @param key          Key of metadata value to return
     * @param defaultValue Value to return if key is not present
     * @return String value, or {@code defaultValue} if not present
     * @see #getAttributeSet()
     * @see AttributeSet#getIntAttribute(String, int)
     */
    default int getMetaInteger(@Nonnull String key, int defaultValue)
    {
        return getAttributeSet().getIntAttribute(key, defaultValue);
    }

    /**
     * Set an attribute (metadata) value.
     *
     * @param key   Key of attribute to set
     * @param value Value to set
     * @see #getAttributeSet()
     * @see AttributeSet#setAttribute(String, int)
     */
    default void setMetaInteger(@Nonnull String key, int value)
    {
        getAttributeSet().setAttribute(key, value);
    }

    /**
     * Returns the value associated with the given key, or {@code defaultValue} if no value has been set for that key.
     * <p>
     * This is a convenience method, equivalent to {@code getAttributeSet().getLongAttribute(key, defaultValue)}
     *
     * @param key          Key of metadata value to return
     * @param defaultValue Value to return if key is not present
     * @return String value, or {@code defaultValue} if not present
     * @see #getAttributeSet()
     * @see AttributeSet#getLongAttribute(String)
     */
    default long getMetaLong(@Nonnull String key, long defaultValue)
    {
        return getAttributeSet().getLongAttribute(key, defaultValue);
    }

    /**
     * Set an attribute (metadata) value.
     *
     * @param key   Key of attribute to set
     * @param value Value to set
     * @see #getAttributeSet()
     * @see AttributeSet#setAttribute(String, long)
     */
    default void setMetaLong(@Nonnull String key, long value)
    {
        getAttributeSet().setAttribute(key, value);
    }

    /**
     * @return Remarks for this map item
     */
    String getRemarks();

    /**
     * Set the "remarks" for this map item.
     *
     * @param remarks Remarks for map item
     */
    void setRemarks(String remarks);

    /**
     * Add the given file as an attachment. The file will be copied into the project's VFS,
     * no reference to the source file is retained. A file that does not exist will be ignored.
     * <p/>
     * <b>Notes:</b> If an attachment already exists by the given {@code attachmentName}, it will be overwritten. Additionally,
     * the original file name will not be used inherently. The passed in {@code attachmentName} will
     * be the only name used to reference this attachment (e.g. - {@link #deleteAttachments(String...)})
     *
     * @param attachmentName The new name for the attachment, the original file name will not be used
     * @param fileToAdd The {@link File} to add to attachments
     * @return The File in the VFS system of the attachment
     * @since 5.2
     */
    File addAttachment(@Nonnull String attachmentName, @Nonnull File fileToAdd);

    /**
     * Copy the contents of the given {@link InputStream} into an attachment {@link File}.
     * A file will be created in the project's VFS, no reference to the source {@link InputStream} is retained.
     * This method does not close the {@link InputStream}, the caller must handle that.
     * <p/>
     * <b>Note:</b> If an attachment already exists by the given {@code attachmentName}, it will be overwritten.
     *
     * @param attachmentName The new name for the attachment, the original file name will not be used
     * @param source The {@link InputStream} to add to attachments
     * @return The File in the VFS system of the attachment
     * @since 5.2
     */
    File addAttachment(@Nonnull String attachmentName, @Nonnull InputStream source);

    /**
     * Delete file attachments, if they exist. The correct way to use this is via the
     * {@link #getAttachments()} or {@link #getAttachment(String)} methods and pass a subset of the returned values.
     *
     * @param filesToDelete Array of {@link File}s to remove from attachments
     * @see #getAttachments()
     * @see #getAttachment(String) 
     * @since 5.2
     */
    void deleteAttachments(@Nonnull File... filesToDelete);

    /**
     * Delete file attachments, if they exist. Accepts file names only, *not* file paths.
     *
     * @param attachmentNamesToDelete Array of {@code String} file names to remove from attachments
     * @since 5.2
     */
    void deleteAttachments(@Nonnull String... attachmentNamesToDelete);

    /**
     * @return A {@link List} of attached {@link File}s.
     * @since 5.2
     */
    @Nonnull
    List<File> getAttachments();

    /**
     * @param attachmentName The name of the attachment to get
     *
     * @return A {@link File} from the attachment list, if one exists with this name or {@code null} if there is no
     * attachment by that name.
     * @since 5.2
     */
    @Nullable
    File getAttachment(@Nonnull String attachmentName);

    /**
     * Add a handler that will be invoked when this map item has been deleted.
     * <p/>
     * Note: There is no need to unregister property change listeners on the Map Object from within this handler.
     * The Map Object will remove all registered listeners as part of its deletion cleanup.
     *
     * @param handler Handler to invoke upon deletion
     * @since 5.3
     */
    void addDeletionHandler(Consumer<MapItem> handler);

    /**
     * Remove a previously registered deletion handler.
     *
     * @param handler Handler to remove
     * @since 5.3
     */
    void removeDeletionHandler(Consumer<MapItem> handler);

    /**
     * Notify deletion handlers of the deletion of this map item.
     * <p/>
     * <em>This is for internal use only and must not be called from plugins.</em>
     *
     * @since 5.4
     */
    @Internal
    void notifyDeletionHandlers();

    /**
     * Determine if this Map Object is a device, meaning it was defined by a Device plugin.
     * This is directly related to the group name.
     *
     * @return {@code true} if this Map Object is a device, {@code false} otherwise
     * @see MapObjectGroupNames
     * @since 2.0.0 (moved here in 5.6)
     */
    boolean isDevice();

    /**
     * @return The Map Object Options used to create this Map Object
     * @since 2.29 (moved here in 5.6)
     */
    MapObjectOptions getMapObjectOptions();

    /**
     * @param option Option to test
     * @return {@code true} If the option is set on this map object
     * @since 2.29 (moved here in 5.6)
     */
    default boolean hasOption(MapObjectOption option)
    {
        return getMapObjectOptions().hasOption(option);
    }

    /**
     * @return {@code true} If the option {@link MapObjectOption#NoStyledTracks} is <em>NOT</em> set on this map object
     * @since 2.29 (moved here in 5.6)
     */
    default boolean canHaveStyledTrackData()
    {
        return isDevice() && !isTransient() && !hasOption(MapObjectOption.NoStyledTracks);
    }

    /**
     * @return {@code true} If the option {@link MapObjectOption#NoAlarms} is <em>NOT</em> set on this map object
     * @since 2.29 (moved here in 5.6)
     */
    default boolean canHaveAlarms()
    {
        return !isTransient() && !hasOption(MapObjectOption.NoAlarms);
    }

    /**
     * @return {@code true} If the option {@link MapObjectOption#ExcludeFromExplorer} is <em>NOT</em> set on this map object
     * @since 2.29 (moved here in 5.6)
     */
    default boolean shouldShowInExplorer()
    {
        return !hasOption(MapObjectOption.ExcludeFromExplorer);
    }

    /**
     * @return {@code true} If the option {@link MapObjectOption#NoEventLogs} is <em>NOT</em> set on this map object
     * @since 2.29 (moved here in 5.6)
     */
    default boolean shouldCreateEventLogEntries()
    {
        return !isTransient() && !hasOption(MapObjectOption.NoEventLogs);
    }

    /**
     * @return {@code true} If the option {@link MapObjectOption#NoPluginState} is <em>NOT</em> set on this map object
     * @since 2.29 (moved here in 5.6)
     */
    default boolean canHavePluginState()
    {
        return !isTransient() && !hasOption(MapObjectOption.NoPluginState);
    }

    /**
     * @return {@code true} if this Map Item is transient, meaning it is not persisted in the database
     * @since 5.6
     */
    default boolean isTransient()
    {
        return hasOption(MapObjectOption.NoPersistence);
    }
}
