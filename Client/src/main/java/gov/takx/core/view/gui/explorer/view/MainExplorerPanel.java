package gov.takx.core.view.gui.explorer.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.core.view.gui.ATitledPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import java.awt.Component;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel that manages either a single Explorer panel or multiple Explorer panels in a tabbed pane.
 *
 * @since 2.4
 */
public class MainExplorerPanel extends ATitledPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The title associated with each panel. We need to keep track of these when transitioning from a non-tabbed state,
     * where the title is unused, to the tabbed state, where the title is used as the panel's tab title.
     */
    private final Map<Component, String> componentTitles = new HashMap<>();

    /**
     * The tabbed pane that will only exist when explicitly required. May be {@code null} if there is a single Explorer panel
     * and {@link #showTabForSingleExplorer} is set to {@code false}.
     */
    private JTabbedPane tabbedPane;

    /**
     * Tracks the option to show a tab even if there is only a single Explorer panel in use.
     */
    private boolean showTabForSingleExplorer;

    /**
     * Constructor. Initializes title map and UI.
     */
    public MainExplorerPanel()
    {
        super(ButtonType.OVERLAYS.getDisplayText());
        initComponents();
    }

    /**
     * Add an Explorer panel at a specific tab index. If transitioning from a single panel to multiple panels when
     * {@link #showTabForSingleExplorer} is set to {@code false}, or when adding the first panel when
     * {@link #showTabForSingleExplorer} is set to {@code true}, a new tabbed pane will be created.
     *
     * @param title         The panel's title that will be placed in the tab when a tabbed pane is employed.
     * @param explorerPanel The panel to add.
     * @param index         The index at which the panel should be added. If the value is out of range, the tab will be
     *                      added to the beginning or end of the list.
     */
    public void addExplorer(String title, JPanel explorerPanel, int index)
    {
        // Track the title.
        componentTitles.put(explorerPanel, title);

        int currentComponentCount = getComponentCount();

        // If there are no components and we are not showing tabs for single components, just add the panel.
        if (currentComponentCount == 0 && !showTabForSingleExplorer)
        {
            add(explorerPanel, new CellConstraints(1, 1));
        }
        // Either there are multiple panels or we need to show a tab for a single component.
        else
        {
            // Add tabs if necessary.
            switchToTabbedPane();

            // If no index was specified, add the new component to the end of the set of tabs.
            if (index == -1)
            {
                tabbedPane.addTab(title, explorerPanel);
            } else
            {
                tabbedPane.insertTab(title, null, explorerPanel, null, Math.min(Math.max(0, index), tabbedPane.getTabCount()));
            }
        }
    }

    /**
     * Select a tab and make its contents visible. If tabs aren't currently being employed or if the panel does not
     * exist, the call will be ignored.
     *
     * @param explorerPanel The panel that should be made visible. Object comparison is used, so the exact object must
     *                      be provided.
     */
    public void showTab(JPanel explorerPanel)
    {
        if (tabbedPane != null)
        {
            int index = tabbedPane.getComponentZOrder(explorerPanel);
            if (index != -1) tabbedPane.setSelectedIndex(index);
        }
    }

    /**
     * Return the component that is currently being displayed.
     *
     * @return The component that is currently being displayed.
     */
    public Component getVisibleComponent()
    {
        if (tabbedPane != null)
        {
            return tabbedPane.getSelectedComponent();
        } else
        {
            // If tabs aren't being used, the panel should be the single child of this panel.
            return getComponent(0);
        }
    }

    /**
     * Remove an Explorer's panel. If only one panel is left after the removal and {@link #showTabForSingleExplorer} is
     * set to {@code false}, the tabbed pane will be removed and the last panel will be added directly to the
     * parent.
     *
     * @param explorerPanel The panel to remove
     */
    public void removeExplorer(JPanel explorerPanel)
    {
        // Check to see if we're working in a tabbed pane.
        if (tabbedPane != null)
        {
            // Remove the panel.
            tabbedPane.remove(explorerPanel);

            // If there's only one panel left, get rid of the tabs.
            if (tabbedPane.getTabCount() == 1 && !showTabForSingleExplorer) switchFromTabbedPane();
        } else
        {
            remove(explorerPanel);
        }

        componentTitles.remove(explorerPanel);
    }

    /**
     * Update the setting for the option that will display a tab for a single panel.
     *
     * @param showTabForSingleExplorer {@code true} to show a tab for a single panel, {@code false} to show
     *                                 a single panel directly in the main explorer panel.
     */
    public void setShowTabForSingleExplorer(boolean showTabForSingleExplorer)
    {
        // Only take action if the state is changing.
        if (this.showTabForSingleExplorer != showTabForSingleExplorer)
        {
            this.showTabForSingleExplorer = showTabForSingleExplorer;

            // Tabs are not being used and there is an existing panel.
            if (showTabForSingleExplorer && tabbedPane == null && getComponentCount() == 1)
            {
                switchToTabbedPane();
            }
            // Tabs are being used and there's a single panel left.
            else if (!showTabForSingleExplorer && tabbedPane != null && tabbedPane.getTabCount() == 1)
            {
                switchFromTabbedPane();
            }
        }
    }

    /**
     * Switch from a single component to a tabbed pane.
     */
    private void switchToTabbedPane()
    {
        // Create a tabbed pane if necessary.
        if (tabbedPane == null)
        {
            Component existingComponent = null;

            // If there's already a panel, grab it.
            if (getComponentCount() > 0)
            {
                existingComponent = getComponent(0);

                // Clear out the existing panel.
                removeAll();
            }

            // Create a tabbed pane.
            tabbedPane = new JTabbedPane();

            // Add it to the parent.
            add(tabbedPane, new CellConstraints(1, 1));

            // Add the existing component back to the tabbed pane.
            if (existingComponent != null) tabbedPane.addTab(componentTitles.get(existingComponent), existingComponent);
        }
    }

    /**
     * Attempt to remove the tabbed pane and switch back to a single component. Requests will be ignored if there are
     * multiple tabs in the pane.
     */
    private void switchFromTabbedPane()
    {
        int tabCount = tabbedPane.getTabCount();

        // Make sure we don't have multiple tabs.
        if (tabCount > 1)
        {
            logger.warn("Attempted to remove tabs but {} tabs are being used. Ignoring.", tabbedPane.getTabCount());
            return;
        }

        // Remove the tabbed pane from the parent.
        remove(tabbedPane);

        // If the tabbed pane contains a tab, add it directly to the parent.
        if (tabCount == 1)
        {
            Component component = tabbedPane.getComponentAt(0);
            add(component, new CellConstraints(1, 1));
        }

        tabbedPane = null;
    }

    /**
     * Quickly clear all panels.
     * @since 5.2
     */
    @InvokeAndWaitOnEDT
    public void clear()
    {
        if (tabbedPane != null) tabbedPane.removeAll();
        componentTitles.clear();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
