package gov.tak.api.cot.exporter;

import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.dataservices.persist.MapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Contract for an exporter that creates CoT Events from Map Objects.
 *
 * @since 3.0
 */
@SuppressWarnings("NewClassNamingConvention")
public interface CotEventExporter
{
    /**
     * @return The type of MapObject handled by this exporter.
     */
    Class<? extends MapObject> getType();

    /**
     * Create a CoT Event from a given Map Object.
     *
     * @param mapObject Map Object to process
     * @return Generated CotEvent, may be {@code null} if the exporter can't generate the event for some reason
     */
    @Nullable
    CotEvent export(@Nonnull IMapObject mapObject);
}
