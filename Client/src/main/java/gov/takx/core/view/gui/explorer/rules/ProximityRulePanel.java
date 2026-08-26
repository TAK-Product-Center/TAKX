package gov.takx.core.view.gui.explorer.rules;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.explorer.AExplorableRulePanel;
import gov.takx.api.explorer.IExplorable;
import gov.takx.core.explorer.rules.ProximityRule;
import jakarta.annotation.Nonnull;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import java.util.List;

/**
 * Panel for proximity match rule.  A locatable explorable is selected and any other locatable explorables within
 * a given radius are positively matched.
 *
 * @since 1.48
 */
public class ProximityRulePanel extends AExplorableRulePanel
{
    private final ProximityRule rule;

    @SuppressWarnings("unchecked")
    public ProximityRulePanel(ProximityRule rule,
                              IExplorable selected,
                              List<IExplorable> explorableList,
                              int maxDistance)
    {
        this.rule = rule;
        initComponents();
        selectedItemComboBox.addItem(new ExplorableWrapper(null));
        for (IExplorable explorable : explorableList) selectedItemComboBox.addItem(new ExplorableWrapper(explorable));
        maxDistanceSpinner.setValue(maxDistance);
        setSelectedItem(selected);

        maxDistanceSpinner.addChangeListener(e -> setDirty());
    }

    @Override
    public void applyAction()
    {
        rule.setSelectedExplorable(getSelectedItem());
        rule.setMaxDistanceMeters((Integer) maxDistanceSpinner.getValue());
    }

    private IExplorable getSelectedItem()
    {
        return ((ExplorableWrapper) selectedItemComboBox.getSelectedItem()).explorable;
    }

    private void setSelectedItem(IExplorable explorable)
    {
        if (explorable == null) return;
        int itemCount = selectedItemComboBox.getItemCount();
        for (int i = 0; i < itemCount; i++)
        {
            ExplorableWrapper wrapper = (ExplorableWrapper) selectedItemComboBox.getItemAt(i);
            if (wrapper.explorable != null && wrapper.explorable.equals(explorable))
            {
                selectedItemComboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectedItemComboBoxActionPerformed()
    {
        setDirty();
    }

    private record ExplorableWrapper(IExplorable explorable)
    {
        @Nonnull
        @Override
        public String toString()
        {
            return explorable != null ? explorable.getDisplayName() : "None";
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var isWithinLabel = new JLabel();
        var panel1 = new JPanel();
        maxDistanceSpinner = new JSpinner();
        var metersLabel = new JLabel();
        var selectedItemLabel = new JLabel();
        selectedItemComboBox = new JComboBox();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelProximityView");
        setLayout(new FormLayout(
                "default, $lcgap, default",
                "default, $lgap, default"));

        //---- isWithinLabel ----
        isWithinLabel.setText("Is within");
        isWithinLabel.setName("isWithinLabel");
        add(isWithinLabel, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $lcgap, default",
                    "default"));

            //---- maxDistanceSpinner ----
            maxDistanceSpinner.setModel(new SpinnerNumberModel(1, 1, 999999, 1));
            maxDistanceSpinner.setName("maxDistanceSpinner");
            panel1.add(maxDistanceSpinner, cc.xy(1, 1));

            //---- metersLabel ----
            metersLabel.setText("meters");
            metersLabel.setName("metersLabel");
            panel1.add(metersLabel, cc.xy(3, 1));
        }
        add(panel1, cc.xy(3, 1));

        //---- selectedItemLabel ----
        selectedItemLabel.setText("Of Item: ");
        selectedItemLabel.setName("selectedItemLabel");
        add(selectedItemLabel, cc.xy(1, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- selectedItemComboBox ----
        selectedItemComboBox.setName("selectedItemComboBox");
        selectedItemComboBox.addActionListener(e -> selectedItemComboBoxActionPerformed());
        add(selectedItemComboBox, cc.xy(3, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JSpinner maxDistanceSpinner;
    private JComboBox selectedItemComboBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
