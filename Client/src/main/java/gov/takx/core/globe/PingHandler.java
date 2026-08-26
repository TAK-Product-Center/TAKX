package gov.takx.core.globe;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.render.WWIcon;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.ping.IPingEvent;
import gov.takx.api.ping.IPingListener;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorShutdownListener;
import gov.takx.commons.media.AudioAlert;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.globe.layers.IGlobeLayer;
import gov.takx.core.mapobject.ICoreAudioManager;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Object to listen for ping events and display them on the map. Also plays ping audio.
 *
 */
@PseudoBean
public class PingHandler implements IPingListener, IRaptorShutdownListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PING_LAYER = "Ping layer";
    private static final String pingAudioFile = "resources" + File.separator + "audio" + File.separator + "ping.wav";

    private IconLayer pingLayer;
    private Timer timer;
    private final Runnable playPingAudio = new PlayPingAudio();
    private final List<WWIcon> activePings = new ArrayList<>();
    private final Object pingLock = new Object();
    private long lastPingTime = 0L;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IRaptorLayerManager raptorLayerManager;

    @Inject
    private ICoreAudioManager audioManager;

    @Inject
    private ICoreExecutorService executorService;

    /**
     * Frequency of ping pulse animation, in milliseconds.
     */
    private static final int FREQUENCY = 200;

    /**
     * Only play audio if more than this amount of time has passed since the last ping.
     */
    private static final long AUDIO_LOCK_TIME = 5000;

    /**
     * Create a new ping handler.
     */
    public PingHandler()
    {
    }

    private void createPingLayer()
    {
        // If we are re-opening a project, then our ping layer will already exist, so try to locate it first

        IRaptorLayer raptorLayer = raptorLayerManager.getLayer(PING_LAYER);
        if (raptorLayer == null)
        {
            raptorLayer = raptorLayerManager.addLayer(PING_LAYER, IconLayer.class.getName(), LayerGroup.TOOLS);
        }
        IGlobeLayer globeLayer = globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager().getGlobeLayer(raptorLayer);

        pingLayer = (IconLayer) globeLayer.getLayer();
    }

    /**
     * Draw a ping event on the map.
     *
     * @param ping The ping event.
     */
    @Override
    public void onPing(IPingEvent ping)
    {
        synchronized (pingLock)
        {
            if (pingLayer == null) createPingLayer();

            final Color color = ping.getColor();
            GraphicsUtils.Shape shape = ping.getShape();
            boolean flip = ping.isVerticalFlip();
            boolean audio = ping.isPlayAudio();

            BufferedImage iconImage = GraphicsUtils.createShape(shape, ImageUtils.SMALL_ICON_WIDTH, 100, color);
            BufferedImage backgroundIcon = GraphicsUtils.createShape(shape, ImageUtils.LARGE_ICON_WIDTH, 100, color);

            if (flip)
            {
                iconImage = GraphicsUtils.flipVertically(iconImage);
                backgroundIcon = GraphicsUtils.flipVertically(backgroundIcon);
            }

            backgroundIcon = GraphicsUtils.createGlow(backgroundIcon, 7, 1F, color);

            WWIcon icon = new UserFacingIcon(iconImage, Position.fromDegrees(ping.getLatitude(), ping.getLongitude(), 0));
            icon.setBackgroundImage(backgroundIcon);

            pingLayer.addIcon(icon);

            activePings.add(icon);

            if (timer == null)
            {
                timer = new Timer("Ping timer");
                timer.scheduleAtFixedRate(new PingAnimation(), 0, FREQUENCY);
            }

            timer.schedule(new ClearPingTask(icon), ping.getDuration());

            if (audio)
            {
                final var audioAlert = UserPreferenceEntry.AUDIO_ALERT.getValueAs(AudioAlert.class);
                boolean playAudio = audioAlert == AudioAlert.ALERTS_ON;

                final long now = System.currentTimeMillis();
                if (playAudio && (now - lastPingTime > AUDIO_LOCK_TIME))
                {
                    // Play audio
                    executorService.execute(playPingAudio);
                }

                lastPingTime = now;
            }
        }
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }

        activePings.clear();
    }

    class PingAnimation extends TimerTask
    {
        protected int scaleIndex = 0;
        protected double[] scales = new double[]{
                1.25, 1.5, 1.75, 2, 2.25, 2.5, 2.75, 3, 3.25, 3.5, 3.25, 3,
                2.75, 2.5, 2.25, 2, 1.75, 1.5};

        @Override
        public void run()
        {
            synchronized (pingLock)
            {
                for (WWIcon icon : activePings)
                {
                    icon.setBackgroundScale(scales[scaleIndex]);
                }
                scaleIndex = (scaleIndex + 1) % scales.length;
            }
            globeManager.requestRedraw("Ping animation");
        }
    }

    class ClearPingTask extends TimerTask
    {
        private WWIcon icon;

        public ClearPingTask(WWIcon icon)
        {
            this.icon = icon;
        }

        @Override
        public void run()
        {
            pingLayer.removeIcon(icon);

            synchronized (pingLock)
            {
                activePings.remove(icon);
                if (activePings.isEmpty())
                {
                    timer.cancel();
                    timer = null;
                }
            }
        }
    }

    class PlayPingAudio implements Runnable
    {
        public void run()
        {
            try
            {
                Thread.currentThread().setName("PlayPingAudio");

                audioManager.playAudio(new File(pingAudioFile));
            } catch (FileNotFoundException e)
            {
                logger.warn("Can't find audio file: {}", pingAudioFile);
            } catch (Exception e)
            {
                logger.warn("Unable to play audio: {}", e.toString());
            }
        }
    }
}
