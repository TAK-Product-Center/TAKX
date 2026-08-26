package gov.takx.core.view.gui.toolbar;

import com.formdev.flatlaf.FlatClientProperties;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.toolbar.IToolbarButton;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.commons.swing.SplitButton;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.toolbar.ToolbarTab;
import gov.takx.core.view.gui.menus.ApplicationMenu;
import gov.takx.core.view.gui.menus.HelpToolbarTabWrapper;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.toolbar.ToolbarButtonLabelLocation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The actual Swing implementation of the application's toolbar. It is made up of a JTabbedPane with JToolBars for the
 * individual tabs in order to emulate the type of toolbar that is familiar from RaptorX and WinTAK.
 * <p>
 * Note: there is a producer for this class, so it can be injected using CDI.
 *
 * @since 3.3
 */
// Package private so only the ToolbarProducer can instantiate the toolbar
class Toolbar extends JTabbedPane implements IToolbar
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Map of group name to button group
    private final Map<String, MultiSelectButtonGroup> buttonGroupMap = new HashMap<>();

    // List of tab ids in the order that they are displayed on the toolbar.
    private final List<UUID> tabIds = new ArrayList<>();

    // Map of tab id to a map of component ids to the actual component & model
    private final Map<UUID, Map<UUID, ComponentWithModel<?, ?>>> tabIdToComponentIdMap = new HashMap<>();

    private final IPreferenceMonitor<ToolbarButtonLabelLocation> toolbarLabelLocationMonitor =
            UserPreferenceEntry.TOOLBAR_BUTTON_LABEL_LOCATION.newMonitor(ToolbarButtonLabelLocation.class,
                    this::onToolbarLabelLocationChanged);

    public Toolbar()
    {
        // This is necessary to avoid the layout reserving space for a second row of tabs if it (erroneously) thinks
        // they may take up too much space, which results in a large blank space between the toolbar and the globe. In
        // reality, unless a user adds a ton of extra custom tabs, the minimum size of the status bar at the bottom of
        // the application is large enough that the tabs will never need to wrap or scroll.
        setTabLayoutPolicy(SCROLL_TAB_LAYOUT);

        // Adding the application menu with this property causes it to be in the same row as the tabs of the toolbar
        putClientProperty(FlatClientProperties.TABBED_PANE_LEADING_COMPONENT, new ApplicationMenu());

        JScrollableToolBar trailingToolbar = new JScrollableToolBar();
        trailingToolbar.setFloatable(false);
        trailingToolbar.add(Box.createHorizontalGlue()); // This forces the help button all the way to the right

        JButton helpButton = new JButton();
        helpButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_HELP);
        helpButton.setFocusable(false);
        helpButton.addActionListener(e -> HelpToolbarTabWrapper.openUserManual());
        trailingToolbar.add(helpButton);

        putClientProperty(FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT, trailingToolbar);
    }

    @Override
    public void setMinimized(boolean minimized)
    {
        // TODO KMB: Determine if users still want the ability to minimize the toolbar view (TAKX-1380)
    }

    @InvokeOnEDT
    @Override
    public void clear()
    {
        removeAll();
        buttonGroupMap.clear();
        tabIdToComponentIdMap.clear();
        tabIds.clear();
    }

    @InvokeAndWaitOnEDT // Using invoke and wait due to return value
    @Override
    public boolean addTab(@Nonnull ToolbarTab tabModel)
    {
        return insertTab(tabModel, getTabCount());
    }

    @InvokeAndWaitOnEDT // Using invoke and wait due to return value
    @Override
    public boolean insertTab(@Nonnull ToolbarTab tabModel, int tabIndex)
    {
        // Skip any tabs that are invalid or invisible
        if (!tabModel.isVisible() || !tabModel.isValid()) return false;

        final JScrollableToolBar jToolBar = new JScrollableToolBar();
        jToolBar.setFloatable(false);

        final UUID tabId = tabModel.getId();
        final String tabTitle = tabModel.getDisplayText();
        tabModel.getComponents().forEach(component -> addComponent(jToolBar, tabId, component));

        tabIds.add(tabIndex, tabId);
        insertTab(tabTitle, null, jToolBar, null, tabIndex);
        return true;
    }

    /**
     * Adds the provided component at the end of the tab.
     *
     * @param jToolBar The toolbar to which to add the component
     * @param tabId The id of the tab to which to add the component
     * @param componentModel The component to add
     * @throws IllegalArgumentException if the component is not one of the supported types
     * @since 3.4
     */
    private void addComponent(JScrollableToolBar jToolBar, UUID tabId, AToolbarComponent componentModel)
    {
        insertComponent(jToolBar, tabId, componentModel, jToolBar.getToolCount());
    }

    /**
     * Adds the provided component to the tab at the specified index.
     *
     * @param jToolBar The toolbar to which to add the component
     * @param tabId The id of the tab to which to add the component
     * @param componentModel The component to add
     * @param componentIndex The index on the toolbar at which to add the component
     * @throws IllegalArgumentException if the component is not one of the supported types
     * @since 3.4
     */
    private void insertComponent(JScrollableToolBar jToolBar, UUID tabId, AToolbarComponent componentModel, int componentIndex)
    {
        if (componentModel instanceof ToolbarSeparator separator)
        {
            insertSeparator(jToolBar, tabId, separator, componentIndex);
        } else if (componentModel instanceof ToolbarButton button)
        {
            insertButton(jToolBar, tabId, button, componentIndex);
        } else
        {
            throw new IllegalArgumentException("An unexpected component was provided; the tab will not be updated. Component: " + componentModel);
        }
    }

    /**
     * Adds a separator to the toolbar at the specified index.
     *
     * @param jToolBar The toolbar to which to add the separator
     * @param tabId The id of the tab to which to add the separator
     * @param separatorModel The model representing the separator to add
     * @param separatorIndex The index on the toolbar at which to add the separator
     */
    private void insertSeparator(JScrollableToolBar jToolBar, UUID tabId, ToolbarSeparator separatorModel, int separatorIndex)
    {
        final JToolBar.Separator separator = new JScrollableToolBar.Separator();
        separator.setName(separatorModel.getId().toString());
        jToolBar.insertTool(separator, separatorIndex);
        tabIdToComponentIdMap.computeIfAbsent(tabId, k -> new HashMap<>())
                .put(separatorModel.getId(), new SeparatorWithModel(separator, separatorModel));
    }

    /**
     * Adds the button to the toolbar at the specified index, assuming the button is valid.
     *
     * @param jToolBar The toolbar to which to add the button
     * @param tabId The id of the tab to which to add the button
     * @param buttonModel The model representing the button to add
     * @param buttonIndex The index on the toolbar at which to add the button
     */
    private void insertButton(JScrollableToolBar jToolBar, UUID tabId, ToolbarButton buttonModel, int buttonIndex)
    {
        if (!buttonModel.isValid()) return;

        AbstractButton button;
        if (buttonModel.isTogglable())
        {
            button = new JToggleButton();

            final Consumer<IToolbarButton> toggleDeselectListener = buttonModel.getToggleDeselectListener();
            if (toggleDeselectListener != null)
            {
                button.addItemListener(e -> {
                    if (e.getStateChange() == ItemEvent.DESELECTED) toggleDeselectListener.accept(buttonModel);
                });
            }

            final String groupName = buttonModel.getToggleButtonGroupName();
            if (groupName != null)
            {
                button.setName(buttonModel.getId().toString()); // Store the id in the name so we have a way to get it from the button

                final MultiSelectButtonGroup buttonGroup = buttonGroupMap.computeIfAbsent(groupName, (key) -> new MultiSelectButtonGroup());
                buttonGroup.add(button);
            }
        } else
        {
            final IPopupMenuProvider popupMenuProvider = buttonModel.getPopupMenuProvider();
            if (popupMenuProvider != null)
            {
                SplitButton splitButton = new SplitButton();
                splitButton.setPopupMenuProvider(popupMenuProvider);

                button = splitButton;
            } else
            {
                button = new JButton();
            }
        }

        final ButtonWithModel buttonWithModel = new ButtonWithModel(button, buttonModel);
        setButtonAttributesFromModel(button, buttonModel);
        setButtonLabelLocation(buttonWithModel, toolbarLabelLocationMonitor.getCurrentValue());
        jToolBar.insertTool(button, buttonIndex);

        // Populate the key maps to give an easy way to remove the appropriate button
        tabIdToComponentIdMap.computeIfAbsent(tabId, k -> new HashMap<>())
                .put(buttonModel.getId(), buttonWithModel);
    }

    /**
     * Sets the various attributes of the button from the model.
     *
     * @param button The actual Swing button
     * @param buttonModel The model representing the button
     */
    private void setButtonAttributesFromModel(AbstractButton button, ToolbarButton buttonModel)
    {
        final String buttonText = buttonModel.getDisplayText();
        button.setText(buttonText);
        button.setIcon(buttonModel.getIcon());
        button.setEnabled(buttonModel.isEnabled());
        button.setToolTipText(buttonModel.getTooltipText());

        // Update the button's action listeners, including removal of any listeners no longer in the model
        Arrays.stream(button.getActionListeners()).forEach(button::removeActionListener);
        buttonModel.getActionListeners().forEach(button::addActionListener);

        // Do the same for the mouse listeners, only removing those that were not added automatically by the UI
        Arrays.stream(button.getMouseListeners())
                .filter(mouseListener -> mouseListener instanceof ToolbarButton.ToolbarMouseListener)
                .forEach(button::removeMouseListener);
        buttonModel.getToolbarMouseListeners().forEach(button::addMouseListener);
    }

    /**
     * Handles an update to the button label location by changing the label location for all buttons on the toolbar.
     *
     * @param labelLocation The new button label location
     * @since 3.4
     */
    @InvokeOnEDT
    private void onToolbarLabelLocationChanged(ToolbarButtonLabelLocation labelLocation)
    {
        tabIdToComponentIdMap.values().stream()
                .flatMap(componentMap -> componentMap.values().stream())
                .filter(componentWithModel -> componentWithModel instanceof ButtonWithModel)
                .map(ButtonWithModel.class::cast)
                .forEach(buttonWithModel -> setButtonLabelLocation(buttonWithModel, labelLocation));
    }

    /**
     * Updates the location of the button's label based on the current user preference.
     *
     * @param buttonWithModel The object containing the button and associated model
     * @since 3.4
     */
    private void setButtonLabelLocation(ButtonWithModel buttonWithModel, ToolbarButtonLabelLocation labelLocation)
    {
        final var button = buttonWithModel.component();
        final String modelText = buttonWithModel.getText();
        final String modelTooltipText = buttonWithModel.model().getTooltipText();

        final boolean labelsOff = labelLocation == ToolbarButtonLabelLocation.NONE;
        final String text = labelsOff ? "" : modelText;
        final String tooltipText = labelsOff ? modelText + ": " + modelTooltipText : modelTooltipText;

        button.setText(text);
        button.setToolTipText(tooltipText);

        switch (labelLocation)
        {
            case RIGHT ->
            {
                button.setHorizontalTextPosition(RIGHT);
                button.setVerticalTextPosition(CENTER);
            }
            case BOTTOM ->
            {
                button.setHorizontalTextPosition(CENTER);
                button.setVerticalTextPosition(BOTTOM);
            }
        }
    }

    @InvokeOnEDT
    @Override
    public void removeTab(@Nonnull ToolbarTab tabModel)
    {
        final UUID tabId = tabModel.getId();
        final int index = tabIds.indexOf(tabId);
        if (index != -1)
        {
            tabModel.getComponents().forEach(component -> removeComponent(component, tabModel));
            removeTabAt(index);
            tabIds.remove(index);
            tabIdToComponentIdMap.remove(tabId);
        } else
        {
            logger.warn("No tab found matching the provided model: {}", tabModel);
        }
    }

    @Override
    @Nullable
    public UUID getSelectedTabId()
    {
        final int selectedIndex = getSelectedIndex();
        return selectedIndex != -1 ? tabIds.get(selectedIndex) : null;
    }

    @InvokeAndWaitOnEDT // Using invoke and wait due to return value
    @Override
    public boolean selectTab(@Nonnull UUID tabId)
    {
        final int tabIndex = tabIds.indexOf(tabId);
        if (tabIndex != -1)
        {
            setSelectedIndex(tabIndex);
            return true;
        } else
        {
            logger.warn("No tab found with id {}; tab selection not changed", tabId);
            return false;
        }
    }

    @Override
    public void updateTabText(@Nonnull ToolbarTab tabModel)
    {
        final UUID tabId = tabModel.getId();
        final int tabIndex = tabIds.indexOf(tabId);
        if (tabIndex != -1)
        {
            setTitleAt(tabIndex, tabModel.getDisplayText());
        } else
        {
            logger.warn("No tab found matching the provided model; tab text not changed: {}", tabModel);
        }
    }

    @InvokeOnEDT
    @Override
    public void addComponent(@Nonnull AToolbarComponent componentModel, @Nonnull ToolbarTab tabModel)
    {
        // If the tab isn't visible/valid, it won't exist on the toolbar and we won't be able to add the component
        if (!tabModel.isVisible() || !tabModel.isValid()) return;

        final UUID tabId = tabModel.getId();
        final JScrollableToolBar jToolBar = getToolBarForTab(tabId);
        addComponent(jToolBar, tabId, componentModel);
    }

    @InvokeOnEDT
    @Override
    public void insertComponent(@Nonnull AToolbarComponent componentModel, @Nonnull ToolbarTab tabModel, int componentIndex)
    {
        // If the tab isn't visible/valid, it won't exist on the toolbar and we won't be able to insert the component
        if (!tabModel.isVisible() || !tabModel.isValid()) return;

        final UUID tabId = tabModel.getId();
        final JScrollableToolBar jToolBar = getToolBarForTab(tabId);
        insertComponent(jToolBar, tabId, componentModel, componentIndex);
    }

    /**
     * Gets the {@link JScrollableToolBar} that contains all the buttons for the tab.
     *
     * @param tabId The id of the tab for which to get the Swing toolbar
     * @return {@link JScrollableToolBar} for the tab
     * @throws IllegalArgumentException if no tab exists on the toolbar for the provided id
     */
    @Nonnull
    private JScrollableToolBar getToolBarForTab(@Nonnull UUID tabId)
    {
        final int index = tabIds.indexOf(tabId);
        if (index == -1) throw new IllegalArgumentException("No tab with the provided id exists on the toolbar: " + tabId);

        return (JScrollableToolBar) getComponentAt(index);
    }

    @InvokeOnEDT
    @Override
    public void removeComponent(@Nonnull AToolbarComponent componentModel, @Nonnull ToolbarTab tabModel)
    {
        final UUID tabId = tabModel.getId();
        final JScrollableToolBar jToolBar = getToolBarForTab(tabId);

        final var componentWithModel = tabIdToComponentIdMap.get(tabId).remove(componentModel.getId());

        // component might be null if it was previously soft-deleted
        if (componentWithModel != null) jToolBar.removeTool(componentWithModel.component());

        if (componentWithModel instanceof ButtonWithModel buttonWithModel)
        {
            final AbstractButton button = buttonWithModel.component();
            final String groupName = buttonWithModel.model().getToggleButtonGroupName();
            if (groupName != null)
            {
                final ButtonGroup buttonGroup = buttonGroupMap.get(groupName);
                buttonGroup.remove(button);

                if (buttonGroup.getButtonCount() == 0)
                {
                    buttonGroupMap.remove(groupName);
                }

                // Remove any item listeners that were added during button creation
                Arrays.stream(button.getItemListeners()).forEach(button::removeItemListener);
            }
        }
    }

    @InvokeOnEDT
    @Override
    public void updateButton(@Nonnull ToolbarButton buttonModel)
    {
        final UUID buttonId = buttonModel.getId();
        tabIdToComponentIdMap.values().stream()
                .filter(map -> map.containsKey(buttonId))
                .map(map -> map.get(buttonId))
                .map(ButtonWithModel.class::cast) // Since the source is a button, it's safe to assume any matches will be buttons
                .forEach(buttonWithModel -> setButtonAttributesFromModel(buttonWithModel.component(), buttonModel));
    }

    @InvokeAndWaitOnEDT // Using invoke and wait due to return value
    @Override
    public boolean isAnyButtonSelectedInGroup(@Nonnull String groupName)
    {
        final MultiSelectButtonGroup buttonGroup = buttonGroupMap.get(groupName);

        if (buttonGroup == null) throw new IllegalArgumentException("No button group found for name " + groupName);

        return buttonGroup.isAnyButtonSelectedInGroup();
    }

    @InvokeOnEDT
    @Override
    public void setSelectedButtonInGroup(@Nullable UUID buttonId, @Nonnull String groupName)
    {
        final MultiSelectButtonGroup buttonGroup = buttonGroupMap.get(groupName);

        if (buttonGroup != null)
        {
            buttonGroup.setSelectedButtonId(buttonId, buttonId != null);
        } else
        {
            logger.warn("No button group found for name {}; button with id {} not selected", groupName, buttonId);
        }
    }

    @Override
    public boolean getToggleButtonState(@Nonnull UUID buttonId)
    {
        // All the buttons with this id will have the same state, so just get the first one
        final AbstractButton button = getStreamOfAllButtonsWithId(buttonId).findFirst().orElse(null);
        return button != null && button.isSelected();
    }

    @InvokeOnEDT
    @Override
    public void setToggleButtonState(@Nonnull UUID buttonId, boolean selected)
    {
        // We want to set all instances of this button as selected, regardless of whether they are on the current tab
        getStreamOfAllButtonsWithId(buttonId)
                .forEach(button -> button.setSelected(selected));
    }

    /**
     * Gets a stream of all buttons with the provided id on any tab.
     *
     * @param buttonId The button id for which to get the stream
     * @return Stream of buttons with the provided key.
     */
    private Stream<AbstractButton> getStreamOfAllButtonsWithId(@Nonnull UUID buttonId)
    {
        return tabIdToComponentIdMap.values().stream()
                .map(componentMap -> componentMap.get(buttonId))
                .filter(Objects::nonNull)
                .map(ButtonWithModel.class::cast)
                .map(ComponentWithModel::component);
    }

    @InvokeOnEDT
    @Override
    public void doButtonClick(@Nonnull UUID buttonId)
    {
        // TODO KMB: If the user removes a button from the toolbar, programmatic clicks won't work anymore for that button (TAKX-1499)
        // There could be multiple tabs with a matching button. We just need to find one.
        getStreamOfAllButtonsWithId(buttonId)
                .findFirst()
                .ifPresentOrElse(b -> {
                    b.doClick(0);
                    // Pass a left-click with the current time and a location of 0,0 to all mouse listeners
                    final MouseEvent mouseEvent = new MouseEvent(b, MouseEvent.BUTTON1, System.currentTimeMillis(),
                            0, 0, 0, 1, false);
                    Arrays.stream(b.getMouseListeners()).forEach(ml -> ml.mouseClicked(mouseEvent));
                },
                        () -> logger.warn("No button found for id {}; button not clicked", buttonId));
    }

    @InvokeAndWaitOnEDT // Using invoke and wait due to return value
    @Override
    public boolean isPointOnButton(@Nonnull IToolbarButton buttonModel, @Nonnull Point point)
    {
        final UUID buttonId = buttonModel.getId();

        // We only care about the buttons on the currently visible tab
        final UUID selectedTabId = getSelectedTabId();
        final ButtonWithModel buttonWithModel = (ButtonWithModel) tabIdToComponentIdMap.get(selectedTabId).get(buttonId);

        if (buttonWithModel != null)
        {
            final AbstractButton button = buttonWithModel.component();
            final Point relativePoint = new Point(point);
            SwingUtilities.convertPointFromScreen(relativePoint, button);
            return button.contains(relativePoint);
        } else
        {
            logger.warn("No button found for id {}", buttonId);
        }

        return false;
    }

    /**
     * Custom button group that allows all buttons with the same button id to be selected at once. This is necessary in
     * case a button exists on multiple tabs.
     */
    private static class MultiSelectButtonGroup extends ButtonGroup
    {
        private final Map<String, List<ButtonModel>> idStringToModelMap = new HashMap<>();
        private final Map<ButtonModel, String> modelToIdStringMap = new HashMap<>();
        private String selectedButtonIdString;
        private boolean updatingSelectionStates;

        @Override
        public void add(@Nonnull AbstractButton button)
        {
            // We stored the id as a string in the name field during creation
            final String buttonIdString = button.getName();
            final ButtonModel buttonModel = button.getModel();
            buttons.addElement(button);
            idStringToModelMap.computeIfAbsent(buttonIdString, k -> new ArrayList<>()).add(buttonModel);

            // There is no easy way to figure out the parent button or id from the button model, so maintain a map
            modelToIdStringMap.put(buttonModel, buttonIdString);

            button.setSelected(buttonIdString.equals(selectedButtonIdString));

            buttonModel.setGroup(this);
        }

        @Override
        public void remove(@Nonnull AbstractButton button)
        {
            final String buttonIdString = button.getName();
            final ButtonModel buttonModel = button.getModel();
            buttons.removeElement(button);
            final List<ButtonModel> buttonModelsWithId = idStringToModelMap.get(buttonIdString);
            buttonModelsWithId.remove(buttonModel);
            modelToIdStringMap.remove(buttonModel);

            if (buttonIdString.equals(selectedButtonIdString))
            {
                // If this was the only button with the key, clear selection
                if (buttonModelsWithId.isEmpty()) selectedButtonIdString = null;
            }

            buttonModel.setGroup(null);
        }

        @Override
        public void clearSelection()
        {
            if (isAnyButtonSelectedInGroup())
            {
                String oldSelectedIdString = selectedButtonIdString;
                selectedButtonIdString = null;
                idStringToModelMap.get(oldSelectedIdString).forEach(button -> button.setSelected(false));
            }
        }

        @Override
        public ButtonModel getSelection()
        {
            // We get lucky here because as far as I can tell, calls to this method in JToggleButton just check for
            // null. It uses isSelected() to check for selection of a specific button model. So, just return any of the
            // selected buttons. If that ever changes, we'll need to create a custom version of JToggleButton.
            return isAnyButtonSelectedInGroup() ? idStringToModelMap.get(selectedButtonIdString).stream().findFirst().orElse(null) : null;
        }

        /**
         * @return True if any button in the group is selected.
         * @since 3.4
         */
        public boolean isAnyButtonSelectedInGroup()
        {
            return selectedButtonIdString != null;
        }

        /**
         * Sets all buttons with the specified button id to the provided selection state.
         *
         * @param buttonId The button id to update
         * @param selected  The desired selection state for buttons with the specified key
         */
        public void setSelectedButtonId(UUID buttonId, boolean selected)
        {
            if (selected)
            {
                idStringToModelMap.get(buttonId.toString()).forEach(model -> model.setSelected(true));
            } else
            {
                clearSelection();
            }
        }

        @Override
        public void setSelected(ButtonModel model, boolean selected)
        {
            if (updatingSelectionStates) return; // Avoid recursively updating the group as each button is updated

            updatingSelectionStates = true;
            if (selected)
            {
                if (model != null)
                {
                    final String buttonKey = modelToIdStringMap.get(model);
                    if (!buttonKey.equals(selectedButtonIdString))
                    {
                        String oldSelectionKey = selectedButtonIdString;
                        selectedButtonIdString = buttonKey;
                        if (oldSelectionKey != null)
                        {
                            idStringToModelMap.get(oldSelectionKey).forEach(buttonModel -> buttonModel.setSelected(false));
                        }

                        model.setSelected(true);
                    }
                }
            } else
            {
                clearSelection();
            }

            updatingSelectionStates = false;
        }

        @Override
        public boolean isSelected(ButtonModel model)
        {
            final String buttonKey = modelToIdStringMap.get(model);
            return buttonKey.equals(selectedButtonIdString);
        }
    }

    /**
     * Wrapper to hold the actual Swing component along with its associated model. This is necessary to support the
     * customization of toolbar button label locations, since the only way to hide text when the user selects the
     * option for no button labels is to clear the text field on the button, and we need a way to get the text back if
     * they select to show the labels later.
     *
     * @since 3.4
     */
    private static class ComponentWithModel<C extends JComponent, M extends AToolbarComponent>
    {
        private final C component;
        private final M model;

        /**
         * @param component The actual Swing component
         * @param model The associated component model
         */
        private ComponentWithModel(C component, M model)
        {
            this.component = component;
            this.model = model;
        }

        public C component()
        {
            return component;
        }

        public M model()
        {
            return model;
        }
    }

    /**
     * @since 3.4
     */
    private static final class ButtonWithModel extends ComponentWithModel<AbstractButton, ToolbarButton>
    {
        private ButtonWithModel(AbstractButton component, ToolbarButton model)
        {
            super(component, model);
        }

        /**
         * @return The text to be displayed as the button label.
         */
        private String getText()
        {
            return model().getDisplayText();
        }
    }

    /**
     * @since 3.4
     */
    private static final class SeparatorWithModel extends ComponentWithModel<JSeparator, ToolbarSeparator>
    {
        private SeparatorWithModel(JSeparator component, ToolbarSeparator model)
        {
            super(component, model);
        }
    }
}
