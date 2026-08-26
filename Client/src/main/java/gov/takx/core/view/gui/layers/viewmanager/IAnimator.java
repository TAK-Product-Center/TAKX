package gov.takx.core.view.gui.layers.viewmanager;

/**
 * An interface to facilitate animation of some object being used on screen.  The object could be a full image, or just
 * a piece of data used to draw an object, such as a Color.  The idea is that whenever 'next()' is called the
 * appropriate data for the current time will be returned.
 *
 */
public interface IAnimator<T>
{
    /**
     * Start the animator.
     */
    void start();

    /**
     * Stop the animator.
     */
    void stop();

    /**
     * Get the next animated object from the animator.
     *
     * @return the next animated object.
     */
    T next();

    /**
     * Determine whether the animator has more data to return.
     *
     * @return {@code true} if the animator has more data to return, or {@code false} otherwise.
     */
    boolean hasNext();

    /**
     * Determine whether the animator has ever been started.
     *
     * @return {@code true} if the animator has ever been started, or {@code false} otherwise.
     */
    boolean hasStarted();
}
