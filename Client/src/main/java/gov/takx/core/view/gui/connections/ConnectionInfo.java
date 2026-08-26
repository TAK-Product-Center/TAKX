package gov.takx.core.view.gui.connections;

import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.commons.lang.IPropertyChangeDelegateSupport;
import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.LayeredIcon;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static gov.takx.commons.swing.ImageUtils.MEDIUM_ICON_WIDTH;
import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Entity to represent connection information for the {@link ConnectionsPanel}. This class is necessary since there is not
 * always a {@link gov.takx.api.plugin.IRaptorObject} instance for a connection.
 *
 * @since 5.8
 */
public class ConnectionInfo implements IPropertyChangeDelegateSupport
{
    public static final String MESSAGE = "Message";
    public static final String STATE = "State";
    public static final String NAME = "Name";

    private static final Map<ConnectionState, Icon> STATE_ICONS = new HashMap<>();
    static
    {
        STATE_ICONS.put(ConnectionState.Disconnected, new ImageIcon(GraphicsUtils.createCircle(MEDIUM_ICON_WIDTH, 200, Color.GRAY)));
        STATE_ICONS.put(ConnectionState.Connecting, new ImageIcon(GraphicsUtils.createCircle(MEDIUM_ICON_WIDTH, 200, Color.ORANGE)));
        STATE_ICONS.put(ConnectionState.Connected, new ImageIcon(GraphicsUtils.createCircle(MEDIUM_ICON_WIDTH, 200, Color.GREEN)));
        STATE_ICONS.put(ConnectionState.Error, new ImageIcon(GraphicsUtils.createCircle(MEDIUM_ICON_WIDTH, 200, Color.RED)));
    }

    private final PrioritizedPropertyChangeSupport propertyChangeSupport = new PrioritizedPropertyChangeSupport(this);

    private final String key;
    private final LayeredIcon icon;
    private String name;
    private ConnectionState state;
    private String message;

    /**
     * Construct a new connection with the given unique key, name, and state, and create a layered icon using the state
     * and the optional icon.
     *
     * @param key the unique key
     * @param name the name
     * @param state the state
     * @param iconURLString the icon url
     * @param resourceSource the resource to use in finding the icon
     * @param message the message text
     */
    public ConnectionInfo(@Nonnull String key, @Nonnull String name, @Nonnull ConnectionState state, @Nullable String iconURLString, @Nonnull Class<?> resourceSource, @Nullable String message)
    {
        this.key = key;
        this.name = name;
        this.state = state;
        this.message = message;
        icon = new LayeredIcon(STATE_ICONS.get(state));

        var iconURL = iconURLString == null ? UIResources.getUrl(PLATFORM_ICONS, "data.png") : resourceSource.getResource(iconURLString.startsWith("/") ? iconURLString : '/' + iconURLString);
        var connectionImage = iconURL != null ? ImageUtils.getScaledImage(iconURL, SMALL_ICON_WIDTH, SMALL_ICON_HEIGHT, false) : null;
        if (connectionImage != null) icon.addLayer(new ImageIcon(connectionImage));
    }

    @Override
    @Nonnull
    public IPropertyChangeListenerSupport getPropertyChangeListenerSupport()
    {
        return propertyChangeSupport;
    }

    /**
     * @return the current icon for this connection
     */
    @Nonnull
    public Icon getIcon()
    {
        return icon;
    }

    /**
     * @return the current name of this connection
     */
    @Nonnull
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name to display for this connection
     */
    public void setName(@Nonnull String name)
    {
        propertyChangeSupport.updateAndFire(STATE, this.name, name, () -> this.name = name);
    }

    /**
     * @return the current state of this connection
     */
    @Nonnull
    public ConnectionState getState()
    {
        return state;
    }

    /**
     * @param state the state to use for this connection
     */
    public void setState(@Nonnull ConnectionState state)
    {
        propertyChangeSupport.updateAndFire(STATE, this.state, state, () -> {
            this.state = state;
            icon.setLayer(0, STATE_ICONS.get(state));
        });
    }

    /**
     * @return the current message for this connection
     */
    @Nullable
    public String getMessage()
    {
        return message;
    }

    /**
     * @param message the message to use for this connection
     */
    public void setMessage(@Nullable String message)
    {

        propertyChangeSupport.updateAndFire(MESSAGE, this.message, message, () -> this.message = message);
    }

    /**
     * @return the unique key for this connection
     */
    @Nonnull
    public String getKey()
    {
        return key;
    }
}
