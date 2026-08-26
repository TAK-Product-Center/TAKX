package gov.takx.core.kml;

import com.google.common.annotations.VisibleForTesting;
import gov.nasa.worldwind.Exportable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ogc.kml.KMLConstants;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.shapes.IShape;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.StatusDialog;
import gov.takx.core.globe.layers.GlobeLayer;
import gov.takx.core.globe.layers.IGlobeLayer;
import gov.takx.core.globe.layers.WWGlobeLayer;
import gov.takx.core.placemarkoverlay.CSVPlaceMark;
import gov.takx.core.surfaces.AnalyticSurface;
import gov.takx.core.view.gui.data.kml.KmlExportTreeManager;
import gov.takx.platform.kml.IExportDocumentBuilder;
import gov.takx.platform.kml.KmlExportDocumentBuilder;
import gov.takx.platform.kml.KmzExportDocumentBuilder;
import gov.takx.platform.kml.KmzUtils;
import gov.takx.platform.layer.WorldWindRenderableLayer;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Utility class for exporting everything on the globe to a kmz file.  Note:  Not thread safe.
 *
 * @since 1.21 (completely reworked in 5.5.1)
 */
@ApplicationScoped
public class KmlExporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String APP_NAME = PreferenceEntry.NAME.getStringValue();

    // The layer groups that we support for export
    private static final Set<LayerGroup> SUPPORTED_LAYERS =
            Set.of(LayerGroup.ANALYTIC_SURFACES, LayerGroup.MAP_DATA, LayerGroup.PLACEMARK_OVERLAY, LayerGroup.SURFACE_IMAGERY, LayerGroup.UNLISTED_OVERLAY);

    private final IMapObjectManager mapObjectManager;
    private final IExtendedGlobeManager globeManager;

    @Inject
    KmlExporter(IMapObjectManager mapObjectManager, IExtendedGlobeManager globeManager)
    {
        this.mapObjectManager = mapObjectManager;
        this.globeManager = globeManager;
    }

    /**
     * Iterates through the Globe Layers, map objects, and shapes. Presents the user with a dialog of all possible items
     * than can be exported to KML for selection.
     */
    public void exportKML()
    {
        final var rootNode = new DefaultMutableTreeNode("Export ALL");

        addExportableLayers(rootNode);
        addExportableMapObjects(rootNode);
        final boolean hasExportableShapes = addExportableShapes(rootNode);

        final int rootNodeChildCount = rootNode.getChildCount();

        if (rootNodeChildCount == 0)
        {
            AlwaysOnTopJOptionPane.showMessageDialog(JFrame.getFrames()[0], "Nothing on the globe to export", "KML/KMZ Export", JOptionPane.INFORMATION_MESSAGE);
        } else
        {
            startExport(rootNode, rootNodeChildCount == 1 && hasExportableShapes);
        }
    }

    /**
     * Add all the layers that contain exportable renderables to the tree.
     */
    private void addExportableLayers(DefaultMutableTreeNode rootNode)
    {
        for (var globeLayer : globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager().getGlobeLayers())
        {
            final var raptorLayer = globeLayer.getRaptorLayer();
            if (logger.isDebugEnabled())
            {
                logger.debug("Layer [{}] of layer group [{}] of type [{}] visibility = [{}]",
                        raptorLayer.getName(), raptorLayer.getLayerGroup(), raptorLayer.getType(), raptorLayer.isVisible());
            }

            if (raptorLayer.isVisible() && SUPPORTED_LAYERS.contains(raptorLayer.getLayerGroup()))
            {
                final var renderables = getExportableRenderables(globeLayer);
                if (logger.isDebugEnabled()) logger.debug("{} has {} exportable Renderables", globeLayer.getName(), renderables.size());

                if (!renderables.isEmpty())
                {
                    final String displayName = raptorLayer.getDisplayName() + " (" + renderables.size() + " items)";
                    rootNode.add(new DefaultMutableTreeNode(new LayerTreeEntry(displayName, new LayerRenderables(globeLayer.getName(), renderables))));
                }
            }
        }
    }

    /**
     * Add all the exportable Map Objects to the tree.
     */
    private void addExportableMapObjects(DefaultMutableTreeNode rootNode)
    {
        final var mapObjectRootNode = new DefaultMutableTreeNode("Map Objects");

        for (var mapObject : mapObjectManager.getMapObjects())
        {
            // Only export visible map objects.
            if (KmlMapObjectWrapper.isSupported(mapObject) && mapObject.isVisible() && mapObject.hasLocation())
            {
                mapObjectRootNode.add(new DefaultMutableTreeNode(new MapObjectTreeEntry(mapObject.getName(), mapObject)));

                if (mapObjectRootNode.getParent() == null) rootNode.add(mapObjectRootNode);
            }
        }
    }

    /**
     * Add all the exportable Shapes to the tree.
     */
    private boolean addExportableShapes(DefaultMutableTreeNode rootNode)
    {
        final var shapesRootNode = new DefaultMutableTreeNode("Shapes");
        boolean hasExportableShapes = false;

        for (var globeShapeMapObject : globeManager.getPrimaryGlobeDelegate().getGlobeMapObjectManager().getShapeMapObjects())
        {
            final var shapeModel = globeShapeMapObject.requireShapeModel();
            if (ShapeModelKmlExportable.isSupported(shapeModel))
            {
                shapesRootNode.add(new DefaultMutableTreeNode(new ShapeTreeEntry(shapeModel.getName(), shapeModel)));
                hasExportableShapes = true;
            }
        }

        if (hasExportableShapes) rootNode.add(shapesRootNode);

        return hasExportableShapes;
    }

    /**
     * Handle the confusion of layer classes to retrieve the exportable renderables they might contain.
     *
     * @param sourceLayer Layer to process
     * @return Collection of {@link Exportable} renderables contained within the native layer underlying the {@code sourceLayer}
     */
    @SuppressWarnings("deprecation")
    private List<Exportable> getExportableRenderables(IGlobeLayer sourceLayer)
    {
        final Collection<Renderable> renderables;

        if (sourceLayer instanceof GlobeLayer globeLayer && globeLayer.getTakLayer() instanceof WorldWindRenderableLayer worldWindRenderableLayer)
        {
            renderables = worldWindRenderableLayer.getRenderables();
        } else if (sourceLayer instanceof WWGlobeLayer wwGlobeLayer && wwGlobeLayer.getLayer() instanceof RenderableLayer renderableLayer)
        {
            renderables = StreamSupport.stream(renderableLayer.getRenderables().spliterator(), false).toList();
        } else
        {
            return List.of();
        }

        return renderables.stream()
                .filter(renderable -> renderable instanceof Exportable)
                .map(Exportable.class::cast)
                .toList();
    }

    /**
     * Display the export selection tree, collect the selected objects, ask the user where to write the export, and
     * launch the background thread to do the export.
     *
     * @param rootNode Root node of the selection tree
     * @param shapesOnly Pass {@code true} if we are only exporting shapes (exports to KML instead of KMZ)
     */
    @InvokeOnEDT
    private void startExport(DefaultMutableTreeNode rootNode, boolean shapesOnly)
    {
        final List<LayerRenderables> selectedLayers = new ArrayList<>();
        final List<IMapObject> selectedMapObjects = new ArrayList<>();
        final List<IShape> selectedShapes = new ArrayList<>();

        getExportSelection(rootNode, selectedLayers, selectedMapObjects, selectedShapes);

        if (selectedLayers.isEmpty() && selectedMapObjects.isEmpty() && selectedShapes.isEmpty())
        {
            AlwaysOnTopJOptionPane.showMessageDialog(JFrame.getFrames()[0], "Nothing selected to export", "KML/KMZ Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final var exportFile = shapesOnly ? KmzUtils.exportKMLRequest() : KmzUtils.exportKMZRequest();
        if (exportFile != null)
        {
            final String extension = shapesOnly ? "KML" : "KMZ";
            final var statusDialog = new StatusDialog(true);
            statusDialog.setDialogTitle("Export");
            statusDialog.setStatusText("Exporting to " + extension);
            statusDialog.setVisible(true);

            new Thread(() -> doExport(shapesOnly, exportFile, selectedShapes, selectedLayers, selectedMapObjects, statusDialog, extension), "KML/KMZ Export: " + exportFile.getName())
                    .start();
        }
    }

    /**
     * Get the selected nodes in the tree (given the {@code rootNode}), and populate the appropriate lists.
     *
     * @param rootNode Root tree node from which to descend
     * @param selectedLayers Selected globe layers are added to this list
     * @param selectedMapObjects Selected Map Objects are added to this list
     * @param selectedShapes Selected shapes are added to this list
     * @since 5.5.1
     */
    private void getExportSelection(DefaultMutableTreeNode rootNode, List<LayerRenderables> selectedLayers,
                                    List<IMapObject> selectedMapObjects, List<IShape> selectedShapes)
    {
        // Just instantiating this displays a model dialog
        final var kmlExportTreeManager = new KmlExportTreeManager(rootNode);
        if (!kmlExportTreeManager.isOkay()) return;

        final var checkedPaths = kmlExportTreeManager.getSelectionModel().getSelectionPaths();
        if (checkedPaths != null)
        {
            // Fill out the selection lists from the tree selection paths
            for (var path : checkedPaths)
            {
                final var node = (DefaultMutableTreeNode) path.getLastPathComponent();

                for (var selectedObject : kmlExportTreeManager.getSelectedUserObjectsFromNode(node))
                {
                    if (selectedObject instanceof LayerTreeEntry layerTreeEntry)
                    {
                        selectedLayers.add(layerTreeEntry.layerRenderables);
                    } else if (selectedObject instanceof MapObjectTreeEntry mapObjectTreeEntry)
                    {
                        selectedMapObjects.add(mapObjectTreeEntry.mapObject);
                    } else if (selectedObject instanceof ShapeTreeEntry shapeTreeEntry)
                    {
                        selectedShapes.add(shapeTreeEntry.shape);
                    }
                }
            }
        }
    }

    /**
     * Perform the actual export given the lists of selected objects.
     */
    private void doExport(boolean shapesOnly, File exportFile, List<IShape> shapes, List<LayerRenderables> layers,
                          List<IMapObject> mapObjects, StatusDialog statusDialog, String extension)
    {
        Thread.currentThread().setName("KmlExporter");
        try
        {
            boolean success = shapesOnly
                    ? writeKmlData(exportFile, shapes)
                    : writeKmzData(exportFile, layers, mapObjects, shapes);

            statusDialog.setVisible(false);

            if (success)
            {
                AlwaysOnTopJOptionPane.showMessageDialog("Exported " + extension + " to " + exportFile.getAbsolutePath(), extension + " Export", JOptionPane.INFORMATION_MESSAGE);
            } else
            {
                AlwaysOnTopJOptionPane.showErrorDialog("Error Exporting " + extension + " data to file", extension + " Export");
            }
        } catch (IOException e)
        {
            statusDialog.setVisible(false);
            logger.warn("Failed to export to {}", exportFile, e);
            AlwaysOnTopJOptionPane.showErrorDialog("Error exporting to " + exportFile.getAbsolutePath(), "Error");
        }
    }

    /**
     * Write the specified globe layers, map objects, and shapes to a kmz file.
     *
     * @param kmzFile file to write to
     * @param layers globe layers to export
     * @param mapObjects map objects to export
     * @param shapes shapes to export
     * @return true on success, false on error
     */
    @VisibleForTesting
    boolean writeKmzData(File kmzFile, List<LayerRenderables> layers, List<IMapObject> mapObjects, List<IShape> shapes)
    {
        try
        {
            // Create a document builder that will write KML to the StringWriter
            final var kmlBuilder = new KmzExportDocumentBuilder(kmzFile, new StringWriter());

            // Export the shapes, this has to be done before the globe layers otherwise the layers will cover the shapes
            exportShapes(shapes, kmlBuilder, true);
            exportMapObjects(mapObjects, kmlBuilder);
            exportGlobeLayers(layers, kmlBuilder);

            kmlBuilder.close();
        } catch (Exception e)
        {
            logger.error("Error Exporting KMZ", e);
            return false;
        }
        return true;
    }

    /**
     * Export Shapes only to KML (instead of KMZ)
     *
     * @param kmlFile file to export to
     * @param shapes shapes to export
     * @throws IOException exception writing to file
     */
    @VisibleForTesting
    boolean writeKmlData(File kmlFile, List<IShape> shapes) throws IOException
    {
        try
        {
            final var kmlBuilder = new KmlExportDocumentBuilder(new StringWriter(), kmlFile);
            exportShapes(shapes, kmlBuilder, false);
            kmlBuilder.close();
        } catch (XMLStreamException e)
        {
            logger.error("XML Error Exporting KML", e);
            return false;
        }
        return true;
    }

    /**
     * Export the specified Shapes to KMZ (or KML).
     *
     * @param shapes the shapes to export
     * @param kmlBuilder the KML document builder.
     * @param nestInFolder whether to nest the shapes in a folder.
     * @throws XMLStreamException XML Exception
     * @throws IOException IO Exception writing the document
     */
    private void exportShapes(List<IShape> shapes, IExportDocumentBuilder kmlBuilder, boolean nestInFolder) throws XMLStreamException, IOException
    {
        // Next, export the shapes in the form of IShapes
        for (var shape : shapes)
        {
            if (!kmlBuilder.isFolderOpen() && nestInFolder) kmlBuilder.openFolder("Shapes", "");
            kmlBuilder.writeObject(new ShapeModelKmlExportable(shape));
        }
        // close the kml folder if one is open
        if (kmlBuilder.isFolderOpen()) kmlBuilder.closeFolder();
    }

    /**
     * Export the specified Map Objects to KMZ.
     *
     * @param mapObjects the map objects to export
     * @param kmlBuilder the KML document builder to write to
     * @throws XMLStreamException XML Exception
     * @throws IOException IO Exception writing the document
     */
    private void exportMapObjects(List<IMapObject> mapObjects, IExportDocumentBuilder kmlBuilder) throws XMLStreamException, IOException
    {
        for (var mapObject : mapObjects)
        {
            final var exportableMapObject = new KmlMapObjectWrapper(mapObject.getController(), KmzUtils.getKMZTempFolder());

            if (Exportable.FORMAT_SUPPORTED.equals(exportableMapObject.isExportFormatSupported(KMLConstants.KML_MIME_TYPE)))
            {
                try
                {   // group the map objects in a KML folder
                    if (!kmlBuilder.isFolderOpen()) kmlBuilder.openFolder("Map Objects", APP_NAME + " Map Objects");

                    kmlBuilder.writeObject(exportableMapObject);
                } catch (UnsupportedOperationException ignored)
                {
                    // No need to print the exception... just means that export has not been defined for this type of
                    // map object yet.
                    logger.warn("KML Export not supported for item {}", exportableMapObject.getMapObject().getKey());
                }
            } else
            {
                logger.warn("Map Object {} not exportable at this time", exportableMapObject);
            }
        }
        // close the kml folder if one is open
        if (kmlBuilder.isFolderOpen()) kmlBuilder.closeFolder();
    }

    /**
     * Export the specified Globe Layers to KMZ.
     *
     * @param layers the globe layers to export
     * @param kmlBuilder the KML document builder to write to
     * @throws XMLStreamException XML Exception
     * @throws IOException IO Exception writing the document
     */
    private void exportGlobeLayers(List<LayerRenderables> layers, IExportDocumentBuilder kmlBuilder) throws XMLStreamException, IOException
    {
        for (var layerRenderables : layers)
        {
            final String layerName = layerRenderables.layerName;

            for (var exportable : layerRenderables.exportables)
            {
                if (exportable instanceof SurfaceImage surfaceImage)
                {
                    kmlBuilder.writeObject(new KmlSurfaceImageWrapper(surfaceImage, kmlBuilder.getKmzTempFolder()));
                } else if (exportable instanceof AnalyticSurface analyticSurface)
                {
                    kmlBuilder.writeObject(new KmlAnalyticSurfaceWrapper(analyticSurface, kmlBuilder.getKmzTempFolder()));
                } else if (exportable instanceof CSVPlaceMark csvPlaceMark)
                {
                    // Group CSVPlaceMark(s) from a layer into a folder as per Linda's request.
                    if (!kmlBuilder.isFolderOpen()) kmlBuilder.openFolder(layerName, "");
                    kmlBuilder.writeObject(new KmlCSVPlaceMarkWrapper(csvPlaceMark, kmlBuilder.getKmzTempFolder()));
                } else
                {
                    // TODO TAKX-2147 rework extruded polygon handling

                    if (Exportable.FORMAT_SUPPORTED.equals(exportable.isExportFormatSupported(KMLConstants.KML_MIME_TYPE)))
                    {
                        try
                        {   // group all the renderables of a layer in a kml folder
                            if (!kmlBuilder.isFolderOpen()) kmlBuilder.openFolder(layerName, "");

                            if (exportable instanceof PointPlacemark pointPlacemark)
                            {
                                adjustPointPlacemarkAttributes(pointPlacemark);
                                final var attributes = pointPlacemark.getAttributes();
                                if (attributes == null || !attributes.isUsePointAsDefaultImage())
                                {   // placemarks that use a point as default image don't translate well at this point in time.
                                    kmlBuilder.writeObject(pointPlacemark);
                                }
                            } else
                            {
                                kmlBuilder.writeObject(exportable);
                            }

                            logger.debug("Exported {}, Name = {}", exportable, layerName);
                        } catch (UnsupportedOperationException ignored)
                        {
                            // No need to print the exception... just means that export has not been defined for this object yet.
                            logger.warn("KML Export not supported for item {}", exportable);
                        }
                    } else
                    {
                        logger.warn("KML Export format not supported for {}", exportable);
                    }
                }
            }
            if (kmlBuilder.isFolderOpen()) kmlBuilder.closeFolder();  // close the kml folder if it is open
        }
    }

    /**
     * Add an 0 image offset to a point placemark, if and only if it has attributes set.  Otherwise, due to a
     * WW bug in 1.23, the export of a placemark will result in an extra "writeEndAttribute" being called in
     * the xmlWriter.
     *
     * @param pointPlacemark placemark
     */
    private void adjustPointPlacemarkAttributes(PointPlacemark pointPlacemark)
    {
        final var attributes = pointPlacemark.getAttributes();

        // placemarks that use a point as default image don't translate well at this point in time.
        if (attributes != null && attributes.getImageOffset() == null)
        {
            attributes.setImageOffset(new Offset(0.0, 0.0, AVKey.INSET_PIXELS, AVKey.INSET_PIXELS));
            pointPlacemark.setAttributes(attributes);
        }
    }

    /** Represents a Layer in the export selection tree. */
    private record LayerTreeEntry(String name, LayerRenderables layerRenderables)
    {
        @Override
        public String toString()
        {
            return name;
        }
    }

    /** Represents a MapObject in the export selection tree. */
    private record MapObjectTreeEntry(String name, IMapObject mapObject)
    {
        @Override
        public String toString()
        {
            return name;
        }
    }

    /** Represents a Shape in the export selection tree. */
    private record ShapeTreeEntry(String name, IShape shape)
    {
        @Override
        public String toString()
        {
            return name;
        }
    }

    private record LayerRenderables(String layerName, List<Exportable> exportables)
    {
    }
}
