package gov.takx.core.globe.tak;

import gov.tak.core.map.layer.IPickContext;
import gov.tak.core.map.layer.IPickedObjectDeconflictor;
import gov.tak.core.map.layer.IPickedObjectManager;
import gov.tak.core.map.layer.IPickedObjectProvider;
import gov.tak.core.map.layer.PickContext;
import gov.tak.core.map.layer.PickedObject;
import gov.tak.core.map.layer.PickedObjectList;
import gov.takx.api.annotation.Headless;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.tak.layer.PickingLayer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles collecting the {@link PickedObject} instances from {@link IPickedObjectProvider IPickedObjectProviders}.
 *
 * @since 5.2
 */
@ApplicationScoped
public class PickedObjectManager implements IPickedObjectManager
{
    private final List<IPickedObjectProvider> pickedObjectProviders = new CopyOnWriteArrayList<>();
    private final List<IPickedObjectDeconflictor> pickedObjectDeconflictors = new CopyOnWriteArrayList<>();
    private final AtomicReference<PickedObjectList> pickedObjectListRef = new AtomicReference<>(new PickedObjectList());

    private PickingLayer pickingLayer;

    @Inject
    PickedObjectManager(@Headless boolean headless)
    {
        if (!headless) pickingLayer = new PickingLayer();
    }

    @Nonnull
    @Override
    public PickedObjectList getPickedObjectList()
    {
        return pickedObjectListRef.get();
    }

    @Override
    public void addPickedObjectProvider(@Nonnull IPickedObjectProvider provider)
    {
        pickedObjectProviders.add(provider);
    }

    @Override
    public void removePickedObjectProvider(@Nonnull IPickedObjectProvider provider)
    {
        pickedObjectProviders.remove(provider);
    }

    @Override
    public void addPickedObjectDeconflictor(@Nonnull IPickedObjectDeconflictor provider)
    {
        pickedObjectDeconflictors.add(provider);
    }

    @Override
    public void removePickedObjectDeconflictor(@Nonnull IPickedObjectDeconflictor provider)
    {
        pickedObjectDeconflictors.remove(provider);
    }

    /**
     * Runs the pick procedure, allowing any registered {@link IPickedObjectProvider} to submit {@link PickedObject} records
     * into the {@link PickedObjectList} which can be consumed by other components.
     *
     * @param pickContext pick context
     */
    public void pick(@Nonnull IPickContext pickContext)
    {
        final var pickedObjectList = new PickedObjectList();
        for (var provider : pickedObjectProviders) pickedObjectList.addAll(provider.pick(pickContext));
        pickedObjectDeconflictors.forEach(d -> d.deconflictPick(pickedObjectList, pickContext));
        pickedObjectListRef.set(pickedObjectList);
    }

    /**
     * Construct a {@link PickContext} for the given globe, using a 1x1 pixel rectangle around the screen {@code point}.
     *
     * @param globe The globe to operate upon
     * @param point Screen point to use for pick selection
     * @return Pick context
     * @since 5.7
     */
    @Nonnull
    public static PickContext makePickContextForPoint(@Nonnull IApplicationGlobe globe, @Nonnull Point point)
    {
        final var location = globe.getGlobeLocationFromPoint(point);
        final var pickRectangle = new Rectangle(point.x, point.y, 1, 1);

        return new PickContext(globe, pickRectangle, location);
    }

    /**
     * A {@link PickingLayer} for performing picking operations.
     *
     * @return the layer
     */
    @Nullable
    public PickingLayer getLayer()
    {
        return pickingLayer;
    }
}
