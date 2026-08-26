package gov.takx.core.explorer.common;

/**
 * A collection of String keys for store/restore of explorer/explorable/group state.
 *  <br>
 *  Note: These keys reference persisted data, and changing their values can result in data loss. Be careful.
 *
 * @since 1.48, renamed from IExplorerStateKeys and converted to class in 2.37
 */
public class ExplorerStateKeys
{
    public static final String UUID_KEY = "uuid";
    public static final String TITLE_KEY = "title";
    public static final String CONTROLLER_STATE_KEY = "groupsControllerState"; // Value of this key is legacy and retained to prevent data loss
    public static final String ICON_FOLDER_LOCATION_KEY = "iconFolderLocation";
    public static final String RULE_GROUPS_KEY = "ruleGroups";
    public static final String STANDARD_GROUPS_KEY = "standardGroups";
    public static final String ICON_IMAGE_KEY = "iconImage";
    public static final String DISPLAY_NAME_KEY = "displayName";
    public static final String UNIQUE_KEY_KEY = "uniqueKey";
    public static final String UNIQUE_KEYS_KEY = "uniqueKeys";
    public static final String MATCH_ALL_KEY = "matchAll";
    public static final String PATH_KEY = "pathKey";
    public static final String RULES_KEY = "rules";
    public static final String RULE_TYPE_KEY = "ruleType";
}
