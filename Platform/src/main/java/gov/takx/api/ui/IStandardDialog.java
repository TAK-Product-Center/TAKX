package gov.takx.api.ui;

import javax.swing.JDialog;

/**
 * Contract for obtaining the {@link JDialog} from an arbitrary derived interface.  {@link IStandardDialogProvider}
 * methods return interfaces with any unique operations that a specific dialog might need to offer plugins.  Those
 * interfaces all extend this interface, providing access to the dialog that implements that interface, allowing them
 * to perform actions like show/hide on the dialog by calling our {@link #getDialog()} method.
 *
 * @see IStandardDialogProvider
 * @since 5.3
 */
public interface IStandardDialog
{
    /**
     * @return The dialog that implements a given sub-interface of this interface.
     */
    default JDialog getDialog()
    {
        return (JDialog) this;
    }
}
