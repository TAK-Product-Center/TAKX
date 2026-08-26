package gov.takx.api.plugin.io;

import gov.takx.api.annotation.Internal;
import gov.takx.api.io.Format;
import gov.takx.commons.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class for iterating through files that are candidates for import.  Used internally by our
 * {@link ACancellableImportWizard} wizard.
 *
 * @since 2.14
 */
@Internal
class ImportFileIterator extends SimpleFileVisitor<Path>
{
    private List<File> fileList = new ArrayList<>();
    private IImportFileValidator validator;
    private final Set<String> supportedExtensions;
    private boolean simpleValidation;
    private boolean valid = false;

    /**
     * Constructor, that also kicks off the file traversal
     *
     * @param directory Directory to traverse
     * @param formats   Formats to match to
     * @throws IOException Exception walking the tree
     */
    ImportFileIterator(String directory, List<Format> formats) throws IOException
    {
        this(directory, formats, null, false);
    }

    /**
     * Constructor, that also kicks off the file traversal
     *
     * @param directory        Directory to traverse.
     * @param formats          The List of formats to match.
     * @param validator        The import file validator.
     * @param simpleValidation true / false, validation only.  Stops after validating a single file.
     * @throws IOException Exception walking the tree
     */
    ImportFileIterator(String directory, List<Format> formats, IImportFileValidator validator, boolean simpleValidation) throws IOException
    {
        this.simpleValidation = simpleValidation;
        this.validator = validator;

        // Pull out the supported extensions before the tree walk to speed up file checks
        supportedExtensions = formats.stream()
                .flatMap(f -> Arrays.stream(f.getFileTypes()))
                .map(String::toLowerCase).collect(Collectors.toSet());

        Files.walkFileTree(Paths.get(directory), this);
    }

    /**
     * Visit file callback.  If there is a validator attached, call it to validate the file.  Otherwise, just
     * validate on the file extension.
     *
     * @param file       next file to validate
     * @param attributes attributes (unused)
     * @return result of file visit.
     * @throws IOException Throws an IOException if a problem occurs.
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
    {
        final String pathname = file.toString();
        final String extension = FileUtils.getExtension(pathname);

        if (extension != null && supportedExtensions.contains(extension))
        {
            // If a validator is configured, it will determine file validity,
            // otherwise, just matching extension is enough.

            final boolean validFile = validator == null || validator.validateFile(pathname, simpleValidation);

            if (validFile)
            {
                fileList.add(new File(pathname));
                valid = true;
            }
        }

        // If we're doing a simple validation, stop looking after finding the first valid file
        return valid && simpleValidation
                ? FileVisitResult.TERMINATE
                : FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException
    {
        return FileVisitResult.CONTINUE;
    }

    /**
     * @return The list of valid files in the directory.  In the case of simple validation, this will only contain
     * the first valid file found.
     */
    List<File> getFileList()
    {
        return fileList;
    }

    /**
     * @return True if the file directory was valid.  In the case of simple validation, this means at least one file
     * was valid.
     */
    boolean isValid()
    {
        return valid;
    }
}
