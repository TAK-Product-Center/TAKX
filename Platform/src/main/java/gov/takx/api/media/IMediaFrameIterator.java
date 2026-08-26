package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

import java.util.Iterator;

/**
 * Allows iteration of {@link IMediaFrame}s found within and {@link IMedia}.
 * <p>
 * Users MUST call {@link IMediaFrameIterator#close()} when done with the iterator in order to release associated resources.
 * It is suggested to use try-with-resources when using the iterator, for example:
 * <pre>
 * {@code
 *     String videoUrl = ...
 *     IMediaManager manager = ...
 *     IMedia media = manager.createMedia(videoUrl);
 *
 *     try(IMediaFrameIterator it = manager.createMediaFrameIterator(media))
 *     {
 *         while(it.hasNext())
 *         {
 *             IMediaFrame frame = it.next();
 *             ...
 *         }
 *     }
 *     catch(Exception e)
 *     {
 *          ...
 *     }}
 * </pre>
 *
 * @since 2.8
 */
@Experimental
public interface IMediaFrameIterator extends Iterator<IMediaFrame>, AutoCloseable
{

}
