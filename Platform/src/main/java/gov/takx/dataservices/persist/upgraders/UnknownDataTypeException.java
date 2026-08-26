package gov.takx.dataservices.persist.upgraders;

/**
 */
public class UnknownDataTypeException extends RuntimeException
{
    public UnknownDataTypeException(Integer i)
    {
        super("IDataField ordinal value " + i + " is unknown");
    }
}
