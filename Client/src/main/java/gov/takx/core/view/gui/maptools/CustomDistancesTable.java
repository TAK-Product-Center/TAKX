package gov.takx.core.view.gui.maptools;

import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValues;

import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Table for displaying/editing custom distances and labels for range fans.
 *
 * @since 1.49
 */
public class CustomDistancesTable extends JXTable
{
    private boolean usingCustomLabels = false;

    /**
     * Constructor.
     *
     * @param distances List of custom distances, may be {@code null} yielding an empty table
     * @param labels    List of custom labels, may be {@code null} if not using labels.  The label column will be hidden.
     */
    public CustomDistancesTable(@Nullable List<Double> distances, @Nullable List<String> labels)
    {
        super(new CustomDistancesTableModel(distances, labels));

        setVisibleRowCount(10);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);
        setSortable(false);

        setDefaultRenderer(String.class, new DefaultTableRenderer(StringValues.TO_STRING, SwingConstants.RIGHT));

        setUsingCustomLabels(labels != null);
    }

    /**
     * Replace the distances and labels in the model.
     *
     * @param distances List of custom distances. if {@code null} a def
     * @param labels    List of custom labels, may be {@code null} if not using labels (defaulting to distance as label)
     */
    public void setDistancesAndLabels(@Nullable List<Double> distances, @Nullable List<String> labels)
    {
        setModel(new CustomDistancesTableModel(distances, labels));
        setUsingCustomLabels(labels != null);
    }

    @Override
    public CustomDistancesTableModel getModel()
    {
        return (CustomDistancesTableModel) super.getModel();
    }

    /**
     * @return {@code true} if the user has configured to use custom labels
     */
    public boolean isUsingCustomLabels()
    {
        return usingCustomLabels;
    }

    /**
     * @return The list of distances as edited by the user
     */
    public List<Double> getDistances()
    {
        return getModel().getDistances();
    }

    /**
     * @return The list of labels as edited by the user, {@code null} if labels are not in use
     * @see #isUsingCustomLabels()
     */
    public List<String> getLabels()
    {
        return usingCustomLabels ? getModel().getLabels() : null;
    }

    /**
     * Set whether custom labels are being used.  Toggle the visibility of the label column appropriately.
     */
    public void setUsingCustomLabels(boolean usingCustomLabels)
    {
        this.usingCustomLabels = usingCustomLabels;

        getColumnExt(CustomDistancesTableModel.COLUMN_NAME[1]).setVisible(usingCustomLabels);
    }

    /**
     * Add a new row immediately after the specified row.
     *
     * @param row Index of row after which to add a new row
     */
    public void addRowAfter(int row)
    {
        getModel().addRowAfter(row);
    }

    /**
     * Remove the specified row from the model.
     *
     * @param row Index of row to remove
     */
    public void removeRow(int row)
    {
        getModel().removeRow(row);
    }

    public void setUnitsLabel(String symbol)
    {
        setModel(getModel().setUnitsLabel(symbol));
    }

    /**
     * Table model to hold the distances and associated label.
     */
    public static final class CustomDistancesTableModel extends AbstractTableModel
    {
        private static final Class<?>[] COLUMN_CLASS = {Double.class, String.class};
        private static final String[] COLUMN_NAME = {"Distance", "Label"};

        private final List<DistanceAndLabel> data = new ArrayList<>();

        /**
         * Constructor.
         *
         * @param distances List of distance values, if {@code null}, and empty model will be created
         * @param labels    List of range labels, may be {@code null}
         */
        public CustomDistancesTableModel(@Nullable List<Double> distances, @Nullable List<String> labels)
        {
            if (distances != null)
            {
                for (int i = 0; i < distances.size(); i++)
                {
                    final String label = labels == null ? "" : labels.get(i);

                    data.add(new DistanceAndLabel(distances.get(i), label));
                }
            }
        }

        public CustomDistancesTableModel setUnitsLabel(String symbol)
        {
            COLUMN_NAME[0] = "Distance (" + symbol + ")";
            return this;
        }

        /**
         * @return List of distances from model
         */
        public List<Double> getDistances()
        {
            return data.stream().map(distanceAndLabel -> distanceAndLabel.distance).toList();
        }

        /**
         * @return List of labels from model
         */
        public List<String> getLabels()
        {
            return data.stream().map(distanceAndLabel -> distanceAndLabel.label).toList();
        }

        /**
         * Add a new empty row after the given row.
         */
        public void addRowAfter(int row)
        {
            final int newRowIndex = row + 1;
            data.add(newRowIndex, new DistanceAndLabel(0, ""));
            fireTableRowsInserted(newRowIndex, newRowIndex);
        }

        /**
         * Remove the given row from the model.
         */
        public void removeRow(int row)
        {
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }

        @Override
        public int getRowCount()
        {
            return data.size();
        }

        @Override
        public int getColumnCount()
        {
            return COLUMN_NAME.length;
        }

        @Override
        public String getColumnName(int column)
        {
            return COLUMN_NAME[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            final DistanceAndLabel distanceAndLabel = data.get(rowIndex);

            return switch (columnIndex)
            {
                case 0 -> distanceAndLabel.distance;
                case 1 -> distanceAndLabel.label;
                default -> throw new ArrayIndexOutOfBoundsException("columnIndex must be 0 or 1");
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex)
        {
            final DistanceAndLabel distanceAndLabel = data.get(rowIndex);

            switch (columnIndex)
            {
                case 0 -> distanceAndLabel.distance = value == null ? 0 : (double) value;
                case 1 -> distanceAndLabel.label = (String) value;
                default -> throw new ArrayIndexOutOfBoundsException("columnIndex must be 0 or 1");
            }

            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return true; // All cells editable
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return COLUMN_CLASS[columnIndex];
        }

        /**
         * Simple distance/label tuple.
         */
        private static final class DistanceAndLabel
        {
            double distance;
            String label;

            DistanceAndLabel(double distance, String label)
            {
                this.distance = distance;
                this.label = label;
            }
        }
    }
}
