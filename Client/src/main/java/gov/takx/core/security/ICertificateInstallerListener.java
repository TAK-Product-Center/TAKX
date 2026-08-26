package gov.takx.core.security;

/**
 * Callback listener for certificate installation
 *
 */
public interface ICertificateInstallerListener
{
    // basically, 4 choices.  Success, Failure cert already Installed and user Cancelled
    public enum RESULTS
    {
        SUCCESS, FAILURE, INSTALLED, CANCELLED
    }

    public void certificateInstalled(RESULTS result, String host);
}
