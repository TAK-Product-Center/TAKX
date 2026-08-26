package gov.takx.core.milsym;

import com.atakmap.math.PointD;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ShapeType;
import gov.tak.api.symbology.Status;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.takx.api.milsym.IExtendedMilStd2525Service;
import gov.takx.api.milsym.IMilSymIconBuilder;
import gov.takx.api.milsym.IMilSymIconCache;
import gov.takx.api.milsym.MilSymIconScaling;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Assistant for creating a mil-sym graphic. Allows specifying image attributes via a builder pattern.
 *
 * @since 5.1
 */
public class MilSymIconBuilder implements IMilSymIconBuilder
{
    private static final double FONT_SIZE_DIVISOR = 2.2;

    private final IExtendedMilStd2525Service milStd2525Service;
    private final IMilSymIconCache cache;

    private ISymbolTable.Symbol symbol;
    private String code;
    private Affiliation affiliation;
    private Amplifier amplifier;
    private Status status;
    private ShapeType shapeType;
    private AttributeSet attributes;
    private int resolution = DEFAULT_PREVIEW_SIZE;
    private MilSymIconScaling scaling = MilSymIconScaling.SCALE_TO_MAX_DIMENSION;
    private Consumer<Point> centerPointConsumer;

    /**
     * @param milStd2525Service 2525 service for modifying codes and rendering symbols.
     * @param cache Generated symbol image cache.
     */
    MilSymIconBuilder(@Nonnull IExtendedMilStd2525Service milStd2525Service, @Nonnull IMilSymIconCache cache)
    {
        this.milStd2525Service = milStd2525Service;
        this.cache = cache;
    }

    @Override
    @Nullable
    public ISymbolTable.Symbol getSymbol()
    {
        return symbol;
    }

    @Override
    @Nullable
    public String getCode()
    {
        return code;
    }

    @Override
    @Nullable
    public Affiliation getAffiliation()
    {
        return affiliation;
    }

    @Override
    @Nullable
    public ShapeType getShapeType()
    {
        return shapeType;
    }

    @Override
    public int getResolution()
    {
        return resolution;
    }

    @Nonnull
    @Override
    public AttributeSet getAttributes()
    {
        if (attributes == null) attributes = new AttributeSet();
        return attributes;
    }

    @Override
    @Nonnull
    public MilSymIconBuilder setCode(@Nonnull String code)
    {
        this.code = code;

        ISymbolTable table = milStd2525Service.getSymbologyProvider().getSymbolTable();
        symbol = table.getSymbol(code);

        return this;
    }

    @Override
    @Nonnull
    public MilSymIconBuilder setAffiliation(@Nullable Affiliation affiliation)
    {
        this.affiliation = affiliation;
        return this;
    }

    @Nonnull
    @Override
    public IMilSymIconBuilder setAmplifier(@Nullable Amplifier amplifier)
    {
        this.amplifier = amplifier;
        return this;
    }

    @Nonnull
    @Override
    public IMilSymIconBuilder setStatus(@Nullable Status status)
    {
        this.status = status;
        return this;
    }

    @Override
    @Nonnull
    public MilSymIconBuilder setShapeType(@Nullable ShapeType shapeType)
    {
        this.shapeType = shapeType;
        return this;
    }

    @Override
    @Nonnull
    public MilSymIconBuilder setResolution(int resolution)
    {
        this.resolution = resolution;
        return this;
    }

    @Nonnull
    @Override
    public IMilSymIconBuilder setResolutionScaling(@Nonnull MilSymIconScaling scaling)
    {
        this.scaling = scaling;
        return this;
    }

    @Nonnull
    @Override
    public IMilSymIconBuilder setCenterPointConsumer(@Nullable Consumer<Point> centerPointConsumer)
    {
        this.centerPointConsumer = centerPointConsumer;
        return this;
    }

    @Nonnull
    @Override
    public IMilSymIconBuilder addAttribute(@Nonnull String name, @Nullable String value)
    {
        getAttributes().setAttribute(name, value);
        return this;
    }

    @Nonnull
    @Override
    public IMilSymIconBuilder addAttributes(@Nullable AttributeSet attributes)
    {
        if (attributes != null) getAttributes().putAll(attributes);
        return this;
    }

    @Override
    @Nonnull
    public BufferedImage render()
    {
        // User must supply a symbol (that hasn't failed to render in the past).
        if (symbol == null)
        {
            // Record the code as a failure if the symbol lookup failed.
            if (code != null && !cache.isCached(code)) cache.setFailure(code);
            return scale(cache.getUnknownImage());
        }

        // Adapt local code value to target the preferred affiliation.
        String localCode = code;
        if (affiliation != null) localCode = milStd2525Service.setAffiliation(localCode, affiliation);
        if (status != null) localCode = milStd2525Service.setStatus(localCode, status);
        if (amplifier != null) localCode = milStd2525Service.setAmplifier(localCode, amplifier);
        String cacheCode = localCode + computeAttributeSuffix();

        // If we couldn't render the symbol before, it won't render this time.
        if (cache.isFailure(cacheCode)) return scale(cache.getUnknownImage());

        // Lookup the code in the cache and see if we've already rendered it before.
        BufferedImage result = cache.lookup(cacheCode, resolution, scaling);
        if (result != null) return result;

        // Render the symbol and cache the result.
        try
        {
            Bitmap bitmap;
            if (symbol.getContentMask().contains(ShapeType.Point))
            {
                var center = new PointD();
                var hints = new ISymbologyProvider.RendererHints();
                var attrs = Objects.requireNonNullElseGet(attributes, AttributeSet::new);
                hints.iconSize = resolution;
                hints.fontSize = (int) (resolution / FONT_SIZE_DIVISOR);
                hints.shapeType = shapeType;
                hints.fontName = "Arial";
                hints.iconCenterOffset = center; // The mutable point is updated by calling 'renderSinglePointIcon'
                bitmap = milStd2525Service.getSymbologyProvider().renderSinglePointIcon(localCode, attrs, hints);
                if (centerPointConsumer != null) centerPointConsumer.accept(new Point((int) center.x, (int) center.y));
            } else
            {
                // Multi-point rendering usually like: symbologyProvider.renderMultipointSymbol(localCode, points, null, hints);
                //  - Returns a Collection<Feature> which cannot easily be transformed into an 2D image
                //  - Multi-point symbols should have previews associated with them that we can use,
                //    or have single-point variants which are covered in the above block.
                Drawable drawable = symbol.getPreviewDrawable(0);
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            }

            if (bitmap == null) throw new RuntimeException("Failed to render icon, but no exception was thrown");

            // MarshalManager is managed by the TAK Kernel. It has a registry of 'A -> B' class conversions that you
            // can add to, though by default it registers a 'Bitmap -> BufferedImage' which we'll use since we want
            // something easily pluggable into our Swing UI.
            result = MarshalManager.marshal(bitmap, Bitmap.class, BufferedImage.class);

            // Some 2525 graphics do not support the requested resolution when being rendered.
            // So we will scale to fit the resulting image. If it's already the right size this is a no-op.
            result = scale(result);

            // Store the image so that we don't re-render it again later.
            cache.put(cacheCode, resolution, scaling, result);
            return result;
        } catch (Throwable t)
        {
            cache.setFailure(cacheCode);
            return scale(cache.getUnknownImage());
        }
    }

    @Nonnull
    private BufferedImage scale(@Nonnull BufferedImage image)
    {
        return switch (scaling)
        {
            case NONE -> image;
            case SCALE_TO_MAX_DIMENSION -> ImageUtils.getScaledInstanceToMaxSize(image, resolution);
            case SCALE_TO_MIN_DIMENSION -> ImageUtils.getScaledInstanceToMinSize(image, resolution);
        };
    }

    @Nonnull
    private String computeAttributeSuffix()
    {
        // We don't really care what format this suffix is in. Our cache is just a basic Map<String, result>
        // so as long as unique attribute sets result in unique non-empty suffixes it's all that matters.
        //
        // So, just find all the modifiers that might affect the resulting image, and make a hash code from those
        // key/value pairs.

        int hash = 0;
        if (attributes != null)
        {
            // Find the attribute keys that affect the icon generation, sort them to ensure hash calculation is stable
            final var keys = attributes.getAttributeNames().stream()
                    .filter(key -> key.startsWith(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX))
                    .sorted().toList();

            for (String key : keys)
            {
                hash = 31 * hash + key.hashCode();
                hash = 31 * hash + attributes.getStringAttribute(key).hashCode();
            }
        }

        return ":" + hash;
    }
}
