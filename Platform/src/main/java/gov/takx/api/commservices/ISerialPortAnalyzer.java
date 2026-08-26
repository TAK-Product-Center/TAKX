package gov.takx.api.commservices;

/**
 * Interface for plugins to extend if they want to participate in analyzing raw data coming over a serial port.
 * The plugin will be expected to return a "port analyzing" parser, which will be responsible for parsing
 * unknown data coming over a serial port and deciding if the data is in a format that the plugin supports.
 *
 * @since 1.14
 */
public interface ISerialPortAnalyzer
{
    /**
     * Get an Parser to be used to analyze the "unknown" data on a serial port.
     *
     * @return serial port analyzing parser
     * @see IPortAnalyzerParser
     */
    IPortAnalyzerParser getAnalyzingParser();
}
