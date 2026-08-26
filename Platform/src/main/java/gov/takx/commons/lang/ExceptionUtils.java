package gov.takx.commons.lang;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * Utility methods for working with exceptions.  All methods are static.
 *
 */
public final class ExceptionUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Perform an operation on a list of objects, catching and reporting any exceptions.  This is often used to
     * protect calls into plugins (like listener callbacks).
     *
     * @param targets List of targets to operate upon
     * @param operation Operation dispatcher
     * @param <T> inferred type
     * @since 3.4
     */
    public static <T> void safeDoOperation(List<T> targets, Consumer<T> operation)
    {
        safeDoOperation((Collection<T>) targets, operation);
    }

    /**
     * Perform an operation on a collection of objects, catching and reporting any exceptions.  This is often used to
     * protect calls into plugins (like listener callbacks).
     *
     * @param targets List of targets to operate upon
     * @param operation Operation dispatcher
     * @param <T> inferred type
     * @since 5.3
     */
    public static <T> void safeDoOperation(Collection<T> targets, Consumer<T> operation)
    {
        targets.forEach(target -> {
            try
            {
                operation.accept(target);
            } catch (Exception e)
            {
                logger.error("Exception in target {}", target, e);
            }
        });
    }

    /**
     * @param redactionIndexMapper Function to map exception messages to the index of where redaction should start.
     * If no redaction is needed, the index yielded is negative.
     * @param t Exception to potentially redact.
     *
     * @return Input exception, or redacted version if a redaction index was found.
     *
     * @since 3.5
     */
    @Nonnull
    public static Throwable redactExceptionMessage(@Nonnull ToIntFunction<String> redactionIndexMapper, @Nonnull Throwable t)
    {
        boolean redaction = false;

        // Check if cause needs to be redacted
        Throwable cause = t.getCause();
        if (cause != null)
        {
            Throwable redactedCause = redactExceptionMessage(redactionIndexMapper, cause);
            if (cause != redactedCause) redaction = true;
        }

        // Check if suppressed exceptions need to be redacted
        Throwable[] suppressed = t.getSuppressed();
        if (suppressed != null)
        {
            for (int i = 0; i < suppressed.length; i++)
            {
                Throwable suppressedItem = suppressed[i];
                Throwable redactedSuppressedItem = redactExceptionMessage(redactionIndexMapper, suppressedItem);
                if (suppressedItem != redactedSuppressedItem)
                {
                    suppressed[i] = redactedSuppressedItem;
                    redaction = true;
                }
            }
        }

        // Check if message needs to be redacted
        String message = t.getMessage();
        int i = message == null ? -1 : redactionIndexMapper.applyAsInt(message);
        if (i >= 0)
        {
            message = message.substring(0, i) + RedactedException.REPLACEMENT;
            redaction = true;
        }

        // If any redaction occurred, return a modified exception with redaction applied
        if (redaction)
        {
            String prefix = t.getClass().getSimpleName() + ": ";
            StackTraceElement[] stackTrace = t.getStackTrace();
            return new RedactedException(prefix + message, cause, stackTrace, suppressed);
        }

        return t;
    }

    /**
     * Get a message describing the given exception and all of its nested causal exceptions
     *
     * @param e The exception to process
     * @return Combined messages from top exception and all causing exceptions
     */
    public static String getMessageWithCauses(Throwable e)
    {
        return getMessageWithCauses(null, e);
    }

    /**
     * Get a message describing the given exception and all of its nested causal exceptions
     *
     * @param prefix String to prefix to each line of output (usually for indentation)
     * @param e      The exception to process
     * @return Combined messages from top exception and all causing exceptions
     */
    public static String getMessageWithCauses(String prefix, Throwable e)
    {
        final StringBuilder buffer = new StringBuilder(512);
        boolean inCauses = false;

        while (e != null)
        {
            if (prefix != null) buffer.append(prefix);
            if (inCauses) buffer.append("   Caused by: ");

            appendMessage(e, buffer);

            // Add in any suppressed/additional exceptions
            for (Throwable throwable : e.getSuppressed())
            {
                buffer.append('\n');
                if (prefix != null) buffer.append(prefix);
                buffer.append("               and: ");
                appendMessage(throwable, buffer);
            }

            e = e.getCause(); // Loop through nested exceptions

            if (e != null)
            {
                buffer.append('\n');
                inCauses = true;
            }
        }

        return buffer.toString();
    }

    /**
     * Append the message for an exception to the given buffer.
     * @since 5.1
     */
    private static void appendMessage(Throwable e, StringBuilder buffer)
    {
        final String msg = e.getMessage();

        // If we don't have a message, then use the class name instead
        // Handle ClassNotFoundException specially since it's message is just the class name, not that useful
        if (msg != null)
        {
            if (e instanceof ClassNotFoundException) buffer.append("Class not found: ");
            buffer.append(msg);
        } else
        {
            buffer.append(e.getClass().getName());
        }
    }

    /**
     * Get the message for the original cause of an exception.
     *
     * @param t Exception to process.
     * @return Message for innermost exception.
     */
    public static String getNestedMessage(Throwable t)
    {
        Throwable cause = t.getCause();
        while (cause != null)
        {
            t = cause;
            cause = cause.getCause();
        }
        return t.getMessage();
    }

    /**
     * Get the message for the original cause of an exception.
     *
     * @param t Exception to process.
     * @return Message for innermost exception.
     */
    public static boolean hasCause(Throwable t, Class<?> c)
    {
        do
        {
            Throwable cause = t.getCause();
            if (c.isInstance(cause)) return true;
            t = cause;
        } while (t != null);
        return false;
    }

    /**
     * Determine if the exception is an instance of the given class.  Test all nested causal exceptions as well.
     *
     * @param e       The exception to test
     * @param classes The class(es) to match against
     * @return {@code true} if an exception matches, false otherwise
     */
    public static boolean anyInstanceOf(Throwable e, Class<?>... classes)
    {
        return anyInstanceOf(e, true, classes);
    }

    /**
     * Determine if the exception is an instance of the given class. If {@code checkCauses} is true, then
     * test all nested causal exceptions as well.
     *
     * @param e           The exception to test
     * @param checkCauses Pass {@code true} to test nested causal exceptions
     * @param classes     The class(es) to match against
     * @return {@code true} if an exception matches, false otherwise
     */
    public static boolean anyInstanceOf(Throwable e, boolean checkCauses, Class<?>... classes)
    {
        return findInstanceOf(e, checkCauses, classes) != null;
    }

    /**
     * Determine if the exception is an instance of the given class. If {@code checkCauses} is true, then
     * test all nested causal exceptions as well. If so, return the exception found, {@code null} otherwise.
     *
     * @param e           The exception to test
     * @param checkCauses Pass {@code true} to test nested causal exceptions
     * @param classes     The class(es) to match against
     * @return The matching exception, if found. {@code null} otherwise.
     * @since 3.3
     */
    @Nullable
    public static Throwable findInstanceOf(Throwable e, boolean checkCauses, Class<?>... classes)
    {
        Throwable current = e;
        while (current != null)
        {
            for (Class<?> cls : classes)
            {
                if (cls.isInstance(current)) return current;
            }
            if (!checkCauses) break;
            current = current.getCause();
        }

        return null;
    }

    /**
     * Get the name of the calling method ad a given depth.  Depth 0 = the name of the method calling
     * {@code getCallingMethodName}, depth 1 = that method's caller, etc.
     *
     * @param depth Which caller up the stack to retrieve
     * @return method name, "&lt;unknown&gt;" if the stack doesn't contain sufficient entries to return "depth" entry
     * @since 1.24
     */
    public static String getCallingMethodName(int depth)
    {
        return StackWalkerUtils.getCallingMethodName(depth);
    }

    /**
     * No instances.
     */
    private ExceptionUtils()
    {
    }

    /**
     * Wrapper for creating a suppressed copy of another exception.
     *
     * @since 3.5
     */
    private static class RedactedException extends Throwable
    {
        private static final String REPLACEMENT = "**REDACTED**";

        private RedactedException(@Nullable String message, @Nullable Throwable cause, @Nonnull StackTraceElement[] stackTraceElements, @Nullable Throwable[] suppressed)
        {
            super(message, cause, false, false);
            setStackTrace(stackTraceElements);
            if (suppressed != null)
            {
                for (Throwable throwable : suppressed)
                {
                    addSuppressed(throwable);
                }
            }
        }
    }
}
