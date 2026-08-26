package gov.takx.core.media;

import gov.tak.api.video.ConnectionEntry;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.VideoBuilder;
import gov.takx.dataservices.utils.ConnectionEntryConverter;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.3
 */
public class VideoConnectionManagerTest extends AIntegrationTest
{
    private VideoConnectionManager videoConnectionManager;
    private IMediaManager mediaManager;

    @Override
    protected void postProjectOpen()
    {
        videoConnectionManager = getBean(VideoConnectionManager.class);
        mediaManager = getBean(IMediaManager.class);

        cleanup();
    }

    @AfterEach
    void afterEach()
    {
        cleanup();
    }

    private void cleanup()
    {
        mediaManager.getAllMedia().forEach(media -> mediaManager.removeMedia(media));
        videoConnectionManager.getConnectionEntries(null, false).forEach(ce -> videoConnectionManager.removeConnectionEntry(ce));
    }

    private ConnectionEntry createConnectionEntry(String alias, String uid)
    {
        var entry = new ConnectionEntry(alias, "commondatastorage.googleapis.com", 80, -1, "/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                ConnectionEntry.Protocol.HTTP, 0, 0, 0, "", ConnectionEntry.Source.EXTERNAL);
        entry.setUID(uid);
        return entry;
    }

    @Test
    void test_converterEncodeDecode()
    {
        final var converter = new ConnectionEntryConverter();
        final var originalEntry = createConnectionEntry("convert", UUID.randomUUID().toString());

        final String json = converter.convertToDatabaseColumn(originalEntry);
        assertThat(json).isNotNull();

        final var decodedEntry = converter.convertToEntityAttribute(json);
        assertThat(decodedEntry).isEqualTo(originalEntry);
    }

    @Test
    public void test_addEntry()
    {
        var entry = createConnectionEntry("test 1", UUID.randomUUID().toString());
        videoConnectionManager.addConnectionEntry(entry);
        assertThat(videoConnectionManager.getConnectionEntries(null, false)).hasSize(1).containsOnly(entry);
        assertThat(mediaManager.getAllMedia()).hasSize(1);
    }

    @Test
    public void test_addMedia_registersGivenEntry()
    {
        var entry = createConnectionEntry("test 2", UUID.randomUUID().toString());
        var media = new VideoBuilder("test 2", "VideoConnectionManagerTest", URI.create(ConnectionEntry.getURL(entry, true)))
                .setConnectionEntry(entry)
                .build();

        mediaManager.addMedia(media);

        assertThat(videoConnectionManager.getConnectionEntries(null, false)).containsExactly(entry);
        assertThat(mediaManager.getAllMedia()).containsExactly(media);
    }

    @Test
    public void test_addMultipleEntries()
    {
        ConnectionEntry entry;
        List<ConnectionEntry> entriesToAdd = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            entry = createConnectionEntry("test 3_" + i, UUID.randomUUID().toString());
            videoConnectionManager.addConnectionEntry(entry);
            entriesToAdd.add(entry);
        }
        var allConnectionEntries = videoConnectionManager.getConnectionEntries(null, false);
        assertThat(allConnectionEntries).hasSize(10).containsOnlyOnceElementsOf(entriesToAdd);

        var allMedia = mediaManager.getAllMedia();
        assertThat(allMedia).hasSize(10);
        assertThat(allMedia.stream().map(media -> videoConnectionManager.getConnectionEntry(media)).toList()).hasSameElementsAs(allConnectionEntries);
    }

    @Test
    public void test_addAndRemoveEntriesByEntry()
    {
        ConnectionEntry entry;
        List<ConnectionEntry> entriesToRemove = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            entry = createConnectionEntry("test 4_" + i, UUID.randomUUID().toString());
            videoConnectionManager.addConnectionEntry(entry);
            if (i % 2 == 0) entriesToRemove.add(entry);
        }

        entriesToRemove.forEach(entryToRemove -> videoConnectionManager.removeConnectionEntry(entryToRemove));

        var allConnectionEntries = videoConnectionManager.getConnectionEntries(null, false);
        assertThat(allConnectionEntries).hasSize(5);
        assertThat(allConnectionEntries).doesNotContainAnyElementsOf(entriesToRemove);

        var allMedia = mediaManager.getAllMedia();
        assertThat(allMedia).hasSize(5);
        assertThat(allMedia.stream().map(media -> videoConnectionManager.getConnectionEntry(media)).toList()).hasSameElementsAs(allConnectionEntries);
    }

    @Test
    public void test_removeEntryByUid()
    {
        String uid1 = UUID.randomUUID().toString();
        var entry1 = createConnectionEntry("test 5_1", uid1);
        String uid2 = UUID.randomUUID().toString();
        var entry2 = createConnectionEntry("test 5_2", uid2);
        videoConnectionManager.addConnectionEntries(List.of(entry1, entry2));

        assertThat(videoConnectionManager.getConnectionEntries(null, false)).hasSize(2);
        assertThat(mediaManager.getAllMedia()).hasSize(2);

        videoConnectionManager.removeConnectionEntry(uid1);

        assertThat(videoConnectionManager.getConnectionEntries(null, false)).hasSize(1).containsExactly(entry2);
        assertThat(mediaManager.getAllMedia()).hasSize(1);
    }

    @Test
    public void test_removeMultipleEntryByUid()
    {
        ConnectionEntry entry;
        Set<String> uids = new HashSet<>();
        List<ConnectionEntry> removedEntries = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            String uid = UUID.randomUUID().toString();
            entry = createConnectionEntry("test 6_" + i, uid);
            videoConnectionManager.addConnectionEntry(entry);
            if (i % 2 == 0)
            {
                uids.add(uid);
                removedEntries.add(entry);
            }
        }

        assertThat(videoConnectionManager.getConnectionEntries(null, false)).hasSize(10);
        assertThat(mediaManager.getAllMedia()).hasSize(10);

        videoConnectionManager.removeConnectionEntries(uids);

        assertThat(videoConnectionManager.getConnectionEntries(null, false))
                .hasSize(5)
                .doesNotContainAnyElementsOf(removedEntries)
                .map(ce -> ce.getUID())
                .doesNotContainAnyElementsOf(uids);
        assertThat(mediaManager.getAllMedia()).hasSize(5);
    }

    @Test
    public void test_removeEntryByMedia()
    {
        var entry = createConnectionEntry("test 7", UUID.randomUUID().toString());
        videoConnectionManager.addConnectionEntry(entry);
        assertThat(videoConnectionManager.getConnectionEntries(null, false)).hasSize(1);
        assertThat(mediaManager.getAllMedia()).hasSize(1);
        // there should only be one IMedia here (the one we added), so this should only be a 1 item search, but I'm being more thorough for the sake of the test
        var correlatedMedia = mediaManager.getAllMedia().stream().filter(media -> videoConnectionManager.getConnectionEntry(media) != null).findFirst().orElse(null);
        assertThat(correlatedMedia).isNotNull();

        mediaManager.removeMedia(correlatedMedia);
        assertThat(videoConnectionManager.getConnectionEntries(null, false)).hasSize(0);
        assertThat(mediaManager.getAllMedia()).hasSize(0);
    }

    @Test
    public void test_getEntryByUid()
    {
        String uid = UUID.randomUUID().toString();
        var entry = createConnectionEntry("test 8", uid);
        videoConnectionManager.addConnectionEntry(entry);
        assertThat(videoConnectionManager.getConnectionEntry(uid)).isSameAs(entry);
        assertThat(videoConnectionManager.getConnectionEntries(null, false)).hasSize(1).containsOnly(entry);
        assertThat(mediaManager.getAllMedia()).hasSize(1);
    }

    @Test
    public void test_getMultipleEntryByUid()
    {
        ConnectionEntry entry;

        Set<String> uids = new HashSet<>();
        List<ConnectionEntry> entriesToCheck = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            String uid = UUID.randomUUID().toString();
            entry = createConnectionEntry("test 9_" + i, uid);
            videoConnectionManager.addConnectionEntry(entry);
            if (i % 2 == 0)
            {
                uids.add(uid);
                entriesToCheck.add(entry);
            }
        }

        var allCEs = videoConnectionManager.getConnectionEntries(null, false);
        var filteredCEs = videoConnectionManager.getConnectionEntries(uids, false);
        assertThat(allCEs).hasSize(10).containsAll(filteredCEs);
        assertThat(mediaManager.getAllMedia()).hasSize(10);
        assertThat(filteredCEs).hasSize(5).containsExactlyInAnyOrderElementsOf(entriesToCheck);
    }

    @Test
    public void test_getEntryByMedia()
    {
        final var entry = createConnectionEntry("test 10", UUID.randomUUID().toString());
        final var media = new VideoBuilder("test 10", "VideoConnectionManagerTest", URI.create(ConnectionEntry.getURL(entry, true)))
                .setConnectionEntry(entry)
                .build();
        mediaManager.addMedia(media);

        assertThat(videoConnectionManager.getConnectionEntry(media)).isEqualTo(entry);
        assertThat(videoConnectionManager.getConnectionEntries(null, false)).hasSize(1);
        assertThat(mediaManager.getAllMedia()).hasSize(1);
    }
}
