package gov.takx.platform.io;

import gov.takx.api.io.IRaptorEmitter;
import gov.takx.api.mapobject.IMapObject;

import java.io.IOException;
import java.io.Writer;

/**
 * Empty implementation of an Import/Export emitter.
 *
 */
public abstract class DefaultEmitter implements IRaptorEmitter
{
    @Override
    public void writeHeader(Writer out) throws IOException
    {
    }

    @Override
    public void writeMapObjectHeader(IMapObject mapObject, Writer out)
    {
    }

    @Override
    public void writeMapObjectFooter(IMapObject mapObject, Writer out)
    {
    }

    @Override
    public void writeFooter(Writer out)
    {
    }
}
