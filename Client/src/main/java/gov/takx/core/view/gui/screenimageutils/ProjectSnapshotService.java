package gov.takx.core.view.gui.screenimageutils;

import gov.takx.api.annotation.Headless;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.globe.IGlobeDelegate.SnapshotFormat;
import gov.takx.api.project.IProjectManager;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.FontUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;

/**
 * Service to manage globe snapshots per-project.
 *
 * @since 5.4.1
 */
@ApplicationScoped
public class ProjectSnapshotService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /** The name of the file in the project directory holding the project snapshot. */
    public static final String PROJECT_SNAP_SHOT_NAME = "snapshot.jpg";

    private final SnapshotUtils snapshotUtils;
    private final IProjectManager projectManager;
    private final boolean headless;

    private boolean shuttingDown;

    @Inject
    ProjectSnapshotService(SnapshotUtils snapshotUtils, IProjectManager projectManager, @Headless boolean headless)
    {
        this.snapshotUtils = snapshotUtils;
        this.projectManager = projectManager;
        this.headless = headless;
    }

    /**
     * Track when we are being shutdown so we skip doing our screen capture on save, because the globe state will
     * have been torn down by the time the project is saved after shutdown is initiated.
     */
    private void handleShuttingDown(@Observes ShuttingDown ignored)
    {
        shuttingDown = true;
    }

    /**
     * Take a snapshot of the globe when the project is saving.
     */
    private void onProjectSaving(@Observes ProjectSaving event)
    {
        if (!shuttingDown) createProjectSnapShot();
    }

    /**
     * Create a project snapshot in the root directory of the current project.
     */
    public void createProjectSnapShot()
    {
        if (!headless) snapshotUtils.createSnapshot(new File(projectManager.getProjectDirectory(), PROJECT_SNAP_SHOT_NAME), SnapshotFormat.JPEG);
    }

    /**
     * Get a scaled image of the snapshot within the given directory.  If both {@code width} and {@code height}
     * are {@code -1}, then the image is returned at its original size (no scaling).
     *
     * @param directory Directory containing the project snapshot
     * @param width Scale to width
     * @param height  Scale to height
     * @return Scaled image, or {@code null} if the snapshot file is not present
     */
    @Nullable
    public BufferedImage getSnapshotForDirectory(@Nonnull File directory, int width, int height)
    {
        final var snapshotFile = new File(directory, PROJECT_SNAP_SHOT_NAME);
        if (!snapshotFile.exists()) return null;

        try
        {
            return width == -1 && height == -1
                    ? ImageUtils.getImage(snapshotFile, true)
                    : ImageUtils.getScaledImage(snapshotFile.toURI().toURL(), width, height, true);
        } catch (MalformedURLException e)
        {
            logger.warn("Failed to get URL for file '{}'", snapshotFile, e);
            return null;
        }
    }

    /**
     * Create a buffered image for one of our default caption types.  One is for the case when a project doesn't
     * have a snapshot file and the other is for when a package is selected.
     *
     * @param forPackage    Pass {@code true} if initializing for package, {@code false} if for project
     * @param width         Width of image to create
     * @param height        Height of image to create
     * @return Generated image
     */
    public BufferedImage makeCaptionImage(boolean forPackage, int width, int height)
    {
        final var bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final var g2 = bufferedImage.createGraphics();
        g2.setColor(Color.darkGray.darker()); // TODO KMB: Fix for FlatLaf (TAKX-739)
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.white);
        Font defaultFont = FontUtils.DEFAULT_FONT;
        g2.setFont(defaultFont.deriveFont(defaultFont.getStyle() | Font.BOLD, defaultFont.getSize() + 12f));

        g2.drawString("No snapshot available", 12, (height / 2));
        g2.drawString(forPackage ? "for packages" : "until project is saved", 20, (height / 2) + 35);

        g2.dispose();
        bufferedImage.flush();

        return bufferedImage;
    }
}
