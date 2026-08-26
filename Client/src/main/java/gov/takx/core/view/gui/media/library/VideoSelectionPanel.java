package gov.takx.core.view.gui.media.library;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.mission.data.Feed;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.PlaceholderTextField;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.core.view.gui.media.library.VideoSelectionTableModel.Column;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.FEATURE_IMAGE_SIZE;

/**
 * Panel to allow the user to select video feeds.
 *
 * @since 5.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class VideoSelectionPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int ROW_PADDING = 5;

    // Size of displayed thumbnails
    private static final Dimension THUMBNAIL_SIZE = new Dimension(96, 48);

    // Keep this one square or it looks wrong
    private static final ImageIcon DEFAULT_VIDEO_ICON = UIResources.getSizedIcon(PLATFORM_ICONS, "video.png", FEATURE_IMAGE_SIZE, true);

    // Feed name bold, URL below and smaller
    private static final String NAME_AND_URL_FMT = "<html><b>%s</b><br><font size='-1'>%s</font></html>";

    @Inject
    private ICoreExecutorService executorService;

    private final VideoSelectionTableModel model = new VideoSelectionTableModel();

    private final Runnable cancelAction; // Invoked when cancel button is clicked
    private final Consumer<List<Feed>> okAction; // Invoked when Ok button is clicked

    /**
     * @param okAction The consumer to invoke with the list of selected video feeds when the Ok button is clicked
     * @param cancelAction The action to invoke when the cancel button is clicked
     */
    public VideoSelectionPanel(@Nonnull Consumer<List<Feed>> okAction, @Nonnull Runnable cancelAction)
    {
        initComponents();

        btnCancel.setIcon(new ImageIcon(ImageUtils.applyColorMask((BufferedImage) UIResources.CANCEL_ICON.getImage(), Color.red)));

        this.okAction = okAction;
        this.cancelAction = cancelAction;

        GUIUtils.addSingleActionDocumentListener(txtSearch, this::applyFilter);

        tblFeeds.setModel(model);
        tblFeeds.getTableHeader().setVisible(false);

        final TableColumnModel cm = tblFeeds.getColumnModel();
        final TableColumn selectedColumn = cm.getColumn(Column.SELECTED.ordinal());
        selectedColumn.setResizable(false);
        selectedColumn.setMaxWidth(30); // keep checkbox column narrow

        final TableColumn thumbnailColumn = cm.getColumn(Column.THUMBNAIL.ordinal());
        thumbnailColumn.setCellRenderer(new ThumbnailRenderer());
        thumbnailColumn.setResizable(false);
        thumbnailColumn.setMaxWidth(THUMBNAIL_SIZE.width + 4); // Icon size plus a little padding

        final TableColumn nameColumn = cm.getColumn(Column.NAME_AND_URL.ordinal());
        nameColumn.setCellRenderer(new NameAndUrlRenderer());
    }

    /**
     * Request that the focus be given to the search text field.
     */
    public void takeFocus()
    {
        txtSearch.requestFocusInWindow();
    }

    /**
     * Add the given list of video feeds to the model.
     */
    @InvokeOnEDT
    public void addFeeds(@Nonnull List<Feed> feeds)
    {
        model.addFeeds(feeds);
        adjustHeights();
    }

    /**
     * @return {@code true} when the selection panel has at least one vide feed.
     */
    public boolean hasFeeds()
    {
        return model.getRowCount() > 0;
    }

    private void okButtonActionPerformed()
    {
        okAction.accept(model.getSelectedFeeds());
    }

    private void btnCancelActionPerformed()
    {
        cancelAction.run();
    }

    /**
     * Add a RowFilter using the current search text to the table.  The existing filter is removed if the search text
     * is empty.
     */
    private void applyFilter(DocumentEvent ignored)
    {
        final String searchText = txtSearch.getText();

        // There's no way to update a filter, so just create a new one if needed
        tblFeeds.setRowFilter(searchText.isEmpty() ? null : new TextFilter(searchText));
    }

    @InvokeOnEDT
    private void adjustHeights()
    {
        GUIUtils.updateRowHeights(tblFeeds, ROW_PADDING);
        repaint();
    }

    /**
     * Render the name and URL.
     */
    private class NameAndUrlRenderer extends AHighlightingRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            final Feed feed = (Feed) value;
            value = String.format(NAME_AND_URL_FMT, highlight(feed.alias()), feed.url());

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Renders the feed thumbnail.  Thumbnails are fetched in the background.
     */
    private class ThumbnailRenderer extends DefaultTableCellRenderer
    {
        private final Set<String> queuedUrls = ConcurrentHashMap.newKeySet();
        private final Map<String, Icon> iconCache = new ConcurrentHashMap<>();

        private ThumbnailRenderer()
        {
            setIconTextGap(0);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            final Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            final String url = (String) value;

            if (url != null)
            {
                // Queue fetch of the thumbnail if we don't have it yet
                if (!iconCache.containsKey(url) && !queuedUrls.contains(url))
                {
                    queuedUrls.add(url);

                    executorService.submit(() -> {
                        Thread.currentThread().setName("retrieve thumbnail: " + url);
                        try
                        {
                            final ImageIcon imageIcon = UIResources.getSizedIcon(new URI(url), THUMBNAIL_SIZE);
                            iconCache.put(url, imageIcon);
                            queuedUrls.remove(url);
                            adjustHeights();
                        } catch (URISyntaxException e)
                        {
                            logger.warn("Failed to retrieve thumbnail from {}: {}", url, e.getMessage());
                        }
                    });
                }
            }

            final Icon icon = url != null
                    ? iconCache.getOrDefault(url, DEFAULT_VIDEO_ICON)
                    : DEFAULT_VIDEO_ICON;

            setIcon(icon);
            setText(null);

            return rendererComponent;
        }
    }

    /**
     * Adds html elements to highlight the current search text (if any) in yellow.
     */
    private abstract class AHighlightingRenderer extends DefaultTableCellRenderer
    {
        protected String highlight(String text)
        {
            final String searchText = txtSearch.getText().toLowerCase();

            if (!searchText.isEmpty())
            {
                final String searchableText = text.toLowerCase();
                final int index = searchableText.indexOf(searchText);

                if (index >= 0)
                {
                    final int endIndex = index + searchText.length();
                    final String start = text.substring(0, index);
                    final String mid = text.substring(index, endIndex);
                    final String end = text.substring(endIndex);

                    return start + "<font color=yellow>" + mid + "</font>" + end;
                }
            }

            return text;
        }
    }

    /**
     * Filter to select rows that contain the search text in the feed alias.
     */
    private static class TextFilter extends RowFilter<VideoSelectionTableModel, Integer>
    {
        private final String filterText;

        private TextFilter(String filterText)
        {
            this.filterText = filterText.toLowerCase();
        }

        @Override
        public boolean include(Entry<? extends VideoSelectionTableModel, ? extends Integer> entry)
        {
            final Feed feed = entry.getModel().getFeed(entry.getIdentifier());
            return feed.alias().toLowerCase().contains(filterText);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel2 = new JPanel();
        var sortPanel = new JPanel();
        btnCancel = new JButton();
        txtSearch = new PlaceholderTextField();
        var scrollPane1 = new JScrollPane();
        tblFeeds = new JXTable();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== panel2 ========
        {
            panel2.setBorder(new CompoundBorder(
                    Borders.DLU2_BORDER,
                    new CompoundBorder(
                            LineBorder.createGrayLineBorder(),
                            Borders.DLU2_BORDER)));
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "default:grow",
                    "2*(default, $lgap), fill:default:grow, $lgap, default"));

            //======== sortPanel ========
            {
                sortPanel.setName("sortPanel");
                sortPanel.setLayout(new FormLayout(
                        "4*(default, $lcgap), default:grow, $lcgap, default",
                        "default"));

                //---- btnCancel ----
                btnCancel.setIconTextGap(0);
                btnCancel.setFocusable(false);
                btnCancel.setName("btnCancel");
                btnCancel.addActionListener(e -> btnCancelActionPerformed());
                sortPanel.add(btnCancel, cc.xy(11, 1));
            }
            panel2.add(sortPanel, cc.xy(1, 1));

            //---- txtSearch ----
            txtSearch.setPlaceholderText("Search...");
            txtSearch.setName("txtSearch");
            panel2.add(txtSearch, cc.xy(1, 3));

            //======== scrollPane1 ========
            {
                scrollPane1.setName("scrollPane1");

                //---- tblFeeds ----
                tblFeeds.setModel(new DefaultTableModel(1, 0));
                tblFeeds.setRowSelectionAllowed(false);
                tblFeeds.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                tblFeeds.setFocusable(false);
                tblFeeds.setHorizontalScrollEnabled(true);
                tblFeeds.setShowVerticalLines(false);
                tblFeeds.setPreferredScrollableViewportSize(new Dimension(200, 100));
                tblFeeds.setName("tblFeeds");
                scrollPane1.setViewportView(tblFeeds);
            }
            panel2.add(scrollPane1, cc.xy(1, 5));

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(4, 1));
            }
            panel2.add(buttonBar, cc.xy(1, 7));
        }
        add(panel2, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton btnCancel;
    private PlaceholderTextField txtSearch;
    private JXTable tblFeeds;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
