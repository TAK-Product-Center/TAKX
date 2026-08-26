package gov.takx.core.globe;

import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IShapeBullseyeMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.shapes.IBullseye;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Map object representing a bullseye, a conglomeration of compass rose spokes and range rings at equidistant intervals.
 *
 * @since 5.5
 */
public class GlobeBullseyeMapObject extends GlobeShapeMapObject implements IGlobeShapeMapObject, PropertyChangeListener
{
    public GlobeBullseyeMapObject()
    {
        super(MapToolType.DRAWING_BULLSEYE);
    }

    public GlobeBullseyeMapObject(@Nonnull IShapeBullseyeMapObject mapObject)
    {
        super(mapObject);
        setShapeModel(mapObject.getAssociatedShape());
    }

    @Override
    protected List<String> getShapePropertiesToWatch()
    {
        return IBullseye.PROPERTIES_TO_WATCH;
    }
}
