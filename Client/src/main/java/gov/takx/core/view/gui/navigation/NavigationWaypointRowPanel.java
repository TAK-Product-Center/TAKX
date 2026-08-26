package gov.takx.core.view.gui.navigation;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.IRoute;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.commons.swing.LineBorderExt;
import gov.takx.core.mapobject.gui.MapObjectField;
import gov.takx.core.navigation.NavigationConfiguration;
import gov.takx.core.navigation.NavigationConstants;
import gov.takx.core.navigation.NavigationTarget;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

import static gov.takx.core.navigation.NavigationConfiguration.CURRENT_TARGET;

/**
 * A panel showing navigation data for a single waypoint on a {@link IRoute}.  The data displayed is navigation data from
 * the device's current position to the given waypoint.  Will update itself based on changes to the waypoint.
 *
 * @since 3.4
 */
public class NavigationWaypointRowPanel extends JPanel implements IResourceCleaner
{
    private final ListenerManager listenerManager = new ListenerManager();
    private final NavigationTarget target;
    private final ArrowDetailsUpdater arrowDetailsUpdater;
    private final NavigationConfiguration configuration;

    protected NavigationWaypointRowPanel(@Nonnull NavigationTarget target)
    {
        initComponents();
        this.target = target;
        configuration = target.getNavigation();
        var arrow = target.getTargetArrow();

        listenerManager.addPropertyChangeListener(configuration, List.of(CURRENT_TARGET), ignored -> update());
        listenerManager.addPropertyChangeListener(target.getTargetArrow(), List.of(ShapeProperties.LINE_COLOR), ignored -> update());

        arrowDetailsUpdater = new ArrowDetailsUpdater(arrow, bearingLabel, rangeLabel, relativeBearingLabel, null, etaLabel, null);
        sourceField.setSelectedItem(arrow.getStartAnchor());
        targetField.setSelectedItem(arrow.getRangeAnchor());

        navigateToButton.setIcon(NavigationConstants.NAVIGATE_TO_ICON);
        update();
    }

    @InvokeOnEDT
    private void update()
    {
        var current = target == configuration.getCurrentTarget();
        sourceField.setVisible(current);
        arrowIcon.setVisible(current);
        ((LineBorderExt) ((CompoundBorder) getBorder()).getInsideBorder()).setLineColor(current ? target.getTargetArrow().getLineColor() : Color.gray);
        repaint(); // Needed after changing border color
    }

    @Override
    public void releaseResources()
    {
        arrowDetailsUpdater.releaseResources();
        sourceField.releaseResources();
        targetField.releaseResources();
        listenerManager.unregisterAll();
    }

    private void navigateToButtonPressed()
    {
        configuration.setCurrentTarget(target);
    }

    @SuppressWarnings("RedundantExplicitVariableType")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var wrapper = new JPanel();
        var headerPanel = new JPanel();
        targetField = new MapObjectField<>();
        arrowIcon = new JLabel();
        sourceField = new MapObjectField<>();
        navigateToButton = new JButton();
        var separator = new JSeparator();
        var infoPanelWrapper = new JPanel();
        var navInfoPanel = new JPanel();
        var rangeTitleLabel = new JLabel();
        rangeLabel = new JLabel();
        var relativeBearingTitleLabel = new JLabel();
        relativeBearingLabel = new JLabel();
        var bearingTitleLabel = new JLabel();
        bearingLabel = new JLabel();
        var etaTitleLabel = new JLabel();
        etaLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new CompoundBorder(
                new EmptyBorder(3, 10, 3, 10),
                new LineBorderExt()));
        setName("this");
        setLayout(new BorderLayout());

        //======== wrapper ========
        {
            wrapper.setBorder(new EmptyBorder(2, 2, 2, 2));
            wrapper.setName("wrapper");
            wrapper.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, fill:default"));

            //======== headerPanel ========
            {
                headerPanel.setName("headerPanel");
                headerPanel.setLayout(new FormLayout(
                        "3*(default, $lcgap), right:default:grow",
                        "default"));

                //---- targetField ----
                targetField.setEditable(false);
                targetField.setIconVisible(false);
                targetField.setName("targetField");
                headerPanel.add(targetField, cc.xy(1, 1));

                //---- arrowIcon ----
                arrowIcon.setText("\u2190");
                arrowIcon.setVisible(false);
                arrowIcon.setName("arrowIcon");
                headerPanel.add(arrowIcon, cc.xy(3, 1));

                //---- sourceField ----
                sourceField.setEditable(false);
                sourceField.setNameVisible(false);
                sourceField.setVisible(false);
                sourceField.setName("sourceField");
                headerPanel.add(sourceField, cc.xy(5, 1));

                //---- navigateToButton ----
                navigateToButton.setName("navigateToButton");
                navigateToButton.addActionListener(e -> navigateToButtonPressed());
                headerPanel.add(navigateToButton, cc.xy(7, 1));
            }
            wrapper.add(headerPanel, cc.xy(1, 1));

            //---- separator ----
            separator.setName("separator");
            wrapper.add(separator, cc.xy(1, 2));

            //======== infoPanelWrapper ========
            {
                infoPanelWrapper.setName("infoPanelWrapper");
                infoPanelWrapper.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default"));

                //======== navInfoPanel ========
                {
                    navInfoPanel.setName("navInfoPanel");
                    navInfoPanel.setLayout(new FormLayout(
                            "right:default, $lcgap, left:65px, $ugap, default, $lcgap, 65px:grow",
                            "default, $lgap, default"));

                    //---- rangeTitleLabel ----
                    rangeTitleLabel.setText("Range:");
                    rangeTitleLabel.setName("rangeTitleLabel");
                    navInfoPanel.add(rangeTitleLabel, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                    //---- rangeLabel ----
                    rangeLabel.setName("rangeLabel");
                    navInfoPanel.add(rangeLabel, cc.xy(3, 1));

                    //---- relativeBearingTitleLabel ----
                    relativeBearingTitleLabel.setText("Relative Bearing:");
                    relativeBearingTitleLabel.setName("relativeBearingTitleLabel");
                    navInfoPanel.add(relativeBearingTitleLabel, cc.xy(5, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                    //---- relativeBearingLabel ----
                    relativeBearingLabel.setName("relativeBearingLabel");
                    navInfoPanel.add(relativeBearingLabel, cc.xy(7, 1));

                    //---- bearingTitleLabel ----
                    bearingTitleLabel.setText("Bearing:");
                    bearingTitleLabel.setName("bearingTitleLabel");
                    navInfoPanel.add(bearingTitleLabel, cc.xy(1, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                    //---- bearingLabel ----
                    bearingLabel.setName("bearingLabel");
                    navInfoPanel.add(bearingLabel, cc.xy(3, 3));

                    //---- etaTitleLabel ----
                    etaTitleLabel.setText("ETA:");
                    etaTitleLabel.setName("etaTitleLabel");
                    navInfoPanel.add(etaTitleLabel, cc.xy(5, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                    //---- etaLabel ----
                    etaLabel.setName("etaLabel");
                    navInfoPanel.add(etaLabel, cc.xy(7, 3));
                }
                infoPanelWrapper.add(navInfoPanel, cc.xy(1, 1));
            }
            wrapper.add(infoPanelWrapper, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));
        }
        add(wrapper, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private MapObjectField<IMapObject> targetField;
    private JLabel arrowIcon;
    private MapObjectField<IMapObject> sourceField;
    private JButton navigateToButton;
    private JLabel rangeLabel;
    private JLabel relativeBearingLabel;
    private JLabel bearingLabel;
    private JLabel etaLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
