package gov.takx.api.mapobject;

import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.ITrackStyleDefinition;

/**
 * Enum for indicating Map Object handling/features to be disabled within the core.
 *
 * @since 2.29
 */
public enum MapObjectOption
{
    /**
     * Indicates that the explorer tree should NOT create a node for this Map Object.
     */
    ExcludeFromExplorer,

    /**
     * Indicates that the Map Object will not use data monitors and will not generate alarms.  This includes
     * disabling the capacity for the user to create "custom alarms" on the Map Object.
     */
    NoAlarms,

    /**
     * Indicates that the Map Object's tracks are not to have any "styled" rendering applied to them.  So, no
     * {@link ITrackStyleDefinition} or {@link IConnectingLineDefinition} will be created, or used.
     */
    NoStyledTracks,

    /**
     * Indicates that the Map Object should not be included in the Event Log.
     */
    NoEventLogs,

    /**
     * Indicates that the plugin does not have any state to be stored in the project database.
     */
    NoPluginState,

    /**
     * Indicates that the Map Object should NOT be persisted in the project database.
     * @since 5.6
     */
    NoPersistence,
    ;

    public static final MapObjectOption[] VALUES = values();

    MapObjectOption()
    {
        //noinspection ConstantValue
        if (ordinal() >= Long.SIZE) throw new IllegalStateException("Can not have more than 64 Map Object Options!");
    }
}
