package gov.takx.commons.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * This class provides a monitor that watches a directory for new files.  New files are obtained by calling the
 * {@link #get()} method, which is a blocking operation.  When finished watching, call {@link #close()}.
 * <p/>
 * This class is not thread safe.  It is intended to be used by a single thread.
 *
 * @since 1.50
 */
public class DirectoryMonitor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final WatchService watcher;
    private final WatchKey watchKey;
    private final Deque<Path> pendingPaths = new ArrayDeque<>();

    /**
     * Construct a new directory monitor watching the specified directory.
     *
     * @param directory Directory to watch
     * @throws IOException If the directory watch could not be set up
     */
    public DirectoryMonitor(File directory) throws IOException
    {
        watcher = FileSystems.getDefault().newWatchService();
        watchKey = directory.toPath().register(watcher, ENTRY_CREATE);
    }

    /**
     * Get the next file added to the monitored directory.  This call blocks until a file is available.
     *
     * @return Path of new file
     */
    public Path get() throws InterruptedException
    {
        // If we have pending entries, return the first one
        if (!pendingPaths.isEmpty()) return pendingPaths.pop();

        // Otherwise, block until we get something
        while (true)
        {
            if (getWork())
            {
                watchKey.reset(); // Reset the key so that we'll keep getting results
                return pendingPaths.pop();
            }
        }
    }

    /**
     * Close this monitor which closes the underlying WatchService.
     *
     * @throws IOException If closing the directory watch fails
     */
    public void close() throws IOException
    {
        watcher.close();
    }

    /**
     * Wait for a watch key and pull off events.
     *
     * @return {@code true} if we got some work, {@code false} otherwise
     */
    private boolean getWork() throws InterruptedException
    {
        final var key = watcher.take();

        if (key.equals(watchKey))
        {
            for (var event : key.pollEvents())
            {
                if (event.kind() == OVERFLOW) continue;

                final var path = (Path) event.context();

                if (path == null)
                {
                    logger.error("NULL context from dir watcher");
                } else
                {
                    pendingPaths.push(path); // Add to pending queue
                }
            }
        } else
        {
            logger.error("Unknown watch key: {}", key);
        }

        return !pendingPaths.isEmpty();
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        File dir = new File("commands");

        //noinspection ResultOfMethodCallIgnored
        dir.mkdir(); // Ensure it exists

        final DirectoryMonitor monitor = new DirectoryMonitor(dir);

        //noinspection InfiniteLoopStatement
        while (true)
        {
            final Path path = monitor.get();

            logger.warn("Got: {}", path);
        }
    }
}
