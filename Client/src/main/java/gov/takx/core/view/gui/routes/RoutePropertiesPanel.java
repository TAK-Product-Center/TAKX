package gov.takx.core.view.gui.routes;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.cot.route.RouteDirection;
import gov.tak.api.cot.route.RouteMethod;
import gov.tak.api.cot.route.RouteOrder;
import gov.tak.api.cot.route.RouteType;
import gov.takx.api.shapes.IRoute;
import gov.takx.commons.swing.ColorChooserUtils;
import gov.takx.commons.swing.DelegatingListCellRenderer;
import gov.takx.dataservices.shapes.Route;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.java.dev.colorchooser.ColorChooser;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * Panel containing properties of a route that are not CP/WP related, used for creating or editing a route.
 *
 * @since 3.3
 */
public class RoutePropertiesPanel extends JPanel
{
    private final List<FinishListener> finishListeners = new ArrayList<>();
    private final PropertyChangeListener propertyChangeListener = pce -> modelChanged();
    private IRoute route;

    /**
     * Construct a new panel with no route information.
     *
     * @since 5.7
     */
    public RoutePropertiesPanel()
    {
        initComponents();

        routeMethodComboBox.setModel(new DefaultComboBoxModel<>(RouteMethod.values()));
        routeMethodComboBox.setRenderer(new DelegatingListCellRenderer<>(RouteMethod::getDisplayText));

        routeDirectionComboBox.setModel(new DefaultComboBoxModel<>(RouteDirection.values()));
        routeDirectionComboBox.setRenderer(new DelegatingListCellRenderer<>(RouteDirection::getDisplayText));

        routeOrderComboBox.setModel(new DefaultComboBoxModel<>(RouteOrder.values()));
        routeOrderComboBox.setRenderer(new DelegatingListCellRenderer<>(RouteOrder::getDisplayText));

        routeTypeComboBox.setModel((new DefaultComboBoxModel<>(RouteType.values())));
        routeTypeComboBox.setRenderer(new DelegatingListCellRenderer<>(RouteType::getDisplayText));

        colorChooser.setPalettes(ColorChooserUtils.getDefaultPalettes());
    }

    /**
     * @param text the text String for the OK button
     * @since 5.7
     */
    public void setOkButtonText(@Nullable String text)
    {
        okButton.setText(text);
    }

    /**
     * @return the text String for the OK button
     * @since 5.7
     */
    @Nullable
    public String getOkButtonText()
    {
        return okButton.getText();
    }

    /**
     * @param listener the listener to add
     * @since 5.7
     */
    public void addFinishListener(@Nonnull FinishListener listener)
    {
        finishListeners.add(listener);
    }

    /**
     * @param listener the listener to remove
     * @since 5.7
     */
    public void removeFinishListener(@Nonnull FinishListener listener)
    {
        finishListeners.remove(listener);
    }

    /**
     * @param route the route to use for populating fields
     * @since 5.7
     */
    public void setRoute(@Nullable IRoute route)
    {
        if (this.route != null) this.route.removePropertyChangeListener(propertyChangeListener);
        this.route = route;
        if (route != null)
        {
            colorChooserPanel.setVisible(false);
            modelChanged();
            this.route.addPropertyChangeListener(propertyChangeListener);
        }
    }

    /**
     * @return the route used for populating fields
     * @since 5.7
     */
    @Nullable
    public IRoute getRoute()
    {
        return route;
    }

    private void modelChanged()
    {
        routeMethodComboBox.setSelectedItem(route.getMethod());
        routeDirectionComboBox.setSelectedItem(route.getDirection());
        routeOrderComboBox.setSelectedItem(route.getRouteOrder());
        routeTypeComboBox.setSelectedItem(route.getRouteType());
        colorChooser.setColor(route.getLineColor());
    }

    @SuppressWarnings("DataFlowIssue")
    private void okButtonActionPerformed(ActionEvent e)
    {
        if (route != null) route.removePropertyChangeListener(propertyChangeListener);
        // Bundle our UI settings into a route model
        var route = new Route();
        route.setLineColor(colorChooser.getColor());
        route.setRouteOrder((RouteOrder) routeOrderComboBox.getSelectedItem());
        route.setDirection((RouteDirection) routeDirectionComboBox.getSelectedItem());
        route.setRouteType((RouteType) routeTypeComboBox.getSelectedItem());
        route.setMethod((RouteMethod) routeMethodComboBox.getSelectedItem());
        this.route = route;
        finishListeners.forEach(l -> l.finished(e));
    }

    private void cancelButtonActionPerformed(ActionEvent e)
    {
        if (route != null) route.removePropertyChangeListener(propertyChangeListener);
        route = null;
        finishListeners.forEach(l -> l.finished(e));
    }

    /**
     * Listener invoked when the user clicks the OK or Cancel button.
     *
     * @since 5.7
     */
    @FunctionalInterface
    public interface FinishListener extends EventListener
    {
        /**
         * @param event the event indicating that the user has clicked the OK or Cancel buttons during route drawing
         */
        void finished(@Nonnull ActionEvent event);
    }

    @SuppressWarnings({"Convert2MethodRef", "RedundantExplicitVariableType"})
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        colorChooserPanel = new JPanel();
        colorChooser = new ColorChooser();
        routeMethodComboBox = new JComboBox<>();
        routeDirectionComboBox = new JComboBox<>();
        routeTypeComboBox = new JComboBox<>();
        routeOrderComboBox = new JComboBox<>();
        var buttonPanel = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "2*(default), 4*($lgap, default)"));

        //======== colorChooserPanel ========
        {
            colorChooserPanel.setName("colorChooserPanel");
            colorChooserPanel.setLayout(new FormLayout(
                    "pref:grow",
                    "default, $lgap"));

            //---- colorChooser ----
            colorChooser.setName("colorChooser");
            colorChooserPanel.add(colorChooser, cc.xy(1, 1));
        }
        add(colorChooserPanel, cc.xy(1, 1));

        //---- routeMethodComboBox ----
        routeMethodComboBox.putClientProperty("FlatLaf.styleClass", "large");
        routeMethodComboBox.setName("routeMethodComboBox");
        add(routeMethodComboBox, cc.xy(1, 2));

        //---- routeDirectionComboBox ----
        routeDirectionComboBox.putClientProperty("FlatLaf.styleClass", "large");
        routeDirectionComboBox.setName("routeDirectionComboBox");
        add(routeDirectionComboBox, cc.xy(1, 4));

        //---- routeTypeComboBox ----
        routeTypeComboBox.putClientProperty("FlatLaf.styleClass", "large");
        routeTypeComboBox.setName("routeTypeComboBox");
        add(routeTypeComboBox, cc.xy(1, 6));

        //---- routeOrderComboBox ----
        routeOrderComboBox.putClientProperty("FlatLaf.styleClass", "large");
        routeOrderComboBox.setName("routeOrderComboBox");
        add(routeOrderComboBox, cc.xy(1, 8));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "default:grow, $lcgap, default:grow",
                    "default"));

            //---- okButton ----
            okButton.setText("OK");
            okButton.setName("okButton");
            okButton.addActionListener(e -> okButtonActionPerformed(e));
            buttonPanel.add(okButton, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.setName("cancelButton");
            cancelButton.addActionListener(e -> cancelButtonActionPerformed(e));
            buttonPanel.add(cancelButton, cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
        }
        add(buttonPanel, cc.xy(1, 10));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel colorChooserPanel;
    private ColorChooser colorChooser;
    private JComboBox<RouteMethod> routeMethodComboBox;
    private JComboBox<RouteDirection> routeDirectionComboBox;
    private JComboBox<RouteType> routeTypeComboBox;
    private JComboBox<RouteOrder> routeOrderComboBox;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
