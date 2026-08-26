package gov.takx.module;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.annotations.ApplyAspectsToJar;
import gov.takx.api.plugin.annotations.ApplyAspectsToJars;
import gov.takx.bootstrap.AopClassTransformer;
import gov.takx.bootstrap.ApplicationModuleLoader;
import gov.takx.commons.file.FileUtils;
import gov.takx.platform.cdi.IndexUtils;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.modules.ClassTransformer;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.JLIClassTransformer;
import org.jboss.modules.LocalDependencySpecBuilder;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.NativeLibraryResourceLoader;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jboss.modules.filter.MultiplePathFilterBuilder;
import org.jboss.modules.filter.PathFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is responsible for constructing the ModuleSpec and Module instance
 * for the deployment. The module spec created defines a set of dependencies, making classes visible to
 * the plugin code. See {@link DependencyGenerator} for details.
 *
 * @since 1.36
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DeploymentModuleBuilder implements IDeploymentModuleBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DotName ASPECTS_TO_JAR_TYPE = DotName.createSimple(ApplyAspectsToJar.class.getName());
    private static final DotName ASPECTS_TO_JARS_TYPE = DotName.createSimple(ApplyAspectsToJars.class.getName());
    private static final DotName[] ALL_ASPECTS_TO_JAR_TYPES = {ASPECTS_TO_JAR_TYPE, ASPECTS_TO_JARS_TYPE};

    private static final String REGEX = "regex";

    private final ClassTransformer classTransformer = new JLIClassTransformer(AopClassTransformer.getInstance());
    private final PluginModuleLoader pluginLoader;
    private final PluginDeployment deployment;
    private final String moduleIdentifier;

    @Inject
    private IndexUtils indexUtils;

    private static final DependencySpec localDependencySpec; // Avoid duplicate classes from plugin jars

    static
    {
        // Try to help our plugin developers out by preventing the inclusion of our classes.  It is easy
        // for a mistake to include our own jar artifacts in a developer's uber-jar.  This filter will prevent
        // any classes in our packages from being included in the imports for a given plugin jar.

        final MultiplePathFilterBuilder filterBuilder = PathFilters.multiplePathFilterBuilder(true);

        ForbiddenPackages.getForbiddenPackages()
                .forEach(p -> filterBuilder.addFilter(PathFilters.isChildOf(p), false));

        localDependencySpec = new LocalDependencySpecBuilder()
                .setImportFilter(filterBuilder.create())
                .setExportFilter(PathFilters.acceptAll())
                .build();
    }

    /**
     * Construct a plugin context from the given deployment.  Build the module spec.
     *
     * @param deployment The plugin deployment to process
     */
    public DeploymentModuleBuilder(PluginDeployment deployment)
    {
        this.deployment = deployment;

        pluginLoader = deployment.getAttachment(DeploymentAttachments.PLUGIN_MODULE_LOADER);
        moduleIdentifier = deployment.getAttachment(DeploymentAttachments.MODULE_ID);

        if (logger.isDebugEnabled())
        {
            logger.debug("Building for: {} Module Id: {}", deployment.getName(), moduleIdentifier);
        }
    }

    @Override
    public void addModuleSpecToLoader()
    {
        pluginLoader.addModuleSpec(buildModuleSpec());
    }

    @Override
    public void build() throws ModuleLoadException, ClassNotFoundException
    {
        final Module pluginModule = pluginLoader.loadModule(moduleIdentifier);

        // And prepare the class loader for use on the deployment
        final ClassLoader classLoader = pluginModule.getClassLoader();
        deployment.putAttachment(DeploymentAttachments.CLASS_LOADER, classLoader);

        // Ensure that our context class loader is the plugins module loader
        Thread.currentThread().setContextClassLoader(classLoader);

        // And process all the plugins in this deployment
        new PluginJarProcessor(deployment).processPlugins();
    }

    /**
     * Construct the module spec based on the structure of this plugin.  Add the resources from the jar file.
     * If it is a "jar of jars," we perform special processing, see {@link #handleJarOfJarsDeployment(ModuleSpec.Builder)}.
     * <p>
     * All other dependencies are handled by {@link DependencyGenerator}.
     * <p>
     * Our AOP class transformer is added to the module so we can get control of all plugin code.
     *
     * @return ModuleSpec for this plugin
     */
    private ModuleSpec buildModuleSpec()
    {
        final ModuleSpec.Builder builder = ModuleSpec.build(moduleIdentifier);

        builder.addDependency(localDependencySpec); // Add in dependency on the local resources

        // Add the resources from this plugins jar file.  Use the original jar file location since we have mounted
        // the jarVF and that seems to hose direct access to the original underlying file (via the VFS).
        addJarResource(builder, deployment.getAttachment(DeploymentAttachments.JAR_FILE)); // Add the jar as a resource root

        if (deployment.getAttachment(DeploymentAttachments.HAS_NESTED_RESOURCES))
        {
            handleJarOfJarsDeployment(builder);
        } else
        {
            // It's an error to use @ApplyAspectsToJar in a deployment with no sub-jars.
            if (getApplyAspectsToJarAnnotations().findAny().isPresent())
            {
                logger.error("Plugin jar [{}] contains an @ApplyAspectsToJar annotation, but the jar contains no sub-jars." +
                        "   No aspects will be applied to the intended jar file.", deployment.getName());
            }
        }

        // Add in all required and declared module dependencies
        new DependencyGenerator(builder, deployment).generate();

        // And ensure that AOP processing is enabled on this module
        builder.setModuleClassLoaderFactory(ApplicationModuleLoader.CLASS_LOADER_FACTORY);
        builder.setClassFileTransformer(classTransformer);

        return builder.create();
    }

    /**
     * @return Stream of all {@link ApplyAspectsToJar} annotations present in the index
     * @since 5.5.1
     */
    private Stream<AnnotationInstance> getApplyAspectsToJarAnnotations()
    {
        return indexUtils.getAnnotationInstances(deployment.getAttachment(DeploymentAttachments.INDEX), ALL_ASPECTS_TO_JAR_TYPES);
    }

    /**
     * Handle the resource specifications for a deployment which is a "jar of jars," because it has a nested "lib"
     * directory.  We will construct a sub-module containing resource roots for all the nested jars.
     * If there is a native lib directory within the lib directory, we set up a native resource loader on it.
     * <p>
     * If any annotations indicate that sub-jars need aspect handling, we will pull them out of the "standard"
     * SUB_JARS list and add them to SUB_JARS_NEEDING_ASPECTS.  All jars in SUB_JARS_NEEDING_ASPECTS will then be
     * added as standard jar resources to the builder provided.
     *
     * @param builder Builder to update with resources and dependencies
     */
    private void handleJarOfJarsDeployment(ModuleSpec.Builder builder)
    {
        logger.debug("Adding exploded jar resources");

        determineSubJarsNeedingAspectHandling();

        // Add all sub-jars that need aspect handling as direct jar resources
        deployment.getAttachmentSet(DeploymentAttachments.SUB_JARS_NEEDING_ASPECTS)
                .forEach(jarFile -> addJarResource(builder, jarFile));

        // If we have any sub-jar files, then we need to package them into their own module so we can avoid
        // having all the code in the libs be handled by our AOP agent.  Note that this just creates the
        // sub-module definition.  The dependency generator will actually add the dependency on it.

        if (!deployment.getAttachmentList(DeploymentAttachments.SUB_JARS).isEmpty())
        {
            makeLibSubModule();
        }

        // If the deployment has additional resource loaders, then add them to the module
        // This handles things like native libraries.

        deployment.getAttachmentList(DeploymentAttachments.RESOURCE_LOADERS).stream()
                .peek(loader -> {
                    if (logger.isDebugEnabled() && loader instanceof NativeLibraryResourceLoader)
                    {
                        logger.debug("Add resource loader: {}", ((NativeLibraryResourceLoader) loader).getRoot());
                    }
                })
                .map(ResourceLoaderSpec::createResourceLoaderSpec)
                .forEach(builder::addResourceRoot);
    }

    /**
     * Process any ApplyAspectsToJar annotations and create the {@link DeploymentAttachments#SUB_JARS_NEEDING_ASPECTS}
     * attachment accordingly.
     *
     * @since 2.18
     */
    private void determineSubJarsNeedingAspectHandling()
    {
        final List<JarFile> subJars = deployment.getAttachmentList(DeploymentAttachments.SUB_JARS);

        // The basic process here is to match any jar files specified as needing aspects applied and
        // remove them from the SUB_JARS list and add them to the SUB_JARS_NEEDING_ASPECTS list.

        final Set<JarFile> subJarsNeedingAspects = getApplyAspectsToJarAnnotations()
                .map(ai -> findMatchingSubJar(subJars, ai))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (logger.isDebugEnabled())
        {
            logger.debug("sub-jars needing aspects: {}", namesForJarFiles(subJarsNeedingAspects));
        }

        // Note that subJars references the list in the attachments, so modifications apply to it directly
        subJars.removeAll(subJarsNeedingAspects);

        // And add them to the "needing aspects" list
        subJarsNeedingAspects.forEach(jarFile -> deployment.addToAttachmentSet(DeploymentAttachments.SUB_JARS_NEEDING_ASPECTS, jarFile));
    }

    /**
     * Find the singular matching sub-jar given an ApplyAspectsToJar annotation instance.  Handle both simple prefix
     * matching and regex matching.
     *
     * @param jarFiles   List of jar files to match against
     * @param annotation Annotation defining the match string and regex setting
     * @return Matching JarFile or {@code null} on any error or if the match fails to find exactly one match
     * @since 2.18
     */
    @Nullable
    private JarFile findMatchingSubJar(List<JarFile> jarFiles, AnnotationInstance annotation)
    {
        final String matchString = annotation.value().asString();
        final AnnotationValue regexValue = annotation.value(REGEX); // null if regex is not specified (i.e., defaulted)
        final boolean regex = regexValue != null && regexValue.asBoolean();
        final Predicate<JarFile> matchPredicate;

        if (regex)
        {
            try
            {
                final Pattern pattern = Pattern.compile(matchString);
                matchPredicate = jarFile -> pattern.matcher(nameForJarFile(jarFile)).matches();
            } catch (Exception e)
            {
                logger.error("Plugin [{}] contains an @ApplyAspectsToJar on [{}] that failed to compile.\n" +
                        "   No aspects will be applied to the intended jar file.",
                        deployment.getName(), matchString, e);
                return null;
            }
        } else
        {
            matchPredicate = jarFile -> nameForJarFile(jarFile).startsWith(matchString); // Simple prefix match
        }

        // Filter according to the predicate we just created
        final List<JarFile> matchingJarFiles = jarFiles.stream().filter(matchPredicate).collect(Collectors.toList());

        // Validate that we matched exactly 1 jar

        final int size = matchingJarFiles.size();
        JarFile matchingJarFile = null;

        if (size == 0)
        {
            logger.error("Plugin [{}] contains an @ApplyAspectsToJar on [{}] that can not be resolved." +
                    "  There are no matching sub-jars.", deployment.getName(), matchString);
        } else if (size > 1)
        {
            logger.error("Plugin [{}] contains an ambiguous @ApplyAspectsToJar on [{}].\n" +
                    "   This matches more than one sub-jar: {}",
                    deployment.getName(), matchString, namesForJarFiles(matchingJarFiles));
        } else
        {
            matchingJarFile = matchingJarFiles.get(0); // There can be only one
            if (logger.isDebugEnabled())
            {
                logger.debug("Plugin [{}] @ApplyAspectsToJar on [{}] matched {}[{}]",
                        deployment.getName(), matchString, regex ? "as REGEX " : "", nameForJarFile(matchingJarFile));
            }
        }

        return matchingJarFile;
    }

    /**
     * Given a JarFile, return the name of the jar.
     *
     * @param jarFile JarFile to process
     * @return The name of the jar file
     * @since 2.18
     */
    private String nameForJarFile(JarFile jarFile)
    {
        return new File(jarFile.getName()).getName();
    }

    /**
     * @return List of jar file names
     * @see #nameForJarFile(JarFile)
     * @since 2.18
     */
    private List<String> namesForJarFiles(Collection<JarFile> jarFiles)
    {
        return jarFiles.stream().map(this::nameForJarFile).collect(Collectors.toList());
    }

    /**
     * In order to avoid AOP processing on jars contained in the "lib" directory, we need to construct a separate
     * module definition for them.  Then the plugin module will have a dependency on this special module.
     */
    private void makeLibSubModule()
    {
        final String libModuleId = deployment.getAttachment(DeploymentAttachments.LIB_MODULE_ID);
        final List<JarFile> subJarFiles = deployment.getAttachmentList(DeploymentAttachments.SUB_JARS);

        if (logger.isDebugEnabled())
        {
            final List<String> jarNames = subJarFiles.stream()
                    .map(f -> FileUtils.extractLastWordFromPath(f.getName()))
                    .collect(Collectors.toList());
            logger.debug("Building library ModuleSpec for: {} jars: {}", libModuleId, jarNames);
        }

        final ModuleSpec.Builder builder = ModuleSpec.build(libModuleId);
        builder.addDependency(DependencySpec.createLocalDependencySpec());

        subJarFiles.forEach(f -> addJarResource(builder, f)); // Add every jar as a resource root

        // Generate the dependencies for the libs sub-module
        final DependencyGenerator generator = new DependencyGenerator(builder, deployment);
        generator.generateForLibsModule();

        pluginLoader.addModuleSpec(builder.create()); // Build the module spec and add it to the plugin module loader
    }

    /**
     * Construct a Jar resource loader and add it as a resource root on the given module builder.
     *
     * @param builder Builder to update
     * @param jarFile Jar file to add as a resource root
     */
    private void addJarResource(ModuleSpec.Builder builder, JarFile jarFile)
    {
        final ResourceLoader loader = ResourceLoaders.createJarResourceLoader(jarFile);
        builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(loader));
    }

    public String toString()
    {
        return "Module Builder for " + deployment.getName();
    }
}
