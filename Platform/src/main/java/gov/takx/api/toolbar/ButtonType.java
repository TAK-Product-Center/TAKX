package gov.takx.api.toolbar;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.shapes.ShapeType;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;
import static gov.takx.platform.ui.UIResources.TOOLBAR_BUTTON_IMAGE_SIZE;

/**
 * Enum used to identify the button type, as well as whether it was created by TAKX or a plugin.
 *
 * @since 2.0 (Renamed and moved into API in 3.4)
 */
@SuppressWarnings("ConstantConditions")
public enum ButtonType
{
    // Home tab buttons
    NAVIGATION_LOCK("Navigation Lock", "nav_lock.png"),
    NEW_GLOBE("New Globe", "globe_new.png"),

    OVERLAYS("Overlays", "Overlays_32x32.png", TAK_ASSETS_PNG),
    MAPS("Maps", "Maps-And-Favorites_32x32.png", TAK_ASSETS_PNG),
    /** @since 3.5 */
    MULTI_SELECTOR("MultiSelector Tool", "multiselector.png"),
    /** @since 5.7 */
    ELEVATION_TOOLS("Elevation Tools", "Elevation-Tools_32x32.png", TAK_ASSETS_PNG),

    /** @since 5.7 */
    ALERT("ALERT", "Emergency_32x32.png", TAK_ASSETS_PNG),

    POINT_DROPPER("Point Dropper", "Enter-Location_32x32.png", TAK_ASSETS_PNG),
    DRAW("Drawing Tools", "Drawing-Tools_32x32.png", TAK_ASSETS_PNG),
    ROUTES("Routes", "route.png"),
    GO_TO_LOCATION("Go to Location", "Go-To_32x32.png", TAK_ASSETS_PNG),
    /** @since 5.2 **/
    CHATS("Chats", "chat.png"),
    CITY_SEARCH("City Search", "search.png"),

    CONTACTS("Contacts", "contacts.png"),
    PLAYBACK("Playback", "playback.png"),
    DEVICES("Devices", "device.png"),
    FILES("Files", "files.png"),
    PROPERTIES("Properties", "settings.png"),

    CONNECTIONS("Sensor Feeds", "connection.png"),
    MEDIA("Media", "media.png"),
    TAK_SERVER("TAK Server", "tak_server.png"),
    DATA_PACKAGES("Data Packages", "Mission-Package_32x32.png", TAK_ASSETS_PNG),
    /** @since 5.0 */
    DATA_SYNC("Data Sync", "Data-Sync_32x32.png", TAK_ASSETS_PNG),
    /** @since 5.1 */
    TAC_GRAPHICS("2525", "icon_tac_graphics.png"),
    /** @since 5.6 */
    ELEVATION_PROFILER("Elevation Profile", "elevation.png"),
    EVENT_LOG("Event Log", "event_log.png"),
    /** @since 5.2 */
    NOTIFICATIONS("Notifications", "notifications.png"),
    SYSTEM_MESSAGES("System Messages", "system_messages.png"),
    /** @since 5.3 */
    TRACKS("Tracks", "Track-History_32x32.png", TAK_ASSETS_PNG),
    /** @since 5.4 */
    TRACK_SEARCH("Track Search", "Track-Search_32x32.png", TAK_ASSETS_PNG),
    /** @since 5.5 */
    TRACK_NEW("New Track", "New-Track_32x32.png", TAK_ASSETS_PNG),
    /** @since 5.5 */
    TRACK_CLEAR("Clear Tracks", "Clear-Tracks_32x32.png"),
    // Connect tab buttons
    CONNECTION_PLUGIN(false, "Connection"),

    // Devices tab buttons
    DEVICE_PLUGIN(false, "Device"),

    // Maps tab buttons
    // TODO KMB: TAKX-1436 will help with the length of these button names
    CONNECT_TO_ELEVATION_SERVER("Connect to Elevation Server", "elevation_server.png"),
    CONNECT_TO_ELEVATION_MAP_SET("Connect to Elevation Map Set", "elevation_cache.png"),
    /** @since 3.4.1 */
    LOAD_ELEVATION_FILES("Load Elevation Files", "elevation_files.png"),

    CONNECT_TO_IMAGERY_SERVER("Connect to Imagery Server", "imagery_server.png"),
    CONNECT_TO_IMAGERY_MAP_SET("Connect to Imagery Map Set", "imagery_cache.png"),
    /** @since 3.4.1 */
    LOAD_IMAGERY_FILES("Load Imagery Files", "imagery_files.png"),
    // TODO - icon
    /** @since 5.0 */
    LOAD_MODEL_FILES("Load Model Files", "folder_open.png"),

    IMAGE_OVERLAY("Image", "imagery_overlay.png"),
    KML_KMZ_IMPORT("KML/KMZ Import", "import.png"),
    KML_KMZ_EXPORT("KML/KMZ Export", "export.png"),
    SHP("SHP", "shp_overlay.png"),

    MAP_SET_BUILDER("Map Set Builder", "cache_builder.png"),
    CACHE_CLEANER("Cache Cleaner", "cache_cleaner.png"),
    TERRAIN_DISCOVERY("Terrain Discovery", "terrain.png"),
    SERVER_PASSWORDS("Server Passwords", "server_security.png"),

    ADD_FAVORITE("Add Favorite", "favorite_add.png"),
    MANAGE_FAVORITES("Manage Favorites", "favorite_settings.png"),

    // Attachments tab buttons
    AUDIO("Audio", "audio.png"),
    PHOTO("Photo", "photo.png"),
    VIDEO("Video", "video.png"),

    FILE("File", "file.png"),

    // Data tab buttons
    IMPORT_PLUGIN(false, "Import"),
    EXPORT_PLUGIN(false, "Export"),

    // Draw tab buttons
    SELECTOR("Selector", "cursor.png"),

    /** @since 5.0 */
    ARROW(ShapeType.ARROW.getShapeType(), ShapeType.ARROW.getIconName()),
    POLYLINE(ShapeType.POLYLINE.getShapeType(), ShapeType.POLYLINE.getIconName()),
    /** @since 3.5 */
    FREEHAND_POLYLINE(ShapeType.FREEHAND_POLYLINE.getShapeType(), ShapeType.FREEHAND_POLYLINE.getIconName()),
    SQUARE(ShapeType.SQUARE.getShapeType(), ShapeType.SQUARE.getIconName()),
    RECTANGLE(ShapeType.RECTANGLE.getShapeType(), ShapeType.RECTANGLE.getIconName()),
    POLYGON(ShapeType.POLYGON.getShapeType(), ShapeType.POLYGON.getIconName()),
    /** @since 3.5 */
    FREEHAND_POLYGON(ShapeType.FREEHAND_POLYGON.getShapeType(), ShapeType.FREEHAND_POLYGON.getIconName()),
    CIRCLE(ShapeType.CIRCLE.getShapeType(), ShapeType.CIRCLE.getIconName()),
    ELLIPSE(ShapeType.ELLIPSE.getShapeType(), ShapeType.ELLIPSE.getIconName()),

    TEXT_BOX("Text Callout", "text_callout.png"),
    TEXT_LABEL("Text Label", "text_box.png"),
    RANGE_RINGS(ShapeType.RANGE_FAN.getShapeType(), ShapeType.RANGE_FAN.getIconName()),
    GEO_PING("Geo Ping", "ping.png"),

    PLUGIN_DRAW_TOOL(false, "Draw Tool"),

    // View tab buttons
    GO_TO_AREA("Go to Area", "go_to_area.png"),
    GLOBE_VIEW("Globe View", "globe.png"),
    DEFAULT_HEADING("Default Heading", "heading.png"),
    DEFAULT_TILT("Default Tilt", "tilt.png"),
    DEFAULT_HEADING_AND_TILT("Default Heading and Tilt", "heading_and_tilt.png"),
    KEEP_IN_VIEW("Keep in View", "keep_in_view.png"),

    PROJECT_FAVORITE(false, "Project Favorite", "favorite_project.png"),
    USER_FAVORITE(false, "User Favorite", "favorite_user.png"),

    // Apps tab buttons
    APPS("Plugins", "plugin.png"),

    PLUGIN_PANEL(false, "Plugin UI"),
    PLUGIN_SCRIPT(false, "Script", "javascript.png"),

    // Range & Bearing tab buttons

    /** @since 5.0.0 */
    DYNAMIC_MEASURE("Dynamic Measure", "dynamic-measure.png"),
    /** @since 5.5.0 */
    MEASURE("Measure", "measure.png"),
    /** @since 5.5 */
    BULLSEYE("Bullseye", "Bullseye_32x32.png", TAK_ASSETS_PNG),
    /** @since 5.5 */
    RB_CIRCLE("R&B Circle", "Range-And-Bearing-Circle_32x32.png", TAK_ASSETS_PNG),
    /** @since 5.6 */
    DRIFTER("Drifter", "Drawing-Compass_32x32.png", TAK_ASSETS_PNG),
    /** @since 5.6 */
    BLOODHOUND("Bloodhound", "Bloodhound_32x32.png", TAK_ASSETS_PNG),

    // Help tab buttons
    ABOUT("About", "info_outline.png"),
    OPENGL_INFO("OpenGL Info", "opengl.png"),
    HELP(" Help", "support.png"),
    REPORT_ERROR("Report Issue", "warning.png"),

    PLUGIN_DOCUMENTATION(false, "Documentation"),

    // Available buttons that are not included in the toolbar by default
    NEW_PROJECT("New", "folder_new.png"),
    OPEN_PROJECT("Open", "folder_open.png"),
    PACKAGE_PROJECT("Package", "folder_zip.png"),
    PLACEMARKS("Placemarks", "placemark.png"),
    PLUGIN_MANAGER("Plugins Manager", "plugin_settings.png"),
    PROJECT_CLEANER("Project Cleaner", "delete.png"),
    /** @since 5.1 */
    CLEAR_CONTENT("Clear Content", "delete_sweep.png"),
    /** @since 5.7 */
    PLUGIN_PREFERENCES("Plugin Preferences", "settings.png"),
    PROJECT_SETTINGS("Project Settings", "folder_settings.png"),
    SAVE_PROJECT("Save", "save.png"),
    SAVE_PROJECT_AS("Save As", "save_as.png"),
    /** @since 5.7 */
    SCREEN_SHOT_MENU("Screen Shot", "print_screen.png"),
    SCREEN_SHOT_APPLICATION("Screen Shot Entire Application", "print_screen.png"),
    SCREEN_SHOT_GEOTIFF("Screen Shot as GeoTIFF", "print_screen.png"),
    SCREEN_SHOT_GLOBE("Screen Shot Globe", "print_screen.png"),
    /**
     * @deprecated Use {@link #PLUGIN_PREFERENCES} instead
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    USER_PREFERENCES("Plugin Preferences", "settings.png"),
    /** @since 5.4 */
    SETTINGS("Settings", "settings.png"),
    QUIT("Quit", "power.png"),

    // Buttons that were created by plugins (and aren't draw tools, which are handled separately above)
    PLUGIN_CREATED(false, null);

    /**
     * The default display text from buttons with this type (may be {@code null}).
     */
    private final String displayText;

    /**
     * Optional string used to clarify the button type (e.g. a device vs connection) in the button list UI.
     */
    private final String category;

    /**
     * Name of the icon to use for buttons with this type.
     */
    private final String iconName;

    /**
     * The resource directory for the icon (default is PLATFORM_ICONS).
     */
    private final ResourceDirectory resourceDirectory;

    /**
     * Indicates if there will only ever be one unique button with this type.
     */
    private final boolean singular;

    /**
     * The id for all buttons of this type (if the type is singular).
     */
    private final UUID singularId;

    /**
     * Constructor for singular button types (i.e., there will only ever be one unique button with that type) and an
     * icon in the default location, {@link ResourceDirectory#PLATFORM_ICONS}.
     * @since 3.4
     */
    ButtonType(@Nullable String displayText, @Nonnull String iconName)
    {
        this(displayText, iconName, PLATFORM_ICONS);
    }

    /**
     * Constructor for singular button types (i.e., there will only ever be one unique button with that type) that need
     * to specify the resource directory of the icon.
     * @since 3.4
     */
    ButtonType(@Nullable String displayText, @Nonnull String iconName, @Nonnull ResourceDirectory resourceDirectory)
    {
        this(displayText, true, null, iconName, resourceDirectory);
    }

    /**
     * Constructor for buttons with a category that don't have a specific icon to assign.
     */
    ButtonType(boolean singular, @Nullable String category)
    {
        this(singular, category, "plugin.png");
    }

    /**
     * Constructor for buttons with a category and an icon in {@link ResourceDirectory#PLATFORM_ICONS}.
     */
    ButtonType(boolean singular, @Nullable String category, @Nonnull String iconName)
    {
        this(null, singular, category, iconName, PLATFORM_ICONS);
    }

    /**
     * @param displayText The default display text for the button
     * @param singular Indicates whether there will only ever be one unique button with this type
     * @param category The category to which the button belongs; used to clarify button type (e.g. Device)
     * @param iconName The file name of the icon to display on the button, relative to the specified resource directory
     * @param resourceDirectory The resource directory containing the icon
     * @since 3.0 (signature changed in 3.4)
     */
    ButtonType(@Nullable String displayText, boolean singular, @Nullable String category, @Nonnull String iconName,
               @Nonnull ResourceDirectory resourceDirectory)
    {
        this.displayText = displayText;
        this.singular = singular;
        this.category = category;
        this.iconName = iconName;
        this.resourceDirectory = resourceDirectory;
        singularId = singular ? UUIDUtils.generateType3UUID(name()) : null;
    }

    /**
     * @return The display text for this button type (may be {@code null} for non-singular buttons).
     */
    @Nullable
    public String getDisplayText()
    {
        return displayText;
    }

    /**
     * @return The display text for this button type.
     * @throws NullPointerException If the display text is {@code null}
     * @since 3.4
     */
    @Nonnull
    public String requireDisplayText()
    {
        return Objects.requireNonNull(displayText, "displayText is null");
    }

    /**
     * Indicates if this is a singular button type, meaning there will only ever be one unique button with this type.
     * Note: there may be multiple button instances with this type, but they will have the same id and will be
     * considered equivalent to one another and have the same behavior; non-singular types may have multiple buttons
     * with that type which will have different ids and behavior.
     * <p/>
     * For example, {@link #POINT_DROPPER} is a singular type so any buttons with that type will have the same id and
     * the same behavior of opening the point dropper panel. There still may be multiple button instances with this type
     * on different tabs for the convenience of the user, but they will all be considered to represent the same button.
     * <p/>
     * Conversely, {@link #USER_FAVORITE} is a non-singular type, meaning there may be multiple buttons with that type.
     * They will have different ids and different behavior: each will move the last focused globe to the view
     * represented by its associated favorite.
     *
     * @return True if there is only ever one button with this type.
     * @since 3.4
     */
    public boolean isSingular()
    {
        return singular;
    }

    /**
     * @return Optional string used to clarify the button type (e.g. a device vs connection) in the button list UI.
     */
    @Nullable
    public String getCategory()
    {
        return category;
    }

    /**
     * @return The URL of the default image for this button type.
     * @since 3.4
     */
    @Nonnull
    public URL getImageUrl()
    {
        return UIResources.getUrl(resourceDirectory, iconName);
    }

    /**
     * @return The default image for this button type, scaled to the toolbar size.
     * @since 2.37
     */
    @Nonnull
    public BufferedImage getImage()
    {
        //noinspection ConstantConditions - won't be null since the default will be returned
        return UIResources.getSizedImage(resourceDirectory, iconName, TOOLBAR_BUTTON_IMAGE_SIZE, true);
    }

    /**
     * @return The id for all buttons of this type (if the type is singular), or {@code null} if the button is not singular.
     * @since 3.4
     * @see #isSingular()
     */
    @Nullable
    public UUID getSingularId()
    {
        return singularId;
    }

    /**
     * @return The id for all buttons of this type.
     * @throws NullPointerException If the singular id is {@code null}
     * @since 3.4
     */
    @Nonnull
    public UUID requireSingularId()
    {
        return Objects.requireNonNull(singularId, "singularId is null");
    }
}
