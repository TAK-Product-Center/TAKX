package gov.tak.platform.datapackage;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * General utility for reading data from Zip files.
 *
 * @since 3.1
 */
public class ZipReader implements AutoCloseable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final String EMPTY_STRING = "";

    protected final File zipFilePath;
    protected final ZipFile zipFile;

    /**
     * Construct on the given zip file.
     */
    public ZipReader(@Nonnull File zipFilePath) throws IOException
    {
        this.zipFilePath = zipFilePath;
        zipFile = new ZipFile(zipFilePath);
    }

    @Override
    public void close() throws IOException
    {
        zipFile.close();
    }

    /**
     * @return The file being read
     */
    public File getZipFilePath()
    {
        return zipFilePath;
    }

    /**
     * Read an entry from the package file and return as a String.
     *
     * @param entry Zip entry to read
     * @return String content, or an empty string if it can't be read
     */
    @Nonnull
    public String readEntryAsString(@Nonnull String entry)
    {
        return readEntryAsString(entry, false);
    }

    /**
     * Read an entry from the package file and return as a String optionally suppressing error messages.
     *
     * @param entry Zip entry to read
     * @param quiet Pass {@code true} to suppress errors reported if the entry doesn't exist
     * @return String content, or an empty string if it can't be read
     * @since 3.2
     */
    @Nonnull
    public String readEntryAsString(@Nonnull String entry, boolean quiet)
    {
        try
        {
            final byte[] bytes = readEntry(entry, quiet);

            // This is strange, but it appears that, on some data packages, there are extraneous illegal bytes
            // at the beginning of an entry.  They do not show up if you use a tool like 7zip to extract the files,
            // but they do when reading directly from the zip input stream.
            //
            // So, scan the returned bytes, skipping any that are <= 0, to find the correct start of the string.

            int start = 0;
            while (start < bytes.length && bytes[start] <= 0) start++;

            return new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
        } catch (Exception e)
        {
            if (!quiet) logger.error("Failed to read {} in {}", entry, zipFilePath, e);
            return EMPTY_STRING;
        }
    }

    /**
     * Read an entry from the package file and return the bytes.
     *
     * @param entry Zip entry to read
     * @return bytes read, an empty array if it can't be read
     */
    @Nonnull
    public byte[] readEntry(@Nonnull String entry)
    {
        return readEntry(entry, false);
    }

    /**
     * Read an entry from the package file and return the bytes.
     *
     * @param entry Zip entry to read
     * @param quiet Pass {@code true} to suppress errors reported if the entry doesn't exist
     * @return bytes read, an empty array if it can't be read
     * @since 3.2
     */
    @Nonnull
    public byte[] readEntry(@Nonnull String entry, boolean quiet)
    {
        try
        {
            try (InputStream inputStream = getInputStream(entry))
            {
                return inputStream.readAllBytes();
            }
        } catch (Exception e)
        {
            if (!quiet) logger.error("Failed to read {} in {}", entry, zipFilePath, e);
            return EMPTY_BYTES;
        }
    }

    /**
     * @return an enumeration of the ZIP file entries
     */
    @Nonnull
    public Enumeration<? extends ZipEntry> entries()
    {
        return zipFile.entries();
    }

    /**
     * @param entryPath Entry to search for
     * @return {@code true} if this zip file contains an entry with the given path
     */
    public boolean hasEntry(@Nonnull String entryPath)
    {
        return zipFile.getEntry(entryPath) != null;
    }

    /**
     * Find the first entry that matches the given {@code matcher}.
     *
     * @param matcher Predicate to test Zip entries
     * @return First matching zip entry path, or {@code null} if not found
     * @since 3.2
     */
    @Nullable
    public String findEntry(@Nonnull Predicate<ZipEntry> matcher)
    {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements())
        {
            final ZipEntry zipEntry = entries.nextElement();

            if (matcher.test(zipEntry)) return zipEntry.getName();
        }

        return null;
    }

    /**
     * Find all entries that match the given {@code matcher}.
     *
     * @param matcher Predicate to test Zip entries
     * @return List of matching zip entry paths
     * @since 5.7
     */
    @Nonnull
    public List<String> findEntries(@Nonnull Predicate<ZipEntry> matcher)
    {
        final List<String> results = new ArrayList<>();
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements())
        {
            final ZipEntry zipEntry = entries.nextElement();

            if (matcher.test(zipEntry)) results.add(zipEntry.getName());
        }

        return results;
    }

    /**
     * @return The size in bytes of the ZipEntry with the given path
     * @since 3.2
     */
    public long getZipEntrySize(@Nonnull String entry)
    {
        final ZipEntry zipEntry = zipFile.getEntry(entry);
        return zipEntry != null ? zipEntry.getSize() : 0;
    }

    /**
     * Obtain an {@link InputStream} on an entry in the package file.  The caller is responsible for closing the
     * returned input stream.
     *
     * @param entry Zip entry to open
     * @return InputStream on the zip entry
     */
    @Nonnull
    public InputStream getInputStream(@Nonnull String entry) throws IOException
    {
        final ZipEntry zipEntry = zipFile.getEntry(entry);
        if (zipEntry == null) throw new IOException("ZipEntry not found: " + entry);

        return zipFile.getInputStream(zipEntry);
    }
}
