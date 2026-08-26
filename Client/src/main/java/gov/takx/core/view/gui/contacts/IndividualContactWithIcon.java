package gov.takx.core.view.gui.contacts;

import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IndividualContactAttributeNames;
import gov.tak.api.contact.IndividualContactState;
import gov.tak.api.contact.IndividualContactUtils;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

/**
 * Wraps an {@link gov.tak.api.contact.IIndividualContact} and manages the contact's icon.
 *
 * @since 3.4.1 (renamed from IndividualContact in 5.1)
 */
class IndividualContactWithIcon
{
    /**
     * Use a standard image size so the image can be cached; see MapObject.getSizedIcon()
     */
    public static final int CONTACT_ICON_SIZE = ImageUtils.DEFAULT_ICON_WIDTH;

    // Status image size, relative to contact icon size
    private static final int STATUS_IMAGE_SIZE = CONTACT_ICON_SIZE / 3;

    // Bottom right corner of main image, ~10% padding
    private static final int STATUS_IMAGE_OFFSET = CONTACT_ICON_SIZE - STATUS_IMAGE_SIZE - (int) (CONTACT_ICON_SIZE * 0.1);

    private static final BufferedImage INACTIVE = GraphicsUtils.createCircle(STATUS_IMAGE_SIZE, 255, Color.gray);
    private static final BufferedImage ACTIVE = GraphicsUtils.createCircle(STATUS_IMAGE_SIZE, 255, Color.green.brighter());

    private IIndividualContact contact;
    private BufferedImage contactImage = UIResources.getSizedImage(ResourceDirectory.PLATFORM_ICONS, "user.png",
            new Dimension(CONTACT_ICON_SIZE, CONTACT_ICON_SIZE), false);
    private Icon contactAndStatusIcon;

    /**
     * @param contact The contact
     */
    IndividualContactWithIcon(@Nonnull IIndividualContact contact)
    {
        this.contact = contact;
        updateContactAndStatusIcon();
        contact.getAttributes().addPropertyChangeListener(IndividualContactAttributeNames.STATE, evt -> updateContactAndStatusIcon());
    }

    /**
     * @return the {@link gov.tak.api.contact.IIndividualContact}'s icon overlaid with its state icon.
     */
    @Nonnull
    public Icon getIconWithStatus()
    {
        return contactAndStatusIcon;
    }

    /**
     * @param contactIcon The new contact icon
     */
    public void updateContactIcon(@Nonnull ImageIcon contactIcon)
    {
        contactImage = getBufferedImage(contactIcon);
        updateContactAndStatusIcon();
    }

    /**
     * @param contact The updated contact
     * @throws IllegalArgumentException if the updated contact reference has a different UID
     */
    public void updateContact(@Nonnull IIndividualContact contact)
    {
        if (!this.contact.getUid().equals(contact.getUid()))
        {
            throw new IllegalArgumentException("The updated contact must have the same UID as the existing contact");
        }

        this.contact = contact;
    }

    public IIndividualContact getContact()
    {
        return contact;
    }

    @Nonnull
    @Override
    public String toString()
    {
        return "IndividualContact{" +
                "contactUid=" + contact.getUid() +
                "contactName=" + contact.getName() +
                '}';
    }

    /**
     * @param icon An {@code ImageIcon}
     * @return The icon converted to a {@code BufferedImage}
     */
    private BufferedImage getBufferedImage(ImageIcon icon)
    {
        return ImageUtils.toBufferedImage(icon.getImage(), ColorModel.getRGBdefault());
    }

    /**
     * Overlay the status image onto the contact's image and update the {@link #contactAndStatusIcon}.
     */
    private void updateContactAndStatusIcon()
    {
        final BufferedImage overlaidImage =
                GraphicsUtils.overlayImage(contactImage,
                        IndividualContactUtils.getState(contact) == IndividualContactState.active ? ACTIVE : INACTIVE,
                        STATUS_IMAGE_OFFSET, STATUS_IMAGE_OFFSET);

        contactAndStatusIcon = new ImageIcon(overlaidImage);
    }
}