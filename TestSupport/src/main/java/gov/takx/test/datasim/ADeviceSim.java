package gov.takx.test.datasim;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.core.plugin.synthetic.SyntheticPluginDefinition;
import gov.takx.dataservices.persist.IconMapObject;
import jakarta.inject.Inject;

import java.util.Random;

/**
 */
@PseudoBean
public abstract class ADeviceSim
{
    public static final double STARTING_LATITUDE = 34.437;
    public static final double STARTING_LONGITUDE = -119.82;

    private ILocation lastLocation = new RaptorLocation(STARTING_LATITUDE, STARTING_LONGITUDE);
    private final Random rand = new Random();
    private final double randomLat = rand.nextDouble();
    private final double randomLong = rand.nextDouble();

    protected double fixedLat = 0;
    protected double fixedLong = 0;
    protected long fixedTime = -1;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    abstract String getDeviceFamily();

    abstract String getDeviceId();

    abstract String getDeviceType();

    abstract IRaptorDataStructure makeDataStructure();

    public IPrePersistRaptorDataMessage makeDataMessage()
    {
        return makeDataMessage(null, null);
    }

    public IPrePersistRaptorDataMessage makeDataMessage(IMapObject mapObject)
    {
        return makeDataMessage(null, mapObject);
    }

    public IPrePersistRaptorDataMessage makeDataMessage(IRaptorDataStructure struct)
    {
        return makeDataMessage(struct, null);
    }

    abstract IPrePersistRaptorDataMessage makeDataMessage(IRaptorDataStructure struct, IMapObject mapObject);

    public ILocation generateLocation()
    {
        double tempLat = lastLocation.getLatitude();
        double tempLon = lastLocation.getLongitude();

        double latOff = fixedLat == 0.0 ? rand.nextDouble() - randomLat : fixedLat;
        double longOff = fixedLong == 0.0 ? rand.nextDouble() - randomLong : fixedLong;
        tempLat += ((latOff) * 1e-1);
        tempLon += ((longOff) * 1e-1);

        tempLat = tempLat % 90;
        tempLon = tempLon % 180;

        double elevation = rand.nextDouble() % 200;
        return lastLocation = new RaptorLocation(tempLat, tempLon, elevation);
    }

    public double getFixedLat()
    {
        return fixedLat;
    }

    public void setFixedLat(double fixedLat)
    {
        this.fixedLat = fixedLat;
    }

    public double getFixedLong()
    {
        return fixedLong;
    }

    public void setFixedLong(double fixedLong)
    {
        this.fixedLong = fixedLong;
    }

    public long getFixedTime()
    {
        return fixedTime;
    }

    public void setFixedTime(long fixedTime)
    {
        this.fixedTime = fixedTime;
    }

    /**
     * Construct a client map object for the given MapEntity and unitId for this simulated device.
     *
     * @param mapEntity Defining Map Entity
     * @param unitId    Unit Id
     * @param loc       location of device
     * @return newly minted CMO
     */
    public IIconMapObject createMapObject(IMapEntity mapEntity, String unitId, ILocation loc)
    {
        makeDataStructure();

        final IDevicePluginDefinition definition =
                SyntheticPluginDefinition.syntheticTestDevice(mapEntity.getFamily(), mapEntity.getType(), "1.0.0");

        final MapObjectSpec spec = MapObjectSpec.builder(mapEntity, unitId).location(loc).build();
        IIconMapObject mo = new IconMapObject(spec, definition);

        persistServiceProvider.getPersistService().persistEntity(mo);
        return mo;
    }

    protected String myDeviceId;

    public void setMapObjectID(String deviceId)
    {
        myDeviceId = deviceId;
    }
}
