package gov.takx.commons.swing;

/**
 * Used to listen to standard dialog actions like confirmation, or cancellation.
 *
 * @since 3.3
 */
public interface IDialogListener
{
    /**
     * Called when the dialog has been confirmed by hitting an "OK" or "Apply" button.
     */
    void onConfirm();

    /**
     * Called when the dialog has been cancelled by hitting a "Cancel" button or closing the dialog.
     */
    void onCancel();
}
