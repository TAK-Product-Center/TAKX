package gov.takx.commons.file;

import com.google.common.io.Resources;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.ZipBreakException;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.Zips;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Assorted utility methods for operating on files and directories.
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
public final class FileUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final int CHUNK_SIZE = 1024 * 1024;

    // A marker to replace an unsafe character
    private static final String ESCAPE_STRING = "_";

    // Characters that are not allowed in Windows file names or do not play well with URLEncoding
    private static final String UNSAFE_CHARS = "[\\\\:\"?*|<>/%^]";

    private static final String REGEX_BAD_FILENAME_CHARS = "[^-_. ',%$#@+=&\\p{L}\\p{N}()\\]\\[]";

    private static final Pattern COM_PATTERN = Pattern.compile("COM\\D", Pattern.CASE_INSENSITIVE);
    private static final Pattern LPT_PATTERN = Pattern.compile("LPT\\D", Pattern.CASE_INSENSITIVE);

    /**
     * Make a safe windows filename string.
     *
     * @param filename filename to convert to a safe filename string
     * @return safe filename string
     */
    public static String makeSafeName(String filename)
    {
        // Construct a character set pattern from the UNSAFE_CHARS, and include a backslash
        return filename.replaceAll(UNSAFE_CHARS, ESCAPE_STRING);
    }

    /**
     * Sanitize a given string to make it "safe" to use as a file name.  This will remove all characters that can
     * cause problems, path separators, and any occurrence of {@code ..}.  Finally, {@link #sanitizeWindowsSpecialNames(String)}
     * is called on the resulting string before returning it.
     * 
     * @param s Source string to process, if {@code null}, then {@code null} is returned
     * @return sanitized string
     * @see #sanitizeWindowsSpecialNames(String)
     * @since 5.2
     */
    public static String sanitizeForFilename(@Nullable String s)
    {
        if (s == null) return null;

        final String sanitized = s.trim()
                .replaceAll(REGEX_BAD_FILENAME_CHARS, "")
                .replaceAll("\\.\\.", "");

        return sanitizeWindowsSpecialNames(sanitized);
    }

    /**
     * Ensure that the given name isn't one of the special Windows file names ({@code COM[n], LPT[n], CON, PRN, AUX, NUL}).
     * If it is, then prepend an underscore to avoid issues using the value as a file name.
     *
     * @param name The name to test
     * @return {@code name} if it is not one of the special names, {@code "_" + name} if it is
     * @since 5.2
     */
    public static String sanitizeWindowsSpecialNames(String name)
    {
        if (COM_PATTERN.matcher(name).matches()
                || LPT_PATTERN.matcher(name).matches()
                || "CON".equalsIgnoreCase(name)
                || "PRN".equalsIgnoreCase(name)
                || "AUX".equalsIgnoreCase(name)
                || "NUL".equalsIgnoreCase(name))
        {
            return "_" + name;
        } else
        {
            return name;
        }
    }

    /**
     * Simple metrics about a directory and all its content.
     *
     * @since 1.25
     */
    public final static class DirInfo
    {
        /**
         * The recursive total size of all files in the directory.
         */
        public long size;

        /**
         * The number of subdirectories beneath the given directory.
         */
        public long dirCount;

        /**
         * The number of files beneath the given directory.
         */
        public long fileCount;
    }

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private FileUtils()
    {
    }

    /**
     * Get all the subdirectories in a given parent directory
     *
     * @param parent Parent directory to search
     * @return array of File objects (subdirectories)
     */
    public static File[] listDirs(File parent)
    {
        return parent.listFiles(File::isDirectory);
    }

    public static String extractLastWordFromPath(String filePath)
    {
        return FilenameUtils.getName(filePath);
    }

    /**
     * Given a file path, i.e. "C:\Program Files\TAKX\bin\Projects\MyProject.pkgx", extract the directory
     * path, i.e. "C:\Program Files\TAKX\bin\Projects"
     *
     * @param filePath file path
     * @return directory
     */
    public static String extractFullDirectoryPathFromPath(String filePath)
    {
        return FilenameUtils.getFullPathNoEndSeparator(filePath);
    }

    /**
     * Copy the given source file to the given destination.  The source can be a directory or a single file.
     * If it is a directory, the entire directory and all its contents are copied to the target.
     * <p>
     * <em>Note: For backward compatibility, this method quietly does nothing if the source file doesn't exist.</em>
     *
     * @param source Source file/directory to copy
     * @param target Destination
     * @throws IOException if source or destination is invalid, an IO error occurs during copying, or the output file
     *                     length is not the same as the input file length after the copy completes for file copies
     * @see org.apache.commons.io.FileUtils#copyDirectory(File, File)
     * @see org.apache.commons.io.FileUtils#copyFile(File, File)
     */
    public static void copy(File source, File target) throws IOException
    {
        // This feels wrong, but to preserve the prior semantics, we quietly don't copy the source if it doesn't exist.
        if (!source.exists())
        {
            logger.debug("Skipping copy of non-existent file: {}", source);
            return;
        }

        if (source.isDirectory())
        {
            org.apache.commons.io.FileUtils.copyDirectory(source, target);
        } else
        {
            org.apache.commons.io.FileUtils.copyFile(source, target);
        }
    }

    /**
     * Given a URL to a resource, copy it to a target file.
     *
     * @param source Non null resource URL
     * @param target Non null destination file.
     */
    public static void copyResource(@Nonnull URL source, @Nonnull File target) throws IOException
    {
        try (FileOutputStream outputStream = new FileOutputStream(target))
        {
            copyResource(source, outputStream);
        }
    }

    /**
     * Given a URL to a resource, copy it into the given output stream.
     *
     * @param source A non-null resource URL
     * @param target A non-null target stream.
     */
    public static void copyResource(URL source, OutputStream target) throws IOException
    {
        Resources.copy(source, target); // Method will check for non-null values.
    }

    /**
     * Given a source string and a target file, will attempt to copy the source as a file first
     * before falling back to access as a resource.  The resource must be visible to this class' class loader.
     *
     * @param source a non-null string for the source to copy.
     * @param target a non-null target destination file.
     * @since 3.0
     */
    public static void copyFileOrResource(@Nonnull String source, @Nonnull File target) throws IOException
    {
        // First, try the source as a File
        final File sourceFile = new File(source);

        if (sourceFile.exists())
        {
            copy(sourceFile, target);
        } else
        {
            copyResource(FileUtils.class.getClassLoader().getResource(source), target); // Fall back to a resource
        }
    }

    /**
     * Attempts to copy the provided {@code sourceFile} into the target directory.  The target directory is identified
     * by concatenating the {@code rootDirectory} and the {@code targetParentPath} to create an absolute path name.
     * Before copying the {@code sourceFile}, the target directory will first be checked to see if a file with the same
     * name already exists.  If a file does exist with the same name, then the file hash (checksum) will be compared to
     * the hash of the {@code sourceFile}.  If they match (implying the files are the same), then the {@code sourceFile}
     * will not be copied and the file name will be returned.  If the hash values do not match (implying the files are
     * different), then "(n)" suffixes are added to generate a unique file name, where n ranges from 1 up.
     * <p/>
     * Each time the "(n)" suffix is added to the file name, the process of checking for an existing file and comparing
     * hash values is repeated until the file is found to already exist in the target directory, or is copied to the
     * target directory.
     * <p/>
     * An example of adding the "(n)" suffix to a file:
     * The original file name: sampleFile.txt
     * The new file name:  sampleFile(1).txt
     *
     * @param sourceFile       The source file to be copied.
     * @param targetParentPath The path relative to the rootDirectory.  Example: "foo/bar/sample"
     * @param rootDirectory    The root directory.  The {@code targetParentPath} will be appended to the root directory
     *                         to create an absolute path.
     * @return The File object representing the copied file.
     */
    public static File copyUniqueFileIfNotPresent(File sourceFile, String targetParentPath, File rootDirectory)
            throws IOException
    {
        if (sourceFile == null || targetParentPath == null || rootDirectory == null)
        {
            throw new IOException();
        }

        // Check to see if the file has already been copied into the target directory
        FilePath vfsFilePath = new FilePath(targetParentPath, sourceFile.getName());
        File vfsFile = new File(vfsFilePath.toSystemPath(rootDirectory));

        int i = 1;
        FilePath testPath;
        String name = getNameWithoutExtension(sourceFile);
        String ext = getExtension(sourceFile);
        String extension = ext == null ? "" : "." + ext;

        while (true)
        {
            if (!vfsFile.exists())
            {
                // The file is not in the target directory, so copy it there
                copy(sourceFile, vfsFile);
                return vfsFile;
            }

            // The file name exists in the target directory. Check to see if the file has the same content.
            if (org.apache.commons.io.FileUtils.contentEquals(sourceFile, vfsFile))
            {
                // The file contents match, so there is not a need to copy the file.  Simply return the path to the
                // existing file.
                return vfsFile;
            }

            // The file contents do not match, so they must be different files.  Add "(n)" just before the file
            // extension, and perform the checks again.
            vfsFilePath = new FilePath(targetParentPath, name + "(" + i++ + ")" + extension);
            vfsFile = new File(vfsFilePath.toSystemPath(rootDirectory));
        }
    }

    public static void copyProgress(File source, File target, IFileProgress progress) throws IOException
    {
        final File targetFile = target.isDirectory() ? new File(target, source.getName()) : target;

        if (!targetFile.exists())
        {
            File parent = targetFile.getParentFile();

            if (!parent.exists() && !parent.mkdirs())
            {
                throw new IOException("Unable to create directory: " + targetFile.getParent());
            }
        }

        if (progress == null)
        {
            Files.copy(source.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else
        {
            try (
                 InputStream input = new FileInputStream(source);
                 OutputStream output = new FileOutputStream(targetFile))
            {
                progress.setFileMaximum(100);
                progress.setFileMinimum(0);

                final long length = source.length();

                final byte[] buffer = new byte[CHUNK_SIZE];
                long copied = 0;
                int size;

                while ((size = input.read(buffer)) > 0)
                {
                    output.write(buffer, 0, size);

                    copied += size;
                    progress.setFileProgress((int) Math.round(((double) copied / length) * 100));
                }
            }
        }
    }

    public static String extractFileNameWithoutExtensionFromPath(String filePath)
    {
        return FilenameUtils.getBaseName(filePath);
    }

    public static boolean deleteDirectory(File path)
    {
        try
        {
            final boolean exists = path.exists();
            org.apache.commons.io.FileUtils.deleteDirectory(path);
            return exists;
        } catch (IOException e)
        {
            logger.error("Failed to delete: {}", path, e);
            return false;
        }
    }

    /**
     * Calculate the size of all the files in a directory and all its subdirectories.  If {@code fileOrDir} is
     * a file, then the file size is returned.  If it is a directory, then the sum of all the files and subdirectories
     * is returned.  An arbitrary number of starting directories can be given.
     *
     * @param fileOrDir Where to start
     * @return Directory info calculated from given starting directory
     * @since 1.25
     */
    public static DirInfo getDirectoryInfo(File... fileOrDir)
    {
        DirInfo dirInfo = new DirInfo();

        for (File startFile : fileOrDir)
        {
            if (!startFile.exists()) continue; // Doesn't exist, so just skip it

            try (final Stream<Path> pathStream = Files.walk(startFile.toPath()))
            {
                pathStream.forEach(path -> {
                    final File file = path.toFile();

                    if (file.isDirectory())
                    {
                        dirInfo.dirCount += 1;
                    } else
                    {
                        dirInfo.fileCount += 1;
                        dirInfo.size += file.length();
                    }
                });
            } catch (IOException e)
            {
                logger.error("Failed to walk directory: {}", startFile, e);
            }
        }

        return dirInfo;
    }

    /**
     * Format the given file size into bytes, KB, MB, or GB depending on the scale of the size.
     *
     * @param fileSize File size to format
     * @return formatted size
     * @see SizeFormatter#formatEstimate(long)
     * @since 1.2
     */
    public static String formatFileSize(long fileSize)
    {
        return SizeFormatter.formatEstimate(fileSize);
    }

    /**
     * Extract the extension (without the period) from the given file name.
     *
     * @param fileName File name to process
     * @return file extension, or {@code null} if no period or nothing after the period
     */
    public static String getExtension(String fileName)
    {
        if (fileName == null) return null;

        // Retain previous semantics:
        //   - return null when no extension is found
        //   - force lower case
        final String extension = FilenameUtils.getExtension(fileName);
        return extension.isEmpty() ? null : extension.toLowerCase();
    }

    /**
     * Extract the extension (without the period) from the given file.
     *
     * @param f File object to work on
     * @return file extension, or {@code null} if no period or nothing after the period
     */
    public static String getExtension(File f)
    {
        return getExtension(f.getName());
    }

    /**
     * Get the name of a file without the file extension or period.
     *
     * @param f File object to work on
     * @return file name without extension
     */
    public static String getNameWithoutExtension(File f)
    {
        return getNameWithoutExtension(f.getName());
    }

    /**
     * Get the name of a file without the file extension or period.
     *
     * @param fileName File name to work on
     * @return file name without extension
     * @see FilenameUtils#removeExtension
     */
    public static String getNameWithoutExtension(String fileName)
    {
        return FilenameUtils.removeExtension(fileName);
    }

    /**
     * Generate a unique file name given the root directory and base file.  If the file exists in the root directory,
     * then "(n)" suffixes are added to generate a unique file name, where n ranges from 1 up.  Only the last component
     * in {@code file} is used, so if {@code file} refers to a path like "foo/bar/gort.txt" only the "gort.txt"
     * component is used relative to {@code rootDir} to determine the unique file name.
     *
     * @param rootDir The directory in which to test for file existence.
     * @param file    The file to use for generating the file name to test, only the last component (no directory names) is used.
     * @return A file representing the full path to the unique file name
     * @since 1.11
     */
    public static File getUniqueFileName(File rootDir, File file)
    {
        final String fileName = file.getName();
        String name = getNameWithoutExtension(fileName);
        String ext = getExtension(fileName);
        String extension = ext == null ? "" : "." + ext;

        int i = 0;

        while (true)
        {
            final String testName = i == 0
                    ? fileName
                    : name + "(" + i + ")" + extension;

            final File target = new File(rootDir, testName);
            if (!target.exists())
            {
                return target;
            }
            i++;
        }
    }

    /**
     * Generates the checksum (hash) value for the given file using the provided MessageDigest (algorithm).
     *
     * @param digest The Message Digest hash algorithm to employ.
     * @param file   The file to generate the hash for.
     * @return The checksum (hash) for the provided file.
     * @since 1.49
     */
    public static String getFileChecksum(MessageDigest digest, File file) throws IOException
    {
        // Get file input stream for reading the file content
        try (FileInputStream fileInputStream = new FileInputStream(file))
        {
            // Create byte array to read data in chunks
            byte[] byteArray = new byte[CHUNK_SIZE];
            int bytesCount;

            // Read file data and update in message digest
            while ((bytesCount = fileInputStream.read(byteArray)) != -1)
            {
                digest.update(byteArray, 0, bytesCount);
            }

            return Hex.encodeHexString(digest.digest()); // Convert the hash bytes to hexadecimal format
        }
    }

    /**
     * Returns the associated system icon (16x16) for a given file.
     *
     * @param f file for which to get the system icon.
     * @return (16x16) sized system icon
     */
    public static Icon getSystemIcon(File f)
    {
        return FileSystemView.getFileSystemView().getSystemIcon(f);
    }

    /**
     * Returns a BufferedImage of the associated system icon (not 16x16) for a given file.
     *
     * @param f a file
     * @return a BufferedImage or {@code null} if the method failed
     */
    public static BufferedImage getSystemIconImage(File f)
    {
        if (f == null || !f.exists())
        {
            logger.warn("Attempt to get icon image for non-existent file: {}", f);
            return null;
        }

        final Icon icon = getSystemIcon(f);

        if (icon instanceof ImageIcon imageIcon)
        {
            final Image img = imageIcon.getImage();

            if (img instanceof BufferedImage bufferedImage)
            {
                return bufferedImage;
            } else
            {
                final int size = ImageUtils.DEFAULT_ICON_WIDTH;
                final Image scaledImage = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                final BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                bi.createGraphics().drawImage(scaledImage, 0, 0, size, size, null);
                return bi;
            }
        }

        if (icon != null)
        {
            final Toolkit tk = Toolkit.getDefaultToolkit();
            final Dimension dim = tk.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());

            final BufferedImage bi = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(null, bi.createGraphics(), 0, 0);
            return bi;
        }

        return null;
    }

    /**
     * Determines if the provided file has one of the specified extensions.
     *
     * @param file              The file to check
     * @param desiredExtensions The extensions to check (without the period)
     * @return True if file ends with one of the specified extensions (regardless of case).
     * @since 3.0
     */
    public static boolean hasExtension(final File file, String... desiredExtensions)
    {
        final String extension = getExtension(file);
        if (extension == null) return false;

        for (String desiredExtension : desiredExtensions)
        {
            if (extension.equalsIgnoreCase(desiredExtension)) return true;
        }

        return false;
    }

    /**
     * Attempts to browse to the file
     *
     * @param file file to launch
     */
    public static void browseFile(final File file)
    {
        if (file.exists() && Desktop.isDesktopSupported())
        {
            try
            {
                Desktop.getDesktop().browse(file.toURI());
            } catch (IOException e)
            {
                // Set max width of the dialog to 1/6 the screen width.
                int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 6;

                // Show the error message. "Access is denied" IOExceptions can occur if there's no application
                // associated with the file.
                AlwaysOnTopJOptionPane.showErrorDialogLater(
                        "<html><body><p style='width: " + width + "px;'>"
                                + "Cannot open <b>"
                                + file.getAbsolutePath()
                                + "</b>. You may need to install an application capable of opening the file."
                                + "</p></body></html>",
                        "Error Opening File");
                logger.warn("Failed to launch file", e);
            }
        } else if (!file.exists())
        {
            AlwaysOnTopJOptionPane.showErrorDialogLater("Can't find file: " + file.getName(), "Error Finding File");
        } else
        {
            logger.warn("Launching file not supported, file: {}", file.getAbsolutePath());
        }
    }

    /**
     * Attempts to launch the file with the OS default application.
     *
     * @param file file to launch
     */
    public static void launchFile(final File file)
    {
        if (file.exists())
        {
            if (Desktop.isDesktopSupported())
            {
                try
                {
                    Desktop.getDesktop().open(file);
                } catch (IOException e)
                {
                    logger.warn("Can't find application to open file: {}", file.getAbsolutePath(), e);
                    AlwaysOnTopJOptionPane.showErrorDialogLater("Can't find application to open file: " + file, "Error Opening File");
                }
            } else
            {
                logger.warn("Launching file not supported, file: {}", file.getAbsolutePath());
                AlwaysOnTopJOptionPane.showErrorDialogLater("Can't launch files on this system: " + file, "Can't Launch Application");
            }
        } else
        {
            AlwaysOnTopJOptionPane.showErrorDialogLater("Can't find file: " + file.getAbsolutePath(), "File Doesn't Exist");
        }
    }

    /**
     * Read all the lines from the given file and return them as a string array.  Blank lines will not be included
     * in the results.  All lines will be trimmed of leading/trailing whitespace.
     *
     * @param f File to read from
     * @return String array, {@code null} if an error occurs reading the file
     */
    public static String[] readLines(File f)
    {
        try
        {
            return Files.readAllLines(f.toPath())
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } catch (IOException e)
        {
            logger.error("Failed to read lines from: {}", f, e);
            return new String[0];
        }
    }

    /**
     * Write a set of strings out to a file, one string per line.
     *
     * @param f     File to write to
     * @param lines Set of strings to write, one per line
     * @return {@code true} if the file was written, {@code false} on error
     */
    public static boolean writeLines(File f, String[] lines)
    {
        try
        {
            Files.write(f.toPath(), Arrays.asList(lines));
            return true;
        } catch (IOException e)
        {
            logger.error("Failed to write file: {}", f);
        }

        return false;
    }

    /**
     * Write a string out to a file.
     *
     * @param f    File to write to
     * @param text Full text to write
     * @return {@code true} if the file was written, {@code false} on error
     * @since 5.7
     */
    public static boolean writeString(File f, String text)
    {
        try
        {
            Files.writeString(f.toPath(), text);
            return true;
        } catch (IOException e)
        {
            logger.error("Failed to write file: {}", f);
        }

        return false;
    }

    /**
     * Zip directory and to include the parent directory in the zip.
     *
     * @param source source directory to zip
     * @param target target zip file
     */
    public static void zipDirectory(String source, String target)
    {
        zipDirectory(source, target, true);
    }

    /**
     * Zip directory, with the option to include the parent directory in the zip.
     *
     * @param source                 source directory to zip
     * @param target                 target zip file
     * @param includeParentDirectory boolean include the parent directory
     */
    public static void zipDirectory(String source, String target, boolean includeParentDirectory)
    {
        ZipUtil.pack(new File(source), new File(target), includeParentDirectory);
    }

    /**
     * unzip the source zip file to the target directory location
     *
     * @param source zip
     * @param target directory
     * @return true if the target was successfully unzipped, false if anything went wrong
     */
    public static boolean unzipFile(File source, File target)
    {
        return catchAndConvertToBoolean(source, "unzip", () -> ZipUtil.unpack(source, target));
    }

    /**
     * Extract a single file from the zip archive.
     *
     * @param source    Zip file
     * @param entryName Name of entry to extract
     * @param target    File to which to write the extracted entry
     * @return true if the file was successfully extracted, false if anything went wrong
     * @since 3.0
     */
    public static boolean extractFileFromZip(File source, String entryName, File target) throws IOException
    {
        // create the parent directory structure if needed
        final File parent = target.getParentFile();
        if (!parent.exists() && !parent.mkdirs())
        {
            throw new IOException("Failed to create destination parent directory: " + parent);
        }

        return ZipUtil.unpackEntry(source, entryName, target);
    }

    public static boolean extractTopLevelDirFromZip(File source, File targetDir, String dirName)
    {
        return extractTopLevelDirFromZip(source, targetDir, dirName, null);
    }

    public static boolean extractTopLevelDirFromZip(File source, File targetDir, String dirName, String newDirName)
    {
        return catchAndConvertToBoolean(source, "extract directory " + dirName, () -> ZipUtil.unpack(source, targetDir,
                fileName -> fileName.startsWith(dirName)
                        ? newDirName == null ? fileName : fileName.replaceFirst(dirName, newDirName)
                        : null));
    }

    /**
     * Perform an operation that may throw a {@link ZipException} and return false if it does.
     *
     * @param source      Source object (for error message)
     * @param description Description of operation (for error message)
     * @param action      The action to perform
     * @return {@code false} if a ZipException is caught. {@code true} otherwise
     * @since 3.0
     */
    private static boolean catchAndConvertToBoolean(Object source, String description, Runnable action)
    {
        try
        {
            action.run();
            return true;
        } catch (ZipException e)
        {
            logger.error("Failed to {}, from: {}", description, source, e);
            return false;
        }
    }

    /**
     * Create a unified zip from a list of different sources.
     *
     * @param filesToZip list of sources to create zip rom
     * @param zipPath    path to where to create the zip
     */
    public static void zipCollection(Set<File> filesToZip, File zipPath)
    {
        zipCollection(filesToZip, zipPath, null, null);
    }

    /**
     * Create a unified zip from a list of different sources.
     *
     * @param filesToZip      list of sources to create zip from
     * @param zipPath         path to where to create the zip
     * @param excludedFiles   list of File objects to exclude from zip
     * @param zipFileProgress Callback interface for reporting zip progress.
     * @since 1.14 (changed first parameter from set to collection in 3.0)
     */
    public static void zipCollection(Collection<File> filesToZip, File zipPath, Set<File> excludedFiles, IZipFileProgress zipFileProgress)
    {
        zipCollection(filesToZip, zipPath, excludedFiles, null, zipFileProgress);
    }

    /**
     * Create a unified zip from a list of different sources.  The optional nameMapper can be used to alter
     * the relative paths of files within the Zip (i.e., translating the file path to an arbitrary zip path).
     *
     * @param filesToZip      list of sources to create zip rom
     * @param zipPath         path to where to create the zip
     * @param excludedFiles   list of File objects to exclude from zip
     * @param nameMapper      Optional name mapper to apply, may be {@code null}
     * @param zipFileProgress Callback interface for reporting zip progress, may be {@code null}
     * @since 3.0
     */
    public static void zipCollection(Collection<File> filesToZip, File zipPath, Set<File> excludedFiles,
                                     NameMapper nameMapper, IZipFileProgress zipFileProgress)
    {
        logger.debug("{} into zipPath: {}, excluding={}", filesToZip, zipPath, excludedFiles);

        // Destination file needs to exist for Zips to work properly
        try
        {
            if (!zipPath.exists()) Files.createFile(zipPath.toPath());
        } catch (IOException e)
        {
            throw new ZipException("Could not create: " + zipPath, e);
        }

        // Due to a couple of bugs in the zero turnaround library, we can't directly apply the name mapper
        // via the Zips operation.  We have to do it as part of constructing the list of zip entries to add.

        final Zips zips = Zips.create()
                .destination(zipPath)
                .addEntries(new ZipEntrySourceCollector(excludedFiles, nameMapper).scanRoots(filesToZip));

        if (zipFileProgress != null)
        {
            // Use a no-op name mapper so we get a call back on every file processed
            zips.nameMapper(name -> {
                zipFileProgress.setSubstatusText("Compressing and adding " + name);
                zipFileProgress.increment();
                return name;
            });

            int filesCount = countFiles(filesToZip) - countFiles(excludedFiles);
            zipFileProgress.setText("Adding " + filesCount + " files to the archive...");
            zipFileProgress.setFileCount(filesCount);
        }

        zips.process(); // Do the work
    }

    public static String findFirstFileWithGivenExtension(File source, String ext)
    {
        final AtomicReference<String> found = new AtomicReference<>(null);

        ZipUtil.iterate(source, zipEntry -> {
            final String name = zipEntry.getName();

            if (StringUtils.endsWithIgnoreCase(name, ext))
            {
                found.set(name);
                throw new ZipBreakException(); // Found it, all done
            }
        });

        return found.get();
    }

    public static String getNextNameIteration(File dir, String file)
    {
        final int lastDot = file.lastIndexOf('.');
        final String ext = lastDot > 0 ? file.substring(lastDot) : "";
        final String root = lastDot > 0 ? file.substring(0, lastDot) : file;
        File nameToCreate = new File(dir, file);

        int packageCount = 2;
        while (nameToCreate.exists())
        {
            nameToCreate = new File(dir, root + "(" + packageCount + ")" + ext);
            packageCount++;
        }
        return nameToCreate.getName();
    }

    /**
     * Count all the files in the specified set of File objects, recursively.
     *
     * @param files The set of File objects (files and directories).
     * @return The total number of files in the set (excluding directories).
     */
    public static int countFiles(Collection<File> files)
    {
        return countFiles(files, -1);
    }

    /**
     * Count all the files in the specified directory, recursively.
     *
     * @param directory The directory to count.
     * @return The number of files in the directory.
     */
    public static int countFiles(File directory)
    {
        return countFiles(directory, -1);
    }

    /**
     * Get file count, with a maximum number of files specified before the routine halts.  If the maxCount
     * is set to -1, it will not be used and the count will go until all files have been traversed.
     *
     * @param files    files to count.  Subdirectories will also be traversed.
     * @param maxCount maximum number of files to count before terminating early.  Set to -1 to ignore.
     * @return number of files counted.
     */
    public static int countFiles(Collection<File> files, int maxCount)
    {
        return files.stream()
                .mapToInt(directory -> countFiles(directory, maxCount))
                .sum();
    }

    /**
     * Get the count of the files in the specified directory, up to the specified maxCount.  If maxCount is set
     * to -1, it is ignored.
     *
     * @param directory the directory to recursively count files in.
     * @param maxCount  the maximum number of files to count before terminating.  Set to -1 to ignore.
     * @return number of files counted.
     */
    public static int countFiles(File directory, int maxCount)
    {
        if (!directory.exists()) return 0; // Doesn't exist, so no files

        try (Stream<Path> walk = Files.walk(directory.toPath()))
        {
            return (int) walk
                    .limit(maxCount > 0 ? maxCount : Integer.MAX_VALUE)
                    .count();
        } catch (IOException e)
        {
            logger.error("Failed to count files starting at: {}", directory, e);
            return 0;
        }
    }

    /**
     * Get the creation time (as epoch millis) of the given file.  If the file attributes can not be obtained,
     * then the file's last modification time will be returned.
     *
     * @param file File of interest
     * @return creation time
     * @since 3.0
     */
    public static long getFileCreationTime(File file)
    {
        try
        {
            final BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return attributes.creationTime().toMillis();
        } catch (IOException e)
        {
            logger.error("Failed to get creation time for file: {}", file.getAbsolutePath());
            return file.lastModified(); // Use last modified if we can't get the created time
        }
    }

    /**
     * File visitor to collect, and optionally rename, Zip entry sources by scanning given root directories.
     *
     * @since 3.0
     */
    private static final class ZipEntrySourceCollector extends SimpleFileVisitor<Path>
    {
        private final Set<String> ignore;
        private final List<ZipEntrySource> entrySources = new ArrayList<>();
        private final NameMapper nameMapper;

        private int countToStrip; // based on current root

        /**
         * @param ignore     Files to ignore (exclude) from collection.  If the specified file is a directory, then
         *                   the entire subtree of that directory will be excluded.
         * @param nameMapper Optional name mapper to change the Zip file entry names as compared to the real file names
         */
        private ZipEntrySourceCollector(Set<File> ignore, NameMapper nameMapper)
        {
            // Convert the file names to absolute paths so comparisons aren't affected by "relative" paths to
            // a given root directory.
            this.ignore = ignore.stream().map(File::getAbsolutePath).collect(Collectors.toSet());

            this.nameMapper = nameMapper;
        }

        /**
         * Scan the given roots for zip entries.
         *
         * @param roots Collection of root files to process
         * @return Array of zip entry sources
         * @since 3.0 (changed parameter from set to collection)
         */
        private ZipEntrySource[] scanRoots(Collection<File> roots)
        {
            roots.forEach(this::scanRoot);

            return entrySources.toArray(new ZipEntrySource[0]);
        }

        /**
         * Scan a given root for zip entries.
         */
        private void scanRoot(File root)
        {
            // The root path, except for the final name, is removed from the paths as we scan them.
            // So, if we're told to scan "a/b/c", "a/b" will be removed from all the paths generated.
            // This provides the semantics for zipping a given directory as the root in the Zip, regardless
            // of where it lives on the local file system.

            final Path rootPath = root.toPath();
            countToStrip = rootPath.getNameCount() - 1;

            try
            {
                Files.walkFileTree(rootPath, this);
            } catch (IOException e)
            {
                logger.error("Failed to scan {}", root, e);
            }
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        {
            if (ignored(dir))
            {
                return FileVisitResult.SKIP_SUBTREE;
            } else
            {
                // Due to a bug in the zero turnaround library, we can't include directory entries.
                // AFAICT, they are not needed (not required by the Zip file spec), so leave them out for now.
//                entrySources.add(new FileSource(makeEntryPath(dir), dir.toFile()));
                return FileVisitResult.CONTINUE;
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        {
            if (!ignored(file))
            {
                entrySources.add(new FileSource(makeEntryPath(file), file.toFile()));
            }
            return FileVisitResult.CONTINUE;
        }

        private boolean ignored(Path path)
        {
            return ignore.contains(path.toFile().getAbsolutePath());
        }

        /**
         * Construct the Zip entry path for the given file path.  First, the current root is stripped from the front
         * of the path, and then any configured name mapper will be applied.
         *
         * @param path File path to process
         * @return Stripped, and optionally mapped, path, as a String
         */
        private String makeEntryPath(Path path)
        {
            final Path newPath = countToStrip > 0
                    ? path.subpath(countToStrip, path.getNameCount())
                    : path;

            return nameMapper != null
                    ? nameMapper.map(newPath.toString())
                    : newPath.toString();
        }
    }
}
