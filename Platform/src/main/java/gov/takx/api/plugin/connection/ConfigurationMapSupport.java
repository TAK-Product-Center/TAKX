package gov.takx.api.plugin.connection;

import com.google.common.collect.ImmutableMap;
import gov.takx.api.annotation.InjectableService;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Support methods for working with a connection's configuration map.  Primarily for converting {@code String}
 * values to required numeric types.
 *
 * @since 2.20
 */
@InjectableService
@ApplicationScoped
public class ConfigurationMapSupport
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Map from supported value types to their respective 'parseXXX' method
    private static final Map<Class, Function<String, Object>> TYPE_TO_PARSE_METHOD = ImmutableMap.<Class, Function<String, Object>>builder()
            .put(String.class, s -> s)
            .put(Boolean.class, Boolean::parseBoolean)
            .put(Short.class, Short::parseShort)
            .put(Integer.class, Integer::parseInt)
            .put(Long.class, Long::parseLong)
            .put(Float.class, Float::parseFloat)
            .put(Double.class, Double::parseDouble)
            .build();

    // Map from supported numeric types to their respective coercion method
    private static final Map<Class, Function<Number, Object>> TYPE_COERCION_METHOD = ImmutableMap.<Class, Function<Number, Object>>builder()
            .put(Short.class, Number::shortValue)
            .put(Integer.class, Number::intValue)
            .put(Long.class, Number::longValue)
            .put(Float.class, Number::floatValue)
            .put(Double.class, Number::doubleValue)
            .build();

    /**
     * Normalize the contents of a given configuration map according to the map of {@code types}.  Each key in
     * {@code configuration} is looked up in {@code types} to determine the desired type of the property.
     * If there is no entry in {@code types} for a given property key, then a type of {@code String} is assumed.
     * <p>
     * Only {@code String}, {@code Boolean}, and the {@code Number} types are supported as target types.
     * <p>
     * If a value in the given configuration is of a {@code Number} type and the required type is a different
     * {@code Number} type, then a type coercion to the target type will be performed.
     *
     * @param configuration Configuration to normalize
     * @param types         Map of target types
     * @return A new configuration map with values normalized according to the {@code types} map
     */
    public Map<String, Object> normalizeConfiguration(Map<String, Object> configuration, Map<String, Class> types)
    {
        final Map<String, Object> normalizedConfiguration = new HashMap<>(configuration.size());

        for (Map.Entry<String, Object> entry : configuration.entrySet())
        {
            final String propertyName = entry.getKey();
            Object value = entry.getValue();
            final Class<?> valueType = value != null ? value.getClass() : Void.class;
            final Class<?> requiredType = types.getOrDefault(propertyName, String.class);

            // Nothing to do if the type is already correct
            if (requiredType != valueType)
            {
                if (requiredType == String.class)
                {
                    // Convert to a String
                    value = String.valueOf(value);
                } else if (value instanceof String)
                {
                    // Convert String to required type
                    value = parseFromString(propertyName, (String) value, requiredType);
                } else if (value instanceof Number && Number.class.isAssignableFrom(requiredType))
                {
                    // Convert between number types
                    value = TYPE_COERCION_METHOD.get(requiredType).apply((Number) value);
                } else
                {
                    // Unsupported conversion
                    logger.error("Unsupported conversion from [{}] to [{}] for configuration property [{}], leaving value [{}] unchanged",
                            valueType, requiredType, propertyName, value);
                }
            }

            normalizedConfiguration.put(propertyName, value);
        }

        return normalizedConfiguration;
    }

    /**
     * Parse one of our supported types from a String.  If the value can't be converted for any reason, then the
     * original value is returned.
     *
     * @param propertyName Name of property being processed
     * @param value        Value of property
     * @param requiredType The type required
     * @return converted value
     */
    private Object parseFromString(String propertyName, String value, Class<?> requiredType)
    {
        try
        {
            final Function<String, Object> parseMethod = TYPE_TO_PARSE_METHOD.get(requiredType);

            if (parseMethod != null)
            {
                return parseMethod.apply(value);
            } else
            {
                logger.error("Unsupported type [{}] for configuration property [{}], leaving value [{}] unchanged",
                        requiredType, propertyName, value);
            }
        } catch (NumberFormatException e)
        {
            logger.error("Invalid value [{}] for configuration property [{}], can't parse as [{}], leaving as String",
                    value, propertyName, requiredType);
        }

        return value;
    }
}
