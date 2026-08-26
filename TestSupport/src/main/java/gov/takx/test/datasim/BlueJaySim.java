package gov.takx.test.datasim;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.platform.KeyUtils;

/**
 */
public class BlueJaySim extends ADeviceSim
{
    public static final String BLUEJAY_FAMILY = "gov.takx.Bluejay";
    public static final String BLUEJAY_TYPE = "Bluejay";

    private static int instanceCount_ = 0;

    private int instanceNum_;

    private IRaptorDataStructure dataStruct;

    /**
     * @return A fully qualified key for the given unitId
     * @since 3.0
     */
    public static String makeUnitIdKey(String unitId)
    {
        return KeyUtils.getUniqueKey(BLUEJAY_FAMILY, BLUEJAY_TYPE, unitId);
    }

    public BlueJaySim()
    {
        this(true);
    }

    public BlueJaySim(boolean rollCount)
    {
        if (rollCount) instanceNum_ = instanceCount_++;
    }

    public static void resetDeviceId()
    {
        instanceCount_ = 0;
    }

    @Override
    public String getDeviceId()
    {
        return Integer.valueOf(instanceNum_).toString();
    }

    @Override
    public String getDeviceFamily()
    {
        return BLUEJAY_FAMILY;
    }

    @Override
    public String getDeviceType()
    {
        return BLUEJAY_TYPE;
    }

    @Override
    public IRaptorDataStructure makeDataStructure()
    {
        if (dataStruct == null)
        {
            DataStructureBuilder builder = new DataStructureBuilder(getDeviceFamily(), getDeviceType(), "1.0.0", "BJ_STATUS_MESSAGE");

            // Standard values
            builder.addString("key");
            builder.addInt("devType");
            builder.addInt("messageID");
            builder.addString("visible");
            builder.addInt("direction");
            builder.addInt("style");

            // Status values
            builder.addInt("receivedTime");
            builder.addFloat("inputVoltage");
            builder.addFloat("battery");
            builder.addInt("temp");
            builder.addString("gsmPower");
            builder.addString("gpsPower");
            builder.addString("wifiPower");
            builder.addString("gsmComms");
            builder.addString("gpsLock");
            builder.addString("wifiFound");
            builder.addString("tilt");
            builder.addString("extendedWifi");
            builder.addString("light");
            builder.addString("breakwire");

            builder.addByteArray("blob");

            // gpsVals
            builder.addInt("height");
            builder.addFloat("gpsAcc");
            builder.addString("gpsAge");

            // gsmVals=>
            builder.addString("bsic");
            builder.addInt("lac");
            builder.addInt("cid");
            builder.addInt("mcc");
            builder.addInt("mnc");
            builder.addInt("gsmRSSI");
            builder.addString("timingAdvance");

            // gsmNeighborVals1=>
            builder.addString("bsic1");
            builder.addInt("bcch1");
            builder.addInt("lac1");
            builder.addInt("cid1");
            builder.addString("rxLevel1");

            // gsmNeighborVals2=>
            builder.addString("bsic2");
            builder.addInt("bcch2");
            builder.addInt("lac2");
            builder.addInt("cid2");
            builder.addString("rxLevel2");

            // gsmNeighborVals3=>
            builder.addString("bsic3");
            builder.addInt("bcch3");
            builder.addInt("lac3");
            builder.addInt("cid3");
            builder.addString("rxLevel3");

            // gsmNeighborVals4=>
            builder.addString("bsic4");
            builder.addInt("bcch4");
            builder.addInt("lac4");
            builder.addInt("cid4");
            builder.addString("rxLevel4");

            // gsmNeighborVals5=>
            builder.addString("bsic5");
            builder.addInt("bcch5");
            builder.addInt("lac5");
            builder.addInt("cid5");
            builder.addString("rxLevel5");

            // wifiVals=>
            builder.addString("ssid");
            builder.addString("bssid");
            builder.addString("wifiRSSI");
            builder.addString("channel");
            builder.addInt("capability");
            builder.addString("encryption");

            // sensorVals=>
            builder.addString("lightLevel");
            builder.addString("motionAmount");
            builder.addInt("eventType");

            // wifiVals0=>
            builder.addString("ssid0");
            builder.addString("bssid0");
            builder.addString("wifiRSSI0");
            builder.addString("channel0");
            builder.addInt("capability0");
            builder.addString("encryption0");

            // wifiVals1=>
            builder.addString("ssid1");
            builder.addString("bssid1");
            builder.addString("wifiRSSI1");
            builder.addString("channel1");
            builder.addInt("capability1");
            builder.addString("encryption1");

            // wifiVals2=>
            builder.addString("ssid2");
            builder.addString("bssid2");
            builder.addString("wifiRSSI2");
            builder.addString("channel2");
            builder.addInt("capability2");
            builder.addString("encryption2");

            // wifiVals3=>
            builder.addString("ssid3");
            builder.addString("bssid3");
            builder.addString("wifiRSSI3");
            builder.addString("channel3");
            builder.addInt("capability3");
            builder.addString("encryption3");

            builder.addShort("shortVal");
            builder.addLong("longVal");

            dataStruct = builder.build();
        }

        return dataStruct;
    }

    @Override
    public IPrePersistRaptorDataMessage makeDataMessage(IRaptorDataStructure struct, IMapObject mapObject)
    {
        if (struct == null) struct = makeDataStructure();

        IPrePersistRaptorDataMessage dataMsg = struct.createRaptorDataMessage("0");

        ILocation location = generateLocation();
        dataMsg.setLocation(location.getLatitude(), location.getLongitude(), location.getElevation());
        dataMsg.setOrientation(42.0, 42.0, 42.0);
        dataMsg.setSpeedInMps(42.0);

        if (fixedTime == -1)
        {
            dataMsg.setTime(System.currentTimeMillis());
        } else if (fixedTime > 0)
        {
            dataMsg.setReceiveTime(fixedTime);
        }
        /* else, do nothing, leave the time empty */

        // Standard values
        dataMsg.setString("key", "mykey");
        dataMsg.setInt("devType", 42);
        dataMsg.setInt("messageID", 42);
        dataMsg.setString("visible", "visible");
        dataMsg.setInt("direction", 42);
        dataMsg.setInt("style", 42);

        byte[] bytes = new byte[2048];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i % 256);
        dataMsg.setByteArray("blob", bytes);

        // Status values
        dataMsg.setInt("receivedTime", 42);
        dataMsg.setFloat("inputVoltage", 42.0f);
        dataMsg.setFloat("battery", 42.0f);
        dataMsg.setInt("temp", 42);
        dataMsg.setString("gsmPower", "gsmPower");
        dataMsg.setString("wifiPower", "wifiPower");
        dataMsg.setString("gsmComms", "gsmComms");
        dataMsg.setString("gpsLock", "gpsLock");
        dataMsg.setString("wifiFound", "wifiFound");
        dataMsg.setString("tilt", "tilt");
        dataMsg.setString("extendedWifi", "extendedWifi");
        dataMsg.setString("light", "light");
        dataMsg.setString("breakwire", "breakwire");

        // gpsVals
        dataMsg.setInt("height", 42);
        dataMsg.setFloat("gpsAcc", 42.0f);
        dataMsg.setString("gpsAge", "gpsAge");

        // gsmVals
        dataMsg.setString("bsic", "bsic");
        dataMsg.setInt("lac", 42);
        dataMsg.setInt("cid", 42);
        dataMsg.setInt("mcc", 42);
        dataMsg.setInt("mnc", 42);
        dataMsg.setInt("gsmRSSI", 42);
        dataMsg.setString("timingAdvance", "timingAdvance");

        // gsmNeighborVals1
        dataMsg.setString("bsic1", "bsic1");
        dataMsg.setInt("bcch1", 42);
        dataMsg.setInt("lac1", 42);
        dataMsg.setInt("cid1", 42);
        dataMsg.setString("rxLevel1", "rxLevel1");

        // gsmNeighborVals2
        dataMsg.setString("bsic2", "bsic2");
        dataMsg.setInt("bcch2", 42);
        dataMsg.setInt("lac2", 42);
        dataMsg.setInt("cid2", 42);
        dataMsg.setString("rxLevel2", "rxLevel2");

        // gsmNeighborVals3
        dataMsg.setString("bsic3", "bsic3");
        dataMsg.setInt("bcch3", 42);
        dataMsg.setInt("lac3", 42);
        dataMsg.setInt("cid3", 42);
        dataMsg.setString("rxLevel3", "rxLevel3");

        // gsmNeighborVals4
        dataMsg.setString("bsic4", "bsic4");
        dataMsg.setInt("bcch4", 42);
        dataMsg.setInt("lac4", 42);
        dataMsg.setInt("cid4", 42);
        dataMsg.setString("rxLevel4", "rxLevel4");

        // gsmNeighborVals5
        dataMsg.setString("bsic5", "bsic5");
        dataMsg.setInt("bcch5", 42);
        dataMsg.setInt("lac5", 42);
        dataMsg.setInt("cid5", 42);
        dataMsg.setString("rxLevel5", "rxLevel5");

        // wifiVals
        dataMsg.setString("ssid", "ssid");
        dataMsg.setString("bssid", "bssid");
        dataMsg.setString("wifiRSSI", "wifiRSSI");
        dataMsg.setString("channel", "channel");
        dataMsg.setInt("capability", 42);
        dataMsg.setString("encryption", "encryption");

        // sensorVals
        dataMsg.setString("lightLevel", "lightLevel");
        dataMsg.setString("motionAmount", "motionAmount");
        dataMsg.setInt("eventType", 42);

        // wifiVals0
        dataMsg.setString("ssid0", "ssid0");
        dataMsg.setString("bssid0", "bssid0");
        dataMsg.setString("wifiRSSI0", "wifiRSSI0");
        dataMsg.setString("channel0", "channel0");
        dataMsg.setInt("capability0", 42);
        dataMsg.setString("encryption0", "encryption0");

        // wifiVals1
        dataMsg.setString("ssid1", "ssid1");
        dataMsg.setString("bssid1", "bssid1");
        dataMsg.setString("wifiRSSI1", "wifiRSSI1");
        dataMsg.setString("channel1", "channel1");
        dataMsg.setInt("capability1", 42);
        dataMsg.setString("encryption1", "encryption1");

        // wifiVals2
        dataMsg.setString("ssid2", "ssid2");
        dataMsg.setString("bssid2", "bssid2");
        dataMsg.setString("wifiRSSI2", "wifiRSSI2");
        dataMsg.setString("channel2", "channel2");
        dataMsg.setInt("capability2", 42);
        dataMsg.setString("encryption2", "encryption2");

        // wifiVals3
        dataMsg.setString("ssid3", "ssid3");
        dataMsg.setString("bssid3", "bssid3");
        dataMsg.setString("wifiRSSI3", "wifiRSSI3");
        dataMsg.setString("channel3", "channel3");
        dataMsg.setInt("capability3", 42);
        dataMsg.setString("encryption3", "encryption3");

        dataMsg.setMapObject(mapObject);

        return dataMsg;
    }
}
