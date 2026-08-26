package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

/**
 * Represents a packet of data (non-audio or video, e.g., subtitles)
 *
 * @since 2.8
 */
@Experimental
public interface IDataFrame
{
    String getDataFormat();

    byte[] getData();
}
