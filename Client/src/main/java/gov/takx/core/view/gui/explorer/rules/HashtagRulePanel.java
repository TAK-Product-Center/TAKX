package gov.takx.core.view.gui.explorer.rules;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.explorer.AExplorableRulePanel;
import gov.takx.commons.swing.CombinedDocumentListener;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.core.explorer.controller.ExplorerHashtagController;
import gov.takx.core.explorer.rules.HashtagRule;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTextField;

import javax.swing.JLabel;

/**
 * Panel for editing a hashtag rule.
 *
 * @since 5.8
 */
public class HashtagRulePanel extends AExplorableRulePanel
{
    private final HashtagRule rule;

    /**
     * Construct a new panel, using the given rule to fill in the name field.
     *
     * @param rule a hashtag rule
     */
    public HashtagRulePanel(@Nonnull HashtagRule rule)
    {
        initComponents();
        this.rule = rule;
        nameField.setDocument(new LimitedDocument(255, ExplorerHashtagController::isValidCharacter, String::toLowerCase));
        nameField.setText(rule.getName());
        nameField.getDocument().addDocumentListener((CombinedDocumentListener) e -> setDirty());
    }

    @Override
    public void applyAction()
    {
        rule.setName(nameField.getText());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var nameLabel = new JLabel();
        nameField = new JXTextField();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "default"));

        //---- nameLabel ----
        nameLabel.setText("Hashtag:");
        nameLabel.setName("nameLabel");
        add(nameLabel, cc.xy(1, 1));

        //---- nameField ----
        nameField.setPrompt("Tag name, not including #");
        nameField.setName("nameField");
        add(nameField, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JXTextField nameField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
