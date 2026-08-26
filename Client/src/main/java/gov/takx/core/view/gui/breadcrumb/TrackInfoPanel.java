package gov.takx.core.view.gui.breadcrumb;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.icons.TakColor;
import gov.tak.api.maps.Marker;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.core.breadcrumb.BreadcrumbManager;
import gov.takx.core.globe.ElevationProfileService;
import gov.takx.core.persist.breadcrumb.Track;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.core.view.gui.pointdropper.ColorSelector;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Displays {@link ITrack} info and allows the user to modify its mutable properties.
 *
 * @since 5.3
 */
@PseudoBean
public class TrackInfoPanel extends ATrackPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private StandardTimeFormatting standardTimeFormatting;
    @Inject
    private IMapObjectFinder mapObjectFinder;
    @Inject
    private ElevationProfileService elevationProfileService;
    @Inject
    private BreadcrumbManager breadcrumbManager;

    public TrackInfoPanel()
    {
        initComponents();

        backButton.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "arrow_left.png"));

        styleComboBox.setModel(new DefaultComboBoxModel<>(ITrack.Style.values()));
        startButton.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "track_start.png"));
        endButton.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "track_end.png"));
        elevationLabel.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "elevation.png"));
        speedLabel.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "speed.png"));
        elevationDeltaLabel.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "moving_up.png"));

        nameApplyButton.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "check_circle_outline.png"));
        nameCancelButton.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "cancel.png"));
        elevationChartButton.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "line_chart.png"));
        elevationChartButton.addActionListener(e -> showElevationProfiler());
        exportButton.setIcon(UIResources.EXPORT_ICON);
        exportButton.addActionListener(e -> showExportPopup(exportButton, track, fileType -> trackExportService.export(track, fileType)));

        nameButtonsPanel.setVisible(false);

        nameTextField.getDocument().addDocumentListener(new GUIUtils.SingleActionDocumentListener(e -> {
            nameApplyButton.setEnabled(e.getDocument().getLength() > 0);
            nameCancelButton.setEnabled(true);
        }));
    }

    /**
     * Display the current {@link ITrack track's} elevation profile.
     *
     * @since 5.6
     */
    private void showElevationProfiler()
    {
        final List<IGeoPoint> geoPoints =
                breadcrumbManager.getTrackBreadcrumbs(track).stream()
                        .map(breadcrumb -> (IGeoPoint) breadcrumb.getGeoPointMetaData().get())
                        .toList();

        elevationProfileService.showProfile(geoPoints, track.getStartTime(), track.getEndTime());
    }

    /**
     * Updates all {@link ITrack} values in this panel if the provided track is currently being displayed.
     *
     * @param track The track that was updated
     */
    public void updateTrack(@Nonnull ITrack track)
    {
        if (this.track != null && this.track.getUid().equals(track.getUid())) refreshAll();
    }

    /**
     * Displays a back button that can be used when the container does not provide one.
     *
     * @param actionListener The listener to call when the back button is clicked
     * @since 5.4
     */
    @InvokeOnEDT
    public void showInternalBackButton(@Nonnull ActionListener actionListener)
    {
        backButton.addActionListener(actionListener);
        backButton.setVisible(true);
        repaint();
    }

    /**
     * Reset the state when hiding the menu.
     */
    public void onHide()
    {
        Arrays.stream(visibilityButton.getActionListeners()).forEach(listener -> visibilityButton.removeActionListener(listener));
        endNameEditing();
    }

    /**
     * @param track The track to display
     */
    public void setTrack(@Nonnull ITrack track)
    {
        this.track = (Track) track;

        final Marker marker = (Marker) mapObjectFinder.findMapObjectByUid(track.getMarkerUid());

        if (marker == null)
        {
            logger.error("Could not find a marker for UID {}", track.getMarkerUid());
            return;
        }

        nameTextField.setText(track.getName());
        styleComboBox.getModel().setSelectedItem(track.getStyle());

        prepareGotoButton(track::getStartPoint, startButton);
        prepareGotoButton(track::getEndPoint, endButton);
        prepareGotoButton(track::getMinElevationPoint, minElevationButton);
        prepareGotoButton(track::getMaxElevationPoint, maxElevationButton);
        prepareGotoButton(track::getMaxSpeedPoint, maxSpeedButton);

        prepareVisibilityButton(this.track, visibilityButton);

        refreshAll();
    }

    @Override
    @InvokeOnEDT
    public void refreshAll()
    {
        if (track == null) return;

        final long startTimeMillis = track.getStartTime().toEpochMilli();
        startDateValueLabel.setText(standardTimeFormatting.formatDateOnlyWithCurrent(startTimeMillis));
        startTimeValueLabel.setText(standardTimeFormatting.formatTimeOnlyWithCurrent(startTimeMillis));
        distanceValueLabel.setText(getLengthString(track.getTotalDistance(), 2));
        elapsedTimeValueLabel.setText(standardTimeFormatting.formatApproximateDuration(track.getStartTime(), track.getEndTime()));

        elevationMinValueLabel.setText(getElevationString(track.getMinElevationPoint()));
        elevationMaxValueLabel.setText(getElevationString(track.getMaxElevationPoint()));
        maxSpeedValueLabel.setText(getSpeedString(track.getMaxSpeed()));
        averageSpeedValueLabel.setText(getSpeedString(track.getAverageSpeed()));
        elevationGainValueLabel.setText(getElevationString(track.getElevationGain()));
        elevationLossValueLabel.setText(getElevationString(track.getElevationLoss()));

        checkGotoButton(startButton, track.getStartPoint());
        checkGotoButton(endButton, track.getEndPoint());
        checkGotoButton(minElevationButton, track.getMinElevationPoint());
        checkGotoButton(maxElevationButton, track.getMaxElevationPoint());
        checkGotoButton(maxSpeedButton, track.getMaxSpeedPoint());

        setVisibilityButtonIcon(track.isVisible());
        colorLabel.setBackground(new Color(track.getColor()));
    }

    @InvokeOnEDT
    @Override
    protected void setVisibilityButtonIcon(boolean visible)
    {
        visibilityButton.setIcon(visible ? VISIBLITY_ON_ICON : VISIBLITY_OFF_ICON);
    }

    private void endNameEditing()
    {
        requestFocus(); // removed the cursor
        nameButtonsPanel.setVisible(false);
    }

    private void colorLabelMouseClicked()
    {
        final var menu = new JPopupMenu();
        final List<TakColor> colors = TakColor.getTrackPalette();
        final var selector = new ColorSelector(colors.size(), 1, colors,
                color -> {
                    track.setColor(color.getColor().getRGB());
                    breadcrumbManager.save(track);
                    menu.setVisible(false);
                });

        menu.add(selector);
        menu.show(colorLabel, 0, 0);
    }

    private void styleComboBoxItemStateChanged()
    {
        track.setStyle((ITrack.Style) styleComboBox.getModel().getSelectedItem());
        breadcrumbManager.save(track);
    }

    private void nameApplyActionPerformed()
    {
        track.setName(nameTextField.getText().trim());
        breadcrumbManager.save(track);
        endNameEditing();
    }

    private void nameCancelActionPerformed()
    {
        nameTextField.setText(track.getName()); // restore the name
        endNameEditing();
    }

    private void nameTextFieldMouseClicked()
    {
        nameButtonsPanel.setVisible(true);
    }

    /**
     * @param geoPoint The location data
     * @return A string representation of the elevation using the current elevation units preference, or "unset" if the
     * {@link GeoPoint} is {@code null}
     */
    @Nonnull
    private String getElevationString(@Nullable GeoPoint geoPoint)
    {
        return geoPoint != null && geoPoint.hasElevation()
                ? getElevationString(geoPoint.getElevation())
                : "unset";
    }

    /**
     * @param elevation The elevation in meters
     * @return A string representation of the elevation using the current elevation units preference
     * @since 5.5
     */
    @Nonnull
    private String getElevationString(double elevation)
    {
        return altitudeUnitsMonitor.getCurrentValue().format(elevation, 3);
    }

    /**
     * @param speedMps The speed in m/s
     * @return A string representation of the speed using the current speed units preference
     */
    @Nonnull
    private String getSpeedString(double speedMps)
    {
        return speedUnitsMonitor.getCurrentValue().format(speedMps, 2);
    }

    /**
     * Prepares a button to respond to user clicks by going to the specified location, or disables the button if there's no
     * point to navigate to.
     *
     * @param geoPointSupplier The point supplier; may return {@code null} if the no point exists
     * @param button The button to update
     */
    private void prepareGotoButton(@Nonnull Supplier<GeoPoint> geoPointSupplier, @Nonnull JButton button)
    {
        // Remove listeners added for previous track
        for (ActionListener actionListener : button.getActionListeners()) button.removeActionListener(actionListener);

        button.setEnabled(false);

        //noinspection DataFlowIssue
        button.addActionListener(e -> globeManager.getLastFocusedGlobe().goTo(geoPointSupplier.get()));

        checkGotoButton(button, geoPointSupplier.get());
    }

    /**
     * Enables a goto button if its associated {@link GeoPoint} is now valid.
     *
     * @param button The button to check
     * @param geoPoint The point associated with the button
     * @since 5.5
     */
    private void checkGotoButton(@Nonnull JButton button, @Nullable GeoPoint geoPoint)
    {
        if (!button.isEnabled() && geoPoint != null && geoPoint.isValid()) button.setEnabled(true);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var headerPanel = new JPanel();
        backButton = new JButton();
        visibilityButton = new JButton();
        elevationChartButton = new JButton();
        exportButton = new JButton();
        var contentsPanel = new JPanel();
        var trackNameLabel = new JLabel();
        var namePanel = new JPanel();
        nameTextField = new JTextField();
        nameButtonsPanel = new JPanel();
        nameApplyButton = new JButton();
        nameCancelButton = new JButton();
        var styleLabel = new JLabel();
        var stylePanel = new JPanel();
        styleComboBox = new JComboBox<>();
        colorLabel = new JLabel();
        startButton = new JButton();
        var startDateLabel = new JLabel();
        startDateValueLabel = new JLabel();
        var startTimeLabel = new JLabel();
        startTimeValueLabel = new JLabel();
        endButton = new JButton();
        var distanceLabel = new JLabel();
        distanceValueLabel = new JLabel();
        var elapsedTimeLabel = new JLabel();
        elapsedTimeValueLabel = new JLabel();
        elevationLabel = new JLabel();
        minElevationButton = new JButton();
        elevationMinValueLabel = new JLabel();
        maxElevationButton = new JButton();
        elevationMaxValueLabel = new JLabel();
        speedLabel = new JLabel();
        maxSpeedButton = new JButton();
        maxSpeedValueLabel = new JLabel();
        var averageSpeedLabel = new JLabel();
        averageSpeedValueLabel = new JLabel();
        elevationDeltaLabel = new JLabel();
        var elevationGainLabel = new JLabel();
        elevationGainValueLabel = new JLabel();
        var elevationLossLabel = new JLabel();
        elevationLossValueLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU2_BORDER);
        setName("trackInfoPanel");
        setLayout(new FormLayout(
                "default:grow",
                "default, $ugap, fill:default:grow"));

        //======== headerPanel ========
        {
            headerPanel.setName("headerPanel");
            headerPanel.setLayout(new FormLayout(
                    "24dlu, $lcgap, default:grow, 3*($lcgap, 24dlu)",
                    "fill:24dlu"));

            //---- backButton ----
            backButton.setVisible(false);
            backButton.setName("backButton");
            headerPanel.add(backButton, cc.xy(1, 1));

            //---- visibilityButton ----
            visibilityButton.setName("visibilityButton");
            headerPanel.add(visibilityButton, cc.xy(5, 1));

            //---- elevationChartButton ----
            elevationChartButton.setName("elevationChartButton");
            headerPanel.add(elevationChartButton, cc.xy(7, 1));

            //---- exportButton ----
            exportButton.setName("exportButton");
            headerPanel.add(exportButton, cc.xy(9, 1));
        }
        add(headerPanel, cc.xy(1, 1));

        //======== contentsPanel ========
        {
            contentsPanel.setName("contentsPanel");
            contentsPanel.setLayout(new FormLayout(
                    "24dlu, $ugap, 36dlu, $lcgap, default:grow, $lcgap, 36dlu, $lcgap, default:grow",
                    "fill:24dlu, $lgap, fill:24dlu, $ugap, 4*(fill:24dlu, $lgap), fill:24dlu"));
            ((FormLayout) contentsPanel.getLayout()).setRowGroups(new int[][]{{3, 5, 7, 9, 11, 13}});

            //---- trackNameLabel ----
            trackNameLabel.setText("Track:");
            trackNameLabel.setName("trackNameLabel");
            contentsPanel.add(trackNameLabel, cc.xy(1, 1));

            //======== namePanel ========
            {
                namePanel.setName("namePanel");
                namePanel.setLayout(new FormLayout(
                        "default:grow, default",
                        "fill:default:grow"));

                //---- nameTextField ----
                nameTextField.setName("nameTextField");
                nameTextField.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        nameTextFieldMouseClicked();
                    }
                });
                namePanel.add(nameTextField, cc.xy(1, 1));

                //======== nameButtonsPanel ========
                {
                    nameButtonsPanel.setName("nameButtonsPanel");
                    nameButtonsPanel.setLayout(new FormLayout(
                            "24dlu, $lcgap, 24dlu",
                            "fill:default:grow"));

                    //---- nameApplyButton ----
                    nameApplyButton.setName("nameApplyButton");
                    nameApplyButton.addActionListener(e -> nameApplyActionPerformed());
                    nameButtonsPanel.add(nameApplyButton, cc.xy(1, 1));

                    //---- nameCancelButton ----
                    nameCancelButton.setName("nameCancelButton");
                    nameCancelButton.addActionListener(e -> nameCancelActionPerformed());
                    nameButtonsPanel.add(nameCancelButton, cc.xy(3, 1));
                }
                namePanel.add(nameButtonsPanel, cc.xy(2, 1));
            }
            contentsPanel.add(namePanel, cc.xywh(3, 1, 7, 1));

            //---- styleLabel ----
            styleLabel.setText("Style:");
            styleLabel.setName("styleLabel");
            contentsPanel.add(styleLabel, cc.xy(1, 3));

            //======== stylePanel ========
            {
                stylePanel.setName("stylePanel");
                stylePanel.setLayout(new FormLayout(
                        "default:grow, $lcgap, 24dlu",
                        "fill:default:grow"));

                //---- styleComboBox ----
                styleComboBox.setName("styleComboBox");
                styleComboBox.addItemListener(e -> styleComboBoxItemStateChanged());
                stylePanel.add(styleComboBox, cc.xy(1, 1));

                //---- colorLabel ----
                colorLabel.setBackground(Color.yellow);
                colorLabel.setOpaque(true);
                colorLabel.setName("colorLabel");
                colorLabel.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        colorLabelMouseClicked();
                    }
                });
                stylePanel.add(colorLabel, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));
            }
            contentsPanel.add(stylePanel, cc.xywh(3, 3, 7, 1));

            //---- startButton ----
            startButton.setEnabled(false);
            startButton.setName("startButton");
            contentsPanel.add(startButton, cc.xy(1, 5));

            //---- startDateLabel ----
            startDateLabel.setText("Date:");
            startDateLabel.setName("startDateLabel");
            contentsPanel.add(startDateLabel, cc.xy(3, 5));

            //---- startDateValueLabel ----
            startDateValueLabel.setText("start date");
            startDateValueLabel.setName("startDateValueLabel");
            contentsPanel.add(startDateValueLabel, cc.xy(5, 5));

            //---- startTimeLabel ----
            startTimeLabel.setText("Time:");
            startTimeLabel.setName("startTimeLabel");
            contentsPanel.add(startTimeLabel, cc.xy(7, 5));

            //---- startTimeValueLabel ----
            startTimeValueLabel.setText("start time");
            startTimeValueLabel.setHorizontalAlignment(SwingConstants.TRAILING);
            startTimeValueLabel.setName("startTimeValueLabel");
            contentsPanel.add(startTimeValueLabel, cc.xy(9, 5));

            //---- endButton ----
            endButton.setEnabled(false);
            endButton.setName("endButton");
            contentsPanel.add(endButton, cc.xy(1, 7));

            //---- distanceLabel ----
            distanceLabel.setText("Dist:");
            distanceLabel.setName("distanceLabel");
            contentsPanel.add(distanceLabel, cc.xy(3, 7));

            //---- distanceValueLabel ----
            distanceValueLabel.setText("distance");
            distanceValueLabel.setName("distanceValueLabel");
            contentsPanel.add(distanceValueLabel, cc.xy(5, 7));

            //---- elapsedTimeLabel ----
            elapsedTimeLabel.setText("Elapsed:");
            elapsedTimeLabel.setName("elapsedTimeLabel");
            contentsPanel.add(elapsedTimeLabel, cc.xy(7, 7));

            //---- elapsedTimeValueLabel ----
            elapsedTimeValueLabel.setText("elapsed time (end)");
            elapsedTimeValueLabel.setHorizontalAlignment(SwingConstants.TRAILING);
            elapsedTimeValueLabel.setName("elapsedTimeValueLabel");
            contentsPanel.add(elapsedTimeValueLabel, cc.xy(9, 7));

            //---- elevationLabel ----
            elevationLabel.setName("elevationLabel");
            contentsPanel.add(elevationLabel, cc.xy(1, 9));

            //---- minElevationButton ----
            minElevationButton.setText("Min:");
            minElevationButton.setEnabled(false);
            minElevationButton.setName("minElevationButton");
            contentsPanel.add(minElevationButton, cc.xy(3, 9));

            //---- elevationMinValueLabel ----
            elevationMinValueLabel.setText("unset");
            elevationMinValueLabel.setBorder(null);
            elevationMinValueLabel.setName("elevationMinValueLabel");
            contentsPanel.add(elevationMinValueLabel, cc.xy(5, 9, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- maxElevationButton ----
            maxElevationButton.setText("Max:");
            maxElevationButton.setEnabled(false);
            maxElevationButton.setName("maxElevationButton");
            contentsPanel.add(maxElevationButton, cc.xy(7, 9));

            //---- elevationMaxValueLabel ----
            elevationMaxValueLabel.setText("unset");
            elevationMaxValueLabel.setBorder(null);
            elevationMaxValueLabel.setName("elevationMaxValueLabel");
            contentsPanel.add(elevationMaxValueLabel, cc.xy(9, 9, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- speedLabel ----
            speedLabel.setName("speedLabel");
            contentsPanel.add(speedLabel, cc.xy(1, 11));

            //---- maxSpeedButton ----
            maxSpeedButton.setText("Max:");
            maxSpeedButton.setEnabled(false);
            maxSpeedButton.setName("maxSpeedButton");
            contentsPanel.add(maxSpeedButton, cc.xy(3, 11));

            //---- maxSpeedValueLabel ----
            maxSpeedValueLabel.setText("unset");
            maxSpeedValueLabel.setBorder(null);
            maxSpeedValueLabel.setName("maxSpeedValueLabel");
            contentsPanel.add(maxSpeedValueLabel, cc.xy(5, 11, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- averageSpeedLabel ----
            averageSpeedLabel.setText("Avg:");
            averageSpeedLabel.setName("averageSpeedLabel");
            contentsPanel.add(averageSpeedLabel, cc.xy(7, 11));

            //---- averageSpeedValueLabel ----
            averageSpeedValueLabel.setText("unset");
            averageSpeedValueLabel.setName("averageSpeedValueLabel");
            contentsPanel.add(averageSpeedValueLabel, cc.xy(9, 11));

            //---- elevationDeltaLabel ----
            elevationDeltaLabel.setName("elevationDeltaLabel");
            contentsPanel.add(elevationDeltaLabel, cc.xy(1, 13));

            //---- elevationGainLabel ----
            elevationGainLabel.setText("Gain:");
            elevationGainLabel.setName("elevationGainLabel");
            contentsPanel.add(elevationGainLabel, cc.xy(3, 13));

            //---- elevationGainValueLabel ----
            elevationGainValueLabel.setText("unset");
            elevationGainValueLabel.setName("elevationGainValueLabel");
            contentsPanel.add(elevationGainValueLabel, cc.xy(5, 13));

            //---- elevationLossLabel ----
            elevationLossLabel.setText("Loss:");
            elevationLossLabel.setName("elevationLossLabel");
            contentsPanel.add(elevationLossLabel, cc.xy(7, 13));

            //---- elevationLossValueLabel ----
            elevationLossValueLabel.setText("unset");
            elevationLossValueLabel.setName("elevationLossValueLabel");
            contentsPanel.add(elevationLossValueLabel, cc.xy(9, 13));
        }
        add(contentsPanel, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JButton backButton;
    private JButton visibilityButton;
    private JButton elevationChartButton;
    private JButton exportButton;
    private JTextField nameTextField;
    private JPanel nameButtonsPanel;
    private JButton nameApplyButton;
    private JButton nameCancelButton;
    private JComboBox<Track.Style> styleComboBox;
    private JLabel colorLabel;
    private JButton startButton;
    private JLabel startDateValueLabel;
    private JLabel startTimeValueLabel;
    private JButton endButton;
    private JLabel distanceValueLabel;
    private JLabel elapsedTimeValueLabel;
    private JLabel elevationLabel;
    private JButton minElevationButton;
    private JLabel elevationMinValueLabel;
    private JButton maxElevationButton;
    private JLabel elevationMaxValueLabel;
    private JLabel speedLabel;
    private JButton maxSpeedButton;
    private JLabel maxSpeedValueLabel;
    private JLabel averageSpeedValueLabel;
    private JLabel elevationDeltaLabel;
    private JLabel elevationGainValueLabel;
    private JLabel elevationLossValueLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
