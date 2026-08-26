package gov.takx.api.media;

import com.formdev.flatlaf.FlatClientProperties;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.slider.ISliderListener;
import gov.takx.api.media.slider.SliderEvent;
import gov.takx.api.media.slider.YouTubeSlider;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
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
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * In general a plugin will want to extend this abstract class for its custom media panels. This abstract panel
 * will provide all the generic functions for a typical moving image content type panel.
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

    protected final static ImageIcon PLAY_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "play.png");
    protected final static ImageIcon PAUSE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "pause.png");
    protected final static ImageIcon AUDIO_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "volume.png");
    protected final static ImageIcon MUTE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "volume_off.png");
    protected final static BufferedImage AUDIO_IMAGE = ImageUtils.getImage(UIResources.getUrl(PLATFORM_ICONS, "audio.png"), false);

    protected MediaPlayerOptions options;
    protected IMedia media;
    protected boolean isBlank = false;
    protected java.util.List<IMediaPlayerListener> listeners = new CopyOnWriteArrayList<>();
    protected JPanel titlePanel;
    protected JPanel controlPanel;
    protected YouTubeSlider slider;
    protected JLabel title;
    protected JLabel elapsedAndDurationLabel;
    protected JToggleButton playPauseButton;
    protected volatile boolean isMuted;
    protected volatile boolean sliderDragging;
    protected AtomicReference<BufferedImage> image =
            new AtomicReference<>(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR));
    protected AtomicReference<BufferedImage> scaledImage =
            new AtomicReference<>(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR));
    protected AtomicReference<BufferedImage> paintImage;

    @Inject
    protected ICoreExecutorService executorService;

    @Inject
    protected IMediaPlayerManager mediaPlayerManager;

    /**
     * Constructor
     *
     * @param options Media player options
     */
    protected AMediaPlayer(MediaPlayerOptions options, IMedia media)
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

        // TODO There's a bug here when you mouse out of the window from inside the control or title panel.
        // Ignore the mouseExited event when it occurs on a child element.
        MouseListener userControlsDisplayer = new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                titlePanel.setVisible(true);
                boolean unhide = !(media instanceof IImage) && ((IPlayable) media).getDuration() != null;
                controlPanel.setVisible(unhide);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                // TODO There's a bug here when you mouse out of the window from inside the control or title panel.
                // Ignore the mouseExited event when it occurs on a child element.
                Point p = new Point(e.getLocationOnScreen());
                SwingUtilities.convertPointFromScreen(p, e.getComponent());
                if (!e.getComponent().contains(p))
                {
                    titlePanel.setVisible(false);
                    controlPanel.setVisible(false);
                }
            }
        };
        addMouseListener(userControlsDisplayer);

        if (media != null)
        {
            setBorderColor(media.getColor());

            media.addPropertyChangeListener(MediaProperties.Common.COLOR, this);

            if (media instanceof IImage)
            {
                paintImage = scaledImage;
            } else
            {
                paintImage = image;
            }
        }
    }

    protected void componentResizeListener()
    {
        // If media is a still image, the player is responsible for resizing it to fit the window. For video, the
        // resizing is done natively for efficiency.
        if (getMedia().isPresent() && getMedia().get() instanceof IImage)
        {
            generateScaledCopy();
            repaint();
        }
    }

    @Override
    public void loadMedia(@Nonnull IMedia media)
    {
        unloadMedia();
        this.media = media;

        logger.debug("Loading {}", media);

        final boolean playable = media instanceof IPlayable;
        if (playPauseButton != null) playPauseButton.setEnabled(playable);
        slider.setEnabled(playable);

        title.setText(media.getName());

        // Notify listeners
        for (IMediaPlayerListener listener : listeners)
        {
            listener.onMediaLoaded(this, media);
        }

        media.addPropertyChangeListener(MediaProperties.Common.COLOR, this);
    }

    @Override
    public Optional<IMedia> getMedia()
    {
        return Optional.ofNullable(media);
    }

    @Override
    public Optional<MediaPlayerOptions> getOptions()
    {
        return Optional.of(options);
    }

    @Override
    public void unloadMedia()
    {
        getMedia().ifPresent(m -> {
            logger.debug("Unloading {}", m);

            m.removePropertyChangeListener(MediaProperties.Common.COLOR, this);
            media = null;
        });

        if (playPauseButton != null) playPauseButton.setEnabled(false);
        slider.setEnabled(false);
        blank();
    }

    @Override
    public void addListener(IMediaPlayerListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IMediaPlayerListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void play()
    {
        logger.debug("Play");

        if (options != null && options.isPlaybackControlsEnabled())
        {
            playPauseButton.setSelected(true);
        }
        isBlank = false;
        repaint();
    }

    @Override
    public void pause()
    {
        logger.debug("Pause");

        if (options != null && options.isPlaybackControlsEnabled())
        {
            playPauseButton.setSelected(false);
        }
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

        if (logger.isDebugEnabled())
        {
            logger.debug("seekByPercentage pct={} duration={}", pct, playable.getDuration());
        }

        double time = playable.getDuration() * pct;
        seek(time);
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
        BufferedImage scaledCopy = generateScaledCopy(image.get());
        scaledImage.set(scaledCopy);
    }

    /**
     * Generate a scaled copy of the original image to fit the media player window
     */
    protected BufferedImage generateScaledCopy(BufferedImage orig)
    {
        int origWidth = orig.getWidth();
        int origHeight = orig.getHeight();
        AffineTransform at = new AffineTransform();

        int windowWidth = getWidth() > 0 ? getWidth() : 1;
        int windowHeight = getHeight() > 0 ? getHeight() : 1;

        double widthRatio = windowWidth / (double) origWidth;
        double heightRatio = windowHeight / (double) origHeight;

        double scaleFactor = Math.min(widthRatio, heightRatio);
        at.scale(scaleFactor, scaleFactor);

        // Recommended approach to scaling is to use bicubic interpolation when enlarging an image,
        // bilinear when reducing it
        int interpolation = AffineTransformOp.TYPE_BILINEAR;
        if (scaleFactor > 1.0)
        {
            interpolation = AffineTransformOp.TYPE_BICUBIC;
        }

        int scaledWidth = (int) Math.ceil(origWidth * scaleFactor);
        int scaledHeight = (int) Math.ceil(origHeight * scaleFactor);
        AffineTransformOp scaleOp =
                new AffineTransformOp(at, interpolation);
        BufferedImage scaledCopy = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);

        logger.debug("Generating scaled image, size = {}x{}", scaledWidth, scaledHeight);

        return scaleOp.filter(orig, scaledCopy);
    }

    @Override
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
    public void setBorderColor(Color color)
    {
        if (color != null)
        {
            setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, color));
        } else
        {
            setBorder(null);
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
        JComponent strut = (JComponent) Box.createHorizontalStrut(1);
        titlePanel.add(strut);

        add(titlePanel, "dock north, growx");

        controlPanel = new JPanel();
        controlPanel.setLayout(new MigLayout("ins 0, gapx 0", "[][][grow]"));
        controlPanel.setOpaque(false);
        controlPanel.setVisible(false);

        slider = new YouTubeSlider(options.isPlaybackControlsEnabled());
        controlPanel.add(slider, "h 16!, dock north");
        if (options.isPlaybackControlsEnabled())
        {
            // Allow user to seek with the slider
            slider.addSliderListener(new ISliderListener()
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
                    if (wasPlayingBeforeDrag)
                    {
                        play();
                    }
                    sliderDragging = false;
                }
            });

            playPauseButton = new JToggleButton(PLAY_ICON);
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

        JToggleButton muteButton = new JToggleButton(AUDIO_ICON);
        muteButton.setSelectedIcon(MUTE_ICON);
        muteButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        muteButton.setSelected(false);
        muteButton.setName("muteButton");
        muteButton.addActionListener(e -> mute(muteButton.isSelected()));
        controlPanel.add(muteButton);

        elapsedAndDurationLabel = new JLabel(EMPTY_ELAPSED_AND_DURATION_LABEL);
        elapsedAndDurationLabel.setForeground(Color.white);
        controlPanel.add(elapsedAndDurationLabel);

        JComponent strut2 = (JComponent) Box.createHorizontalStrut(1);
        controlPanel.add(strut2);

        add(controlPanel, "dock south, growx");
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getPropertyName().equals(MediaProperties.Common.COLOR))
        {
            getMedia().ifPresent(media -> {
                if (event.getSource() == media && event.getNewValue() instanceof Color color) setBorderColor(color);
            });
        }
    }
}
