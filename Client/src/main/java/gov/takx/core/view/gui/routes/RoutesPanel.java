package gov.takx.core.view.gui.routes;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.common.CommonEntityTable;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;

import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.ADD_ACTION_BUTTON_ICON;

/**
 * Panel listing the routes currently present in the system, as well as providing UI options to create/edit/delete Routes.
 *
 * @since 3.3 (significant redesign in 5.7 to use common entity table UI/UX)
 */
@PseudoBean
public class RoutesPanel extends ATitledPanel
{
    public static final String TITLE = ButtonType.ROUTES.getDisplayText();

    private boolean editInProgress;

    @Inject
    private RoutesManager routesManager;

    @Inject
    private IExtendedUISpaceManager uiSpaceManager;

    private final RouteEntityDefinition entityDefinition = new RouteEntityDefinition(this::isEditInProgress);
    private final EntityTableModel<IRouteMapObject> tableModel = new EntityTableModel<>(entityDefinition);

    /**
     * Create a panel to display the routes we have in the system.
     */
    public RoutesPanel()
    {
        super(TITLE);
        initComponents();

        addButton.setIcon(ADD_ACTION_BUTTON_ICON);

        table.setModel(tableModel);
        table.getNameColumn().setVisible(true);
        table.getActionsColumn().setVisible(true);
        table.getVisibilityColumn().setVisible(true);
        table.setRowSelectionAllowed(false);
        table.setRowHeight(ACTION_BUTTON_IMAGE_SIZE.height + 15);

        table.getTableHeader().addActionButton(addButton);
        table.getTableHeader().addActionButton(importButton);

        FormLayoutUtils.hideRow(inProgressPanel);
        FormLayoutUtils.hideRow(routePropertiesPanel);
    }

    /**
     * Handle a new map object, add it to our panel if it is a route.
     */
    @InvokeOnEDT
    private void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        if (event.getMapObject() instanceof IRouteMapObject routeMapObject)
        {
            tableModel.addEntity(routeMapObject);
            routeMapObject.addDeletionHandler(mi -> onRouteDeleted(routeMapObject));
            GUIUtils.setColumnWidthsFromContents(table, table.getActionsColumn());
        }
    }

    /**
     * Handle a route map object being deleted.
     */
    @InvokeOnEDT
    private void onRouteDeleted(IRouteMapObject routeMapObject)
    {
        tableModel.removeEntity(routeMapObject);
    }

    private void addRouteButtonPressed()
    {
        editInProgress = true;
        addButton.setEnabled(false);
        topPanel.setVisible(true);
        FormLayoutUtils.restoreRow(routePropertiesPanel);
    }

    /**
     * Disables route creation/editing, and adds UI for finishing/cancelling when route drawing is in progress.
     */
    @InvokeOnEDT
    protected void startRouteDrawing()
    {
        editInProgress = true;
        addButton.setEnabled(false);
        topPanel.setVisible(true);
        FormLayoutUtils.hideRow(routePropertiesPanel);
        FormLayoutUtils.restoreRow(inProgressPanel);
        uiSpaceManager.showComponent(CoreComponentType.ROUTES_PANEL);
    }

    /**
     * Method to be called when we are told by core that route drawing has finished.
     */
    @InvokeOnEDT
    protected void finishRouteDrawing()
    {
        addButton.setEnabled(true);
        FormLayoutUtils.hideRow(routePropertiesPanel);
        FormLayoutUtils.hideRow(inProgressPanel);
        topPanel.setVisible(false);
        editInProgress = false;
    }

    private boolean isEditInProgress()
    {
        return editInProgress;
    }

    private void importButtonActionPressed()
    {
        // TODO TAKX-1259 add in import of routes from .kml, .kmz, .gpz
    }

    private void routePropertiesPanelFinished()
    {
        var route = routePropertiesPanel.getRoute();
        if (route != null)
        {
            startRouteDrawing();
            routesManager.startNewRouteEditing(route);
        } else
        {
            finishRouteDrawing();
        }
    }

    private void finishButtonActionPerformed()
    {
        routesManager.finishRouteEditing();
    }

    private void cancelButtonActionPerformed()
    {
        routesManager.cancelRouteEditing();
    }

    @InvokeOnEDT
    @Override
    public void releaseResources()
    {
        tableModel.clear();
    }

    @SuppressWarnings("RedundantExplicitVariableType")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        topPanel = new JPanel();
        inProgressPanel = new JPanel();
        var label1 = new JLabel();
        var label2 = new JLabel();
        var label3 = new JLabel();
        var label4 = new JLabel();
        var label5 = new JLabel();
        var inProgressButtonPanel = new JPanel();
        var finishButton = new JButton();
        var cancelButton = new JButton();
        routePropertiesPanel = new RoutePropertiesPanel();
        var scrollPane = new JScrollPane();
        table = new CommonEntityTable<>();
        importButton = new JButton();
        addButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setPreferredSize(new Dimension(300, 400));
        setLayout(new BorderLayout());

        //======== topPanel ========
        {
            topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            topPanel.setMaximumSize(new Dimension(2147483647, 75));
            topPanel.setVisible(false);
            topPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:min, 2*($lgap, default)"));

            //======== inProgressPanel ========
            {
                inProgressPanel.setLayout(new FormLayout(
                        "center:default:grow",
                        "5*(default, $lgap), default"));

                //---- label1 ----
                label1.setText("Route drawing in progress.");
                inProgressPanel.add(label1, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                //---- label2 ----
                label2.setText("Click the map to add a point.");
                inProgressPanel.add(label2, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));

                //---- label3 ----
                label3.setText("Double-click to add a check point.");
                inProgressPanel.add(label3, cc.xy(1, 5, CellConstraints.CENTER, CellConstraints.DEFAULT));

                //---- label4 ----
                label4.setText("Ctrl+click a point to remove it.");
                inProgressPanel.add(label4, cc.xy(1, 7, CellConstraints.CENTER, CellConstraints.DEFAULT));

                //---- label5 ----
                label5.setText("Shift + click to promote an existing point.");
                inProgressPanel.add(label5, cc.xy(1, 9, CellConstraints.CENTER, CellConstraints.DEFAULT));

                //======== inProgressButtonPanel ========
                {
                    inProgressButtonPanel.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default"));

                    //---- finishButton ----
                    finishButton.setText("Finish");
                    finishButton.addActionListener(e -> finishButtonActionPerformed());
                    inProgressButtonPanel.add(finishButton, cc.xy(1, 1));

                    //---- cancelButton ----
                    cancelButton.setText("Cancel");
                    cancelButton.setToolTipText("Cancel route drawing.");
                    cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                    inProgressButtonPanel.add(cancelButton, cc.xy(3, 1));
                }
                inProgressPanel.add(inProgressButtonPanel, cc.xy(1, 11));
            }
            topPanel.add(inProgressPanel, cc.xy(1, 3));

            //---- routePropertiesPanel ----
            routePropertiesPanel.setOkButtonText("Start");
            routePropertiesPanel.addFinishListener(e -> routePropertiesPanelFinished());
            topPanel.add(routePropertiesPanel, cc.xy(1, 5));
        }
        add(topPanel, BorderLayout.NORTH);

        //======== scrollPane ========
        {
            scrollPane.setViewportView(table);
        }
        add(scrollPane, BorderLayout.CENTER);

        //---- importButton ----
        importButton.setText("Import");
        importButton.setVisible(false);
        importButton.addActionListener(e -> importButtonActionPressed());

        //---- addButton ----
        addButton.setText("Add");
        addButton.addActionListener(e -> addRouteButtonPressed());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel topPanel;
    private JPanel inProgressPanel;
    private RoutePropertiesPanel routePropertiesPanel;
    private CommonEntityTable<IRouteMapObject> table;
    private JButton importButton;
    private JButton addButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
