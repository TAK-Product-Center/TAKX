package gov.takx.core.view.gui.preferences;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.filesystem.VfsConstants;
import gov.takx.api.ui.AIconSelectionPanel;
import gov.takx.api.url.Schemes;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.ImageFileChooserFilter;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

/**
 */
@PseudoBean
public class GlyphSelectionPanel extends AIconSelectionPanel
{
    private static final Map<String, ImageIcon> scaledImagesAndAddresses = new ConcurrentHashMap<>();
    private static final Set<String> addedIconsGroup = new ConcurrentSkipListSet<>();
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int GLYPH_HEIGHT = 32;
    private static final int GLYPH_WIDTH = 32;
    private static final String GLYPH_DIR = "icons/glyphs/";
    private static final ImageFileChooserFilter fileFilter = new ImageFileChooserFilter();

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("png", "jpg", "bmp", "gif");

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager vfsManager;

    public GlyphSelectionPanel()
    {
        setTransferHandler(new GlyphSelectionPanelTransferHandler(this, fileFilter));
    }

    @PostConstruct
    private void postConstruct()
    {
        addGlyphsInJar();
        addGlyphsInVFSDirectory();
        initIconList();
    }

    @Override
    protected void browseImages()
    {
        JFileChooser openFileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        openFileChooser.setMultiSelectionEnabled(true);
        openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        openFileChooser.setDialogTitle("Choose an image");
        openFileChooser.setFileFilter(fileFilter);

        int retVal = openFileChooser.showOpenDialog(this);
        if (retVal != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        File[] files = openFileChooser.getSelectedFiles();
        for (File file : files)
        {
            if (!file.exists())
            {
                JOptionPane.showMessageDialog(this, "Error reading image file : " + file);
            } else
            {
                addImage(file.getPath());
            }
        }
    }

    private void addGlyphsInVFSDirectory()
    {
        final File glyphDir = vfsManager.getVirtualFileSystem(VfsConstants.GLYPHS_NAMESPACE).getRootDir();

        if (glyphDir.exists() && glyphDir.isDirectory())
        {
            //noinspection ConstantConditions - we know it is a directory and it exists
            for (String glyphFileName : glyphDir.list())
            {
                if (SUPPORTED_EXTENSIONS.contains(FilenameUtils.getExtension(glyphFileName)))
                {
                    final String imagePath = glyphDir.getPath() + "/" + glyphFileName;
                    logger.debug("VFS glyph: {}", imagePath);
                    scaledImagesAndAddresses.put(makeVfsUrlString(glyphFileName), new ImageIcon(imagePath));
                }
            }
        }
    }

    /**
     * @return the full VFS URL string for the given file name
     * @since 5.2
     */
    @Nonnull
    private static String makeVfsUrlString(@Nonnull String glyphFileName)
    {
        return Schemes.VFS_PREFIX + VfsConstants.GLYPHS_NAMESPACE + "/" + glyphFileName;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType") // Intentionally returning mutable collection
    @Override
    public Map<String, ImageIcon> getScaledImagesAndAddresses()
    {
        return scaledImagesAndAddresses;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType") // Intentionally returning mutable collection
    @Override
    public Set<String> getAddedIconsGroup()
    {
        return addedIconsGroup;
    }

    /**
     * Adds the image to the GlyphSelectionPanel and adds a scaled copy of the image to our VFS.
     * The image in the VFS will not be persisted unless it is selected as a glyph.
     */
    @Override
    public ImageIcon addImage(String imagePath)
    {
        try
        {
            final File sourceImageFile = new File(imagePath);

            // Get the file name and extension of the image
            final String fileName = sourceImageFile.getName();

            // Read the image
            final BufferedImage bufferedImage = ImageIO.read(sourceImageFile);

            // Scale the image to be our standard glyph size
            BufferedImage scaledImage = ImageUtils.getScaledInstance(bufferedImage, GLYPH_WIDTH, GLYPH_HEIGHT, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
            // Create a file for the image in the VFS
            final File glyphDir = vfsManager.getVirtualFileSystem(VfsConstants.GLYPHS_NAMESPACE).getRootDir();
            final File imageFile = new File(glyphDir, fileName);

            if (!glyphDir.exists() && !glyphDir.mkdirs())
            {
                logger.error("Failed to create directory: {}", imageFile.getParentFile());
            }

            // Write the image out to the VFS file
            ImageIO.write(scaledImage, FilenameUtils.getExtension(imagePath), imageFile);
            final ImageIcon imageIcon = new ImageIcon(scaledImage);

            // Add the new image to the icon panel
            scaledImagesAndAddresses.put(makeVfsUrlString(fileName), imageIcon);
            initIconList(); // Refresh the icon list

            return imageIcon;
        } catch (IOException e)
        {
            logger.error("Error adding image {}", imagePath, e);
        }
        return null;
    }

    /**
     * Find all the glyphs in our jar, located at {@code GLYPH_DIR}.
     */
    private void addGlyphsInJar()
    {
        final URL dirUrl = GlyphSelectionPanel.class.getClassLoader().getResource(GLYPH_DIR);
        if (dirUrl == null)
        {
            logger.error("Glyphs directory not found: {}", GLYPH_DIR);
            return;
        }

        if ("jar".equals(dirUrl.getProtocol()))
        {
            // Trim off the '!' and trailing path in the jar URL
            final String dirStr = dirUrl.toString();
            final int index = dirStr.indexOf('!');
            final String jarPath = dirStr.substring(0, index);

            try (FileSystem jarFs = FileSystems.newFileSystem(new URI(jarPath), Map.of());
                 Stream<Path> glyphFiles = Files.find(jarFs.getPath(GLYPH_DIR), 1,
                         (path, attr) -> SUPPORTED_EXTENSIONS.contains(FilenameUtils.getExtension(path.toString()))))
            {
                glyphFiles.forEach(path -> {
                    final String pathString = path.toString();
                    final URL url = GlyphSelectionPanel.class.getClassLoader().getResource(pathString);
                    if (url != null) scaledImagesAndAddresses.put(pathString, new ImageIcon(url));
                });
            } catch (IOException | URISyntaxException e)
            {
                logger.error("Failed to scan JAR file: {}", jarPath, e);
            }
        }
    }
}
