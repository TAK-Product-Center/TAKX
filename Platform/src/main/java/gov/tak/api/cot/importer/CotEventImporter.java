package gov.tak.api.cot.importer;

import gov.tak.api.cot.event.CotEvent;

/**
 * Contract for an importer that processes CoT Events.
 *
 * @since 3.0
 */
@SuppressWarnings("NewClassNamingConvention")
public interface CotEventImporter
{
    /**
     * Process a CoT Event and "import" the data it contains.
     *
     * @param event CoT event to process
     * @return Import result
     */
    ImportResult importData(CotEvent event);
}
