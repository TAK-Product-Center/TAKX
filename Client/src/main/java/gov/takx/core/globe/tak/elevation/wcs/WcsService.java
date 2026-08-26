package gov.takx.core.globe.tak.elevation.wcs;

import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.map.projection.EquirectangularMapProjection;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.ogc.gml.GMLRectifiedGrid;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capabilities;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capability;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100ContentMetadata;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100CoverageOffering;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100CoverageOfferingBrief;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100DescribeCoverage;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100LonLatEnvelope;
import gov.tak.api.engine.map.coords.GeoCalculations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.xml.stream.XMLStreamException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for connecting to and querying a WCS 1.0 elevation tile server.
 *
 * @since 5.3
 */
public class WcsService
{
    private static final int DEFAULT_NUM_LEVELS = 18;

    private final URI uri;
    // coverageDescriptions access must be synchronized
    private final Map<String, WCS100DescribeCoverage> coverageDescriptions = new HashMap<>();
    private WCS100Capabilities capabilities;
    private String urlTemplate;

    public WcsService(String uri)
    {
        this.uri = URI.create(uri);
    }

    /**
     * Sends a request to the WCS server for its {@link WCS100Capabilities capabilities} document. Once
     * retrieved, the  document can be retrieved from {@link #getCapabilities()}. This method must be
     * called before requesting further information from the WCS server.
     */
    public void retrieveCapabilities() throws Exception
    {
        if (capabilities == null)
        {
            capabilities = WCS100Capabilities.retrieve(uri);
            capabilities.parse();
        }
    }

    /**
     * @return the server's capabilities document
     * @throws IllegalStateException if the capabilities document has not been retrieved by calling {@link #retrieveCapabilities()}
     */
    @Nonnull
    public WCS100Capabilities getCapabilities()
    {
        if (capabilities == null) throw new IllegalStateException("retrieveCapabilities() not called");
        return capabilities;
    }

    /**
     * Returns a list of {@link WCS100CoverageOfferingBrief coverage offering brief} documents.
     * @return a list of coverage offering briefs
     * @throws IllegalStateException if the capabilities document has not been retrieved by calling {@link #retrieveCapabilities()}
     */
    @Nonnull
    public List<WCS100CoverageOfferingBrief> getCoverageOfferingBriefs()
    {
        WCS100ContentMetadata contentMetadata = getCapabilities().getContentMetadata();
        return contentMetadata != null ? contentMetadata.getCoverageOfferings() : Collections.emptyList();
    }

    /**
     * Sends a request to the WCS server for its {@link WCS100DescribeCoverage coverage description} document for
     * the provided {@code coverageName}. Once retrieved, the document can be retrieved from {@link #getCoverageDescription(String)}.
     * @param coverageName the coverage name
     * @throws IllegalStateException if the capabilities document has not been retrieved by calling {@link #retrieveCapabilities()}
     */
    public void retrieveCoverage(String coverageName) throws XMLStreamException, URISyntaxException
    {
        synchronized (coverageDescriptions)
        {
            WCS100DescribeCoverage coverageDescription = coverageDescriptions.get(coverageName);
            if (coverageDescription != null) return;

            String describeCoverageUrlString = getCapabilities().getCapability().getGetOperationAddress("DescribeCoverage");
            URI descCoverageUri = new URI(describeCoverageUrlString);
            coverageDescription = WCS100DescribeCoverage.retrieve(descCoverageUri, coverageName);
            coverageDescription.parse();
            coverageDescriptions.put(coverageName, coverageDescription);
        }
    }

    /**
     * Return a {@link WCS100DescribeCoverage coverage description} document for the provided {@code coverageName}.
     * @param coverageName the coverage name
     * @return the coverage description document
     * @throws IllegalStateException if the coverage description document has not been retrieved by calling {@link #retrieveCoverage(String)}
     */
    @Nonnull
    public WCS100DescribeCoverage getCoverageDescription(String coverageName)
    {
        synchronized (coverageDescriptions)
        {
            WCS100DescribeCoverage coverageDescription = coverageDescriptions.get(coverageName);
            if (coverageDescription == null) throw new IllegalStateException("retrieveCoverage() not called");
            return coverageDescription;
        }
    }

    /**
     * Return a {@link WCS100CoverageOffering coverage offering} document for the provided {@link WCS100DescribeCoverage coverage description} document and {@code coverageOfferingName}.
     * @param coverageOfferingName the coverage offering name
     * @return the coverage offering document
     * @throws IllegalStateException if the coverage offering name is not found in the coverage description document
     */
    @Nonnull
    public WCS100CoverageOffering getCoverageOffering(WCS100DescribeCoverage describeCoverage, @Nonnull String coverageOfferingName)
    {
        return describeCoverage.getCoverageOfferings().stream()
                .filter(o -> coverageOfferingName.equals(o.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("coverage offering not found"));
    }

    /**
     * @return the WCS server version string
     * @throws IllegalStateException if the capabilities document has not been retrieved by calling {@link #retrieveCapabilities()}
     */
    @Nonnull
    public String getVersion()
    {
        return getCapabilities().getVersion() != null ? getCapabilities().getVersion() : "1.0.0";
    }

    /**
     * The coverage area for the provided {@link WCS100CoverageOffering coverage offering} document, or {@code null} if there is no
     * coverage reported from the WCS server.
     * @param coverageOffering the coverage offering document
     * @return the covered area, or {@code null} if not available
     */
    @Nullable
    public Envelope getCoverageArea(@Nonnull WCS100CoverageOffering coverageOffering)
    {
        WCS100LonLatEnvelope envelope = coverageOffering.getLonLatEnvelope();
        if (envelope != null)
        {
            double[] sw = envelope.getPositions().get(0).getPos2();
            double[] ne = envelope.getPositions().get(1).getPos2();

            if (sw != null && ne != null)
            {
                return new Envelope(sw[0], sw[1], 0, ne[0], ne[1], 0);
            }
        }
        return null;
    }

    /**
     * @return the preferred image format for the provided {@link WCS100CoverageOffering coverage offering} document
     */
    @Nonnull
    private String getImageFormat(WCS100CoverageOffering offering)
    {
        List<String> imageFormats = offering.getSupportedFormats().getStrings();
        // Get the first "tiff" image format or the first or an empty string.
        return imageFormats.stream()
                .filter(i -> i.toLowerCase().contains("tiff"))
                .findFirst()
                .or(() -> imageFormats.stream().findFirst()).orElse("");
    }

    /**
     * Build an array of {@link TileMatrix.ZoomLevel} that is appropriate for adequately covering the provided {@link WCS100CoverageOffering coverage offering} document.
     * @param coverageOffering the coverage offering document
     * @param tileWidth the width of the tiles in pixels
     * @param tileHeight the height of the tiles in pixels
     * @param levelZeroLatDelta the distance between latitude posts for level zero in degrees
     * @param levelZeroLonDelta the distance between longitude posts for level zero in degrees
     * @return an array of {@link TileMatrix.ZoomLevel}
     */
    public TileMatrix.ZoomLevel[] buildZoomLevels(WCS100CoverageOffering coverageOffering, int tileWidth, int tileHeight, double levelZeroLatDelta, double levelZeroLonDelta)
    {
        TileMatrix.ZoomLevel zoomLevel0 = new TileMatrix.ZoomLevel();
        zoomLevel0.level = 0;
        zoomLevel0.tileWidth = tileWidth;
        zoomLevel0.tileHeight = tileHeight;
        zoomLevel0.pixelSizeX = levelZeroLonDelta / tileWidth;
        zoomLevel0.pixelSizeY = levelZeroLatDelta / tileHeight;
        zoomLevel0.resolution = (GeoCalculations.approximateMetersPerDegreeLongitude(0.0) * levelZeroLonDelta) / tileWidth;
        int numOfZoomLevels = getNumLevels(coverageOffering, zoomLevel0.tileWidth, levelZeroLatDelta);
        return TileMatrix.Util.createQuadtree(zoomLevel0, numOfZoomLevels);
    }

    /**
     * Calculate an appropriate number of levels to adequately cover the provided {@link WCS100CoverageOffering coverage offering} document.
     * @param coverageOffering the coverage offering document
     * @param tileWidth the width of the tiles in pixels
     * @param levelZeroLatDelta the distance between latitude posts for level zero in degrees
     * @return the appropriate number of levels to generate
     */
    private int getNumLevels(WCS100CoverageOffering coverageOffering, int tileWidth, double levelZeroLatDelta)
    {
        List<GMLRectifiedGrid> grids = coverageOffering.getDomainSet().getSpatialDomain().getRectifiedGrids();
        List<Vec4> offsetVectors = grids.get(0).getOffsetVectors();
        if (grids.isEmpty() || offsetVectors.size() < 2) return DEFAULT_NUM_LEVELS;

        double xRes = Math.abs(offsetVectors.get(0).x);
        double yRes = Math.abs(offsetVectors.get(1).y);
        double dataResolution = Math.min(xRes, yRes);

        double n = Math.log(levelZeroLatDelta / (dataResolution * tileWidth)) / Math.log(2);
        return (int) (Math.ceil(n) + 1);
    }

    /**
     * Build a URL to request coverage data from the WCS server for the provided {@link WCS100CoverageOffering coverage offering} document.
     * @param coverageOffering the coverage offering document
     * @param tileBounds the area of requested coverage
     * @param tileWidth the tile width of the returned image
     * @param tileHeight the tile height of the returned image
     * @return the url to request coverage
     * @throws IllegalStateException if the WCS server did not return sufficient information to build a URL
     */
    @Nonnull
    public String getGetCoverageUrl(WCS100CoverageOffering coverageOffering, Envelope tileBounds, int tileWidth, int tileHeight)
    {
        StringBuilder sb;
        if (this.urlTemplate == null)
        {
            WCS100Capability capability = getCapabilities().getCapability();
            if (capability == null) throw new IllegalStateException("capability document missing");
            String getCoverageUrl = capability.getGetOperationAddress("GetCoverage");
            if (getCoverageUrl == null) throw new IllegalStateException("getcoverage url missing");
            getCoverageUrl = fixGetUrl(getCoverageUrl);
            sb = new StringBuilder(getCoverageUrl);
            if (!sb.toString().toLowerCase().contains("service=wcs")) sb.append("service=WCS&");
            sb.append("request=GetCoverage");
            sb.append("&version=");
            sb.append(getVersion());
            sb.append("&crs=EPSG:");
            sb.append(EquirectangularMapProjection.DISPLAY_MODEL.srid);

            this.urlTemplate = sb.toString();
        } else
        {
            sb = new StringBuilder(this.urlTemplate);
        }

        sb.append("&coverage=");
        sb.append(coverageOffering.getName());
        sb.append("&format=");
        sb.append(getImageFormat(coverageOffering));

        sb.append("&width=");
        sb.append(tileWidth);
        sb.append("&height=");
        sb.append(tileHeight);

        sb.append("&bbox=");
        sb.append(tileBounds.minX);
        sb.append(",");
        sb.append(tileBounds.minY);
        sb.append(",");
        sb.append(tileBounds.maxX);
        sb.append(",");
        sb.append(tileBounds.maxY);

        sb.append("&"); // terminate the query string

        return sb.toString().replace(" ", "%20");
    }

    /**
     * Takes in a URL string and prepares it for appending new url params. The string will either end in a {@code ?} or a {@code @}.
     */
    @Nonnull
    private static String fixGetUrl(@Nonnull String url)
    {
        url = url.trim();
        int qMarkIndex = url.indexOf("?");
        if (qMarkIndex < 0)
        {
            url += "?";
        } else if (qMarkIndex != url.length() - 1 && url.lastIndexOf("&") != url.length() - 1)
        {
            url += "&";
        }
        return url;
    }
}
