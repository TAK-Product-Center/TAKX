package gov.takx.core.surfaces;

import gov.nasa.worldwind.util.BufferWrapper;
import gov.takx.api.surfaces.IBufferWrapper;

import java.nio.Buffer;

/**
 * Wrapper around the WW BufferWrapper exposed through the IBufferWrapper interface.  This design protects the plugin
 * developer from having to be exposed to any WorldWind classes.
 *
 * @since 1.4
 */
public class SurfaceBuffer implements IBufferWrapper
{
    private BufferWrapper buffer;

    public SurfaceBuffer(BufferWrapper buffer)
    {
        this.buffer = buffer;
    }

    @Override
    public int length()
    {
        return buffer.length();
    }

    @Override
    public int getGLDataType()
    {
        return buffer.getGLDataType();
    }

    @Override
    public long getSizeInBytes()
    {
        return buffer.getSizeInBytes();
    }

    @Override
    public byte getByte(int i)
    {
        return buffer.getByte(i);
    }

    @Override
    public void putByte(int i, byte b)
    {
        buffer.putByte(i, b);
    }

    @Override
    public short getShort(int i)
    {
        return buffer.getShort(i);
    }

    @Override
    public void putShort(int i1, short i2)
    {
        buffer.putShort(i1, i2);
    }

    @Override
    public int getInt(int i)
    {
        return buffer.getInt(i);
    }

    @Override
    public void putInt(int i1, int i2)
    {
        buffer.putInt(i1, i2);
    }

    @Override
    public float getFloat(int i)
    {
        return buffer.getFloat(i);
    }

    @Override
    public void putFloat(int i, float v)
    {
        buffer.putFloat(i, v);
    }

    @Override
    public double getDouble(int i)
    {
        return buffer.getDouble(i);
    }

    @Override
    public void putDouble(int i, double v)
    {
        buffer.putDouble(i, v);
    }

    @Override
    public void getByte(int i, byte[] bytes, int i1, int i2)
    {
        buffer.getByte(i, bytes, i1, i2);
    }

    @Override
    public void putByte(int i, byte[] bytes, int i1, int i2)
    {
        buffer.putByte(i, bytes, i1, i2);
    }

    @Override
    public void getShort(int i, short[] shorts, int i1, int i2)
    {
        buffer.getShort(i, shorts, i1, i2);
    }

    @Override
    public void putShort(int i, short[] shorts, int i1, int i2)
    {
        buffer.putShort(i, shorts, i1, i2);
    }

    @Override
    public void getInt(int i, int[] ints, int i1, int i2)
    {
        buffer.getInt(i, ints, i1, i2);
    }

    @Override
    public void putInt(int i, int[] ints, int i1, int i2)
    {
        buffer.putInt(i, ints, i1, i2);
    }

    @Override
    public void getFloat(int i, float[] floats, int i1, int i2)
    {
        buffer.getFloat(i, floats, i1, i2);
    }

    @Override
    public void putFloat(int i, float[] floats, int i1, int i2)
    {
        buffer.putFloat(i, floats, i1, i2);
    }

    @Override
    public void getDouble(int i, double[] doubles, int i1, int i2)
    {
        buffer.getDouble(i, doubles, i1, i2);
    }

    @Override
    public void putDouble(int i, double[] doubles, int i1, int i2)
    {
        buffer.putDouble(i, doubles, i1, i2);
    }

    @Override
    public Buffer getBackingBuffer()
    {
        return buffer.getBackingBuffer();
    }
}
