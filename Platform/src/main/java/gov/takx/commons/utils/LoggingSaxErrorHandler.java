package gov.takx.commons.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.lang.invoke.MethodHandles;

/**
 * Special error handler to report using our logger instead of the default behavior, which writes to {@link System#err}.
 *
 * @since 5.1
 */
public class LoggingSaxErrorHandler implements ErrorHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void warning(SAXParseException exception)
    {
        logger.warn(makeMessage(exception));
    }

    @Override
    public void error(SAXParseException exception)
    {
        logger.error(makeMessage(exception));
    }

    @Override
    public void fatalError(SAXParseException exception)
    {
        logger.error("[FATAL] {}", makeMessage(exception));
    }

    private String makeMessage(SAXParseException exception)
    {
        return exception.getLineNumber() + ":" + exception.getColumnNumber() + ": " + exception.getMessage();
    }
}
