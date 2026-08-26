package gov.tak.core.icons;

import gov.tak.api.icons.IUserIcon;
import gov.tak.api.icons.IUserIconSet;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Holds the icon set definition from a custom icon set XML document.
 *
 * @since 3.1 (moved in 5.2)
 */
@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "unused"})
@XmlRootElement(name = "iconset")
@XmlAccessorType(XmlAccessType.NONE)
public class UserIconSet implements IUserIconSet
{
    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute(required = true)
    private String uid;

    @XmlAttribute(name = "defaultGroup")
    private String selectedGroup = "";

    @XmlAttribute
    private String defaultFriendly;

    @XmlAttribute
    private String defaultHostile;

    @XmlAttribute
    private String defaultNeutral;

    @XmlAttribute
    private String defaultUnknown;

    /**
     * Flag to skip auto resizing of icons to 32x32px
     */
    @XmlAttribute
    private boolean skipResize = false;

    /**
     * iconset XML schema version
     */
    @XmlAttribute(name = "version", required = true)
    private int version = 1;

    @XmlElement(name = "icon")
    private Set<UserIcon> icons = new HashSet<>();

    @XmlTransient
    private Path iconSetPath; // Path to source zip

    @XmlTransient
    private final Map<String, UserIcon> iconsByName = new HashMap<>();

    @XmlTransient
    private final Map<String, UserIcon> groupIcons = new HashMap<>();

    @SuppressWarnings("unused") // For JAXB
    UserIconSet()
    {
    }

    /**
     * @param name Name of the icon set
     * @param uid UID of the icon set
     * @since 5.2
     */
    UserIconSet(String name, String uid)
    {
        this.name = name;
        this.uid = uid;
    }

    @Override
    public boolean isValid()
    {
        return !StringUtils.isEmpty(uid) && !StringUtils.isEmpty(name);
    }

    @Override
    @Nonnull
    public Path getIconSetPath()
    {
        return iconSetPath;
    }

    /**
     * @param iconSetPath Path to our source zip file
     */
    public void setIconSetPath(Path iconSetPath)
    {
        this.iconSetPath = iconSetPath;
    }

    @Override
    @Nonnull
    public String getName()
    {
        return name;
    }

    @Override
    @Nonnull
    public String getUid()
    {
        return uid;
    }

    @Override
    public int getVersion()
    {
        return version;
    }

    @Override
    @Nonnull
    public List<IUserIcon> getIcons()
    {
        return icons.stream().filter(IUserIcon::isValid).map(IUserIcon.class::cast).toList();
    }

    @Override
    public boolean hasIcons()
    {
        return !icons.isEmpty();
    }

    @Override
    @Nonnull
    public String getSelectedGroup()
    {
        return selectedGroup;
    }

    @Override
    public void setSelectedGroup(@Nonnull String selectedGroup)
    {
        this.selectedGroup = selectedGroup;
    }

    @Override
    @Nullable
    public String getDefaultFriendly()
    {
        return defaultFriendly;
    }

    @Override
    @Nullable
    public String getDefaultHostile()
    {
        return defaultHostile;
    }

    @Override
    @Nullable
    public String getDefaultNeutral()
    {
        return defaultNeutral;
    }

    @Override
    @Nullable
    public String getDefaultUnknown()
    {
        return defaultUnknown;
    }

    @Override
    public boolean isSkipResize()
    {
        return skipResize;
    }

    /**
     * Add (create) a new icon or update an existing icon with the given properties.  The existence check is based
     * on the given {@code filename}.
     *
     * @param filename The filename of the icon the add or update
     * @param group The group in which the icon belongs
     * @param zipEntryPath Entry path within the containing zip file
     * @param imageBytes Raw image bytes for the icon
     * @since 5.2
     */
    public void addOrUpdateIcon(@Nonnull String filename, @Nonnull String group, @Nonnull String zipEntryPath, @Nonnull byte[] imageBytes)
    {
        UserIcon icon = removeIcon(filename); // Remove it since the changes below will alter its equality metrics
        if (icon == null) icon = new UserIcon(filename);

        icon.setGroup(group);
        icon.setIconsetUid(getUid());
        icon.setZipEntryPath(zipEntryPath);
        icon.setImageBytes(imageBytes);

        addIcon(icon);
    }

    /**
     * Add the given icon to our list and name index.
     */
    private void addIcon(@Nonnull UserIcon icon)
    {
        icons.add(icon);
        iconsByName.put(icon.getFileName(), icon);

        // The first time we see an icon for a group, save it, so we can provide a default icon for that group.
        // And, if no default group has been set, then use the group of the first icon added.
        final String group = icon.getGroup();
        groupIcons.putIfAbsent(group, icon);
        if (StringUtils.isEmpty(selectedGroup)) selectedGroup = group;
    }

    /**
     * Remove the icon with the given {@code filename}.
     *
     * @param filename File name of icon to remove
     * @return Removed {@link UserIcon}, or {@code null} if not found
     * @since 5.2
     */
    @Nullable
    private UserIcon removeIcon(@Nonnull String filename)
    {
        final UserIcon icon = getIcon(filename);
        if (icon != null)
        {
            icons.remove(icon);
            iconsByName.remove(filename);
        }
        return icon;
    }

    @Override
    @Nullable
    public UserIcon getIcon(@Nonnull String filename)
    {
        if (iconsByName.isEmpty()) makeNameIndex();
        return iconsByName.get(filename);
    }

    @Override
    public boolean hasIcon(@Nonnull String filename)
    {
        return getIcon(filename) != null;
    }

    @Override
    @Nonnull
    public Collection<String> getGroups()
    {
        return new TreeSet<>(groupIcons.keySet());
    }

    @Override
    @Nonnull
    public List<IUserIcon> getIconsInGroup(@Nonnull String group)
    {
        return icons.stream().filter(icon -> icon.isValid() && group.equals(icon.getGroup())).collect(Collectors.toList());
    }

    @Override
    @Nullable
    public IUserIcon getIconForGroup(@Nonnull String group)
    {
        return groupIcons.get(group);
    }

    /**
     * Create an index on the icons by name.
     */
    private void makeNameIndex()
    {
        icons.forEach(icon -> iconsByName.put(icon.getFileName(), icon));
    }

    @Override
    public String toString()
    {
        return "UserIconSet{" +
                "name='" + name + '\'' +
                ", uid='" + uid + '\'' +
                ", selectedGroup='" + selectedGroup + '\'' +
                ", icons=" + icons.size() +
                '}';
    }
}
