package gov.takx.api.media;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.slider.ISliderListener;
import gov.takx.api.media.slider.SliderEvent;
import gov.takx.api.media.slider.YouTubeSlider;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.MouseEnteredExitedListener;
import gov.takx.commons.swing.MouseRolloverListener;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Extend this abstract class if you need to access its media panels to work with plugin-generated media content.
 * If reading, decoding, and displaying standard media formats (audio and video), use or extend {@link gov.takx.core.media.MediaPlayer}.
 *
 * @implNote The image set on {@link #paintImage} is what will be rendered each time this component is painted.
 *
 * @since 2.37
 */
@Experimental
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection"})
@PseudoBean
public abstract class AMediaPlayer extends JPanel implements IMediaPlayer, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected static final String EMPTY_ELAPSED_AND_DURATION_LABEL = "-:-- / -:--";

    protected final static ImageIcon PLAY_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "play.svg");
    protected final static ImageIcon PAUSE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "pause.svg");
    protected final static ImageIcon AUDIO_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "volume.svg");
    protected final static ImageIcon MUTE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "volume_off.svg");
    protected final static BufferedImage AUDIO_IMAGE = ImageUtils.getImage(UIResources.getUrl(PLATFORM_ICONS, "audio.svg"), false);

    /**
     * @deprecated do not use this to perform long-running operations, like stream decoding.
     */
    @Deprecated
    @DeprecatedApi(since = "5.8", forRemoval = true, removeAt = "5.11")
    @Inject
    protected ICoreExecutorService executorService;

    @Inject
    protected IMediaPlayerManager mediaPlayerManager;

    protected MediaPlayerOptions options;
    protected IMedia media;
    protected boolean isBlank;
    protected final List<IMediaPlayerListener> listeners = new CopyOnWriteArrayList<>();
    protected JPanel titlePanel;
    protected JPanel controlPanel;
    protected YouTubeSlider slider;
    protected JLabel title;
    protected JLabel elapsedAndDurationLabel;
    protected JToggleButton playPauseButton;
    protected volatile boolean isMuted;
    protected volatile boolean sliderDragging;
    protected final AtomicReference<BufferedImage> image = new AtomicReference<>(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR));
    protected final AtomicReference<BufferedImage> scaledImage = new AtomicReference<>(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR));
    protected AtomicReference<BufferedImage> paintImage;
    private boolean stillImage;
    private final MouseEnteredExitedListener mouseEnteredExitedListener = this::mouseEnteredOrExited;

    /**
     * @param options Media player options
     * @param media Media to play
     * @see #loadMedia(IMedia)
     */
    protected AMediaPlayer(@Nonnull MediaPlayerOptions options, @Nullable IMedia media)
    {
        this.options = options;
        this.media = media;

        initComponents();

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                componentResizeListener();
            }
        });

        setName(options.getTitle());

        MouseRolloverListener.addRolloverListener(this, mouseEnteredExitedListener);

        if (media != null) loadMedia(media);
    }

    /**
     * Handle a mouse enter/exit event, to show or hide our control panel.
     * @since 5.8
     */
    protected void mouseEnteredOrExited(MouseEvent e)
    {
        if (e.getID() == MouseEvent.MOUSE_ENTERED)
        {
            titlePanel.setVisible(true);
            boolean showControls = !(media instanceof IImage) && ((IPlayable) media).getDuration() != null;
            controlPanel.setVisible(showControls);
        } else
        {
            titlePanel.setVisible(false);
            controlPanel.setVisible(false);
        }
    }

    protected void componentResizeListener()
    {
        // If media is a still image, the player is responsible for resizing it to fit the window. For video, the
        // resizing is done natively for efficiency.
        if (media instanceof IImage)
        {
            generateScaledCopy();
            repaint();
        }
    }

    @Override
    public void loadMedia(@Nonnull IMedia media)
    {
        logger.debug("Loading {}", media);

        unloadMedia();
        this.media = media;
        stillImage = media instanceof IImage;

        setBorderColor(media.getColor());
        media.addPropertyChangeListener(MediaProperties.Common.COLOR, this);
        paintImage = media instanceof IImage ? scaledImage : image;

        final boolean playable = media instanceof IPlayable;
        playPauseButton.setEnabled(playable);
        slider.setEnabled(playable);

        title.setText(media.getName());

        // Notify listeners
        ExceptionUtils.safeDoOperation(listeners, listener -> listener.onMediaLoaded(this, media));
    }

    @Override
    @Nonnull
    public Optional<IMedia> getMedia()
    {
        return Optional.ofNullable(media);
    }

    @Override
    @Nonnull
    public Optional<MediaPlayerOptions> getOptions()
    {
        return Optional.of(options);
    }

    @Override
    public void unloadMedia()
    {
        getMedia().ifPresent(media -> {
            logger.debug("Unloading {}", media);

            media.removePropertyChangeListener(MediaProperties.Common.COLOR, this);
            this.media = null;
            stillImage = false;
        });

        playPauseButton.setEnabled(false);
        slider.setEnabled(false);
        blank();
    }

    /**
     * @return {@code true} if we're processing a still image
     * @since 5.8
     */
    protected boolean isStillImage()
    {
        return stillImage;
    }

    @Override
    public void addListener(@Nonnull IMediaPlayerListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull IMediaPlayerListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void play()
    {
        logger.debug("Play");

        playPauseButton.setSelected(true);
        isBlank = false;
        repaint();
    }

    @Override
    public void pause()
    {
        logger.debug("Pause");
        playPauseButton.setSelected(false);
    }

    @Override
    public void blank()
    {
        logger.debug("Blank");
        isBlank = true;
        title.setText("");
        repaint();
    }

    @Override
    public void seek(double timestamp)
    {
        logger.debug("Seek {}", timestamp);

        isBlank = false;

        repaint();
    }

    private void seekByPercentage(double pct)
    {
        if (getMedia().isEmpty() || !(getMedia().get() instanceof final IPlayable playable)) return;

        if (logger.isDebugEnabled()) logger.debug("seekByPercentage pct={} duration={}", pct, playable.getDuration());

        //noinspection DataFlowIssue - IJ is wrong
        seek(playable.getDuration() * pct);
    }

    /**
     * Mute or unmute audio
     *
     * @param mute True to mute audio
     */
    protected void mute(boolean mute)
    {
        isMuted = mute;
    }

    /**
     * Called by media manager to release any resources and notify listeners that we closed.
     *
     * @since 5.8
     */
    @OverridingMethodsMustInvokeSuper
    @Override
    public void destroy()
    {
        MouseRolloverListener.removeRolloverListener(this, mouseEnteredExitedListener);
        listeners.clear();
        unloadMedia();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        BufferedImage img = paintImage.get();

        if (img != null)
        {
            Graphics2D g2 = (Graphics2D) g.create();

            // Make sure the image is centered
            Dimension panelDims = getSize();
            int x = Math.max((panelDims.width - img.getWidth()) / 2, 0);
            int y = Math.max((panelDims.height - img.getHeight()) / 2, 0);

            // Draw the image
            if (!isBlank)
            {
                g2.drawImage(img, x, y, null);
            }

            // For every component contained by this panel:
            // * make it transparent
            // * paint the translucent layer in its place
            g2.setColor(new Color(0, 0, 0, 64));
            Area area = new Area();
            for (Component component : getComponents())
            {
                JComponent c = (JComponent) component;
                if (c.isVisible())
                {
                    c.setOpaque(false);
                    if (c instanceof JPanel)
                    {
                        area.add(new Area(c.getBounds()));
                    }
                }
            }
            g2.fill(area);
            g2.dispose();
        }
    }

    /**
     * Generate a scaled copy of the original image to fit the media player window
     */
    protected void generateScaledCopy()
    {
        scaledImage.set(generateScaledCopy(image.get()));
    }

    /**
     * Generate a scaled copy of the original image to fit the media player window
     */
    protected BufferedImage generateScaledCopy(BufferedImage orig)
    {
        int origWidth = orig.getWidth();
        int origHeight = orig.getHeight();
        var at = new AffineTransform();

        int windowWidth = getWidth() > 0 ? getWidth() : 1;
        int windowHeight = getHeight() > 0 ? getHeight() : 1;

        double widthRatio = windowWidth / (double) origWidth;
        double heightRatio = windowHeight / (double) origHeight;

        double scaleFactor = Math.min(widthRatio, heightRatio);
        at.scale(scaleFactor, scaleFactor);

        // Recommended approach to scaling is to use bicubic interpolation when enlarging an image,
        // bilinear when reducing it
        int interpolation = scaleFactor > 1.0 ? AffineTransformOp.TYPE_BICUBIC : AffineTransformOp.TYPE_BILINEAR;
        int scaledWidth = (int) Math.ceil(origWidth * scaleFactor);
        int scaledHeight = (int) Math.ceil(origHeight * scaleFactor);
        var scaleOp = new AffineTransformOp(at, interpolation);
        var scaledCopy = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);

        logger.debug("Generating scaled image, size = {}x{}", scaledWidth, scaledHeight);

        return scaleOp.filter(orig, scaledCopy);
    }

    @Override
    @Nonnull
    public Component getComponent()
    {
        return this;
    }

    /**
     * Set the color for the border of this player
     *
     * @param color the border color (ARGB)
     */
    @Override
    public void setBorderColor(@Nullable Color color)
    {
        setBorder(color != null ? BorderFactory.createMatteBorder(2, 2, 2, 2, color) : null);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getPropertyName().equals(MediaProperties.Common.COLOR))
        {
            getMedia().ifPresent(media -> {
                if (event.getSource() == media) setBorderColor((Color) event.getNewValue());
            });
        }
    }

    private class SliderListener implements ISliderListener
    {
        boolean wasPlayingBeforeDrag;

        @Override
        public void sliderMoved(SliderEvent e)
        {
            seekByPercentage(e.getThumbPercent());
        }

        @Override
        public void sliderDragBegin(SliderEvent e)
        {
            wasPlayingBeforeDrag = isPlaying();
            pause();
            sliderDragging = true;
        }

        @Override
        public void sliderDragEnd(SliderEvent e)
        {
            if (wasPlayingBeforeDrag) play();
            sliderDragging = false;
        }
    }

    private void initComponents()
    {
        setLayout(new MigLayout("fill, ins 0, gap 0"));

        titlePanel = new JPanel();
        titlePanel.setLayout(new MigLayout("ins 0, gapx 0", "[][grow][]"));
        titlePanel.setOpaque(false);
        title = new JLabel("No Media Loaded");
        title.setForeground(Color.white);
        final Font font = title.getFont();
        title.setFont(font.deriveFont(font.getSize() + 4f));
        titlePanel.add(title);
        titlePanel.add(Box.createHorizontalStrut(1));

        add(titlePanel, "dock north, growx");

        controlPanel = new JPanel();
        controlPanel.setLayout(new MigLayout("ins 0, gapx 0", "[][][grow]"));
        controlPanel.setOpaque(false);
        controlPanel.setVisible(false);

        slider = new YouTubeSlider(options.isPlaybackControlsEnabled());
        controlPanel.add(slider, "h 16!, dock north");
        playPauseButton = new JToggleButton(PLAY_ICON);

        if (options.isPlaybackControlsEnabled())
        {
            // Allow user to seek with the slider
            slider.addSliderListener(new SliderListener());

            playPauseButton.setSelectedIcon(PAUSE_ICON);
            playPauseButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
            playPauseButton.setSelected(false);
            playPauseButton.setName("playPauseButton");
            playPauseButton.addActionListener(e -> {
                if (playPauseButton.isSelected())
                {
                    play();
                } else
                {
                    pause();
                }
            });
            controlPanel.add(playPauseButton);
        }

        var muteButton = new JToggleButton(AUDIO_ICON);
        muteButton.setSelectedIcon(MUTE_ICON);
        muteButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        muteButton.setSelected(false);
        muteButton.setName("muteButton");
        muteButton.addActionListener(e -> mute(muteButton.isSelected()));
        controlPanel.add(muteButton);

        elapsedAndDurationLabel = new JLabel(EMPTY_ELAPSED_AND_DURATION_LABEL);
        elapsedAndDurationLabel.setForeground(Color.white);
        controlPanel.add(elapsedAndDurationLabel);
        controlPanel.add(Box.createHorizontalStrut(1));

        add(controlPanel, "dock south, growx");
    }
}
