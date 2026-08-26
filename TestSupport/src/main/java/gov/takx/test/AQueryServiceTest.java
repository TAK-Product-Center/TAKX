package gov.takx.test;

import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.core.messagequery.PluginQueryService;
import gov.takx.platform.KeyUtils;
import gov.takx.test.datasim.BloodHoundSim;
import gov.takx.test.datasim.BlueJaySim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.HashSet;
import java.util.Set;

/**
 * Class that sets up a QueryService and some data, for tests that need that
 */
public abstract class AQueryServiceTest extends AContainerTest
{
    protected static final String BH_FAMILY = BloodHoundSim.BLOODHOUND_FAMILY;
    protected static final String BJ_FAMILY = BlueJaySim.BLUEJAY_FAMILY;
    protected static final String BH_DEV_TYPE = BloodHoundSim.BLOODHOUND_TYPE;
    protected static final String BJ_DEV_TYPE = BlueJaySim.BLUEJAY_TYPE;
    protected static final String MV = "MV";
    protected static final String MULTI_VERSION_MESSAGE = "multiVersionMessage";
    protected static final String ORIGIN = "ORIGIN";
    protected static final String ORIGIN_MESSAGE = "origin";

    protected PersistTestSetup test;

    protected long startTime;
    protected long finishTime;
    protected IRaptorDataStructure bh_struct;
    protected IRaptorDataStructure bj_struct;

    protected IMapObject originMo;
    protected IRaptorDataStructure originStruct;
    protected IMapObject mvMo1;
    protected IRaptorDataStructure mvStruct1;
    protected IRaptorDataStructure mvStruct2;

    @SuppressWarnings("Duplicates")
    @BeforeAll
    public void setup()
    {
        test = new PersistTestSetup();
        test.setup();

        startTime = test.getStartTime();
        finishTime = test.getFinishTime();
        bh_struct = test.getBloodhoundStruct();
        bj_struct = test.getBlueJayStruct();

        // Construct a ME and Structure for tests using originUuid (so all the other counts aren't screwed)
        originStruct = new DataStructureBuilder(ORIGIN, ORIGIN, "0.0.1", ORIGIN_MESSAGE).addInt("int").build();
        final IMapEntity originME = test.createMapEntity(ORIGIN, ORIGIN, originStruct);
        originMo = test.createMapObject(originME, "1", new RaptorLocation(5, 5));

        // Construct multiple versions of a Structure
        mvStruct1 = new DataStructureBuilder(MV, MV, "0.0.1", MULTI_VERSION_MESSAGE).addInt("int").addInt("int2").addString("string").build();
        mvStruct2 = new DataStructureBuilder(MV, MV, "0.0.2", MULTI_VERSION_MESSAGE).addInt("int").addInt("int2").addString("string").build();

        final IMapEntity mvME1 = test.createMapEntity(MV, MV, mvStruct1, mvStruct2);
        mvMo1 = test.createMapObject(mvME1, "1", new RaptorLocation(5, 5));

        // And add some RDMs to each.  We will end up with two identical sets of 5 RDMs, one set for each structure
        // version.  Each set will have "int" values 0-4, "string" values equal to the "int" field as a string, and
        // "int2" values of 1, 2, 2, 3, 3.

        int i2 = 1;

        for (int i = 0; i < 5; i++)
        {
            makeMVRDM(mvStruct1, i, i2);
            makeMVRDM(mvStruct2, i, i2);

            if (i % 2 == 0) i2 += 1;
        }
    }

    /**
     * Construct and persist an RDM for multi-version testing.
     *
     * @param structure Structure to use
     * @param i         int value
     * @param i2        int2 value
     */
    private void makeMVRDM(IRaptorDataStructure structure, int i, int i2)
    {
        IPrePersistRaptorDataMessage rdm = structure.createRaptorDataMessage(MV, MV, "1");
        rdm.setLocation(i, i);
        rdm.setMapObject(mvMo1);
        rdm.setSourceName("source" + i);
        rdm.setInt("int", i);
        rdm.setInt("int2", i2);
        rdm.setString("string", String.valueOf(i));
        test.getService().persistRDM(rdm);
    }

    @AfterAll
    public void teardown()
    {
        test.shutdown();
    }

    /**
     * Create a Query Service for the given plugin configuration
     *
     * @param family     plugin family
     * @param deviceType Device type
     * @param unitID     unit ID
     * @param structure  Structure used by device, may be {@code null} for no structures
     * @return newly minted query service
     */
    protected PluginQueryService createPluginQueryService(String family, String deviceType, String unitID, IRaptorDataStructure structure)
    {
        final Set<IRaptorDataStructure> structs = new HashSet<>();
        if (structure != null)
        {
            structs.add(structure);
        }

        final IMapObject mo = test.getMapObject(KeyUtils.getUniqueKey(family, deviceType, unitID));
        return new PluginQueryService(mo, structs);
    }

    /**
     * Construct a query service initialized with multiple versions of the same structure.
     *
     * @return new query service
     */
    protected PluginQueryService createMultiVersionQueryService()
    {
        final Set<IRaptorDataStructure> structs = new HashSet<>();
        structs.add(mvStruct1);
        structs.add(mvStruct2);

        return new PluginQueryService(mvMo1, structs);
    }
}
