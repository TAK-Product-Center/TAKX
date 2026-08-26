package gov.takx.core.view.gui.screenimageutils;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.data.BufferedImageRaster;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IGlobeDelegate.SnapshotFormat;
import gov.takx.api.globe.ISnapshotListener;
import gov.takx.api.globe.IWidgetInstance;
import gov.takx.api.globe.IWidgetLayerManager;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.ApplicationGlobe;
import gov.takx.core.view.gui.DialogHeaderPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utilities used to create snapshots of the globe, including geo-rectified TIFF images.
 *
 * @since 1.14 (pretty completely reworked in 5.4.1)
 */
@ApplicationScoped
public class SnapshotUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ICON = "print_screen.png";
    private static final String DESCRIPTION = """
            <html>Take a screenshot of the globe and save it to the chosen location. The screenshot will only include the globe.<br><br>
            <font color="red"><b>Warning: This feature is intended for printing only. Geo-rectification accuracy is not guaranteed!</b><br>
            NOTE: It may be necessary for this feature to adjust the globe view in order to capture an image.</font></html>""";

    private final IExtendedGlobeManager globeManager;
    private final IWidgetLayerManager widgetLayerManager;
    private final INotificationService notificationService;

    private volatile boolean snapshotInProgress;

    @Inject
    SnapshotUtils(IExtendedGlobeManager globeManager, IWidgetLayerManager widgetLayerManager, INotificationService notificationService)
    {
        this.globeManager = globeManager;
        this.widgetLayerManager = widgetLayerManager;
        this.notificationService = notificationService;
    }

    /**
     * @param formats Formats to show in the file chooser, if empty all formats will be allowed
     * @return File chooser for selecting a file into which to save a captured snapshot
     * @since 5.4.1
     */
    public AlwaysOnTopFileChooser getSnapshotFileChooser(@Nonnull SnapshotFormat... formats)
    {
        // If not specified, allow all supported formats
        if (formats.length == 0) formats = SnapshotFormat.values();

        final String[] extensions = Stream.of(formats).map(SnapshotFormat::getExtension).toArray(String[]::new);
        final String description = formats.length == 1
                ? formats[0] + " (*." + formats[0].getExtension() + ")"
                : "Snapshot formats (" + StringUtils.join(extensions, ", ", "*.") + ")";

        final var fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(getClass());
        fileChooser.resetChoosableFileFilters();
        fileChooser.setFileFilter(new FileNameExtensionFilter(description, extensions));

        // Configure the description and icon on the file chooser dialog.
        DialogHeaderPanel headerPanel = new DialogHeaderPanel();
        headerPanel.setIcon(ICON);
        headerPanel.setDescription(DESCRIPTION);
        fileChooser.addHeaderPanel(headerPanel);

        return fileChooser;
    }

    /**
     * Create a prompt for the user to select a location to create a snapshot of globe using the given {@code snapshotFormat}.
     * @since 5.4.1
     */
    @SuppressWarnings("DataFlowIssue")
    @InvokeOnEDT
    public void createSnapShotLocationPrompt(SnapshotFormat snapshotFormat)
    {
        final JFileChooser fileChooser = getSnapshotFileChooser(snapshotFormat);

        // User selected a location to create the snapshot.
        if (fileChooser.showSaveDialog(JFrame.getFrames()[0]) == JFileChooser.APPROVE_OPTION)
        {
            File outputFile = fileChooser.getSelectedFile();
            final String extension = FileUtils.getExtension(outputFile);
            if (!snapshotFormat.getExtension().equals(extension))
            {
                outputFile = new File(outputFile.getAbsolutePath() + '.' + snapshotFormat.getExtension());
            }

            createSnapshot(globeManager.getLastFocusedGlobe(), null, null, outputFile, snapshotFormat,
                    null, true, false);
        }
    }

    /**
     * Silently create a snapshot of the primary globe.
     * 
     * @param outputFile The file to write the snapshot image to
     * @param snapshotFormat The image format to use
     * @since 5.4.1
     */
    public void createSnapshot(@Nonnull File outputFile, @Nonnull SnapshotFormat snapshotFormat)
    {
        createSnapshot(globeManager.getPrimaryGlobeDelegate(), null, null, outputFile, snapshotFormat,
                null, false, true);
    }

    /**
     * Creates a snapshot and saves it in the given file.
     *
     * @param globe Globe to operate upon
     * @param minLocation The minimum lat lon bounds for the snapshot.
     * @param maxLocation The maximum lat lon bounds for the snapshot.
     * @param outputFile The file where the snapshot will be saved.
     * @param snapshotFormat The image format that the snapshot will be saved as.
     * @param snapshotListener A listener that will be notified on snapshot completion
     * @param showAfterCreation Pass {@code true} to open the captured image with the default program for viewing the snapshot's file type
     * @param silent Pass {@code true} to suppress the user notifications
     * @since 5.4.1
     */
    public void createSnapshot(@Nonnull IApplicationGlobe globe, @Nullable ILocation minLocation, @Nullable ILocation maxLocation,
                               @Nonnull File outputFile, @Nonnull SnapshotFormat snapshotFormat, @Nullable ISnapshotListener snapshotListener,
                               boolean showAfterCreation, boolean silent)
    {
        if (snapshotInProgress)
        {
            logger.debug("A call to createSnapshot was ignored because a snapshot was already in progress.", new RuntimeException());
        } else
        {
            snapshotInProgress = true;

            new SnapshotHandler(globe, outputFile, snapshotFormat, snapshotListener, showAfterCreation, silent)
                    .captureImage(minLocation, maxLocation);
        }
    }

    /**
     * Write an image out as a GEOTIFF with the given {@code sector} bounds for geo-rectification.
     * 
     * @param image Image to write
     * @param sector Geo-bounds of the image
     * @param outputFile File to write to
     */
    public void writeGeotiff(@Nonnull BufferedImage image, @Nonnull Sector sector, @Nonnull File outputFile) throws IOException
    {
        final var params = new AVListImpl();

        params.setValue(AVKey.SECTOR, sector);
        params.setValue(AVKey.COORDINATE_SYSTEM, AVKey.COORDINATE_SYSTEM_GEOGRAPHIC);
        params.setValue(AVKey.PIXEL_FORMAT, AVKey.IMAGE);
        params.setValue(AVKey.BYTE_ORDER, AVKey.BIG_ENDIAN);

        final var writer = new GeoTiffWriter(outputFile);
        writer.write(BufferedImageRaster.wrapAsGeoreferencedRaster(image, params));
        writer.close();
    }

    /**
     * Handles the operations needed to capture a globe snapshot, including any view manipulations that may be needed.
     * @since 5.4.1
     */
    private class SnapshotHandler
    {
        private final IApplicationGlobe globe;
        private final IWorldWindowGL worldWindow;
        private final IFavorite originalView;
        private final File outputFile;
        private final SnapshotFormat snapshotFormat;
        private final ISnapshotListener snapshotListener;
        private final boolean showAfterCreation;
        private final boolean silent;

        private final GlobeRenderingListener globeRenderingListener = new GlobeRenderingListener();
        private final List<IWidgetInstance> widgetsMadeInvisible = new ArrayList<>();

        /**
         * @param globe Globe to operate upon
         * @param outputFile The file where the snapshot will be saved
         * @param snapshotFormat The image format that the snapshot will be saved as
         * @param snapshotListener A listener that will be notified on snapshot completion
         * @param showAfterCreation Pass {@code true} to open the captured image with the default program for viewing the snapshot's file type
         * @param silent Pass {@code true} to suppress the user notifications
         */
        private SnapshotHandler(IApplicationGlobe globe, File outputFile, SnapshotFormat snapshotFormat,
                                ISnapshotListener snapshotListener, boolean showAfterCreation, boolean silent)
        {
            this.globe = globe;
            worldWindow = globe.getWorldWindow();
            originalView = globe.getViewBookmark();

            this.outputFile = outputFile;
            this.snapshotFormat = snapshotFormat;
            this.snapshotListener = snapshotListener;
            this.showAfterCreation = showAfterCreation;
            this.silent = silent;
        }

        /**
         * Adjust our view as needed, and capture the globe image.
         *
         * @param minLocation Optional min location of the snapshot
         * @param maxLocation Optional max location of the snapshot
         */
        @InvokeOnEDT
        private void captureImage(@Nullable ILocation minLocation, @Nullable ILocation maxLocation)
        {
            if (worldWindow != null)
            {
                if (snapshotFormat == SnapshotFormat.GEOTIFF) hideWidgets();
                adjustViewAndCapture(minLocation, maxLocation);
            } else
            {
                logger.debug("worldWindow is null, skipping screen capture");
            }
        }

        /**
         * Remove listener and restore widgets and original view.
         */
        private void restore()
        {
            worldWindow.removeRenderingListener(globeRenderingListener);

            // Restore any widgets we made invisible at the start of the capture
            widgetsMadeInvisible.forEach(instance -> instance.setVisible(true));

            // Turn off flat world mode and go back to the original position.
            if (snapshotFormat == SnapshotFormat.GEOTIFF)
            {
                globe.setFlatWorldMode(false);
                worldWindow.redrawNow();

                // We have reset flat world mode, but the globe view (ApplicationOrbitView) doesn't yet know it.
                // The view updates its globe reference on each frame rendered, so we need to toss this next
                // operation into a future event on the EDT so that the view has a chance to be updated.  Further,
                // this is only needed because with a flat globe still installed, the pitch saved in originalView
                // will NOT be restored since flat earth views are always top-down, pitch of 0.
                if (originalView != null) SwingUtilities.invokeLater(() -> globe.goTo(originalView));
            }

            snapshotInProgress = false;
        }

        /**
         * Capturing an image has several steps, depending on the image format desired.
         * <p/>
         * For capturing a GEOTIFF image:
         * <ol>
         *     <li>Temporarily hide all widgets</li>
         *     <li>Reset heading and tilt</li>
         *     <li>Zoom to sector (IFF min/maxLocation are specified)</li>
         *     <li>Switch to flat-world mode</li>
         *     <li>Force a redraw of the globe and capture the resulting image</li>
         *     <li>Reset the view to the ellipsoidal globe and restore widgets</li>
         * </ol>
         * For all other image formats simply force a redraw of the globe and capture the resulting image.
         *
         * @param minLocation Optional min location of the snapshot
         * @param maxLocation Optional max location of the snapshot
         */
        private void adjustViewAndCapture(ILocation minLocation, ILocation maxLocation)
        {
            final var snapshotSector = minLocation != null && maxLocation != null
                    ? new Sector(Angle.fromDegrees(minLocation.getLatitude()), Angle.fromDegrees(maxLocation.getLatitude()),
                            Angle.fromDegrees(minLocation.getLongitude()), Angle.fromDegrees(maxLocation.getLongitude()))
                    : null;

            if (snapshotSector != null)
            {
                ((ApplicationGlobe) globe).goToImmediate(snapshotSector);
            } else
            {
                if (snapshotFormat == SnapshotFormat.GEOTIFF)
                {
                    // Change our view to top-down and north up, don't use animation to make these changes
                    final var viewController = globe.getGlobeViewController();
                    viewController.setTilt(0, false);
                    viewController.setHeading(0, false);
                }
            }

            worldWindow.redrawNow();
            SwingUtilities.invokeLater(() -> worldWindow.addRenderingListener(globeRenderingListener));
        }

        /**
         * Hide all widgets on the globe so they won't appear in the snapshot.
         */
        private void hideWidgets()
        {
            widgetLayerManager.getWidgetInstancesByGlobe(globe).forEach(instance -> {
                if (instance.isVisible())
                {
                    widgetsMadeInvisible.add(instance);
                    instance.setVisible(false);
                }
            });
        }

        /**
         * @return Rectangle with the width/height of the GL viewport.
         */
        private Rectangle getViewport()
        {
            final var viewport = new int[4];
            worldWindow.getGL().glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
            return new Rectangle(viewport[2], viewport[3]);
        }

        /**
         * Get the Sector of the current globe view extent, if one exists.
         *
         * @return a Sector representing the globe view extent. Or {@code null} if no Sector is available. This will happen
         * if the corner points of the globe component do not point to actual lat/lon positions (i.e. if the entire globe is
         * in view, and any corner of the screen is in outer space)
         */
        private Sector getSector()
        {
            // For some reason, getGlobeLocationFromPoint() sometimes gets an NPE from the WW method view.unproject().
            // Catch it and treat it as any other unavailable Sector scenario.
            try
            {
                final var viewport = getViewport();
                final var minLocation = globe.getGlobeLocationFromPoint(new Point(0, viewport.height - 3));
                final var maxLocation = globe.getGlobeLocationFromPoint(new Point(viewport.width, 0));

                return minLocation == null || maxLocation == null
                        ? null
                        : Sector.fromDegrees(minLocation.getLatitude(), maxLocation.getLatitude(), minLocation.getLongitude(), maxLocation.getLongitude());
            } catch (NullPointerException e)
            {
                return null;
            }
        }

        /**
         * Listener added to the globe in order to catch the rendering events when we force the globe to be redrawn.
         * Once we catch the rendering event we use its source to capture the image of the gl viewport, which contains the globe.
         */
        private class GlobeRenderingListener implements RenderingListener
        {
            private boolean flatWorldInstalled;

            @Override
            public void stageChanged(RenderingEvent renderingEvent)
            {
                // Make sure the rendering event stage is the one that occurs AFTER the globe has been refreshed,
                // so that we draw the most up-to-date globe.
                if (renderingEvent.getStage().equals(RenderingEvent.AFTER_BUFFER_SWAP))
                {
                    if (snapshotFormat == SnapshotFormat.GEOTIFF && !flatWorldInstalled)
                    {
                        installFlatWorld();
                    } else
                    {
                        doCapture();
                    }
                }
            }

            private void installFlatWorld()
            {
                final var eyePosition = worldWindow.getView().getEyePosition();

                // Set the globe to flat world mode and re-install the eyePosition above
                globe.setFlatWorldMode(true);
                worldWindow.getView().setEyePosition(eyePosition);

                flatWorldInstalled = true;
            }

            /**
             * Read the GL buffer into an image and save it.
             */
            private void doCapture()
            {
                try
                {
                    final var sector = getSector();
                    if (sector == null)
                    {
                        notify("Could not capture the image - invalid Sector", Notification.Type.Error);
                    } else
                    {
                        processSnapshotImage(readImage(), sector);
                    }
                } catch (IOException e)
                {
                    logger.error("IOException creating snapshot: {}", outputFile, e);
                    notify("Failed to create snapshot", Notification.Type.Error);
                } finally
                {
                    restore();
                }
            }

            private BufferedImage readImage()
            {
                final GL gl = worldWindow.getGL();
                // TODO - if any prior GL errors exist, this call to readPixelsToBufferedImage will report it,
                //        calling getError clears that.  I don't know why the error is happening, and it doesn't
                //        seem to interfere with the screen capture.
                gl.glGetError();

                final var viewport = getViewport();
                return new AWTGLReadBufferUtil(worldWindow.getGLProfile(), false)
                        .readPixelsToBufferedImage(gl, 0, 0, viewport.width, viewport.height, true);
            }

            /**
             * Write the given image as a geotiff using the vien sector, and display it to the user, if configured.
             */
            private void processSnapshotImage(BufferedImage image, Sector sector) throws IOException
            {
                if (snapshotFormat == SnapshotFormat.GEOTIFF)
                {
                    writeGeotiff(image, sector, outputFile);
                } else
                {
                    ImageIO.write(image, IOUtil.getFileSuffix(outputFile), outputFile);
                }

                if (outputFile.exists())
                {
                    notify("Snapshot saved to " + outputFile, Notification.Type.Success);

                    // Launch the file if indicated
                    if (showAfterCreation) FileUtils.launchFile(outputFile);

                    // Notify the snapshot listener that the snapshot is complete. Don't want to do this on the EDT.
                    if (snapshotListener != null) new Thread(() -> snapshotListener.snapshotCreated(outputFile)).start();
                } else
                {
                    notify("Snapshot file is missing: " + outputFile, Notification.Type.Error);
                }
            }

            private void notify(String message, Notification.Type type)
            {
                if (!silent) notificationService.show(Notification.builder(message, type).build());
            }
        }
    }
}
