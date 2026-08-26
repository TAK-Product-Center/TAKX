package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.render.RendererConstants;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.ISmartTrackPersist;
import gov.takx.api.registry.IIconSelectionListener;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.IExtendedSmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.ui.AProgramPreferencesPanel;
import gov.takx.api.url.Schemes;
import gov.takx.commons.date.DateFormatter;
import gov.takx.commons.swing.ColorChooserUtils;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.WrappingLabel;
import gov.takx.core.mapobject.gui.MapObjectSmartTracksPreferencesPanel;
import gov.takx.core.view.gui.ProgressDialog;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.tracks.BucketMode;
import gov.takx.platform.tracks.LineStyle;
import gov.takx.platform.tracks.TrackShape;
import gov.takx.platform.tracks.TrackStyleDefinitionBuilder;
import gov.takx.platform.tracks.TrackUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.java.dev.colorchooser.ColorChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.NONE_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.getActionButtonIcon;

/**
 * This Panel is used to populate the ITrackStyleDefinition attributes.  This panel only sets attributes that make sense
 * for the user to set globally. The panel will be subclassed or added to other component panels that will allow the
 * user to assign the rest of the attributes (track color, glyphs, etc).
 *
 * @see ITrackStyleDefinition
 */
@PseudoBean
public class SmartTracksPreferencesPanel extends AProgramPreferencesPanel implements IIconSelectionListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String RENDERING_DESCRIPTION = "<html>Choose an option below to select the desired level of " +
            "track detail. The higher the detail level, the higher the number of tracks that will be shown before " +
            "grouping logic is applied.</html>";

    private static final String DESCRIPTION_GROUPING_PREFIX = "<html>Up to ";
    private static final String DESCRIPTION_GROUPING_SUFFIX = " tracks will be displayed before grouping logic is applied.";

    private static final String HIGH_DESCRIPTION = "<br><br>The maximum allowable track will be displayed before " +
            "grouping is applied. Choosing this option will allow the maximum number of tracks to be displayed at " +
            "once but may severely affect application performance.</html>";

    private static final String MEDIUM_DESCRIPTION = "<br><br>A mid-range number of tracks will be displayed before " +
            "grouping is applied. Choosing this option will allow more tracks to be viewed at once but may affect " +
            "the application's performance.</html>";

    private static final String LOW_DESCRIPTION = "<br><br>A low number of tracks will be displayed before grouping " +
            "is applied. Choose this option for the best performance.</html>";

    private static final ImageIcon TRACK_CONE_ICON = getActionButtonIcon(PLATFORM_ICONS, "track_cone.png");
    private static final ImageIcon TRACK_CYLINDER_ICON = getActionButtonIcon(PLATFORM_ICONS, "track_cylinder.png");
    private static final ImageIcon TRACK_CUBE_ICON = getActionButtonIcon(PLATFORM_ICONS, "track_cube.png");
    private static final ImageIcon TRACK_SPHERE_ICON = getActionButtonIcon(PLATFORM_ICONS, "track_sphere.png");
    private static final ImageIcon LINE_ICON = getActionButtonIcon(PLATFORM_ICONS, "line.png");
    private static final ImageIcon LINE_ARROW_HEAD_ICON = getActionButtonIcon(PLATFORM_ICONS, "line_arrow_head.png");

    private static final int TRACK = 0;
    private static final int LINE = 1;
    private static final int STOP_RECORD = 2;

    private static final String COLOR = "color";
    private static final String COLORCHOOSER = "colorChooser";
    private static final String LINE_COLOR_CHOOSER = "lineColorChooser";
    private static final String STOP_RECORDS_COLOR_CHOOSER = "stopRecordsColorChooser";

    private boolean trackStyleDirty;
    private boolean lineStyleDirty;
    private boolean stopRecordStyleDirty;

    // Global TSD info.
    private ITrackStyleDefinition globalTrackStyleDefinition;
    private IConnectingLineDefinition globalConnectingLineDefinition;

    // Map Object TSD info.
    private IMapObject mapObject;
    private ITrackStyleDefinition mapObjectTrackStyleDefinition;
    private boolean createdPlaceholderTSD = false;
    private IConnectingLineDefinition mapObjectConnectingLineDefinition;
    private boolean createdPlaceholderCLD = false;
    private ITrackStyleDefinition mapObjectStopRecordsTrackStyleDefinition;

    // Other state info.
    private boolean initializing;
    private boolean globalMode;
    private final Set<Component> modifiedComponents = new HashSet<>(); // Track which components have been modified
    private boolean maxAllowableVisibleTracks;

    private boolean glyphPanelInitialized = false;

    @Inject
    private IExtendedSmartTrackManager smartTrackManager;

    @Inject
    private ISmartTrackPersist smartTrackPersist;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    public SmartTracksPreferencesPanel()
    {
        initComponents();

        noStyleToggleButton.setIcon(NONE_ACTION_BUTTON_ICON);
        solidToggleButton.setIcon(LINE_ICON);
        arrowHeadToggleButton.setIcon(LINE_ARROW_HEAD_ICON);

        noneToggleButton.setIcon(NONE_ACTION_BUTTON_ICON);
        coneToggleButton.setIcon(TRACK_CONE_ICON);
        cubeToggleButton.setIcon(TRACK_CUBE_ICON);
        cylinderToggleButton.setIcon(TRACK_CYLINDER_ICON);
        sphereToggleButton.setIcon(TRACK_SPHERE_ICON);

        stopRecordsConeToggleButton.setIcon(TRACK_CONE_ICON);
        stopRecordsCubeToggleButton.setIcon(TRACK_CUBE_ICON);
        stopRecordsCylinderToggleButton.setIcon(TRACK_CYLINDER_ICON);
        stopRecordsSphereToggleButton.setIcon(TRACK_SPHERE_ICON);

        renderingDescriptionLabel.setText(RENDERING_DESCRIPTION);
        highDescriptionLabel.setText(HIGH_DESCRIPTION);
        mediumDescriptionLabel.setText(MEDIUM_DESCRIPTION);
        lowDescriptionLabel.setText(LOW_DESCRIPTION);
    }

    @PostConstruct
    private void postConstruct()
    {
        // Whether global or map object, always retrieve the global TSD and CLD.
        globalTrackStyleDefinition = smartTrackManager.getGlobalTrackStyleDefinition();
        globalConnectingLineDefinition = smartTrackManager.getGlobalConnectingLineDefinition();

        // Prepare the Group By options.
        groupByComboBox.setModel(new DefaultComboBoxModel<>(
                new BucketMode[]{BucketMode.None, BucketMode.HourOfDay, BucketMode.DayOfWeek, BucketMode.MonthOfYear}));

        // Elide SECONDS from the time combo box model
        timeComboBox.setModel(new DefaultComboBoxModel<>(
                new DateFormatter.TimePeriod[]{DateFormatter.TimePeriod.YEARS, DateFormatter.TimePeriod.WEEKS,
                        DateFormatter.TimePeriod.DAYS, DateFormatter.TimePeriod.HOURS, DateFormatter.TimePeriod.MINUTES, DateFormatter.TimePeriod.SECONDS}));

        // Prepare other GUI components.
        lineColorChooser.setPalettes(ColorChooserUtils.getDefaultPalettes());
        enableLineNonStyleComponents(!lineButtonGroup.isSelected(noStyleToggleButton.getModel()));
    }

    /**
     * Called by {@link MapObjectSmartTracksPreferencesPanel} to apply current Map Object
     * settings and to allow the settings to be updated.
     *
     * @param moc The Map Object's controller
     */
    public void setMapObjectController(IMapObjectController moc)
    {
        // Extract everything we need from the Map Object.
        mapObject = moc.getMapObject();

        // Allow both the TSD and CLD to be null, craft up new deferring ones in this case

        mapObjectTrackStyleDefinition = moc.getTrackStyleDefinition();
        if (mapObjectTrackStyleDefinition == null)
        {
            mapObjectTrackStyleDefinition = smartTrackManager.createDeferringTSD(mapObject);
            createdPlaceholderTSD = true;
        }

        mapObjectConnectingLineDefinition = moc.getConnectingLineDefinition();
        if (mapObjectConnectingLineDefinition == null)
        {
            mapObjectConnectingLineDefinition = smartTrackManager.createDeferringCLD(mapObject);
            createdPlaceholderCLD = true;
        }

        mapObjectStopRecordsTrackStyleDefinition = moc.getStopRecordsTrackStyleDefinition();

        // Further UI setup for panels that are only used by Map Objects.
        glyphSelectionPanel.setIconSelectionListener(this);
        glyphSelectionPanel.setIconListSize(200, 100);
        colorChooser.setPalettes(ColorChooserUtils.getDefaultPalettes());
        stopRecordsColorChooser.setPalettes(ColorChooserUtils.getDefaultPalettes());

        // Hide all components that do not apply to map object preferences.
        tabbedPane.remove(panelRendering);

        // Now that we know what type of panel this is, add the listeners.
        addListeners();
    }

    /**
     * Called by the {@link GlobalSmartTracksPanel} to indicate that this is not a Map Object configuration panel.
     */
    public void setGlobalMode()
    {
        globalMode = true;

        // Hide all components that do not apply to global preferences.
        checkBoxRestoreDefaults.setVisible(false);
        tabbedPane.remove(glyphPanel);
        tabbedPane.remove(stopRecordsPanel);
        colorChooser.setVisible(false);
        colorLabel.setVisible(false);

        // Now that we know what type of panel this is, add the listeners.
        addListeners();
    }

    /**
     * Update the GUI.
     */
    public void initializeFields()
    {
        initializing = true;

        if (!globalMode)
        {
            initColorChooser();
            initGlyphChooser();
            initStopRecordsOptions();
        } else
        {
            initTrackRenderingDetailOptions();
        }

        // Init show tracks
        ITrackStyleDefinition tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedVisible());
        showTracksCheckBox.setSelected(tsd.isVisible());

        // Init track shapes
        tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedTrackShape());
        initShapeToggleButtons(tsd, trackShapeButtonGroup.getElements());

        // Init track opacity
        tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedTrackShapeConstantOpacity());
        trackOpacitySpinner.setValue((int) (tsd.getTrackShapeConstantOpacity() * 100));

        // Init track size
        tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedTrackShapeConstantSize());
        trackSizeSpinner.setValue(tsd.getTrackShapeConstantSize());

        // Init track heading
        tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedLinkTrackShapeToHeading());
        linkShapeOrientationToTrackHeadingCheckBox.setSelected(tsd.isTrackShapeLinkedToHeading());

        // Init Track grouping
        tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedBucketMode());
        groupByComboBox.setSelectedItem(tsd.getBucketMode());

        // Init time threshold
        tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedTimeThreshold());
        initTimeThreshold(tsd);

        // Init show heading
        tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedShowHeading());
        showHeadingCheckBox.setSelected(tsd.isShowingHeading());

        // Init fade over time
        tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedFadedOverTime());
        fadeOverTmeCheckBox.setSelected(tsd.isFadedOverTime());

        // Init snap to surface
        tsd = getTSDForProperty(!globalMode && mapObjectTrackStyleDefinition.hasDefinedSnapToSurface());
        snapToSurfaceCheckBox.setSelected(tsd.isSnappedToSurface());

        // Init connecting line properties
        // Init the line style
        IConnectingLineDefinition cldForProperty = getCLDForProperty(mapObjectConnectingLineDefinition != null && mapObjectConnectingLineDefinition.hasDefinedLineStyle());
        initLineStyle(cldForProperty);

        // Init the line color
        cldForProperty = getCLDForProperty(mapObjectConnectingLineDefinition != null && mapObjectConnectingLineDefinition.hasDefinedLineColor());
        lineColorChooser.setColor(cldForProperty.getLineColor());

        // Init line width
        cldForProperty = getCLDForProperty(mapObjectConnectingLineDefinition != null && mapObjectConnectingLineDefinition.hasDefinedLineWidth());
        lineWidthSpinner.setValue(cldForProperty.getLineWidth());

        // Enable the components if we tracks are set to be visible.
        enableAllComponents(showTracksCheckBox.isSelected());

        // Reset state.
        trackStyleDirty = false;
        lineStyleDirty = false;
        stopRecordStyleDirty = false;
        modifiedComponents.clear();

        initializing = false;
    }

    private void initColorChooser()
    {
        if (mapObjectTrackStyleDefinition.getTrackShapeConstantColor() != null)
        {
            colorChooser.setColor(mapObjectTrackStyleDefinition.getTrackShapeConstantColor());
        } else
        {
            Color c = mapObject.getDefaultTrackColor();
            if (null == c)
            {
                c = ImageUtils.generateRandomColor();
                mapObject.setDefaultTrackColor(c);
            }
            colorChooser.setColor(c);
        }
    }

    private void initGlyphChooser()
    {
        String glyphResource = mapObjectTrackStyleDefinition.getGlyphResource();
        boolean hasGlyph = glyphResource != null;

        showGlyphCheckBox.setSelected(hasGlyph);

        if (hasGlyph)
        {
            glyphSelectionPanel.setSelectedIcon(glyphResource);
        } else
        {
            glyphLabel.setIcon(null);
        }
    }

    private void initStopRecordsOptions()
    {
        boolean hasStopRecordTSD = mapObjectStopRecordsTrackStyleDefinition != null;

        ITrackStyleDefinition tsd = hasStopRecordTSD
                ? mapObjectStopRecordsTrackStyleDefinition
                : mapObjectTrackStyleDefinition;

        // Only enable the check box if the map object has a stop record track style with
        // one of the options defined.
        showStopRecordsCheckBox.setSelected(hasStopRecordTSD &&
                (tsd.hasDefinedTrackShape() || tsd.hasDefinedTrackShapeConstantOpacity() ||
                        tsd.hasDefinedTrackShapeConstantSize() || tsd.hasDefinedTrackShapeConstantColor()));

        // Init the stop records shape
        initShapeToggleButtons(tsd, stopRecordsShapeButtonGroup.getElements());

        // Init the stop records color
        if (hasStopRecordTSD && mapObjectStopRecordsTrackStyleDefinition.getTrackShapeConstantColor() != null)
        {
            stopRecordsColorChooser.setColor(mapObjectStopRecordsTrackStyleDefinition.getTrackShapeConstantColor());
        } else
        {
            Color c = mapObject.getDefaultTrackColor();
            if (null == c)
            {
                c = ImageUtils.generateRandomColor();
                mapObject.setDefaultTrackColor(c);
            }
            stopRecordsColorChooser.setColor(c);
        }

        // Init stop records track opacity
        stopRecordsOpacitySpinner.setValue((int) (tsd.getTrackShapeConstantOpacity() * 100));

        // Init stop records track size
        stopRecordsSizeSpinner.setValue(tsd.getTrackShapeConstantSize());

        enableStopRecordComponents(showStopRecordsCheckBox.isSelected());
    }

    private void initTrackRenderingDetailOptions()
    {
        String userSetting = UserPreferenceEntry.TRACK_DETAIL_LEVEL.getStringValue();

        switch (userSetting)
        {
            case "ludicrous", RendererConstants.HIGH_TRACK_DETAIL_LEVEL ->
            {
                radioButtonHigh.setSelected(true);
                maxAllowableVisibleTracks = userSetting.equals("ludicrous");
            }
            case RendererConstants.MEDIUM_TRACK_DETAIL_LEVEL -> radioButtonMedium.setSelected(true);
            case RendererConstants.LOW_TRACK_DETAIL_LEVEL -> radioButtonLow.setSelected(true);
            default ->
            {
                logger.warn("Invalid track detail level set: {}, using {}", userSetting, RendererConstants.MEDIUM_TRACK_DETAIL_LEVEL);
                radioButtonMedium.setSelected(true);
            }
        }

        final PreferencesNode detailLevelsPrefs = PreferenceEntry.TRACK_DETAIL_LEVELS_NODE.node();

        String high = maxAllowableVisibleTracks
                ? "<font color=\"#ff0000\">10000000</font>"
                : String.valueOf(
                        detailLevelsPrefs.getInt(
                                RendererConstants.HIGH_TRACK_DETAIL_LEVEL,
                                RendererConstants.DEFAULT_HIGH_DETAIL_VISIBLE_TRACKS));
        String medium =
                String.valueOf(
                        detailLevelsPrefs.getInt(
                                RendererConstants.MEDIUM_TRACK_DETAIL_LEVEL,
                                RendererConstants.DEFAULT_MEDIUM_DETAIL_VISIBLE_TRACKS));
        String low =
                String.valueOf(
                        detailLevelsPrefs.getInt(
                                RendererConstants.LOW_TRACK_DETAIL_LEVEL,
                                RendererConstants.DEFAULT_LOW_DETAIL_VISIBLE_TRACKS));

        highDescriptionLabel.setText(DESCRIPTION_GROUPING_PREFIX + high + DESCRIPTION_GROUPING_SUFFIX +
                (maxAllowableVisibleTracks ? " Use with extreme caution!" : "") + HIGH_DESCRIPTION);
        mediumDescriptionLabel.setText(DESCRIPTION_GROUPING_PREFIX + medium + DESCRIPTION_GROUPING_SUFFIX +
                MEDIUM_DESCRIPTION);
        lowDescriptionLabel.setText(DESCRIPTION_GROUPING_PREFIX + low + DESCRIPTION_GROUPING_SUFFIX +
                LOW_DESCRIPTION);
    }

    private void initLineStyle(IConnectingLineDefinition cldForProperty)
    {
        if (cldForProperty.showDirectionalArrow())
        {
            arrowHeadToggleButton.setSelected(true);
        } else if (cldForProperty.getLineStyle() == LineStyle.Solid)
        {
            solidToggleButton.setSelected(true);
        } else
        {
            noStyleToggleButton.setSelected(true);
        }
    }

    private void initShapeToggleButtons(ITrackStyleDefinition tsd, Enumeration<AbstractButton> buttonEnumeration)
    {
        while (buttonEnumeration.hasMoreElements())
        {
            AbstractButton button = buttonEnumeration.nextElement();
            if (button.getActionCommand().equals(tsd.getTrackShape().toString()))
            {
                button.setSelected(true);
                break;
            }
        }
    }

    private void initTimeThreshold(ITrackStyleDefinition tsd)
    {
        // Init the time threshold components
        Map<DateFormatter.TimePeriod, Long> map = DateFormatter.getSimplifiedTime(tsd.getTimeThreshold());

        long timeAmount = 0;
        long origTimeAmount;
        DateFormatter.TimePeriod origTimePeriod;
        DateFormatter.TimePeriod timePeriod = DateFormatter.TimePeriod.MINUTES;
        for (DateFormatter.TimePeriod tp : DateFormatter.TimePeriod.values())
        {
            if (map.containsKey(tp))
            {
                origTimeAmount = map.get(tp);
                if (origTimeAmount == 0)
                {
                    continue;
                }
                timeAmount = origTimeAmount;
                origTimePeriod = tp;
                timePeriod = origTimePeriod;
                break;
            }
        }
        timeComboBox.setSelectedItem(timePeriod);
        timeThresholdSpinner.setValue(timeAmount);
    }

    @Override
    public boolean applyAction()
    {
        // Reset. Can only be set when not in global mode.
        if (checkBoxRestoreDefaults.isSelected())
        {
            // Clear all of the map object's TSD settings and commit them.
            clearProperties();
            if (!createdPlaceholderTSD) persistTSDChanges(mapObjectTrackStyleDefinition);
            if (!createdPlaceholderCLD) persistCLDChanges(mapObjectConnectingLineDefinition);

            checkBoxRestoreDefaults.setSelected(false);
            enableAllComponents(showTracksCheckBox.isSelected());

            // Update UI because the default settings may not be what was
            // being displayed.
            initializeFields();
        } else if (!modifiedComponents.isEmpty())
        {
            if (trackStyleDirty)
            {
                applyTrackStyleDefinitionChanges(globalMode ? globalTrackStyleDefinition : mapObjectTrackStyleDefinition);
            }

            if (lineStyleDirty)
            {
                applyConnectingLineDefinitionChanges(globalMode ? globalConnectingLineDefinition : mapObjectConnectingLineDefinition);
            }

            // Apply the stop record changes
            if (stopRecordStyleDirty && !globalMode)
            {
                applyStopRecordChanges();
            }

            // Special handling for the glyph panel.
            if (!globalMode)
            {
                initGlyphChooser();
            }

            // If we had originally created placeholder TSD/CLD objects for the MO, then we need to also persist
            // the MO here to get the linkage from MO -> TSD/CLD persisted.
            if (createdPlaceholderTSD || createdPlaceholderCLD)
            {
                if (createdPlaceholderTSD && trackStyleDirty)
                {
                    TrackUtils.setTrackStyleDefinition(mapObject, mapObjectTrackStyleDefinition);
                    createdPlaceholderTSD = false;
                }

                if (createdPlaceholderCLD && lineStyleDirty)
                {
                    TrackUtils.setConnectingLineDefinition(mapObject, mapObjectConnectingLineDefinition);
                    createdPlaceholderCLD = false;
                }

                mapObjectPersist.persistMapObject(mapObject);
            }
        }

        // Reset state.
        trackStyleDirty = false;
        lineStyleDirty = false;
        stopRecordStyleDirty = false;
        modifiedComponents.clear();

        return true;
    }

    private void applyTrackStyleDefinitionChanges(ITrackStyleDefinition tsdToModify)
    {
        // Apply show tracks
        if (modifiedComponents.contains(showTracksCheckBox))
        {
            tsdToModify.setVisible(showTracksCheckBox.isSelected());
        }

        // Apply shape
        if (modifiedComponents.contains(sphereToggleButton) ||
                modifiedComponents.contains(coneToggleButton) ||
                modifiedComponents.contains(cubeToggleButton) ||
                modifiedComponents.contains(cylinderToggleButton) ||
                modifiedComponents.contains(noneToggleButton))
        {
            tsdToModify.setTrackShape(TrackShape.valueOf(trackShapeButtonGroup.getSelection().getActionCommand()));
        }

        // Apply color
        if (modifiedComponents.contains(colorChooser))
        {
            tsdToModify.setTrackShapeConstantColor(colorChooser.getColor());
        }

        // Apply track opacity
        if (modifiedComponents.contains(trackOpacitySpinner))
        {
            tsdToModify.setTrackShapeConstantOpacity(((Integer) trackOpacitySpinner.getValue()).floatValue() / 100F);
        }

        // Apply track size
        if (modifiedComponents.contains(trackSizeSpinner))
        {
            Integer trackSizeSpinnerValue = (Integer) trackSizeSpinner.getValue();
            tsdToModify.setTrackShapeConstantSize(trackSizeSpinnerValue);
            tsdToModify.setGlyphConstantSize(trackSizeSpinnerValue);
        }

        // Apply track heading
        if (modifiedComponents.contains(linkShapeOrientationToTrackHeadingCheckBox))
        {
            boolean shapeOrientationToHeading = linkShapeOrientationToTrackHeadingCheckBox.isSelected();
            tsdToModify.setLinkTrackShapeToHeading(shapeOrientationToHeading);
            tsdToModify.setLinkGlyphToHeading(shapeOrientationToHeading);
        }

        // Apply the BucketMode/Group By
        if (modifiedComponents.contains(groupByComboBox))
        {
            tsdToModify.setBucketMode((BucketMode) groupByComboBox.getSelectedItem());
        }

        // Apply the time threshold components
        if (modifiedComponents.contains(timeThresholdSpinner) || modifiedComponents.contains(timeComboBox))
        {
            // Get the user settings.
            long timeAmount = (Long) timeThresholdSpinner.getValue();
            DateFormatter.TimePeriod timePeriod = (DateFormatter.TimePeriod) timeComboBox.getSelectedItem();

            // Map the time to the specified time period.
            HashMap<DateFormatter.TimePeriod, Long> map = new HashMap<>();
            map.put(timePeriod, timeAmount);

            // Combine the time amount and period to produce the threshold.
            long time = DateFormatter.getTimeFromSimplifiedTime(map);

            tsdToModify.setTimeThreshold(time);
        }

        // Apply show heading
        if (modifiedComponents.contains(showHeadingCheckBox))
        {
            tsdToModify.setShowHeading(showHeadingCheckBox.isSelected());
        }

        // Apply fade over time
        if (modifiedComponents.contains(fadeOverTmeCheckBox))
        {
            tsdToModify.setFadedOverTime(fadeOverTmeCheckBox.isSelected());
        }

        // Apply snap to surface
        if (modifiedComponents.contains(snapToSurfaceCheckBox))
        {
            tsdToModify.setSnapToSurface(snapToSurfaceCheckBox.isSelected());
        }

        if (globalMode)
        {
            applyTrackRenderingDetailOptions();
        } else
        {
            applyGlyphSelection(tsdToModify);
        }

        // Persist the changes
        persistTSDChanges(tsdToModify);
    }

    private void applyConnectingLineDefinitionChanges(IConnectingLineDefinition modifiedCLD)
    {
        // Apply line style.
        if (modifiedComponents.contains(noStyleToggleButton)
                || modifiedComponents.contains(arrowHeadToggleButton)
                || modifiedComponents.contains(solidToggleButton))
        {
            modifiedCLD.setLineStyle(noStyleToggleButton.isSelected() ? LineStyle.None : LineStyle.Solid);
            modifiedCLD.setDirectionalArrow(arrowHeadToggleButton.isSelected());
        }

        // Apply line color
        if (modifiedComponents.contains(lineColorChooser))
        {
            modifiedCLD.setLineColor(lineColorChooser.getColor());
        }

        // Apply line width
        if (modifiedComponents.contains(lineWidthSpinner))
        {
            Integer lineWidthSpinnerValue = (Integer) lineWidthSpinner.getValue();
            modifiedCLD.setLineWidth(lineWidthSpinnerValue);
        }

        persistCLDChanges(modifiedCLD);
    }

    private void applyStopRecordChanges()
    {
        if (showStopRecordsCheckBox.isSelected())
        {
            boolean firstCreation = false;

            // Create the new TrackStyleDefinition for our stop records
            if (mapObjectStopRecordsTrackStyleDefinition == null)
            {
                mapObjectStopRecordsTrackStyleDefinition =
                        new TrackStyleDefinitionBuilder(
                                mapObjectTrackStyleDefinition.getFamily(),
                                mapObjectTrackStyleDefinition.getType(),
                                true).setDisplayName("Stop Records for " + mapObjectTrackStyleDefinition.getDisplayName()).build();

                firstCreation = true;
            }

            // Apply the shape
            if (modifiedComponents.contains(stopRecordsSphereToggleButton)
                    || modifiedComponents.contains(stopRecordsCubeToggleButton)
                    || modifiedComponents.contains(stopRecordsConeToggleButton)
                    || modifiedComponents.contains(stopRecordsCylinderToggleButton)
                    || firstCreation)
            {
                String shape = stopRecordsShapeButtonGroup.getSelection().getActionCommand();
                mapObjectStopRecordsTrackStyleDefinition.setTrackShape(TrackShape.valueOf(shape));
            }

            // Apply the color
            if (modifiedComponents.contains(stopRecordsColorChooser) || firstCreation)
            {
                mapObjectStopRecordsTrackStyleDefinition.setTrackShapeConstantColor(stopRecordsColorChooser.getColor());
            }

            // Apply the opacity
            if (modifiedComponents.contains(stopRecordsOpacitySpinner) || firstCreation)
            {
                mapObjectStopRecordsTrackStyleDefinition.setTrackShapeConstantOpacity(((Integer) stopRecordsOpacitySpinner.getValue()).floatValue() / 100F);
            }

            //Apply the size
            if (modifiedComponents.contains(stopRecordsSizeSpinner) || firstCreation)
            {
                mapObjectStopRecordsTrackStyleDefinition.setTrackShapeConstantSize((Integer) stopRecordsSizeSpinner.getValue());
            }

            smartTrackManager.registerTrackStyle(mapObjectStopRecordsTrackStyleDefinition);

            // If we just created this, then find all the stop record RDMs and apply it to them
            if (firstCreation)
            {
                applyStopRecordsToExistingTracks(mapObjectStopRecordsTrackStyleDefinition);
            }
        } else
        {
            // Clear out the stop record associations from the RDMs and the MO
            applyStopRecordsToExistingTracks(null);
        }
    }

    private void applyTrackRenderingDetailOptions()
    {
        String userSetting = UserPreferenceEntry.TRACK_DETAIL_LEVEL.getStringValue();
        String newSetting = null;

        if (radioButtonHigh.isSelected())
        {
            if (maxAllowableVisibleTracks && !userSetting.equals("ludicrous"))
            {
                newSetting = "ludicrous";
            } else if (!maxAllowableVisibleTracks && !userSetting.equals(RendererConstants.HIGH_TRACK_DETAIL_LEVEL))
            {
                newSetting = RendererConstants.HIGH_TRACK_DETAIL_LEVEL;
            }
        } else if (radioButtonMedium.isSelected() && !userSetting.equals(RendererConstants.MEDIUM_TRACK_DETAIL_LEVEL))
        {
            newSetting = RendererConstants.MEDIUM_TRACK_DETAIL_LEVEL;
        } else if (radioButtonLow.isSelected() && !userSetting.equals(RendererConstants.LOW_TRACK_DETAIL_LEVEL))
        {
            newSetting = RendererConstants.LOW_TRACK_DETAIL_LEVEL;
        }

        if (newSetting != null)
        {
            UserPreferenceEntry.TRACK_DETAIL_LEVEL.put(newSetting);
        }
    }

    private void applyGlyphSelection(ITrackStyleDefinition tsd)
    {
        final String selectedImagePath = glyphSelectionPanel.getSelectedImagePath();

        // Glyph changed.
        if (showGlyphCheckBox.isSelected() && selectedImagePath != null && !selectedImagePath.equals(tsd.getGlyphResource()))
        {
            tsd.setGlyphResource(selectedImagePath);

            // If the glyph selected doesn't come from a jar resource, then copy it into the VFS so it's available later
//            if (!selectedImagePath.startsWith("jar"))
//            {
//                if (logger.isDebugEnabled()) logger.debug("Saving glyph to FS: " + selectedImagePath);
//                saveGlyphToVFS(selectedImagePath);
//            }

        } else if (!showGlyphCheckBox.isSelected() && modifiedComponents.contains(showGlyphCheckBox))
        {
            tsd.setGlyphResource(null);
        }
    }

    @InvokeOnEDT
    private void applyStopRecordsToExistingTracks(final ITrackStyleDefinition stopRecordsTSD)
    {
        @SuppressWarnings({"VariableNotUsedInsideIf"})
        String title = (stopRecordsTSD == null ? "Clearing style on " : "Applying style to ") + " Stop Records";

        final ProgressDialog pd = new ProgressDialog(JOptionPane.getRootFrame());
        pd.setText(title + "...");
        pd.pack();

        SwingWorker<Object, Void> sw = new ErrorReportingSwingWorker<>(title, "Failed to update style associations")
        {
            @Override
            protected Object doInBackground()
            {
                pd.setVisible(true);

                long startTime = System.currentTimeMillis();

                // Construct a SQL update to set all the appropriate values in one pass

                String hql = "update MapPoint " +
                        " set overrideTrackStyleId = " + (stopRecordsTSD == null ? "NULL" : stopRecordsTSD.getId());
                // I don't think this is correct, but within the scope of fixing CORE-4017 I don't have time
                // to go dig around to verify it.  You shouldn't need to set the MO's TSD on every RDM for that
                // MO.  That's the whole point of the TrackStylingContext.
                if (mapObjectTrackStyleDefinition != null)
                {
                    hql += ", trackStyleId = " + mapObjectTrackStyleDefinition.getId();
                }
                hql += " where hasMotion = false and mapObject_id = " + mapObject.getId();

                final long count = persistServiceProvider.getPersistService().executeNativeUpdateSQL(hql);

                final long endTime = System.currentTimeMillis();

                if (logger.isDebugEnabled())
                {
                    logger.debug("Updated {} RDMs in {} ms", count, (endTime - startTime));

                    if (stopRecordsTSD != null)
                    {
                        logger.debug(stopRecordsTSD.getTrackShape().toString());
                        logger.debug("{}", stopRecordsTSD.getTrackShapeConstantSize());
                        logger.debug(stopRecordsTSD.getTrackShapeConstantColor().toString());
                    } else
                    {
                        logger.debug("StopRecordsTSD is null");
                    }
                }

                // If we are clearing out the stop record TSD, then update everything accordingly
                final boolean changed;

                if (stopRecordsTSD == null)// && ((Long) result) > 0)
                {
                    // Unregistering the TSD (also deletes it and is why we need to persist the MO above)

                    //TODO dont think we want to be deleting the tsd
                    // Raptor.getProjectSession().getSmartTrackManager().unregisterTrackStyle(mapObjectStopRecordsTrackStyleDefinition);
                    mapObjectStopRecordsTrackStyleDefinition = null;

                    changed = null != TrackUtils.getStopRecordsTrackStyleDefinition(mapObject);

                    // clear the reference in the MO and Persist it so that it's reference is properly null in the DB
                    TrackUtils.setStopRecordsTrackStyleDefinition(mapObject, null);
                } else
                {
                    if (smartTrackManager.getTrackStyleDefinitionByID(mapObjectStopRecordsTrackStyleDefinition.getId()) == null)
                    {
                        smartTrackManager.registerTrackStyle(mapObjectStopRecordsTrackStyleDefinition);
                    }

                    changed = !Objects.equals(TrackUtils.getStopRecordsTrackStyleDefinition(mapObject), stopRecordsTSD);

                    TrackUtils.setStopRecordsTrackStyleDefinition(mapObject, stopRecordsTSD);
                }

                // If a change was made to the MO, get it persisted
                if (changed)
                {
                    mapObjectPersist.persistMapObject(mapObject);
                }
                return count;
            }

            @Override
            protected void cleanup()
            {
                // Ensure that this occurs after the "setVisible" call, which was also called using invokeLater
                SwingUtilities.invokeLater(() -> {
                    pd.setVisible(false);
                    pd.dispose();
                });
            }
        };
        sw.execute();
    }

    private void addListeners()
    {
        // Track options
        // -----------------------------------------------------------------------------

        ComponentActionListener trackActionListener = new ComponentActionListener(TRACK);
        ComponentChangeListener trackChangeListener = new ComponentChangeListener(TRACK);

        // General panel
        // ------------------------------------------
        showTracksCheckBox.addActionListener(trackActionListener);
        noneToggleButton.addActionListener(trackActionListener);
        sphereToggleButton.addActionListener(trackActionListener);
        coneToggleButton.addActionListener(trackActionListener);
        cylinderToggleButton.addActionListener(trackActionListener);
        cubeToggleButton.addActionListener(trackActionListener);
        trackOpacitySpinner.addChangeListener(trackChangeListener);
        trackSizeSpinner.addChangeListener(trackChangeListener);
        showHeadingCheckBox.addActionListener(trackActionListener);
        snapToSurfaceCheckBox.addActionListener(trackActionListener);

        if (!globalMode)
        {
            colorChooser.addActionListener(trackActionListener);
        }

        // Glyphs panel
        // ------------------------------------------
        if (!globalMode)
        {
            showGlyphCheckBox.addActionListener(trackActionListener);
        }

        // Track rendering panel
        // ------------------------------------------
        if (globalMode)
        {
            radioButtonHigh.addActionListener(trackActionListener);
            radioButtonMedium.addActionListener(trackActionListener);
            radioButtonLow.addActionListener(trackActionListener);
        }

        // Advanced panel
        // ------------------------------------------
        groupByComboBox.addActionListener(trackActionListener);
        timeThresholdSpinner.addChangeListener(trackChangeListener);
        timeComboBox.addActionListener(trackActionListener);
        fadeOverTmeCheckBox.addActionListener(trackActionListener);
        linkShapeOrientationToTrackHeadingCheckBox.addActionListener(trackActionListener);

        // Connecting line related options
        // -----------------------------------------------------------------------------

        ComponentActionListener lineActionListener = new ComponentActionListener(LINE);
        ComponentChangeListener lineChangeListener = new ComponentChangeListener(LINE);

        noStyleToggleButton.addActionListener(lineActionListener);
        solidToggleButton.addActionListener(lineActionListener);
        arrowHeadToggleButton.addActionListener(lineActionListener);
        lineColorChooser.addActionListener(lineActionListener);
        lineWidthSpinner.addChangeListener(lineChangeListener);

        // Stop record track options
        // -----------------------------------------------------------------------------

        if (!globalMode)
        {
            ComponentActionListener stopRecordActionListener = new ComponentActionListener(STOP_RECORD);
            ComponentChangeListener stopRecordChangeListener = new ComponentChangeListener(STOP_RECORD);

            showStopRecordsCheckBox.addActionListener(stopRecordActionListener);
            stopRecordsSphereToggleButton.addActionListener(stopRecordActionListener);
            stopRecordsCubeToggleButton.addActionListener(stopRecordActionListener);
            stopRecordsCylinderToggleButton.addActionListener(stopRecordActionListener);
            stopRecordsCylinderToggleButton.addActionListener(stopRecordActionListener);
            stopRecordsColorChooser.addActionListener(stopRecordActionListener);
            stopRecordsOpacitySpinner.addChangeListener(stopRecordChangeListener);
            stopRecordsSizeSpinner.addChangeListener(stopRecordChangeListener);
        }
    }

    private IConnectingLineDefinition getCLDForProperty(boolean propertyDefinedForMapObject)
    {
        if (propertyDefinedForMapObject)
        {
            return mapObjectConnectingLineDefinition;
        } else
        {
            return globalConnectingLineDefinition;
        }
    }

    /**
     * This will update the Map Object's TSD and fire the events associated with each
     * clear request, so only call this method when you are sure you want to commit the
     * clears!
     */
    private void clearProperties()
    {
        // Reset track style properties
        mapObjectTrackStyleDefinition.clearDefinedVisible();
        mapObjectTrackStyleDefinition.clearDefinedTrackShape();
        mapObjectTrackStyleDefinition.clearDefinedTrackShapeConstantColor();
        mapObjectTrackStyleDefinition.clearDefinedTrackShapeConstantOpacity();
        mapObjectTrackStyleDefinition.clearDefinedTrackShapeConstantSize();
        mapObjectTrackStyleDefinition.clearDefinedLinkTrackShapeToHeading();
        mapObjectTrackStyleDefinition.clearDefinedBucketMode();
        mapObjectTrackStyleDefinition.clearDefinedTimeThreshold();
        mapObjectTrackStyleDefinition.clearDefinedShowHeading();
        mapObjectTrackStyleDefinition.clearDefinedFadedOverTime();
        mapObjectTrackStyleDefinition.clearDefinedSnapToSurface();
        mapObjectTrackStyleDefinition.clearDefinedGlyphConstantSize();
        mapObjectTrackStyleDefinition.clearDefinedGlyphResource();

        // Reset connecting line properties
        mapObjectConnectingLineDefinition.clearDefinedLineStyle();
        mapObjectConnectingLineDefinition.clearDefinedDirectionalArrow();
        mapObjectConnectingLineDefinition.clearDefinedLineColor();
        mapObjectConnectingLineDefinition.clearDefinedLineWidth();

        // Reset stop record properties
        if (mapObjectStopRecordsTrackStyleDefinition != null)
        {
            mapObjectStopRecordsTrackStyleDefinition.clearDefinedVisible();
            mapObjectStopRecordsTrackStyleDefinition.clearDefinedTrackShapeConstantColor();
            mapObjectStopRecordsTrackStyleDefinition.clearDefinedTrackShapeConstantOpacity();
            mapObjectStopRecordsTrackStyleDefinition.clearDefinedTrackShapeConstantSize();
        }
    }

    private void persistTSDChanges(ITrackStyleDefinition modifiedTSD)
    {
        smartTrackPersist.persistTrackStyleDefinition(modifiedTSD);
    }

    private void persistCLDChanges(IConnectingLineDefinition modifiedCLD)
    {
        smartTrackPersist.persistConnectingLineDefinition(modifiedCLD);
    }

    @Override
    public void restoreDefaults()
    {
        checkBoxRestoreDefaults.setSelected(true);
    }

    private void enableTimeThresholdComponents(boolean enable)
    {
        // We don't want to enable the components if the timeThresholdCheckBox isn't selected
        timeThresholdLabel.setEnabled(enable);
        timeThresholdSpinner.setEnabled(enable);
        timeComboBox.setEnabled(enable);
    }

    private void enableGroupByComponents(boolean enable)
    {
        groupByLabel.setEnabled(enable);
        groupByComboBox.setEnabled(enable);
    }

    private void enableTrackShapeComponents(boolean enable)
    {
        final Enumeration<AbstractButton> buttonEnumeration = trackShapeButtonGroup.getElements();
        while (buttonEnumeration.hasMoreElements())
        {
            AbstractButton button = buttonEnumeration.nextElement();
            button.setEnabled(enable);
        }
    }

    private void enableOpacityComponents(boolean enable)
    {
        trackOpacityLabel.setEnabled(enable);
        trackOpacitySpinner.setEnabled(enable);
    }

    private void enableSizeComponents(boolean enable)
    {
        trackSizeLabel.setEnabled(enable);
        trackSizeSpinner.setEnabled(enable);
    }

    private void enableLinkOrientationToTrackHeadingComponents(boolean enable)
    {
        linkShapeOrientationToTrackHeadingCheckBox.setEnabled(enable);
    }

    private void enableAllComponents(boolean enable)
    {
        enableTrackShapeComponents(enable);
        enableColorComponents(enable);
        enableOpacityComponents(enable);
        enableSizeComponents(enable);
        enableLinkOrientationToTrackHeadingComponents(enable);
        enableGroupByComponents(enable);
        enableTimeThresholdComponents(enable);
        showHeadingCheckBox.setEnabled(enable);
        fadeOverTmeCheckBox.setEnabled(enable);
        snapToSurfaceCheckBox.setEnabled(enable);
        showGlyphCheckBox.setEnabled(enable);
        showStopRecordsCheckBox.setEnabled(enable);

        glyphSelectionPanel.setEnabled(enable && showGlyphCheckBox.isSelected());

        enableLineStyleComponents(enable);

        if (enable)
        {
            enableLineNonStyleComponents(!lineButtonGroup.isSelected(noStyleToggleButton.getModel()));
        } else
        {
            enableLineNonStyleComponents(false);
        }
    }

    private void enableColorComponents(boolean enable)
    {
        colorLabel.setEnabled(enable);
        colorChooser.setEnabled(enable);
    }

    // Connecting Lines enable
    private void enableLineWidthComponents(boolean enable)
    {
        lineWidthLabel.setEnabled(enable);
        lineWidthSpinner.setEnabled(enable);
    }

    private void enableLineColorComponents(boolean enable)
    {
        lineColorLabel.setEnabled(enable);
        lineColorChooser.setEnabled(enable);
    }

    private void enableLineStyleComponents(boolean enabled)
    {
        lineStyleLabel.setEnabled(enabled);
        Enumeration<AbstractButton> buttons = lineButtonGroup.getElements();
        while (buttons.hasMoreElements())
        {
            AbstractButton button = buttons.nextElement();
            button.setEnabled(enabled);
        }
    }

    private void enableLineNonStyleComponents(boolean enable)
    {
        enableLineColorComponents(enable);
        enableLineWidthComponents(enable);
    }

    // Stop records enable
    private void enableStopRecordComponents(boolean enable)
    {
        Enumeration<AbstractButton> buttons = stopRecordsShapeButtonGroup.getElements();
        while (buttons.hasMoreElements())
        {
            AbstractButton button = buttons.nextElement();
            button.setEnabled(enable);
        }
        stopRecordsColorLabel.setEnabled(enable);
        stopRecordsColorChooser.setEnabled(enable);
        stopRecordsOpacityLabel.setEnabled(enable);
        stopRecordsOpacitySpinner.setEnabled(enable);
        stopRecordsSizeLabel.setEnabled(enable);
        stopRecordsSizeSpinner.setEnabled(enable);
    }

    @Override
    public void iconSelected(String iconPath)
    {
        if (iconPath != null)
        {
            setGlyphLabelImage(iconPath);

            modifiedComponents.add(glyphLabel);

            setDirty();

            // Must set this manually since none of our action listeners were involved in the change
            trackStyleDirty = true;
        } else
        {
            glyphLabel.setIcon(null);
        }
    }

    /**
     * Set the image on the glyph label.
     *
     * @param iconPath The path to the icon, may be a URL (like vfs:...) or a file path.
     */
    private void setGlyphLabelImage(String iconPath)
    {
        try
        {
            final URL url = iconPath.startsWith(Schemes.VFS_PREFIX)
                    ? new URL(iconPath)
                    : getClass().getClassLoader().getResource(iconPath);

            if (url != null)
            {
                final BufferedImage image = ImageUtils.getImage(url, false);
                if (image != null) glyphLabel.setIcon(ImageUtils.getScaledIcon(image, 24, -1));
            }
        } catch (MalformedURLException e)
        {
            logger.error("Failed to read icon: {} : {}", iconPath, e.getMessage());
        }
    }

    private void setDirty()
    {
        setPanelDirty();
    }

    private ITrackStyleDefinition getTSDForProperty(boolean overrideGlobalDefinition)
    {
        if (overrideGlobalDefinition)
        {
            return mapObjectTrackStyleDefinition;
        } else
        {
            return globalTrackStyleDefinition;
        }
    }

    private void showStopRecordsCheckBoxItemStateChanged()
    {
        enableStopRecordComponents(showStopRecordsCheckBox.isSelected());
        modifiedComponents.add(showStopRecordsCheckBox);

        setDirty();
    }

    private void showGlyphCheckBoxItemStateChanged()
    {
        glyphSelectionPanel.setEnabled(showGlyphCheckBox.isSelected());

        if (glyphSelectionPanel.getSelectedImagePath() != null)
        {
            setDirty();
        }
    }

    private void noStyleToggleButtonItemStateChanged()
    {
        enableLineNonStyleComponents(!lineButtonGroup.isSelected(noStyleToggleButton.getModel()));
        setPanelDirty();
    }

    private void showTracksCheckBoxItemStateChanged()
    {
        enableAllComponents(showTracksCheckBox.isSelected());
        setPanelDirty();
    }

    private void tabbedPaneStateChanged()
    {
        // We don't want to create the list of glyphs until the panel is selected
        if (tabbedPane.getSelectedComponent().equals(glyphPanel))
        {
            if (!glyphPanelInitialized)
            {
                initGlyphChooser();
                glyphPanelInitialized = true;
            }
        }
    }

    private void radioButtonHighActionPerformed()
    {
        setPanelDirty();
    }

    private void radioButtonMediumActionPerformed()
    {
        setPanelDirty();
    }

    private void radioButtonLowActionPerformed()
    {
        setPanelDirty();
    }

    private void radioButtonHighMouseClicked(MouseEvent e)
    {
        // Quintuple click while holding CTRL gets you God
        // rendering, as in "So help you, God."
        if (e.getClickCount() == 5 && e.getModifiers() == (InputEvent.CTRL_MASK + InputEvent.BUTTON1_MASK))
        {
            maxAllowableVisibleTracks = true;
            String text = "<html>Up to <font color=\"#ff0000\">10000000</font> tracks " +
                    "will be displayed before grouping logic is applied. Use with extreme caution!";
            highDescriptionLabel.setText(text + HIGH_DESCRIPTION);
            logger.warn("User has entered ludicrous track rendering.");
        }
    }

    private void checkBoxRestoreDefaultsItemStateChanged()
    {
        if (checkBoxRestoreDefaults.isSelected())
        {
            // Disable panels, update restoreDefaults flag.
            showTracksCheckBox.setEnabled(false);
            enableAllComponents(false);

            if (!globalMode)
            {
                enableStopRecordComponents(false);
            }

            setDirty();
        } else
        {
            // Enable panels, update restoreDefaults flag.
            showTracksCheckBox.setEnabled(true);
            enableAllComponents(showTracksCheckBox.isSelected());

            if (!globalMode)
            {
                enableStopRecordComponents(showStopRecordsCheckBox.isSelected());
            }

            setDirty();
        }
    }

    @Override
    public void releaseResources()
    {
    }

    private void componentActionOrChange(Component c, int type)
    {
        modifiedComponents.add(c);

        switch (type)
        {
            case TRACK -> trackStyleDirty = true;
            case LINE -> lineStyleDirty = true;
            case STOP_RECORD -> stopRecordStyleDirty = true;
        }
    }

    private void colorChooserPropertyChange(PropertyChangeEvent e)
    {
        if (COLOR.equals(e.getPropertyName()) && !e.getNewValue().equals(e.getOldValue()))
        {
            setDirty();
            if (e.getSource() instanceof ColorChooser source)
            {
                componentActionOrChange(source, 0);
                if (COLORCHOOSER.equals(source.getName()))
                {
                    trackStyleDirty = true;
                } else if (LINE_COLOR_CHOOSER.equals(source.getName()))
                {
                    lineStyleDirty = true;
                } else if (STOP_RECORDS_COLOR_CHOOSER.equals(source.getName()))
                {
                    stopRecordStyleDirty = true;
                }
            }
        }
    }

    private class ComponentActionListener implements ActionListener
    {
        private final int type;

        public ComponentActionListener(int type)
        {
            this.type = type;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (!initializing)
            {
                componentActionOrChange((Component) e.getSource(), type);
            }
        }
    }

    private class ComponentChangeListener implements ChangeListener
    {
        private final int type;

        public ComponentChangeListener(int type)
        {
            this.type = type;
        }

        @Override
        public void stateChanged(ChangeEvent e)
        {
            if (!initializing)
            {
                componentActionOrChange((Component) e.getSource(), type);
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        showTracksCheckBox = new JCheckBox();
        tabbedPane = new JTabbedPane();
        var generalPanel = new JPanel();
        var styleLabel = new JLabel();
        var shapeButtonPanel = new JPanel();
        noneToggleButton = new JToggleButton();
        sphereToggleButton = new JToggleButton();
        coneToggleButton = new JToggleButton();
        cylinderToggleButton = new JToggleButton();
        cubeToggleButton = new JToggleButton();
        colorLabel = new JLabel();
        colorChooser = new ColorChooser();
        trackOpacityLabel = new JLabel();
        trackOpacitySpinner = new JSpinner();
        trackSizeLabel = new JLabel();
        trackSizeSpinner = new JSpinner();
        showHeadingCheckBox = new JCheckBox();
        snapToSurfaceCheckBox = new JCheckBox();
        var connectingLinesPanel = new JPanel();
        lineStyleLabel = new JLabel();
        var panel6 = new JPanel();
        noStyleToggleButton = new JToggleButton();
        solidToggleButton = new JToggleButton();
        arrowHeadToggleButton = new JToggleButton();
        lineColorLabel = new JLabel();
        lineColorChooser = new ColorChooser();
        lineWidthLabel = new JLabel();
        lineWidthSpinner = new JSpinner();
        stopRecordsPanel = new JPanel();
        showStopRecordsCheckBox = new JCheckBox();
        var stopRecordsShapeButtonPanel = new JPanel();
        stopRecordsSphereToggleButton = new JToggleButton();
        stopRecordsConeToggleButton = new JToggleButton();
        stopRecordsCylinderToggleButton = new JToggleButton();
        stopRecordsCubeToggleButton = new JToggleButton();
        stopRecordsColorLabel = new JLabel();
        stopRecordsColorChooser = new ColorChooser();
        stopRecordsOpacityLabel = new JLabel();
        stopRecordsOpacitySpinner = new JSpinner();
        stopRecordsSizeLabel = new JLabel();
        stopRecordsSizeSpinner = new JSpinner();
        glyphPanel = new JPanel();
        showGlyphCheckBox = new JCheckBox();
        glyphLabel = new JLabel();
        glyphSelectionPanel = new GlyphSelectionPanel();
        panelRendering = new JPanel();
        renderingDescriptionLabel = new WrappingLabel();
        var separator3 = new JSeparator();
        radioButtonHigh = new JRadioButton();
        highDescriptionLabel = new WrappingLabel();
        var separator2 = new JSeparator();
        radioButtonMedium = new JRadioButton();
        mediumDescriptionLabel = new WrappingLabel();
        var separator4 = new JSeparator();
        radioButtonLow = new JRadioButton();
        lowDescriptionLabel = new WrappingLabel();
        var advancedPanel = new JPanel();
        groupByLabel = new JLabel();
        groupByComboBox = new JComboBox<>();
        timeThresholdLabel = new JLabel();
        timeThresholdSpinner = new JSpinner();
        timeComboBox = new JComboBox<>();
        fadeOverTmeCheckBox = new JCheckBox();
        linkShapeOrientationToTrackHeadingCheckBox = new JCheckBox();
        checkBoxRestoreDefaults = new JCheckBox();
        trackShapeButtonGroup = new ButtonGroup();
        lineButtonGroup = new ButtonGroup();
        stopRecordsShapeButtonGroup = new ButtonGroup();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, fill:default:grow, $lgap, default"));

        //---- showTracksCheckBox ----
        showTracksCheckBox.setText("Show Tracks");
        showTracksCheckBox.setToolTipText("Show or hide all tracks. This overrides any other settings");
        showTracksCheckBox.setName("showTracksCheckBox");
        showTracksCheckBox.addItemListener(e -> showTracksCheckBoxItemStateChanged());
        add(showTracksCheckBox, cc.xy(1, 1));

        //======== tabbedPane ========
        {
            tabbedPane.setName("tabbedPane");
            tabbedPane.addChangeListener(e -> tabbedPaneStateChanged());

            //======== generalPanel ========
            {
                generalPanel.setName("generalPanel");
                generalPanel.setLayout(new FormLayout(
                        "2*(default, $lcgap), default:grow",
                        "5*(default, $lgap), default"));

                //---- styleLabel ----
                styleLabel.setText("Style:");
                styleLabel.setName("styleLabel");
                generalPanel.add(styleLabel, cc.xy(1, 1));

                //======== shapeButtonPanel ========
                {
                    shapeButtonPanel.setName("shapeButtonPanel");
                    shapeButtonPanel.setLayout(new FormLayout(
                            "4*(default, $rgap), default",
                            "fill:default"));

                    //---- noneToggleButton ----
                    noneToggleButton.setActionCommand("None");
                    noneToggleButton.setToolTipText("Track shapes will be hidden");
                    noneToggleButton.setName("noneToggleButton");
                    noneToggleButton.addItemListener(e -> setDirty());
                    shapeButtonPanel.add(noneToggleButton, cc.xy(1, 1));

                    //---- sphereToggleButton ----
                    sphereToggleButton.setSelected(true);
                    sphereToggleButton.setToolTipText("Track shape will be a Sphere");
                    sphereToggleButton.setActionCommand("Sphere");
                    sphereToggleButton.setName("sphereToggleButton");
                    sphereToggleButton.addItemListener(e -> setDirty());
                    shapeButtonPanel.add(sphereToggleButton, cc.xy(3, 1));

                    //---- coneToggleButton ----
                    coneToggleButton.setToolTipText("Track shape will be a Cone");
                    coneToggleButton.setActionCommand("Cone");
                    coneToggleButton.setName("coneToggleButton");
                    coneToggleButton.addItemListener(e -> setDirty());
                    shapeButtonPanel.add(coneToggleButton, cc.xy(5, 1));

                    //---- cylinderToggleButton ----
                    cylinderToggleButton.setToolTipText("Track shape will be a Cylinder");
                    cylinderToggleButton.setActionCommand("Cylinder");
                    cylinderToggleButton.setName("cylinderToggleButton");
                    cylinderToggleButton.addItemListener(e -> setDirty());
                    shapeButtonPanel.add(cylinderToggleButton, cc.xy(7, 1));

                    //---- cubeToggleButton ----
                    cubeToggleButton.setToolTipText("Track shape will be a Cube");
                    cubeToggleButton.setActionCommand("Cube");
                    cubeToggleButton.setName("cubeToggleButton");
                    cubeToggleButton.addItemListener(e -> setDirty());
                    shapeButtonPanel.add(cubeToggleButton, cc.xy(9, 1));
                }
                generalPanel.add(shapeButtonPanel, cc.xywh(3, 1, 3, 1));

                //---- colorLabel ----
                colorLabel.setText("Color:");
                colorLabel.setToolTipText("Sets the color of the track shape");
                colorLabel.setName("colorLabel");
                generalPanel.add(colorLabel, cc.xy(1, 3));

                //---- colorChooser ----
                colorChooser.setToolTipText("Sets the color of the track shape");
                colorChooser.setName("colorChooser");
                colorChooser.addActionListener(e -> setDirty());
                colorChooser.addPropertyChangeListener(e -> colorChooserPropertyChange(e));
                generalPanel.add(colorChooser, cc.xy(3, 3));

                //---- trackOpacityLabel ----
                trackOpacityLabel.setText("Opacity:");
                trackOpacityLabel.setToolTipText("Sets the opacity of the track shape");
                trackOpacityLabel.setName("trackOpacityLabel");
                generalPanel.add(trackOpacityLabel, cc.xy(1, 5));

                //---- trackOpacitySpinner ----
                trackOpacitySpinner.setModel(new SpinnerNumberModel(100, 0, 100, 10));
                trackOpacitySpinner.setToolTipText("Sets the opacity of the track shape");
                trackOpacitySpinner.setName("trackOpacitySpinner");
                trackOpacitySpinner.addChangeListener(e -> setDirty());
                generalPanel.add(trackOpacitySpinner, cc.xy(3, 5));

                //---- trackSizeLabel ----
                trackSizeLabel.setText("Size:");
                trackSizeLabel.setToolTipText("Sets the size of the track shape and the glyph if selected");
                trackSizeLabel.setName("trackSizeLabel");
                generalPanel.add(trackSizeLabel, cc.xy(1, 7));

                //---- trackSizeSpinner ----
                trackSizeSpinner.setModel(new SpinnerNumberModel(3, 1, 10, 1));
                trackSizeSpinner.setToolTipText("Sets the size of the track shape and the glyph if selected");
                trackSizeSpinner.setName("trackSizeSpinner");
                trackSizeSpinner.addChangeListener(e -> setDirty());
                generalPanel.add(trackSizeSpinner, cc.xy(3, 7));

                //---- showHeadingCheckBox ----
                showHeadingCheckBox.setText("Show heading indicator");
                showHeadingCheckBox.setToolTipText("Shows an arrow pointed in the direction of the heading");
                showHeadingCheckBox.setName("showHeadingCheckBox");
                showHeadingCheckBox.addItemListener(e -> setDirty());
                generalPanel.add(showHeadingCheckBox, cc.xywh(1, 9, 5, 1));

                //---- snapToSurfaceCheckBox ----
                snapToSurfaceCheckBox.setText("Snap track shapes to surface");
                snapToSurfaceCheckBox.setToolTipText("Used to make all tracks snap to the surface of the globe");
                snapToSurfaceCheckBox.setName("snapToSurfaceCheckBox");
                snapToSurfaceCheckBox.addItemListener(e -> setDirty());
                generalPanel.add(snapToSurfaceCheckBox, cc.xywh(1, 11, 5, 1));
            }
            tabbedPane.addTab("Shapes", generalPanel);

            //======== connectingLinesPanel ========
            {
                connectingLinesPanel.setName("connectingLinesPanel");
                connectingLinesPanel.setLayout(new FormLayout(
                        "pref, $lcgap, default, $lcgap, default:grow",
                        "2*(default, $lgap), default"));

                //---- lineStyleLabel ----
                lineStyleLabel.setText("Line Style:");
                lineStyleLabel.setName("lineStyleLabel");
                connectingLinesPanel.add(lineStyleLabel, cc.xy(1, 1));

                //======== panel6 ========
                {
                    panel6.setName("panel6");
                    panel6.setLayout(new FormLayout(
                            "2*(default, $rgap), default",
                            "fill:default"));

                    //---- noStyleToggleButton ----
                    noStyleToggleButton.setSelected(true);
                    noStyleToggleButton.setActionCommand("None");
                    noStyleToggleButton.setToolTipText("Removes lines between tracks");
                    noStyleToggleButton.setName("noStyleToggleButton");
                    noStyleToggleButton.addItemListener(e -> noStyleToggleButtonItemStateChanged());
                    panel6.add(noStyleToggleButton, cc.xy(1, 1));

                    //---- solidToggleButton ----
                    solidToggleButton.setActionCommand("Solid");
                    solidToggleButton.setToolTipText("Adds a solid line between tracks");
                    solidToggleButton.setName("solidToggleButton");
                    solidToggleButton.addItemListener(e -> setDirty());
                    panel6.add(solidToggleButton, cc.xy(3, 1));

                    //---- arrowHeadToggleButton ----
                    arrowHeadToggleButton.setActionCommand("Dashed");
                    arrowHeadToggleButton.setToolTipText("Adds a line with a directional arrow between tracks");
                    arrowHeadToggleButton.setName("arrowHeadToggleButton");
                    arrowHeadToggleButton.addItemListener(e -> setDirty());
                    panel6.add(arrowHeadToggleButton, cc.xy(5, 1));
                }
                connectingLinesPanel.add(panel6, cc.xywh(3, 1, 3, 1));

                //---- lineColorLabel ----
                lineColorLabel.setText("  Color:");
                lineColorLabel.setToolTipText("Sets the color of the connecting line");
                lineColorLabel.setName("lineColorLabel");
                connectingLinesPanel.add(lineColorLabel, cc.xy(1, 3));

                //---- lineColorChooser ----
                lineColorChooser.setToolTipText("Sets the color of the connecting line");
                lineColorChooser.setName("lineColorChooser");
                lineColorChooser.addActionListener(e -> setDirty());
                lineColorChooser.addPropertyChangeListener(e -> colorChooserPropertyChange(e));
                connectingLinesPanel.add(lineColorChooser, cc.xy(3, 3));

                //---- lineWidthLabel ----
                lineWidthLabel.setText("  Width:");
                lineWidthLabel.setToolTipText("Sets the width of the connecting lines");
                lineWidthLabel.setName("lineWidthLabel");
                connectingLinesPanel.add(lineWidthLabel, cc.xy(1, 5));

                //---- lineWidthSpinner ----
                lineWidthSpinner.setModel(new SpinnerNumberModel(5, 1, 10, 1));
                lineWidthSpinner.setToolTipText("Sets the width of the connecting lines");
                lineWidthSpinner.setName("lineWidthSpinner");
                lineWidthSpinner.addChangeListener(e -> setDirty());
                connectingLinesPanel.add(lineWidthSpinner, cc.xy(3, 5));
            }
            tabbedPane.addTab("Lines", connectingLinesPanel);

            //======== stopRecordsPanel ========
            {
                stopRecordsPanel.setName("stopRecordsPanel");
                stopRecordsPanel.setLayout(new FormLayout(
                        "left:default, $lcgap, default, $lcgap, default:grow",
                        "top:default, 4*($lgap, default)"));

                //---- showStopRecordsCheckBox ----
                showStopRecordsCheckBox.setText("Show Stop Records");
                showStopRecordsCheckBox.setToolTipText("Shows or hides Stop Records");
                showStopRecordsCheckBox.setName("showStopRecordsCheckBox");
                showStopRecordsCheckBox.addItemListener(e -> showStopRecordsCheckBoxItemStateChanged());
                stopRecordsPanel.add(showStopRecordsCheckBox, cc.xywh(1, 1, 5, 1));

                //======== stopRecordsShapeButtonPanel ========
                {
                    stopRecordsShapeButtonPanel.setName("stopRecordsShapeButtonPanel");
                    stopRecordsShapeButtonPanel.setLayout(new FormLayout(
                            "3*(default, $rgap), default",
                            "fill:default"));

                    //---- stopRecordsSphereToggleButton ----
                    stopRecordsSphereToggleButton.setSelected(true);
                    stopRecordsSphereToggleButton.setToolTipText("Stop Record track shape will be a Sphere");
                    stopRecordsSphereToggleButton.setActionCommand("Sphere");
                    stopRecordsSphereToggleButton.setName("stopRecordsSphereToggleButton");
                    stopRecordsSphereToggleButton.addItemListener(e -> setDirty());
                    stopRecordsShapeButtonPanel.add(stopRecordsSphereToggleButton, cc.xy(1, 1));

                    //---- stopRecordsConeToggleButton ----
                    stopRecordsConeToggleButton.setToolTipText("Stop Record track shape will be a Cone");
                    stopRecordsConeToggleButton.setActionCommand("Cone");
                    stopRecordsConeToggleButton.setName("stopRecordsConeToggleButton");
                    stopRecordsConeToggleButton.addItemListener(e -> setDirty());
                    stopRecordsShapeButtonPanel.add(stopRecordsConeToggleButton, cc.xy(3, 1));

                    //---- stopRecordsCylinderToggleButton ----
                    stopRecordsCylinderToggleButton.setToolTipText("Stop Record track shape will be a Cylinder");
                    stopRecordsCylinderToggleButton.setActionCommand("Cylinder");
                    stopRecordsCylinderToggleButton.setName("stopRecordsCylinderToggleButton");
                    stopRecordsCylinderToggleButton.addItemListener(e -> setDirty());
                    stopRecordsShapeButtonPanel.add(stopRecordsCylinderToggleButton, cc.xy(5, 1));

                    //---- stopRecordsCubeToggleButton ----
                    stopRecordsCubeToggleButton.setToolTipText("Stop Record track shape will be a Cube");
                    stopRecordsCubeToggleButton.setActionCommand("Cube");
                    stopRecordsCubeToggleButton.setName("stopRecordsCubeToggleButton");
                    stopRecordsCubeToggleButton.addItemListener(e -> setDirty());
                    stopRecordsShapeButtonPanel.add(stopRecordsCubeToggleButton, cc.xy(7, 1));
                }
                stopRecordsPanel.add(stopRecordsShapeButtonPanel, cc.xywh(1, 3, 5, 1));

                //---- stopRecordsColorLabel ----
                stopRecordsColorLabel.setText("Color:");
                stopRecordsColorLabel.setToolTipText("Used to set Stop Record track color");
                stopRecordsColorLabel.setName("stopRecordsColorLabel");
                stopRecordsPanel.add(stopRecordsColorLabel, cc.xy(1, 5));

                //---- stopRecordsColorChooser ----
                stopRecordsColorChooser.setToolTipText("Used to set Stop Record track color");
                stopRecordsColorChooser.setName("stopRecordsColorChooser");
                stopRecordsColorChooser.addActionListener(e -> setDirty());
                stopRecordsColorChooser.addPropertyChangeListener(e -> colorChooserPropertyChange(e));
                stopRecordsPanel.add(stopRecordsColorChooser, cc.xy(3, 5));

                //---- stopRecordsOpacityLabel ----
                stopRecordsOpacityLabel.setText("Opacity:");
                stopRecordsOpacityLabel.setToolTipText("Used to set Stop Record track opacity");
                stopRecordsOpacityLabel.setName("stopRecordsOpacityLabel");
                stopRecordsPanel.add(stopRecordsOpacityLabel, cc.xy(1, 7));

                //---- stopRecordsOpacitySpinner ----
                stopRecordsOpacitySpinner.setModel(new SpinnerNumberModel(100, 0, 100, 10));
                stopRecordsOpacitySpinner.setToolTipText("Used to set Stop Record track opacity");
                stopRecordsOpacitySpinner.setName("stopRecordsOpacitySpinner");
                stopRecordsOpacitySpinner.addChangeListener(e -> setDirty());
                stopRecordsPanel.add(stopRecordsOpacitySpinner, cc.xy(3, 7));

                //---- stopRecordsSizeLabel ----
                stopRecordsSizeLabel.setText("Size:");
                stopRecordsSizeLabel.setToolTipText("Used to set Stop Record track size");
                stopRecordsSizeLabel.setName("stopRecordsSizeLabel");
                stopRecordsPanel.add(stopRecordsSizeLabel, cc.xy(1, 9));

                //---- stopRecordsSizeSpinner ----
                stopRecordsSizeSpinner.setModel(new SpinnerNumberModel(3, 1, 10, 1));
                stopRecordsSizeSpinner.setToolTipText("Used to set Stop Record track size");
                stopRecordsSizeSpinner.setName("stopRecordsSizeSpinner");
                stopRecordsSizeSpinner.addChangeListener(e -> setDirty());
                stopRecordsPanel.add(stopRecordsSizeSpinner, cc.xy(3, 9));
            }
            tabbedPane.addTab("Stop Records", stopRecordsPanel);

            //======== glyphPanel ========
            {
                glyphPanel.setName("glyphPanel");
                glyphPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "default, $lgap, fill:default:grow"));

                //---- showGlyphCheckBox ----
                showGlyphCheckBox.setText("Show Glyphs");
                showGlyphCheckBox.setToolTipText("Shows or hides all glyphs");
                showGlyphCheckBox.setName("showGlyphCheckBox");
                showGlyphCheckBox.addItemListener(e -> showGlyphCheckBoxItemStateChanged());
                glyphPanel.add(showGlyphCheckBox, cc.xy(1, 1));

                //---- glyphLabel ----
                glyphLabel.setName("glyphLabel");
                glyphPanel.add(glyphLabel, cc.xy(3, 1));

                //---- glyphSelectionPanel ----
                glyphSelectionPanel.setEnabled(false);
                glyphSelectionPanel.setToolTipText("Used to add a glyph to a track");
                glyphSelectionPanel.setName("glyphSelectionPanel");
                glyphPanel.add(glyphSelectionPanel, cc.xywh(1, 3, 3, 1));
            }
            tabbedPane.addTab("Glyphs", glyphPanel);

            //======== panelRendering ========
            {
                panelRendering.setName("panelRendering");
                panelRendering.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "6*(default, $lgap), default"));

                //---- renderingDescriptionLabel ----
                renderingDescriptionLabel.setName("renderingDescriptionLabel");
                panelRendering.add(renderingDescriptionLabel, cc.xywh(1, 1, 3, 1));

                //---- separator3 ----
                separator3.setName("separator3");
                panelRendering.add(separator3, cc.xywh(1, 3, 3, 1));

                //---- radioButtonHigh ----
                radioButtonHigh.setText("High");
                radioButtonHigh.setName("radioButtonHigh");
                radioButtonHigh.addActionListener(e -> radioButtonHighActionPerformed());
                radioButtonHigh.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        radioButtonHighMouseClicked(e);
                    }
                });
                panelRendering.add(radioButtonHigh, cc.xy(1, 5));

                //---- highDescriptionLabel ----
                highDescriptionLabel.setName("highDescriptionLabel");
                panelRendering.add(highDescriptionLabel, cc.xy(3, 5));

                //---- separator2 ----
                separator2.setName("separator2");
                panelRendering.add(separator2, cc.xywh(1, 7, 3, 1));

                //---- radioButtonMedium ----
                radioButtonMedium.setText("Medium");
                radioButtonMedium.setName("radioButtonMedium");
                radioButtonMedium.addActionListener(e -> radioButtonMediumActionPerformed());
                panelRendering.add(radioButtonMedium, cc.xy(1, 9));

                //---- mediumDescriptionLabel ----
                mediumDescriptionLabel.setName("mediumDescriptionLabel");
                panelRendering.add(mediumDescriptionLabel, cc.xy(3, 9));

                //---- separator4 ----
                separator4.setName("separator4");
                panelRendering.add(separator4, cc.xywh(1, 11, 3, 1));

                //---- radioButtonLow ----
                radioButtonLow.setText("Low");
                radioButtonLow.setName("radioButtonLow");
                radioButtonLow.addActionListener(e -> radioButtonLowActionPerformed());
                panelRendering.add(radioButtonLow, cc.xy(1, 13));

                //---- lowDescriptionLabel ----
                lowDescriptionLabel.setName("lowDescriptionLabel");
                panelRendering.add(lowDescriptionLabel, cc.xy(3, 13));
            }
            tabbedPane.addTab("Rendering", panelRendering);

            //======== advancedPanel ========
            {
                advancedPanel.setName("advancedPanel");
                advancedPanel.setLayout(new FormLayout(
                        "3*(default, $lcgap), default:grow",
                        "3*(default, $lgap), default"));

                //---- groupByLabel ----
                groupByLabel.setText("Color By:");
                groupByLabel.setToolTipText("Colors tracks by time");
                groupByLabel.setName("groupByLabel");
                advancedPanel.add(groupByLabel, cc.xy(1, 1));

                //---- groupByComboBox ----
                groupByComboBox.setToolTipText("Colors tracks by time");
                groupByComboBox.setName("groupByComboBox");
                groupByComboBox.addItemListener(e -> setDirty());
                advancedPanel.add(groupByComboBox, cc.xywh(3, 1, 3, 1));

                //---- timeThresholdLabel ----
                timeThresholdLabel.setText("Hide after:");
                timeThresholdLabel.setToolTipText("Hides tracks after the selected amount of time");
                timeThresholdLabel.setName("timeThresholdLabel");
                advancedPanel.add(timeThresholdLabel, cc.xy(1, 3));

                //---- timeThresholdSpinner ----
                timeThresholdSpinner.setModel(new SpinnerNumberModel(0L, 0L, null, 1L));
                timeThresholdSpinner.setToolTipText("Hides tracks after the selected amount of time");
                timeThresholdSpinner.setName("timeThresholdSpinner");
                timeThresholdSpinner.addChangeListener(e -> setDirty());
                advancedPanel.add(timeThresholdSpinner, cc.xy(3, 3));

                //---- timeComboBox ----
                timeComboBox.setToolTipText("Hides tracks after the selected amount of time");
                timeComboBox.setName("timeComboBox");
                timeComboBox.addActionListener(e -> setDirty());
                advancedPanel.add(timeComboBox, cc.xy(5, 3));

                //---- fadeOverTmeCheckBox ----
                fadeOverTmeCheckBox.setText("Fade over time");
                fadeOverTmeCheckBox.setToolTipText("Tracks will fade over time if selected");
                fadeOverTmeCheckBox.setName("fadeOverTmeCheckBox");
                fadeOverTmeCheckBox.addItemListener(e -> setDirty());
                advancedPanel.add(fadeOverTmeCheckBox, cc.xywh(1, 5, 7, 1));

                //---- linkShapeOrientationToTrackHeadingCheckBox ----
                linkShapeOrientationToTrackHeadingCheckBox.setText("Set track shape orientation to match track orientation");
                linkShapeOrientationToTrackHeadingCheckBox.setToolTipText("The shape of the track and the glyph will be oriented in the direction of the tracks orientation");
                linkShapeOrientationToTrackHeadingCheckBox.setName("linkShapeOrientationToTrackHeadingCheckBox");
                linkShapeOrientationToTrackHeadingCheckBox.addItemListener(e -> setDirty());
                advancedPanel.add(linkShapeOrientationToTrackHeadingCheckBox, cc.xywh(1, 7, 7, 1));
            }
            tabbedPane.addTab("Advanced", advancedPanel);
        }
        add(tabbedPane, cc.xy(1, 3));

        //---- checkBoxRestoreDefaults ----
        checkBoxRestoreDefaults.setText("Restore Defaults");
        checkBoxRestoreDefaults.setName("checkBoxRestoreDefaults");
        checkBoxRestoreDefaults.addItemListener(e -> checkBoxRestoreDefaultsItemStateChanged());
        add(checkBoxRestoreDefaults, cc.xy(1, 5));

        //---- trackShapeButtonGroup ----
        trackShapeButtonGroup.add(noneToggleButton);
        trackShapeButtonGroup.add(sphereToggleButton);
        trackShapeButtonGroup.add(coneToggleButton);
        trackShapeButtonGroup.add(cylinderToggleButton);
        trackShapeButtonGroup.add(cubeToggleButton);

        //---- lineButtonGroup ----
        lineButtonGroup.add(noStyleToggleButton);
        lineButtonGroup.add(solidToggleButton);
        lineButtonGroup.add(arrowHeadToggleButton);

        //---- stopRecordsShapeButtonGroup ----
        stopRecordsShapeButtonGroup.add(stopRecordsSphereToggleButton);
        stopRecordsShapeButtonGroup.add(stopRecordsConeToggleButton);
        stopRecordsShapeButtonGroup.add(stopRecordsCylinderToggleButton);
        stopRecordsShapeButtonGroup.add(stopRecordsCubeToggleButton);

        //---- buttonGroupTrackRendering ----
        var buttonGroupTrackRendering = new ButtonGroup();
        buttonGroupTrackRendering.add(radioButtonHigh);
        buttonGroupTrackRendering.add(radioButtonMedium);
        buttonGroupTrackRendering.add(radioButtonLow);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JCheckBox showTracksCheckBox;
    private JTabbedPane tabbedPane;
    private JToggleButton noneToggleButton;
    private JToggleButton sphereToggleButton;
    private JToggleButton coneToggleButton;
    private JToggleButton cylinderToggleButton;
    private JToggleButton cubeToggleButton;
    private JLabel colorLabel;
    private ColorChooser colorChooser;
    private JLabel trackOpacityLabel;
    private JSpinner trackOpacitySpinner;
    private JLabel trackSizeLabel;
    private JSpinner trackSizeSpinner;
    private JCheckBox showHeadingCheckBox;
    private JCheckBox snapToSurfaceCheckBox;
    private JLabel lineStyleLabel;
    private JToggleButton noStyleToggleButton;
    private JToggleButton solidToggleButton;
    private JToggleButton arrowHeadToggleButton;
    private JLabel lineColorLabel;
    private ColorChooser lineColorChooser;
    private JLabel lineWidthLabel;
    private JSpinner lineWidthSpinner;
    private JPanel stopRecordsPanel;
    private JCheckBox showStopRecordsCheckBox;
    private JToggleButton stopRecordsSphereToggleButton;
    private JToggleButton stopRecordsConeToggleButton;
    private JToggleButton stopRecordsCylinderToggleButton;
    private JToggleButton stopRecordsCubeToggleButton;
    private JLabel stopRecordsColorLabel;
    private ColorChooser stopRecordsColorChooser;
    private JLabel stopRecordsOpacityLabel;
    private JSpinner stopRecordsOpacitySpinner;
    private JLabel stopRecordsSizeLabel;
    private JSpinner stopRecordsSizeSpinner;
    private JPanel glyphPanel;
    private JCheckBox showGlyphCheckBox;
    private JLabel glyphLabel;
    private GlyphSelectionPanel glyphSelectionPanel;
    private JPanel panelRendering;
    private WrappingLabel renderingDescriptionLabel;
    private JRadioButton radioButtonHigh;
    private WrappingLabel highDescriptionLabel;
    private JRadioButton radioButtonMedium;
    private WrappingLabel mediumDescriptionLabel;
    private JRadioButton radioButtonLow;
    private WrappingLabel lowDescriptionLabel;
    private JLabel groupByLabel;
    private JComboBox<BucketMode> groupByComboBox;
    private JLabel timeThresholdLabel;
    private JSpinner timeThresholdSpinner;
    private JComboBox<DateFormatter.TimePeriod> timeComboBox;
    private JCheckBox fadeOverTmeCheckBox;
    private JCheckBox linkShapeOrientationToTrackHeadingCheckBox;
    private JCheckBox checkBoxRestoreDefaults;
    private ButtonGroup trackShapeButtonGroup;
    private ButtonGroup lineButtonGroup;
    private ButtonGroup stopRecordsShapeButtonGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public static void main(String[] args)
    {
//        SmartTracksPreferencesPanel panel = (SmartTracksPreferencesPanel) GUIUtils.lauchPanel(SmartTracksPreferencesPanel.class, null);
//        panel.setGlobalMode();
    }
}
