package gov.takx.core.view.gui.layers.viewmanager.properties;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.persist.ILayerPersist;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.BorderLayout;

/**
 * Dialog allowing the user to view and edit the properties of a layer.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public class LayerPropertiesDialog extends AStandardDialog implements IPanelStateListener
{
    @Inject
    private ILayerPersist layerPersist;

    private final LayerPropertiesPanel panel = new LayerPropertiesPanel();
    private final IPanelStateListener panelStateListener;

    private IRaptorLayer layer;

    /**
     * @param panelStateListener Clean/dirty listener for this panel's state
     */
    public LayerPropertiesDialog(@Nonnull IPanelStateListener panelStateListener)
    {
        this.panelStateListener = panelStateListener;

        initComponents();

        contentPanel.add(panel, new CellConstraints().xy(1, 1));
        panel.setPanelListener(this);

        addResourceCleaner(panel);
    }

    public void setRaptorLayer(IRaptorLayer layer)
    {
        this.layer = layer;
        panel.setRaptorLayer(layer);
        pack();
        setPanelClean();
    }

    @Override
    public void setPanelDirty()
    {
        okButton.setEnabled(true);
        applyButton.setEnabled(true);

        if (panelStateListener != null) panelStateListener.setPanelDirty();
    }

    @Override
    public void setPanelClean()
    {
        applyButton.setEnabled(false);

        if (panelStateListener != null) panelStateListener.setPanelClean();
    }

    private void okButtonActionPerformed()
    {
        applyChanges();
        dispose();
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    @Override
    protected void onWindowClosing()
    {
        setPanelClean();
    }

    private void applyButtonActionPerformed()
    {
        applyChanges();
    }

    private void applyChanges()
    {
        panel.applyAction();
        setPanelClean();

        layerPersist.persistLayer(layer);
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        contentPanel = new JPanel();
        var buttonBar = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        applyButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Layer Properties");
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
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, 2*($button, $rgap), $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setEnabled(false);
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));

                //---- applyButton ----
                applyButton.setText("Apply");
                applyButton.setEnabled(false);
                applyButton.setName("applyButton");
                applyButton.addActionListener(e -> applyButtonActionPerformed());
                buttonBar.add(applyButton, cc.xy(6, 1));
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
    private JButton okButton;
    private JButton applyButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
