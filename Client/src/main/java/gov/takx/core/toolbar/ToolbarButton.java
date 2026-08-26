package gov.takx.core.toolbar;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.IPlugin;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.IToolbarButton;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;

import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static gov.takx.platform.ui.UIResources.TOOLBAR_BUTTON_IMAGE_SIZE;

/**
 * Object representing a toolbar button. The toolbar is composed of tabs. Each tab contains one or more components,
 * such as buttons.
 *
 * @since 2.0 (Renamed in 3.4)
 */
@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
@XmlAccessorType(XmlAccessType.NONE)
@PseudoBean
public final class ToolbarButton extends AToolbarComponent implements Comparable<ToolbarButton>, IToolbarButton
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private transient Event<ToolbarChangeEvent> event;

    @XmlElement
    private final ButtonType type;

    @XmlElement
    private String displayText;

    // Tooltip text for the button.
    private String tooltipText;

    // The list of action listeners that will be invoked when the button is pressed.
    private final List<ActionListener> actionListeners = new ArrayList<>();

    // The list of mouse listeners that will be invoked when the button is pressed.
    private final List<MouseListener> mouseListeners = new ArrayList<>();

    // The id of the component descriptor toggled by the button when the button is associated with a UI component.
    private String componentDescriptorId = null;

    // Indicates if this button is enabled.
    private boolean enabled = true;

    // Indicates if this button is complete and valid for adding to the toolbar.
    private boolean valid = true;

    // Indicates if this is a toggle button.
    private boolean togglable;

    // Listener to be notified when the toggle button is deselected (may be null)
    private Consumer<IToolbarButton> toggleDeselectListener;

    // Image icon for use in the UI (generated from the provided image or the default image if none set).
    private Icon icon;

    // The image from which the button icon is created.
    private BufferedImage image;

    // The toggle button group this button belongs to (if any).
    private String toggleButtonGroupName;

    private IPopupMenuProvider popupMenuProvider;

    /**
     * No-arg constructor for JAXB.
     */
    @SuppressWarnings("unused") // Used by JAXB
    private ToolbarButton()
    {
        // JAXB will use reflection to set this value correctly; setting to null here so it can be final
        type = null;
    }

    /**
     * Constructor.
     *
     * @param type The type of button
     * @param id The id for the button
     * @param displayText The text to display for the button
     * @param image The image for the button
     * @param actionListeners List of action listeners to add to the button (may be empty)
     * @param mouseListeners List of mouse listeners to add to the button (may be empty)
     * @param togglable True if this is a toggle button
     * @param toggleButtonGroupName The name of the toggle button group to which the button belongs (if this is a
     *                              togglable button that is part of a toggle button group)
     * @param toggleDeselectListener The listener to be notified when a toggle button is deselected (may be {@code null})
     * @param popupMenuProvider The provider for the popup menu (if this button has a drop-down menu)
     * @param plugin Plugin creating the button (or {@code null} for core buttons)
     * @see ToolbarButtonBuilder
     * @since 3.4
     */
    ToolbarButton(@Nonnull ButtonType type, @Nonnull UUID id, @Nonnull String displayText, @Nonnull BufferedImage image,
                  @Nonnull List<ActionListener> actionListeners, @Nonnull List<MouseListener> mouseListeners, boolean togglable,
                  @Nullable String toggleButtonGroupName, @Nullable Consumer<IToolbarButton> toggleDeselectListener,
                  @Nullable IPopupMenuProvider popupMenuProvider, @Nullable IPlugin plugin)
    {
        super(id, plugin);
        this.type = type;
        this.displayText = displayText;
        this.image = image;
        icon = createIcon(image);
        this.actionListeners.addAll(actionListeners);
        this.mouseListeners.addAll(mouseListeners);
        this.togglable = togglable;
        this.toggleButtonGroupName = toggleButtonGroupName;
        this.toggleDeselectListener = toggleDeselectListener;
        this.popupMenuProvider = popupMenuProvider;
    }

    /**
     * Copy constructor.
     *
     * @param origButton The button to copy
     * @see #copy()
     */
    private ToolbarButton(@Nonnull ToolbarButton origButton)
    {
        super(origButton);
        type = origButton.type;
        displayText = origButton.displayText;
        tooltipText = origButton.tooltipText;
        actionListeners.addAll(origButton.actionListeners);
        mouseListeners.addAll(origButton.mouseListeners);
        componentDescriptorId = origButton.componentDescriptorId;
        enabled = origButton.enabled;
        valid = origButton.valid;
        togglable = origButton.togglable;
        toggleDeselectListener = origButton.toggleDeselectListener;
        image = origButton.image;
        icon = origButton.icon;
        toggleButtonGroupName = origButton.toggleButtonGroupName;
        popupMenuProvider = origButton.popupMenuProvider;
    }

    /**
     * Copy the transient state (all fields other than those encoded in the XML) from the given reference button
     * to this button.
     *
     * @param referenceButton Button from which to copy state
     * @since 2.37
     */
    void copyTransientStateFromReference(@Nonnull ToolbarButton referenceButton)
    {
        actionListeners.addAll(referenceButton.getActionListeners());
        mouseListeners.addAll(referenceButton.getMouseListeners());
        componentDescriptorId = referenceButton.componentDescriptorId;
        setTooltipText(referenceButton.tooltipText);
        setEnabled(referenceButton.enabled);
        togglable = referenceButton.togglable;
        toggleDeselectListener = referenceButton.toggleDeselectListener;
        toggleButtonGroupName = referenceButton.toggleButtonGroupName;
        setImage(referenceButton.image);
        popupMenuProvider = referenceButton.popupMenuProvider;
        setValid(true);
    }

    @Override
    @Nonnull
    public String getDisplayText()
    {
        return displayText;
    }

    @Override
    @Nonnull
    public ToolbarButton setDisplayText(@Nonnull String displayText)
    {
        String oldDisplayText = this.displayText;
        this.displayText = displayText;
        fireEvent(ToolbarChangeEventType.BUTTON_TEXT_CHANGED, oldDisplayText, displayText);
        return this;
    }

    @Override
    @Nonnull
    public ToolbarButton addActionListener(@Nonnull ActionListener actionListener)
    {
        actionListeners.add(actionListener);
        fireEvent(ToolbarChangeEventType.BUTTON_LISTENER_ADDED, null, actionListener);
        return this;
    }

    @Override
    @Nonnull
    public IToolbarButton removeActionListener(@Nonnull ActionListener actionListener)
    {
        actionListeners.remove(actionListener);
        fireEvent(ToolbarChangeEventType.BUTTON_LISTENER_REMOVED, actionListener, null);
        return this;
    }

    @Nonnull
    @Override
    public IToolbarButton addMouseListener(@Nonnull MouseListener mouseListener)
    {
        mouseListeners.add(mouseListener);
        fireEvent(ToolbarChangeEventType.BUTTON_LISTENER_ADDED, null, mouseListener);
        return this;
    }

    @Nonnull
    @Override
    public IToolbarButton removeMouseListener(@Nonnull MouseListener mouseListener)
    {
        mouseListeners.remove(mouseListener);
        fireEvent(ToolbarChangeEventType.BUTTON_LISTENER_REMOVED, mouseListener, null);
        return this;
    }

    @Nonnull
    public List<ActionListener> getActionListeners()
    {
        return Collections.unmodifiableList(actionListeners);
    }

    /**
     * @return A copy of the {@code MouseListener}s
     * @since 5.0
     */
    @Nonnull
    public List<MouseListener> getMouseListeners()
    {
        return Collections.unmodifiableList(mouseListeners);
    }

    /**
     * @return A copy of the {@code MouseListener}s wrapped in {@link ToolbarMouseListener}s
     * @since 5.0
     */
    @Nonnull
    public List<ToolbarMouseListener> getToolbarMouseListeners()
    {
        return mouseListeners.stream().map(ToolbarMouseListener::new).collect(Collectors.toList());
    }

    /**
     * @return The id of the component descriptor toggled by the button when it is associated with a UI component.
     * @since 3.4
     */
    @Nullable
    public String getComponentDescriptorId()
    {
        return componentDescriptorId;
    }

    /**
     * Sets the id of the component descriptor toggled by the button when it is associated with a UI component.
     *
     * @param componentDescriptorId The id of the associated component descriptor
     * @return this button instance (to allow for chaining calls)
     * @since 3.4
     */
    @Nonnull
    public ToolbarButton setComponentDescriptorId(@Nullable String componentDescriptorId)
    {
        this.componentDescriptorId = componentDescriptorId;
        return this;
    }

    @Override
    @Nullable
    public String getTooltipText()
    {
        return tooltipText;
    }

    @Override
    @Nonnull
    public ToolbarButton setTooltipText(@Nullable String tooltipText)
    {
        if (!Objects.equals(tooltipText, this.tooltipText))
        {
            String oldTooltipText = this.tooltipText;
            this.tooltipText = tooltipText;

            fireEvent(ToolbarChangeEventType.BUTTON_TOOLTIP_CHANGED, oldTooltipText, tooltipText);
        }

        return this;
    }

    @Nonnull
    public Icon getIcon()
    {
        if (icon == null) icon = createIcon(image);
        return icon;
    }

    /**
     * Creates an icon of the appropriate size from the provided image (or the default image if no image is provided).
     *
     * @param image The image from which to create the icon
     * @return The icon
     */
    @Nonnull
    private Icon createIcon(@Nullable BufferedImage image)
    {
        if (image == null)
        {
            logger.debug("No image set for button; using default image");
            image = ImageUtils.getDefaultImage();
        }

        return ImageUtils.getSquareIcon(image, TOOLBAR_BUTTON_IMAGE_SIZE.width);
    }

    @Nullable
    @Override
    public BufferedImage getImage()
    {
        return image;
    }

    @Override
    @Nonnull
    public ToolbarButton setImage(@Nonnull BufferedImage image)
    {
        final BufferedImage oldImage = this.image;
        this.image = image;

        if (GraphicsUtils.compareImages(oldImage, image)) return this; // Don't change the icon if the same to avoid flicker

        icon = createIcon(image);
        fireEvent(ToolbarChangeEventType.BUTTON_IMAGE_CHANGED, oldImage, image);

        return this;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    @Nonnull
    public ToolbarButton setEnabled(boolean enabled)
    {
        if (this.enabled != enabled)
        {
            this.enabled = enabled;

            fireEvent(ToolbarChangeEventType.BUTTON_STATE_CHANGED, !this.enabled, enabled);
        }

        return this;
    }

    @Override
    public boolean isValid()
    {
        return valid;
    }

    /**
     * Sets the valid state of the button. When a button is set to invalid, it is important to remove references to any
     * objects that might belong to a plugin. Otherwise, a memory leak could occur. These fields will be populated
     * when the button is made available again by {@code ToolbarConfigurationManager#populateButtonFields(ToolbarButton)}.
     *
     * @param valid Boolean indicating if the button is valid
     * @return this button instance (to allow for chaining calls)
     */
    @Nonnull
    public ToolbarButton setValid(boolean valid)
    {
        if (valid != this.valid)
        {
            this.valid = valid;

            if (!valid)
            {
                tooltipText = null;
                actionListeners.clear();
                mouseListeners.clear();
                icon = null;
                toggleButtonGroupName = null;
            }

            fireEvent(ToolbarChangeEventType.BUTTON_VALIDITY_CHANGED, !this.valid, this.valid);
        }

        return this;
    }

    @Override
    public boolean isTogglable()
    {
        return togglable;
    }

    /**
     * @return The name of the toggle button group to which the button belongs (may be {@code null}).
     */
    @Nullable
    public String getToggleButtonGroupName()
    {
        return toggleButtonGroupName;
    }

    /**
     * @return The listener to be notified when the toggle button is deselected.
     * @since 3.4
     */
    @Nullable
    public Consumer<IToolbarButton> getToggleDeselectListener()
    {
        return toggleDeselectListener;
    }

    @Override
    @Nullable
    public IPopupMenuProvider getPopupMenuProvider()
    {
        return popupMenuProvider;
    }

    @Override
    @Nonnull
    public ButtonType getType()
    {
        return type;
    }

    /**
     * Fires a change event after ensuring this button is part of a configuration.
     *
     * @param type The type of change event
     * @param oldValue The old value of the changed item
     * @param newValue The new value of the changed item
     */
    private void fireEvent(@Nonnull ToolbarChangeEventType type, @Nullable Object oldValue, @Nullable Object newValue)
    {
        // For buttons on the toolbar, only send the event if the parent tab is present, valid, and visible. If it is
        // not, it will not exist on the toolbar and will cause an exception when retrieved.
        if (!isParentVisible()) return;

        ToolbarConfiguration parentConfiguration = getParentConfiguration();
        if (parentConfiguration != null && !Objects.equals(oldValue, newValue))
        {
            event.fire(new ToolbarChangeEvent(parentConfiguration, type, this, oldValue, newValue));
        }
    }

    @Override
    public int compareTo(@Nonnull ToolbarButton other)
    {
        // Buttons with the same id are the same, even if other fields differ (such as the display text)
        if (id.compareTo(other.id) == 0) return 0;

        // Compare the toString() representation so the buttons will be sorted alphabetically
        final String identificationString = getIdentificationString();
        String otherIdentificationString = other.getIdentificationString();
        return identificationString.compareToIgnoreCase(otherIdentificationString);
    }

    @Override
    @Nonnull
    public ToolbarButton copy()
    {
        return new ToolbarButton(this);
    }

    /**
     * @return A display string for identifying the button in the user interface for toolbar configuration.
     * @since 3.4
     */
    public String getIdentificationString()
    {
        if (pluginName != null)
        {
            return displayText + " (" + pluginName + ")";
        } else
        {
            final String category = type.getCategory();
            if (!StringUtils.isEmpty(category))
            {
                return displayText + " (" + category + ")";
            }

            return displayText;
        }
    }

    @Override
    public String toString()
    {
        // Instances are only equal if they are the same object, so include the hashcode in the toString output to
        // assist with debugging.
        return "ToolbarButton@" + Integer.toHexString(hashCode()) + "{" +
                "id=" + id +
                ", pluginName='" + pluginName + '\'' +
                ", pluginUniqueKey='" + pluginUniqueKey + '\'' +
                ", type=" + type +
                ", displayText='" + displayText + '\'' +
                ", tooltipText='" + tooltipText + '\'' +
                ", componentDescriptorId='" + componentDescriptorId + '\'' +
                ", valid=" + valid +
                ", togglable=" + togglable +
                ", toggleButtonGroupName='" + toggleButtonGroupName + '\'' +
                ", popupMenuProvider=" + popupMenuProvider +
                '}';
    }

    /**
     * Wrapper used to distinguish the model's {@code MouseListener}s from those automatically added by the UI
     *
     * @param mouseListener The mouse listener to wrap
     * @since 5.0
     */
    public record ToolbarMouseListener(MouseListener mouseListener) implements MouseListener
    {
        public ToolbarMouseListener(@Nonnull MouseListener mouseListener)
        {
            this.mouseListener = mouseListener;
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            mouseListener.mouseClicked(e);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            mouseListener.mousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            mouseListener.mouseReleased(e);
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            mouseListener.mouseEntered(e);
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            mouseListener.mouseExited(e);
        }
    }
}
