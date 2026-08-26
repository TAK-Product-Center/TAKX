package gov.takx.core.globe.tak.editor;

import com.atakmap.map.layer.LegacyAdapters;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.control.FeatureEditControl;
import gov.tak.api.util.Visitor;
import gov.tak.core.map.layer.feature.IFeatureRendererManager;
import gov.tak.core.map.layer.feature.datastore.SharedFeatureDataStore;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.globe.tak.features.IFeatureProvider;
import jakarta.inject.Inject;

import java.util.Collections;

/**
 * Base class for building an {@link IShapeEditorRenderer} implementation that uses the TAK feature renderer.
 *
 * @since 5.4
 */
@PseudoBean
public abstract class AFeatureRenderer extends AShapeEditorRenderer
{
    @Inject
    protected IFeatureRendererManager featureManager;

    @Inject
    protected ApplicationGlobeManager globeManager;

    @Inject
    @SharedFeatureDataStore
    protected FeatureDataStore4 dataStore;

    /**
     * Insert a {@link FeatureSet} to the datastore.
     * @param featureSet the feature set to insert
     * @return the feature set id that was inserted
     */
    protected long insertFeatureSet(FeatureSet featureSet)
    {
        try
        {
            dataStore.acquireModifyLock(true);
            return dataStore.insertFeatureSet(featureSet);
        } catch (DataStoreException | InterruptedException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Delete a feature set by feature set id from the datastore.
     * @param fsid the feature set id to delete
     */
    protected void deleteFeatureSet(long fsid)
    {
        try
        {
            dataStore.acquireModifyLock(true);
            dataStore.deleteFeatureSet(fsid);
        } catch (DataStoreException | InterruptedException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Insert a {@link Feature} to the datastore.
     * @param feature the feature to insert
     * @return the feature id that was inserted
     */
    protected long insertFeature(Feature feature)
    {
        try
        {
            dataStore.acquireModifyLock(true);
            long fid = dataStore.insertFeature(feature);
            visitControl(fec -> fec.startEditing(fid));
            return fid;
        } catch (DataStoreException | InterruptedException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Update a feature by feature id in the datastore.
     * @param fid the feature id to update
     * @param feature the feature to write to the data store
     * @param updatePropertyMask the mask of valid feature components to update
     */
    protected void updateFeature(long fid, Feature feature, int updatePropertyMask)
    {
        try
        {
            dataStore.acquireModifyLock(true);
            dataStore.updateFeature(fid, updatePropertyMask, feature.getName(), feature.getGeometry(), feature.getStyle(), feature.getAttributes(), feature.getAltitudeMode(), feature.getExtrude(), 0);
            visitControl(fec -> fec.updateFeature(fid, updatePropertyMask, feature.getName(), feature.getGeometry(), feature.getStyle(), feature.getAltitudeMode(), feature.getExtrude()));
        } catch (DataStoreException | InterruptedException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Delete a feature by feature id from the datastore.
     * @param fid the feature id to delete
     */
    protected void deleteFeature(long fid)
    {
        try
        {
            dataStore.acquireModifyLock(true);
            visitControl(fec -> fec.stopEditing(fid));
            FeatureDataStore2.FeatureQueryParameters params = new FeatureDataStore2.FeatureQueryParameters();
            params.ids = Collections.singleton(fid);
            dataStore.deleteFeatures(params);
        } catch (DataStoreException | InterruptedException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Create a default feature set to add features to.
     * @return a feature set
     */
    protected FeatureSet createFeatureSet()
    {
        return new FeatureSet("", IFeatureProvider.ALL_GLOBES_GROUP_NAME, "", Double.MAX_VALUE, 0);
    }

    /**
     * Visit the {@link FeatureEditControl} for each globe.
     *
     * @param visitor the visitor
     */
    private void visitControl(Visitor<FeatureEditControl> visitor)
    {
        globeManager.getAllGlobeDelegates().forEach(globe -> {
            FeatureLayer3 featureLayer = featureManager.getFeatureRenderer(globe);
            if (featureLayer != null) globe.getPortableMapRenderer().visitControl(LegacyAdapters.adapt2(featureLayer), visitor, FeatureEditControl.class);
        });
    }
}
