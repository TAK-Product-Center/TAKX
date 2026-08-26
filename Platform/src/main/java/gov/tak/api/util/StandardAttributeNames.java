package gov.tak.api.util;

import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.MapItem;
import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.IMapObject;

import java.io.File;

/**
 * Constants defining commonly used attribute names for use in AttributeSet.
 * <p>
 * Note: constants defining XML elements and attributes for CoT details belong in {@link DetailConstants}.
 *
 * @see AttributeSet
 * @since 3.0
 */
public class StandardAttributeNames
{
    public static final String ACCESS = CotEvent.ACCESS;
    /**
     * If {@code false}, then the marker icon will not be adapted based on CoT type, team, etc.
     *
     * @since 5.1
     */
    public static final String ADAPT_MARKER_ICON = "adapt_marker_icon";
    public static final String ARCHIVE = DetailConstants.ARCHIVE;
    public static final String ATAK_ROLE_TYPE = "atakRoleType";

    /**
     * Array of file names "attached" to the MapObject. The referenced files are stored in the
     * project under the {@code Attachments} VFS in a per-MapObject format.
     * <p/>
     * Direct access use of this attribute is heavily discouraged. Please use one of the below methods instead.
     *
     * @see MapItem#getAttachments()
     * @see MapItem#addAttachment(String, File)
     * @see MapItem#addAttachment(String, java.io.InputStream)
     * @see MapItem#deleteAttachments(File...)
     * @see MapItem#deleteAttachments(String...)
     * @since 5.2
     */
    @Internal
    public static final String ATTACHED_FILES = "attachedFiles";
    /**
     * {@code long} value. The number of milliseconds a CoT marker must be inactive before it is automatically set to be
     * stale.
     *
     * @since 5.1
     **/
    public static final String AUTO_STALE_DURATION_MS = "autoStaleDuration";
    /** @since 5.0 */
    public static final String AXIS_ALIGNED_BOUNDING_BOX = "aabb";
    public static final String BATTERY = DetailConstants.ATTR_BATTERY;
    public static final String BEARING = "bearing";

    /**
     * {@code boolean} value, {@code true} indicates breadcrumbs are being recorded, {@code false} if not.
     * @since 5.6
     */
    public static final String BREADCRUMBS_ENABLED = "breadcrumbs_enabled";

    /** @since 5.3 */
    public static final String CAVEAT = CotEvent.CAVEAT;
    public static final String CALLSIGN = DetailConstants.ATTR_CALLSIGN;
    public static final String COLOR = DetailConstants.COLOR;
    /**
     * If set, overrides the default stale time when a marker is converted to a CoT event.
     *
     * @since 5.1
     */
    public static final String COT_DEFAULT_STALE_SECONDS = "cotDefaultStaleSeconds";
    public static final String COT_EVENT_XML = "cotEventXml"; // The XML of the latest received CoT event
    public static final String COT_TYPE = CotEvent.TYPE; // CoT Type string

    /**
     * {@code String} value. Defines the users custom role (ex: Sideways Observer, Sergeant Major, etc.) for use with the kernel GroupDetailHandler.
     * If present (with {@link #CUSTOM_ROLE_ABBREVIATION}), the kernel will use this as the role for CoT messaging and display.
     * @since 5.6
     */
    public static final String CUSTOM_ROLE = "customRole";
    /**
     * {@code String} value. Defines the users custom role abbreviation (ex: SO, SM, etc.) for use with the kernel GroupDetailHandler.
     * If present (with {@link #CUSTOM_ROLE}), the kernel will use this as the role for CoT messaging and display.
     * @since 5.6
     */
    public static final String CUSTOM_ROLE_ABBREVIATION = "customRoleAbbreviation";

    /**
     * Metadata key for a {@code boolean} value that will be {@code true} if the marker is currently being dragged.
     *
     * @since 5.3
     **/
    public static final String DRAGGING = "Dragging";
    public static final String EDITABLE = "editable";

    /**
     * Indicates an emergency alert.
     * @since 5.7
     */
    public static final String EMERGENCY = "emergency";

    /**
     * Message associated with an emergency alert.
     * @since 5.7
     */
    public static final String EMERGENCY_MESSAGE = "emergency_message";

    public static final String ENTRY = "entry";

    public static final String EST_COURSE = "est.course";
    public static final String EST_DISTANCE = "est.dist";
    public static final String EST_SPEED = "est.speed";
    public static final String EST_TIME = "est.time";
    /**
     * {@code boolean} value. If set to {@code true}, the CoT marker will immediately be updated to be {@link #STALE}.
     * After the marker has been marked as stale, this flag will be set back to {@code false}.
     *
     * @since 5.1
     */
    public static final String FORCE_STALE = "forceStale";
    public static final String FROM = "from";
    public static final String HOW = "how";
    public static final String ICONSET_PATH = "IconsetPath"; // 3-tuple formatted as: <iconsetpath UID>/<group>/<filename>
    public static final String INCLINATION = "inclination"; // Stores the arrow inclination value so it can be exported
    public static final String IMPORTING = "importing";
    /**
     * Tracks the number of pending attachments to a marker after data-sync downloads it from a feed.
     *
     * @since 5.3
     */
    public static final String DATA_SYNC_PENDING_ATTACHMENTS = "pending-file-attachments";
    public static final String INITIAL_BEARING = DetailConstants.INITIAL_BEARING;
    public static final String LAST_UPDATE_TIME = "lastUpdateTime";
    public static final String MOVABLE = "movable";
    /**
     * MIL-STD symbology code
     *
     * @since 5.1
     */
    public static final String MIL_SYM = "milsym"; // TODO ATAK-20119: Investigate our usage of this (also this does not match DetailConstants.MILSYM which has "__" as a prefix)
    /**
     * X coordinate for centering non-symmetric MIL-STD icons.
     *
     * @since 5.5.1
     */
    public static final String MIL_SYM_CENTER_X = "milsym-center-x";
    /**
     * Y coordinate for centering non-symmetric MIL-STD icons.
     *
     * @since 5.5.1
     */
    public static final String MIL_SYM_CENTER_Y = "milsym-center-y";
    /**
     * Flag indicating the current marker is currently having its 2525 icon adapted.
     * After the icon is applied this flag is removed. This is effectively a backup to
     * {@link #ADAPT_MARKER_ICON} since that attribute is temporarily {@code false}
     * during adaption.
     *
     * @since 5.6
     */
    public static final String MIL_SYM_ADAPTING = "milsym-adapt-in-progress";
    public static final String NAME = "name";
    public static final String NAVIGATING = "navigating";
    public static final String NEVER_COT = "nevercot"; // Indicates item should never be turned into CoT
    public static final String OFFSCREEN_INTEREST = "offscreen_interest";
    public static final String OPEX = CotEvent.OPEX;
    /** @since 5.1 */
    public static final String PARENT_CALLSIGN = DetailConstants.ATTR_PARENT_CALLSIGN;
    /** @since 5.1 */
    public static final String PARENT_TYPE = "parent_type";
    /** @since 5.1 */
    public static final String PARENT_UID = "parent_uid";
    /** @since 5.1 */
    public static final String PRODUCTION_TIME = DetailConstants.ATTR_PRODUCTION_TIME;
    public static final String QOS = CotEvent.QOS;
    /**
     * Boolean value, {@code true} indicates the item is a range&amp;bearing object
     * 
     * @since 5.5
     */
    public static final String RANGE_BEARING = "RANGE_BEARING";
    public static final String READINESS = "readiness";
    public static final String READY_TO_NAV = "ready_to_nav";
    /** @since 5.3 */
    public static final String RELEASABLE_TO = CotEvent.RELEASABLE_TO;
    public static final String REMARKS = DetailConstants.REMARKS; // MapItem remarks
    /**
     * Does this item allow remote deletion?
     *
     * @since 5.0
     */
    public static final String REMOTE_DELETE = "remoteDelete";
    public static final String REMOVABLE = "removable";
    public static final String SPEED = DetailConstants.ATTR_SPEED;
    /**
     * {@code boolean} value. If {@code true},  the CoT marker has timed out due to inactivity.
     *
     * @since 5.1
     */
    public static final String STALE = "stale";
    /**
     * {@code long} value. The time at which a CoT marker was marked as {@link #STALE}. If a marker isn't stale, this
     * attribute will not be present.
     *
     * @since 5.1
     */
    public static final String STALE_TIME = "staleTime";
    /**
     * Attribute for tracking assigned role/color of a marker so that we can detect a change
     * @since 5.6
     */
    public static final String LAST_TEAM_ICON_AND_COLOR = "last team icon/color";
    public static final String TEAM = "team";
    public static final String TEAM_ICON_ASSIGNED = "team_icon_assigned";
    public static final String TITLE = "title";

    /**
     * {@code String} value. Controls when a Map Object's tooltip is displayed on the globe.
     * @see IMapObject.TooltipVisibility
     * @since 5.6
     */
    public static final String TOOLTIP_VISIBILITY = "tooltip_visibility";

    public static final String UID = CotEvent.UID;
    /**
     * {@code boolean} value. If {@code true}, the relevant Marker will display as the CoT icon rather than the "skittle"
     * team icon. This data will be sent with CoT messages, if it exists.
     * @since 5.6
     */
    public static final String USE_COT_TYPE_ICON = "displayAsSymbologyGraphic";
    /**
     * {@code Boolean} value. If {@code true}, this MO is a viewshed. 
     * @since 5.7
     */
    public static final String VIEWSHED = "viewshed";
    public static final String VISIBLE = "visible";

    private StandardAttributeNames()
    {
        /* no instances */
    }
}
