package gov.tak.platform.datapackage.content;

import gov.takx.api.annotation.Internal;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.file.SizeFormatter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;

/**
 * Simple holder class for data pertaining to a single data package entry and its associated Map Object, if any.
 * Members are public for easy direct access.
 *
 * @since 3.2
 */
@Internal
public class DataPackageEntry
{
    /**
     * Internal ZIP path within the containing data package file.
     *
     * @since 3.5
     */
    public final String zipEntryPath;
    public final Icon icon;
    public final String type;
    public final String name;
    public final String size;
    public IMapObject mapObject;

    /**
     * Constructor for an entry with no Map Object and no known icon.
     *
     * @param zipEntryPath Internal ZIP path within the containing data package file.
     * @param type Entry type, matching the file extension of the entry path, but only for supported types like "KML" or a CoT type.
     * @param name Display name.
     * @param size Size of the data package entry within the ZIP file.
     *
     * @since 3.5 Added {@code zipEntryPath}
     */
    public DataPackageEntry(@Nonnull String zipEntryPath, @Nonnull String type, @Nonnull String name, long size)
    {
        this(zipEntryPath, null, null, type, name, size);
    }

    /**
     * Constructor for an entry with no Map Object, but has a pre-defined icon.
     *
     * @param zipEntryPath Internal ZIP path within the containing data package file.
     * @param icon Associated icon of the data package entry map object, or file type if not a map object.
     * @param name Display name.
     * @param size Size of the data package entry within the ZIP file.
     *
     * @since 3.5 Added {@code zipEntryPath}
     */
    public DataPackageEntry(@Nonnull String zipEntryPath, @Nonnull Icon icon, @Nonnull String name, long size)
    {
        this(zipEntryPath, null, icon, null, name, size);
    }

    /**
     * Constructor for an entry with an associated Map Object.  The name will come from the Map Object's title.
     *
     * @param zipEntryPath Internal ZIP path within the containing data package file.
     * @param mapObject Associated map object of the data package entry. The {@link IMapObject#getTitle()} will be used for the display name.
     * @param icon Associated icon of the data package entry map object, or file type if not a map object.
     * @param size Size of the data package entry within the ZIP file.
     *
     * @since 3.5 Added {@code zipEntryPath}
     */
    public DataPackageEntry(@Nonnull String zipEntryPath, @Nonnull IMapObject mapObject, @Nonnull Icon icon, long size)
    {
        this(zipEntryPath, mapObject, icon, null, mapObject.getTitle(), size);
    }

    /**
     * @param zipEntryPath Internal ZIP path within the containing data package file.
     * @param mapObject Associated map object of the data package entry.
     * @param icon Associated icon of the data package entry map object, or file type if not a map object.
     * @param type Entry type, matching the file extension of the entry path, but only for supported types like "KML" or a CoT type.
     * @param name Display name.
     * @param size Size of the data package entry within the ZIP file.
     */
    private DataPackageEntry(@Nonnull String zipEntryPath, @Nullable IMapObject mapObject, @Nullable Icon icon,
                             @Nullable String type, @Nonnull String name, long size)
    {
        this.zipEntryPath = zipEntryPath;
        this.mapObject = mapObject;
        this.icon = icon;
        this.type = type;
        this.name = name;
        this.size = SizeFormatter.formatEstimate(size);
    }

    /**
     * Set the {@code mapObject} to {@code null}.  Used when the MO is deleted.
     */
    public void clearMapObject()
    {
        mapObject = null;
    }

    /**
     * @return {@code true} If the {@code mapObject} is not {@code null}
     */
    public boolean hasMapObject()
    {
        return mapObject != null;
    }

    /**
     * @return {@code true} If the {@code mapObject} is not {@code null} and the Map Object has a location
     */
    public boolean hasLocation()
    {
        return mapObject != null && mapObject.hasLocation();
    }

    /**
     * @return location from Map Object, if it exists and has a location
     */
    @Nullable
    public ILocation getLocation()
    {
        return hasLocation() ? mapObject.getLocation() : null;
    }
}
