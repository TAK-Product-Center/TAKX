package gov.tak.api.cot.importer;

import java.util.Set;

/**
 * Contract for importers that process a specific type, collection of types, or types with a given prefix.
 *
 * @since 3.0
 */
@SuppressWarnings("NewClassNamingConvention")
public interface CotEventTypeImporter extends CotEventImporter
{
    /**
     * @return Collection of CoT types (or type prefixes) handled by this importer
     */
    Set<String> getSupportedCotTypes();

    /**
     * @return {@code true} if the supported types are prefixes of CoT types and not discrete types
     */
    boolean isPrefixOnly();
}
