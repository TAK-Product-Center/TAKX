package gov.tak.api.comms.takserver.mission.call;

import jakarta.annotation.Nonnull;

/**
 * Information wrapper of parameters for endpoints.
 *
 * @param name Parameter name.
 * @param defaultValue Default parameter value.
 * Must be able to be represented as a string <i>(Any type that defines a {@link Object#toString()})</i>
 * @param required Flag indicating if the parameter is a required one.
 *
 * @since 3.5
 */
public record ParameterInfo(@Nonnull String name, @Nonnull Object defaultValue, boolean required)
{
}
