package gov.tak.api.comms.commo;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.cot.event.CotEvent;
import jakarta.annotation.Nonnull;

/**
 * Contract for being offered {@link CotEvent}s immediately after they are received, before
 * they are processed, and immediately before they are sent out, after all detail handlers have been applied.
 * <p/>
 * This allows a plugin to make any adjustments to the event prior to ingestion or dissemination of the event.
 *
 * @see ICommonCommoManager#addCotEventInterceptor(ICotEventInterceptor)
 * @see ICommonCommoManager#removeCotEventInterceptor(ICotEventInterceptor)
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public interface ICotEventInterceptor
{
    /**
     * Called when a {@link CotEvent} has been received via Commo, before it is queued for internal processing.
     * Recipients can make any changes needed to the content of the event.
     */
    default void onReceive(@Nonnull CotEvent event)
    {
    }

    /**
     * Called immediately before a CoT event message is to be sent out via Commo.
     * Recipients can make any changes needed to the content of the event.
     */
    default void onSend(@Nonnull CotEvent event)
    {
    }
}
