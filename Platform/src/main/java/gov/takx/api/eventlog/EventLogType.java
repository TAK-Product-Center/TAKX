package gov.takx.api.eventlog;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 */
public enum EventLogType
{
    // Do not change the order of these -- the ordinal determines the int value in the database
    CONTAINMENT("Map Object Containment"),
    STATE_CHANGE("Map Object State Changed"),
    MAP_OBJECT_OFFLINE("Map Object Suppressed"),
    MAP_OBJECT_ONLINE("Map Object Added"),
    USER_EVENT("User Entry"),
    PLUGIN_EVENT("Plugin Event"),
    APP_EVENT("App Event"),
    ALARM_STATE_CHANGE("Alarm State Changed"),
    MAP_OBJECT_NAME_CHANGE("Map Object Name Changed");

    private final String description;

    private static final List<Integer> MAP_OBJECT_TYPE_IDS = new ImmutableList.Builder<Integer>()
            .add(CONTAINMENT.ordinal())
            .add(STATE_CHANGE.ordinal())
            .add(MAP_OBJECT_OFFLINE.ordinal())
            .add(MAP_OBJECT_ONLINE.ordinal())
            .add(PLUGIN_EVENT.ordinal())
            .add(ALARM_STATE_CHANGE.ordinal())
            .add(MAP_OBJECT_NAME_CHANGE.ordinal())
            .build();

    private static final List<Integer> APP_OBJECT_TYPE_IDS = new ImmutableList.Builder<Integer>()
            .add(APP_EVENT.ordinal())
            .build();

    private static final List<Integer> USER_TYPE_IDS = new ImmutableList.Builder<Integer>()
            .add(USER_EVENT.ordinal())
            .build();

    EventLogType(String description)
    {
        this.description = description;
    }

    /**
     * @return The ids for the event types associated with map objects
     */
    public static List<Integer> getMapObjectTypeIds()
    {
        return MAP_OBJECT_TYPE_IDS;
    }

    /**
     * @return The ids for event types associated with apps
     */
    public static List<Integer> getAppObjectTypeIds()
    {
        return APP_OBJECT_TYPE_IDS;
    }

    /**
     * @return The ids for the event types associated with users
     */
    public static List<Integer> getUserTypeIds()
    {
        return USER_TYPE_IDS;
    }

    @Override
    public String toString()
    {
        return description;
    }
}
