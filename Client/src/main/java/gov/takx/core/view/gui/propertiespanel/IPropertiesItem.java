package gov.takx.core.view.gui.propertiespanel;

import gov.takx.api.ui.IResourceCleaner;

import javax.swing.JPanel;

/**
 * A wrapper interface for selectable objects which need to be displayed in {@link PropertiesPanel}.
 */
public interface IPropertiesItem extends IResourceCleaner
{
    /**
     * @return custom properties panel to display in the main PropertiesPanel
     */
    JPanel getPanel();

    /**
     * @return label text to display above the custom panel
     */
    String getLabelText();

    /**
     * @return the identifier object for this properties item, i.e. the object for which this properties item is built
     */
    Object getIdentityObject();

    /**
     * @return a boolean value indicating whether the panel is being displayed through another dialog, in which case the
     * PropertiesPanel will not display the panel.
     */
    boolean isDialogOpen();

    /**
     * @return a type ID string
     */
    String getType();

    /**
     * @return {@code true} if the panel returned from {@link #getPanel()} should be wrapped in a {@link javax.swing.JScrollPane}
     *         before adding it to the properties panel.  Default value is {@code true}.
     * @since 5.5
     */
    default boolean addScrollPane()
    {
        return true;
    }

    /**
     * @return {@code true} if the object this properties item represents supports attachments.  This controls the
     *         visibility of the attachments icon in the properties panel. Default value is {@code false}.
     * @since 5.5
     */
    default boolean supportsAttachments()
    {
        return false;
    }
}
