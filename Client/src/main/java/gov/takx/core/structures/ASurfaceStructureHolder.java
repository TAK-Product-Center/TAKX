package gov.takx.core.structures;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import gov.nasa.worldwind.Exportable;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.terrain.Terrain;
import gov.nasa.worldwind.util.CompoundVecBuffer;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.structures.IPersistableSurfaceStructure;
import gov.takx.api.structures.ISurfaceStructure;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Abstract base for Surface Structure holders.  They are "holders" since each is a composite object, with references
 * to the renderable element representing the structure and to the persistable state of the structure.
 * <p>
 * This base handles the common elements, such as renderable attributes.
 * Many of the setter methods here simply redirect to the persistable entity.
 *
 * @since 3.0
 */
public abstract class ASurfaceStructureHolder<R extends OrderedRenderable, P extends IPersistableSurfaceStructure> implements ISurfaceStructure
{
    private static final ShapeAttributes SELECTED_ATTRIBUTES = makeShapeAttributes(Color.PINK, Color.RED);
    private static final ShapeAttributes HIGHLIGHT_ATTRIBUTES = makeShapeAttributes(Color.DARK_GRAY, Color.CYAN);
    private static final ShapeAttributes DEFAULT_ATTRIBUTES = makeShapeAttributes(Color.LIGHT_GRAY, Color.DARK_GRAY);

    private static final double DEFAULT_INTERIOR_OPACITY = .65;

    protected final Sector sector;
    protected final P persistable;
    protected R renderable; // Not final, it can be cleared to conserve memory

    protected ShapeAttributes attributes = DEFAULT_ATTRIBUTES;
    protected boolean selected;

    /**
     * @return shape attributes with the given interior and outline colors.
     */
    private static ShapeAttributes makeShapeAttributes(Color interiorColor, Color outlineColor)
    {
        final ShapeAttributes attributes = new BasicShapeAttributes();

        attributes.setOutlineMaterial(new Material(outlineColor));
        attributes.setInteriorMaterial(new Material(interiorColor));
        attributes.setInteriorOpacity(DEFAULT_INTERIOR_OPACITY);

        return attributes;
    }

    /**
     * @param sector      Bounding sector for this structure
     * @param persistable Persistable state
     */
    protected ASurfaceStructureHolder(Sector sector, P persistable)
    {
        this.sector = sector;
        this.persistable = persistable;
    }

    @Override
    public Renderable getRenderable()
    {
        if (renderable != null) return renderable;

        renderable = makeRenderable();

        applyAttributesToRenderable(attributes);

        return renderable;
    }

    /**
     * @return Construct our renderable from our persistable state.
     */
    protected abstract R makeRenderable();

    @Override
    public Sector getSector()
    {
        return sector;
    }

    @Override
    public UUID getUUID()
    {
        return persistable.getUUID();
    }

    @Override
    public IPersistableSurfaceStructure getPersistable()
    {
        return persistable;
    }

    @Override
    public void setLayer(IRaptorLayer raptorLayer)
    {
        persistable.setLayer(raptorLayer);
    }

    @Override
    public void setName(String name)
    {
        persistable.setName(name);
    }

    @Override
    public void setDescription(String description)
    {
        persistable.setDescription(description);
    }

    @Override
    public String getUrl()
    {
        return persistable.getUrl();
    }

    @Override
    public String getName()
    {
        return persistable.getName();
    }

    @Override
    public String getDescription()
    {
        return persistable.getDescription();
    }

    @Override
    public IRaptorLayer getLayer()
    {
        return persistable.getLayer();
    }

    @Override
    public ILocation getLocation()
    {
        return persistable.getLocation();
    }

    @Override
    public void setUrl(String url)
    {
        persistable.setUrl(url);
    }

    @Override
    public void dispose()
    {
        renderable = null;
    }

    @Override
    public AttributeSet getMetadata()
    {
        return persistable.getMetadata();
    }

    @Override
    public void setSelected(boolean selected)
    {
        this.selected = selected;
        saveAndApplyAttributes(this.selected ? SELECTED_ATTRIBUTES : DEFAULT_ATTRIBUTES);
    }

    @Override
    public ShapeAttributes getAttributes()
    {
        return attributes;
    }

    /**
     * Set default attributes for a new surface structure. Defaults to gray interior with dark gray outline.
     */
    public void setDefaultAttributes()
    {
        saveAndApplyAttributes(DEFAULT_ATTRIBUTES);
    }

    /**
     * Record the given attributes and then apply them to the renderable, if it is present.
     *
     * @param attributes Attributes to apply
     */
    protected void saveAndApplyAttributes(ShapeAttributes attributes)
    {
        this.attributes = attributes;

        if (renderable != null) applyAttributesToRenderable(attributes);
    }

    /**
     * Apply the give attributes to the actual renderable.  Since the renderable types have no common base
     * class/interface that handles attributes, we need to delegate upwards to handle this.
     *
     * @param attributes Attributes to apply
     */
    protected abstract void applyAttributesToRenderable(ShapeAttributes attributes);

    @Override
    public void resetAttributes()
    {
        saveAndApplyAttributes(selected ? SELECTED_ATTRIBUTES : DEFAULT_ATTRIBUTES);
    }

    @Override
    public void setStructureAttributes(AttributeStyle attributeStyle)
    {
        switch (attributeStyle)
        {
            case DEFAULT -> saveAndApplyAttributes(DEFAULT_ATTRIBUTES);
            case HIGHLIGHTED -> saveAndApplyAttributes(HIGHLIGHT_ATTRIBUTES);
            case SELECTED -> saveAndApplyAttributes(SELECTED_ATTRIBUTES);
        }
    }

    @Override
    public String isExportFormatSupported(String s)
    {
        return Exportable.FORMAT_NOT_SUPPORTED;
    }

    @Override
    public void export(String s, Object o) throws IOException, UnsupportedOperationException
    {
    }

    @Override
    public boolean isIntersectable()
    {
        return false; // Default to not intersectable
    }

    @Override
    public List<Intersection> intersect(Line line, Terrain terrain) throws InterruptedException
    {
        return null;
    }

    /**
     * Create a block locations list from a given compound vec buffer.  Each sub-buffer becomes a sub-list
     * of the created block locations.  The given mapping function is applied to each vertex in the vec buffer.
     *
     * @param compoundVecBuffer Vec buffer to process
     * @param mapper            Mapping function
     * @param <L>               Inferred type for resulting block locations list elements
     * @return Block locations
     */
    protected static <L extends LatLon> List<List<L>> vecBufferToBlockLocations(CompoundVecBuffer compoundVecBuffer,
                                                                                Function<LatLon, L> mapper)
    {
        final int numberOfBlocks = compoundVecBuffer.size();
        final List<List<L>> blockLocations = new ArrayList<>(numberOfBlocks);

        // Collect the vertex locations for our blocks
        for (int i = 0; i < numberOfBlocks; i++)
        {
            final List<L> locations = StreamSupport
                    .stream(compoundVecBuffer.subBuffer(i).getLocations().spliterator(), false)
                    .map(mapper)
                    .collect(Collectors.toList());

            blockLocations.add(locations);
        }

        return blockLocations;
    }

    /**
     * Create a block locations list from the given KML polygon geometry.  Each boundary becomes a sub-list
     * of the created block locations.
     *
     * @param kmlPolygon Polygon geometry to process
     * @param <L>        Inferred type for resulting block locations list elements
     * @return Block locations
     */
    protected static <L extends LatLon> List<List<L>> kmlToBlockLocations(Polygon kmlPolygon, boolean asPosition)
    {
        final List<List<L>> blockLocations = new ArrayList<>();

        // Add the outer boundary first
        blockLocations.add(linearRingCoordinateList(kmlPolygon.getOuterBoundaryIs().getLinearRing(), kmlPolygon.getAltitudeMode(), asPosition));

        // Then add each inner boundary
        final var innerBoundaries = kmlPolygon.getInnerBoundaryIs();
        for (var boundary : innerBoundaries)
        {
            blockLocations.add(linearRingCoordinateList(boundary.getLinearRing(), kmlPolygon.getAltitudeMode(), asPosition));
        }

        return blockLocations;
    }

    /**
     * Convert the coordinates of the given {@code ring} to either a {@link LatLon} or {@link Position}, according to {@code asPosition}.
     *
     * @param ring Ring to process
     * @param asPosition Pass {@code true} to convert coordinates to {@link Position}, {@code false} to convert to {@link LatLon}
     * @return List of appropriate type
     * @param <L> Inferred list element type
     * @since 5.5
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static <L extends LatLon> List<L> linearRingCoordinateList(@Nonnull LinearRing ring, @Nullable AltitudeMode altitudeMode, boolean asPosition)
    {
        return (List<L>) ring.getCoordinates().stream()
                .map(coordinate -> asPosition ? coordinateToPosition(coordinate, altitudeMode) : coordinateToLatLon(coordinate))
                .toList();
    }

    /**
     * @return Convert the given {@link Coordinate} to a {@link LatLon}
     * @since 5.5
     */
    @Nonnull
    public static LatLon coordinateToLatLon(@Nonnull Coordinate coordinate)
    {
        return new LatLon(Angle.fromDegreesLatitude(coordinate.getLatitude()), Angle.fromDegreesLongitude(coordinate.getLongitude()));
    }

    /**
     * @return Convert the given {@link Coordinate} to a {@link Position}
     * @since 5.5
     */
    @Nonnull
    public static Position coordinateToPosition(@Nonnull Coordinate coordinate, @Nullable AltitudeMode altitudeMode)
    {
        double altitude = altitudeMode == AltitudeMode.ABSOLUTE ? GeoCalculations.mslToHae(coordinate.getLatitude(), coordinate.getLongitude(), coordinate.getAltitude()) : coordinate.getAltitude();
        return new Position(Angle.fromDegreesLatitude(coordinate.getLatitude()), Angle.fromDegreesLongitude(coordinate.getLongitude()), altitude);
    }
}
