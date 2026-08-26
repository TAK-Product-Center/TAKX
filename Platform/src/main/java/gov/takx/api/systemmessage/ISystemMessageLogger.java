package gov.takx.api.systemmessage;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Contract for adding messages to the system message log (visible in the UI in the System Messages panel).
 *
 * @since 3.2
 */
@InjectableService
@NoPluginImplementations
public interface ISystemMessageLogger
{
    /**
     * Add a message to the system message log at {@code info} level.
     */
    void info(@Nonnull String message);

    /**
     * Add a message to the system message log at {@code info} level, including the stack trace of the given exception.
     */
    void info(@Nonnull String message, @Nullable Throwable exception);

    /**
     * Add a message to the system message log at {@code warn} level.
     */
    void warn(@Nonnull String message);

    /**
     * Add a message to the system message log at {@code warn} level, including the stack trace of the given exception.
     */
    void warn(@Nonnull String message, @Nullable Throwable exception);

    /**
     * Add a message to the system message log at {@code error} level.
     */
    void error(@Nonnull String message);

    /**
     * Add a message to the system message log at {@code error} level, including the stack trace of the given exception.
     */
    void error(@Nonnull String message, @Nullable Throwable exception);
}
