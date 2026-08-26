package gov.takx.core.view.gui.maps.terraindiscovery.controller.render;

import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.Renderable;

/**
 */
public interface ITerrainRenderable extends PreRenderable, Renderable
{
    boolean isVisible();
}
