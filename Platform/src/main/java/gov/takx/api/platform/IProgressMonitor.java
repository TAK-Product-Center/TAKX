package gov.takx.api.platform;

/**
 */
public interface IProgressMonitor
{
    /**
     * Set the progress bar to be indeterminate.
     */
    int INDETERMINATE = -1;
    /**
     * Sets the sub-status progress bar invisible. It is not possible to set the primary progress bar to invisible.
     */
    int INVISIBLE = -2;

    void setText(String text);

    void setSubstatus(String text);

    void setVisible(final boolean b);

    void setMaximum(int maximum);

    void increment();

    void setSubstatusMaximum(int maximum);

    void incrementSubstatus();

    void dispose();
}
