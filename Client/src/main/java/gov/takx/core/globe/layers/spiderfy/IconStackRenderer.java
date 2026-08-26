package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.IconRenderer;
import gov.nasa.worldwind.render.WWIcon;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.takx.api.globe.spiderfy.IIconStack;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * IconRenderer processes collections of {@code IconStack} instances for picking and rendering.
 *
 * @since 2.26
 */
public class IconStackRenderer extends IconRenderer
{
    private final Map<IIconStack, Position> spiderfyPositions = new HashMap<>();

    @Override
    protected void drawMany(@Nonnull DrawContext drawContext, Iterable<? extends WWIcon> icons, Layer layer)
    {
        if (drawContext.getVisibleSector() != null)
        {
            SectorGeometryList geos = drawContext.getSurfaceGeometry();
            if (geos != null)
            {
                if (icons == null)
                {
                    throw new IllegalArgumentException("The icons being drawn must be non null");
                } else
                {
                    Iterator<? extends WWIcon> iterator = icons.iterator();
                    if (iterator.hasNext())
                    {
                        double horizon = drawContext.getView().getHorizonDistance();
                        Globe globe = drawContext.getGlobe();

                        while (iterator.hasNext())
                        {
                            WWIcon icon = iterator.next();

                            if (!isIconValid(icon, true))
                            {
                                if (icon != null)
                                {
                                    recordFeedback(drawContext, icon, null, null);
                                }
                            } else if (!icon.isVisible())
                            {
                                recordFeedback(drawContext, icon, null, null);
                            } else
                            {
                                Position pos = icon.getPosition();
                                Vec4 iconPoint = null;

                                // This overrides the default position to allow rendering the icons in a different
                                // location without altering their positions
                                pos = spiderfyPositions.getOrDefault(icon, pos);

                                if (drawContext.is2DGlobe())
                                {
                                    iconPoint = globe.computePointFromLocation(pos);
                                } else if (pos.getElevation() < globe.getMaxElevation() && !isAlwaysUseAbsoluteElevation())
                                {
                                    iconPoint = drawContext.getSurfaceGeometry().getSurfacePoint(pos);
                                }

                                if (iconPoint == null)
                                {
                                    Angle lat = pos.getLatitude();
                                    Angle lon = pos.getLongitude();
                                    double elevation = pos.getElevation();
                                    if (!isAlwaysUseAbsoluteElevation())
                                    {
                                        elevation += globe.getElevation(lat, lon);
                                    }

                                    iconPoint = globe.computePointFromPosition(lat, lon, elevation);
                                }

                                double eyeDistance = icon.isAlwaysOnTop() ? 0.0D : drawContext.getView().getEyePoint().distanceTo3(iconPoint);
                                if (isHorizonClippingEnabled() && !drawContext.is2DGlobe() && eyeDistance > horizon)
                                {
                                    recordFeedback(drawContext, icon, iconPoint, null);
                                } else if (isViewClippingEnabled() && !drawContext.getView().getFrustumInModelCoordinates().contains(iconPoint))
                                {
                                    recordFeedback(drawContext, icon, iconPoint, null);
                                } else
                                {
                                    drawContext.addOrderedRenderable(new IconRenderer.OrderedIcon(icon, iconPoint, layer, eyeDistance, horizon));
                                    if (icon.isShowToolTip())
                                    {
                                        addToolTip(drawContext, icon, iconPoint);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the location where an icon should be rendered which may be different than the icon's actual position.
     *
     * @param icon     The icon that will be rendered at a new location.
     * @param position The new location to render the icon. The icon's actual position is NOT altered by this.
     *                 If the parameter {@code position} is {@code null} then the icons default position will be used.
     */
    void setIconPosition(IIconStack icon, Position position)
    {
        if (position == null)
        {
            spiderfyPositions.remove(icon);
        } else
        {
            spiderfyPositions.put(icon, position);
        }
    }

    /**
     * Resets the render locations of all icons. After invoking this icons will be drawn at their internal position.
     */
    void resetIconPositions()
    {
        spiderfyPositions.clear();
    }
}
