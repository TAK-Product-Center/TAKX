package gov.takx.commons.swing.entity;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.ButtonTextHideResizeListener;
import jakarta.annotation.Nonnull;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Panel to hold consistently styled action buttons above entity tables and other main panel components.
 * 
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class ActionButtonPanel extends JPanel
{
    private final CellConstraints cellConstraints = new CellConstraints();
    private final ButtonTextHideResizeListener textHideResizeListener = new ButtonTextHideResizeListener();

    /**
     * Construct a new empty panel.
     */
    public ActionButtonPanel()
    {
        initComponents();
        addComponentListener(textHideResizeListener);
    }

    /**
     * Add a button to this panel and style it to be consistent with other buttons.
     * 
     * @param button the button
     */
    public void addActionButton(@Nonnull AbstractButton button)
    {
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setBackground(null);
        button.setBorder(templateButton.getBorder());
        textHideResizeListener.registerButton(button);
        button.setFocusable(false);
        var formLayout = (FormLayout) getLayout();
        var column = formLayout.getColumnCount();
        formLayout.insertColumn(column, FormFactory.LABEL_COMPONENT_GAP_COLSPEC);
        formLayout.insertColumn(column, new ColumnSpec(ColumnSpec.DEFAULT, Sizes.DEFAULT, ColumnSpec.NO_GROW));
        add(button, cellConstraints.xy(column, 1));
    }

    /**
     * @param enabled {@code true} if all buttons in this component should be enabled, {@code false} otherwise
     */
    public void setEnabled(boolean enabled)
    {
        for (var component : getComponents())
        {
            if (component instanceof AbstractButton button) button.setEnabled(enabled);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var fillerLabel = new JLabel();
        templateButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBackground(null);
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default"));

        //---- fillerLabel ----
        fillerLabel.setName("fillerLabel");
        add(fillerLabel, cc.xy(1, 1));

        //---- templateButton ----
        templateButton.setBorder(new EmptyBorder(3, 6, 3, 9));
        templateButton.setName("templateButton");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JButton templateButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
