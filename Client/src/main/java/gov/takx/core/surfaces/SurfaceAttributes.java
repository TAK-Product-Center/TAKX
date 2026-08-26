package gov.takx.core.surfaces;

import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.takx.api.surfaces.ISurfaceAttributes;

/**
 * Wrapper around the WorldWind AnalyticSurfaceAttributes class exposed through the ISurfaceAttributes interface.
 * This design protects the plugin developer from having to be exposed to any WorldWind classes.
 *
 * @since 1.4
 */
public class SurfaceAttributes implements ISurfaceAttributes
{
    protected AnalyticSurfaceAttributes attributes;

    public SurfaceAttributes(AnalyticSurfaceAttributes attributes)
    {
        this.attributes = attributes;
    }

    @Override
    public boolean isDrawInterior()
    {
        return attributes.isDrawInterior();
    }

    @Override
    public void setDrawInterior(boolean b)
    {
        attributes.setDrawInterior(b);
    }

    @Override
    public boolean isDrawOutline()
    {
        return attributes.isDrawOutline();
    }

    @Override
    public void setDrawOutline(boolean b)
    {
        attributes.setDrawOutline(b);
    }

    @Override
    public boolean isDrawShadow()
    {
        return attributes.isDrawShadow();
    }

    @Override
    public void setDrawShadow(boolean b)
    {
        attributes.setDrawShadow(b);
    }

    @Override
    public double getInteriorOpacity()
    {
        return attributes.getInteriorOpacity();
    }

    @Override
    public void setInteriorOpacity(double v)
    {
        attributes.setInteriorOpacity(v);
    }

    @Override
    public double getOutlineOpacity()
    {
        return attributes.getOutlineOpacity();
    }

    @Override
    public void setOutlineOpacity(double v)
    {
        attributes.setOutlineOpacity(v);
    }

    @Override
    public double getShadowOpacity()
    {
        return attributes.getShadowOpacity();
    }

    @Override
    public void setShadowOpacity(double v)
    {
        attributes.setShadowOpacity(v);
    }

    @Override
    public double getOutlineWidth()
    {
        return attributes.getOutlineWidth();
    }

    @Override
    public void setOutlineWidth(double v)
    {
        attributes.setOutlineWidth(v);
    }
}
