package gov.takx.core.structures;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.AbstractShape;
import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.dataservices.persist.AExtrudedPolygonalStructure;

import java.io.IOException;
import java.util.List;

/**
 * Common base for extruded polygonal surface structures.
 *
 * @param <R> The type of our Renderable (Shape)
 * @param <P> The type of our persistable entity
 * @since 3.0
 */
public abstract class AExtrudedPolygonalHolder<R extends AbstractShape, P extends AExtrudedPolygonalStructure> extends ASurfaceStructureHolder<R, P>
{
    /**
     * @param sector      Bounding sector for this structure
     * @param persistable Persistable state
     */
    protected AExtrudedPolygonalHolder(Sector sector, P persistable)
    {
        super(sector, persistable);
    }

    @Override
    public boolean isIntersectable()
    {
        return true;
    }

    @Override
    public List<Intersection> intersect(Line line, Terrain terrain) throws InterruptedException
    {
        return renderable.intersect(line, terrain);
    }

    @Override
    public String isExportFormatSupported(String mimeType)
    {
        return renderable.isExportFormatSupported(mimeType);
    }

    @Override
    public void export(String mimeType, Object output) throws IOException, UnsupportedOperationException
    {
        renderable.export(mimeType, output);
    }

    /**
     * Given an altitude mode as a string, convert it to the equivalent WW altitude mode constant.
     *
     * @param altitudeMode Altitude mode from a KML element
     * @param defaultAltitudeMode Altitude mode to return if the given string is an unknown value or is {@code null}/empty
     * @return One of {@link WorldWind#CLAMP_TO_GROUND}, {@link WorldWind#RELATIVE_TO_GROUND}, {@link WorldWind#ABSOLUTE},
     * or {@code defaultAltitudeMode} if the given string is {@code null} or empty.
     */
    protected static int getAltitudeModeFromKml(AltitudeMode altitudeMode, int defaultAltitudeMode)
    {
        if (altitudeMode == null) return defaultAltitudeMode;

        return switch (altitudeMode)
        {
            case RELATIVE_TO_GROUND -> WorldWind.RELATIVE_TO_GROUND;
            case ABSOLUTE -> WorldWind.ABSOLUTE;
            case CLAMP_TO_GROUND -> WorldWind.CLAMP_TO_GROUND;
            default -> defaultAltitudeMode;
        };
    }
}
