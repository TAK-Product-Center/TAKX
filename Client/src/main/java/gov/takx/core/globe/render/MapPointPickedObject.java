package gov.takx.core.globe.render;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.pick.PickedObject;
import gov.takx.api.globe.render.IRendererSearchResult;
import gov.takx.api.globe.render.ISearchablePickedObject;
import gov.takx.core.mapdata.MapPoint;

/**
 * A {@link PickedObject} representing a {@link MapPoint}.
 *
 * @since 2.29
 */
public class MapPointPickedObject extends PickedObject implements ISearchablePickedObject
{
    private final GlobeMapPointRenderer renderer;
    private final Globe globe;

    public MapPointPickedObject(GlobeMapPointRenderer renderer, Globe globe, int colorCode, MapPoint mapPoint, Position position)
    {
        super(colorCode, mapPoint, position, false);

        this.renderer = renderer;
        this.globe = globe;
    }

    @Override
    public Sector getSearchSector()
    {
        return ((MapPoint) getObject()).getSearchSector(globe);
    }

    @Override
    public IRendererSearchResult searchForRdms()
    {
        return renderer.searchForRdms(getSearchSector());
    }

    @Override
    public int getMapObjectId()
    {
        return ((MapPoint) getObject()).getMoId();
    }
}
