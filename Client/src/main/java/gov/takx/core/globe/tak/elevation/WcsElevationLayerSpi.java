package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.TileMatrixElevationSource;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100CoverageOffering;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100CoverageOfferingBrief;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.tak.OfflineCacheRegistry;
import gov.takx.core.globe.tak.elevation.ElevationLayerFactory.ElevationLayerPair;
import gov.takx.core.globe.tak.elevation.wcs.WcsElevationSource;
import gov.takx.core.globe.tak.elevation.wcs.WcsService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * An {@link ElevationLayerFactorySpi} implementation for WCS terrain server data. This implementation handles {@link IRaptorLayer layers} with
 * a {@link IRaptorLayer#getRestorableState() restorable state} that contains an XML configuration that describes a WCS connection.
 * <pre>
 *     {@code
 *     <?xml version="1.0" encoding="UTF-8"?>
 *     <wcsSource>
 *         <uri>https://server/path</uri>
 * 	       <coverageName>US East</coverageName> <!-- Optional -->
 * 	       <coverageOfferingName>US East 50m</coverageOfferingName> <!-- Optional -->
 *     </wcsSource>
 *     }
 * </pre>
 *
 * @since 5.3
 */
public class WcsElevationLayerSpi
{
    public static final String CONFIGURATION_ELEMENT = "wcsSource";

    static final ElevationLayerFactorySpi SPI = new ElevationLayerFactorySpi();

    @PseudoBean
    static final class ElevationLayerFactorySpi extends AElevationLayerFactorySpi
    {
        private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        @Inject
        private OfflineCacheRegistry offlineCacheRegistry;

        // Suppress JSON parsing error logging
        private static final DefaultHandler noopErrorHandler = new DefaultHandler();

        @Override
        public ElevationLayerPair create(IRaptorLayer raptorLayer)
        {
            if (raptorLayer.getRestorableState() == null) return null;

            // Parse configuration from restorable state
            String uri;
            String coverageNameXml;
            String coverageOfferingNameXml;
            try
            {
                DocumentBuilder builder = SafeXmlDocumentUtils.getDocumentBuilder();
                builder.setErrorHandler(noopErrorHandler);
                Document document = builder.parse(new InputSource(new StringReader(raptorLayer.getRestorableState())));
                final XPath xpath = XPathFactory.newInstance().newXPath();
                String rootElement = "/" + CONFIGURATION_ELEMENT + "/";
                uri = (String) xpath.evaluate(rootElement + "uri", document, XPathConstants.STRING);
                coverageNameXml = (String) xpath.evaluate(rootElement + "coverageName", document, XPathConstants.STRING);
                coverageOfferingNameXml = (String) xpath.evaluate(rootElement + "coverageOfferingName", document, XPathConstants.STRING);
            } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException ignored)
            {
                // Could not parse configuration data
                return null;
            }

            if (StringUtils.isEmpty(uri)) return null;

            TileMatrixElevationSource elevationSource;
            try
            {
                // Connect to WCS server and retrieve capabilities
                WcsService wcsService = new WcsService(uri);
                wcsService.retrieveCapabilities();

                // Parse coverage brief offerings
                List<WCS100CoverageOfferingBrief> coverageOfferingBriefs = wcsService.getCoverageOfferingBriefs();
                if (coverageOfferingBriefs.isEmpty()) throw new IllegalStateException("WCS server returned zero coverage offering briefs");
                String coverageName = StringUtils.isEmpty(coverageNameXml) ? coverageOfferingBriefs.get(0).getName() : coverageNameXml;
                WCS100CoverageOfferingBrief coverageOfferingBrief = coverageOfferingBriefs.stream()
                        .filter(o -> o.getName().equals(coverageName))
                        .findFirst()
                        .orElse(null);
                if (coverageOfferingBrief == null) throw new IllegalStateException("WCS server did not return coverage offering brief for " + coverageName);

                // Parse coverage offerings
                wcsService.retrieveCoverage(coverageName);
                List<WCS100CoverageOffering> coverageOfferings = wcsService.getCoverageDescription(coverageName).getCoverageOfferings();
                if (coverageOfferings.isEmpty()) throw new IllegalStateException("WCS server returned zero coverage offerings for " + coverageName);

                String coverageOfferingName = StringUtils.isEmpty(coverageOfferingNameXml) ? coverageOfferings.get(0).getName() : coverageOfferingNameXml;
                WCS100CoverageOffering coverageOffering = coverageOfferings.stream()
                        .filter(o -> o.getName().equals(coverageOfferingName))
                        .findFirst()
                        .orElse(null);
                if (coverageOffering == null) throw new IllegalStateException("WCS server did not return coverage offering for " + coverageOfferingName);

                // Check for cache conflicts
                final File cacheFile = WcsElevationSource.getCacheFile(coverageOfferingName, cacheDir);
                offlineCacheRegistry.registerCacheFile(raptorLayer.getName(), cacheFile); // Will throw an exception if already in use

                // Create elevation source
                elevationSource = WcsElevationSource.create(wcsService, coverageName, coverageOfferingName, cacheDir);
            } catch (Exception e)
            {
                logger.error("Could not connect or retrieve documents from WCS server {} for coverage {}", uri, coverageNameXml, e);
                return null;
            }
            TileMatrixElevationModel elevationModel = TileMatrixElevationModel.create(elevationSource, raptorLayer.getName(), raptorLayer.getDisplayName());
            return new ElevationLayerPair(elevationSource, elevationModel);
        }

        @Override
        protected boolean probe(@Nonnull IRaptorLayer raptorLayer, @Nullable Callback callback)
        {
            if (raptorLayer.getRestorableState() == null) return false;

            // Parse configuration from restorable state
            String uri;
            try
            {
                DocumentBuilder builder = SafeXmlDocumentUtils.getDocumentBuilder();
                builder.setErrorHandler(noopErrorHandler);
                Document document = builder.parse(new InputSource(new StringReader(raptorLayer.getRestorableState())));
                final XPath xpath = XPathFactory.newInstance().newXPath();
                String rootElement = "/" + CONFIGURATION_ELEMENT + "/";
                uri = (String) xpath.evaluate(rootElement + "uri", document, XPathConstants.STRING);
            } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException ignored)
            {
                // Could not parse configuration data
                return false;
            }

            return !StringUtils.isEmpty(uri);
        }
    }
}
