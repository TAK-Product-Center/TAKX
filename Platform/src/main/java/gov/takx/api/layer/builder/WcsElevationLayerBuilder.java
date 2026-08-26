package gov.takx.api.layer.builder;

import com.google.auto.value.AutoBuilder;
import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerConfiguration.LayerClassType;
import gov.takx.api.layer.LayerGroup;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;

/**
 * A builder to construct a {@link LayerConfiguration} for adding WCS elevation layers to the globe.
 *
 * @since 5.4
 */
@AutoBuilder(ofClass = WcsElevationLayerBuilder.class, callMethod = "build")
public abstract class WcsElevationLayerBuilder extends LayerBuilder<WcsElevationLayerBuilder>
{
    /**
     * Construct a {@link WcsElevationLayerBuilder}.
     * @return the builder
     */
    public static WcsElevationLayerBuilder builder()
    {
        return LayerBuilder.setOptionals(new AutoBuilder_WcsElevationLayerBuilder());
    }

    static LayerConfiguration build(@Nonnull String name, @Nullable String displayName, boolean visible, @Nonnull String uri, @Nullable String coverageName, @Nullable String coverageOfferingName)
    {
        String restorableXmlString = buildXml(uri, coverageName, coverageOfferingName);
        return new LayerConfiguration(name, displayName, LayerClassType.Elevation, visible, false, null,
                LayerGroup.ELEVATION, null, restorableXmlString, false, null);
    }

    /**
     * Set the URI for the WCS server.
     * @param uri the WCS server uri
     * @return the builder for chaining
     */
    public abstract WcsElevationLayerBuilder setUri(@Nonnull String uri);

    /**
     * Set the coverage name to use for the WCS data. If {@code null} the first coverage name found is used.
     * @param coverageName the coverage name, or {@code null}
     * @return the builder for chaining
     */
    public abstract WcsElevationLayerBuilder setCoverageName(@Nullable String coverageName);

    /**
     * Set the coverage offering name to use for the WCS data. If {@code null} the first coverage offering name found is used.
     * @param coverageOfferingName the coverage offering name, or {@code null}
     * @return the builder for chaining
     */
    public abstract WcsElevationLayerBuilder setCoverageOfferingName(@Nullable String coverageOfferingName);

    /**
     * Build the layer configuration.
     * @return the layer configuration
     */
    public abstract LayerConfiguration build();

    /**
     * Build a WCS XML configuration string for creating an elevation layer that WcsElevationLayerSpi can parse.
     * @param uri the uri of the WCS server
     * @param coverageName the name of the coverage
     * @param coverageOfferingName the name of the coverage offering
     * @return the XML configuration string
     */
    @VisibleForTesting
    static String buildXml(@Nonnull String uri, @Nullable String coverageName, @Nullable String coverageOfferingName)
    {
        try
        {
            Document document = SafeXmlDocumentUtils.getDocumentBuilder().newDocument();
            Node rootNode = document.appendChild(document.createElement("wcsSource"));
            var uriElement = (Element) rootNode.appendChild(document.createElement("uri"));
            uriElement.setNodeValue(uri);
            if (!StringUtils.isEmpty(coverageName))
            {
                var coverageNameElement = (Element) rootNode.appendChild(document.createElement("coverageName"));
                coverageNameElement.setNodeValue(coverageName);
            }
            if (!StringUtils.isEmpty(coverageOfferingName))
            {
                var coverageOfferingNameElement = (Element) rootNode.appendChild(document.createElement("coverageOfferingName"));
                coverageOfferingNameElement.setNodeValue(coverageOfferingName);
            }
            return SafeXmlDocumentUtils.formatXml(document);
        } catch (ParserConfigurationException | IOException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
