package gov.takx.core.view.gui.gazetteer;

import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.core.map.layer.feature.datastore.SharedFeatureDataStore;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.gazetteer.GazetteerEntry;
import gov.takx.api.gazetteer.IGazetteerService;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.MouseClickedListener;
import gov.takx.commons.swing.SelectAllTableSupport;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.core.globe.tak.features.IFeatureProvider;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.gazetteer.SearchResultsTableModel.Columns;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides user access to the gazetteer search services.  With the search results, the user can select to show
 * placemark icons at the location of the result, and may navigate the globe to a selected result.
 *
 * @see IGazetteerService
 * @since 3.0 (existed in 2.x, but has been re-implemented using the new gazetteer service)
 */
@PseudoBean(singletonObserver = true)
public class ContextSearchPanel extends ATitledPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PLACEMARK_ICON = FeatureUtility.sanitizeUri(UIResources.getUrl(ResourceDirectory.RESOURCES, "blue_placemark.png").toString());
    public static final Style ICON_POINT_STYLE = FeatureUtility.makeNoLabelStyle(new IconPointStyle(-1, PLACEMARK_ICON, 32f, 32f, 0, 0, 0, false));
    private static final int SELECTED_COL_INDEX = Columns.Selected.ordinal();
    private static final int SELECTED_COL_WIDTH = 30;

    @Inject
    private IGazetteerService gazetteerService;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    @SharedFeatureDataStore
    private FeatureDataStore4 dataStore;

    private final SearchResultsTableModel resultsModel = new SearchResultsTableModel();
    private final Map<GazetteerEntry, Long> featureIdMap = new HashMap<>();

    private long featureSetId = -1;

    public ContextSearchPanel()
    {
        super(ButtonType.CITY_SEARCH.getDisplayText());

        initComponents();

        buttonGoto.setIcon(UIResources.GOTO_ACTION_BUTTON_ICON);
        buttonClearPlacemarks.setIcon(UIResources.REMOVE_ACTION_BUTTON_ICON);

        resultsModel.addTableModelListener(this::handleModelChange); // Listen for "selected" changes in the model

        resultsTable.setModel(resultsModel);
        resultsTable.getSelectionModel().addListSelectionListener(new RowListener());
        resultsTable.addMouseListener((MouseClickedListener) this::mouseClicked);
        resultsTable.getTableHeader().setReorderingAllowed(false);

        // This sets up the selected column header to render as a checkbox which will cause all rows to be selected/deselected
        SelectAllTableSupport.configureSelectAllColumn(resultsTable, resultsModel, SELECTED_COL_INDEX, false,
                selected -> globeManager.getPrimaryGlobeDelegate().redraw(),
                selected -> selected ? "Remove placemarks for all" : "Add placemarks for all", SELECTED_COL_WIDTH);

        // Only allow sorting on the "Name" column
        final var sorter = new TableRowSorter<TableModel>(resultsModel);
        sorter.setSortable(SELECTED_COL_INDEX, false);
        resultsTable.setRowSorter(sorter);

        addFocusListener(new FocusHandler()); // Give the search text field focus whenever panel gets focus
    }

    /**
     * Create the {@link FeatureSet} that will hold all of our placemark features.
     * @since 5.7
     */
    @PostConstruct
    private void postConstruct()
    {
        try
        {
            featureSetId = dataStore.insertFeatureSet(
                    new FeatureSet(getClass().getName(), IFeatureProvider.ALL_GLOBES_GROUP_NAME, "Placemarks", Double.MAX_VALUE, 0));
        } catch (DataStoreException e)
        {
            logger.error("Failed to create FeatureSet for Gazetteer placemarks", e);
        }
    }

    /**
     * When the "selected" value for a given row in the model changes, we need to show/hide the placemark.
     */
    private void handleModelChange(TableModelEvent event)
    {
        // We only want "update" events on the "selected" column
        if (event.getType() == TableModelEvent.UPDATE && event.getColumn() == SELECTED_COL_INDEX)
        {
            final int row = event.getFirstRow();
            final var result = resultsModel.getSearchResultAtRow(row);
            final boolean create = resultsModel.isSelected(row);

            try
            {
                if (create)
                {
                    if (!featureIdMap.containsKey(result)) featureIdMap.put(result, createFeature(result));
                } else
                {
                    final var featureId = featureIdMap.remove(result);
                    if (featureId != null) dataStore.deleteFeature(featureId);
                }
            } catch (DataStoreException e)
            {
                logger.warn("Failed to {} Feature for {}", create ? "create" : "remove", result.getName(), e);
            }

            buttonClearPlacemarks.setEnabled(!featureIdMap.isEmpty());
            globeManager.requestRedraw("placemarks changed", false);
        }
    }

    /**
     * @return ID of the feature representing the given search result
     * @since 5.7
     */
    private long createFeature(@Nonnull GazetteerEntry result) throws DataStoreException
    {
        final var geometry = new Point(result.getLongitude(), result.getLatitude());
        return dataStore.insertFeature(new Feature(featureSetId, result.getName(), geometry, ICON_POINT_STYLE, null));
    }

    /**
     * Perform the search using the current search text.
     */
    private void doSearch()
    {
        final String query = textFieldSearch.getText();

        if (StringUtils.isBlank(query)) return; // don't attempt to search with an empty query string

        removeAllPlacemarks(true); // Clear previous search results

        final long startTime = System.currentTimeMillis();
        final var results = gazetteerService.search(query);
        final double queryTime = (System.currentTimeMillis() - startTime) / 1_000.0;

        labelResultCount.setText(String.format("<html>Showing %d results (%.2f seconds)</html>", results.size(), queryTime));

        if (results.isEmpty())
        {
            AlwaysOnTopJOptionPane.showMessageDialog(buttonSearch,
                    "Your search for \"" + query + "\" returned 0 results.", "No Matches Found",
                    JOptionPane.INFORMATION_MESSAGE);

            textFieldSearch.requestFocus();
        } else
        {
            resultsModel.setSearchResults(results);
        }

        panelSearchSummary.revalidate();

        resultsTable.revalidate();
        resultsTable.repaint();

        revalidate();
        repaint();
    }

    /**
     * Clear all the placemarks.
     */
    private void buttonClearPlacemarksActionPerformed()
    {
        removeAllPlacemarks(false); // retain the model contents
    }

    /**
     * Remove all placemarks, reset UI controls, and optionally clearing the search results model.
     *
     * @param clearModel Pass {@code true} to also clear the results model
     * @since 5.7
     */
    private void removeAllPlacemarks(boolean clearModel)
    {
        buttonClearPlacemarks.setEnabled(false);
        buttonGoto.setEnabled(false);
        resultsModel.setAllSelected(false);
        featureIdMap.clear();

        if (clearModel) resultsModel.clear();

        globeManager.repaintAllGlobes();
        resultsTable.revalidate();
        repaint();
    }

    /**
     * Go to the location of the specified result row (in view coordinates).
     *
     * @param row The view index of the row
     */
    private void goTo(int row)
    {
        final var searchResult = resultsModel.getSearchResultAtRow(resultsTable.convertRowIndexToModel(row));
        globeManager.requireLastFocusedGlobe().goTo(searchResult.getLocation());
    }

    /**
     * Go to the location of the currently selected result row.
     */
    private void buttonGotoActionPerformed()
    {
        goTo(resultsTable.getSelectedRow());
    }

    /**
     * Perform a goto when the user double-clicks a result row.
     */
    private void mouseClicked(MouseEvent e)
    {
        final var point = e.getPoint();
        final int row = resultsTable.rowAtPoint(point);
        final int col = resultsTable.columnAtPoint(point);

        if (row >= 0 && e.getClickCount() > 1 && col != SELECTED_COL_INDEX) goTo(row);
    }

    /**
     * Remove all our placemarks and clear our model.
     * @since 5.7
     */
    @Override
    public void releaseResources()
    {
        removeAllPlacemarks(true);
    }

    /**
     * Enable/disable the goto button based on the current list selection.
     */
    private class RowListener implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (!e.getValueIsAdjusting()) buttonGoto.setEnabled(resultsTable.getSelectedRowCount() > 0);
        }
    }

    /**
     * Give the focus to the search text field.
     */
    private class FocusHandler extends FocusAdapter
    {
        @Override
        public void focusGained(FocusEvent e)
        {
            textFieldSearch.requestFocus();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var label1 = new JLabel();
        var label2 = new JLabel();
        textFieldSearch = new JTextField();
        buttonSearch = new JButton();
        var separator1 = new JSeparator();
        var panel2 = new JPanel();
        buttonGoto = new JButton();
        buttonClearPlacemarks = new JButton();
        panelSearchSummary = new JPanel();
        labelResultCount = new JLabel();
        var scrollPane1 = new JScrollPane();
        resultsTable = new JXTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setPreferredSize(new Dimension(300, 400));
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, $button",
                "default, $ugap, 2*(default, $lgap), fill:default, $lgap, default, $lgap, fill:default:grow"));

        //---- label1 ----
        label1.setText("Search World Cities");
        label1.setName("label1");
        add(label1, cc.xywh(1, 1, 4, 1));

        //---- label2 ----
        label2.setText("For");
        label2.setName("label2");
        add(label2, cc.xy(1, 3));

        //---- textFieldSearch ----
        textFieldSearch.setName("textFieldSearch");
        textFieldSearch.addActionListener(e -> doSearch());
        add(textFieldSearch, cc.xy(3, 3));

        //---- buttonSearch ----
        buttonSearch.setText("Search");
        buttonSearch.setName("buttonSearch");
        buttonSearch.addActionListener(e -> doSearch());
        add(buttonSearch, cc.xy(5, 3));

        //---- separator1 ----
        separator1.setName("separator1");
        add(separator1, cc.xywh(1, 5, 5, 1));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "$button, $lcgap, default:grow, $lcgap, $button",
                    "default"));

            //---- buttonGoto ----
            buttonGoto.setText("Go To");
            buttonGoto.setEnabled(false);
            buttonGoto.setToolTipText("Go to the location of the selected search result.");
            buttonGoto.setName("buttonGoto");
            buttonGoto.addActionListener(e -> buttonGotoActionPerformed());
            panel2.add(buttonGoto, cc.xy(1, 1));

            //---- buttonClearPlacemarks ----
            buttonClearPlacemarks.setText("Clear Placemarks");
            buttonClearPlacemarks.setEnabled(false);
            buttonClearPlacemarks.setToolTipText("Clear search placemarks.");
            buttonClearPlacemarks.setName("buttonClearPlacemarks");
            buttonClearPlacemarks.addActionListener(e -> buttonClearPlacemarksActionPerformed());
            panel2.add(buttonClearPlacemarks, cc.xy(5, 1));
        }
        add(panel2, cc.xywh(1, 7, 5, 1));

        //======== panelSearchSummary ========
        {
            panelSearchSummary.setName("panelSearchSummary");
            panelSearchSummary.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));

            //---- labelResultCount ----
            labelResultCount.setName("labelResultCount");
            panelSearchSummary.add(labelResultCount, cc.xy(1, 1));
        }
        add(panelSearchSummary, cc.xywh(1, 9, 5, 1));

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- resultsTable ----
            resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            resultsTable.setSortable(false);
            resultsTable.setPreferredScrollableViewportSize(new Dimension(100, 400));
            resultsTable.setName("resultsTable");
            scrollPane1.setViewportView(resultsTable);
        }
        add(scrollPane1, cc.xywh(1, 11, 5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField textFieldSearch;
    private JButton buttonSearch;
    private JButton buttonGoto;
    private JButton buttonClearPlacemarks;
    private JPanel panelSearchSummary;
    private JLabel labelResultCount;
    private JXTable resultsTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
