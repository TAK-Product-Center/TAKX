package gov.takx.api.query;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.device.IDevicePluginController;

/**
 * Specialized query service interface used to offer special handling for query service injection.  The CDI
 * producer for this type uses special handling to try and determine the specific device (Map Object) on
 * which to construct the query service by inspecting the injection location.  If it is within a
 * {@link IDevicePluginController} implementation, then the Map Object can
 * be determined and a query service constrained to that Map Object will be returned.  If it is used in any
 * other context, a deployment exception will be thrown because there is no context to allow the determination
 * of the appropriate Map Object.
 * <p>
 * A typical use looks like this:
 * <pre>
 * public class AcmeDevicePluginController extends ADevicePluginController implements IDevicePluginController
 * {
 *     &#64;Inject
 *     private IDeviceQueryService deviceQueryService;
 * }
 * </pre>
 *
 * @since 2.0.0
 */
@NoPluginImplementations
public interface IDeviceQueryService extends IQueryService
{
}
