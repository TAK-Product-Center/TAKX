package gov.takx.api.layer.feature;

import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureSet;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for a feature data store that is shared by the core and available to plugins.
 *
 * @since 5.8
 */
@NoPluginImplementations
@InjectableService
public interface ISharedFeatureDataStore extends FeatureDataStore4
{
    /** A value for {@link FeatureSet#getType()} to specify to the {@link ISharedFeatureDataStore} that the feature should be visible on all globes. */
    String ALL_GLOBES_FEATURE_SET_TYPE = "AllGlobes";

    /**
     * @return {@code true} if the feature with the given {@code id} is visible
     */
    boolean isFeatureVisible(long id);
}
