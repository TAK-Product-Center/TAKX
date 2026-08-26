package gov.takx.core.view.gui.routes;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.event.ActionListener;

/**
 * Small panel to inform the user that route drawing is in progress, and contain buttons for finishing or cancelling route creation
 *
 * @since 3.3
 */
public class RouteDrawingInProgressPanel extends JPanel
{
    /**
     * Create a panel to show that Route drawing is currently in progress
     *
     * @param finishAction An {@link ActionListener} for when the "Finish" button is pressed
     * @param cancelAction An {@link ActionListener} for when the "Cancel" button is pressed
     */
    public RouteDrawingInProgressPanel(@Nonnull ActionListener finishAction, @Nonnull ActionListener cancelAction)
    {
        initComponents();
        finishButton.addActionListener(finishAction);
        cancelButton.addActionListener(cancelAction);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var label1 = new JLabel();
        var label2 = new JLabel();
        var label3 = new JLabel();
        var label4 = new JLabel();
        var label5 = new JLabel();
        finishButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "center:default:grow, $lcgap, center:default:grow",
                "5*(default, $lgap), default"));

        //---- label1 ----
        label1.setText("Route drawing in progress.");
        label1.setName("label1");
        add(label1, cc.xywh(1, 1, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- label2 ----
        label2.setText("Click the map to add a point.");
        label2.setName("label2");
        add(label2, cc.xywh(1, 3, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- label3 ----
        label3.setText("Double-click to add a check point.");
        label3.setName("label3");
        add(label3, cc.xywh(1, 5, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- label4 ----
        label4.setText("Ctrl+click a point to remove it.");
        label4.setName("label4");
        add(label4, cc.xywh(1, 7, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- label5 ----
        label5.setText("Shift + click to promote an existing point.");
        label5.setName("label5");
        add(label5, cc.xywh(1, 9, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- finishButton ----
        finishButton.setText("Finish");
        finishButton.setName("finishButton");
        add(finishButton, cc.xy(1, 11));

        //---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.setToolTipText("Cancel route drawing.");
        cancelButton.setName("cancelButton");
        add(cancelButton, cc.xy(3, 11));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton finishButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
