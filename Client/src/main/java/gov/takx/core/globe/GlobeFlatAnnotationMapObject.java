package gov.takx.core.globe;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeFlatAnnotationMapObject;
import gov.takx.api.globe.IHighlightable;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Map Object that renders a multi-line text string on the surface of the globe. Rotation and text alignment is supported.
 *
 */
public class GlobeFlatAnnotationMapObject extends AGlobeMapObject implements IHighlightable, IGlobeFlatAnnotationMapObject
{
    /**
     * Constructor
     *
     * @param mapObject The {@link IFlatAnnotationMapObject} we are referencing.
     */
    public GlobeFlatAnnotationMapObject(IFlatAnnotationMapObject mapObject)
    {
        super(mapObject);
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

    @Override
    public IFlatAnnotationMapObject getMapObject()
    {
        return (IFlatAnnotationMapObject) super.getMapObject();
    }
}
