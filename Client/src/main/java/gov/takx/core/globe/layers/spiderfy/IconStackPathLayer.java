package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.ShapeAttributes;

/**
 * Manages ground paths between the multiple positions associated with stackable icons.
 *
 * @since 2.26
 */
class IconStackPathLayer extends RenderableLayer
{
    private Material groundTraceLineMaterial = Material.WHITE;

    IconStackPathLayer()
    {
        setName("Icon Stack Ground Lines");
    }

    /**
     * Draws a line along the globe's surface connecting two positions.
     *
     * @param startPosition The starting location of the line.
     * @param endPosition   The ending location of the line.
     * @return The renderable that was added to the layer.
     */
    Path addPath(Position startPosition, Position endPosition)
    {
        Path path = new Path(startPosition, endPosition);
        ShapeAttributes attributes = new BasicShapeAttributes();

        attributes.setOutlineMaterial(groundTraceLineMaterial);
        attributes.setOutlineWidth(2);

        path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        path.setAttributes(attributes);
        path.setFollowTerrain(true);

        addRenderable(path);

        return path;
    }

    /**
     * Removes all lines.
     */
    void clearPolylines()
    {
        removeAllRenderables();
    }
}
