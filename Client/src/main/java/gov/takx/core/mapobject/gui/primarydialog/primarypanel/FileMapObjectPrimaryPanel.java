package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.core.view.gui.files.LaunchAction;

import javax.swing.JButton;
import javax.swing.JLabel;

import java.awt.Font;
import java.util.Collections;

/**
 */
public class FileMapObjectPrimaryPanel extends AMapObjectPanel
{
    private final IFileMapObject file;

    public FileMapObjectPrimaryPanel(IMapObjectController controller)
    {
        super(controller);
        initComponents();

        file = (IFileMapObject) controller.getMapObject();

        launchButton.addActionListener(new LaunchAction(() -> Collections.singletonList(file)));
        nameLabel.setText(file.getVfsFile().getFileName());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        launchButton = new JButton();
        nameLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "$button, $lcgap, default",
                "default"));

        //---- launchButton ----
        launchButton.setText("Launch");
        launchButton.setToolTipText("Open file in default application");
        launchButton.setName("launchButton");
        add(launchButton, cc.xy(1, 1));

        //---- nameLabel ----
        nameLabel.setText("NewTextDocument.txt");
        nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | Font.BOLD, nameLabel.getFont().getSize() + 1f));
        nameLabel.setName("nameLabel");
        add(nameLabel, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton launchButton;
    private JLabel nameLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
