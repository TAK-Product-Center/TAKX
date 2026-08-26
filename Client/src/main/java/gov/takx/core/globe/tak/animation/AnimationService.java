package gov.takx.core.globe.tak.animation;

import gov.tak.api.engine.map.MapRenderer;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * An implementation of {@link IAnimationService} for TAK globe.
 *
 * @since 3.4.1
 */
@ApplicationScoped
public class AnimationService implements IAnimationService
{
    private final IdentityHashMap<MapRenderer<?>, Queue<IAnimation>> animationQueueMap = new IdentityHashMap<>();

    @Override
    public synchronized void addAnimation(MapRenderer<?> mapRenderer, IAnimation animation)
    {
        getAnimationQueue(mapRenderer).add(animation);
    }

    @Override
    public synchronized IAnimation peekAnimation(MapRenderer<?> mapRenderer)
    {
        return getAnimationQueue(mapRenderer).peek();
    }

    @Override
    public synchronized IAnimation pollAnimation(MapRenderer<?> mapRenderer)
    {
        return getAnimationQueue(mapRenderer).poll();
    }

    @Override
    public synchronized void stopAllAnimations(MapRenderer<?> mapRenderer)
    {
        getAnimationQueue(mapRenderer).clear();
    }

    private Queue<IAnimation> getAnimationQueue(MapRenderer<?> mapRenderer)
    {
        return animationQueueMap.computeIfAbsent(mapRenderer, m -> new LinkedList<>());
    }
}
