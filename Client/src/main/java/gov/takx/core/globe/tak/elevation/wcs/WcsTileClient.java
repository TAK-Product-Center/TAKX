package gov.takx.core.globe.tak.elevation.wcs;

import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.tilematrix.AbstractURLTileClient;
import com.atakmap.map.projection.EquirectangularMapProjection;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100CoverageOffering;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100DescribeCoverage;
import jakarta.annotation.Nonnull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

/**
 * A {@link AbstractURLTileClient TileClient} implementation for describing a WCS elevation tile server.
 * 
 * @since 5.3
 */
class WcsTileClient extends AbstractURLTileClient
{
    private static final int TILE_WIDTH = 150;
    private static final int TILE_HEIGHT = 150;
    private static final double TILE_ZERO_DELTA_LAT = 20.0;
    private static final double TILE_ZERO_DELTA_LON = 20.0;

    private final WcsService wcsService;
    private final ZoomLevel[] zoomLevels;
    private final Envelope coverageArea;
    private final WCS100CoverageOffering coverageOffering;

    /**
     * Construct a tile client. The {@code coverageName} must be retrieved via {@link WcsService#retrieveCoverage(String)} and the {@code coverageOfferingName} must exist in the coverage description.
     * @param wcsService a WCS service
     * @param coverageName the name of the coverage
     * @param coverageOfferingName the name of the coverage offering
     * @throws IllegalStateException if the coverage description is not retrieved or if the coverage offering name does not exist
     */
    public WcsTileClient(@Nonnull WcsService wcsService, String coverageName, String coverageOfferingName)
    {
        super(coverageOfferingName, ONE_WEEK_MILLIS, null);
        this.wcsService = wcsService;

        WCS100DescribeCoverage coverageDescription = wcsService.getCoverageDescription(coverageName);
        coverageOffering = wcsService.getCoverageOffering(coverageDescription, coverageOfferingName);
        zoomLevels = wcsService.buildZoomLevels(coverageOffering, TILE_WIDTH, TILE_HEIGHT, TILE_ZERO_DELTA_LAT, TILE_ZERO_DELTA_LON);
        coverageArea = wcsService.getCoverageArea(coverageOffering);
    }

    @Override
    protected URL getTileURL(int zoom, int x, int y)
    {
        Envelope tileBounds = Util.getTileBounds(this, zoom, x, y);
        // Expand requested area by 1 delta in each direction to provide additional data for interpolation along tile edges
        double deltaLat = tileBounds.maxY - tileBounds.minY;
        double deltaLon = tileBounds.maxX - tileBounds.minX;
        tileBounds.minX -= deltaLon;
        tileBounds.maxX += deltaLon;
        tileBounds.minY -= deltaLat;
        tileBounds.maxY += deltaLat;
        String retrieveCoverageUrl = wcsService.getGetCoverageUrl(coverageOffering, tileBounds, TILE_WIDTH, TILE_HEIGHT);
        try
        {
            return new URL(retrieveCoverageUrl);
        } catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T getControl(Class<T> controlClazz)
    {
        // No controls implemented for this tile client
        return null;
    }

    @Override
    public void getControls(Collection<Object> controls)
    {
        // No controls implemented for this tile client
    }

    @Override
    public int getSRID()
    {
        return EquirectangularMapProjection.DISPLAY_MODEL.srid;
    }

    @Override
    public ZoomLevel[] getZoomLevel()
    {
        return zoomLevels;
    }

    @Override
    public double getOriginX()
    {
        return coverageArea.minX;
    }

    @Override
    public double getOriginY()
    {
        return coverageArea.maxY;
    }

    @Override
    public Envelope getBounds()
    {
        return coverageArea;
    }
}
