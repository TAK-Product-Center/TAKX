package gov.takx.api.plugin.datamonitor;

import gov.takx.api.mapobject.IDataMonitor;

/**
 * A set of the pre-defined {@link IDataMonitor}s provided by
 * the application. Implementations of these types may be accessed by providing one of the
 * enumerated types to the constructor
 * {@link DataMonitorBuilder#DataMonitorBuilder(String, String, StandardDataMonitorType)}.
 *
 */
public enum StandardDataMonitorType
{
    Comms,
    Battery,
    Power
}
