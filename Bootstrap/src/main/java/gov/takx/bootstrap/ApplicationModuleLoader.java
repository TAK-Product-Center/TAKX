package gov.takx.bootstrap;

import org.jboss.modules.ConcurrentClassLoader;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleClassLoaderFactory;
import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoader;

/**
 * In order to add AOP processing to all of our classes, but none of our libraries, we use a custom Module loader.
 * We could (and in fact did) use a java agent to process ALL the class files as we load them, but that adds
 * overhead to all class loading and we have to add further logic to exclude processing on classes we don't want
 * to touch. Further, you have to specify the agent on the command line and since our jar files have version
 * numbers in the names, we have to change this setting (in intellij) every time we roll a new release.
 * <p/>
 * For all those reasons, we had to chop it.  Now, we use this custom module loader so we can build module
 * specs that indicate the class transformer to use.
 *
 * @since 1.36 (renamed from RaptorModuleLoader in 3.4)
 */
public class ApplicationModuleLoader extends ModuleLoader
{
    /**
     * Our singleton instance.
     */
    public static final ApplicationModuleLoader INSTANCE = new ApplicationModuleLoader();

    public static final ModuleClassLoaderFactory CLASS_LOADER_FACTORY = new ThreadSafeClassLoader.Factory();

    /**
     * Construct using the {@link ApplicationModuleFinder} as our finder.
     */
    public ApplicationModuleLoader()
    {
        super(new ModuleFinder[]{ApplicationModuleFinder.INSTANCE});
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }

    /**
     * There appears to be a threading issue with the default JBoss Modules {@link ConcurrentClassLoader}.  It allows an
     * arbitrary number of threads to call down into defineClass, which results in some form of corruption with
     * our {@link AopClassTransformer}, leading to {@code @PseudoBean} objects not getting injection handling.
     * <p>
     * So, this class loader overrides the {@link #loadClassLocal(String, boolean)} method to make it synchronized
     * which ensures that only a single call is ever made to {@code defineClass}.
     *
     * @since 2.22
     */
    private static final class ThreadSafeClassLoader extends ModuleClassLoader
    {
        static
        {
            try
            {
                ClassLoader.registerAsParallelCapable();
            } catch (Throwable ignored)
            {
            }
        }

        /**
         * Construct a new instance.
         *
         * @param configuration the module class loader configuration to use
         */
        private ThreadSafeClassLoader(Configuration configuration)
        {
            super(configuration);
        }

        /**
         * Overridden just to make it synchronized.
         * <p>
         * {@inheritDoc}
         */
        @Override
        public synchronized Class<?> loadClassLocal(String className, boolean resolve) throws ClassNotFoundException
        {
            return super.loadClassLocal(className, resolve);
        }

        /**
         * Internal factory.  This has to be nested within the {@code ThreadSafeClassLoader} because it needs to
         * reference the package protected {@link org.jboss.modules.ModuleClassLoader.Configuration} class.
         */
        private static final class Factory implements ModuleClassLoaderFactory
        {
            @Override
            public ModuleClassLoader create(ModuleClassLoader.Configuration configuration)
            {
                return new ThreadSafeClassLoader(configuration);
            }
        }
    }
}
