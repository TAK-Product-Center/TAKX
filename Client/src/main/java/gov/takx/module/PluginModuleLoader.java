package gov.takx.module;

import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom module loader to allow plugin-based modules to be defined on the fly.  Access to all the modules on the
 * system module path is also included so plugins can declare dependencies on pre-packaged modules.
 *
 * @since 1.36
 */
public class PluginModuleLoader extends ModuleLoader
{
    private static PluginModuleFinder pluginModuleFinder = new PluginModuleFinder();

    /**
     * Construct the module loader.  We use a special finder to support dynamic module creation and a local
     * module loader to handle pre-defined (module.xml) modules on our module path.
     */
    public PluginModuleLoader()
    {
        super(new ModuleFinder[]{pluginModuleFinder});
    }

    /**
     * Add a module spec to our dynamic set.
     *
     * @param moduleSpec Module spec to add
     */
    public void addModuleSpec(final ModuleSpec moduleSpec)
    {
        pluginModuleFinder.addModuleSpec(moduleSpec);
    }

    /**
     * Reset to an initial, clean state.  For test purposes only.
     */
    void reset()
    {
        pluginModuleFinder.moduleSpecs.clear();
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }

    /**
     * Custom finder to allow us to create module specs from plugin jars on the fly.
     */
    private static final class PluginModuleFinder implements ModuleFinder
    {
        private final Map<String, ModuleSpec> moduleSpecs = new ConcurrentHashMap<>();

        public void addModuleSpec(final ModuleSpec moduleSpec)
        {
            moduleSpecs.put(moduleSpec.getName(), moduleSpec);
        }

        @Override
        public ModuleSpec findModule(String identifier, ModuleLoader delegateLoader)
        {
            return moduleSpecs.get(identifier);
        }
    }
}
