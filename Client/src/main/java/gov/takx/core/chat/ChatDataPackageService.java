package gov.takx.core.chat;

import com.atakmap.commoncommo.Contact;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatService;
import gov.tak.api.comms.commo.IContactManager;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileTypeManager;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.lang.ErrorThrowingFunction;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static gov.takx.api.filesystem.VfsConstants.FILES_DIR;

/**
 * Offers services required to send data packages directly to other {@link IContact IContacts}.
 *
 * @since 5.2
 */
@ApplicationScoped
public class ChatDataPackageService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ATTACHMENTS_DIR = "ChatAttachments";
    private static final String TEMP_PACKAGE_NAME = "chat-transfer";
    // ATAK chat DP file names can take the form "chat-transfer.zip" or "chat-transfer(1).zip"
    private static final String TEMP_FILE_NAME_REGEX = TEMP_PACKAGE_NAME + "\\s?(?:\\(\\d+\\))?\\.zip";
    private static final Predicate<String> IS_TEMP_FILE_NAME = Pattern.compile(ChatDataPackageService.TEMP_FILE_NAME_REGEX).asMatchPredicate();

    private final IExtendedDataPackageManager dataPackageManager;
    private final IContactManager contactManager;
    private final IVirtualFileSystemManager vfsManager;
    private final INotificationService notificationService;
    private final IChatService chatService;
    private final String clientUid;

    @Inject
    ChatDataPackageService(IExtendedDataPackageManager dataPackageManager, IContactManager contactManager,
                           @VFSManagerScope(VFSScope.Project) IVirtualFileSystemManager vfsManager,
                           INotificationService notificationService, IChatService chatService, @NodeUUID UUID nodeUuid)
    {
        this.dataPackageManager = dataPackageManager;
        this.contactManager = contactManager;
        this.vfsManager = vfsManager;
        this.notificationService = notificationService;
        this.chatService = chatService;
        clientUid = nodeUuid.toString();
    }

    /**
     * @param packageFile The package file to check.
     * @return {@code true} if the file is a temp data package used in chats.
     * @since 5.7
     */
    public boolean isChatPackage(@Nonnull File packageFile)
    {
        return isChatPackage(packageFile.getName());
    }

    /**
     * @param packageFile The package file to check.
     * @return {@code true} if the file is a temp data package used in chats.
     * @since 5.7
     */
    public boolean isChatPackage(@Nonnull Path packageFile)
    {
        return isChatPackage(packageFile.getFileName().toString());
    }

    /**
     * @param packageFileName The package file name to check.
     * @return {@code true} if the file is a temp data package used in chats.
     * @since 5.7
     */
    public boolean isChatPackage(@Nonnull String packageFileName)
    {
        // We can only check by looking at the name...
        int slash = packageFileName.indexOf('/');
        if (slash > 0) packageFileName = packageFileName.substring(slash + 1);
        return IS_TEMP_FILE_NAME.test(packageFileName);
    }

    /**
     * Creates and sends a data package to a {@link IContact Contact}.
     *
     * @param contact The recipient {@link IContact}
     * @param addFilesToDataPackageFunction Function that accepts a data package file, adds files to it and returns a
     *                                      list of {@link ChatLink ChatLinks} representing the files added
     */
    public void sendPackage(@Nonnull IContact contact,
                            @Nonnull ErrorThrowingFunction<File, List<ChatLink>, Exception> addFilesToDataPackageFunction)
    {
        final List<Contact> commoContacts = getCommoContacts(contact);
        final String contactName = contact.getName();

        if (commoContacts.isEmpty())
        {
            logger.error("Could not find commo contact for contact {}", contactName);
            showSendErrorNotification(contactName, "Unable to retrieve info for contact " + contactName + ".");
            return;
        }

        try
        {
            final var packageFile = dataPackageManager.createNewTempDataPackage(TEMP_PACKAGE_NAME);
            final var chatLinks = addFilesToDataPackageFunction.apply(packageFile);

            // Send the package
            dataPackageManager.sendPackageToContacts(packageFile, commoContacts);

            // Send the associated chat message
            chatService.sendMessage(buildChatMessage(contact, chatLinks), null);
        } catch (Exception ex)
        {
            logger.error("Failed to send data package to send to user {}", contact.getName(), ex);
            showSendErrorNotification(contactName, "Package send failure.");
        }
    }

    /**
     * @param name Attachment name.
     * @return Local attachment by name if found.
     * @since 5.7
     */
    @Nullable
    public File getAttachmentByName(@Nonnull String name)
    {
        // TODO: The only other discriminator we can pull into here is the timestamp of the item.
        //  So we could possibly deconflict duplicate named items by whatever attachment has the closest associated time.
        //  That's sadly the best I can think of with what we can cram into the ChatLink record... Not a whole lot of inputs to work with.
        //  - But if we do go with the timestamp, we need a way to have multiple of the same named file.
        //    We could differentiate by subfolders but that means more annoying IO work.
        //  - Or we can keep the existing logic and just overwrite files by the same name.
        File attachment = new File(getAttachmentsDirectory(), name);
        return attachment.exists() ? attachment : null;
    }

    /**
     * @return Local chat file attachments folder.
     * @since 5.7
     */
    @Nonnull
    public File getAttachmentsDirectory()
    {
        return vfsManager.getRoot(new FilePath(FILES_DIR, ATTACHMENTS_DIR).toString());
    }

    /**
     * @param contactName The sender's contact name.
     * @param packageFile The package sent by the contact.
     * @since 5.7
     */
    public void receivePackage(@Nonnull String contactName, @Nonnull File packageFile)
    {
        File root = vfsManager.getRoot(new FilePath(FILES_DIR, ATTACHMENTS_DIR).toString());
        if (!root.exists()) root.mkdirs();

        try (var zis = new ZipInputStream(new FileInputStream(packageFile)))
        {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null)
            {
                if (zipEntry.isDirectory()) continue;
                String zipEntryPath = zipEntry.getName();
                int slash = zipEntryPath.lastIndexOf('/');
                if (slash <= 0) continue;
                String zipEntryName = zipEntryPath.substring(slash + 1);
                String ext = Objects.requireNonNullElse(FileUtils.getExtension(zipEntryName), "").toLowerCase();
                if (FileTypeManager.getTypeFromExtension(ext) == null) continue;
                File attachmentDest = new File(root, zipEntryName);
                try (var fos = new FileOutputStream(attachmentDest))
                {
                    zis.transferTo(fos);
                }
            }
            zis.closeEntry();
        } catch (Throwable t)
        {
            logger.error("Failed to extract received package '{}' from '{}'", packageFile.getName(), contactName, t);
        }
    }

    /**
     * @param contactName The recipient contact name.
     * @param extraMessage Details to include in addition to the standard error message.
     */
    private void showSendErrorNotification(@Nonnull String contactName, @Nonnull String extraMessage)
    {
        notificationService.show(
                Notification.builder("Failed to send data package to send to contact " + contactName + ". " + extraMessage)
                        .setType(Notification.Type.Error)
                        .build());
    }

    /**
     * Converts an {@link IContact} to one or more commo {@link Contact Contacts}.
     *
     * @param contact The individual or group contact
     * @return The commo contacts associated with the {@link IContact}
     */
    @Nonnull
    private List<Contact> getCommoContacts(@Nonnull IContact contact)
    {
        final List<IContact> contacts = contact instanceof IGroupContact groupContact
                // If the contact is a group, retrieve all of its non-group members
                ? groupContact.getGroupMembers().stream().filter(c -> c instanceof IIndividualContact).toList()
                : List.of(contact);

        // Retrieve and return the commo contacts
        return contacts.stream()
                .filter(c -> !c.getUid().equals(clientUid))
                .map(c -> contactManager.findContactByUid(c.getUid()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Constructs a standard chat data package chat message.
     *
     * @param contact The recipient contact
     * @param chatLinks The data package content links that must be included in the message
     * @return A new chat message
     */
    @Nonnull
    private IChatMessage buildChatMessage(@Nonnull IContact contact, @Nonnull List<ChatLink> chatLinks)
    {
        final int itemCount = chatLinks.size();

        return new ChatMessageBuilder(
                clientUid,
                PreferenceEntry.CALL_SIGN.getStringValue(),
                "sent a datapackage with " + itemCount + (itemCount == 1 ? " item" : " items"),
                System.currentTimeMillis(),
                contact.getUid())
                .withLinks(chatLinks)
                .build();
    }
}
