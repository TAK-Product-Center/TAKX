package gov.takx.core.ioservice;

import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.plugin.io.IImportExportPluginDelegate;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import gov.takx.test.datasim.BloodHoundSim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.7
 */
public class ImportExportPluginDelegateTest extends AContainerTest
{
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private PersistTestSetup test;
    private IImportExportPluginDelegate bh_delegate;

    @BeforeAll
    public void setup()
    {
        test = new PersistTestSetup();
        test.setup();

        IPersistService service = test.getService();
        BloodHoundSim bhSim = new BloodHoundSim();
        bh_delegate = new ImportExportPluginDelegate(bhSim.getDeviceFamily(), bhSim.getDeviceType());

        startService(IRaptorDataPortal.class);
        startService(IDeviceGateway.class);
        startService(IMapObjectManager.class);

        IMapObject bh = test.getMapObject(BloodHoundSim.makeUnitIdKey("0"));
        int cnt = 100;
        if (logger.isDebugEnabled()) logger.debug("Adding {} messages to Bloodhound.0", cnt);
        for (int i = 0; i < cnt; i++)
        {
            bhSim.setFixedTime(i * 10);
            bhSim.setMapObjectID(bhSim.getDeviceId());
            IPrePersistRaptorDataMessage rdm = bhSim.makeDataMessage(bh);
            rdm.setMapObject(bh);
            service.persistRDM(rdm);
        }
    }

    @AfterAll
    public void teardown()
    {
        stopService(IRaptorDataPortal.class);
        stopService(IDeviceGateway.class);
        stopService(IMapObjectManager.class);

        test.shutdown();
    }

    @Test
    public void createMapObject()
    {
        IMapObject mapObject = bh_delegate.getOrCreateMapObject(MapObjectSpec.builder(test.getBloodhoundDef(), "100").build());
        assertThat(mapObject).isNotNull();
        assertThat(mapObject.getUnitID()).isEqualTo("100");

        // Create the structure
        DataStructureBuilder builder = new DataStructureBuilder("gov.takx.test", "Analysis", "1.0.0", "COPY");
        IRaptorDataStructure rds = builder.build();

        mapObject.getMapEntity().addStructure(rds.getStructure());
    }
}
