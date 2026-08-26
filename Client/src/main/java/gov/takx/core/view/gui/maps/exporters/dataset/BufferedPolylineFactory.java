package gov.takx.core.view.gui.maps.exporters.dataset;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;

import java.util.ArrayList;
import java.util.List;

public class BufferedPolylineFactory
{

    public static List<ILocation> createPolygonFromBufferedPolyline(List<ILocation> vertices, double bufferDistanceDegrees)
    {
        Coordinate[] coordinates = new Coordinate[vertices.size()];
        for (int i = 0; i < vertices.size(); i++)
        {
            ILocation loc = vertices.get(i);
            coordinates[i] = new Coordinate(loc.getLatitude(), loc.getLongitude());
        }
        Geometry polyline = new GeometryFactory().createLineString(coordinates);
        Geometry polygon = polyline.buffer(bufferDistanceDegrees);
        BufferOp bufOp = new BufferOp(polygon);
        bufOp.setEndCapStyle(BufferOp.CAP_SQUARE);
        Coordinate[] polyCoordinates = polygon.getCoordinates();
        List<ILocation> polyVertices = new ArrayList();
        for (Coordinate coordinate : polyCoordinates)
        {
            polyVertices.add(new RaptorLocation(coordinate.x, coordinate.y));
        }
        return polyVertices;
    }

    public List<ILocation> test()
    {
        List<ILocation> verts = new ArrayList();
        verts.add(new RaptorLocation(0, 0));
        verts.add(new RaptorLocation(0, 1));
        verts.add(new RaptorLocation(0, 2));
        List<ILocation> poly = createPolygonFromBufferedPolyline(verts, 10);
        return poly;
    }

    public static void main(String args[])
    {
        BufferedPolylineFactory f = new BufferedPolylineFactory();
        f.test();
    }
}
