package gov.takx.core.view.gui.takserver;

import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.comms.commo.IContactManager;
import gov.tak.api.comms.takserver.IDataPackageManager;
import gov.tak.api.comms.takserver.IDataPackageVfsListener;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.contact.IContactNameResolver;
import gov.tak.platform.datapackage.content.DataPackageContents;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IMultiSelector;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.shapes.ShapeType;
import gov.takx.commons.lang.AlphaNumericComparator;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.ImageFileChooserFilter;
import gov.takx.commons.swing.SplitButton;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static gov.takx.platform.preferences.PreferenceEntry.DATA_PACKAGE_ENABLE_SHARING;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_PACKAGE_NO_GO_THRESHOLD_MB;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_PACKAGE_SUCCESS_THRESHOLD_HIGH_MB;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_PACKAGE_SUCCESS_THRESHOLD_LOW_MB;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static org.apache.commons.io.FileUtils.ONE_MB;

/**
 * Panel to hold a list of data packages and the details of their contents.
 *
 * @since 3.2
 */
@PseudoBean(singletonObserver = true)
public class DataPackageListPanel extends JPanel implements IDataPackageVfsListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Icon SEND_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "send.png");
    private static final Icon DELETE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "delete.png");
    private static final RowSpec ROW_SPEC = RowSpec.decode("fill:pref"); // Fill, but don't grow vertically
    private static final String ENCODED_COLUMN_SPEC = "default:grow"; // Grow horizontally

    // Comparator to get contacts sorted by callsign
    private static final AlphaNumericComparator<ContactAndCallsign> CONTACT_COMPARATOR = new AlphaNumericComparator<>(ContactAndCallsign::callsign);

    private final Map<File, PackageContentsPanel> dataPackagePanelMap = new ConcurrentHashMap<>();
    private final Map<File, Boolean> dataPackageExpansions = new ConcurrentHashMap<>();
    private AlwaysOnTopFileChooser imageChooser;
    private AlwaysOnTopFileChooser fileChooser;
    private FormLayout formLayout;

    @Inject
    private IDataPackageManager dataPackageManager;

    @Inject
    private IContactManager contactManager;

    @Inject
    private IContactNameResolver contactNameResolver;

    @Inject
    private ITakServerConnectionManager serverConnectionManager;

    @Inject
    private IMultiSelector multiSelector;

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    @NodeUUID
    private String nodeUuid;

    public DataPackageListPanel()
    {
        resetLayout();
    }

    @PostConstruct
    private void postConstruct()
    {
        executorService.execute(() -> {
            Thread.currentThread().setName("Data Package Loader");
            dataPackageManager.getAllDataPackageContents().forEach(this::addPackageContents);
            dataPackageManager.addDataPackageVfsListener(this);
        });

        DATA_PACKAGE_SUCCESS_THRESHOLD_LOW_MB.newIntegerMonitor(ignored -> updateEntries());
        DATA_PACKAGE_SUCCESS_THRESHOLD_HIGH_MB.newIntegerMonitor(ignored -> updateEntries());
        DATA_PACKAGE_NO_GO_THRESHOLD_MB.newIntegerMonitor(ignored -> updateEntries());
        DATA_PACKAGE_ENABLE_SHARING.newBooleanMonitor(ignored -> updateEntries());
    }

    @Override
    public void onPackageAdded(@Nonnull File packageFile, @Nonnull DataPackageContents contents)
    {
        addPackageContents(contents);
    }

    @Override
    public void onPackageUpdated(@Nonnull File packageFile, @Nonnull DataPackageContents contents)
    {
        removePackageContents(packageFile);
        addPackageContents(contents);
    }

    @Override
    public void onPackageRemoved(@Nonnull File packageFile)
    {
        removePackageContents(packageFile);
    }

    /**
     * Clear contents and reset our layout.
     */
    public void clear()
    {
        removeAll();
        resetLayout();

        dataPackageExpansions.clear();
        dataPackagePanelMap.clear();
    }

    private void resetLayout()
    {
        formLayout = new FormLayout(ENCODED_COLUMN_SPEC); // Start with no rows, rows are added dynamically
        setLayout(formLayout);
    }

    /**
     * Updates all entries sendButtons based on shared state and sizeLabel color based on package size in the package list.
     *
     * @since 5.0
     */
    private void updateEntries()
    {
        boolean enableSend = DATA_PACKAGE_ENABLE_SHARING.getBooleanValue();
        for (PackageContentsPanel contents : dataPackagePanelMap.values())
        {
            contents.sizeLabel.setForeground(contents.calculateSizeColor(contents.packageSizeMB));
            contents.sendButton.setEnabled(enableSend);
        }
    }

    /**
     * Add the package contents to our display panel.
     *
     * @param packageContents Outline of data package content to add to the display.
     */
    @InvokeOnEDT
    private void addPackageContents(@Nonnull DataPackageContents packageContents)
    {
        final PackageContentsPanel panel = new PackageContentsPanel(packageContents);
        File packageFile = packageContents.getPackageFile();
        dataPackagePanelMap.put(packageFile, panel);

        // If the panel existed previously and was expanded, expand the newly created one to match the prior state
        if (dataPackageExpansions.getOrDefault(packageFile, false)) panel.toggle();

        // The added panel should be constrained to the x/y of the new row.
        // For the y position, always use what the form-layout says is the current row count.
        // Using another value like 'getComponentCount()' can lead to some odd behaviors.
        formLayout.appendRow(ROW_SPEC);
        add(panel, new CellConstraints(1, formLayout.getRowCount()));

        revalidate();
        repaint();
    }

    /**
     * Looks up the {@link PackageContentsPanel} associated with the given package and removes it from the display.
     *
     * @param dataPackage File path to data package to remove.
     */
    @InvokeOnEDT
    private void removePackageContents(@Nonnull File dataPackage)
    {
        PackageContentsPanel panelToRemove = dataPackagePanelMap.remove(dataPackage);
        if (panelToRemove != null)
        {
            remove(panelToRemove);

            revalidate();
            repaint();
        }
    }

    /**
     * Panel per package, with a collapsible table showing the content entries.
     */
    private class PackageContentsPanel extends JPanel
    {
        // File name bold, user callsign below
        private static final String NAME_AND_USER_FMT = "<html><b>%s</b><br><span style=\"font-size:80%%;\">modified by: </span>%s</html>";

        private static final Dimension TOGGLE_BUTTON_SIZE = new Dimension(30, 20);

        private static final String RIGHT_ARROW = "\u02c3";
        private static final String DOWN_ARROW = "\u02c5";

        // Shift the table to the right, past the toggle button
        private static final EmptyBorder SHIFT_RIGHT_BORDER = new EmptyBorder(0, TOGGLE_BUTTON_SIZE.width, 0, 0);
        private static final EmptyBorder EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);

        private final File packageFile;

        private final JButton toggleButton = makeToggleButton();
        private final JPanel entriesPanel;

        private final SplitButton sendButton;
        private final JLabel sizeLabel;
        private final long packageSizeMB;

        PackageContentsPanel(DataPackageContents packageContents)
        {
            super(new BorderLayout());
            setBorder(new EmptyBorder(5, 0, 0, 0));

            packageFile = packageContents.getPackageFile();

            final JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.setBorder(new EmptyBorder(1, 0, 1, 0));

            panel.add(toggleButton);
            panel.add(new JLabel(String.format(NAME_AND_USER_FMT, packageFile.getName(), packageContents.getOwner())));

            sizeLabel = new JLabel(packageContents.getFormattedSize());
            packageSizeMB = packageFile.length() / ONE_MB;
            sizeLabel.setForeground(calculateSizeColor(packageSizeMB));
            sizeLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
            sizeLabel.setBorder(new EmptyBorder(0, 0, 0, 4));
            panel.add(sizeLabel);

            final File[] noFiles = new File[0];
            final SplitButton addButton = new SplitButton();
            addButton.setToolTipText("Add contents to this data package");
            addButton.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);
            addButton.setPopupMenuProvider(() -> {
                JPopupMenu menu = new JPopupMenu();

                JMenuItem imageFile = new JMenuItem("Add images");
                imageFile.addActionListener(me -> {
                    if (imageChooser == null)
                    {
                        imageChooser = new AlwaysOnTopFileChooser();
                        imageChooser.setDialogType(JFileChooser.OPEN_DIALOG);
                        imageChooser.setDialogTitle("Open");
                        imageChooser.setMultiSelectionEnabled(true);
                        imageChooser.setFileFilter(new ImageFileChooserFilter());
                    }

                    imageChooser.setSelectedFiles(noFiles);
                    if (imageChooser.showDialog(addButton, "Open") == JFileChooser.APPROVE_OPTION)
                    {
                        File[] selectedFiles = imageChooser.getSelectedFiles();
                        if (selectedFiles == null) return;
                        try
                        {
                            dataPackageManager.addImagesToDataPackage(packageFile, Arrays.asList(selectedFiles));
                        } catch (IOException ex)
                        {
                            logger.error("Failed adding image to data package '{}'", packageFile.getName(), ex);
                        }
                    }
                });
                menu.add(imageFile);

                JMenuItem genericFile = new JMenuItem("Add files");
                genericFile.addActionListener(me -> {
                    if (fileChooser == null)
                    {
                        fileChooser = new AlwaysOnTopFileChooser();
                        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
                        fileChooser.setDialogTitle("Open");
                        fileChooser.setMultiSelectionEnabled(true);
                        fileChooser.setAcceptAllFileFilterUsed(true);
                    }

                    fileChooser.setSelectedFiles(noFiles);
                    if (fileChooser.showDialog(addButton, "Open") == JFileChooser.APPROVE_OPTION)
                    {
                        File[] selectedFiles = fileChooser.getSelectedFiles();
                        if (selectedFiles == null) return;
                        try
                        {
                            dataPackageManager.addFilesToDataPackage(packageFile, Arrays.asList(selectedFiles));
                        } catch (IOException ex)
                        {
                            logger.error("Failed adding image to data package '{}'", packageFile.getName(), ex);
                        }
                    }
                });
                menu.add(genericFile);

                JMenuItem mapObjects = new JMenuItem("Add map items with Lasso tool");
                mapObjects.addActionListener(me -> {
                    multiSelector.arm(ShapeType.FREEHAND_POLYGON, selected -> {
                        try
                        {
                            dataPackageManager.addMapObjectsToDataPackage(packageFile, selected);
                        } catch (IOException ex)
                        {
                            logger.error("Failed adding map objects to data package '{}'", packageFile.getName(), ex);
                        } finally
                        {
                            multiSelector.disarmAndClear();
                        }
                    });
                });
                menu.add(mapObjects);

                return menu;
            });
            panel.add(addButton);

            boolean sharingEnabled = DATA_PACKAGE_ENABLE_SHARING.getBooleanValue();
            sendButton = new SplitButton();
            sendButton.setEnabled(sharingEnabled);
            sendButton.setToolTipText(sharingEnabled ? "Send this data package to a TAK Contact" : "Sharing is disabled in the user preferences");
            sendButton.setPopupMenuProvider(() -> {
                final var menu = new JPopupMenu();
                final var contactMenu = new JMenu("Send to TAK Contact");

                // Add contacts in sorted order
                contactManager.getAvailableContacts().stream()
                        .filter(contact -> !contact.contactUID.equals(nodeUuid)) // Skip ourself
                        .map(contact -> new ContactAndCallsign(contact, contactNameResolver.resolveNameOrElseUnknown(contact.contactUID)))
                        .sorted(CONTACT_COMPARATOR)
                        .map(cac -> {
                            final var menuItem = new JMenuItem(cac.callsign);
                            menuItem.addActionListener(ignored -> {
                                try
                                {
                                    dataPackageManager.sendPackageToContact(packageFile, cac.contact);
                                } catch (CommoException e)
                                {
                                    logger.error("Data package '{}' failed to send to contact '{} ({})'",
                                            packageFile.getName(), cac.callsign, cac.contact.contactUID, e);
                                }
                            });
                            return menuItem;
                        })
                        .forEach(contactMenu::add);

                // Add servers
                JMenuItem itemTakServer;
                Consumer<ITakServerConnection> r = connection -> {
                    try
                    {
                        dataPackageManager.sendPackageToServer(packageFile, connection)
                                .whenComplete((response, t) -> {
                                    if (response != null)
                                    {
                                        logger.info("Data package '{}' uploaded to server '{}'",
                                                packageFile.getName(), connection.getDisplayName());
                                    } else if (t != null)
                                    {
                                        logger.error("Data package '{}' failed to upload to server '{}'",
                                                packageFile.getName(), connection.getDisplayName(), t);
                                    }
                                });
                    } catch (IOException ex)
                    {
                        logger.error("Data package '{}' failed to upload to server '{}'",
                                packageFile.getName(), connection.getDisplayName(), ex);
                    }
                };
                List<? extends ITakServerConnection> connections = serverConnectionManager.getTakServerConnections().stream()
                        .filter(ITakServerConnection::isConnected).toList();
                if (connections.isEmpty())
                {
                    itemTakServer = new JMenuItem("Send to TAK Server");
                    itemTakServer.setEnabled(false);
                } else if (connections.size() == 1)
                {
                    ITakServerConnection connection = connections.iterator().next();
                    itemTakServer = new JMenuItem("Send to TAK Server");
                    itemTakServer.addActionListener(e -> r.accept(connection));
                } else
                {
                    JMenu tmpServerMenu = new JMenu("Send to TAK Server");
                    itemTakServer = tmpServerMenu;
                    for (ITakServerConnection connection : connections)
                    {
                        JMenuItem menuItem = new JMenuItem(connection.getDisplayName());
                        menuItem.addActionListener(e -> r.accept(connection));
                        tmpServerMenu.add(menuItem);
                    }
                }
                menu.add(contactMenu);
                menu.add(itemTakServer);
                return menu;
            });
            sendButton.setIcon(SEND_ICON);
            if (!sharingEnabled)
            {
                sendButton.setEnabled(false);
                sendButton.setToolTipText("Data package sharing has been disabled");
            } else if (packageSizeMB > DATA_PACKAGE_NO_GO_THRESHOLD_MB.getLongValue())
            {
                sendButton.setEnabled(false);
                sendButton.setToolTipText("Data package is larger than the no-go threshold");
            }
            panel.add(sendButton);

            final JButton deleteButton = new JButton();
            deleteButton.setToolTipText("Delete this data package locally");
            deleteButton.setIcon(DELETE_ICON);
            deleteButton.addActionListener(e -> {
                try
                {
                    dataPackageManager.deleteDataPackage(packageFile);
                } catch (IOException ex)
                {
                    logger.error("Data package cannot be deleted, not present in VFS", ex);
                }
            });
            panel.add(deleteButton);

            entriesPanel = makeEntriesPanel(packageContents);

            add(panel, BorderLayout.NORTH);
            add(entriesPanel, BorderLayout.CENTER);
        }

        /**
         * @param sizeMB Size in megabytes.
         * @return Color indicating which threshold the size falls under.
         */
        @Nonnull
        private Color calculateSizeColor(long sizeMB)
        {
            if (sizeMB <= DATA_PACKAGE_SUCCESS_THRESHOLD_LOW_MB.getLongValue()) return Color.GREEN;
            if (sizeMB <= DATA_PACKAGE_SUCCESS_THRESHOLD_HIGH_MB.getLongValue()) return Color.ORANGE;
            return Color.RED;
        }

        /**
         * @return A new button that calls {@link #toggle()}
         */
        private JButton makeToggleButton()
        {
            final JButton button = new JButton(RIGHT_ARROW);

            // Make the toggle button appear flat. This is achieved in FlatLAF by making it borderless
            button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
            button.setFocusable(false);
            button.setMaximumSize(TOGGLE_BUTTON_SIZE);
            button.setPreferredSize(TOGGLE_BUTTON_SIZE);
            button.setIconTextGap(0);
            button.setAlignmentY(TOP_ALIGNMENT);
            button.setForeground(Color.darkGray);

            final Font font = button.getFont();
            button.setFont(font.deriveFont(font.getStyle() | Font.BOLD, font.getSize() + 2f));

            button.addActionListener(e -> toggle());

            return button;
        }

        /**
         * Create the panel that holds the entries table for the given contents.
         *
         * @see PackageEntryTable
         */
        private JPanel makeEntriesPanel(DataPackageContents packageContents)
        {
            final JPanel panel = new JPanel(new BorderLayout());
            panel.setVisible(false);
            panel.setBorder(SHIFT_RIGHT_BORDER);

            final PackageEntryTable table = new PackageEntryTable(packageContents);

            // This is incredibly annoying, but if we set the border to null here, then when it and the table it
            // contains are added to the hierarchy (when it is made visible), it will reinstall the default border.
            // This behavior is unique to a scroll pane containing a table. The table is reinstalling the border in
            // JTable#configureEnclosingScrollPaneUI().
            //
            // So, by setting an empty border here, we prevent that from happening.
            //
            // Yes, I lost 5 hours of my life finding out where a stupid 1 pixel wide line of the wrong color was
            // coming from.

            final JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(EMPTY_BORDER);

            // Don't force the table to be taller than it wants, but keep it from being too tall
            final Dimension viewportSize = new Dimension(200, Math.min(table.getPreferredSize().height, 300));
            table.setPreferredScrollableViewportSize(viewportSize);

            panel.add(scrollPane);

            // Install a lighter color to all sub-components
            final Color background = panel.getBackground().brighter();
            panel.setBackground(background);
            table.setBackground(background);
            scrollPane.getViewport().setBackground(background);

            return panel;
        }

        /**
         * Toggle the visibility of the entries panel.
         */
        private void toggle()
        {
            final boolean wasShowing = entriesPanel.isShowing();

            entriesPanel.setVisible(!wasShowing);
            toggleButton.setText(wasShowing ? RIGHT_ARROW : DOWN_ARROW);

            dataPackageExpansions.put(packageFile, !wasShowing);
        }
    }

    /**
     * Simple holder to support creation and sorting of the "send to contact" menu entries.
     * @since 5.4
     */
    private record ContactAndCallsign(Contact contact, String callsign)
    {
    }
}
