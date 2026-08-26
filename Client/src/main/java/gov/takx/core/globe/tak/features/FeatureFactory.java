
package gov.takx.core.globe.tak.features;

import android.net.Uri;
import com.atakmap.android.maps.graphics.GLBitmapLoader;
import com.atakmap.android.maps.graphics.GLBitmapLoader.QueueType;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.math.MathUtils;
import com.atakmap.spi.PriorityServiceProviderRegistry2;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.url.Schemes;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;

/**
 * A factory for creating the composite parts of a {@link Feature} from legacy rendering objects.
 *
 * @since 3.4
 */
@ApplicationScoped
public class FeatureFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final PriorityServiceProviderRegistry2<IFeatureProvider<?, ?>, Object, FeatureFactorySpi> registry =
            new PriorityServiceProviderRegistry2<>();

    static
    {
        registerSpi(PointPlacemarkFeatureProvider.SPI);
        registerSpi(PathFeatureProvider.SPI);
        registerSpi(AnnotationFeatureProvider.SPI);
        registerSpi(SurfaceShapeFeatureProvider.SPI);
        registerSpi(GlobeShapeMapObjectSpi.SPI);
        registerSpi(MapPointFeatureProvider.SPI);
        registerSpi(MarkerFeatureProvider.SPI);
        registerSpi(GlobeMarkerFeatureProvider.SPI);
        registerSpi(MilStdGlobeShapeFeatureProvider.SPI);
    }

    public FeatureFactory()
    {
        // Install an icon cache to avoid hitting maps.google.com for various icons referenced by the kernel
        GLBitmapLoader.setIconCacheDb(new File("resources/tak-assets/dbs/iconcache.sqlite"), null);

        // Install our custom bitmap/icon URL handlers
        GLBitmapLoader.registerBitmapLoader(Schemes.VFS, new RawUrlBitmapLoaderSpi(), QueueType.ICON);
        GLBitmapLoader.registerBitmapLoader(Schemes.ICONSET, new RawUrlBitmapLoaderSpi(), QueueType.ICON);
        GLBitmapLoader.registerBitmapLoader(Schemes.UI_RESOURCE, new RawUrlBitmapLoaderSpi(), QueueType.ICON);
        GLBitmapLoader.registerBitmapLoader(IIconMapObject.PROTOCOL_SCHEME_SHELL, new ShellResourceBitmapLoaderSpi(), QueueType.ICON);
    }

    public static void registerSpi(FeatureFactorySpi spi)
    {
        synchronized (registry)
        {
            registry.register(spi, spi.getPriority());
        }
    }

    public static void unregisterSpi(FeatureFactorySpi spi)
    {
        synchronized (registry)
        {
            registry.unregister(spi);
        }
    }

    public boolean isSupported(Object object)
    {
        synchronized (registry)
        {
            return registry.create(object) != null;
        }
    }

    /**
     * Create a state object for the provided object. This can be pre-calculated and used to defer creation of {@link FeatureSetResult}.
     * @param obj the object
     * @return the state object
     * @param <T> the object type
     * @param <U> the state object type
     */
    @Nonnull
    public <T, U> FeatureFactoryState<T, U> createState(T obj)
    {
        IFeatureProvider<T, U> takFeatureProvider;
        synchronized (registry)
        {
            //noinspection unchecked
            takFeatureProvider = (IFeatureProvider<T, U>) registry.create(obj);
        }
        if (takFeatureProvider == null) return new FeatureFactoryState<>(null, null);
        U state = takFeatureProvider.createState(obj);
        return new FeatureFactoryState<>(takFeatureProvider, state);
    }

    /**
     * Create a {@link FeatureSetResult} for the provided {@link FeatureFactoryState}. The scope of the returned results can be limited
     * with the update property mask constants found in {@link FeatureDataStore4}. The state is obtained by {@link #createState(Object)} and can be precalculated.
     * @param featureFactoryState the feature factory state
     * @param updatePropertyMask the update property mask
     * @return the feature set results
     */
    @Nonnull
    public <T, U> FeatureSetResult create(@Nonnull FeatureFactoryState<T, U> featureFactoryState, int updatePropertyMask)
    {
        var takFeatureProvider = featureFactoryState.featureProvider;
        var state = featureFactoryState.state;
        if (takFeatureProvider == null || state == null) return new FeatureSetResult(new FeatureResult[0], false);
        int featureCount = takFeatureProvider.getFeatureCount(state);
        var objectKeys = takFeatureProvider.createObjectKey(state);
        var groupingNames = takFeatureProvider.createGroupingNames(state);

        String[] name = new String[featureCount];
        Geometry[] geometry = new Geometry[featureCount];
        Style[] style = new Style[featureCount];
        AttributeSet[] attributeSet = new AttributeSet[featureCount];
        Feature.AltitudeMode[] altitudeMode = new Feature.AltitudeMode[featureCount];
        double[] extrude = new double[featureCount];
        if (MathUtils.hasBits(updatePropertyMask, FeatureDataStore4.PROPERTY_FEATURE_NAME))
        {
            name = takFeatureProvider.createNames(state);
        }
        if (MathUtils.hasBits(updatePropertyMask, FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY))
        {
            geometry = takFeatureProvider.createGeometry(state);
        }
        if (MathUtils.hasBits(updatePropertyMask, FeatureDataStore4.PROPERTY_FEATURE_STYLE))
        {
            style = takFeatureProvider.createStyle(state);
        }
        if (MathUtils.hasBits(updatePropertyMask, FeatureDataStore4.PROPERTY_FEATURE_ATTRIBUTES))
        {
            attributeSet = takFeatureProvider.createAttributes(state);
        }
        if (MathUtils.hasBits(updatePropertyMask, FeatureDataStore4.PROPERTY_FEATURE_ALTITUDE_MODE))
        {
            altitudeMode = takFeatureProvider.createAltitudeModes(state);
        }
        if (MathUtils.hasBits(updatePropertyMask, FeatureDataStore4.PROPERTY_FEATURE_EXTRUDE))
        {
            extrude = takFeatureProvider.createExtrusions(state);
        }
        FeatureResult[] featureComponents = new FeatureResult[featureCount];
        for (int i = 0; i < featureComponents.length; i++)
        {
            int featureUpdatePropertyMask = updatePropertyMask;
            if (MathUtils.hasBits(updatePropertyMask, FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY) && geometry[i] == null) featureUpdatePropertyMask ^= FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY;
            if (MathUtils.hasBits(updatePropertyMask, FeatureDataStore4.PROPERTY_FEATURE_STYLE) && style[i] == null) featureUpdatePropertyMask ^= FeatureDataStore4.PROPERTY_FEATURE_STYLE;

            featureComponents[i] = new FeatureResult(featureUpdatePropertyMask, name[i], geometry[i], style[i], attributeSet[i], altitudeMode[i], extrude[i], objectKeys[i], groupingNames[i]);
        }
        boolean isVisible = takFeatureProvider.isVisible(state);
        return new FeatureSetResult(featureComponents, isVisible);
    }

    /**
     * A registration object to hold the elements of a feature set.
     * @param featureResults an array of feature results for the feature set
     * @param visible true if the feature set is visible
     */
    public record FeatureSetResult(FeatureResult[] featureResults, boolean visible)
    {
    }

    /**
     * A result object to hold the elements of a feature.
     * @param updatePropertyMask the mask of valid features contained in this object
     * @param name the feature name
     * @param geometry the feature geometry
     * @param style the feature style
     * @param attributes the feature attributes
     * @param altitudeMode the feature altitude mode
     * @param extrude the feature extrusion
     * @param objectId the feature object id
     * @param groupingName the feature grouping name
     */
    public record FeatureResult(int updatePropertyMask, String name, Geometry geometry, Style style, AttributeSet attributes, Feature.AltitudeMode altitudeMode, double extrude, Object objectId, String groupingName)
    {
    }

    /**
     * A state object to hold the elements required to produce a set of features by the feature factory.
     * @param featureProvider the feature provider
     * @param state the state object
     * @param <T> the object type served by the feature provider
     * @param <StateObject> the state object type produced by the feature provider
     * @since 5.1
     */
    public record FeatureFactoryState<T, StateObject>(@Nullable IFeatureProvider<T, StateObject> featureProvider, @Nullable StateObject state)
    {
    }

    private static class RawUrlBitmapLoaderSpi extends GLBitmapLoader.AbstractInputStreamLoaderSpi
    {
        protected RawUrlBitmapLoaderSpi()
        {
            super(0);
        }

        @Override
        protected InputStream openStream(String uriString) throws Exception
        {
            return new URL(uriString).openStream();
        }
    }

    /**
     * Handle {@code shell:} protocol URIs and convert them to an input stream of bytes.  The appropriate system
     * icon is converted to a PNG, and then those bytes are returned.
     *
     * @see FileUtils#getSystemIconImage
     * @since 3.5
     */
    private static class ShellResourceBitmapLoaderSpi extends GLBitmapLoader.AbstractInputStreamLoaderSpi
    {
        protected ShellResourceBitmapLoaderSpi()
        {
            super(0);
        }

        @Override
        protected InputStream openStream(String uriString) throws Exception
        {
            final File localFile = new File(Uri.parse(uriString).getPath());
            final BufferedImage systemIconImage = FileUtils.getSystemIconImage(localFile);

            if (systemIconImage != null)
            {
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(systemIconImage, "png", bytes);
                return new ByteArrayInputStream(bytes.toByteArray());
            } else
            {
                logger.error("Failed to get system icon for {}", localFile);
                return null;
            }
        }
    }
}
