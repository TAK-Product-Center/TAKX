package gov.takx.javadoc;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.platform.cdi.AnnotationIndexer;
import org.apache.commons.text.StringEscapeUtils;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class is not part of the TAKX general runtime.  It is used during the build to produce a list of all the
 * API deprecations to be included in our Javadoc.  Its output is used in the build to set properties used in the
 * {@code copyJavaTemplates} task.
 *
 * @since 5.5
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class DeprecationListsGenerator
{
    private static final DotName DEPRECATED_API_NAME = DotName.createSimple(DeprecatedApi.class.getName());

    private final Index index;
    private final List<DeprecationInfo> deprecations = new ArrayList<>();

    /**
     * Main entry point.
     *
     * @param args 0 = build dir, remainder are directories to scan to build the index
     */
    public static void main(String[] args) throws Exception
    {
        new DeprecationListsGenerator(args[0], Stream.of(args).map(File::new).toList()).generate();
    }

    /**
     * @param indexDir Directory in which to construct the index
     * @param inputs List of directories to include in the index
     */
    private DeprecationListsGenerator(String indexDir, List<File> inputs)
    {
        index = new AnnotationIndexer(indexDir, "docs-index", inputs).readIndex();
    }

    /**
     * Process the index and generate our output.
     */
    private void generate()
    {
        collectDeprecations();
        emitDeprecations();
        System.out.flush();
    }

    /**
     * Emit the sections and lists for deprecations.  One section per "since" value.
     */
    private void emitDeprecations()
    {
        final var sb = new StringBuilder();
        String currentSince = "";

        deprecations.sort(Comparator.comparing(DeprecationInfo::since).thenComparing(di -> di.annotation));
        for (var dep : deprecations)
        {
            if (!currentSince.equals(dep.since))
            {
                currentSince = dep.since;
                if (!sb.isEmpty()) sb.append("* </ul>\n");
                sb.append(" * <h1>Deprecations since ").append(currentSince).append("</h1>\n");
                sb.append(" * <ul>\n");
            }

            sb.append(" * <li>").append(dep.annotation).append("<br>\n").append(dep.link).append("</li><br>\n");
        }
        sb.append("* </ul>\n");

        System.out.println(sb);
    }

    /**
     * Collect information on all deprecations in the API.
     */
    private void collectDeprecations()
    {
        final var annotationInstances = new ArrayList<>(index.getAnnotations(DEPRECATED_API_NAME));

        annotationInstances.forEach(ai -> {
            final String annotationDisplay = ai.toString().replaceAll(",", ", ");

            final var sinceValue = ai.value("since");
            final String since = sinceValue == null ? "" : sinceValue.asString();

            final var target = ai.target();
            if (target instanceof ClassInfo classInfo)
            {
                final String link = String.format("<a href=\"%s\">%s</a>", classToRelativeDoc(classInfo.toString()), cleanString(classInfo));
                deprecations.add(new DeprecationInfo(annotationDisplay, since, link));
            } else if (target instanceof MethodInfo methodInfo)
            {
                final String className = methodInfo.declaringClass().name().toString();
                final String link = String.format("<a href=\"%s#%s\">%s</a>", classToRelativeDoc(className),
                        makeMethodSignatureForLink(methodInfo), makeMethodSignatureForDisplay(methodInfo));

                deprecations.add(new DeprecationInfo(annotationDisplay, since, link));
            } else if (target instanceof FieldInfo fieldInfo)
            {
                final String name = fieldInfo.name();
                final String className = fieldInfo.declaringClass().name().toString();
                final String link = String.format("<a href=\"%s#%s\">%s.%s</a>", classToRelativeDoc(className), name, cleanString(className), name);

                deprecations.add(new DeprecationInfo(annotationDisplay, since, link));
            } else
            {
                System.err.println("Unknown target type: " + target.getClass());
            }
        });
    }

    /**
     * @return A "clean" string form of the given object, converting any '$' characters to '.' and escaped for use in HTML
     */
    private static String cleanString(Object o)
    {
        return StringEscapeUtils.escapeHtml4(o.toString().replaceAll("\\$", "."));
    }

    /**
     * @return The relative path from the javadoc root to the given class' docs
     */
    private static String classToRelativeDoc(String className)
    {
        return "../../../" + className.replace('.', '/').replaceAll("\\$", ".") + ".html";
    }

    /**
     * @return The signature (method name and argument types) to use in an HTML link to the method
     */
    private static StringBuilder makeMethodSignatureForLink(MethodInfo methodInfo)
    {
        final var sb = new StringBuilder();
        sb.append(methodInfo.name()).append('(');
        final int startLength = sb.length();

        methodInfo.parameterTypes().forEach(t -> {
            if (sb.length() > startLength) sb.append(",");
            sb.append(t.name().toString().replaceAll("\\$", "."));
        });
        sb.append(')');
        return sb;
    }

    /**
     * @return the method signature to display (vs. the link), including return type, fully qualified containing class name,
     * method name, parameter types, and parameter names (if available).
     */
    private static String makeMethodSignatureForDisplay(MethodInfo methodInfo)
    {
        final var sb = new StringBuilder();
        sb.append(typeToString(methodInfo.returnType())).append(' ').append(methodInfo.declaringClass()).append('.').append(methodInfo.name());

        sb.append('(');
        final var parameters = methodInfo.parameterTypes();
        for (int i = 0, sz = parameters.size(); i < sz; i++)
        {
            sb.append(typeToString(parameters.get(i)));

            final String parameterName = methodInfo.parameterName(i);
            if (parameterName != null) sb.append(' ').append(parameterName);

            if (i < sz - 1) sb.append(", ");
        }
        sb.append(')');
        return cleanString(sb.toString());
    }

    /**
     * @return a string with type names stripped of their package prefix, including any parameterized types
     */
    private static String typeToString(Type type)
    {
        if (type instanceof ParameterizedType parameterizedType)
        {
            final var sb = new StringBuilder();
            sb.append(parameterizedType.name().withoutPackagePrefix()).append('<');

            final int startLength = sb.length();
            for (Type argument : parameterizedType.arguments())
            {
                if (sb.length() > startLength) sb.append(", ");
                sb.append(typeToString(argument));
            }
            sb.append('>');

            return sb.toString();
        } else
        {
            return type.name().withoutPackagePrefix();
        }
    }

    private record DeprecationInfo(String annotation, String since, String link)
    {
    }
}
