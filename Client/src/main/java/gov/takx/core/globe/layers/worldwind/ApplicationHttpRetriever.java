package gov.takx.core.globe.layers.worldwind;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.retrieve.HTTPRetriever;
import gov.nasa.worldwind.retrieve.JarRetriever;
import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.URLRetriever;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

/**
 * @since 1.43 (renamed from RaptorHTTPRetriever in 3.4)
 */
public class ApplicationHttpRetriever extends HTTPRetriever
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ApplicationHttpRetriever(@Nonnull URL url, @Nullable RetrievalPostProcessor postProcessor)
    {
        super(url, postProcessor);
    }

    @Nonnull
    public static URLRetriever createRetriever(@Nonnull URL url, @Nullable RetrievalPostProcessor postProcessor)
    {
        String protocol = url.getProtocol();

        if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))
        {
            return new ApplicationHttpRetriever(url, postProcessor);
        } else if ("jar".equalsIgnoreCase(protocol))
        {
            return new JarRetriever(url, postProcessor);
        } else
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }

    @Override
    protected URLConnection openConnection() throws IOException
    {
        super.openConnection();

        // Update this connection with authentication data (if any exists)
        if (connection instanceof HttpURLConnection httpURLConnection)
        {
            HTTPPasswordManager.getInstance().configureHTTPAuthorization(httpURLConnection);
        }

        return connection;
    }

    /**
     * Overridden so we can mark a host as unavailable when we get a connection timeout.
     * {@inheritDoc}
     * @since 3.4
     */
    @Override
    protected ByteBuffer read() throws Exception
    {
        try
        {
            return super.read();
        } catch (SocketTimeoutException e)
        {
            WorldWind.getNetworkStatus().logUnavailableHost(url);
            throw e;
        }
    }

    /**
     * Overridden so we can mark a connection as needing authentication if we receive an {@code SC_UNAUTHORIZED} status code.
     * {@inheritDoc}
     * @since 3.4
     */
    @Override
    protected ByteBuffer doRead(URLConnection connection) throws Exception
    {
        final ByteBuffer buffer = super.doRead(connection);

        final int responseCode = getResponseCode();
        if (responseCode != HttpStatus.SC_OK) logger.warn("Got {} on retrieval attempt", responseCode);
        if (responseCode == HttpStatus.SC_UNAUTHORIZED) HTTPPasswordManager.getInstance().setNeedsAuthentication(connection);

        return buffer;
    }
}
