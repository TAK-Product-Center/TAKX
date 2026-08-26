package gov.takx.core.globe.tak;

import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureQueryParameters;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.opengl.GLLabelManager;
import com.atakmap.map.opengl.GLMapView;
import gov.nasa.worldwind.render.Annotation;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.core.map.layer.IPickContext;
import gov.tak.core.map.layer.IPickedObjectProvider;
import gov.tak.core.map.layer.PickedObject;
import gov.tak.core.map.layer.PickedObjectList;
import gov.tak.core.map.layer.feature.IFeatureRendererManager;
import gov.tak.core.map.layer.feature.IPickedFeatureProvider;
import gov.tak.core.map.layer.feature.datastore.SharedFeatureDataStore;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.globe.tak.features.IFeatureProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages a feature renderer layer and picking operations on the feature layer.
 * @since 5.2
 */
@ApplicationScoped
public class FeatureRendererManager implements IFeatureRendererManager, IPickedObjectProvider, IGlobeLifeCycleListener
{
    private final List<IPickedFeatureProvider> pickedFeatureProviders = new CopyOnWriteArrayList<>();

    private final Map<IApplicationGlobe, FeatureLayer3> featureLayerMap = new HashMap<>();
    private final FeatureDataStore4 dataStore;

    @Inject
    FeatureRendererManager(@SharedFeatureDataStore FeatureDataStore4 dataStore, ApplicationGlobeManager globeManager)
    {
        this.dataStore = dataStore;
        globeManager.addGlobeLifeCycleListener(this);
    }

    @Override
    public void globeRemoved(IGlobeDelegate globe)
    {
        featureLayerMap.remove(globe);
    }

    @Nonnull
    @Override
    public List<PickedObject> pick(@Nonnull IPickContext context)
    {
        PickedObjectList pickedObjectList = new PickedObjectList();
        FeatureLayer3 featureLayer = getFeatureRenderer(context.globe());
        if (featureLayer == null) return Collections.emptyList();
        GLMapView glMapView = context.globe().getGLMapView();
        float size = Math.max(context.rectangle().width, context.rectangle().height);
        HitTestQueryParameters queryParameters = new HitTestQueryParameters(glMapView.getRenderContext().getRenderSurface(), context.rectangle().x, context.rectangle().y, size, DisplayOrigin.UpperLeft);
        queryParameters.initGeoPoint(glMapView);
        queryParameters.limit = 128;
        List<HitTestResult> hitTestResults = new LinkedList<>();
        glMapView.visitControl(featureLayer, hitTestControl -> hitTestControl.hitTest(glMapView, queryParameters, hitTestResults), HitTestControl.class);

        for (HitTestResult hitTestResult : hitTestResults)
        {
            long fid = (long) hitTestResult.subject;
            for (IPickedFeatureProvider pickedFeatureProvider : pickedFeatureProviders)
            {
                Object pickedObject = pickedFeatureProvider.getPickObject(context, fid);
                if (pickedObject != null)
                {
                    // Give an artificial hit test boost to Annotations with isAlwaysOnTop().
                    boolean isTopmostAnnotation = pickedObject instanceof Annotation annotation && annotation.isAlwaysOnTop();
                    if (isTopmostAnnotation)
                    {
                        pickedObjectList.add(0, new PickedObject(context.location(), pickedObject));
                    } else
                    {
                        pickedObjectList.add(new PickedObject(context.location(), pickedObject));
                    }
                    break;
                }
            }
        }
        return pickedObjectList;
    }

    @Override
    public void addPickedFeatureProvider(@Nonnull IPickedFeatureProvider pickedObjectProvider)
    {
        pickedFeatureProviders.add(pickedObjectProvider);
    }

    @Override
    public void removePickedFeatureProvider(@Nonnull IPickedFeatureProvider pickedObjectProvider)
    {
        pickedFeatureProviders.remove(pickedObjectProvider);
    }

    /**
     * Create and track a {@link FeatureLayer3} for the provided {@link IApplicationGlobe globe}.
     * @param globe the globe
     * @return a feature layer
     * @since 5.3
     */
    @Nullable
    public FeatureLayer3 createFeatureRenderer(@Nonnull IApplicationGlobe globe)
    {
        var opts = new FeatureLayer3.Options();
        opts.filter = new FeatureQueryParameters();
        opts.filter.featureSetFilter = new FeatureDataStore4.FeatureSetQueryParameters();
        opts.filter.featureSetFilter.types = Set.of("", IFeatureProvider.ALL_GLOBES_GROUP_NAME, Integer.toString(globe.getGlobeId()));
        opts.labelHints |= GLLabelManager.HINT_XRAY;
        FeatureLayer3 featureLayer = new FeatureLayer3("Shared Feature Layer", dataStore, opts);
        featureLayerMap.put(globe, featureLayer);
        return featureLayer;
    }

    @Override
    @Nullable
    public FeatureLayer3 getFeatureRenderer(@Nonnull IApplicationGlobe globe)
    {
        return featureLayerMap.get(globe);
    }
}
