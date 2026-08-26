package gov.takx.core.globe.tak;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.ModelInfoFactory;
import com.atakmap.map.layer.model.SceneLayer;
import gov.takx.api.layer.builder.ModelLayerBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A Layer that controls a 3D model within a {@link SceneLayer}.
 *
 * @since 5.1
 */
public class TakSceneLayer extends AbstractLayer
{
    private final List<String> scenePaths;
    private final SceneLayer sceneLayer;
    private Envelope envelope;

    public TakSceneLayer(@Nonnull String name, @Nonnull String scenePaths, @Nonnull SceneLayer sceneLayer)
    {
        super(name);
        this.scenePaths = ModelLayerBuilder.parseRestorableString(scenePaths);
        this.sceneLayer = sceneLayer;
    }

    @Override
    public synchronized void setVisible(boolean visible)
    {
        scenePaths.forEach(scenePath -> sceneLayer.setVisible(scenePath, visible));
        super.setVisible(visible);
    }

    /**
     * Get the model's bounding envelope from its metadata. Returns {@code null} if envelope data is not available.
     *
     * @return the envelope, or {@code null}
     */
    @Nullable
    public synchronized Envelope getEnvelope()
    {
        if (envelope == null)
        {
            final var envelopeBuilder = new Envelope.Builder();

            // Convert to flat map of ModelInfos and merge envelopes
            scenePaths.stream()
                    .map(File::new)
                    .map(file -> IOProviderFactory.exists(file) ? file.toString() : file.getPath())
                    .map(ModelInfoFactory::create)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(ModelLayerManager::getModelEnvelope)
                    .filter(Objects::nonNull)
                    .forEach(envelopeBuilder::add);

            envelope = envelopeBuilder.build();
        }

        return envelope;
    }
}
