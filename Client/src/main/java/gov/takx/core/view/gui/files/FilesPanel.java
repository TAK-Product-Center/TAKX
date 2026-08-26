package gov.takx.core.view.gui.files;

import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.RaptorTransferable;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.common.CommonEntityTable;
import gov.takx.core.view.ApplicationDataFlavor;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.maps.importers.FileImporter;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel to display a table of {@link IFileMapObject} instances.
 */
@PseudoBean(singletonObserver = true)
public class FilesPanel extends ATitledPanel implements IRaptorLifecycleListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FileEntityDefinition entityDefinition = new FileEntityDefinition();
    private final EntityTableModel<IFileMapObject> tableModel = new EntityTableModel<>(entityDefinition);
    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::updateSummary);
    private IVirtualFileSystem fileSystem;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    public FilesPanel()
    {
        super(ButtonType.FILES.getDisplayText());
        initComponents();

        table.setModel(tableModel);

        table.getSummaryColumn().setVisible(true);
        table.getIconColumn().setVisible(true);
        table.getActionsColumn().setVisible(true);
        table.requireColumn(entityDefinition.typeColumn).setVisible(false);
        table.requireColumn(entityDefinition.sizeColumn).setVisible(false);

        importButton.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);
        table.getTableHeader().addActionButton(importButton);
        table.setDropMode(DropMode.INSERT_ROWS);
    }

    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        fileSystem = projectVFSManager.getVirtualFileSystemForAttachments();

        table.setTransferHandler(new TransferHandler()
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
            public int getSourceActions(JComponent component)
            {
                return COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent component)
            {
                List<File> files = new ArrayList<>();
                List<IFileMapObject> mapObjects = new ArrayList<>();
                mapObjects.add(table.getSelectedEntity());
                var transferable = new RaptorTransferable();
                transferable.setTransferData(DataFlavor.javaFileListFlavor, files);
                transferable.setTransferData(ApplicationDataFlavor.geoFileListFlavor, mapObjects);
                return transferable;
            }
        });
    }

    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        tableModel.clear();
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

    @Override
    public void releaseResources()
    {
        coordinateSystemMonitor.dispose();
    }

    @InvokeOnEDT
    private void updateSummary(Object ignored)
    {
        tableModel.fireTableColumnUpdated(entityDefinition.getSummaryColumn());
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

    private void tableMouseClicked(MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)
        {
            var point = e.getPoint();
            var row = table.rowAtPoint(point);
            var col = table.columnAtPoint(point);
            if (row != -1 && col != -1)
            {
                if (table.convertColumnIndexToModel(col) == tableModel.getColumnIndex(entityDefinition.getSummaryColumn()))
                {
                    var mapObject = tableModel.getRow(table.convertRowIndexToModel(row));
                    FileUtils.launchFile(mapObject.getVfsFile().getFile());
                }
            }
        }
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added.
     *
     * @param event The event providing the added map object
     * @since 2.38
     */
    @InvokeOnEDT
    private void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        if (event.getMapObject() instanceof IFileMapObject file)
        {
            tableModel.addEntity(file);
        }
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    @InvokeOnEDT
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        if (event.getMapObject() instanceof IFileMapObject file)
        {
            tableModel.removeEntity(file);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panelButtons = new JPanel();
        var scrollPane = new JScrollPane();
        table = new CommonEntityTable<>();
        importButton = new JButton();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== panelButtons ========
        {
            panelButtons.setVisible(false);
            panelButtons.setName("panelButtons");
            panelButtons.setLayout(new FormLayout(
                    "3*(default, $lcgap), default",
                    "fill:default"));
        }
        add(panelButtons, BorderLayout.NORTH);

        //======== scrollPane ========
        {
            scrollPane.setName("scrollPane");

            //---- table ----
            table.setFocusable(false);
            table.setDragEnabled(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowSelectionAllowed(false);
            table.setTextLinesForRowHeight(3);
            table.setName("table");
            table.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    tableMouseClicked(e);
                }
            });
            scrollPane.setViewportView(table);
        }
        add(scrollPane, BorderLayout.CENTER);

        //---- importButton ----
        importButton.setText("Add");
        importButton.setToolTipText("Add a File to this project. Once a file is added, you can drag and drop the file from the left panel to place it on the globe.");
        importButton.setName("importButton");
        importButton.addActionListener(e -> importButtonActionPerformed());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private CommonEntityTable<IFileMapObject> table;
    private JButton importButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
