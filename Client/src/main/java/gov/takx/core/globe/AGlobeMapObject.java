package gov.takx.core.globe;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.core.globe.render.GlobeMarkerAttributes;

/**
 */
@PseudoBean
public abstract class AGlobeMapObject implements IGlobeMapObject
{
    private IMapObject mapObject;
    private boolean selected = false;
    private boolean highlighted;

    protected AGlobeMapObject(IMapObject mapObject)
    {
        this.mapObject = mapObject;
    }

    @Override
    public void setMapObject(IMapObject mapObject)
    {
        this.mapObject = mapObject;
    }

    @Override
    public void setHighlighted(boolean highlighted)
    {
        this.highlighted = highlighted;

        GlobeMarkerAttributes.setSecondaryHighlight(highlighted ? mapObject.getId() : null);
    }

    @Override
    public boolean isHighlighted()
    {
        return highlighted;
    }

    @Override
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    @Override
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    @Override
    public boolean isSelected()
    {
        return selected;
    }

    @Override
    public boolean isViewable()
    {
        return mapObject != null && mapObject.isVisible();
    }

    @Override
    public abstract void setLocation(ILocation location);

    @Override
    public void peerRemoved()
    {
        // Empty implementation for base class
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "{" +
                "cmo=" + (mapObject != null ? mapObject.getKey() : "null") +
                ", selected=" + selected +
                ", highlighted=" + highlighted +
                '}';
    }
}
