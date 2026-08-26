package gov.takx.core.view.gui.breadcrumb;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.core.breadcrumb.TrackExportService;
import gov.takx.core.persist.breadcrumb.Track;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Displays basic info about a single {@link ITrack}, to be displayed in a {@link gov.tak.api.maps.Marker marker's}
 * list of tracks.
 *
 * @since 5.3
 */
@PseudoBean
public class TrackListEntryPanel extends ATrackPanel
{
    private static final int REFRESH_RATE_SEC = 1;
    private static final Icon INFO_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "info_outline.png");
    private static final Icon DELETE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "delete.png");
    private static final DecimalFormat DECIMAL_FORMAT_SHORTEST = new DecimalFormat("0.0");

    @Inject
    private ICoreExecutorService executorService;
    @Inject
    private StandardTimeFormatting standardTimeFormatting;

    private ScheduledFuture<?> refreshFuture;

    /**
     * @param track The track to display in this panel
     * @param detailsButtonListener Listener that will respond when the user clicks the back button
     */
    public TrackListEntryPanel(@Nonnull Track track, @Nonnull ActionListener detailsButtonListener,
                               @Nonnull Consumer<TrackExportService.FileType> exportRequestConsumer)
    {
        this.track = track;
        initComponents();

        detailsButton.setIcon(INFO_ICON);
        exportButton.setIcon(UIResources.EXPORT_ICON);
        deleteButton.setIcon(DELETE_ICON);

        prepareVisibilityButton(track, visibleButton);

        detailsButton.addActionListener(detailsButtonListener);

        exportButton.addActionListener(e -> showExportPopup(this, track, exportRequestConsumer));

        deleteButton.addActionListener(e -> ActionPopupFactory.yesNo(
                "Are you sure you want to delete " + track.getName() + "?", deleteButton,
                () -> breadcrumbManager.deleteTrack(track)).show());

        // starts/stops the refresh action when panel is shown/hidden
        addHierarchyListener(e -> updateAutoRefresh());
    }

    /**
     * @return The track's UID
     */
    public String getTrackUid()
    {
        return track.getUid();
    }

    /**
     * Refreshes the info displayed in this panel with the current {@link ITrack} data.
     */
    @InvokeOnEDT
    @Override
    public void refreshAll()
    {
        trackNameLabel.setText(track.getName());
        colorLabel.setBackground(new Color(track.getColor()));

        final ITrack currentTrack = breadcrumbManager.getCurrentTrack(track.getMarkerUid());
        currentTrackLabel.setVisible(currentTrack != null && track.getUid().equals(currentTrack.getUid()));

        final var startInstant = track.getStartTime();
        final var endInstant = track.getEndTime();

        refreshPeriodic();

        distanceLabel.setText(getLengthString(track.getTotalDistance(), 1));
        durationLabel.setText(standardTimeFormatting.formatApproximateDuration(startInstant, endInstant));
        setVisibilityButtonIcon(track.isVisible());

        repaint();
    }

    /**
     * Refreshes any values that should be refreshed at regular intervals.
     */
    @InvokeOnEDT
    void refreshPeriodic()
    {
        elapsedLabel.setText(standardTimeFormatting.formatApproximateDurationSince(track.getStartTime(), "ago"));
    }

    @InvokeOnEDT
    @Override
    protected void setVisibilityButtonIcon(boolean visible)
    {
        visibleButton.setIcon(visible ? VISIBLITY_ON_ICON : VISIBLITY_OFF_ICON);
    }

    /**
     * Starts the refresh action when this panel is displayed and stops it when it's hidden.
     */
    private void updateAutoRefresh()
    {
        final boolean showing = isShowing();

        if (showing && refreshFuture == null)
        {
            refreshFuture = executorService.scheduleAtFixedRate(new RefreshRunnable(), 0, REFRESH_RATE_SEC, TimeUnit.SECONDS);
        } else if (!showing && refreshFuture != null)
        {
            refreshFuture.cancel(true);
            refreshFuture = null;
        }
    }

    /**
     * Refreshes values that should be updated periodically.
     */
    private class RefreshRunnable implements Runnable
    {
        @Override
        public void run()
        {
            Thread.currentThread().setName("Track list entry refresher");
            refreshPeriodic();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        colorLabel = new JLabel();
        trackNameLabel = new JLabel();
        visibleButton = new JButton();
        currentTrackLabel = new JLabel();
        var detailsAndButtonsPanel = new JPanel();
        var detailsPanel = new JPanel();
        elapsedLabel = new JLabel();
        distanceLabel = new JLabel();
        durationLabel = new JLabel();
        detailsButton = new JButton();
        exportButton = new JButton();
        deleteButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.createEmptyBorder("4dlu, 2dlu, 4dlu, 2dlu"));
        setName("this");
        setLayout(new FormLayout(
                "8dlu, $lcgap, default:grow, $lcgap, 24dlu",
                "fill:24dlu, 2*(default, $lgap), default"));

        //---- colorLabel ----
        colorLabel.setBackground(Color.red);
        colorLabel.setOpaque(true);
        colorLabel.setName("colorLabel");
        add(colorLabel, cc.xywh(1, 1, 1, 4));

        //---- trackNameLabel ----
        trackNameLabel.setText("text");
        trackNameLabel.setFont(trackNameLabel.getFont().deriveFont(trackNameLabel.getFont().getStyle() | Font.BOLD, trackNameLabel.getFont().getSize() + 2f));
        trackNameLabel.setName("trackNameLabel");
        add(trackNameLabel, cc.xy(3, 1));

        //---- visibleButton ----
        visibleButton.setName("visibleButton");
        add(visibleButton, cc.xy(5, 1));

        //---- currentTrackLabel ----
        currentTrackLabel.setText("This is your current active track");
        currentTrackLabel.setName("currentTrackLabel");
        add(currentTrackLabel, cc.xy(3, 2));

        //======== detailsAndButtonsPanel ========
        {
            detailsAndButtonsPanel.setName("detailsAndButtonsPanel");
            detailsAndButtonsPanel.setLayout(new FormLayout(
                    "default:grow, 3*($lcgap, 24dlu)",
                    "fill:24dlu"));

            //======== detailsPanel ========
            {
                detailsPanel.setName("detailsPanel");
                detailsPanel.setLayout(new FormLayout(
                        "default:grow, $lcgap, default:grow",
                        "default, $nlgap, default"));
                ((FormLayout) detailsPanel.getLayout()).setColumnGroups(new int[][]{{1, 3}});

                //---- elapsedLabel ----
                elapsedLabel.setText("elapsed");
                elapsedLabel.setName("elapsedLabel");
                detailsPanel.add(elapsedLabel, cc.xy(1, 1));

                //---- distanceLabel ----
                distanceLabel.setText("distance");
                distanceLabel.setName("distanceLabel");
                detailsPanel.add(distanceLabel, cc.xy(3, 1));

                //---- durationLabel ----
                durationLabel.setText("duration");
                durationLabel.setName("durationLabel");
                detailsPanel.add(durationLabel, cc.xy(3, 3));
            }
            detailsAndButtonsPanel.add(detailsPanel, cc.xy(1, 1));

            //---- detailsButton ----
            detailsButton.setName("detailsButton");
            detailsAndButtonsPanel.add(detailsButton, cc.xy(3, 1));

            //---- exportButton ----
            exportButton.setName("exportButton");
            detailsAndButtonsPanel.add(exportButton, cc.xy(5, 1));

            //---- deleteButton ----
            deleteButton.setName("deleteButton");
            detailsAndButtonsPanel.add(deleteButton, cc.xy(7, 1));
        }
        add(detailsAndButtonsPanel, cc.xywh(3, 4, 3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel colorLabel;
    private JLabel trackNameLabel;
    private JButton visibleButton;
    private JLabel currentTrackLabel;
    private JLabel elapsedLabel;
    private JLabel distanceLabel;
    private JLabel durationLabel;
    private JButton detailsButton;
    private JButton exportButton;
    private JButton deleteButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
