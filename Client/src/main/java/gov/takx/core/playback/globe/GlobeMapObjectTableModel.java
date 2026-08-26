package gov.takx.core.playback.globe;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.globe.IGlobeModel;
import gov.takx.commons.swing.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

import java.awt.RenderingHints;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 */
public class GlobeMapObjectTableModel extends DefaultTableModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<GlobeMO> dataVector;
    private final List<GlobeMO> showOnTableVector;

    public enum Columns
    {
        SELECTED("Show"),
        NAME("Name");

        private static final Columns[] VALUES = values();

        private final String description;

        Columns(String description)
        {
            this.description = description;
        }

        @Override
        public String toString()
        {
            return description;
        }
    }

    public GlobeMapObjectTableModel(IGlobeModel globeModel, Collection<IMapObject> currentMapObjects)
    {
        dataVector = new ArrayList<>();

        List<IMapObject> allMos = globeModel.getAllDevices();
        // In some cases (like sandbar) the plugin is going to add devices to playback that would otherwise not be
        // addable by the user. In this case, we need to go ahead and show them in the list. This allows the user to
        // remove them, but they won't be able to add them back. At least this will allow the user to new MO's to the
        // list and not loose the plugin added MO.
        currentMapObjects.forEach(iMapObject -> {
            if (!allMos.contains(iMapObject)) allMos.add(iMapObject);
        });
        allMos.forEach(mo -> dataVector.add(new GlobeMO(mo)));
        showOnTableVector = new ArrayList<>();
        updateVisibleVector("");
        setSelectedMapObjects(currentMapObjects);
    }

    public void toggleAllSelected()
    {
        int selectedOrdinal = Columns.SELECTED.ordinal();

        // Iterate through rows, and if one or more map objects are not selected, then select all.  Otherwise,
        // de-select all.
        boolean selectAll = false;
        for (int i = 0; i < getRowCount(); i++)
        {
            if (!(Boolean) getValueAt(i, selectedOrdinal))
            {
                selectAll = true;
            }
        }

        for (int i = 0; i < getRowCount(); i++)
        {
            setValueAt(selectAll, i, selectedOrdinal);
        }
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return Columns.VALUES[col] == Columns.SELECTED;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return (Columns.VALUES[columnIndex] == Columns.SELECTED) ? Boolean.class : String.class;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column)
    {
        if (Columns.VALUES[column] == Columns.SELECTED)
        {
            if (showOnTableVector.size() > row)
            {
                GlobeMO gmo = showOnTableVector.get(row);
                gmo.setSelected((Boolean) aValue);
                fireTableCellUpdated(row, column);
            }
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (showOnTableVector.size() > rowIndex)
        {
            GlobeMO gmo = showOnTableVector.get(rowIndex);
            return switch (Columns.VALUES[columnIndex])
            {
                case SELECTED -> gmo.isSelected();
                case NAME -> gmo.toString();
            };
        }

        return null;
    }

    @Override
    public int getRowCount()
    {
        if (showOnTableVector == null)
        {
            return 0;
        } else
        {
            return showOnTableVector.size();
        }
    }

    @Override
    public int getColumnCount()
    {
        return Columns.VALUES.length;
    }

    @Override
    public String getColumnName(int column)
    {
        return Columns.VALUES[column].toString();
    }

    public ImageIcon getMapObjectIcon(int row)
    {
        if (row < showOnTableVector.size())
        {
            return showOnTableVector.get(row).getIcon();
        }
        return null;
    }

    public List<IMapObject> getSelectedMapObjects()
    {
        List<IMapObject> mapObjects = new ArrayList<>();
        for (GlobeMO gmo : dataVector)
        {
            if (gmo.isSelected())
            {
                mapObjects.add(gmo.getMapObject());
            }
        }
        return mapObjects;
    }

    public void setSelectedMapObjects(Collection<IMapObject> mapObjects)
    {
        Set<IMapObject> mapObjectSet = new HashSet<>(mapObjects);
        for (GlobeMO mo : dataVector)
        {
            boolean selected = mapObjectSet.contains(mo.getMapObject());
            mo.setSelected(selected);
        }
        fireTableDataChanged();
    }

    public void updateVisibleVector(String filterText)
    {
        clearTable();
        if (filterText == null || filterText.isEmpty())
        {
            showOnTableVector.addAll(dataVector);
            showOnTableVector.sort(GlobeMOComparator);
            fireTableDataChanged();
            return;
        }

        Pattern pattern;

        try
        {
            pattern = Pattern.compile(".*" + filterText + ".*", Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e)
        {
            logger.debug("Exception parsing regex: {}", filterText);
            return;
        }

        for (GlobeMO gmo : dataVector)
        {
            if (pattern.matcher(gmo.name).matches())
            {
                showOnTableVector.add(gmo);
            }
        }
        showOnTableVector.sort(GlobeMOComparator);
        fireTableDataChanged();
    }

    private void clearTable()
    {
        showOnTableVector.clear();
        fireTableDataChanged();
    }

    /**
     * Holder class to track a MO, its name, scaled icon, and selected status.
     */
    private static final class GlobeMO
    {
        private boolean selected = false;
        private final IMapObject mapObject;
        private final String name;
        private final ImageIcon icon;

        public GlobeMO(IMapObject mapObject)
        {
            this.mapObject = mapObject;

            name = mapObject.getName();

            icon = new ImageIcon(ImageUtils.getScaledInstance(
                    getMapObject().getIconImage(),
                    16,
                    16,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                    true));
        }

        public String toString()
        {
            return name;
        }

        public boolean isSelected()
        {
            return selected;
        }

        public void setSelected(boolean selected)
        {
            this.selected = selected;
        }

        public IMapObject getMapObject()
        {
            return mapObject;
        }

        public ImageIcon getIcon()
        {
            return icon;
        }
    }

    /**
     * Comparator to order map objects by name.
     */
    private static final Comparator<GlobeMO> GlobeMOComparator = Comparator.comparing(o -> o.name);
}
