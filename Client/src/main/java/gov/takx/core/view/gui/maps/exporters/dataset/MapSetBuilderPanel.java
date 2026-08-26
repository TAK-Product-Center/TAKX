package gov.takx.core.view.gui.maps.exporters.dataset;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.geom.Sector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The primary MapSet Builder panel.
 *
 * @since 1.43
 */
public class MapSetBuilderPanel extends JPanel implements TableModelListener, IBoundaryChangeListener
{
    private IBoundaryDrawingListener boundaryDrawingListener;

    public MapSetBuilderPanel()
    {
        initComponents();

        // Initialize the label.
        updateApproxSizeLabel(0);

        boundarySelectionPanel.setBoundarySelectorListener(this);
        layerManagementPanel.addTableModelsListener(this);

        addLayers();
    }

    private void addLayers()
    {
        final List<MapDataWrapper> imagery = MapBuilderUtils.getImageryWrappers();
        final List<MapDataWrapper> elevation = MapBuilderUtils.getElevationWrappers();

        layerManagementPanel.addLayers(imagery, elevation);
    }

    public void setBoundaryDrawingListener(IBoundaryDrawingListener listener)
    {
        boundaryDrawingListener = listener;
        boundarySelectionPanel.setBoundaryDrawingListener(listener);
    }

    public Optional<Sector> getSector()
    {
        return boundarySelectionPanel.getSector();
    }

    public Boundary getBoundary()
    {
        return boundarySelectionPanel.getBoundary();
    }

    @Override
    public void onBoundaryChanged(Boundary boundary)
    {
        if (boundary != null && !boundary.getVertices().isEmpty())
        {
            layerManagementPanel.updateSizes(boundary);
            layerManagementPanel.updateSector(boundary.getSector());
            updateLabels();
        }

        if (boundaryDrawingListener != null) boundaryDrawingListener.onDrawingBoundary(false);
    }

    public boolean isSizeManaged()
    {
        return managedCheckBox.isSelected();
    }

    public Collection<MapDataWrapper> getSelectedSources()
    {
        return layerManagementPanel.getAllSelectedLayers();
    }

    public boolean isAutoConnect()
    {
        return autoConnectCheckbox.isSelected();
    }

    private void updateLabels()
    {
        updateApproxSizeLabel(computeApproxSizeInBytes(getSelectedSizes()));
    }

    public Collection<Long> getSelectedSizes()
    {
        return layerManagementPanel.getAllSelectedLayers().stream()
                .filter(input -> null != input.getTotalAcceptableTileCount())
                .map(input -> input.getTotalAcceptableTileCount() * input.getTileDataSize())
                .collect(Collectors.toList());
    }

    private void updateApproxSizeLabel(long size)
    {
        sizeValueLabel.setText(MapBuilderUtils.getByteString(size));
    }

    public long computeApproxSizeInBytes(Collection<Long> selectedLayerSizes)
    {
        long value = 0;

        for (Long size : selectedLayerSizes)
        {
            // Check the estimated size against null, and if non-null add.
            value += null != size ? size : 0;
        }

        return value;
    }

    @Override
    public void tableChanged(TableModelEvent e)
    {
        updateLabels();
    }

    public void cleanUp()
    {
        boundarySelectionPanel.cleanUp();
        layerManagementPanel.cleanUp();
    }

    private void managedCheckBoxActionPerformed()
    {
        layerManagementPanel.setManageMapSetSize(managedCheckBox.isSelected());
        updateLabels();
        validate();
        repaint();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        boundarySelectionPanel = new MultishapeBoundarySelectionPanel();
        layerManagementPanel = new LayerManagementPanel();
        var panel1 = new JPanel();
        var sizeLabel = new JLabel();
        sizeValueLabel = new JLabel();
        managedCheckBox = new JCheckBox();
        autoConnectCheckbox = new JCheckBox();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelMapSetBuilder");
        setBorder(Borders.DLU2_BORDER);
        setLayout(new FormLayout(
                "default:grow",
                "default, $nlgap, fill:default:grow, $nlgap, default, $pgap, default"));

        //---- boundarySelectionPanel ----
        boundarySelectionPanel.setName("boundarySelectionPanel");
        add(boundarySelectionPanel, cc.xy(1, 1));

        //---- layerManagementPanel ----
        layerManagementPanel.setName("layerManagementPanel");
        add(layerManagementPanel, cc.xy(1, 3));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, $ugap, right:default:grow",
                    "$nlgap, default, $nlgap"));

            //---- sizeLabel ----
            sizeLabel.setText("Approximate Size:");
            sizeLabel.putClientProperty("FlatLaf.styleClass", "large");
            sizeLabel.setName("sizeLabel");
            panel1.add(sizeLabel, cc.xy(1, 2));

            //---- sizeValueLabel ----
            sizeValueLabel.setText("0.00");
            sizeValueLabel.putClientProperty("FlatLaf.styleClass", "large");
            sizeValueLabel.setName("sizeValueLabel");
            panel1.add(sizeValueLabel, cc.xy(3, 2));

            //---- managedCheckBox ----
            managedCheckBox.setText("Manage Map Set Size");
            managedCheckBox.setName("managedCheckBox");
            managedCheckBox.addActionListener(e -> managedCheckBoxActionPerformed());
            panel1.add(managedCheckBox, cc.xy(5, 2, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        }
        add(panel1, cc.xy(1, 5));

        //---- autoConnectCheckbox ----
        autoConnectCheckbox.setText("Connect To Map Set When Completed");
        autoConnectCheckbox.setName("autoConnectCheckbox");
        add(autoConnectCheckbox, cc.xy(1, 7, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private MultishapeBoundarySelectionPanel boundarySelectionPanel;
    private LayerManagementPanel layerManagementPanel;
    private JLabel sizeValueLabel;
    private JCheckBox managedCheckBox;
    private JCheckBox autoConnectCheckbox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
