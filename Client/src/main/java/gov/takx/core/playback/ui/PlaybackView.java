package gov.takx.core.playback.ui;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.playback.IPlaybackSystem;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.core.view.gui.ATitledPanel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.BoxLayout;

import java.awt.Dimension;

/**
 * The Playback System UI container.  This container is responsible for creating and showing the actual playback UI.
 * Since the playback UI is dependent upon the project session, and this view is created as part of the permanent
 * UI, it is responsible for creating the actual UI component whenever a new project session is started.
 *
 * @since 1.47
 */
@PseudoBean
public class PlaybackView extends ATitledPanel implements IRaptorLifecycleListener
{
    public static final String TITLE = ButtonType.PLAYBACK.getDisplayText();

    @Inject
    private IPlaybackSystem playbackSystem;

    private PlaybackStage playbackStage;

    public PlaybackView()
    {
        super(TITLE);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(770, 120));
        setMinimumSize(new Dimension(450, 70));
    }

    @PostConstruct
    private void postConstruct()
    {
        playbackStage = new PlaybackStage(playbackSystem);
        playbackSystem.setPlaybackStage(playbackStage);
    }

    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        add(playbackStage);
        playbackSystem.goLive();

        revalidate();
        repaint();
    }

    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        // The playbackSystem handles its own lifecycle, so nothing to do for it here

        if (playbackStage != null)
        {
            remove(playbackStage);
            playbackStage.disposeResources();
            playbackStage = null;
        }
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        onClearState(progressMonitor);
    }
}
