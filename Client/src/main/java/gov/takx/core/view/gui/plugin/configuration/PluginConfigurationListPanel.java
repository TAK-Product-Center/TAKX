package gov.takx.core.view.gui.plugin.configuration;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.Internal;
import gov.takx.api.plugin.IMutableRaptorPanel;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.commons.swing.IValidateAction;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Provides a JList of available panels that can be displayed on the left and a JPanel on the right
 * to display the user's selected panel.
 *
 * @since 1.4 (renamed from ListPanel in 5.0)
 */
@Internal
public class PluginConfigurationListPanel extends JPanel implements IResourceCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final BufferedImage DEFAULT_PANEL_ICON =
            ImageUtils.getImage(UIResources.getUrl(PLATFORM_ICONS, "settings.png"), true);
    private static final int MAX_WIDTH = (int) Math.round(ComponentUtils.getActiveMonitorScreenSize().width * .9);
    private static final int MAX_HEIGHT = (int) Math.round(ComponentUtils.getActiveMonitorScreenSize().height * .9);

    private final DefaultListModel<PanelInfo> model = new DefaultListModel<>();

    /**
     * List of panels.  Needs to be in a COWAL in case an apply action changes the visibility of the map object
     * and triggers dispose to be called on this JPanel during an iteration on the panel list.
     */
    private final List<IRaptorPanel> panels = new CopyOnWriteArrayList<>();
    private final CardLayout cardLayout;

    public PluginConfigurationListPanel()
    {
        initComponents();
        panelList.setModel(model);
        panelList.setCellRenderer(new ListItemCellRenderer());
        cardLayout = (CardLayout) panelCanvas.getLayout();
    }

    @Override
    public void releaseResources()
    {
        logger.debug("Disposing ListPanel UI Component");

        panels.forEach(panel -> {
            if (panel instanceof IResourceCleaner cleaner)
            {
                safeInvoke(cleaner, IResourceCleaner::releaseResources, panel.getTitle(),
                        "releaseResources");
            }
        });

        panelCanvas.removeAll();
        panels.clear();
        model.clear();
    }

    /**
     * Adds the mutable {@link IRaptorPanel} to the list in the requested position.
     *
     * @param panel Panel to add
     * @throws IllegalStateException when a panel with the given title already exists in the list
     */
    public void addPanel(@Nonnull IRaptorPanel panel)
    {
        addPanel(panel, getImage(panel.getIcon(), ImageUtils.SMALL_ICON_WIDTH));
    }

    /**
     * Adds a panel with a custom icon. This should only be used in cases where {@link IRaptorPanel#getIcon()} will not
     * provide the icon, e.g. {@link gov.takx.core.view.gui.maptools.ShapePropertiesEditorPanel}. Most callers should
     * use {@link #addPanel(IRaptorPanel)}.
     *
     * @param panel panel to add
     * @param imageIcon the icon to use with the panel
     * @throws IllegalStateException when a panel with the given title already exists in the list
     * */
    public void addPanel(@Nonnull IRaptorPanel panel, @Nonnull ImageIcon imageIcon)
    {
        String panelTitle = panel.getTitle();

        if (panelExists(panelTitle))
        {
            throw new IllegalStateException("Panel with the title '" + panelTitle + "' already exists");
        }

        panels.add(panel);

        final JPanel jPanel = panel.getPanel();
        panelCanvas.add(jPanel, panelTitle); // Add the panel to the CardLayout, keyed by the title
        recursivelyUpdateUI(jPanel);

        model.addElement(new PanelInfo(panelTitle, imageIcon));

        panelList.revalidate();
        panelList.repaint();
    }

    /**
     * Removes a panel from the panel list UI and model.  
     * 
     * @param configPanel the panel to remove
     * @since 5.2
     */
    public void removePanel(IRaptorPanel configPanel)
    {
        int index = findPanelByTitle(configPanel.getTitle());
        if (index >= 0)
        {
            model.removeElementAt(index);
            panels.remove(configPanel);
            panelList.revalidate();
            panelList.repaint();
        }
    }

    /**
     * Change the title of a panel, as displayed in the list of panels.  Supports an API method that lets the
     * panel names be changed on the fly without closing the containing dialog and re-displayed by removing the 
     * panel and re-adding it with the updated name.
     *
     * @param configPanel panel to rename
     * @param newTitle    new title for the panel
     * @since 5.2
     */
    public void changePanelTitle(IRaptorPanel configPanel, String newTitle)
    {
        String currentTitle = configPanel.getTitle();
        logger.debug("changePanelTitle() called for a panel title change from {} to {}", currentTitle, newTitle);

        int index = findPanelByTitle(currentTitle);
        if (index >= 0)
        {
            boolean selectFlag = panelList.isSelectedIndex(index);
            PanelInfo info = model.remove(index);
            if (info != null)
            {
                final JPanel panel = panels.get(index).getPanel();
                panelCanvas.remove(panel);
                panelCanvas.add(panel, newTitle);
                PanelInfo newInfo = new PanelInfo(newTitle, info.icon);
                model.add(index, newInfo);
                if (selectFlag) panelList.setSelectedIndex(index);
                logger.debug("Changed the panel title from {} to {}", currentTitle, newTitle);
            }
        }
    }

    /**
     * Select a panel in the panel list control by its display name, as it appears in the JList control.  The 
     * panel selection will be ignored if the panel name is not found or is null.
     * 
     * @param panelName the panel name to select
     * @since 5.2.0
     */
    public void selectPanel(@Nullable String panelName)
    {
        int index = findPanelByTitle(panelName);
        if (index >= 0) panelList.setSelectedIndex(index);
    }

    // TODO KMB: Fix for FlatLAF (TAKX-739)
    /**
     * Hit a component with a sledgehammer to make sure it has the current skin; when Substance changes the skin,
     * it doesn't get everything.  Panels that already exist but aren't attached anywhere get missed, so we need
     * to make sure we update them when they get attached.
     * <p/>
     * This is almost the same as {@link SwingUtilities#updateComponentTreeUI(java.awt.Component)}}, but it
     * adds a hack to fix an NPE in SubstanceListUI.
     *
     * @param component the component to update
     */
    private void recursivelyUpdateUI(JComponent component)
    {
        // skip JLists, substance barfs on those.
        if (!(component instanceof JList)) component.updateUI();

        for (Component child : component.getComponents())
        {
            if (child instanceof JComponent) recursivelyUpdateUI((JComponent) child);
        }
    }

    /**
     * Cycle through all the panels, select and re-pack the given owner to determine the new size for the
     * owning dialog.
     *
     * @param owner Owner to process (for pack and size operations)
     * @return new calculated dimension
     * @since 2.37
     */
    public Dimension repackPanels(Window owner)
    {
        if (!panels.isEmpty())
        {
            int width = 0;
            int height = 0;

            // Cycle through each panel, select it, repack the owner and see what the total size of the dialog is.
            for (int i = 0; i < panels.size(); i++)
            {
                setSelectedIndex(i);
                owner.pack();
                width = Math.max(width, owner.getWidth());
                height = Math.max(height, owner.getHeight());
            }

            return new Dimension(Math.min(MAX_WIDTH, width), Math.min(MAX_HEIGHT, height));
        }
        return owner.getSize();
    }

    /**
     * Displays the panel associated with the given index.
     *
     * @param index Zero-based index of panel to display
     * @throws IllegalStateException     if invoked when there are no panels defined
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public void setSelectedIndex(int index)
    {
        if (panels.isEmpty()) throw new IllegalStateException("No panels defined");

        if (index < 0 || index >= panels.size())
        {
            throw new IndexOutOfBoundsException("Index specified is out of range (" + index + ")");
        }

        logger.trace("Switching to panel index {}", index);

        panelList.setSelectedIndex(index);
        refreshPreferencePanel();
    }

    /**
     * Displays the panel associated with the given panel title.
     *
     * @param title Panel title to display
     * @throws IllegalStateException    if invoked when there are no panels defined
     * @throws IllegalArgumentException if the title is {@code null} or not found in the panel map
     */
    public void setSelectedIndex(@Nonnull String title)
    {
        if (panels.isEmpty()) throw new IllegalStateException("No panels defined");

        int index = findPanelByTitle(title);

        if (index < 0)
        {
            throw new IllegalArgumentException("No panel with the title of '" + title + "' could be located");
        } else
        {
            panelList.setSelectedIndex(index);
            refreshPreferencePanel();
        }
    }

    /**
     * Get the title of the currently selected panel.
     * 
     * @return the title for the selected panel, or null if there is no selection
     * @since 5.2
     */
    @Nullable
    public String getSelectedPanelTitle()
    {
        PanelInfo panelInfo = panelList.getSelectedValue();
        return panelInfo != null ? panelInfo.name : null;
    }

    /**
     * Case-insensitive check to determine if a panel with the given title already exists in the list.
     *
     * @param title Title to look for
     * @return true if the panel title already exists, false otherwise
     */
    public boolean panelExists(String title)
    {
        return findPanelByTitle(title) >= 0;
    }

    private int findPanelByTitle(String title)
    {
        int index = -1;

        int n = panels.size();
        for (int i = 0; i < n; i++)
        {
            if (panels.get(i).getTitle().equalsIgnoreCase(title))
            {
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * Triggers the validation routine on each panel and display a validation error message, if applicable.
     *
     * @return true if the validation was successful, false if validation failed and an error message was displayed
     */
    public boolean validateAction()
    {
        StringBuilder buf = new StringBuilder();

        for (IRaptorPanel curPanel : panels)
        {
            if (curPanel instanceof IValidateAction action)
            {
                String feedbackMessage = action.validateContents();

                if (null != feedbackMessage)
                {
                    buf.append(feedbackMessage).append("\n");
                }
            }
        }

        if (!buf.isEmpty())
        {
            JOptionPane.showMessageDialog(getParent(), buf.toString(), "Validation Errors", JOptionPane.ERROR_MESSAGE);
        }

        return (buf.isEmpty());
    }

    /**
     * Triggers the commit routine on each panel.
     */
    public void applyAction()
    {
        panels.forEach(panel -> {
            if (panel instanceof IMutableRaptorPanel mutablePanel)
            {
                safeInvoke(mutablePanel, IMutableRaptorPanel::applyAction, panel.getTitle(),
                        "applyAction");
            }
        });
    }

    /**
     * Add a list selection listener.
     *
     * @param listener Listener
     */
    public void addListSelectionListener(@Nonnull ListSelectionListener listener)
    {
        listenerList.add(ListSelectionListener.class, listener);
    }

    /**
     * Remove a list selection listener.
     *
     * @param listener Listener
     */
    public void removeListSelectionListener(ListSelectionListener listener)
    {
        listenerList.remove(ListSelectionListener.class, listener);
    }

    /**
     * Get the icon for the provided URL.
     *
     * @param url The URL of the image
     * @param maxDimension The max width/height of the image
     * @return A scaled icon for the image at the URL (respecting aspect ratio) or the {@link #DEFAULT_PANEL_ICON} if
     * the {@code URL} is {@code null} or an error occurred while trying to retrieve the image
     */
    @Nonnull
    public ImageIcon getImage(@Nullable URL url, int maxDimension)
    {
        final BufferedImage image = url != null
                ? Objects.requireNonNullElse(ImageUtils.getImage(url, false), DEFAULT_PANEL_ICON)
                : DEFAULT_PANEL_ICON;

        final BufferedImage scaledImage =
                ImageUtils.getScaledInstanceRespectingAspectRatio(image, maxDimension, maxDimension);

        return new ImageIcon(scaledImage);
    }

    private void listPanelsValueChanged(ListSelectionEvent e)
    {
        refreshPreferencePanel();

        // Notify any listeners
        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2)
        {
            if (listeners[i] == ListSelectionListener.class)
            {
                ListSelectionListener listener = ((ListSelectionListener) listeners[i + 1]);
                ListSelectionEvent event = new ListSelectionEvent(e.getSource(), e.getFirstIndex(), e.getLastIndex(), e.getValueIsAdjusting());
                listener.valueChanged(event);
            }
        }
    }

    private void refreshPreferencePanel()
    {
        int selection = panelList.getSelectedIndex();

        if (selection >= 0) cardLayout.show(panelCanvas, panels.get(selection).getTitle());
    }

    /**
     * @param panelCast The panel, cast to the required type
     * @param consumer The consumer that will act on the object
     * @param panelTitle The panel title, for error reporting
     * @param methodName The name of the method being called by the consumer, for error reporting
     * @param <T> The type of object that will be consumed
     * @since 5.0
     */
    private <T> void safeInvoke(@Nonnull T panelCast, @Nonnull Consumer<T> consumer,
                                @Nonnull String panelTitle, @Nonnull String methodName)
    {
        try
        {
            consumer.accept(panelCast);
        } catch (Exception e)
        {
            logger.error("Error calling {} on panel {}", methodName, panelTitle, e);
        }
    }

    private static class ListItemCellRenderer extends DefaultListCellRenderer
    {
        private final PluginConfigurationPanelEntry entry = new PluginConfigurationPanelEntry();

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus)
        {
            // Called up update the foreground and background colors retrieved below
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            final PanelInfo panelInfo = (PanelInfo) value;
            entry.update(panelInfo.name, panelInfo.icon, getForeground(), getBackground());
            entry.revalidate(); // required to get the horizontal scroll bar to show when the text is long

            return entry;
        }
    }

    /**
     * @param name The panel name that is also used as the key in the CardLayout
     * @param icon The panel icon
     */
    private record PanelInfo(String name, Icon icon)
    {
    }

    @SuppressWarnings("Convert2MethodRef")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var labelTitle = new JLabel();
        var scrollPaneList = new JScrollPane();
        panelList = new JList<>();
        var scrollPaneCanvas = new JScrollPane();
        panelCanvas = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "120dlu, $lcgap, default:grow",
                "default, $lgap, fill:default:grow"));

        //---- labelTitle ----
        labelTitle.setText("Select Panel:");
        labelTitle.setFont(labelTitle.getFont().deriveFont(labelTitle.getFont().getStyle() | Font.BOLD));
        labelTitle.setName("labelTitle");
        add(labelTitle, cc.xy(1, 1));

        //======== scrollPaneList ========
        {
            scrollPaneList.setName("scrollPaneList");

            //---- panelList ----
            panelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            panelList.setBorder(Borders.DLU4_BORDER);
            panelList.setName("panelList");
            panelList.addListSelectionListener(e -> listPanelsValueChanged(e));
            scrollPaneList.setViewportView(panelList);
        }
        add(scrollPaneList, cc.xy(1, 3));

        //======== scrollPaneCanvas ========
        {
            scrollPaneCanvas.setRequestFocusEnabled(false);
            scrollPaneCanvas.setName("scrollPaneCanvas");

            //======== panelCanvas ========
            {
                panelCanvas.setBorder(Borders.DLU4_BORDER);
                panelCanvas.setName("panelCanvas");
                panelCanvas.setLayout(new CardLayout());
            }
            scrollPaneCanvas.setViewportView(panelCanvas);
        }
        add(scrollPaneCanvas, cc.xy(3, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JList<PanelInfo> panelList;
    private JPanel panelCanvas;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
