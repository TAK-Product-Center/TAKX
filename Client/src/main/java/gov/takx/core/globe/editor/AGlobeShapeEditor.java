package gov.takx.core.globe.editor;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.platform.KeyParts;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;

/**
 * Abstract base class for all globe shape (Map Object) editors.
 *
 * @since 1.26
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AGlobeShapeEditor extends AGlobeMapObjectEditor<IGlobeShapeMapObject>
{
    @Inject
    @UiEventBus
    protected EventBus uiEventBus;

    @Inject
    private IMapObjectManager mapObjectManager;

    protected AGlobeShapeEditor()
    {
    }

    @Override
    protected void updateMapObject()
    {
        final IMutableShape shapeModel = getGlobeMapObject().getShapeModel();

        if (shapeModel != null && applyUpdatesToModel()) createShapeMapObjectIfNeeded();
    }

    protected abstract boolean applyUpdatesToModel();

    /**
     * If the shape being edited doesn't have an associated MO, then create it now.
     * @since 3.3
     */
    protected void createShapeMapObjectIfNeeded()
    {
        final IMutableShape model = globeMapObject.requireShapeModel();

        if (model.getShapeMapObject() == null)
        {
            final KeyParts parts = new KeyParts(model.getKey());
            final MapObjectSpec spec = MapObjectSpec.builder(parts.getFamily(), parts.getType(), parts.getUnitId())
                    .location(model.getLocation())
                    .name(model.getName())
                    .initializer(mo -> ((IShapeMapObject<?>) mo).setAssociatedShape(model))
                    .build();

            mapObjectManager.getOrCreateMapObject(spec);
        }
    }
}
