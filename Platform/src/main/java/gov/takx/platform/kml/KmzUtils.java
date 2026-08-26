package gov.takx.platform.kml;

import gov.nasa.worldwind.ogc.kml.KMLRoot;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.stream.XMLStreamException;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * KMZ Utility methods
 *
 * @since 1.21
 */
public class KmzUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String KML_FOLDER = "CoreKMZExport";

    /**
     * Format a color to KML ABGR color format.  The alpha channel is forced to FF, so Red is {@code FF0000FF}.
     *
     * @param color The color
     * @return the KML ABGR color string.
     * @since 5.5.1
     */
    public static String formatKmlColor(Color color)
    {
        int abgr = (color.getRed() & 0xFF)
                | (color.getGreen() & 0xFF) << 8
                | (color.getBlue() & 0xFF) << 16
                | (0xFF) << 24;
        return String.format("%08X", abgr);
    }

    /**
     * Return a file name for a map object image guaranteed to be unique to the project.
     *
     * @param mapObject the map object
     * @return string key
     */
    public static String getMapObjectImageKey(IMapObject mapObject)
    {
        return FileUtils.makeSafeName(mapObject.getKey()) + ".png";
    }

    /**
     * Return a pathname that will be used as a temporary folder for creating a KMZ file.  Plugins which contribute
     * additional image files will want to use this as the path to store their image files.
     *
     * @return temp folder path string
     */
    public static String getKMZTempFolder()
    {
        return System.getProperty("java.io.tmpdir") + File.separator + KML_FOLDER;
    }

    /**
     * Utility method to for plugins that export their own state to ask the user to select a KML filename to
     * export to.  Returns a File object if the operator succeeds, otherwise {@code null}.
     *
     * @return File object on success, {@code null} otherwise
     */
    public static File exportKMLRequest()
    {
        return exportRequest("KML");
    }

    /**
     * Utility method to for plugins that export their own state to ask the user to select a KMZ filename to
     * export to.  Returns a File object if the operator succeeds, otherwise {@code null}.
     *
     * @return File object on success, {@code null} otherwise
     */
    public static File exportKMZRequest()
    {
        return exportRequest("KMZ");
    }

    /**
     * Utility method for plugins that import their own state separately to ask the user to select a KMZ file to
     * import.  Returns a parsed KMLRoot on success, otherwise {@code null}.
     *
     * @return KMLRoot
     */
    @SuppressWarnings("unused") // called from plugins
    public static KMLRoot importKMZFile()
    {
        final AlwaysOnTopFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(KmzUtils.class);
        chooser.setDialogTitle("Select KMZ file to Import");
        chooser.resetChoosableFileFilters();
        FileFilter kmzType = new FileNameExtensionFilter("KMZ File (.kmz)", "kmz");
        chooser.setFileFilter(kmzType);

        if (chooser.showOpenDialog(JFrame.getFrames()[0]) == JFileChooser.APPROVE_OPTION)
        {
            String kmzPath = chooser.getSelectedFile().getPath();
            File kmzFile = new File(kmzPath);
            if (kmzFile.exists())
            {
                Throwable t = null;
                try
                {
                    return KMLRoot.createAndParse(kmzPath);
                } catch (IOException | XMLStreamException e)
                {
                    t = e;
                } finally
                {
                    if (t != null)
                    {
                        logger.error("Error parsing KMZ file {}", kmzPath, t);
                        AlwaysOnTopJOptionPane.showErrorDialog("Error parsing KMZ File", "Import Error");
                    }
                }
            }
        }
        return null;
    }

    /**
     * Do the actual export.  File extension, either KML or KMZ is specified in the type parameter.
     *
     * @param type String type, KML or KMZ.
     * @return the File object to export to.
     */
    private static File exportRequest(String type)
    {
        String extension = type.toLowerCase();
        AlwaysOnTopFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(KmzUtils.class);
        chooser.setDialogTitle("Save " + type + " file");
        FileFilter kmzType = new FileNameExtensionFilter(type + " File (" + extension + ")", extension);
        chooser.resetChoosableFileFilters();
        chooser.addChoosableFileFilter(kmzType);
        int returnVal = chooser.showSaveDialog(JFrame.getFrames()[0]);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            int overwriteFile = JOptionPane.YES_OPTION;

            if (logger.isDebugEnabled())
            {
                logger.debug("Exporting {} to file {}", type, chooser.getSelectedFile().getAbsolutePath());
            }

            File chooserFile = chooser.getSelectedFile();

            boolean addSuffix = !chooserFile.getAbsolutePath().toLowerCase().endsWith(extension);

            final File kmzFile = (addSuffix)
                    ? new File(chooserFile.getAbsolutePath() + '.' + extension)
                    : chooserFile;

            // AlwaysOnTopJFileChooser already overrides approveSelection to ask for overwrite.  Only need to ask
            // about overwriting if a suffix was added.
            if (kmzFile.exists() && addSuffix)
            {
                Object[] options = {"Yes", "No"};
                overwriteFile = AlwaysOnTopJOptionPane.showOptionDialog(chooser, "A file already exists at " + kmzFile.getAbsolutePath() + ".\nAre you sure you want to overwrite it?", "Confirm Overwrite", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, JOptionPane.NO_OPTION);
            }

            return overwriteFile == JOptionPane.YES_OPTION ? kmzFile : null;
        }
        return null;
    }
}
