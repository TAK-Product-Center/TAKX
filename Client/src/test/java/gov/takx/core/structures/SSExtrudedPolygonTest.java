package gov.takx.core.structures;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.VecBuffer;
import gov.nasa.worldwind.util.VecBufferSequence;
import gov.takx.api.persist.IPersistable;
import gov.takx.dataservices.persist.PersistableExtrudedPolygon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class SSExtrudedPolygonTest
{
    private static final String DOC_FACTORY_PROPERTY = "javax.xml.parsers.DocumentBuilderFactory";
    private static final String REPLACEMENT_DOC_FACTORY =
            "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";

    private static final List<Position> POSITIONS = List.of(Position.fromDegrees(1, 1, 1),
            Position.fromDegrees(1, 2, 1),
            Position.fromDegrees(2, 2, 1),
            Position.fromDegrees(2, 1, 1));
    private static final List<Coordinate> COORDINATES = List.of(new Coordinate(1, 1, 1),
            new Coordinate(1, 2, 1),
            new Coordinate(2, 2, 1),
            new Coordinate(2, 1, 1));

    private String originalDocumentBuilderFactory;

    @BeforeEach
    public void beforeTest()
    {
        // Necessary to setup a valid document builder factory implementation - otherwise WW's ExtrudedPolygon
        // constructor will throw exceptions.
        originalDocumentBuilderFactory = System.getProperty(DOC_FACTORY_PROPERTY);
        System.setProperty(DOC_FACTORY_PROPERTY, REPLACEMENT_DOC_FACTORY);
    }

    @Test
    public void ssExtrudedPolygonCreationTest()
    {
        testSSExtrudedPolygonBasicProperties(createSSExtrudedPolygonFromVecBuffer());
        testSSExtrudedPolygonBasicProperties(createSSExtrudedPolygonFromKML());
        testSSExtrudedPolygonBasicProperties(createSSExtrudedPolygonFromPersistable());
    }

    @AfterEach
    public void afterTest()
    {
        // Attempt to replace the original document builder factory in System properties.  It was likely null though,
        // And setting properties to null is not permitted, so do a null check first.
        if (originalDocumentBuilderFactory != null)
        {
            System.setProperty(DOC_FACTORY_PROPERTY, originalDocumentBuilderFactory);
        }
    }

    private void testSSExtrudedPolygonBasicProperties(SSExtrudedPolygon ssep)
    {
        IPersistable persistable = ssep.getPersistable();

        assertThat(persistable).withFailMessage("Persistable Type").isInstanceOf(PersistableExtrudedPolygon.class);

        final ExtrudedPolygon renderable = (ExtrudedPolygon) ssep.getRenderable();
        PersistableExtrudedPolygon pep = (PersistableExtrudedPolygon) persistable;

        assertThat(pep.getAltitudeMode()).withFailMessage("Altitude Mode").isEqualTo(renderable.getAltitudeMode());
        assertThat(renderable.isEnableSides()).withFailMessage("Enable Sides").isTrue();
        assertThat(renderable.getGeometryRegenerationInterval()).withFailMessage("Geometry Regeneration Interval").isEqualTo(SSExtrudedPolygon.GEOMETRY_REGEN_INTERVAL);
        assertThat(pep.isExtrude()).withFailMessage("Persistable is Extrude").isTrue();

        assertThat(renderable.getBaseDepth()).withFailMessage("Base Depth").isEqualTo(SSExtrudedPolygon.DEFAULT_BASE_DEPTH);
    }

    private SSExtrudedPolygon createSSExtrudedPolygonFromVecBuffer()
    {
        DoubleBuffer doubleBuffer = DoubleBuffer.allocate(4 * 2); // 4 points, each point is lon and lat
        doubleBuffer.put(1).put(1);
        doubleBuffer.put(1).put(2);
        doubleBuffer.put(2).put(2);
        doubleBuffer.put(2).put(1);
        VecBuffer vecBuffer = new VecBuffer(2, new BufferWrapper.DoubleBufferWrapper(doubleBuffer));
        VecBufferSequence vecBufferSequence = new VecBufferSequence(vecBuffer, 1);
        return new SSExtrudedPolygon(Sector.FULL_SPHERE, vecBufferSequence, 10d);
    }

    private SSExtrudedPolygon createSSExtrudedPolygonFromPersistable()
    {
        List<List<Position>> positionBlocks = new ArrayList<>();
        positionBlocks.add(POSITIONS);

        PersistableExtrudedPolygon persistable = new PersistableExtrudedPolygon(Sector.FULL_SPHERE, positionBlocks, null);
        persistable.setIsExtrude(true);
        return new SSExtrudedPolygon(persistable);
    }

    private SSExtrudedPolygon createSSExtrudedPolygonFromKML()
    {
        final Polygon polygon = new Polygon();
        polygon.setExtrude(true);

        LinearRing linearRing = new LinearRing();
        linearRing.setCoordinates(COORDINATES);

        Boundary boundary = new Boundary();
        boundary.setLinearRing(linearRing);

        polygon.setOuterBoundaryIs(boundary);
        polygon.addToInnerBoundaryIs(boundary);

        return SSExtrudedPolygon.fromKml(polygon);
    }
}
