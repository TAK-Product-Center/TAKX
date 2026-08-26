package gov.tak.core.icons;

import gov.tak.api.icons.IUserIcon;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Holds a single icon definition from a custom icon set XML document.
 *
 * @since 3.1 (moved in 5.2)
 */
@SuppressWarnings("FieldMayBeFinal")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class UserIcon implements IUserIcon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @XmlAttribute(name = "name", required = true)
    private String fileName;

    /**
     * Optional CoT for compliance with other CoT viewers (e.g. when sending points out over the network)
     */
    @XmlAttribute(name = "type2525b")
    private String type2525c = "";

    /**
     * Derived from directory in zip file, not specified in XML.
     */
    @XmlTransient
    private String group = "Other";

    @XmlTransient
    private String iconsetUid;

    @XmlTransient
    private String zipEntryPath;

    @XmlTransient
    private byte[] imageBytes;

    @SuppressWarnings("unused") // For JAXB
    UserIcon()
    {
    }

    /**
     * @param fileName Icon file name
     * @since 5.2
     */
    UserIcon(String fileName)
    {
        this.fileName = fileName;
    }

    @Override
    public String getFileName()
    {
        return fileName;
    }

    @Override
    public String get2525cType()
    {
        return type2525c;
    }

    @Override
    @Nonnull
    public String getGroup()
    {
        return group;
    }

    /**
     * @param group The group containing this icon
     */
    public void setGroup(@Nonnull String group)
    {
        this.group = group;
    }

    @Override
    @Nonnull
    public String getIconsetUid()
    {
        return iconsetUid;
    }

    /**
     * @param iconsetUid UID of the {@link UserIconSet} containing this icon
     * @since 5.2
     */
    public void setIconsetUid(String iconsetUid)
    {
        this.iconsetUid = iconsetUid;
    }

    @Override
    @Nonnull
    public String getZipEntryPath()
    {
        return zipEntryPath;
    }

    /**
     * @param zipEntryPath Path within the icon set zip file for this icon
     */
    public void setZipEntryPath(@Nonnull String zipEntryPath)
    {
        this.zipEntryPath = zipEntryPath;
    }

    @Override
    @Nonnull
    public byte[] getImageBytes()
    {
        return imageBytes;
    }

    /**
     * @param imageBytes The raw image bytes for this icon
     * @since 5.2
     */
    public void setImageBytes(@Nonnull byte[] imageBytes)
    {
        this.imageBytes = imageBytes;
    }

    @Override
    public boolean isValid()
    {
        return !StringUtils.isEmpty(iconsetUid) && !StringUtils.isEmpty(group) && !StringUtils.isEmpty(fileName) && imageBytes != null;
    }

    @Override
    @Nonnull
    public URI toUri()
    {
        final String path = iconsetUid + "/" + group + "/" + fileName;
        try
        {
            return isValid() ? new URI("iconset", path, null) : ImageUtils.DEFAULT_IMAGE_URI;
        } catch (URISyntaxException e)
        {
            logger.warn("Failed to construct URI from 'iconset:{}", path);
            return ImageUtils.DEFAULT_IMAGE_URI;
        }
    }

    @Override
    public String toString()
    {
        return group + "/" + fileName + "/" + type2525c;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserIcon userIcon = (UserIcon) o;

        return Objects.equals(fileName, userIcon.fileName) && Objects.equals(iconsetUid, userIcon.iconsetUid);
    }

    @Override
    public int hashCode()
    {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + (iconsetUid != null ? iconsetUid.hashCode() : 0);
        return result;
    }
}
