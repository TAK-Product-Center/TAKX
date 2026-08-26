package gov.takx.core.structures;

import de.micromata.opengis.kml.v_2_2_0.LineString;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.takx.api.structures.IPersistableSurfaceStructure;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.dataservices.persist.PersistableExtrudedLine;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Concrete implementation of {@link ISurfaceStructure}.  This implementation uses the WW {@link gov.nasa.worldwind.render.Path} as
 * the WW representation of the structure.
 *
 */
public class SSExtrudedLine extends AExtrudedPolygonalHolder<Path, PersistableExtrudedLine>
{
    /**
     * Factory method to build from a given persistable.
     *
     * @param persistable Persistable entity holding the state from which to build
     * @since 3.0
     */
    static ISurfaceStructure createFromPersistable(IPersistableSurfaceStructure persistable)
    {
        return new SSExtrudedLine((PersistableExtrudedLine) persistable);
    }

    /**
     * Factory method to construct an instance from a KML line string definition.
     *
     * @param lineString kml line string / kml linear ring geometry
     * @return new SSExtrudedLine instance
     * @since 3.0
     */
    public static SSExtrudedLine fromKml(LineString lineString)
    {
        final List<Position> positions = lineString.getCoordinates().stream()
                .filter(Objects::nonNull)
                .map(c -> ASurfaceStructureHolder.coordinateToPosition(c, lineString.getAltitudeMode()))
                .collect(Collectors.toList());

        final Sector sector = Sector.boundingSector(positions);

        final PersistableExtrudedLine persistable = new PersistableExtrudedLine(sector, positions);

        persistable.setAltitudeMode(getAltitudeModeFromKml(lineString.getAltitudeMode(), WorldWind.CLAMP_TO_GROUND));
        persistable.setIsExtrude(lineString.isExtrude());
        persistable.setTessellate(lineString.isTessellate());

        return new SSExtrudedLine(persistable);
    }

    /**
     * Construct from persistable state.
     *
     * @param persistable Persistable holding our configuration
     * @since 3.0
     */
    private SSExtrudedLine(PersistableExtrudedLine persistable)
    {
        super(persistable.getSector(), persistable);
    }

    @Override
    protected Path makeRenderable()
    {
        final ExtrudedLineRenderable renderable = new ExtrudedLineRenderable();

        renderable.setPositions(persistable.getVertices());
        renderable.setExtrude(persistable.isExtrude());
        renderable.setAltitudeMode(persistable.getAltitudeMode());
        renderable.setFollowTerrain(persistable.isTessellate());

        return renderable;
    }

    @Override
    protected void applyAttributesToRenderable(ShapeAttributes attributes)
    {
        renderable.setAttributes(attributes);
    }

    /**
     * Our renderable.
     *
     * @since 3.0
     */
    private class ExtrudedLineRenderable extends Path implements ISurfaceStructureRenderable
    {
        private ExtrudedLineRenderable()
        {
            setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            setExtrude(true);
            setEnableBatchRendering(true);
            setEnableBatchPicking(true);
            setFollowTerrain(true);
            setPathType(AVKey.LINEAR);
        }

        @Override
        public ISurfaceStructure getSurfaceStructure()
        {
            return SSExtrudedLine.this;
        }
    }
}
