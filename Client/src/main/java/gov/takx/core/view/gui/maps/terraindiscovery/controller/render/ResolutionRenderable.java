package gov.takx.core.view.gui.maps.terraindiscovery.controller.render;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.ScreenImage;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.proposed.tile.ElevationTile;
import gov.takx.api.terrain.experimental.proposed.tile.TileCollection;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.TerrainDiscoveryUtils;
import gov.takx.core.view.gui.maps.terraindiscovery.model.ITerrainDiscoveryModel;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The purpose of this class is to visualize the highest-resolution terrain data available on disk, in a given region.
 * In this context, highest-resolution terrain data refers to the best-overall resolution, amongst all selected
 * elevation layers.  Resolution is visualized as coverage shapes.  Each shape is given a color to represent the
 * resolution at that particular region.  These shapes may by arbitrary polygons, with the possibility of holes.  Since
 * the resolution coverage is based upon elevation tiles on disk, these shapes will typically have a 'blocky'
 * appearance.
 * <p/>
 * This class basically acts as a stack of shapes representing resolution coverage, sorted in order of resolution,
 * from worst-to-best.  The worst resolution shapes are rendered first, the best are rendered last.
 * <p/>
 * The trick to this class is how it handles opacity.  If each resolution shape is an arbitrary polygon, there is the
 * possibility for shapes to overlap.  If the overlaps are not managed, they will be visible when opacity is not 100%.
 * The rule is that when two or more shapes overlap, the shape representing the highest resolution should be the only
 * one visible at the intersection.  This means we need the ability to remove parts of shapes (e.g. to 'punch holes' in
 * shapes).  AreaSurfaceShape is backed by a java.awt.geom.Area, which provides that capability.  Overlaps are handled
 * by iterating through the shapes from highest-to-lowest resolution, appending last seen shapes to a mask, and
 * removing the mask shape from the current shape.
 *
 */
public class ResolutionRenderable implements ITerrainRenderable
{
    private static final String RESOLUTION_KEY = "resolutionKey";
    private final Map<Double, Set<ElevationTile>> resolutionMap;
    private final Sector sector;
    private final List<AreaSurfaceShape> shapes;
    private final List<Double> resolutions;
    private final Map<Double, Color> shapeColorMap;
    private final AtomicReference<ScreenImage> screenImage;
    private final double earthRadius;
    private boolean visible;
    private double opacity;

    public ResolutionRenderable(Sector sector, double earthRadius)
    {
        this.sector = Sector.boundingSector(sector);
        this.earthRadius = earthRadius;
        resolutionMap = new ConcurrentHashMap<>();
        shapes = new CopyOnWriteArrayList<>();
        resolutions = new CopyOnWriteArrayList<>();
        shapeColorMap = new ConcurrentHashMap<>();
        visible = true;
        opacity = 1d;
        screenImage = new AtomicReference<>(null);
    }

    public void update(ITerrainDiscoveryController controller)
    {
        updateResolution(controller);
        createSurfaceShapes();
    }

    public double getResolution(AreaSurfaceShape shape)
    {
        double resolution = Double.MAX_VALUE;
        if (shape.hasKey(RESOLUTION_KEY))
        {
            resolution = (Double) shape.getValue(RESOLUTION_KEY);
        }
        return resolution;
    }

    public ElevationTile getBestResolutionTile(double latitude, double longitude)
    {
        if (!sector.containsDegrees(latitude, longitude))
        {
            return null;
        }

        List<ElevationTile> bestTiles = new ArrayList<>();
        for (int i = resolutions.size() - 1; i >= 0 && bestTiles.isEmpty(); i--)
        {
            Set<ElevationTile> tiles = resolutionMap.get(resolutions.get(i));
            if (tiles != null)
            {
                for (ElevationTile tile : tiles)
                {
                    if (tile.contains(latitude, longitude))
                    {
                        bestTiles.add(tile);
                    }
                }
            }
        }

        if (bestTiles.size() > 0)
        {
            sortBestTiles(bestTiles);
            return bestTiles.get(bestTiles.size() - 1);
        } else
        {
            return null;
        }
    }

    public void clear()
    {
        resolutionMap.clear();
        resolutions.clear();
        shapes.clear();
        shapeColorMap.clear();
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public void setOpacity(double opacity)
    {
        this.opacity = opacity;
        updateOpacity();
    }

    @Override
    public void preRender(DrawContext dc)
    {
        if (!isVisible())
        {
            return;
        }

        for (PreRenderable shape : shapes)
        {
            shape.preRender(dc);
        }
    }

    @Override
    public void render(DrawContext dc)
    {
        if (!isVisible())
        {
            return;
        }

        ScreenImage si = screenImage.get();
        if (si != null)
        {
            updateScreenLegendLocation(si, dc);
            // TODO: TAKX-2404, implement as widget so render can be removed
            si.render(dc);
        }

        for (AreaSurfaceShape shape : shapes)
        {
            dc.addOrderedRenderable(shape);
        }
    }

    private void updateOpacity()
    {
        for (AreaSurfaceShape shape : shapes)
        {
            shape.setOpacity(opacity);
        }
    }

    private void updateResolution(ITerrainDiscoveryController controller)
    {
        resolutionMap.clear();
        ITerrainDiscoveryModel model = controller.getModel();
        for (IRaptorLayer elevationLayer : model.getElevationLayers())
        {
            TileCollection tileCollection = model.getTileCollection(elevationLayer);
            if (tileCollection != null && tileCollection.isEnabled())
            {
                Sector intersectingSector = tileCollection.intersection(sector);
                if (intersectingSector != null)
                {
                    for (ElevationTile tile : tileCollection.getBestAvailableTiles(intersectingSector))
                    {
                        addTile(tile);
                    }
                }
            }
        }
    }

    private void createSurfaceShapes()
    {
        shapes.clear();
        getSortedResolutions();
        if (resolutions.isEmpty())
        {
            return;
        }

        for (Double resolution : resolutions)
        {
            AreaSurfaceShape shape = new AreaSurfaceShape(sector);

            // Store the resolution in the shape (it is an AVList).
            // This value will later be used to generate a color for the shape.
            shape.setValue(RESOLUTION_KEY, resolution);

            for (ElevationTile tile : resolutionMap.get(resolution))
            {
                shape.add(tile.getSector());
            }
            shapes.add(shape);
        }

        updateShapeColors();
        updateOpacity();
        removeOverlappingAreas();
        updateScreenLegend();
    }

    private List<Double> getSortedResolutions()
    {
        resolutions.clear();
        if (!resolutionMap.isEmpty())
        {
            List<Double> keys = new ArrayList<>(resolutionMap.keySet());

            // Sort the resolutions to ascending order.
            Collections.sort(keys);

            // Reverse the resolutions because resolution is really texel size, meaning the smaller the resolution,
            // the better the quality.  We want lowest quality resolutions to be first on the list, so reverse the
            // list to convert it to a descending sorted list.
            Collections.reverse(keys);

            resolutions.addAll(keys);
        }
        return resolutions;
    }

    private void updateShapeColors()
    {
        shapeColorMap.clear();
        if (resolutions.isEmpty())
        {
            getSortedResolutions();
        }
        int numResolutions = resolutions.size();

        // Create the color pallette for resolutions
        int count = 0;
        for (int i = resolutions.size() - 1; i >= 0; i--)
        {
            Color shapeColor = TerrainDiscoveryUtils.hueColorFromRange(
                    i,
                    0,
                    numResolutions - 1,
                    TerrainDiscoveryUtils.HUE_BLUE,
                    TerrainDiscoveryUtils.HUE_RED);

            // Make odd-count colors slightly less saturated to improve contrast between resolution colors.
            if (count % 2 == 1)
            {
                shapeColor = scaleColorSaturation(shapeColor, 0.85f);
            }
            shapeColorMap.put(resolutions.get(i) * earthRadius, shapeColor);
            count++;
        }

        for (AreaSurfaceShape shape : shapes)
        {
            Color shapeColor = shapeColorMap.get(getResolution(shape) * earthRadius);
            if (shapeColor != null)
            {
                shape.setColor(shapeColor);
            }
        }
    }

    private Color scaleColorSaturation(Color c, float factor)
    {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1] * factor, hsb[2]));
    }

    private void addTile(ElevationTile tile)
    {
        if (tile == null)
        {
            return;
        }

        double resolution = tile.getHighestResolution();
        synchronized (this)
        {
            if (!resolutionMap.containsKey(resolution))
            {
                resolutionMap.put(
                        resolution,
                        Collections.newSetFromMap(new ConcurrentHashMap<ElevationTile, Boolean>()));
            }
            resolutionMap.get(resolution).add(tile);
        }
    }

    private void removeOverlappingAreas()
    {
        int size = shapes.size();
        if (size < 2)
        {
            return;
        }

        // Mask is a combined area of all removed area.
        // At each iteration of the following loop, the mask will grow with the last-seen shape.
        Area mask = new Area();

        // Start at the highest resolution shape and descend.
        AreaSurfaceShape last = shapes.get(size - 1);
        for (int i = size - 2; i >= 0; i--)
        {
            // Add the area of the last seen shape to the mask.
            mask.add(last.getArea());

            // One step lower in resolution...
            // Remove the combined mask from this lower resolution shape
            // and prepare for the next iteration.
            AreaSurfaceShape current = shapes.get(i);
            current.remove(mask);
            last = current;
        }
    }

    private void updateScreenLegend()
    {
        Map<Double, Color> colorMap = new HashMap<>(shapeColorMap);
        if (colorMap.isEmpty())
        {
            return;
        }

        BufferedImage legendImage = TerrainDiscoveryUtils.createResolutionColorLegend("Terrain Resolution", colorMap);
        if (legendImage == null)
        {
            return;
        }

        ScreenImage renderableScreenImage = new ScreenImage();
        renderableScreenImage.setImageSource(legendImage);

        renderableScreenImage.setScreenLocation(new Point(240, 320));
        screenImage.set(renderableScreenImage);
    }

    private void updateScreenLegendLocation(ScreenImage si, DrawContext dc)
    {
        Object o = si.getImageSource();
        if (o instanceof BufferedImage)
        {
            BufferedImage img = (BufferedImage) o;
            int yOffset = img.getHeight() / 2;
            int y = Math.max(yOffset, dc.getDrawableHeight() - yOffset - 1);
            si.setScreenLocation(new Point(240, y));
        }
    }

    /**
     * Mostly copied from TerrainUtils.sortModelsByResolution().
     */
    private void sortBestTiles(List<ElevationTile> tiles)
    {
        Collections.sort(tiles, new Comparator<ElevationTile>()
        {
            @Override
            public int compare(ElevationTile o1, ElevationTile o2)
            {
                ITakElevationModel em1 = o1.getAttributes().getElevationModel();
                ITakElevationModel em2 = o2.getAttributes().getElevationModel();

                // next, prioritize by network connectivity (local caches vs. networked layers)
                boolean em1Networked = em1.isNetworkRetrievalEnabled();
                boolean em2Networked = em2.isNetworkRetrievalEnabled();
                if (em1Networked != em2Networked)
                {
                    if (!em2Networked)
                    {
                        return -1;
                    } else
                    {
                        return 1;
                    }
                }
                return em2.getName().compareTo(em1.getName());
            }
        });
    }
}
