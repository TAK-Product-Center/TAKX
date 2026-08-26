package gov.takx.core.view.gui.navigation;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.commons.swing.JToggleButtonExt;
import gov.takx.commons.swing.entity.ActionButtonPanel;
import gov.takx.core.navigation.NavigationConfiguration;
import gov.takx.core.navigation.NavigationManager;
import gov.takx.core.navigation.NavigationTarget;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.CANCEL_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.KEEP_IN_VIEW_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.KEEP_IN_VIEW_ICON_FILE;

/**
 * The main panel for the navigation feature. It takes a {@link NavigationConfiguration} and displays the inner
 * {@link NavigationTarget} instances in panels, with the current target displayed in a header alongside a compass widget.
 *
 * @since 3.4 (significant redesign in 5.7)
 */
@PseudoBean
public class NavigationPanel extends ATitledPanel implements IResourceCleaner, NavigationManager.NavigationManagerListener
{
    public static final String PANEL_TITLE = "Navigation";
    private static final ImageIcon REVERSE_BUTTON_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "reverse.svg");
    private static final Icon KEEP_IN_VIEW_ICON_SELECTED = UIResources.getSizedColorizedIcon(UIResources.getUrl(PLATFORM_ICONS, KEEP_IN_VIEW_ICON_FILE), ACTION_BUTTON_IMAGE_SIZE, UIManager.getColor("Table.background"));

    private final List<NavigationWaypointRowPanel> waypointRows = new ArrayList<>();
    private final ListenerManager listenerManager = new ListenerManager();

    @Inject
    private NavigationManager navigationManager;
    private NavigationConfiguration currentConfiguration;

    /**
     * Construct a new empty panel.
     *
     * @since 5.7
     */
    public NavigationPanel()
    {
        super(PANEL_TITLE);
        initComponents();
        checkPointScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        reverseButton.setIcon(REVERSE_BUTTON_ICON);
        keepInViewButton.setIcon(KEEP_IN_VIEW_ACTION_BUTTON_ICON);
        keepInViewButton.setSelectedIcon(KEEP_IN_VIEW_ICON_SELECTED);
        keepInViewButton.setSelectedBackground(Color.WHITE);
        keepInViewButton.setSelectedForeground(UIManager.getColor("Table.background"));

        cancelButton.setIcon(CANCEL_ACTION_BUTTON_ICON);

        actionButtonPanel.addActionButton(reverseButton);
        actionButtonPanel.addActionButton(keepInViewButton);
        actionButtonPanel.addActionButton(cancelButton);
    }

    @PostConstruct
    private void postConstruct()
    {
        navigationManager.addConfigurationListener(this);
    }

    @Override
    public void added(@Nonnull NavigationConfiguration configuration)
    {
    }

    @Override
    @InvokeOnEDT
    public void removed(@Nonnull NavigationConfiguration configuration)
    {
        if (configuration == currentConfiguration)
        {
            navigationManager.hideNavigationPanel();
        }
    }

    /**
     * @param navigation the navigation configuration to display in this panel, or {@code null} to clear the panel
     * @since 5.7
     */
    @InvokeOnEDT
    public void setNavigation(@Nullable NavigationConfiguration navigation)
    {
        if (currentConfiguration == navigation) return;
        clearWaypointRowPanels();
        listenerManager.unregisterAll();
        currentConfiguration = navigation;

        if (navigation == null)
        {
            navigationHeaderPanel.setTarget(null);
            actionButtonPanel.setButtonsEnabled(false);
            keepInViewButton.setSelected(false);
        } else
        {
            actionButtonPanel.setButtonsEnabled(true);
            listenerManager.addPropertyChangeListener(navigation, List.of(ShapeProperties.LOCATIONS, NavigationConfiguration.REVERSED), ignored -> createWaypointRowPanels());
            listenerManager.addPropertyChangeListener(navigation, List.of(NavigationConfiguration.CURRENT_TARGET), ignored -> navigationHeaderPanel.setTarget(currentConfiguration.getCurrentTarget()));
            if (navigation.getTargets().size() > 1) createWaypointRowPanels();
            navigationHeaderPanel.setTarget(navigation.getCurrentTarget());
            reverseButton.setEnabled(currentConfiguration.getTargets().size() > 1);
            keepInViewButton.setSelected(navigationManager.isKeepInView(currentConfiguration));
        }
        repaint();
    }

    @Override
    public void releaseResources()
    {
        setNavigation(null);
        navigationManager.removeConfigurationListener(this);
    }

    /**
     * Creates the {@link NavigationWaypointRowPanel} for all the waypoints in the route, removing and disposing any
     * rows that currently exist first.  Executes on EDT to control thread access to waypointRowPanels
     */
    @InvokeOnEDT
    private void createWaypointRowPanels()
    {
        clearWaypointRowPanels();
        currentConfiguration.getTargetsOrdered().forEach(this::addNavigationWaypointRow);
        checkPointPanel.revalidate();
        repaint();
    }

    private void clearWaypointRowPanels()
    {
        waypointRows.forEach(NavigationWaypointRowPanel::releaseResources);
        waypointRows.clear();
        checkPointPanel.removeAll();
    }

    /**
     * Creates a {@link NavigationWaypointRowPanel} for the provided target and adds it to this panel.
     */
    private void addNavigationWaypointRow(@Nonnull NavigationTarget target)
    {
        var rowPanel = new NavigationWaypointRowPanel(target);
        checkPointPanel.add(rowPanel);
        waypointRows.add(rowPanel);
    }

    private void reverseButtonActionPerformed()
    {
        currentConfiguration.setReversed(!currentConfiguration.isReversed());
    }

    private void cancelButtonAction()
    {
        navigationManager.removeConfiguration(currentConfiguration, cancelButton);
    }

    private void keepInViewAction()
    {
        if (navigationManager.isKeepInView(currentConfiguration) != keepInViewButton.isSelected())
        {
            navigationManager.toggleKeepInView(currentConfiguration);
        }
    }

    @SuppressWarnings("RedundantExplicitVariableType")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        actionButtonPanel = new ActionButtonPanel();
        navigationHeaderPanel = new NavigationHeaderPanel();
        var separator = new JSeparator();
        checkPointScrollPane = new JScrollPane();
        checkPointPanel = new JPanel();
        reverseButton = new JButton();
        keepInViewButton = new JToggleButtonExt();
        var panel = new JPanel();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "2*(default, $lgap), $rgap, $lgap, fill:default:grow"));

        //---- actionButtonPanel ----
        actionButtonPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, UIManager.getColor("TableHeader.separatorColor")),
                new EmptyBorder(5, 5, 5, 5)));
        actionButtonPanel.setName("actionButtonPanel");
        add(actionButtonPanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

        //---- navigationHeaderPanel ----
        navigationHeaderPanel.setName("navigationHeaderPanel");
        add(navigationHeaderPanel, cc.xy(1, 3));

        //---- separator ----
        separator.setBorder(null);
        separator.setName("separator");
        add(separator, cc.xy(1, 5));

        //======== checkPointScrollPane ========
        {
            checkPointScrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));
            checkPointScrollPane.setName("checkPointScrollPane");

            //======== checkPointPanel ========
            {
                checkPointPanel.setBorder(null);
                checkPointPanel.setName("checkPointPanel");
                checkPointPanel.setLayout(new VerticalLayout());
            }
            checkPointScrollPane.setViewportView(checkPointPanel);
        }
        add(checkPointScrollPane, cc.xy(1, 7));

        //---- reverseButton ----
        reverseButton.setToolTipText("Reverse");
        reverseButton.setText("Reverse");
        reverseButton.setName("reverseButton");
        reverseButton.addActionListener(e -> reverseButtonActionPerformed());

        //---- keepInViewButton ----
        keepInViewButton.setToolTipText("Toggle Keep-In-View");
        keepInViewButton.setText("Keep-In-View");
        keepInViewButton.setName("keepInViewButton");
        keepInViewButton.addActionListener(e -> keepInViewAction());

        //======== panel ========
        {
            panel.setName("panel");
            panel.setLayout(new FormLayout(
                    "default:grow, 2*($lcgap, default)",
                    "default"));
        }

        //---- cancelButton ----
        cancelButton.setToolTipText("Cancel Navigation");
        cancelButton.setText("Cancel");
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(e -> cancelButtonAction());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private ActionButtonPanel actionButtonPanel;
    private NavigationHeaderPanel navigationHeaderPanel;
    private JScrollPane checkPointScrollPane;
    private JPanel checkPointPanel;
    private JButton reverseButton;
    private JToggleButtonExt keepInViewButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

}
