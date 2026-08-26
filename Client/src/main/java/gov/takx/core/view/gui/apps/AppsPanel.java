package gov.takx.core.view.gui.apps;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.apps.IAppInstance;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.plugin.app.IAppListener;
import gov.takx.api.plugin.app.IImmutableAppInstance;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.bootstrap.Verification;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.commons.swing.DelegatingTableCellRenderer;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.IconTableCellRenderer;
import gov.takx.commons.swing.MouseClickedListener;
import gov.takx.commons.swing.PopupTriggerListener;
import gov.takx.commons.swing.SimpleAction;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.apps.AppConfigurationDialog;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.TOOLBAR_BUTTON_IMAGE_SIZE;

/**
 * Panel displaying App plugins, providing actions to start/stop, configure, and get information on them.
 */
@PseudoBean
public class AppsPanel extends ATitledPanel implements IRaptorLifecycleListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Icon START_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "play.png");
    private static final Icon STOP_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "stop.png");
    private static final Icon INFO_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "info_outline.svg");
    private static final Icon TRUSTED_ICON = UIResources.getColorizedToolbarIcon(PLATFORM_ICONS, "shield_check.png", new Color(0, 160, 0));
    private static final Icon UNTRUSTED_ICON = UIResources.getColorizedToolbarIcon(PLATFORM_ICONS, "shield.png", new Color(160, 0, 0));
    private static final Icon UNVERIFIED_ICON = UIResources.getColorizedToolbarIcon(PLATFORM_ICONS, "shield_slash.png", new Color(75, 75, 75));
    private static final int ICON_DIMENSION = TOOLBAR_BUTTON_IMAGE_SIZE.width + 6;

    @Inject
    private ICoreAppManager appManager;

    private final ImageIcon runningAppIcon = new ImageIcon(GraphicsUtils.createShape(GraphicsUtils.Shape.CIRCLE, TOOLBAR_BUTTON_IMAGE_SIZE.width, 200, Color.GREEN));
    private final ImageIcon stoppedAppIcon = new ImageIcon(GraphicsUtils.createShape(GraphicsUtils.Shape.CIRCLE, TOOLBAR_BUTTON_IMAGE_SIZE.width, 200, Color.RED));
    private final AppListModel appListModel = new AppListModel();

    public AppsPanel()
    {
        super(ButtonType.APPS.getDisplayText());

        initComponents();

        appsTable.setModel(appListModel);
        appsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final var columnModel = appsTable.getColumnModel();
        final var runningIconColumn = columnModel.getColumn(0);
        final var descriptionColumn = columnModel.getColumn(1);
        final var verificationColumn = columnModel.getColumn(2);

        runningIconColumn.setCellRenderer(new IconTableCellRenderer<Boolean>(running -> running ? runningAppIcon : stoppedAppIcon));
        runningIconColumn.setPreferredWidth(ICON_DIMENSION);
        runningIconColumn.setMaxWidth(ICON_DIMENSION);

        final var descriptionRenderer = new DelegatingTableCellRenderer<IImmutableAppInstance>(instance -> "<html><body>" + instance.getName() + "<br>" + (instance.isRunning() ? "Running" : "Stopped") + "</body></html>");
        descriptionRenderer.setTooltipMapping(IImmutableAppInstance::getDescription);
        descriptionColumn.setCellRenderer(descriptionRenderer);

        verificationColumn.setCellRenderer(new IconTableCellRenderer<Verification>(verification -> switch (verification)
        {
            case Trusted -> TRUSTED_ICON;
            case Untrusted -> UNTRUSTED_ICON;
            default -> UNVERIFIED_ICON;
        }));
        verificationColumn.setPreferredWidth(ICON_DIMENSION);
        verificationColumn.setMaxWidth(ICON_DIMENSION);

        appsTable.setTableHeader(null);
        appsTable.setRowHeight(ICON_DIMENSION);
        appsTable.getSelectionModel().addListSelectionListener(e -> updateButtons());

        startStopButton.setIcon(START_ICON);
        configButton.setIcon(UIResources.SETTINGS_ACTION_BUTTON_ICON);
        infoButton.setIcon(INFO_ICON);

        // Make the buttons square
        List.of(startStopButton, configButton, infoButton).forEach(b -> b.putClientProperty(FlatClientProperties.SQUARE_SIZE, true));

        appsTable.addMouseListener((PopupTriggerListener) this::appsListPopupTrigger);
        appsTable.getSelectionModel().addListSelectionListener(e -> updateButtons());
        appsTable.addMouseListener((MouseClickedListener) this::appsListMouseClicked);
    }

    private IAppInstance getSelectedAppInstance()
    {
        return appListModel.getAppInstance(appsTable.getSelectedRow());
    }

    /**
     * Currently unused, but retained description for posterity or eventual resurrection.
     */
    public String getDescription()
    {
        return "<html>The status of each application in the current project is displayed below. Start or Stop applications using the start and stop buttons. Configure application settings using the Configure button, or double-click the desired application.</html>";
    }

    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        appListModel.connect(appManager);
    }

    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        appListModel.disconnect(appManager);
    }

    @InvokeOnEDT
    private void updateButtons()
    {
        if (appsTable.getSelectedRow() != -1)
        {
            final var appInstance = getSelectedAppInstance();
            final boolean haveAppInstance = appInstance != null;

            if (haveAppInstance)
            {
                configButton.setEnabled(true);
                startStopButton.setEnabled(true);
                infoButton.setEnabled(true);

                final boolean isRunning = appInstance.isRunning();
                startStopButton.setIcon(isRunning ? STOP_ICON : START_ICON);
                startStopButton.setText(isRunning ? "Stop" : "Start");
                startStopButton.setToolTipText(isRunning ? "Stop the selected App." : "Start the selected App.");

                return;
            }
        }

        configButton.setEnabled(false);
        infoButton.setEnabled(false);
        startStopButton.setEnabled(false);
        startStopButton.setIcon(START_ICON);
        startStopButton.setText("Start");
        startStopButton.setToolTipText(null);
    }

    private void appsListMouseClicked(MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 && appsTable.getSelectedRow() != -1)
        {
            startOrStop(getSelectedAppInstance());
        }
    }

    private void startOrStop(@Nonnull IAppInstance appInstance)
    {
        try
        {
            if (!appInstance.isRunning())
            {
                appInstance.start();
            } else
            {
                String warning = appInstance.getApp().displayWarningOnStop();
                if (warning != null)
                {
                    int result = AlwaysOnTopJOptionPane.showConfirmDialog(this, warning, "Stop " + appInstance.getName() + "?", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) return;
                }
                appInstance.stop();
            }
        } catch (Exception e)
        {
            final String name = appInstance.getName();
            final String verb = appInstance.isRunning() ? "stopping" : "starting";
            logger.error("Error {} app {}", verb, name, e);
            AlwaysOnTopJOptionPane.showErrorDialog("Error " + verb + " app " + name + ": " + e.getMessage(), "Error");
        }
    }

    private void startStopButtonActionPerformed()
    {
        startOrStop(getSelectedAppInstance());
    }

    private void configButtonActionPerformed()
    {
        showConfigDialog(getSelectedAppInstance());
    }

    private void infoButtonActionPerformed()
    {
        showInfoDialog(getSelectedAppInstance());
    }

    private void showInfoDialog(IAppInstance appInstance)
    {
        JOptionPane.showMessageDialog(this, new AppInfoPanel(appInstance), "App Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showConfigDialog(IAppInstance appInstance)
    {
        final var dialog = new AppConfigurationDialog(appInstance);
        ComponentUtils.centerComponentOnScreen(dialog);
        dialog.setVisible(true);
    }

    protected void appsListPopupTrigger(@Nonnull MouseEvent e)
    {
        int tableRow = appsTable.rowAtPoint(e.getPoint());
        if (tableRow > -1)
        {
            appsTable.getSelectionModel().setSelectionInterval(tableRow, tableRow);
            final var appInstance = appListModel.getAppInstance(appsTable.convertRowIndexToModel(tableRow));
            if (appInstance != null)
            {
                final var menu = new JPopupMenu();
                menu.add(new SimpleAction(appInstance.isRunning() ? "Stop" : "Start", ae -> startOrStop(appInstance)));
                menu.add(new SimpleAction("Configure...", ae -> showConfigDialog(appInstance)));
                menu.add(new SimpleAction("Info...", ae -> showInfoDialog(appInstance)));
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class AppListModel extends AbstractTableModel implements IAppListener
    {
        private final List<IAppInstance> apps = new ArrayList<>();

        /**
         * Fill our model with all the apps and start listening for app events.
         * @since 3.4
         */
        private void connect(ICoreAppManager appManager)
        {
            appManager.getApps().forEach(this::addApp);
            appManager.addListener(this);
        }

        /**
         * Disconnect from listening to app events and clear our contents.
         * @since 3.4
         */
        private void disconnect(ICoreAppManager appManager)
        {
            appManager.removeListener(this);

            final int size = apps.size();
            if (size > 0)
            {
                apps.clear();
                fireTableRowsDeleted(0, size - 1);
            }
        }

        /**
         * Add the given {@code app} to our list, in sorted order by name.  If the app is a background app, then
         * it is not added to the list.
         *
         * @since 3.4
         */
        private void addApp(IImmutableAppInstance app)
        {
            if (app.isBackground()) return;

            final String appName = app.getName();
            final int size = apps.size();
            int index = 0;
            while (index < size && apps.get(index).getName().compareTo(appName) < 0) index++;

            apps.add(index, (IAppInstance) app);
            fireTableRowsInserted(index, index);
        }

        private IAppInstance getAppInstance(int index)
        {
            return apps.get(index);
        }

        @Override
        public void onAppInstalled(IImmutableAppInstance app)
        {
            addApp(app);
            updateButtons();
        }

        @Override
        public void onAppStarted(IImmutableAppInstance app)
        {
            onStartOrStop(app);
        }

        @Override
        public void onAppStopped(IImmutableAppInstance app)
        {
            onStartOrStop(app);
        }

        private void onStartOrStop(IImmutableAppInstance app)
        {
            int index = apps.indexOf((IAppInstance) app);
            if (index != -1)
            {
                fireTableRowsUpdated(index, index);
                updateButtons();
            }
        }

        @Override
        public int getRowCount()
        {
            return apps.size();
        }

        @Override
        public int getColumnCount()
        {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            final var app = apps.get(rowIndex);
            return switch (columnIndex)
            {
                case 0 -> app.isRunning();
                case 1 -> app;
                case 2 -> app.getPluginDefinition().getVerification();
                default -> null;
            };
        }
    }

    /**
     * Panel to show the app name, version, and description.
     * @since 5.7
     */
    private static class AppInfoPanel extends JPanel
    {
        private static final CellConstraints CC = new CellConstraints();

        AppInfoPanel(IAppInstance appInstance)
        {
            final var definition = appInstance.getPluginDefinition();

            setLayout(new FormLayout("default:grow", "2*(default, $lgap), fill:default:grow"));

            final var nameLabel = new JLabel(definition.getName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | Font.BOLD));
            add(nameLabel, CC.xy(1, 1));

            add(new JLabel("Version: " + definition.getVersion()), CC.xy(1, 3));

            final var descriptionTextArea = new JTextArea(5, 30);
            descriptionTextArea.setEditable(false);
            descriptionTextArea.setLineWrap(true);
            descriptionTextArea.setWrapStyleWord(true);

            final var longDescription = definition.getLongDescription();
            descriptionTextArea.setText(StringUtils.isBlank(longDescription) ? definition.getDescription() : longDescription);

            add(new JScrollPane(descriptionTextArea), CC.xy(1, 5));
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var buttonPanel = new JPanel();
        startStopButton = new JButton();
        configButton = new JButton();
        infoButton = new JButton();
        var appsScrollPane = new JScrollPane();
        appsTable = new JTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "min:grow",
                "default, $lgap, fill:default:grow"));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "2*(min, $lcgap), min",
                    "default"));

            //---- startStopButton ----
            startStopButton.setText("Start");
            startStopButton.setEnabled(false);
            startStopButton.setIconTextGap(1);
            startStopButton.setHorizontalTextPosition(SwingConstants.CENTER);
            startStopButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            startStopButton.setToolTipText("Run the selected App.");
            startStopButton.setFocusable(false);
            startStopButton.setName("startStopButton");
            startStopButton.addActionListener(e -> startStopButtonActionPerformed());
            buttonPanel.add(startStopButton, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- configButton ----
            configButton.setText("Config");
            configButton.setEnabled(false);
            configButton.setIconTextGap(1);
            configButton.setHorizontalTextPosition(SwingConstants.CENTER);
            configButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            configButton.setToolTipText("Configure the selected App.");
            configButton.setFocusable(false);
            configButton.setName("configButton");
            configButton.addActionListener(e -> configButtonActionPerformed());
            buttonPanel.add(configButton, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- infoButton ----
            infoButton.setText("Info");
            infoButton.setEnabled(false);
            infoButton.setToolTipText("See information about this App");
            infoButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            infoButton.setIconTextGap(1);
            infoButton.setHorizontalTextPosition(SwingConstants.CENTER);
            infoButton.setFocusable(false);
            infoButton.setName("infoButton");
            infoButton.addActionListener(e -> infoButtonActionPerformed());
            buttonPanel.add(infoButton, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        }
        add(buttonPanel, cc.xy(1, 1));

        //======== appsScrollPane ========
        {
            appsScrollPane.setName("appsScrollPane");

            //---- appsTable ----
            appsTable.setBorder(new EmptyBorder(0, 10, 0, 10));
            appsTable.setName("appsTable");
            appsScrollPane.setViewportView(appsTable);
        }
        add(appsScrollPane, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton startStopButton;
    private JButton configButton;
    private JButton infoButton;
    private JTable appsTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
