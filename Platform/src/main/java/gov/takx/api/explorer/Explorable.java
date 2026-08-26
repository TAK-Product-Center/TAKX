package gov.takx.api.explorer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Concrete implementation of IExplorable.   Implementors of {@link IExplorable} are encouraged
 * to extend this class (or {@link AExplorable}) as the basis for their custom explorables.
 *
 * @since 1.48
 */
public class Explorable extends AExplorable
{
    private final String pathName;
    private volatile String displayName;
    private volatile Color displayNameColor;
    private volatile BufferedImage iconImage;

    /**
     * @param displayName name of entry in the explorer tree
     * @param parentPath  parent/ancestor path, will be created if necessary
     */
    public Explorable(@Nonnull String displayName, @Nullable List<String> parentPath)
    {
        this(displayName, parentPath, null);
    }

    /**
     * @param displayName name of entry in the explorer tree
     * @param uniqueKey   unique key identifier
     * @param parentPath  parent/ancestor path, will be created if necessary
     * @since 5.3
     */
    public Explorable(@Nonnull String displayName, @Nonnull String uniqueKey, @Nullable List<String> parentPath)
    {
        this(null, displayName, uniqueKey, parentPath, null, false, true);
    }

    /**
     * Constructor (overloaded), with only display name, parent path and initial "checked" state specified.
     * Owner, key and icon image will default to {@code null}, and group to false.
     *
     * @param displayName name of entry in the explorer tree
     * @param parentPath  parent/ancestor path, will be created if necessary
     * @param checked     initial state of checkbox
     * @since 2.5
     */
    public Explorable(@Nonnull String displayName, @Nullable List<String> parentPath, boolean checked)
    {
        this(null, displayName, null, parentPath, null, false, checked);
    }

    public Explorable(@Nonnull String displayName, @Nullable List<String> parentPath, @Nullable BufferedImage iconImage)
    {
        this(displayName, parentPath, iconImage, false);
    }

    public Explorable(@Nonnull String displayName, @Nullable List<String> parentPath, @Nullable BufferedImage iconImage, boolean group)
    {
        this(null, displayName, null, parentPath, iconImage, group, true);
    }

    /**
     * Constructor, overloaded ability to set everything except the initial check state which will default to true, and
     * the display name color which will default to {@code null}.
     *
     * @param owner       owner of the explorable, as in the map object
     * @param displayName string name to appear in the node text
     * @param uniqueKey   unique key identifier, if {@code null} a random UUID will be generated
     * @param parentPath  parent node path as a list of Strings
     * @param iconImage   Icon image to use for the node along with the display name text
     * @param group       {@code true} if this explorable is a group, {@code false} otherwise.
     */
    public Explorable(@Nullable Object owner, @Nonnull String displayName, @Nullable String uniqueKey, @Nullable List<String> parentPath,
                      @Nullable BufferedImage iconImage, boolean group)
    {
        this(owner, displayName, uniqueKey, parentPath, iconImage, group, true);
    }

    /**
     * Constructor, overloaded ability to set everything except the display name color which will default to {@code null}.
     *
     * @param owner       owner of the explorable, as in the map object
     * @param displayName string name to appear in the node text
     * @param uniqueKey   unique key identifier, if {@code null} a random UUID will be generated
     * @param parentPath  parent node path as a list of Strings
     * @param iconImage   Icon image to use for the node along with the display name text
     * @param group       {@code true} if this explorable is a group, {@code false} otherwise.
     * @param checked     {@code true} if the checkbox should be initially checked, {@code false} otherwise.
     * @since 2.5
     */
    public Explorable(@Nullable Object owner, @Nonnull String displayName, @Nullable String uniqueKey, @Nullable List<String> parentPath,
                      @Nullable BufferedImage iconImage, boolean group, boolean checked)
    {
        this(owner, displayName, uniqueKey, parentPath, iconImage, group, checked, null);
    }

    /**
     * Constructor, fully overloaded to allow pretty much everything to be set, including the initial checked /
     * unchecked state of the node in the tree, and the display name font color.
     *
     * @param owner                owner of the explorable, as in the map object
     * @param displayName          string name to appear in the node text
     * @param uniqueKey            unique key identifier, if {@code null} a random UUID will be generated
     * @param parentPath           parent node path as a list of Strings
     * @param iconImage            Icon image to use for the node along with the display name text
     * @param group                {@code true} if this explorable is a group, {@code false} otherwise.
     * @param checked              {@code true} if the checkbox should be initially checked, {@code false} otherwise.
     * @param displayNameFontColor {@code null} if the default color should be employed, or a {@link Color} to set the
     *                             display name font color.
     * @since 2.13
     */
    public Explorable(@Nullable Object owner, @Nonnull String displayName, @Nullable String uniqueKey, @Nullable List<String> parentPath,
                      @Nullable BufferedImage iconImage, boolean group, boolean checked, @Nullable Color displayNameFontColor)
    {
        super(owner, parentPath, uniqueKey != null ? uniqueKey : UUID.randomUUID().toString(), group);
        this.displayName = displayName;
        pathName = displayName;
        displayNameColor = displayNameFontColor;
        this.iconImage = iconImage;
        visible.set(checked);
    }

    @Nonnull
    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String getPathName()
    {
        return pathName;
    }

    public void setDisplayName(@Nonnull String displayName)
    {
        String old = this.displayName;

        if (!Objects.equals(displayName, old))
        {
            this.displayName = displayName;
            firePropertyChange(DISPLAY_NAME, old, displayName);
        }
    }

    @Override
    public Color getDisplayNameColor()
    {
        return displayNameColor;
    }

    @Override
    public void setDisplayNameColor(Color displayNameColor)
    {
        Color old = this.displayNameColor;

        if (!Objects.equals(displayNameColor, old))
        {
            this.displayNameColor = displayNameColor;
            firePropertyChange(DISPLAY_NAME_COLOR, old, displayNameColor);
        }
    }

    @Override
    public BufferedImage getIconImage()
    {
        return iconImage;
    }

    @Override
    public void setIconImage(BufferedImage iconImage)
    {
        BufferedImage old = this.iconImage;

        if (!Objects.equals(iconImage, old))
        {
            this.iconImage = iconImage;
            firePropertyChange(ICON_IMAGE, old, iconImage);
        }
    }

    @Override
    public void dispose()
    {
        displayNameColor = null;
        iconImage = null;
        super.dispose();
    }

    @Override
    public String toString()
    {
        return "Explorable{" +
                "displayName='" + displayName + '\'' +
                ", parentPath=" + parentPath + '\'' +
                ", uniqueKey='" + uniqueKey + '\'' +
                '}';
    }
}
