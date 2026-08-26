package gov.takx.commons.file;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableSet;

// TODO TAKX-4149: Clean this up and make publicly available in platform somewhere (maybe not this package)
//  - Works as a static utility class for now, maybe make it an injectable service? 
//  - Allow plugins to register their own recognized file types?
//  - Consider possible use cases
//    - Example MapObjectAttachmentPanelItem needs to know file types to determine icons, but is more specific than we currently are here
//      - PPTX vs PDF both are both grouped as 'document' but are different kinds of documents. Maybe make grouping a hierarchy?

/**
 * Utility for information about common file types.
 *
 * @since 5.7
 */
public class FileTypeManager
{
    private static final FileGrouping[] ARR_IMAGE = new FileGrouping[]{FileGrouping.Image};
    private static final FileGrouping[] ARR_AUDIO = new FileGrouping[]{FileGrouping.Audio};
    private static final FileGrouping[] ARR_VIDEO = new FileGrouping[]{FileGrouping.Video};
    private static final FileGrouping[] ARR_DOCUMENT = new FileGrouping[]{FileGrouping.Document};
    private static final FileGrouping[] ARR_ARCHIVE = new FileGrouping[]{FileGrouping.Archive};
    private static final FileGrouping[] ARR_EXECUTABLE = new FileGrouping[]{FileGrouping.Executable};
    private static final FileGrouping[] ARR_DATA = new FileGrouping[]{FileGrouping.Data};

    /** Default mime type for any arbitrary binary data. */
    public static final String DEFAULT_BINARY_MIME_TYPE = "application/octet-stream";

    private static final Set<FileType> types = new HashSet<>();
    private static final Map<String, List<FileType>> extToType = new HashMap<>();

    static
    {
        // Images
        add("JPEG Image", ARR_IMAGE, "image/jpeg", "jpg", "jpeg", "jpe");
        add("PNG Image", ARR_IMAGE, "image/png", "png");
        add("GIF Image", ARR_IMAGE, "image/gif", "gif");
        add("BMP Image", ARR_IMAGE, "image/bmp", "bmp");
        add("TIFF Image", ARR_IMAGE, "image/tiff", "tif", "tiff");
        add("WebP Image", ARR_IMAGE, "image/webp", "webp");
        add("AVIF Image", ARR_IMAGE, "image/avif", "avif");
        add("HEIC Image", ARR_IMAGE, "image/heic", "heic", "heif");

        // Audio
        add("MP3 Audio", ARR_AUDIO, "audio/mpeg", "mp3");
        add("M4A Audio", ARR_AUDIO, "audio/mp4", "m4a", "mp4");
        add("WAV Audio", ARR_AUDIO, "audio/x-wav", "wav");
        add("AAC Audio", ARR_AUDIO, "audio/aac", "aac", "m4a");
        add("FLAC Audio", ARR_AUDIO, "audio/flac", "flac");
        add("Ogg Audio", ARR_AUDIO, "audio/ogg", "oga", "ogg");
        add("WMA Audio", ARR_AUDIO, "audio/x-ms-wma", "wma");

        // Video
        add("MP4 Video", ARR_VIDEO, "video/mp4", "mp4", "m4v");
        add("WebM Video", ARR_VIDEO, "video/webm", "webm");
        add("AVI Video", ARR_VIDEO, "video/x-msvideo", "avi");
        add("QuickTime Video", ARR_VIDEO, "video/quicktime", "mov", "qt");
        add("Matroska Video", ARR_VIDEO, "video/x-matroska", "mkv");
        add("WMV Video", ARR_VIDEO, "video/x-ms-wmv", "wmv");
        add("MPEG Video", ARR_VIDEO, "video/mpeg", "mpeg", "mpg");
        add("3GP Video", ARR_VIDEO, "video/3gpp", "3gp", "3gpp");
        add("3G2 Video", ARR_VIDEO, "video/3gpp2", "3g2");
        add("FLV Video", ARR_VIDEO, "video/x-flv", "flv");
        add("MPEG-TS Video", ARR_VIDEO, "video/mp2t", "ts", "m2ts");

        // Documents
        add("PDF Document", ARR_DOCUMENT, "application/pdf", "pdf");
        add("Word Document", ARR_DOCUMENT, "application/msword", "doc");
        add("Word Document", ARR_DOCUMENT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        add("PowerPoint Presentation", ARR_DOCUMENT, "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
        add("Excel Spreadsheet", ARR_DOCUMENT, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
        add("PowerPoint Presentation", ARR_DOCUMENT, "application/vnd.ms-powerpoint", "ppt");
        add("Excel Spreadsheet", ARR_DOCUMENT, "application/vnd.ms-excel", "xls");
        add("Plain Text", ARR_DOCUMENT, "text/plain", "txt", "text", "log");
        add("HTML Document", ARR_DOCUMENT, "text/html", "html", "htm");
        add("XML Document", ARR_DOCUMENT, "application/xml", "xml", "cot");
        add("RTF Document", ARR_DOCUMENT, "application/rtf", "rtf");
        add("ODT Document", ARR_DOCUMENT, "application/vnd.oasis.opendocument.text", "odt");

        // Archives / Compressed
        add("ZIP Archive", ARR_ARCHIVE, "application/zip", "zip");
        add("7z Archive", ARR_ARCHIVE, "application/x-7z-compressed", "7z");
        add("RAR Archive", ARR_ARCHIVE, "application/x-rar-compressed", "rar");
        add("TAR Archive", ARR_ARCHIVE, "application/x-tar", "tar");
        add("GZIP Archive", ARR_ARCHIVE, "application/gzip", "gz", "tgz");
        add("JAR Archive", ARR_ARCHIVE, "application/java-archive", "jar");
        add("APK Archive", ARR_ARCHIVE, "application/vnd.android.package-archive", "apk");

        // Executables / Binaries
        add("Windows Executable", ARR_EXECUTABLE, "application/x-msdownload", "exe", "dll", "msi", "bat", "cmd");
        add("DOS Executable", ARR_EXECUTABLE, "application/x-msdos-program", "com");
        add("ELF Binary", ARR_EXECUTABLE, "application/x-elf", "so");
        add("Shell Script", ARR_EXECUTABLE, "application/x-sh", "sh", "bash");

        // GIS / ESRI
        add("GIS Data", ARR_DATA, DEFAULT_BINARY_MIME_TYPE, "shp", "shx", "dbf", "sbn", "sbx", "atx", "fbn", "fbx", "ain", "aih", "ixs", "mxs", "prj", "cpg");

        // Arbitrary data
        add("Binary Data", ARR_DATA, DEFAULT_BINARY_MIME_TYPE, "bin", "dat");
    }

    private static void add(@Nonnull String name, @Nonnull FileGrouping[] groupings, @Nonnull String mimeType, @Nonnull String... extensions)
    {
        FileType type = new FileType(name, mimeType, extensions, groupings);
        types.add(type);
        for (String extension : extensions) extToType.computeIfAbsent(extension, ext -> new ArrayList<>()).add(type);
    }

    /**
     * @return Set of all recognized file types.
     */
    @Nonnull
    public static Set<FileType> getTypes()
    {
        return unmodifiableSet(types);
    }

    /**
     * @param file Some file to get the type of.
     * @return First recognized file type, or {@code null}.
     */
    @Nullable
    public static FileType getTypeFromExtension(@Nonnull File file)
    {
        return getTypeFromExtension(FileUtils.getExtension(file));
    }

    /**
     * @param extension Some file extension.
     * @return First recognized file type, or {@code null}.
     */
    @Nullable
    public static FileType getTypeFromExtension(@Nullable String extension)
    {
        if (extension == null) return null;
        List<FileType> fileTypes = extToType.get(extension.toLowerCase());
        if (fileTypes != null) return fileTypes.get(0);
        return null;
    }

    /**
     * @param file Some file to get the types of.
     * @return All recognized file types, or an empty list.
     */
    @Nullable
    public static List<FileType> getTypesFromExtension(@Nonnull File file)
    {
        return getTypesFromExtension(FileUtils.getExtension(file));
    }

    /**
     * @param extension Some file extension.
     * @return All recognized file types, or an empty list.
     */
    @Nullable
    public static List<FileType> getTypesFromExtension(@Nullable String extension)
    {
        if (extension == null) return emptyList();
        return extToType.getOrDefault(extension.toLowerCase(), emptyList());
    }

    /**
     * File type entry.
     * @param name File type name.
     * @param mimeType File mime type.
     * @param ext File extensions associated with the type.
     * @param groups File categorizations. Some file types are multipurpose.
     */
    public record FileType(String name, String mimeType, String[] ext, FileGrouping[] groups)
    {
    }

    /**
     * File type grouping.
     */
    public enum FileGrouping
    {
        Image, Audio, Video, Document, Archive, Executable, Data
    }
}