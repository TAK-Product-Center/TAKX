package gov.takx.api.commservices;

/**
 * Delegate for use by the Port analyzer parser to validate that the parser understands the "unknown" data
 * coming over a serial port.
 *
 * @see IPortAnalyzerParser
 */
public interface IPortAnalyzerDelegate
{
    void validateData(String name);
}
