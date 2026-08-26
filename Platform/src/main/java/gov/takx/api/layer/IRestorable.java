package gov.takx.api.layer;

/**
 */
public interface IRestorable
{
    public String getRestorableState();

    public void restoreState(String restorableState);
}
