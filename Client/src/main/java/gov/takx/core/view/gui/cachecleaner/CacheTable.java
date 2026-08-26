package gov.takx.core.view.gui.cachecleaner;

import gov.nasa.worldwindx.examples.util.FileStoreDataSet;
import gov.takx.commons.swing.SelectAllTableSupport;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.Color;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

/**
 */
public class CacheTable extends JTable
{
    protected CacheCleanerDialog owner;
    private final CacheModel model;

    private static class CacheModel extends AbstractTableModel
    {
        private static final String[] columnTitles = {"Select", "Map Set", "Size (MB)", "Last Used"};
        private static final Class<?>[] columnTypes = new Class[]{Boolean.class, String.class, Long.class, String.class};

        private final ArrayList<FileStoreDataSet> datasets = new ArrayList<>();
        private final ArrayList<Integer> selected = new ArrayList<>();

        public void setDataSets(List<FileStoreDataSet> sets)
        {
            datasets.clear();
            selected.clear();

            if (sets != null)
            {
                datasets.addAll(sets);
            }
        }

        @Override
        public int getRowCount()
        {
            return datasets.size();
        }

        @Override
        public int getColumnCount()
        {
            return columnTitles.length;
        }

        @Override
        public String getColumnName(int column)
        {
            return columnTitles[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return columnTypes[columnIndex];
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            if (columnIndex != 0) return;

            if ((Boolean) aValue && !selected.contains(rowIndex))
            {
                selected.add(rowIndex);
            } else if (!(Boolean) aValue && selected.contains(rowIndex))
            {
                selected.remove((Integer) rowIndex);
            }

            fireTableCellUpdated(rowIndex, columnIndex);
        }

        public int getNumSelected()
        {
            return selected.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            FileStoreDataSet ds = datasets.get(rowIndex);

            return switch (columnIndex)
            {
                case 0 -> selected.contains(rowIndex);
                case 1 -> ds.getName();
                case 2 -> String.format("%5.1f", ((float) ds.getSize()) / 1e6);
                case 3 ->
                {
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.setTimeInMillis(ds.getLastModified());
                    SimpleDateFormat sdf = new SimpleDateFormat("  MM/dd/yy");
                    yield sdf.format(cal.getTime());
                }
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex == 0;
        }
    }

    public CacheTable(final CacheCleanerDialog owner)
    {
        super(new CacheModel());
        this.owner = owner;

        model = ((CacheModel) getModel());
        model.addTableModelListener(e -> owner.enableCleanButton(model.getNumSelected() > 0));
        setShowGrid(true);
        setGridColor(Color.BLACK);
        setShowHorizontalLines(true);
        setShowVerticalLines(true);
        setColumnSelectionAllowed(false);
        setRowSelectionAllowed(false);
        setPreferredScrollableViewportSize(new Dimension(200, 100));

        SelectAllTableSupport.configureSelectAllColumn(this, model, 0, false, 28);

        setDragEnabled(false);
        getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Set this table with a list of cache data sets.
     *
     * @param sets a list of cache data sets.
     */
    public void setDataSets(List<FileStoreDataSet> sets)
    {
        model.setDataSets(sets);
        setPreferredColumnWidths();
    }

    /**
     * Get a list of selected (checked) data sets from the table.
     *
     * @return a list of selected data sets.
     */
    public List<FileStoreDataSet> getSelectedDataSets()
    {
        if (model.selected.isEmpty())
        {
            return Collections.emptyList();
        }

        ArrayList<FileStoreDataSet> selected = new ArrayList<>();
        for (Integer i : model.selected)
        {
            if (i < model.datasets.size())
            {
                selected.add(model.datasets.get(i));
            }
        }

        return selected;
    }

    /**
     * Give the columns custom widths based upon their contents.
     */
    private void setPreferredColumnWidths()
    {
        final TableColumnModel columnModel = getColumnModel();
        for (int columnIndex = 1; columnIndex < columnModel.getColumnCount(); columnIndex++)
        {
            // Start with size of column header
            JLabel label = new JLabel(getColumnName(columnIndex));
            int size = label.getPreferredSize().width;

            // Find any cells in column that have a wider value
            TableColumn column = columnModel.getColumn(columnIndex);
            for (int row = 0; row < model.getRowCount(); row++)
            {
                label = new JLabel(getValueAt(row, columnIndex).toString());
                if (label.getPreferredSize().width > size)
                {
                    size = label.getPreferredSize().width;
                }
            }
            column.setPreferredWidth(size);
        }
    }
}
