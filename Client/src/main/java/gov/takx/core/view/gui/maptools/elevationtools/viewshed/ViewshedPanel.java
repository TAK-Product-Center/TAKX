package gov.takx.core.view.gui.maptools.elevationtools.viewshed;

import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.elevation.ElevationManager;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.terrain.experimental.ElevationSourceUtils;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.common.CommonEntityTable;
import gov.takx.core.globe.GlobeClickService;
import gov.takx.core.globe.ViewshedManager;
import gov.takx.core.globe.tak.elevation.ElevationDownloaderManager;
import gov.takx.core.globe.tak.layer.ViewshedLayer;
import gov.takx.core.view.gui.ProgressDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An elevation tool panel specific to displaying and editing {@link ViewshedLayer Viewsheds}
 * @since 5.7
 */
@PseudoBean
public class ViewshedPanel extends JPanel implements ViewshedManager.ViewshedListener, IResourceCleaner
{
    private final ViewshedEntity entityDefinition = new ViewshedEntity();
    private final EntityTableModel<ViewshedLayer> tableModel = new EntityTableModel<>(entityDefinition);

    @Inject
    private GlobeClickService globeClickService;

    @Inject
    private ViewshedManager viewshedManager;

    @Inject
    private INotificationService notificationService;

    @Inject
    private ElevationDownloaderManager elevationDownloaderManager;

    @Inject
    private ICoreExecutorService executorService;

    public ViewshedPanel()
    {
        initComponents();

        viewshedTable.setModel(tableModel);
        viewshedTable.setRowSelectionAllowed(true);
        viewshedTable.getTableHeader().setSortButtonVisible(false);
        viewshedTable.getActionsColumn().setVisible(true);
        viewshedTable.getNameColumn().setVisible(true);

        final var newViewshedButton = new JButton("Add", UIResources.ADD_ICON);
        newViewshedButton.setToolTipText("Create a new viewshed");
        newViewshedButton.addActionListener(e -> newViewshed());
        viewshedTable.getTableHeader().addActionButton(newViewshedButton);

        viewshedTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            viewshedEditorPanel.setViewshed(viewshedTable.getSelectedEntity());
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
            ViewshedLayer viewshedLayer = viewshedTable.getSelectedEntity();
            double radius = viewshedLayer != null ? viewshedLayer.getRadius() : ViewshedLayer.DEFAULT_VIEWSHED_RADIUS;
            if (viewshedHasMissingElevationData(location, radius))
            {
                downloadElevationData(location, radius);
                double elevation = ElevationManager.getElevation(new GeoPoint(location), null);
                location = new gov.tak.api.maps.coords.GeoPoint(location.getLatitude(), location.getLongitude(), elevation);
                if (viewshedHasMissingElevationData(location, radius))
                {
                    notificationService.show(Notification.builder("Cannot create a Viewshed, invalid/missing elevation data at selected location. Zoom into or import elevation data for the desired location.", Notification.Type.Error).build());
                    return;
                }
            }

            final var viewshed = viewshedManager.createViewshed(false);
            viewshed.setMarkerLocation(location, false);
            viewshed.setVisible(true);
        });
    }

    private GeoPoint[] getViewshedBoundaryLocations(ILocation location, double radius)
    {
        GeoPoint[] locations = new GeoPoint[4];
        locations[0] = new GeoPoint(GeoCalculations.pointAtDistance(location, 0, radius));
        locations[1] = new GeoPoint(GeoCalculations.pointAtDistance(location, 90, radius));
        locations[2] = new GeoPoint(GeoCalculations.pointAtDistance(location, 180, radius));
        locations[3] = new GeoPoint(GeoCalculations.pointAtDistance(location, 270, radius));
        return locations;
    }

    private boolean viewshedHasMissingElevationData(ILocation location, double radius)
    {
        return !location.hasElevation() ||
                Arrays.stream(getViewshedBoundaryLocations(location, radius)).anyMatch(l -> Double.isNaN(ElevationManager.getElevation(new GeoPoint(l), null)));
    }

    private void downloadElevationData(ILocation location, double radius)
    {
        AtomicBoolean cancelSignal = new AtomicBoolean(false);
        ProgressDialog progressDialog = new ProgressDialog(null);
        progressDialog.setModal(true);
        progressDialog.setAlwaysOnTop(true);
        progressDialog.setText("Please wait...");
        progressDialog.setSubstatus("Downloading elevation data");
        progressDialog.setMaximum(IProgressMonitor.INDETERMINATE);
        progressDialog.showCancelButton(() -> cancelSignal.set(true));

        GeoPoint[] locations = getViewshedBoundaryLocations(location, radius);
        ElevationSourceUtils.fillDtedCache(elevationDownloaderManager.getDtedDir(), new GeoBounds[]{GeoBounds.createFromPoints(locations)}, () -> {
            progressDialog.setVisible(false);
        }, executorService, cancelSignal);

        progressDialog.setVisible(true);
    }

    @Override
    @InvokeOnEDT
    public void viewshedCreated(@Nonnull ViewshedLayer layer)
    {
        if (layer.isForRoute()) return;

        // Add the new layer and select it
        tableModel.addEntity(layer);
        viewshedTable.setSelectedEntity(layer);
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
            viewshedTable.setFocusable(false);
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
