package gov.takx.core.globe.layers.worldwind;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.layers.worldwind.ui.HTTPLoginDialog;
import gov.takx.core.globe.layers.worldwind.ui.HTTPLoginEditorDialog;
import gov.takx.core.view.gui.StandardDialogManager;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;

/**
 * @since 1.43
 */
@PseudoBean
public class PasswordManagerView implements IPasswordManagerView
{
    @Inject
    private StandardDialogManager dialogManager;

    /**
     * Set of host/port (site) keys currently being processed.  Parallel requests may be made to the same site.  This
     * set tracks the in-process user prompts to avoid displaying multiple simultaneous login prompts for the same
     * site.  Multiple simultaneous login prompts for different sites are still possible.
     * <p/>
     * This set is not thread-safe because it is only intended for use on the EDT.
     */
    private final Set<String> inProcess;

    public PasswordManagerView()
    {
        inProcess = new HashSet<>();
    }

    @InvokeAndWaitOnEDT
    @Override
    public void displayUserPrompt(final String url)
    {
        // Determine whether this site is currently in process (e.g. another thread has already requested a prompt for
        // this site).  If it is, return.
        if (inProcess.contains(url)) return;

        // Mark this site as in-process to avoid displaying multiple simultaneous prompts for it.
        inProcess.add(url);

        // Prompt the user for login information with the login dialog.
        HTTPLoginDialog dialog = new HTTPLoginDialog(null);
        dialog.update(url);
        dialog.setVisible(true);

        // User prompt has completed - unmark this site as in-process.
        inProcess.remove(url);
    }

    @InvokeOnEDT
    @Override
    public void displayEditor()
    {
        dialogManager.showDialog(HTTPLoginEditorDialog.class);
    }
}
