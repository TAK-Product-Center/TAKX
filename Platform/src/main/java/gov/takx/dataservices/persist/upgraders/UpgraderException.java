package gov.takx.dataservices.persist.upgraders;

/**
 * General exception thrown when an upgrader operation fails.
 */
public class UpgraderException extends RuntimeException
{
    public UpgraderException(Throwable e)
    {
        super(e);
    }
}
