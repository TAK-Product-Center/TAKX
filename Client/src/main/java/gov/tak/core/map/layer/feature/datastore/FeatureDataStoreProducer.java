package gov.tak.core.map.layer.feature.datastore;

import gov.tak.core.map.layer.feature.datastore.memory.InMemoryFeatureDataStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Produces {@link InMemoryFeatureDataStore} instances.
 *
 * @since 5.2
 */
@ApplicationScoped
public class FeatureDataStoreProducer
{
    /**
     * @return an {@link InMemoryFeatureDataStore} singleton instance for use by various feature providers
     */
    @Produces
    @SharedFeatureDataStore
    @Singleton
    public InMemoryFeatureDataStore produceSharedFeatureDataStore()
    {
        return new InMemoryFeatureDataStore();
    }
}
