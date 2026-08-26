package gov.takx.platform.utils;

import gov.takx.api.plugin.IRaptorObject;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;

/**
 * Convenience utility for creating and safely cloning (reducing castability) an {@link IRaptorObject}.
 *
 * @since 1.7
 */
public final class RaptorObjectFactory
{
    private RaptorObjectFactory()
    {
    }

    public static IRaptorObject createRaptorObject(String family, String type)
    {
        return create(family, type, KeyUtils.getUniqueKey(family, type));
    }

    public static IRaptorObject createRaptorObject(String family, String type, String uniqueKey)
    {
        return create(family, type, uniqueKey);
    }

    /**
     * Creates a copy of the {@link IRaptorObject}, fully guaranteeing that the original IRaptorObject cannot be maliciously
     * cast to its actual implementation type.  Family, Type, and Unique Key will be maintained.
     *
     * @param source IRaptorObject that is the source of the connection event
     * @return protected IRaptorObject that cannot be cast to the actual event source
     */
    public static IRaptorObject clone(final IRaptorObject source)
    {
        return clone(source, false);
    }

    /**
     * Creates a copy of the {@link IRaptorObject}, fully guaranteeing that the original IRaptorObject cannot be maliciously
     * cast to its actual implementation type.  Family will be maintained, and type and unique key will be optionally
     * stripped based on the stripFlag.
     *
     * @param source IRaptorObject that is the source of the connection event
     * @return protected IRaptorObject that cannot be cast to the actual event source
     */
    public static IRaptorObject clone(@Nonnull final IRaptorObject source, final boolean stripFlag)
    {
        final String type = stripFlag ? "" : source.getType();
        final String uniqueKey = stripFlag ? "" : source.getUniqueKey();

        return new IRaptorObject()
        {
            @Override
            public String getFamily()
            {
                return source.getFamily();
            }

            @Override
            public String getType()
            {
                return type;
            }

            @Override
            public String getUniqueKey()
            {
                return uniqueKey;
            }

            @Nonnull
            @Override
            public Set<String> getMetadataKeys()
            {
                return source.getMetadataKeys();
            }

            @Override
            public String getMetadataValue(String key)
            {
                return source.getMetadataValue(key);
            }
        };
    }

    private static IRaptorObject create(final String family, final String type, final String uniqueKey)
    {
        return new IRaptorObject()
        {
            @Override
            public String getFamily()
            {
                return family;
            }

            @Override
            public String getType()
            {
                return type;
            }

            @Override
            public String getUniqueKey()
            {
                return uniqueKey;
            }

            @Nonnull
            @Override
            public Set<String> getMetadataKeys()
            {
                return Collections.emptySet();
            }

            @Override
            public String getMetadataValue(String key)
            {
                return null;
            }
        };
    }
}
