package gov.takx.platform;

import gov.takx.api.commservices.ICommService;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IRaptorMessage;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 */
public class KeyUtils
{
    private static final char KEY_SEP_CHAR = '/';

    /**
     * The separator used to combine components into unique keys.
     */
    public static final String KEY_SEP = String.valueOf(KEY_SEP_CHAR);

    /**
     * Family name for a simple map object shape.
     */
    public static final String TOOL_FAMILY = "gov.takx.TOOL";

    /**
     * Family name for a simple map object file.
     */
    public static final String FILE_FAMILY = "gov.takx.FILE";

    /**
     * Family name for a mission map object.
     *
     * @since 5.0
     */
    public static final String MISSION_FAMILY = "gov.takx.MISSION";

    /**
     * Family name for a compound map object (e.g. routes or multi-polyline).
     *
     * @since 3.0
     */
    public static final String COMPOUND_FAMILY = "gov.takx.COMPOUND";

    /**
     * Family name for default devices.
     */
    public static final String DEFAULT_FAMILY = "Default Family";

    /**
     * Family name for a simple map object icon.
     */
    public static final String RESOURCE_ICON_FAMILY = "gov.takx.RESOURCE_ICON";
    public static final String EMPTY_FAMILY = "";

    private static final List<String> nonDeviceFamilyList =
            Collections.unmodifiableList(Arrays.asList(TOOL_FAMILY, COMPOUND_FAMILY, RESOURCE_ICON_FAMILY, FILE_FAMILY));

    public static String getUniqueKey(IRaptorPlugin plugin)
    {
        return plugin.getFamily() + KEY_SEP + plugin.getType();
    }

    public static String getUniqueKey(IDevicePluginDefinition definition, String unitID)
    {
        return definition.getFamily() + KEY_SEP + definition.getType() + KEY_SEP + unitID;
    }

    public static String getUniqueKey(IDevicePlugin plugin, String unitID)
    {
        return plugin.getFamily() + KEY_SEP + plugin.getType() + KEY_SEP + unitID;
    }

    public static String getUniqueKey(IMapObject mapObject)
    {
        return mapObject.getFamily() + KEY_SEP + mapObject.getType() + KEY_SEP + mapObject.getUnitID();
    }

    /**
     * Generate unique key for the given MapEntity.
     *
     * @param mapEntity MapEntity of interest
     * @return Unique key consisting of family and type
     */
    public static String getUniqueKey(IMapEntity mapEntity)
    {
        return mapEntity.getFamily() + KEY_SEP + mapEntity.getType();
    }

    public static String getUniqueKey(String family, String type)
    {
        return family + KEY_SEP + type;
    }

    public static String getUniqueKey(String family, @Nullable String type, @Nullable String unitID)
    {
        return family + (type != null ? KEY_SEP + type + (unitID != null ? KEY_SEP + unitID : "") : "");
    }

    public static String getUniqueKey(CommServiceType type, String name)
    {
        return type + KEY_SEP + name;
    }

    public static String getUniqueKey(CommServiceType sourceType, String sourceName, String pluginType)
    {
        return sourceType + KEY_SEP + sourceName + KEY_SEP + pluginType;
    }

    public static String getUniqueKey(ICommService commService)
    {
        return commService.getType() + KEY_SEP + commService.getName();
    }

    public static String getUniqueKey(IRaptorMessage message)
    {
        return message.getFamily() + KEY_SEP + message.getType() + KEY_SEP + message.getUnitID();
    }

    /**
     * Generate a key from the given key parts.
     *
     * @param parts Array of parts to join to create the unique key
     * @return unique key
     */
    public static String getUniqueKey(String... parts)
    {
        return StringUtils.join(parts, KEY_SEP, null);
    }

    public static Boolean isNonDevice(String family)
    {
        return family != null && nonDeviceFamilyList.contains(family) && !family.startsWith(RESOURCE_ICON_FAMILY);
    }

    public static List<String> getNonDeviceList()
    {
        return nonDeviceFamilyList;
    }

    public static String getFamilyFromKey(String key)
    {
        final int index = key.indexOf(KEY_SEP);
        return index >= 0 ? key.substring(0, index) : null;
    }

    public static String getTypeFromKey(String key)
    {
        final int index1 = key.indexOf(KEY_SEP);
        final int index2 = index1 >= 0 ? key.indexOf(KEY_SEP, index1 + 1) : -1;

        // If index1 is > 0 and index2 is < 0, it means there wasn't a second delimiter, so go from index1 to end
        if (index1 > 0) return index2 > index1 ? key.substring(index1 + 1, index2) : key.substring(index1 + 1);

        return null;
    }

    public static String getUnitIDFromKey(String key)
    {
        final int index = StringUtils.ordinalIndexOf(key, KEY_SEP_CHAR, 2);
        return index > 0 ? key.substring(index + 1) : null;
    }
}
