package gov.takx.core.globe.tak.ww;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.terrain.Tessellator;

import java.beans.PropertyChangeEvent;

/**
 * A no-op implementation of {@link Tessellator}.
 * @since 5.2
 */
public class NoOpTessellator extends AVListImpl implements Tessellator
{
    @Override
    public SectorGeometryList tessellate(DrawContext dc)
    {
        SectorGeometryList sectorGeometries = new SectorGeometryList();
        sectorGeometries.add(new NoOpSectorGeometry(dc.getGlobe(), dc.getVisibleSector()));
        sectorGeometries.setSector(dc.getVisibleSector());
        return sectorGeometries;
    }

    @Override
    public boolean isMakeTileSkirts()
    {
        return false;
    }

    @Override
    public void setMakeTileSkirts(boolean makeTileSkirts)
    {
    }

    @Override
    public long getUpdateFrequency()
    {
        return 0;
    }

    @Override
    public void setUpdateFrequency(long updateFrequency)
    {
    }

    @Override
    public void onMessage(Message msg)
    {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
    }
}
