package gov.tak.api.cot.importer;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for service that manages CoT event importers.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface ICotImporterManager
{
    /**
     * Register a CoT event importer.  If the given {@code importer} implements {@link CotEventTypeImporter},
     * then it will be given preference over importers that are not specific to applicable CoT types.
     *
     * @param importer Importer to register
     */
    void registerImporter(CotEventImporter importer);

    /**
     * Unregister a CoT importer.
     *
     * @param importer Importer to unregister
     */
    void unregisterImporter(CotEventImporter importer);
}
