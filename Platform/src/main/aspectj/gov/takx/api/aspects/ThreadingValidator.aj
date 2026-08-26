package gov.takx.api.aspects;

import gov.takx.api.annotation.UiThread;
import gov.takx.api.annotation.WorkerThread;

import javax.swing.SwingUtilities;

/**
 * This aspect handles methods/classes annotated with the threading related annotations, {@link UiThread}
 * and {@link WorkerThread}.  If annotated methods are called on the wrong thread, an {@link IllegalStateException}
 * is thrown.
 *
 * @since 5.8
 */
public aspect ThreadingValidator
{
    // Point cuts to match direct method annotations, class-level annotations, and annotated constructors
    pointcut uiThreadMethod(): execution(@gov.takx.api.annotation.UiThread * *.*(..));
    pointcut uiThreadClassMethod(): execution(* (@gov.takx.api.annotation.UiThread *).*(..));
    pointcut uiThreadCtor(): execution((@gov.takx.api.annotation.UiThread *).new(..));

    before(): uiThreadMethod() || uiThreadClassMethod() || uiThreadCtor() {
        if (!SwingUtilities.isEventDispatchThread())
        {
            throw new IllegalStateException("Must be called on the EDT: " + AspectUtils.dumpCall(thisJoinPoint));
        }
    }

    pointcut workerThreadMethod(): execution(@gov.takx.api.annotation.WorkerThread * *.*(..));
    pointcut workerThreadClassMethod(): execution(* (@gov.takx.api.annotation.WorkerThread *).*(..));
    pointcut workerThreadCtor(): execution((@gov.takx.api.annotation.WorkerThread *).new(..));

    before(): workerThreadMethod() || workerThreadClassMethod() || workerThreadCtor() {
        if (SwingUtilities.isEventDispatchThread())
        {
            throw new IllegalStateException("Must NOT be called on the EDT: " + AspectUtils.dumpCall(thisJoinPoint));
        }
    }
}
