package gov.takx.module;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.annotations.LibraryDependencies;
import gov.takx.api.plugin.annotations.LibraryDependency;
import gov.takx.api.plugin.annotations.PluginDependencies;
import gov.takx.api.plugin.annotations.PluginDependency;
import gov.takx.api.plugin.annotations.SubJarsAccessPluginJarResources;
import gov.takx.bootstrap.ApplicationModuleFinder;
import gov.takx.platform.cdi.IndexUtils;
import jakarta.inject.Inject;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleDependencySpecBuilder;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.filter.PathFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * This class handles the generation of the dependency specifications on a plugin module definition.
 * Dependencies are generated based on the following annotations:
 * <p>
 * <ul>
 * <li>Use of {@link PluginDependency} on a plugin class declaration</li>
 * <li>Use of {@link LibraryDependency} on a plugin class declaration</li>
 * </ul>
 * <p>
 * And a set of automatic dependencies on elements like the TAKX platform components.
 *
 * @since 2.0.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DependencyGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String OPTIONAL = "optional";
    public static final String EXPORT = "export";

    private static final DotName PLUGIN_DEPENDENCY_TYPE = DotName.createSimple(PluginDependency.class.getName());
    private static final DotName SUB_JARS_ACCESS_PLUGIN_TYPE = DotName.createSimple(SubJarsAccessPluginJarResources.class.getName());

    private static final DotName[] ALL_DEPENDENCY_TYPES = {
            DotName.createSimple(PluginDependencies.class.getName()),
            PLUGIN_DEPENDENCY_TYPE,
            DotName.createSimple(LibraryDependencies.class.getName()),
            DotName.createSimple(LibraryDependency.class.getName())};

    private static final String JAKARTA_INJECT_MODULE_ID = "jakarta.inject.jakarta.inject-api";
    private static final String JAKARTA_CDI_API_MODULE_ID = "jakarta.enterprise.jakarta.enterprise.cdi-api";
    private static final String JAKARTA_ANNOTATION_MODULE_ID = "jakarta.annotation.jakarta.annotation-api";
    private static final String WELD_MODULE_ID = "org.jboss.weld";

    private static final String JAXB_API_MODULE_ID = "jakarta.xml.bind.jakarta.xml.bind-api";
    private static final String JAXB_RI_MODULE_ID = "com.sun.xml.bind.jaxb-ri";

    @Inject
    private IPluginModuleManager pluginModuleManager;

    @Inject
    private IndexUtils indexUtils;

    private final ModuleSpec.Builder builder;
    private final PluginDeployment deployment;
    private final Index index;

    private final Set<String> dependenciesAdded = new HashSet<>();

    private final List<String> subJarFileNames = new ArrayList<>();

    /**
     * Construct a dependency generator on the given deployment.
     *
     * @param builder    The module builder for the plugin
     * @param deployment The plugin deployment to process
     */
    public DependencyGenerator(ModuleSpec.Builder builder, PluginDeployment deployment)
    {
        this.builder = builder;
        this.deployment = deployment;
        index = deployment.getAttachment(DeploymentAttachments.INDEX);
    }

    /**
     * Generate all the required/declared dependencies for this deployment.
     */
    public void generate()
    {
        // Determine the list of all the jar file names in "/lib"
        deployment.getAttachmentList(DeploymentAttachments.SUB_JARS).stream()
                .map(jarFile -> new File(jarFile.getName()).getName())
                .forEach(subJarFileNames::add);

        warnAboutPluginInclusion();

        // Add in nested library submodule, if this is a jar of jars
        if (deployment.getAttachment(DeploymentAttachments.HAS_NESTED_RESOURCES) && deployment.getAttachment(DeploymentAttachments.LIB_MODULE_ID) != null)
        {
            addLibModuleDependency();
        }

        addCommonDependencies(false); // Add shared dependencies
    }

    /**
     * It is a common mistake when building an uber-jar to pull in way too many dependencies (especially those that
     * should be declared as @PluginDependencies).  So, here we scan all the known deployments and warn if any of
     * the sub-jars match a deployment.
     */
    private void warnAboutPluginInclusion()
    {
        pluginModuleManager.getAllKnownDeployments().stream()
                // This first filter is a bit odd.  It allows a plugin jar to contain a jar that has the same
                // name as the plugin.  Odd as it sounds, the Axis2 plugin does this.
                .filter(pd -> !subJarFileNames.contains(deployment.getName()))
                .filter(pd -> subJarFileNames.contains(pd.getName()))
                .forEach(pd -> {
                    // Ok, this plugin contains another plugin in /lib, so warn about that

                    final JarFile jarFile = deployment.getAttachment(DeploymentAttachments.JAR_FILE);
                    final String jarName = new File(jarFile.getName()).getName(); // Get last component of name

                    final String dependencyName = pd.getName();
                    final String nameWithoutJar = dependencyName.toLowerCase().endsWith(".jar")
                            ? dependencyName.substring(0, dependencyName.length() - ".jar".length())
                            : dependencyName;

                    logger.error("""
                            Plugin jar [{}] contains [{}] in '/lib' that is another plugin.
                            In general, you should not include other plugins in '/lib', you should use an @PluginDependency instead.
                            The @PluginDependency is specified on one of your plugin classes as an annotation.
                            In this case, that would look like @PluginDependency("{}")""",
                            jarName, dependencyName, nameWithoutJar);
                });
    }

    /**
     * Generate the dependencies for a private libs sub-module.
     */
    public void generateForLibsModule()
    {
        addCommonDependencies(true);

        // If the SubJarsAccessPluginJarResources annotation is present, then add a cyclic dependency from the libs
        // module back to the main plugin module.
        final boolean subJarsAccessPlugin = indexUtils.getAnnotationInstances(index, SUB_JARS_ACCESS_PLUGIN_TYPE).findAny().isPresent();
        if (subJarsAccessPlugin)
        {
            final String mainModuleId = deployment.getAttachment(DeploymentAttachments.MODULE_ID);
            if (logger.isDebugEnabled()) logger.debug("add dependency from {} to {}", builder.getName(), mainModuleId);
            builder.addDependency(createModuleDependencySpec(true, null, mainModuleId, false));
        }

        // If the deployment has additional resource loaders, then add them to the module
        // This handles things like native libraries

        deployment.getAttachmentList(DeploymentAttachments.RESOURCE_LOADERS).stream()
                .map(ResourceLoaderSpec::createResourceLoaderSpec)
                .forEach(loaderSpec -> {
                    logger.debug("Add resource loader: {}", loaderSpec);
                    builder.addResourceRoot(loaderSpec);
                });
    }

    /**
     * Add in the dependency on the submodule holding the resources from the "lib" directory.
     * <p>
     * We have to export the contents of this module since it previously used to be handled as local
     * resources to the plugin jar, meaning that they would be visible to modules that depended on the plugin.
     */
    private void addLibModuleDependency()
    {
        final String libModuleId = deployment.getAttachment(DeploymentAttachments.LIB_MODULE_ID);

        builder.addDependency(createModuleDependencySpec(true, null, libModuleId, false));
    }

    /**
     * Add all the module dependencies that are common for both plugin modules and nested lib modules.
     * <p>
     * The handling of CDI dependencies is slightly different between the two, which is the reason for the
     * {@code isLibsModule} argument.  Plugin deployments all get a dependency on Weld (which brings in the
     * underlying CDI libraries).  For sub-lib modules, we don't want to force a dependency on Weld, but we do
     * require that they use the same CDI libraries as the core.  So, for lib modules, we add the CDI libraries
     * specifically.
     * <p>
     * All deployments get:
     * <ul>
     * <li>the "shared" dependency.</li>
     * <li>a dependency on jakarta.inject and Weld (for CDI support)</li>
     * <li>a dependency on the JAXB API and implementation</li>
     * </ul>
     *
     * @param isLibsModule Pass {@code true} if processing a lib module, {@code false} if processing a plugin module
     * @see ApplicationModuleFinder
     * @since 2.35
     */
    private void addCommonDependencies(boolean isLibsModule)
    {
        // Add in Weld for CDI support
        logger.debug("Adding CDI dependencies for plugin deployment");

        if (isLibsModule)
        {
            Stream.of(JAKARTA_INJECT_MODULE_ID, JAKARTA_ANNOTATION_MODULE_ID, JAKARTA_CDI_API_MODULE_ID)
                    .forEach(id -> builder.addDependency(createBootModuleDependencySpec(id)));
        } else
        {
            builder.addDependency(createBootModuleDependencySpec(WELD_MODULE_ID));
        }

        // Add in the JAXB API and implementation since it is no longer in the JDK
        builder.addDependency(createBootModuleDependencySpec(JAXB_API_MODULE_ID));
        builder.addDependency(createBootModuleDependencySpec(JAXB_RI_MODULE_ID));

        addDeclaredDependencies(); // Add the dependencies declared via annotations in the deployment

        // Add in the shared module (given to all modules)
        ApplicationModuleFinder.addSharedModuleDependency(builder, false);
    }

    /**
     * Add all the dependencies (library and plugin) declared using annotations on the plugin classes.
     */
    private void addDeclaredDependencies()
    {
        indexUtils.getAnnotationInstances(index, ALL_DEPENDENCY_TYPES)
                .map(ModuleDependency::new) // Make a module dependency
                .filter(this::testDuplicateLibDependency)
                .filter(this::testDependencyOnSelf)
                .filter(dep -> !ApplicationModuleFinder.SHARED_MODULE_ID.equals(dep.moduleId)) // This will be added automatically
                .filter(dep -> !dependenciesAdded.contains(dep.moduleId)) // Don't add dependencies twice
                .forEach(this::addDeclaredModuleDependency);
    }

    /**
     * Add a declared module dependency to the current deployment under construction.  In this case we filter out
     * certain potential modules to prevent "clever" programmers from trying to access our internal modules.
     *
     * @param dependency The module dependency
     */
    private void addDeclaredModuleDependency(ModuleDependency dependency)
    {
        String moduleId = dependency.moduleId;
        final boolean optional = dependency.optional;
        final boolean export = dependency.export;
        final ModuleLoader moduleLoader = dependency.moduleLoader;

        if (moduleId.equals(ApplicationModuleFinder.CLIENT_MODULE_ID))
        {
            logger.warn("Dependency on {} ignored: these classes are not available to plugins!", moduleId);
        } else
        {
            logger.debug("Add dependency: {} optional={} export={}", moduleId, optional, export);

            builder.addDependency(createModuleDependencySpec(export, moduleLoader, moduleId, optional));

            // If this is a plugin dependency, then add it to the list on the deployment
            if (dependency.pluginDependency)
            {
                deployment.addToAttachmentSet(DeploymentAttachments.PLUGIN_DEPENDENCIES, moduleId);
            }

            dependenciesAdded.add(moduleId); // It's been added
        }
    }

    /**
     * Verify that a given dependency is not already contained in the "/lib" directory of this jar.
     * This only tests plugin dependencies since library/module dependencies don't really have names that we
     * can turn into jar names to perform the test.
     *
     * @param dependency The dependency to test
     * @return {@code true} if the dependency is fine, {@code false} if it is a duplicate
     */
    private boolean testDuplicateLibDependency(ModuleDependency dependency)
    {
        if (!dependency.pluginDependency) return true; // Always fine on library dependencies

        // See if this dependency duplicates a sub-jar
        final String dependencyJarName = dependency.moduleId + ".jar";

        if (subJarFileNames.contains(dependencyJarName))
        {
            final JarFile jarFile = deployment.getAttachment(DeploymentAttachments.JAR_FILE);
            final String jarName = new File(jarFile.getName()).getName(); // Get last component of name

            logger.error("Plugin jar [{}] contains [{}] in '/lib' that duplicates an {} specification\n" +
                    "You should not include a given jar file in /lib if it is already declared as a dependency.",
                    jarName, dependencyJarName, dependency.source);
            return false;
        }
        return true;
    }

    /**
     * Validate that a deployment doesn't have a dependency on itself.
     *
     * @param dependency The dependency to test
     * @return {@code true} if the dependency is fine, {@code false} if it is a self reference
     * @since 2.29
     */
    private boolean testDependencyOnSelf(ModuleDependency dependency)
    {
        if (dependency.moduleId.equals(deployment.getAttachment(DeploymentAttachments.MODULE_ID)))
        {
            final JarFile jarFile = deployment.getAttachment(DeploymentAttachments.JAR_FILE);
            final String jarName = new File(jarFile.getName()).getName(); // Get last component of name

            // Inform the user, and then skip this dependency
            logger.warn("Plugin jar [{}] contains an @PluginDependency on itself.\n" +
                    "This dependency will be ignored.",
                    jarName);
            return false;
        }
        return true;
    }

    /**
     * Construct a non-optional, non-exported module dependency spec on a module defined in the boot module loader.
     *
     * @param name the module name (must not be {@code null})
     * @return The constructed module dependency spec
     * @since 2.35
     */
    private DependencySpec createBootModuleDependencySpec(String name)
    {
        return createModuleDependencySpec(false, Module.getBootModuleLoader(), name, false);
    }

    /**
     * Construct a module dependency spec.
     *
     * @param export       Pass {@code true} to export the module contents
     * @param moduleLoader the module loader of the dependency, or {@code null} to use the module's own module loader
     * @param name         the module name (must not be {@code null})
     * @param optional     Pass {@code true} if the dependency is optional
     * @return The constructed module dependency spec
     * @since 2.22
     */
    private DependencySpec createModuleDependencySpec(boolean export, ModuleLoader moduleLoader, String name, boolean optional)
    {
        // PLUGIN-2422: To ensure plugins can declare services (in META-INF/services) and include
        // additional files within META-INF, we need to add a dependency that imports ALL files.
        // The default was excluding everything in META-INF.

        return new ModuleDependencySpecBuilder()
                .setImportFilter(PathFilters.acceptAll())
                .setExport(export)
                .setModuleLoader(moduleLoader)
                .setName(name)
                .setOptional(optional)
                .build();
    }

    /**
     * Holder for module dependency data.
     * <p>
     * Note that equality is strictly based on the module id.
     */
    private final class ModuleDependency
    {
        final String moduleId;
        final ModuleLoader moduleLoader;
        final boolean optional;
        final boolean export;
        final boolean pluginDependency;
        final String source;

        /**
         * Construct a ModuleDependency from an instance of either a {@link LibraryDependency} or a {@link PluginDependency}.
         *
         * @param instance Annotation instance to process
         */
        ModuleDependency(AnnotationInstance instance)
        {
            final DotName annotationName = instance.name();
            pluginDependency = annotationName.equals(PLUGIN_DEPENDENCY_TYPE);

            source = makeSourceName(annotationName);

            // For plugin dependencies, we allow for a partial match on module Id, so match that here
            final String initialModuleId = instance.value().asString();
            moduleId = pluginDependency
                    ? performPartialMatchOnPluginDependency(initialModuleId)
                    : initialModuleId;

            // Library modules need to use the boot module loader
            moduleLoader = pluginDependency ? null : Module.getBootModuleLoader();

            AnnotationValue av;

            optional = ((av = instance.value(OPTIONAL)) != null) && av.asBoolean();
            export = ((av = instance.value(EXPORT)) != null) && av.asBoolean();
        }

        /**
         * Extract the last component of a DotName and turn it into an annotation name String by prepending '&#64;'.
         *
         * @param annotationName Dot name of annotation
         * @return String form of just the last component
         * @since 2.18
         */
        private String makeSourceName(DotName annotationName)
        {
            final String nameString = annotationName.toString();
            final int dotIndex = nameString.lastIndexOf('.');
            return "@" + (dotIndex > 0 ? nameString.substring(dotIndex + 1) : nameString);
        }

        /**
         * Scan the known deployments for those that match the given dependency using a name prefix match.
         * This supports specifying a dependency without caring about the version.  For example
         * &#64;PluginDependency("plugin.foo") will match any plugin whose module id starts with "plugin.foo"
         * (say "plugin.foo-1.2.0" for example). It is an error to match 0 or more than 1 plugin deployment.
         *
         * @param moduleId Module Id to process
         * @return Matching module Id (or original if no single match)
         */
        private String performPartialMatchOnPluginDependency(String moduleId)
        {
            final PluginDeployment deployment = pluginModuleManager.resolveDeploymentByPartialNameMatch(moduleId, DependencyGenerator.this.deployment, makeSourceName(PLUGIN_DEPENDENCY_TYPE));
            return deployment != null ? deployment.getAttachment(DeploymentAttachments.MODULE_ID) : moduleId;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ModuleDependency that = (ModuleDependency) o;

            return moduleId.equals(that.moduleId);
        }

        @Override
        public int hashCode()
        {
            return moduleId.hashCode();
        }

        @Override
        public String toString()
        {
            return "ModuleDependency{" +
                    "moduleId=" + moduleId +
                    ", source=" + source +
                    ", plugin=" + pluginDependency +
                    ", export=" + export +
                    ", optional=" + optional +
                    '}';
        }
    }
}
