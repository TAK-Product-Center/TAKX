package gov.takx.core.globe;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.layer.IRaptorLayer;
import jakarta.annotation.PostConstruct;

/**
 * Specialized globe layer manager to be used when running in headless mode.  Essentially all layer operations
 * are ignored except:
 * <ul>
 * <li>Adding user file stores</li>
 * <li>dealing with elevation models</li>
 * </ul>
 *
 * @since 1.50
 */
@PseudoBean
public class HeadlessGlobeLayerManager extends AGlobeLayerManager
{
    /**
     * Construct the headless globe layer manager.
     *
     * @param globe The globe we are the layer manager for
     */
    public HeadlessGlobeLayerManager(IApplicationGlobe globe)
    {
        super(globe, true);
    }

    @PostConstruct
    private void postConstruct()
    {
        restoreTerrainService();
    }

    @Override
    public int getLayerUseCount(IRaptorLayer raptorLayer)
    {
        return 0;
    }
}
