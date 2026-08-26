package gov.takx.core.globe;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeAnnotationMapObject;
import gov.takx.api.globe.IHighlightable;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 */
public class GlobeAnnotationMapObject
        extends AGlobeMapObject
        implements IGlobeAnnotationMapObject, IHighlightable
{
    public GlobeAnnotationMapObject(IAnnotationMapObject mapObject)
    {
        super(mapObject);
        setMapObject(mapObject);
    }

    /**
     * @return Properly typed instance of our peer map object
     */
    @Override
    public IAnnotationMapObject getMapObject()
    {
        return (IAnnotationMapObject) super.getMapObject();
    }

    @Nonnull
    @Override
    public ILocation getLocation()
    {
        final IMapObject mapObject = getMapObject();
        return mapObject.getLocation();
    }

    @Override
    public void setLocation(ILocation location)
    {
        final IMapObject mapObject = getMapObject();

        if (!Objects.equals(location, mapObject.getLocation()))
        {
            mapObject.setLocation(location, true);
        }
    }
}
