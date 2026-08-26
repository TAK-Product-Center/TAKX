package gov.takx.commons.lang;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility methods for operating on a thread's stack frame.
 *
 * @see StackWalker
 * @since 3.3
 */
@SuppressWarnings("unused")
public class StackWalkerUtils
{
    /**
     * StackWalker configured to return all normal stack frames.
     */
    public static final StackWalker BASIC_WALKER = StackWalker.getInstance();

    /**
     * StackWalker configured to retain class references in the stack frames.  This can be used to easily
     * get a method's caller class:
     * <pre>{@code StackWalkerUtils.WALKER_WITH_CLASSES.getCallerClass())}</pre>
     * Or, it can be used with {@link #getStack(StackWalker, int)} if you need to troll the stack frames.
     */
    public static final StackWalker WALKER_WITH_CLASSES = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

    /**
     * StackWalker instance that includes ALL frame types (including hidden and reflection frames).
     *
     * @see Option#SHOW_HIDDEN_FRAMES
     * @see Option#SHOW_REFLECT_FRAMES
     */
    public static final StackWalker WALKER_ALL_FRAME_TYPES =
            StackWalker.getInstance(Set.of(Option.SHOW_HIDDEN_FRAMES, Option.SHOW_REFLECT_FRAMES));

    /**
     * @return List of all stack frames for the caller
     */
    public static List<StackFrame> getStack()
    {
        return getStack(BASIC_WALKER, Integer.MAX_VALUE, 1);
    }

    /**
     * @return List of at most {@code limit} stack frames for the caller
     */
    public static List<StackFrame> getStack(int limit)
    {
        return getStack(BASIC_WALKER, limit, 1);
    }

    /**
     * @return List of at most {@code limit} stack frames for the caller, using the given {@code walker}
     */
    public static List<StackFrame> getStack(StackWalker walker, int limit)
    {
        return getStack(walker, limit, 1);
    }

    private static List<StackFrame> getStack(StackWalker walker, int limit, int internalFrameCount)
    {
        return walker.walk(s -> s
                .skip(internalFrameCount + 1) // Skip our internal frames
                .limit(limit)
                .toList());
    }

    /**
     * @return A multi-line string reporting all the stack frames, excluding this methods frame.
     */
    public static String getStackTraceAsString()
    {
        return getStackTraceAsString(BASIC_WALKER, Integer.MAX_VALUE, 1);
    }

    /**
     * @return A multi-line string reporting at most {@code limit} stack frames for the caller
     */
    public static String getStackTraceAsString(int limit)
    {
        return getStackTraceAsString(BASIC_WALKER, limit, 1);
    }

    /**
     * @return A multi-line string reporting all the stack frames for the caller, using the given {@code walker}
     */
    public static String getStackTraceAsString(StackWalker walker)
    {
        return getStackTraceAsString(walker, Integer.MAX_VALUE, 1);
    }

    /**
     * @return A multi-line string reporting at most {@code limit} stack frames for the caller, using the given {@code walker}
     */
    public static String getStackTraceAsString(StackWalker walker, int limit)
    {
        return getStackTraceAsString(walker, limit, 1);
    }

    private static String getStackTraceAsString(StackWalker walker, int limit, int internalFrameCount)
    {
        return getStack(walker, limit, internalFrameCount + 1).stream()
                .map(frame -> "  at " + frame)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Get the name of the calling method at a given depth.  Depth 0 = the name of the method calling
     * {@code getCallingMethodName}, depth 1 = that method's caller, etc.
     *
     * @param depth Which caller up the stack to retrieve
     * @return method name, "&lt;unknown&gt;" if the stack doesn't contain sufficient entries to return "depth" entry
     * @since 5.1
     */
    public static String getCallingMethodName(int depth)
    {
        // Adding 1 removes this method
        return WALKER_WITH_CLASSES.walk(stream -> stream.skip(depth + 1)
                .findFirst())
                .map(frame -> {
                    String className = frame.getClassName();
                    final int i = className.lastIndexOf('.');
                    className = i > 0 ? className.substring(i + 1) : className;
                    final String fileName = frame.getFileName();
                    final int lineNumber = frame.getLineNumber();

                    return className + "." + frame.getMethodName() +
                            (frame.isNativeMethod() ? "(Native Method)"
                                    : (fileName != null && lineNumber >= 0
                                            ? "(" + fileName + ":" + lineNumber + ")"
                                            : (fileName != null ? "(" + fileName + ")" : "(Unknown Source)")));

                }).orElse("<unknown>");
    }

    private StackWalkerUtils()
    {
    }
}
