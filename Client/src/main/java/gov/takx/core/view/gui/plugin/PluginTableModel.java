package gov.takx.core.view.gui.plugin;

import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.PluginCategory;

import javax.swing.table.AbstractTableModel;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Table model used by the PluginManagerDialog to display installed plugins.
 *
 * @since 1.3
 */
class PluginTableModel extends AbstractTableModel
{
    public enum Columns
    {
        Family("Family"),
        Type("Type"),
        Version("Version"),
        Category("Plugin Category");

        private final String name;

        Columns(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public static final Columns[] COLUMN_VALUES = values();
    }

    private final List<IPluginDefinition<? extends IRaptorPlugin>> pluginDefinitions;

    PluginTableModel(Set<IPluginDefinition<? extends IRaptorPlugin>> pluginDefinitionSet)
    {
        // Filter out any plugin without an instance (which includes synthetic plugins) to avoid clutter in the table
        pluginDefinitions = pluginDefinitionSet.stream()
                .filter(pd -> pd.getInstance() != null)
                .collect(Collectors.toList());
    }

    public IPluginDefinition<? extends IRaptorPlugin> getPluginDefinition(int row)
    {
        return pluginDefinitions.get(row);
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (getRowCount() > rowIndex)
        {
            IPluginDefinition<? extends IRaptorPlugin> pluginDefinition = pluginDefinitions.get(rowIndex);
            switch (Columns.COLUMN_VALUES[columnIndex])
            {
                case Family:
                    return pluginDefinition.getFamily();
                case Type:
                    return pluginDefinition.getType();
                case Version:
                    return pluginDefinition.getVersion();
                case Category:
                    return pluginDefinition.getPluginCategory();
            }
        }
        return "";
    }

    @Override
    public String getColumnName(int column)
    {
        return Columns.COLUMN_VALUES[column].getName();
    }

    @Override
    public int getRowCount()
    {
        return pluginDefinitions.size();
    }

    @Override
    public int getColumnCount()
    {
        return Columns.COLUMN_VALUES.length;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return Columns.COLUMN_VALUES[columnIndex] == Columns.Category ? PluginCategory.class : String.class;
    }
}
