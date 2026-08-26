package gov.takx.api.annotation;

import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used to select an {@link IVirtualFileSystemManager} according to the
 * specified {@link VFSScope}.
 *
 * @since 2.0.0
 */
@Qualifier
@Retention(RUNTIME)
@Target({FIELD, TYPE, METHOD, PARAMETER})
public @interface VFSManagerScope
{
    VFSScope value();

    /**
     * Annotation literals for use in bean building.
     */
    class Literal extends AnnotationLiteral<VFSManagerScope> implements VFSManagerScope
    {
        final VFSScope scope;

        public Literal(VFSScope scope)
        {
            this.scope = scope;
        }

        @Override
        public VFSScope value()
        {
            return scope;
        }

        public static final Literal GLOBAL_INSTANCE = new Literal(VFSScope.Global);
        public static final Literal PROJECT_INSTANCE = new Literal(VFSScope.Project);
    }
}
