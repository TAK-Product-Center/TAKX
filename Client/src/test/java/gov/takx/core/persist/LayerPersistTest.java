package gov.takx.core.persist;

import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.persist.ILayerPersist;
import gov.takx.core.layer.RaptorLayerManager;
import gov.takx.platform.logging.NullProgressMonitor;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 */
public class LayerPersistTest extends AContainerTest
{
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected PersistTestSetup test;

    @BeforeAll
    public void setup()
    {
        if (logger.isDebugEnabled()) logger.debug("{} is starting", getClass().getCanonicalName());
        test = new PersistTestSetup();
        test.init();
    }

    @AfterAll
    public void close()
    {
        stopService(RaptorLayerManager.class);
        test.shutdown();
    }

    @Test
    public void persistTest()
    {
        final RaptorLayerManager mgr = getBean(RaptorLayerManager.class);

        mgr.addLayer("Stars", null, "gov.nasa.worldwind.layers.StarsLayer", true, false, "resources/globe.png", LayerGroup.EXTRAS, null, null, false);
        mgr.addLayerFromXML("USGSUrbanAreaOrtho", "config/Earth/USGSUrbanAreaOrthoLayer.xml", LayerGroup.IMAGERY);
        mgr.addLayer("Open Street Maps", null, "gov.nasa.worldwind.layers.Earth.OSMMapnikLayer", false, true, "resources/text.png", LayerGroup.FEATURES, null, null, false);
        mgr.addLayerFromXML("SBA", "layers/SBAirPhotoLayer.xml", LayerGroup.IMAGERY);

        fireEventToServiceInstance(RaptorLayerManager.class, new ProjectSaving("test", new NullProgressMonitor()));

        final ILayerPersist persist = getBean(ILayerPersist.class);

        Assertions.assertThat(persist.queryAll(LayerGroup.EXTRAS)).hasSize(1);
        Assertions.assertThat(persist.queryAll(LayerGroup.IMAGERY)).hasSize(2);
    }
}
