/**
 * The TAKX platform makes heavy use of CDI (Contexts and Dependency Injection) to make services available to plugins.
 * A brief description of CDI principles is provided here, but for the real gory details, please consult the
 * <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html"> Official Specification</a>.
 * <p/>
 * CDI handling within TAKX allows a plugin class that is managed by the CDI container to have values injected into it.
 * CDI-managed classes are identified by a specific annotation, {@code ApplicationScoped}.  All of the defined plugin
 * annotations ({@link gov.takx.api.plugin.annotations.AppPlugin AppPlugin},
 * {@link gov.takx.api.plugin.annotations.ConnectionPlugin ConnectionPlugin},
 * {@link gov.takx.api.plugin.annotations.DevicePlugin DevicePlugin}, and
 * {@link gov.takx.api.plugin.annotations.ImportExportPlugin ImportExportPlugin}) include the {@code @ApplicationScoped}
 * annotation.  This means that any plugin class can use injection.  Additionally, any other class can be annotated
 * with {@code @ApplicationScoped}, and the CDI container will automatically create an instance of that class if it
 * is injected elsewhere.
 * <p/>
 * To inject a reference to another CDI managed bean (service), use the {@Inject} annotation, either on a member
 * field declaration, or the constructor. E.g.,
 * <pre>{@code
 * @ApplicationScoped
 * public class SomeClass {
 *     @Inject
 *     private ICoreExceutorService;
 *
 *     ...
 * }}</pre>
 * or
 * <pre>{@code
 * @ApplicationScoped
 * public class SomeClass {
 *     @Inject
 *     SomeClass(ICoreExceutorService coreExceutorService) {
 *         ...
 *     }
 * }}</pre>
 * Note that in the examples above, the {@code @ApplicationScoped} annotation could be replaced by any of the TAKX
 * plugin defining annotations mentioned above.
 * <p/>
 * In addition to CDI managed beans (classes), TAKX provides a mechanism to have arbitrary classes partially behave
 * as if they were container managed, allowing injection and event observation.  Such classes are annotated using
 * {@link gov.takx.api.annotation.PseudoBean {@code @PseudoBean}} on the class declaration.  A key differentiator
 * between container managed beans and {@code @PseudoBean} classes is that the plugin code (not the container) is
 * responsible for allocating instances of the class using the standard {@code new} operator.  Further, since the
 * plugin's code performs the constructor call, constructor injection can't be used.  For more complete documentation
 * see the docs on the {@link gov.takx.api.annotation.PseudoBean {@code @PseudoBean}} annotation.
 * <p/>
 * The runtime platform provides a large collection of services that are available for injection using {@code @Inject}
 * annotation, as detailed in the list below.
 * <p/>
 * <ul>
$serviceList
 * </ul>
 * <p/>
 * Additionally, the following objects can be "produced" to satisfy an injection point, based on the return type
 * and any additional qualifying annotations.
 * <p/>
 * <table border="1" cellpadding="5" cellspacing="0">
 * <tr align="left">
 * <th>Object Type</th>
 * <th>Qualifying Annotations</th>
 * </tr>
$producerTable
 * </table>
 * <p/>
 * Some of the qualifying annotations listed in the table above have configurable values that control exactly how to
 * resolve the injected object. The table below lists the values for each qualifying annotation.
 * <p/>
 * <table border="1" cellpadding="5" cellspacing="0">
 * <tr align="left">
 * <th>Qualifying Annotation</th>
 * <th>Values</th>
 * </tr>
$qualifiersTable
 * </table>
 */
package gov.takx.servicelist;
