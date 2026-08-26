package gov.takx.commons.swing;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;

import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.6
 */
public class AbstractTableModelExtTest
{
    @Test
    public void testFireColumn()
    {
        TestTableModel model = new TestTableModel();
        MutableInt row = new MutableInt();
        MutableInt column = new MutableInt();
        TableModelListener listener = e -> {
            row.setValue(e.getFirstRow());
            column.setValue(e.getColumn());
        };
        model.addTableModelListener(listener);

        int updateColumn = 2;
        model.fireTableColumnsUpdated(updateColumn, updateColumn);
        assertThat(row.getValue()).isEqualTo(updateColumn);
        assertThat(column.getValue()).isEqualTo(updateColumn);

        updateColumn = 0;
        int updateRow = 1;
        model.fireTableCellsUpdated(updateRow, updateRow, updateColumn, updateColumn);
        assertThat(row.getValue()).isEqualTo(updateRow);
        assertThat(column.getValue()).isEqualTo(updateColumn);

        updateColumn = 1;
        updateRow = 2;
        model.fireTableCellsUpdated(new int[]{updateRow}, new int[]{updateColumn});
        assertThat(row.getValue()).isEqualTo(updateRow);
        assertThat(column.getValue()).isEqualTo(updateColumn);
    }

    static class TestTableModel extends AbstractTableModelExt
    {
        static final String[] columns = new String[]{"c0", "c1", "c2"};
        private final DefaultTableModel delegate = new DefaultTableModel();

        TestTableModel()
        {
            for (String column : columns)
            {
                delegate.addColumn(column);
            }
            for (int i = 0; i < 3; i++)
            {
                Object[] row = new Object[columns.length];
                for (int j = 0; j < columns.length; j++)
                {
                    row[j] = i + "" + j;
                }
                delegate.addRow(row);
            }
        }

        @Override
        public int getRowCount()
        {
            return delegate.getRowCount();
        }

        @Override
        public int getColumnCount()
        {
            return delegate.getColumnCount();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            return delegate.getValueAt(rowIndex, columnIndex);
        }
    }
}
