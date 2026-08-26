package gov.takx.core.view.gui.maps.terraindiscovery.view;

import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TerrainDiscoveryPositionListener implements PositionListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IGlobeManager globeManager;

    private final ITerrainDiscoveryController controller;
    private boolean added = false;

    public TerrainDiscoveryPositionListener(ITerrainDiscoveryController controller)
    {
        this.controller = controller;
    }

    public synchronized void update(boolean addToWorldWind)
    {
        if (addToWorldWind)
        {
            add();
        } else
        {
            remove();
        }
    }

    public synchronized void add()
    {
        if (!added)
        {
            globeManager.getPrimaryGlobeDelegate().getWorldWindow().addPositionListener(this);
            added = true;
        }
    }

    public synchronized void remove()
    {
        globeManager.getPrimaryGlobeDelegate().getWorldWindow().removePositionListener(this);
        added = false;
    }

    @Override
    public void moved(PositionEvent event)
    {
        logger.debug("{}", event);
        controller.setPosition(event.getPosition());
    }
}
