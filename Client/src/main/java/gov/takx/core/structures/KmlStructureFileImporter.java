package gov.takx.core.structures;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.WWIO;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.mapobject.CreationFailedException;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.api.structures.ISurfaceStructureCallback;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.layers.LayerImportParameters;
import gov.takx.core.globe.layers.SurfaceImageLayer;
import gov.takx.core.globe.layers.SurfaceImageLayerState;
import gov.takx.core.kml.KmlIconFetcher;
import gov.takx.core.kml.KmlModel;
import gov.takx.core.view.gui.maps.importers.SimpleMapFilesImporter;
import gov.takx.core.view.gui.maps.importers.SurfaceImageryFileImporter;
import gov.takx.core.view.gui.maps.importers.kml.KMLArtifactCopier;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static gov.takx.core.view.gui.maps.importers.SurfaceImageryFileImporter.GEO_FILE_EXTENSION;

/**
 * Surface structure importer. Used to import a KML/KMZ file, in the background, from the plugin api or the app frame. All
 * object creation and persistence is done off of the EDT. The only actions that take place on the EDT are the initial
 * creation of the progress panel for this import, and when the surface structure gets added to the cached renderable
 * layer.
 * 
 * @since 5.5 (extracted from StructureFileImporter, which has been deleted)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class KmlStructureFileImporter extends AStructureImporter<CachedSurfaceStructureLayer>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Pattern FAMILY_IN_DESCRIPTION_PATTERN = Pattern.compile("Family: (.+)<br/>");
    private static final Pattern TYPE_IN_DESCRIPTION_PATTERN = Pattern.compile("<br>Type: (.+)<br/>");

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private KmlIconFetcher iconFetcher;

    private final List<Placemark> placemarks = new ArrayList<>();
    private final List<GroundOverlay> groundOverlays = new ArrayList<>();

    private KmlModel model;

    public KmlStructureFileImporter(@Nonnull File file, @Nonnull IRaptorLayer raptorLayer, @Nullable ISurfaceStructureCallback callback)
    {
        super(file, raptorLayer, callback);
    }

    @Override
    protected boolean prepareForImport()
    {
        try
        {
            if (!makeVfsRootDir(StructureUtils.KML_FILE_DIR)) return false;

            copiedFile = new KMLArtifactCopier().copyKmlArtifacts(originalFile.toPath(), rootVfsImportDir.toPath()).toFile();
            model = KmlModel.parseModel(copiedFile);
            return true;
        } catch (IOException e)
        {
            logger.error("Unable to parse given KML/KMZ file. File name: {} -- Exception: {}", originalFile.getName(), e);
            return false;
        }
    }

    @Override
    protected int countShapes()
    {
        model.visitFeaturesOfType(Placemark.class, placemarks::add);
        model.visitFeaturesOfType(GroundOverlay.class, groundOverlays::add);

        //TODO remove the lines above and uncomment line below once we support all kml placemark types as structures
        return getSupportedFeatureCount();
    }

    @Override
    protected void loadStructures()
    {
        createSurfaceStructures();
    }

    /**
     * After all the structures have been created, we need to add the renderables to the layer.
     */
    @Override
    protected void finalizeImport()
    {
        if (logger.isDebugEnabled()) logger.debug("Finalizing {} shapes", importedStructures.size());

        // Add all the renderables to their layer
        importedStructures.forEach(structureLayer::add);

        // tell the layer that no more structures will be added so that the quad tree may be created with the
        // current bounding sector
        structureLayer.batchAddDone();

        if (structureLayer instanceof ApplicationCachedSurfaceStructureLayer cachedLayer)
        {
            new SimpleMapFilesImporter(cachedLayer.getImageFolder(), cachedLayer.getName()).updateFileSetsAndImport();
        }
    }

    private int getSupportedFeatureCount()
    {
        int count = 0;
        for (Placemark placemark : placemarks)
        {
            final var geometry = placemark.getGeometry();

            //TODO update this each time we add support for other kml structures until we support them all
            if (geometry instanceof Polygon || geometry instanceof LineString || geometry instanceof Point)
            {
                if (model.hasGeometry(placemark)) count++;
            } else if (geometry instanceof MultiGeometry multiGeometry)
            {
                count += countGeometries(model, multiGeometry);
            }
        }

        count += groundOverlays.size(); // Add ground overlays

        return count;
    }

    private int countGeometries(KmlModel model, MultiGeometry multiGeometry)
    {
        int count = 0;
        for (var geom : multiGeometry.getGeometry())
        {
            count += geom instanceof MultiGeometry subGeometry
                    ? countGeometries(model, subGeometry)
                    : (model.geometryIsNotEmpty(geom) ? 1 : 0);
        }

        return count;
    }

    // TODO:  This logic really needs to be consolidated somehow with the login in AddKMLOverlayController where it
    // determines if the KML file has any importable surface structures. I see a potential to have one or the other
    // change and then the initial check determines there are 3D structures but on import does not.
    private void createSurfaceStructures()
    {
        for (var placemark : placemarks)
        {
            if (!model.hasGeometry(placemark)) continue; // Skip features with no geometry

            for (var geometry : model.getGeometry(placemark))
            {
                if (geometry instanceof Point point)
                {
                    createPoint(placemark, point);
                } else if (geometry instanceof LineString lineString)
                {
                    createLineString(placemark, lineString);
                } else if (geometry instanceof Polygon polygon)
                {
                    createPolygon(placemark, polygon);
                } else
                {
                    if (geometry == null)
                    {
                        logger.warn("Null geometry type, skipping creation. Check {}.", originalFile.getName());
                    } else
                    {
                        logger.warn("Unexpected geometry type: {}. Skipping creation.", geometry.getClass().getSimpleName());
                    }
                }
                testAborted();
            }
        }

        for (var overlay : groundOverlays)
        {
            if (model.hasGeometry(overlay)) createGroundOverlay(overlay);
            updateProgress();

            testAborted();
        }
    }

    /**
     * Create a KML Point - currently assumes all points are icons.
     * Should eventually have a default "point" image so if the point doesn't come with its
     * own image, it will render as a standard dot or point, perhaps use PointPlacemark?
     *
     * @param placemark KML Placemark
     * @param point     KML Point geometry from placemark
     */
    private void createPoint(Placemark placemark, Point point)
    {
        /*
         * This is a new capability where a plugin can include family/type/version metadata
         * in the KML it exports. If it does, we can create an actual {@link IMapObject} rather than
         * just importing the icon as a static image ({@link gov.nasa.worldwind.render.SurfaceImage}).
         * @since 1.50.8
         */
        final var desc = placemark.getDescription();
        final var position = point.getCoordinates().get(0);

        final String family = extractMatch(desc, FAMILY_IN_DESCRIPTION_PATTERN);
        final String type = extractMatch(desc, TYPE_IN_DESCRIPTION_PATTERN);

        IMapObject mapObject = null;

        // If we're able to get a valid family and type, create a map object
        if (!StringUtils.isEmpty(family) && !StringUtils.isEmpty(type))
        {
            final var location = new RaptorLocation(position.getLatitude(), position.getLongitude(), position.getAltitude());
            final var spec = MapObjectSpec.builder(family, type).location(location).build();

            try
            {
                mapObject = mapObjectManager.getOrCreateMapObject(spec);
            } catch (CreationFailedException e)
            {
                logger.warn("Failed to create MapObject {}", spec.getKey(), e);
            }
        }

        // If the MO wasn't created, import the icon as a static image.
        if (mapObject == null) createPointImageOverlay(placemark, position);
    }

    /**
     * Given a String, attempt to find the given pattern, and if found return the 1st subgroup.
     * This is a custom match operation for extracting data from a KML Point description.
     *
     * @param description String to process
     * @param pattern Pattern to use for matching
     * @return 1st subgroup if matched, else {@code null}
     * @since 3.0
     */
    private String extractMatch(String description, Pattern pattern)
    {
        if (description == null) return null;

        final var matcher = pattern.matcher(description);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void createPointImageOverlay(Placemark placemark, Coordinate position)
    {
        final var iconStyle = model.findStyleOfType(placemark, IconStyle.class);
        final var icon = iconStyle != null ? iconStyle.getIcon() : null;
        if (icon != null)
        {
            iconFetcher.fetch(model, icon, placemark.getName()).thenAccept(image -> {
                final File imageFile = writeImageToVfs(image);
                if (imageFile != null)
                {
                    // Figure out if the image file contains geolocation (found in a .rxp with the same file name).
                    // If it doesn't, create that info by generating a bounding sector and exporting that as
                    // geolocation data.
                    createGeolocationFileForImage(imageFile, createIconBoundingSector(position));

                    makeSurfaceImageLayer(imageFile, placemark.getName(), false);
                }
            });
        }
    }

    /**
     * To import a surface image, there has to be a geolocation file with the same name as the image
     * file, except with a .rxg extension. This file is very basic, just contains an imagePath and
     * a cornerList property. See {@link SurfaceImageLayerState}
     * <p>
     * The {@link SurfaceImageLayer}.importSurfaceImageryFile method needs this geolocation file to
     * exist otherwise it will just dump the image file onto the map with no control over how large
     * or where it goes.
     * <p/>
     * This method creates that file if it does not already exist.
     *
     * @param pointImageFile Image file from placemark
     * @param sector Sector defining the bounds of the image
     */
    private void createGeolocationFileForImage(File pointImageFile, Sector sector)
    {
        final String geolocationFileName = WWIO.replaceSuffix(pointImageFile.getName(), "." + GEO_FILE_EXTENSION);
        final var geoFile = new File(pointImageFile.getParent(), geolocationFileName);

        if (!geoFile.exists())
        {
            try
            {
                final var surfaceImageLayerState = new SurfaceImageLayerState(pointImageFile.getAbsolutePath(), Arrays.asList(sector.getCorners()));
                final String stateString = surfaceImageLayerState.buildRestorableState();
                Files.writeString(geoFile.toPath(), stateString);
            } catch (IOException e)
            {
                logger.warn("Error processing the geolocation information for the KML surface image.");
            }
        }
    }

    /**
     * Create the bounding sector for the icon image. This is very non-exact, but it works as a plan B
     * when importing Map Objects that aren't recognized as MOs and get imported as static surface
     * image files.
     *
     * @param position Center for bounding sector
     * @return Sector that bounds where we will draw the icon of the {@link IMapObject}
     */
    private Sector createIconBoundingSector(Coordinate position)
    {
        // This is a reasonable size, but is entirely arbitrary since it depends on the zoom level
        final double iconSize = 140d;

        //noinspection DataFlowIssue
        return Sector.boundingSector(globeManager.getLastFocusedGlobe().getWorldWindow().getModel().getGlobe(),
                LatLon.fromDegrees(position.getLatitude(), position.getLongitude()),
                iconSize); // size determined by trying to match MO icon size, but that changes w/ zoom so is very arbitrary
    }

    private void createLineString(Placemark placemark, LineString lineString)
    {
        final var ss = SSExtrudedLine.fromKml(lineString);

        applyPlacemarkValues(placemark, ss);
        addImportedStructure(ss);
    }

    private void createPolygon(Placemark placemark, Polygon polygon)
    {
        final var ss = polygon.getAltitudeMode() == AltitudeMode.CLAMP_TO_GROUND
                ? SSPolygon.fromKml(polygon)
                : SSExtrudedPolygon.fromKml(polygon);

        applyPlacemarkValues(placemark, ss);
        addImportedStructure(ss);
    }

    /**
     * Apply the name and description from the KML placemark to the given surface structure.
     *
     * @param placemark Placemark holding values
     * @param structure structure to update
     * @since 3.0
     */
    private void applyPlacemarkValues(Placemark placemark, ISurfaceStructure structure)
    {
        final String name = placemark.getName();
        if (name != null) structure.setName(name);

        final String description = placemark.getDescription();
        if (description != null) structure.setDescription(description);
    }

    /**
     * Used for analytic surfaces that are represented in KML as images and
     * coordinates that define the bounds (usually corners) of the image.
     *
     * @param overlay KML overlay definition to process
     */
    private void createGroundOverlay(GroundOverlay overlay)
    {
        iconFetcher.fetch(model, overlay.getIcon(), overlay.getName()).thenAccept(image -> {
            final var imageFile = writeImageToVfs(image);
            if (imageFile != null)
            {
                final var box = overlay.getLatLonBox();
                final var boundingSector = new Sector(Angle.fromDegrees(box.getSouth()), Angle.fromDegrees(box.getNorth()),
                        Angle.fromDegrees(box.getWest()), Angle.fromDegrees(box.getEast()));
                createGeolocationFileForImage(imageFile, boundingSector);

                String groundOverlayName = overlay.getName();
                makeSurfaceImageLayer(imageFile, groundOverlayName, true);
            }
        });
    }

    /**
     * Write the given image to our root dir in VFS.
     * @return The file in VFS, or {@code null} if the image write failed
     */
    @Nullable
    private File writeImageToVfs(@Nonnull BufferedImage image)
    {
        final var file = new File(rootVfsImportDir, ImageUtils.generateHexMD5HashFromImage(image) + ".png");
        return ImageUtils.writeBufferedImageToPNGFile(image, file) ? file : null;
    }

    /**
     * Create a surface image layer from the given image.
     *
     * @param imageFile Source image
     * @param name Name to use for the created layer
     * @param asGroundOverlay Pass {@code true} to treat it as a ground overlay vs. a map object/point image
     */
    private void makeSurfaceImageLayer(File imageFile, String name, boolean asGroundOverlay)
    {
        try
        {
            String restorableState = new SurfaceImageryFileImporter(new LayerImportParameters(true)).importSurfaceImageryFile(imageFile);

            IRaptorLayer newLayer = raptorLayerManager.addLayer(
                    name,
                    name,
                    RenderableLayer.class.getName(),
                    true,
                    true,
                    imageFile.getAbsolutePath(),
                    LayerGroup.SURFACE_IMAGERY,
                    null,
                    restorableState,
                    false);

            if (asGroundOverlay)
            {
                // Analytic surfaces should pretty much always be the bottom, this can be manually reordered
                newLayer.setOrderkey(raptorLayerManager.getLayerCount(LayerGroup.SURFACE_IMAGERY));
                newLayer.setOpacity(0.4d);
            }

            newLayer.setEditable(false);
        } catch (IOException e)
        {
            logger.warn("Unable to import surface imagery file for point.", e);
        }

    }
}
