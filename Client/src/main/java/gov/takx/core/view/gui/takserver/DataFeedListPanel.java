package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.MissionDownloadStatus;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.core.comms.takserver.TakServerConnectionManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.takserver.TakServerConnectionClosed;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.JButtonExt;
import gov.takx.commons.swing.PlaceholderTextField;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.utils.TimeBoundedTask;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;

/**
 * Shows available data feeds <i>(missions)</i> on connected TAK Servers.
 *
 * @since 5.0
 */
@PseudoBean
public class DataFeedListPanel extends ATitledPanel
{
    private static final DataFeedListPanel INSTANCE = new DataFeedListPanel();
    private static final RowSpec ROW_SPEC = RowSpec.decode("fill:pref"); // Fill, but don't grow vertically
    private static final String ENCODED_COLUMN_SPEC = "default:grow"; // Grow horizontally
    private static final long POPULATING_MESSAGE_DURATION_MS = 6000;
    private static final int SORT_STATUS = 0;
    private static final int SORT_TIME = 1;
    private static final int SORT_NAME = 2;
    private static final int SORT_UNREAD = 3;
    private static final Comparator<DataFeedListEntry> CMP_TIME = (e1, e2) -> {
        Instant t1 = e1.getLastMissionUpdate();
        Instant t2 = e2.getLastMissionUpdate();
        if (t1 == null) return 1;
        if (t2 == null) return -1;
        return -t1.compareTo(t2);
    };
    private static final Comparator<DataFeedListEntry> CMP_NAME = (e1, e2) -> {
        Mission m1 = e1.getCurrentMission();
        Mission m2 = e2.getCurrentMission();
        if (m1 == null) return 1;
        if (m2 == null) return -1;
        return String.CASE_INSENSITIVE_ORDER.compare(m1.name(), m2.name());
    };
    private final DefaultComboBoxModel<ITakServerConnection> serverComboModel = new DefaultComboBoxModel<>();
    private final TimeBoundedTask populatingMessageHideTask = new TimeBoundedTask("Feed population popup display",
            POPULATING_MESSAGE_DURATION_MS, POPULATING_MESSAGE_DURATION_MS, this::hidePopulatingMessage);
    private Comparator<DataFeedListEntry> statusComparator; // Dependence on data-sync, initialized later
    private Collection<DataFeedListEntry> lastEntries = Collections.emptyList();

    @Inject
    private TakServerConnectionManager takServerManager;

    private DataFeedListPanel()
    {
        super(ButtonType.DATA_SYNC.getDisplayText());
        initComponents();

        // Filter text field input hint.
        filterTextfield.setPlaceholderText("Search...");

        // Add edge case to allow any content.
        serverComboModel.addElement(null);
        serverComboBox.setModel(serverComboModel);
        serverComboBox.setRenderer(new ServerFilterRenderer());

        // Initially show the "no feeds available" message
        scrollWrapperWrapper.add(noFeedsPanel, BorderLayout.CENTER);
        scrollWrapper.getVerticalScrollBar().setUnitIncrement(20);

        // Setup layered display; everything in the layered panel has to be manually sized..
        populatingMessagePanel.setVisible(false);
        layeredContentPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateLayeredPanelChildrenBounds();
            }
        });
        layeredContentPanel.add(scrollWrapperWrapper, JLayeredPane.DEFAULT_LAYER);
        layeredContentPanel.add(populatingMessagePanel, JLayeredPane.POPUP_LAYER);
        updateLayeredPanelChildrenBounds();
    }

    /**
     * Set up the panel.
     *
     * @since 5.2
     */
    @PostConstruct
    private void postConstruct()
    {
        createFeedButton.setEnabled(!takServerManager.getTakServerConnections(true).isEmpty());
    }

    /**
     * Handles resetting the combo model on project close, or else the list continues to grow
     * @since 5.4
     */
    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        serverComboModel.removeAllElements();
        serverComboModel.addElement(null);
    }

    /**
     * @return Shared instance of the data feed list panel.
     */
    @Nonnull
    public static DataFeedListPanel getInstance()
    {
        return INSTANCE;
    }

    /**
     * @param statusLookup Lookup for the download status of a feed.
     */
    void setStatusLookup(@Nonnull BiFunction<ITakServerConnection, String, MissionDownloadStatus> statusLookup)
    {
        statusComparator = (e1, e2) -> {
            ITakServerConnection c1 = e1.getCurrentConnection();
            ITakServerConnection c2 = e2.getCurrentConnection();
            if (c1 == null) return 1;
            if (c2 == null) return -1;

            Mission m1 = e1.getCurrentMission();
            Mission m2 = e2.getCurrentMission();
            if (m1 == null) return 1;
            if (m2 == null) return -1;

            // Higher state items should appear first, so the comparison result is inverted.
            MissionDownloadStatus s1 = statusLookup.apply(c1, m1.name());
            MissionDownloadStatus s2 = statusLookup.apply(c2, m2.name());
            int cmp = -s1.compareTo(s2);

            // Status shows things that are downloaded first, then when the download states are equal, sorts by time.
            //  - By the time we're sorting feeds the current connection/mission values should be set and non-null
            return cmp == 0 ? CMP_TIME.compare(e1, e2) : cmp;
        };
    }

    /**
     * Refreshes the {@link #scrollWrapperWrapper} and {@link #populatingMessagePanel} to always fit
     * in the {@link #layeredContentPanel} which does not automatically layout children.
     * <p/>
     * This gives us an effective pop-over effect for the {@link #populatingMessagePanel} when
     * a new connection is added, indicating to users a background request is being made.
     * @since 5.2
     */
    @InvokeOnEDT
    private void updateLayeredPanelChildrenBounds()
    {
        Dimension parentDimensions = layeredContentPanel.getSize();

        // The scroll wrapper must fit the entire space.
        scrollWrapperWrapper.setBounds(0, 0, parentDimensions.width, parentDimensions.height);

        // The "fetching..." message should sit near the bottom with padding space.
        int padding = 20;
        int messageHeight = 45;
        int messageY = parentDimensions.height - padding - messageHeight;
        int messageWidth = Math.max(1, parentDimensions.width - padding * 2);
        populatingMessagePanel.setBounds(padding, messageY, messageWidth, messageHeight);

        // Refresh the UI so layout changes are visible.
        layeredContentPanel.revalidate();
        layeredContentPanel.repaint();
    }

    /**
     * Swaps out any component in the main display wrapper for the scroll wrapper holding the feed list entries.
     *
     * @since 5.2
     */
    @InvokeOnEDT
    private void ensureFeedListLayout()
    {
        Component[] components = scrollWrapperWrapper.getComponents();
        if (components.length > 0)
        {
            Component child = components[0];
            if (child == noFeedsPanel)
            {
                scrollWrapperWrapper.removeAll();
                scrollWrapperWrapper.add(scrollWrapper, BorderLayout.CENTER);
                scrollWrapperWrapper.revalidate();
                scrollWrapperWrapper.repaint();
            }
        }
    }

    /**
     * Hide the <i>"Fetching missions..."</i> popup panel.
     * @since 5.2
     */
    private void hidePopulatingMessage()
    {
        populatingMessagePanel.setVisible(false);
    }

    /**
     * Sort feed panels by the current sort preference.
     */
    private void sortFeeds()
    {
        sortFeeds(lastEntries);
    }

    /**
     * Sort feed panels by the current sort preference.
     */
    @InvokeOnEDT
    void sortFeeds(@Nonnull Collection<DataFeedListEntry> feedEntries)
    {
        ensureFeedListLayout();

        // Keep a copy so that we can re-sort later on the last known collection of entries.
        lastEntries = new ArrayList<>(feedEntries);

        // Create a sorted set of the feeds to place them in order in the UI.
        SortedSet<DataFeedListEntry> set = new TreeSet<>((o1, o2) -> switch (sortByComboBox.getSelectedIndex())
        {
            // TODO: Handle SORT_UNREAD
            case SORT_STATUS -> statusComparator.compare(o1, o2);
            case SORT_UNREAD, SORT_TIME -> CMP_TIME.compare(o1, o2);
            case SORT_NAME -> CMP_NAME.compare(o1, o2);
            default -> 0;
        });
        set.addAll(feedEntries);

        // Remove servers that are not the target server.
        // If no target server is specified, then no removals will be made.
        ITakServerConnection targetServer = (ITakServerConnection) serverComboModel.getSelectedItem();
        if (targetServer != null) set.removeIf(e -> !Objects.equals(e.getCurrentConnection(), targetServer));

        // Re-create the feed entry rows.
        FormLayout formLayout = new FormLayout(ENCODED_COLUMN_SPEC); // Start with no rows, rows are added dynamically
        feedsList.removeAll();
        feedsList.setLayout(formLayout);
        CellConstraints constraints = new CellConstraints(1, 1);
        for (DataFeedListEntry entry : set)
        {
            formLayout.appendRow(ROW_SPEC);
            constraints.xy(1, formLayout.getRowCount());
            feedsList.add(entry, constraints);
        }
        feedsList.revalidate();
        feedsList.repaint();
    }

    /**
     * Removes a single feed entry from the feeds list.
     *
     * @param feedEntry Feed to remove.
     */
    @InvokeOnEDT
    void removeListEntry(@Nonnull DataFeedListEntry feedEntry)
    {
        ensureFeedListLayout();

        feedsList.remove(feedEntry);
        feedsList.revalidate();
        feedsList.repaint();
    }

    /**
     * Opens a new dialog that allows for the creation of a new data feed.
     */
    private void createFeed()
    {
        Window window = SwingUtilities.windowForComponent(this);
        DataFeedEditorDialog dialog = DataFeedEditorDialog.forCreation(window);
        dialog.setVisible(true);
    }

    /**
     * Enable the create feed button when any connections are opened.
     *
     * @since 5.2
     */
    @SuppressWarnings("unused")
    private void onConnectionOpened(@Observes TakServerConnectionOpened event)
    {
        // Show "fetching..." message.
        populatingMessagePanel.setVisible(true);
        populatingMessageHideTask.request();
        layeredContentPanel.revalidate();
        layeredContentPanel.repaint();

        // If there's at least one connection we can create feeds.
        createFeedButton.setEnabled(true);

        // Add the server to the server filter combo model.
        serverComboModel.addElement(takServerManager.findConnectionByUuid(event.getConnection().getId()));
    }

    /**
     * Disable the create feed button if all connections are closed.
     *
     * @since 5.2
     */
    @SuppressWarnings("unused")
    private void onConnectionClosed(@Observes TakServerConnectionClosed event)
    {
        if (takServerManager.getTakServerConnections(true).isEmpty()) createFeedButton.setEnabled(false);
        serverComboModel.removeElement(takServerManager.findConnectionByUuid(event.getConnection().getId()));
    }

    /**
     * Filter visibility of feeds based on filter text.
     */
    @InvokeOnEDT
    private void filterFeeds()
    {
        String filterText = Objects.requireNonNullElse(filterTextfield.getText(), "").toLowerCase();
        if (filterText.isBlank())
        {
            for (Component component : feedsList.getComponents())
            {
                component.setVisible(true);
            }
        } else
        {
            for (Component component : feedsList.getComponents())
            {
                if (component instanceof DataFeedListEntry entry)
                {
                    Mission mission = entry.getCurrentMission();
                    String name = mission == null ? "" : mission.name().toLowerCase();
                    component.setVisible(name.contains(filterText));
                } else
                {
                    component.setVisible(true);
                }
            }
        }
    }

    /**
     * A renderer to show which TAK Server we're filtering entries for.
     *
     * @since 5.2
     */
    private static class ServerFilterRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof ITakServerConnection connection)
            {
                label.setText(connection.getDisplayName());
            } else if (value == null)
            {
                // Null is an edge case we use to support showing all feeds at once.
                label.setText("<All servers>");
            }
            return label;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        filterPanel = new JPanel();
        filterTextfield = new PlaceholderTextField();
        createFeedButton = new JButtonExt();
        serverComboBox = new JComboBox<>();
        sortByComboBox = new JComboBox<>();
        layeredContentPanel = new JLayeredPane();
        noFeedsPanel = new JPanel();
        var noFeedsLabel = new JLabel();
        scrollWrapper = new JScrollPane();
        feedsList = new JPanel();
        scrollWrapperWrapper = new JPanel();
        populatingMessagePanel = new JPanel();
        var populatingLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "pref:grow",
                "pref, $lgap, fill:default:grow"));

        //======== filterPanel ========
        {
            filterPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            filterPanel.setName("filterPanel");
            filterPanel.setLayout(new FormLayout(
                    "pref:grow, $lcgap, pref, $lcgap, default, $lcgap, pref",
                    "pref"));

            //---- filterTextfield ----
            filterTextfield.setToolTipText("Filter feeds by name");
            filterTextfield.setName("filterTextfield");
            filterTextfield.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyReleased(KeyEvent e)
                {
                    filterFeeds();
                }
            });
            filterPanel.add(filterTextfield, cc.xy(1, 1));

            //---- createFeedButton ----
            createFeedButton.setText("Create Feed");
            createFeedButton.setDisabledToolTipText("No Active Connections");
            createFeedButton.setToolTipText("Create a new feed");
            createFeedButton.setName("createFeedButton");
            createFeedButton.addActionListener(e -> createFeed());
            filterPanel.add(createFeedButton, cc.xy(3, 1));

            //---- serverComboBox ----
            serverComboBox.setToolTipText("Feed item server filter");
            serverComboBox.setName("serverComboBox");
            serverComboBox.addItemListener(e -> sortFeeds());
            filterPanel.add(serverComboBox, cc.xy(5, 1));

            //---- sortByComboBox ----
            sortByComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                    "Feed Status",
                    "Modified Time",
                    "Name",
                    "Unread items"
            }));
            sortByComboBox.setToolTipText("Feed item sort preference");
            sortByComboBox.setName("sortByComboBox");
            sortByComboBox.addItemListener(e -> sortFeeds());
            filterPanel.add(sortByComboBox, cc.xy(7, 1));
        }
        add(filterPanel, cc.xy(1, 1));

        //======== layeredContentPanel ========
        {
            layeredContentPanel.setName("layeredContentPanel");
        }
        add(layeredContentPanel, cc.xy(1, 3));

        //======== noFeedsPanel ========
        {
            noFeedsPanel.setName("noFeedsPanel");
            noFeedsPanel.setLayout(new FormLayout(
                    "center:default:grow",
                    "default:grow"));

            //---- noFeedsLabel ----
            noFeedsLabel.setText("No feeds available");
            noFeedsLabel.setName("noFeedsLabel");
            noFeedsPanel.add(noFeedsLabel, cc.xy(1, 1));
        }

        //======== scrollWrapper ========
        {
            scrollWrapper.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollWrapper.setName("scrollWrapper");

            //======== feedsList ========
            {
                feedsList.setBorder(new EmptyBorder(5, 5, 5, 5));
                feedsList.setName("feedsList");
                feedsList.setLayout(new FormLayout(
                        "left:default:grow",
                        "default:grow"));
            }
            scrollWrapper.setViewportView(feedsList);
        }

        //======== scrollWrapperWrapper ========
        {
            scrollWrapperWrapper.setName("scrollWrapperWrapper");
            scrollWrapperWrapper.setLayout(new BorderLayout());
        }

        //======== populatingMessagePanel ========
        {
            populatingMessagePanel.setBorder(new LineBorder(UIManager.getColor("Component.borderColor")));
            populatingMessagePanel.setName("populatingMessagePanel");
            populatingMessagePanel.setLayout(new FormLayout(
                    "center:default:grow",
                    "default:grow"));

            //---- populatingLabel ----
            populatingLabel.setText("Fetching feeds...");
            populatingLabel.setName("populatingLabel");
            populatingMessagePanel.add(populatingLabel, cc.xy(1, 1));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel filterPanel;
    private PlaceholderTextField filterTextfield;
    private JButtonExt createFeedButton;
    private JComboBox<ITakServerConnection> serverComboBox;
    private JComboBox<String> sortByComboBox;
    private JLayeredPane layeredContentPanel;
    private JPanel noFeedsPanel;
    private JScrollPane scrollWrapper;
    private JPanel feedsList;
    private JPanel scrollWrapperWrapper;
    private JPanel populatingMessagePanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
