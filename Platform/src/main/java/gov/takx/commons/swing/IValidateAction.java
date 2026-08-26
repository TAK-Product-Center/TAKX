package gov.takx.commons.swing;

/**
 * Validation notification for mutable panels.  This interface provides the mutable panel the opportunity to
 * validate its contents prior to closing the containing dialog.
 *
 */
public interface IValidateAction
{
    /**
     * Validate the panel prior to committing the changes that would occur in an OK or Apply action.
     *
     * @return {@code null} if there is nothing invalid, or string that contains
     * user feedback as to what is invalid on the panel.  Returning a non-null value cancels the pending OK or
     * Apply action.
     */
    String validateContents();
}
