package gov.takx.platform.preferences;

import com.atakmap.commoncommo.Commo;
import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.comms.takserver.DeleteOption;
import gov.tak.api.comms.takserver.PublishOption;
import gov.tak.api.comms.takserver.TimeThresholdOption;
import gov.tak.platform.comms.commo.DeviceDisplayType;
import gov.tak.platform.comms.commo.ReportingStrategy;
import gov.tak.platform.location.providers.NoLocationProvider;
import gov.takx.api.annotation.PreferenceDescriptor;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.IProgramPreferenceManager;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.bootstrap.FlavorConstants;
import gov.takx.commons.constants.State;
import gov.takx.platform.cdi.InjectableContainerSupport;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Definitions for all of our preference/property keys and default values.  These can be used for direct value
 * injection using the {@link ProgramPreference} annotation.
 *
 * @see ProgramPreference
 * @since 2.0.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public enum PreferenceEntry implements IPreferenceEntry
{
    /**
     * Minimum length of time for a fly-to animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_FLYTO_MIN_LENGTH("Globe/Animation/FlyToMinLength", "1000"),
    /**
     * Maximum length of time for a fly-to animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_FLYTO_MAX_LENGTH("Globe/Animation/FlyToMaxLength", "2500"),
    /**
     * Minimum length of time for a heading animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_HEADING_MIN_LENGTH("Globe/Animation/HeadingMinLength", "500"),
    /**
     * Maximum length of time for a heading animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_HEADING_MAX_LENGTH("Globe/Animation/HeadingMaxLength", "3000"),
    /**
     * Minimum length of time for a pan animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_PAN_MIN_LENGTH("Globe/Animation/PanMinLength", "1000"),
    /**
     * Maximum length of time for a pan animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_PAN_MAX_LENGTH("Globe/Animation/PanMaxLength", "2500"),
    /**
     * Minimum length of time for a tilt animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_TILT_MIN_LENGTH("Globe/Animation/TiltMinLength", "500"),
    /**
     * Maximum length of time for a tilt animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_TILT_MAX_LENGTH("Globe/Animation/TiltMaxLength", "3000"),
    /**
     * Minimum length of time for a zoom animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_ZOOM_MIN_LENGTH("Globe/Animation/ZoomMinLength", "1000"),
    /**
     * Maximum length of time for a zoom animation in milliseconds.
     *
     * @since 3.4.1
     */
    ANIMATION_ZOOM_MAX_LENGTH("Globe/Animation/ZoomMaxLength", "4000"),

    /**
     * Holds a comma-delimited list of menu items that should be hidden. Items should be referenced by their {@link ButtonType#name()}.
     *
     * @since 5.7
     */
    @PreferenceDescriptor(title = "Hidden Application Menu Items", description = "Contains a comma-delimited list of menu items that should be hidden")
    APPLICATION_MENU_HIDDEN_ITEMS("ApplicationMenu/hiddenMenuItems", ""),

    APPLICATION_TITLE("ApplicationTitle", null),
    APPS_CONFIGURATION_NODE("apps/configuration", null),
    AUTO_RECONNECT_SERVICE_NODE("AutoReconnectService", null),
    AUTO_RECONNECT_CONNECTIONS("autoReconnectConnections", "false"),

    /** @since 5.6 */
    @PreferenceDescriptor(title = "Display Intercept Line & Marker", description = "Display R&B line from the follow marker to a marker at the intercept point")
    BLOODHOUND_DISPLAY_INTERCEPT("bloodhound/displayIntercept", "true"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Middle ETA Threshold", description = "ETA in seconds for the middle radius")
    BLOODHOUND_MIDDLE_ETA_THRESHOLD("bloodhound/middleEtaThreshold", "180"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Inner ETA Threshold", description = "ETA in seconds for the inner radius")
    BLOODHOUND_INNER_ETA_THRESHOLD("bloodhound/innerEtaThreshold", "60"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Outer ETA Color", description = "Color of the bloodhound line when outside the middle ETA radius")
    BLOODHOUND_OUTER_ETA_COLOR("bloodhound/outerEtaColor", "GREEN"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Middle ETA Color", description = "Color of the bloodhound line when within the middle ETA radius")
    BLOODHOUND_MIDDLE_ETA_COLOR("bloodhound/middleEtaColor", "YELLOW"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Inner ETA Color", description = "Color of the bloodhound line when within the inner ETA radius")
    BLOODHOUND_INNER_ETA_COLOR("bloodhound/innerEtaColor", "RED"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Flash ETA Color", description = "Color of the bloodhound line flash effect")
    BLOODHOUND_FLASH_ETA_COLOR("bloodhound/flashEtaColor", "BLACK"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Flash ETA Threshold", description = "ETA in seconds for when to flash")
    BLOODHOUND_FLASH_ETA_THRESHOLD("bloodhound/flashEtaThreshold", "360"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Flash When Closing In", description = "Flash the bloodhound lines and table rows when within the flash threshold")
    BLOODHOUND_FLASH("bloodhound/flash", "true"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Low Speed Color", description = "Color of the bloodhound line when follow marker's speed is too low to intercept")
    BLOODHOUND_LOW_SPEED_COLOR("bloodhound/lowSpeedColor", "ORANGE"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Auto Keep In View", description = "Enable \"Keep In View\" by default for new bloodhound configurations")
    BLOODHOUND_AUTO_KEEP_IN_VIEW("bloodhound/autoKeepInView", "true"),
    /** @since 5.6 */
    @PreferenceDescriptor(title = "Auto Target Breadcrumbs", description = "Enable breadcrumbs by default for new bloodhound configuration target markers")
    BLOODHOUND_AUTO_TARGET_BREADCRUMBS("bloodhound/autoTargetBreadcrumbs", "true"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Auto Rotate Track Colors", description = "Select to change color of each new track segment")
    BREADCRUMBS_AUTO_ROTATE_COLORS("breadcrumbs/autoRotateColors", "true"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "CSV Export Header", description = "Include column headers for CSV exports")
    BREADCRUMBS_CSV_EXPORT_HEADER("breadcrumbs/csvExportHeader", "true"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Default Track Color", description = "The default color to use for new tracks")
    BREADCRUMBS_DEFAULT_TRACK_COLOR("breadcrumbs/defaultTrackColor", "red"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Distance Threshold (in meters)", description = "The minimum distance that must be " +
            "traversed before another breadcrumb will be generated. Default is 2m")
    BREADCRUMBS_DISTANCE_THRESHOLD("breadcrumbs/distanceThreshold", "2"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "KML Export Clamp-to-Ground", description = "Select to clamp altitude values to the " +
            "ground in exported KML track files")
    BREADCRUMBS_KML_EXPORT_CLAMP_TO_GROUND("breadcrumbs/kmlClampToGround", "false"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "KML Export Timestamps", description = "Select to use gx:Track with timestamps (more " +
            "detailed info). Deselect to use LineString (more compatibility)")
    BREADCRUMBS_KML_EXPORT_TIMESTAMPS("breadcrumbs/kmlExportTimestamps", "true"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Log Self Tracks", description = "Select to always log your tracks, even when not " +
            "manually enabled and visible")
    BREADCRUMBS_LOG_SELF_TRACKS("breadcrumbs/logSelfTracksByDefault", "true"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Max Breadcrumbs", description = "Sets the max number of breadcrumbs to display on-screen")
    BREADCRUMBS_MAX_BREADCRUMBS("breadcrumbs/maxBreadcrumbs", "200"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Persistent Self Track", description = "Select to generate a polyline representing all " +
            "self marker trail breadcrumbs generated since startup, including points that have faded away")
    BREADCRUMBS_PERSISTENT_SELF_TRACKS("breadcrumbs/persistentSelfTracks", "false"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Render Line to Ground in 3D", description = "Render a line to the ground from each " +
            "breadcrumb in 3D view")
    BREADCRUMBS_RENDER_LINE_TO_GROUND("breadcrumbs/renderLineToGround", "false"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Server Track Time Gap (minutes)", description = "A server query track will be split into " +
            "separate tracks if the time gap between breadcrumbs is greater than or equal to this value, in minutes")
    BREADCRUMBS_SERVER_TRACK_TIME_GAP("breadcrumbs/serverTrackTimeGap", "10"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Breadcrumb Size", description = "The size at which breadcrumbs will be rendered on " +
            "the map (\"Normal\", \"Large\" or \"XLarge\")")
    BREADCRUMBS_SIZE("breadcrumbs/size", "Normal"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Track Name Prefix", description = "The default name prefix to use for new tracks")
    BREADCRUMBS_TRACK_TITLE_PREFIX("breadcrumbs/trackNamePrefix", "tracks_"),

    /**
     * Generated (or user entered) Call Sign.
     *
     * @since 3.0
     */
    @PreferenceDescriptor(title = "My Callsign", description = "The callsign for this device")
    CALL_SIGN("callSign", null),

    CHECK_SCREEN_CONFIG("checkScreenConfig", "true"),

    /** @since 5.3 */
    @PreferenceDescriptor(title = "On Closed Network", description = "Specifies whether we are operating on a closed network or not")
    CLOSED_NETWORK("closedNetwork", "false"),

    COMMON_COMMO_NODE("CommonCommo", null),
    COMMON_COMMO_DEFAULT_INPUTS_NODE("CommonCommo/DefaultInputs", null),
    COMMON_COMMO_DEFAULT_OUTPUTS_NODE("CommonCommo/DefaultOutputs", null),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "Use All Network Interfaces", description = "Use all available network interfaces as " +
            "Input and Output channels (Default)")
    COMMON_COMMO_USE_ALL_NETWORK_INTERFACES("CommonCommo/NetworkInterfaceOptions/UseAllNetworkInterfaces", "true"),

    /** @since 5.1 */
    COMMON_COMMO_NETWORK_INTERFACES_NODE("CommonCommo/NetworkInterfaceOptions/NetworkInterfaces", null),

    /**
     * Updates {@link com.atakmap.commoncommo.Commo#setDestUidInsertionEnabled(boolean)}
     *
     * @since 5.5
     */
    @PreferenceDescriptor(title = "Add Destination UID to directed CoT messages", description = "On certain gateway systems, " +
            "directed messages need to contain information on how to relay the message.   Only enable if this is required.")
    COMMON_COMMO_ADD_DEST_UID("CommonCommo/AddDestUid", "false"),

    /**
     * Updates {@link com.atakmap.commoncommo.Commo#setEnableAddressReuse(boolean)}
     *
     * @since 5.0
     */
    @PreferenceDescriptor(title = "Enable Address Reuse", description = "Enables the ability of datagram sockets to " +
            "share the local address with other sockets on the system. Use with caution if you expect to be able to " +
            "receive unicast datagrams.")
    COMMON_COMMO_ENABLE_ADDRESS_REUSE("CommonCommo/EnableAddressReuse", "false"),

    /**
     * Updates {@link com.atakmap.commoncommo.Commo#setMulticastLoopbackEnabled(boolean)}
     *
     * @since 5.8
     */
    @PreferenceDescriptor(title = "Enable Multicast Loopback", description = "Determines whether sent multicast packets should " +
            "be looped back to local sockets. This is the same as setting the socket option 'IP_MULTICAST_LOOP'. " +
            "NOTE: this is only functional on Linux, it has no effect on Windows.")
    COMMON_COMMO_ENABLE_MULTICAST_LOOPBACK("CommonCommo/EnableMulticastLoopback", "false"),

    /**
     * Updates {@link com.atakmap.commoncommo.Commo#setTTL(int)}
     *
     * @since 5.0
     */
    @PreferenceDescriptor(title = "Multicast TTL", description = "Multicast TTL (in hops) for outgoing multicast")
    COMMON_COMMO_MULTICAST_TTL("CommonCommo/MulticastTTL", "64"),

    /**
     * Updates {@link com.atakmap.commoncommo.Commo#setTcpConnTimeout(int)}
     *
     * @since 5.0
     */
    @PreferenceDescriptor(title = "TCP Connection Timeout", description = "How long (in sec) to wait for TCP connections to " +
            "complete. Shorter values make failed connections happen sooner (more responsive) but if the value is too " +
            "short for your network then you may get spurious connection failures.")
    COMMON_COMMO_TCP_CONNECTION_TIMEOUT("CommonCommo/TcpConnectionTimeout", "20"),

    /**
     * Updates the key used by commo to encrypt mesh network traffic. When the preference value changes, the new value
     * is stored in the {@code AtakAuthenticationDatabase} and applied to commo by calling
     * {@link com.atakmap.commoncommo.Commo#setCryptoKeys(byte[], byte[])}. The preference value is then replaced with
     * an empty String. This is a little funky, but mirrors ATAK and will allow developers to change the key
     * programmatically via this entry.
     *
     * @since 5.0
     */
    @PreferenceDescriptor(title = "Mesh Encryption", description = "Enables encryption of mesh traffic between " +
            "devices. Once encryption mode is set up, no plain text traffic will be processed by the mesh network")
    COMMON_COMMO_MESH_ENCRYPTION_KEY("CommonCommo/MeshEncryptionKey", null),

    COMMON_COMMO_TAK_SERVERS_NODE("CommonCommo/TakServers", null),

    /**
     * Updates {@link com.atakmap.commoncommo.Commo#setMissionPackageHttpsPort(int)} and the port used by the
     * {@link gov.tak.api.comms.takserver.mission.ApiProvider} for SSL connections.
     * <p>
     * Note: used when contacting a TAK server's web API for short-lived operations and <em>not</em> for the
     * long-running CoT traffic connections with the server.
     *
     * @since 5.0
     */
    @PreferenceDescriptor(title = "Secure Server API Port", description = "Secure Port of the Server API")
    COMMON_COMMO_TAK_SERVER_SECURE_API_PORT("CommonCommo/SecureServerApiPort", "8443"),

    /**
     * Updates {@link com.atakmap.commoncommo.Commo#setMissionPackageHttpPort(int)}
     * <p>
     * Note: used when contacting a TAK server's web API for short-lived operations and <em>not</em> for the
     * long-running CoT traffic connections with the server.
     *
     * @since 5.0
     */
    @PreferenceDescriptor(title = "Unsecure Server API Port", description = "Unsecure Port of the Server API")
    COMMON_COMMO_TAK_SERVER_UNSECURE_API_PORT("CommonCommo/UnsecureServerApiPort", "8080"),

    /**
     * Passed to {@link Commo#enrollmentInit(String, int, boolean, String, String, boolean, byte[], String, String, String, String, int, String)}
     * as the cert enrollment port and used by {@link gov.tak.api.comms.takserver.mission.ApiProvider} for authentication
     * connections.
     *
     * @since 5.0
     */
    @PreferenceDescriptor(title = "Certificate Enrollment API Port", description = "Port of the Certificate Enrollment API")
    COMMON_COMMO_TAK_SERVER_CERT_ENROLLMENT_API_PORT("CommonCommo/CertEnrollmentApiPort", "8446"),

    /**
     * Used by {@link Commo#enrollmentInit(String, int, boolean, String, String, boolean, byte[], String, String, String, String, int, String)}
     * If {@code true}, the system CAs are added before calling init
     *
     * @since 5.0
     */
    @PreferenceDescriptor(title = "Use System Certificate Authorities", description = "During enrollment, allow " +
            "certificate authorities installed at the system level to be used as trust anchors to verify the server")
    COMMON_COMMO_TAK_SERVER_USE_SYSTEM_CERT_AUTHORITIES("CommonCommo/UseSystemCertAuthorities", "false"),

    CONFIGURATION_NODE("Configuration", null),

    /**
     * The amount of time that must pass before an inactive {@link gov.tak.api.contact.IIndividualContact} is
     * removed from the Contacts list.
     *
     * @since 3.4.1
     */
    @PreferenceDescriptor(title = "Contact Gone Timeout",
            description = "The number of seconds to wait before removing an inactive contact from the Contacts list. " +
                    "A negative value will disable this feature.")
    CONTACT_GONE_TIMEOUT("ContactGoneTimeout", "300"),

    /** @since 5.4 */
    @PreferenceDescriptor(title = "My Email", description = "Preferred email address.  Leave blank to not publish.")
    CONTACT_EMAIL("Contact/email", ""),

    /** @since 5.4 */
    @PreferenceDescriptor(title = "Unit Reference Number",
            description = "Identifying number assigned to this device for use in legacy systems. This is an assigned number and should not be set unless provided.")
    CONTACT_URN("Contact/URN", "-1"),

    /** @since 5.1 */
    @PreferenceDescriptor(title = "Enable Individual Contact Deletion", description = "When enabled, a button that can " +
            "be used to delete individual contacts will be visible in the Contacts panel.")
    INDIVIDUAL_CONTACT_DELETION_ENABLED("EnableIndividualContactDeletion", "false"),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "Enable File Sharing",
            description = "Enables peer to peer sharing of data packages")
    DATA_PACKAGE_ENABLE_SHARING("DataPackages/EnableFileSharing", "true"),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "File Transfer Success Threshold Low",
            description = "Size (MB) of files to be sent below this threshold are colored green")
    DATA_PACKAGE_SUCCESS_THRESHOLD_LOW_MB("DataPackages/SuccessThresholdLowMB", "3"),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "File Transfer Success Threshold High",
            description = "Size (MB) of files to be sent above this threshold are colored red")
    DATA_PACKAGE_SUCCESS_THRESHOLD_HIGH_MB("DataPackages/SuccessThresholdHighMB", "15"),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "File Transfer No-Go Threshold",
            description = "Disallow transfer of files if total size (MB) is above this threshold")
    DATA_PACKAGE_NO_GO_THRESHOLD_MB("DataPackages/SuccessThresholdHighMB", "50"),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "Package Listing Query Timeout",
            description = "Max seconds to wait before cancelling a request of available data packages from TAK Server")
    DATA_PACKAGE_QUERY_TIMEOUT_SEC("DataPackages/QueryTimeoutSeconds", "15"),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "Auto Accept Data Sync Feed Invites",
            description = "Select to automatically subscribe to feeds when invited")
    DATA_SYNC_AUTO_ACCEPT_FEED_INVITES("AutoAcceptFeedInvites", "true"),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "Auto Download Updates",
            description = "Select to automatically download all feed updates regardless of file size")
    DATA_SYNC_AUTO_DOWNLOAD_UPDATES("AutoDownloadUpdates", "true"),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "Auto-Delete Threshold",
            description = "Specify how long to keep an inactive mission before prompting to delete")
    DATA_SYNC_AUTO_DELETE_THRESHOLD("AutoDeleteThreshold", TimeThresholdOption.none.name()),

    /** @since 5.7 */
    @PreferenceDescriptor(title = "Auto-Delete Removed Missions",
            description = "Prompt before deleting locally synced and downloaded missions that have been deleted from TAK Server")
    DATA_SYNC_AUTO_DELETE_REMOVED("AutoDeleteRemoteRemoved", DeleteOption.Prompt.name()),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "Large File Prompt",
            description = "Prompt before publishing or downloading files larger then the specified size (in MB). " +
                    "Download prompt is only applicable if 'Auto Download Updates' is disabled.")
    DATA_SYNC_LARGE_FILE_PROMPT("LargeFilePrompt", "5"),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "Publish Options",
            description = "Select how to publish local edit of map items")
    DATA_SYNC_PUBLISH_OPTIONS("PublishOptions", PublishOption.Prompt.name()),

    /** @since 5.0 */
    @PreferenceDescriptor(title = "Sync Threshold",
            description = "Specify how far in the past to sync to newly downloaded feeds")
    DATA_SYNC_THRESHOLD("SyncThreshold", TimeThresholdOption.none.name()),

    DATABASE_TYPE("DatabaseType", "hsqldb"),

    DEFAULT_TO_NEW_PROJECT_ON_STARTUP("defaultToNewProjectOnStart", "true"),

    DEFAULT_WMS_OFFLINE_MODE("defaultWMSOfflineMode", "false"),

    /**
     * Developer mode settings for features that ease plugin development and debugging.
     *
     * @since 3.2
     */
    @PreferenceDescriptor(title = "Unlock Developer Settings", description = "Check this to have developer preferences visible")
    DEV_MODE_SETTINGS_ENABLED("DeveloperMode/DeveloperSettingsEnabled", "false"),

    @PreferenceDescriptor(title = "CoT Message Inspection",
            description = "Adds a radial menu item to CoT objects which lets you view the most recent CoT message that created/updated that object")
    DEV_MODE_COT_MESSAGE_INSPECTION_ENABLED("DeveloperMode/CotMessageInspectionEnabled", "false"),

    /**
     * Enables continuous rendering mode to debug rendering issues.
     *
     * @since 3.5
     */
    @PreferenceDescriptor(title = "Continuous Rendering",
            description = "Enables continuous rendering mode and the frame rate overlay to debug rendering issues")
    DEV_MODE_CONTINUOUS_RENDERING_ENABLED("DeveloperMode/ContinuousRenderingEnabled", "false"),

    DISABLE_PKI_PREFERENCES("DisablePKIPreferences", "false"),

    /**
     * Docking controls.
     */
    DOCKING_FRAMES_DRAG_AND_DROP_TO_EXTERNALIZE_ENABLED("DockingFrames/DragAndDropToExternalizeEnabled", "false"),
    DOCKING_FRAMES_FRAME_MAXIMIZATION_ENABLED("DockingFrames/FrameMaximizationEnabled", "true"),
    DOCKING_FRAMES_GLOBE_MINIMIZATION_ENABLED("DockingFrames/GlobeMinimizationEnabled", "false"),
    DOCKING_FRAMES_GLOBE_EXTERNALIZATION_ENABLED("DockingFrames/GlobeExternalizationEnabled", "true"),
    DOCKING_FRAMES_MAXIMIZE_ON_DOUBLE_CLICK_ENABLED("DockingFrames/MaximizeOnDoubleClickEnabled", "false"),
    DOCKING_FRAMES_MINIMIZE_AREAS_ENABLED("DockingFrames/MinimizeAreasEnabled", "true"),
    DOCKING_FRAMES_SECONDARY_GLOBES_CLOSE_ENABLED("DockingFrames/SecondaryGlobesCloseEnabled", "true"),

    ENABLE_COMM_LOGGING("EnableCommLogging", "false"),
    ENABLE_EVENT_LOG("enableEventLog", "false"),
    ENABLE_EXTERNAL_SIMULATOR("EnableExternalSimulator", "false"),
    ENABLE_SERIAL_PORTS("EnableSerialPorts", "true"),

    /**
     * This key is used to enable the {@link java.sql.PreparedStatement#setFetchSize} hint when requesting a ResultSet.
     * Should be set to false in the presence of pgAudit.
     */
    ENABLE_SET_FETCH_SIZE("enableSetFetchSize", "true"),
    ENABLE_2D_KML_SHAPE_IMPORT_AS_STRUCTURE("enable2DKMLShapeImportAsStructure", "false"),

    /**
     * Icon size for the nodes in the Explorer Tree.  Default is 24x24 pixels.
     *
     * @since 5.4
     */
    EXPLORER_ICON_SIZE("ExplorerTree/iconSize", "24"),

    /**
     * Percent scaling for Explorer Tree text font.  Default is 110% (Changed from 130% in 5.7).
     *
     * @since 5.4
     */
    EXPLORER_TEXT_SIZE_SCALING_PERCENT("ExplorerTree/textSizeScalingPercent", "110"),

    GLOBE_NODE("Globe", null),

    /**
     * The number of clicks required to cause a "center on" action. Integer property, legal values are 1 or 2.
     */
    GLOBE_CENTER_GLOBE_ON_CLICK_COUNT("Globe/CenterGlobeOnClickCount", "1"),

    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    GLOBE_DEFAULT_ELEVATION_NUM_LEVELS("Globe/defaultElevationNumLevels", "15"),
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    GLOBE_DEFAULT_IMAGERY_NUM_LEVELS("Globe/defaultImageryNumLevels", "14"),

    /**
     * Whether mouse clicks cause a globe "center on" action. Boolean property.
     */
    GLOBE_DISABLE_CENTER_GLOBE_ON_CLICK("Globe/DisableCenterGlobeOnClick", "false"),

    /**
     * Whether keyboard input can navigate/alter the globe view. Boolean property.
     */
    GLOBE_DISABLE_KEYBOARD_GLOBE_NAVIGATION("Globe/DisableKeyboardGlobeNavigation", "false"),

    /**
     * Whether dragging the mouse can navigate/alter the globe view. Boolean property.
     */
    GLOBE_DISABLE_MOUSE_GLOBE_NAVIGATION("Globe/DisableMouseGlobeNavigation", "false"),

    /**
     * Whether multiple globe instances may be created. Default is currently false due to crashes and other bugs with
     * multiple globes on the TAK Map Engine.
     *
     * @since 3.4
     */
    GLOBE_ENABLE_MULTIPLE_GLOBES("Globe/EnableMultipleGlobes", "true"),

    /**
     * Whether the PositionOverlayLayer reports the mouse position: true - yes, false - shows view center position.
     *
     * @since 3.1
     */
    @PreferenceDescriptor(title = "Show Mouse Position",
            description = "Enable to show the current location of the mouse in the position overlay, disable to show the center location of the map")
    GLOBE_POSITION_OVERLAY_TRACKS_MOUSE("Globe/positionOverlayTracksMouse", "true"),

    /** @since 5.4 */
    @PreferenceDescriptor(title = "Radial Wheel Center on Click",
            description = "Center the map on the clicked item when showing the Radial Menu")
    GLOBE_RADIAL_CENTER_ON_CLICK("Globe/RadialCenterOnClick", "true"),

    GLOBE_SPLIT_SCALE("Globe/splitScale", "0.0"),
    GLOBE_USE_FRAME_BUFFER_OBJECTS("Globe/frameBuffering", "on"),

    /**
     * The node containing icons for go to location markers
     *
     * @since 2.38
     */
    GO_TO_LOCATION_ICONS_NODE("GoToLocationIcons", null),

    KNOWN_PROJECT_DIRECTORIES_NODE("KnownProjectDirectories", null),
    LOCAL_TIME_ZONE("localTimeZone", "UTC"),
    LAST_PROJECTS_DIR("LastProjectsDir", null),

    /**
     * Selected location provider and provider configurations (saved state).
     *
     * @since 3.0
     */
    SELECTED_LOCATION_PROVIDER("locationProvider/selectedProvider", NoLocationProvider.KEY),

    @PreferenceDescriptor(title = "Show Self Location Marker", description = "Show a marker for the current \"self location\"")
    SHOW_CURRENT_LOCATION_MARKER("Globe/showCurrentLocationMarker", "true"),

    /** @since 5.4 */
    @PreferenceDescriptor(title = "Show Self Location Widget", description = "Show the self location bubble and all self location information")
    SHOW_SELF_LOCATION_WIDGET("Globe/showSelfLocationWidget", "true"),

    LOCATION_PROVIDER_CONFIGURATIONS_NODE("locationProvider/configurations", null),

    /**
     * Set map object tooltips to always on (on), always off (off) or use default behavior (default).
     *
     * @since 2.38
     */
    @PreferenceDescriptor(title = "Show Marker Labels",
            description = "Controls if/when marker labels are displayed: 'on' = always, 'off' = never, 'default' = on mouse hover")
    MAP_OBJECT_TOOLTIPS("mapObjectTooltips", "default"),

    MAX_EVENT_LOG_TABLE_ROWS("maxEventLogTableRows", "1000"),
    MAX_SURFACE_IMAGE_DIMENSION("maxSurfaceImageDimension", "2048"),
    MAX_VISIBLE_SYSTEM_MESSAGES("maxVisibleSystemMessages", "200"),
    MIN_HUD_DATA_MONITOR_STATE("MinHUDDataMonitorState", State.Warning.name()),

    NAME("Name", "TAKX"),
    NODE_UUID("NODE_UUID", null),

    /** @since 5.2 */
    @PreferenceDescriptor(title = "Notification Duration",
            description = "The length of time to display notifications, in milliseconds. This value may be overridden by plugins.")
    NOTIFICATIONS_DURATION("Notifications/DurationMs", "4000"),

    /** @since 5.2 */
    @PreferenceDescriptor(title = "Notification Popups Enabled",
            description = "Enables notifications popups. If disabled, notifications will still be logged in the Notifications panel.")
    NOTIFICATIONS_POPUPS_ENABLED("Notifications/PopupsEnabled", "true"),

    /** @since 5.7 */
    @PreferenceDescriptor(title = "Auto-scroll the timeline",
            description = "When set, instead of stopping at the currently displayed end of the timeline, the timeline is scrolled," +
                    "and playback continues.")
    PLAYBACK_AUTO_SCROLL_AT_END("Playback/autoScrollAtEnd", "false"),

    /** @since 5.7 */
    @PreferenceDescriptor(title = "Show Live Markers in Playback",
            description = "When set, the 'live' icon will be displayed when in playback mode.  When unset, the 'live' icon will not be shown.")
    PLAYBACK_SHOW_LIVE_MARKERS("Playback/showLiveMarkers", "true"),

    /** @since 5.0 */
    PRODUCT_FLAVOR("productFlavor", FlavorConstants.CIV),

    RDM_BATCHING("RDMBatching", "false"),
    RECENT_PROJECTS_NODE("RecentProjects", null),

    /** @since 5.8 */
    @PreferenceDescriptor(title = "Checkpoint Name Prefix", description = "Prefix for all automatically generated checkpoint names")
    ROUTE_CHECKPOINT_PREFIX("route/checkpointPrefix", "CP"),
    /** @since 5.8 */
    @PreferenceDescriptor(title = "Driving Checkpoint Radius (meters)", description = "When within this distance, navigation will transition to the next waypoint")
    ROUTE_DRIVING_CHECKPOINT_RADIUS("route/drivingCheckpointRadius", "100"),
    /** @since 5.8 */
    @PreferenceDescriptor(title = "Flying Checkpoint Radius (meters)", description = "When within this distance, navigation will transition to the next waypoint")
    ROUTE_FLYING_CHECKPOINT_RADIUS("route/flyingCheckpointRadius", "1000"),
    /** @since 5.8 */
    @PreferenceDescriptor(title = "Swimming Checkpoint Radius (meters)", description = "When within this distance, navigation will transition to the next waypoint")
    ROUTE_SWIMMING_CHECKPOINT_RADIUS("route/swimmingCheckpointRadius", "10"),
    /** @since 5.8 */
    @PreferenceDescriptor(title = "Walking Checkpoint Radius (meters)", description = "When within this distance, navigation will transition to the next waypoint")
    ROUTE_WALKING_CHECKPOINT_RADIUS("route/walkingCheckpointRadius", "10"),
    /** @since 5.8 */
    @PreferenceDescriptor(title = "Watercraft Checkpoint Radius (meters)", description = "When within this distance, navigation will transition to the next waypoint")
    ROUTE_WATERCRAFT_CHECKPOINT_RADIUS("route/watercraftCheckpointRadius", "100"),
    /** @since 5.8 */
    @PreferenceDescriptor(title = "Elevation Profile Center on Seeker", description = "Center the globe on the elevation profile seeker point")
    ROUTE_ELEVATION_PROFILE_CENTER_ON_SEEKER("route/elevationProfileCenterOnSeeker", "true"),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "MIL-STD 2525 Variant",
            description = "Determines what MilStd2525 Variant will be used for new 2525 objects.")
    MIL_STD_2525_PREF("milstd2525preference", "Charlie"),

    /**
     * @since 3.4 (renamed from RIBBON_BAR_ACTIVE_CONFIGURATION)
     */
    TOOLBAR_ACTIVE_CONFIGURATION("ToolbarConfiguration/ActiveConfiguration", "Default Configuration"),

    /**
     * Preference for using CoT types in lieu of role icons
     * @since 5.6
     */
    @PreferenceDescriptor(title = "Use CoT type", description = "Use CoT type for icon instead of Team/Role")
    USE_COT_TYPE("useCotType", "false"),

    /**
     * @since 3.4 (renamed from RIBBON_BAR_DISPLAY_DEFAULT_CONFIGURATION)
     */
    TOOLBAR_DISPLAY_DEFAULT_CONFIGURATION("ToolbarConfiguration/DisplayDefaultConfiguration", "true"),

    @PreferenceDescriptor(title = "Reporting Strategy",
            description = "Specifies whether to report location at a constant rate or dynamically send more often when moving")
    SA_REPORTING_STRATEGY("SituationalAwarenessReporting/Strategy", ReportingStrategy.Dynamic.name()),

    @PreferenceDescriptor(title = "Send Location Over Network", description = "Send CoT event based on location gathering")
    SA_REPORTING_SEND_LOCATION("SituationalAwarenessReporting/SendLocation", "true"),

    @PreferenceDescriptor(title = "Report Location before obtaining location fix",
            description = "If true, report SA message with (lat=0,lon=0) before either GPS fix or manual placement of self-marker.")
    SA_REPORTING_REPORT_NO_LOCATION("SituationalAwarenessReporting/ReportNoLocation", "true"),

    // Reporting intervals for dynamic/constant SA reporting rates. All values in seconds
    @PreferenceDescriptor(title = "Dynamic Reporting Rate Stationary (Mesh)",
            description = "Reporting rate in seconds (e.g. once per 30 seconds when standing still) for mesh protocols (e.g. multicast). Note: other events may also trigger reporting (e.g. a callsign change)")
    SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_UNRELIABLE("SituationalAwarenessReporting/DynamicIntervalStationaryUnreliable", "30"),

    @PreferenceDescriptor(title = "Dynamic Reporting Rate Minimum (Mesh)",
            description = "Minimum reporting rate in seconds (e.g. once per 20 seconds when moving slow) for mesh protocols (e.g. multicast). Note: other events may also trigger reporting (e.g. a callsign change)")
    SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_UNRELIABLE("SituationalAwarenessReporting/DynamicIntervalMinimumUnreliable", "20"),

    @PreferenceDescriptor(title = "Dynamic Reporting Rate Maximum (Mesh)",
            description = "Maximum reporting rate in seconds (e.g. every other second when moving fast) for mesh protocols (e.g. multicast). Note: other events may also trigger reporting (e.g. a callsign change)")
    SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_UNRELIABLE("SituationalAwarenessReporting/DynamicIntervalMaximumUnreliable", "2"),

    @PreferenceDescriptor(title = "Dynamic Reporting Rate Stationary (Server)",
            description = "Reporting rate in seconds (e.g. once per 180 seconds when standing still) across TAK Server. Note: other events may also trigger reporting (e.g. a callsign change)")
    SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_RELIABLE("SituationalAwarenessReporting/DynamicIntervalStationaryReliable", "180"),

    @PreferenceDescriptor(title = "Dynamic Reporting Rate Minimum (Server)",
            description = "Minimum reporting rate in seconds (e.g. once per 20 seconds when moving slow) across TAK Server. Note: other events may also trigger reporting (e.g. a callsign change)")
    SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_RELIABLE("SituationalAwarenessReporting/DynamicIntervalMinimumReliable", "20"),

    @PreferenceDescriptor(title = "Dynamic Reporting Rate Maximum (Server)",
            description = "Maximum reporting rate in seconds (e.g. every other second when moving fast) across TAK Server. Note: other events may also trigger reporting (e.g. a callsign change)")
    SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_RELIABLE("SituationalAwarenessReporting/DynamicIntervalMaximumReliable", "2"),

    @PreferenceDescriptor(title = "Constant Reporting Rate (Mesh)",
            description = "Constant interval between reporting self-SA messages in seconds for mesh protocols (e.g. multicast)")
    SA_REPORTING_CONSTANT_INTERVAL_UNRELIABLE("SituationalAwarenessReporting/ConstantIntervalUnreliable", "3"),

    @PreferenceDescriptor(title = "Constant Reporting Rate (Server)",
            description = "Constant interval between reporting self-SA messages in seconds across TAK Server")
    SA_REPORTING_CONSTANT_INTERVAL_RELIABLE("SituationalAwarenessReporting/ConstantIntervalReliable", "15"),

    /**
     * Determines the CoT type, and therefore the icon representing this system in SA messages
     *
     * @since 3.1
     */
    @PreferenceDescriptor(title = "My Display Type", description = "The type of unit the world shall see this device as")
    SA_REPORTING_DISPLAY_TYPE("SituationalAwarenessReporting/DisplayType", DeviceDisplayType.GROUND_TROOP.name()),

    /**
     * Role the current user is playing, sent in SA message
     *
     * @since 3.1
     */
    @PreferenceDescriptor(title = "My Role", description = "Specifies the role of this device")
    SA_REPORTING_ROLE("SituationalAwarenessReporting/Role", "Team Member"),

    /**
     * Team name (color), sent in SA message
     *
     * @since 3.1
     */
    @PreferenceDescriptor(title = "My Team", description = "Select your squad")
    SA_REPORTING_TEAM("SituationalAwarenessReporting/Team", "Cyan"),

    @PreferenceDescriptor(title = "Save/Restore Window Settings", description = "Save and restore the window location and size upon restart")
    SAVE_WINDOW_SETTINGS("windowSettings/saveWindowSettings", "true"),

    /** @since 5.7 */
    @PreferenceDescriptor(title = "Self Marker Quick Move", description = "Allow the self marker to be moved by right-clicking it and selecting a new location")
    SELF_MARKER_QUICK_MOVE("selfMarkerQuickMove", "true"),

    /**
     * If true, the toolbar and side docked panels will not be visible to the user.
     *
     * @since 3.0 (renamed from RIBBONLESS_MODE in 3.4)
     */
    SIMPLIFIED_USER_INTERFACE("SimplifiedUserInterface", "false"),

    SKINS_NODE("Skins", null),

    /** @since 5.1 */
    @PreferenceDescriptor(title = "Stale Check Period", description = "Integer time period (in seconds) to wait between stale checks.")
    STALE_TIME_CHECK_PERIOD_SEC("StaleTime/StaleCheckPeriodSeconds", "5"),

    /** @since 5.1 */
    @PreferenceDescriptor(title = "Expire all marker types",
            description = "Enable to delete all non-archived types when they become stale")
    STALE_TIME_EXPIRE_EVERYTHING("StaleTime/ExpireEverything", "true"),

    /** @since 5.1 */
    @PreferenceDescriptor(title = "Remove unknown markers",
            description = "Enable to remove CoT markers with unknown affiliations when they become stale")
    STALE_TIME_EXPIRE_UNKNOWNS("StaleTime/ExpireUnknowns", "true"),

    /** @since 5.1 */
    @PreferenceDescriptor(title = "Stale marker cleanup time",
            description = "Length of time (in minutes) a CoT maker will remain on the map after becoming stale")
    STALE_TIME_MINUTES_UNTIL_DELETE_STALE("StaleTime/MinutesUntilDeleteStale", "5"),

    /**
     * Email address to display in the issue reporting dialog.
     */
    SUPPORT_EMAIL("supportEmail", "takx@tak.gov"),

    /**
     * Set true to suppress the inclusion of the program version number from the main window title bar.
     */
    SUPPRESS_PROGRAM_VERSION("suppressProgramVersion", "false"),

    /**
     * Set true to suppress the inclusion of the current project name from the main window title bar.
     *
     * @since 3.1
     */
    SUPPRESS_PROJECT_NAME("suppressProjectName", "false"),

    /**
     * Set {@code true} to provide a link to show exception stack traces in the system message view; {@code false} to not.
     *
     * @since 5.3
     */
    SYSTEM_MESSAGE_SHOW_EXCEPTION_STACK("systemMessageShowExceptionStack", "true"),

    /** @since 5.6 */
    @PreferenceDescriptor(title = "Show \"System Messages\" notifications tab",
            description = "Controls visibility of the \"System Messages\" notifications (count) tab on the bottom toolbar")
    SYSTEM_MESSAGE_COUNT_VISIBLE("systemMessageNotificationsVisible", "true"),

    /**
     * Node to hold arbitrary preferences consistent with ATAK/WTK.  There is no hierarchy below this node.
     * The keys are typically dot separated names, but we will infer no semantics from the key structure.
     * The key names are defined by other TAK products, and a subset of those are supported by TAKX.
     *
     * @since 5.3
     */
    TAK_PREFERENCES_NODE("TAK_preferences", null),

    TIME_SERVER_HOST("TimeServerHost", "time.nist.gov"),
    TOOLTIP_FONT_SIZE("tooltipFontSize", "-1"),
    TRACK_DETAIL_LEVELS_NODE("TrackDetailLevels", null),
    TRACK_PRIMARY_RENDERING_MODE("TrackRendering/primaryRenderingMode", "TopDown"),

    @PreferenceDescriptor(title = "Use Maximum Window Bounds", description = "If checked, make the application take the full screen size upon startup")
    USE_MAX_WINDOW_BOUNDS("useMaximumWindowBounds", "false"),
    USE_PROJECT_EXPLORER("useProjectExplorer", "false"),
    VIEW_LIMITS_NODE("viewLimits", null),
    WINDOW_SETTINGS_NODE("windowSettings", null),

    /**
     * Comma separated list of servers to exclude when calculating network availability.
     * Example usage: {@code "data.worldwind.arc.nasa.gov,worldwind27.arc.nasa.gov"}.
     * <p/>
     * At present, the default value includes a known unresponsive NASA server (handling elevation models).
     *
     * @since 3.4 (cherry-picked from 2.39.1)
     */
    @PreferenceDescriptor(title = "Excluded Unresponsive Servers",
            description = "Comma separated list of servers to exclude when calculating network availability")
    EXCLUDED_UNRESPONSIVE_SERVERS("excludedUnresponsiveServers", "data.worldwind.arc.nasa.gov"),

    NONE("NONE", "NONE"); // Never use this

    private final String key;
    private final String defaultValue;

    private IProgramPreferenceManager preferenceManager;
    private List<IPreferenceEntryChangeListener> changeListeners;

    @Override
    public String getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public String getKey()
    {
        return key;
    }

    PreferenceEntry(String key, String defaultValue)
    {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public IProgramPreferenceManager getPreferenceManager()
    {
        return preferenceManager != null
                ? preferenceManager
                : (preferenceManager = new InjectableContainerSupport().getBean(IProgramPreferenceManager.class));
    }

    @Override
    public List<IPreferenceEntryChangeListener> getChangeListeners()
    {
        return changeListeners != null
                ? changeListeners
                : (changeListeners = new CopyOnWriteArrayList<>());
    }
}
