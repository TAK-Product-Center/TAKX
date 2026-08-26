package gov.takx.core.view.gui.explorer.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.explorer.IExplorerDelegateListener;
import gov.takx.commons.swing.IconTableCellRenderer;
import gov.takx.commons.swing.MouseClickedListener;
import gov.takx.commons.swing.PopupTriggerListener;
import gov.takx.commons.swing.SimpleAction;
import gov.takx.commons.swing.TableUtils;
import gov.takx.commons.swing.TreeModelCombinedListener;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.EntityTreeTable;
import gov.takx.commons.swing.entity.SortFilterEntityTreeTableModel;
import gov.takx.commons.swing.entity.common.CommonEntityTable;
import gov.takx.core.explorer.common.CoreExplorerUtils;
import gov.takx.core.explorer.common.DeleteExplorableAction;
import gov.takx.core.explorer.common.IControllerExplorable;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.explorer.model.IExplorerModel;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.treetable.TreeTableModel;

import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static gov.takx.commons.swing.entity.EntityTable.ICON_COLUMN_SIZE;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.ALERTS_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.ANNOTATIONS_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.APPS_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.FILE_OVERLAYS_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.GROUPS_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.HASHTAGS_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.MARKERS_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.MEDIA_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.NAVIGATION_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.SHAPES_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.TEAM_PATH;
import static gov.takx.core.view.gui.explorer.view.VisibilityManager.VISIBILITY_ICON_MAP;

/**
 * The main display panel for the explorer
 * <p>
 * Components may be added to the border of the explorer panel by plugins.
 * @since 1.48
 */
@PseudoBean
public class ExplorerPanel extends JPanel
{
    private static final List<String> ACCEPTABLE_COMPONENT_BORDER_LOCATIONS = Arrays.asList(BorderLayout.NORTH, BorderLayout.SOUTH, BorderLayout.EAST, BorderLayout.WEST);

    private final Map<IExplorable, Boolean> showHideExplorables = new HashMap<>();

    @SuppressWarnings("FieldCanBeLocal")
    private final VisibilityManager visibilityManager = new VisibilityManager();
    private final LazyManager lazyManager = new LazyManager();
    private final EmptyPathManager emptyPathManager = new EmptyPathManager();

    protected final IExplorerController controller;
    protected final IExplorerModel model;
    protected final ExplorableEntityDefinition entityDefinition;
    protected final ExplorerTreeTableModel baseExplorerModel;
    protected final EntityTableModel<IExplorable> tableModel;
    protected final SortFilterEntityTreeTableModel<IExplorable> filteredExplorerModel;
    protected final SortFilterEntityTreeTableModel<IExplorable> treeExplorerModel;
    protected final ExplorerSummaryProvider summaryProvider;

    private boolean singlePane; // If true, the explorer is tree only
    private int textScalingPercent = 100;
    protected TreePath treeSelectionPath;
    protected IExplorable treeSelectionExplorable;
    protected Predicate<IExplorable> treeFilter;

    /**
     * Construct a new explorer panel for the given model and controller, specifying various flags to control whether
     * certain default nodes are shown or hidden.
     *
     * @param model the explorer model
     * @param controller the explorer controller
     * @param rootVisible {@code true} if the explorer should display the root node, or {@code false} to hide it
     * @param showCoreFamilies {@code true} if the explorer should display core family nodes or {@code false} to hide them
     * @param showGroups {@code true} if the explorer should display the groups node or {@code false} to hide it
     * @param showAlerts {@code true} if the explorer should display the alerts node or {@code false} to hide it
     * @param showTeam {@code true} if the explorer should display the team node or {@code false} to hide it
     * @since 5.4 (Added boolean flags in 5.7)
     */
    public ExplorerPanel(@Nonnull IExplorerModel model, @Nonnull IExplorerController controller, boolean rootVisible, boolean showCoreFamilies, boolean showGroups, boolean showAlerts, boolean showTeam)
    {
        this.model = model;
        this.controller = controller;

        initComponents();
        initListeners();

        explorerTree.setRootVisible(rootVisible);
        List.of(MEDIA_PATH, FILE_OVERLAYS_PATH, MARKERS_PATH, SHAPES_PATH, NAVIGATION_PATH, ANNOTATIONS_PATH).forEach(path -> showOrHideExplorable(model.getRoot().getOrCreatePath(path), showCoreFamilies));
        showOrHideExplorable(model.getRoot().getOrCreatePath(GROUPS_PATH), showGroups);
        showOrHideExplorable(model.getRoot().getOrCreatePath(ALERTS_PATH), showAlerts);
        showOrHideExplorable(model.getRoot().getOrCreatePath(TEAM_PATH), showTeam);

        // Hide the Apps node
        showOrHideExplorable(model.getRoot().getOrCreatePath(APPS_PATH), false);

        // Configure the entity definition and the base swing models
        entityDefinition = new ExplorableEntityDefinition(controller, visibilityManager);
        baseExplorerModel = new ExplorerTreeTableModel(model.getRoot(), entityDefinition, visibilityManager);
        emptyPathManager.init(baseExplorerModel);
        lazyManager.init(baseExplorerModel);

        var visRollupModelColumn = entityDefinition.getVisibilityRollUpColumn();

        // Configure the table
        tableModel = new EntityTableModel<>(entityDefinition);
        explorerTable.setModel(tableModel);
        // Allow the default sort option to accommodate explorables that need insertion order sort e.g. geofence alerts
        explorerTable.getTableHeader().getSortMenu().setDefaultSortVisible(true);
        explorerTable.setAutoHideColumns(true,
                tableModel.getColumnIndex(entityDefinition.getActionsColumn()),
                tableModel.getColumnIndex(entityDefinition.getAlarmColumn()),
                tableModel.getColumnIndex(visRollupModelColumn));

        explorerTable.requireColumn(visRollupModelColumn).setCellRenderer(new IconTableCellRenderer<>(VISIBILITY_ICON_MAP));
        explorerTable.requireColumn(entityDefinition.getAlarmColumn()).setVisible(true);
        explorerTable.requireColumn(entityDefinition.getHierarchicalColumn()).setVisible(false);
        explorerTable.getSummaryColumn().setVisible(true);
        explorerTable.getIconColumn().setVisible(true);
        explorerTable.getVisibilityColumn().setVisible(false);

        explorerTable.requireColumn(entityDefinition.getAlarmColumn()).setCellRenderer(new IconTableCellRenderer<>(CoreExplorerUtils::getIconForState));
        GUIUtils.setColumnWidths(ICON_COLUMN_SIZE, explorerTable.getColumn(entityDefinition.getAlarmColumn()));
        GUIUtils.setColumnWidths(ICON_COLUMN_SIZE, explorerTable.getColumn(visRollupModelColumn));

        // This model applies shared filters that are needed in both the table and tree
        filteredExplorerModel = new SortFilterEntityTreeTableModel<>(baseExplorerModel);
        // Comparator for Path nodes that sorts by display name by default
        filteredExplorerModel.addComparator(o -> o instanceof IPathExplorableGroup, Comparator.comparing(IExplorable::getDisplayName, String::compareToIgnoreCase));
        // Comparator that forces the order of some nodes at the top
        filteredExplorerModel.addComparator(baseExplorerModel.getRoot(), new RootComparator<>(List.of(
                model.getRoot().getOrCreatePath(ALERTS_PATH),
                model.getRoot().getOrCreatePath(GROUPS_PATH),
                model.getRoot().getOrCreatePath(HASHTAGS_PATH),
                model.getRoot().getOrCreatePath(TEAM_PATH))));

        filteredExplorerModel.addTreeModelListener((TreeModelCombinedListener) this::explorerTreeModelEvent);
        filteredExplorerModel.addFilter(this::applySharedFilters);
        visibilityManager.init(entityDefinition, this, filteredExplorerModel, tableModel);

        summaryProvider = new ExplorerSummaryProvider(lazyManager, filteredExplorerModel, entityDefinition);
        entityDefinition.setSummaryProvider(summaryProvider);
        setTextScalingPercent(PreferenceEntry.EXPLORER_TEXT_SIZE_SCALING_PERCENT.getIntegerValue());

        // This model applies our tree-only filters - e.g. hiding the lowest level explorables like map objects
        treeExplorerModel = new SortFilterEntityTreeTableModel<>(filteredExplorerModel);
        treeFilter = this::applyTreeFilters;
        treeExplorerModel.addFilter(treeFilter);
        explorerTree.setModels(treeExplorerModel, baseExplorerModel);

        // Configure the vis rollup column in the tree
        var visRollupTableColumn = explorerTree.getColumn(visRollupModelColumn);
        visRollupTableColumn.setVisible(true);
        visRollupTableColumn.setCellRenderer(new IconTableCellRenderer<>(VISIBILITY_ICON_MAP));
        GUIUtils.setColumnWidths(ICON_COLUMN_SIZE, visRollupTableColumn);

        explorerTree.setTableHeader(null);
        explorerTree.expandPath(new TreePath(filteredExplorerModel.getRoot()));

        // TODO DCW TAKX-3257 handle multi-select
        explorerTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        explorerTree.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    }

    /**
     * @param textScalingPercent the percent to scale the explorer text and row height
     * @since 5.7
     */
    public void setTextScalingPercent(int textScalingPercent)
    {
        if (this.textScalingPercent != textScalingPercent)
        {
            this.textScalingPercent = textScalingPercent;
            var fontSize = explorerTree.getFontMetrics(explorerTree.getFont()).getHeight();
            var padding = singlePane ? 4 : 16;
            explorerTree.setRowHeight((fontSize * textScalingPercent / 100) + padding);
            summaryProvider.setTextScalingPercent(textScalingPercent);
        }
    }

    /**
     * @return the underlying model used for the explorer panel
     * @since 5.7
     */
    @Nonnull
    public SortFilterEntityTreeTableModel<IExplorable> getFilteredExplorerModel()
    {
        return filteredExplorerModel;
    }

    /**
     * @param singlePane {@code true} if the explorer panel should be in single-pane mode, or {@code false} for two-pane mode
     * @since 5.3
     */
    public void setSinglePane(boolean singlePane)
    {
        if (this.singlePane != singlePane)
        {
            this.singlePane = singlePane;
            if (singlePane)
            {
                explorerTree.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                summaryProvider.setEmbedIcon(true);
                treeExplorerModel.removeFilter(treeFilter);
                treeExplorerModel.getEntityColumnData().clearValues();
            }
            splitPane.setRightComponent(singlePane ? null : explorerTable);
            splitPane.setDividerSize(singlePane ? 0 : 5); // TODO DCW TAKX-3282 Ideally we should be able to restore previous value, but might require extending JSplitPane
        }
    }

    /**
     * @return {@code true} if this panel is in single-pane mode, otherwise {@code false}
     */
    public boolean isSinglePane()
    {
        return singlePane;
    }

    /**
     * Add a component to be drawn in a border location around the explorer tree. The previous such component, if any,
     * will be removed.
     *
     * @param component the component to be added. May be {@code null}, in which case no component will be drawn in the
     *                  given location.
     * @param borderLayoutLocation the location in the border where the component is to be drawn. Must be one of
     *                             {@code BorderLayout.NORTH}, {@code BorderLayout.SOUTH}, {@code BorderLayout.EAST},
     *                             or {@code BorderLayout.WEST}.
     * @throws IllegalArgumentException if {@code borderLayoutComponent} is not one of the listed values
     * @since 2.39
     */
    public void addComponentToBorder(@Nullable Component component, @Nonnull String borderLayoutLocation)
    {
        if (!ACCEPTABLE_COMPONENT_BORDER_LOCATIONS.contains(borderLayoutLocation))
        {
            throw new IllegalArgumentException("Illegal border layout location " + borderLayoutLocation);
        }

        BorderLayout borderLayout = (BorderLayout) getLayout();
        Component componentAtLocation = borderLayout.getLayoutComponent(borderLayoutLocation);
        if (componentAtLocation != null) remove(componentAtLocation);
        if (component != null) add(component, borderLayoutLocation);
    }

    private void initListeners()
    {
        explorerTree.addMouseListener((PopupTriggerListener) this::explorerTreePopupTrigger);
        explorerTree.addTreeSelectionListener(this::explorerTreeSelectionChanged);
        explorerTree.addMouseListener((MouseClickedListener) this::explorerTreeMouseClicked);

        explorerTable.addMouseListener((MouseClickedListener) this::explorerTableMouseClicked);
        explorerTable.addMouseListener((PopupTriggerListener) this::explorerTablePopupTrigger);
        explorerTable.getSelectionModel().addListSelectionListener(this::explorerTableSelectionChanged);
    }

    /**
     * Set a comparator for a node's children.
     *
     * @param explorable the explorable node
     * @param comparator the comparator for the children of the provided explorable
     * @since 5.4
     */
    public void registerExplorableComparator(@Nonnull IExplorable explorable, @Nonnull Comparator<IExplorable> comparator)
    {
        filteredExplorerModel.addComparator(explorable, comparator);
    }

    /**
     * Register a comparator on the model, applied to the root.
     *
     * @param checkFunction a Predicate to test if the comparator applies
     * @param comparator    the Comparator to use to sort the nodes
     * @since 5.7.1
     */
    public void registerExplorableComparator(@Nonnull Predicate<IExplorable> checkFunction, @Nonnull Comparator<IExplorable> comparator)
    {
        filteredExplorerModel.addComparator(checkFunction, comparator);
    }

    /**
     * Add a filter to this explorer panel. This filter will affect which explorables are visible in the
     * user interface, but will not affect the underlying explorable model.
     *
     * @param filter the Predicate to filter the view by
     * @since 5.7
     */
    public void addViewFilter(@Nonnull Predicate<IExplorable> filter)
    {
        filteredExplorerModel.addFilter(filter);
    }

    /**
     * Add a filter to this explorer panel. This filter will affect which explorables are visible in the
     * user interface, but will not affect the underlying explorable model.
     *
     * @param parentFilter the Predicate to determine which parent nodes the filter applies to
     * @param filter the Predicate to filter the view by
     * @since 5.7
     */
    public void addViewFilter(@Nonnull Predicate<IExplorable> parentFilter, @Nonnull Predicate<IExplorable> filter)
    {
        filteredExplorerModel.addFilter(parentFilter, filter);
    }

    /**
     * Remove a filter from this panel.
     *
     * @param filter the Predicate filter to remove
     * @since 5.7
     */
    public void removeViewFilter(@Nonnull Predicate<IExplorable> filter)
    {
        filteredExplorerModel.removeFilter(filter);
    }

    /**
     * @return the explorable currently selected in the tree
     * @since 5.7
     */
    @Nullable
    public IExplorable getTreeSelectionExplorable()
    {
        return treeSelectionExplorable;
    }

    /**
     * Notify the controller of the specific explorer nodes that had their selection status change.
     *
     * @param listSelectionEvent The table selection event including the tree nodes whose state changed, and their new selection value.
     * @since 5.3
     */
    private void explorerTableSelectionChanged(ListSelectionEvent listSelectionEvent)
    {
        List<IExplorable> changedExplorables = new ArrayList<>();
        List<IExplorerDelegateListener.SelectionState> changedStates = new ArrayList<>();
        for (int i = listSelectionEvent.getFirstIndex(); i <= listSelectionEvent.getLastIndex(); i++)
        {
            // Not sure why this happens, but after switching our selection in the tree, and clearing the table selection, we get
            // passed indexes from the prior table contents, which we have to ignore by checking the index against the row count
            if (i < explorerTable.getRowCount())
            {
                int modelIndex = explorerTable.convertRowIndexToModel(i);
                changedStates.add(IExplorerDelegateListener.SelectionState.valueOf(explorerTable.getSelectionModel().isSelectedIndex(i)));
                changedExplorables.add(tableModel.getRow(modelIndex));
            }
        }
        controller.requestSetSelected(changedExplorables, changedStates);
    }

    /**
     * Expand the tree to make a tree node visible.
     *
     * @param explorable the explorable that should be made visible
     * @since 5.3
     */
    public void expandTreeToNode(@Nonnull IExplorable explorable)
    {
        expandTreeToNode(explorable.getExplorablePath());
    }

    /**
     * Expand the tree to make a tree node visible.
     *
     * @param expandPath the path to the explorable that should be visible
     * @since 5.3
     */
    @InvokeOnEDT
    public void expandTreeToNode(@Nonnull List<IExplorable> expandPath)
    {
        // The call to expandPath cannot end with a leaf node
        for (var i = expandPath.size() - 1; i >= 0; i--)
        {
            if (!treeExplorerModel.isLeaf(expandPath.get(i)))
            {
                explorerTree.expandPath(new TreePath((expandPath.subList(0, i + 1)).toArray()));
                break;
            }
        }
    }

    /**
     * Handle table mouse popup event.
     *
     * @param event the event
     * @since 5.3
     */
    protected void explorerTablePopupTrigger(@Nonnull MouseEvent event)
    {
        IExplorable explorable = explorerTable.getEntityAtPoint(event.getPoint());
        if (explorable != null)
        {
            showPopup(explorable, event); // TODO DCW TAKX-3257 handle multi-select
        }
    }

    /**
     * Handle tree mouse popup event.
     *
     * @param event the event
     * @since 5.3
     */
    protected void explorerTreePopupTrigger(@Nonnull MouseEvent event)
    {
        var point = event.getPoint();
        var explorable = explorerTree.getEntityAtPoint(point);
        if (explorable != null) showPopup(explorable, event); // TODO DCW TAKX-3232 provider interface for popup menus
    }

    private void showPopup(IExplorable explorable, MouseEvent event)
    {
        List<Action> actions = new ArrayList<>();
        if (explorable == controller.getGroupsRoot())
        {
            // Special popup for the groups node
            actions.add(new SimpleAction("Add Group", UIResources.ADD_ICON, e -> showAddGroupsDialog()));
        } else if (explorable instanceof IPathExplorableGroup path)
        {
            // Have to handle this here because we need a reference to this panel
            var explorables = findDeletableExplorables(path);
            if (!explorables.isEmpty()) actions.add(new DeleteExplorableAction(explorables));
        }
        showPopupMenu(explorable, actions, event);
    }

    /**
     * Finds all descendants than can be deleted.
     */
    @Nonnull
    private List<IExplorable> findDeletableExplorables(@Nonnull IPathExplorableGroup path)
    {
        var deletableExplorables = new ArrayList<IExplorable>();
        for (var child : filteredExplorerModel.getChildren(path))
        {
            if (child instanceof IPathExplorableGroup childPath)
            {
                deletableExplorables.addAll(findDeletableExplorables(childPath));
            } else if (child.supportsSilentDeletion())
            {
                deletableExplorables.add(child);
            }
        }
        return deletableExplorables;
    }

    /**
    * Listener for when a selection occurs on the explorer tree. Handles updating the table contents and expanding lazy nodes
    *
    * @param treeSelectionEvent selection event for the Explorer tree
    * @since 5.3
    */
    protected void explorerTreeSelectionChanged(TreeSelectionEvent treeSelectionEvent)
    {
        treeSelectionPath = treeSelectionEvent.getNewLeadSelectionPath();
        treeSelectionExplorable = treeSelectionPath != null ? (IExplorable) treeSelectionPath.getLastPathComponent() : null;
        if (singlePane)
        {
            if (treeSelectionExplorable != null) controller.requestSetSelected(List.of(treeSelectionExplorable), List.of(IExplorerDelegateListener.SelectionState.SELECTED));
        } else
        {
            if (treeSelectionExplorable != null) lazyManager.expand(treeSelectionExplorable);
            explorerTable.resetSortOrder();
            refreshTable();
        }
    }

    /**
     * Refresh the table with the children of the selected tree node.
     *
     * @since 5.7
     */
    protected void refreshTable()
    {
        if (treeSelectionExplorable == null)
        {
            tableModel.clear();
        } else
        {
            var newChildren = filteredExplorerModel.getChildren(treeSelectionExplorable);
            tableModel.setEntities(newChildren);
            TableUtils.setRowHeightFromContents(explorerTable, 5, 10);
        }
    }

    /**
     * Handle table mouse click event.
     *
     * @param event the event
     * @since 5.3
     */
    protected void explorerTableMouseClicked(@Nonnull MouseEvent event)
    {
        if (event.getButton() != MouseEvent.BUTTON1) return;
        if (event.getClickCount() % 2 == 0)
        {
            var point = event.getPoint();
            var modelColumn = explorerTable.getColumnAtPoint(point);
            if (modelColumn == entityDefinition.getSummaryColumn())
            {
                var explorable = explorerTable.getEntityAtPoint(point);
                if (explorable != null) explorerTableDrillDown(explorable);
            }
        }
    }

    /**
     * Handle tree mouse click event.
     *
     * @param event the event
     * @since 5.7
     */
    protected void explorerTreeMouseClicked(@Nonnull MouseEvent event)
    {
        if (event.getClickCount() == 2)
        {
            var point = event.getPoint();
            if (explorerTree.getColumnAtPoint(point) == entityDefinition.getHierarchicalColumn())
            {
                var explorable = explorerTree.getEntityAtPoint(point);
                // If the node has children then do nothing and let the tree expand the node, otherwise try to open the dialog
                if (explorable != null && filteredExplorerModel.getChildCount(explorable) == 0) showDialog(explorable);
            }
        }
    }

    /**
     * Select a node in the tree and populate the table with its children.
     *
     * @param explorable the explorable to select
     * @since 5.7
     */
    protected void explorerTableDrillDown(@Nonnull IExplorable explorable)
    {
        if (filteredExplorerModel.getChildCount(explorable) > 0 || lazyManager.isLazyAndUnexpanded(explorable))
        {
            explorerTree.getTreeSelectionModel().setSelectionPath(treeSelectionPath.pathByAddingChild(explorable));
        } else
        {
            showDialog(explorable);
        }
    }

    /**
     * Opens the selected explorable in the tree and selects it.
     *
     * @param explorable the explorable to select, or {@code null} to clear the tree selection
     * @since 5.3
     */
    public void setTreeSelected(@Nullable IExplorable explorable)
    {
        explorerTree.getTreeSelectionModel().setSelectionPath(explorable != null ? new TreePath(explorable.getExplorablePath().toArray()) : null);
    }

    /**
     * Show the dialog for an explorable.
     *
     * @param explorable explorable of interest.
     */
    @InvokeOnEDT
    public void showDialog(@Nonnull IExplorable explorable)
    {
        explorable.showDialog();
    }

    /**
     * @return the explorer tree component
     * @since 5.3
     */
    @Nonnull
    public EntityTreeTable<IExplorable> getExplorerTree()
    {
        return explorerTree;
    }

    /**
     * @return the explorer table component
     * @since 5.3
     */
    @Nonnull
    public CommonEntityTable<IExplorable> getExplorerTable()
    {
        return explorerTable;
    }

    /**
     * Shows or hides an explorable in the explorer tree and table.
     *
     * @param explorable the explorable
     * @param show {@code true} to show the explorable in the explorer, {@code false} to hide it, or {@code null} to force neither
     * @since 5.3 (Renamed from setExplorableVisible in 5.7)
     */
    public void showOrHideExplorable(@Nonnull IExplorable explorable, Boolean show)
    {
        if (!Objects.equals(show, showHideExplorables.get(explorable)))
        {
            if (show == null)
            {
                showHideExplorables.remove(explorable);
            } else
            {
                showHideExplorables.put(explorable, show);
            }
            if (baseExplorerModel != null) baseExplorerModel.fireEntityChanged(explorable);
        }
    }

    /**
     * Show popup menu for an explorable.  Extra actions may be provided to be inserted into the popup before being
     * displayed.
     *
     * @param explorable explorable of interest.
     * @param actions    extra actions to insert into the popup menu.
     * @param event      the originating event.
     */
    @InvokeOnEDT
    public void showPopupMenu(@Nonnull IExplorable explorable, @Nonnull List<Action> actions, @Nonnull MouseEvent event)
    {
        // TODO DCW TAKX-3282 this couples the view pretty tightly to the explorable. Should probably create some other provider interface for adding UI controls.
        JPopupMenu menu = explorable.createPopupMenu(event);
        if (!actions.isEmpty())
        {
            List<Component> originalComponents = new ArrayList<>();
            if (menu != null)
            {
                for (int i = 0, componentCount = menu.getComponentCount(); i < componentCount; i++) originalComponents.add(menu.getComponent(i));
                menu.removeAll();
            }

            menu = new JPopupMenu();

            for (Action action : actions) menu.add(action);

            if (!originalComponents.isEmpty())
            {
                menu.add(new JSeparator());
                for (Component component : originalComponents) menu.add(component);
            }
        }

        if (menu != null) menu.show(event.getComponent(), event.getX(), event.getY());
    }

    /**
     * Show an 'add group' dialog where the user can create a name and select a type of group to create.
     *
     * @since 5.3
     */
    @InvokeOnEDT
    public void showAddGroupsDialog()
    {
        new EditGroupDialog(controller).setVisible(true);
    }

    private boolean applyTreeFilters(IExplorable explorable)
    {
        return isTreeNodeType(explorable, filteredExplorerModel);
    }

    /**
     * Apply filters to the base explorer tree model:
     * <ul>
     *     <li>If present in {@link #showHideExplorables} then use the value from there</li>
     *     <li>Hide empty path nodes</li>
     * </ul>
     *
     * @param explorable An Explorable in this panel
     * @return {@code true} to allow, or {@code false} to hide from model
     */
    private boolean applySharedFilters(IExplorable explorable)
    {
        return showHideExplorables.containsKey(explorable) ? showHideExplorables.get(explorable) : !emptyPathManager.isEmptyPath(explorable) && (explorable).isVisibleInExplorer();
    }

    private void explorerTreeModelEvent(TreeModelCombinedListener.CombinedTreeModelEvent event)
    {
        if (event.getTreePath() == null || event.getTreePath().getLastPathComponent() == treeSelectionExplorable)
        {
            switch (event.getType())
            {
                case Structure, Remove, Insert -> refreshTable();
            }
        }
        explorerTree.getTreeSelectionModel().resetRowSelection();
        // Forcing a repaint because often the tree does not repaint when other components (e.g. Dialogs) have focus
        explorerTree.repaint();
    }

    /**
     * Determines whether a given explorable is eligible to be shown in the left hand tree panel of the explorer. Requires
     * passing the tree model as a fallback so that we don't ever hide nodes that have children.
     *
     * @param explorable The explorable to test
     * @param model a tree model to use in this calculation
     * @return {@code true} if the explorable is eligible to be shown in the tree, or {@code false} if it is considered a leaf
     * @since 5.7
     */
    public static boolean isTreeNodeType(@Nonnull IExplorable explorable, @Nonnull TreeTableModel model)
    {
        return explorable instanceof IControllerExplorable || explorable instanceof IExplorableGroup || model.getChildCount(explorable) > 0;
    }

    /**
     * A comparator that pushes any values in the {@link #order} List to the top and into the same order as the List. All other values
     * are considered equal and greater than those in the List
     */
    private record RootComparator<T>(List<T> order) implements Comparator<T>
    {
        @Override
        public int compare(T o1, T o2)
        {
            var i1 = order.indexOf(o1);
            var i2 = order.indexOf(o2);
            if (i1 == -1 && i2 == -1)
            {
                return 0; // Neither in list, fallback to other comparator
            } else if (i1 != -1 && i2 != -1)
            {
                return i1 < i2 ? -1 : 1; // Both in list, use list order
            } else
            {
                return i1 != -1 ? -1 : 1; // Only one in list, use listed one first
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        mainPanel = new JPanel();
        explorablePanel = new JPanel();
        splitPane = new JSplitPane();
        treeScrollPane = new JScrollPane();
        explorerTree = new EntityTreeTable<>();
        tableScrollPane = new JScrollPane();
        explorerTable = new CommonEntityTable<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== mainPanel ========
        {
            mainPanel.setName("mainPanel");
            mainPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default:grow"));

            //======== explorablePanel ========
            {
                explorablePanel.setName("explorablePanel");
                explorablePanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== splitPane ========
                {
                    splitPane.setResizeWeight(0.6);
                    splitPane.setName("splitPane");

                    //======== treeScrollPane ========
                    {
                        treeScrollPane.setName("treeScrollPane");

                        //---- explorerTree ----
                        explorerTree.setDropMode(DropMode.ON_OR_INSERT);
                        explorerTree.setShowsRootHandles(true);
                        explorerTree.setRootVisible(true);
                        explorerTree.setName("explorerTree");
                        treeScrollPane.setViewportView(explorerTree);
                    }
                    splitPane.setLeftComponent(treeScrollPane);

                    //======== tableScrollPane ========
                    {
                        tableScrollPane.setName("tableScrollPane");

                        //---- explorerTable ----
                        explorerTable.setDropMode(DropMode.ON_OR_INSERT);
                        explorerTable.setRowSelectionAllowed(false);
                        explorerTable.setActionButtonPanelVisible(false);
                        explorerTable.setName("explorerTable");
                        tableScrollPane.setViewportView(explorerTable);
                    }
                    splitPane.setRightComponent(tableScrollPane);
                }
                explorablePanel.add(splitPane, cc.xy(1, 1));
            }
            mainPanel.add(explorablePanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        }
        add(mainPanel, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    protected JPanel mainPanel;
    protected JPanel explorablePanel;
    protected JSplitPane splitPane;
    protected JScrollPane treeScrollPane;
    protected EntityTreeTable<IExplorable> explorerTree;
    protected JScrollPane tableScrollPane;
    protected CommonEntityTable<IExplorable> explorerTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
