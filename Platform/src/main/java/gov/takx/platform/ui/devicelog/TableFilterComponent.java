package gov.takx.platform.ui.devicelog;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.TableColumnModelListenerAdapter;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.TableColumn;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.regex.PatternSyntaxException;

/**
 * Custom component to manage the Search/Filter feature of a given table.  It provides a text input field
 * allowing the user to enter search text.  If configured, there is also a combo box that lets the user select
 * the column against which the filter is applied.  If not configured, the search text is matched against all
 * visible columns.
 * <p>
 * Consumers of this component must call {@link #setTable(JXTable)} prior to any other configuration.
 *
 * @see FilterColumnSelector
 * @see #setUseColumnSpecificFiltering(boolean)
 * @since 2.16
 */
public class TableFilterComponent extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Color HIGHLIGHT_COLOR = Color.red;

    private JXTable table;
    private boolean useColumnSpecificFiltering = false; // Filter against all columns, or add control to pick one
    private String lastFilterString = "";
    private ColumnListener columnListener = null;

    public TableFilterComponent()
    {
        initComponents();
        btnClearSearch.setIcon(UIResources.CANCEL_ICON);

        filterColumnSelector.addActionListener(e -> filterColumnChanged());
        filterColumnSelector.setVisible(false); // Default to "all columns" filtering
    }

    /**
     * Set the table for which we are to offer filtering.  This must be called prior to trying to configure
     * column specific filtering.
     *
     * @param table The table to filter
     * @see #setUseColumnSpecificFiltering(boolean)
     */
    public void setTable(@Nonnull JXTable table)
    {
        this.table = table;

        // If the model is ever replaced, reset our internal controls
        table.addPropertyChangeListener("model", evt -> clear());
    }

    /**
     * Set whether filtering is performed against all visible columns or a single column.  If
     * {@code useColumnSpecificFiltering} is {@code true} then a control will be displayed allowing the user to
     * select a specific column against which the filter text will be applied.  If it is {@code false} then
     * the column selection control will be hidden, and the filter will be matched against every visible column.
     * <p>
     * Be aware that on very large models (100K+ rows), filtering against all columns is incredibly slow and should
     * be avoided.
     *
     * @param useColumnSpecificFiltering Pass {@code true} to enable column-specific filtering
     * @throws IllegalStateException If {@link #setTable(JXTable)} has not been called
     */
    public void setUseColumnSpecificFiltering(boolean useColumnSpecificFiltering) throws IllegalStateException
    {
        if (table == null) throw new IllegalStateException("You must call setTable() first");

        this.useColumnSpecificFiltering = useColumnSpecificFiltering;

        if (useColumnSpecificFiltering)
        {
            filterColumnSelector.setModelFromTable(table);
            filterColumnSelector.setVisible(true);
            filterColumnSelector.selectNone();
        } else
        {
            filterColumnSelector.setVisible(false);
            txtSearchString.setEnabled(true);
        }
    }

    /**
     * Clear the filter text and reset the column selector to None.
     */
    public void clear()
    {
        filterColumnSelector.selectNone();
        clearSearchStringTextField();
    }

    /**
     * Handle a change in the filter column selection.
     */
    private void filterColumnChanged()
    {
        if (logger.isDebugEnabled()) logger.debug("filter col: {}", filterColumnSelector.getSelectedColumn());

        if (useColumnSpecificFiltering)
        {
            if (filterColumnSelector.getSelectedColumn() == null) // They selected "None"
            {
                table.setRowFilter(null);
                lastFilterString = ""; // So it gets reinstated if the column is set back to not "None"
                txtSearchString.setEnabled(false); // Not until we have a column selected
            } else
            {
                txtSearchString.setEnabled(true);
                lastFilterString = ""; // Force the update on the filter
                txtSearchString.requestFocus(); // And give the focus back to the search text field

                txtSearchUpdate(true); // Update with new column/text
            }
        }
    }

    /**
     * Refresh the filter to either use a regex or regular text string.
     *
     * @since 2.36
     */
    private void regexCheckBoxActionPerformed()
    {
        txtSearchUpdate(true);
    }

    /**
     * Clear the search text and give focus to the text field if it is enabled.
     */
    private void btnClearSearchActionPerformed()
    {
        clearSearchStringTextField();

        if (txtSearchString.isEnabled())
        {
            txtSearchString.requestFocus(); // And give the focus back to the search text field
        }
    }

    /**
     * Handle the caret moving in the search text field.  If the text has actually changed, then update the
     * installed filter accordingly.
     */
    private void txtSearchStringCaretUpdate()
    {
        txtSearchUpdate(false);
    }

    /**
     * Trigger a refresh of the table filter.  By default, the filter will only refresh if the user entered text has
     * changed.  This can be overridden using the {@code forceRefresh} flag.
     *
     * @param forceRefresh True if the search filter should be refreshed regardless of text updates, false if the search
     *                     filter should only update if the user entered text has changed.
     * @since 2.36
     */
    private void txtSearchUpdate(boolean forceRefresh)
    {
        final String text = txtSearchString.getText();

        if (StringUtils.isEmpty(text))
        {
            clearFilter(); // No text, no filter
        } else if (forceRefresh || !lastFilterString.equals(text))
        {
            lastFilterString = text;

            if (useColumnSpecificFiltering)
            {
                final String filterColumn = filterColumnSelector.getSelectedColumn();
                if (filterColumn != null)
                {
                    try
                    {
                        table.setRowFilter(new SimpleMatchRowFilter(text, regexCheckBox.isSelected(), table.getColumnExt(filterColumn).getModelIndex()));
                        txtSearchString.setBackground(null);
                        txtSearchString.setToolTipText(null);
                    } catch (PatternSyntaxException e)
                    {
                        setRegexTextFieldInvalid();
                    }
                }
            } else
            {
                installAllColumnFilter(text);
            }
        }
    }

    /**
     * Clear the row filter.  Reset the filter text.
     */
    public void clearFilter()
    {
        lastFilterString = "";
        table.setRowFilter(null);
        clearSearchStringTextField();
        removeColumnModelListener();
    }

    /**
     * Clears the search string and removes the background color if present.
     *
     * @since 2.36
     */
    private void clearSearchStringTextField()
    {
        txtSearchString.setText("");
        txtSearchString.setBackground(null);
        txtSearchString.setToolTipText(null);
    }

    /**
     * Updates the search/filter text field to show that an invalid Regex has been entered, and removes the row filter
     * since the provided Regex can't be used.
     *
     * @since 2.36
     */
    private void setRegexTextFieldInvalid()
    {
        txtSearchString.setBackground(HIGHLIGHT_COLOR);
        txtSearchString.setToolTipText("The Regex is invalid; see https://regexr.com for help.");
        table.setRowFilter(null);
    }

    /**
     * Construct and install a filter that matches against all visible columns.
     *
     * @param text The text to match against
     */
    private void installAllColumnFilter(String text)
    {
        // Get the column indices of the visible columns
        final int[] columnIndices = table.getColumns(false).stream()
                .mapToInt(TableColumn::getModelIndex).toArray();

        // And install the filter
        try
        {
            table.setRowFilter(new SimpleMatchRowFilter(text, regexCheckBox.isSelected(), columnIndices));
            txtSearchString.setBackground(null);
            txtSearchString.setToolTipText(null);
        } catch (PatternSyntaxException e)
        {
            setRegexTextFieldInvalid();
        }

        addColumnModelListener(); // Watch for model changes
    }

    /**
     * If one is not already installed, create and install a table column model listener that will force the
     * regeneration/application of the filter.  This handles the case where a column is added or removed and
     * we need to re-filter the table (I would have expected this to be automatic, but apparently not).
     */
    private void addColumnModelListener()
    {
        if (columnListener == null)
        {
            columnListener = new ColumnListener();
            table.getColumnModel().addColumnModelListener(columnListener);
        }
    }

    /**
     * Remove any column model listener that may have been applied.
     */
    private void removeColumnModelListener()
    {
        if (columnListener != null)
        {
            table.getColumnModel().removeColumnModelListener(columnListener);
            columnListener = null;
        }
    }

    /**
     * Handler for when columns are added/removed.  When this happens we need to rebuild our filter.
     */
    private class ColumnListener extends TableColumnModelListenerAdapter
    {
        @Override
        public void columnAdded(TableColumnModelEvent e)
        {
            installAllColumnFilter(lastFilterString);
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e)
        {
            installAllColumnFilter(lastFilterString);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var lblFilter = new JLabel();
        filterColumnSelector = new FilterColumnSelector();
        txtSearchString = new JTextField();
        regexCheckBox = new JCheckBox();
        btnClearSearch = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "2*(default, $lcgap), default:grow, 2*($lcgap, default)",
                "default"));

        //---- lblFilter ----
        lblFilter.setText("Filter:");
        lblFilter.setName("lblFilter");
        add(lblFilter, cc.xy(1, 1));

        //---- filterColumnSelector ----
        filterColumnSelector.setToolTipText("Select a column to search against");
        filterColumnSelector.setModel(new DefaultComboBoxModel<>(new String[]{
                "None"
        }));
        filterColumnSelector.setName("filterColumnSelector");
        add(filterColumnSelector, cc.xy(3, 1));

        //---- txtSearchString ----
        txtSearchString.setToolTipText("Enter search text");
        txtSearchString.setName("searchTextField");
        txtSearchString.addCaretListener(e -> txtSearchStringCaretUpdate());
        add(txtSearchString, cc.xy(5, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

        //---- regexCheckBox ----
        regexCheckBox.setText("Regex");
        regexCheckBox.setToolTipText("Allows using Regular Expression (Regex) searches");
        regexCheckBox.setName("regexCheckBox");
        regexCheckBox.addActionListener(e -> regexCheckBoxActionPerformed());
        add(regexCheckBox, cc.xy(7, 1));

        //---- btnClearSearch ----
        btnClearSearch.setToolTipText("Clear Search");
        btnClearSearch.setName("btnClearSearch");
        btnClearSearch.addActionListener(e -> btnClearSearchActionPerformed());
        add(btnClearSearch, cc.xy(9, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private FilterColumnSelector filterColumnSelector;
    private JTextField txtSearchString;
    private JCheckBox regexCheckBox;
    private JButton btnClearSearch;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
