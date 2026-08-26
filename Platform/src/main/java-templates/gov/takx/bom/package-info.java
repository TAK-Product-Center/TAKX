/**
 * The runtime platform provides the following libraries as compile time dependencies and they are automatically
 * added to the runtime class path for all plugins.  No dependency is required (other than the one on TAKX platform),
 * nor does the plugin need to use an {@code @LibraryDependency} annotation for these libraries.
 * <br><br>
 * <table border="1" cellpadding="5" cellspacing="0">
 * <tr align="left">
 * <th>Library</th>
 * <th>Version</th>
 * </tr>
 * <tr>
 * <td>{@code gov.nasa:worldwind}</td>
 * <td>{@code $project.worldwindVersion}</td>
 * </tr>
 * <tr>
 * <td>{@code gov.nasa:worldwindx}</td>
 * <td>{@code $project.worldwindVersion}</td>
 * </tr>
 * <tr>
 * <td>{@code gov.tak.kernel:takkernel-all}</td>
 * <td>{@code $project.takKernelVersion}</td>
 * </tr>
 * <tr>
 * <td>{@code jakarta.enterprise:jakarta.enterprise.cdi-api}</td>
 * <td>{@code $project.jakartaCdiApiVersion}</td>
 * </tr>
 * <tr>
 * <td>{@code jcommoncommo:jcommoncommo}</td>
 * <td>{@code $project.commonCommoVersion}</td>
 * </tr>
 * <tr>
 * <td>{@code org.slf4j:slf4j-api}</td>
 * <td>{@code $project.slf4jApiVersion}</td>
 * </tr>
 * </table>
 * <p/>
 * The following libraries are also available at runtime, and they are {@code api} (transitive compile) dependencies
 * from 'platform' and thus do not require a dependency declaration in a consuming project.  However, to use any of
 * these libraries you will need to add a {@code @LibraryDependency} annotation on your plugin for the desired
 * library.
 * <p/>
 * <table border="1" cellpadding="5" cellspacing="0">
 * <tr align="left">
 * <th>Library</th>
 * <th>Version</th>
 * <th>{@code @LibraryDependency}</th>
 * </tr>
 * $apiLibraryVersionList
 * </table>
 * <p/>
 * The following libraries are also available at runtime, but they are not part of the compilation class path.
 * To use these libraries in a plugin, you will need to add a dependency declaration in your build script, and add an
 * {@code @LibraryDependency} annotation for the appropriate module on your plugin class.
 * <br><br>
 * <table border="1" cellpadding="5" cellspacing="0">
 * <tr align="left">
 * <th>Library</th>
 * <th>Version</th>
 * <th>{@code @LibraryDependency}</th>
 * </tr>
 * $runtimeLibraryVersionList
 * </table>
 */
package gov.takx.bom;
