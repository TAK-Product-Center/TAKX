package gov.takx.commons.swing;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.lang.ExceptionList;
import jakarta.inject.Inject;
import org.hibernate.HibernateException;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import java.awt.Dimension;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Convenience class to use a SwingWorker and easily report exceptions that occur during the execution of the
 * {@link #doInBackground()} method.  In the simple case, the {@link #doInBackground()} method can be written
 * without try/catch blocks and this class will take care of detecting exceptions and reporting them. If you
 * do want to catch exceptions within {@link #doInBackground()}, you can always use the {@link #addException(Throwable)}
 * method to ensure they are reported properly when execution has completed.
 * <p/>
 * A typical (trivial) usage looks like this:
 * <pre>
 *     final ProgressDialog pd = new ProgressDialog();
 *
 *     new ErrorReportingSwingWorker("Failed to Save Project", "Error while saving project") {
 *
 *      {@code @Override}
 *       protected Object doInBackground() throws Exception {
 *           pd.setText("Saving Project");
 *           pd.setVisible(true);
 *           saveProject();
 *           return {@code null};
 *       }
 *
 *       {@code @Override}
 *       protected void cleanup() {
 *           pd.dispose();
 *       }
 *   }.execute();
 * </pre>
 *
 */
@PseudoBean
public abstract class ErrorReportingSwingWorker<T, V> extends SwingWorker<T, V>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final ExceptionList elist = new ExceptionList();

    /** @since 5.2 */
    @Inject
    protected IUserProxy userProxy;

    protected boolean useDialog = true;
    protected final String title;
    protected String message;

    /**
     * Constructor.
     *
     * @param title   The title to use on the error dialog
     * @param message The primary error message to use in the error dialog.  The messages from any caught
     *                exceptions will be appended to this message.
     */
    protected ErrorReportingSwingWorker(String title, String message)
    {
        this.title = title;
        this.message = message;
    }

    /**
     * @return true if a dialog should be shown to display any errors
     */
    public boolean isUseDialog()
    {
        return useDialog;
    }

    /**
     * Set if a dialog should be used to display errors.  If false, then errors are logged but not shown in a dialog.
     *
     * @param useDialog True to show errors in a dialog
     */
    public void setUseDialog(boolean useDialog)
    {
        this.useDialog = useDialog;
    }

    @Override
    protected abstract T doInBackground() throws Exception;

    @Override
    protected void done()
    {
        // Get the result/exception from our execution.  Since the execution is over, calling get will
        // not block the current (EDT) thread.
        T result = null;
        try
        {
            result = get();
        } catch (InterruptedException e)
        {
            logger.error("Failed to get background result due to interrupt", e);
            elist.add(e);
        } catch (ExecutionException e)
        {
            // Some exception occurred that didn't get caught in doInBackground
            Throwable cause = e.getCause();
            logger.error("Uncaught exception in doInBackground", cause);
            elist.add(cause);
        } catch (CancellationException e)
        {
            // worker.cancel() was called.  No need to inform the user of the exception
            logger.warn("Failed to get background result because thread was cancelled");
        }

        try
        {
            cleanup();  // Let derived classes perform cleanup after execution
        } catch (Exception e)
        {
            logger.error("Exception during cleanup()", e);
            elist.add(e);
        }

        // Now dispatch the success/failure status

        try
        {
            if (isSuccess())
            {
                onSuccess(result);
            } else
            {
                showError(title, message, elist);    // tell the user about the failure
                onFailure();
            }
        } catch (Exception e)
        {
            logger.error("Error dispatching success/failure", e);
            showError(title, message, e);
        }
    }

    /**
     * Add an exception.  This will result (eventually) to a call to {@link #onFailure()} from the {@link #done()}
     * method.  Use this if you catch an exception in the {@link #doInBackground()} method and want it reported
     * as a failure causing error.
     *
     * @param e Exception to add
     */
    protected void addException(Throwable e)
    {
        elist.add(e);
    }

    /**
     * Set the error message to use when reporting errors.  This is typically used when the background execution
     * proceeds through multiple "phases" and the error message needs to reflect this.
     *
     * @param message The message to use when reporting errors
     */
    protected void setErrorMessage(String message)
    {
        this.message = message;
    }

    /**
     * Determine if the background execution was a success. This default implementation just checks if the
     * exception list is empty.
     *
     * @return true if execution was a success
     */
    protected boolean isSuccess()
    {
        return elist.isEmpty();
    }

    /**
     * Test to see if any exceptions occurred during background execution.
     *
     * @return true if any exceptions were detected during execution
     */
    protected boolean anyExceptions()
    {
        return !elist.isEmpty();
    }

    /**
     * Called prior to either {@link #onSuccess(Object)} or {@link #onFailure()} so the derived class can cleanup
     * any resources/UI components (like progress dialogs for example) used during the background execution.
     * <br>
     * This method is called on the Swing EDT.
     */
    protected void cleanup()
    {
        // default to empty
    }

    /**
     * Called after {@link #cleanup()} if no exceptions occurred during the background execution.  The argument is
     * the result of the {@link #doInBackground()} method.
     * <br>
     * This method is called on the Swing EDT.
     *
     * @param result The result the {@link #doInBackground()} method, may be {@code null}.
     */
    protected void onSuccess(T result)
    {
        // default to empty
    }

    /**
     * Called after {@link #cleanup()} if exceptions occurred during the background execution, but prior to the
     * display of the error message.
     * <br>
     * This method is called on the Swing EDT.
     */
    protected void onFailure()
    {
        // default to empty
    }

    /**
     * Display an error to the user.  This must be called on the EDT.
     * <br>
     * This method must be called on the Swing EDT.
     *
     * @param title   Title to use on the dialog
     * @param message Initial message to prefix to exception messages
     * @param ex      The exception that caused the failure
     */
    protected void showError(final String title, String message, Throwable ex)
    {
        showError(title, message, new ExceptionList(ex));
    }

    /**
     * Display an error to the user.  This must be called on the EDT.
     * <br>
     * This method must be called on the Swing EDT.
     *
     * @param title   Title to use on the dialog
     * @param message Initial message to prefix to exception messages
     * @param elist   The exception list that caused the failure
     */
    protected void showError(final String title, String message, ExceptionList elist)
    {
        final StringBuilder sb = new StringBuilder(512);

        if (elist.anyInstanceOf(JDBCConnectionException.class, HibernateException.class))
        {
            sb.append("There is a problem connecting to the database.\n\n");
        }

        sb.append(message).append("\nErrors were:\n");
        sb.append(elist.getMessage("   "));

        if (useDialog)
        {
            // Since these error messages can be large, use a scroll pane to avoid breaking the screen boundaries
            JTextArea errorText = new JTextArea(sb.toString(), 40, 80);
            errorText.setBackground(null);
            errorText.setEditable(false);
            JScrollPane scroll = new JScrollPane(errorText);
            scroll.setPreferredSize(new Dimension(400, 400));

            userProxy.doWithoutApplicationLock(() -> {
                AlwaysOnTopJOptionPane.showErrorDialog(scroll, title);
                return null;
            });
        } else
        {
            logger.error(sb.toString());
        }
    }
}
