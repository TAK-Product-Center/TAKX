package gov.takx.core.globe.tak.ww.layer;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Earth.MGRSGraticuleLayer;
import gov.nasa.worldwind.layers.GraticuleRenderingParams;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Path;

import java.awt.Font;

/**
 * Implementation of {@link MGRSGraticuleLayer} that reduces unnecessary elevation queries and improves performance.
 *
 * @since 5.2
 */
public class TakMGRSGraticuleLayer extends MGRSGraticuleLayer
{
    public TakMGRSGraticuleLayer()
    {
        graticuleSupport = new TakGraticuleSupport();
        initRenderingParams();
        // Bump font size down a bit
        for (String orderedType : getOrderedTypes())
        {
            GraticuleRenderingParams renderingParams = getRenderingParams(orderedType);
            Font labelFont = renderingParams.getLabelFont();
            renderingParams.setLabelFont(labelFont.deriveFont(labelFont.getSize2D() - 2));
        }
    }

    @Override
    protected Object createLineRenderable(Iterable<? extends Position> positions, String pathType)
    {
        Path path = new TakPath(positions);
        path.setPathType(pathType);
        path.setFollowTerrain(true);
        path.setTerrainConformance(1);
        path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        return path;
    }

    @Override
    protected Vec4 getSurfacePoint(DrawContext dc, Angle latitude, Angle longitude)
    {
        return dc.getGlobe().computePointFromPosition(new Position(latitude, longitude, 0));
    }
}
