package gov.takx.core.mapdata;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.globe.render.IRendererSearchResult;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.StandardTimeFormatting;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Table model for holding a collection of {@link IRaptorDataMessage}s selected from a given geographic region.  The total
 * possible number of messages in the region is available since use of this model typically limits the number of
 * messages displayed to some reasonable limit.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class RaptorDataMessageTableModel extends AbstractTableModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Monitors MONITORS = new Monitors();

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private StandardTimeFormatting timeFormatting;

    public enum Columns
    {
        Time, Description, Location, Type,
        ID, Heading, Speed, Latitude, Longitude,
        Elevation, Received;

        public static final Columns[] VALUES = values();
    }

    protected List<IRaptorDataMessage> dataVector;
    protected long possibleMessageCount;
    protected Sector bounds;
    private AltitudeUnits altitudeUnits;

    /**
     * Construct the table model with the count of messages that will be displayed and the total number
     * of messages that could have been displayed in the given region.
     *
     * @param result Data query result.
     * @param bounds region this model holds data for
     */
    public RaptorDataMessageTableModel(IRendererSearchResult result, Sector bounds)
    {
        dataVector = new ArrayList<>(result.getMapPointCount());
        possibleMessageCount = result.getTotalMapPointCount();
        this.bounds = bounds;

        for (List<IRaptorDataMessage> messages : result.getData().values()) addMessages(messages);
    }

    /**
     * Update the altitude units being used to display the data in this model.
     *
     * @param altitudeUnits The new altitude units
     * @since 5.5 (signature updated)
     */
    public void updateAltitudeUnits(@Nonnull AltitudeUnits altitudeUnits)
    {
        this.altitudeUnits = altitudeUnits;
    }

    /**
     * @return the number of messages in the model
     */
    public int getMessageCount()
    {
        return dataVector.size();
    }

    /**
     * @return the total possible count of messages in the region
     */
    public long getPossibleMessageCount()
    {
        return possibleMessageCount;
    }

    /**
     * @return the min corner of the region
     */
    public LatLon getMinCorner()
    {
        return bounds.getCorners()[0]; // SW corner
    }

    /**
     * @return the max corner of the region
     */
    public LatLon getMaxCorner()
    {
        return bounds.getCorners()[2]; // NE corner
    }

    /**
     * Add messages to the table model.
     *
     * @param messages The messages to add
     */
    public void addMessages(Collection<IRaptorDataMessage> messages)
    {
        int origCount = dataVector.size();
        dataVector.addAll(messages);
        fireTableRowsInserted(origCount, dataVector.size());
    }

    @Override
    public String getColumnName(int column)
    {
        return Columns.VALUES[column].toString();
    }

    @Override
    public Class<?> getColumnClass(int column)
    {
        return switch (Columns.VALUES[column])
        {
            case Heading, Latitude, Location, Longitude, Description, ID, Type, Received, Time -> String.class;
            case Elevation, Speed -> Double.class;
        };
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        IRaptorDataMessage msg = dataVector.get(row);
        return switch (Columns.VALUES[column])
        {
            case Description ->
            {
                final var mapObject = mapObjectManager.getMapObject(msg.getUniqueKey());
                if (mapObject != null)
                {
                    final String name = mapObject.getName();
                    if (name != null) yield name;
                }
                logger.error("Unable to get map object from RDM");
                yield "Unknown";
            }
            case Type -> msg.getType();
            case ID -> msg.getUnitID();
            case Time -> msg.hasTime() ? timeFormatting.formatWithCurrent(msg.getTime()) : "Unknown";
            case Location ->
            {
                String location = "Unknown";
                if (msg.hasLocation())
                {
                    CoordinateSystem coordSys = MONITORS.coordinateSystemMonitor.getCurrentValue();
                    location = altitudeUnits == null
                            ? CoordinateSystem.getLocationString(msg.getLocation(), coordSys)
                            : CoordinateSystem.getLocationString(msg.getLocation(), coordSys, false) + " " // Lat/Lon
                                    + altitudeUnits.format(msg.getElevation(), 2); // Altitude
                }
                yield location;
            }
            case Heading -> msg.hasHeading() ? "" + msg.getHeading() : "Unknown";
            case Speed -> msg.hasSpeed() ? msg.getSpeedInMps() : null;
            case Latitude -> msg.hasLocation() ? CoordinateSystem.getLatitudeStringInDecimalDegrees(msg.getLatitude()) : "Unknown";
            case Longitude -> msg.hasLocation() ? CoordinateSystem.getLongitudeStringInDecimalDegrees(msg.getLongitude()) : "Unknown";
            case Elevation -> msg.hasElevation() ? msg.getElevation() : null;
            case Received -> timeFormatting.formatWithDefault(msg.getReceiveTime());
        };
    }

    @Override
    public int getRowCount()
    {
        return dataVector.size();
    }

    @Override
    public int getColumnCount()
    {
        return Columns.VALUES.length;
    }

    /**
     * Holds preference monitors to prevent repeated calls to {@link gov.takx.api.preferences.ITypedPreferenceValue#getValueAs(Class)}
     * and the subsequent String -> Object conversions.
     *
     * @since 5.5
     */
    private static class Monitors
    {
        private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, u -> {
        });
    }
}
