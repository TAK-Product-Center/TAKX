package gov.tak.api.comms.takserver.mission.call;

import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.MissionApiAuthenticationException;
import gov.tak.api.comms.takserver.mission.data.Mission;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.ConnectException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Base implementation for endpoints, outlining what known parameters are available, what their default values are,
 * and the ability to construct a REST call via a builder pattern.
 * <p>
 * The design operates off of the assumption we are targeting consistently designed Feign API interfaces.
 * The endpoint definition format in these classes looks like this:
 * <pre>
 * {@code
 * @RequestLine("GET /path/{pathVar｝")
 * Foo getExample(@HeaderMap Map<String, Object> headers,
 *        @QueryMap Map<String, Object> queryMap,
 *        @Param("pathVar") String pathVar);
 * }
 * </pre>
 * The path variables come first, if any are present, and then the header and query maps are provided last.
 * Our constructor parameter {@link Endpoint} lambda should be able to direct to any endpoint outlined
 * by these api interfaces. To show an example, this would be an implementation for the example endpoint above:
 * <pre>
 * {@code
 * public class Example extends AbstractEndpointCaller<Foo> {
 *     private static final Map<String, ParameterInfo> PARAM_MAP = map(
 *             paramOptional("foo", false),
 *             paramOptional("fizz", "buzz"));
 *
 *     public Example(FeignApi api, String pathVar) {
 *         super((headers, params) -> api.getExample(headers, params, pathVar), PARAM_MAP);
 *     }
 *
 *     public Example withFoo(boolean foo) { return setParam("foo", foo); }
 *     public Example withFizz(String fizz) { return setParam("fizz", fizz); }
 *     // Foo get() is implemented by the abstract base
 * }
 * }
 * </pre>
 * Implementations only need to determine the {@code T} type, provide an {@link Endpoint} reference,
 * and provide information about parameters.
 * <p>
 * For endpoints that change over time, such as additional query parameters being added, the {@code Example} class
 * won't need to be adjusted so long as the new parameters are not required. Users can add these parameters themselves
 * if the class does not have explicit support for them via {@link #setParam(String, Object)}.
 *
 * @param <T> Endpoint return value type.
 *
 * @since 3.5
 */
public abstract class AbstractEndpointCaller<T> implements IConfigurableEndpointCaller<T>
{
    private final Map<String, Object> defaultParameterValues = new HashMap<>();
    private final Map<String, Object> parameterValues;
    private final Map<String, Object> headerValues = new HashMap<>();
    private final Set<String> requiredParameters = new HashSet<>();
    private final Map<String, ParameterInfo> parameters;
    private final Endpoint<T> endpoint;

    /**
     * Constructs the base of any feign endpoint call, with no query parameters.
     *
     * @param endpoint Endpoint reference to a Feign API method.
     */
    protected AbstractEndpointCaller(@Nonnull Endpoint<T> endpoint)
    {
        this(endpoint, Collections.emptyMap());
    }

    /**
     * Constructs the base of any Feign endpoint call.
     *
     * @param endpoint Endpoint reference to a Feign API method.
     * @param parameters Map of known parameters available for use on the endpoint.
     */
    protected AbstractEndpointCaller(@Nonnull Endpoint<T> endpoint, @Nonnull Map<String, ParameterInfo> parameters)
    {
        this.endpoint = endpoint;
        this.parameters = parameters;

        // Initialize default value and requirement maps
        parameters.forEach((name, parameter) -> {
            defaultParameterValues.put(name, parameter.defaultValue());
            if (parameter.required()) requiredParameters.add(name);
        });
        parameterValues = new HashMap<>(defaultParameterValues);
    }

    @Nullable
    @Override
    public T get() throws ApiException
    {
        // Endpoint delegates to feign API class calls, which throw the runtime exception.
        // We wrap and rethrow to force our API consumers to handle errors properly.
        try
        {
            return endpoint.accept(buildHeaderMap(), buildParameterMap());
        } catch (MissionApiAuthenticationException ex)
        {
            // Thrown when our session is expired.
            // Shouldn't happen as we attempt to re-auth a little bit before the window closes.
            throw new ApiException(ex);
        } catch (Throwable t)
        {
            // In some cases the cause actually is much more informative than the wrapper exception provided by feign.
            Throwable cause = t;
            if (cause.getCause() instanceof ConnectException connectException) cause = connectException;
            throw new ApiException(cause);
        }
    }

    @Nonnull
    @Override
    public CompletableFuture<T> asyncGet()
    {
        return CompletableFuture.supplyAsync(this::wrapAsync);
    }

    @Nonnull
    @Override
    public CompletableFuture<T> asyncGet(@Nonnull ExecutorService executorService)
    {
        return CompletableFuture.supplyAsync(this::wrapAsync, executorService);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends IConfigurableEndpointCaller<T>> S withAuthorizationTokenIfAvailable(@Nonnull IDataSyncManager dataSyncManager, @Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        Mission mission = dataSyncManager.getMission(connection, missionName);
        if (mission != null && mission.passwordProtected())
        {
            String token = dataSyncManager.getMissionSubscriptionToken(connection, missionName);
            if (token != null) setAuthorizationToken(token);
        }
        return (S) this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <S extends IConfigurableEndpointCaller<T>> S setHeader(@Nonnull String key, @Nullable Object value)
    {
        if (value == null) return unsetHeader(key);
        headerValues.put(key, value);
        return (S) this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <S extends IConfigurableEndpointCaller<T>> S unsetHeader(@Nonnull String key)
    {
        headerValues.remove(key);
        return (S) this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <S extends IConfigurableEndpointCaller<T>> S setParam(@Nonnull String name, @Nullable Object value)
    {
        if (value == null) return unsetParam(name);
        parameterValues.put(name, value);
        return (S) this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <S extends IConfigurableEndpointCaller<T>> S unsetParam(@Nonnull String name)
    {
        parameterValues.remove(name);
        return (S) this;
    }

    @Nonnull
    @Override
    public Map<String, ParameterInfo> getParameters()
    {
        return parameters;
    }

    /**
     * @return Map of filled in values for the endpoint's parameters.
     *
     * @since 5.0
     */
    @Nonnull
    protected Map<String, Object> getParameterValues()
    {
        return parameterValues;
    }

    /**
     * Wraps {@link #get()} for async calls, throwing a {@link CompletionException} when failures occur.
     *
     * @return Value of {@link #get()}.
     *
     * @throws CompletionException for any exception from the wrapped {@link #get()}.
     */
    private T wrapAsync() throws CompletionException
    {
        try
        {
            Thread.currentThread().setName("TAK Server endpoint call: " + getClass().getSimpleName());
            return get();
        } catch (Throwable t)
        {
            // NOTE: If you're seeing stack-traces originating from this method, and you cannot
            // see the async calling context, put a breakpoint on the 'asyncGet' methods.
            throw new CompletionException(t);
        }
    }

    /**
     * @return Map of headers to pass to Feign API methods <i>(via {@link Endpoint})</i>.
     * By default, this returns an empty map.
     */
    @Nonnull
    protected Map<String, Object> buildHeaderMap()
    {
        return headerValues;
    }

    /**
     * @return Map of query parameters to pass to Feign API methods <i>(via {@link Endpoint})</i>.
     * By default, this returns a map of any user-defined values plus any default values of required parameters.
     */
    @Nonnull
    protected Map<String, Object> buildParameterMap()
    {
        // Only include values that have been modified from the default, or are required.
        return parameterValues.entrySet().stream()
                .filter(e -> {
                    // Any required parameter must be included.
                    String paramName = e.getKey();
                    if (requiredParameters.contains(paramName)) return true;

                    // Parameters that are not the same as the defaults should be included.
                    return !Objects.equals(defaultParameterValues.get(paramName), e.getValue());
                }).collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    // If for some reason the parameters map is updated to include a null value,
                    // we should fall back to the value in the default parameters map.
                    String key = e.getKey();
                    Object value = e.getValue();
                    if (value == null) value = defaultParameterValues.get(key);

                    // If for some reason both values are null, something is wrong.
                    if (value == null) throw new IllegalStateException("Parameter '" + key + "' was assigned null");
                    return value;
                }));
    }

    /**
     * Helper for child types to create maps for the constructor call.
     *
     * @param parameters Variable array of parameters.
     *
     * @return Map representation of the array, with name keys.
     */
    @Nonnull
    protected static Map<String, ParameterInfo> map(ParameterInfo... parameters)
    {
        Map<String, ParameterInfo> map = new HashMap<>();
        for (ParameterInfo parameter : parameters)
        {
            map.put(parameter.name(), parameter);
        }
        return map;
    }

    /**
     * Helper for construction of parameters.
     *
     * @param name Query parameter name.
     * @param defaultValue Query parameter default value.
     *
     * @return Parameter info wrapper for a required parameter.
     */
    @Nonnull
    protected static ParameterInfo param(@Nonnull String name, @Nonnull Object defaultValue)
    {
        return new ParameterInfo(name, defaultValue, true);
    }

    /**
     * Helper for construction of parameters.
     *
     * @param name Query parameter name.
     * @param defaultValue Query parameter default value.
     *
     * @return Parameter info wrapper for an optional parameter.
     */
    @Nonnull
    protected static ParameterInfo paramOptional(@Nonnull String name, @Nonnull Object defaultValue)
    {
        return new ParameterInfo(name, defaultValue, false);
    }

    /**
     * Many TAK-Server endpoints wrap the results of API calls in singleton lists for some weird reason.
     * This allows for a simpler declaration of a {@link Endpoint} lambda, which can then be unpacked.
     *
     * @param endpoint Endpoint to transform into {@code Endpoint<T>} from {@code Endpoint<List<T>>}.
     * @param <T> List content type.
     *
     * @return Type transformed endpoint.
     */
    @Nonnull
    protected static <T> Endpoint<T> unpack(@Nonnull Endpoint<List<T>> endpoint)
    {
        return (headers, params) -> {
            List<T> values = endpoint.accept(headers, params);
            return values.isEmpty() ? null : values.get(0);
        };
    }

    /**
     * Allows the simplified declarations of {@link VoidEndpoint} over a void-typed {@link Endpoint} which would
     * require expanding the lambda to include {@code return null} always.
     *
     * @param endpoint Endpoint to transform into {@code Endpoint<Void>}.
     *
     * @return Type transformed endpoint.
     */
    @Nonnull
    protected static Endpoint<Void> devoid(@Nonnull VoidEndpoint endpoint)
    {
        // Null can be safely cast to any type.
        return (headers, params) -> (Void) endpoint.accept(headers, params);
    }

    /**
     * Functional interface for invoking Feign endpoints that do not yield responses.
     */
    @FunctionalInterface
    protected interface VoidEndpoint extends Endpoint<Void>
    {
        /**
         * @param headers Optional headers to add to the request.
         * @param params Map of query parameter names to their values.
         */
        void handle(@Nonnull Map<String, Object> headers, @Nonnull Map<String, Object> params);

        @Override
        default Void accept(@Nonnull Map<String, Object> headers, @Nonnull Map<String, Object> params)
        {
            handle(headers, params);
            return null;
        }
    }

    /**
     * Functional interface for invoking Feign endpoints.
     *
     * @param <T> Endpoint return value type.
     */
    @FunctionalInterface
    protected interface Endpoint<T>
    {
        /**
         * @param headers Optional headers to add to the request.
         * @param params Map of query parameter names to their values.
         *
         * @return Result of request.
         */
        T accept(@Nonnull Map<String, Object> headers, @Nonnull Map<String, Object> params);
    }
}
