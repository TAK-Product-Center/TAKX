package gov.takx.core.view.gui.explorer.rules;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.explorer.AExplorableRulePanel;
import gov.takx.api.shapes.IShape;
import gov.takx.core.explorer.rules.GeofenceRule;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * View panel for {@link GeofenceRule} configuration.
 *
 * @since 1.48
 */
public class GeofenceRulePanel extends AExplorableRulePanel
{
    private final GeofenceRule rule;

    @SuppressWarnings("unchecked")
    public GeofenceRulePanel(GeofenceRule geofenceRule, List<IShape> shapes, IShape selectedShape, boolean insideMode)
    {
        this.rule = geofenceRule;
        initComponents();
        shapeComboBox.addItem(new ShapeWrapper(null));
        for (IShape shape : getSortedShapes(shapes)) shapeComboBox.addItem(new ShapeWrapper(shape));
        setSelectedShape(selectedShape);
        setInsideMode(insideMode);
    }

    @Override
    public void applyAction()
    {
        Object item = shapeComboBox.getSelectedItem();
        rule.setSelectedShape(item instanceof ShapeWrapper shapeWrapper ? shapeWrapper.shape : null);
        rule.setInsideMode(isInsideMode());
    }

    private void setSelectedShape(IShape selectedShape)
    {
        if (selectedShape == null) return;
        for (int i = 0; i < shapeComboBox.getItemCount(); i++)
        {
            ShapeWrapper shapeWrapper = (ShapeWrapper) shapeComboBox.getItemAt(i);
            if (shapeWrapper.getKey().equals(selectedShape.getKey()))
            {
                shapeComboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private List<IShape> getSortedShapes(List<IShape> shapes)
    {
        List<IShape> sorted = new ArrayList<>(shapes);
        return sorted.stream().sorted(Comparator.comparing(IShape::getName)).toList();
    }

    private void setInsideMode(boolean insideMode)
    {
        insideModeComboBox.setSelectedIndex(insideMode ? 0 : 1);
    }

    private boolean isInsideMode()
    {
        return insideModeComboBox.getSelectedIndex() == 0;
    }

    private void shapeComboBoxActionPerformed()
    {
        setDirty();
    }

    private void insideModeComboBoxActionPerformed()
    {
        setDirty();
    }

    private class ShapeWrapper
    {
        public final IShape shape;

        public ShapeWrapper(IShape shape)
        {
            this.shape = shape;
        }

        @Override
        public String toString()
        {
            return shape != null ? shape.getName() : "None";
        }

        public String getKey()
        {
            return shape != null ? shape.getKey() : "null";
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panel1 = new JPanel();
        label1 = new JLabel();
        shapeComboBox = new JComboBox();
        label2 = new JLabel();
        panel3 = new JPanel();
        insideModeComboBox = new JComboBox<>();
        label3 = new JLabel();
        panel2 = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelGeofenceRule");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, default"));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default, $lgap, default"));

            //---- label1 ----
            label1.setText("Selected Geofence: ");
            label1.setName("label1");
            panel1.add(label1, cc.xy(1, 1));

            //---- shapeComboBox ----
            shapeComboBox.setName("shapeComboBox");
            shapeComboBox.addActionListener(e -> shapeComboBoxActionPerformed());
            panel1.add(shapeComboBox, cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

            //---- label2 ----
            label2.setText("Match against items ");
            label2.setName("label2");
            panel1.add(label2, cc.xy(1, 3));

            //======== panel3 ========
            {
                panel3.setName("panel3");
                panel3.setLayout(new FormLayout(
                        "default, $lcgap, default",
                        "default"));

                //---- insideModeComboBox ----
                insideModeComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                        "inside",
                        "outside"
                }));
                insideModeComboBox.setName("insideModeComboBox");
                insideModeComboBox.addActionListener(e -> insideModeComboBoxActionPerformed());
                panel3.add(insideModeComboBox, cc.xy(1, 1));

                //---- label3 ----
                label3.setText("geofence");
                label3.setName("label3");
                panel3.add(label3, cc.xy(3, 1));
            }
            panel1.add(panel3, cc.xy(3, 3));
        }
        add(panel1, cc.xy(1, 1));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "2*(default, $lcgap), default",
                    "default"));
        }
        add(panel2, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel1;
    private JLabel label1;
    private JComboBox shapeComboBox;
    private JLabel label2;
    private JPanel panel3;
    private JComboBox<String> insideModeComboBox;
    private JLabel label3;
    private JPanel panel2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
