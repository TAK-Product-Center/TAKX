package gov.takx.api.surfaces;

import java.nio.Buffer;

/**
 * BufferWrapper provides an interface for reading and writing primitive data to and from data buffers, without having
 * to know the underlying data type.
 *
 * @since 1.4
 */
public interface IBufferWrapper
{
    /**
     * Returns the length of the buffer, in units of the underlying data type (e.g. bytes, shorts, ints, floats,
     * doubles).
     *
     * @return the buffer's length.
     */
    int length();

    /**
     * Returns the OpenGL data type corresponding to the buffer's underlying data type (e.g. GL_BYTE, GL_SHORT, GL_INT,
     * GL_FLOAT, GL_DOUBLE).
     *
     * @return the buffer's OpenGL data type.
     */
    int getGLDataType();

    /**
     * Returns the size of this buffer, in bytes.
     *
     * @return the buffer's size in bytes.
     */
    long getSizeInBytes();

    /**
     * Returns the value at the specified index, cast to a byte.
     *
     * @param index the index of the value to be returned.
     * @return the byte at the specified index.
     */
    byte getByte(int index);

    /**
     * Sets the value at the specified index as a byte. The byte is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the byte value to be set.
     */
    void putByte(int index, byte value);

    /**
     * Returns the value at the specified index, cast to a short.
     *
     * @param index the index of the value to be returned.
     * @return the short at the specified index.
     */
    short getShort(int index);

    /**
     * Sets the value at the specified index as a short. The short is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the short value to be set.
     */
    void putShort(int index, short value);

    /**
     * Returns the value at the specified index, cast to an int.
     *
     * @param index the index of the value to be returned.
     * @return the int at the specified index.
     */
    int getInt(int index);

    /**
     * Sets the value at the specified index as an int. The int is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the int value to be set.
     */
    void putInt(int index, int value);

    /**
     * Returns the value at the specified index, cast to a float.
     *
     * @param index the index of the value to be returned.
     * @return the float at the specified index.
     */
    float getFloat(int index);

    /**
     * Sets the value at the specified index as a float. The float is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the float value to be set.
     */
    void putFloat(int index, float value);

    /**
     * Returns the value at the specified index, cast to a double.
     *
     * @param index the index of the value to be returned.
     * @return the double at the specified index.
     */
    double getDouble(int index);

    /**
     * Sets the value at the specified index as a double. The double is cast to the underlying data type.
     *
     * @param index the index of the value to be returned.
     * @param value the double value to be set.
     */
    void putDouble(int index, double value);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to bytes.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    void getByte(int index, byte[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as bytes. The bytes
     * are cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    void putByte(int index, byte[] array, int offset, int length);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to shorts.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    void getShort(int index, short[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as ints. The ints are
     * cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    void putShort(int index, short[] array, int offset, int length);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to ints.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    void getInt(int index, int[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as ints. The ints are
     * cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    void putInt(int index, int[] array, int offset, int length);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to floats.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    void getFloat(int index, float[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as floats. The floats
     * are cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    void putFloat(int index, float[] array, int offset, int length);

    /**
     * Returns the sequence of values starting at the specified index and with the specified length, cast to doubles.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to get.
     */
    void getDouble(int index, double[] array, int offset, int length);

    /**
     * Sets the sequence of values starting at the specified index and with the specified length, as doubles. The
     * doubles are cast to the underlying data type.
     *
     * @param index  the buffer starting index.
     * @param array  the array.
     * @param offset the array starting index.
     * @param length the number of values to put.
     */
    void putDouble(int index, double[] array, int offset, int length);

    /**
     * Returns the buffer's backing data store. For the standard BufferWrapper types (ByteBufferWrapper,
     * ShortBufferWrapper, IntBufferWrapper, FloatBufferWrapper, and DoubleBufferWrapper), this returns the backing
     * {@link java.nio.Buffer}.
     *
     * @return the backing data store.
     */
    Buffer getBackingBuffer();
}
