package gov.tak.core.comms.takserver.mission;

import com.atakmap.filesystem.HashingUtils;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.core.comms.takserver.DataSyncManager;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.time.Instant;

/**
 * Data-sync component for tracking local mission resources.
 *
 * @since 5.7
 */
public class MissionResources
{
    private final File missionsDir;
    private final File filesDir;

    MissionResources(@Nonnull DataSyncManager dataSyncManager, @Nonnull ITakServerConnection connection)
    {
        // We need to use a directory name for the server that is:
        //  - Unique per-server, but consistent
        //  - Unique per-user on a given server, but consistent
        //  - Safe for use on all operating systems (no illegal chars, banned names)
        // Thus hashing the data-sync key for the connection works well
        //  - Key is uri:username
        //  - Hashing the key is thus unique per-user/per-server (but consistent)
        //  - SHA1 hashes are safe file names on all systems
        String connectionKey = DataSyncManager.key(connection);
        String serverDirectoryName = HashingUtils.sha1sum(connectionKey);
        File baseServerDir = new File(dataSyncManager.getRootDir(), serverDirectoryName);
        missionsDir = new File(baseServerDir, "missions");
        filesDir = new File(baseServerDir, "files");
    }

    /**
     * This is the root directory where data-sync files are stored.
     * There is a subdirectory for each resource, where the directory name is the
     * sha256 hash of the resource. The subdirectory will contain the actual file.
     *
     * @return Directory containing data-sync files.
     */
    @Nonnull
    protected File getFilesDir()
    {
        return filesDir;
    }

    /**
     * This is the root directory where data-sync mission models are stored.
     *
     * @return Directory containing data-sync mission models.
     */
    @Nonnull
    protected File getMissionsDir()
    {
        return missionsDir;
    }

    /**
     * @param sha256 Sha256 hash of file to look up.
     * @return Local file path to resource, if found.
     */
    @Nullable
    public File findResourceFileByHash(@Nonnull String sha256)
    {
        File dir = new File(filesDir, sha256);
        File[] files = dir.listFiles();
        if (files != null && files.length == 1) return files[0];
        return null;
    }

    /**
     * @param resource Resource to get local file path of.
     * @return Local file path to resource.
     */
    @Nonnull
    public File resourceFile(@Nonnull Resource resource)
    {
        return resourceFileByHashAndName(resource.hash(), resource.name());
    }

    /**
     * @param sha256 Sha256 hash of file to get path of.
     * @param name Name of file to get path of.
     * @return Local file path to resource.
     */
    @Nonnull
    public File resourceFileByHashAndName(@Nonnull String sha256, @Nonnull String name)
    {
        return new File(filesDir, sha256 + "/" + name);
    }

    /**
     * @param cot CoT outline to get local file path of.
     * @return Local file path to CoT XML.
     */
    @Nonnull
    public File cotFile(@Nonnull MissionAdd<String> cot)
    {
        return cotFile(cot.data());
    }

    /**
     * @param uid UID of CoT item to get local file path of.
     * @return Local file path to CoT XML.
     */
    @Nonnull
    public File cotFile(@Nonnull String uid)
    {
        final String name = FileUtils.sanitizeForFilename(uid);
        return new File(filesDir, "cot/" + name + ".cot");
    }

    /**
     * @param resource Resource to check for.
     * @return {@code true} if it is locally downloaded.
     */
    public boolean hasLocalResource(@Nonnull Resource resource)
    {
        return resourceFile(resource).exists();
    }

    /**
     * @param cot CoT event to check for.
     * @return {@code true} if it is locally downloaded.
     */
    public boolean hasLocalUpToDateCot(@Nonnull MissionAdd<String> cot)
    {
        return hasLocalUpToDateCot(cot.data(), cot.timestamp());
    }

    /**
     * @param uid          UID of CoT item to check local file path of.
     * @param upToDateTime Timestamp of expected up-to-date CoT.
     * @return {@code true} when we have a local CoT file that has been modified at or after the up-to-date time.
     */
    public boolean hasLocalUpToDateCot(@Nonnull String uid, @Nonnull Instant upToDateTime)
    {
        // The local cot file should be up-to-date with the new cot item defined in the mission-add.
        // Otherwise, our local file is out of date and should be disregarded.
        File localCot = cotFile(uid);
        if (!localCot.exists()) return false;

        // We shift our local time forward a bit as a basic same-time deconfliction mechanism.
        // The idea is that we have the following control flow:
        //  - We have some map-item update
        //  - We submit an update to the server (REST request)
        //  - We get an acknowledgement from the server that our update was received (REST response)
        //  - We record the current local CoT of what we sent to the local cot path
        //  - The server accepts our update
        //  - The server sends out "hey the mission updated" to everyone (including us, the ones who made the update)
        //  - The update includes the timestamp that the update was acknowledged at on the server,
        //    which isn't going to be exactly the time we made the request, but a little later
        // So if we say our file modified date is "1 second ahead" of the actual time it was written, this
        // is a really naive way to accommodate for any network latency. It's not great per-say but will
        // prevent most redundant map-item updates. We update this file after the server gives us back the
        // acknowledgement, so this small amount should be fine.
        Instant localTime = Instant.ofEpochMilli(localCot.lastModified() + 1000);
        return localCot.exists() && localTime.isAfter(upToDateTime);
    }

    /**
     * Clears local resource contents.
     */
    public void clear()
    {
        FileUtils.deleteDirectory(filesDir);
        FileUtils.deleteDirectory(missionsDir);
    }
}
