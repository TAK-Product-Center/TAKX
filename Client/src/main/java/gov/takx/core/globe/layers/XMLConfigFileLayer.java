package gov.takx.core.globe.layers;

import gov.nasa.worldwind.layers.Layer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * This class represents a globe layer defined by an XML configuration file (a standard WW config file used to define
 * the layer parameters).
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class XMLConfigFileLayer extends WWGlobeLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String configFilePath;

    /**
     * Construct an instance on the given {@link IRaptorLayer} and WW layer.
     *
     * @param raptorLayer    RaptorLayer associated with this globe layer
     * @param layer          WW layer implementing this layers functionality
     * @param configFilePath Path to config file that was loaded
     */
    XMLConfigFileLayer(IRaptorLayer raptorLayer, Layer layer, String configFilePath)
    {
        super(raptorLayer, layer);

        this.configFilePath = configFilePath;
    }

    /**
     * @return Our restorable state.  It is a simple string containing the directory and file name of the
     * shape file data stored in our VFS.
     */
    @Override
    public String getRestorableState()
    {
        return configFilePath;
    }

    /**
     * Restore the layer from a string - do nothing as we have no state to load.
     *
     * @param state Our state string
     */
    @Override
    public void restoreState(String state)
    {
        if (logger.isDebugEnabled()) logger.debug("SKIPPING: restore from state: {}", state);
    }
}
