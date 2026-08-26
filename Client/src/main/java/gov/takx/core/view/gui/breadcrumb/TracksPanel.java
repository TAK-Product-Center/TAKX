package gov.takx.core.view.gui.breadcrumb;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.maps.Marker;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.contacts.lists.ImprovedSelectionJList;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.CardLayout;
import java.awt.Container;
import java.time.Duration;
import java.util.Comparator;
import java.util.function.Predicate;

/**
 * Card layout that transitions between 3 cards: a list of {@link gov.tak.api.maps.Marker markers} and their 
 * {@link ITrack} counts, a marker's tracks, and individual track details.
 *
 * @since 5.3
 */
@PseudoBean
public class TracksPanel extends ATitledPanel implements IResourceCleaner
{
    // Only include tracks if they have a duration of greater than 10 minutes or a total distance more than 100m
    private static final Predicate<ITrack> TEMP_TRACK_PREDICATE =
            track -> Duration.between(track.getStartTime(), track.getEndTime()).toMinutes() >= 10
                    || track.getTotalDistance() >= 100;
    // Sorts tracks by start time in descending order
    private static final Comparator<ITrack> NEWEST_FIRST_COMPARATOR = (t1, t2) -> t2.getStartTime().compareTo(t1.getStartTime());

    @Inject
    private IMapObjectFinder mapObjectFinder;
    @Inject
    private IBreadcrumbManager breadcrumbManager;

    private final TrackListTitlePanel trackListTitlePanel;
    private String displayedMarkerUid;

    /**
     * @param model The {@link gov.tak.api.maps.Marker} list model
     */
    public TracksPanel(@Nonnull DefaultListModel<Marker> model)
    {
        super(ButtonType.TRACKS.getDisplayText());

        initComponents();

        markerList.setModel(model);
        markerList.setCellRenderer(new MarkerEntryRenderer());
        markerList.addListSelectionListener(new MarkerListSelectionListener());

        trackListTitlePanel = new TrackListTitlePanel();
        trackListTitlePanel.init(() -> {
            displayedMarkerUid = null;
            showComponent(markerScrollPane);
        });

        cardLayoutPanel.add(trackListTitlePanel, trackListTitlePanel.getName());
    }

    @Override
    public void releaseResources()
    {
        trackListTitlePanel.releaseResources();
    }

    /**
     * Clears the UI if the {@link Marker} being displayed is deleted.
     * 
     * @param markerUid The deleted marker's UID
     */
    void onMarkerDeleted(@Nonnull String markerUid)
    {
        if (markerUid.equals(displayedMarkerUid))
        {
            showComponent(markerScrollPane);
            displayedMarkerUid = null;
        }
    }

    /**
     * Forces a repaint of the {@link gov.tak.api.maps.Marker} list.
     */
    @InvokeOnEDT
    void refreshMarkerList()
    {
        markerList.repaint();
    }

    /**
     * Refreshes the {@link ITrack} list if the specified {@link gov.tak.api.maps.Marker marker's} tracks are being 
     * displayed. This should be called when tracks are added or removed and the marker's track list needs to be rebuilt.
     * 
     * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
     */
    void refreshTrackList(@Nonnull String markerUid)
    {
        // The MO will always be a marker, but the instanceof handles the null check and cast
        if (markerUid.equals(displayedMarkerUid) && mapObjectFinder.findMapObjectByUid(markerUid) instanceof Marker marker)
        {
            showMarker(marker);
        }
    }

    /**
     * Updates an individual {@link ITrack track's} info. This should be called when a track's details change.
     * 
     * @param track The {@link ITrack} that changed
     */
    void updateTrack(@Nonnull ITrack track)
    {
        trackListTitlePanel.getTrackListPanel().updateTrack(track);
    }

    /**
     * @param marker The {@link gov.tak.api.maps.Marker} whose {@link ITrack tracks} should be displayed
     */
    @InvokeOnEDT
    void showMarker(@Nonnull Marker marker)
    {
        final String markerUid = marker.getUid();
        final var tracklist = breadcrumbManager.getTracks(markerUid);
        var stream = tracklist.stream().sorted(NEWEST_FIRST_COMPARATOR);
        final var trackListPanel = trackListTitlePanel.getTrackListPanel();

        if (!trackListPanel.isShowAll())
        {
            if (trackListPanel.isHideTemp())
            {
                // Always include the current track
                final ITrack currentTrack = breadcrumbManager.getCurrentTrack(markerUid);
                final String currentTrackUid = currentTrack != null ? currentTrack.getUid() : null;
                final var tempTrackPredicate = currentTrackUid != null
                        ? TEMP_TRACK_PREDICATE.or(track -> track.getUid().equals(currentTrackUid))
                        : TEMP_TRACK_PREDICATE;

                stream = stream.filter(tempTrackPredicate);
            }

            // Limit to 15 results if "Show All" is not selected
            stream = stream.limit(15);
        }

        final var filteredTrackList = stream.toList();

        trackListTitlePanel.setTrackList(marker, filteredTrackList,
                tracklist.size() - filteredTrackList.size(), () -> showMarker(marker));
        showComponent(trackListTitlePanel);
        displayedMarkerUid = markerUid;
    }

    /**
     * Repaints this panel and updates the sub-panel titles if the updated marker is currently being displayed.
     *
     * @param marker The marker that was updated
     */
    @InvokeOnEDT
    void markerInfoUpdated(@Nonnull Marker marker)
    {
        if (marker.getUid().equals(displayedMarkerUid)) trackListTitlePanel.updateTitle(marker);
        repaint();
    }

    /**
     * Helper method to show the provided panel.
     *
     * @param panel The panel to show
     */
    @InvokeOnEDT
    private void showComponent(Container panel)
    {
        ((CardLayout) cardLayoutPanel.getLayout()).show(cardLayoutPanel, panel.getName());
    }

    /**
     * Shows a selected {@link Marker marker's} track list
     */
    private class MarkerListSelectionListener implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (e.getValueIsAdjusting() || markerList.getSelectedIndex() < 0) return;

            showMarker(markerList.getSelectedValue());

            markerList.clearSelection();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        cardLayoutPanel = new JPanel();
        markerScrollPane = new JScrollPane();
        markerList = new ImprovedSelectionJList<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setBorder(Borders.DLU4_BORDER);
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== cardLayoutPanel ========
        {
            cardLayoutPanel.setName("cardLayoutPanel");
            cardLayoutPanel.setLayout(new CardLayout());

            //======== markerScrollPane ========
            {
                markerScrollPane.setName("markerScrollPane");

                //---- markerList ----
                markerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                markerList.setName("markerList");
                markerScrollPane.setViewportView(markerList);
            }
            cardLayoutPanel.add(markerScrollPane, "markerScrollPane");
        }
        add(cardLayoutPanel, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel cardLayoutPanel;
    private JScrollPane markerScrollPane;
    private ImprovedSelectionJList<Marker> markerList;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
