package gov.takx.platform.io.panels;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.plugin.io.AWizardPanel;
import gov.takx.commons.wizard.IWizardModel;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

import static gov.takx.platform.io.panels.CommonWizardSettings.NEW_MAP_OBJECT;

/**
 * A wizard panel to be used by import wizards for selecting whether to import to a new map object or an existing one.
 *
 * @since 3.0 (renamed from NewOrExistingPanel)
 */
@SuppressWarnings("unused") // Used by plugins
@PseudoBean
public class NewOrExistingMapObjectPanel extends AWizardPanel
{
    public static final String IDENTIFIER = "platform.io.panels.NewOrExistingMapObjectPanel";
    private static final String NEW = "new";
    private static final String EXISTING = "existing";

    private ImportInfoPanel newPanel;
    private ChooseMapObjectPanel existingPanel;
    private final CardLayout layout;

    /**
     * The preconfiguration options provided to initialize this panel.
     */
    private final Map<String, Object> preconfigMap;

    /**
     * Track whether or not a map object was specified in the pre-configuration map.
     */
    private boolean preconfiguredMapObject;

    @Inject
    private IMapObjectManager mapObjectManager;

    /**
     * Constructor.
     *
     * @since 3.0 (delegate parameter removed)
     */
    public NewOrExistingMapObjectPanel()
    {
        this(null);
    }

    /**
     * Constructor.
     *
     * @param preconfigMap Map of configuration parameters for the wizard
     * @since 3.0 (delegate parameter removed)
     */
    public NewOrExistingMapObjectPanel(Map<String, Object> preconfigMap)
    {
        initComponents();
        layout = (CardLayout) panelNewAndImport.getLayout();
        this.preconfigMap = preconfigMap;

        if (preconfigMap != null
                && preconfigMap.get(CommonWizardSettings.MAP_OBJECT_FAMILY) != null
                && preconfigMap.get(CommonWizardSettings.MAP_OBJECT_TYPE) != null
                && preconfigMap.get(CommonWizardSettings.UNIT_ID) != null)
        {
            setPanelIsBeingSkipped(true);
            preconfiguredMapObject = true;
        }
    }

    @Override
    public String getIdentifier()
    {
        return IDENTIFIER;
    }

    @Override
    public void aboutToDisplayPanel()
    {
        IWizardModel model = wizard.getModel();

        if (newPanel == null)
        {
            newPanel = new ImportInfoPanel(model);
        }

        if (existingPanel == null)
        {
            existingPanel = new ChooseMapObjectPanel(model);

            if (preconfiguredMapObject)
            {
                if (existingPanel.selectMapObject(
                        (String) preconfigMap.get(CommonWizardSettings.MAP_OBJECT_FAMILY),
                        (String) preconfigMap.get(CommonWizardSettings.MAP_OBJECT_TYPE),
                        (String) preconfigMap.get(CommonWizardSettings.UNIT_ID)))
                {
                    existingRadioButton.setSelected(true);
                }

                preconfiguredMapObject = false;
            }
        }

        panelNewAndImport.add(newPanel, NEW);
        panelNewAndImport.add(existingPanel, EXISTING);
        layout.show(panelNewAndImport, newRadioButton.isSelected() ? NEW : EXISTING);

        revalidate();
    }

    @Override
    public void displayingPanel()
    {
        existingRadioButton.setEnabled(!mapObjectManager.getMapObjectControllers().isEmpty());
    }

    @Override
    public void aboutToHidePanel()
    {
        if (newRadioButton.isSelected())
        {
            newPanel.updateModel();
        } else
        {
            existingPanel.updateModel();
        }
    }

    private void radioButtonActionPerformed()
    {
        if (newRadioButton.isSelected())
        {
            layout.show(panelNewAndImport, NEW);
            newPanel.validateContents();
        } else
        {
            layout.show(panelNewAndImport, EXISTING);
            wizard.getModel().setNextButtonEnabled(true);
        }
    }

    @Override
    @Nullable
    public Map<String, Object> getPreconfigureProperties()
    {
        Map<String, Object> map = new HashMap<>();

        map.put(NEW_MAP_OBJECT, newRadioButton.isSelected());

        return map;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        textArea1 = new JTextArea();
        newRadioButton = new JRadioButton();
        existingRadioButton = new JRadioButton();
        panelNewAndImport = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $ugap, default, $rgap, fill:default, $ugap, fill:default:grow"));

        //---- textArea1 ----
        textArea1.setText("Do you want to create a new map object for this data, or import the data into an existing map object?");
        textArea1.setWrapStyleWord(true);
        textArea1.setLineWrap(true);
        textArea1.setEditable(false);
        textArea1.setOpaque(false);
        textArea1.setName("textArea1");
        add(textArea1, cc.xy(1, 1));

        //---- newRadioButton ----
        newRadioButton.setText("Create a new map object for this data");
        newRadioButton.setSelected(true);
        newRadioButton.setName("newRadioButton");
        newRadioButton.addActionListener(e -> radioButtonActionPerformed());
        add(newRadioButton, cc.xy(1, 3));

        //---- existingRadioButton ----
        existingRadioButton.setText("Import data into an existing map object");
        existingRadioButton.setName("existingRadioButton");
        existingRadioButton.addActionListener(e -> radioButtonActionPerformed());
        add(existingRadioButton, cc.xy(1, 5));

        //======== panelNewAndImport ========
        {
            panelNewAndImport.setName("panelNewAndImport");
            panelNewAndImport.setLayout(new CardLayout());
        }
        add(panelNewAndImport, cc.xy(1, 7, CellConstraints.DEFAULT, CellConstraints.FILL));

        //---- buttonGroup ----
        var buttonGroup = new ButtonGroup();
        buttonGroup.add(newRadioButton);
        buttonGroup.add(existingRadioButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextArea textArea1;
    private JRadioButton newRadioButton;
    private JRadioButton existingRadioButton;
    private JPanel panelNewAndImport;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
