package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.ui.IDeviceSwingUIProvider;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.uibridge.SwingUIBinder;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectHelpDialog extends AMapObjectDialog
{

    @Inject
    private SwingUIBinder swingUIBinder;

    public MapObjectHelpDialog(IMapObjectController moc)
    {
        super(moc);

        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        final IMapObject mapObject = mapObjectController.getMapObject();
        setTitle(mapObject.getType() + " - (" + mapObject.getName() + ") Help");

        final IDeviceSwingUIProvider uiProvider = swingUIBinder.forDevice(mapObjectController);
        JPanel panel = uiProvider.getPluginHelpPanel();

        if (null != panel)
        {
            contentPanel.add(panel, BorderLayout.CENTER);
            if (panel instanceof IResourceCleaner resourceCleaner) addResourceCleaner(resourceCleaner);
        }

        pack();
    }

    private void okButtonActionPerformed()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        contentPanel = new JPanel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(300, 50));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new BorderLayout());
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel contentPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
