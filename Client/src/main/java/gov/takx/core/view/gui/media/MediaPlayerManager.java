package gov.takx.core.view.gui.media;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.media.AMediaPlayer;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaPlayer;
import gov.takx.api.media.IMediaPlayerManager;
import gov.takx.api.media.IMediaPlayerManagerListener;
import gov.takx.api.media.MediaPlayerOptions;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.core.media.FfmpegLog;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;
import static org.bytedeco.ffmpeg.global.avutil.setLogCallback;

/**
 * @since 2.10
 */
@ApplicationScoped
public class MediaPlayerManager implements IMediaPlayerManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static
    {
        // Need to give ourselves a little more leeway to allocate memory for playing videos
        long maxMemory = 4 * Runtime.getRuntime().maxMemory();
        System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", Long.toString(maxMemory));
    }

    private final IGlobeManager globeManager;
    private final IUISpaceManager uiSpaceManager;
    private final boolean headless;

    private final Map<String, MediaPanel> panelMap = new HashMap<>();
    private final List<IMediaPlayerManagerListener> mediaPlayerManagerListeners = new CopyOnWriteArrayList<>();

    @Inject
    MediaPlayerManager(IGlobeManager globeManager, IUISpaceManager uiSpaceManager, @Headless boolean headless)
    {
        this.globeManager = globeManager;
        this.uiSpaceManager = uiSpaceManager;
        this.headless = headless;
    }

    @Override
    public void addMediaPlayerManagerListener(IMediaPlayerManagerListener mediaManagerListener)
    {
        mediaPlayerManagerListeners.add(mediaManagerListener);
    }

    @Override
    public void removeMediaPlayerManagerListener(IMediaPlayerManagerListener mediaManagerListener)
    {
        mediaPlayerManagerListeners.remove(mediaManagerListener);
    }

    /**
     * Start the media player manager service.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        if (headless) return;

        logger.debug("Registering ffmpeg...");

        // Some videos cause a lot of output, only show errors
        av_log_set_level(AV_LOG_ERROR);

        // Redirect ffmpeg's log to the application log
        setLogCallback(FfmpegLog.getInstance());

        avformat_network_init();
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        logger.debug("Stopping media player manager service");

        // Get the players to unload any playing media and shutdown
        for (MediaPanel panel : panelMap.values())
        {
            panel.getPlayer().unloadMedia();
        }

        mediaPlayerManagerListeners.clear();
    }

    @Override
    public IMediaPlayer createDefaultPlayer(IMedia media)
    {
        MediaPlayerOptions options = new MediaPlayerOptions(media.getName());
        return createMediaPlayer(options, media);
    }

    /**
     * {@inheritDoc}
     * <p>
     * When the media player is created by the manager directly as a result of restoring a project this method ends up
     * not already being on the EDT. Thus we need the @InvokeAndWaitOnEDT.
     */
    @InvokeAndWaitOnEDT
    @Override
    public IMediaPlayer createMediaPlayer(MediaPlayerOptions options, IMedia media)
    {
        AMediaPlayer player = new MediaPlayer(options, media);
        createMediaPlayer(options, media, player);
        return player;
    }

    @Override
    public void createMediaPlayer(MediaPlayerOptions options, IMedia media, IMediaPlayer player)
    {
        if (headless) throw new UnsupportedOperationException("Can not create a media player in headless mode");

        // [KEYMAKER-104] - Externalized media windows should be smaller when first popped out, so
        // they won't obscure too much of the application window. Also make sure they are centered.

        Dimension windowSize = new Dimension(320, 240);
        Dimension screenSize = ComponentUtils.getActiveMonitorScreenSize();
        Rectangle rect = new Rectangle((screenSize.width - windowSize.width) / 2,
                (screenSize.height - windowSize.height) / 2, windowSize.width, windowSize.height);

        // TODO: i think we need to seriously rethink this. At some point we will want to incorporate Media Analysis
        //  into the core and at that point having a one-to-one relationship with the media will be problematic.
        String key = media.getKey();
        String uuid = options.getUUID();
        if (uuid == null)
        {
            options.setUUID(key);
        } else
        {
            // Internally TAKX will not populate the uuid, even though it will use it later for mapping things
            // in the MediaPlayerManager. So if we have a uuid here we know the player is being managed externally
            // by something like media analysis.
            key = uuid;
        }

        ComponentDescriptor descriptor = new ComponentDescriptorBuilder(MediaPanel.class, options.getTitle())
                .setComponentPosition(ComponentPosition.CENTER)
                .setToolbarButtonVisible(true)
                .setExternalizedRect(rect)
                .build();

        MediaPanel panel = new MediaPanel(player, descriptor);
        String finalKey = key;

        // Since we're adding this component to the CENTER, the globe must be ready before we can do this
        globeManager.getPrimaryGlobeDelegate().doWhenReady(() -> {
            uiSpaceManager.addComponent(descriptor, panel);
            panelMap.put(finalKey, panel);
        });

        // Load the media if it was specified
        player.loadMedia(media);

        if (options.isAutoplayMedia()) player.play();

        // All internal mechanisms lead to this method for creating Media Players, so if we want to guarantee that the
        // IMediaManagerListener's receive information on the creation of the Media Player (even one created by an
        // external component and not the MediaManager or the MediaPlayerManager, we might want to fire this here. In
        // the future, if this method becomes protected or private, we may want to fire this at the individual interface
        // creation points above, as this method currently will fire when any mechanism creates a new player and adds it
        // to the UI here, and not just the MediaPlayerManager through the two creation entry points above. I
        // think this is the safest approach to guarantee all listeners are aware of all players being added.
        mediaPlayerManagerListeners.forEach(listener -> listener.playerAdded(player));
    }

    @Override
    public void setMediaPlayer(String uuid, IMediaPlayer player)
    {
        MediaPanel panel = panelMap.get(uuid);
        if (panel != null) panel.setPlayer(player);
    }

    @InvokeOnEDT
    @Override
    public void removeMediaPlayer(IMediaPlayer mediaPlayer)
    {
        // This method may be explicitly called by the plugin, or when a user closes a media player window. In either
        // case, it should clean up all resources, and notify listeners that the player has closed.
        String key = null;
        if (mediaPlayer.getOptions().isPresent())
        {
            key = mediaPlayer.getOptions().get().getUUID();
        }
        MediaPanel panel = panelMap.get(key);
        if (panel != null)
        {
            uiSpaceManager.removeComponent(panel);
            panelMap.remove(key);

            if (mediaPlayer instanceof MediaPlayer)
            {
                ((MediaPlayer) mediaPlayer).destroy();
            }

            mediaPlayerManagerListeners.forEach(listener -> listener.playerRemoved(mediaPlayer));
        }
    }

    @InvokeOnEDT
    @Override
    public void removeMediaPlayer(IMedia media)
    {
        Predicate<MediaPanel> predicate = panel -> {
            Optional<IMedia> pm = panel.getPlayer().getMedia();
            return pm.filter(filteredMedia -> filteredMedia == media).isPresent();
        };
        Optional<MediaPanel> panel = panelMap.values().stream().filter(predicate).findFirst();
        panel.ifPresent(p -> removeMediaPlayer(p.getPlayer()));
    }

    /**
     * This panel is used so that the player can be swapped out while maintaining the docking layout. This is used
     * particularly for the custom players
     *
     * @since 2.37
     */
    private static class MediaPanel extends JPanel
    {
        ComponentDescriptor descriptor;

        MediaPanel(IMediaPlayer player, ComponentDescriptor descriptor)
        {
            super();

            this.descriptor = descriptor;
            BorderLayout layout = new BorderLayout();
            setLayout(layout);
            add(player.getComponent(), BorderLayout.CENTER);
        }

        @InvokeOnEDT
        void setPlayer(IMediaPlayer player)
        {
            remove(0);
            add(player.getComponent(), BorderLayout.CENTER);
        }

        IMediaPlayer getPlayer()
        {
            Component component = getComponent(0);
            return component instanceof IMediaPlayer ? (IMediaPlayer) component : null;
        }
    }
}
