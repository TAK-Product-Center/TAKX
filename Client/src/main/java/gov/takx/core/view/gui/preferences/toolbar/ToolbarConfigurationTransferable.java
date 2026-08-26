package gov.takx.core.view.gui.preferences.toolbar;

import com.google.common.collect.Maps;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarTab;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Map;
import java.util.Set;

/**
 * Transferable for drag and drop of toolbar components.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarConfigurationTransferable implements Transferable
{
    public static final DataFlavor COMPONENT_DATA_FLAVOR = new DataFlavor(AToolbarComponent.class, "AToolbarComponent");
    public static final DataFlavor TAB_DATA_FLAVOR = new DataFlavor(ToolbarTab.class, "ToolbarTab");

    private final Map<DataFlavor, Object> objects = Maps.newHashMap();

    public ToolbarConfigurationTransferable(DataFlavor dataFlavor, Object data)
    {
        objects.put(dataFlavor, data);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        Set<DataFlavor> dataFlavors = objects.keySet();
        return dataFlavors.toArray(new DataFlavor[0]);
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return objects.containsKey(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
    {
        return objects.get(flavor);
    }
}
