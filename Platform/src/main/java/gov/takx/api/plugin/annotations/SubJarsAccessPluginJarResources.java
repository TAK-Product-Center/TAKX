package gov.takx.api.plugin.annotations;

/**
 * This is a tag annotation that informs the plugin module deployment that sub-jars (those located within the
 * {@code /lib} directory of the plugin's jar) need to access resources/classes within the main plugin jar.
 * <p/>
 * This will cause an additional module dependency to be added to the private lib module (the module that contains
 * all the jars in {@code /lib}) referencing the plugin deployment's module.  This is essentially a cyclic dependency,
 * but JBoss Modules properly handles cycles in the dependency graph.
 *
 * @since 5.7
 */
public @interface SubJarsAccessPluginJarResources
{
}
