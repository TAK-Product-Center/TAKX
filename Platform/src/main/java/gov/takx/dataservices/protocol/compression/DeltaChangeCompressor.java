package gov.takx.dataservices.protocol.compression;

/**
 * A data compressor that uses a "delta change" algorithm for arrays of longs.  This works well for spectra.
 * The extreme edge cases of delta values near Long.MAX_VALUE will result in a compressed result that is larger
 * (byte-wise) than the original input.  The consumer of this class should expect this and deal with it reasonably.
 *
 * @since 1.22
 */
public class DeltaChangeCompressor
{
    private final static int CONTROL_MASK = 0xC0;
    private final static int DATA_PART_MASK = 0x3F;

    private final static int DATA_BYTE_BIT = 0x00;
    private final static int ZERO_COUNT_BYTE_BIT = 0x40;
    private final static int BYTE_EXTENSION_BIT = 0x80;
    private final static int NIBBLE_BYTE_BIT = 0xC0;

    public DeltaChangeCompressor()
    {
    }

    /**
     * Uncompress a given buffer of data compressed by this algorithm into an array of longs.  The data must have
     * been compressed from an array of longs.
     *
     * @param compressedInput  The buffer of compressed data to uncompress
     * @param compressedLength The number of bytes of input to process (in case the input buffer is not full)
     * @param output           The buffer in which to place the results.  It must be sized large enough to hold the
     *                         uncompressed data.
     * @return The number of elements in the uncompressed data
     * @throws ArrayIndexOutOfBoundsException If the input or output buffer is overrun
     * @see #compressData(long[], byte[])
     */
    public int uncompressData(byte[] compressedInput, int compressedLength, long[] output) throws ArrayIndexOutOfBoundsException
    {
        int unCompressedIndex = 0;
        int nibbleValue;
        long extendedValue;
        int extensionLength;
        long currentValue = 0;

        for (int i = 0; i < compressedLength; i++)
        {
            switch ((compressedInput[i] & CONTROL_MASK))
            {
                case ZERO_COUNT_BYTE_BIT:
                {
                    if ((compressedInput[i] & DATA_PART_MASK) != DATA_PART_MASK)
                    {
                        for (int j = 0; j < (compressedInput[i] & DATA_PART_MASK); j++)
                        {
                            output[unCompressedIndex++] = currentValue;
                        }
                    } else
                    {
                        extendedValue = (compressedInput[++i] & 0xff);
                        extendedValue <<= 8;
                        extendedValue |= (compressedInput[++i] & 0xff);

                        for (int j = 0; j < extendedValue; j++)
                        {
                            output[unCompressedIndex++] = currentValue;
                        }
                    }
                    break;
                }
                case NIBBLE_BYTE_BIT:
                {
                    nibbleValue = (compressedInput[i] & 0x07);
                    if ((nibbleValue & 0x04) > 0)
                    {
                        nibbleValue -= 0x08;
                    }
                    currentValue += nibbleValue;
                    output[unCompressedIndex++] = currentValue;

                    nibbleValue = (compressedInput[i] & 0x38) >> 3;
                    if ((nibbleValue & 0x04) > 0)
                    {
                        nibbleValue -= 0x08;
                    }
                    currentValue += nibbleValue;
                    output[unCompressedIndex++] = currentValue;
                    break;
                }
                case DATA_BYTE_BIT:
                {
                    nibbleValue = (compressedInput[i] & DATA_PART_MASK);
                    if ((nibbleValue & 0x20) > 0)
                    {
                        nibbleValue -= 0x40;
                    }

                    currentValue += nibbleValue;
                    output[unCompressedIndex++] = currentValue;
                    break;
                }
                case BYTE_EXTENSION_BIT:
                {
                    extendedValue = 0;
                    extensionLength = 0;

                    do
                    {
                        extendedValue <<= 7;
                        extendedValue |= (compressedInput[i] & 0x7f);
                        extensionLength++;
                        i++;
                    } while ((compressedInput[i] & BYTE_EXTENSION_BIT) == BYTE_EXTENSION_BIT);

                    extendedValue <<= 7;
                    extendedValue |= (compressedInput[i] & 0x7f);

//                    System.out.println("extended value = " + extendedValue);
//                    System.out.println(String.format("%64s", Long.toBinaryString(extendedValue)).replace(' ', '0'));

                    int shift = Math.min(63, (extensionLength * 7 + 5));
//                    System.out.println("Shift=" + shift);
//                    System.out.println(String.format("%64s", Long.toBinaryString((0x1L << shift))).replace(' ', '0'));

                    if ((extendedValue & (0x1L << shift)) > 0)
                    {
                        extendedValue -= (0x1L << shift + 1);
                    }
                    currentValue += extendedValue;
                    output[unCompressedIndex++] = currentValue;

                    break;
                }
            }
        }

        return unCompressedIndex;
    }

    /**
     * Compress the data given into the supplied buffer of longs.  If the output buffer is not large enough an
     * ArrayIndexOutOfBoundsException wil be thrown.
     *
     * @param input  Array of longs to compress
     * @param output Array into which to write the compressed results
     * @return The length of the compressed data (in bytes)
     * @throws ArrayIndexOutOfBoundsException If the output buffer is overrun
     */
    public int compressData(long[] input, byte[] output) throws ArrayIndexOutOfBoundsException
    {
        int i = 0;
        long difference;
        long nextDifference;
        int zeroCounter = 0;
        int compressDataIndex = 0;

        while (i < input.length)
        {
            difference = i == 0 ? input[i] : input[i] - input[i - 1];
            nextDifference = i + 1 < input.length
                    ? input[i + 1] - input[i]
                    : -input[i];

            if (difference == 0)
            {
                zeroCounter++;
                if ((zeroCounter == 1) & (nextDifference != 0))
                {
                    if ((nextDifference > -4) && (nextDifference < 4))
                    {
                        output[compressDataIndex++] = (byte) (((nextDifference & 0x7) << 3) | NIBBLE_BYTE_BIT);
                        zeroCounter = 0;
                        i++;
                    } else
                    {
                        output[compressDataIndex++] = (byte) ((zeroCounter & DATA_PART_MASK) | ZERO_COUNT_BYTE_BIT);
                        zeroCounter = 0;
                    }
                } else if ((nextDifference != 0) || (zeroCounter >= 65535) || (i + 1 == input.length))
                {
                    if (zeroCounter > 62)
                    {
                        //if we have more than 62 zeros, we will overflow and use 3 bytes to store zeros. this will support upto 65535 zeros at a time
                        output[compressDataIndex++] = (byte) ((DATA_PART_MASK) | ZERO_COUNT_BYTE_BIT);
                        output[compressDataIndex++] = (byte) ((zeroCounter >> 8));
                        output[compressDataIndex++] = (byte) ((zeroCounter));
                    } else
                    {
                        output[compressDataIndex++] = (byte) ((zeroCounter & DATA_PART_MASK) | ZERO_COUNT_BYTE_BIT);
                    }
                    zeroCounter = 0;
                }
            } else if ((difference >= -4) && (difference < 4))
            {
                if (i + 1 < input.length && (nextDifference >= -4) && (nextDifference < 4))
                {
                    output[compressDataIndex++] = (byte) (((nextDifference & 0x7) << 3) | (difference & 0x7) | NIBBLE_BYTE_BIT);
                    i++;
                } else
                {
                    output[compressDataIndex++] = (byte) ((difference & DATA_PART_MASK));
                }
            } else if ((difference >= -0x20) & (difference < 0x20))
            {
                output[compressDataIndex++] = (byte) ((difference & DATA_PART_MASK));
            } else if ((difference >= -0x1000) & (difference < 0x1000))
            {
                output[compressDataIndex++] = (byte) ((difference >> 7 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else if ((difference >= -0x80000) & (difference < 0x80000))
            {
                // 20 bits
                output[compressDataIndex++] = (byte) ((difference >> 14 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else if ((difference >= -0x4000000) & (difference < 0x4000000))
            {
                // 27 bits, 3*7 + 6
                output[compressDataIndex++] = (byte) ((difference >> 21 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 14 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else if ((difference >= -0x200000000L) & (difference < 0x200000000L))
            {
                // 34 bits, 4*7 + 6
                output[compressDataIndex++] = (byte) ((difference >> 28 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 21 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 14 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else if ((difference >= -0x10000000000L) & (difference < 0x10000000000L))
            {
                // 41 bits, 5*7 + 6
                output[compressDataIndex++] = (byte) ((difference >> 35 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 28 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 21 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 14 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else if ((difference >= -0x800000000000L) & (difference < 0x800000000000L))
            {
                // 48 bits, 6*7 + 6
                output[compressDataIndex++] = (byte) ((difference >> 42 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 35 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 28 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 21 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 14 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else if ((difference >= -0x40000000000000L) & (difference < 0x40000000000000L))
            {
                // 55 bits, 7*7 + 6
                output[compressDataIndex++] = (byte) ((difference >> 49 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 42 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 35 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 28 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 21 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 14 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else if ((difference >= -0x2000000000000000L) & (difference < 0x2000000000000000L))
            {
                // 62 bits, 8*7 + 6
                output[compressDataIndex++] = (byte) ((difference >> 56 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 49 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 42 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 35 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 28 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 21 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 14 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else
            {
                // 63 bits, 9*7 (with excess)
                output[compressDataIndex++] = (byte) ((difference >> 63 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 56 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 49 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 42 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 35 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 28 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 21 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 14 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            }
            i++;
        }

        return compressDataIndex;
    }

    /**
     * Uncompress a given buffer of data compressed by this algorithm into an array of ints.  The data must have
     * been compressed from an array of ints.
     *
     * @param compressedInput  The buffer of compressed data to uncompress
     * @param compressedLength The number of bytes of input to process (in case the input buffer is not full)
     * @param output           The buffer in which to place the results.  It must be sized large enough to hold the
     *                         uncompressed data.
     * @return The number of elements in the uncompressed data
     * @throws ArrayIndexOutOfBoundsException If the input or output buffer is overrun
     * @see #compressData(int[], byte[])
     */
    public int uncompressData(byte[] compressedInput, int compressedLength, int[] output) throws ArrayIndexOutOfBoundsException
    {
        int unCompressedIndex = 0;
        int nibbleValue;
        int extendedValue;
        int extensionLength;
        int currentValue = 0;

        for (int i = 0; i < compressedLength; i++)
        {
            switch ((compressedInput[i] & CONTROL_MASK))
            {
                case ZERO_COUNT_BYTE_BIT:
                {
                    if ((compressedInput[i] & DATA_PART_MASK) != DATA_PART_MASK)
                    {
                        for (int j = 0; j < (compressedInput[i] & DATA_PART_MASK); j++)
                        {
                            output[unCompressedIndex++] = currentValue;
                        }
                    } else
                    {
                        extendedValue = (compressedInput[++i] & 0xff);
                        extendedValue <<= 8;
                        extendedValue |= (compressedInput[++i] & 0xff);

                        for (int j = 0; j < extendedValue; j++)
                        {
                            output[unCompressedIndex++] = currentValue;
                        }
                    }
                    break;
                }
                case NIBBLE_BYTE_BIT:
                {
                    nibbleValue = (compressedInput[i] & 0x07);
                    if ((nibbleValue & 0x04) > 0)
                    {
                        nibbleValue -= 0x08;
                    }
                    currentValue += nibbleValue;
                    output[unCompressedIndex++] = currentValue;

                    nibbleValue = (compressedInput[i] & 0x38) >> 3;
                    if ((nibbleValue & 0x04) > 0)
                    {
                        nibbleValue -= 0x08;
                    }
                    currentValue += nibbleValue;
                    output[unCompressedIndex++] = currentValue;
                    break;
                }
                case DATA_BYTE_BIT:
                {
                    nibbleValue = (compressedInput[i] & DATA_PART_MASK);
                    if ((nibbleValue & 0x20) > 0)
                    {
                        nibbleValue -= 0x40;
                    }

                    currentValue += nibbleValue;
                    output[unCompressedIndex++] = currentValue;
                    break;
                }
                case BYTE_EXTENSION_BIT:
                {
                    extendedValue = 0;
                    extensionLength = 0;

                    do
                    {
                        extendedValue <<= 7;
                        extendedValue |= (compressedInput[i] & 0x7f);
                        extensionLength++;
                        i++;
                    } while ((compressedInput[i] & BYTE_EXTENSION_BIT) == BYTE_EXTENSION_BIT);

                    extendedValue <<= 7;
                    extendedValue |= (compressedInput[i] & 0x7f);

//                    System.out.println("extended value = " + extendedValue);
//                    System.out.println(String.format("%32s", Integer.toBinaryString(extendedValue)).replace(' ', '0'));

                    int shift = Math.min(31, (extensionLength * 7 + 5));
//                    System.out.println("Shift=" + shift);
//                    System.out.println(String.format("%32s", Integer.toBinaryString((0x1 << shift))).replace(' ', '0'));

                    if ((extendedValue & (0x1 << shift)) > 0)
                    {
                        extendedValue -= (0x1 << shift + 1);
                    }
                    currentValue += extendedValue;
                    output[unCompressedIndex++] = currentValue;

                    break;
                }
            }
        }

        return unCompressedIndex;
    }

    /**
     * Compress the data given into the supplied buffer of ints.  If the output buffer is not large enough an
     * ArrayIndexOutOfBoundsException wil be thrown.
     *
     * @param input  Array of ints to compress
     * @param output Array into which to write the compressed results
     * @return The length of the compressed data (in bytes)
     * @throws ArrayIndexOutOfBoundsException If the output buffer is overrun
     */
    public int compressData(int[] input, byte[] output) throws ArrayIndexOutOfBoundsException
    {
        int i = 0;
        int difference;
        int nextDifference;
        int zeroCounter = 0;
        int compressDataIndex = 0;

        while (i < input.length)
        {
            difference = i == 0 ? input[i] : input[i] - input[i - 1];
            nextDifference = i + 1 < input.length
                    ? input[i + 1] - input[i]
                    : -input[i];

//            System.out.println("Diff: " + difference + " ND: " + nextDifference);

            if (difference == 0)
            {
                zeroCounter++;
                if ((zeroCounter == 1) & (nextDifference != 0))
                {
                    if ((nextDifference > -4) && (nextDifference < 4))
                    {
                        output[compressDataIndex++] = (byte) (((nextDifference & 0x7) << 3) | NIBBLE_BYTE_BIT);
                        zeroCounter = 0;
                        i++;
                    } else
                    {
                        output[compressDataIndex++] = (byte) ((zeroCounter & DATA_PART_MASK) | ZERO_COUNT_BYTE_BIT);
                        zeroCounter = 0;
                    }
                } else if ((nextDifference != 0) || (zeroCounter >= 65535) || (i + 1 == input.length))
                {
                    if (zeroCounter > 62)
                    {
                        //if we have more than 62 zeros, we will overflow and use 3 bytes to store zeros. this will support upto 65535 zeros at a time
                        output[compressDataIndex++] = (byte) ((DATA_PART_MASK) | ZERO_COUNT_BYTE_BIT);
                        output[compressDataIndex++] = (byte) ((zeroCounter >> 8));
                        output[compressDataIndex++] = (byte) ((zeroCounter));
                    } else
                    {
                        output[compressDataIndex++] = (byte) ((zeroCounter & DATA_PART_MASK) | ZERO_COUNT_BYTE_BIT);
                    }
                    zeroCounter = 0;
                }
            } else if ((difference >= -4) && (difference < 4))
            {
                if (i + 1 < input.length && (nextDifference >= -4) && (nextDifference < 4))
                {
                    output[compressDataIndex++] = (byte) (((nextDifference & 0x7) << 3) | (difference & 0x7) | NIBBLE_BYTE_BIT);
                    i++;
                } else
                {
                    output[compressDataIndex++] = (byte) ((difference & DATA_PART_MASK));
                }
            } else if ((difference >= -0x20) & (difference < 0x20))
            {
                output[compressDataIndex++] = (byte) ((difference & DATA_PART_MASK));
            } else if ((difference >= -0x1000) & (difference < 0x1000))
            {
                output[compressDataIndex++] = (byte) ((difference >> 7 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else if ((difference >= -0x80000) & (difference < 0x80000))
            {
                // 20 bits, 2*7 + 6
                output[compressDataIndex++] = (byte) ((difference >> 14 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else if ((difference >= -0x4000000) & (difference < 0x4000000))
            {
                // 27 bits, 3*7 + 6
                output[compressDataIndex++] = (byte) ((difference >> 21 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 14 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            } else
            {
                // 32 bits, 4*7 + 4 (with excess)
                output[compressDataIndex++] = (byte) ((difference >> 28 & DATA_PART_MASK) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 21 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 14 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) ((difference >> 7 & 0x7f) | BYTE_EXTENSION_BIT);
                output[compressDataIndex++] = (byte) (difference & 0x7f);
            }
            i++;
        }

        return compressDataIndex;
    }
}
