package gov.takx.dataservices.filesystem;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IFileGeoCoder;

import java.io.File;

/**
 */
public abstract class AFileGeoCoder implements IFileGeoCoder
{
    @Override
    public ILocation geoDecode(File file)
    {
        return null;
    }

    @Override
    public boolean geoEncode(File file, ILocation location)
    {
        return false;
    }
}
