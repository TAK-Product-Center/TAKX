package gov.takx.api.io;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;

import java.io.IOException;
import java.io.Writer;

/**
 * Interface for objects that write TAKX data in a certain format. IRaptorEmitters allow different parts of the
 * serialized data stream to be sent to different writers. This is useful, for example, for implementing a rolling
 * buffer where you want to keep a static header and footer, but change the body.
 * <p/>
 * Some formats may wish to keep different map objects in different sections of the document (for example, an GeoRSS
 * feed may be implemented with each map object in a separate channel). To support these cases this interface provides
 * {@link #writeMapObjectHeader} and {@link #writeMapObjectFooter}. This allows the client to separate each map object
 * into its own output channel.
 *
 */

public interface IRaptorEmitter
{

    /**
     * Write the document header.
     */
    void writeHeader(Writer out) throws IOException;

    /**
     * Write the header for a section specific to one map object. Some
     * formats may wish to separate each map object into it's own
     * section of the document.
     *
     * @param mapObject The Map Object
     * @param out       Writer to receive the output
     */
    void writeMapObjectHeader(IMapObject mapObject, Writer out) throws IOException;

    /**
     * Write the footer for a map object section.
     *
     * @param mapObject The Map Object
     * @param out       Writer to receive the output
     */
    void writeMapObjectFooter(IMapObject mapObject, Writer out) throws IOException;

    /**
     * Write a data message.
     *
     * @param msg Message to write.
     * @param out Writer to receive output.
     */
    void write(IRaptorDataMessage msg, Writer out) throws IOException;

    /**
     * Write the document footer.
     */
    void writeFooter(Writer out) throws IOException;
}
