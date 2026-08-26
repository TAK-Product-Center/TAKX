package gov.takx.api.event.cdi;

/**
 * An event fired whenever a pseudo-bean is created, after injection completes.
 *
 * @param bean Instance of class annotated as {@link gov.takx.api.annotation.PseudoBean}.
 * @since 3.4
 */
public record PseudoBeanCreationEvent(Object bean)
{
}
