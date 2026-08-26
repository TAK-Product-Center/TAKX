package gov.takx.api.plugin.annotations;

import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IRenderingEngine;
import gov.takx.platform.globe.renderer.ARenderingEngine;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Annotation indicating that a class will provide an additional renderer for each globe that is created.  It is
 * highly recommended that such renderers extend the provided {@link ARenderingEngine}
 * base class to simplify implementation and offer consistent handling of operations such as tracking deleted
 * Map Objects.
 * <p>
 * The class so annotated must:
 * <ol>
 * <li>Implement the interface {@link IRenderingEngine}</li>
 * <li>Provide a constructor that accepts a single argument of type {@link IGlobeDelegate}</li>
 * </ol>
 * If either of those conditions are not met, then the renderer will not be used.
 * <p>
 * This is an example usage indicating that all Map Objects with a family of '{@code gov.my.stuff}' will be exclusively
 * handled by this renderer, as will any RDM with a message type of '{@code Special1}' or '{@code Special2}':
 * <pre>
 * {@code
 * @GlobeRenderer(
 *     exclusiveMapObjects = "gov.my.stuff/*",
 *     exclusiveMessageTypes = {"Special1", "Special2"}
 * )
 * public class MyRenderingEngine extends ARenderingEngine
 * {
 *     public PriorityRenderingEngine(IGlobeDelegate globe) {
 *         ...
 *     }
 *
 *     ...
 * }}
 * </pre>
 *
 * @see ARenderingEngine
 * @since 2.29
 */
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobeRenderer
{
    /**
     * Declare the set of Map Objects for which this renderer wants <em>exclusive</em> control.  All other renderers
     * will NOT render data associated with these map objects.  The map objects can be specified with wildcards
     * to match arbitrary types in a family.  Although legal, it is highly recommended that no renderer request
     * exclusive control of a wildcard pattern on family (which would mean ALL data in the system).
     * <p>
     * <p>
     * Examples of MO patterns:
     * <ul>
     * <li> &#42;/&#42; - All MOs in the system</li>
     * <li>{@code some.family/*} - all MOs in the {@code some.family} family</li>
     * <li>{@code some.family/some.device} - a specific device type</li>
     * </ul>
     *
     * @return array of exclusively controlled Map Object patterns
     */
    String[] exclusiveMapObjects() default {};

    /**
     * Declare a set of message types for which this renderer wants <em>exclusive</em> control.  All other renderers
     * will NOT render data associated with these message types.  These are not patterns.  They must match exactly
     * to a message type to have affect.
     *
     * @return array of exclusively controlled Message Types
     */
    String[] exclusiveMessageTypes() default {};
}
