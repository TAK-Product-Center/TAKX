package gov.takx.core.view.gui.breadcrumb;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.breadcrumb.TrackExportService;
import gov.takx.core.persist.breadcrumb.Track;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import java.awt.CardLayout;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a filterable list of {@link gov.takx.api.playback.IMarker} {@link gov.takx.api.breadcrumb.ITrack tracks}
 * and track details in a {@link TrackInfoPanel} when a track's info button is clicked.
 *
 * @since 5.4
 */
@PseudoBean
public class TrackListAndDetailsPanel extends JPanel implements IResourceCleaner
{
    private static final RowSpec ROW_SPEC = RowSpec.decode("fill:pref");
    private static final CellConstraints CONSTRAINTS = new CellConstraints();

    @Inject
    private TrackExportService trackExportService;

    @VisibleForTesting
    final List<TrackListEntryPanel> panels = new ArrayList<>();
    private Runnable filtersUpdatedRunnable;
    private String currentCardName;
    private int filteredTrackCount;

    public TrackListAndDetailsPanel()
    {
        initComponents();

        currentCardName = trackListPanel.getName();
        add(trackInfoPanel, trackInfoPanel.getName()); // add track info card

        filterCountLabel.setVisible(false);
    }

    /**
     * Notifies this panel that an external back button has been clicked. If already at the top of the card stack, the
     * provided {@code backButtonListener} will be called.
     * 
     * @param backButtonListener Called if already at the top of the card stack
     */
    public void onExternalBackButtonClicked(@Nonnull Runnable backButtonListener)
    {
        if (currentCardName.equals(trackInfoPanel.getName()))
        {
            trackInfoPanel.onHide();
            showComponent(trackListPanel);
        } else
        {
            backButtonListener.run();
        }
    }

    /**
     * Hides the filters (Hide Temp, Show All).
     */
    public void hideFilters()
    {
        filtersPanel.setVisible(false);
    }

    /**
     * Sets the internal {@link TrackInfoPanel} back button visible and configures it to return to the track list when
     * clicked. This can be used when the parent container does not provide its own back button.
     */
    public void showInternalBackButton()
    {
        trackInfoPanel.showInternalBackButton(e -> showComponent(trackListPanel));
    }

    /**
     * Clears all tracks from the track list.
     */
    @InvokeOnEDT
    public void clearTrackList()
    {
        removeAllPanels();
    }

    /**
     * @param sortedTracks The marker's tracks, sorted from newest to oldest
     * @param filteredTrackCount The number of tracks that were filtered
     * @param filtersUpdatedRunnable Callback that responds when the user changes the "hide temp" and "show all"
     *                               filters. May be {@code null} if the caller does not support filtering.
     */
    @InvokeOnEDT
    public void setTrackList(@Nonnull List<ITrack> sortedTracks, int filteredTrackCount,
                             @Nullable Runnable filtersUpdatedRunnable)
    {
        clearTrackList();

        sortedTracks.forEach(track -> addPanel(createPanel(track)));
        this.filtersUpdatedRunnable = filtersUpdatedRunnable;
        this.filteredTrackCount = filteredTrackCount;

        // Update the filtered track count label
        if (filteredTrackCount > 0)
        {
            filterCountLabel.setVisible(true);
            filterCountLabel.setText(filteredTrackCount + (filteredTrackCount == 1 ? " track " : " tracks ")
                    + "hidden by filters");
        } else
        {
            filterCountLabel.setVisible(false);
        }

        revalidate();
        repaint();
    }

    /**
     * Refreshes the panel being used to display the provided track.
     *
     * @param track The track that was updated
     */
    public void updateTrack(@Nonnull ITrack track)
    {
        final String uid = track.getUid();
        panels.stream()
                .filter(panel -> panel.getTrackUid().equals(uid))
                .findFirst()
                .ifPresent(TrackListEntryPanel::refreshAll);

        trackInfoPanel.updateTrack(track);
    }

    /**
     * Removes a track from the track list.
     *
     * @param track The track to remove
     */
    public void removeTrack(@Nonnull ITrack track)
    {
        final var uid = track.getUid();
        final var updatedList = panels.stream().map(panel -> (ITrack) panel.track).filter(t -> !t.getUid().equals(uid)).toList();

        if (panels.size() != updatedList.size()) setTrackList(updatedList, filteredTrackCount, () -> {
        });
    }

    @Override
    public void releaseResources()
    {
        trackInfoPanel.releaseResources();
        removeAllPanels();
    }

    /**
     * @return {@code true} if track list should omit tracks with a duration less than 10 minutes and distance less
     * than 100m.
     */
    public boolean isHideTemp()
    {
        return hideTempCheckBox.isVisible() && hideTempCheckBox.isSelected();
    }

    /**
     * @return {@code true} if all track (including temp) should be listed, {@code false} if only the last 15 tracks
     * should be displayed.
     */
    public boolean isShowAll()
    {
        return showAllCheckBox.isSelected();
    }

    /**
     * @param panel The panel to add to the bottom of the {@link #trackEntriesPanel}
     */
    private void addPanel(@Nonnull TrackListEntryPanel panel)
    {
        final FormLayout formLayout = (FormLayout) trackEntriesPanel.getLayout();
        formLayout.appendRow(ROW_SPEC);
        trackEntriesPanel.add(panel, CONSTRAINTS.xy(1, formLayout.getRowCount()));

        panels.add(panel);
    }

    /**
     * Removes all panels from the {@link #trackEntriesPanel} and all rows from its FormLayout
     */
    private void removeAllPanels()
    {
        trackEntriesPanel.removeAll();
        panels.forEach(ATrackPanel::releaseResources);
        panels.clear();

        final FormLayout formLayout = (FormLayout) trackEntriesPanel.getLayout();
        int rowCount;
        while ((rowCount = formLayout.getRowCount()) > 0) formLayout.removeRow(rowCount);
    }

    private void showAllActionPerformed()
    {
        hideTempCheckBox.setVisible(!showAllCheckBox.isSelected());
        if (filtersUpdatedRunnable != null) filtersUpdatedRunnable.run();
    }

    private void hideTempActionPerformed()
    {
        if (filtersUpdatedRunnable != null) filtersUpdatedRunnable.run();
    }

    /**
     * Helper method to show the provided panel.
     *
     * @param panel The panel to show
     */
    @InvokeOnEDT
    private void showComponent(Container panel)
    {
        currentCardName = panel.getName();
        ((CardLayout) getLayout()).show(this, currentCardName);
    }

    /**
     * Creates a new {@link TrackListEntryPanel} for the provided {@link ITrack}.
     *
     * @param track The track
     * @return A new panel that will display the track's basic info
     */
    private TrackListEntryPanel createPanel(@Nonnull ITrack track)
    {
        return new TrackListEntryPanel((Track) track, e -> {
            trackInfoPanel.setTrack(track);
            showComponent(trackInfoPanel);
        }, fileType -> trackExportService.export(track, fileType));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        trackListPanel = new JPanel();
        filtersPanel = new JPanel();
        hideTempCheckBox = new JCheckBox();
        showAllCheckBox = new JCheckBox();
        var trackScrollPane = new JScrollPane();
        trackEntriesPanel = new JPanel();
        filterCountLabel = new JLabel();
        trackInfoPanel = new TrackInfoPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU2_BORDER);
        setName("trackListPanel");
        setLayout(new CardLayout());

        //======== trackListPanel ========
        {
            trackListPanel.setName("trackListPanel");
            trackListPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, fill:default:grow, $lgap, default"));

            //======== filtersPanel ========
            {
                filtersPanel.setName("filtersPanel");
                filtersPanel.setLayout(new FormLayout(
                        "default, $lcgap, default",
                        "default"));

                //---- hideTempCheckBox ----
                hideTempCheckBox.setText("Hide Temp");
                hideTempCheckBox.setToolTipText("A \"temp\" track has a duration shorter than 10 min and a total distance less than 100m");
                hideTempCheckBox.setSelected(true);
                hideTempCheckBox.setName("hideTempCheckBox");
                hideTempCheckBox.addActionListener(e -> hideTempActionPerformed());
                filtersPanel.add(hideTempCheckBox, cc.xy(1, 1));

                //---- showAllCheckBox ----
                showAllCheckBox.setText("Show All");
                showAllCheckBox.setToolTipText("When not selected, only the last 15 tracks are displayed");
                showAllCheckBox.setName("showAllCheckBox");
                showAllCheckBox.addActionListener(e -> showAllActionPerformed());
                filtersPanel.add(showAllCheckBox, cc.xy(3, 1));
            }
            trackListPanel.add(filtersPanel, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

            //======== trackScrollPane ========
            {
                trackScrollPane.setName("trackScrollPane");

                //======== trackEntriesPanel ========
                {
                    trackEntriesPanel.setName("trackEntriesPanel");
                    trackEntriesPanel.setLayout(new FormLayout(
                            "default:grow",
                            ""));
                }
                trackScrollPane.setViewportView(trackEntriesPanel);
            }
            trackListPanel.add(trackScrollPane, cc.xy(1, 3));

            //---- filterCountLabel ----
            filterCountLabel.setText("text");
            filterCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
            filterCountLabel.setToolTipText("Use the \"Hide Temp\" and \"Show All\" options to modify filters");
            filterCountLabel.setName("filterCountLabel");
            trackListPanel.add(filterCountLabel, cc.xy(1, 5));
        }
        add(trackListPanel, "trackListPanel");

        //---- trackInfoPanel ----
        trackInfoPanel.setName("trackInfoPanel");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel trackListPanel;
    private JPanel filtersPanel;
    private JCheckBox hideTempCheckBox;
    private JCheckBox showAllCheckBox;
    private JPanel trackEntriesPanel;
    private JLabel filterCountLabel;
    private TrackInfoPanel trackInfoPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
