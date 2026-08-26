package gov.takx.core.view.gui.breadcrumb;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.maps.Marker;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.components.RangedSlider;
import gov.takx.platform.utils.Interval;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import java.awt.Component;
import java.awt.Font;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Displays search configuration and results.
 *
 * @since 5.4
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TrackSearchPanel extends ATitledPanel
{
    private static final int RANGE_MAX = 30; // day 30 of 30, where day 30 is today
    private static final int DEFAULT_MIN = RANGE_MAX - 7; // one week before today

    @Inject
    private StandardTimeFormatting standardTimeFormatting;

    @VisibleForTesting
    final Set<Marker> markers = new TreeSet<>(Comparator.comparing(Marker::getName));
    private volatile boolean markersChanged;
    private DefaultComboBoxModel<Marker> markerModel = new DefaultComboBoxModel<>();
    @VisibleForTesting
    final DefaultComboBoxModel<ITakServerConnection> serverModel = new DefaultComboBoxModel<>();
    private Instant start;
    private Instant end;
    private boolean searching;

    /**
     * @param searchRunnable called when a search is initiated
     * @param cancelRunnable called when a search is cancelled
     */
    public TrackSearchPanel(@Nonnull Runnable searchRunnable, @Nonnull Runnable cancelRunnable)
    {
        super("Track Search");

        initComponents();

        searchInfoPanel.setVisible(false);
        progressBar.setVisible(false);

        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(false);
        slider.setPaintLabels(false);
        slider.setPaintTrack(true);
        slider.setMinimum(0);
        slider.setMaximum(RANGE_MAX);
        slider.setLowerValue(DEFAULT_MIN);
        slider.setUpperValue(RANGE_MAX);
        slider.addChangeListener(changeEvent -> updateDates());

        serverComboBox.setModel(serverModel);
        serverComboBox.setRenderer(new TakServerRenderer());
        serverComboBox.setEnabled(false);
        serverCheckbox.addActionListener(e -> serverComboBox.setEnabled(serverCheckbox.isSelected()));

        markerComboBox.setModel(markerModel);
        markerComboBox.setRenderer(new MarkerEntryRenderer());
        markerComboBox.addPopupMenuListener(new MarkerMenuHandler());

        trackListAndDetailsPanel.hideFilters();
        trackListAndDetailsPanel.showInternalBackButton();

        searchButton.addActionListener(e -> {
            // Search started: update the UI and notify the callback
            if (!searching)
            {
                searchRequested();
                searching = true;
                searchRunnable.run();
            }
            // Search cancelled: notify the callback
            else
            {
                searchButton.setEnabled(false);
                cancelRunnable.run();
            }
        });

        updateDates();
    }

    @Override
    public void releaseResources()
    {
        markerModel.removeAllElements();
        serverModel.removeAllElements();
        clearSearchResults();
    }

    /**
     * @param marker The marker to add to the combo box
     */
    public void addMarker(@Nonnull Marker marker)
    {
        synchronized (markers)
        {
            markers.add(marker);
            markersChanged = true;

            if (markers.size() == 1) installMinimalMarkerModel(marker);
        }
    }

    /**
     * In order to get the marker combobox properly sized, we have to install a model.  We do this once,
     * and after that we recreate the model as needed whenever the combobox menu is opened.
     *
     * @see MarkerMenuHandler
     * @since 5.5
     */
    @InvokeOnEDT
    private void installMinimalMarkerModel(Marker marker)
    {
        markerModel = new DefaultComboBoxModel<>(new Marker[]{marker});
        markerComboBox.setModel(markerModel);
        searchButton.setEnabled(true);
        repaint();
    }

    /**
     * Forces the marker combo box UI to refresh.
     */
    @InvokeOnEDT
    public void refreshMarkers()
    {
        markerComboBox.repaint();
    }

    /**
     * Called when a search is complete.
     *
     * @param trackList The search results
     */
    @InvokeOnEDT
    public void onSearchComplete(@Nonnull List<ITrack> trackList)
    {
        trackListAndDetailsPanel.setTrackList(trackList, 0, null);

        resetForNewSearch();
    }

    /**
     * Called when a search is cancelled.
     */
    public void onSearchCancelled()
    {
        searchInfoPanel.setVisible(false);
        trackListAndDetailsPanel.clearTrackList();

        resetForNewSearch();
    }

    /**
     * Removes a marker from the combo box, clearing the search results if needed.
     *
     * @param marker The marker that was deleted
     */
    @InvokeOnEDT
    public void removeMarker(@Nonnull Marker marker)
    {
        final var selectedMarker = (Marker) markerComboBox.getSelectedItem();

        synchronized (markers)
        {
            markers.remove(marker);
            markersChanged = true;

            if (markers.isEmpty()) searchButton.setEnabled(false);
        }

        if (markerComboBox.isPopupVisible()) markerModel.removeElement(marker);

        // If the marker was selected, clear the search results
        if (selectedMarker != null && selectedMarker.getUid().equals(marker.getUid())) clearSearchResults();
    }

    /**
     * @return The selected marker
     */
    @Nullable
    public Marker getSelectedMarker()
    {
        return (Marker) markerModel.getSelectedItem();
    }

    /**
     * @return {@code true} if the selected TAK server should be searched
     */
    public boolean isSearchServer()
    {
        return serverCheckbox.isSelected();
    }

    /**
     * @return The selected TAK server connection
     */
    @Nullable
    public ITakServerConnection getSelectedServer()
    {
        return (ITakServerConnection) serverModel.getSelectedItem();
    }

    /**
     * @return The search time interval
     */
    @Nonnull
    public Interval getInterval()
    {
        return new Interval(start, end);
    }

    /**
     * @return The panel that displays query results
     */
    @Nonnull
    public TrackListAndDetailsPanel getTrackListPanel()
    {
        return trackListAndDetailsPanel;
    }

    /**
     * @param serverConnection The server to add
     */
    public void addTakServer(@Nonnull ITakServerConnection serverConnection)
    {
        final var serverName = serverConnection.getDisplayName();

        int index = 0;
        while (index < serverModel.getSize() && serverModel.getElementAt(index).getDisplayName().compareTo(serverName) <= 0) index++;

        serverModel.insertElementAt(serverConnection, index);
        serverCheckbox.setEnabled(true);
        serverComboBox.setEnabled(true);

        if (serverModel.getSize() == 1) serverComboBox.setSelectedIndex(0);
    }

    /**
     * @param serverConnection The server to remove
     */
    public void removeTakServer(@Nonnull ITakServerConnection serverConnection)
    {
        serverModel.removeElement(serverConnection);

        if (serverModel.getSize() == 0)
        {
            serverCheckbox.setSelected(false);
            serverCheckbox.setEnabled(false);
            serverComboBox.setEnabled(false);
        }
    }

    /**
     * Updates the dates displays.
     */
    private void updateDates()
    {
        final var now = Instant.now();
        start = now.minus(RANGE_MAX - slider.getLowerValue(), ChronoUnit.DAYS);
        end = now.minus(RANGE_MAX - slider.getUpperValue(), ChronoUnit.DAYS);
        startDatePanel.setDate(start);
        endDatePanel.setDate(end);
    }

    /**
     * Updates the UI when the user requests a search.
     */
    private void searchRequested()
    {
        clearSearchResults();

        final var marker = getSelectedMarker();

        if (marker == null) return;

        selectedMarkerLabel.setIcon(marker.getSizedIcon(ImageUtils.DEFAULT_ICON_WIDTH));
        selectedMarkerLabel.setText(marker.getName());
        fromDateLabel.setText(standardTimeFormatting.formatMonth(start) + " " + standardTimeFormatting.formatDay(start));
        toLabel.setVisible(true);
        toDateLabel.setText(standardTimeFormatting.formatMonth(end) + " " + standardTimeFormatting.formatDay(end));
        searchInfoPanel.setVisible(true);

        progressBar.setVisible(true);
        markerComboBox.setEnabled(false);
        slider.setEnabled(false);

        searchButton.setText("Cancel");
        repaint();
    }

    /**
     * Resets the UI to prepare for a new search. Search results will not be affected.
     */
    @InvokeOnEDT
    private void resetForNewSearch()
    {
        progressBar.setVisible(false);
        markerComboBox.setEnabled(true);
        slider.setEnabled(true);
        searchButton.setEnabled(true);
        searchButton.setText("Search");
        searching = false;
    }

    /**
     * Clears panels that display search results and track details.
     */
    @InvokeOnEDT
    private void clearSearchResults()
    {
        // Close the details panel if it's open
        trackListAndDetailsPanel.onExternalBackButtonClicked(() -> {
        });
        trackListAndDetailsPanel.clearTrackList();
        searchInfoPanel.setVisible(false);
    }

    /**
     * Renders {@link ITakServerConnection TAK server connections} in a list.
     */
    private static class TakServerRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value != null) setText(((ITakServerConnection) value).getDisplayName());

            return this;
        }
    }

    /**
     * This class handles the recreation of the model for the marker combobox when the list of markers has changed.
     * This is much more efficient than trying to keep the model up to date as new markers come in.
     *
     * @since 5.5
     */
    private class MarkerMenuHandler implements PopupMenuListener
    {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e)
        {
            synchronized (markers)
            {
                if (markersChanged)
                {
                    markerModel = new DefaultComboBoxModel<>(markers.toArray(new Marker[0]));
                    markerComboBox.setModel(markerModel);
                    markersChanged = false;
                }
            }
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
        {
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e)
        {
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var titleLabel = new JLabel();
        var markerSelectionPanel = new JPanel();
        var markerLabel = new JLabel();
        markerComboBox = new JComboBox<>();
        var querySliderPanel = new JPanel();
        var startDateLabel = new JLabel();
        var endDateLabel = new JLabel();
        startDatePanel = new TrackDatePanel();
        slider = new RangedSlider();
        endDatePanel = new TrackDatePanel();
        var searchServerPanel = new JPanel();
        serverCheckbox = new JCheckBox();
        serverComboBox = new JComboBox<>();
        var searchAndProgressPanel = new JPanel();
        progressBar = new JProgressBar();
        searchButton = new JButton();
        var resultsLabel = new JLabel();
        searchInfoPanel = new JPanel();
        selectedMarkerLabel = new JLabel();
        fromDateLabel = new JLabel();
        toLabel = new JLabel();
        toDateLabel = new JLabel();
        trackListAndDetailsPanel = new TrackListAndDetailsPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU4_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "4*(default, $lgap), default, $ugap, 3*(default, $lgap), fill:default:grow"));

        //---- titleLabel ----
        titleLabel.setText("Track Search");
        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD, titleLabel.getFont().getSize() + 4f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setName("titleLabel");
        add(titleLabel, cc.xy(1, 1));

        //======== markerSelectionPanel ========
        {
            markerSelectionPanel.setName("markerSelectionPanel");
            markerSelectionPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default"));

            //---- markerLabel ----
            markerLabel.setText("Marker");
            markerLabel.setFont(markerLabel.getFont().deriveFont(markerLabel.getFont().getStyle() | Font.BOLD));
            markerLabel.setName("markerLabel");
            markerSelectionPanel.add(markerLabel, cc.xy(1, 1));

            //---- markerComboBox ----
            markerComboBox.setName("markerComboBox");
            markerSelectionPanel.add(markerComboBox, cc.xy(3, 1));
        }
        add(markerSelectionPanel, cc.xy(1, 3));

        //======== querySliderPanel ========
        {
            querySliderPanel.setName("querySliderPanel");
            querySliderPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, $lcgap, default",
                    "2*(default)"));

            //---- startDateLabel ----
            startDateLabel.setText("Start");
            startDateLabel.setHorizontalAlignment(SwingConstants.CENTER);
            startDateLabel.setName("startDateLabel");
            querySliderPanel.add(startDateLabel, cc.xy(1, 1));

            //---- endDateLabel ----
            endDateLabel.setText("End");
            endDateLabel.setHorizontalAlignment(SwingConstants.CENTER);
            endDateLabel.setName("endDateLabel");
            querySliderPanel.add(endDateLabel, cc.xy(5, 1));

            //---- startDatePanel ----
            startDatePanel.setName("startDatePanel");
            querySliderPanel.add(startDatePanel, cc.xy(1, 2));

            //---- slider ----
            slider.setName("slider");
            querySliderPanel.add(slider, cc.xy(3, 2));

            //---- endDatePanel ----
            endDatePanel.setName("endDatePanel");
            querySliderPanel.add(endDatePanel, cc.xy(5, 2));
        }
        add(querySliderPanel, cc.xy(1, 5));

        //======== searchServerPanel ========
        {
            searchServerPanel.setName("searchServerPanel");
            searchServerPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default"));

            //---- serverCheckbox ----
            serverCheckbox.setText("Search server");
            serverCheckbox.setEnabled(false);
            serverCheckbox.setToolTipText("Enable server search (if a server is available)");
            serverCheckbox.setName("serverCheckbox");
            searchServerPanel.add(serverCheckbox, cc.xy(1, 1));

            //---- serverComboBox ----
            serverComboBox.setEnabled(false);
            serverComboBox.setToolTipText("Select a server to search");
            serverComboBox.setName("serverComboBox");
            searchServerPanel.add(serverComboBox, cc.xy(3, 1));
        }
        add(searchServerPanel, cc.xy(1, 7));

        //======== searchAndProgressPanel ========
        {
            searchAndProgressPanel.setName("searchAndProgressPanel");
            searchAndProgressPanel.setLayout(new FormLayout(
                    "default:grow, $lcgap, default",
                    "default"));

            //---- progressBar ----
            progressBar.setStringPainted(true);
            progressBar.setString("Searching...");
            progressBar.setIndeterminate(true);
            progressBar.setName("progressBar");
            searchAndProgressPanel.add(progressBar, cc.xy(1, 1));

            //---- searchButton ----
            searchButton.setText("Search");
            searchButton.setEnabled(false);
            searchButton.setName("searchButton");
            searchAndProgressPanel.add(searchButton, cc.xy(3, 1));
        }
        add(searchAndProgressPanel, cc.xy(1, 9));

        //---- resultsLabel ----
        resultsLabel.setText("Search Results");
        resultsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultsLabel.setFont(resultsLabel.getFont().deriveFont(resultsLabel.getFont().getStyle() | Font.BOLD));
        resultsLabel.setName("resultsLabel");
        add(resultsLabel, cc.xy(1, 11));

        //======== searchInfoPanel ========
        {
            searchInfoPanel.setName("searchInfoPanel");
            searchInfoPanel.setLayout(new FormLayout(
                    "3*(default, $lcgap), default",
                    "default"));

            //---- selectedMarkerLabel ----
            selectedMarkerLabel.setText("marker");
            selectedMarkerLabel.setName("selectedMarkerLabel");
            searchInfoPanel.add(selectedMarkerLabel, cc.xy(1, 1));

            //---- fromDateLabel ----
            fromDateLabel.setText("start");
            fromDateLabel.setName("fromDateLabel");
            searchInfoPanel.add(fromDateLabel, cc.xy(3, 1));

            //---- toLabel ----
            toLabel.setText("to");
            toLabel.setName("toLabel");
            searchInfoPanel.add(toLabel, cc.xy(5, 1));

            //---- toDateLabel ----
            toDateLabel.setText("end");
            toDateLabel.setName("toDateLabel");
            searchInfoPanel.add(toDateLabel, cc.xy(7, 1));
        }
        add(searchInfoPanel, cc.xy(1, 13, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- trackListAndDetailsPanel ----
        trackListAndDetailsPanel.setName("trackListAndDetailsPanel");
        add(trackListAndDetailsPanel, cc.xy(1, 17));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JComboBox<Marker> markerComboBox;
    private TrackDatePanel startDatePanel;
    private RangedSlider slider;
    private TrackDatePanel endDatePanel;
    private JCheckBox serverCheckbox;
    private JComboBox<ITakServerConnection> serverComboBox;
    private JProgressBar progressBar;
    private JButton searchButton;
    private JPanel searchInfoPanel;
    private JLabel selectedMarkerLabel;
    private JLabel fromDateLabel;
    private JLabel toLabel;
    private JLabel toDateLabel;
    private TrackListAndDetailsPanel trackListAndDetailsPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
