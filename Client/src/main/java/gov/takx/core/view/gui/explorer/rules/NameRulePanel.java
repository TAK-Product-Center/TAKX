package gov.takx.core.view.gui.explorer.rules;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.explorer.AExplorableRulePanel;
import gov.takx.core.explorer.rules.NameRule;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * View panel for {@link NameRule} configuration.
 *
 * @since 1.48
 */
public class NameRulePanel extends AExplorableRulePanel
{
    private final NameRule owner;

    public NameRulePanel(NameRule owner)
    {
        initComponents();
        this.owner = owner;
        updateFromOwner();
        matchStringField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                setDirty();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                setDirty();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                setDirty();
            }
        });
    }

    @Override
    public void applyAction()
    {
        owner.setMatchString(matchStringField.getText());
    }

    private void updateFromOwner()
    {
        if (owner != null) matchStringField.setText(owner.getMatchString());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        var matchStringLabel = new JLabel();
        matchStringField = new JTextField();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelNameRule");
        setLayout(new FormLayout(
                "default:grow",
                "default"));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default"));

            //---- matchStringLabel ----
            matchStringLabel.setText("Match String: ");
            matchStringLabel.setName("matchStringLabel");
            panel1.add(matchStringLabel, cc.xy(1, 1));

            //---- matchStringField ----
            matchStringField.setName("matchStringField");
            panel1.add(matchStringField, cc.xy(3, 1));
        }
        add(panel1, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField matchStringField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
