package gov.tak.core.comms.commo;

import com.atakmap.commoncommo.CoTPointData;
import com.atakmap.commoncommo.MissionPackageIO;
import com.atakmap.commoncommo.MissionPackageReceiveStatusUpdate;
import com.atakmap.commoncommo.MissionPackageSendStatusUpdate;
import com.atakmap.commoncommo.MissionPackageTransferException;
import com.atakmap.commoncommo.MissionPackageTransferStatus;
import gov.tak.api.location.ILocationProviderManager;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.NodeUUID;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.util.UUID;

/**
 * Implementation of {@link MissionPackageIO} for {@link CommonCommoManager} to enable support for
 * sending and receiving mission (data) packages.
 *
 * @since 3.5
 */
@ApplicationScoped
public class MissionPackageIOHandler implements MissionPackageIO
{
    private static final CoTPointData ZERO_POS = new CoTPointData(0, 0);

    private final ILocationProviderManager locationProvider;
    private final String nodeUuid;

    private PackageReceiver receiver;

    @Inject
    MissionPackageIOHandler(ILocationProviderManager locationProvider, @NodeUUID UUID nodeUuid)
    {
        this.locationProvider = locationProvider;
        this.nodeUuid = nodeUuid.toString();
    }

    /**
     * @param receiver Receiver implementation to handle where to place mission packages sent to our client.
     */
    public void setReceiver(@Nonnull PackageReceiver receiver)
    {
        this.receiver = receiver;
    }

    @Override
    public File missionPackageReceiveInit(String fileName, String transferName, String sha256hash, long sizeInBytes, String senderCallsign) throws MissionPackageTransferException
    {
        if (receiver == null) throw new MissionPackageTransferException(MissionPackageTransferStatus.FINISHED_DISABLED_LOCALLY);
        return receiver.handlePackageReceive(fileName, sha256hash, sizeInBytes, senderCallsign);
    }

    @Override
    public void missionPackageReceiveStatusUpdate(MissionPackageReceiveStatusUpdate update)
    {
        if (receiver != null) receiver.handlePackageReceiveStatus(update);
    }

    @Override
    public void missionPackageSendStatusUpdate(MissionPackageSendStatusUpdate update)
    {
        // no-op
    }

    @Override
    public CoTPointData getCurrentPoint()
    {
        GeoPointMetaData currentLocation = locationProvider.getCurrentLocation();
        if (currentLocation == null) return ZERO_POS;

        GeoPoint point = currentLocation.get();
        return new CoTPointData(point.getLatitude(), point.getLongitude(), point.getAltitude(), point.getCE(), point.getLE());
    }

    @Override
    public String createUUID()
    {
        return nodeUuid;
    }

    /**
     * Handler to support receiving mission packages from other TAK clients.
     */
    public interface PackageReceiver
    {
        /**
         * @param fileName Name of the file being sent.
         * @param sha256hash Expected SHA-256 hash of file being sent.
         * @param sizeBytes Size in bytes of the file.
         * @param senderCallsign Callsign of sender giving us the file.
         *
         * @return Local file path to store the file at. Must not be {@code null}.
         *
         * @throws MissionPackageTransferException Used to indicate to common commo alternative handling for the download.
         */
        @Nonnull
        File handlePackageReceive(@Nonnull String fileName, @Nonnull String sha256hash, long sizeBytes, @Nonnull String senderCallsign) throws MissionPackageTransferException;

        /**
         * @param update Status update on an existing package transfer.
         */
        void handlePackageReceiveStatus(@Nonnull MissionPackageReceiveStatusUpdate update);
    }
}
