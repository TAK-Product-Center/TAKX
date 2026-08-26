package gov.takx.core.globe.tak.animation;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.engine.map.MapRenderer;

/**
 * A service for controlling {@link IAnimation animations} on the globe.
 *
 * @since 3.4.1
 */
@IncubatingApi(since = "3.4.1")
public interface IAnimationService
{
    /**
     * Queue an {@link IAnimation} for animating the map globe.
     * @param mapRenderer the map renderer
     * @param animation the animation
     */
    void addAnimation(MapRenderer<?> mapRenderer, IAnimation animation);

    /**
     * Return the topmost {@link IAnimation} without removing it from the queue.
     *
     * @param mapRenderer the map renderer
     * @return the animation
     */
    IAnimation peekAnimation(MapRenderer<?> mapRenderer);

    /**
     * Return the topmost {@link IAnimation} and remove it from the queue.
     *
     * @param mapRenderer the map renderer
     * @return the animation
     */
    IAnimation pollAnimation(MapRenderer<?> mapRenderer);

    /**
     * Clear all animations from the queue.
     * @param mapRenderer the map renderer
     */
    void stopAllAnimations(MapRenderer<?> mapRenderer);
}
