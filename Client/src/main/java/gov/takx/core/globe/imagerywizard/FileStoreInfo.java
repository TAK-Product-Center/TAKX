/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public class FileStoreInfo extends AVListImpl
{
    private final String location;
    private final boolean isDefaultFileStore;
    private final ArrayList<ImportedDataSet> dataSets = new ArrayList<>();

    public FileStoreInfo(String location, boolean defaultFileStore)
    {
        this.location = location;
        isDefaultFileStore = defaultFileStore;
        setValue(AVKey.FILE_STORE_LOCATION, this.location);
        setValue(InstallConstants.DEFAULT_FILE_STORE, isDefaultFileStore);
    }

    public String getLocation()
    {
        return location;
    }

    public boolean isDefaultFileStore()
    {
        return isDefaultFileStore;
    }

    public List<ImportedDataSet> getDataSets()
    {
        return Collections.unmodifiableList(dataSets);
    }

    public void setDataSets(Iterable<? extends ImportedDataSet> dataSets)
    {
        this.dataSets.clear();
        if (dataSets != null)
        {
            for (ImportedDataSet ds : dataSets) this.dataSets.add(ds);
        }

        firePropertyChange(InstallConstants.FILE_STORE_INFO, null, this);
    }

    public String toString()
    {
        return "fileStore=" + location;
    }
}
