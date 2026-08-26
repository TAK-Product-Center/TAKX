package gov.tak.api.cot.exporter;

import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Contract for service that manages exporters that produce CoT events.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface ICotExporterManager
{
    /**
     * Register a CoT Event exporter based on the type it handles.  Exporters registered with a more derived type will
     * override exporters registered on any super type.
     *
     * @param exporter Exporter to register
     * @see CotEventExporter#getType()
     */
    void registerExporter(@Nonnull CotEventExporter exporter);

    /**
     * Unregister an exporter.
     *
     * @param exporter Exporter to unregister
     */
    void unregisterExporter(@Nonnull CotEventExporter exporter);

    /**
     * Export the given MapObject as a CoT Event.  If no registered exporter is found for the type of MapObject,
     * {@code null} will be returned.  Note that the exporter may also return {@code null} if it determines that the Map Object
     * can/should not be exported.
     *
     * @param mapObject Map Object to convert into a CoT Event.
     * @return CoT Event or {@code null}
     */
    @Nullable
    CotEvent export(@Nonnull IMapObject mapObject);
}
