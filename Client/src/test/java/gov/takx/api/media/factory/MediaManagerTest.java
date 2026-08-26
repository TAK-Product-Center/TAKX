package gov.takx.api.media.factory;

import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.media.AudioBuilder;
import gov.takx.api.media.IAudio;
import gov.takx.api.media.IImage;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IVideo;
import gov.takx.api.media.ImageBuilder;
import gov.takx.api.media.MediaFormats;
import gov.takx.api.media.MediaType;
import gov.takx.api.media.VideoBuilder;
import gov.takx.core.media.MediaFactory;
import gov.takx.core.persist.MediaPersist;
import gov.takx.dataservices.persist.Image;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @since 2.15
 */
@SuppressWarnings("unused")
public class MediaManagerTest extends AIntegrationTest
{
    private static final URI FAKE_URI;

    private static final List<String> IMAGE_TYPES = Arrays.asList(
            "t.bmp", "t.gif", "t.jpg", "t.jpeg", "t.png", "t.tif", "t.tiff");

    private static final List<String> VIDEO_TYPES = Arrays.asList(
            "t.3gp", "t.3g2", "t.avi", "t.dv", "t.flv", "t.m2ts", "t.m4v", "t.mjpeg", "t.mkv", "t.mov", "t.mp4",
            "t.mpeg", "t.mpg", "t.mxf", "t.swf", "t.ts", "t.webm", "t.wmv");

    private static final List<String> AUDIO_TYPES = Arrays.asList(
            "t.aac", "t.au", "t.flac", "t.mp3", "t.wav", "t.wma");

    private static final List<String> UNKNOWN_TYPES = Arrays.asList(
            "mp3", "mp3.", "t.mp3.bob", "t.mp", "t.wm", "t.bob", "t.doc");

    private final MediaFactory mf = new MediaFactory();

    static
    {
        try
        {
            FAKE_URI = new URI("https://no.host/something");
        } catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }

    private IMediaManager mediaManager;

    @Mocked
    private MediaPersist mediaPersist;

    @BeforeEach
    public void beforeMethod()
    {
        mediaManager = getBean(IMediaManager.class);
    }

    @Test
    void testGetSupportedFormats()
    {
        assertThat(mediaManager.getSupportedFormats()).containsOnly(MediaFormats.ALL_FORMATS);
    }

    @Test
    public void testExtensions()
    {
        // Image
        IMAGE_TYPES.forEach(s -> assertThat(mediaManager.getType(s)).isEqualTo(MediaType.Image));

        // Video
        VIDEO_TYPES.forEach(s -> assertThat(mediaManager.getType(s)).isEqualTo(MediaType.Video));

        // Audio
        AUDIO_TYPES.forEach(s -> assertThat(mediaManager.getType(s)).isEqualTo(MediaType.Audio));

        // More complicated path
        assertThat(mediaManager.getType("/lkfj/j.0.t.wma")).isEqualTo(MediaType.Audio);

        // Bad or unknown extensions
        UNKNOWN_TYPES.forEach(s -> assertThat(mediaManager.getType(s)).isEqualTo(MediaType.Unknown));
    }

    /**
     * Makes sure that all media added are returned by getAllMedia.
     */
    @Test
    @SuppressLogging(Image.class)
    public void testGetAllMedia()
    {
        final long time = System.currentTimeMillis();

        AudioBuilder audioBuilder = new AudioBuilder("1", "source", FAKE_URI);
        audioBuilder.mediaFactory = mf;
        IAudio m1 = audioBuilder.build();
        mediaManager.addMedia(m1);

        VideoBuilder videoBuilder = new VideoBuilder("2", "source", FAKE_URI);
        videoBuilder.mediaFactory = mf;
        IVideo m2 = videoBuilder.build();
        mediaManager.addMedia(m2);

        ImageBuilder imageBuilder = new ImageBuilder("3", "source", FAKE_URI);
        imageBuilder.mediaFactory = mf;
        IImage m3 = imageBuilder.build();
        mediaManager.addMedia(m3);

        audioBuilder = new AudioBuilder("4", "source", FAKE_URI);
        audioBuilder.mediaFactory = mf;
        IAudio m4 = audioBuilder.build();
        mediaManager.addMedia(m4);

        videoBuilder = new VideoBuilder("5", "source", FAKE_URI);
        videoBuilder.mediaFactory = mf;
        IVideo m5 = videoBuilder.build();
        mediaManager.addMedia(m5);

        imageBuilder = new ImageBuilder("6", "source", FAKE_URI);
        imageBuilder.mediaFactory = mf;
        IImage m6 = imageBuilder.build();
        mediaManager.addMedia(m6);

        imageBuilder = new ImageBuilder("7", "source", FAKE_URI);
        imageBuilder.mediaFactory = mf;
        IImage m7 = imageBuilder.build();
        mediaManager.addMedia(m7);

        Stream.of(m1, m2, m3, m4, m5, m6, m7).forEach(m -> mediaManager.mediaExists(m.getName(), m.getKeyOfSource()));

        assertThat(mediaManager.getAllMedia()).containsOnly(m1, m2, m3, m4, m5, m6, m7);
    }

    @Test
    public void testAlreadyExists()
    {
        AudioBuilder audioBuilder = new AudioBuilder("name1", "source", FAKE_URI);
        audioBuilder.mediaFactory = mf;
        IAudio m1 = audioBuilder.build();
        mediaManager.addMedia(m1);

        // Fail. Can't have 2 with the same name
        audioBuilder = new AudioBuilder("name1", "source", FAKE_URI);
        audioBuilder.mediaFactory = mf;
        IAudio m1Copy = audioBuilder.build();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> mediaManager.addMedia(m1Copy));
    }
}
