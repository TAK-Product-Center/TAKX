package gov.takx.core.view.gui.maptools.elevationtools.viewshed;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.elevation.ElevationManager;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.common.CommonEntityTable;
import gov.takx.core.globe.GlobeClickService;
import gov.takx.core.globe.ViewshedManager;
import gov.takx.core.globe.tak.layer.ViewshedLayer;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.table.TableColumnExt;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * An elevation tool panel specific to displaying and editing {@link ViewshedLayer Viewsheds}
 * @since 5.7
 */
@PseudoBean
public class ViewshedPanel extends JPanel implements ViewshedManager.ViewshedListener, IResourceCleaner
{
    private static final int ROW_HEIGHT = UIResources.ACTION_BUTTON_IMAGE_SIZE.height + 15;

    private final ViewshedEntity entityDefinition = new ViewshedEntity();
    private final EntityTableModel<ViewshedLayer> tableModel = new EntityTableModel<>(entityDefinition);

    @Inject
    private GlobeClickService globeClickService;

    @Inject
    private ViewshedManager viewshedManager;

    @Inject
    private INotificationService notificationService;

    public ViewshedPanel()
    {
        initComponents();

        viewshedTable.setModel(tableModel);
        viewshedTable.setRowSelectionAllowed(true);
        viewshedTable.getTableHeader().setSortButtonVisible(false);

        TableColumnExt actionsColumn = viewshedTable.getActionsColumn();
        actionsColumn.setSortable(false);
        actionsColumn.setVisible(true);
        TableColumnExt nameColumn = viewshedTable.getNameColumn();
        nameColumn.setSortable(false);
        nameColumn.setVisible(true);

        final var newViewshedButton = new JButton("Add", UIResources.ADD_ICON);
        newViewshedButton.setToolTipText("Create a new viewshed");
        newViewshedButton.addActionListener(e -> newViewshed());
        viewshedTable.getTableHeader().addActionButton(newViewshedButton);

        viewshedTable.setRowHeight(ROW_HEIGHT);
        scrollPane1.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT);

        viewshedTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int selectedRow = viewshedTable.getSelectedRow();
            ViewshedLayer layer = selectedRow > -1 ? tableModel.getRow(viewshedTable.convertRowIndexToModel(selectedRow)) : null;
            viewshedEditorPanel.setViewshed(layer);
        });
    }

    @PostConstruct
    private void postConstruct()
    {
        viewshedManager.addViewshedListener(this);
    }

    private void newViewshed()
    {
        globeClickService.activate("Click to place a viewshed", location -> {
            globeClickService.deactivate();
            // TODO: TAKX-4380, determine how to download the elevation data here
            double elevation = ElevationManager.getElevation(new GeoPoint(location), null);
            if (Double.isNaN(elevation))
            {
                notificationService.show(Notification.builder("Cannot create a Viewshed, invalid/missing elevation data at selected location. Zoom into or import elevation data for the desired location.", Notification.Type.Error).build());
            } else
            {
                final var viewshed = viewshedManager.createViewshed(false);
                viewshed.setMarkerLocation(location);
                viewshed.setVisible(true);
            }
        });
    }

    @Override
    @InvokeOnEDT
    public void viewshedCreated(@Nonnull ViewshedLayer layer)
    {
        if (layer.isForRoute()) return;

        // Add the new layer and select it
        tableModel.addEntity(layer);
        final int index = tableModel.indexOf(layer);
        viewshedTable.setRowSelectionInterval(index, index);
    }

    @Override
    @InvokeOnEDT
    public void viewshedDeleted(@Nonnull ViewshedLayer layer)
    {
        tableModel.removeEntity(layer);
    }

    @Override
    @InvokeOnEDT
    public void viewshedLocationChanged(@Nonnull ViewshedLayer layer)
    {
        viewshedEditorPanel.updatePanel();
    }

    @Override
    public void releaseResources()
    {
        viewshedManager.removeViewshedListener(this);
        viewshedEditorPanel.releaseResources();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        scrollPane1 = new JScrollPane();
        viewshedTable = new CommonEntityTable<>();
        viewshedEditorPanel = new ViewshedEditorPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setPreferredSize(null);
        setMinimumSize(null);
        setName("this");
        setLayout(new FormLayout(
                "[100dlu,default]:grow, $lcgap, left:default",
                "fill:default:grow"));

        //======== scrollPane1 ========
        {
            scrollPane1.setPreferredSize(null);
            scrollPane1.setName("scrollPane1");

            //---- viewshedTable ----
            viewshedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            viewshedTable.setPreferredScrollableViewportSize(null);
            viewshedTable.setPreferredSize(null);
            viewshedTable.setName("viewshedTable");
            scrollPane1.setViewportView(viewshedTable);
        }
        add(scrollPane1, cc.xy(1, 1));

        //---- viewshedEditorPanel ----
        viewshedEditorPanel.setPreferredSize(null);
        viewshedEditorPanel.setName("viewshedEditorPanel");
        add(viewshedEditorPanel, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JScrollPane scrollPane1;
    private CommonEntityTable<ViewshedLayer> viewshedTable;
    private ViewshedEditorPanel viewshedEditorPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
