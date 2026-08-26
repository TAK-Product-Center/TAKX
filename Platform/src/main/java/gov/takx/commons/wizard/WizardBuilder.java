package gov.takx.commons.wizard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to construct simple wizards. The wizard framework offers a lot of flexibility, but many wizards
 * are a simple sequence of panels, with no special branching logic. The WizardBuilder simplifies the task of
 * assembling a simple linear wizard (one that does not require branching logic).
 *
 * @since 1.3
 */
public final class WizardBuilder
{
    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private WizardBuilder()
    {
    }

    /**
     * Build a wizard panel map from a list of panels. The next and previous pointers of each panel in the list
     * will be connected to its neighbors in the list. The wizard will finish after the last panel in the list.
     *
     * @param panels Array of wizard panels. The first panel in the array will become the first panel of the
     *               wizard, which will link to the next panel in the array, and so on till the end.
     * @return Map of wizard panels.
     */
    public static Map<String, IWizardPanelDescriptor> buildPanelMap(ILinearWizardPanelDescriptor... panels)
    {
        Map<String, IWizardPanelDescriptor> panelMap = new HashMap<String, IWizardPanelDescriptor>(panels.length);

        ILinearWizardPanelDescriptor panel;
        ILinearWizardPanelDescriptor previousPanel = null;

        int numPanels = panels.length;
        int lastPanel = numPanels - 1;

        for (int i = 0; i < panels.length; i++)
        {
            panel = panels[i];
            if (previousPanel == null)
            {
                panel.setPreviousPanel(IWizardPanelDescriptor.BEGIN);
            } else
            {
                panel.setPreviousPanel(previousPanel.getIdentifier());
            }

            if (i == lastPanel)
            {
                panel.setNextPanel(IWizardPanelDescriptor.FINISH);
            } else
            {
                panel.setNextPanel(panels[i + 1].getIdentifier());
            }

            previousPanel = panel;

            panelMap.put(panel.getIdentifier(), panel);
        }

        return panelMap;
    }

    /**
     * Build a wizard panel map from a list of panels. The next and previous pointers of each panel in the list
     * will be connected to its neighbors in the list. The wizard will finish after the last panel in the list.
     *
     * @param panels List of wizard panels. The first panel in the list will become the first panel of the
     *               wizard, which will link to the next panel in the list, and so on till the end.
     * @return Map of wizard panels.
     */
    public static Map<String, IWizardPanelDescriptor> buildPanelMap(List<? extends ILinearWizardPanelDescriptor> panels)
    {
        return buildPanelMap((ILinearWizardPanelDescriptor[]) panels.toArray(new ILinearWizardPanelDescriptor[panels.size()]));
    }
}
