package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.WWIcon;

/**
 * Manages WorldWind stackable icons.
 *
 * @since 2.26
 */
public class IconStackLayer extends IconLayer
{
    IconStackLayer()
    {
        setName("Icon Stacks");
        iconRenderer = new IconStackRenderer();
    }

    @Override
    public void removeIcon(WWIcon icon)
    {
        synchronized (icons)
        {
            super.removeIcon(icon);

            getIconRenderer().setIconPosition((IconStack) icon, null);
        }
    }

    @Override
    public void removeAllIcons()
    {
        synchronized (icons)
        {
            for (WWIcon icon : icons)
            {
                getIconRenderer().setIconPosition((IconStack) icon, null);
            }

            super.removeAllIcons();
        }
    }

    @Override
    protected Iterable<WWIcon> getActiveIcons(DrawContext dc)
    {
        return icons;
    }

    /**
     * Gets the icon stack renderer that enables visually displacing icons.
     *
     * @return The icon stack renderer that enables visually displacing icons.
     */
    IconStackRenderer getIconRenderer()
    {
        return (IconStackRenderer) iconRenderer;
    }
}
