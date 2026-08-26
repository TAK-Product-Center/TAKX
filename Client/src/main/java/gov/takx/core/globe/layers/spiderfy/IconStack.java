package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.render.WWIcon;
import gov.takx.api.globe.spiderfy.IIconStack;
import gov.takx.api.globe.spiderfy.IconStackExpansionShape;
import gov.takx.api.globe.spiderfy.IconStackPropertyConstants;
import gov.takx.platform.globe.GlobeTextUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * An icon that can be expanded to reveal a subset of icons.
 *
 * @since 2.26
 */
public class IconStack extends UserFacingIcon implements IIconStack
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UUID uniqueId = UUID.randomUUID();
    private final int hashCode = uniqueId.hashCode();

    private IIconStack parentIconStack;
    private IconStackExpansionShape iconStackExpansionShape = IconStackExpansionShape.CIRCLE;
    private final Collection<IIconStack> childIconStacks = new CopyOnWriteArrayList<>();

    private BufferedImage sourceImage;

    /**
     * Creates a stack of icons.
     *
     * @param imageSource  A URL or image that will be shown at the icon's location.
     * @param iconPosition The geographical location where the icon should be displayed.
     */
    IconStack(Object imageSource, Position iconPosition)
    {
        super(imageSource, iconPosition);

        draggableSupport = new IconStackDraggableSupport(this, WorldWind.RELATIVE_TO_GROUND);
        sourceImage = getImageFromSource(imageSource);
    }

    @Override
    public Stream<IIconStack> getChildIconStacks()
    {
        return childIconStacks.stream();
    }

    @Override
    public IIconStack getParentIconStack()
    {
        return parentIconStack;
    }

    @Override
    public void setParentIconStack(@Nullable IIconStack parentIconStack)
    {
        if (!match(this.parentIconStack, parentIconStack))
        {
            if (this.parentIconStack != null)
            {
                this.parentIconStack.removeChildIconStack(this);
            }
            this.parentIconStack = parentIconStack;

            if (parentIconStack != null)
            {
                parentIconStack.addChildIconStack(this);
            }
        }
    }

    @Override
    public boolean isExpanded()
    {
        return getChildIconStacks().anyMatch(WWIcon::isVisible);
    }

    @Override
    public void addChildIconStack(@Nonnull IIconStack childIconStack)
    {
        if (!childIconStacks.contains(childIconStack))
        {
            childIconStacks.add(childIconStack);
            childIconStack.setParentIconStack(this);

            if (sourceImage != null)
            {
                if (!childIconStacks.isEmpty())
                {
                    String text = Integer.toString(childIconStacks.size());
                    imageTexture = new BasicWWTexture(overlayTextOnImage(text, sourceImage));
                } else
                {
                    imageTexture = new BasicWWTexture(sourceImage);
                }
            }

            firePropertyChange(IconStackPropertyConstants.CHILD_ADDED, null, childIconStack);
        }
    }

    @Override
    public void removeChildIconStack(@Nonnull IIconStack iconStack)
    {
        if (childIconStacks.contains(iconStack))
        {
            childIconStacks.remove(iconStack);
            iconStack.setParentIconStack(null);
            firePropertyChange(IconStackPropertyConstants.CHILD_REMOVED, iconStack, null);
        }
    }

    @Override
    public IconStackExpansionShape getExpansionShape()
    {
        return iconStackExpansionShape;
    }

    @Override
    public void setExpansionShape(IconStackExpansionShape iconStackExpansionShape)
    {
        IconStackExpansionShape oldExpansionShape = this.iconStackExpansionShape;

        this.iconStackExpansionShape = iconStackExpansionShape;

        firePropertyChange(IconStackPropertyConstants.EXPANSION_SHAPE, oldExpansionShape, iconStackExpansionShape);
    }

    @Override
    public void setImageSource(Object imageSource)
    {
        super.setImageSource(imageSource);

        this.sourceImage = getImageFromSource(imageSource);
    }

    @Override
    public void setPosition(Position iconPosition)
    {
        Position oldPosition = getPosition();

        super.setPosition(iconPosition);

        firePropertyChange(IconStackPropertyConstants.POSITION, oldPosition, iconPosition);
    }

    @Override
    public void setAllPositions(Position newRootIconStackPosition)
    {
        Position oldPosition = getPosition();

        setPosition(newRootIconStackPosition);

        firePropertyChange(IconStackPropertyConstants.ALL_POSITIONS, oldPosition, newRootIconStackPosition);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof IconStack iconStack)) return false;
        return Objects.equals(uniqueId, iconStack.uniqueId);
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    /**
     * Appends text to the upper right corner of an image.
     *
     * @param text  The string to show in the upper right corner of the image.
     * @param image The source image to overlay text over.
     * @return A new image containing the content from the {@link #getImageSource()} as well as {@code text}.
     */
    private BufferedImage overlayTextOnImage(String text, BufferedImage image)
    {
        // Compute the size of the text to determine how much padding to add to the new image
        Graphics2D sourceGraphics = image.createGraphics();
        sourceGraphics.setFont(GlobeTextUtils.ICON_STACK_LABEL_FONT);
        int textWidth = image.createGraphics().getFontMetrics().stringWidth(text);

        // Pad the image to make room for the text. Need to double the text width to add equal padding to both
        // sides to prevent the visual position from not matching the actual icon position
        BufferedImage bufferedImage = new BufferedImage(image.getWidth() + textWidth * 2 + 1, image.getHeight(), BufferedImage.TRANSLUCENT);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setFont(GlobeTextUtils.ICON_STACK_LABEL_FONT);
        GlyphVector glyphVector = graphics2D.getFont().createGlyphVector(graphics2D.getFontRenderContext(), text);

        int textHeight = glyphVector.getPixelBounds(graphics2D.getFontRenderContext(), 0, 0).height;

        graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));

        graphics2D.drawImage(image, textWidth, 0, null);
        graphics2D.setColor(Color.WHITE);
        graphics2D.drawString(text, image.getWidth() + textWidth + 1, textHeight + 1);
        graphics2D.dispose();

        return bufferedImage;
    }

    /**
     * Convert the image source for this icon into a BufferedImage. If the source cannot be converted then returns {@code null}.
     *
     * @return A bufferedImage representation of the image source.
     */
    private BufferedImage getImageFromSource(Object imageSource)
    {
        BufferedImage source = null;

        if (imageSource instanceof BufferedImage)
        {
            source = (BufferedImage) getImageSource();
        } else if (imageSource instanceof URL)
        {
            try
            {
                source = ImageIO.read((URL) imageSource);
            } catch (IOException exception)
            {
                logger.warn("Failed to determine icon's area: ", exception);
            }
        }

        return source;
    }

    /**
     * Determines if two icon stacks are equal after checking if either parameter is {@code null}.
     *
     * @param first  The first {@code IconStack} to compare.
     * @param second The second {@code IconStack} to compare.
     * @return True if the parameters are equal; otherwise, false.
     */
    private boolean match(IIconStack first, IIconStack second)
    {
        return first == second || (first != null && first.equals(second));
    }
}
