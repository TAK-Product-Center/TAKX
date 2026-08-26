package gov.takx.core.globe;

import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.intern.DefaultCommonDockable;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.view.gui.docking.PlaybackAwareDockable;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.inject.Inject;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A dockable globe panel.
 *
 * @since 1.40
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeDockable extends DefaultMultipleCDockable
{
    @Inject
    @ProgramPreference(PreferenceEntry.DOCKING_FRAMES_GLOBE_MINIMIZATION_ENABLED)
    private boolean globeMinimizationEnabled;

    @Inject
    @ProgramPreference(PreferenceEntry.DOCKING_FRAMES_FRAME_MAXIMIZATION_ENABLED)
    private boolean frameMaximizationEnabled;

    @Inject
    @ProgramPreference(PreferenceEntry.DOCKING_FRAMES_GLOBE_EXTERNALIZATION_ENABLED)
    private boolean globeExternalizationEnabled;

    @Inject
    @ProgramPreference(PreferenceEntry.DOCKING_FRAMES_SECONDARY_GLOBES_CLOSE_ENABLED)
    private boolean secondaryGlobesCloseEnabled;

    private IApplicationGlobe globe;

    /**
     * Create a dockable globe which will have the globe applied later in the lifecycle. This is need to accommodate
     * layout save/restore where the layout is being restored prior to the globes being ready for use.
     *
     * @param globeDockableFactory The factory that can create instances of this dockable
     */
    public GlobeDockable(GlobeDockableFactory globeDockableFactory)
    {
        super(globeDockableFactory);
    }

    /**
     * Apply the globe to this globe dockable.
     *
     * @param globe The globe
     */
    public void addGlobe(IApplicationGlobe globe)
    {
        this.globe = globe;

        final boolean isPrimary = globe.isPrimaryGlobe();

        setTitleText(isPrimary ? "Primary Globe" : "Globe: " + globe.getGlobeId());

        setTitleShown(true);
        setTitleIcon(UIResources.getStandardSizedIcon(PLATFORM_ICONS, "globe.png"));
        setExternalizable(globeExternalizationEnabled);
        setCloseable(secondaryGlobesCloseEnabled && !isPrimary);
        setMinimizable(globeMinimizationEnabled);
        setMaximizable(frameMaximizationEnabled);

        add(globe.createFocusHandlingPanel());
    }

    /**
     * @return The underlying globe component
     */
    public IApplicationGlobe getGlobe()
    {
        return globe;
    }

    /**
     * @return New PlaybackAwareDockable
     */
    @Override
    protected DefaultCommonDockable createCommonDockable()
    {
        return new PlaybackAwareDockable(this, getClose());
    }
}
