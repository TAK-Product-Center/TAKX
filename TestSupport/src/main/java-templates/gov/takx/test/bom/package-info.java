/**
 * The test bill of materials (BOM) identifies the libraries that the test platform includes on the test runtime
 * classpath. If plugins include a dependency on the test-bom as shown in the example below, they do not need to declare
 * versions for any of the libraries listed in the tables.
 * <p/>
 * Example:
 * <pre>
 *     testImplementation "gov.takx:test:&#36;takxVersion"
 *     testImplementation platform("gov.takx:test-bom:&#36;takxVersion")
 *     testImplementation "org.assertj:assertj-core"
 * </pre>
 * In the example above, there is a dependency on "gov.takx:test-bom". The plugin does not need to include a dependency
 * on JUnit since it is provided by the test library as a transitive dependency <i>(listed in the first table)</i>.
 * Since AssertJ is included in the test BOM and is listed in the second table below, a version is not required for the
 * dependency on {@code "org.assertj:assertj-core"}.
 * <p/>
 * <b>Note</b>: because the test library has a dependency on the platform library, the runtime dependencies of platform
 * are also on the test runtime classpath. This means, for example, that plugins do not need to declare a test runtime
 * dependency on {@code "org.apache.logging.log4j:log4j-slf4j2-impl"} since it is provided by platform.
 * See {@link gov.takx.bom} for more details on the dependencies provided by the platform library.
 * <p/>
 * The following libraries are available at runtime, and they are {@code api} <i>(transitive compile)</i> dependencies
 * from {@code 'test'} and thus do not require a dependency declaration in a consuming project.
 * <p/>
 * <table border="1" cellpadding="5" cellspacing="0">
 * <tr align="left">
 * <th>Library</th>
 * <th>Version</th>
 * </tr>
 * $apiLibraryVersionList
 * </table>
 * <p/>
 * <p/>
 * The following libraries are available at test runtime, but they are not part of the test compilation class path.
 * To use these libraries in a plugin, you will need to add a dependency declaration in your build script, but you do
 * not need to specify a version if a platform dependency is present on {@code "gov.takx:test-bom"}.
 * <br><br>
 * <table border="1" cellpadding="5" cellspacing="0">
 * <tr align="left">
 * <th>Library</th>
 * <th>Version</th>
 * </tr>
 * $runtimeLibraryVersionList
 * </table>
 */
package gov.takx.test.bom;
