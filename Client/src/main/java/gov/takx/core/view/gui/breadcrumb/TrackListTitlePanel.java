package gov.takx.core.view.gui.breadcrumb;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.maps.Marker;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Font;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Adds a title (icon, name) component to an {@link TrackListAndDetailsPanel}.
 *
 * @since 5.4
 */
public class TrackListTitlePanel extends JPanel implements IResourceCleaner
{
    public TrackListTitlePanel()
    {
        initComponents();
        backButton.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "arrow_left.png"));
    }

    /**
     * @param backButtonListener back button listener
     */
    void init(@Nonnull Runnable backButtonListener)
    {
        backButton.addActionListener(e -> trackListAndDetailsPanel.onExternalBackButtonClicked(backButtonListener));
    }

    @Override
    public void releaseResources()
    {
        trackListAndDetailsPanel.releaseResources();
    }

    /**
     * @param marker The marker to display
     * @param sortedTracks The marker's tracks, sorted from newest to oldest
     * @param filteredTrackCount The number of tracks that were filtered
     * @param filtersUpdatedRunnable Callback that responds when the user changes the "hide temp" and "show all" filters
     */
    @InvokeOnEDT
    public void setTrackList(@Nonnull Marker marker, @Nonnull List<ITrack> sortedTracks, int filteredTrackCount,
                             @Nonnull Runnable filtersUpdatedRunnable)
    {
        updateTitle(marker);
        trackListAndDetailsPanel.setTrackList(sortedTracks, filteredTrackCount, filtersUpdatedRunnable);
    }

    /**
     * @param marker The updated marker; the caller should ensure that it's the marker currently being displayed
     */
    public void updateTitle(@Nonnull Marker marker)
    {
        titleLabel.setIcon(marker.getSizedIcon(ImageUtils.DEFAULT_ICON_WIDTH));
        titleLabel.setText(marker.getName());
    }

    /**
     * @return the panel contained by this title panel
     */
    public TrackListAndDetailsPanel getTrackListPanel()
    {
        return trackListAndDetailsPanel;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var titlePanel = new JPanel();
        backButton = new JButton();
        titleLabel = new JLabel();
        trackListAndDetailsPanel = new TrackListAndDetailsPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU2_BORDER);
        setName("trackListPanel");
        setLayout(new FormLayout(
                "default:grow",
                "fill:24dlu, fill:default:grow"));

        //======== titlePanel ========
        {
            titlePanel.setName("titlePanel");
            titlePanel.setLayout(new FormLayout(
                    "24dlu, $lcgap, default:grow",
                    "fill:default:grow"));

            //---- backButton ----
            backButton.setName("backButton");
            titlePanel.add(backButton, cc.xy(1, 1));

            //---- titleLabel ----
            titleLabel.setText("text");
            titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD, titleLabel.getFont().getSize() + 2f));
            titleLabel.setName("titleLabel");
            titlePanel.add(titleLabel, cc.xy(3, 1));
        }
        add(titlePanel, cc.xy(1, 1));

        //---- trackListAndDetailsPanel ----
        trackListAndDetailsPanel.setName("trackListAndDetailsPanel");
        add(trackListAndDetailsPanel, cc.xy(1, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JButton backButton;
    private JLabel titleLabel;
    private TrackListAndDetailsPanel trackListAndDetailsPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
