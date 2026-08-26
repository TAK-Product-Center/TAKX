package gov.takx.core.globe;

import com.google.common.collect.Lists;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.globe.tak.PickedObjectManager;
import gov.takx.core.view.ApplicationDataFlavor;
import gov.takx.core.view.gui.maps.importers.FileImporter;
import gov.takx.core.view.gui.maps.importers.PluginJarImporter;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Handler for Drag and Drop actions when the globe is the drop target.
 *
 * @since 2.37 (pulled out from RaptorGlobe)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class GlobeDropTargetHandler extends DropTargetAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String GEO_FILE_LOCATION_CHANGE_PROMPT =
            "<html><font color=\"red\">Warning:</font> Do you want to override the existing location for </html>";
    private static final String GEO_FILE_LOCATION_CHANGE_PROMPT_TITLE = "Confirm GeoFile Location Change";
    private static final Object[] GEO_FILE_LOCATION_CHANGE_OPTIONS = {"Yes", "No"};

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private PickedObjectManager pickedObjectManager;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    private final IApplicationGlobe globe;

    GlobeDropTargetHandler(IApplicationGlobe globe)
    {
        this.globe = globe;
    }

    @Override
    public void dragEnter(DropTargetDragEvent event)
    {
        handleDrag(event);
    }

    @Override
    public void dragOver(DropTargetDragEvent event)
    {
        handleDrag(event);
    }

    @Override
    public void drop(DropTargetDropEvent event)
    {
        if (canImport(event.getTransferable()))
        {
            event.acceptDrop(DnDConstants.ACTION_COPY);
            event.dropComplete(importData(event.getTransferable(), event.getLocation()));
        } else
        {
            event.rejectDrop();
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event)
    {
        handleDrag(event);
    }

    private boolean canImport(Transferable transferable)
    {
        return transferable.isDataFlavorSupported(ApplicationDataFlavor.pluginDefinitionFlavor)
                || transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @SuppressWarnings("unchecked")
    private boolean importData(Transferable transferable, Point point)
    {
        try
        {
            final var location = globe.getGlobeLocationFromPoint(point);

            if (transferable.isDataFlavorSupported(ApplicationDataFlavor.pluginDefinitionFlavor))
            {
                final IPluginDefinition<?> definition = (IPluginDefinition<?>) transferable.getTransferData(ApplicationDataFlavor.pluginDefinitionFlavor);

                if (definition.getPluginCategory() == PluginCategory.DevicePlugin)
                {
                    return mapObjectManager.createDevice((IDevicePluginDefinition) definition, location);
                } else
                {
                    final MapObjectSpec spec = MapObjectSpec.builder(definition).location(location).build();
                    mapObjectManager.getOrCreateMapObject(spec);
                    return true;
                }
            } else if (transferable.isDataFlavorSupported(ApplicationDataFlavor.geoFileListFlavor))
            {
                // When dragging a file map object from the files panel onto the globe, a prompt will be displayed
                // whenever the file is already on the globe. This keeps a user from accidentally overwriting the
                // location of the file when dragging from the files panel. This only occurs when selecting and
                // dragging from the files panel to the globe, and will not occur when selecting it from the globe
                // and dragging it to another spot on the globe.

                for (IFileMapObject file : (List<IFileMapObject>) transferable.getTransferData(ApplicationDataFlavor.geoFileListFlavor))
                {
                    if (file.hasLocation())
                    {
                        // file map object is already on the globe, so prompt the location change box
                        int overwriteLocation = AlwaysOnTopJOptionPane.showOptionDialog(JFrame.getFrames()[0],
                                GEO_FILE_LOCATION_CHANGE_PROMPT + "\n" + file.getName() + "?",
                                GEO_FILE_LOCATION_CHANGE_PROMPT_TITLE, JOptionPane.WARNING_MESSAGE,
                                JOptionPane.YES_NO_OPTION, null, GEO_FILE_LOCATION_CHANGE_OPTIONS, JOptionPane.NO_OPTION);

                        if (overwriteLocation == JOptionPane.YES_OPTION)
                        {
                            file.getController().sendLocationChangeMessage(location);
                        }
                    } else
                    {
                        // file is not on the globe yet, go ahead and set the location like normal
                        file.getController().sendLocationChangeMessage(location);
                    }
                }

                return true;
            } else if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
            {
                final var fileSystem = projectVFSManager.getVirtualFileSystemForAttachments();
                final var fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                final boolean allJars = fileList.stream().allMatch(file -> "jar".equals(FileUtils.getExtension(file)));
                if (allJars)
                {
                    final String message = """
                            You have dropped jar file(s) on the globe.

                            Do you want want to deploy them as plugins?""";

                    final int response = AlwaysOnTopJOptionPane.showConfirmDialog(null, message);
                    if (response == JOptionPane.CANCEL_OPTION) return true;

                    if (response == JOptionPane.YES_OPTION)
                    {
                        logger.debug("Install plugin jars: {}", fileList);
                        new PluginJarImporter(fileList).doImport();
                        return true;
                    }
                    // Otherwise just handle them as any other file
                }

                final var mapObject = getPickedMapObject();
                logger.debug("Import file list, location {}, key {}: {}", location, mapObject, fileList);
                new FileImporter(fileList, fileSystem, location, mapObject).doImport();

                return true;
            }
        } catch (UnsupportedFlavorException | IOException exception)
        {
            logger.error("Error:", exception);
        }

        return false;
    }

    /**
     * @return The Map Object under the mouse when the drop occurs, or {@code null} if none.
     * @since 5.7
     */
    @Nullable
    private IMapObject getPickedMapObject()
    {
        // The "top" object is the last in the list
        final var pickedObjectList = Lists.reverse(pickedObjectManager.getPickedObjectList());

        final var pickedObject = pickedObjectList.stream().filter(po -> !po.terrain()).findFirst().orElse(null);
        final var userObject = pickedObject != null ? pickedObject.userObject() : null;
        return userObject instanceof IGlobeMapObject gmo ? gmo.getMapObject() : null;
    }

    private void handleDrag(DropTargetDragEvent event)
    {
        final var point = event.getLocation();
        final var location = globe.getGlobeLocationFromPoint(point);

        // Apply a "pick" operation at this location.
        // This will cause a map object to be highlighted if the dragged object hovers over it.
        pickedObjectManager.pick(PickedObjectManager.makePickContextForPoint(globe, point));

        final Transferable transferable = event.getTransferable();
        final boolean canImport = canImport(transferable);

        if (logger.isDebugEnabled())
        {
            logger.debug("pos {}, canImport {}, flavors {}", location, canImport, transferable.getTransferDataFlavors());
        }

        // Devices and geoFiles require a globe location, an external file list does not
        final boolean noLocationRequired = transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor);

        if (canImport && (location != null || noLocationRequired))
        {
            event.acceptDrag(DnDConstants.ACTION_COPY);
        } else
        {
            event.rejectDrag();
        }
    }
}
