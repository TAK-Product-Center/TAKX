package gov.takx.test.mock.plugin.device;

import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.device.ADataStructureFactory;
import gov.takx.api.plugin.device.ADevicePlugin;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.gateway.IGatewayFramer;
import gov.takx.api.plugin.gateway.IGatewayFramerDelegate;
import gov.takx.api.plugin.gateway.IGatewayParser;
import gov.takx.api.plugin.gateway.IGatewayParserDelegate;
import gov.takx.commons.constants.CommServiceType;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Base class for integration test plugin instantiations.
 *
 * @since 2.0
 */
public abstract class ATestDevicePlugin extends ADevicePlugin
{
    public static final String FAMILY = "gov.takx.TEST";
    public static final String DEFAULT_UNIT_ID = "1";
    public static final String STRUCTURE_NAME_SHARED = "TEST_STRUCTURE_SHARED";

    public static final String ALARM = "ALARM";
    public static final boolean ALARM_DEFAULT = false;

    public static final String BATTERY = "BATTERY";
    public static final double BATTERY_DEFAULT = 12.3;

    public static final String STATUS = "STATUS";
    public static final int STATUS_DEFAULT = 0;

    public static final String RAD = "RAD";
    public static final long RAD_DEFAULT = 100;

    public static final String SPECTRUM = "SPECTRUM";
    public static final float SPECTRUM_DEFAULT = 4.56F;

    public static final double STARTING_LATITUDE = 34.437;
    public static final double STARTING_LONGITUDE = -119.82;

    protected final Random random = new Random();

    protected final String structureName;

    protected double latitude = STARTING_LATITUDE;
    protected double longitude = STARTING_LONGITUDE;
    protected double elevation = 10.0;

    // Field values to inject into next message if non-null
    protected Boolean alarmValue = null;
    protected Integer statusValue = null;
    protected Double batteryValue = null;

    protected IRaptorDataStructure currentStructure;
    protected IRaptorDataStructure sharedStructure;

    // Supplier of currently configured structure
    protected Supplier<IRaptorDataStructure> currentStructureSupplier = null;

    // Custom factory to install configured structures
    private final IDataStructureFactory factory = new ADataStructureFactory()
    {
        @Override
        protected void createStructureMap()
        {
            sharedStructure = makeSharedStructure();
            defineDataStructure(STRUCTURE_NAME_SHARED, sharedStructure);

            if (currentStructureSupplier != null)
            {
                currentStructure = currentStructureSupplier.get();
                defineDataStructure(structureName, currentStructure);
            }
        }
    };

    protected ATestDevicePlugin(String structureName)
    {
        this.structureName = structureName;
    }

    @Override
    public IDataStructureFactory getDataStructureFactory()
    {
        return factory; // Return our custom factory
    }

    /**
     * Use configuration 1 of the plugin structures.
     */
    public void initStructures1()
    {
        currentStructureSupplier = this::makeStructure1;
    }

    /**
     * Use configuration 2 of the plugin structures.
     */
    public void initStructures2()
    {
        currentStructureSupplier = this::makeStructure2;
    }

    /**
     * Use configuration 3 of the plugin structures.
     */
    public void initStructures3()
    {
        currentStructureSupplier = this::makeStructure3;
    }

    /**
     * Create our shared structure.
     */
    private IRaptorDataStructure makeSharedStructure()
    {
        final DataStructureBuilder builder = new DataStructureBuilder(getFamily(), getType(), "1", STRUCTURE_NAME_SHARED);

        // Add in some simple fields
        builder.addInt(STATUS, STATUS_DEFAULT);
        builder.addLong(RAD, RAD_DEFAULT);
        builder.addFloat(SPECTRUM, SPECTRUM_DEFAULT);

        return builder.build();
    }

    /**
     * Initialize data structure version 1, add basic fields.
     */
    private IRaptorDataStructure makeStructure1()
    {
        final DataStructureBuilder builder = new DataStructureBuilder(getFamily(), getType(), "1", structureName);

        builder.addBoolean(ALARM, ALARM_DEFAULT);
        builder.addDouble(BATTERY, BATTERY_DEFAULT);
        builder.addInt(STATUS, STATUS_DEFAULT);

        return builder.build();
    }

    /**
     * Initialize data structure version 2. In this version we have changed the fields, but not the version.
     */
    private IRaptorDataStructure makeStructure2()
    {
        final DataStructureBuilder builder = new DataStructureBuilder(getFamily(), getType(), "1", structureName);

        builder.addBoolean(ALARM, ALARM_DEFAULT);
        builder.addDouble(BATTERY, BATTERY_DEFAULT);
        builder.addInt(STATUS, STATUS_DEFAULT);
        builder.addLong(RAD, RAD_DEFAULT);

        return builder.build();
    }

    /**
     * Initialize data structure version 3. Here we change both the fields and the version.
     */
    private IRaptorDataStructure makeStructure3()
    {
        final DataStructureBuilder builder = new DataStructureBuilder(getFamily(), getType(), "3", structureName);

        builder.addBoolean(ALARM, ALARM_DEFAULT);
        builder.addDouble(BATTERY, BATTERY_DEFAULT);
        builder.addInt(STATUS, STATUS_DEFAULT);
        builder.addLong(RAD, RAD_DEFAULT);
        builder.addFloat(SPECTRUM, SPECTRUM_DEFAULT);

        return builder.build();
    }

    /**
     * Set the battery value to be used on the next message generated.
     *
     * @param batteryValue Value to use
     */
    public void setBatteryValue(double batteryValue)
    {
        this.batteryValue = batteryValue;
    }

    /**
     * Set the alarm value to be used on the next message generated.
     *
     * @param alarmValue Value to use
     */
    public void setAlarmValue(boolean alarmValue)
    {
        this.alarmValue = alarmValue;
    }

    /**
     * Set the status value to be used on the next message generated.
     *
     * @param statusValue Value to use
     */
    public void setStatusValue(int statusValue)
    {
        this.statusValue = statusValue;
    }

    /**
     * Generate an RDM based on the current data structure using the default Unit ID.
     * Each message generated will have a varying location.
     */
    public IPrePersistRaptorDataMessage generateMessage()
    {
        return generateMessage(DEFAULT_UNIT_ID);
    }

    /**
     * Generate an RDM based on the current data structure using the specified Unit ID.
     * Each message generated will have a varying location.
     *
     * @param unitId Unit Id to use on the message
     */
    public IPrePersistRaptorDataMessage generateMessage(String unitId)
    {
        return fillOutRdm(currentStructure.createRaptorDataMessage(unitId));
    }

    /**
     * Generate a new RDM from the shared structure.
     */
    public IPrePersistRaptorDataMessage generateSharedMessage()
    {
        return generateSharedMessage(getFamily(), getType());
    }

    /**
     * Generate a new RDM from the shared structure, but using a different family and type.
     */
    public IPrePersistRaptorDataMessage generateSharedMessage(String family, String type)
    {
        return fillOutRdm(sharedStructure.createRaptorDataMessage(family, type, DEFAULT_UNIT_ID));
    }

    /**
     * Fill out an RDM with the configured data.
     *
     * @param rdm The rdm to populate
     * @return The given rdm
     */
    private IPrePersistRaptorDataMessage fillOutRdm(IPrePersistRaptorDataMessage rdm)
    {
        rdm.setSourceName(getClass().getSimpleName());
        rdm.setLocation(latitude, longitude, elevation);
        rdm.setOrientation(10.0, 20.0, 30.0);
        rdm.setSpeedInMps(42.0);

        if (alarmValue != null) rdm.setBoolean(ALARM, alarmValue);
        if (batteryValue != null) rdm.setDouble(BATTERY, batteryValue);
        if (statusValue != null) rdm.setInt(STATUS, statusValue);

        alarmValue = null; // Only use them once
        batteryValue = null;
        statusValue = null;

        updateLocation();

        return rdm;
    }

    /**
     * Update the location values in preparation for generating the next message.
     */
    private void updateLocation()
    {
        latitude = (latitude + (random.nextDouble() * .1)) % 90;
        longitude = (longitude + (random.nextDouble() * .1)) % 180;
        elevation = (elevation + 10) % 2000;
    }

    @Override
    public IGatewayParser createGatewayParser(CommServiceType sourceType, String sourceName, String format, IGatewayParserDelegate delegate)
    {
        return null;
    }

    @Override
    public IGatewayFramer createGatewayFramer(CommServiceType sourceType, String sourceName, String format, IGatewayFramerDelegate delegate)
    {
        return null;
    }
}
