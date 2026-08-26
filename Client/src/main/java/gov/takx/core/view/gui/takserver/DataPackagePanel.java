package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IDataPackageDownloadListener;
import gov.tak.api.comms.takserver.IDataPackageManager;
import gov.tak.api.comms.takserver.IDataPackageQueryListener;
import gov.tak.api.comms.takserver.IResource;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.comms.takserver.mission.data.AssociatedResource;
import gov.tak.platform.datapackage.PackageImporter;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.takserver.TakServerConnectionEvent;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.datapackage.PackageSelectionPanel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * This panel offers controls to view the list of downloaded data packages, obtain a list of available data packages,
 * and to initiate the download of selected packages.
 *
 * @since 3.1
 */
@PseudoBean(singletonObserver = true)
public class DataPackagePanel extends ATitledPanel implements IRaptorLifecycleListener, IDataPackageQueryListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ImageIcon TRANSFER_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "transfer-log.png");
    private static final ImageIcon FOLDER_ICON = UIResources.getActionButtonIcon(TAK_ASSETS_PNG, "Folder-Open_32x32.png");

    private static final String PROGRESS_CARD = "progressCard";
    private static final String SELECTION_CARD = "selectionCard";
    private static final String PACKAGES_CARD = "packagesCard";

    private final List<ITakServerConnection> pendingQueriedConnections = new CopyOnWriteArrayList<>();
    private final List<ITakServerConnection> erroredQueriedConnections = new CopyOnWriteArrayList<>();

    private final DataPackageListPanel dataPackageListPanel = new DataPackageListPanel();
    private final CardLayout cardLayout;

    private DataPackageTransferHistoryDialog transferHistoryDialog;
    private PackageSelectionPanel packageSelectionPanel;
    private DataPackageCreateDialog createDialog;

    @Inject
    private ITakServerConnectionManager takServerManager;

    @Inject
    private IDataPackageManager dataPackageManager;

    public DataPackagePanel()
    {
        super(ButtonType.DATA_PACKAGES.getDisplayText());

        initComponents();

        cardLayout = (CardLayout) contentPanel.getLayout();

        detailsScrollPane.setViewportView(dataPackageListPanel);

        folderButton.setIcon(FOLDER_ICON);
        logButton.setIcon(TRANSFER_ICON);
        newButton.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);

        showPackages();
    }

    @PostConstruct
    private void postConstruct()
    {
        onTakServerConnectionEvent(null); // Get our initial state set
    }

    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        dataPackageListPanel.clear();
    }

    @Override
    public void onPackageListResponse(@Nonnull ITakServerConnection connection, @Nonnull List<AssociatedResource> results)
    {
        pendingQueriedConnections.remove(connection);

        packageSelectionPanel.addFiles(connection, results);

        checkPackageFeedback();
    }

    @Override
    public void onPackageListError(@Nonnull ITakServerConnection connection, @Nonnull Throwable error)
    {
        pendingQueriedConnections.remove(connection);
        erroredQueriedConnections.add(connection);

        final String message = "Failed to query Data Packages from: " + connection.getDisplayName() + "\n" + error;
        AlwaysOnTopJOptionPane.showErrorDialogLater(message, "Data Package Query Failed");

        checkPackageFeedback();
    }

    /**
     * Check if there are no more pending connection queries, and show the resulting packages.
     * <br>
     * Must be invoked on the EDT since we're checking against the model state in the {@link #packageSelectionPanel}
     * which is updated on the EDT. If the calling context is not on the EDT we want to wait until the model updates
     * before checking if packages were found.
     *
     * @since 5.0
     */
    @InvokeOnEDT
    private void checkPackageFeedback()
    {
        // Show the selection card once all queried connections have gotten data back.
        // If there are no packages to show, jump straight back to the packages panel.
        if (pendingQueriedConnections.isEmpty())
        {
            if (packageSelectionPanel.hasFiles())
            {
                showSelection();
            } else
            {
                String message = "The requested servers had no packages to offer.";
                if (!erroredQueriedConnections.isEmpty())
                {
                    message += "\n\nThe following connections encountered errors when querying for packages:\n" +
                            erroredQueriedConnections.stream()
                                    .map(ITakServerConnection::getDisplayName)
                                    .collect(Collectors.joining("\n - "));
                }
                AlwaysOnTopJOptionPane.showErrorDialogLater(message, "No Data Packages found");
                showPackages();
            }
        }
    }

    /**
     * On any connection event, update the downloadButton.  Enable it if we have any connected connections,
     * otherwise, disable it.
     *
     * @since 3.2
     */
    @InvokeOnEDT
    private void onTakServerConnectionEvent(@Observes TakServerConnectionEvent ignore)
    {
        downloadWrapper.removeAll();

        final JButton downloadButton = UiUtilities.buildDownloadButton(takServerManager, this::onDownloadRequest,
                "Download packages");
        downloadWrapper.add(BorderLayout.CENTER, downloadButton);
    }

    /**
     * Opens a window showing the data package transfer log.
     */
    private void logButtonActionPerformed()
    {
        if (transferHistoryDialog == null)
        {
            transferHistoryDialog = new DataPackageTransferHistoryDialog(SwingUtilities.getWindowAncestor(this));
        }
        transferHistoryDialog.setVisible(true);
    }

    /**
     * Open an explorer/file manager on the data packages root directory.
     */
    private void folderButtonActionPerformed()
    {
        if (Desktop.isDesktopSupported())
        {
            final File rootDir = dataPackageManager.getRootDir();
            try
            {
                Desktop.getDesktop().browse(rootDir.toURI());
            } catch (IOException e)
            {
                logger.warn("Failed to open {}", rootDir.getAbsolutePath());
                AlwaysOnTopJOptionPane.showErrorDialog(e.toString(), "Failed to open browser");
            }
        }
    }

    /**
     * Kick off a query for available packages.
     *
     * @param connection Connection to query for data packages.
     * Can be {@code null} to query from all available servers.
     */
    private void onDownloadRequest(@Nullable ITakServerConnection connection)
    {
        if (packageSelectionPanel != null) packageSelectionCard.remove(packageSelectionPanel);

        packageSelectionPanel = new PackageSelectionPanel(this::downloadFiles, this::showPackages);

        packageSelectionCard.add(packageSelectionPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, PROGRESS_CARD);

        populateFileList(connection);
    }

    /**
     * Prompts for a name to create a new data package.
     */
    private void newButtonActionPerformed()
    {
        if (createDialog != null)
        {
            createDialog.requestFocus();
            return;
        }

        createDialog = new DataPackageCreateDialog(created -> {
            // Ensure current packages list is shown when creating a new package
            showPackages();
        }, () -> createDialog = null);

        createDialog.setVisible(true);
    }

    /**
     * Download the given list of files.
     */
    private void downloadFiles(@Nonnull ITakServerConnection connection, @Nonnull List<? extends IResource> fileEntries)
    {
        showPackages();

        fileEntries.forEach(fileEntry -> dataPackageManager.downloadDataPackage(connection, fileEntry, new IDataPackageDownloadListener()
        {
            @Override
            public void onPackageDownloadSuccess(@Nonnull ITakServerConnection connection, @Nonnull AssociatedResource resource, @Nonnull Path localPath)
            {
                importPackage(resource, localPath.toFile());
            }

            @Override
            public void onPackageDownloadError(@Nonnull ITakServerConnection connection, @Nonnull Throwable error)
            {
                // no-op
            }
        }));
    }

    /**
     * Import the contents of the downloaded file.
     *
     * @param resource Information about the data package file.
     * @param packageFile Location of the locally downloaded package file.
     *
     * @since 3.5
     */
    private static void importPackage(@Nonnull AssociatedResource resource, @Nonnull File packageFile)
    {
        try
        {
            final String ownerUid = resource.creatorUid();
            new PackageImporter(packageFile, ownerUid).importPackage();
        } catch (Exception e)
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Import failed: " + e.getMessage(), "Import failed");
            throw new RuntimeException("Import failed", e);
        }
    }

    /**
     * Show the packages list card.
     *
     * @since 3.2
     */
    private void showPackages()
    {
        cardLayout.show(contentPanel, PACKAGES_CARD);
    }

    /**
     * Shows the package selection card.
     *
     * @since 5.0
     */
    private void showSelection()
    {
        cardLayout.show(contentPanel, SELECTION_CARD);
        packageSelectionPanel.takeFocus();
    }

    /**
     * Kick off the retrieval of the available packages from all servers.
     *
     * @param connection Connection to query for data packages.
     * Can be {@code null} to query from all available servers.
     */
    private void populateFileList(@Nullable ITakServerConnection connection)
    {
        pendingQueriedConnections.clear();
        if (connection == null)
        {
            pendingQueriedConnections.addAll(takServerManager.getTakServerConnections(true));
            dataPackageManager.queryRemoteDataPackages(this);
        } else
        {
            pendingQueriedConnections.add(connection);
            dataPackageManager.queryRemoteDataPackages(connection, this);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var buttonBar = new JPanel();
        newButton = new JButton();
        downloadWrapper = new JPanel();
        logButton = new JButton();
        folderButton = new JButton();
        contentPanel = new JPanel();
        var progressPanel = new JPanel();
        var progressBar = new JProgressBar();
        var packageDetailsPanel = new JPanel();
        detailsScrollPane = new JScrollPane();
        packageSelectionCard = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "top:default, $rgap, fill:default:grow"));

        //======== buttonBar ========
        {
            buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
            buttonBar.setName("buttonBar");
            buttonBar.setLayout(new FormLayout(
                    "$glue, default, 2*($lcgap, min), $rgap, min",
                    "pref"));

            //---- newButton ----
            newButton.setToolTipText("Create new package");
            newButton.setName("newButton");
            newButton.addActionListener(e -> newButtonActionPerformed());
            buttonBar.add(newButton, cc.xy(2, 1));

            //======== downloadWrapper ========
            {
                downloadWrapper.setName("downloadWrapper");
                downloadWrapper.setLayout(new BorderLayout());
            }
            buttonBar.add(downloadWrapper, cc.xy(4, 1));

            //---- logButton ----
            logButton.setToolTipText("Show transfer log");
            logButton.setName("logButton");
            logButton.addActionListener(e -> logButtonActionPerformed());
            buttonBar.add(logButton, cc.xy(6, 1));

            //---- folderButton ----
            folderButton.setFocusable(false);
            folderButton.setToolTipText("Open packages folder");
            folderButton.setName("folderButton");
            folderButton.addActionListener(e -> folderButtonActionPerformed());
            buttonBar.add(folderButton, cc.xy(8, 1));
        }
        add(buttonBar, cc.xy(1, 1));

        //======== contentPanel ========
        {
            contentPanel.setName("contentPanel");
            contentPanel.setLayout(new CardLayout());

            //======== progressPanel ========
            {
                progressPanel.setName("progressPanel");
                progressPanel.setLayout(new BorderLayout());

                //---- progressBar ----
                progressBar.setStringPainted(true);
                progressBar.setString("Checking server for packages...");
                progressBar.setIndeterminate(true);
                progressBar.setName("progressBar");
                progressPanel.add(progressBar, BorderLayout.NORTH);
            }
            contentPanel.add(progressPanel, "progressCard");

            //======== packageDetailsPanel ========
            {
                packageDetailsPanel.setName("packageDetailsPanel");
                packageDetailsPanel.setLayout(new BorderLayout());

                //======== detailsScrollPane ========
                {
                    detailsScrollPane.setName("detailsScrollPane");
                }
                packageDetailsPanel.add(detailsScrollPane, BorderLayout.CENTER);
            }
            contentPanel.add(packageDetailsPanel, "packagesCard");

            //======== packageSelectionCard ========
            {
                packageSelectionCard.setName("packageSelectionCard");
                packageSelectionCard.setLayout(new BorderLayout());
            }
            contentPanel.add(packageSelectionCard, "selectionCard");
        }
        add(contentPanel, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton newButton;
    private JPanel downloadWrapper;
    private JButton logButton;
    private JButton folderButton;
    private JPanel contentPanel;
    private JScrollPane detailsScrollPane;
    private JPanel packageSelectionCard;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
