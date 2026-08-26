package gov.takx.api.structures;

import gov.takx.api.platform.IProgressCallback;

/**
 * The SurfaceStructureCallback can be used to get feedback during the structure file loading process. The file load
 * process is asynchronous in nature, so it is advisable that any API invocation implement this interface in order
 * to know when the load is complete and structures are available.
 * <p/>
 * This interface is optional, and if it is not used a {@code null} can be passed in instead. Note that this interface has no
 * reachback into the actual load process, so if you need to know what file this is referring to, it is incumbent upon
 * the implementation to remember this.
 *
 */
public interface ISurfaceStructureCallback extends IProgressCallback
{
    /**
     * onCountComplete will be called after the file has been interrogated and the structures discovered have been
     * counted.
     *
     * @param count
     */
    void onCountComplete(int count);

    /**
     * onLoadComplete will be called at the very end of the load process. Note that the count may in fact differ from
     * the count found in the file if any of the structures are not able to be loaded for some reason.
     *
     * @param count
     */
    void onLoadComplete(int count);
}
