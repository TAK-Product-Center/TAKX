package gov.takx.platform.ui;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.url.Schemes;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.cdi.ContainerFinder;
import gov.takx.platform.cdi.InjectableContainerSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static gov.takx.commons.swing.ImageUtils.DEFAULT_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.DEFAULT_ICON_WIDTH;
import static gov.takx.commons.swing.ImageUtils.DEFAULT_IMAGE_PATH;
import static gov.takx.commons.swing.ImageUtils.DEFAULT_IMAGE_URI;
import static gov.takx.commons.swing.ImageUtils.DEFAULT_IMAGE_URL;
import static gov.takx.commons.swing.ImageUtils.FEATURE_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.FEATURE_ICON_WIDTH;
import static gov.takx.commons.swing.ImageUtils.MEDIUM_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.MEDIUM_ICON_WIDTH;
import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * Collection of utility methods and constants for accessing resources for our UI.
 * <p>
 * This is an internal support class. Our UI resources and their organization is not part of the published API, and can
 * be changed at will.
 * <p>
 * All images and icons returned by this class will be sized at {@link #STANDARD_IMAGE_SIZE} unless otherwise specified.
 *
 * @since 2.37
 */
@Internal
public class UIResources
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Important: don't move this below the icon constants below, since it is used as part of getting the icons
    private static final boolean PLATFORM_UNDER_TEST;

    static
    {
        // While we would like plugins to extend APluginContainerTest so that a container is present, there are some
        // cases where this isn't possible. In such cases we are guaranteed to be under test.
        PLATFORM_UNDER_TEST = !ContainerFinder.hasContainers() ||
                new InjectableContainerSupport().getBean(Boolean.class, PlatformUnderTest.Literal.INSTANCE);
    }

    /**
     * Dimension for an image/icon used as a feature image on a dialog/panel. The main use of this dimension is icons
     * in DialogHeaderPanel or map object icons in various panels.
     *
     * @since 3.3
     */
    public static final Dimension FEATURE_IMAGE_SIZE = new Dimension(FEATURE_ICON_WIDTH, FEATURE_ICON_HEIGHT);

    /**
     * Dimension for an icon used in a button on the toolbar.
     *
     * @since 3.3
     */
    public static final Dimension TOOLBAR_BUTTON_IMAGE_SIZE = new Dimension(DEFAULT_ICON_WIDTH, DEFAULT_ICON_HEIGHT);

    /**
     * Dimension for an icon used in a large action button on the top of a panel (e.g. Add, Remove, Properties, etc.)
     *
     * @since 3.3
     */
    public static final Dimension ACTION_BUTTON_IMAGE_SIZE = new Dimension(MEDIUM_ICON_WIDTH, MEDIUM_ICON_HEIGHT);

    /**
     * Standard size at which all other images are scaled. This prevents problems with various UI components when a
     * plugin provides a much larger image/icon. This size should be used for: buttons, menu items, list items, tabs, etc.
     */
    public static final Dimension STANDARD_IMAGE_SIZE = new Dimension(SMALL_ICON_WIDTH, SMALL_ICON_HEIGHT);

    /** @since 5.7 */
    public static final String PAUSE_ICON_FILE = "pause.svg";
    /** @since 5.7 */
    public static final String PLAY_ICON_FILE = "play.svg";
    /** @since 5.7 */
    public static final String ADD_ICON_FILE = "add.svg";
    /** @since 5.7 */
    public static final String EDIT_ICON_FILE = "edit.svg";
    /** @since 5.7 */
    public static final String REMOVE_ICON_FILE = "remove.svg";
    /** @since 5.7 */
    public static final String DELETE_ICON_FILE = "delete.svg";
    /** @since 5.7 */
    public static final String KEEP_IN_VIEW_ICON_FILE = "keep_in_view.svg";
    /** @since 5.7 */
    public static final String SETTINGS_ICON_FILE = "settings.svg";
    /** @since 5.7 */
    public static final String REFRESH_ICON_FILE = "refresh.svg";
    /** @since 5.7 */
    public static final String CANCEL_ICON_FILE = "cancel.svg";
    /** @since 5.8 */
    public static final String OPEN_ICON_FILE = "open.svg";
    /** @since 5.8 */
    public static final String LAUNCH_ICON_FILE = "launch.svg";
    /** @since 5.8 */
    public static final String ATTACH_ICON_FILE = "attach.svg";
    /** @since 5.8 */
    public static final String ROUTE_ICON_FILE = "route.svg";

    /** @since 5.8 */
    public static final URL ATTACH_ICON_URL = getUrl(PLATFORM_ICONS, ATTACH_ICON_FILE);
    /**
     * Do not use. Only needed by data monitor.
     * @since 5.8
     */
    public static final URL BATTERY_BLACK_ICON_URL = getUrl(PLATFORM_ICONS, "battery_black.png");

    // Various icons at the small standard size for reuse throughout the application.
    public static final ImageIcon ADD_ICON = getStandardSizedIcon(PLATFORM_ICONS, ADD_ICON_FILE);
    public static final ImageIcon CANCEL_ICON = getStandardSizedIcon(PLATFORM_ICONS, CANCEL_ICON_FILE);
    public static final ImageIcon DEVICE_ICON = getStandardSizedIcon(PLATFORM_ICONS, "device.png");
    public static final ImageIcon DELETE_ICON = getStandardSizedIcon(PLATFORM_ICONS, DELETE_ICON_FILE);
    /** @since 5.1 **/
    public static final ImageIcon DELETE_SWEEP_ICON = getStandardSizedIcon(PLATFORM_ICONS, "delete_sweep.png");
    public static final ImageIcon EDIT_ICON = getStandardSizedIcon(PLATFORM_ICONS, EDIT_ICON_FILE);
    public static final ImageIcon EXPORT_ICON = getStandardSizedIcon(PLATFORM_ICONS, "export.png");
    public static final ImageIcon IMPORT_ICON = getStandardSizedIcon(PLATFORM_ICONS, "import.png");
    public static final ImageIcon LOCKED_ICON = getStandardSizedIcon(PLATFORM_ICONS, "lock.png");
    public static final ImageIcon NONE_ICON = getStandardSizedIcon(PLATFORM_ICONS, "none.png");
    public static final ImageIcon OPEN_ICON = getStandardSizedIcon(PLATFORM_ICONS, "open.svg");
    public static final ImageIcon REMOVE_ICON = getStandardSizedIcon(PLATFORM_ICONS, REMOVE_ICON_FILE);
    public static final ImageIcon SETTINGS_ICON = getStandardSizedIcon(PLATFORM_ICONS, SETTINGS_ICON_FILE);
    /** @since 5.2 */
    public static final ImageIcon SPOT_ICON = getStandardSizedIcon(TAK_ASSETS_ICONS, "gugpgpow------.png");
    public static final ImageIcon UNLOCKED_ICON = getStandardSizedIcon(PLATFORM_ICONS, "lock_open.png");
    public static final ImageIcon WARNING_ICON = getStandardSizedIcon(PLATFORM_ICONS, "warning.png");
    /** @since 5.7 */
    public static final ImageIcon KEEP_IN_VIEW_ICON = getStandardSizedIcon(PLATFORM_ICONS, KEEP_IN_VIEW_ICON_FILE);
    /** @since 5.7.1 */
    public static final ImageIcon SORT_ASCENDING_ICON = getStandardSizedIcon(PLATFORM_ICONS, "sort_ascending.svg");
    /** @since 5.7.1 */
    public static final ImageIcon SORT_DESCENDING_ICON = getStandardSizedIcon(PLATFORM_ICONS, "sort_descending.svg");
    /** @since 5.8 */
    public static final ImageIcon ROUTE_ICON = getStandardSizedIcon(PLATFORM_ICONS, ROUTE_ICON_FILE);

    // Icons at the action button size for reuse across multiple panels.
    public static final ImageIcon ADD_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, ADD_ICON_FILE);
    public static final ImageIcon DEVICE_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, "device.png");
    public static final ImageIcon EDIT_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, EDIT_ICON_FILE);
    public static final ImageIcon EXPORT_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, "export.svg");
    public static final ImageIcon GOTO_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, "go_to.png");
    public static final ImageIcon IMPORT_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, "import.svg");
    public static final ImageIcon NONE_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, "none.png");
    public static final ImageIcon REMOVE_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, REMOVE_ICON_FILE);
    public static final ImageIcon SETTINGS_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, SETTINGS_ICON_FILE);
    /** @since 5.7 */
    public static final ImageIcon DELETE_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, DELETE_ICON_FILE);
    /** @since 5.7 */
    public static final ImageIcon KEEP_IN_VIEW_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, KEEP_IN_VIEW_ICON_FILE);
    /** @since 5.7 */
    public static final ImageIcon PLAY_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, PLAY_ICON_FILE);
    /** @since 5.7 */
    public static final ImageIcon PAUSE_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, PAUSE_ICON_FILE);
    /** @since 5.7 */
    public static final ImageIcon REFRESH_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, REFRESH_ICON_FILE);
    /** @since 5.7 */
    public static final ImageIcon CANCEL_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, CANCEL_ICON_FILE);
    /** @since 5.8 */
    public static final ImageIcon LAUNCH_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, LAUNCH_ICON_FILE);
    /** @since 5.8 */
    public static final ImageIcon OPEN_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, OPEN_ICON_FILE);
    /** @since 5.8 */
    public static final ImageIcon ATTACH_ACTION_BUTTON_ICON = getActionButtonIcon(PLATFORM_ICONS, ATTACH_ICON_FILE);

    /** @since 5.8 **/
    public static final ImageIcon ROUTE_TOOLBAR_ICON = getToolbarIcon(PLATFORM_ICONS, ROUTE_ICON_FILE);

    /**
     * A color to use when highlighting elements of the user interface e.g. during hover operations. Should work well as a
     * border but alpha may need to be applied before using as a background behind text. Beware of reusing this reference for
     * things other than borders, as someone may come back in the future and change the color without realizing the impact.
     *
     * @since 5.8
     */
    public static final Color BORDER_HIGHLIGHT_COLOR = new Color(50, 153, 205);

    private UIResources()
    {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the image read from the file in the specified directory. If not found, a default image will be returned.
     * <p>
     * The image is <i>not</i> scaled. If a scaled image is needed, one of the other methods should be used.
     *
     * @param directory The resource directory containing the icon
     * @param imagePath Name of file containing the image to load
     * @return The image from the file in the specified directory at the original size, or default image if getting the
     * image fails
     * @see #getUrl(ResourceDirectory, String, boolean)
     * @see #getImage(URL)
     * @since 3.3
     */
    @Nonnull
    public static BufferedImage getImage(@Nonnull ResourceDirectory directory, @Nonnull String imagePath)
    {
        //noinspection ConstantConditions - won't be null since the default will be returned
        return getImage(getUrl(directory, imagePath, true));
    }

    /**
     * Get the image read from the given URL. If not found, a default image will be returned.
     * <p>
     * The image is <i>not</i> scaled. If a scaled image is needed, one of the other methods should be used.
     *
     * @param imageUrl URL of image to load
     * @return new image from the URL at the original size, or an image for {@link ImageUtils#DEFAULT_IMAGE_URI} if an
     * error occurs.
     * @since 3.3
     */
    @Nonnull
    public static BufferedImage getImage(@Nonnull URL imageUrl)
    {
        //noinspection ConstantConditions - will never be null since defaultOnError is set to true
        return ImageUtils.getImage(imageUrl, true);
    }

    /**
     * Get an image from the given URI resized to the given dimensions.
     *
     * @param imageUri URI of image
     * @param size     Size of desired image
     * @return new image from the image at the URI, at the given size, or an image for
     * {@link ImageUtils#DEFAULT_IMAGE_URI} if an error occurs.
     * @since 3.3
     */
    @Nonnull
    public static BufferedImage getSizedImage(@Nonnull URI imageUri, @Nonnull Dimension size)
    {
        URL imageUrl = null;
        try
        {
            imageUrl = imageUri.toURL();
        } catch (Exception e)
        {
            logger.error("Error initializing image {}, returning default image", imageUri, e);
        }

        //noinspection ConstantConditions - won't be null since the default will be returned
        return imageUrl == null ? ImageUtils.getScaledDefaultImage(size) : getSizedImage(imageUrl, size, true);
    }

    /**
     * Get an image from the given URL, resized to the given dimensions.
     *
     * @param imageUrl URL of image
     * @param size     Size of desired image
     * @return new image from the image at the URL, at the given size, or an image for
     * {@link ImageUtils#DEFAULT_IMAGE_URI} if an error occurs.
     * @since 3.3
     */
    @Nonnull
    public static BufferedImage getSizedImage(@Nonnull URL imageUrl, @Nonnull Dimension size)
    {
        //noinspection ConstantConditions - won't be null since the default will be returned
        return getSizedImage(imageUrl, size, true);
    }

    /**
     * Get an image from the file in the specified directory, resized to the given dimensions.
     *
     * @param directory      The resource directory containing the file
     * @param fileName       The name of the file containing the image
     * @param size           Size of desired image
     * @param defaultOnError Whether to return the default image if an error occurs
     * @return new image from the image at the URL, at the given size. If failure occurs and defaultOnError is true, an
     * image for {@link ImageUtils#DEFAULT_IMAGE_URL} is returned. Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static BufferedImage getSizedImage(@Nonnull ResourceDirectory directory, @Nonnull String fileName,
                                              @Nonnull Dimension size, boolean defaultOnError)
    {
        final URL url = getUrl(directory, fileName, defaultOnError);
        return url != null ? getSizedImage(url, size, defaultOnError)
                : defaultOnError ? ImageUtils.getScaledDefaultImage(size) : null;
    }

    /**
     * Get an image from the given URL resized to the given dimensions.
     *
     * @param imageUrl       URL of image
     * @param size           Size of desired image
     * @param defaultOnError Whether to return the default image if an error occurs
     * @return new image from the image at the URL, at the given size. If failure occurs and defaultOnError is true, an
     * image for {@link ImageUtils#DEFAULT_IMAGE_URL} is returned. Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static BufferedImage getSizedImage(@Nonnull URL imageUrl, @Nonnull Dimension size, boolean defaultOnError)
    {
        return ImageUtils.getScaledImage(imageUrl, size.width, size.height, defaultOnError);
    }

    /**
     * Get an image correctly sized for use as a feature in a dialog/panel.
     *
     * @param directory The resource directory containing the file
     * @param fileName  The name of the file containing the image
     * @return new image from the image file, sized at {@link #FEATURE_IMAGE_SIZE}, or an image for
     * {@link ImageUtils#DEFAULT_IMAGE_URI} if an error occurs.
     * @since 5.3
     */
    @Nonnull
    public static BufferedImage getFeatureSizedImage(@Nonnull ResourceDirectory directory, @Nonnull String fileName)
    {
        return getSizedImage(getUrl(directory, fileName), FEATURE_IMAGE_SIZE);
    }

    /**
     * Get an icon correctly sized for use in nearly all areas of the UI (except for the toolbar, action buttons, or as
     * a feature image).
     *
     * @param directory The resource directory containing the file
     * @param fileName  The name of the file containing the image
     * @return new icon created from the image file, sized at {@link #STANDARD_IMAGE_SIZE}, or
     * {@link ImageUtils#DEFAULT_IMAGE_URI} if an error occurs.
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getStandardSizedIcon(@Nonnull ResourceDirectory directory, @Nonnull String fileName)
    {
        return getStandardSizedIcon(getUrl(directory, fileName));
    }

    /**
     * Get an icon correctly sized for use in nearly all areas of the UI (except for the toolbar, action buttons, or as
     * a feature image).
     *
     * @param imageUrl URL of image to load
     * @return new icon from the image at the URL, sized at {@link #STANDARD_IMAGE_SIZE}, or
     * {@link ImageUtils#DEFAULT_IMAGE_URI} if an error occurs.
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getStandardSizedIcon(@Nonnull URL imageUrl)
    {
        return getSizedIcon(imageUrl, STANDARD_IMAGE_SIZE);
    }

    /**
     * Get an icon correctly sized for use in nearly all areas of the UI (except for the toolbar, action buttons, or as
     * a feature image).
     *
     * @param imageUri URI of image to load
     * @return new icon from the image at the URI, sized at {@link #STANDARD_IMAGE_SIZE}, or
     * {@link ImageUtils#DEFAULT_IMAGE_URI} if an error occurs.
     * @since 5.6
     */
    @Nonnull
    public static ImageIcon getStandardSizedIcon(@Nonnull URI imageUri)
    {
        try
        {
            return getSizedIcon(imageUri.toURL(), STANDARD_IMAGE_SIZE);
        } catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Cannot get URL from URI", e);
        }
    }

    /**
     * Get an icon correctly sized for use in an action button (i.e., the large buttons at the top of panels for Add,
     * Remove, etc.)
     *
     * @param directory The resource directory containing the file
     * @param fileName  The name of the file containing the image
     * @return new icon created from the image file, sized at {@link #ACTION_BUTTON_IMAGE_SIZE}, or an icon for
     * {@link ImageUtils#DEFAULT_IMAGE_URL} if getting the icon fails.
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getActionButtonIcon(@Nonnull ResourceDirectory directory, @Nonnull String fileName)
    {
        return getSizedIcon(getUrl(directory, fileName), ACTION_BUTTON_IMAGE_SIZE);
    }

    /**
     * Get an icon correctly sized for use in an action button (i.e., the large buttons at the top of panels for Add, Remove,
     * etc.)
     *
     * @param url the icon resource URL
     * @return new icon created from the URL, sized at {@link #ACTION_BUTTON_IMAGE_SIZE}, or an icon for
     * {@link ImageUtils#DEFAULT_IMAGE_URL} if getting the icon fails.
     * @since 5.7
     */
    @Nonnull
    public static ImageIcon getActionButtonIcon(@Nonnull URL url)
    {
        return getSizedIcon(url, ACTION_BUTTON_IMAGE_SIZE);
    }

    /**
     * Get an icon correctly sized for use in a toolbar button.
     *
     * @param directory The resource directory containing the file
     * @param fileName  The name of the file containing the image
     * @return new icon created from the image file, sized at {@link #TOOLBAR_BUTTON_IMAGE_SIZE}, or an icon for
     * {@link ImageUtils#DEFAULT_IMAGE_URL} if getting the icon fails.
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getToolbarIcon(@Nonnull ResourceDirectory directory, @Nonnull String fileName)
    {
        return getSizedIcon(getUrl(directory, fileName), TOOLBAR_BUTTON_IMAGE_SIZE);
    }

    /**
     * Get a colorized icon correctly sized for use in a toolbar button.
     *
     * @param directory the resource directory containing the file
     * @param fileName  the name of the file containing the image
     * @param color the Color to use for colorization, or {@code null} for no colorization
     * @return the new icon created from the image file, sized at {@link #TOOLBAR_BUTTON_IMAGE_SIZE}, or an icon for
     * {@link ImageUtils#DEFAULT_IMAGE_URL} if getting the icon fails.
     * @since 5.5
     */
    @Nonnull
    public static ImageIcon getColorizedToolbarIcon(@Nonnull ResourceDirectory directory, @Nonnull String fileName, @Nullable Color color)
    {
        return getSizedColorizedIcon(getUrl(directory, fileName), TOOLBAR_BUTTON_IMAGE_SIZE, color);
    }

    /**
     * Get an icon correctly sized for use as a feature image on a dialog/panel.
     *
     * @param directory The resource directory containing the file
     * @param fileName  The name of the file containing the image
     * @return new icon created from the image file, sized at {@link #FEATURE_IMAGE_SIZE}, or an icon for
     * {@link ImageUtils#DEFAULT_IMAGE_URL} if getting the icon fails.
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getFeatureIcon(@Nonnull ResourceDirectory directory, @Nonnull String fileName)
    {
        return getSizedIcon(getUrl(directory, fileName), FEATURE_IMAGE_SIZE);
    }

    /**
     * Get an icon resized to the given dimensions.
     *
     * @param imageUri URI of image
     * @param size     Size of desired image
     * @return new icon from the image at the URI, at the given size, or an icon for
     * {@link ImageUtils#DEFAULT_IMAGE_URL} if an error occurs.
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getSizedIcon(@Nonnull URI imageUri, @Nonnull Dimension size)
    {
        try
        {
            return getSizedIcon(imageUri.toURL(), size);
        } catch (Exception e)
        {
            logger.error("Error initializing image {}, returning default image", imageUri, e);
        }

        return getSizedIcon(DEFAULT_IMAGE_URL, size);
    }

    /**
     * Get an icon resized to the given dimensions.
     *
     * @param imageUrl URL of image
     * @param size     Size of desired icon
     * @return new icon created from the image at the URL, at the given size, or an icon for
     * {@link ImageUtils#DEFAULT_IMAGE_URL} if getting the icon fails
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getSizedIcon(@Nonnull URL imageUrl, @Nonnull Dimension size)
    {
        //noinspection ConstantConditions - won't be null since the default will be returned
        return getSizedIcon(imageUrl, size, true);
    }

    /**
     * Get a colorized icon resized to the given dimensions.
     *
     * @param imageUrl the URL of image
     * @param size the size of desired icon
     * @param color the Color to use for colorization, or {@code null} for no colorization
     * @return new icon created from the image at the URL, at the given size, or an icon for
     * {@link ImageUtils#DEFAULT_IMAGE_URL} if getting the icon fails
     * @since 5.5
     */
    @Nonnull
    public static ImageIcon getSizedColorizedIcon(@Nonnull URL imageUrl, @Nonnull Dimension size, @Nullable Color color)
    {
        //noinspection ConstantConditions - won't be null since the default will be returned
        return ImageUtils.getScaledColorizedIcon(imageUrl, size.width, size.height, true, color);
    }

    /**
     * Get an icon from the file in the specified directory, resized to the given dimensions.
     *
     * @param directory The resource directory containing the file
     * @param fileName The name of the file containing the image
     * @param size Size of desired icon
     * @return new icon from the image at the URL, at the given size. If failure occurs an icon for
     * {@link ImageUtils#DEFAULT_IMAGE_URL} is returned.
     * @since 5.7
     */
    @Nonnull
    public static ImageIcon getSizedIcon(@Nonnull ResourceDirectory directory, @Nonnull String fileName,
                                         @Nonnull Dimension size)
    {
        //noinspection DataFlowIssue - Nonnull when defaultOnError flag is true
        return getSizedIcon(directory, fileName, size, true);
    }

    /**
     * Get an icon from the file in the specified directory, resized to the given dimensions.
     *
     * @param directory      The resource directory containing the file
     * @param fileName       The name of the file containing the image
     * @param size           Size of desired icon
     * @param defaultOnError Whether to return the default image icon if an error occurs
     * @return new icon from the image at the URL, at the given size. If failure occurs and defaultOnError is true, an
     * icon for {@link ImageUtils#DEFAULT_IMAGE_URL} is returned. Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static ImageIcon getSizedIcon(@Nonnull ResourceDirectory directory, @Nonnull String fileName,
                                         @Nonnull Dimension size, boolean defaultOnError)
    {
        final URL url = getUrl(directory, fileName, defaultOnError);
        return url != null ? getSizedIcon(url, size, defaultOnError)
                : defaultOnError ? getSizedIcon(DEFAULT_IMAGE_URL, size, true) : null;
    }

    /**
     * Get an icon for the provided image URL.
     *
     * @param imageUrl       URL of image to load
     * @param size           Size of desired icon
     * @param defaultOnError Whether to return the default image icon if an error occurs
     * @return new icon from the image at the URL, at the given size. If failure occurs and defaultOnError is true, an
     * icon for {@link ImageUtils#DEFAULT_IMAGE_URL} is returned. Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static ImageIcon getSizedIcon(@Nonnull URL imageUrl, @Nonnull Dimension size, boolean defaultOnError)
    {
        return ImageUtils.getScaledIcon(imageUrl, size.width, size.height, defaultOnError);
    }

    /**
     * Get the URI for the image file in the specified directory.
     *
     * @param directory The resource directory containing the file
     * @param fileName Name of the file containing the image
     * @return URI for the image file in the specified resource directory or {@link ImageUtils#DEFAULT_IMAGE_URI} if an
     * error occurs.
     * @since 3.3
     */
    @Nonnull
    public static URI getUri(@Nonnull ResourceDirectory directory, @Nonnull String fileName)
    {
        //noinspection ConstantConditions - won't be null since the default will be returned
        return getUri(directory, fileName, true);
    }

    /**
     * Get the URI for the image file in the specified directory.
     *
     * @param directory The resource directory containing the file
     * @param fileName Name of the file containing the image
     * @return URI for the image file. If failure occurs and defaultOnError is true, {@link ImageUtils#DEFAULT_IMAGE_URI}
     * is returned. Otherwise, {@code null} is returned.
     * @since 3.0 (renamed in 3.3)
     */
    @Nullable
    public static URI getUri(@Nonnull ResourceDirectory directory, @Nonnull String fileName, boolean defaultOnError)
    {
        final URL url = getUrl(directory, fileName, false);
        try
        {
            if (url != null) return url.toURI();
        } catch (URISyntaxException e)
        {
            logger.error("Error getting URI for URL {}", url, e);
        }

        return defaultOnError ? DEFAULT_IMAGE_URI : null;
    }

    /**
     * Get the URL for the image file in the specified directory.
     *
     * @param directory The resource directory containing the file
     * @param fileName The name of the file containing the image
     * @return URL for the image file, or {@link ImageUtils#DEFAULT_IMAGE_URL} if the resulting URL is malformed
     * @since 3.0 (renamed in 3.3)
     */
    @Nonnull
    public static URL getUrl(@Nonnull ResourceDirectory directory, @Nonnull String fileName)
    {
        //noinspection ConstantConditions - won't be null since the default will be returned
        return getUrl(directory, fileName, true);
    }

    /**
     * Get the URL for the image file in the specified directory, returning the default icon URL on failure.
     *
     * @apiNote The URL returned from this method is "portable" in that it doesn't include any reference to the platform
     * jar file (in the case of resources in {@link ResourceDirectory#PLATFORM_ICONS}).  It should be used whenever
     * resource URLs are to be persisted in the project database.
     *
     * @param directory      The resource directory containing the file
     * @param fileName       The name of the file containing the image
     * @param defaultOnError Whether to return the default URL if an error occurs
     * @return URL for the image file. If failure occurs and defaultOnError is true, {@link ImageUtils#DEFAULT_IMAGE_URL}
     * is returned. Otherwise, {@code null} is returned.
     * @since 3.0 (renamed in 3.3)
     */
    @Nullable
    public static URL getUrl(@Nonnull ResourceDirectory directory, @Nonnull String fileName, boolean defaultOnError)
    {
        // When running under test, there is a chance the external resources won't be available so just return the
        // default image URL to avoid any errors.
        if (shouldReturnDefault(directory)) return DEFAULT_IMAGE_URL;

        try
        {
            final String defaultOption = defaultOnError ? ":defaultOnError" : "";
            return new URL(Schemes.UI_RESOURCE + ':' + directory.name() + defaultOption + "/" + fileName);
        } catch (MalformedURLException e)
        {
            logger.error("Error getting URL for {} in {}, returning default image URL: {}", fileName, directory.getDirectoryPath(), defaultOnError, e);
        }

        return defaultOnError ? DEFAULT_IMAGE_URL : null;
    }

    /**
     * Get the path for an image file. This is needed for some WorldWind classes that take a resource name instead of a
     * file, URL, or URI.
     *
     * @param directory The resource directory containing the file
     * @param fileName  The name of the file containing the image
     * @return The path to use with {@link java.lang.ClassLoader#getResource(java.lang.String)} to obtain the image.
     * @since 3.3
     */
    public static String getPath(@Nonnull ResourceDirectory directory, @Nonnull String fileName)
    {
        // When running under test, there is a chance the external resources won't be available so just return the
        // default image path to avoid any errors.
        if (shouldReturnDefault(directory)) return DEFAULT_IMAGE_PATH;

        return directory.getDirectoryPath() + fileName;
    }

    /**
     * Checks whether we are under test and if so, whether the resource directory exists. When plugins use the test
     * framework, any references to external resources will fail because the test isn't being run in deployment/bin. If
     * this method determines that is the case, it will return true to indicate that a default URL/path should be
     * returned to avoid errors.
     *
     * @param directory The directory from which a resource is being requested
     * @return True if the default resource should be returned because we are under test and the provided directory
     * doesn't exist.
     * @since 3.4
     */
    private static boolean shouldReturnDefault(ResourceDirectory directory)
    {
        if (!directory.isExternal()) return false;
        if (!PLATFORM_UNDER_TEST) return false;

        // If we got here, check if the external directory exists so we don't return the default URL during TAKX
        // tests, since some verify that the expected URL was provided in order to verify the logic worked as
        // expected (e.g. MarkerIconAdapterTest).
        final File directoryFile = new File(directory.getDirectoryPath());
        return !directoryFile.exists();
    }

    /**
     * Loads in an SVG located in the specified directory.
     * <p/>
     * Defaults to {@link BufferedImage#TYPE_INT_ARGB} for the image type
     *
     * @param loader         The {@link SVGLoader} to use
     * @param directory      The directory from which a resource is being requested
     * @param imagePath      Name of file containing the image to load
     * @param width          The width to render the SVG; pass -1 use SVG width
     * @param height         The height to render the SVG; pass -1 use SVG height
     * @param defaultOnError Whether to return the default image if an error occurs
     * @return {@code BufferedImage} created from the SVG file. If failure occurs and defaultOnError is {@code true},
     *         returns {@link ImageUtils#getDefaultImage()}, else {@code null}
     *
     * @since 5.0
     */
    @Nullable
    public static BufferedImage getSvg(@Nullable SVGLoader loader, @Nonnull ResourceDirectory directory, @Nonnull String imagePath, int width, int height, boolean defaultOnError)
    {
        URL svgUrl = getUrl(directory, imagePath, defaultOnError);
        if (svgUrl == null) return defaultOnError ? ImageUtils.getDefaultImage() : null;
        return getSvg(loader, svgUrl, width, height, defaultOnError);
    }

    /**
     * Loads in an SVG located at the given URL.
     * <p/>
     * Defaults to {@link BufferedImage#TYPE_INT_ARGB} for the image type
     *
     * @param loader         The {@link SVGLoader} to use
     * @param svgUrl         The URL to load the SVG from
     * @param width          The width to render the SVG; pass -1 to use SVG width
     * @param height         The height to render the SVG; pass -1 to use SVG height
     * @param defaultOnError Whether to return the default image if an error occurs
     * @return {@code BufferedImage} created from the SVG file. If failure occurs and defaultOnError is {@code true},
     *         returns {@link ImageUtils#getDefaultImage()}, else {@code null}
     *
     * @since 5.0
     */
    @Nullable
    public static BufferedImage getSvg(@Nullable SVGLoader loader, @Nonnull URL svgUrl, int width, int height, boolean defaultOnError)
    {
        try (var inputStream = svgUrl.openStream())
        {
            return getSvg(loader, inputStream, width, height, BufferedImage.TYPE_INT_ARGB, defaultOnError);
        } catch (IOException e)
        {
            logger.warn("Failed to open '{}', returning default image: {}", svgUrl, defaultOnError);
            return defaultOnError ? ImageUtils.getDefaultImage() : null;
        }
    }

    /**
     * Loads in an SVG from the provided {@link InputStream}.
     *
     * @param loader            The {@link SVGLoader} to use, a new loader will be created if {@code null}
     * @param svgInputStream    The {@link InputStream} to load the SVG from
     * @param width             The width to render the SVG; pass -1 to use SVG width
     * @param height            The height to render the SVG; pass -1 to use SVG height
     * @param bufferedImageType The {@link BufferedImage} type to use when rendering the SVG
     * @param defaultOnError    Whether to return the default image if an error occurs
     * @return {@link BufferedImage} created from the SVG file. If failure occurs and defaultOnError is {@code true},
     *         returns {@link ImageUtils#getDefaultImage()}, else {@code null}
     *
     * @since 5.4
     */
    @Nullable
    public static BufferedImage getSvg(@Nullable SVGLoader loader, @Nonnull InputStream svgInputStream, int width, int height, int bufferedImageType, boolean defaultOnError)
    {
        loader = loader == null ? new SVGLoader() : loader;
        SVGDocument svgDocument = loader.load(svgInputStream, null, LoaderContext.createDefault());
        if (svgDocument == null)
        {
            logger.warn("Cannot create SVG from stream, returning default image: {}", defaultOnError);
            return defaultOnError ? ImageUtils.getDefaultImage() : null;
        }
        return getBufferedImage(width, height, bufferedImageType, svgDocument);
    }

    /**
     * Handle the common creation of {@link BufferedImage} files.
     * @param width             The width to render the SVG; pass -1 to use SVG width
     * @param height            The height to render the SVG; pass -1 to use SVG height
     * @param bufferedImageType The {@link BufferedImage} type to use when rendering the SVG
     * @param svgDocument       The {@link SVGDocument} to be rendered
     * @return a {@link BufferedImage} of the provided {@link SVGDocument}
     *
     * @since 5.4
     */
    @Nonnull
    private static BufferedImage getBufferedImage(int width, int height, int bufferedImageType, SVGDocument svgDocument)
    {
        FloatSize svgSize = svgDocument.size();
        int svgWidth = width <= -1 ? (int) svgSize.width : width;
        int svgHeight = height <= -1 ? (int) svgSize.height : height;
        BufferedImage image = new BufferedImage(svgWidth, svgHeight, bufferedImageType);
        Graphics2D g = ImageUtils.createHighQualityGraphics(image);
        svgDocument.render(null, g, new ViewBox(svgWidth, svgHeight));
        g.dispose();
        return image;
    }
}
