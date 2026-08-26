package gov.takx.core.view.gui.geofence;

import gov.tak.api.maps.PointMapItem;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.geofence.MonitoredType;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.view.gui.explorer.view.SelectionExplorerPanel;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Dialog to allow the user to select a custom set of map items to monitor for {@link gov.takx.api.geofence.GeoFence} crossings.
 *
 * @since 5.3
 * @see SelectionExplorerPanel
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SelectCustomItemsDialog extends AStandardDialog
{
    @Inject
    private IExplorerManager explorerManager;

    // Only present the user with map items that are usable for Custom monitoring, filter out all others
    private static final Predicate<IExplorable> POINT_MAP_ITEM_FILTER =
            explorable -> explorable.getOwner() instanceof PointMapItem pmi && MonitoredType.Custom.matches(pmi);

    private final List<PointMapItem> initialSelection;
    private final Consumer<List<PointMapItem>> selectionHandler;
    private final Runnable cancelHandler;

    private SelectionExplorerPanel selectionExplorerPanel;

    /**
     * @param initialSelection Map items to initially select
     * @param selectionHandler Handler to process the user selected map items
     * @param cancelHandler Optional runnable to invoke when the user cancels this dialog
     */
    public SelectCustomItemsDialog(@Nullable Window owner, @Nonnull Collection<PointMapItem> initialSelection,
                                   @Nonnull Consumer<List<PointMapItem>> selectionHandler, @Nullable Runnable cancelHandler)
    {
        super(owner);
        this.initialSelection = List.copyOf(initialSelection);
        this.selectionHandler = selectionHandler;
        this.cancelHandler = cancelHandler;

        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        final var explorer = explorerManager.getDefaultExplorerDelegate();
        final var model = explorer.getModel();
        final var root = model.getRoot();

        selectionExplorerPanel = new SelectionExplorerPanel(model, explorer.getController());
        selectionExplorerPanel.getFilteredExplorerModel().setLeafFilter(POINT_MAP_ITEM_FILTER);
        selectionExplorerPanel.setSelectedExplorables(getExplorablesToSelect(explorer));

        // Expand all the tree nodes and select the root group
        expandNodeAndChildGroups(root);
        selectionExplorerPanel.setTreeSelected(root);

        contentPanel.add(selectionExplorerPanel);
    }

    private List<IExplorable> getExplorablesToSelect(IExplorer explorer)
    {
        // The Map Object's key is the unique key for the explorable representing it
        return initialSelection.stream()
                .map(mapItem -> explorer.getExplorable(mapItem.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    private void expandNodeAndChildGroups(IExplorable node)
    {
        selectionExplorerPanel.expandTreeToNode(node);
        node.getChildren().stream().filter(IExplorable::isGroup).forEach(this::expandNodeAndChildGroups);
    }

    private void ok()
    {
        dispose();

        // Regardless of what the user selected, we only want PointMapItems
        final var mapItems = selectionExplorerPanel.getSelectionPaths().stream()
                .map(path -> path.get(path.size() - 1).getOwner())
                .filter(PointMapItem.class::isInstance)
                .map(PointMapItem.class::cast)
                .toList();

        selectionHandler.accept(mapItems);
    }

    private void cancel()
    {
        dispose();
        if (cancelHandler != null) cancelHandler.run();
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPane = new JPanel();
        contentPanel = new JPanel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();

        //======== this ========
        setModal(true);
        setMinimumSize(new Dimension(550, 250));
        setTitle("Select the Map Items to Monitor");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new BorderLayout());
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 85, 80};
                ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText("Select");
                okButton.setName("okButton");
                okButton.addActionListener(e -> ok());
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancel());
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel contentPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
