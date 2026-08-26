package gov.takx.core.view.gui.takserver.timeline;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.takx.api.annotation.InvokeOnEDT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Displays data feed timelines.
 *
 * @since 5.0
 */
public class DataFeedTimelinePanel extends JPanel
{
    private final DefaultComboBoxModel<MissionConnectionPair> feedModel = new DefaultComboBoxModel<>();
    private final BiFunction<ITakServerConnection, String, List<TimelineEntry>> feedToTimelineFunction;

    /**
     * @param feedToTimelineFunction Given a connection and mission name, supplies the current list of {@link TimelineEntry}s to
     * display in the timeline
     */
    public DataFeedTimelinePanel(@Nonnull BiFunction<ITakServerConnection, String, List<TimelineEntry>> feedToTimelineFunction)
    {
        this.feedToTimelineFunction = feedToTimelineFunction;
        initComponents();

        feedComboBox.setModel(feedModel);
        feedComboBox.addActionListener(e -> onSelectionChanged());

        clear();
    }

    /**
     * Clears the timeline list model.
     */
    public void clear()
    {
        timelineEntriesPanel.removeAll();
        timelineEntriesPanel.revalidate();
        timelineEntriesPanel.repaint();
    }

    /**
     * @param connection Connection to server defining the feed.
     * @param feedName The feed name to add to the list of available feeds
     */
    public void addFeed(@Nonnull ITakServerConnection connection, @Nonnull String feedName)
    {
        int index = 0;
        while (index < feedModel.getSize() && feedModel.getElementAt(index).compareToIgnoreCase(feedName) < 0) index++;

        feedModel.insertElementAt(new MissionConnectionPair(connection, feedName), index);
    }

    /**
     * @param connection Connection to server defining the feed.
     * @param feedName The feed name to remove from the list of available feeds
     */
    public void removeFeed(@Nonnull ITakServerConnection connection, @Nonnull String feedName)
    {
        if (isFeedSelected(connection, feedName)) feedComboBox.setSelectedIndex(-1);
        feedModel.removeElement(new MissionConnectionPair(connection, feedName));
    }

    /**
     * @param connection Connection to server defining the feed.
     * @param feedName The feed to select in the combo box, which will in turn trigger {@link #onSelectionChanged()}
     */
    public void setSelectedFeed(@Nonnull ITakServerConnection connection, @Nonnull String feedName)
    {
        feedComboBox.setSelectedItem(new MissionConnectionPair(connection, feedName));
    }

    /**
     * @param connection Connection to server defining the feed.
     * @param feedName The feed name to check
     *
     * @return {@code true} if the feed is currently selected, {@code false} otherwise
     */
    public boolean isFeedSelected(@Nonnull ITakServerConnection connection, @Nonnull String feedName)
    {
        MissionConnectionPair pair = feedComboBox.getItemAt(feedComboBox.getSelectedIndex());
        return new MissionConnectionPair(connection, feedName).equals(pair);
    }

    /**
     * Displays the timeline for the selected feed. If no feed is selected, the timeline will be cleared.
     */
    private void onSelectionChanged()
    {
        MissionConnectionPair pair = feedComboBox.getItemAt(feedComboBox.getSelectedIndex());
        if (pair != null)
        {
            showTimeline(pair.connection(), pair.mission());
        } else
        {
            showTimeline(null, null);
        }
    }

    /**
     * @param connection Connection to server defining the feed. If {@code null}, the timeline will be cleared.
     * @param feedName The name of the feed to display. If {@code null}, the timeline will be cleared.
     *
     * @throws IllegalStateException thrown if there is no timeline associated with the feed name
     */
    @InvokeOnEDT
    public void showTimeline(@Nullable ITakServerConnection connection, @Nullable String feedName) throws IllegalStateException
    {
        clear();

        if (connection != null && feedName != null)
        {
            final List<TimelineEntry> timelineEntries = feedToTimelineFunction.apply(connection, feedName);

            if (timelineEntries == null) throw new IllegalStateException("Feed " + feedName + " has no timeline");
            CellConstraints cc = new CellConstraints();
            FormLayout layout = (FormLayout) timelineEntriesPanel.getLayout();

            for (TimelineEntry timelineEntry : timelineEntries)
            {
                TimelineEntryPanel panel = new TimelineEntryPanel();
                panel.setValues(timelineEntry);
                layout.appendRow(layout.getRowSpec(1));
                timelineEntriesPanel.add(panel, cc.xy(1, timelineEntriesPanel.getComponentCount() + 1));
            }

            timelineEntriesPanel.revalidate();
            timelineEntriesPanel.repaint();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var feedSelectionPanel = new JPanel();
        var feedLabel = new JLabel();
        feedComboBox = new JComboBox<>();
        timelineEntryListScrollPane = new JScrollPane();
        timelineEntriesPanel = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "[200dlu,default]:grow",
                "default, $rgap, fill:[200dlu,default]:grow"));

        //======== feedSelectionPanel ========
        {
            feedSelectionPanel.setBorder(Borders.DLU2_BORDER);
            feedSelectionPanel.setName("feedSelectionPanel");
            feedSelectionPanel.setLayout(new FormLayout(
                    "default, $ugap, default:grow",
                    "default"));

            //---- feedLabel ----
            feedLabel.setText("Feed");
            feedLabel.setFont(feedLabel.getFont().deriveFont(feedLabel.getFont().getStyle() | Font.BOLD));
            feedLabel.setName("feedLabel");
            feedSelectionPanel.add(feedLabel, cc.xy(1, 1));

            //---- feedComboBox ----
            feedComboBox.setName("feedComboBox");
            feedSelectionPanel.add(feedComboBox, cc.xy(3, 1));
        }
        add(feedSelectionPanel, cc.xy(1, 1));

        //======== timelineEntryListScrollPane ========
        {
            timelineEntryListScrollPane.setPreferredSize(new Dimension(44, 164));
            timelineEntryListScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            timelineEntryListScrollPane.setName("timelineEntryListScrollPane");

            //======== timelineEntriesPanel ========
            {
                timelineEntriesPanel.setName("timelineEntriesPanel");
                timelineEntriesPanel.setLayout(new FormLayout(
                        "pref:grow",
                        "fill:pref:grow"));
            }
            timelineEntryListScrollPane.setViewportView(timelineEntriesPanel);
        }
        add(timelineEntryListScrollPane, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<MissionConnectionPair> feedComboBox;
    private JScrollPane timelineEntryListScrollPane;
    private JPanel timelineEntriesPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
