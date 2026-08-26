package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.commons.swing.IPanelStateListener;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DeviceCommsPreferencesPanel extends MapObjectMainPreferencesPanel
{
    @Inject
    private IDeviceGateway deviceGateway;

    private Map<CommServiceType, Collection<String>> commPathMap;

    public DeviceCommsPreferencesPanel(IMapObjectController mapObjectController, IPanelStateListener panelListener)
    {
        super("Comms", mapObjectController, panelListener);
    }

    @PostConstruct
    private void postConstruct()
    {
        // get map from device gateway of valid comm paths for this device type
        final String family = mapObjectController.getMapObject().getFamily();
        final String type = mapObjectController.getMapObject().getType();

        commPathMap = deviceGateway.getFramedCommPaths(family, type);

        initComponents();

        initControlsForCommType(serialRadioButton, serialComboBox, CommServiceType.SERIAL);
        initControlsForCommType(tcpClientRadioButton, tcpClientComboBox, CommServiceType.CLIENT_SOCKET);
        initControlsForCommType(tcpServerRadioButton, tcpServerComboBox, CommServiceType.SERVER_SOCKET);
        initControlsForCommType(udpClientRadioButton, udpClientComboBox, CommServiceType.DATAGRAM_CLIENT);
        initControlsForCommType(udpRadioButton, udpComboBox, CommServiceType.DATAGRAM_SOCKET);
        initControlsForCommType(iridiumRadioButton, iridiumComboBox, CommServiceType.IRIDIUM);

        CommServiceType commType = mapObjectController.getDefaultCommType();
        String commName = mapObjectController.getDefaultCommName();

        if (commType == CommServiceType.NONE || "None".equalsIgnoreCase(commName))
        {
            noneRadioButton.setSelected(true);
        } else
        {
            switch (commType)
            {
                case SERIAL:
                    serialRadioButton.setSelected(true);
                    serialComboBox.setSelectedItem(commName);
                    break;
                case CLIENT_SOCKET:
                    tcpClientRadioButton.setSelected(true);
                    tcpClientComboBox.setSelectedItem(commName);
                    break;
                case SERVER_SOCKET:
                    tcpServerRadioButton.setSelected(true);
                    tcpServerComboBox.setSelectedItem(commName);
                    break;
                case DATAGRAM_SOCKET:
                    udpRadioButton.setSelected(true);
                    udpComboBox.setSelectedItem(commName);
                    break;
                case DATAGRAM_CLIENT:
                    udpClientRadioButton.setSelected(true);
                    udpClientComboBox.setSelectedItem(commName);
                    break;
                case IRIDIUM:
                    iridiumRadioButton.setSelected(true);
                    iridiumComboBox.setSelectedItem(commName);
                    break;
            }
        }
    }

    /**
     * For a given comm service type, enable the radio button and combo box if there is data available in the
     * commPathMap.
     *
     * @param button          Button to process
     * @param comboBox        Combo box to process
     * @param commServiceType Comm service type to process
     */
    private void initControlsForCommType(JRadioButton button, JComboBox<String> comboBox, CommServiceType commServiceType)
    {
        if (commPathMap.containsKey(commServiceType))
        {
            button.setVisible(true); // Show the button

            comboBox.setVisible(true); // Show and populate the individual comm paths

            List<String> list = new ArrayList<>(commPathMap.get(commServiceType));
            Collections.sort(list);
            list.forEach(comboBox::addItem);
        }
    }

    @Override
    protected void applyActionPerformed()
    {
        CommServiceType commType = CommServiceType.NONE;
        String commName = "None";

        if (serialRadioButton.isSelected())
        {
            commType = CommServiceType.SERIAL;
            commName = (String) serialComboBox.getSelectedItem();
        } else if (tcpClientRadioButton.isSelected())
        {
            commType = CommServiceType.CLIENT_SOCKET;
            commName = (String) tcpClientComboBox.getSelectedItem();
        } else if (tcpServerRadioButton.isSelected())
        {
            commType = CommServiceType.SERVER_SOCKET;
            commName = (String) tcpServerComboBox.getSelectedItem();
        } else if (udpRadioButton.isSelected())
        {
            commType = CommServiceType.DATAGRAM_SOCKET;
            commName = (String) udpComboBox.getSelectedItem();
        } else if (udpClientRadioButton.isSelected())
        {
            commType = CommServiceType.DATAGRAM_CLIENT;
            commName = (String) udpClientComboBox.getSelectedItem();
        } else if (iridiumRadioButton.isSelected())
        {
            commType = CommServiceType.IRIDIUM;
            commName = (String) iridiumComboBox.getSelectedItem();
        }

        getMapObjectController().updateDefaultCommPath(commType, commName);
    }

    @Override
    public String validateContents()
    {
        return null;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        noneRadioButton = new JRadioButton();
        serialRadioButton = new JRadioButton();
        serialComboBox = new JComboBox<>();
        tcpClientRadioButton = new JRadioButton();
        tcpClientComboBox = new JComboBox<>();
        tcpServerRadioButton = new JRadioButton();
        tcpServerComboBox = new JComboBox<>();
        udpClientRadioButton = new JRadioButton();
        udpClientComboBox = new JComboBox<>();
        udpRadioButton = new JRadioButton();
        udpComboBox = new JComboBox<>();
        gsmRadioButton = new JRadioButton();
        gsmComboBox = new JComboBox<>();
        iridiumRadioButton = new JRadioButton();
        iridiumComboBox = new JComboBox<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new TitledBorder(null, "Default Communication Path", TitledBorder.LEADING, TitledBorder.TOP));
        setName("this");
        setLayout(new FormLayout(
                "2*(default, $lcgap), default",
                "7*(default, $lgap), default"));

        //---- noneRadioButton ----
        noneRadioButton.setText("None");
        noneRadioButton.setName("noneRadioButton");
        add(noneRadioButton, cc.xy(3, 1));

        //---- serialRadioButton ----
        serialRadioButton.setText("Serial");
        serialRadioButton.setVisible(false);
        serialRadioButton.setName("serialRadioButton");
        add(serialRadioButton, cc.xy(3, 3));

        //---- serialComboBox ----
        serialComboBox.setVisible(false);
        serialComboBox.setName("serialComboBox");
        add(serialComboBox, cc.xy(5, 3));

        //---- tcpClientRadioButton ----
        tcpClientRadioButton.setText("TCP Client");
        tcpClientRadioButton.setVisible(false);
        tcpClientRadioButton.setName("tcpClientRadioButton");
        add(tcpClientRadioButton, cc.xy(3, 5));

        //---- tcpClientComboBox ----
        tcpClientComboBox.setVisible(false);
        tcpClientComboBox.setName("tcpClientComboBox");
        add(tcpClientComboBox, cc.xy(5, 5));

        //---- tcpServerRadioButton ----
        tcpServerRadioButton.setText("TCP Server");
        tcpServerRadioButton.setVisible(false);
        tcpServerRadioButton.setName("tcpServerRadioButton");
        add(tcpServerRadioButton, cc.xy(3, 7));

        //---- tcpServerComboBox ----
        tcpServerComboBox.setVisible(false);
        tcpServerComboBox.setName("tcpServerComboBox");
        add(tcpServerComboBox, cc.xy(5, 7));

        //---- udpClientRadioButton ----
        udpClientRadioButton.setText("UDP Client");
        udpClientRadioButton.setVisible(false);
        udpClientRadioButton.setName("udpClientRadioButton");
        add(udpClientRadioButton, cc.xy(3, 9));

        //---- udpClientComboBox ----
        udpClientComboBox.setVisible(false);
        udpClientComboBox.setName("udpClientComboBox");
        add(udpClientComboBox, cc.xy(5, 9));

        //---- udpRadioButton ----
        udpRadioButton.setText("UDP Server");
        udpRadioButton.setVisible(false);
        udpRadioButton.setName("udpRadioButton");
        add(udpRadioButton, cc.xy(3, 11));

        //---- udpComboBox ----
        udpComboBox.setVisible(false);
        udpComboBox.setName("udpComboBox");
        add(udpComboBox, cc.xy(5, 11));

        //---- gsmRadioButton ----
        gsmRadioButton.setText("GSM");
        gsmRadioButton.setVisible(false);
        gsmRadioButton.setName("gsmRadioButton");
        add(gsmRadioButton, cc.xy(3, 13));

        //---- gsmComboBox ----
        gsmComboBox.setVisible(false);
        gsmComboBox.setName("gsmComboBox");
        add(gsmComboBox, cc.xy(5, 13));

        //---- iridiumRadioButton ----
        iridiumRadioButton.setText("Iridium");
        iridiumRadioButton.setVisible(false);
        iridiumRadioButton.setName("iridiumRadioButton");
        add(iridiumRadioButton, cc.xy(3, 15));

        //---- iridiumComboBox ----
        iridiumComboBox.setVisible(false);
        iridiumComboBox.setName("iridiumComboBox");
        add(iridiumComboBox, cc.xy(5, 15));

        //---- buttonGroup1 ----
        ButtonGroup buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(noneRadioButton);
        buttonGroup1.add(serialRadioButton);
        buttonGroup1.add(tcpClientRadioButton);
        buttonGroup1.add(tcpServerRadioButton);
        buttonGroup1.add(udpClientRadioButton);
        buttonGroup1.add(udpRadioButton);
        buttonGroup1.add(gsmRadioButton);
        buttonGroup1.add(iridiumRadioButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JRadioButton noneRadioButton;
    private JRadioButton serialRadioButton;
    private JComboBox<String> serialComboBox;
    private JRadioButton tcpClientRadioButton;
    private JComboBox<String> tcpClientComboBox;
    private JRadioButton tcpServerRadioButton;
    private JComboBox<String> tcpServerComboBox;
    private JRadioButton udpClientRadioButton;
    private JComboBox<String> udpClientComboBox;
    private JRadioButton udpRadioButton;
    private JComboBox<String> udpComboBox;
    private JRadioButton gsmRadioButton;
    private JComboBox<String> gsmComboBox;
    private JRadioButton iridiumRadioButton;
    private JComboBox<String> iridiumComboBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
