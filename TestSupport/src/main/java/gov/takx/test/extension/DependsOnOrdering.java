package gov.takx.test.extension;

import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JUnit method ordering strategy that ensures any test method that declares a {@link DependsOn} for another test method
 * will run after all of its declared dependencies.
 *
 * @since 3.4.0
 */
public class DependsOnOrdering implements MethodOrderer
{
    @Override
    public void orderMethods(MethodOrdererContext context)
    {
        List<? extends MethodDescriptor> descriptors = context.getMethodDescriptors();
        descriptors.sort(new DependencySorter(descriptors));
    }

    private static class DependencySorter implements Comparator<MethodDescriptor>
    {
        private final Map<MethodDescriptor, Set<MethodDescriptor>> dependencies = new HashMap<>();
        private final Map<String, MethodDescriptor> nameLookup;

        public DependencySorter(List<? extends MethodDescriptor> descriptors)
        {
            // Make name lookup for methods
            nameLookup = descriptors.stream()
                    .collect(Collectors.toMap(d -> d.getMethod().getName(), d -> d));

            // Make map of methods depending on others
            for (MethodDescriptor descriptor : descriptors)
            {
                descriptor.findAnnotation(DependsOn.class).ifPresent(dependsOn -> Arrays.stream(dependsOn.value())
                        .map(nameLookup::get)
                        .forEach(dependencyMethod -> dependencies.computeIfAbsent(descriptor, d -> new HashSet<>())
                                .add(dependencyMethod)));
            }
        }

        @Override
        public int compare(MethodDescriptor descA, MethodDescriptor descB)
        {
            if (dependsOn(descA, descB)) return 1; //  A depends on B ==> B runs first (1)
            if (dependsOn(descB, descA)) return -1; // B depends on A ==> B runs last  (-1)

            // If no dependency relationship, order by name
            return descA.getMethod().getName().compareToIgnoreCase(descB.getMethod().getName());
        }

        private boolean dependsOn(MethodDescriptor a, MethodDescriptor b)
        {
            Set<MethodDescriptor> allDependencies = dependencies.getOrDefault(a, Collections.emptySet());
            boolean keepScanning = !allDependencies.isEmpty();
            while (keepScanning)
            {
                boolean anyAdded = false;
                for (MethodDescriptor dependency : new HashSet<>(allDependencies))
                {
                    anyAdded |= allDependencies.addAll(dependencies.getOrDefault(dependency, Collections.emptySet()));
                }
                keepScanning = anyAdded;
            }
            return allDependencies.contains(b);
        }
    }
}
