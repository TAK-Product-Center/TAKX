package gov.takx.core.view.gui.maps.exporters.dataset;

import com.google.common.collect.ImmutableList;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IWorldWindowGL;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelListener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.List;

/**
 * A panel that contains imagery and elevation tables.
 */
@PseudoBean
public class LayerManagementPanel extends JPanel
{
    @Inject
    private IGlobeManager globeManager;

    private final LayerTableController imageryTableController;
    private final LayerTableController elevationTableController;
    private final MouseListener mouseListener;

    private RenderableLayer layer;
    private SurfaceCrossHairs crossHairs;
    private Sector sector;

    public LayerManagementPanel()
    {
        mouseListener = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e == null || e.isConsumed()) return;

                final var position = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getCurrentPosition();
                if (position != null) updatePreviews(position);
            }
        };

        initComponents();

        imageryTableController = new LayerTableController(imageryLayerTable.getModel(), this::refresh);
        elevationTableController = new LayerTableController(elevationModelTable.getModel(), this::refresh);
    }

    @PostConstruct
    private void postConstruct()
    {
        IWorldWindowGL wwd = globeManager.getPrimaryGlobeDelegate().getWorldWindow();
        wwd.getInputHandler().addMouseListener(mouseListener);

        // Create a layer to display a rectangle around the preview area
        layer = new RenderableLayer();
        wwd.getModel().getLayers().add(layer);

        crossHairs = new SurfaceCrossHairs();
        layer.addRenderable(crossHairs);
    }

    public void setManageMapSetSize(boolean manage)
    {
        imageryTableController.getRows().forEach(wrapper -> wrapper.setSizeManaged(manage));
        elevationTableController.getRows().forEach(wrapper -> wrapper.setSizeManaged(manage));

        //Force invalidate the LayerManagementList to prevent an issue with the elevationModelList in particular not invalidating
        //and thus not being redrawn correctly when the only change made is the checkbox
        imageryLayerTable.invalidate();
        elevationModelTable.invalidate();
    }

    /**
     * Adds all the imagery and elevation layers to the corresponding tables.
     *
     * @param imagery The imagery layer wrappers
     * @param elevation The elevation model wrappers
     */
    public void addLayers(@Nonnull List<MapDataWrapper> imagery, @Nonnull List<MapDataWrapper> elevation)
    {
        imageryTableController.addCachedWrappers(imagery);
        elevationTableController.addCachedWrappers(elevation);
    }

    public void addTableModelsListener(TableModelListener listener)
    {
        imageryLayerTable.getModel().addTableModelListener(listener);
        elevationModelTable.getModel().addTableModelListener(listener);
    }

    public void updateSizes(Boundary boundary)
    {
        imageryTableController.updateEstimatedSizes(boundary);
        elevationTableController.updateEstimatedSizes(boundary);
    }

    /**
     * Update layer previews to a new location.
     *
     * @param position Location to preview. May not be {@code null}.
     */
    public void updatePreviews(@Nonnull LatLon position)
    {
        // Only update the preview location if it's within the selected sector
        if (sector == null || !sector.contains(position)) return;

        crossHairs.setLocation(position);
        imageryTableController.updatePreviews(position);
        elevationTableController.updatePreviews(position);
    }

    /**
     * Update the sector selected for the new map set. Updating the sector resets the preview location to the sector
     * centroid.
     *
     * @param sector Map set sector.
     */
    public void updateSector(Sector sector)
    {
        this.sector = sector;

        if (sector == null || Sector.EMPTY_SECTOR.equals(sector))
        {
            // Disable the crosshairs layer if no sector is selected.
            if (layer != null) layer.setEnabled(false);
            return;
        }

        layer.setEnabled(true);

        // Set the crosshairs to a size that will be easy to see relative to the sector.
        // Take the absolute value of the delta because a sector flipped over an axis can
        // result in a negative delta.
        double minDimension = Math.min(Math.abs(sector.getDeltaLatDegrees()), Math.abs(sector.getDeltaLonDegrees()));
        crossHairs.setWidth(Angle.fromDegrees(minDimension / 4.0));

        // Reset the preview location to the sector centroid.
        updatePreviews(sector.getCentroid());
    }

    public Collection<MapDataWrapper> getAllSelectedLayers()
    {
        return ImmutableList.<MapDataWrapper>builder()
                .addAll(getSelectedImageryLayers())
                .addAll(getSelectedElevationLayers())
                .build();
    }

    public Collection<MapDataWrapper> getSelectedImageryLayers()
    {
        return getSelectedCachesFromController(imageryTableController);
    }

    public Collection<MapDataWrapper> getSelectedElevationLayers()
    {
        return getSelectedCachesFromController(elevationTableController);
    }

    /**
     * Remove the preview crosshairs layer and mouse listener.
     */
    public void cleanUp()
    {
        IWorldWindowGL wwd = globeManager.getPrimaryGlobeDelegate().getWorldWindow();
        wwd.getInputHandler().removeMouseListener(mouseListener);

        if (layer != null)
        {
            wwd.getModel().getLayers().remove(layer);
            layer = null;
        }
    }

    @InvokeOnEDT
    private void refresh()
    {
        revalidate();
        repaint();
    }

    private Collection<MapDataWrapper> getSelectedCachesFromController(LayerTableController controller)
    {
        return ImmutableList.copyOf(controller.getSelectedRows());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var splitPane1 = new JSplitPane();
        var panelImagery = new JPanel();
        imageryLayerTable = new LayerManagementTable();
        panelElevation = new JPanel();
        elevationModelTable = new LayerManagementTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelLayerManagement");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== splitPane1 ========
        {
            splitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
            splitPane1.setResizeWeight(0.75);
            splitPane1.setName("splitPane1");

            //======== panelImagery ========
            {
                panelImagery.setBorder(new TitledBorder("Imagery"));
                panelImagery.setName("panelImagery");
                panelImagery.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //---- imageryLayerTable ----
                imageryLayerTable.setName("imageryLayerTable");
                panelImagery.add(imageryLayerTable, cc.xy(1, 1));
            }
            splitPane1.setTopComponent(panelImagery);

            //======== panelElevation ========
            {
                panelElevation.setBorder(new TitledBorder("Elevation"));
                panelElevation.setName("panelElevation");
                panelElevation.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //---- elevationModelTable ----
                elevationModelTable.setName("elevationModelTable");
                panelElevation.add(elevationModelTable, cc.xy(1, 1));
            }
            splitPane1.setBottomComponent(panelElevation);
        }
        add(splitPane1, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private LayerManagementTable imageryLayerTable;
    private JPanel panelElevation;
    private LayerManagementTable elevationModelTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
