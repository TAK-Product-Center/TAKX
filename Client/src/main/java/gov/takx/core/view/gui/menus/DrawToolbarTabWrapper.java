package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.EditorFailedEvent;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import gov.takx.core.shapes.ShapeDrawingService.DrawToolListener;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarButtonBuilder;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.DrawingTools;
import gov.takx.core.view.gui.maptools.AnnotationPanel;
import gov.takx.core.view.gui.maptools.DrawingToolsPropertiesItem;
import gov.takx.core.view.gui.maptools.FlatAnnotationPanel;
import gov.takx.core.view.gui.maptools.IToolEditorPanel;
import gov.takx.core.view.gui.maptools.RangeFanEditorPanel;
import gov.takx.core.view.gui.maptools.ShapePropertiesEditorPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.function.Supplier;

/**
 */
@PseudoBean
public class DrawToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String DRAW_BUTTON_GROUP_NAME = TabType.DRAW.getDisplayText();

    private static final String ARROW_TOOLTIP = "Draw a Range & Bearing arrow on the globe.";
    private static final String CIRCLE_TOOLTIP = "Draw a circle on the globe.";
    private static final String ELLIPSE_TOOLTIP = "Draw an ellipse on the globe.";
//    private static final String GEO_PING_TOOLTIP = "Draw a ping alert on the globe."; // TODO KMB: Uncomment if geo-ping re-added
    private static final String POLYGON_TOOLTIP = "Draw a polygon on the globe.";
    private static final String FREEHAND_POLYGON_TOOLTIP = "Draw a freehand polygon on the globe.";
    private static final String POLYLINE_TOOLTIP = "Draw a polyline on the globe.";
    private static final String FREEHAND_POLYLINE_TOOLTIP = "Draw a freehand polyline on the globe.";
    private static final String RANGE_RINGS_TOOLTIP = "Draw range rings on the globe.";
    private static final String RECTANGLE_TOOLTIP = "Draw a rectangle on the globe.";
    private static final String SELECTOR_TOOLTIP = "Change tool back to a selector.";
    private static final String SQUARE_TOOLTIP = "Draw a square on the globe.";
    private static final String TEXT_BOX_TOOLTIP = "Enter text into a text box on the globe.";
    private static final String TEXT_LABEL_TOOLTIP = "Enter text into a text label on the globe.";

    private static final String RUBBERBAND_INSTRUCTIONS = "Press and drag to create the shape";
    private static final String VERTEX_INSTRUCTIONS = "Click to add vertices; double-click to finish the shape";

    private final DrawToolListener drawToolListener = new DrawToolListener(this::disableDrawingTool);
    private final UUID selectorButtonId = ButtonType.SELECTOR.requireSingularId();
//    private final PingPanel pingPanel = new PingPanel();

    private IToolEditorPanel currentEditorPanel;
    private DrawingTools currentDrawingTool = DrawingTools.SELECTOR;
    private boolean ignoreSelectionChange = false;

    @PostConstruct
    private void postConstruct()
    {
        createTab(TabType.DRAW, true);
    }

    @Override
    protected void addComponents()
    {
        tab.addComponent(createDrawToolButton(ButtonType.SELECTOR, getSelectorActionListener(), SELECTOR_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.DRAW, tab.getComponents().size()));

        tab.addComponent(createDrawToolButton(ButtonType.ARROW, getShapeActionListener(MapToolType.DRAWING_ARROW, RUBBERBAND_INSTRUCTIONS), ARROW_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.POLYLINE, getShapeActionListener(MapToolType.DRAWING_PATH, VERTEX_INSTRUCTIONS), POLYLINE_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.FREEHAND_POLYLINE, getShapeActionListener(MapToolType.DRAWING_FREEHAND_PATH, RUBBERBAND_INSTRUCTIONS), FREEHAND_POLYLINE_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.SQUARE, getShapeActionListener(MapToolType.DRAWING_SQUARE, RUBBERBAND_INSTRUCTIONS), SQUARE_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.RECTANGLE, getShapeActionListener(MapToolType.DRAWING_QUAD, RUBBERBAND_INSTRUCTIONS), RECTANGLE_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.POLYGON, getShapeActionListener(MapToolType.DRAWING_POLYGON, VERTEX_INSTRUCTIONS), POLYGON_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.FREEHAND_POLYGON, getShapeActionListener(MapToolType.DRAWING_FREEHAND_POLYGON, RUBBERBAND_INSTRUCTIONS), FREEHAND_POLYGON_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.CIRCLE, getShapeActionListener(MapToolType.DRAWING_CIRCLE, RUBBERBAND_INSTRUCTIONS), CIRCLE_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.ELLIPSE, getShapeActionListener(MapToolType.DRAWING_ELLIPSE, RUBBERBAND_INSTRUCTIONS), ELLIPSE_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.DRAW, tab.getComponents().size()));

        tab.addComponent(createDrawToolButton(ButtonType.TEXT_BOX,
                makeDrawButtonActionListener(Cursor.TEXT_CURSOR, DrawingTools.ANNOTATION, AnnotationPanel::new,
                        DrawingTools.ANNOTATION.toString(),
                        () -> getLastFocusedGlobe().setMapTool(MapToolType.DRAWING_ANNOTATION)),
                TEXT_BOX_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.TEXT_LABEL,
                makeDrawButtonActionListener(Cursor.TEXT_CURSOR, DrawingTools.FLAT_ANNOTATION, FlatAnnotationPanel::new,
                        DrawingTools.FLAT_ANNOTATION.toString(),
                        () -> getLastFocusedGlobe().setMapTool(MapToolType.DRAWING_FLAT_ANNOTATION)),
                TEXT_LABEL_TOOLTIP));
        tab.addComponent(createDrawToolButton(ButtonType.RANGE_RINGS,
                makeDrawButtonActionListener(Cursor.CROSSHAIR_CURSOR, DrawingTools.RANGE_FAN, RangeFanEditorPanel::new,
                        MapToolType.DRAWING_RANGE_FAN.getDisplayName(),
                        () -> getLastFocusedGlobe().setMapTool(MapToolType.DRAWING_RANGE_FAN)),
                RANGE_RINGS_TOOLTIP));

        // TODO KMB: if we ever re-enable geo-ping, uncomment this code to add the geo-ping button to the toolbar
//        tab.addComponent(createDrawToolButton(ButtonType.GEO_PING,
//                makeDrawButtonActionListener(Cursor.CROSSHAIR_CURSOR, DrawingTools.PING, () -> pingPanel,
//                        DrawingTools.PING.toString(), null),
//                GEO_PING_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.DRAW, tab.getComponents().size()));
    }

    /**
     * Creates a button, adds it to list of buttons, and adds it to the toggle group.
     *
     * @param buttonType The type of button to create
     * @param actionListener The action listener to be notified when the button is clicked
     * @param tooltipText The tooltip text for the button
     * @since 3.4
     */
    @Nonnull
    private ToolbarButton createDrawToolButton(@Nonnull ButtonType buttonType, @Nonnull ActionListener actionListener,
                                               @Nullable String tooltipText)
    {
        return ToolbarButtonBuilder.createCoreButtonBuilder(buttonType, buttonType.requireDisplayText())
                .addActionListener(actionListener)
                .setTogglable(true)
                .setToggleButtonGroupName(DRAW_BUTTON_GROUP_NAME)
                .setTooltipText(tooltipText)
                .build();
    }

    /**
     * Adds an action listener to a draw tool button contributed by a plugin.
     *
     * @param toolbarButton the draw tool button to which to add the listener
     */
    public void addListenerToPluginDrawTool(@Nonnull ToolbarButton toolbarButton)
    {
        // Add additional action listener which allows TAKX to process the button action in addition to the plugin.
        toolbarButton.addActionListener(new PluginToggleButtonActionListener());
    }

    /**
     * @return Action listener for a given tool type
     * @since 3.3
     */
    @Nonnull
    private ActionListener getShapeActionListener(@Nonnull MapToolType mapToolType, @Nonnull String instructions)
    {
        return makeDrawButtonActionListener(Cursor.CROSSHAIR_CURSOR, DrawingTools.SHAPE,
                () -> new ShapePropertiesEditorPanel(mapToolType),
                mapToolType.getDisplayName(),
                () -> {
                    final var lastFocusedGlobe = getLastFocusedGlobe();
                    lastFocusedGlobe.setMapTool(mapToolType);
                    globeManager.showGlobeMessage(instructions, lastFocusedGlobe);
                });
    }

    /**
     * @return Action listener for the select button
     * @since 2.37
     */
    @Nonnull
    private ActionListener getSelectorActionListener()
    {
        return new DrawingToolsButtonActionListener(DrawingTools.SELECTOR, Cursor.DEFAULT_CURSOR)
        {
            @Override
            protected void buttonActionPerformed()
            {
                clearPropertiesPanel();
            }
        };
    }

    /**
     * Create an action listener to handle one of our tool buttons being pressed.
     *
     * @param cursor The cursor to install when pressed
     * @param drawingTool The tool being invoked
     * @param editorPanelSupplier Supplier to obtain the editor/properties panel for the tool
     * @param toolName Name for the tool
     * @param additionalSetup Additional steps to perform before firing the {@link DrawingToolsPropertiesItem} event
     * @return action listener
     * @since 2.37
     */
    @Nonnull
    private ActionListener makeDrawButtonActionListener(int cursor, @Nonnull DrawingTools drawingTool,
                                                        @Nonnull Supplier<IToolEditorPanel> editorPanelSupplier,
                                                        String toolName, @Nullable Runnable additionalSetup)
    {
        return new DrawingToolsButtonActionListener(drawingTool, cursor)
        {
            @Override
            protected void buttonActionPerformed()
            {
                currentEditorPanel = editorPanelSupplier.get();
                final JPanel panel = currentEditorPanel.getPanel();

                setSelectListenersArmedForMapTool(drawingTool);

                if (additionalSetup != null) additionalSetup.run();

                uiEventBus.post(new DrawingToolsPropertiesItem(panel, toolName));
            }
        };
    }

    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        clearPropertiesPanel();
        clearEditorPanel();

        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);  // Un-wire event processing
    }

    private void onProjectOpened(@Observes ProjectOpened ignored)
    {
        selectSelector(); // The selection tool should be selected by default

        uiEventBus.register(this);  // Wire up event processing
    }

    /**
     * Clear the state of the current editor panel.
     * @since 2.37
     */
    private void clearEditorPanel()
    {
        if (currentEditorPanel != null)
        {
            currentEditorPanel.releaseResources();
            currentEditorPanel = null;
        }
    }

    /**
     * Clean up if drawing failed.
     *
     * @param failedEvent The failure event
     * @since 5.1
     */
    @Subscribe
    @SuppressWarnings("unused")
    public void editorFailedEvent(EditorFailedEvent failedEvent)
    {
        clearPropertiesPanel();
        clearEditorPanel();
    }

    private void clearPropertiesPanel()
    {
        globeManager.hideGlobeMessage();
        getLastFocusedGlobe().setMapTool(MapToolType.NONE);
        setSelectListenersArmedForMapTool(DrawingTools.SELECTOR);
        selectSelector();

        uiEventBus.post(new DrawingToolsPropertiesItem(null, DrawingTools.SELECTOR.getDisplayName()));
    }

    private void setSelectListenersArmedForMapTool(DrawingTools drawingTool)
    {
        logger.debug("arming {}", drawingTool);

        globeManager.setSelectListenersActive(drawingTool == DrawingTools.SELECTOR);
    }

    /**
     * Set the Selector to "selected".
     * @since 2.37
     */
    private void selectSelector()
    {
        toolbar.setSelectedButtonInGroup(selectorButtonId, DRAW_BUTTON_GROUP_NAME);
    }

    /**
     * Set the selector button as the current selection if we have a button selected that isn't already the selection tool.
     */
    void disableDrawingTool()
    {
        toolbar.doButtonClick(selectorButtonId);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void selectionChanged(MapObjectSelectedEvent event)
    {
        if (ignoreSelectionChange) return;

        clearEditorPanel();

        if (currentDrawingTool != DrawingTools.PLUGIN_TOOL && currentDrawingTool != DrawingTools.SELECTOR)
        {
            disableDrawingTool();
        }
    }

    private abstract class DrawingToolsButtonActionListener implements ActionListener
    {
        private final DrawingTools drawingTool;
        private final Cursor toolCursor;

        protected DrawingToolsButtonActionListener(@Nonnull DrawingTools drawingTool, int cursor)
        {
            this.drawingTool = drawingTool;
            toolCursor = Cursor.getPredefinedCursor(cursor);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean anyButtonSelectedInGroup = toolbar.isAnyButtonSelectedInGroup(DRAW_BUTTON_GROUP_NAME);

            logger.debug("Action: {}, button selected: {}, CDT: {}", e, anyButtonSelectedInGroup, currentDrawingTool);

            if (!anyButtonSelectedInGroup)
            {
                if (currentDrawingTool == DrawingTools.SELECTOR)
                {
                    selectSelector(); // Just make it selected again
                } else
                {
                    disableDrawingTool(); // When a toggle button is deselected we want to "click" the selector button
                }
            } else
            {
                currentDrawingTool = drawingTool;
                if (currentDrawingTool != DrawingTools.PLUGIN_TOOL) activateCoreDrawTool();
            }
        }

        /**
         * Activate the current core drawing tool.
         */
        private void activateCoreDrawTool()
        {
            logger.debug("invoke tool: {}", currentDrawingTool);

            clearEditorPanel();

            getLastFocusedGlobe().setMapTool(MapToolType.NONE);
            globeManager.setCursorOnAllGlobes(toolCursor);

            ignoreSelectionChange = true;
            mapObjectManager.setSelected(null); // Deselect any currently selected map objects
            ignoreSelectionChange = false;

            buttonActionPerformed(); // Invoke the draw tools action

            if (currentDrawingTool != DrawingTools.SELECTOR)
            {
                uiSpaceManager.showComponent(CoreComponentType.PROPERTIES_PANEL);
                drawToolListener.attach();
            } else
            {
                drawToolListener.detach();
            }
        }

        protected abstract void buttonActionPerformed();
    }

    /**
     * Action listener for the plugin contributed buttons to be able to make the callback to the
     * plugins that a button has been deselected.
     */
    private class PluginToggleButtonActionListener extends DrawingToolsButtonActionListener
    {
        private PluginToggleButtonActionListener()
        {
            super(DrawingTools.PLUGIN_TOOL, Cursor.DEFAULT_CURSOR);
        }

        @Override
        protected void buttonActionPerformed()
        {
            // Nothing to do here since the plugin will be notified of the button being deselected via the callback that
            // was provided on button creation.
        }
    }
}
