package gov.tak.test.mock;

import gov.tak.api.location.ALocationProvider;
import gov.tak.api.location.ILocationProviderConfiguration;
import gov.tak.api.maps.coords.GeoPointMetaData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Test location provider to use in validating Location Provider functionality.
 *
 * @since 3.0
 */
@ApplicationScoped
public class TestPluginLocationProvider extends ALocationProvider<TestPluginLocationProvider.NullConfiguration>
{
    public static final String PROVIDER_KEY = "TestPluginLocationProvider";

    TestPluginLocationProvider()
    {
        super(PROVIDER_KEY, "plugin defined location provider", false, new NullConfiguration());
    }

    @Nullable
    @Override
    protected NullConfiguration makeConfigurationFromRestoredState(String restoredState)
    {
        return null;
    }

    @Nullable
    @Override
    public GeoPointMetaData getCurrentLocation() throws IllegalStateException
    {
        return null;
    }

    @Nullable
    @Override
    public ILocationProviderConfiguration showConfiguration(@Nonnull ILocationProviderConfiguration configuration)
    {
        return null;
    }

    public static final class NullConfiguration implements ILocationProviderConfiguration
    {
        @Override
        public boolean isEmpty()
        {
            return false;
        }

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
    }
}
