package gov.takx.core.view.gui.milsym;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.core.view.gui.ATitledPanel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.awt.BorderLayout;
import java.util.IdentityHashMap;
import java.util.Map;

import static gov.takx.platform.preferences.PreferenceEntry.MIL_STD_2525_PREF;

/**
 * Wrapper for the {@link MilSymRootPanel} shown by the 2525 toolbar button.
 * Ensures the panel is swapped out for the correct variant when the preference updates.
 *
 * @since 5.5
 */
@PseudoBean
public class MilSymRootPaneWrapper extends ATitledPanel
{
    private final Map<IMilStd2525Service, MilSymRootPanel> roots = new IdentityHashMap<>();

    @Inject
    private IMilStd2525ServiceProvider milStd2525ServiceProvider;

    public MilSymRootPaneWrapper()
    {
        super(ButtonType.TAC_GRAPHICS.getDisplayText());

        setLayout(new BorderLayout());
    }

    @PostConstruct
    private void postConstruct()
    {
        reset();
        MIL_STD_2525_PREF.newStringMonitor(s -> reset());
    }

    @InvokeOnEDT
    private void reset()
    {
        MilSymRootPanel root = roots.computeIfAbsent(milStd2525ServiceProvider.getPreferred2525Service(), MilSymRootPanel::new);
        removeAll();
        add(root, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
