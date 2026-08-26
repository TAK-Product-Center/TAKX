package gov.tak.api.cot.importer;

import gov.tak.api.cot.event.CotEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base for CoT Event importers that selectively process events based on the CoT type.
 *
 * @since 3.0
 */
public abstract class AbstractCotEventTypeImporter extends AbstractCotEventImporter implements CotEventTypeImporter
{
    // Set of types this importer supports (unmodifiable)
    private final Set<String> types = new HashSet<>();

    private final Set<String> unmodifiableTypes = Collections.unmodifiableSet(types);

    // Whether to check if the type only starts with any of the set types
    private final boolean prefixOnly;

    /**
     * Construct on a list of types.
     *
     * @param prefixOnly {@code true} if the type list is to be interpreted as prefixes of types, not discrete types.
     * @param types      List of type codes or type prefixes
     */
    protected AbstractCotEventTypeImporter(boolean prefixOnly, String... types)
    {
        if (types.length == 0) throw new IllegalArgumentException("'types' must not be empty");

        this.prefixOnly = prefixOnly;
        this.types.addAll(Arrays.asList(types));
    }

    @Override
    public Set<String> getSupportedCotTypes()
    {
        return unmodifiableTypes;
    }

    @Override
    public boolean isPrefixOnly()
    {
        return prefixOnly;
    }

    @Override
    public ImportResult importData(CotEvent event)
    {
        final String type = event.getType();

        if (prefixOnly)
        {
            for (String t : types)
            {
                if (type.startsWith(t)) return ImportResult.SUCCESS;
            }
            return ImportResult.IGNORE;
        } else if (types.contains(type))
        {
            return ImportResult.SUCCESS;
        } else
        {
            return ImportResult.IGNORE;
        }
    }
}
