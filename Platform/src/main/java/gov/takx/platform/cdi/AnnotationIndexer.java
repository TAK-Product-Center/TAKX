package gov.takx.platform.cdi;

import gov.takx.api.annotation.Internal;
import gov.takx.commons.utils.StringUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Manage the annotation index for the deployment.  Recreate on demand or if any of the deployment roots have
 * been modified more recently than the index.
 *
 * @since 2.0.0
 */
@Internal
public class AnnotationIndexer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String indexDir;
    private final Collection<File> deploymentRoots;
    private final File indexFile;
    private int classCount = 0;

    /**
     * Create an annotation indexer for the given deployment.
     *
     * @param indexDir        Directory in which to create "indices" directory
     * @param deploymentName  Deployment name, used to derive index file name
     * @param deploymentRoots List of deployment roots, input to indexing operations
     */
    public AnnotationIndexer(String indexDir, String deploymentName, Collection<File> deploymentRoots)
    {
        this.indexDir = indexDir;
        this.deploymentRoots = deploymentRoots;

        // If the deployment name includes a ":" then cut the name at that point.
        final String baseIndexName = StringUtils.trimAt(deploymentName, ":");

        indexFile = new File(getIndexDirectory(), baseIndexName + ".idx");

        if (logger.isDebugEnabled())
        {
            logger.debug("deployment '{}' index file is '{}", deploymentName, indexFile);
            logger.debug("Deployment roots: {}", deploymentRoots);
        }

        makeIndexDirectory();
    }

    /**
     * @return File representing the index file for the configured deployment
     */
    private File getIndexDirectory()
    {
        return new File(indexDir, "indices");
    }

    /**
     * Read in the index.  If it is not present or out of date, recreate it.
     *
     * @return annotation index
     */
    public Index readIndex()
    {
        return internalReadIndex(true);
    }

    /**
     * Internal method to read in the index.  If it is not present or out of date, recreate it.
     * If we fail to read it, and retry is true, then recreate the index and try one additional time to read it.
     *
     * @param retry Pass true to allow a retry if reading the index fails
     * @return annotation index
     */
    private Index internalReadIndex(boolean retry)
    {
        if (!indexFile.exists() || indexOutOfDate()) recreateIndex(); // Create if it's missing or out of date

        try (InputStream stream = new FileInputStream(indexFile))
        {
            return new IndexReader(stream).read();
        } catch (Exception e)
        {
            logger.warn("Failed to read index: {}", indexFile, e);
        }

        // If we failed to read the index on the first try, attempt to recreate it and try once more
        if (retry)
        {
            logger.warn("Attempting to recreate index: {}", indexFile);
            recreateIndex();

            //noinspection TailRecursion
            return internalReadIndex(false);
        }
        return null;
    }

    /**
     * Reconstruct the index from the contents of all the deployment roots.
     */
    public void recreateIndex()
    {
        final long start = System.currentTimeMillis();

        deleteIndex();

        final Indexer indexer = new Indexer();
        classCount = 0;

        for (File root : deploymentRoots)
        {
            if (root.isDirectory())
            {
                indexDirectory(indexer, root);
            } else
            {
                indexJarFile(indexer, root);
            }
        }

        // Write out our accumulated index
        try (OutputStream os = new FileOutputStream(indexFile))
        {
            new IndexWriter(os).write(indexer.complete());
        } catch (IOException e)
        {
            logger.error("Failed to write annotation index: {}", indexFile, e);
        }

        if (logger.isDebugEnabled())
        {
            final long end = System.currentTimeMillis();
            logger.debug(String.format("Index %s created on %d classes took %.2f seconds", indexFile, classCount, (end - start) / 1000f));
        }
    }

    /**
     * Determine if the index is out of date compared to the deployment roots.
     *
     * @return {@code true} if the index is out of date
     */
    private boolean indexOutOfDate()
    {
        final long indexUpdateTime = getUpdateTime(indexFile);

        return deploymentRoots.stream().anyMatch(deploymentRoot -> anyClassFileNewer(deploymentRoot, indexUpdateTime));
    }

    /**
     * Determine if any class file in (or beneath) the given root is newer than the given index update time.
     * If the deploymentRoot is a normal file (jar), we can just test the update time directly.  Otherwise,
     * it is a directory, and we need to scan it and test all {@code .class} files beneath it.
     *
     * @param deploymentRoot  Deployment root (directory or jar file)
     * @param indexUpdateTime Time the index file was last updated
     * @return {@code true} if any class file within/beneath {@code deploymentRoot} was updated more recently
     * than {@code indexUpdateTime}
     */
    private boolean anyClassFileNewer(final File deploymentRoot, final long indexUpdateTime)
    {
        boolean rootIsNewer;

        if (deploymentRoot.isDirectory())
        {
            // Scan the directory for any file newer than the index

            try (Stream<Path> newerFiles = Files.find(deploymentRoot.toPath(), Integer.MAX_VALUE,
                    (path, attrs) -> attrs.isRegularFile() && getUpdateTime(attrs) > indexUpdateTime))
            {
                final Optional<Path> newerFile = newerFiles.findFirst(); // All we need is one

                rootIsNewer = newerFile.isPresent();

                if (rootIsNewer && logger.isDebugEnabled()) logger.debug("Found newer file: {}", newerFile.get());
            } catch (IOException e)
            {
                logger.error("Failed to determine new class files in {}", deploymentRoot, e);
                rootIsNewer = true; // Recreate index on failure
            }
        } else
        {
            // Use simple test of jar file update time against index update time
            rootIsNewer = getUpdateTime(deploymentRoot) > indexUpdateTime;
        }

        if (rootIsNewer && logger.isDebugEnabled())
        {
            logger.debug("Root {} newer than index {}", deploymentRoot, indexFile);
        }

        return rootIsNewer;
    }

    /**
     * Get the "update" time of a given file.  This is the newer of the creation time and last modified time.
     *
     * @param file File to test
     * @return Newer of creation time and last modified time on the file
     * @see #getUpdateTime(BasicFileAttributes)
     * @since 2.35
     */
    private long getUpdateTime(File file)
    {
        try
        {
            return getUpdateTime(Files.readAttributes(file.toPath(), BasicFileAttributes.class));
        } catch (IOException e)
        {
            logger.error("Failed to get update time for file: {}", file.getAbsolutePath());
            return file.lastModified(); // Use last modified if we can't get the created time
        }
    }

    /**
     * Get the "update" time from the given attributes, which is the newer of the creation time and last modified time.
     * Conditions can occur (using an Install4J installer) where the creation time is more recent than the last
     * modified time (since it is apparently maintaining the last modified times of installed files from the time the
     * installer was created).
     *
     * @param attributes File attributes to test
     * @return Newer of creation time and last modified time from the file attributes
     * @since 2.35
     */
    private long getUpdateTime(BasicFileAttributes attributes)
    {
        return Math.max(attributes.creationTime().toMillis(), attributes.lastModifiedTime().toMillis());
    }

    /**
     * Scan the directory hierarchy starting at root and add all the class files to the index.
     *
     * @param indexer Indexer to update
     * @param dir     directory to scan
     */
    private void indexDirectory(Indexer indexer, File dir)
    {
        try (Stream<Path> classFiles = Files.find(dir.toPath(), Integer.MAX_VALUE,
                (path, attrs) -> attrs.isRegularFile() && path.toString().endsWith(".class")))
        {
            classFiles.forEach(path -> {
                try (InputStream stream = Files.newInputStream(path))
                {
                    addToIndex(indexer, stream);
                } catch (Exception e)
                {
                    logger.error("Could not index {}", path, e);
                }
            });
        } catch (IOException e)
        {
            logger.error("Failed to find class files in {}", dir, e);
        }
    }

    /**
     * Scan the jar and add all the class files to the index.
     *
     * @param indexer Indexer to update
     * @param jarFile Jar file to process
     */
    private void indexJarFile(Indexer indexer, File jarFile)
    {
        try (JarFile jar = new JarFile(jarFile);
             Stream<JarEntry> entryStream = jar.stream())
        {
            entryStream.filter(entry -> entry.getName().endsWith(".class")).forEach(jarEntry -> {
                try (InputStream stream = jar.getInputStream(jarEntry))
                {
                    addToIndex(indexer, stream);
                } catch (Exception e)
                {
                    logger.error("Could not index jar entry {}!/{}", jarFile, jarEntry, e);
                }
            });
        } catch (Exception e)
        {
            logger.error("Failed to index jar file: {}", jarFile, e);
        }
    }

    /**
     * Add the content of the given input stream to the index.
     *
     * @param stream Input stream (of a class file)
     * @throws java.io.IOException on error
     */
    private void addToIndex(Indexer indexer, InputStream stream) throws IOException
    {
        classCount += 1;

        final ClassInfo info = indexer.index(stream);

        if (logger.isTraceEnabled())
        {
            final Map<DotName, List<AnnotationInstance>> annotations = info.annotationsMap();

            if (!annotations.isEmpty())
            {
                logger.trace("Indexed {} ({} annotations)", info.name(), annotations.size());
            }
        }
    }

    /**
     * Delete the index file.
     */
    private void deleteIndex()
    {
        if (indexFile.exists())
        {
            logger.debug("Deleting index file: {}", indexFile);

            if (!indexFile.delete())
            {
                logger.warn("Failed to delete index file: {}", indexFile);
            }
        }
    }

    /**
     * Create the index directory if it doesn't already exist.
     */
    private void makeIndexDirectory()
    {
        final File indices = getIndexDirectory();

        if (!indices.exists() && !indices.mkdirs())
        {
            logger.error("Failed to create index directory: {}", indices);
        }
    }
}
