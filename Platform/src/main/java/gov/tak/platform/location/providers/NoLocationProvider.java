package gov.tak.platform.location.providers;

import gov.tak.api.location.ALocationProvider;
import gov.tak.api.location.ILocationProviderConfiguration;
import gov.tak.api.maps.coords.GeoPointMetaData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Location provider that always returns {@code null} for the current location.
 * <p>
 * This is the default location provider installed at startup, unless a different one has been configured.
 *
 * @since 3.0
 */
@ApplicationScoped
public class NoLocationProvider extends ALocationProvider<NoLocationProvider.Configuration>
{
    public static final String KEY = "NoLocationProvider";
    public static final String DISPLAY_NAME = "No Location";

    NoLocationProvider()
    {
        super(KEY, DISPLAY_NAME, false, new Configuration());
    }

    @Nullable
    @Override
    public GeoPointMetaData getCurrentLocation() throws IllegalStateException
    {
        return null;
    }

    @Override
    public boolean isConfigured()
    {
        return true;
    }

    @Override
    public ILocationProviderConfiguration showConfiguration(@Nonnull ILocationProviderConfiguration configuration)
    {
        return null;
    }

    @Nullable
    @Override
    protected Configuration makeConfigurationFromRestoredState(String restoredState)
    {
        return new Configuration();
    }

    /**
     * Trivially empty configuration.
     */
    public static final class Configuration implements ILocationProviderConfiguration
    {
        @Nonnull
        @Override
        public String getRestorableState()
        {
            return "";
        }

        @Nonnull
        @Override
        public String getDisplayableString()
        {
            return "";
        }

        @Override
        public boolean isEmpty()
        {
            return false;
        }
    }
}
