package gov.takx.bootstrap;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory to provide {@link URLStreamHandler} instances for a given protocol string.
 * This class is discovered via the normal {@link java.util.ServiceLoader} model, and is
 * declared in {@code META-INF/services/java.net.URLStreamHandlerFactory}.
 * <p/>
 * This class is also a registry to manage said handlers per protocol name. Handlers are registered from within
 * the main application code.  This class must be in the Bootstrap module, since that is the only module known
 * to JBoss modules when launching the application.  JBoss modules has installed its own factory using
 * {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)}, which precludes us from registering one, and we
 * are forced to tie into that factory using this service provider model.
 *
 * @since 5.1
 */
public class ApplicationUrlStreamHandlerFactory implements URLStreamHandlerFactory
{
    private static final Map<String, URLStreamHandler> HANDLERS = new HashMap<>(); // Map from protocol to handler

    /**
     * Register a stream handler for the given protocol.
     *
     * @param protocol Protocol to handle
     * @param handler Handler to return from {@link #createURLStreamHandler} when called with the given {@code protocol}.
     */
    public static void registerStreamHandler(String protocol, URLStreamHandler handler)
    {
        HANDLERS.put(protocol, handler);
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol)
    {
        return HANDLERS.get(protocol);
    }
}
