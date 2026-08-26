package gov.takx.core.view.gui.layers;

import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities to manage "default layers" as defined in {@code GlobeLayers.xml}.  Default layers are added to all new
 * projects.
 */
@ApplicationScoped
public class DefaultLayerUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Only imagery and elevation data can be default layers
    private static final List<LayerGroup> LAYER_GROUPS = List.of(LayerGroup.IMAGERY, LayerGroup.ELEVATION);

    private final IRaptorLayerManager layerManager;
    private final IProgramEnvironment programEnvironment;
    private final File globeLayersFile;

    private final PropertyChangeListener changeListener = this::propertyChanged;
    private final XPath xpath = XPathFactory.newInstance().newXPath();

    // Map from layer group to the list of default layer names (those defined in GlobeLayers.xml)
    private final Map<LayerGroup, List<String>> defaultLayersByGroup = new EnumMap<>(LayerGroup.class);

    @Inject
    DefaultLayerUtils(IRaptorLayerManager layerManager, IProgramEnvironment programEnvironment)
    {
        this.layerManager = layerManager;
        this.programEnvironment = programEnvironment;
        globeLayersFile = programEnvironment.getGlobeLayersFile();

        loadDefaultLayers();
    }

    /**
     * Start the service.  Monitor all default layers for changes and update the {@code GlobeLayers.xml}.
     * @since 5.4.1
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        for (LayerGroup layerGroup : LAYER_GROUPS)
        {
            layerManager.getLayerList(layerGroup).forEach(layer -> layer.addPropertyChangeListener(changeListener));
        }
    }

    /**
     * When a property on a default layer changes, update the entry in {@code GlobeLayers.xml}.
     * @since 5.4.1
     */
    private void propertyChanged(PropertyChangeEvent event)
    {
        // Ignore DELETED property changes because the layer will be removed by removeDefaultLayer later, and the
        // underlying native layer on this RaptorLayer will be null, which would lead to NPEs trying to read its state.
        if (event.getPropertyName().equals(IRaptorLayer.DELETED)) return;

        final var layer = (IRaptorLayer) event.getSource();

        if (isDefaultLayer(layer))
        {
            removeDefaultLayer(layer);
            addDefaultLayer(layer);
        }
    }

    private void loadDefaultLayers()
    {
        try
        {
            final Document document = readGlobeLayersFile();

            // Only Imagery and ELEVATION layers can be managed as default layers by the user
            for (LayerGroup layerGroup : LAYER_GROUPS)
            {
                final String expression = String.format("/layerGroups/layerGroup[@name='%s']", layerGroup.name());
                final Node layerGroupNode = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);

                if (layerGroupNode != null)
                {
                    final ArrayList<String> defaultLayerNames = new ArrayList<>();
                    defaultLayersByGroup.put(layerGroup, defaultLayerNames);

                    final NodeList layers = (NodeList) xpath.evaluate("layer", layerGroupNode, XPathConstants.NODESET);

                    for (int i = 0, size = layers.getLength(); i < size; i++)
                    {
                        defaultLayerNames.add(xpath.evaluate("@name", layers.item(i)));
                    }
                } else
                {
                    logger.warn("{} file is malformed, can't find LayerGroup {}", globeLayersFile, layerGroup);
                }
            }
        } catch (Exception e)
        {
            logger.error("Failed to load default layer list from: {}", globeLayersFile, e);
        }
    }

    /**
     * Determines whether a particular {@link IRaptorLayer} is included in the {@code GlobeLayers.xml}.
     *
     * @param layer raptor layer to check is included
     * @return {@code true} if the {@code GlobeLayers.xml} file contains the layer, {@code false} otherwise
     */
    public boolean isDefaultLayer(@Nonnull IRaptorLayer layer)
    {
        return defaultLayersByGroup.getOrDefault(layer.getLayerGroup(), Collections.emptyList())
                .contains(layer.getName());
    }

    /**
     * Removes the given {@code layer} from the {@code GlobeLayers.xml} file.
     */
    public void removeDefaultLayer(@Nonnull IRaptorLayer layer)
    {
        final String layerName = layer.getName();
        final LayerGroup layerGroup = layer.getLayerGroup();

        checkLayerGroup(layerGroup);

        layer.removePropertyChangeListener(changeListener); // Stop monitoring for changes

        try
        {
            final Document document = readGlobeLayersFile();
            final String expression = String.format("/layerGroups/layerGroup[@name='%s']/layer[@name='%s']",
                    layerGroup.name(), layerName);
            final Node layerNode = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);

            if (layerNode != null)
            {
                defaultLayersByGroup.get(layerGroup).remove(layerName);
                layerNode.getParentNode().removeChild(layerNode);
                writeGlobeLayersFile(document);
            } else
            {
                logger.warn("Layer '{}' not found", layerName);
            }
        } catch (Exception e)
        {
            logger.error("Failed to remove layer '{}' from layers file: {}", layerName, globeLayersFile, e);
        }
    }

    /**
     * Adds the given {@code layer} to the {@code GlobeLayers.xml} file.
     */
    public void addDefaultLayer(@Nonnull IRaptorLayer layer)
    {
        final LayerGroup layerGroup = layer.getLayerGroup();

        checkLayerGroup(layerGroup);

        try
        {
            final Document document = readGlobeLayersFile();
            final String expression = String.format("/layerGroups/layerGroup[@name='%s']", layerGroup.name());
            final Node layerGroupNode = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);

            if (layerGroupNode != null)
            {
                final Element element = makeLayerElement(layer, document);

                // Add the new node and write out the new xml file.
                defaultLayersByGroup.get(layerGroup).add(layer.getName());
                layerGroupNode.appendChild(element);
                writeGlobeLayersFile(document);

                layer.addPropertyChangeListener(changeListener); // Monitor this new layer for changes
            } else
            {
                logger.warn("{} file is malformed, can't find LayerGroup {}", globeLayersFile, layerGroup);
            }
        } catch (Exception e)
        {
            logger.error("Failed to add layer '{}' to layers file: {}", layer.getName(), globeLayersFile, e);
        }
    }

    /**
     * Check the given {@code layerGroup} is valid for use as a default layer.
     * @throws IllegalArgumentException if not
     * @since 5.4.1
     */
    private static void checkLayerGroup(LayerGroup layerGroup)
    {
        if (!LAYER_GROUPS.contains(layerGroup))
        {
            throw new IllegalArgumentException("Layer Group " + layerGroup + " can't be default layers");
        }
    }

    /**
     * Create the Element (Node) to add for a new default layer.
     *
     * @param layer Layer being added
     * @param document Document being processed
     * @return Created element
     * @since 3.5
     */
    private Element makeLayerElement(IRaptorLayer layer, Document document)
    {
        final Element element = document.createElement("layer");

        // Layer cannot have both class and config name set.
        final String configFile = layer.getConfigFileName();
        if (StringUtils.isEmpty(configFile))
        {
            element.setAttribute("class", layer.getClassName());
        } else
        {
            element.setAttribute("configFile", configFile);
        }

        element.setAttribute("displayName", layer.getDisplayName());
        element.setAttribute("iconPath", layer.getIconPath());
        element.setAttribute("maxActiveAltitude", Double.toString(layer.getMaxActiveAltitude()));
        element.setAttribute("minActiveAltitude", Double.toString(layer.getMinActiveAltitude()));
        element.setAttribute("name", layer.getName());
        element.setAttribute("permanent", Boolean.toString(layer.isPermanent()));
        element.setAttribute("pickable", Boolean.toString(layer.isPickable()));
        element.setAttribute("visible", Boolean.toString(layer.isVisible()));

        final String restorableStateFilename = saveDefaultLayerState(layer, layer.getLayerGroup());
        if (restorableStateFilename != null)
        {
            element.setAttribute("restorableStateLocation", restorableStateFilename);
        }
        return element;
    }

    private String saveDefaultLayerState(IRaptorLayer raptorLayer, LayerGroup layerGroup)
    {
        String restorableState = null;
        if (layerGroup == LayerGroup.IMAGERY || layerGroup == LayerGroup.ELEVATION)
        {
            restorableState = raptorLayer.getLayer().getRestorableState();
        }

        if (restorableState == null) return null;

        // Sanitize the display name to be a valid file name
        final String stateFileName = raptorLayer.getDisplayName()
                .replaceAll("[^\\w+.()\\-]", "") + ".xml";

        final Path stateFile = Paths.get(programEnvironment.getGlobeLayersDirectoryPath(), stateFileName);

        try
        {
            Files.writeString(stateFile, restorableState);
        } catch (IOException e)
        {
            logger.error("Failed to write layer state file: {}", stateFile, e);
            return null;
        }

        // Relativize the path to the program root
        return Paths.get(programEnvironment.getProgramRoot()).relativize(stateFile).toString();
    }

    /**
     * Get a {@link Document} from parsing the globe layers file.
     * @since 3.5
     */
    private Document readGlobeLayersFile() throws ParserConfigurationException, IOException, SAXException
    {
        return SafeXmlDocumentUtils.getDocumentBuilder().parse(globeLayersFile);
    }

    /**
     * Normalize and write out the given {@code document} to the globe layers file.
     * @since 3.5
     */
    private void writeGlobeLayersFile(Document document) throws TransformerException
    {
        document.normalize();

        TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(document), new StreamResult(globeLayersFile));
    }
}
