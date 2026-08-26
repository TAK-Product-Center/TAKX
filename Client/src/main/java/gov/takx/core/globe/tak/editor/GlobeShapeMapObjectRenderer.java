package gov.takx.core.globe.tak.editor;

import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.layer.feature.IFeatureRendererManager;
import gov.takx.api.layer.pick.IPickContext;
import gov.takx.api.layer.pick.IPickedFeatureProvider;
import gov.takx.core.globe.tak.editor.IShapeEditorSource.ShapeEditorSourceListener;
import gov.takx.core.globe.tak.features.FeatureFactory;
import gov.takx.core.globe.tak.features.FeatureFactory.FeatureFactoryState;
import gov.takx.core.globe.tak.features.FeatureFactory.FeatureResult;
import gov.takx.core.globe.tak.features.FeatureFactory.FeatureSetResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An implementation of {@link IShapeEditorRenderer} that renders the {@link IGlobeShapeMapObject} source object using the TAK feature renderer.
 *
 * @since 5.4
 */
@PseudoBean
public class GlobeShapeMapObjectRenderer extends AFeatureRenderer implements ShapeEditorSourceListener, IPickedFeatureProvider
{
    private static final int PROPERTY_MASK = FeatureDataStore4.PROPERTY_FEATURE_NAME | FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY
            | FeatureDataStore4.PROPERTY_FEATURE_STYLE | FeatureDataStore4.PROPERTY_FEATURE_ATTRIBUTES
            | FeatureDataStore4.PROPERTY_FEATURE_ALTITUDE_MODE | FeatureDataStore4.PROPERTY_FEATURE_EXTRUDE;

    @Inject
    private FeatureFactory featureFactory;

    @Inject
    private IFeatureRendererManager featureRendererManager;

    private Long fsid;
    private Object selectedStateObject;
    private final Map<Object, Long> featureIdMap = new HashMap<>();

    @Override
    public void setEnabled(boolean enabled)
    {
        if (enabled)
        {
            fsid = insertFeatureSet(createFeatureSet());
            featureRendererManager.addPickedFeatureProvider(this);
            getShapeEditor().getShapeEditorSource().addShapeEditorSourceListener(this);
            onSourceUpdated(getShapeEditor().getShapeEditorSource().getSource());
            IGlobeShapeMapObject editorShapeMapObject = (IGlobeShapeMapObject) getShapeEditor().getShapeEditorSource().getSource();
            editorShapeMapObject.setEditing(true);
        } else
        {
            featureIdMap.values().forEach(this::deleteFeature);
            featureIdMap.clear();
            deleteFeatureSet(fsid);
            fsid = null;
            featureRendererManager.removePickedFeatureProvider(this);
            getShapeEditor().getShapeEditorSource().removeShapeEditorSourceListener(this);
            IGlobeShapeMapObject editorShapeMapObject = (IGlobeShapeMapObject) getShapeEditor().getShapeEditorSource().getSource();
            editorShapeMapObject.setEditing(false);
        }
    }

    @Override
    public void setArmed(boolean armed)
    {
    }

    @Override
    public <T> void onSourceUpdated(T source)
    {
        IGlobeShapeMapObject editorShapeMapObject = (IGlobeShapeMapObject) source;
        FeatureFactoryState<IGlobeMapObject, Object> featureFactoryState = featureFactory.createState(editorShapeMapObject);
        var state = featureFactoryState.state();
        if (state != null && !Objects.deepEquals(selectedStateObject, state))
        {
            selectedStateObject = state;
            FeatureSetResult featureSetResult = featureFactory.create(featureFactoryState, PROPERTY_MASK);
            for (FeatureResult featureResult : featureSetResult.featureResults())
            {
                Long fid = featureIdMap.get(featureResult.objectId());
                Feature feature = new Feature(fsid, featureResult.name(), featureResult.geometry(), featureResult.style(), featureResult.attributes(), featureResult.altitudeMode(), featureResult.extrude());
                if (fid == null)
                {
                    fid = insertFeature(feature);
                    featureIdMap.put(featureResult.objectId(), fid);
                } else
                {
                    updateFeature(fid, feature, featureResult.updatePropertyMask());
                }
            }
            for (Object objectId : featureIdMap.keySet().stream().toList())
            {
                if (Arrays.stream(featureSetResult.featureResults()).noneMatch(fr -> fr.objectId().equals(objectId)))
                {
                    Long fid = featureIdMap.remove(objectId);
                    deleteFeature(fid);
                }
            }
        }
    }

    @Override
    public @Nullable Object getPickObject(@Nonnull IPickContext context, long fid)
    {
        return featureIdMap.entrySet().stream()
                .filter(e -> e.getValue() == fid)
                .map(e -> getShapeEditor().getShapeEditorSource().getSource())
                .findFirst()
                .orElse(null);
    }
}
