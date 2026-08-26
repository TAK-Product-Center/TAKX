package gov.takx.dataservices.persist.upgraders;

/**
 * Upgrade from DB version 29 to 30.  Changes include:
 * <ul>
 *     <li>Add Track table</li>
 *     <li>Add Breadcrumb table</li>
 * </ul>
 * @since 5.3
 */
public class UpgradeToV30 extends AUpgrader
{
    @Override
    public void doUpgrade() throws UpgraderException
    {
        createTrackTable();
        createBreadcrumbTable();
    }

    /**
     * Creates the Track table and adds a uniqueness constraint on UID
     */
    private void createTrackTable()
    {
        // Create table
        executeUpdate("""
                create table Track (
                        id serial not null,
                        breadcrumbCount integer not null,
                        callsign text,
                        color integer not null,
                        elevationGain float(53) not null,
                        elevationLoss float(53) not null,
                        endPoint_access smallint,
                        endPoint_altitude float(53) not null,
                        endPoint_altitudeReference smallint,
                        endPoint_ce90 float(53) not null,
                        endPoint_latitude float(53) not null,
                        endPoint_le90 float(53) not null,
                        endPoint_longitude float(53) not null,
                        endTime timestamp(6) with time zone not null,
                        geoBoundsDegreesAndAlt float(53) array,
                        lastElevation float(53) not null,
                        markerUid varchar(255) not null,
                        maxElevationPoint_access smallint,
                        maxElevationPoint_altitude float(53) not null,
                        maxElevationPoint_altitudeReference smallint,
                        maxElevationPoint_ce90 float(53) not null,
                        maxElevationPoint_latitude float(53) not null,
                        maxElevationPoint_le90 float(53) not null,
                        maxElevationPoint_longitude float(53) not null,
                        maxSpeed float(53) not null,
                        maxSpeedPoint_access smallint,
                        maxSpeedPoint_altitude float(53) not null,
                        maxSpeedPoint_altitudeReference smallint,
                        maxSpeedPoint_ce90 float(53) not null,
                        maxSpeedPoint_latitude float(53) not null,
                        maxSpeedPoint_le90 float(53) not null,
                        maxSpeedPoint_longitude float(53) not null,
                        minElevationPoint_access smallint,
                        minElevationPoint_altitude float(53) not null,
                        minElevationPoint_altitudeReference smallint,
                        minElevationPoint_ce90 float(53) not null,
                        minElevationPoint_latitude float(53) not null,
                        minElevationPoint_le90 float(53) not null,
                        minElevationPoint_longitude float(53) not null,
                        name text not null,
                        speedSum float(53) not null,
                        speedValueCount integer not null,
                        startPoint_access smallint,
                        startPoint_altitude float(53) not null,
                        startPoint_altitudeReference smallint,
                        startPoint_ce90 float(53) not null,
                        startPoint_latitude float(53) not null,
                        startPoint_le90 float(53) not null,
                        startPoint_longitude float(53) not null,
                        startTime timestamp(6) with time zone not null,
                        style varchar(255),
                        totalDistance float(53) not null,
                        uid varchar(255) not null,
                        primary key (id)
                    )
                """);

        // Add uniqueness constraint on uid
        addUniqueConstraint("Track", new String[]{"uid"}, "track_uid");
    }

    private void createBreadcrumbTable()
    {
        // Create table
        executeUpdate("""
                create table Breadcrumb (
                   id serial not null,
                    bearing float(53),
                    geoPointMetaData_geopoint_access smallint,
                    geoPointMetaData_geopoint_altitude float(53) not null,
                    geoPointMetaData_geopoint_altitudeReference smallint,
                    geoPointMetaData_geopoint_ce90 float(53) not null,
                    geoPointMetaData_geopoint_latitude float(53) not null,
                    geoPointMetaData_geopoint_le90 float(53) not null,
                    geoPointMetaData_geopoint_longitude float(53) not null,
                    geoPointMetaData_metadata text,
                    speed float(53),
                    time timestamp(6) with time zone not null,
                    trackUid varchar(255) not null,
                    uid varchar(255) not null,
                    primary key (id)
                )
                """);

        // Add uniqueness constraint on uid
        addUniqueConstraint("Breadcrumb", new String[]{"uid"}, "breadcrumb_uid");
    }
}
