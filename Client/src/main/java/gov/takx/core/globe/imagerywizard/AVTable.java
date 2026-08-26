/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.avlist.AVList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 */
public class AVTable<T extends AVList> extends JTable
{
    public static class AVModel<T extends AVList> extends AbstractTableModel
    {
        // AVList properties.
        private int numColumns;
        private int numRows;
        private final String[] names;
        private final Class<?>[] types;
        private final String[] attributes;
        private List<T> values = new ArrayList<>();

        public AVModel(int numColumns, String[] names, Class<?>[] types, String[] attributes)
        {
            this.numColumns = numColumns;
            this.names = new String[numColumns];
            this.types = new Class[numColumns];
            this.attributes = new String[numColumns];
            System.arraycopy(names, 0, this.names, 0, this.numColumns);
            System.arraycopy(types, 0, this.types, 0, this.numColumns);
            System.arraycopy(attributes, 0, this.attributes, 0, this.numColumns);
        }

        @Override
        public int getColumnCount()
        {
            return numColumns;
        }

        public void setColumnCount(int count)
        {
            numColumns = count;
        }

        @Override
        public int getRowCount()
        {
            return numRows;
        }

        public void setRowCount(int count)
        {
            numRows = count;
        }

        @Nullable
        @Override
        public String getColumnName(int columnIndex)
        {
            return names[columnIndex];
        }

        @Nullable
        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return types[columnIndex];
        }

        @Nullable
        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            T values = getValuesAt(rowIndex);
            String attribute = getColumnAttribute(columnIndex);
            return getValue(values, attribute);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            T values = getValuesAt(rowIndex);
            String attribute = getColumnAttribute(columnIndex);
            setValue(values, attribute, aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            T values = getValuesAt(rowIndex);
            String attribute = getColumnAttribute(columnIndex);
            return isAttributeEditable(values, attribute);
        }

        @Nonnull
        public List<T> getValues()
        {
            return new ArrayList<>(values);
        }

        public void setValues(List<? extends T> newValues)
        {
            numRows = newValues.size();
            values = new ArrayList<>(newValues);
            fireTableDataChanged();
        }

        @Nullable
        protected Object getValue(T values, String attribute)
        {
            return values.getValue(attribute);
        }

        protected void setValue(T values, String attribute, Object aValue)
        {
            values.setValue(attribute, aValue);
        }

        protected boolean isAttributeEditable(T values, String attribute)
        {
            return false;
        }

        @Nullable
        public String getColumnAttribute(int columnIndex)
        {
            return attributes[columnIndex];
        }

        @Nullable
        public T getValuesAt(int rowIndex)
        {
            return values.get(rowIndex);
        }

        public int getColumnFor(String attribute)
        {
            int columnIndex;
            for (columnIndex = 0; columnIndex < numColumns; columnIndex++)
            {
                if (getColumnAttribute(columnIndex).equals(attribute)) break;
            }
            return (columnIndex < numColumns) ? columnIndex : -1;
        }

        public int getRowFor(T values)
        {
            int rowIndex;
            for (rowIndex = 0; rowIndex < numRows; rowIndex++)
            {
                if (getValuesAt(rowIndex).equals(values)) break;
            }
            return (rowIndex < numRows) ? rowIndex : -1;
        }
    }

    @Nullable
    public static List<? extends AVList> sortValues(List<? extends AVList> values, String[] keys)
    {
        if (values != null && keys != null && keys.length > 0) values.sort(new AVListComparator(keys));
        return values;
    }

    protected AVModel<T> model;

    public AVTable(AVModel<T> model)
    {
        super(model);
        this.model = model;
    }

    @Nullable
    @Override
    public String getToolTipText(MouseEvent event)
    {
        java.awt.Point p = event.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        int realColIndex = convertColumnIndexToModel(colIndex);

        String toolTip = getToolTipText(rowIndex, realColIndex);
        if (toolTip != null) return toolTip;

        return super.getToolTipText(event);
    }

    @Nullable
    protected String getToolTipText(int rowIndex, int columnIndex)
    {
        if (rowIndex < 0 || columnIndex < 0 || rowIndex >= model.getRowCount() || columnIndex >= model.getColumnCount())
        {
            return null;
        }

        T values = getValuesAt(rowIndex);
        String attribute = getColumnAttribute(columnIndex);

        return getToolTipText(values, attribute);
    }

    @Nullable
    protected String getToolTipText(T values, String attribute)
    {
        Object o = values.getValue(attribute);
        return (o != null) ? o.toString() : null;
    }

    @Nonnull
    public List<T> getValues()
    {
        return model.getValues();
    }

    public void setValues(List<? extends T> values)
    {
        model.setValues(values);
        setPreferredColumnWidths();
    }

    public void setAllValues(String attribute, Object aValue)
    {
        for (T values : model.getValues())
        {
            if (model.isAttributeEditable(values, attribute)) values.setValue(attribute, aValue);
        }
        fireTableDataChanged();
    }

    @Nullable
    public String getColumnAttribute(int columnIndex)
    {
        return model.getColumnAttribute(columnIndex);
    }

    @Nullable
    public T getValuesAt(int rowIndex)
    {
        return model.getValuesAt(rowIndex);
    }

    public int getRowFor(T values)
    {
        return model.getRowFor(values);
    }

    @Nonnull
    public List<T> getSelectedValues()
    {
        int rowCount = getSelectedRowCount();
        int[] rows = getSelectedRows();
        if (rowCount < 1 || rows == null) return Collections.emptyList();

        List<T> selected = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) selected.add(model.getValuesAt(rows[i]));

        return selected;
    }

    @Nonnull
    public List<T> getAllValues(String attribute, Object aValue)
    {
        List<T> valueList = new ArrayList<>();
        for (T values : model.getValues())
        {
            Object value = values.getValue(attribute);
            if (Objects.equals(aValue, value)) valueList.add(values);
        }

        return valueList;
    }

    public void fireTableDataChanged()
    {
        model.fireTableDataChanged();
    }

    public void fireTableValuesChanged(T values)
    {
        int rowIndex = model.getRowFor(values);
        if (rowIndex >= 0 && rowIndex < model.getRowCount()) model.fireTableRowsUpdated(rowIndex, rowIndex);
    }

    protected void setPreferredColumnWidths()
    {
        for (int col = 0; col < getColumnModel().getColumnCount(); col++)
        {
            // Start with size of column header
            JLabel label = new JLabel(getColumnName(col));
            int size = label.getPreferredSize().width;

            // Find any cells in column that have a wider value
            Class<?> cls = getColumnClass(col);
            TableCellRenderer renderer = getDefaultRenderer(cls);

            for (int row = 0; row < model.getRowCount(); row++)
            {
                Object value = getValueAt(row, col);
                Component c = renderer.getTableCellRendererComponent(this, value, false, false, row, col);
                if (c != null && c.getPreferredSize().width > size) size = c.getPreferredSize().width;
            }

            TableColumn tableCol = getColumnModel().getColumn(col);
            tableCol.setPreferredWidth(size);
        }
    }
}
