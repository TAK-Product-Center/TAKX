package gov.takx.core.view.gui.maptools;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.ColorChooserUtils;
import net.java.dev.colorchooser.ColorChooser;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import java.awt.Color;

/**
 */
public class PingPanel extends JPanel implements IToolEditorPanel
{
    public PingPanel()
    {
        initComponents();

        pingColorChooser.setPalettes(ColorChooserUtils.getDefaultPalettes());
        pingColorChooser.setColor(Color.BLUE);
    }

    @Override
    public JPanel getPanel()
    {
        return this;
    }

    @Override
    public void releaseResources()
    {
        // No resources
    }

    public Color getColor()
    {
        return pingColorChooser.getColor();
    }

    public int getDuration()
    {
        return (Integer) timeSpinner.getValue() * 1000;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var colorLabel = new JLabel();
        pingColorChooser = new ColorChooser();
        var durationField = new JLabel();
        timeSpinner = new JSpinner();
        var panel2 = new JPanel();
        var seconds = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new TitledBorder(null, "Geo Ping", TitledBorder.LEADING, TitledBorder.TOP));
        setName("panelPing");
        setLayout(new FormLayout(
                "2*(default, $lcgap), default:grow",
                "default, $lgap, default"));

        //---- colorLabel ----
        colorLabel.setText("Color:");
        colorLabel.setName("colorLabel");
        add(colorLabel, cc.xy(1, 1));

        //---- pingColorChooser ----
        pingColorChooser.setColor(Color.white);
        pingColorChooser.setName("pingColorChooser");
        add(pingColorChooser, cc.xywh(3, 1, 3, 1));

        //---- durationField ----
        durationField.setText("Duration:");
        durationField.setName("durationField");
        add(durationField, cc.xy(1, 3));

        //---- timeSpinner ----
        timeSpinner.setModel(new SpinnerNumberModel(10, 1, null, 1));
        timeSpinner.setName("timeSpinner");
        add(timeSpinner, cc.xy(3, 3));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "3*(default, $lcgap), default",
                    "default"));

            //---- seconds ----
            seconds.setText("seconds");
            seconds.setLabelFor(timeSpinner);
            seconds.setName("seconds");
            panel2.add(seconds, cc.xy(1, 1));
        }
        add(panel2, cc.xy(5, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private ColorChooser pingColorChooser;
    private JSpinner timeSpinner;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
