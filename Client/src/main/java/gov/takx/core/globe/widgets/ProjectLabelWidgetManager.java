package gov.takx.core.globe.widgets;

import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.platform.widgets.TextWidget;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IProjectLabel;
import gov.takx.api.globe.IWidget;
import gov.takx.api.globe.IWidgetLayerManager;
import gov.takx.api.globe.ProjectLabelLocation;
import gov.takx.api.globe.WidgetLayoutPosition;
import gov.takx.commons.swing.ColorChooserUtils;
import gov.takx.core.globe.tak.WidgetLayerManager;
import gov.takx.platform.globe.GlobeTextUtils;
import gov.takx.platform.globe.ProjectLabel;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.awt.Color;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Manager of widget displays for {@link IProjectLabel}s.
 *
 * @since 5.2
 */
@ApplicationScoped
public class ProjectLabelWidgetManager
{
    private static final IProjectLabel MISSING_LABEL = new ProjectLabel("");

    private static final Map<ProjectLabelLocation, WidgetLayoutPosition> LOCATION_TO_POSITION = Map.of(
            ProjectLabelLocation.UPPER_LEFT, WidgetLayoutPosition.TOP_LEFT,
            ProjectLabelLocation.UPPER_CENTER, WidgetLayoutPosition.TOP_EDGE,
            ProjectLabelLocation.UPPER_RIGHT, WidgetLayoutPosition.TOP_RIGHT,
            ProjectLabelLocation.LOWER_LEFT, WidgetLayoutPosition.BOTTOM_LEFT,
            ProjectLabelLocation.LOWER_CENTER, WidgetLayoutPosition.BOTTOM_EDGE,
            ProjectLabelLocation.LOWER_RIGHT, WidgetLayoutPosition.BOTTOM_RIGHT);

    private static final List<ProjectLabelLocation> LABEL_LOCATIONS = List.of(ProjectLabelLocation.values());

    private final Map<ProjectLabelLocation, ProjectLabelGlobeWidgetManager> adapterMap = new EnumMap<>(ProjectLabelLocation.class);

    private final Function<ProjectLabelLocation, IWidgetLayerManager.IWidgetFactory> factoryGenerator = location -> globe -> {
        final var adapter = adapterMap.get(location);
        if (adapter == null) return null;

        final var labelWidget = new LabelWidget(globe, location);
        adapter.addWidget(globe, labelWidget);
        return labelWidget;
    };

    /**
     * We need to clear the widget label map when the project closes to prevent preference updating calls
     * from actually doing anything. If we do not clear the map attempts to update dead/locked preference
     * nodes will throw exceptions.
     */
    @SuppressWarnings("unused")
    private void onProjectClosing(@Observes @Nonnull ProjectClosing ignored)
    {
        // Clear label/widgets. We'll remake them when opening a new project.
        adapterMap.clear();
    }

    /**
     * Initialize preference access and label widgets for each {@link ProjectLabelLocation}.
     */
    @SuppressWarnings("unused")
    private void onProjectOpen(@Observes @Nonnull ProjectOpened ignored)
    {
        // When opening a project we initialize a reference to the label preference node, and create widget adapters
        final var labelConfigurationPreferences = ProjectPreferenceEntry.PROJECT_LABELS_NODE.getNodeAs(LabelConfigurationPreferences.class, true);

        // Create widget adapters for each location.
        for (var location : LABEL_LOCATIONS)
        {
            adapterMap.put(location, new ProjectLabelGlobeWidgetManager(labelConfigurationPreferences.getLabelConfiguration(location)));
        }
    }

    /**
     * Registers widget factories for each {@link ProjectLabelLocation}. Must be called before other widget factories are registered
     * in order to ensure that the labels are placed as close to their intended locations as possible without being offset by other
     * widgets.
     *
     * @param widgetLayerManager Layout manager to register our factories within.
     */
    public void registerWidgetFactories(@Nonnull WidgetLayerManager widgetLayerManager)
    {
        // TODO TAKX-2406: Investigate if we wanna tweak how this is registered when priority handling is supported.

        // We'll add one factory for each project label location since one widget cannot be laid out to cover each desired location.
        LABEL_LOCATIONS.forEach(location -> widgetLayerManager.addWidgetFactory(factoryGenerator.apply(location), LOCATION_TO_POSITION.get(location)));
    }

    /**
     * Get a {@link ProjectLabelGlobeWidgetManager} representing the configuration for the requested location.
     *
     * @param location Label location.
     * @return Project label instance for that location.
     */
    @Nonnull
    public IProjectLabel getProjectLabel(@Nonnull ProjectLabelLocation location)
    {
        IProjectLabel label = adapterMap.get(location);
        if (label == null) label = MISSING_LABEL; // Only can happen if called while no project is open, which should never happen
        return label;
    }

    /**
     * Set the project label data for the given location.
     *
     * @param location Label location to update.
     * @param label    Label configuration to install.
     */
    public void setProjectLabel(@Nonnull ProjectLabelLocation location, @Nonnull IProjectLabel label)
    {
        final var adapter = adapterMap.get(location);
        if (adapter != null) adapter.applyFromLabel(label);
    }

    /**
     * @return {@code true} when any of the {@link IProjectLabel}s are visible.
     */
    public boolean isVisible()
    {
        return adapterMap.values().stream().anyMatch(IProjectLabel::isVisible);
    }

    /**
     * Set the visibility of all contained {@link IProjectLabel}s.
     *
     * @param visible Visibility to set.
     */
    public void setVisible(boolean visible)
    {
        LABEL_LOCATIONS.forEach(location -> setVisible(location, visible));
    }

    /**
     * Set the visibility of a specific {@link IProjectLabel} by its location.
     *
     * @param location Label location to update.
     * @param visible Visibility to set.
     */
    public void setVisible(@Nonnull ProjectLabelLocation location, boolean visible)
    {
        final var adapter = adapterMap.get(location);
        if (adapter != null) adapter.setVisible(visible);
    }

    void removeLabelWidget(@Nonnull LabelWidget labelWidget)
    {
        final var adapter = adapterMap.get(labelWidget.location);
        if (adapter != null) adapter.removeWidget(labelWidget.globe);
    }

    /**
     * A label implementation that tracks/updates a label configuration and forwards changes to the underlying widget(s).
     * A single widget is managed for each globe instance.
     */
    private static final class ProjectLabelGlobeWidgetManager implements IProjectLabel
    {
        private final Map<IGlobeDelegate, LabelWidget> widgetForGlobe = new HashMap<>();
        private final IProjectLabel labelConfiguration;

        /**
         * @param labelConfiguration Label configuration to apply to widgets
         */
        private ProjectLabelGlobeWidgetManager(@Nonnull LabelConfiguration labelConfiguration)
        {
            this.labelConfiguration = labelConfiguration;
        }

        /**
         * Update our configuration from the given label, and then update all our widgets.
         */
        void applyFromLabel(@Nonnull IProjectLabel label)
        {
            labelConfiguration.setColor(label.getColor());
            labelConfiguration.setLabel(label.getLabel());
            labelConfiguration.setSize(label.getSize());
            labelConfiguration.setVisible(label.isVisible());

            widgetForGlobe.values().forEach(this::updateWidget);
        }

        /**
         * Add a widget on the specified globe to our collection.
         */
        void addWidget(@Nonnull IGlobeDelegate globeDelegate, @Nonnull LabelWidget widget)
        {
            widgetForGlobe.put(globeDelegate, widget);
            updateWidget(widget);
        }

        /**
         * Apply our configuration to the given widget.
         */
        private void updateWidget(@Nonnull LabelWidget widget)
        {
            widget.setColor(labelConfiguration.getColor());
            widget.setText(labelConfiguration.getLabel());
            widget.setFontSize(labelConfiguration.getSize());
            widget.setVisible(labelConfiguration.isVisible());
        }

        /**
         * Remove the widget for the given globe from our collection.
         */
        void removeWidget(@Nonnull IGlobeDelegate globe)
        {
            widgetForGlobe.remove(globe);
        }

        @Override
        public Color getColor()
        {
            return labelConfiguration.getColor();
        }

        @Override
        public void setColor(Color color)
        {
            labelConfiguration.setColor(color);
            widgetForGlobe.values().forEach(labelWidget -> labelWidget.setColor(color));
        }

        @Override
        public String getLabel()
        {
            return labelConfiguration.getLabel();
        }

        @Override
        public void setLabel(String label)
        {
            labelConfiguration.setLabel(label);
            widgetForGlobe.values().forEach(labelWidget -> labelWidget.setText(label));
        }

        @Override
        public int getSize()
        {
            return labelConfiguration.getSize();
        }

        @Override
        public void setSize(int size)
        {
            labelConfiguration.setSize(size);
            widgetForGlobe.values().forEach(labelWidget -> labelWidget.setFontSize(size));
        }

        @Override
        public boolean isVisible()
        {
            return labelConfiguration.isVisible();
        }

        @Override
        public void setVisible(boolean visible)
        {
            labelConfiguration.setVisible(visible);
            widgetForGlobe.values().forEach(labelWidget -> labelWidget.setVisible(visible));
        }
    }

    /**
     * A widget to render a project label on a specific globe.
     */
    private class LabelWidget extends AWidget
    {
        private static final String FONT_FAMILY = GlobeTextUtils.DEFAULT_PROJECT_LABEL_FONT.getName();
        private static final int FONT_SIZE = GlobeTextUtils.DEFAULT_PROJECT_LABEL_FONT.getSize();
        private static final Font.Style FONT_STYLE = Font.Style.Normal;
        private final TextWidget textWidget;
        private final IGlobeDelegate globe;
        private final ProjectLabelLocation location;

        private LabelWidget(@Nonnull IGlobeDelegate globe, @Nonnull ProjectLabelLocation location)
        {
            this.globe = globe;
            this.location = location;

            textWidget = new TextWidget("", format(FONT_SIZE));
            textWidget.setTouchable(false);
            textWidget.setBackground(TextWidget.TRANSLUCENT);
            textWidget.setColor(Color.yellow.getRGB());
            textWidget.setPadding(3);

            setVisible(false);
            setTouchable(false);
            setMargins(5, 5, 5, 5);
            addChildWidget(textWidget);
            setName("Project Label Widget: " + location);
        }

        @Override
        public boolean showInWidgetList()
        {
            return false;
        }

        /**
         * Overridden to take location into account.
         */
        @Override
        public boolean sameKindAs(IWidget other)
        {
            return other instanceof LabelWidget labelWidget && location == labelWidget.location;
        }

        @Override
        public void releaseResources()
        {
            removeLabelWidget(this);
        }

        /**
         * Update the widget font size and its associated config.
         * @param size Font size to assign.
         */
        private void setFontSize(float size)
        {
            textWidget.setWidgetTextFormat(format(size));
        }

        /**
         * Update the widget text and its associated config.
         * @param text Text to assign.
         */
        private void setText(@Nonnull String text)
        {
            textWidget.setText(text);
        }

        /**
         * Update the widget color and its associated config.
         *
         * @param color Text color to assign.
         */
        private void setColor(@Nonnull Color color)
        {
            textWidget.setColor(color.getRGB());
        }

        /**
         * @param size Desired font size.
         * @return Widget text format of the desired size.
         */
        @Nonnull
        private static TextFormat format(float size)
        {
            return new TextFormat(new Font(FONT_FAMILY, FONT_STYLE, size), 0);
        }
    }

    /**
     * Interns the project label configuration/preferences.
     *
     * @since 2.35 (copied from ProjectLabelGlobeLayer)
     */
    private static final class LabelConfigurationPreferences extends PreferencesNode
    {
        private final Map<String, LabelConfiguration> labelConfigurationMap;

        private LabelConfigurationPreferences(@Nonnull PreferencesNode rootNode)
        {
            super(rootNode);

            labelConfigurationMap = rootNode.childrenToMap(LabelConfiguration::new);
        }

        @Nonnull
        @SuppressWarnings("DataFlowIssue")
        private LabelConfiguration getLabelConfiguration(@Nonnull ProjectLabelLocation location)
        {
            // The warning on 'node(k)' being nullable if 'rootNode' is also null is suppressed.
            // The return value will never be null because 'rootNode' is non-null as specified in this class's constructor.
            return labelConfigurationMap.computeIfAbsent(location.toString(), k -> new LabelConfiguration(node(k)));
        }
    }

    /**
     * Holds the configuration for a single project label.
     *
     * @since 2.35 (copied from ProjectLabelGlobeLayer)
     */
    private static final class LabelConfiguration extends PreferencesNode implements IProjectLabel
    {
        private static final String VISIBLE_KEY = "Visible";
        private static final String COLOR_KEY = "Color";
        private static final String SIZE_KEY = "Size";
        private static final String TEXT_KEY = "Text";
        private static final String DEFAULT_COLOR = getHexColor(Color.yellow);

        private String text;
        private String hexColor;
        private int size;
        private boolean visible;

        private LabelConfiguration(@Nonnull PreferencesNode node)
        {
            super(node);

            text = node.get(TEXT_KEY, "");
            hexColor = node.get(COLOR_KEY, DEFAULT_COLOR);
            size = node.getInt(SIZE_KEY, 24);
            visible = node.getBoolean(VISIBLE_KEY, false);
        }

        @Override
        @Nonnull
        public String getLabel()
        {
            return text;
        }

        @Override
        @Nonnull
        public Color getColor()
        {
            return ColorChooserUtils.getColorFromString(hexColor);
        }

        @Override
        public int getSize()
        {
            return size;
        }

        @Override
        public boolean isVisible()
        {
            return visible;
        }

        @Override
        public void setLabel(@Nonnull String text)
        {
            put(TEXT_KEY, this.text = text);
        }

        @Override
        public void setColor(@Nonnull Color color)
        {
            put(COLOR_KEY, hexColor = getHexColor(color));
        }

        @Override
        public void setSize(int size)
        {
            put(SIZE_KEY, this.size = size);
        }

        @Override
        public void setVisible(boolean visible)
        {
            put(VISIBLE_KEY, this.visible = visible);
        }

        /**
         * @return Color RGB as a hex string, with the leading '0x' chopped
         * @since 5.5.1
         */
        @Nonnull
        private static String getHexColor(@Nonnull Color color)
        {
            return Integer.toHexString(color.getRGB()).substring(2);
        }
    }
}
