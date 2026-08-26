package gov.takx.core.view.gui.takserver;

import gov.tak.api.comms.takserver.mission.data.ExternalMissionData;
import gov.tak.api.comms.takserver.mission.data.Location;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import gov.tak.api.comms.takserver.mission.data.MissionChangeType;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.data.UidDetails;
import gov.tak.api.contact.IContactNameResolver;
import gov.tak.api.icons.ICotIconResolver;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.media.MediaType;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.SizeFormatter;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.view.gui.takserver.timeline.MissionChangeOrLogEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;

import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;

/**
 * Provides a common way to extract and resolve {@link MissionChange} event info.
 *
 * @since 5.0
 */
@ApplicationScoped
public class MissionChangeInfoResolver
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ImageIcon IMAGE_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "imagery_overlay.png");
    private static final ImageIcon VIDEO_ICON = UIResources.getToolbarIcon(ResourceDirectory.PLATFORM_ICONS, "video.png");
    private static final ImageIcon AUDIO_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "audio.png");
    private static final ImageIcon CREATE_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "add.png");
    private static final ImageIcon LOG_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "file.png");
    private static final ImageIcon UNKNOWN_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "unknown.png");
    private static final Map<MediaType, ImageIcon> MEDIA_ICONS;

    static
    {
        MEDIA_ICONS = new EnumMap<>(MediaType.class);
        MEDIA_ICONS.put(MediaType.Image, IMAGE_ICON);
        MEDIA_ICONS.put(MediaType.Video, VIDEO_ICON);
        MEDIA_ICONS.put(MediaType.Audio, AUDIO_ICON);
    }

    private final IContactNameResolver contactNameResolver;
    private final ICotIconResolver cotIconResolver;
    private final IMapObjectFinder mapObjectFinder;

    @Inject
    public MissionChangeInfoResolver(IContactNameResolver contactNameResolver, ICotIconResolver cotIconResolver,
                                     IMapObjectFinder mapObjectFinder)
    {
        this.contactNameResolver = contactNameResolver;
        this.cotIconResolver = cotIconResolver;
        this.mapObjectFinder = mapObjectFinder;
    }

    /**
     * Returns the icon that should be associated with the event or the {@link #UNKNOWN_ICON} if the icon cannot be 
     * determined.
     *
     * @param change The change or log event
     * @return The event icon or the {@link #UNKNOWN_ICON} if the icon cannot be determined
     */
    @Nonnull
    public ImageIcon getChangeIcon(@Nonnull MissionChangeOrLogEntry change)
    {
        if (change.logEntry() != null) return LOG_ICON;

        //noinspection DataFlowIssue
        return getChangeIcon(change.missionChange());
    }

    /**
     * Returns the icon that should be associated with the event or the {@link #UNKNOWN_ICON} if the icon cannot be 
     * determined.
     * 
     * @param change The change event
     * @return The event icon or the {@link #UNKNOWN_ICON} if the icon cannot be determined
     */
    @Nonnull
    public ImageIcon getChangeIcon(@Nonnull MissionChange change)
    {
        //noinspection DataFlowIssue
        return getChangeIcon(change, true);
    }

    /**
     * Returns the icon that should be associated with the event.
     * <p>
     * If the icon cannot be determined and {@code fallbackOnDefaultIcon} is {@code true}, the {@link #UNKNOWN_ICON}
     * will be returned; if {@code false}, {@code null} will be returned.
     *
     * @param change The change event
     * @param fallbackOnDefaultIcon Pass {@code true} if the {@link #UNKNOWN_ICON} should be returned if the icon cannot 
     *                              be resolved or {@code false} if {@code null} should be returned.
     * @return The icon that should be associated with the event, or the {@link #UNKNOWN_ICON}/{@code null} if the icon 
     * cannot be determined.
     */
    @Nullable
    public ImageIcon getChangeIcon(@Nonnull MissionChange change, boolean fallbackOnDefaultIcon)
    {
        final MissionChangeType changeType = change.type();
        if (changeType == MissionChangeType.CREATE_MISSION || changeType == MissionChangeType.CREATE_DATA_FEED)
        {
            return CREATE_ICON;
        }

        final UidDetails details = change.details();
        if (details != null)
        {
            final String contentUid = change.contentUid();
            final IMapObject mapObject = mapObjectFinder.findMapObjectByUid(contentUid);
            final BufferedImage image = mapObject != null ? mapObject.getIconImage()
                    : cotIconResolver.getIcon(ACTION_BUTTON_IMAGE_SIZE.width, details.type());

            return ImageUtils.getScaledIcon(image, ACTION_BUTTON_IMAGE_SIZE.width, ACTION_BUTTON_IMAGE_SIZE.height);
        } else if (change.contentResource() != null)
        {
            // Change holds a file
            String fileName = change.contentResource().safeFileName();
            String fileExtension = FileUtils.getExtension(fileName);
            MediaType mediaType = MediaType.getMediaTypeForExtension(fileExtension);
            return MEDIA_ICONS.getOrDefault(mediaType, fallbackOnDefaultIcon ? UNKNOWN_ICON : null);
        } else if (change.logEntry() != null)
        {
            return LOG_ICON;
        } else if (change.contentUid() != null)
        {
            // Some entries do not have the 'details' attribute, but denote a map item in the 'content-uid' value.
            // We do not have the luxury of knowing what type the item is in this case.
            final String contentUid = change.contentUid();
            final IMapObject mapObject = mapObjectFinder.findMapObjectByUid(contentUid);
            final BufferedImage image = mapObject != null ? mapObject.getIconImage() : null;

            if (image != null) return ImageUtils.getScaledIcon(image, ACTION_BUTTON_IMAGE_SIZE.width, ACTION_BUTTON_IMAGE_SIZE.height);
        }

        logger.debug("The icon could not be determined for mission '{}', change type '{}'", change.missionName(), change.type());

        return fallbackOnDefaultIcon ? UNKNOWN_ICON : null;
    }

    /**
     * @param entries Combined change entries.
     * @param entry Change/user log entry.
     * @param index Index of changelog entry.
     *
     * @return Message for changelog entry.
     */
    @Nonnull
    public String getChangeMessage(@Nonnull List<MissionChangeOrLogEntry> entries, @Nonnull MissionChangeOrLogEntry entry, int index)
    {
        // Check for user-log content.
        LogEntry log = entry.logEntry();
        if (log != null) return log.content();

        // Handle changelog content.
        MissionChange change = entry.missionChange();
        MissionChangeType missionChangeType = change.type();
        String changeType = switch (missionChangeType)
        {
            case CREATE_MISSION, CREATE_DATA_FEED -> "created " + change.missionName();
            case DELETE_MISSION, DELETE_DATA_FEED -> "deleted " + change.missionName();
            case ADD_CONTENT -> "added " + getEventName(change);
            case REMOVE_CONTENT -> "removed " + getEventName(change);
        };
        String callsign = getCallSign(change.creatorUid());

        // Map items can be modified, but those get represented as 'addition' types, which is misleading in the UI.
        // To determine if this item is actually an update to an existing map item, we check previous log entries
        // for matching map items by their uid, and see if the most recent change type for the item was also an 'addition'.
        // If it is a removal, then this is re-adding it, and the type name can be kept.
        if (missionChangeType == MissionChangeType.ADD_CONTENT && change.details() != null)
        {
            int max = Math.min(entries.size() - 1, index - 1);
            for (int i = max; i > 0; i--)
            {
                MissionChangeOrLogEntry previousEntry = entries.get(i);
                MissionChange previousChange = previousEntry.missionChange();
                if (previousChange != null && previousChange.details() != null && Objects.equals(change.contentUid(), previousChange.contentUid()))
                {
                    MissionChangeType type = previousChange.type();
                    if (type == MissionChangeType.ADD_CONTENT)
                    {
                        changeType = "updated " + getEventName(change);
                        break;
                    } else if (type == MissionChangeType.REMOVE_CONTENT)
                    {
                        break;
                    }
                }
            }
        }

        return callsign + " " + changeType;
    }

    /**
     * @param mission The mission
     * @param entry The mission change event
     * @return The change event details message
     */
    @Nonnull
    public String getDetailsMessage(@Nonnull Mission mission, @Nonnull MissionChangeOrLogEntry entry)
    {
        // Check for user-log content. No extra details needed.
        if (entry.logEntry() != null) return "";

        // Handle as changelog entry.
        MissionChange change = entry.missionChange();
        if (change == null) return "";

        // Handle external data
        final ExternalMissionData data = change.externalData();
        if (data != null) return data.name();

        // Handle resources
        final Resource resource = change.contentResource();
        if (resource != null)
        {
            final String name = resource.name();
            final String extension = FileUtils.getExtension(name);

            // Use the file extension if the resource is a file with an extension; otherwise, use the entire resource name
            return (extension != null ? extension.toUpperCase() + " File" : name)
                    // Add the resource size
                    + " " + SizeFormatter.formatEstimate(resource.size());
        }

        // Handle other change types
        return switch (change.type())
        {
            case CREATE_MISSION -> "This feed was created to facilitate the sharing of content.";
            // If the content's timestamp is newer than this change event, show a message indicating that it's changed
            // instead of showing the content name
            case ADD_CONTENT -> isNewestContentEntry(mission, change)
                    ? getContentLocation(change)
                    : "Note: This item has been updated more recently.";
            case REMOVE_CONTENT -> "This content was removed from the feed."; // todo: add undo option?
            case DELETE_MISSION, CREATE_DATA_FEED, DELETE_DATA_FEED -> change.type().name(); // todo: are these ever used
        };
    }

    /**
     * @param missionChange The change event
     * @return Returns the location of the content associated with a change event or an empty String if a location
     * cannot be determined.
     */
    public String getContentLocation(@Nonnull MissionChange missionChange)
    {
        final UidDetails uidDetails = missionChange.details();

        if (uidDetails != null)
        {
            final Location location = uidDetails.location();

            // WinTAK resolves the position to the address or city/country; we'll just show the coordinates for now
            if (location != null)
            {
                return CoordinateSystem
                        .getLocationString(location.toPosition(), UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class));
            }
        }

        logger.debug("Cannot determine location of mission change within '{}'", missionChange.missionName());

        return "";
    }

    /**
     * @param uid The user's UID
     * @return The user's call sign, or "User" if it cannot be determined
     */
    private String getCallSign(@Nonnull String uid)
    {
        final String callSign = contactNameResolver.resolveName(uid);

        // If the UID is not mapped, WinTAK displays "User" instead of the UID
        return callSign == null || callSign.isEmpty() || callSign.equalsIgnoreCase(uid) ? "User" : callSign;
    }

    /**
     * Returns the relevant event name base on the data included in the change event
     *
     * @param missionChange The change event
     * @return The event name
     */
    private static String getEventName(@Nonnull MissionChange missionChange)
    {
        final ExternalMissionData data = missionChange.externalData();
        if (data != null) return data.name();

        final Resource resource = missionChange.contentResource();
        if (resource != null) return resource.safeFileName();

        final UidDetails details = missionChange.details();
        if (details != null) return details.callsign();

        final LogEntry log = missionChange.logEntry();
        if (log != null) return log.content();

        logger.warn("Unable to determine timeline item name with for entry with timestamp {} in mission '{}'",
                missionChange.timestamp(), missionChange.missionName());

        return "(unknown)";
    }

    /**
     * Returns a boolean indicating if a change event is the most recent one associated with its content.
     *
     * @param mission The mission
     * @param missionChange The mission change event
     * @return {@code true} if the change event is the most recent change associated with its associated content,
     * {@code false} otherwise.
     */
    private static boolean isNewestContentEntry(@Nonnull Mission mission, @Nonnull MissionChange missionChange)
    {
        final String contentUid = missionChange.contentUid();

        // Look for mission content with a matching UID
        final MissionAdd.TString tstring = mission.uids().stream()
                .filter(uid -> uid.data().equals(contentUid))
                .findFirst().orElse(null);

        // Compare the timestamps
        return tstring != null && missionChange.timestamp().equals(tstring.timestamp());
    }
}