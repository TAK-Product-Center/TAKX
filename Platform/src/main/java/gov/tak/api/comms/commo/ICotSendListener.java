package gov.tak.api.comms.commo;

/**
 * Contract for listeners to receive notification of CoT messages being sent out via Common Commo.
 *
 * @since 5.3
 */
public interface ICotSendListener
{
    /**
     * A CoT message has been sent.
     *
     * @param cotMessage CoT message in XML form
     */
    void cotMessageSent(String cotMessage);
}
