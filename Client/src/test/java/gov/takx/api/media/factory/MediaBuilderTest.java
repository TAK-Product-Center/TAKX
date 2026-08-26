package gov.takx.api.media.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.media.AudioBuilder;
import gov.takx.api.media.IAudio;
import gov.takx.api.media.IImage;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IVideo;
import gov.takx.api.media.ImageBuilder;
import gov.takx.api.media.MediaProperties;
import gov.takx.api.media.VideoBuilder;
import gov.takx.core.media.MediaFactory;
import gov.takx.core.media.MediaLengthAnalyzer;
import gov.takx.dataservices.persist.Image;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 2.15
 */
@SuppressWarnings({"NonConstantFieldWithUpperCaseName", "DataFlowIssue"})
public class MediaBuilderTest extends AContainerTest
{
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 764;
    private static final URL LOCAL_VIDEO_URL = MediaBuilderTest.class.getResource("/mediaBuilderTestVid.mp4");
    private static final URL LOCAL_IMAGE_URL = MediaBuilderTest.class.getResource("/mediaBuilderTestImage.png");
    private static URI LOCAL_IMAGE_URI;
    private static URI LOCAL_VIDEO_URI;
    private static URI FAKE_URI;
    private static File LOCAL_VIDEO_FILE;
    private static File LOCAL_IMAGE_FILE;
    private static String THUMBNAIL_URL;
    private static final String MEDIA_NAME = "audioName";
    private static final String SOURCE_KEY = "sourceKey";
    private static final long FILE_SIZE = 3000L;
    private static final String INFO = "someInfo";
    private static final long LAST_MODIFIED = System.currentTimeMillis();
    private static final long DURATION = 1000L;
    private static final String RAWAUDIO_FORMAT = "f32le";
    private static final Map<String, String> META_DATA = new HashMap<>();
    // Since we don't actually try to play this back, we can reuse the data structure.
    private static final Map<String, String> PLAYBACK_OPTIONS = META_DATA;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, String>> typeRef = new TypeReference<>()
    {
    };

    private MediaFactory mediaFactory;
    private MediaLengthAnalyzer mediaLengthAnalyzer;

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @BeforeAll
    public void setUp() throws Exception
    {
        FAKE_URI = new URI("https://no.host/something");
        LOCAL_IMAGE_URI = LOCAL_IMAGE_URL.toURI();
        LOCAL_VIDEO_URI = LOCAL_VIDEO_URL.toURI();
        LOCAL_VIDEO_FILE = new File(LOCAL_VIDEO_URI);
        LOCAL_IMAGE_FILE = new File(LOCAL_IMAGE_URI);
        THUMBNAIL_URL = LOCAL_IMAGE_URL.toExternalForm();

        mediaFactory = getBean(MediaFactory.class);
        mediaLengthAnalyzer = getBean(MediaLengthAnalyzer.class);

        META_DATA.put("metaKey1", "metaVal1");
        META_DATA.put("metaKey2", "metaVal2");
    }

    /**
     * Make sure that no fields are assigned except for the required constructor fields
     */
    @Test
    public void testUnassignedBaseFields()
    {
        AudioBuilder audioBuilder = new AudioBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        audioBuilder.mediaFactory = mediaFactory;
        IAudio media = audioBuilder.build();

        // Required by constructor
        assertThat(media.getName()).isEqualTo(MEDIA_NAME);
        assertThat(media.getKeyOfSource()).isEqualTo(SOURCE_KEY);

        // Unassigned fields
        assertThat(media.getThumbnailURL()).isNull();
        assertThat(media.getLocalFilePath()).isNull();
        assertThat(media.getFileSize()).isNull();
        assertThat(media.getLastModified()).isNull();
        assertThat(media.getMetaData()).isNull();
        assertThat(media.getFormat()).isNull();
        assertThat(media.getOptions()).isNull();
    }

    @Test
    public void testBuilderBaseFieldAssignment()
    {
        AudioBuilder audioBuilder = new AudioBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        audioBuilder.mediaFactory = mediaFactory;

        audioBuilder.setThumbnailURL(THUMBNAIL_URL)
                .setLastModified(LAST_MODIFIED)
                .setFileSize(FILE_SIZE)
                .setMetaData(META_DATA)
                .setInfo(INFO);

        IAudio audio = audioBuilder.build();

        assertThat(audio.getName()).isEqualTo(MEDIA_NAME);
        assertThat(audio.getKeyOfSource()).isEqualTo(SOURCE_KEY);
        assertThat(audio.getThumbnailURL()).isEqualTo(THUMBNAIL_URL);
        assertThat(audio.getInfo()).isEqualTo(INFO);
        assertThat(audio.getSourceUri()).isEqualTo(FAKE_URI);
        assertThat(audio.getLastModified()).isEqualTo(LAST_MODIFIED);
        assertThat(audio.getFileSize()).isEqualTo(FILE_SIZE);
        assertThat(audio.getMetaData()).isEqualTo(META_DATA);

        validateMetadata(audio, META_DATA);
    }

    /**
     * Verify that a JSON string properly reifies into a map that is equivalent to what we set.
     * Previously tests were trying to compare the JSON representations of both as a string.
     * This could randomly fail since the ordering of properties in a JSON object is not
     * guaranteed to be consistent.
     *
     * @since 5.0
     */
    private void validateJsonMap(String fieldName, IMedia media, Map<String, String> map)
    {
        final String jsonString = CdiTestUtils.getField(media, fieldName);
        if (map == null)
        {
            assertThat(jsonString).isNull();
        } else
        {
            try
            {
                assertThat(mapper.readValue(jsonString, typeRef)).isEqualTo(map);
            } catch (JsonProcessingException e)
            {
                throw new RuntimeException(e); // Avoid a checked exception so this method can be called in a lambda
            }
        }
    }

    /**
     * Verify that the JSON metadata string properly reifies into a map that is equivalent to what we set.
     *
     * @since 5.0
     */
    private void validateMetadata(IMedia media, Map<String, String> metaData)
    {
        validateJsonMap("jsonMetaData", media, metaData);
    }

    /**
     * Verify that the JSON options string properly reifies into a map that is equivalent to what we set.
     *
     * @since 5.0
     */
    private void validateOptions(IMedia media, Map<String, String> options)
    {
        validateJsonMap("jsonOptions", media, options);
    }

    @Test
    @SuppressLogging(Image.class)
    public void testBaseFieldPropertyChanges()
    {
        AudioBuilder audioBuilder = new AudioBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        audioBuilder.mediaFactory = mediaFactory;
        IAudio audio = audioBuilder.build();

        testSetUnset(audio, MediaProperties.Common.INFO, INFO, audio::getInfo, audio::setInfo);
        testSetUnset(audio, MediaProperties.Common.THUMBNAIL_URL, THUMBNAIL_URL, audio::getThumbnailURL, audio::setThumbnailURL);
        testSetUnset(audio, MediaProperties.Common.FILE_SIZE, FILE_SIZE, audio::getFileSize, audio::setFileSize);
        testSetUnset(audio, MediaProperties.Common.LAST_MODIFIED, LAST_MODIFIED, audio::getLastModified, audio::setLastModified);
        testSetUnset(audio, MediaProperties.Common.META_DATA, META_DATA, audio::getMetaData, metadata -> {
            audio.setMetaData(metadata);
            validateMetadata(audio, metadata);
        });
        testSetUnset(audio, MediaProperties.Common.FORMAT, RAWAUDIO_FORMAT, audio::getFormat, audio::setFormat);
        testSetUnset(audio, MediaProperties.Common.OPTIONS, PLAYBACK_OPTIONS, audio::getOptions, options -> {
            audio.setOptions(options);
            validateOptions(audio, options);
        });
    }

    /**
     * Test that a given property (expected to be null initially) fires expected PCEs when given a value and then
     * set back to null.
     *
     * @since 5.0
     */
    private <T> void testSetUnset(IMedia media, String property, T value, Supplier<T> getter, Consumer<T> setter)
    {
        var pcv = new PropertyChangeVerifier(media, property, null, value);
        setter.accept(value);
        assertThat(getter.get()).isEqualTo(value);
        pcv.assertReceived();

        pcv = new PropertyChangeVerifier(media, property, value, null);
        setter.accept(null);
        assertThat(getter.get()).isNull();
        pcv.assertReceived();
    }

    /**
     * Tests fields that should be auto-populated when a media file is set
     */
    @Test
    @SuppressLogging(Image.class)
    public void testAutoPopulateBaseFields() throws IOException
    {
        AudioBuilder audioBuilder = new AudioBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        audioBuilder.mediaFactory = mediaFactory;

        IAudio audio = audioBuilder.build();

        // Can't populate from a URL
        assertThat(audio.getLastModified()).isNull();
        assertThat(audio.getFileSize()).isNull();

        // Setting the source to a local file will populate the last modified and file size fields
        final long lastModified = Files.getLastModifiedTime(LOCAL_IMAGE_FILE.toPath()).toMillis();
        final var pcvLastModified = new PropertyChangeVerifier(audio, MediaProperties.Common.LAST_MODIFIED, null, lastModified);
        final long fileSize = Files.size(LOCAL_IMAGE_FILE.toPath());
        final var pcvSize = new PropertyChangeVerifier(audio, MediaProperties.Common.FILE_SIZE, null, fileSize);

        audio.setSourceUri(LOCAL_IMAGE_URI);

        pcvLastModified.assertReceived();
        pcvSize.assertReceived();

        // Now the fields should be populated
        assertThat(audio.getLastModified()).isEqualTo(lastModified);
        assertThat(audio.getFileSize()).isEqualTo(fileSize);
    }

    @Test
    public void testAudioBuilder()
    {
        AudioBuilder audioBuilder = new AudioBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        audioBuilder.mediaFactory = mediaFactory;
        IAudio audio = audioBuilder.setDuration(DURATION).build();

        assertThat(audio.getDuration()).isEqualTo(DURATION);
    }

    @Test
    public void testAudioPropertyChange()
    {
        AudioBuilder audioBuilder = new AudioBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        audioBuilder.mediaFactory = mediaFactory;

        IAudio audio = audioBuilder.build();

        assertThat(audio.getDuration()).isNull();

        var pcv = new PropertyChangeVerifier(audio, MediaProperties.Audio.DURATION, null, DURATION);
        audio.setDuration(DURATION);
        pcv.assertReceived();

        // The duration should be automatically generated
        Long extractedDuration = mediaLengthAnalyzer.analyzeDuration(LOCAL_VIDEO_FILE.getAbsolutePath()).orElse(null);
        assertThat(extractedDuration).isNotNull();
        pcv = new PropertyChangeVerifier(audio, MediaProperties.Audio.DURATION, DURATION, extractedDuration);
        audio.setSourceUri(LOCAL_VIDEO_URI);
        pcv.assertReceived();
    }

    @Test
    public void testVideoBuilder()
    {
        VideoBuilder mediaBuilder = new VideoBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        mediaBuilder.mediaFactory = mediaFactory;
        IVideo media = mediaBuilder.setDuration(DURATION).build();

        assertThat(media.getDuration()).isEqualTo(DURATION);
    }

    @Test
    public void testVideoPropertyChange()
    {
        VideoBuilder mediaBuilder = new VideoBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        mediaBuilder.mediaFactory = mediaFactory;
        IVideo media = mediaBuilder.build();

        assertThat(media.getDuration()).isNull();

        var pcv = new PropertyChangeVerifier(media, MediaProperties.Video.DURATION, null, DURATION);
        media.setDuration(DURATION);
        pcv.assertReceived();

        // The duration should be automatically generated
        Long extractedDuration = mediaLengthAnalyzer.analyzeDuration(LOCAL_VIDEO_FILE.getAbsolutePath()).orElse(null);
        assertThat(extractedDuration).isNotNull();
        pcv = new PropertyChangeVerifier(media, MediaProperties.Video.DURATION, DURATION, extractedDuration);
        media.setSourceUri(LOCAL_VIDEO_URI);
        pcv.assertReceived();
    }

    @Test
    @SuppressLogging(Image.class)
    public void testImageBuilder()
    {
        ImageBuilder mediaBuilder = new ImageBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        mediaBuilder.mediaFactory = mediaFactory;

        IImage media = mediaBuilder
                .setWidth(WIDTH)
                .setHeight(HEIGHT)
                .build();

        assertThat(media.getWidth()).isEqualTo(WIDTH);
        assertThat(media.getHeight()).isEqualTo(HEIGHT);
    }

    @Test
    @SuppressLogging(Image.class)
    public void testImagePropertyChange() throws IOException
    {
        ImageBuilder mediaBuilder = new ImageBuilder(MEDIA_NAME, SOURCE_KEY, FAKE_URI);
        mediaBuilder.mediaFactory = mediaFactory;

        IImage media = mediaBuilder.build();

        assertThat(media.getWidth()).isNull();
        assertThat(media.getHeight()).isNull();

        assertThat(LOCAL_IMAGE_FILE).exists();
        BufferedImage image = ImageIO.read(LOCAL_IMAGE_FILE);
        assertThat(image.getWidth()).isPositive();
        assertThat(image.getHeight()).isPositive();

        final var pcvWidth = new PropertyChangeVerifier(media, MediaProperties.Image.WIDTH, null, image.getWidth());
        final var pcvHeight = new PropertyChangeVerifier(media, MediaProperties.Image.HEIGHT, null, image.getHeight());
        media.setSourceUri(LOCAL_IMAGE_URI);
        pcvWidth.assertReceived();
        pcvHeight.assertReceived();
    }

    /**
     * PropertyChangeListener that can verify old/new values and that an event was actually received.
     * @since 5.0
     */
    private static class PropertyChangeVerifier implements PropertyChangeListener
    {
        private final String propertyName;
        private final Object oldValue;
        private final Object newValue;
        private final IMedia media;
        private boolean eventReceived = false;

        public PropertyChangeVerifier(IMedia media, String propertyName, Object oldValue, Object newValue)
        {
            this.propertyName = propertyName;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.media = media;

            media.addPropertyChangeListener(propertyName, this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            eventReceived = true;
            assertThat(evt.getPropertyName()).isEqualTo(propertyName);
            assertThat(evt.getOldValue()).isEqualTo(oldValue);
            assertThat(evt.getNewValue()).isEqualTo(newValue);
            media.removePropertyChangeListener(propertyName, this);
        }

        void assertReceived()
        {
            media.removePropertyChangeListener(propertyName, this);
            assertThat(eventReceived).withFailMessage("'%s' PCE not received", propertyName).isTrue();
        }
    }
}
