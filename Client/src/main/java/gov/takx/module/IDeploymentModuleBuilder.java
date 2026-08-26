package gov.takx.module;

import org.jboss.modules.ModuleLoadException;

import java.io.IOException;

/**
 * Contract for classes that can build a "module" for a deployment.  Originally, this term was used since we
 * built actual JBoss Module instances, with tightly controlled class loaders.  Now, for testing, we don't actually
 * build a module, just a special class loader to ensure all the resources are in the deployment.
 *
 * @since 2.0.0
 */
public interface IDeploymentModuleBuilder
{
    /**
     * Build the module spec for the plugin and add it to the plugin module loader.
     *
     * @since 2.18
     */
    void addModuleSpecToLoader();

    /**
     * Build the "module" and prepare the class loader for use.  Once ready, process all the plugins in the
     * deployment.  If successful, the class loader will be added to the deployment under the attachment key
     * {@link DeploymentAttachments#CLASS_LOADER}.
     */
    void build() throws IOException, ModuleLoadException, ClassNotFoundException;
}
