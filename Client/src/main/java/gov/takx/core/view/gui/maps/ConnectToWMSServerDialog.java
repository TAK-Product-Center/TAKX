package gov.takx.core.view.gui.maps;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.WWXML;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.systemmessage.ISystemMessageManager;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.SelectAllTableSupport;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.imagerywizard.FileStoreUtil;
import gov.takx.core.globe.imagerywizard.InstallConstants;
import gov.takx.core.globe.imagerywizard.LayerInfo;
import gov.takx.core.globe.imagerywizard.WMSInfo;
import gov.takx.core.globe.imagerywizard.WMSValidator;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.core.view.gui.maps.importers.WWXMLImporter;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static gov.takx.commons.swing.ImageUtils.MEDIUM_ICON_WIDTH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class ConnectToWMSServerDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    //map of different serverMaps, differentiated by WMSType name - static so state is remembered between
    //instantiations of this class.
    private static final Map<String, Map<String, WMSInfo>> serverTypeMaps = new ConcurrentHashMap<>();
    private static final String LAYER_SELECTION_PANEL = "layerSelectionPanel";
    private static final String ERROR_PANEL = "errorPanel";
    private static final String NO_NETWORK_PANEL = "noNetworkPanel";

    // TODO KMB: May want to flatten glossy circles for new UI LAF
    private static final ImageIcon CONNECTED_ICON = new ImageIcon(GraphicsUtils.createGlossyCircle(MEDIUM_ICON_WIDTH, Color.GREEN));
    private static final ImageIcon DISCONNECTED_ICON = new ImageIcon(GraphicsUtils.createGlossyCircle(MEDIUM_ICON_WIDTH, Color.RED));
    private static final ImageIcon UNKNOWN_ICON = new ImageIcon(GraphicsUtils.createGlossyCircle(MEDIUM_ICON_WIDTH, Color.GRAY));
    private static final ImageIcon REFRESH_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "refresh.png");

    private static final Map<String, ImageIcon> ICON_FOR_STATUS = Map.of(
            InstallConstants.OK, CONNECTED_ICON,
            InstallConstants.WAITING, UIResources.ADD_ICON,
            InstallConstants.CONNECTION_ERROR, DISCONNECTED_ICON,
            InstallConstants.INCOMPATIBLE_FORMATS, DISCONNECTED_ICON,
            InstallConstants.PARSE_ERROR, DISCONNECTED_ICON,
            InstallConstants.UNKNOWN_ERROR, DISCONNECTED_ICON);

    private static final TitleComparator TITLE_COMPARATOR = new TitleComparator();

    private static final String BASE_DESCRIPTION = " Online servers will display a green dot; offline will be red. " +
            "Choose layers from an online server by selecting the server on the left and choosing " +
            "which layer(s) on the right that you would like to display. Layers can only be added from " +
            "one server at a time. To add an additional server, press the Add Server button. To delete " +
            "a server, press the Delete Server button. To refresh the status (online/offline) of the " +
            "servers, press the Refresh button.";

    @Inject
    protected ITaskManager taskManager;

    @Inject
    protected IExtendedGlobeManager globeManager;

    @Inject
    protected ISystemMessageManager messageManager;

    private final FileStoreUtil.WMSType wmsType;
    private final LayerGroup layerGroup;
    private final Map<String, WMSInfo> serverMap;
    private final DefaultListModel<WMSInfo> urlListModel;
    private final ConnectToWMSServerSelection serverSelection;
    private final DefaultTableModel dataModel = new LayerTableModel(new Object[]{true, "Name", "Style"}, 0);
    private JFileChooser xmlChooser;
    private Timer networkCheckTimer;
    private final AtomicBoolean isNetworkAvailable = new AtomicBoolean(true);
    private static boolean enableNoNetworkPanel = true;
    private final CardLayout layerSelectionCardLayout;

    //server map becomes dirty when a user manually adds a new server to the map
    //and becomes clean again once the serverSelection update servers thread has completed its work
    private final AtomicBoolean dirtyServerMap = new AtomicBoolean(true);

    protected ConnectToWMSServerDialog(@Nonnull String description, @Nonnull String iconName, @Nonnull LayerGroup layerGroup,
                                       @Nonnull FileStoreUtil.WMSType wmsType, @Nonnull WMSValidator wmsValidator)
    {
        this.wmsType = wmsType;
        this.layerGroup = layerGroup;

        initComponents();
        initNetworkTestTimer();
        layerSelectionCardLayout = (CardLayout) layerCardPanel.getLayout();
        layerCardPanel.add(layerSelectionPanel, LAYER_SELECTION_PANEL);
        layerCardPanel.add(errorPanel, ERROR_PANEL);
        layerCardPanel.add(noNetworkPanel, NO_NETWORK_PANEL);
        showLayerSelectionPanel();

        addButton.setIcon(UIResources.ADD_ICON);
        deleteButton.setIcon(UIResources.REMOVE_ICON);
        refreshButton.setIcon(REFRESH_ICON);

        dialogHeaderPanel.setDescription("<html>" + description + BASE_DESCRIPTION + "</html>");
        dialogHeaderPanel.setIcon(iconName);

        //attempt to get the serverMap from previous instantiation of this class
        serverMap = serverTypeMaps.computeIfAbsent(wmsType.getName(), k -> {
            // load default servers from preferences and create WMSInfo objects for each server
            return FileStoreUtil.registerDefaultServers(new ConcurrentHashMap<>(), wmsType);
        });

        //create layer info objects for each WMSInfo object in serverMap
        FileStoreUtil.registerInstalledWMSObjects(serverMap);

        //set up the URL JList
        urlListModel = new DefaultListModel<>();
        wmsURLList.setModel(urlListModel);
        wmsURLList.setCellRenderer(new WMSListCellRenderer());
        updateURLList();
        wmsURLList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
            {
                deleteButton.setEnabled(wmsURLList.getSelectedIndex() >= 0);
                updateLayerTable();
            }
        });

        wmsURLList.setFixedCellHeight(MEDIUM_ICON_WIDTH + 8); // Ensure tall enough for icon and 2 lines of text

        // Create serverSelection - this handles the footwork of connecting to WMS servers and getting available layers
        serverSelection = new ConnectToWMSServerSelection(this, wmsValidator, serverMap.values(), wmsURLList);

        // set up the layer table
        wmsLayerTable.setModel(dataModel);
        wmsLayerTable.setDragEnabled(false);
        wmsLayerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        wmsLayerTable.getTableHeader().setReorderingAllowed(false);

        SelectAllTableSupport.configureSelectAllColumn(wmsLayerTable, dataModel, 0, false, 28);

        wmsLayerTable.getColumnModel().getColumn(1).setCellRenderer(new LayerTableRenderer());

        final TableRowSorter<TableModel> sorter = new TableRowSorter<>(dataModel);
        sorter.setSortable(0, false);
        sorter.setComparator(1, TITLE_COMPARATOR);
        wmsLayerTable.setRowSorter(sorter);

        enableSelectedPanelComponents();
        setConnectButtonState();
    }

    protected void addLayers(List<LayerInfo> layerInfoList)
    {
        taskManager.execute("Connect to " + layerGroup.getName().toLowerCase() + " server", () -> {
            for (LayerInfo layerInfo : layerInfoList)
            {
                try
                {
                    createAndAddLayer(layerInfo);
                } catch (Exception e)
                {
                    final String layerType = layerGroup == LayerGroup.IMAGERY ? "layer" : "elevation model";
                    final String msg = "Unable to create " + layerType + " for " + layerInfo.getLayerName();

                    logger.warn(msg, e);
                    messageManager.error(msg + ": " + ExceptionUtils.getNestedMessage(e), e);
                }
            }
        });
    }

    /**
     * Create an appropriate element (layer or elevation model) from the given {@code layerInfo} and add it
     * to the primary globe.
     *
     * @param layerInfo Info from which to create the layer/model
     * @since 3.4
     */
    protected abstract void createAndAddLayer(LayerInfo layerInfo) throws Exception;

    protected void importWWXMLConfig(String path)
    {
        new WWXMLImporter(layerGroup, path).beginImport();
    }

    protected void showLayerSelectionPanel()
    {
        layerSelectionCardLayout.show(layerCardPanel, LAYER_SELECTION_PANEL);
    }

    protected void showErrorPanel(String wmsURL, String errorMessage)
    {
        serverURLLabel.setText("<html>" + wmsURL + "</html>");
        errorMessageLabel.setText("<html>" + errorMessage + "</html>");
        layerSelectionCardLayout.show(layerCardPanel, ERROR_PANEL);
    }

    protected void showNoNetworkPanel()
    {
        layerSelectionCardLayout.show(layerCardPanel, NO_NETWORK_PANEL);
    }

    @InvokeOnEDT
    private void updateURLList()
    {
        urlListModel.removeAllElements();
        serverMap.values().forEach(urlListModel::addElement);
        wmsURLList.setSelectedIndex(-1); // Clear selection when list is updated
    }

    /**
     * Uses a timer to iterate through the available network interfaces every 2 seconds to determine whether
     * this machine is connected to a network.  If the timer task finds any non-loopback network interface with an "up"
     * status, the machine will be considered connected to a network.  The network availability member is used to
     * alter the WMS status icons (and selected layer table).
     */
    private void initNetworkTestTimer()
    {
        if (networkCheckTimer == null)
        {
            // Check for network connectivity every 2 seconds.
            networkCheckTimer = new Timer("Network Status Check", true);

            TimerTask networkCheckTask = new TimerTask()
            {
                /**
                 * The action to be performed by this timer task.
                 */
                @Override
                public void run()
                {
                    boolean available = testNetworkInterfaces();
                    boolean doRepaint = isNetworkAvailable.get() != available;
                    isNetworkAvailable.set(available);

                    logger.debug("network available: {}, doRepaint {}", available, doRepaint);

                    if (doRepaint)
                    {
                        updateLayerTable();
                        setConnectButtonState();
                        repaint();
                    }
                }
            };
            networkCheckTimer.schedule(networkCheckTask, 2000, 2000);
        }
    }

    /**
     * Detects whether this machine is connected to a network.
     * <p/>
     * Iterate through the available network interfaces and determine whether any of the non-loopback interfaces have
     * IPv4 address.  Note: this method will not work properly on an IPv6 network.
     *
     * @return {@code true} if the machine is network connected, or {@code false} otherwise.
     * <p/>
     * The solution is discussed by user 'harald' on Stack Overflow at:
     * <a href="http://stackoverflow.com/questions/1402005/how-to-check-if-internet-connection-is-present-in-java">
     *     how-to-check-if-internet-connection-is-present-in-java</a>
     */
    private boolean testNetworkInterfaces()
    {
        try
        {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements())
            {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isLoopback())
                {
                    for (InterfaceAddress address : networkInterface.getInterfaceAddresses())
                    {
                        if (address.getAddress() instanceof Inet4Address) return true;
                    }
                }
            }
        } catch (Exception ignore)
        {
        }
        return false;
    }

    @InvokeOnEDT
    private void updateLayerTable()
    {
        dataModel.setRowCount(0);

        if (!isNetworkAvailable.get() && enableNoNetworkPanel)
        {
            showNoNetworkPanel();
            return;
        }

        if (wmsURLList.getSelectedIndex() < 0 || wmsURLList.getSelectedIndex() >= urlListModel.size()) return;

        WMSInfo wmsInfo = serverSelection.getSelectedServer();

        // ensure the status on the selected WMS server is good before continuing
        String status = wmsInfo.getStringValue(InstallConstants.STATUS);
        if (!InstallConstants.OK.equals(status))
        {
            // If we're still waiting to connect to the server, then just return. Otherwise, assume that an error
            // has occurred, and show the error panel.
            if (!InstallConstants.WAITING.equals(status))
            {
                String errorMessage = wmsInfo.hasKey(ConnectToWMSServerSelection.WMS_ERROR)
                        ? wmsInfo.getStringValue(ConnectToWMSServerSelection.WMS_ERROR) : "";
                showErrorPanel(wmsInfo.getServerURI(), errorMessage);
            }
            return;
        }
        showLayerSelectionPanel();

        //get a list of layers from the selected WMS server
        List<LayerInfo> unmodifiableList = serverSelection.getSelectedServerLayers();
        if (unmodifiableList == null) return;

        List<LayerInfo> layerList = new ArrayList<>(unmodifiableList);
        layerList.sort(TITLE_COMPARATOR);

        // Add rows to the table
        for (LayerInfo layer : layerList)
        {
            dataModel.addRow(new Object[]{isInstalled(layer), layer, layer.getStyles()});
        }

        wmsLayerTable.packAll(); // resize the columns according to the width of the new data

        setConnectButtonState();
    }

    private void urlRadioButtonItemStateChanged()
    {
        setConnectButtonState();
        enableSelectedPanelComponents();
    }

    private void xmlRadioItemStateChanged()
    {
        setConnectButtonState();
        enableSelectedPanelComponents();
    }

    private void addLayerRadioItemStateChanged()
    {
        setConnectButtonState();
        enableSelectedPanelComponents();
    }

    private void enableSelectedPanelComponents()
    {
        disableAllPanelComponents();
        if (urlRadio.isSelected())
        {
            urlTextField.setEnabled(true);
        } else if (addLayerRadio.isSelected())
        {
            GUIUtils.enableComponents(addWMSPanel, true);

            // Delete button is only enabled when we have a selection
            deleteButton.setEnabled(wmsURLList.getSelectedIndex() >= 0);
        } else
        {
            xmlTextField.setEnabled(true);
            browseXMLButton.setEnabled(true);
        }
    }

    private void disableAllPanelComponents()
    {
        GUIUtils.enableComponents(addWMSPanel, false);
        xmlTextField.setEnabled(false);
        browseXMLButton.setEnabled(false);
        urlTextField.setEnabled(false);
    }

    /**
     * Enable the connect button based on which radio button is selected and whether valid layers have been selected
     */
    @InvokeOnEDT
    private void setConnectButtonState()
    {
        //if the server map is set to dirty (i.e. it will be updated soon on a different thread), disable connect button
        if (addLayerRadio.isSelected())
        {
            okButton.setEnabled(false);

            if (!dirtyServerMap.get())
            {
                okButton.setEnabled(false);
                for (int i = 0; i < wmsLayerTable.getRowCount(); i++)
                {
                    LayerInfo layer = (LayerInfo) wmsLayerTable.getValueAt(i, 1);
                    if (!isInstalled(layer) && (Boolean) wmsLayerTable.getValueAt(i, 0))
                    {
                        okButton.setEnabled(true);
                        return;
                    }
                }
            }
        } else if (urlRadio.isSelected())//todo: allow the user to connect to a layer directly from a URL
        {
            URL layerURL = FileStoreUtil.getURL(urlTextField.getText());
            okButton.setEnabled(layerURL != null);
        } else if (xmlRadio.isSelected())
        {
            okButton.setEnabled(!xmlTextField.getText().isEmpty());
        } else
        {
            okButton.setEnabled(false);
        }
    }

    /**
     * We will typically re-use this dialog, so whenever we make it visible, refresh its contents.
     *
     * @param visible {@code true} to make the dialog visible and refresh its contents, or {@code false} to hide the dialog.
     */
    @Override
    public void setVisible(final boolean visible)
    {
        if (visible)
        {
            FileStoreUtil.registerInstalledWMSObjects(serverMap);
            serverSelection.refreshServerInfo();

            // We have to repack with an invokeLater() to be sure all the HTML
            // wrapping is complete and the wrapped text sizes have been calculated.
            SwingUtilities.invokeLater(this::pack);
        }
        super.setVisible(visible);
    }

    private void connectButtonActionPerformed()
    {
        dispose();
        if (networkCheckTimer != null) networkCheckTimer.cancel();
        if (addLayerRadio.isSelected())
        {
            addLayers(getLayerInfoList());
        } else if (xmlRadio.isSelected())
        {
            importWWXMLConfig(xmlTextField.getText());
        } else
        {
            //load the layer specified by the layer URL text field
            String urlText = urlTextField.getText();
            URL layerURL = FileStoreUtil.getURL(urlText);
            if (layerURL != null)
            {
                boolean validLayers = serverSelection.addLayersFromURL(layerURL);
                if (!validLayers)
                {
                    AlwaysOnTopJOptionPane.showErrorDialog("URL:  " + urlText + " does not contain any valid layers",
                            "Add Layers from URL");
                }
            } else
            {
                AlwaysOnTopJOptionPane.showErrorDialog("URL:  " + urlText + " is an invalid URL", "Add Layers from URL");
            }
        }

        FileStoreUtil.persistServers(serverMap.values(), wmsType);
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    @Override
    protected void onWindowClosing()
    {
        if (networkCheckTimer != null) networkCheckTimer.cancel();
        serverSelection.cancelServerUpdate();

        FileStoreUtil.persistServers(serverMap.values(), wmsType);
    }

    /**
     * Get a list of available layers from the selected WMS server
     *
     * @return list of available layers from the selected WMS server
     */
    private List<LayerInfo> getLayerInfoList()
    {
        List<LayerInfo> layerInfoList = new ArrayList<>();
        for (int i = 0; i < wmsLayerTable.getRowCount(); i++)
        {
            if ((Boolean) wmsLayerTable.getValueAt(i, 0))
            {
                LayerInfo layerInfo = (LayerInfo) wmsLayerTable.getValueAt(i, 1);

                if (!isInstalled(layerInfo)) layerInfoList.add(layerInfo);
            }
        }
        return layerInfoList;
    }

    /**
     * A utility method to determine whether a {@link LayerInfo} object has already been installed in TAKX.
     *
     * @param info the layer info object
     * @return {@code true} if this layer info object is already installed, or {@code false} otherwise
     */
    private boolean isInstalled(LayerInfo info)
    {
        return info.getWmsInfo().getInstalledLayers().contains(info);
    }

    private void refreshButtonActionPerformed()
    {
        serverSelection.refreshServerInfo();
    }

    private void addButtonActionPerformed()
    {
        String input = (String) JOptionPane.showInputDialog(this,
                "Please enter a map server URL", "Add Server", JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (input == null) return;

        if (input.isBlank())
        {
            JOptionPane.showMessageDialog(this, "Please enter a valid URL");
            return;
        }

        String mapUrlString = WWXML.fixGetMapString(input);

        // Validate that the URL is at least parsable
        try
        {
            //noinspection ResultOfObjectAllocationIgnored
            new URL(mapUrlString);
        } catch (MalformedURLException ignored)
        {
            JOptionPane.showMessageDialog(this, "URL is malformed");
            return;
        }

        // Only add the server if it isn't already in the map.
        WMSInfo wmsInfo = serverMap.get(mapUrlString);
        if (wmsInfo == null)
        {
            wmsInfo = createServerInfo(mapUrlString);

            // in the case of a proxy server, set to override with the proxy url.
            wmsInfo.setValue(AVKey.GET_CAPABILITIES_URL, mapUrlString);
            wmsInfo.setValue(AVKey.GET_MAP_URL, mapUrlString);
            serverMap.put(wmsInfo.getOriginalURI(), wmsInfo);
            dirtyServerMap.set(true);

            updateURLList();
            setConnectButtonState();
            serverSelection.refreshServerInfo();

            refreshComponents();
        } else
        {
            String wmsTitle = wmsInfo.getStringValue(AVKey.TITLE);
            String message = (wmsTitle == null ? "The server" : wmsTitle) + " has already been added.";
            JOptionPane.showMessageDialog(this, message, "Duplicate Server", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Create a WMSInfo object for a server.
     *
     * @param uri URI of the server.
     * @return new server info object.
     */
    private WMSInfo createServerInfo(String uri)
    {
        WMSInfo.ServiceType serviceType = switch (wmsType)
        {
            // Use Web Map Service protocol for imagery servers.
            case IMAGERY_WMS_SERVERS -> WMSInfo.ServiceType.WMS;

            // Use either Web Coverage Service or Web Map Service for elevation servers. Try WCS first, and fallback
            // to WMS.
            case ELEVATION_WMS_SERVERS -> WMSInfo.ServiceType.AUTO;
        };

        return new WMSInfo(uri, serviceType);
    }

    /**
     * serverSelection can change the server URI (it will simplify the URI to its base), so sync the server map back
     * up with these changes.  This is called from a new thread.
     */
    public synchronized void syncMapToURLList()
    {
        serverMap.clear();

        urlListModel.elements().asIterator()
                .forEachRemaining(wmsInfo -> serverMap.put(wmsInfo.getOriginalURI(), wmsInfo));

        dirtyServerMap.set(false);
        updateLayerTable();     // also want to sync the table list of layers
        setConnectButtonState();
    }

    private void deleteButtonActionPerformed()
    {
        WMSInfo selected = wmsURLList.getSelectedValue();
        if (selected == null) return;

        String name = selected.getStringValue(AVKey.TITLE);
        if (name == null) name = selected.getServerURI();

        int choice = JOptionPane.showConfirmDialog(this, "<html>Are you sure you want to delete (no undo): <br>" + name + "</html>",
                "Delete Server", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) removeWmsServer(selected);
    }

    protected void removeWmsServer(WMSInfo wmsInfo)
    {
        serverMap.remove(wmsInfo.getOriginalURI());
        urlListModel.removeElement(wmsInfo);
        updateURLList();
    }

    /**
     * Allow the user to import a config WW XML file
     */
    private void browseXMLButtonActionPerformed()
    {
        if (xmlChooser == null)
        {
            xmlChooser = new AlwaysOnTopFileChooser();
            xmlChooser.setFileFilter(new FileNameExtensionFilter("XML Config Files (*.xml)", "xml"));
            xmlChooser.setDialogTitle("Select the WW XML configuration file to import");
            xmlChooser.setMultiSelectionEnabled(false);
        }
        if (xmlChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            File xmlFile = xmlChooser.getSelectedFile();
            if (xmlFile != null && xmlFile.exists())
            {
                xmlTextField.setText(xmlFile.getAbsolutePath());
                setConnectButtonState();
            }
        }
    }

    private void okIgnoreButtonActionPerformed()
    {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        enableNoNetworkPanel = false;
        showLayerSelectionPanel();
        updateLayerTable();
    }

    private void urlTextFieldKeyTyped()
    {
        setConnectButtonState();
    }

    private static class LayerTableRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            JLabel r = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            LayerInfo layer = (LayerInfo) value;
            r.setText("<html>" + layer.getValue(AVKey.TITLE) + "</html>");
            return r;
        }
    }

    @InvokeOnEDT
    public void refreshComponents()
    {
        invalidate();
        validate();
        repaint();
    }

    /**
     * Comparator to order LayerInfo instances by title.
     * @since 3.4
     */
    private static class TitleComparator implements Comparator<LayerInfo>
    {
        @Override
        public int compare(LayerInfo o1, LayerInfo o2)
        {
            String s1 = o1.getStringValue(AVKey.TITLE);
            String s2 = o2.getStringValue(AVKey.TITLE);
            return s1.compareToIgnoreCase(s2);
        }
    }

    private class WMSListCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus)
        {
            JLabel r = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            WMSInfo wmsInfo = (WMSInfo) value;

            final String status = !isNetworkAvailable.get()
                    ? InstallConstants.CONNECTION_ERROR
                    : wmsInfo.getStringValue(InstallConstants.STATUS);
            final ImageIcon icon = status == null ? UNKNOWN_ICON : ICON_FOR_STATUS.getOrDefault(status, UNKNOWN_ICON);

            r.setIcon(icon);

            StringBuilder sb = new StringBuilder();
            sb.append("<html>");

            String title = wmsInfo.getStringValue(AVKey.TITLE);
            if (!StringUtils.isEmpty(title))
            {
                sb.append(title);
                sb.append("<br>");
            }

            sb.append(wmsInfo.getOriginalURI());
            sb.append("</html>");
            r.setText(sb.toString());

            return r;
        }
    }

    /**
     * Table model for the layer list for a given server.
     * @since 3.4
     */
    private class LayerTableModel extends DefaultTableModel
    {
        private static final Class<?>[] COLUMN_CLASSES = {Boolean.class, LayerInfo.class, String.class};

        public LayerTableModel(Object[] columnNames, int rowCount)
        {
            super(columnNames, rowCount);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return COLUMN_CLASSES[columnIndex];
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex)
        {
            if (columnIndex == 0)
            {
                // if the layer of the selected row is already installed, disallow deselection of the checkbox
                final LayerInfo layer = (LayerInfo) getValueAt(rowIndex, 1);
                final Object newValue = isInstalled(layer) ? true : value;
                super.setValueAt(newValue, rowIndex, 0);

                setConnectButtonState();
            } else
            {
                super.setValueAt(value, rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return column == 0;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        var panel2 = new JPanel();
        urlRadio = new JRadioButton();
        urlTextField = new JTextField();
        xmlRadio = new JRadioButton();
        xmlTextField = new JTextField();
        browseXMLButton = new JButton();
        addLayerRadio = new JRadioButton();
        addWMSPanel = new JPanel();
        var label2 = new JLabel();
        var panel4 = new JPanel();
        var scrollPane1 = new JScrollPane();
        wmsURLList = new JList<>();
        var panel5 = new JPanel();
        addButton = new JButton();
        deleteButton = new JButton();
        refreshButton = new JButton();
        layerCardPanel = new JPanel();
        var buttonBar = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        layerSelectionPanel = new JPanel();
        var label3 = new JLabel();
        var scrollPane2 = new JScrollPane();
        wmsLayerTable = new JXTable();
        errorPanel = new JPanel();
        var scrollPane3 = new JScrollPane();
        var panel6 = new JPanel();
        var errorHeaderLabel = new JLabel();
        serverURLLabel = new JLabel();
        var separator2 = new JSeparator();
        errorMessageLabel = new JLabel();
        noNetworkPanel = new JPanel();
        var networkErrorLabel = new JLabel();
        var label1 = new JLabel();
        var panel7 = new JPanel();
        var okIgnoreButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setTitle("Connect to Imagery Server");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "2*(default, $lgap), fill:default:grow"));

                //======== panel2 ========
                {
                    panel2.setName("panel2");
                    panel2.setLayout(new FormLayout(
                            "default, $lcgap, default:grow, $lcgap, default",
                            "default, $lgap, default"));

                    //---- urlRadio ----
                    urlRadio.setText("Layer URL");
                    urlRadio.setName("urlRadio");
                    urlRadio.addItemListener(e -> urlRadioButtonItemStateChanged());
                    panel2.add(urlRadio, cc.xy(1, 1));

                    //---- urlTextField ----
                    urlTextField.setEnabled(false);
                    urlTextField.setName("urlTextField");
                    urlTextField.addKeyListener(new KeyAdapter()
                    {
                        @Override
                        public void keyTyped(KeyEvent e)
                        {
                            urlTextFieldKeyTyped();
                        }
                    });
                    panel2.add(urlTextField, cc.xywh(3, 1, 3, 1));

                    //---- xmlRadio ----
                    xmlRadio.setText("WW XML");
                    xmlRadio.setName("xmlRadio");
                    xmlRadio.addItemListener(e -> xmlRadioItemStateChanged());
                    panel2.add(xmlRadio, cc.xy(1, 3));

                    //---- xmlTextField ----
                    xmlTextField.setEditable(false);
                    xmlTextField.setEnabled(false);
                    xmlTextField.setName("xmlTextField");
                    panel2.add(xmlTextField, cc.xy(3, 3));

                    //---- browseXMLButton ----
                    browseXMLButton.setText("Browse");
                    browseXMLButton.setEnabled(false);
                    browseXMLButton.setName("browseXMLButton");
                    browseXMLButton.addActionListener(e -> browseXMLButtonActionPerformed());
                    panel2.add(browseXMLButton, cc.xy(5, 3));
                }
                contentPanel.add(panel2, cc.xy(1, 1));

                //---- addLayerRadio ----
                addLayerRadio.setSelected(true);
                addLayerRadio.setText("Add layer from Web Map Service (WMS)");
                addLayerRadio.setName("addLayerRadio");
                addLayerRadio.addItemListener(e -> addLayerRadioItemStateChanged());
                contentPanel.add(addLayerRadio, cc.xy(1, 3));

                //======== addWMSPanel ========
                {
                    addWMSPanel.setName("addWMSPanel");
                    addWMSPanel.setLayout(new FormLayout(
                            "default, $lcgap, default:grow",
                            "default, fill:default:grow"));

                    //---- label2 ----
                    label2.setText("Select a server:");
                    label2.setName("label2");
                    addWMSPanel.add(label2, cc.xy(1, 1));

                    //======== panel4 ========
                    {
                        panel4.setName("panel4");
                        panel4.setLayout(new FormLayout(
                                "default:grow",
                                "fill:default:grow, $lgap, default"));

                        //======== scrollPane1 ========
                        {
                            scrollPane1.setName("scrollPane1");

                            //---- wmsURLList ----
                            wmsURLList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            wmsURLList.setName("wmsURLList");
                            scrollPane1.setViewportView(wmsURLList);
                        }
                        panel4.add(scrollPane1, cc.xy(1, 1));

                        //======== panel5 ========
                        {
                            panel5.setName("panel5");
                            panel5.setLayout(new FormLayout(
                                    "2*($button, $lcgap), $button",
                                    "fill:default"));

                            //---- addButton ----
                            addButton.setText("Add Server");
                            addButton.setName("addButton");
                            addButton.addActionListener(e -> addButtonActionPerformed());
                            panel5.add(addButton, cc.xy(1, 1));

                            //---- deleteButton ----
                            deleteButton.setText("Delete Server");
                            deleteButton.setEnabled(false);
                            deleteButton.setName("deleteButton");
                            deleteButton.addActionListener(e -> deleteButtonActionPerformed());
                            panel5.add(deleteButton, cc.xy(3, 1));

                            //---- refreshButton ----
                            refreshButton.setText("Refresh");
                            refreshButton.setName("refreshButton");
                            refreshButton.addActionListener(e -> refreshButtonActionPerformed());
                            panel5.add(refreshButton, cc.xy(5, 1));
                        }
                        panel4.add(panel5, cc.xy(1, 3));
                    }
                    addWMSPanel.add(panel4, cc.xy(1, 2));

                    //======== layerCardPanel ========
                    {
                        layerCardPanel.setName("layerCardPanel");
                        layerCardPanel.setLayout(new CardLayout());
                    }
                    addWMSPanel.add(layerCardPanel, cc.xywh(3, 1, 1, 2));
                }
                contentPanel.add(addWMSPanel, cc.xy(1, 5));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("Connect");
                okButton.setName("okButton");
                okButton.addActionListener(e -> connectButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //======== layerSelectionPanel ========
        {
            layerSelectionPanel.setName("layerSelectionPanel");
            layerSelectionPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, fill:default:grow"));

            //---- label3 ----
            label3.setText("Select layers below:");
            label3.setName("label3");
            layerSelectionPanel.add(label3, cc.xy(1, 1));

            //======== scrollPane2 ========
            {
                scrollPane2.setName("scrollPane2");

                //---- wmsLayerTable ----
                wmsLayerTable.setPreferredScrollableViewportSize(new Dimension(450, 250));
                wmsLayerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                wmsLayerTable.setShowVerticalLines(false);
                wmsLayerTable.setName("wmsLayerTable");
                scrollPane2.setViewportView(wmsLayerTable);
            }
            layerSelectionPanel.add(scrollPane2, cc.xy(1, 3));
        }

        //======== errorPanel ========
        {
            errorPanel.setName("errorPanel");
            errorPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default:grow"));

            //======== scrollPane3 ========
            {
                scrollPane3.setName("scrollPane3");

                //======== panel6 ========
                {
                    panel6.setName("panel6");
                    panel6.setLayout(new FormLayout(
                            "default:grow",
                            "3*(default, $lgap), default"));

                    //---- errorHeaderLabel ----
                    errorHeaderLabel.setText("Error connecting to server:");
                    errorHeaderLabel.setForeground(Color.red);
                    errorHeaderLabel.setName("errorHeaderLabel");
                    panel6.add(errorHeaderLabel, cc.xy(1, 1));

                    //---- serverURLLabel ----
                    serverURLLabel.setName("serverURLLabel");
                    panel6.add(serverURLLabel, cc.xy(1, 3));

                    //---- separator2 ----
                    separator2.setName("separator2");
                    panel6.add(separator2, cc.xy(1, 5));

                    //---- errorMessageLabel ----
                    errorMessageLabel.setName("errorMessageLabel");
                    panel6.add(errorMessageLabel, cc.xy(1, 7));
                }
                scrollPane3.setViewportView(panel6);
            }
            errorPanel.add(scrollPane3, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        }

        //======== noNetworkPanel ========
        {
            noNetworkPanel.setName("noNetworkPanel");
            noNetworkPanel.setLayout(new FormLayout(
                    "default:grow",
                    "2*(default, $ugap), default"));

            //---- networkErrorLabel ----
            networkErrorLabel.setForeground(Color.red);
            networkErrorLabel.setVerticalAlignment(SwingConstants.TOP);
            networkErrorLabel.setText("<html><center>No Networks available:<br>Map servers may be unreachable</center></html>");
            networkErrorLabel.setName("networkErrorLabel");
            noNetworkPanel.add(networkErrorLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

            //---- label1 ----
            label1.setText("<html><center>If you would like to ignore this warning<br>and attempt to connect to a server,<br>please press OK</center></html>");
            label1.setHorizontalAlignment(SwingConstants.LEFT);
            label1.setName("label1");
            noNetworkPanel.add(label1, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));

            //======== panel7 ========
            {
                panel7.setName("panel7");
                panel7.setLayout(new FormLayout(
                        "default",
                        "default"));

                //---- okIgnoreButton ----
                okIgnoreButton.setText("OK");
                okIgnoreButton.setName("okIgnoreButton");
                okIgnoreButton.addActionListener(e -> okIgnoreButtonActionPerformed());
                panel7.add(okIgnoreButton, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
            }
            noNetworkPanel.add(panel7, cc.xy(1, 5, CellConstraints.CENTER, CellConstraints.DEFAULT));
        }

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(urlRadio);
        buttonGroup1.add(xmlRadio);
        buttonGroup1.add(addLayerRadio);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JRadioButton urlRadio;
    private JTextField urlTextField;
    private JRadioButton xmlRadio;
    private JTextField xmlTextField;
    private JButton browseXMLButton;
    protected JRadioButton addLayerRadio;
    private JPanel addWMSPanel;
    private JList<WMSInfo> wmsURLList;
    private JButton addButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private JPanel layerCardPanel;
    private JButton okButton;
    private JPanel layerSelectionPanel;
    private JXTable wmsLayerTable;
    private JPanel errorPanel;
    private JLabel serverURLLabel;
    private JLabel errorMessageLabel;
    private JPanel noNetworkPanel;
// JFormDesigner - End of variables declaration  //GEN-END:variables
}
