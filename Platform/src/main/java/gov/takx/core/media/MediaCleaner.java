package gov.takx.core.media;

import gov.tak.api.annotation.IncubatingApi;

import java.lang.ref.Cleaner;

/**
 * A common cleaner instance for members of the core media package.
 *
 * @since 5.1.0 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
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
