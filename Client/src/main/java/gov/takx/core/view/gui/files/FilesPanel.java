package gov.takx.core.view.gui.files;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.RaptorTransferable;
import gov.takx.core.mapobject.gui.DeleteAction;
import gov.takx.core.uimodel.ACoordinateSystemListModel;
import gov.takx.core.view.ApplicationDataFlavor;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.maps.importers.FileImporter;
import gov.takx.platform.ui.UIResources;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static gov.takx.api.coordinatesystem.CoordinateSystem.getLocationString;

/**
 */
@PseudoBean
public class FilesPanel extends ATitledPanel implements IRaptorLifecycleListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String LAUNCH_FILE_ACTION_NAME = "launchFile";

    private IVirtualFileSystem fileSystem;
    private FileSystemListModel model;
    private final Map<IFileMapObject, Icon> icons = new HashMap<>();
    private final LaunchAction launchAction;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IGlobeManager globeManager;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    public FilesPanel()
    {
        super(ButtonType.FILES.getDisplayText());
        initComponents();

        importButton.setIcon(UIResources.IMPORT_ACTION_BUTTON_ICON);
        removeButton.setIcon(UIResources.REMOVE_ACTION_BUTTON_ICON);
        exportButton.setIcon(UIResources.EXPORT_ACTION_BUTTON_ICON);
        gotoButton.setIcon(UIResources.GOTO_ACTION_BUTTON_ICON);

        launchAction = new LaunchAction(() -> filesList.getSelectedValuesList());
        filesList.getActionMap().put(LAUNCH_FILE_ACTION_NAME, launchAction);

        filesList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), LAUNCH_FILE_ACTION_NAME);
    }

    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        fileSystem = projectVFSManager.getVirtualFileSystemForAttachments();
        model = new FileSystemListModel(mapObjectManager.getMapObjectControllers(), this::filesListValueChanged);

        filesList.setModel(model);
        filesList.setTransferHandler(new TransferHandler()
        {
            @Override
            @SuppressWarnings("unchecked")
            public boolean canImport(TransferSupport support)
            {
                if (support.isDrop())
                {
                    support.setDropAction(COPY);
                }

                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) && !support.isDataFlavorSupported(ApplicationDataFlavor.geoFileListFlavor))
                {
                    try
                    {
                        for (File file : (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor))
                        {
                            if (file.isDirectory())
                            {
                                return false;
                            }
                        }
                    } catch (InvalidDnDOperationException exception)
                    {
                        return true;
                    } catch (UnsupportedFlavorException | IOException exception)
                    {
                        logger.error("Error:", exception);
                    }

                    return true;
                }

                return false;
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support)
            {
                if (canImport(support))
                {
                    try
                    {
                        new FileImporter((List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor), fileSystem).doImport(true);

                        return true;
                    } catch (UnsupportedFlavorException | IOException exception)
                    {
                        logger.error("Error:", exception);
                    }
                }

                return false;
            }

            @Override
            protected void exportDone(JComponent source, Transferable data, int action)
            {
                // Need to ensure GoTo button gets enabled if we dropped onto the globe.
                filesListValueChanged();
            }

            @Override
            public int getSourceActions(JComponent component)
            {
                return COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent component)
            {
                List<File> files = new ArrayList<>();
                List<IFileMapObject> mapObjects = new ArrayList<>();

                for (IFileMapObject fileMapObject : filesList.getSelectedValuesList())
                {
                    files.add(fileMapObject.getVfsFile().getFile());
                    mapObjects.add(fileMapObject);
                }

                RaptorTransferable transferable = new RaptorTransferable();

                transferable.setTransferData(DataFlavor.javaFileListFlavor, files);
                transferable.setTransferData(ApplicationDataFlavor.geoFileListFlavor, mapObjects);

                return transferable;
            }
        });
        filesList.setDragEnabled(true);
    }

    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        if (model != null)
        {
            model.dispose();

            filesList.setModel(new DefaultListModel<>());
            icons.clear();
        }
    }

    /**
     * Currently unused, but retained description for posterity or eventual resurrection.
     */
    public String getDescription()
    {
        return "<html>All of the files that have been attached to the current project are displayed below. Attach new " +
                "files to the project using the Attach button. Drag files onto the globe to set geo-location coordinates. " +
                "Delete Files from the panel using the Delete Button.  Navigate the globe to a file using " +
                "the Go To button. Export a copy of a file using the Save button.  Double-click on a file to" +
                "launch in its default viewer</html>";
    }

    private void importButtonActionPerformed()
    {
        AlwaysOnTopFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);

        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Add a File");
        chooser.setAcceptAllFileFilterUsed(true);

        if (chooser.showDialog(this, "Add") == JFileChooser.APPROVE_OPTION)
        {
            List<File> files = FileImporter.validateAttachmentFiles(chooser.getSelectedFiles());
            if (!files.isEmpty())
            {
                new FileImporter(chooser.getSelectedFiles(), fileSystem).doImport(true);
            }
        }
    }

    private void exportButtonActionPerformed()
    {
        final IFileMapObject mo = filesList.getSelectedValue();
        final IVFSFile vfsFile = mo.getVfsFile();
        if (vfsFile.exists())
        {
            AlwaysOnTopFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
            chooser.setDialogTitle("Save File");

            String ext = FileUtils.getExtension(vfsFile.getFile());
            if (null != ext)
            {
                chooser.resetChoosableFileFilters();
                chooser.setFileFilter(new FileNameExtensionFilter("*." + ext + " File Type", ext));
            }

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
            {
                File targetFile = chooser.getSelectedFile();
                if (null != ext && FileUtils.getExtension(targetFile) == null)
                {
                    targetFile = new File(targetFile.getParent(), targetFile.getName() + "." + ext);
                }
                new ExportDialog(mo, fileSystem, targetFile).setVisible(true);
            }
        } else
        {
            AlwaysOnTopJOptionPane.showErrorDialogLater("Can't find file: " + vfsFile.getFileName() + ".  Very possibly it has been removed from the project.", "Error Opening File");
        }
    }

    private void filesListValueChanged()
    {
        final int idx = filesList.getSelectedIndex();
        final boolean elementSelected = idx != -1;

        exportButton.setEnabled(elementSelected);
        removeButton.setEnabled(elementSelected);

        if (elementSelected)
        {
            gotoButton.setEnabled(filesList.getModel().getElementAt(idx).hasLocation());
        } else
        {
            gotoButton.setEnabled(false);
        }
    }

    private void filesListMouseClicked(MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2 && filesList.getSelectedIndex() != -1)
        {
            launchAction.actionPerformed(new ActionEvent(event.getSource(), event.getID(), "Launch"));
        }
    }

    private void filesListMousePressedOrReleased(MouseEvent event)
    {
        if (event.isPopupTrigger())
        {
            int index = filesList.locationToIndex(event.getPoint());

            if (!filesList.isSelectedIndex(index))
            {
                filesList.setSelectedIndex(index);
            }

            if (filesList.getSelectedIndex() == -1)
            {
                return;
            }

            JPopupMenu menu = new JPopupMenu();

            menu.add(launchAction);
            menu.add(new AbstractAction("Export")
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                    exportButtonActionPerformed();
                }
            });
            menu.add(new AbstractAction("Open")
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                    filesList.getSelectedValue().getController().showPluginDialog();
                }
            });
            menu.add(new AbstractAction("Preferences")
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                    filesList.getSelectedValue().getController().showConfigurationDialog();
                }
            });
            menu.add(new AbstractAction("Go To")
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                    globeManager.getLastFocusedGlobe().goTo((filesList.getSelectedValue()).getLocation());
                }
            }).setEnabled(filesList.getSelectedValuesList().size() == 1 && (filesList.getSelectedValue()).hasLocation());
            menu.add(new AbstractAction("Delete")
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                    new DeleteAction("Delete", filesList.getSelectedValue()).actionPerformed(event);
                }
            });
            menu.show(event.getComponent(), event.getX(), event.getY());
        }
    }

    private void gotoButtonActionPerformed()
    {
        final IMapObject mo = filesList.getModel().getElementAt(filesList.getSelectedIndex());
        final GeoPoint location = mo.getLocation();

        if (location.isValid())
        {
            globeManager.getLastFocusedGlobe().goTo(location);
        }
    }

    private void removeButtonActionPerformed()
    {
        new DeleteAction("Delete", filesList.getSelectedValue()).actionPerformed(null);
    }

    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @PseudoBean
    private static final class FileSystemListModel extends ACoordinateSystemListModel<IFileMapObject>
            implements PropertyChangeListener
    {
        private final List<IFileMapObject> fileMapObjects = new CopyOnWriteArrayList<>();
        private final Runnable onUpdate;

        public FileSystemListModel(Collection<? extends IMapObjectController> mapObjectControllers, Runnable onUpdate)
        {
            this.onUpdate = onUpdate;

            for (IMapObjectController controller : mapObjectControllers)
            {
                addMapObject(controller.getMapObject());
            }
        }

        @RemoveObserver
        @Override
        public void dispose()
        {
            super.dispose();
            fileMapObjects.clear();
        }

        @Override
        public int getSize()
        {
            return fileMapObjects.size();
        }

        @Override
        public IFileMapObject getElementAt(int index)
        {
            return fileMapObjects.get(index);
        }

        /**
         * Notified by {@link IMapObjectManager} when a new map object is added.
         *
         * @param event The event providing the added map object
         * @since 2.38
         */
        private void onClientMapObjectAdded(@Observes MapObjectAdded event)
        {
            addMapObject(event.getMapObject());
        }

        /**
         * Add the map object to the model.
         *
         * @param mapObject The map object to add.
         * @since 2.38 - formerly clientMapObjectAdded
         */
        private void addMapObject(IMapObject mapObject)
        {
            if (!(mapObject instanceof IFileMapObject fileMapObject)) return;  // Not a file, don't process

            mapObject.addPropertyChangeListener(IMapObject.LOCATION, this);

            final int index = fileMapObjects.size();
            fileMapObjects.add(fileMapObject);

            SwingUtilities.invokeLater(() -> fireIntervalAdded(this, index, index));
        }

        /**
         * Notified by {@link IMapObjectManager} when a map object is removed.
         *
         * @param event The event providing the removed map object
         * @since 2.38 - formerly clientMapObjectRemoved
         */
        private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
        {
            final IMapObject mapObject = event.getMapObject();

            if (!(mapObject instanceof IFileMapObject)) return;  // Not a file, don't process

            mapObject.removePropertyChangeListener(IMapObject.LOCATION, this);

            final int index = fileMapObjects.indexOf(mapObject);
            fileMapObjects.remove(index);

            SwingUtilities.invokeLater(() -> fireIntervalRemoved(this, index, index));
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            Object source = evt.getSource();

            if (source instanceof IFileMapObject)
            {
                int index = fileMapObjects.indexOf(source);
                fireContentsChanged(this, index, index);
                onUpdate.run();
            }
        }
    }

    private class FilesListCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            final IFileMapObject fmo = (IFileMapObject) value;
            final IVFSFile vfsFile = fmo.getVfsFile();

            final Icon icon = vfsFile.exists()
                    ? icons.computeIfAbsent(fmo, k -> vfsFile.getSystemIcon())
                    : null;

            label.setIcon(icon);

            final String location = fmo.hasLocation()
                    ? getLocationString(fmo.getLocation(), model.getCoordinateSystem())
                    : "Not on Globe";

            label.setText("<html><body>" + vfsFile.getFileName() + "<br/>" + location + "</body></html>");

            return label;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panelButtons = new JPanel();
        importButton = new JButton();
        removeButton = new JButton();
        exportButton = new JButton();
        gotoButton = new JButton();
        var filesScrollPane = new JScrollPane();
        filesList = new JList<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default, $lgap, fill:default:grow"));

        //======== panelButtons ========
        {
            panelButtons.setName("panelButtons");
            panelButtons.setLayout(new FormLayout(
                    "3*(default, $lcgap), default",
                    "fill:default"));

            //---- importButton ----
            importButton.setText("Attach");
            importButton.setToolTipText("Attach a File to this project. Once a file is attached, you can drag and drop the file from the left panel to place it on the globe.");
            importButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            importButton.setIconTextGap(1);
            importButton.setHorizontalTextPosition(SwingConstants.CENTER);
            importButton.setName("importButton");
            importButton.addActionListener(e -> importButtonActionPerformed());
            panelButtons.add(importButton, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- removeButton ----
            removeButton.setText("Delete");
            removeButton.setHorizontalTextPosition(SwingConstants.CENTER);
            removeButton.setEnabled(false);
            removeButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            removeButton.setToolTipText("Delete the selected File.");
            removeButton.setIconTextGap(1);
            removeButton.setName("removeButton");
            removeButton.addActionListener(e -> removeButtonActionPerformed());
            panelButtons.add(removeButton, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- exportButton ----
            exportButton.setText("Save");
            exportButton.setToolTipText("Save the selected File to this computer. ");
            exportButton.setEnabled(false);
            exportButton.setIconTextGap(1);
            exportButton.setHorizontalTextPosition(SwingConstants.CENTER);
            exportButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            exportButton.setName("exportButton");
            exportButton.addActionListener(e -> exportButtonActionPerformed());
            panelButtons.add(exportButton, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- gotoButton ----
            gotoButton.setText("Go To");
            gotoButton.setToolTipText("Go to the location of the selected File.");
            gotoButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            gotoButton.setIconTextGap(1);
            gotoButton.setHorizontalTextPosition(SwingConstants.CENTER);
            gotoButton.setEnabled(false);
            gotoButton.setName("gotoButton");
            gotoButton.addActionListener(e -> gotoButtonActionPerformed());
            panelButtons.add(gotoButton, cc.xy(7, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        }
        add(panelButtons, cc.xy(1, 1));

        //======== filesScrollPane ========
        {
            filesScrollPane.setName("filesScrollPane");

            //---- filesList ----
            filesList.setDropMode(DropMode.INSERT);
            filesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            filesList.setName("filesList");
            filesList.addListSelectionListener(e -> filesListValueChanged());
            filesList.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    filesListMouseClicked(e);
                }

                @Override
                public void mousePressed(MouseEvent e)
                {
                    filesListMousePressedOrReleased(e);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    filesListMousePressedOrReleased(e);
                }
            });
            filesList.setCellRenderer(new FilesListCellRenderer());
            filesScrollPane.setViewportView(filesList);
        }
        add(filesScrollPane, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton importButton;
    private JButton removeButton;
    private JButton exportButton;
    private JButton gotoButton;
    private JList<IFileMapObject> filesList;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
