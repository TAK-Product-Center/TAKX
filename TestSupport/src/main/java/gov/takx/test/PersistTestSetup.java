package gov.takx.test;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.project.IProjectManager;
import gov.takx.core.project.EmptyProjectFactory;
import gov.takx.core.project.ProjectSelection;
import gov.takx.core.registry.PluginRegistryManager;
import gov.takx.dataservices.persist.IconMapObject;
import gov.takx.platform.logging.NullProgressMonitor;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.datasim.ADeviceSim;
import gov.takx.test.datasim.BloodHoundSim;
import gov.takx.test.datasim.BlueJaySim;
import gov.takx.test.utils.DatabaseTestUtils;
import gov.takx.test.utils.PersistTestUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Test harness setup for persistence tests.
 */
@PseudoBean
public class PersistTestSetup
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static int instanceCount = 0;

    public static final double OFFSET = -10;
    public static final long FIXED_TIME_1 = 1000;
    public static final long FIXED_TIME_2 = FIXED_TIME_1 + (3600 * 1000); // Plus one hour
    public static final long FIXED_TIME_3 = FIXED_TIME_2 + (3600 * 1000);
    public static final long FIXED_TIME_4 = FIXED_TIME_3 + (24 * 3600 * 1000); // Plus one day
    private long startTime;
    private long finishTime;

    private long minTime = Long.MAX_VALUE;
    private long maxTime = 0;

    private long minReceiveTime = Long.MAX_VALUE;
    private long maxReceiveTime = 0;

    public static final ZoneId ZONE_ID_UTC = ZoneId.of("UTC");
    private TimeZone timeZoneBeforeInit;

    private UUID uuid;

    private IRaptorDataStructure bh_struct;
    private IMapEntity bh_def;

    private IRaptorDataStructure bj_struct;
    private IMapEntity bj_def;

    private final Map<String, IMapObject> mapObjectsByKey = new HashMap<>();

    @Inject
    private PluginRegistryManager pluginRegistryManager;

    @Inject
    private IProjectManager projectManager;

    @Inject
    private AContainerTest containerTest;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    public PersistTestSetup()
    {
        if (instanceCount > 0) logger.error("PTS not shutdown prior to CTOR", new RuntimeException("stack"));

        logger.info("Create PTS: {}", PersistTestUtils.getCaller());
        instanceCount += 1;

        PreferenceEntry.RDM_BATCHING.put(false);
    }

    /**
     * Initialize the database with caching turned off and load default set of devices.
     */
    public void setup()
    {
        init();
        load();
    }

    /**
     * Initialize the database with caching turned off.
     */
    public void init()
    {
        // Since this method is called from within @BeforeAll methods, uncaught exceptions fail to get reported
        // by JUnit.  So, keep all the setup work in this try/catch.
        try
        {
            // Set the JVM timezone to UTC so the DB can correctly query date values.
            // This is typically done in ApplicationLauncher.java in production but that does not run during this unit test.
            timeZoneBeforeInit = TimeZone.getDefault();
            TimeZone.setDefault(TimeZone.getTimeZone(ZONE_ID_UTC));

            // Install our own project factory so we can create projects that aren't based on real project files.
            containerTest.destroyProject();
            pluginRegistryManager.loadRegistries();
            projectManager.setProjectFactory(new EmptyProjectFactory());

            final String projectPath = projectManager.getProjectFilePath("empty");
            final ProjectSelection projectSelection = ProjectSelection.makeNew(projectPath, null, true);
            projectManager.openProjectForTest(projectSelection, new NullProgressMonitor(),
                    () -> DatabaseTestUtils.setupDatabase(true), false);

            BloodHoundSim.resetDeviceId();
            BlueJaySim.resetDeviceId();

            BloodHoundSim bhSim = new BloodHoundSim(false);
            bh_struct = bhSim.makeDataStructure();

            BlueJaySim bjSim = new BlueJaySim(false);
            bj_struct = bjSim.makeDataStructure();
        } catch (Exception e)
        {
            logger.error("Failed to open empty project", e);
            throw new RuntimeException("Failed to open project: " + e.getMessage());
        }
    }

    public void load()
    {
        try
        {
            startTime = System.currentTimeMillis();
            persistBloodHound(10, OFFSET, 0);
            persistBloodHound(10, OFFSET, FIXED_TIME_1);
            persistBloodHound(10, OFFSET, FIXED_TIME_2);
            persistBloodHound(10, OFFSET, FIXED_TIME_3);
            persistBloodHound(10, OFFSET, FIXED_TIME_4);
            persistBlueJay(10, OFFSET, 0);
            finishTime = System.currentTimeMillis();
        } catch (Exception e)
        {
            logger.error("Failed to load test data", e);
        }
    }

    /**
     * Completely teardown the DB, including dropping all the tables.
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    public void shutdown()
    {
        logger.info("shutdown PTS : {}", PersistTestUtils.getCaller());

        // Reset the JVM's TimeZone default back to value before these tests to not interfere with other unit tests
        TimeZone.setDefault(timeZoneBeforeInit);

        instanceCount -= 1;
        if (instanceCount < 0)
        {
            logger.error("Too many calls to shutdown", new RuntimeException("stack"));
            instanceCount = 0;
        }

        DatabaseTestUtils.dropDatabaseTables(containerTest);

        mapObjectsByKey.clear();
    }

    /**
     * Persist a set of bloodhound messages.
     *
     * @param num       Number of RDMs to create
     * @param offset    location offset to use during message generation
     * @param fixedTime fixed time base for time device and receive generation
     * @return created client map object
     */
    public IIconMapObject persistBloodHound(int num, double offset, long fixedTime)
    {
        if (num <= 0) throw new IllegalArgumentException("'num' must be > 0");

        IPersistService service = getService();
        BloodHoundSim bhSim = new BloodHoundSim();
        bhSim.setFixedLat(offset);
        bhSim.setFixedLong(offset);

        if (bh_struct == null)
        {
            bh_struct = bhSim.makeDataStructure();
            service.persistEntity(bh_struct.getStructure());
        }
        IRaptorDataStructure dataStruct = bh_struct;

        // XXX don't really need to send this for each device.
        if (bh_def == null)
        {
            bh_def = mapObjectPersist.getOrCreateMapEntity(bhSim.getDeviceFamily(), bhSim.getDeviceType());
            bh_def.addStructure(dataStruct.getStructure());
        }

        final IIconMapObject cmo = makeMapObject(bhSim, bh_def, bhSim.getDeviceId());

        bhSim.setMapObjectID(bhSim.getDeviceId());

        for (int i = 0; i < num; i++)
        {
            bhSim.setFixedTime(fixedTime + i * 10L);

            IPrePersistRaptorDataMessage rdm = bhSim.makeDataMessage(bh_struct, cmo);

            service.persistRDM(rdm);
            if (rdm.hasTime()) updateTime(rdm.getTime());
            updateReceiveTime(rdm.getReceiveTime());
        }

        return cmo;
    }

    private IIconMapObject makeMapObject(ADeviceSim sim, IMapEntity mapEntity, String unitId)
    {
        final IIconMapObject mapObject = sim.createMapObject(mapEntity, unitId, new RaptorLocation(ADeviceSim.STARTING_LATITUDE, ADeviceSim.STARTING_LONGITUDE));

        mapObjectsByKey.put(mapObject.getKey(), mapObject);

        return mapObject;
    }

    /**
     * Persist a set of bluejay messages.
     *
     * @param num       Number of RDMs to create
     * @param offset    location offset to use during message generation
     * @param fixedTime fixed time base for time device and receive generation
     * @return created client map object
     */
    public IIconMapObject persistBlueJay(int num, double offset, long fixedTime)
    {
        if (num <= 0) throw new IllegalArgumentException("'num' must be > 0");

        IPersistService service = getService();
        BlueJaySim bjSim = new BlueJaySim();
        bjSim.setFixedLat(offset);
        bjSim.setFixedLong(offset);
        bjSim.setFixedTime(fixedTime);

        if (bj_struct == null)
        {
            bj_struct = bjSim.makeDataStructure();
            service.persistEntity(bj_struct.getStructure());
        }
        IRaptorDataStructure dataStruct = bj_struct;

        // XXX don't really need to send this for each device.
        if (bj_def == null)
        {
            bj_def = mapObjectPersist.getOrCreateMapEntity(bjSim.getDeviceFamily(), bjSim.getDeviceType());
            bj_def.addStructure(dataStruct.getStructure());
        }

        final IIconMapObject cmo = makeMapObject(bjSim, bj_def, bjSim.getDeviceId());

        for (int i = 0; i < num; i++)
        {
            IPrePersistRaptorDataMessage rdm = bjSim.makeDataMessage(cmo);

            service.persistRDM(rdm);
            if (rdm.hasTime()) updateTime(rdm.getTime());
            updateReceiveTime(rdm.getReceiveTime());
            uuid = rdm.getUUID();
        }

        // Now construct the client map object for this device instance
        return cmo;
    }

    /**
     * Get a previously created MapObject by key.
     *
     * @param mapObjectKey Key to look up
     * @return Map Object
     * @since 2.29
     */
    public IMapObject getMapObject(String mapObjectKey)
    {
        return mapObjectsByKey.get(mapObjectKey);
    }

    private void updateTime(long time)
    {
        if (time < minTime)
        {
            minTime = time;
        }
        if (time > maxTime)
        {
            maxTime = time;
        }
    }

    private void updateReceiveTime(long time)
    {
        if (time < minReceiveTime)
        {
            minReceiveTime = time;
        }
        if (time > maxReceiveTime)
        {
            maxReceiveTime = time;
        }
    }

    /**
     * Create a MapEntity with the given values and structures.
     *
     * @param family     ME Family
     * @param type       ME Type
     * @param structures List of structures to add to ME
     * @return ME created
     */
    public IMapEntity createMapEntity(String family, String type, IRaptorDataStructure... structures)
    {
        final IMapEntity me = mapObjectPersist.getOrCreateMapEntity(family, type);

        for (IRaptorDataStructure rds : structures)
        {
            me.addStructure(rds.getStructure());
        }

        return me;
    }

    /**
     * Create a Map Object.
     *
     * @param mapEntity Defining Map Entity
     * @param unitId    Unit Id
     * @param location  initial location, may be {@code null}
     * @return Map Object
     * @since 2.29
     */
    public IIconMapObject createMapObject(IMapEntity mapEntity, String unitId, ILocation location)
    {
        final MapObjectSpec mvSpec = MapObjectSpec.builder(mapEntity, unitId).location(location).build();
        final IconMapObject mapObject = new IconMapObject(mvSpec, null);
        getService().persistEntity(mapObject);
        mapObjectsByKey.put(mapObject.getKey(), mapObject);

        return mapObject;
    }

    public IPersistService getService()
    {
        return persistServiceProvider.getPersistService();
    }

    public long getStartTime()
    {
        return startTime;
    }

    public long getFinishTime()
    {
        return finishTime;
    }

    public long getMinTime()
    {
        return minTime;
    }

    public long getMaxTime()
    {
        return maxTime;
    }

    public long getMinReceiveTime()
    {
        return minReceiveTime;
    }

    public long getMaxReceiveTime()
    {
        return maxReceiveTime;
    }

    public IRaptorDataStructure getBloodhoundStruct()
    {
        return bh_struct;
    }

    public IRaptorDataStructure getBlueJayStruct()
    {
        return bj_struct;
    }

    public UUID getUUID()
    {
        return uuid;
    }

    public IMapEntity getBloodhoundDef()
    {
        return bh_def;
    }

    public IMapEntity getBlueJayDef()
    {
        return bj_def;
    }
}
