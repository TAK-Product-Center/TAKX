package gov.takx.cdi;

import gov.takx.api.annotation.ExcludedProducerMethod;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.platform.cdi.AnnotationIndexer;
import jakarta.enterprise.inject.Produces;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * This class is not part of the TAKX general runtime.  It is used during the build to produce a list of all the
 * injectable objects available to a plugin.
 *
 * @since 3.4
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class InjectableServiceListGenerator
{
    private static final DotName INJECTABLE_SERVICE_NAME = DotName.createSimple(InjectableService.class.getName());
    private static final DotName EXCLUDED_PRODUCER_METHOD_NAME = DotName.createSimple(ExcludedProducerMethod.class.getName());
    private static final DotName PRODUCES_NAME = DotName.createSimple(Produces.class.getName());

    private static final String TYPE_LINK_FMT = "<a href=\"../../../%s.html\">%s</a>";
    private static final String GENERIC_TYPE_LINK_FMT = "Class&lt;? extends <a href=\"../../../%s.html\">%s</a>&gt;";
    private static final String ANNOTATION_LINK_FMT = "<a href=\"../../../%s.html\">@%s</a>";
    private static final String TYPE_LIST_LINK_FMT = " * <li><a href=\"../../../%s.html\">%s</a></li>\n";

    private static final Function<DotName, String> TRIMMED_NAME = DotName::withoutPackagePrefix;
    private static final Function<DotName, String> FULL_NAME_LIST = name -> String.format(" * <li>%s</li>\n", name.toString());

    // Instead of doing a lot more reflection, I'm going to just keep the single annotation that has bound values here
    private static final List<String> ANNOTATIONS_WITH_BOUND_VALUES = List.of(VFSManagerScope.class.getName());

    private final Index index;

    // Collection of types that DO NOT have qualifiers
    private final Set<DotName> unqualifiedTypes = new TreeSet<>(Comparator.comparing(DotName::withoutPackagePrefix));

    // Collection of producer methods and their qualifiers
    private final List<ProducerInfo> producerMethods = new ArrayList<>();

    // Collection of unique qualifying annotations found
    private final Set<ClassInfo> uniqueQualifiers = new TreeSet<>(Comparator.comparing(m -> m.name().withoutPackagePrefix()));

    /**
     * Main entry point.
     *
     * @param args 0 = build dir, remainder are directories to scan to build the index
     */
    public static void main(String[] args) throws Exception
    {
        final List<File> inputs = new ArrayList<>();
        for (int i = 1; i < args.length; i++) inputs.add(new File(args[i]));

        new InjectableServiceListGenerator(args[0], inputs).generate();
    }

    /**
     * @param indexDir Directory in which to construct the index
     * @param inputs List of directories to include in the index
     */
    private InjectableServiceListGenerator(String indexDir, List<File> inputs)
    {
        index = new AnnotationIndexer(indexDir, "cdi-index", inputs).readIndex();
    }

    /**
     * Process the index and generate our output.
     */
    private void generate()
    {
        collectInjectables();

        emitServiceList();
        System.out.print("<cut here>");
        emitProducerMethods();
        System.out.print("<cut here>");
        emitQualifiers();

        System.out.flush();
    }

    /**
     * Find all our injectable types (directly injectable services and producer methods). Collect data into two buckets:
     * <ul>
     *     <li>Types that have no qualifying annotations</li>
     *     <li>Producer methods that have DO have qualifying annotations</li>
     * </ul>
     */
    private void collectInjectables()
    {
        // Get the list of annotated classes, sorted by class name
        final List<AnnotationInstance> annotationInstances = new ArrayList<>(index.getAnnotations(INJECTABLE_SERVICE_NAME));
        annotationInstances.sort(Comparator.comparing(ai -> ai.target().asClass().name().withoutPackagePrefix()));

        for (AnnotationInstance ai : annotationInstances)
        {
            final ClassInfo classInfo = ai.target().asClass();
            final boolean hasProducerMethods = classInfo.methods().stream().anyMatch(method -> method.hasAnnotation(PRODUCES_NAME));

            if (hasProducerMethods)
            {
                // If the producer class has a 'registerAs' value, then we need to add that to our injectable list
                final var registerAs = ai.value("registerAs");
                if (registerAs != null) unqualifiedTypes.add(registerAs.asClass().name());

                collectProducerMethods(classInfo);
            } else
            {
                unqualifiedTypes.add(classInfo.name());
            }
        }

        // Get the producers sorted by their simple name
        producerMethods.sort((m1, m2) -> String.CASE_INSENSITIVE_ORDER.compare(m1.returnType().withoutPackagePrefix(), m2.returnType().withoutPackagePrefix()));

        // Find unique qualifiers
        producerMethods.forEach(info -> info.qualifiers().forEach(ai -> uniqueQualifiers.add(index.getClassByName(ai.name()))));
    }

    /**
     * Process a class that has producer methods.  Collect data into two buckets, according to whether the method has
     * any qualifying annotations or not.
     *
     * @param classInfo Class to scan
     */
    private void collectProducerMethods(ClassInfo classInfo)
    {
        classInfo.methods().stream()
                .filter(method -> method.hasAnnotation(PRODUCES_NAME))
                .forEach(method -> {
                    final List<AnnotationInstance> qualifiers = method.annotations().stream()
                            .filter(ai -> ai.name().toString().startsWith("gov.tak"))
                            .toList();

                    if (!method.hasAnnotation(EXCLUDED_PRODUCER_METHOD_NAME))
                    {
                        if (qualifiers.isEmpty())
                        {
                            unqualifiedTypes.add(method.returnType().name());
                        } else if (!method.hasAnnotation(EXCLUDED_PRODUCER_METHOD_NAME))
                        {
                            producerMethods.add(new ProducerInfo(method.returnType().name(), qualifiers));
                        }
                    }
                });
    }

    /**
     * Emit the list rows for simple injectable services.
     */
    private void emitServiceList()
    {
        final StringBuilder serviceList = new StringBuilder();
        unqualifiedTypes.forEach(name -> serviceList.append(makeLink(name, TYPE_LIST_LINK_FMT, FULL_NAME_LIST)));
        System.out.print(serviceList);
    }

    /**
     * Emit the table rows for all the producer methods.
     */
    private void emitProducerMethods()
    {
        final StringBuilder table = new StringBuilder();

        producerMethods.forEach(info -> {
            table.append(" * <tr><td>");
            table.append(makeLink(info.returnType(), TYPE_LINK_FMT, TRIMMED_NAME));
            table.append("</td>");

            table.append("<td>");
            info.qualifiers().forEach(ai -> table.append(makeLink(ai.name(), ANNOTATION_LINK_FMT, TRIMMED_NAME))
                    .append(' ').append(getDescriptionForBoundValues(ai)).append("<br>"));
            table.append("</td></tr>\n");
        });

        System.out.print(table);
    }

    /**
     * Emit the table rows for all the qualifying annotations.
     */
    private void emitQualifiers()
    {
        final StringBuilder table = new StringBuilder();

        uniqueQualifiers.forEach(info -> {
            try
            {
                table.append(" * <tr>");
                final DotName className = info.name();
                table.append("<td>").append(makeLink(className, ANNOTATION_LINK_FMT, TRIMMED_NAME)).append("</td>");

                table.append("<td>");
                final Class<?> qualifierClass = Class.forName(className.toString());
                final List<Method> declaredMethods = List.of(qualifierClass.getDeclaredMethods());

                if (declaredMethods.isEmpty())
                {
                    table.append("<i>none</i>");
                } else
                {
                    declaredMethods.forEach(method -> table.append(makeMethodEntry(method)).append("<br>"));
                }
                table.append("</td></tr>\n");

            } catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        });

        System.out.print(table);
    }

    /**
     * @param annotationInstance Annotation instance to inspect
     * @return If the qualifier has bound values, then return a string specifying those, else empty string.
     */
    private String getDescriptionForBoundValues(AnnotationInstance annotationInstance)
    {
        if (ANNOTATIONS_WITH_BOUND_VALUES.contains(annotationInstance.name().toString()))
        {
            // Something like this: @VFSManagerScope(value = Project)
            final String asString = annotationInstance.toString();
            return asString.substring(asString.indexOf('('));
        } else
        {
            return "";
        }
    }

    private String makeMethodEntry(Method method)
    {
        final Class<?> returnType = method.getReturnType();
        final Object defaultValue = method.getDefaultValue();

        if (returnType.isArray())
        {
            final DotName componentTypeName = DotName.createSimple(returnType.componentType().getName());

            return String.format("%s[] %s()%s",
                    makeLink(componentTypeName, TYPE_LINK_FMT, TRIMMED_NAME), method.getName(),
                    (defaultValue != null ? " [default=" + Arrays.toString((Object[]) defaultValue) + "] " : ""));
        } else
        {
            final String link;

            // Special handling for return types that are generic (like Class<? extends ...>)
            if (returnType.getTypeParameters().length > 0)
            {
                final String genericReturnType = method.getGenericReturnType().toString();

                // Parse this: java.lang.Class<? extends gov.takx.api.plugin.device.IDevicePlugin>
                final int boundIndex = genericReturnType.lastIndexOf(' ');
                final String bound = genericReturnType.substring(boundIndex + 1, genericReturnType.length() - 1);
                link = makeLink(DotName.createSimple(bound), GENERIC_TYPE_LINK_FMT, TRIMMED_NAME);
            } else
            {
                link = makeLink(DotName.createSimple(returnType.getName()), TYPE_LINK_FMT, TRIMMED_NAME);
            }

            return String.format("%s %s()%s", link, method.getName(),
                    defaultValue != null ? " [default=" + defaultValue + "] " : "");
        }
    }

    /**
     * Construct an HTML link for the given class name.  If it is not one of ours, then use the {@code nonTakxForm}
     * function to provide the result.
     *
     * @param dotName Name to process
     * @param format Format to apply, accepting the relative path to the class and short name as arguments
     * @param nonTakxForm Function to apply if the name doesn't start with {@code gov.tak}.
     * @return link text
     */
    private String makeLink(DotName dotName, String format, Function<DotName, String> nonTakxForm)
    {
        final String className = dotName.toString();
        return className.startsWith("gov.tak")
                ? String.format(format, className.replace('.', '/'), dotName.withoutPackagePrefix())
                : nonTakxForm.apply(dotName);
    }

    private record ProducerInfo(DotName returnType, List<AnnotationInstance> qualifiers)
    {
    }
}
