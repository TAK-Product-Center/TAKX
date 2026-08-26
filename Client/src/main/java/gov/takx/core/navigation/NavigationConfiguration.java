package gov.takx.core.navigation;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.IPropertyChangeDelegateSupport;
import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * This class holds navigation configuration data to be shown in the Navigation panel. It maintains references to the source
 * and target objects, and fires property change events from them so that other components can listen directly on this class.
 * It's subclasses can determine how individual {@link NavigationTarget} instances are created depending on the source and
 * target object type.
 *
 * @since 5.6 (Renamed from BloodhoundConfiguration in 5.7)
 */
@PseudoBean
public abstract class NavigationConfiguration implements IPropertyChangeDelegateSupport, IResourceCleaner
{
    /** The property name for ETA property change events */
    public static final String ETA = "ETA";
    public static final String CURRENT_TARGET = "CURRENT_TARGET";
    public static final String REVERSED = "REVERSED";
    private static final List<String> LISTENER_PROPERTIES = List.of(IMapObject.LOCATION, IMapObject.NAME);

    @Inject
    protected NavigationManager navigationManager;

    @Inject
    @Headless
    private boolean headless;

    protected final PrioritizedPropertyChangeSupport propertyChangeSupport = new PrioritizedPropertyChangeSupport(this);
    protected final ListenerManager listenerManager = new ListenerManager();
    protected final List<NavigationTarget> targets = new ArrayList<>();
    protected final String id = UUID.randomUUID().toString();
    protected final IPointMapObject sourceMapObject;
    protected final IMapObject targetMapObject;

    protected boolean colorEnabled;
    protected boolean flashEnabled;
    private NavigationFlasher flasher;
    private int currentTargetIndex;
    private boolean reversed;

    /**
     * Construct a new navigation instance for the source object to navigate to / along the target.
     *
     * @param sourceMapObject the source map object
     * @param targetMapObject the target map object
     */
    NavigationConfiguration(@Nonnull IPointMapObject sourceMapObject, @Nonnull IMapObject targetMapObject)
    {
        this.sourceMapObject = sourceMapObject;
        this.targetMapObject = targetMapObject;

        listenerManager.addPropertyChangeListener(sourceMapObject, LISTENER_PROPERTIES, propertyChangeSupport::firePropertyChangeCopy);
        listenerManager.addPropertyChangeListener(targetMapObject, LISTENER_PROPERTIES, propertyChangeSupport::firePropertyChangeCopy);
        listenerManager.addPropertyChangeListener(sourceMapObject, LISTENER_PROPERTIES, ignored -> update());
        listenerManager.addPropertyChangeListener(targetMapObject, LISTENER_PROPERTIES, ignored -> update());
    }

    @PostConstruct
    private void postConstruct()
    {
        flasher = headless ? null : new NavigationFlasher(this);
    }

    /**
     * @since 5.7
     */
    @Override
    public void releaseResources()
    {
        listenerManager.unregisterAll();
        targets.forEach(NavigationTarget::releaseResources);
        if (flasher != null) flasher.releaseResources();
    }

    @Nonnull
    @Override
    public IPropertyChangeListenerSupport getPropertyChangeListenerSupport()
    {
        return propertyChangeSupport;
    }

    /**
     * @return the id
     */
    @Nonnull
    public String getId()
    {
        return id;
    }

    /**
     * @return the source map object
     */
    @Nonnull
    public IMapObject getSourceMapObject()
    {
        return sourceMapObject;
    }

    /**
     * @return the target map object
     */
    @Nonnull
    public IMapObject getTargetMapObject()
    {
        return targetMapObject;
    }

    /**
     * @return the current navigation target
     * @since 5.7
     */
    @Nonnull
    public NavigationTarget getCurrentTarget()
    {
        return targets.get(currentTargetIndex);
    }

    /**
     * @return {@code true} if this navigation configuration will flash lines according to ETA, otherwise {@code false}
     * @since 5.7
     */
    public boolean isFlashEnabled()
    {
        return flashEnabled;
    }

    /**
     * @return {@code true} if this navigation configuration will color its lines according to ETA, otherwise {@code false}
     * @since 5.7
     */
    public boolean isColorEnabled()
    {
        return colorEnabled;
    }

    /**
     * @return {@code true} if the current target index should descend on arrival, or {@code false} if it should ascend
     * @since 5.7
     */
    public boolean isReversed()
    {
        return reversed;
    }

    /**
     * @param reversed {@code true} if the current target index should descend on arrival, or {@code false} if it should ascend
     * @since 5.7
     */
    public void setReversed(boolean reversed)
    {
        propertyChangeSupport.updateAndFire(REVERSED, this.reversed, reversed, () -> this.reversed = reversed);
    }

    /**
     * @return the index of the current navigation target
     * @since 5.7
     */
    public int getCurrentTargetIndex()
    {
        return currentTargetIndex;
    }

    /**
     * Increment the target index. Descends if {@link #isReversed()} is {@code true}, otherwise ascends.
     *
     * @since 5.7
     */
    @SuppressWarnings("UnusedReturnValue")
    public void moveToNextTarget()
    {
        var nextTarget = currentTargetIndex + (reversed ? -1 : 1);
        if (nextTarget > -1 && nextTarget < targets.size())
        {
            setCurrentTarget(nextTarget);
        }
    }

    /**
     * @param target the new navigation target, must be present in {@link #getTargets()}
     * @throws IllegalArgumentException if the given target is not present in {@link #getTargets()}
     * @since 5.7
     */
    public void setCurrentTarget(@Nonnull NavigationTarget target)
    {
        var newTarget = targets.indexOf(target);
        if (newTarget == -1) throw new IllegalArgumentException("Target " + target.getTarget().getName() + "  not found in navigation");
        setCurrentTarget(newTarget);
    }

    /**
     * @param targetIndex the new target index
     * @since 5.7
     */
    public void setCurrentTarget(int targetIndex)
    {
        var nextTarget = targets.get(targetIndex); // Validates index
        propertyChangeSupport.updateAndFire(CURRENT_TARGET, currentTargetIndex, targetIndex, () -> {
            getCurrentTarget().setVisible(false);
            currentTargetIndex = targetIndex;
            nextTarget.setVisible(true);
        });
    }

    /**
     * @return the current targets intercept eta, if present, or else the direct eta
     */
    public long getEtaSec()
    {
        return getCurrentTarget().getInterceptOrDirectEtaSec();

    }

    /**
     * @return the List of navigation targets
     * @since 5.7
     */
    @Nonnull
    public List<NavigationTarget> getTargets()
    {
        return List.copyOf(targets);
    }

    /**
     *
     * @return the List of navigation targets, reverse ordered if {@link #isReversed()} is {@code true}
     * @since 5.7
     */
    @Nonnull
    public List<NavigationTarget> getTargetsOrdered()
    {
        if (reversed)
        {
            var list = new ArrayList<>(targets);
            Collections.reverse(list);
            return list;
        } else
        {
            return getTargets();
        }
    }

    /**
     * @return all map objects that are owned by this navigation
     * @since 5.7
     */
    @Nonnull
    public List<IMapObject> getMapObjects()
    {
        return targets.stream().flatMap(t -> t.getMapObjects().stream()).toList();
    }

    /**
     * @param target the target map object
     * @return {@code true} if the source has arrived at the target
     * @since 5.8
     */
    protected abstract boolean arrivedAt(@Nonnull IMapObject target);

    /**
     * Recalculates ETAs for all navigation targets and checks to see if we've arrived at the current target.
     */
    void update()
    {
        // This check is necessary because sometimes this update is called prior to the navigation subclass having setup
        //  targets. It may be necessary in the future to refactor how we construct these navigation objects to better handle
        //  the case of having no targets, since currently the assumption is that post-construction there should always be
        //  at least 1.
        if (!targets.isEmpty())
        {
            if (arrivedAt(getCurrentTarget().getTarget())) moveToNextTarget();
            getCurrentTarget().update();
            if (flasher != null) flasher.update();
        }
    }
}
