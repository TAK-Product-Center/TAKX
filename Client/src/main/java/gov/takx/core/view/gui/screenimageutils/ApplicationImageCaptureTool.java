package gov.takx.core.view.gui.screenimageutils;

import com.google.common.util.concurrent.Uninterruptibles;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.ScreenImage;
import gov.takx.core.view.gui.DialogHeaderPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.AWTException;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Creates a JPEG image capture of the app frame and saves it to the user specified location.
 */
public class ApplicationImageCaptureTool
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final SimpleDateFormat FILENAME_FORMAT = new SimpleDateFormat("'screen_image_'yyyy_MM_dd_HH_mm_ss'.jpg'");

    private final Frame frame = JFrame.getFrames()[0];

    /**
     * Initiate the capture.
     *
     * @since 2.37
     */
    @InvokeOnEDT
    public void startCapture()
    {
        final File fileToCreate = getFileToSave();
        if (fileToCreate == null) return;

        // Due to the way that windows are animated, they slowly fade out over time, and thus are visible on
        // the screen, even after they have been set invisible, and disposed.  Even the window closed event has
        // been fired before they are totally gone.  Since we use the Robot class to grab the screenshot, it shows
        // whatever is on the screen at the time it is invoked.  So, we need to delay the time at which we grab
        // the screen content to account for this fade out.  AFAIK, there's no programmatic way to determine that fade
        // out, so I'm just going to use .3 seconds.  Using .1 seconds, I could still see a mostly translucent version
        // of the chooser dialog, but .3 seems to work reliably on my Windows machine.
        new Thread(() -> {
            Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);

            SwingUtilities.invokeLater(() -> createScreenImage(fileToCreate)); // Get this back onto the EDT
        }, "Screen capture").start();
    }

    private void createScreenImage(File fileToCreate)
    {
        try
        {
            ScreenImage.createImage(frame.getBounds(), fileToCreate.getAbsolutePath());
            FileUtils.launchFile(fileToCreate);
        } catch (IOException | AWTException exception)
        {
            logger.error("Error:", exception);
        }
    }

    private File getFileToSave()
    {
        final var chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);

        chooser.setSelectedFile(new File(FILENAME_FORMAT.format(new Date())));
        chooser.resetChoosableFileFilters();
        chooser.setFileFilter(new FileNameExtensionFilter("JPEG (*.jpg)", "jpg"));

        final var headerPanel = new DialogHeaderPanel();
        headerPanel.setDescription("<html>Take a screenshot of the application and save it to the chosen location. " +
                "The screenshot will include all visible menus, side panels, and dialogs.</html>");
        headerPanel.setIcon("print_screen.png");
        chooser.addHeaderPanel(headerPanel);

        File fileToCreate = null;

        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
        {
            fileToCreate = chooser.getSelectedFile();

            // If there is no extension on the file, add one
            if (!"jpg".equals(FileUtils.getExtension(fileToCreate)))
            {
                fileToCreate = new File(fileToCreate.getAbsolutePath() + ".jpg");
            }
        }
        return fileToCreate;
    }
}
