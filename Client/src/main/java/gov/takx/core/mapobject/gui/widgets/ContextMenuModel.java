package gov.takx.core.mapobject.gui.widgets;

import android.util.Base64;
import gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum;
import gov.takx.core.globe.radial.RadialMenuAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Model that represents a ContextMenu for creating and controlling IMapMenuWidgets
 *
 * @since 3.0
 */
public class ContextMenuModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Map<Image, URI> ENCODED_IMAGE_CACHE = new IdentityHashMap<>();

    private final JPopupMenu menu;

    /**
     * Creates a new {@code ContextMenuModel} instance
     *
     * @param menu A {@link JPopupMenu} that represents the data model.
     */
    public ContextMenuModel(JPopupMenu menu)
    {
        this.menu = menu;
    }

    /**
     * @return the number of menu items
     */
    public int getItemCount()
    {
        return menu.getComponentCount();
    }

    /**
     * Get the <I>submenu</I> at a given index. If the item at the index is not a submenu, {@code null} is
     * returned
     *
     * @param index the menu item index
     * @return The {@link ContextMenuEnum} at the given index or {@code null} if the index does not represent a
     * submenu
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= getItemCount())
     */
    @Nullable
    public ContextMenuModel getSubmenu(int index)
    {
        Component component = menu.getComponent(index);
        if (component instanceof JMenu subMenu)
        {
            final var popup = subMenu.getPopupMenu();
            if (popup != null) return new ContextMenuModel(popup);
        }
        return null;
    }

    /**
     * Returns the icon name for the item at the given index, or {@code null} if there is no icon for the
     * item.
     *
     * @param index the menu item index
     * @return The name for the icon or {@code null} if there is no icon for the item
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= getItemCount())
     */
    @Nullable
    public URI getIconURI(int index)
    {
        Component component = menu.getComponent(index);
        if (component instanceof JMenuItem menuItem)
        {
            if (menuItem.getIcon() instanceof ImageIcon imageIcon)
            {
                // If a ContextMenuIcon has been used for this menu item, get the larger image for radial use
                Image image = (imageIcon instanceof ContextMenuIcon contextMenuIcon) ? contextMenuIcon.getRadialImage() : imageIcon.getImage();

                final URI uri = ENCODED_IMAGE_CACHE.computeIfAbsent(image, k -> makeEncodedUri(menuItem.getText(), image));
                if (uri != null) return uri;
            }

            if (menuItem.getAction() instanceof RadialMenuAction menuAction)
            {
                String iconName = menuAction.getIconName();
                try
                {
                    if (iconName != null) return new URI(iconName);
                } catch (URISyntaxException exception)
                {
                    logger.error("Error resolving icon for {}", iconName, exception);
                }
            }
        }
        return null;
    }

    /**
     * Convert a given image to a URI with a {@code base64} scheme.
     *
     * @param name Name to use for error reporting
     * @param image Image to encode
     * @return encoded URI
     * @since 5.0
     */
    @Nullable
    private static URI makeEncodedUri(@Nonnull String name, @Nonnull Image image)
    {
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] bytes = baos.toByteArray();

            return new URI("base64://" + Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.URL_SAFE));
        } catch (IOException | URISyntaxException exception)
        {
            logger.error("Failed to encode ImageIcon for menu item {}", name, exception);
        }
        return null;
    }

    /**
     * Returns the name for the item at the given index, or {@code null} if there is no name for the item. When an
     * item has a name but does not have an icon, the name will be displayed as the button's text.
     *
     * @param index the menu item index
     * @return The name for the item or {@code null} if there is no name for the item
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= getItemCount())
     */
    @Nullable
    public String getName(int index)
    {
        Component component = menu.getComponent(index);
        if (component instanceof JMenuItem menuItem)
        {
            if (menuItem.getAction() instanceof RadialMenuAction menuAction)
            {
                return menuAction.getName();
            }
            return menuItem.getText();
        }
        return null;
    }

    /**
     * Returns {@code true} if the item represents a menu separator, {@code false} otherwise.
     *
     * @param index the menu item index
     * @return {@code true} if the item represents a menu separator, {@code false} otherwise
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= getItemCount())
     */
    public boolean isSeparator(int index)
    {
        return menu.getComponent(index) instanceof JSeparator;
    }

    /**
     * Get the menu action at a given index.
     *
     * @param index the menu item index
     * @return a {@link RadialMenuAction} instance or {@code null} if no action is defined for that menu item
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= getItemCount())
     */
    public RadialMenuAction getMenuAction(int index)
    {
        return (RadialMenuAction) ((JMenuItem) menu.getComponent(index)).getAction();
    }
}
