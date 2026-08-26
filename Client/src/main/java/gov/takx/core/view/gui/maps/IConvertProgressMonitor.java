package gov.takx.core.view.gui.maps;

/**
 */
public interface IConvertProgressMonitor
{
    public void finishImageryConversion(String message);

    public void setImageryConversionProgress(int progress, int totalProgress, String message);

    public void setLocatingImagery();

    /**
     * Inject a Runnable task for the progress monitor to run if/when the progress monitor cancels the conversion.
     *
     * @param cancelTask a Runnable which the progress monitor should call upon canceling the conversion.
     */
    public void addCancelTask(Runnable cancelTask);
}
