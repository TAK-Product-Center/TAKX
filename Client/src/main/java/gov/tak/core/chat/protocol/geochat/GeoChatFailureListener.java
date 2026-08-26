package gov.tak.core.chat.protocol.geochat;

import com.atakmap.commoncommo.CoTSendFailureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Listens for CoT-sending failures and logs them accordingly.
 *
 * @since 3.0
 */
public class GeoChatFailureListener implements CoTSendFailureListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void sendCoTFailure(String host, int port, String errorReason)
    {
        logger.error("Unable to send CoT message to {}:{} for reason: {}", host, port, errorReason);
    }
}
