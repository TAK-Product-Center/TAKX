package gov.takx.core.project;

import gov.takx.api.project.IProjectMapObjectMetadata;
import gov.takx.api.project.IProjectMetadata;
import gov.takx.commons.utils.PlatformVersion;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 */
public class ProjectMetadata implements IProjectMetadata
{
    private UUID projectUuid;
    private String projectName;
    private String title;
    private boolean isPackage;
    private PlatformVersion platformVersion;
    private List<IProjectMapObjectMetadata> mapObjects;
    private long creationDate;
    private long startDate;
    private long endDate;
    private String size;
    private String vfsSize;
    private String description;
    private String location;
    private String notes;
    private boolean wmsOfflineMode;
    private String dbType;
    private String dbVersion;

    @Override
    public UUID getProjectUUID()
    {
        return projectUuid;
    }

    public void setProjectUuid(UUID projectUuid)
    {
        this.projectUuid = projectUuid;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Override
    public long getCreationDate()
    {
        return creationDate;
    }

    public void setCreationDate(long creationDate)
    {
        this.creationDate = creationDate;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setEndDate(long endDate)
    {
        this.endDate = endDate;
    }

    public void setPackage(boolean aPackage)
    {
        isPackage = aPackage;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public void setMapObjects(List<IProjectMapObjectMetadata> mapObjects)
    {
        this.mapObjects = Collections.unmodifiableList(mapObjects);
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    public void setPlatformVersion(PlatformVersion platformVersion)
    {
        this.platformVersion = platformVersion;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public void setSize(String size)
    {
        this.size = size;
    }

    public void setStartDate(long startDate)
    {
        this.startDate = startDate;
    }

    public void setVfsSize(String vfsSize)
    {
        this.vfsSize = vfsSize;
    }

    public void setWmsOfflineMode(boolean wmsOfflineMode)
    {
        this.wmsOfflineMode = wmsOfflineMode;
    }

    public void setDbType(String dbType)
    {
        this.dbType = dbType;
    }

    public void setDbVersion(String dbVersion)
    {
        this.dbVersion = dbVersion;
    }

    @Override
    public boolean isPackage()
    {
        return isPackage;
    }

    @Override
    public String getProjectName()
    {
        return projectName;
    }

    @Override
    public List<IProjectMapObjectMetadata> getMapObjects()
    {
        return Collections.unmodifiableList(mapObjects);
    }

    @Override
    public PlatformVersion getPlatformVersion()
    {
        return platformVersion;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public long getEndDate()
    {
        return endDate;
    }

    @Override
    public String getLocation()
    {
        return location;
    }

    @Override
    public String getNotes()
    {
        return notes;
    }

    @Override
    public String getSize()
    {
        return size;
    }

    @Override
    public long getStartDate()
    {
        return startDate;
    }

    @Override
    public String getVfsSize()
    {
        return vfsSize;
    }

    @Override
    public boolean getWmsOfflineMode()
    {
        return wmsOfflineMode;
    }

    @Override
    public String getDBType()
    {
        return dbType;
    }

    @Override
    public String getDBVersion()
    {
        return dbVersion;
    }
}
