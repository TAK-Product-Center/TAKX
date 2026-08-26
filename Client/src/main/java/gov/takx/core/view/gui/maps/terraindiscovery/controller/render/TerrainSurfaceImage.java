package gov.takx.core.view.gui.maps.terraindiscovery.controller.render;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.takx.api.terrain.experimental.proposed.TerrainDataUtils;
import gov.takx.api.terrain.experimental.proposed.tile.IRaster;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 */
public class TerrainSurfaceImage implements ITerrainRenderable
{
    public static final int SIZE = 1024;
    private IRaster raster;
    private Sector sector;
    private boolean visible;
    private Color lowColor;
    private Color highColor;
    private SurfaceImage surfaceImage;
    private double opacity;
    private boolean topographic;
    private boolean hillshade;

    public TerrainSurfaceImage()
    {
        sector = null;
        raster = null;
        visible = true;
        lowColor = Color.BLUE;
        highColor = Color.RED;
        surfaceImage = null;
        opacity = 1;
        topographic = false;
        hillshade = true;
    }

    /**
     * @return {@code true} if the raster is fully loaded, or if a worker is currently loading it.  Otherwise
     * {@code false} is returned.
     */
    public synchronized boolean isRasterLoaded()
    {
        return raster != null;
    }

    public void setOpacity(double opacity)
    {
        this.opacity = opacity;
        if (surfaceImage != null)
        {
            surfaceImage.setOpacity(opacity);
        }
    }

    public void setSector(Sector sector)
    {
        this.sector = sector;
        updateImage();
    }

    public void setRaster(IRaster raster)
    {
        synchronized (this)
        {
            this.raster = raster;
        }
        updateImage();
    }

    public boolean isHillshadeEnabled()
    {
        return hillshade;
    }

    public void setHillshadeEnabled(boolean enabled)
    {
        this.hillshade = enabled;
    }

    public boolean isTopographicEnabled()
    {
        return topographic;
    }

    public void setTopographicEnabled(boolean enabled)
    {
        this.topographic = enabled;
    }

    public void setColor(Color color)
    {
        this.lowColor = color;

        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float hue = hsb[0];
        hue += 0.25f;
        if (hue > 1)
        {
            hue -= 1;
        }
        this.highColor = new Color(Color.HSBtoRGB(hue, hsb[1], hsb[2]));
        updateImage();
    }

    public void setColors(Color lowColor, Color highColor)
    {
        this.lowColor = lowColor;
        this.highColor = highColor;
        updateImage();
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public void preRender(DrawContext dc)
    {
        if (visible && surfaceImage != null)
        {
            surfaceImage.preRender(dc);
        }
    }

    @Override
    public void render(DrawContext dc)
    {
        if (visible && surfaceImage != null)
        {
            dc.addOrderedSurfaceRenderable(surfaceImage);
        }
    }

    public void clear()
    {
        surfaceImage = null;
    }

    private void updateImage()
    {
        synchronized (this)
        {
            if (raster == null || sector == null)
            {
                return;
            }

            if (surfaceImage == null)
            {
                surfaceImage = new SurfaceImage();
            }
        }

        BufferedImage image = TerrainDataUtils.createImage(
                raster.getValues(),
                raster.getWidth(),
                raster.getHeight(),
                topographic,
                hillshade,
                lowColor,
                highColor,
                5,
                null);

        synchronized (this)
        {
            surfaceImage.setImageSource(image, sector);
            surfaceImage.setOpacity(opacity);
        }
    }
}
