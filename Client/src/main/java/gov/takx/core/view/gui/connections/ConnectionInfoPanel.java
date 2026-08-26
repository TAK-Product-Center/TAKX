package gov.takx.core.view.gui.connections;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import java.awt.Font;

/**
 */
public class ConnectionInfoPanel extends JPanel
{

    public ConnectionInfoPanel(String name, String status, ImageIcon icon, String message)
    {
        initComponents();
        labelName.setText(name);
        labelIcon.setIcon(icon);
        if (message != null)
        {
            labelStatus.setText(message);
        } else
        {
            labelStatus.setText(status);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        labelIcon = new JLabel();
        var panelInfo = new JPanel();
        labelName = new JLabel();
        labelStatus = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EtchedBorder());
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "top:default"));

        //---- labelIcon ----
        labelIcon.setName("labelIcon");
        add(labelIcon, cc.xy(1, 1));

        //======== panelInfo ========
        {
            panelInfo.setName("panelInfo");
            panelInfo.setLayout(new FormLayout(
                    "default:grow",
                    "default, fill:default"));

            //---- labelName ----
            labelName.setText("Name");
            labelName.setFont(labelName.getFont().deriveFont(labelName.getFont().getStyle() | Font.BOLD));
            labelName.setName("labelName");
            panelInfo.add(labelName, cc.xy(1, 1));

            //---- labelStatus ----
            labelStatus.setText("Status");
            labelStatus.putClientProperty("FlatLaf.styleClass", "medium");
            labelStatus.setName("labelStatus");
            panelInfo.add(labelStatus, cc.xy(1, 2));
        }
        add(panelInfo, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel labelIcon;
    private JLabel labelName;
    private JLabel labelStatus;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("connection panel");

            frame.add(new ConnectionInfoPanel("TEST", "Error", null, "my long long long long  long long long long"));
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
