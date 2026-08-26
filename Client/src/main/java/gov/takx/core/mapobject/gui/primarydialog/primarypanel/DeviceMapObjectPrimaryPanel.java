package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.api.ui.DevicePanelMetadataKeys;
import gov.takx.api.ui.IDeviceSwingUIProvider;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.ChipField;
import gov.takx.commons.swing.FormLayoutUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;

/**
 * The Panel for the old "Properties" panel header and "Open" dialog
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DeviceMapObjectPrimaryPanel extends AMapObjectPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<String> propertyList = Arrays.asList(
            IIconMapObject.CONTAINED_ADDED,
            IIconMapObject.CONTAINED_REMOVED,
            IIconMapObject.ALLOW_DEVICE_CONTAINMENT);

    private static final CellConstraints CELL_CONSTRAINTS = new CellConstraints();

    private JPanel pluginPanel;

    public DeviceMapObjectPrimaryPanel(IMapObjectController controller)
    {
        this(controller, false);
    }

    public DeviceMapObjectPrimaryPanel(IMapObjectController controller, boolean forDialog)
    {
        super(controller);

        initComponents();

        cancelButton.setVisible(forDialog);

        // We can't add this listener until AFTER the UI components have been instantiated (above)
        // because the property change listener fires, trying to update via setLocation and the location
        // label didn't yet exist.
        addProperties(propertyList);

        // For contained MapObjects
        containerList.setTextProvider(IMapObject::getName);
        containerList.setIconProvider(mo -> mo.getSizedIcon(SMALL_ICON_WIDTH));
    }

    @PostConstruct
    private void postConstruct()
    {
        final IDeviceSwingUIProvider deviceUiProvider = swingUIBinder.forDevice(mapObjectController);

        // Check to see if the developer has properly implemented the IRaptorPanel to return a JPanel.
        // If not, check to see if the primary panel is the JPanel. We used to assume that the primary
        // panel was a JPanel and have coded our sample plugins that way, but that is not a valid assumption
        // given our API contract. (CORE-3174)
        IRaptorPanel primaryPanel = deviceUiProvider.getPrimaryPanel();

        if (primaryPanel == null)
        {
            pluginPanel = null;
        } else
        {
            final JPanel panelFromPanel = primaryPanel.getPanel();

            pluginPanel = panelFromPanel == null && primaryPanel instanceof JPanel
                    ? (JPanel) primaryPanel
                    : panelFromPanel;
        }

        // Add the plugins' custom panel, if provided
        if (pluginPanel != null)
        {
            panelCustomAndContained.add(pluginPanel, CELL_CONSTRAINTS.xy(1, 1));
        }

        updateForContainment(); // Add containment panel, if the device can contain other MOs

        mapObjectController.addPropertyChangeListener(this);

        // Add data monitors to UI (if necessary)
        if (!mapObjectController.getDataMonitors().isEmpty()
                && mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_DATA_MONITORS, true))
        {
            dataMonitorsDetailsPanel.setMapObject(mapObject);
        } else
        {
            FormLayoutUtils.hideColumn(dataMonitorsDetailsPanel);
        }

        final boolean isDevice = mapObject.isDevice();

        // Optionally enable help button.
        final JPanel helpPanel = deviceUiProvider.getPluginHelpPanel();
        final boolean enableHelp = isDevice && helpPanel != null;

        // Since we just created it to see if it was non-null, we need to release resources on it
        if (helpPanel instanceof IResourceCleaner resourceCleaner) resourceCleaner.releaseResources();

        helpButton.setEnabled(enableHelp);
        helpButton.setVisible(enableHelp);
    }

    /**
     * Display map object configuration dialog with any custom panels.
     */
    private void configureButtonActionPerformed()
    {
        mapObjectController.showConfigurationDialog();
    }

    private void helpButtonActionPerformed()
    {
        mapObjectController.showHelpDialog();
    }

    private void cancelButtonActionPerformed()
    {
        requestClose();
    }

    /**
     * Update our UI to show or hide the contained MOs panel.  If the contained MOs panel should be visible,
     * its model is recreated with the current list of contained MOs.
     *
     * @since 2.37
     */
    private void updateForContainment()
    {
        if (mapObject.isAllowDeviceToBeContainer() && mapObject instanceof IIconMapObject iconMapObject)
        {
            // Create a new model with all the children currently contained by the map object
            containerList.setModelElements(iconMapObject.getContainedMapObjects());
            panelCustomAndContained.add(panelContainedMapObjects, CELL_CONSTRAINTS.xy(1, 2));
        } else
        {
            panelCustomAndContained.remove(panelContainedMapObjects);
        }
        revalidate();
    }

    @InvokeOnEDT
    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();

        if (propertyName.equals(IIconMapObject.CONTAINED_ADDED))
        {
            containerList.addModelElement((IMapObject) evt.getNewValue());
        } else if (propertyName.equals(IIconMapObject.CONTAINED_REMOVED))
        {
            containerList.removeModelElement((IMapObject) evt.getNewValue());
        } else if (propertyName.equals(IIconMapObject.ALLOW_DEVICE_CONTAINMENT))
        {
            updateForContainment();

            // Force the window containing this panel (if any) to update its layout
            // as well; this is necessary especially when the panel gets bigger, so that
            // it doesn't end up cut off at the bottom.
            Window window = SwingUtilities.windowForComponent(this);
            if (window != null)
            {
                window.pack();
            }
        }
    }

    @Override
    public void releaseResources()
    {
        mapObjectController.removePropertyChangeListener(this);
        dataMonitorsDetailsPanel.releaseResources();

        if (pluginPanel instanceof IResourceCleaner resourceCleaner) resourceCleaner.releaseResources();

        super.releaseResources();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel5 = new JPanel();
        dataMonitorsDetailsPanel = new DataMonitorsDetailsPanel();
        var panel6 = new JPanel();
        panelCustomAndContained = new JPanel();
        var buttonPanelPrimaryButtons = new JPanel();
        var configureButton = new JButton();
        helpButton = new JButton();
        cancelButton = new JButton();
        panelContainedMapObjects = new JPanel();
        var label1 = new JLabel();
        containerList = new ChipField<>();
        panelStatusHolder = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== panel5 ========
        {
            panel5.setName("panel5");
            panel5.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "fill:default:grow"));

            //---- dataMonitorsDetailsPanel ----
            dataMonitorsDetailsPanel.setName("dataMonitorsDetailsPanel");
            panel5.add(dataMonitorsDetailsPanel, cc.xy(1, 1));

            //======== panel6 ========
            {
                panel6.setName("panel6");
                panel6.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default"));

                //======== panelCustomAndContained ========
                {
                    panelCustomAndContained.setName("panelCustomAndContained");
                    panelCustomAndContained.setLayout(new FormLayout(
                            "default:grow",
                            "2*(fill:default)"));
                }
                panel6.add(panelCustomAndContained, cc.xy(1, 1));
            }
            panel5.add(panel6, cc.xy(3, 1));
        }
        add(panel5, BorderLayout.CENTER);

        //======== buttonPanelPrimaryButtons ========
        {
            buttonPanelPrimaryButtons.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
            buttonPanelPrimaryButtons.setName("buttonPanelPrimaryButtons");
            buttonPanelPrimaryButtons.setLayout(new FormLayout(
                    "$button, $lcgap, $button, $glue, $button, $lcgap, $button",
                    "default:grow"));

            //---- configureButton ----
            configureButton.setText("Configure");
            configureButton.setName("configureButton");
            configureButton.addActionListener(e -> configureButtonActionPerformed());
            buttonPanelPrimaryButtons.add(configureButton, cc.xy(1, 1));

            //---- helpButton ----
            helpButton.setText("Help");
            helpButton.setName("helpButton");
            helpButton.addActionListener(e -> helpButtonActionPerformed());
            buttonPanelPrimaryButtons.add(helpButton, cc.xy(5, 1));

            //---- cancelButton ----
            cancelButton.setText("Close");
            cancelButton.setVisible(false);
            cancelButton.setName("cancelButton");
            cancelButton.addActionListener(e -> cancelButtonActionPerformed());
            buttonPanelPrimaryButtons.add(cancelButton, cc.xy(7, 1));
        }
        add(buttonPanelPrimaryButtons, BorderLayout.SOUTH);

        //======== panelContainedMapObjects ========
        {
            panelContainedMapObjects.setName("panelContainedMapObjects");
            panelContainedMapObjects.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, fill:default"));

            //---- label1 ----
            label1.setText("Contained Map Objects:");
            label1.setName("label1");
            panelContainedMapObjects.add(label1, cc.xy(1, 1));

            //---- containerList ----
            containerList.setName("containerList");
            panelContainedMapObjects.add(containerList, cc.xy(1, 3));
        }

        //======== panelStatusHolder ========
        {
            panelStatusHolder.setName("panelStatusHolder");
            panelStatusHolder.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DataMonitorsDetailsPanel dataMonitorsDetailsPanel;
    private JPanel panelCustomAndContained;
    private JButton helpButton;
    private JButton cancelButton;
    private JPanel panelContainedMapObjects;
    private ChipField<IMapObject> containerList;
    private JPanel panelStatusHolder;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
