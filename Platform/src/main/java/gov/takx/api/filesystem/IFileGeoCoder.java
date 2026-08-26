package gov.takx.api.filesystem;

import gov.takx.api.coordinatesystem.ILocation;

import java.io.File;

/**
 * A object to extract a geographic location from a file, or write a location into a file. For example, a geocoder
 * may inspect a file to see if it is an image with geographic metadata.
 *
 * @since 1.3
 */
public interface IFileGeoCoder
{
    /**
     * Attempt to geodecode a file. If geocoding is successful, return the location of the file.
     *
     * @param file A file to geocode.
     * @return The location decoded from the file, or {@code null} if no decoding was not successful.
     */
    ILocation geoDecode(File file);

    /**
     * Attempt to encode a location into a file.
     *
     * @param file     The file to encode a location into
     * @param location The location
     * @return true if the location was encoded into the file
     */
    boolean geoEncode(File file, ILocation location);
}
