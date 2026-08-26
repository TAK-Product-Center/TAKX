package gov.takx.core.globe.layers.worldwind;

/**
 * Password manager view.  Responsible for displaying site login prompt and authentication editor.
 *
 * @since 1.43
 */
public interface IPasswordManagerView
{
    /**
     * Display login user prompt for site.
     *
     * @param url site URL.
     */
    void displayUserPrompt(String url);

    /**
     * Display login information editor (add/remove arbitrary authentications).
     *
     * @since 5.6 (signature change)
     */
    void displayEditor();
}
