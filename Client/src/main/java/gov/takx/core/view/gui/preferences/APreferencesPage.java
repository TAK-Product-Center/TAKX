package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.icons.TakColor;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.MouseButtonUtils;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.ui.FontUtils;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * Abstract base for a page of preferences.  A page is a vertically laid out collection of tiles, one of:
 * <table border="1" style="width:100%; table-layout:fixed;">
 *    <tr><td style="width:30%"><b>Section&nbsp;Header</b></td><td>A textual separator, rendered in a larger font (usually prefacing a list of related tiles).</td></tr>
 *    <tr><td><b>Page&nbsp;Link</b></td><td>A tile with a title and description that when clicked opens and pushes another preference page.</td></tr>
 *    <tr><td><b>Preference&nbsp;Editor</b></td><td>A tile that shows the title/description/value of a preference, and allows for editing that value when clicked.</td></tr>
 *    <tr><td><b>Action</b></td><td>A tile with a title and description that when clicked invokes a specified action (like opening a dialog).</td></tr>
 *    <tr><td><b>Custom&nbsp;Panel</b></td><td>A custom panel, simply added to the layout.</td></tr>
 * </table>
 * <p>
 * Preference pages are managed as a stack, with each link action pushing a new page onto the stack.  Controls at the
 * top of each page (except the fist page of the stack) allow the user to "go back" by popping the current page and
 * restoring the view of the previous page.
 * <p>
 * Note that all editors added as tiles are wrapped in a panel with a border to support highlighting the tile
 * under the mouse.  That means that to properly hide a tile, you should call {@link JComponent#setVisible(boolean)}
 * on the {@link Container#getParent() parent} of the editor component.
 * 
 * @since 5.4
 */
@PseudoBean
public abstract class APreferencesPage extends ATitledPanel implements IResourceCleaner
{
    static final Dimension ICON_SIZE = new Dimension(32, 32);

    private static final Border SECTION_BORDER = new EmptyBorder(0, 0, 8, 0);
    private static final Border SEPARATOR_BORDER = new EmptyBorder(4, 0, 0, 0);
    private static final Border TITLED_PANEL_BORDER = new EmptyBorder(4, 0, 4, 0);
    private static final Border NO_HIGHLIGHT_BORDER = new EmptyBorder(1, 1, 1, 1);
    private static final Border HIGHLIGHT_BORDER = new LineBorder(Color.CYAN);

    @Inject
    private PreferencePagesController controller;

    @Inject
    private ICoreExecutorService executorService;

    private final AtomicInteger panelCounter = new AtomicInteger(0);
    private final Map<Integer, JComponent> actionPanels = new ConcurrentHashMap<>();
    private final Map<String, APreferenceEditorPanel> editorPanels = new ConcurrentHashMap<>();
    private final List<JComponent> sectionPanels = new ArrayList<>();
    private final List<IResourceCleaner> cleanableComponents = new ArrayList<>();  // Components that have resources

    protected final JScrollPane scrollPane = new JScrollPane();
    private final JPanel contentPanel = new JPanel();
    private NavigationPanel navigationPanel;
    private JComponent highlightedPanel;

    /**
     * @return an icon from the {@link ResourceDirectory#TAK_ASSETS_PNG} directory.
     */
    static ImageIcon assetIcon(@Nonnull String iconName)
    {
        return UIResources.getSizedIcon(TAK_ASSETS_PNG, iconName, ICON_SIZE, true);
    }

    /**
     * @return an icon from the {@link ResourceDirectory#PLATFORM_ICONS} directory.
     */
    static ImageIcon platformIcon(@Nonnull String iconName)
    {
        return UIResources.getSizedIcon(PLATFORM_ICONS, iconName, ICON_SIZE, true);
    }

    APreferencesPage(@Nonnull String title)
    {
        super(title);

        setLayout(new BorderLayout());

        contentPanel.setLayout(new VerticalLayout());

        scrollPane.setName("scroller for " + title);
        scrollPane.setViewportView(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    @PostConstruct
    private void postConstruct()
    {
        navigationPanel = new NavigationPanel();
        add(navigationPanel, BorderLayout.NORTH);
    }

    /**
     * Make this page current, updating the display and controls as needed.
     */
    public void makeCurrent()
    {
        navigationPanel.update();
    }

    /**
     * Add a section header with the given title.
     */
    protected void addSectionHeader(@Nonnull String title)
    {
        final var panel = new JPanel(new BorderLayout());
        panel.setBorder(SECTION_BORDER);
        panel.add(FontUtils.styleFont(makeResizableTextArea(title, null, scrollPane), true, 6), BorderLayout.CENTER);
        final var sectionPanel = addHighlightBorder(panel);
        sectionPanels.add(sectionPanel);
        contentPanel.add(sectionPanel);
    }

    /**
     * Add a page link tile with no icon and the given text and description.
     *
     * @param title Title text
     * @param description Description text
     * @param pageSupplier Supplier of the page to open
     */
    protected void addPageLink(@Nonnull String title, @Nonnull String description, @Nonnull Supplier<APreferencesPage> pageSupplier)
    {
        addAction(null, title, description, () -> controller.push(pageSupplier.get()));
    }

    /**
     * Add a page link tile with the given icon, text, and description.
     *
     * @param icon Optional icon
     * @param title Title text
     * @param description Description text
     * @param pageSupplier Supplier of the page to open
     * @return Unique id for the panel created for the page link
     */
    protected int addPageLink(@Nullable ImageIcon icon, @Nonnull String title, @Nonnull String description,
                              @Nonnull Supplier<APreferencesPage> pageSupplier)
    {
        return addAction(icon, title, description, () -> controller.push(pageSupplier.get()));
    }

    /**
     * Add a page link tile at the given index, with the given icon, text, and description.
     *
     * @param icon Optional icon
     * @param title Title text
     * @param description Description text
     * @param pageSupplier Supplier of the page to open
     * @param index Index at which to add the new panel
     * @return Unique id for the panel created for the page link
     * @since 5.7
     */
    protected int addPageLink(@Nullable ImageIcon icon, @Nonnull String title, @Nonnull String description,
                              @Nonnull Supplier<APreferencesPage> pageSupplier, int index)
    {
        return addAction(icon, title, description, () -> controller.push(pageSupplier.get()), index);
    }

    /**
     * Add an action tile.
     *
     * @param icon Optional icon
     * @param title Title text
     * @param description Description text
     * @param action Action to perform when the user clicks anywhere on the tile
     * @return Unique id for the panel created for the action
     */
    protected int addAction(@Nullable ImageIcon icon, @Nonnull String title, @Nonnull String description, @Nonnull Runnable action)
    {
        return addAction(icon, title, description, action, contentPanel.getComponentCount());
    }

    /**
     * Add an action tile at the specified index.
     *
     * @param icon Optional icon
     * @param title Title text
     * @param description Description text
     * @param action Action to perform when the user clicks anywhere on the tile
     * @param index Index at which to add the new panel
     * @return Unique id for the panel created for the action
     * @since 5.7
     */
    protected int addAction(@Nullable ImageIcon icon, @Nonnull String title, @Nonnull String description, @Nonnull Runnable action, int index)
    {
        final JPanel actionPanel = addHighlightBorder(new ActionPanel(icon, title, description, action));
        final int panelId = panelCounter.incrementAndGet();
        actionPanels.put(panelId, actionPanel);
        contentPanel.add(actionPanel, index);
        repaint();
        return panelId;
    }

    /**
     * Remove the action panel with the given id.
     *
     * @param panelId id of action panel to remove
     * @since 5.7
     */
    protected void removeActionPanel(int panelId)
    {
        final var actionPanel = actionPanels.remove(panelId);
        if (actionPanel != null)
        {
            contentPanel.remove(actionPanel);
            revalidate();
            repaint();
        }
    }

    /**
     * Add a preference editor tile.
     *
     * @param preferenceEntry The preference entry being edited
     * @param editorComponent The component to use for editing the preference value
     */
    protected void addEditor(@Nonnull IPreferenceEntry preferenceEntry, @Nonnull IEditorComponent editorComponent)
    {
        final var valueDisplayLabel = FontUtils.styleFont(new JLabel(""), true, 0);
        final var panel = new StandardPreferenceEditorPanel(scrollPane, preferenceEntry, editorComponent,
                valueDisplayLabel, e -> valueDisplayLabel.setText(editorComponent.getDisplayValue(preferenceEntry)));

        editorPanels.put(preferenceEntry.getKey(), panel);
        if (editorComponent instanceof IResourceCleaner cleanable) cleanableComponents.add(cleanable);

        contentPanel.add(addHighlightBorder(panel));
    }

    /**
     * Add a checkbox editor for the given preference.
     */
    protected void addCheckboxEditor(@Nonnull IPreferenceEntry preferenceEntry)
    {
        addCheckboxEditor(preferenceEntry, null);
    }

    /**
     * Add a checkbox editor for the given preference.
     *
     * @param actionHandler Optional consumer to invoke when the checkbox state changes
     */
    protected void addCheckboxEditor(@Nonnull IPreferenceEntry preferenceEntry, @Nullable Consumer<Boolean> actionHandler)
    {
        final var panel = ToggleButtonPreferenceEditorPanel.usingCheckBox(scrollPane, preferenceEntry, actionHandler);
        editorPanels.put(preferenceEntry.getKey(), panel);
        contentPanel.add(addHighlightBorder(panel));
    }

    /**
     * Add a toggle button editor for the given preference, using the given icon and text on the button.
     */
    protected void addToggleButtonEditor(@Nonnull IPreferenceEntry preferenceEntry, @Nullable String text, @Nullable Icon icon)
    {
        final var panel = ToggleButtonPreferenceEditorPanel.usingToggleButton(text, icon, scrollPane, preferenceEntry, null);
        editorPanels.put(preferenceEntry.getKey(), panel);
        contentPanel.add(addHighlightBorder(panel));
    }

    /**
     * Add a color chooser editor for the given preference, using the given list to choose from.
     *
     * @since 5.6
     */
    protected void addTakColorChooserEditor(@Nonnull IPreferenceEntry preferenceEntry, @Nonnull List<TakColor> colors)
    {
        var panel = new TakColorChooserPreferenceEditorPanel(scrollPane, preferenceEntry, colors);
        editorPanels.put(preferenceEntry.getKey(), panel);
        contentPanel.add(addHighlightBorder(panel));
    }

    /**
     * Add a custom editor for the given preference.
     *
     * @param preferenceEntry The preference entry being edited
     * @param editorComponent Component to use as the editor
     * @param action Action to perform when the title or description are clicked
     */
    protected void addCustomEditor(@Nonnull IPreferenceEntry preferenceEntry, @Nonnull JComponent editorComponent, @Nonnull Runnable action)
    {
        final var innerPanel = makeTitledPanel(preferenceEntry.getTitle(), preferenceEntry.getDescription(), editorComponent, action);
        final var panel = wrapInPanelWithSeparator(innerPanel);
        contentPanel.add(addHighlightBorder(panel));
    }

    /**
     * Add an arbitrary component as a tile.
     */
    protected void addCustomTile(@Nonnull JComponent component)
    {
        if (component instanceof IResourceCleaner cleanable) cleanableComponents.add(cleanable);
        contentPanel.add(addHighlightBorder(wrapInPanelWithSeparator(component)));
    }

    /**
     * Add a tile with the given {@code title}, {@code description}, and {@code valueComponent}.
     *
     * @param title Title text
     * @param description Description text
     * @param valueComponent Component to display in the value area (right edge) of the panel
     * @param action Action to perform when the title or description are clicked
     * @return the panel created for this tile
     */
    protected JPanel addCustomTile(@Nonnull String title, @Nonnull String description, @Nullable JComponent valueComponent, @Nullable Runnable action)
    {
        if (valueComponent instanceof IResourceCleaner cleanable) cleanableComponents.add(cleanable);
        final var panel = wrapInPanelWithSeparator(makeTitledPanel(title, description, valueComponent, action));
        contentPanel.add(addHighlightBorder(panel));
        return panel;
    }

    /**
     * Wrap a component in a panel with a border we can use to show tile highlight.
     */
    private JPanel addHighlightBorder(@Nonnull JComponent component)
    {
        final var panel = new JPanel(new BorderLayout());
        panel.add(component, BorderLayout.CENTER);
        panel.setBorder(NO_HIGHLIGHT_BORDER);
        return panel;
    }

    /**
     * Make a panel that has a layout consistent with other tiles/editors, but has no defining preference entry.
     * This can be used when editing sub-elements of preferences node.
     * 
     * @param title Title text
     * @param description Description text
     * @param valueComponent Component to display in the value area (right edge) of the panel
     * @param action Action to perform when the title or description are clicked
     * @return new panel
     */
    protected JPanel makeTitledPanel(@Nonnull String title, @Nonnull String description,
                                     @Nullable JComponent valueComponent, @Nullable Runnable action)
    {
        final var outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBorder(TITLED_PANEL_BORDER);

        final var innerPanel = new JPanel(new BorderLayout());
        final JLabel titleLabel = FontUtils.styleFont(new JLabel(title), true, 0);
        final var textArea = PreferencesComponentFactory.configureResizingTextArea(new JTextArea(description), scrollPane);

        if (action != null)
        {
            textArea.addMouseListener(MouseButtonUtils.specificButtonListener(MouseEvent.BUTTON1, action));
            titleLabel.addMouseListener(MouseButtonUtils.specificButtonListener(MouseEvent.BUTTON1, action));
        }

        innerPanel.add(titleLabel, BorderLayout.NORTH);
        innerPanel.add(textArea, BorderLayout.CENTER);

        outerPanel.add(innerPanel, BorderLayout.CENTER);
        if (valueComponent != null) outerPanel.add(valueComponent, BorderLayout.EAST);

        return outerPanel;
    }

    @RemoveObserver
    @Override
    public void releaseResources()
    {
        cleanableComponents.forEach(IResourceCleaner::releaseResources);
        cleanableComponents.clear();
        editorPanels.clear();
        sectionPanels.clear();
    }

    /**
     * @return The editor panel handling the given {@code preferenceEntry}, or {@code null} if not found
     */
    @Nullable
    protected APreferenceEditorPanel getEditorPanel(IPreferenceEntry preferenceEntry)
    {
        return editorPanels.get(preferenceEntry.getKey());
    }

    /**
     * Execute the given {@code action} on a background thread.
     */
    protected void doInBackground(@Nonnull Runnable action)
    {
        executorService.execute(() -> {
            Thread.currentThread().setName("Background task for " + title);
            action.run();
        });
    }

    /**
     * The mouse exited the whole preferences panel, clear any tile highlight.
     */
    public void mouseExited()
    {
        clearHighlight();
    }

    /**
     * Handle a mouse event and, if it is over one of our tiles, add a highlight to it.
     */
    public void mouseOver(@Nonnull MouseEvent mouseEvent)
    {
        for (var panel : contentPanel.getComponents())
        {
            if (!panel.isShowing()) continue;

            final var point = mouseEvent.getLocationOnScreen();
            SwingUtilities.convertPointFromScreen(point, panel);

            if (panel.contains(point))
            {
                highlight((JComponent) panel);
                return;
            }
        }

        clearHighlight(); // Got here if the mouse is not over any of our panels
    }

    /**
     * Add the highlight border to the given panel if it is not already highlighted.
     */
    private void highlight(JComponent panel)
    {
        if (panel != highlightedPanel)
        {
            clearHighlight();

            // Don't highlight section headers
            if (!sectionPanels.contains(panel))
            {
                panel.setBorder(HIGHLIGHT_BORDER);
                highlightedPanel = panel;
            }
        }
    }

    /**
     * Clear the highlight, if any.
     */
    private void clearHighlight()
    {
        if (highlightedPanel != null)
        {
            highlightedPanel.setBorder(NO_HIGHLIGHT_BORDER);
            highlightedPanel = null;
        }
    }

    /**
     * On a preference change, update the display value on its editor panel, if we have one.
     */
    private void onPreferenceEntryChange(@Observes PreferenceEntryChange event)
    {
        final var editorPanel = getEditorPanel(event.getPreferenceEntry());
        if (editorPanel != null) editorPanel.updateDisplayValue();
    }

    /**
     * @return a panel with the given {@code centerComponent} in the center and a separator at the bottom
     */
    private JPanel wrapInPanelWithSeparator(JComponent centerComponent)
    {
        final var panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 0, 0, 4));
        panel.add(centerComponent, BorderLayout.CENTER);
        panel.add(makeSeparatorPanel(), BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Make a text area that will resize when the specified parent changes size, and invokes the action when clicked.
     *
     * @param text Text to display
     * @param action Action to take when clicked
     * @param parent Parent to monitor for size changes
     * @return configured text component
     */
    private static JTextArea makeResizableTextArea(@Nonnull String text, @Nullable Runnable action, @Nonnull JScrollPane parent)
    {
        final var textArea = PreferencesComponentFactory.configureResizingTextArea(new JTextArea(text), parent);
        if (action != null) textArea.addMouseListener(MouseButtonUtils.specificButtonListener(MouseEvent.BUTTON1, action));

        return textArea;
    }

    private static JPanel makeSeparatorPanel()
    {
        final var panel = new JPanel(new BorderLayout());
        panel.setBorder(SEPARATOR_BORDER);
        panel.add(new JSeparator());
        return panel;
    }

    /**
     * Panel to display an optional icon, title, and description that when clicked will perform a given action.
     */
    private class ActionPanel extends JPanel
    {
        private static final Border BORDER = Borders.createEmptyBorder("4px, 4px, 4px, 0px");
        private static final Border ICON_BORDER = Borders.createEmptyBorder("0px, 0px, 0px, 4px");
        private final String title;

        private ActionPanel(@Nullable ImageIcon icon, @Nonnull String title, @Nonnull String description, @Nonnull Runnable action)
        {
            super(new BorderLayout());
            setBorder(BORDER);

            this.title = title;

            if (icon != null)
            {
                final var label = new JLabel(icon);
                label.setBorder(ICON_BORDER);
                add(label, BorderLayout.WEST);
            }

            final var mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(FontUtils.styleFont(new JLabel(title), true, 1), BorderLayout.NORTH);
            mainPanel.add(makeResizableTextArea(description, action, scrollPane), BorderLayout.CENTER);
            add(mainPanel, BorderLayout.CENTER);
            add(makeSeparatorPanel(), BorderLayout.SOUTH);

            addMouseListener(MouseButtonUtils.specificButtonListener(MouseEvent.BUTTON1, action));
        }

        @Override
        public String toString()
        {
            return "ActionPanel{title='" + title + '\'' + '}';
        }
    }

    /**
     * Displays buttons to navigate backward and forward in the page stack, enabled/disabled according to the stack contents.
     */
    private class NavigationPanel extends JPanel
    {
        private static final CellConstraints CC = new CellConstraints();
        private static final ImageIcon BACK_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "arrow_left.png");
        private static final ImageIcon FORWARD_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "arrow_right.png");

        private final JButton backButton = makeNavButton(BACK_ICON, controller::goBackward);
        private final JButton forwardButton = makeNavButton(FORWARD_ICON, controller::goForward);

        private static JButton makeNavButton(ImageIcon icon, Runnable action)
        {
            final var button = new JButton(icon);
            button.setFocusable(false);
            button.addActionListener(e -> action.run());
            return button;
        }

        private NavigationPanel()
        {
            setLayout(new FormLayout(
                    "center:default, $lcgap, center:default, $lcgap, left:default:grow",
                    "default, $lgap"));

            add(backButton, CC.xy(1, 1));
            add(forwardButton, CC.xy(3, 1));

            add(FontUtils.styleFont(new JLabel(title), false, 6), CC.xy(5, 1));
        }

        /**
         * Update the state of our buttons.
         */
        private void update()
        {
            backButton.setEnabled(controller.canGoBackward());
            forwardButton.setEnabled(controller.canGoForward());
        }
    }
}
