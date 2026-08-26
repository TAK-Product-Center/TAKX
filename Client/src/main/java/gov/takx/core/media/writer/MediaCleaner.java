package gov.takx.core.media.writer;

import java.lang.ref.Cleaner;

/**
 * Get a common cleaner instance for members of the core media package.
 *
 * @since 5.1.0
 */
public final class MediaCleaner
{
    private static final Cleaner INSTANCE = Cleaner.create();

    /**
     * Private constructor as this should never be instantiated.
     */
    private MediaCleaner()
    {
    }

    public static Cleaner getInstance()
    {
        return INSTANCE;
    }
}
