package gov.takx.test.datasim;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.KeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 */
public class BloodHoundSim extends ADeviceSim
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String BLOODHOUND_FAMILY = "gov.takx.Bloodhound";
    public static final String BLOODHOUND_TYPE = "Bloodhound";

    private static int instanceCount_ = 0;

    private int instanceNum_;

    private IRaptorDataStructure dataStruct = null;

    private boolean panic = false;
    private double batteryVoltage = 8.0;
    private double pitch = 40.0;
    private double pitchIncrement = 20.0;
    private double heading = 10.0;
    private double roll = 10.0;
    private double speed = 100.0;

    /**
     * @return A fully qualified key for the given unitId
     * @since 3.0
     */
    public static String makeUnitIdKey(String unitId)
    {
        return KeyUtils.getUniqueKey(BLOODHOUND_FAMILY, BLOODHOUND_TYPE, unitId);
    }

    public BloodHoundSim()
    {
        this(true);
    }

    public BloodHoundSim(boolean rollCount)
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
        return BLOODHOUND_FAMILY;
    }

    @Override
    public String getDeviceType()
    {
        return BLOODHOUND_TYPE;
    }

    @Override
    public IRaptorDataStructure makeDataStructure()
    {
        if (dataStruct == null)
        {
            DataStructureBuilder builder = new DataStructureBuilder(getDeviceFamily(), getDeviceType(), "1.0.0", "BH_STATUS_MESSAGE");

            builder.addString("key");

            builder.addInt("devType");
            builder.addBoolean("visible");
            builder.addInt("direction");
            builder.addInt("style");
            builder.addFloat("batt");
            builder.addDouble("Voltage");
            builder.addDouble("CURRENT");
            builder.addDouble("TEMPERATURE");
            builder.addBoolean("GPS_POWER");
            builder.addBoolean("FREEWAVE_POWER");
            builder.addBoolean("ACCEL_POWER");
            builder.addDouble("ACCEL_X");
            builder.addDouble("ACCEL_Y");
            builder.addDouble("ACCEL_Z");
            builder.addString("Fix");
            builder.addInt("Height");
            builder.addBoolean("Tilt");
            builder.addBoolean("Panic");
            builder.addString("MessageType");

            dataStruct = builder.build();
        }
        return dataStruct;
    }

    private Double generateVoltage()
    {
        batteryVoltage = batteryVoltage - 0.1;
        if (batteryVoltage <= 0)
        {
            batteryVoltage = 8.0;
        }
        return batteryVoltage;
    }

    @Override
    public IPrePersistRaptorDataMessage makeDataMessage(IRaptorDataStructure struct, IMapObject mapObject)
    {
        if (struct == null) struct = makeDataStructure();

        IPrePersistRaptorDataMessage dataMsg = struct.createRaptorDataMessage(myDeviceId);

        dataMsg.setSourceType(CommServiceType.FILE);
        dataMsg.setSourceName("TESTING");

        ILocation location = generateLocation();
        dataMsg.setLocation(location.getLatitude(), location.getLongitude(), location.getElevation());

        logger.trace("BH{} heading, pitch, roll, speed=[{},{},{},{}]", instanceNum_, heading, pitch, roll, speed);

        dataMsg.setOrientation(heading, pitch, roll);
        dataMsg.setSpeedInMps(speed);
        dataMsg.setHasMotion(true);

        // Update the 3D orientation and speed values
        pitch += pitchIncrement;
        if (pitch > 90)
        {
            pitch = 90.0;
            pitchIncrement = -20.0;
        } else if (pitch < -90)
        {
            pitch = -90.0;
            pitchIncrement = 20.0;
        }
        heading = (heading + 10.0) % 360;
        roll = (roll + 10.0) % 360;

        speed += 100;

        if (fixedTime == -1)
        {
            dataMsg.setTime(System.currentTimeMillis());
        } else if (fixedTime > 0)
        {
            dataMsg.setReceiveTime(fixedTime);
            dataMsg.setTime(fixedTime - 10);
        }
        /* else, do nothing, leave the time empty */

        dataMsg.setString("key", "mykey");
        dataMsg.setInt("devType", 42);
        dataMsg.setBoolean("visible", true);
        dataMsg.setInt("direction", 0);
        dataMsg.setInt("style", 42);
        dataMsg.setFloat("batt", 42.0f);
        dataMsg.setDouble("Voltage", generateVoltage());
        dataMsg.setDouble("CURRENT", 42.0D);
        dataMsg.setDouble("TEMPERATURE", 42D);
        dataMsg.setBoolean("GPS_POWER", true);
        dataMsg.setBoolean("FREEWAVE_POWER", true);
        dataMsg.setBoolean("ACCEL_POWER", true);
        dataMsg.setDouble("ACCEL_X", 42.0f);
        dataMsg.setDouble("ACCEL_Y", 42.0f);
        dataMsg.setDouble("ACCEL_Z", 42.0f);
        dataMsg.setString("Fix", "myfix");
        dataMsg.setInt("Height", 42);
        dataMsg.setBoolean("Tilt", true);
        panic = !panic;
        dataMsg.setBoolean("Panic", panic);
        dataMsg.setString("MessageType", "BH_STATUS_MESSAGE");

        dataMsg.setMapObject(mapObject);

        return dataMsg;
    }
}
