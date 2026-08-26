package gov.tak.api.comms.takserver.mission;

import com.atakmap.net.CertificateManager;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Client;
import feign.Feign;
import feign.RequestTemplate;
import feign.Response;
import feign.Util;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.security.ISslContextProvider;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.Session;
import gov.tak.api.comms.takserver.mission.feign.FeignContactManagerApi;
import gov.tak.api.comms.takserver.mission.feign.FeignCotApi;
import gov.tak.api.comms.takserver.mission.feign.FeignGroupsApi;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import gov.tak.api.comms.takserver.mission.feign.FeignSessionApi;
import gov.tak.api.comms.takserver.mission.feign.FeignSyncApi;
import gov.tak.api.comms.takserver.mission.feign.FeignTracksApi;
import gov.tak.api.comms.takserver.mission.feign.FeignVideoApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Client for interacting with the multiple offered API's from TAK Server.
 *
 * @since 3.5
 */
@InjectableService
@ApplicationScoped
public class ApiProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String API_SCHEME_SECURE = "https";
    private static final String API_SCHEME_UNSECURE = "http";
    private static final String API_PREFIX = "/Marti/api/";
    private static final String HEADER_API_VERSION = "API_VERSION";
    private static final String HEADER_API_VERSION_TARGET = "3";
    private static final String HEADER_COOKIE = "Cookie";

    private final Map<String, SessionHandler> sessionHandlers = new ConcurrentHashMap<>();
    private final Map<String, FeignMissionApi> missionApiClients = new ConcurrentHashMap<>();
    private final Map<String, FeignCotApi> cotApiClients = new ConcurrentHashMap<>();
    private final Map<String, FeignGroupsApi> groupApiClients = new ConcurrentHashMap<>();
    private final Map<String, FeignSyncApi> syncApiClients = new ConcurrentHashMap<>();
    private final Map<String, FeignContactManagerApi> contactManagerApiClients = new ConcurrentHashMap<>();
    private final Map<String, FeignVideoApi> videoApiClients = new ConcurrentHashMap<>();
    private final Map<String, FeignTracksApi> tracksApiClients = new ConcurrentHashMap<>();
    private final ObjectMapper standardObjectMapper = new ObjectMapper();
    private final ObjectMapper unwrappingObjectMapper = new UnwrappingObjectMapper();
    private final ISslContextProvider sslContextProvider;

    @Inject
    ApiProvider(@Nonnull ISslContextProvider sslContextProvider)
    {
        this.sslContextProvider = sslContextProvider;
    }

    /**
     * Get or create the API wrapper for the mission API located at the target uri.
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     *
     * @return The {@link FeignMissionApi} client associated with the uri.
     */
    @Nonnull
    public FeignMissionApi getMissionApi(@Nonnull ITakServerConnection connection)
    {
        return missionApiClients.computeIfAbsent(key(connection),
                key -> createAuthorizedApiClient(connection, FeignMissionApi.class));
    }

    /**
     * Get or create the API wrapper for the CoT API located at the target uri.
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     *
     * @return The {@link FeignCotApi} client associated with the uri.
     *
     * @since 5.0
     */
    @Nonnull
    public FeignCotApi getCotApi(@Nonnull ITakServerConnection connection)
    {
        return cotApiClients.computeIfAbsent(key(connection),
                key -> createAuthorizedApiClient(connection, FeignCotApi.class));
    }

    /**
     * Get or create the API wrapper for the Groups API located at the target uri.
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     *
     * @return The {@link FeignGroupsApi} client associated with the uri.
     *
     * @since 5.0
     */
    @Nonnull
    public FeignGroupsApi getGroupsApi(@Nonnull ITakServerConnection connection)
    {
        return groupApiClients.computeIfAbsent(key(connection),
                key -> createAuthorizedApiClient(connection, FeignGroupsApi.class));
    }

    /**
     * Get or create the API wrapper for the sync API located at the target uri.
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     *
     * @return The {@link FeignSyncApi} client associated with the uri.
     */
    @Nonnull
    public FeignSyncApi getSyncApi(@Nonnull ITakServerConnection connection)
    {
        return syncApiClients.computeIfAbsent(key(connection),
                key -> createAuthorizedApiClient(connection, "/Marti/", FeignSyncApi.class));
    }

    /**
     * Get or create the API wrapper for the contact-manager API located at the target uri.
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     *
     * @return The {@link FeignContactManagerApi} client associated with the uri.
     */
    @Nonnull
    public FeignContactManagerApi getContactManagerApi(@Nonnull ITakServerConnection connection)
    {
        return contactManagerApiClients.computeIfAbsent(key(connection),
                key -> createAuthorizedApiClient(connection, FeignContactManagerApi.class));
    }

    /**
     * Get or create the API wrapper for the Video Feeds API located at the target uri.
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     *
     * @return The {@link FeignVideoApi} client associated with the uri.
     * @since 5.0
     */
    @Nonnull
    public FeignVideoApi getVideoApi(@Nonnull ITakServerConnection connection)
    {
        return videoApiClients.computeIfAbsent(key(connection),
                key -> createAuthorizedApiClient(connection, FeignVideoApi.class));
    }

    /**
     * Get or create the API wrapper for the Tracks API located at the target uri.
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     *
     * @return The {@link FeignVideoApi} client associated with the uri.
     * @since 5.4
     */
    public FeignTracksApi getTracksApi(@Nonnull ITakServerConnection connection)
    {
        return tracksApiClients.computeIfAbsent(key(connection),
                key -> createAuthorizedApiClient(connection, "/Marti/", FeignTracksApi.class));
    }

    /**
     * Get or create the session handler for the API located at the target uri.
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     *
     * @return The {@link SessionHandler} client associated with the uri.
     */
    @Nonnull
    public SessionHandler getSessionHandler(@Nonnull ITakServerConnection connection)
    {
        return sessionHandlers.computeIfAbsent(key(connection), key -> {
            // The OAuth login API is not auth-locked behind the secure port.
            // It uses the same port for cert enrollment oddly enough.
            String host = toHostString(connection.getUri(), PortUse.SETUP);

            // In some cases a TAK Server connection may be configured to require custom certificates
            // to communicate to the OAuth login API. In such cases we need to provide our extended client
            // to feign when creating the session API instance.
            SslClient client = newClientForConnection(connection);

            // Construct the session API and wrap it in our session handler.
            FeignSessionApi sessionApi = Feign.builder()
                    .client(client)
                    .encoder(new JacksonEncoder(standardObjectMapper))
                    .decoder(new JacksonDecoder(standardObjectMapper))
                    .target(FeignSessionApi.class, host);
            return new SessionHandler(connection.getConnectionConfig(), sessionApi);
        });
    }

    /**
     * Allows for the creation of authorization-backed clients for a given feign API interface.
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     * @param apiClass Target feign API interface class to create a client for.
     * @param <T> Feign API type.
     *
     * @return A new client for interacting with the provided feign API class.
     */
    @Nonnull
    public <T> T createAuthorizedApiClient(@Nonnull ITakServerConnection connection,
                                           @Nonnull Class<T> apiClass)
    {
        return createAuthorizedApiClient(connection, API_PREFIX, apiClass);
    }

    /**
     * Allows for the creation of authorization-backed clients for a given feign API interface.
     * <p/>
     * An <i>"authorized client"</i> in this context is just one that ensures:
     * <ul>
     *     <li>A valid session token is maintained and added to all TAK Server endpoint requests</li>
     *     <li>SSL is handled appropriately based on the server config's attached certificates <i>(or lack thereof)</i></li>
     * </ul>
     *
     * @param connection Connection instance, to look up an instance based on
     * the target {@link ITakServerConnection#getUri() uri}.
     * @param apiPrefix API path prefix to append to {@link ITakServerConnection#getUri() uri},
     * and before endpoint paths declared in the API class.
     * @param apiClass Target feign API interface class to create a client for.
     * @param <T> Feign API type.
     *
     * @return A new client for interacting with the provided feign API class.
     */
    @Nonnull
    public <T> T createAuthorizedApiClient(@Nonnull ITakServerConnection connection,
                                           @Nonnull String apiPrefix, @Nonnull Class<T> apiClass)
    {
        // Construct a client handling SSL appropriately for the given connection.
        SslClient client = newClientForConnection(connection);

        // Transform the connection URI into the appropriate REST host.
        String host = toHostString(connection.getUri(), client.hasSocketFactory() ? PortUse.API_SECURE : PortUse.API_UNSECURE);

        // Create builder that fills in auth headers with our desired form of authentication.
        return Feign.builder()
                .client(client)
                .requestInterceptor(template -> fillHeaders(connection, template))
                .encoder(new JsonAndByteArrayEncoder(unwrappingObjectMapper))
                .decoder(new JacksonDecoder(unwrappingObjectMapper))
                .target(apiClass, host + apiPrefix);
    }

    /**
     * @param connection Connection to construct a feign client for.
     * @return A client to handle SSL enabled communication <i>(if certs are provided in
     * the connection config)</i> for the connection.
     * @since 5.2
     */
    @Nonnull
    private SslClient newClientForConnection(@Nonnull ITakServerConnection connection)
    {
        SslClient client;
        StreamingConnectionConfiguration config = connection.getConnectionConfig();
        if (sslContextProvider.hasRequiredCertificates(config))
        {
            // When the connection config has certificates associated with it, we'll want to use it in our client.
            // It is common that these certificates are self-signed and otherwise not trusted by public authorities,
            // so our provided SSL context will allow them to be utilized.
            SSLContext sslContext = sslContextProvider.getSslContext(config);
            SSLSocketFactory socketFactory = Objects.requireNonNull(sslContext,
                    "Missing SSL context for: " + config.getNetConnectString()).getSocketFactory();
            client = new SslClient(socketFactory);
        } else
        {
            // No certs in the config, we can fall back to default SSL handling.
            client = new SslClient(null);
        }
        return client;
    }

    /**
     * Fills the request template with required headers.
     *
     * @param config Config to use as a key when looking up session details.
     * @param template Request template to populate headers of.
     */
    private void fillHeaders(@Nonnull ITakServerConnection config, @Nonnull RequestTemplate template)
    {
        // Set current version of the API. Some responses change format if you change this value, or do not include it.
        template.header(HEADER_API_VERSION, HEADER_API_VERSION_TARGET);

        // If the config specifies a user/password, it should use a session-handler to adopt that into a session/token.
        // Not all servers lock API usage behind account access, so if there is no user/pass we skip this step.
        String username = config.getConnectionConfig().getUsername();
        if (!StringUtils.isEmpty(username))
        {
            // You need to pass along the session info through the cookie header, where values are separated by ';'
            Session session = getSessionHandler(config).getSession();
            template.header(HEADER_COOKIE, "JSESSIONID=" + session.jti() + "; access_token=" + session.accessToken());
        }
    }

    /**
     * @param uri Input URI.
     * @param portUse Kind of intended port use.
     *
     * @return Host name string.
     */
    @Nonnull
    private String toHostString(@Nonnull URI uri, @Nonnull PortUse portUse)
    {
        try
        {
            // Different ports/scheme get used for different use cases.
            int port = switch (portUse)
            {
                case SETUP -> PreferenceEntry.COMMON_COMMO_TAK_SERVER_CERT_ENROLLMENT_API_PORT.getIntegerValue();
                case API_SECURE -> PreferenceEntry.COMMON_COMMO_TAK_SERVER_SECURE_API_PORT.getIntegerValue();
                case API_UNSECURE -> PreferenceEntry.COMMON_COMMO_TAK_SERVER_UNSECURE_API_PORT.getIntegerValue();
            };
            String scheme = switch (portUse)
            {
                case SETUP, API_SECURE -> API_SCHEME_SECURE;
                case API_UNSECURE -> API_SCHEME_UNSECURE;
            };

            // Feign only supports 'http(s)' schemes & the ports used by the config need to be mapped
            // to the appropriate ones for API usage. The URIs coming in originate from the TAK connection config
            // which targets different ports than what we're dealing with here.
            return new URIBuilder(uri)
                    .setPort(port)
                    .setScheme(scheme)
                    .build().toString();
        } catch (URISyntaxException ex)
        {
            // Should never happen, given the input URI is valid.
            throw new IllegalStateException("Swapping port and uri scheme yielded invalid URI", ex);
        }
    }

    /**
     * @param connection Connection to generate a key for.
     * @return Lookup key for the given connection.
     * @since 5.1
     */
    @Nonnull
    private static String key(@Nonnull ITakServerConnection connection)
    {
        // A connection should consider not only the URL of the remote server, but also the user
        // when creating a unique key. Some real-world use includes multiple people sharing the same
        // system and switching sign-ins. Ensuring data is unique per server/user combo allows for
        // such cases.
        var user = Objects.requireNonNullElse(connection.getConnectionConfig().getUsername(), "default");
        return connection.getUri() + ":" + user;
    }

    /**
     * An extension to the default JSON encoder, which encodes data in both cases where we want to use JSON and where we do not.
     * This can be differentiated by when the content-type of a body is indicative of binary data.
     * <br>
     * We do not have such an implementation for decoding since binary downloads circumvent the decoding stage entirely.
     */
    private static class JsonAndByteArrayEncoder extends JacksonEncoder
    {
        private JsonAndByteArrayEncoder(@Nonnull ObjectMapper mapper)
        {
            super(mapper);
        }

        @Override
        public void encode(Object object, Type bodyType, RequestTemplate template)
        {
            // We want to ensure binary data is written as-is when specified.
            //
            // IMPORTANT: The 'template' does not have contents outside of what is
            // explicitly annotated on the REST interface methods. If you are passing
            // headers via the "@HeadersMap Map" parameter, those values are NOT VISIBLE
            // here at all. Thus, any endpoint MUST set the 'Content-Type' via annotations.
            //
            // Data models do not have a body type of "byte[]" and thus will be mapped
            // according to their Jackson configuration (below).
            if (bodyType == byte[].class && template.headers().containsKey("Content-Type"))
            {
                template.body((byte[]) object, Util.UTF_8);
                return;
            }

            // Fall back to making everything JSON.
            super.encode(object, bodyType, template);
        }
    }

    /**
     * Extension of {@link ObjectMapper} to ignore unknown fields, and unbox wrapped results
     * from the Mission API <i>(As most endpoints wrap results to include API version information)</i>.
     * <p>
     * Most endpoints wrap data types in the given model:
     * <table>
     *    <tr><th><b>Key</b></th><th>Value</th></tr>
     *    <tr><td><b>version</b></td><td>api version of server</td></tr>
     *    <tr><td><b>type</b></td><td>class name on server</td></tr>
     *    <tr><td><b>node-id</b></td><td>server id</td></tr>
     *    <tr><td><b>data</b></td><td><u>The actual stuff we want</u></td></tr>
     * </table>
     * We want to extract and deserialize the {@code data} entry.
     * Typically, you'd use {@link DeserializationFeature#UNWRAP_ROOT_VALUE} and use
     * {@link JsonRootName} or {@link #setAnnotationIntrospector(AnnotationIntrospector) set the annotation inspector}
     * to handle extraction.
     * <p>
     * But consider if you want to unpack a {@link List} of some type.
     * The internals of {@link ObjectMapper} are not written to support this case.
     * As {@link List} is not annotated with {@link JsonRootName} it fails on that approach,
     * and the custom inspector falls to the same trap. Even if your inspector checks {@link JavaType#getContentType()}
     * of arrays/collections, the internal handling will operate on the top-level type, and there is no way to override
     * the behavior where it does that due to the design.
     * <p>
     * As such we will deserialize the model to {@link Intermediate} which stores the {@code data} element as a
     * raw string, and then pass along the raw content of {@link Intermediate#data()} for deserialization.
     * This is a less than stellar solution, but it works.
     */
    private static class UnwrappingObjectMapper extends ObjectMapper
    {
        private UnwrappingObjectMapper()
        {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            registerModule(new JavaTimeModule());

            // Override certain features to ensure instants are serialized as longs (of their epoch millis equivalent)
            enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        }

        /**
         * This acts as a hook before any deserialization occurs, allowing us to add our hooks to handle extraction of
         * the {@code data} element, and handle that content instead of the root.
         *
         * @param parser Parser operating on the literal {@link Response.Body} content.
         * @param type Target type to deserialize to.
         *
         * @return Deserialized instance.
         *
         * @throws IOException When super-type deserialization handling fails.
         */
        @Override
        protected Object _readMapAndClose(@Nonnull JsonParser parser, @Nonnull JavaType type) throws IOException
        {
            // Since the results may not be wrapped in the intermediate format (seen below) we need to be able to
            // re-read the source of the json. By default, read operations close the parser source.
            // Thus, we read the whole input here so if we need to reparse the json we can.
            String json;
            if (parser.getInputSource() instanceof BufferedReader reader)
            {
                json = reader.lines().collect(Collectors.joining());
            } else
            {
                // In practice, we only ever see 'BufferedReader' from usage in feign.
                throw new IOException("Unsupported json input source");
            }

            // Extract the JSON of the 'data' section then pass it along to the super implementation
            // of this method as a new parser.
            Intermediate intermediate = readValue(createParser(json), Intermediate.class);
            if (intermediate.data != null && !intermediate.data.isBlank())
            {
                try (JsonParser newParser = createParser(intermediate.data()))
                {
                    return super._readMapAndClose(newParser, type);
                }
            }

            // Handle as standard
            return super._readMapAndClose(createParser(json), type);
        }

        /**
         * Dummy model to unpack TAK Server's endpoint results which are packaged in a wrapper format.
         *
         * @param data Field to house the literal JSON of the {@code data} element's contents.
         *
         * @see UnwrappingObjectMapper More info on why this exists.
         * @see KeepAsJsonDeserializer Custom serializer for the {@code data} field to enable this use case.
         */
        private record Intermediate(@JsonDeserialize(using = KeepAsJsonDeserializer.class) String data)
        {
        }

        /**
         * Deserializer that allows annotated fields to store the content's of their respective JSON tree element
         * as the literal JSON literal text of their contents, rather than a mapped model of the tree structure.
         * <p>
         * <b>Example:</b>
         * <pre>
         * {@code
         * {
         *     "data": [ "one", "two", "three" ]
         * }}
         * </pre>
         * The {@link Intermediate} class, which annotated its {@code data} field with this
         * {@link KeepAsJsonDeserializer} will store {@code data} as a {@link String} with
         * the contents of {@code "[ "one", "two", "three" ]"} rather than attempting to
         * deserialize the model into a {@code List<String>}.
         *
         * @see Intermediate Example usage.
         */
        private static class KeepAsJsonDeserializer extends JsonDeserializer<String>
        {
            @Override
            public String deserialize(@Nonnull JsonParser parser, @Nonnull DeserializationContext context) throws IOException
            {
                return parser.getCodec().readTree(parser).toString();
            }
        }
    }

    /**
     * Simple management of API sessions, ensuring the access token is always up-to-date when
     * interacting with the mission API.
     */
    public static class SessionHandler
    {
        private final StreamingConnectionConfiguration config;
        private final FeignSessionApi sessionApi;
        private Session session;

        private SessionHandler(@Nonnull StreamingConnectionConfiguration config,
                               @Nonnull FeignSessionApi sessionApi)
        {
            this.sessionApi = sessionApi;
            this.config = config;
        }

        /**
         * Gets the current session details, and if they are missing our expired, requests an up-to-date session.
         *
         * @return Current session details.
         *
         * @throws MissionApiAuthenticationException When an up-to-date session cannot be acquired.
         */
        @Nonnull
        public synchronized Session getSession()
        {
            // NOTE: If at any point the authenticated session becomes invalid or expires,
            // calling mission API endpoints will give you back some HTML denoting a failure with
            // some wonky headers that make HttpClient throw exceptions.
            //    <?xml version="1.0" encoding="UTF-8"?>
            //    <event version="2.0" uid=... type="t-x-takp-v" time=... start=... stale=... how="m-g"> .. </event>
            // It fails to understand if this is supposed to be content, or a malformed redirect page and errors out.
            if (session == null || session.isExpired())
            {
                String username = config.getUsername();
                try
                {
                    session = sessionApi.login(username, config.getPassword());
                    logger.debug("Logged in as {} to {}", username, config.getUri().getHost());
                } catch (Throwable t)
                {
                    Throwable redactedCause = ExceptionUtils.redactExceptionMessage(msg -> msg.indexOf("password="), t);
                    throw new MissionApiAuthenticationException("Failed to log in as " + username, redactedCause);
                }
            }
            return session;
        }
    }

    /**
     * Client backed by {@link HttpsURLConnection} configured to ignore hostname validation.
     * <p/>
     * This is intentional, as the only portion of TAK Server's communication that is validated is
     * certificate enrollment. Once we have the certificate from that process all further validation ceases.
     *
     * @since 5.1
     */
    private static class SslClient extends Client.Default
    {
        private final boolean hasSocketFactory;

        /**
         * @param socketFactory SSL socket factory to use for this client.
         *                      Generally you will need to provide a value for dealing with self-signed certificates.
         *                      For publicly acknowledged certs recognized by the JVM, {@code null} can be passed.
         * @since 5.2
         */
        public SslClient(@Nullable SSLSocketFactory socketFactory)
        {
            super(socketFactory, CertificateManager.ALL_TRUSTING_HOSTNAME_VERIFIER);

            hasSocketFactory = socketFactory != null;
        }

        /**
         * @return {@code true} when this client has specified a custom {@link SSLSocketFactory}.
         * {@code false} when it has no custom factory, implying default SSL handling will be used.
         * @since 5.2
         */
        public boolean hasSocketFactory()
        {
            return hasSocketFactory;
        }
    }

    /**
     * General port use-cases for API requests.
     * @see #toHostString(URI, PortUse)
     * @since 5.8
     */
    private enum PortUse
    {
        SETUP, API_SECURE, API_UNSECURE
    }
}
