package gov.takx.core.view.gui;

import gov.takx.api.ui.IResourceCleaner;

import javax.swing.JPanel;

/**
 * Simple base class for panels that have a title, such as all the side/bottom panels.
 *
 * @since 2.37
 */
public abstract class ATitledPanel extends JPanel implements IResourceCleaner
{
    protected final String title;

    /**
     * Construct a panel with empty title.
     * <p>Note: this mostly exists so that we can work with these components in JFormDesigner.
     *
     * @since 5.6
     */
    protected ATitledPanel()
    {
        this("");
    }

    /**
     * @param title Title for panel
     */
    protected ATitledPanel(String title)
    {
        this.title = title;

        setName(title);
    }

    /**
     * @return panel title
     */
    public String getTitle()
    {
        return title;
    }

    @Override
    public void releaseResources()
    {
    }
}
