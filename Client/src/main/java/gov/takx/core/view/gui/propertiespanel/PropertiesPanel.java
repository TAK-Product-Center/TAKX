package gov.takx.core.view.gui.propertiespanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.DevicePanelMetadataKeys;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.swing.entity.ActionButtonPanel;
import gov.takx.core.mapobject.gui.MapObjectAttachmentsPanel;
import gov.takx.core.mapobject.gui.primarydialog.IMapObjectPrimaryDialog;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.DrawingTools;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import java.awt.Dimension;
import java.awt.Font;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ATTACH_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.ATTACH_ICON_URL;

/**
 * Properties panel. Displays the data from the map object dialog, as well as editing tools for shapes.
 *
 */
@PseudoBean(singletonObserver = true)
public class PropertiesPanel extends ATitledPanel implements IRaptorLifecycleListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TITLE = ButtonType.PROPERTIES.getDisplayText();

    private static final CellConstraints XY_1_1 = new CellConstraints().xy(1, 1);
    private static final String NOTHING_SELECTED_TEXT = "Nothing Selected";
    private static final ImageIcon WARNING_ICON = UIResources.getToolbarIcon(PLATFORM_ICONS, "warning.svg");

    @Inject
    private IExtendedUISpaceManager spaceManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    private final List<IPropertiesItemListener> propertiesItemListeners = new ArrayList<>();
    private IPropertiesItem selectedItem;
    private JPanel currentPanel;
    private MapObjectAttachmentsPanel mapObjectAttachmentsPanel;

    /**
     * Contract for receiving notifications when the installed properties item changes.
     * @since 5.5
     */
    public interface IPropertiesItemListener
    {
        /**
         * Called when the installed properties item changes.
         *
         * @param item The new properties item, may be {@code null} if no item is being shown
         */
        void propertiesItemChanged(@Nullable IPropertiesItem item);
    }

    /**
     * Allows properties panel instance to only be instantiated once the class is accessed.
     */
    private static final class PropertiesPanelSingleton
    {
        public static final PropertiesPanel instance = new PropertiesPanel();
    }

    /**
     * Provide a singleton instance of the properties panel.
     *
     * @return PropertiesPanel
     */
    public static PropertiesPanel getInstance()
    {
        return PropertiesPanelSingleton.instance;
    }

    /**
     * Keep outside instantiation of this class from occurring.
     */
    private PropertiesPanel()
    {
        super(TITLE);
        initComponents();

        // Make the scroll wheel input move in larger increments
        propertiesPane.getVerticalScrollBar().setUnitIncrement(10);
        propertiesPane.getHorizontalScrollBar().setUnitIncrement(10);
        openAttachments.setIcon(ATTACH_ACTION_BUTTON_ICON);
        dialogOpenLabel.setIcon(WARNING_ICON);
        configureButton.setIcon(UIResources.SETTINGS_ACTION_BUTTON_ICON);

        actionButtonPanel.addActionButton(configureButton);
        actionButtonPanel.addActionButton(openAttachments);

        noSelection();
    }

    /**
     * Add a {@link IPropertiesItemListener}.
     * @since 5.5
     */
    public void addPropertiesItemListener(@Nonnull IPropertiesItemListener listener)
    {
        propertiesItemListeners.add(listener);
    }

    /**
     * Remove a {@link IPropertiesItemListener}.
     * @since 5.5
     */
    public void removePropertiesItemListener(@Nonnull IPropertiesItemListener listener)
    {
        propertiesItemListeners.remove(listener);
    }

    /**
     * Currently unused, but retained description for posterity or eventual resurrection.
     */
    public String getDescription()
    {
        return "<html>The properties for the currently selected object on the globe are displayed below. This panel will display these properties until the panel is closed or another object on the globe is selected.</html>";
    }

    public void onDialogClosing(IMapObjectPrimaryDialog<?> dialog)
    {
        if (selectedItem != null && dialog.getMapObjectController() == selectedItem.getIdentityObject())
        {
            newSelection(selectedItem);
        }
    }

    public void onDialogOpening(IMapObjectPrimaryDialog<?> dialog)
    {
        final IMapObjectController controller = dialog.getMapObjectController();

        // If the dialog is opening for one of our drawing tools, then we don't need to remove it from this
        // panel since it always creates a new panel instance.
        final boolean isCurrentItem = selectedItem != null && controller == selectedItem.getIdentityObject();
        final boolean isMapTool = KeyUtils.TOOL_FAMILY.equals(controller.getFamily());

        if (isCurrentItem && isMapTool) return;

        trySelection(new MapObjectPropertiesItem(controller));
    }

    /**
     * Try to set the properties panel to the new map object selection if we have one, or clear the panel if we have
     * a {@code null} map object controller.
     *
     * @param event MapObjectSelectedEvent
     */
    @Subscribe
    @SuppressWarnings("unused")
    public void selectionChanged(MapObjectSelectedEvent event)
    {
        IMapObjectController controller = event.getMapObjectController();
        if (logger.isDebugEnabled())
        {
            logger.debug("selectionChanged({}) - was {}", event.getMapObjectController(), selectedItem == null ? " null" : selectedItem.getIdentityObject());
        }

        if (controller != null)
        {
            if (selectedItem == null || controller != selectedItem.getIdentityObject())
            {
                trySelection(new MapObjectPropertiesItem(controller));
            }
        }
    }

    /**
     * This is used by non-map objects like drawing tools before a map object is created.
     *
     * @param propertiesItem newly selected item, may be {@code null}
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void selectionChanged(IPropertiesItem propertiesItem)
    {
        trySelection(propertiesItem);
    }

    /**
     * Handle a request to remove the display of a properties item (used when non-MO properties items are removed).
     *
     * @param removePropertiesItem Item to remove
     * @since 2.37
     */
    @Subscribe
    @SuppressWarnings("unused")
    public void removeRequest(RemovePropertiesItem removePropertiesItem)
    {
        if (selectedItem != null && selectedItem.getIdentityObject() == removePropertiesItem.getIdentityObject())
        {
            noSelection();
        }
    }

    /**
     * Try to set the properties panel to the given item. We only set the properties panel
     * to the properties of the map object if we have a new selection and the selections main map object dialog isn't
     * already open.
     * <p/>
     *
     * @param propertiesItem item to process
     */
    @InvokeOnEDT
    private void trySelection(final IPropertiesItem propertiesItem)
    {
        /*
         * Only set the properties panel if we have a new selection and the selections map object dialog isn't open
         * already. We do this to avoid the issue with developers that don't return a new primary panel each time for
         * their device. If they simply return a reference, the panel will get removed from either the primary dialog
         * if it's open, or the properties panel if its open and the user then opens the map object dialog. This is a
         * result of a swing component only being able to have one parent container.
         */

        newSelection(propertiesItem);

    }

    @InvokeOnEDT
    private void noSelection()
    {
        clearPanel();
        clearSelectedItem();

        messageLabel.setIcon(UIResources.WARNING_ICON);
        messageLabel.setText(NOTHING_SELECTED_TEXT);
        openAttachments.setVisible(false);
        configureButton.setVisible(false);
        currentPanel = null;
        repaint();
    }

    @InvokeOnEDT
    private void newSelection(final IPropertiesItem propertiesItem)
    {
        clearSelectedItem();

        // If the SELECTOR tool is clicked and there IS an existing MO selection, then we want to ignore it.
        final boolean isSelector = propertiesItem.getType().equals(DrawingTools.SELECTOR.getDisplayName());
        final IMapObject selectedMapObject = mapObjectManager.getSelectedMapObject();
        final boolean ignore = isSelector && selectedMapObject != null;

        if (!ignore)
        {
            clearPanel();
            selectedItem = propertiesItem;
            currentPanel = propertiesItem.getPanel();

            configureButton.setVisible(selectedMapObject != null);

            if (currentPanel == null)
            {
                noSelection();
            } else
            {
                messageLabel.setIcon(null);
                messageLabel.setText(propertiesItem.getLabelText());

                var extraButtons = selectedMapObject != null && selectedMapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_EXTRA_BUTTONS, true);
                openAttachments.setVisible(extraButtons && selectedItem.supportsAttachments());

                // Only place the currentPanel in a scroll pane if requested
                if (selectedItem.addScrollPane())
                {
                    propertiesHolderPanel.add(propertiesPane, XY_1_1);
                    propertiesPanel.add(currentPanel, XY_1_1);
                } else
                {
                    propertiesHolderPanel.removeAll();
                    propertiesHolderPanel.add(currentPanel, XY_1_1);
                }

                notifyPropertiesItemListeners();

                revalidate();
                repaint();
            }
        }
    }

    /**
     * Cleanup previously selected item by releasing resources.
     * You have to do this since a new panel is created every time a user closes a map object dialog or clicks on
     * a device when placing the device panel in the Properties Panel.
     */
    private void clearSelectedItem()
    {
        if (null != selectedItem)
        {
            selectedItem.releaseResources();
            selectedItem = null;
            if (mapObjectAttachmentsPanel != null)
            {
                mapObjectAttachmentsPanel.releaseResources();
                spaceManager.removeComponent(mapObjectAttachmentsPanel);
                mapObjectAttachmentsPanel = null;
            }
            notifyPropertiesItemListeners();
        }
    }

    @InvokeOnEDT
    private void clearPanel()
    {
        remove(dialogOpenLabel);
        propertiesPanel.removeAll();
        propertiesHolderPanel.removeAll();
        repaint();
    }

    /** @since 5.5 */
    private void notifyPropertiesItemListeners()
    {
        ExceptionUtils.safeDoOperation(propertiesItemListeners, l -> l.propertiesItemChanged(selectedItem));
    }

    /**
     * Set panel to no selection.
     *
     * @param progressMonitor progressMonitor
     */
    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        uiEventBus.register(this);

        noSelection();
    }

    /**
     * Set panel to no selection.
     *
     * @param progressMonitor progressMonitor
     */
    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);

        noSelection();
    }

    /**
     * Set panel to no selection.
     *
     * @param progressMonitor progressMonitor
     */
    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        onClearState(progressMonitor);
    }

    @InvokeOnEDT
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        if (selectedItem != null && selectedItem.getIdentityObject() instanceof IMapObjectController controller
                && controller.getMapObject() == event.getMapObject())
        {
            noSelection();
        }
    }

    /**
     * Create and open an {@link MapObjectAttachmentsPanel} for the selected {@link IMapObject}
     * @since 5.3
     */
    private void openAttachmentsClicked()
    {
        if (selectedItem.getIdentityObject() instanceof IMapObjectController controller)
        {
            mapObjectAttachmentsPanel = new MapObjectAttachmentsPanel(controller.getMapObject(), true);

            final var componentDescriptorBuilder = new ComponentDescriptorBuilder(mapObjectAttachmentsPanel, "Attachments").setComponentPosition(ComponentPosition.RIGHT).setExternalizable(false).setPinned(true).setToolbarButtonVisible(false).setIcon(ATTACH_ICON_URL);

            spaceManager.addComponent(componentDescriptorBuilder.build(), mapObjectAttachmentsPanel);
            spaceManager.showComponent(mapObjectAttachmentsPanel);
        }
    }

    private void configureButtonActionPerformed()
    {
        if (selectedItem.getIdentityObject() instanceof IMapObjectController controller) controller.showConfigurationDialog();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var headerPanel = new JPanel();
        actionButtonPanel = new ActionButtonPanel();
        messageLabel = new JLabel();
        propertiesHolderPanel = new JPanel();
        propertiesPane = new JScrollPane();
        propertiesPanel = new JPanel();
        dialogOpenLabel = new JLabel();
        openAttachments = new JButton();
        configureButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setPreferredSize(new Dimension(300, 400));
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, fill:default:grow"));

        //======== headerPanel ========
        {
            headerPanel.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, UIManager.getColor("TableHeader.separatorColor")),
                    new EmptyBorder(5, 5, 5, 5)));
            headerPanel.setName("headerPanel");
            headerPanel.setLayout(new FormLayout(
                    "default:grow, $lcgap, default",
                    "default"));

            //---- actionButtonPanel ----
            actionButtonPanel.setBorder(null);
            actionButtonPanel.setName("actionButtonPanel");
            headerPanel.add(actionButtonPanel, cc.xy(1, 1));

            //---- messageLabel ----
            messageLabel.putClientProperty("FlatLaf.styleClass", "large");
            messageLabel.setFont(messageLabel.getFont().deriveFont(messageLabel.getFont().getStyle() | Font.BOLD));
            messageLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
            messageLabel.setName("messageLabel");
            headerPanel.add(messageLabel, cc.xy(3, 1));
        }
        add(headerPanel, cc.xy(1, 1));

        //======== propertiesHolderPanel ========
        {
            propertiesHolderPanel.setName("propertiesHolderPanel");
            propertiesHolderPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));

            //======== propertiesPane ========
            {
                propertiesPane.setOpaque(false);
                propertiesPane.setBorder(null);
                propertiesPane.setName("propertiesPane");

                //======== propertiesPanel ========
                {
                    propertiesPanel.setName("propertiesPanel");
                    propertiesPanel.setLayout(new FormLayout(
                            "default:grow",
                            "default"));
                }
                propertiesPane.setViewportView(propertiesPanel);
            }
            propertiesHolderPanel.add(propertiesPane, cc.xy(1, 1));
        }
        add(propertiesHolderPanel, cc.xy(1, 2));

        //---- dialogOpenLabel ----
        dialogOpenLabel.setText("Dialog Already Open");
        dialogOpenLabel.putClientProperty("FlatLaf.styleClass", "large");
        dialogOpenLabel.setName("dialogOpenLabel");

        //---- openAttachments ----
        openAttachments.setText("Attach");
        openAttachments.setName("openAttachments");
        openAttachments.addActionListener(e -> openAttachmentsClicked());

        //---- configureButton ----
        configureButton.setText("Configure");
        configureButton.setName("configureButton");
        configureButton.addActionListener(e -> configureButtonActionPerformed());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private ActionButtonPanel actionButtonPanel;
    private JLabel messageLabel;
    private JPanel propertiesHolderPanel;
    private JScrollPane propertiesPane;
    private JPanel propertiesPanel;
    private JLabel dialogOpenLabel;
    private JButton openAttachments;
    private JButton configureButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
