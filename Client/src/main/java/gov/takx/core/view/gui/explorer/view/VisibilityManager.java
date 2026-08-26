package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.explorer.IExplorable;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.MouseClickedListener;
import gov.takx.commons.swing.entity.EntityColumn;
import gov.takx.commons.swing.entity.EntityTable;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.EntityTreeModelEvent;
import gov.takx.commons.swing.entity.EntityTreeTable;
import gov.takx.commons.swing.entity.EntityTreeTableModel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gov.takx.commons.swing.entity.common.CommonEntityDefinition.VISIBILITY_OFF_ICON;
import static gov.takx.commons.swing.entity.common.CommonEntityDefinition.VISIBILITY_ON_ICON;
import static gov.takx.core.view.gui.explorer.view.Visibility.Invisible;
import static gov.takx.core.view.gui.explorer.view.Visibility.Partial;
import static gov.takx.core.view.gui.explorer.view.Visibility.Unsupported;
import static gov.takx.core.view.gui.explorer.view.Visibility.Visible;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;

/**
 * This class manages the complex rollup of visibility state in the explorer tree. The cascading nature of the visibility
 * property requires a careful implementation to avoid unnecessary and/or redundant execution. Because the visibility state
 * is shown in both the tree and the table, this class has its claws in multiple places, but the goal is for all significant
 * logic related to it to remain in this class.
 * <p>
 * The {@link Visibility} state of a given explorable is determined by the {@link IExplorable#isVisible()} flag of the
 * explorable itself and / or the visibility state of it's direct children. If the explorable itself does not support the
 * {@link IExplorable#isVisible()} flag (See {@link IExplorable#supportsVisibility()}) then the state is determined only by
 * the state of the children. Two important pieces of logic must be implemented for this feature to function:
 * <ol>
 *     <li>When the {@link Visibility} state of an explorable changes, that change must "roll up" i.e. it's parents must
 *     re-calculate their own state.</li>
 *     <li>When the user clicks the {@link Visibility} state icon, that change must "roll down" i.e. it's children must
 *     reset their {@code visible} flag, if supported.</li>
 * </ol>
 * The "roll-up" logic operates as follows: <p>
 * <ul>
 *      <li>If the explorable is visible (or does not support visibility) and all children have the {@link Visibility#Visible} state,
 * then the explorable is {@link Visibility#Visible}.</li>
 *      <li>If the explorable is not visible (or does not support visibility) and all children have the {@link Visibility#Invisible} state
 * then the explorable is {@link Visibility#Invisible}</li>
 *      <liL>If the explorable and none of its children support visibility, it will have the {@link Visibility#Unsupported} state.</liL>
 *      <li>In all other cases the explorable has the {@link Visibility#Partial} state.</li>
 * </ul>
 * The "roll-down" logic operates as follows:
 * <ul>
 * <li>If the explorable is in {@link Visibility#Partial} or {@link Visibility#Invisible} state, all descendants that
 *     support the visibility are set to visible, the result being
 *     that the given explorable moves to the {@link Visibility#Visible} state</li>
 * <li>If the explorable is in the {@link Visibility#Visible} state, all descendants that support visibility are set to
 * invisible, the result being that the given explorable moves to the {@link Visibility#Invisible} state.</li>
 * </ul>
 *
 * @since 5.7
 */
public class VisibilityManager
{
    private static final Icon VISIBILITY_PARTIAL_ICON = UIResources.getSizedIcon(PLATFORM_ICONS, "visibility_partial.png", ACTION_BUTTON_IMAGE_SIZE);
    // Yes, this needs to be a HashMap because it needs to allow null key lookups
    static final HashMap<Visibility, Icon> VISIBILITY_ICON_MAP = new HashMap<>();
    static
    {
        VISIBILITY_ICON_MAP.put(Visible, VISIBILITY_ON_ICON);
        VISIBILITY_ICON_MAP.put(Partial, VISIBILITY_PARTIAL_ICON);
        VISIBILITY_ICON_MAP.put(Invisible, VISIBILITY_OFF_ICON);
    }

    private final Map<IExplorable, Visibility> explorableVisibility = new HashMap<>();
    private EntityTreeTableModel<IExplorable> treeTableModel;
    private EntityTableModel<IExplorable> tableModel;
    private EntityColumn<IExplorable, ?> visibilityRollUpColumn;
    private boolean rollingDown;

    /**
     * Initialize references to other components.
     * 
     * @param entityDefinition the explorer entity definition
     * @param panel the explorer UI panel
     * @param treeTableModel the explorer tree model to use for rollup
     * @param tableModel the explorer table model
     */
    public void init(@Nonnull ExplorableEntityDefinition entityDefinition, @Nonnull ExplorerPanel panel, @Nonnull EntityTreeTableModel<IExplorable> treeTableModel, @Nonnull EntityTableModel<IExplorable> tableModel)
    {
        this.tableModel = tableModel;
        this.treeTableModel = treeTableModel;
        visibilityRollUpColumn = entityDefinition.getVisibilityRollUpColumn();
        treeTableModel.addEntityTreeModelListener(this::modelChanged);
        calculateVisibility(treeTableModel.getRoot());
        // TODO DCW TAKX-4227 We are using the old table-cell-click trick here rather than proper buttons, due to
        //  potential performance issues with the ActionList renderer when rendering 100s of rows. This is fine for now 
        //  because there is only a single action in this column.
        panel.getExplorerTable().addMouseListener((MouseClickedListener) this::explorerTableMouseClicked);
        panel.getExplorerTree().addMouseListener((MouseClickedListener) this::explorerTreeMouseClicked);
    }

    /**
     * @param explorable An explorable in this manager
     * @return The current visibility state of the given explorable, if it is supported, otherwise {@code null}
     */
    @Nullable
    public Visibility getVisibilityIfSupported(@Nonnull IExplorable explorable)
    {
        var state = explorableVisibility.get(explorable);
        return state == Unsupported ? null : state;
    }

    void updateAndRollUpVisibility(@Nonnull IExplorable explorable)
    {
        if (rollingDown || !explorableVisibility.containsKey(explorable)) return;

        var visibility = calculateVisibility(explorable);
        if (explorableVisibility.get(explorable) != visibility)
        {
            explorableVisibility.put(explorable, visibility);
            rollUpVisibility(List.of(explorable));
        }
    }

    @SuppressWarnings("unchecked")
    private void explorerTableMouseClicked(@Nonnull MouseEvent event)
    {
        if (event.getButton() != MouseEvent.BUTTON1) return;
        var table = (EntityTable<IExplorable>) event.getSource();
        var point = event.getPoint();
        var modelColumn = table.convertColumnIndexToModel(table.columnAtPoint(point));
        if (modelColumn == tableModel.getColumnIndex(visibilityRollUpColumn))
        {
            toggleVisibility(table.getEntityAtPoint(point));
            event.consume();
        }
    }

    @SuppressWarnings("unchecked")
    private void explorerTreeMouseClicked(@Nonnull MouseEvent event)
    {
        if (event.getButton() != MouseEvent.BUTTON1) return;
        var treeTable = (EntityTreeTable<IExplorable>) event.getSource();
        var point = event.getPoint();
        var modelColumn = treeTable.convertColumnIndexToModel(treeTable.columnAtPoint(point));
        if (modelColumn == treeTableModel.getColumnIndex(visibilityRollUpColumn))
        {
            toggleVisibility(treeTable.getEntityAtPoint(point));
            event.consume();
        }
    }

    /**
     * Calculates the visibility state of an explorable based on the state of its children.
     */
    @Nonnull
    private Visibility calculateVisibility(IExplorable explorable)
    {
        var visibility = !explorable.supportsVisibility() ? Unsupported : explorable.isVisible() ? Visible : Invisible;
        var children = treeTableModel.getChildren(explorable);
        for (var child : children)
        {
            if (!explorableVisibility.containsKey(child)) explorableVisibility.put(child, calculateVisibility(child));
            var childVis = explorableVisibility.get(child);
            if (childVis != Unsupported)
            {
                if (visibility == Unsupported)
                {
                    visibility = childVis;
                } else if (visibility != childVis)
                {
                    visibility = Visibility.Partial;
                }
                if (visibility == Visibility.Partial) break; // Can't come back from partial
            }
        }
        return visibility;
    }

    /**
     * Toggles the visibility state of an explorable due to a user clicking on the icon.
     */
    private void toggleVisibility(IExplorable explorable)
    {
        var state = explorableVisibility.get(explorable);
        var visible = switch (state)
        {
            case Visible -> false;
            case Invisible, Partial -> true;
            case Unsupported -> null;
        };
        if (visible != null)
        {
            rollingDown = true;
            var rollDownChanged = new HashSet<IExplorable>();
            var visibility = visible ? Visibility.Visible : Visibility.Invisible;
            checkAndRollDown(explorable, visible, visibility, rollDownChanged);
            rollDownChanged.forEach(exp -> {
                explorableVisibility.put(exp, visibility);
                exp.setVisible(visible); // This causes PCEs
            });
            rollingDown = false;
            rollUpVisibility(rollDownChanged);
        }
    }

    /**
     * This logic prevents redundant rollup calculations. Since our nodes can exist at multiple places in the tree, we
     * start with the longest paths i.e. the furthest out, then move up the tree. This ensures that we roll up all
     * children prior to rolling up any parents, and also allows us to skip any nodes that we have already rolled up in
     * another location. I can't say for certain that this is strictly "optimal" but it can handle bulk changes on the
     * order of thousands of nodes.
     */
    @SuppressWarnings("UnusedReturnValue")
    private Set<IExplorable> rollUpVisibility(Collection<IExplorable> changed)
    {
        var paths = changed
                .stream()
                .flatMap(exp -> treeTableModel.getAllPaths(exp).stream())
                .distinct()
                .sorted(Comparator.comparing((List<?> l) -> l.size()).reversed())
                .toList();
        var rollUpChanged = new HashSet<IExplorable>();
        var rolledUp = new HashSet<IExplorable>();

        for (var i = paths.get(0).size(); i > 0; i--)
        {
            for (var path : paths)
            {
                if (path.size() < i) break;
                var node = path.get(i - 1);
                if (!rolledUp.contains(node))
                {
                    var newVis = calculateVisibility(node);
                    if (explorableVisibility.get(node) != newVis)
                    {
                        explorableVisibility.put(node, newVis);
                        rollUpChanged.add(node);
                    }
                    rolledUp.add(node);
                }
            }
        }
        var columnIndex = tableModel.getColumnIndex(visibilityRollUpColumn);
        rollUpChanged.forEach(exp -> {
            treeTableModel.fireEntityChanged(exp);
            tableModel.fireTableCellUpdated(exp, columnIndex);
        });
        return rollUpChanged;
    }

    /**
     * Attempts to set the visible state of an explorable and then rolls down that state to children, keeping track of
     * which explorables changed.
     */
    private void checkAndRollDown(IExplorable explorable, boolean visible, Visibility visibility, Set<IExplorable> change)
    {
        // TODO DCW TAKX-4168 better handling of locked explorables
        if (explorable.isVisibilityLocked())
        {
            var currentVisible = explorable.isVisible();
            if (visible != currentVisible) AlwaysOnTopJOptionPane.showMessageDialog("<html>Visibility for <b>" + explorable.getDisplayName() + "</b> is locked to <i>" + (currentVisible ? "visible" : "hidden") + "</i> by device and cannot be changed by the user</html>", "Visibility Locked", JOptionPane.INFORMATION_MESSAGE);
        } else
        {
            if (explorableVisibility.get(explorable) != visibility)
            {
                if (explorable.supportsVisibility()) change.add(explorable);
                var children = treeTableModel.getChildren(explorable);
                for (var child : children)
                {
                    checkAndRollDown(child, visible, visibility, change);
                }
            }
        }
    }

    /**
     * Handle insert and remove events in the tree. The update events are handled in
     * {@link ExplorerTreeTableModel#addListeners(IExplorable)}.
     */
    private void modelChanged(EntityTreeModelEvent<IExplorable> event)
    {
        var child = event.child();
        switch (event.type())
        {
            case Insert ->
            {
                //noinspection DataFlowIssue child is always present for insert event
                var visibility = calculateVisibility(child);
                explorableVisibility.put(child, visibility);
                updateAndRollUpVisibility(event.entity());
            }
            case Remove ->
            {
                updateAndRollUpVisibility(event.entity());
                //noinspection DataFlowIssue child is always present for remove event
                if (treeTableModel.getParents(child).isEmpty()) explorableVisibility.remove(child);
            }
        }
    }
}
