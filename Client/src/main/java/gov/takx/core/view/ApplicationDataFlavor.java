package gov.takx.core.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.datatransfer.DataFlavor;
import java.lang.invoke.MethodHandles;

/**
 * @since 3.4 (renamed from RaptorDataFlavor)
 */
public class ApplicationDataFlavor extends DataFlavor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String geoFileListMimeType = javaJVMLocalObjectMimeType + "; class=java.util.List";
    public static final String pluginDefinitionMimeType = javaJVMLocalObjectMimeType + "; class=gov.takx.api.plugin.IPluginDefinition";
    public static DataFlavor geoFileListFlavor;
    public static DataFlavor pluginDefinitionFlavor;

    static
    {
        try
        {
            geoFileListFlavor = new ApplicationDataFlavor(geoFileListMimeType);
            pluginDefinitionFlavor = new ApplicationDataFlavor(pluginDefinitionMimeType);
        } catch (ClassNotFoundException exception)
        {
            logger.error("Error creating ApplicationDataFlavor", exception);
        }
    }

    protected ApplicationDataFlavor(String mimeType) throws ClassNotFoundException
    {
        super(mimeType);
    }
}
