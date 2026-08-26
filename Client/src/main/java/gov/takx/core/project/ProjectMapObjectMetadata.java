package gov.takx.core.project;

import gov.takx.api.project.IProjectMapObjectMetadata;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.KeyParts;
import gov.takx.platform.KeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.regex.Pattern;

/**
 * Simple object to hold the component data relating to a map object, its unique key and display name.
 *
 */
public class ProjectMapObjectMetadata implements IProjectMapObjectMetadata
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String UNKNOWN_KEY = "unknown/unknown/unknown";

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final String name;
    private final String key;
    private final KeyParts keyParts;

    /**
     * Construct from a string of the form {@code "uniqueKey,name"}.
     */
    public ProjectMapObjectMetadata(String mapObjectInfo)
    {
        // It is legal to have an empty name, so include trailing empty strings (pass -1 to split)
        final String[] metaArray = mapObjectInfo.split(",", -1);

        if (metaArray.length > 2) // Support for V2 format
        {
            String family = metaArray[2];
            if (ShapeProperties.PUBLISHED.equals(family)) family = KeyUtils.TOOL_FAMILY;

            key = KeyUtils.getUniqueKey(family, metaArray[1], metaArray[0]);
            name = metaArray.length > 3 ? metaArray[3] : ""; // Empty name is allowed
        } else if (metaArray.length == 2) // V3 format
        {
            key = metaArray[0];
            name = metaArray[1];
        } else
        {
            key = UNKNOWN_KEY;
            name = "";
            logger.debug("Invalid MapObject.entry '{}'", mapObjectInfo);
        }

        keyParts = new KeyParts(key);
    }

    @Override
    public String getFamily()
    {
        return keyParts.getFamily();
    }

    @Override
    public String getType()
    {
        return keyParts.getType();
    }

    @Override
    public String getUnitID()
    {
        return keyParts.getUnitId();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getKey()
    {
        return key;
    }

    @Override
    public boolean isUnitIdAUuid()
    {
        return UUID_PATTERN.matcher(getUnitID()).matches();
    }

    @Override
    public String getDisplayText()
    {   // Replace a UUID with an empty string since UUIDs are meaningless to the user
        final String id = isUnitIdAUuid() ? "" : ": " + getUnitID();

        return getType() + id + " (" + getName() + ")";
    }
}
