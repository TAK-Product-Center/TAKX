package gov.takx.api.playback;

/**
 * Certain classes within the playback framework hold on to resources that should be properly released as things
 * are removed.
 *
 * @since 1.47
 */
public interface IDisposable
{
    /**
     * This object is about to be destroyed.  Dispose any resources (that won't automatically be released by the
     * garbage collector) associated with this object.
     */
    void disposeResources();
}
