package gov.takx.core.view.gui.maptools.elevationtools.contour;

import com.atakmap.coremap.conversions.Span;
import com.atakmap.map.elevation.ElevationSource;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.contour.ContourFunction;
import gov.tak.api.contour.ContourLayerParameters;
import gov.tak.api.contour.ContourLineFactory;
import gov.tak.api.contour.ContourStyle;
import gov.tak.core.map.layer.feature.datastore.memory.InMemoryFeatureDataStore;
import gov.tak.platform.contour.ContourGroupListener;
import gov.tak.platform.contour.ContourLineGroup;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.commons.swing.entity.ActionButtonPanel;
import gov.takx.core.contour.ContourLinesService;
import gov.takx.core.contour.ContourLinesService.ExtendedContourLineGroup;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panel for listing/managing available contour line groups.
 *
 * @since 5.8
 */
@PseudoBean
public class ContourListPanel extends ATitledPanel
{
    private static final Icon ADD_ICON = UIResources.ADD_ACTION_BUTTON_ICON;
    private static final Icon DELETE_ICON = UIResources.DELETE_ACTION_BUTTON_ICON;
    private static final Icon SETTINGS_ICON = UIResources.SETTINGS_ACTION_BUTTON_ICON;

    private static ContourPanelListener lastContourListener;

    private final ContourLineFactory.Generation generationParams = new ContourLineFactory.Generation(
            // The elevation query parameters we use here are overwritten when the contour service draws an area.
            new ElevationSource.QueryParameters(),
            // Interval of 50 meters, every other line is a major, making each 100 meters a major line.
            50,
            Span.METER,
            2);
    private final ContourLayerParameters layerParams = new ContourLayerParameters(
            new ContourLayerParameters.Style(
                    // Majors are white, minors are off-white.
                    0xFFFFFFFF, 0xFFAAAAAA,
                    // Majors are 2X thicker than minors.
                    2, 1,
                    // Both lines are enabled initially.
                    true, true,
                    // Labels use size 20 font, display as meters, and are enabled initially.
                    20f, true, Span.METER),
            // Layer name gets replaced before each generation.
            "Contours");

    private final VerticalCellListPanel<ExtendedContourLineGroup> contourCellListPanel;
    private boolean editorHandlingDisabled;
    private GenerationDialog editorDialog;

    @Inject
    private ContourLinesService contourLinesService;
    @Inject
    private IExtendedGlobeManager globeManager;
    @Inject
    private INotificationService notificationService;

    private ExtendedContourLineGroup activeGroup;

    public ContourListPanel()
    {
        super("Contours");

        initComponents();
        ComponentUtils.recurseEnabled(bottomEditorPanel, false);
        showColorSchemePanel(0);

        layerParams.setDataStoreSupplier(InMemoryFeatureDataStore::new);

        // No, you cannot share the same document.
        majorThicknessText.setDocument(new LimitedDocument(c -> Character.isDigit(c) || c == '.'));
        minorThicknessText.setDocument(new LimitedDocument(c -> Character.isDigit(c) || c == '.'));
        contourCellListPanel = new VerticalCellListPanel<>(ContourPanel::new, IdentityHashMap::new)
        {
            @Override
            protected boolean supportsSelection()
            {
                return true;
            }

            @Override
            protected void onSelect(@Nonnull JPanel panel, @Nonnull ExtendedContourLineGroup item)
            {
                if (activeGroup == item) return;
                select(panel, item, true);
            }
        };
        contourCellListPanel.getScrollPane().getVerticalScrollBar().setUnitIncrement(10);
        scrollWrapper.add(contourCellListPanel, BorderLayout.CENTER);

        // We have a single listener that receives updates from all editor controls.
        //
        // This centralizes control, allowing us to easily enable/disable "live edit" mode to the contour layer
        // which is vital since we need to update the controls when changing the selected layer without having
        // those changes forward to real layer style updates.
        EditorChangeListener listener = new EditorChangeListener();
        flatMajorColorButton.addActionListener(listener);
        flatMinorColorButton.addActionListener(listener);
        gradientMaxColorButton.addActionListener(listener);
        gradientMinColorButton.addActionListener(listener);
        labelsCheck.addItemListener(listener);
        majorsCheck.addItemListener(listener);
        minorsCheck.addItemListener(listener);
        majorThicknessText.addActionListener(listener);
        minorThicknessText.addActionListener(listener);
        outlinesCheck1.addItemListener(listener);
        outlinesCheck2.addItemListener(listener);

        // When losing focus on text input fields, we'll want to delegate an update to the above listener.
        // This lets users tab between fields without having to press [enter] to submit changes.
        majorThicknessText.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                listener.actionPerformed(new ActionEvent(majorThicknessText, -1, ""));
            }
        });
        minorThicknessText.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                listener.actionPerformed(new ActionEvent(minorThicknessText, -1, ""));
            }
        });
    }

    @PostConstruct
    private void postConstruct()
    {
        // Remove old listener if one was made (occurs when user changes projects)
        if (lastContourListener != null) contourLinesService.removeContourGroupListener(lastContourListener);

        ContourPanelListener listener = new ContourPanelListener();
        contourLinesService.addContourGroupListener(listener);
        lastContourListener = listener;
    }

    /**
     * Select the given panel/contour-group.
     *
     * @param panel Associated display for the contour group.
     * @param item Newly selected contour group.
     * @param navigate {@code true} to navigate to the location of the contour group on the globe. {@code false} to only update the UI selection state.
     */
    private void select(@Nullable JPanel panel, @Nonnull ExtendedContourLineGroup item, boolean navigate)
    {
        for (JPanel other : contourCellListPanel.getPanels())
        {
            other.setBackground(other == panel ? new Color(87, 86, 84, 255) : new Color(0, 0, 0, 0));
        }
        contourCellListPanel.invalidate();
        contourCellListPanel.repaint();

        activeGroup = item;
        onSelectionChange(navigate);
    }

    /**
     * Called when the selected contour group changes.
     *
     * @param navigate {@code true} to navigate to the location of the contour group on the globe. {@code false} to only update the UI selection state.
     */
    private void onSelectionChange(boolean navigate)
    {
        ExtendedContourLineGroup group = activeGroup;
        boolean hasGroup = group != null;
        deleteButton.setEnabled(hasGroup);
        ComponentUtils.recurseEnabled(bottomEditorPanel, hasGroup);

        // Skip updates if there is no group to pull values from.
        if (!hasGroup) return;

        // Center the contour group in the globe panel if desired.
        if (navigate)
        {
            // The last focused globe falls back to the primary globe so it realistically should never be null by the time the user is interacting with the UI.
            IApplicationGlobe globe = Objects.requireNonNull(globeManager.getLastFocusedGlobe(), "No available globes");
            globe.goTo(group.getBoundingBox());
        }

        // Pulling out constants for different kinds of updates.
        ContourStyle style = group.getStyle();
        ContourFunction<Integer> colorMapper = style.getContourColorMapper();
        int colorScheme = group.getColorScheme();
        int majorColor = colorMapper.apply(0, true);
        int minorColor = colorMapper.apply(0, false);
        int maxColor = colorMapper.apply(group.getMaxElevationMeters(), true);
        int minColor = colorMapper.apply(group.getMinElevationMeters(), true);
        float majorThickness = style.getContourStrokeThicknessMapper().apply(0, true);
        float minorThickness = style.getContourStrokeThicknessMapper().apply(0, false);

        // Disable editor listener callbacks while we update the editor controls.
        editorHandlingDisabled = true;

        // Update UI state to match the selected contour group.
        if (colorScheme == ExtendedContourLineGroup.COLOR_SCHEME_FLAT)
        {
            flatMajorColorButton.setBackground(new Color(majorColor));
            flatMinorColorButton.setBackground(new Color(minorColor));
        } else if (colorScheme == ExtendedContourLineGroup.COLOR_SCHEME_GRAD)
        {
            gradientMaxColorButton.setBackground(new Color(maxColor));
            gradientMinColorButton.setBackground(new Color(minColor));
        }
        labelsCheck.setSelected(style.isLabelsEnabled());
        majorsCheck.setSelected(style.isMajorLinesEnabled());
        minorsCheck.setSelected(style.isMinorLinesEnabled());
        majorThicknessText.setText(String.format("%.1f", majorThickness));
        minorThicknessText.setText(String.format("%.1f", minorThickness));
        outlinesCheck1.setSelected(style.isLinesOutlined());
        outlinesCheck2.setSelected(style.isLinesOutlined());
        showColorSchemePanel(colorScheme);
        updateUnits();

        // Now we re-enable editor listener callbacks once we're done
        editorHandlingDisabled = false;
    }

    /**
     * Updates the {@link #labelUnitsCombo} {@link Span} value.
     * Assumes {@link EditorChangeListener} is not active.
     */
    private void updateUnits()
    {
        if (activeGroup == null) return;
        int index = ContourUnits.toIndex(activeGroup.getStyle().getLabelUnits());
        labelUnitsCombo.setSelectedIndex(index);
    }

    /**
     * Shows an editor panel for the given color scheme.
     *
     * @param scheme Color scheme to show.
     *
     * @see ExtendedContourLineGroup#COLOR_SCHEME_FLAT
     * @see ExtendedContourLineGroup#COLOR_SCHEME_GRAD
     */
    private void showColorSchemePanel(int scheme)
    {
        int shownScheme = colorSchemeCombo.getSelectedIndex();
        if (shownScheme != scheme)
        {
            colorSchemeCombo.setSelectedIndex(scheme);
            return;
        }

        // Clear the wrapper, add the respective scheme panel, then refresh.
        var schemePanel = scheme == ExtendedContourLineGroup.COLOR_SCHEME_FLAT ? colorSchemeFlatPanel : colorSchemeGradientPanel;
        colorSchemeWrapperPanel.removeAll();
        colorSchemeWrapperPanel.add(schemePanel, BorderLayout.CENTER);
        colorSchemeWrapperPanel.invalidate();
        colorSchemeWrapperPanel.repaint();
    }

    /**
     * Called from {@link EditorChangeListener} when any UI control is changed by the user.
     * @param sourceEditor Source editor where the change was made.
     */
    private void updateStyleFromSourceEditor(@NonNull Object sourceEditor)
    {
        if (activeGroup == null) return;

        ContourStyle style = activeGroup.getStyle();
        ContourFunction<Integer> colorMapper = style.getContourColorMapper();

        int majorColor = colorMapper.apply(0, true);
        int minorColor = colorMapper.apply(0, false);
        int maxColor = colorMapper.apply(activeGroup.getMaxElevationMeters(), true);
        int minColor = colorMapper.apply(activeGroup.getMinElevationMeters(), true);
        float majorThickness = style.getContourStrokeThicknessMapper().apply(0, true);
        float minorThickness = style.getContourStrokeThicknessMapper().apply(0, false);
        if (sourceEditor == flatMajorColorButton)
        {
            // Note, the color chooser dialog uses 'this' panel for positioning rather than the button
            // because of how low the button is. This results in the dialog appearing off-screen consistently
            // which is very annoying.
            Color chosen = JColorChooser.showDialog(this, "Choose Major Contour Color", new Color(majorColor));
            if (chosen != null)
            {
                flatMajorColorButton.setBackground(chosen);
                style.setContourColors(chosen.getRGB(), minorColor);
            }
        } else if (sourceEditor == flatMinorColorButton)
        {
            Color chosen = JColorChooser.showDialog(this, "Choose Minor Contour Color", new Color(minorColor));
            if (chosen != null)
            {
                flatMinorColorButton.setBackground(chosen);
                style.setContourColors(majorColor, chosen.getRGB());
            }
        } else if (sourceEditor == gradientMaxColorButton)
        {
            Color chosen = JColorChooser.showDialog(this, "Choose Maximum Elevation Contour Color", new Color(maxColor));
            if (chosen != null)
            {
                gradientMaxColorButton.setBackground(chosen);
                style.setContourLineColorMapper(new GradientColorFunction(chosen, new Color(minColor), activeGroup.getMaxElevationMeters(), activeGroup.getMinElevationMeters()));
            }
        } else if (sourceEditor == gradientMinColorButton)
        {
            Color chosen = JColorChooser.showDialog(this, "Choose Minimum Elevation Contour Color", new Color(maxColor));
            if (chosen != null)
            {
                gradientMinColorButton.setBackground(chosen);
                style.setContourLineColorMapper(new GradientColorFunction(new Color(maxColor), chosen, activeGroup.getMaxElevationMeters(), activeGroup.getMinElevationMeters()));
            }
        } else if (sourceEditor == majorThicknessText)
        {
            try
            {
                float thickness = Float.parseFloat(majorThicknessText.getText());
                style.setContourStrokeThicknessMapper(((elevationMeters, isMajor) -> isMajor ? thickness : minorThickness));
            } catch (NumberFormatException ignored)
            {
            }
        } else if (sourceEditor == minorThicknessText)
        {
            try
            {
                float thickness = Float.parseFloat(minorThicknessText.getText());
                style.setContourStrokeThicknessMapper(((elevationMeters, isMajor) -> isMajor ? majorThickness : thickness));
            } catch (NumberFormatException ignored)
            {
            }
        } else if (sourceEditor == labelsCheck)
        {
            style.setLabelsEnabled(labelsCheck.isSelected());
        } else if (sourceEditor == minorsCheck)
        {
            style.setMinorLinesEnabled(minorsCheck.isSelected());
        } else if (sourceEditor == majorsCheck)
        {
            style.setMajorLinesEnabled(majorsCheck.isSelected());
        } else if (sourceEditor == outlinesCheck1)
        {
            style.setLinesOutlined(outlinesCheck1.isSelected());
        } else if (sourceEditor == outlinesCheck2)
        {
            style.setLinesOutlined(outlinesCheck2.isSelected());
        }
    }

    /**
     * Called by {@link #addButton}.
     */
    private void onAdd()
    {
        // Disable button while the user has engaged the drawing tool.
        addButton.setEnabled(false);

        // Add an in-progress display.
        AtomicBoolean cancellation = new AtomicBoolean(false);
        ContourProgressPanel progressPanel = new ContourProgressPanel(cancellation);
        scrollWrapper.add(progressPanel, BorderLayout.NORTH);

        // Give the upcoming layer a unique name.
        layerParams.setLayerName("Contours:" + UUID.randomUUID());

        // Prompt the user to draw an area, updating our UI state when completed.
        contourLinesService.initiateContourDrawing(generationParams, layerParams, cancellation, progressPanel, progressPanel)
                .whenComplete((group, error) -> {
                    if (error != null) notificationService.show(new Notification(error.getMessage(), Notification.Type.Error));
                    scrollWrapper.remove(progressPanel);
                    addButton.setEnabled(true);
                });
    }

    /**
     * Called by {@link #deleteButton}.
     */
    private void onDelete()
    {
        if (activeGroup == null) return;
        ActionPopupFactory
                .yesNo("Remove selected contours?", deleteButton, () -> contourLinesService.removeContourGroup(activeGroup))
                .show();
    }

    /**
     * Called by {@link #generationButton}.
     */
    private void onShowGenerationOptions()
    {
        // This is initialized lazily because the elevation is generally unavailable by the time this class initializes.
        // However, if we create the dialog only upon the first user interaction the low-res bundled elevation will be ready by then.
        if (editorDialog == null) editorDialog = new GenerationDialog(generationParams, layerParams);
        editorDialog.setVisible(true);
        editorDialog.requestFocus();
    }

    /**
     * Called when {@link #labelUnitsCombo} updates.
     */
    private void onLabelUnitsChanged()
    {
        if (activeGroup == null) return;

        // The combo model just has:
        //  - meters
        //  - feet
        //  - yards
        // The larger ones are kinda redundant.
        int selectedIndex = labelUnitsCombo.getSelectedIndex();
        Span units = ContourUnits.fromSelectionIndex(selectedIndex);
        activeGroup.getStyle().setLabelUnits(units);
    }

    /**
     * Called when {@link #colorSchemeCombo} updates.
     */
    private void onColorSchemeChanged()
    {
        if (activeGroup == null) return;

        // Our color scheme combo model only has two styles:
        // - COLOR_SCHEME_FLAT
        // - COLOR_SCHEME_GRAD
        int scheme = colorSchemeCombo.getSelectedIndex();
        activeGroup.setColorScheme(scheme);

        // Update the layer's contour color mapping function to fit the new scheme.
        if (scheme == ExtendedContourLineGroup.COLOR_SCHEME_FLAT)
        {
            Color majorColor = flatMajorColorButton.getBackground();
            Color minorColor = flatMinorColorButton.getBackground();
            activeGroup.getStyle().setContourColors(majorColor.getRGB(), minorColor.getRGB());
        } else if (scheme == ExtendedContourLineGroup.COLOR_SCHEME_GRAD)
        {
            Color maxColor = gradientMaxColorButton.getBackground();
            Color minColor = gradientMinColorButton.getBackground();
            activeGroup.getStyle().setContourLineColorMapper(new GradientColorFunction(maxColor, minColor, activeGroup.getMaxElevationMeters(), activeGroup.getMinElevationMeters()));
        }

        // Ensure the UI shows the proper scheme editor panel.
        showColorSchemePanel(scheme);
    }

    /**
     * @param group Group to remove.
     */
    @InvokeOnEDT
    private void removeContourPanel(@Nonnull ExtendedContourLineGroup group)
    {
        contourCellListPanel.removeItem(group);
        if (group == activeGroup) activeGroup = null;
        onSelectionChange(false);
    }

    /**
     * @param group Group to display.
     */
    @InvokeOnEDT
    private void addContourPanel(@Nonnull ExtendedContourLineGroup group)
    {
        contourCellListPanel.addItem(group);

        List<JPanel> panels = contourCellListPanel.getPanels();
        JPanel panel = panels.get(panels.size() - 1);
        select(panel, group, false);
    }

    /**
     * Listener implementation that updates contour panel displays for new and removed contour groups.
     * @see #postConstruct()
     */
    private class ContourPanelListener implements ContourGroupListener
    {
        @Override
        public void onContourGroupCreated(@Nonnull ContourLineGroup group)
        {
            ExtendedContourLineGroup extendedGroup = (ExtendedContourLineGroup) group;

            // Ensure the contour group has a display panel.
            addContourPanel(extendedGroup);

            // Refresh contour display panel.
            group.getStyle().addChangeListener(property -> {
                // Skip updates for properties that don't require a graphic refresh in our preview.
                if (property == ContourStyle.PROP_LABEL_ENABLED
                        || property == ContourStyle.PROP_LABEL_SIZE_MAPPER
                        || property == ContourStyle.PROP_LABEL_UNITS
                        || property == ContourStyle.PROP_LABEL_TEXT_MAPPER)
                    return;
                ContourPanel contourPanel = (ContourPanel) contourCellListPanel.getPanel(extendedGroup);
                if (contourPanel != null) contourPanel.refreshGraphic();
            });
        }

        @Override
        public void onContourGroupRemoved(@Nonnull ContourLineGroup group)
        {
            removeContourPanel((ExtendedContourLineGroup) group);
        }
    }

    /**
     * Swing listener that is shared among all editable controls.
     * The respective updates are dispatched by checking the event source.
     * @see #updateStyleFromSourceEditor(Object) 
     */
    private class EditorChangeListener implements ItemListener, ActionListener
    {
        @Override
        public void itemStateChanged(ItemEvent e)
        {
            if (editorHandlingDisabled) return;
            updateStyleFromSourceEditor(e.getSource());
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (editorHandlingDisabled) return;
            updateStyleFromSourceEditor(e.getSource());
        }
    }

    /**
     * Hue rotating gradient function for {@link ExtendedContourLineGroup#COLOR_SCHEME_GRAD}.
     * 
     * @param maxColor Color for maximum elevation.
     * @param minColor Color for minimum elevation.
     * @param maxMeters Max elevation, meters.
     * @param minMeters Min elevation, meters.
     */
    private record GradientColorFunction(@NonNull Color maxColor, @NonNull Color minColor, double maxMeters, double minMeters) implements ContourFunction<Integer>
    {
        @Override
        public Integer apply(double elevationMeters, boolean isMajor)
        {
            // Edge case if there's just a single layer of contours.
            if (maxMeters == minMeters) return minColor.getRGB();

            float t = (float) ((elevationMeters - minMeters) / (maxMeters - minMeters));
            t = Math.max(0.0f, Math.min(1.0f, t));

            // Convert both colors to HSV.
            float[] hsvMin = rgbToHsv(minColor);
            float[] hsvMax = rgbToHsv(maxColor);

            // Interpolate in HSV space.
            float h = interpolateHue(hsvMin[0], hsvMax[0], t);
            float s = hsvMin[1] * (1 - t) + hsvMax[1] * t;
            float v = hsvMin[2] * (1 - t) + hsvMax[2] * t;
            int a = Math.round(minColor.getAlpha() * (1 - t) + maxColor.getAlpha() * t);

            Color result = hsvToRgb(h, s, v, a);
            return result.getRGB();
        }

        @Nonnull
        private float[] rgbToHsv(@Nonnull Color c)
        {
            float r = c.getRed() / 255f;
            float g = c.getGreen() / 255f;
            float b = c.getBlue() / 255f;

            float max = Math.max(Math.max(r, g), b);
            float min = Math.min(Math.min(r, g), b);
            float delta = max - min;

            float h = 0;
            if (delta != 0)
            {
                if (max == r)
                {
                    h = (g - b) / delta;
                } else if (max == g)
                {
                    h = (b - r) / delta + 2;
                } else
                {
                    h = (r - g) / delta + 4;
                }

                h *= 60;
                if (h < 0) h += 360;
            }

            float s = (max == 0) ? 0 : delta / max;
            float v = max;

            return new float[]{h, s, v};
        }

        @Nonnull
        private Color hsvToRgb(float h, float s, float v, int alpha)
        {
            float c = v * s;
            float x = c * (1 - Math.abs((h / 60) % 2 - 1));
            float m = v - c;

            float r, g, b;
            if (h < 60)
            {
                r = c;
                g = x;
                b = 0;
            } else if (h < 120)
            {
                r = x;
                g = c;
                b = 0;
            } else if (h < 180)
            {
                r = 0;
                g = c;
                b = x;
            } else if (h < 240)
            {
                r = 0;
                g = x;
                b = c;
            } else if (h < 300)
            {
                r = x;
                g = 0;
                b = c;
            } else
            {
                r = c;
                g = 0;
                b = x;
            }

            int red = Math.round((r + m) * 255);
            int green = Math.round((g + m) * 255);
            int blue = Math.round((b + m) * 255);

            return new Color(red, green, blue, alpha);
        }

        private float interpolateHue(float h1, float h2, double t)
        {
            // Handle hue wrapping properly (shortest path)
            float diff = h2 - h1;
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;
            float h = (float) (h1 + diff * t);
            if (h < 0) h += 360;
            if (h >= 360) h -= 360;
            return h;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        addButton = new JButton(ADD_ICON);
        deleteButton = new JButton(DELETE_ICON);
        generationButton = new JButton(SETTINGS_ICON);
        var topToolsPanel = new ActionButtonPanel(addButton, deleteButton, generationButton);
        scrollWrapper = new JPanel();
        bottomEditorPanel = new JPanel();
        var featureTogglePanel = new JPanel();
        majorsCheck = new JCheckBox();
        minorsCheck = new JCheckBox();
        var majorThicknessPrefix = new JLabel();
        majorThicknessText = new JTextField();
        var minorThicknessPrefix = new JLabel();
        minorThicknessText = new JTextField();
        var labelControlsPanel = new JPanel();
        labelsCheck = new JCheckBox();
        var labelUnitsPrefix = new JLabel();
        labelUnitsCombo = new JComboBox<>();
        var colorSchemePanel = new JPanel();
        colorSchemeCombo = new JComboBox<>();
        colorSchemeWrapperPanel = new JPanel();
        colorSchemeFlatPanel = new JPanel();
        var flatMajorColorPrefix = new JLabel();
        flatMajorColorButton = new JButton();
        var flatMinorColorPrefix = new JLabel();
        flatMinorColorButton = new JButton();
        outlinesCheck2 = new JCheckBox();
        colorSchemeGradientPanel = new JPanel();
        var gradientMaxPrefixLabel = new JLabel();
        gradientMaxColorButton = new JButton();
        var gradientMinPrefixLabel = new JLabel();
        gradientMinColorButton = new JButton();
        outlinesCheck1 = new JCheckBox();
        CellConstraints cc = new CellConstraints();

        //---- addButton ----
        addButton.setToolTipText("Add a new contour area");
        addButton.setName("addButton");
        addButton.addActionListener(e -> onAdd());

        //---- deleteButton ----
        deleteButton.setEnabled(false);
        deleteButton.setToolTipText("Delete the selected contour area");
        deleteButton.setName("deleteButton");
        deleteButton.addActionListener(e -> onDelete());

        //---- generationButton ----
        generationButton.setToolTipText("Modify contour area creation options");
        generationButton.setName("generationButton");
        generationButton.addActionListener(e -> onShowGenerationOptions());

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //---- topToolsPanel ----
        topToolsPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Color.gray),
                new EmptyBorder(4, 4, 4, 4)));
        topToolsPanel.setName("topToolsPanel");
        add(topToolsPanel, BorderLayout.NORTH);

        //======== scrollWrapper ========
        {
            scrollWrapper.setName("scrollWrapper");
            scrollWrapper.setLayout(new BorderLayout());
        }
        add(scrollWrapper, BorderLayout.CENTER);

        //======== bottomEditorPanel ========
        {
            bottomEditorPanel.setBorder(new MatteBorder(1, 0, 0, 0, Color.gray));
            bottomEditorPanel.setEnabled(false);
            bottomEditorPanel.setName("bottomEditorPanel");
            bottomEditorPanel.setLayout(new FormLayout(
                    "2*(default, $lcgap), default:grow",
                    "fill:default"));

            //======== featureTogglePanel ========
            {
                featureTogglePanel.setBorder(new CompoundBorder(
                        new TitledBorder("Contour display"),
                        new EmptyBorder(4, 4, 4, 4)));
                featureTogglePanel.setName("featureTogglePanel");
                featureTogglePanel.setLayout(new FormLayout(
                        "default, $lcgap, default",
                        "3*(default, $lgap), default"));

                //---- majorsCheck ----
                majorsCheck.setText("Major contours");
                majorsCheck.setName("majorsCheck");
                featureTogglePanel.add(majorsCheck, cc.xywh(1, 1, 3, 1));

                //---- minorsCheck ----
                minorsCheck.setText("Minor contours");
                minorsCheck.setName("minorsCheck");
                featureTogglePanel.add(minorsCheck, cc.xywh(1, 3, 3, 1));

                //---- majorThicknessPrefix ----
                majorThicknessPrefix.setText("Major thickness");
                majorThicknessPrefix.setName("majorThicknessPrefix");
                featureTogglePanel.add(majorThicknessPrefix, cc.xy(1, 5));

                //---- majorThicknessText ----
                majorThicknessText.setName("majorThicknessText");
                featureTogglePanel.add(majorThicknessText, cc.xy(3, 5));

                //---- minorThicknessPrefix ----
                minorThicknessPrefix.setText("Minor thickness");
                minorThicknessPrefix.setName("minorThicknessPrefix");
                featureTogglePanel.add(minorThicknessPrefix, cc.xy(1, 7));

                //---- minorThicknessText ----
                minorThicknessText.setName("minorThicknessText");
                featureTogglePanel.add(minorThicknessText, cc.xy(3, 7));
            }
            bottomEditorPanel.add(featureTogglePanel, cc.xy(1, 1));

            //======== labelControlsPanel ========
            {
                labelControlsPanel.setBorder(new CompoundBorder(
                        new TitledBorder("Label display"),
                        new EmptyBorder(4, 4, 4, 4)));
                labelControlsPanel.setName("labelControlsPanel");
                labelControlsPanel.setLayout(new FormLayout(
                        "default, $lcgap, default",
                        "default, $lgap, default"));

                //---- labelsCheck ----
                labelsCheck.setText("Show labels");
                labelsCheck.setName("labelsCheck");
                labelControlsPanel.add(labelsCheck, cc.xywh(1, 1, 3, 1));

                //---- labelUnitsPrefix ----
                labelUnitsPrefix.setText("Label units");
                labelUnitsPrefix.setName("labelUnitsPrefix");
                labelControlsPanel.add(labelUnitsPrefix, cc.xy(1, 3));

                //---- labelUnitsCombo ----
                labelUnitsCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                        "Meters",
                        "Feet",
                        "Yards"
                }));
                labelUnitsCombo.setName("labelUnitsCombo");
                labelUnitsCombo.addItemListener(e -> onLabelUnitsChanged());
                labelControlsPanel.add(labelUnitsCombo, cc.xy(3, 3));
            }
            bottomEditorPanel.add(labelControlsPanel, cc.xy(3, 1));

            //======== colorSchemePanel ========
            {
                colorSchemePanel.setBorder(new CompoundBorder(
                        new TitledBorder("Color scheme"),
                        new EmptyBorder(4, 4, 4, 4)));
                colorSchemePanel.setName("colorSchemePanel");
                colorSchemePanel.setLayout(new FormLayout(
                        "default",
                        "default, $lgap, default"));

                //---- colorSchemeCombo ----
                colorSchemeCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                        "Flat major/minor",
                        "Elevation gradient"
                }));
                colorSchemeCombo.setName("colorSchemeCombo");
                colorSchemeCombo.addItemListener(e -> onColorSchemeChanged());
                colorSchemePanel.add(colorSchemeCombo, cc.xy(1, 1));

                //======== colorSchemeWrapperPanel ========
                {
                    colorSchemeWrapperPanel.setName("colorSchemeWrapperPanel");
                    colorSchemeWrapperPanel.setLayout(new BorderLayout());
                }
                colorSchemePanel.add(colorSchemeWrapperPanel, cc.xy(1, 3));
            }
            bottomEditorPanel.add(colorSchemePanel, cc.xy(5, 1));
        }
        add(bottomEditorPanel, BorderLayout.SOUTH);

        //======== colorSchemeFlatPanel ========
        {
            colorSchemeFlatPanel.setName("colorSchemeFlatPanel");
            colorSchemeFlatPanel.setLayout(new FormLayout(
                    "default, $lcgap, default",
                    "2*(default, $lgap), default"));

            //---- flatMajorColorPrefix ----
            flatMajorColorPrefix.setText("Major contour: ");
            flatMajorColorPrefix.setName("flatMajorColorPrefix");
            colorSchemeFlatPanel.add(flatMajorColorPrefix, cc.xy(1, 1));

            //---- flatMajorColorButton ----
            flatMajorColorButton.setBorder(new LineBorder(Color.white));
            flatMajorColorButton.setPreferredSize(new Dimension(16, 16));
            flatMajorColorButton.setBackground(Color.white);
            flatMajorColorButton.setName("flatMajorColorButton");
            colorSchemeFlatPanel.add(flatMajorColorButton, cc.xy(3, 1));

            //---- flatMinorColorPrefix ----
            flatMinorColorPrefix.setText("Minor contour:");
            flatMinorColorPrefix.setName("flatMinorColorPrefix");
            colorSchemeFlatPanel.add(flatMinorColorPrefix, cc.xy(1, 3));

            //---- flatMinorColorButton ----
            flatMinorColorButton.setBorder(new LineBorder(Color.white));
            flatMinorColorButton.setPreferredSize(new Dimension(16, 16));
            flatMinorColorButton.setBackground(new Color(0xcccccc));
            flatMinorColorButton.setName("flatMinorColorButton");
            colorSchemeFlatPanel.add(flatMinorColorButton, cc.xy(3, 3));

            //---- outlinesCheck2 ----
            outlinesCheck2.setText("Outlines");
            outlinesCheck2.setSelected(true);
            outlinesCheck2.setName("outlinesCheck2");
            colorSchemeFlatPanel.add(outlinesCheck2, cc.xywh(1, 5, 3, 1));
        }

        //======== colorSchemeGradientPanel ========
        {
            colorSchemeGradientPanel.setName("colorSchemeGradientPanel");
            colorSchemeGradientPanel.setLayout(new FormLayout(
                    "default, $lcgap, default",
                    "2*(default, $lgap), default"));

            //---- gradientMaxPrefixLabel ----
            gradientMaxPrefixLabel.setText("Max elevation");
            gradientMaxPrefixLabel.setName("gradientMaxPrefixLabel");
            colorSchemeGradientPanel.add(gradientMaxPrefixLabel, cc.xy(1, 1));

            //---- gradientMaxColorButton ----
            gradientMaxColorButton.setBorder(new LineBorder(Color.white));
            gradientMaxColorButton.setPreferredSize(new Dimension(16, 16));
            gradientMaxColorButton.setBackground(Color.red);
            gradientMaxColorButton.setName("gradientMaxColorButton");
            colorSchemeGradientPanel.add(gradientMaxColorButton, cc.xy(3, 1));

            //---- gradientMinPrefixLabel ----
            gradientMinPrefixLabel.setText("Min elevation");
            gradientMinPrefixLabel.setName("gradientMinPrefixLabel");
            colorSchemeGradientPanel.add(gradientMinPrefixLabel, cc.xy(1, 3));

            //---- gradientMinColorButton ----
            gradientMinColorButton.setBorder(new LineBorder(Color.white));
            gradientMinColorButton.setPreferredSize(new Dimension(16, 16));
            gradientMinColorButton.setBackground(new Color(0x33ff33));
            gradientMinColorButton.setName("gradientMinColorButton");
            colorSchemeGradientPanel.add(gradientMinColorButton, cc.xy(3, 3));

            //---- outlinesCheck1 ----
            outlinesCheck1.setText("Outlines");
            outlinesCheck1.setSelected(true);
            outlinesCheck1.setName("outlinesCheck1");
            colorSchemeGradientPanel.add(outlinesCheck1, cc.xywh(1, 5, 3, 1));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JButton addButton;
    private JButton deleteButton;
    private JButton generationButton;
    private JPanel scrollWrapper;
    private JPanel bottomEditorPanel;
    private JCheckBox majorsCheck;
    private JCheckBox minorsCheck;
    private JTextField majorThicknessText;
    private JTextField minorThicknessText;
    private JCheckBox labelsCheck;
    private JComboBox<String> labelUnitsCombo;
    private JComboBox<String> colorSchemeCombo;
    private JPanel colorSchemeWrapperPanel;
    private JPanel colorSchemeFlatPanel;
    private JButton flatMajorColorButton;
    private JButton flatMinorColorButton;
    private JCheckBox outlinesCheck2;
    private JPanel colorSchemeGradientPanel;
    private JButton gradientMaxColorButton;
    private JButton gradientMinColorButton;
    private JCheckBox outlinesCheck1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
