package gov.takx.bootstrap;

import org.jboss.modules.ClassTransformer;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.JLIClassTransformer;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleDependencySpecBuilder;
import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jboss.modules.filter.PathFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Custom module finder to generate the TAKX module definitions at runtime. Instead of using module.xml files,
 * we construct the module specs dynamically. This allows us to add a class file transformer on the modules to
 * ensure our AOP preprocessor only handles classes in our core and in plugins (no libraries).
 * <p>
 * If you work on this class, pay close attention to the module loader specified in the module dependencies.
 *
 * @since 1.36 (renamed from RaptorModuleFinder in 3.4)
 */
public class ApplicationModuleFinder implements ModuleFinder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String CLIENT_MODULE_ID = "takx-client";
    public static final String SHARED_MODULE_ID = "takx-shared";

    public static final ApplicationModuleFinder INSTANCE = new ApplicationModuleFinder();

    private static final String BOOTSTRAP_ID = "takx-bootstrap";
    private static final String LIBS_ID = "all-libs";
    private static final String WORLDWIND_ID = "worldwind";
    private static final String JDK_ID = "jdk";
    private static final String SLF4J_ID = "org.slf4j.slf4j-api";
    private static final String ASPECTJ_ID = "org.aspectj";
    private static final String MODULES_ID = "org.jboss.modules";
    private static final String CDI_ID = "jakarta.enterprise.jakarta.enterprise.cdi-api";
    private static final String COMMO_ID = "jcommoncommo";
    private static final String TAK_KERNEL_ID = "gov.tak.kernel";

    private static final String LOAD = "load";
    private static final String LIB = "lib";

    private final ModuleLoader bootModuleLoader = Module.getBootModuleLoader();
    private final ClassTransformer classTransformer = new JLIClassTransformer(AopClassTransformer.getInstance());
    private final Map<String, ModuleSpec> specMap = new ConcurrentHashMap<>();

    private ApplicationModuleFinder()
    {
        // No external creation allowed
    }

    /**
     * Build our module specs on the fly as requested.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public ModuleSpec findModule(String identifier, ModuleLoader delegateLoader)
    {
        return specMap.computeIfAbsent(identifier, k -> buildApplicationModule(identifier));
    }

    /**
     * Add the "shared module" module as a dependency to the given builder. The shared module contains all
     * the libraries that are added to all class loaders (meaning plugin deployments).
     *
     * @param builder Builder to update
     * @param export  Pass {@code true} to export this dependency. In general, this will be false. But, for the
     *                client module, it is re-exported, so that anything depending on client also gets access to shared.
     */
    public static void addSharedModuleDependency(ModuleSpec.Builder builder, boolean export)
    {
        builder.addDependency(createModuleDependencyWithServices(ApplicationModuleLoader.INSTANCE, SHARED_MODULE_ID, export));
    }

    /**
     * Build the module spec for one of the modules we manage.
     *
     * @param identifier Module identifier
     * @return module spec, {@code null} if identifier is unknown or an error occurs
     */
    private ModuleSpec buildApplicationModule(String identifier)
    {
        try
        {
            switch (identifier)
            {
                case CLIENT_MODULE_ID:
                    return buildClientSpec();
                case SHARED_MODULE_ID:
                    return buildSharedSpec();
                case WORLDWIND_ID:
                    return buildWorldwindSpec();
                default:
                    logger.error("Unknown Module Identifier: {}", identifier);
                    break;
            }
        } catch (IOException e)
        {
            logger.error("Failed to build module spec for: {}", identifier, e);
        }

        return null;
    }

    /**
     * Create the ModuleSpec equivalent of a modules.xml file containing:
     * <pre>
     *     &lt;resources>
     *         &lt;resource-root path='load/client-${project.version}.jar' /&gt;
     *         &lt;resource-root path='load/processmanager-${project.version}.jar' /&gt;
     *     &lt;/resources&gt;
     *
     *     &lt;dependencies&gt;
     *         &lt;module name="all-libs" export="true" services="export" /&gt;
     *         &lt;module name="takx-bootstrap" /&gt;
     *         &lt;module name="org.jboss.modules" /&gt;
     *         &lt;module name="takx-shared" export="true" services="export" /&gt;
     *     &lt;/dependencies&gt;
     * </pre>
     * Also configure the class file transformer so all direct resources get AOP handling.
     *
     * @return ModuleSpec
     */
    private ModuleSpec buildClientSpec() throws IOException
    {
        final ModuleSpec.Builder builder = ModuleSpec.build(CLIENT_MODULE_ID);

        addJarResourceSpec(builder, LOAD, "client-");
        addJarResourceSpec(builder, LOAD, "processmanager-");

        // Add in dependency on the local resources
        builder.addDependency(DependencySpec.createLocalDependencySpec());

        addSharedModuleDependency(builder, true); // We need this exported

        // Specify our real main class
        builder.setMainClass("gov.takx.cdi.ContainerLauncher");

        builder.addDependency(createModuleDependencyWithServices(bootModuleLoader, LIBS_ID, true));
        builder.addDependency(createModuleDependencySpec(bootModuleLoader, BOOTSTRAP_ID, false));
        builder.addDependency(createModuleDependencySpec(bootModuleLoader, MODULES_ID, false));

        builder.setModuleClassLoaderFactory(ApplicationModuleLoader.CLASS_LOADER_FACTORY);
        builder.setClassFileTransformer(classTransformer);

        return builder.create();
    }

    /**
     * Create the ModuleSpec equivalent of a modules.xml file containing:
     * <pre>
     * &lt;resources&gt;
     *   &lt;resource-root path='load/platform-${project.version}.jar' /&gt;
     *   &lt;resource-root path='conf' /&gt;
     * &lt;/resources&gt;
     * </pre>
     * Also configure the class file transformer so all direct resources get AOP handling.
     *
     * @return ModuleSpec
     */
    private ModuleSpec buildSharedSpec() throws IOException
    {
        final ModuleSpec.Builder builder = ModuleSpec.build(SHARED_MODULE_ID);

        addJarResourceSpec(builder, LOAD, "platform-");

        builder.addResourceRoot(makeFileResourceLoaderSpec("conf"));

        // Add in dependency on the local resources
        builder.addDependency(DependencySpec.createLocalDependencySpec());

        // We need all the libs, but don't export them
        builder.addDependency(createModuleDependencyWithServices(bootModuleLoader, LIBS_ID, false));

        builder.addDependency(createModuleDependencySpec(bootModuleLoader, BOOTSTRAP_ID, false));
        builder.addDependency(createModuleDependencySpec(bootModuleLoader, MODULES_ID, false));
        builder.addDependency(createModuleDependencySpec(bootModuleLoader, SLF4J_ID, true));
        builder.addDependency(createModuleDependencySpec(bootModuleLoader, ASPECTJ_ID, true));
        builder.addDependency(createModuleDependencySpec(bootModuleLoader, CDI_ID, true));
        builder.addDependency(createModuleDependencySpec(bootModuleLoader, COMMO_ID, true));
        builder.addDependency(createModuleDependencySpec(bootModuleLoader, TAK_KERNEL_ID, true));

        builder.addDependency(createModuleDependencySpec(WORLDWIND_ID, true));

        builder.addDependency(createModuleDependencyWithServices(bootModuleLoader, JDK_ID, true));

        builder.setModuleClassLoaderFactory(ApplicationModuleLoader.CLASS_LOADER_FACTORY);
        builder.setClassFileTransformer(classTransformer);

        return builder.create();
    }

    /**
     * Create the ModuleSpec equivalent of a modules.xml file containing:
     * <pre>
     * &lt;resources&gt;
     *   &lt;resource-root path='../../../lib/worldwind-${worldwind.version}.jar' /&gt;
     * &lt;/resources&gt;
     *
     * &lt;dependencies&gt;
     *   &lt;module name="takx-client" export="false" services="none" /&gt;
     * &lt;/dependencies&gt;
     * </pre>
     *
     * @return ModuleSpec
     */
    private ModuleSpec buildWorldwindSpec() throws IOException
    {
        final ModuleSpec.Builder builder = ModuleSpec.build(WORLDWIND_ID);

        addJarResourceSpec(builder, LIB, "worldwind");

        // Add in dependency on the local resources
        builder.addDependency(DependencySpec.createLocalDependencySpec());

        // Give WW access to classes in client since it needs to be able to create instances from class names
        builder.addDependency(createModuleDependencySpec(CLIENT_MODULE_ID, false));

        return builder.create();
    }

    /**
     * Find a jar file with the given name prefix. This deals with the fact that our jar names have version
     * numbers in them and are not constant across releases.
     * <p>
     * The {@code exclude} parameter is used to disambiguate jar files that have common prefixes. For example,
     * assume you have two jar files, named:
     * <pre>
     *     prefix-1.0.jar
     *     prefix-modifier-1.0.jar
     * </pre>
     * If you want to load the {@code prefix-1.0.jar} file, then you would need to specify an {@code exclude}
     * value of {@code "modifier"} to ensure that file is not found first.
     *
     * @param dirName The directory in which to look for the jar file
     * @param root    The file name root to test
     * @param exclude List of strings that, if found in the file name, means it should be excluded
     * @return The name of the matching jar (the one that starts with root), or {@code null} if not found
     */
    private String findJarFile(String dirName, String root, String... exclude)
    {
        final File dir = new File(dirName);

        if (!dir.exists())
        {
            logger.error("Missing directory: {}", dirName);
            return null;
        }

        final String[] fileNames = dir.list();

        if (fileNames != null)
        {
            return Stream.of(fileNames)
                    .filter(fileName -> fileName.startsWith(root))
                    .filter(fileName -> exclude.length == 0 || Stream.of(exclude).noneMatch(fileName::contains))
                    .findAny()
                    .map(fileName -> dirName + "/" + fileName)
                    .orElse(null);
        }
        return null;
    }

    /**
     * Construct a resource loader spec on the given file/path.
     *
     * @param path Path to file/dir from which to load resources
     * @return resource loader spec
     */
    private ResourceLoaderSpec makeFileResourceLoaderSpec(String path)
    {
        final File root = new File(path);

        if (!root.exists()) logger.error("Missing resource root: {}", path);

        return ResourceLoaderSpec.createResourceLoaderSpec(ResourceLoaders.createPathResourceLoader(root.toPath()));
    }

    /**
     * Add a resource loader spec for the (required) jar file located in the given directory and having the
     * given name prefix.
     *
     * @param builder The builder to which to add the resource loader spec
     * @param dirName The directory in which to look for the jar file
     * @param root    The file name root to test
     * @param exclude List of strings that, if found in the file name, means it should be excluded
     * @see #findJarFile(String, String, String...)
     */
    private void addJarResourceSpec(ModuleSpec.Builder builder, String dirName, String root, String... exclude)
            throws IOException
    {
        addJarResourceSpec(builder, dirName, root, true, exclude);
    }

    /**
     * Add a resource loader spec for the jar file located in the given directory and having the given name prefix.
     *
     * @param builder  The builder to which to add the resource loader spec
     * @param dirName  The directory in which to look for the jar file
     * @param root     The file name root to test
     * @param required Pass {@code true} if the jar is required, {@code false} if optional
     * @param exclude  List of strings that, if found in the file name, means it should be excluded
     * @see #findJarFile(String, String, String...)
     */
    private void addJarResourceSpec(ModuleSpec.Builder builder, String dirName, String root, boolean required, String... exclude)
            throws IOException
    {
        final String jarFile = findJarFile(dirName, root, exclude);

        if (jarFile == null)
        {
            final String message = "Unable to locate jar resource: " + dirName + "/" + root + "*.jar";
            if (required)
            {
                logger.error(message);
            } else
            {
                logger.info(message);
            }
        } else
        {
            builder.addResourceRoot(makeJarResourceLoaderSpec(jarFile));
        }
    }

    /**
     * Construct a resource loader spec on the given jar file.
     *
     * @param jarFile Path to jar file from which to load resources
     * @return resource loader spec
     */
    private ResourceLoaderSpec makeJarResourceLoaderSpec(String jarFile) throws IOException
    {
        return ResourceLoaderSpec.createResourceLoaderSpec(ResourceLoaders.createJarResourceLoader(new JarFile(jarFile)));
    }

    /**
     * Create a module dependency spec. No services (nothing from META-INF) will be imported from the dependency.
     *
     * @param loader the module loader of the dependency, or {@code null} to use the module's own module loader
     * @param name   the module name (must not be {@code null})
     * @param export the export flag
     * @return The constructed module dependency spec
     * @since 2.22
     */
    private ModuleDependencySpec createModuleDependencySpec(ModuleLoader loader, String name, boolean export)
    {
        return createModuleDependencySpec(loader, name, export, false, false);
    }

    /**
     * Create a non-optional module dependency spec using a {@code null} module loader.
     *
     * @param name   the module name (must not be {@code null})
     * @param export the export flag
     * @return The constructed module dependency spec
     * @since 2.22
     */
    private ModuleDependencySpec createModuleDependencySpec(String name, boolean export)
    {
        return createModuleDependencySpec(null, name, export, false, false);
    }

    /**
     * Construct a non-optional module dependency spec that includes the services from the specified module.
     *
     * @param moduleLoader Module loader that loaded the module
     * @param name         Module name (must not be {@code null})
     * @param export       Pass {@code true} if the imported resources should be exported
     * @return DependencySpec
     * @since 2.36
     */
    private static DependencySpec createModuleDependencyWithServices(ModuleLoader moduleLoader, String name, boolean export)
    {
        return createModuleDependencySpec(moduleLoader, name, export, false, true);
    }

    /**
     * Create a module dependency spec.
     *
     * @param loader       the module loader of the dependency, or {@code null} to use the module's own module loader
     * @param name         the module name (must not be {@code null})
     * @param export       the export flag
     * @param optional     the optional flag
     * @param withServices Pass {@code true} to include services (META-INF and all sub-elements)
     * @return The constructed module dependency spec
     * @since 2.36
     */
    private static ModuleDependencySpec createModuleDependencySpec(ModuleLoader loader, String name, boolean export, boolean optional, boolean withServices)
    {
        return new ModuleDependencySpecBuilder()
                .setName(name)
                .setImportFilter(withServices ? PathFilters.acceptAll() : PathFilters.getDefaultImportFilter())
                .setExport(export)
                .setModuleLoader(loader)
                .setOptional(optional)
                .build();
    }

    public String toString()
    {
        return getClass().getSimpleName();
    }
}
