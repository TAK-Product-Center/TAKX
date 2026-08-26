package gov.takx.api.commservices;

/**
 * Parser which will decide whether or not the data passed into the parseBytes method is of a format that
 * is understandable.  Data will continue to be passed to the parser until a timeout has been exceeded, or
 * until a parser from the list of parsers performing the analysis calls the validation method through
 * its delegate
 *
 * @see ISerialPortAnalyzer
 * @since 1.14
 */
public interface IPortAnalyzerParser
{
    /**
     * Parse an array of bytes to be analyzed
     *
     * @param data   data to parse
     * @param length number of bytes of data
     * @see IPortAnalyzerDelegate
     */
    void parseBytes(byte[] data, int length);

    /**
     * Delegate is injected.  Delegate is used to validate the serial port data.
     *
     * @param delegate port analyzing delegate
     */
    void setDelegate(IPortAnalyzerDelegate delegate);
}
