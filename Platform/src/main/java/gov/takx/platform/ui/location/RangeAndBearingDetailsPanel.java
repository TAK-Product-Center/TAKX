package gov.takx.platform.ui.location;

import com.atakmap.coremap.maps.coords.NorthReference;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * A swing panel that displays range and bearing details in multiple unit systems, between two map objects. This component is
 * specifically styled for use in specific places of the UI (e.g. the Properties panel), and is not necessarily the proper
 * component to use in all places where range and bearing details are needed.
 *
 * @since 5.7
 */
public class RangeAndBearingDetailsPanel extends JPanel implements IResourceCleaner
{
    private static final Icon ARROW_DOWN_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "double_arrow_down.svg");
    private static final Icon ARROW_UP_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "double_arrow_up.svg");
    private static final List<String> LISTENER_PROPERTIES = List.of(IMapObject.LOCATION);

    private final ListenerManager listenerManager = new ListenerManager();
    private IMapObject source;
    private IMapObject target;

    /**
     * Construct a new component with no range and bearing data.
     */
    public RangeAndBearingDetailsPanel()
    {
        initComponents();
        detailsButton.setIcon(ARROW_UP_ICON);
        detailsButton.setSelected(true);
    }

    /**
     * @return the source map object, or {@code null} if one has not yet been set
     */
    @Nullable
    public IMapObject getSource()
    {
        return source;
    }

    /**
     * @param source the source map object, or {@code null} to clear the value and hide all derived values
     */
    public void setSource(@Nullable IMapObject source)
    {
        if (this.source == source) return;
        if (this.source != null) listenerManager.unregister(this.source);
        this.source = source;
        if (this.source != null) listenerManager.addPropertyChangeListener(this.source, LISTENER_PROPERTIES, e -> update());
        update();
    }

    /**
     * @return the target map object, or {@code null} if one has not been set
     */
    @Nullable
    public IMapObject getTarget()
    {
        return target;
    }

    /**
     * @param target the source map object, or {@code null} to clear the value and hide all derived values
     */
    public void setTarget(@Nullable IMapObject target)
    {
        if (this.target == target) return;
        if (this.target != null) listenerManager.unregister(this.target);
        this.target = target;
        if (this.target != null) listenerManager.addPropertyChangeListener(this.target, List.of(IMapObject.LOCATION), e -> update());
        update();
    }

    @Override
    public void releaseResources()
    {
        listenerManager.unregisterAll();
    }

    @InvokeOnEDT
    private void update()
    {
        var rangeKm = "";
        var rangeMi = "";
        var rangeNm = "";
        var bearingDegM = "";
        var bearingDegT = "";
        var bearingMilM = "";
        var bearingMilT = "";
        var direction = "";
        if (source != null && target != null)
        {
            var srcLocation = source.getLocation();
            var tarLocation = target.getLocation();
            var distance = srcLocation.distanceTo(tarLocation);
            rangeKm = RangeUnits.METERS.format(distance, 2);
            rangeMi = RangeUnits.FEET.format(distance, 2);
            rangeNm = RangeUnits.NAUTICAL_MILES.format(distance, 2);
            var bearing = srcLocation.bearingTo(tarLocation);
            bearingDegM = NorthReference.format(bearing, srcLocation, distance, BearingUnits.DEGREE.getAngle(), NorthReference.MAGNETIC, 0);
            bearingDegT = NorthReference.format(bearing, srcLocation, distance, BearingUnits.DEGREE.getAngle(), NorthReference.TRUE, 0);
            bearingMilM = NorthReference.format(bearing, srcLocation, distance, BearingUnits.MIL.getAngle(), NorthReference.MAGNETIC, 0);
            bearingMilT = NorthReference.format(bearing, srcLocation, distance, BearingUnits.MIL.getAngle(), NorthReference.TRUE, 0);
            direction = BearingUnits.Direction.fromDegrees8(bearing).toString();
        }
        rangeKmValue.setText(rangeKm);
        rangeMiValue.setText(rangeMi);
        rangeNmValue.setText(rangeNm);
        bearingDegMValue.setText(bearingDegM);
        bearingDegTValue.setText(bearingDegT);
        bearingMilMValue.setText(bearingMilM);
        bearingMilTValue.setText(bearingMilT);
        directionValue.setText(direction);
    }

    private void detailsButtonActionPerformed()
    {
        detailsButton.setSelected(!detailsButton.isSelected());
        detailsButton.setIcon(detailsButton.isSelected() ? ARROW_UP_ICON : ARROW_DOWN_ICON);
        detailsButton.setToolTipText(detailsButton.isSelected() ? "Close Details" : "Open Details");
        FormLayoutUtils.hideOrRestoreRow(detailsPanel, !detailsButton.isSelected());
    }

    private void separatorPanelMouseClicked()
    {
        detailsButtonActionPerformed();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var label = new JLabel();
        var separatorPanel = new JPanel();
        var separator = new JSeparator();
        detailsButton = new JButton();
        detailsPanel = new JPanel();
        var detailsForm = new JPanel();
        var rangeLabel = new JLabel();
        var rangePanel = new JPanel();
        rangeKmValue = new JLabel();
        rangeMiValue = new JLabel();
        rangeNmValue = new JLabel();
        var bearingLabel = new JLabel();
        var bearingPanel = new JPanel();
        bearingDegMValue = new JLabel();
        bearingDegTValue = new JLabel();
        var bearingPanel2 = new JPanel();
        bearingMilMValue = new JLabel();
        bearingMilTValue = new JLabel();
        directionValue = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, default",
                "12dlu, $lgap, fill:default:grow"));

        //---- label ----
        label.setText("From You");
        label.setName("label");
        add(label, cc.xy(1, 1));

        //======== separatorPanel ========
        {
            separatorPanel.setName("separatorPanel");
            separatorPanel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    separatorPanelMouseClicked();
                }
            });
            separatorPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:12dlu"));

            //---- separator ----
            separator.setForeground(UIManager.getColor("Component.borderColor"));
            separator.setName("separator");
            separatorPanel.add(separator, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.CENTER));
        }
        add(separatorPanel, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));

        //---- detailsButton ----
        detailsButton.setContentAreaFilled(false);
        detailsButton.setBackground(null);
        detailsButton.setBorderPainted(false);
        detailsButton.setBorder(null);
        detailsButton.setName("detailsButton");
        detailsButton.addActionListener(e -> detailsButtonActionPerformed());
        add(detailsButton, cc.xy(5, 1));

        //======== detailsPanel ========
        {
            detailsPanel.setBorder(new CompoundBorder(
                    new MatteBorder(0, 1, 1, 1, UIManager.getColor("Component.borderColor")),
                    Borders.DLU2_BORDER));
            detailsPanel.setName("detailsPanel");
            detailsPanel.setLayout(new BorderLayout());

            //======== detailsForm ========
            {
                detailsForm.setName("detailsForm");
                detailsForm.setLayout(new FormLayout(
                        "17dlu, default:grow",
                        "top:12dlu, 12dlu, fill:12dlu"));

                //---- rangeLabel ----
                rangeLabel.setText("R:");
                rangeLabel.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
                        new EmptyBorder(5, 5, 5, 5)));
                rangeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                rangeLabel.setName("rangeLabel");
                detailsForm.add(rangeLabel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                //======== rangePanel ========
                {
                    rangePanel.setName("rangePanel");
                    rangePanel.setLayout(new FormLayout(
                            "3*(default:grow)",
                            "top:12dlu"));
                    ((FormLayout) rangePanel.getLayout()).setColumnGroups(new int[][]{{1, 2, 3}});

                    //---- rangeKmValue ----
                    rangeKmValue.setText("3.5km");
                    rangeKmValue.setHorizontalAlignment(SwingConstants.CENTER);
                    rangeKmValue.setBorder(new CompoundBorder(
                            new MatteBorder(0, 1, 1, 0, UIManager.getColor("Component.borderColor")),
                            new EmptyBorder(5, 5, 5, 5)));
                    rangeKmValue.setName("rangeKmValue");
                    rangePanel.add(rangeKmValue, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.CENTER));

                    //---- rangeMiValue ----
                    rangeMiValue.setText("2.63mi");
                    rangeMiValue.setBorder(new CompoundBorder(
                            new MatteBorder(0, 1, 1, 0, UIManager.getColor("Component.borderColor")),
                            new EmptyBorder(5, 5, 5, 5)));
                    rangeMiValue.setHorizontalAlignment(SwingConstants.CENTER);
                    rangeMiValue.setName("rangeMiValue");
                    rangePanel.add(rangeMiValue, cc.xy(2, 1, CellConstraints.FILL, CellConstraints.FILL));

                    //---- rangeNmValue ----
                    rangeNmValue.setText("2.21nm");
                    rangeNmValue.setBorder(new CompoundBorder(
                            new MatteBorder(0, 1, 1, 0, UIManager.getColor("Component.borderColor")),
                            new EmptyBorder(5, 5, 5, 5)));
                    rangeNmValue.setHorizontalAlignment(SwingConstants.CENTER);
                    rangeNmValue.setName("rangeNmValue");
                    rangePanel.add(rangeNmValue, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));
                }
                detailsForm.add(rangePanel, cc.xy(2, 1));

                //---- bearingLabel ----
                bearingLabel.setText("B:");
                bearingLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
                bearingLabel.setHorizontalAlignment(SwingConstants.CENTER);
                bearingLabel.setName("bearingLabel");
                detailsForm.add(bearingLabel, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.DEFAULT));

                //======== bearingPanel ========
                {
                    bearingPanel.setName("bearingPanel");
                    bearingPanel.setLayout(new FormLayout(
                            "default:grow, $lcgap, default:grow",
                            "default:grow"));
                    ((FormLayout) bearingPanel.getLayout()).setColumnGroups(new int[][]{{1, 3}});

                    //---- bearingDegMValue ----
                    bearingDegMValue.setHorizontalAlignment(SwingConstants.CENTER);
                    bearingDegMValue.setBorder(new CompoundBorder(
                            new MatteBorder(0, 1, 0, 1, UIManager.getColor("Component.borderColor")),
                            new EmptyBorder(5, 5, 5, 5)));
                    bearingDegMValue.setText("text");
                    bearingDegMValue.setName("bearingDegMValue");
                    bearingPanel.add(bearingDegMValue, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.CENTER));

                    //---- bearingDegTValue ----
                    bearingDegTValue.setHorizontalAlignment(SwingConstants.CENTER);
                    bearingDegTValue.setBorder(new EmptyBorder(5, 5, 5, 5));
                    bearingDegTValue.setText("text");
                    bearingDegTValue.setName("bearingDegTValue");
                    bearingPanel.add(bearingDegTValue, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));
                }
                detailsForm.add(bearingPanel, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

                //======== bearingPanel2 ========
                {
                    bearingPanel2.setName("bearingPanel2");
                    bearingPanel2.setLayout(new FormLayout(
                            "default:grow, $lcgap, default:grow",
                            "default:grow"));
                    ((FormLayout) bearingPanel2.getLayout()).setColumnGroups(new int[][]{{1, 3}});

                    //---- bearingMilMValue ----
                    bearingMilMValue.setText("text");
                    bearingMilMValue.setHorizontalAlignment(SwingConstants.CENTER);
                    bearingMilMValue.setBorder(new CompoundBorder(
                            new MatteBorder(0, 1, 0, 1, UIManager.getColor("Component.borderColor")),
                            new EmptyBorder(5, 5, 5, 5)));
                    bearingMilMValue.setName("bearingMilMValue");
                    bearingPanel2.add(bearingMilMValue, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

                    //---- bearingMilTValue ----
                    bearingMilTValue.setText("text");
                    bearingMilTValue.setHorizontalAlignment(SwingConstants.CENTER);
                    bearingMilTValue.setBorder(new EmptyBorder(5, 5, 5, 5));
                    bearingMilTValue.setName("bearingMilTValue");
                    bearingPanel2.add(bearingMilTValue, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));
                }
                detailsForm.add(bearingPanel2, cc.xy(2, 3));

                //---- directionValue ----
                directionValue.setText("W");
                directionValue.setBorder(new EmptyBorder(5, 5, 5, 5));
                directionValue.setHorizontalAlignment(SwingConstants.CENTER);
                directionValue.setFont(new Font("Inter", Font.BOLD, 12));
                directionValue.setName("directionValue");
                detailsForm.add(directionValue, cc.xy(1, 3));
            }
            detailsPanel.add(detailsForm, BorderLayout.CENTER);
        }
        add(detailsPanel, cc.xywh(1, 3, 5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JButton detailsButton;
    private JPanel detailsPanel;
    private JLabel rangeKmValue;
    private JLabel rangeMiValue;
    private JLabel rangeNmValue;
    private JLabel bearingDegMValue;
    private JLabel bearingDegTValue;
    private JLabel bearingMilMValue;
    private JLabel bearingMilTValue;
    private JLabel directionValue;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
