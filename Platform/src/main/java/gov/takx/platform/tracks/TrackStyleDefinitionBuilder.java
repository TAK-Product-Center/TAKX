package gov.takx.platform.tracks;

import gov.takx.api.tracks.ISize;
import gov.takx.api.tracks.ITrackStyle;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.dataservices.persist.TrackStyleDefinition;
import gov.takx.platform.schema.trackstyle.GlyphType;
import gov.takx.platform.schema.trackstyle.GroupabilityType;
import gov.takx.platform.schema.trackstyle.ScopeType;
import gov.takx.platform.schema.trackstyle.TrackShapeType;
import gov.takx.platform.schema.trackstyle.TrackStyleDefinitionType;
import gov.takx.platform.schema.trackstyle.TrackStyleDefinitionsCollection;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder pattern for describing and building an {@link ITrackStyleDefinition}. Track style
 * definitions are by design immutable; to change an existing track style definition, use one of the cloning
 * constructors, {@link #TrackStyleDefinitionBuilder(ITrackStyleDefinition, String)} or
 * {@link #TrackStyleDefinitionBuilder(String, String, String, ITrackStyleDefinition)}.
 * <p/>
 * The uniqueKey assigned to a track style definition is truly arbitrary. Developers are encouraged to use a UUID
 * for the unique key.
 * <p/>
 * <b>Note:</b> For complete explanations of the various track style attributes, please refer to the JavaDocs found in
 * {@link ITrackStyleDefinition}.
 *
 * @since 1.12
 */
public class TrackStyleDefinitionBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private boolean reachBackToParent;

    private String family;
    private String type;
    private String uniqueKey;

    private boolean isUserDefined = false;

    private boolean showHeading = false;
    private String displayName = "New Track Style Definition";
    private boolean visible = true;
    private Color trackShapeConstantColor = null; // Null so default is to use the map object's default color
    private float trackShapeConstantOpacity = 1.0f;
    private int trackShapeConstantSize = ITrackStyle.NORMAL;
    private boolean linkTrackShapeToHeading;
    private String glyphResource = null;
    private VerticalAlignment glyphAlignment = VerticalAlignment.Top;
    private double glyphVerticalOffset = 0.0;
    private boolean linkGlyphToHeading;
    private boolean linkGlyphOpacityToTrackOpacity = true;
    private boolean linkGlyphSizeToTrackSize;
    private int glyphConstantSize = ITrackStyle.NORMAL;
    private boolean snapToSurface = true;
    private boolean fadeOverTime;
    private long timeThreshold;
    private TrackShape trackShape = TrackShape.Sphere;
    private float glyphConstantOpacity = 1.0f;
    private double elevationThreshold = 0.0;
    private Groupability groupability = Groupability.SimilarOnly;
    private BucketMode bucketMode = BucketMode.None;

    private boolean hasDefinedShowHeading = false;
    private boolean hasDefinedFadedOverTime = false;
    private boolean hasDefinedTimeThreshold = false;
    private boolean hasDefinedLinkGlyphSizeToTrackSize = false;
    private boolean hasDefinedGlyphConstantSize = false;
    private boolean hasDefinedLinkGlyphOpacityToTrackOpacity = false;
    private boolean hasDefinedLinkGlyphToHeading = false;
    private boolean hasDefinedSnapToSurface = false;
    private boolean hasDefinedTrackShape = false;
    private boolean hasDefinedLinkTrackShapeToHeading = false;
    private boolean hasDefinedTrackShapeConstantColor = false;
    private boolean hasDefinedTrackShapeConstantOpacity = false;
    private boolean hasDefinedTrackShapeConstantSize = false;
    private boolean hasDefinedGlyphResource = false;
    private boolean hasDefinedGlyphConstantOpacity = false;
    private boolean hasDefinedGlyphVerticalAlignment = false;
    private boolean hasDefinedGlyphVerticalOffset = false;
    private boolean hasDefinedElevationThreshold = false;
    private boolean hasDefinedGroupability = false;
    private boolean hasDefinedBucketMode = false;
    private boolean hasDefinedVisible = false;

    /**
     * Create a map of track style definitions built from an XML document.  Each definition is in a pre-persist state,
     * allowing the additional definition(s) to occur before the styles are persisted by the core.
     *
     * @param xmlTrackStyleDefn URL that points to an XML document that can be validated against the
     *                          http://www.doe.gov/2011/RaptorTrackStyleSchema schema.
     * @return Map of pre-persist track style definitions contained within the XML document.
     * @throws IllegalArgumentException     if URL is {@code null}
     * @throws IOException if the xmlTrackStyleDefn URL cannot be accessed
     * @throws JAXBException if the XML document pointed to by xmlTrackStyleDefn is invalid and cannot be
     *                                      bound to Java objects
     */
    public static Map<String, ITrackStyleDefinition> buildFromXML(URL xmlTrackStyleDefn)
            throws IOException, JAXBException
    {
        if (xmlTrackStyleDefn == null) throw new IllegalArgumentException("Parameter xmlStructDefn must not be null");

        Map<String, ITrackStyleDefinition> defnMap = new HashMap<>();

        InputStream is = xmlTrackStyleDefn.openStream();
        TrackStyleDefinitionsCollection styleCollection = processXML(is);

        for (TrackStyleDefinitionType tsd : styleCollection.getTrackStyleDefinition())
        {
            boolean reachBack = tsd.getScope() == ScopeType.USER;
            TrackStyleDefinitionBuilder builder = new TrackStyleDefinitionBuilder(tsd.getFamily(),
                    tsd.getType(),
                    tsd.getUniqueKey(),
                    reachBack);

            // In the case of XML, a user defined scope is automatically user-defined and supports reach-back
            builder.setUserDefined(reachBack);
            builder.setDisplayName(tsd.getDisplayName());

            if (tsd.isVisible() != null) builder.setVisible(tsd.isVisible());

            TrackShapeType tst = tsd.getTrackShape();

            if (null != tst)
            {
                if (null != tst.getColor()) builder.setTrackShapeConstantColor(new Color(tst.getColor(), true));
                if (null != tst.getOpacity()) builder.setTrackShapeConstantOpacity(tst.getOpacity());
                if (null != tst.isLinkToHeading()) builder.setLinkTrackShapeToHeading(tst.isLinkToHeading());
                if (null != tst.getShape())
                {
                    switch (tst.getShape())
                    {
                        case SPHERE -> builder.setTrackShape(TrackShape.Sphere);
                        case CYLINDER -> builder.setTrackShape(TrackShape.Cylinder);
                        case CUBE -> builder.setTrackShape(TrackShape.Cube);
                        case NONE -> builder.setTrackShape(TrackShape.None);
                        case CONE -> builder.setTrackShape(TrackShape.Cone);
                    }
                }
                if (null != tst.getSize())
                {
                    switch (tst.getSize())
                    {
                        case MINIMUM -> builder.setTrackShapeConstantSize(ISize.MINIMUM);
                        case SMALL -> builder.setTrackShapeConstantSize(ISize.SMALL);
                        case NORMAL -> builder.setTrackShapeConstantSize(ISize.NORMAL);
                        case LARGE -> builder.setTrackShapeConstantSize(ISize.LARGE);
                        case MAXIMUM -> builder.setTrackShapeConstantSize(ISize.MAXIMUM);
                    }
                }
            }

            GlyphType gt = tsd.getGlyph();

            if (null != gt)
            {
                if (null != gt.getGlyphResource()) builder.setGlyphResource(gt.getGlyphResource());
                if (null != gt.getOpacity()) builder.setGlyphConstantOpacity(gt.getOpacity());
                if (null != gt.getSize())
                {
                    switch (gt.getSize())
                    {
                        case MINIMUM -> builder.setGlyphConstantSize(ISize.MINIMUM);
                        case SMALL -> builder.setGlyphConstantSize(ISize.SMALL);
                        case NORMAL -> builder.setGlyphConstantSize(ISize.NORMAL);
                        case LARGE -> builder.setGlyphConstantSize(ISize.LARGE);
                        case MAXIMUM -> builder.setGlyphConstantSize(ISize.MAXIMUM);
                    }
                }
                if (null != gt.getVerticalAlignment())
                {
                    switch (gt.getVerticalAlignment())
                    {
                        case TOP -> builder.setGlyphVerticalAlignment(VerticalAlignment.Top);
                        case MIDDLE -> builder.setGlyphVerticalAlignment(VerticalAlignment.Middle);
                        case BOTTOM -> builder.setGlyphVerticalAlignment(VerticalAlignment.Bottom);
                    }
                }

                if (null != gt.getVerticalOffset()) builder.setGlyphVerticalOffset(gt.getVerticalOffset());
                if (null != gt.isLinkToHeading()) builder.setLinkGlyphToHeading(gt.isLinkToHeading());
                if (null != gt.isLinkToTrackOpacity())
                {
                    builder.setLinkGlyphOpacityToTrackOpacity(gt.isLinkToTrackOpacity());
                }
                if (null != gt.isLinkToTrackSize()) builder.setLinkGlyphSizeToTrackSize(gt.isLinkToTrackSize());
            }

            if (null != tsd.isSnapToSurface()) builder.setSnapToSurface(tsd.isSnapToSurface());
            if (null != tsd.isFadedOverTime()) builder.setFadedOverTime(tsd.isFadedOverTime());
            if (null != tsd.getTimeThreshold()) builder.setTimeThreshold(tsd.getTimeThreshold());
            if (null != tsd.getElevationThreshold()) builder.setElevationThreshold(tsd.getElevationThreshold());
            final GroupabilityType tsdGroupability = tsd.getGroupability();
            if (null != tsdGroupability)
            {
                switch (tsdGroupability)
                {
                    case SIMILAR_ONLY -> builder.setGroupability(Groupability.SimilarOnly);
                    case UNGROUPABLE -> builder.setGroupability(Groupability.Ungroupable);
                    default -> logger.error("Unsupported Groupability: {}", tsdGroupability);
                }
            }

            if (null != tsd.getBucketMode())
            {
                switch (tsd.getBucketMode())
                {
                    case NONE -> builder.setBucketMode(BucketMode.None);
                    case HOUR_OF_DAY -> builder.setBucketMode(BucketMode.HourOfDay);
                    case DAY_OF_WEEK -> builder.setBucketMode(BucketMode.DayOfWeek);
                    case MONTH_OF_YEAR -> builder.setBucketMode(BucketMode.MonthOfYear);
                    case YEAR -> logger.warn("Skipping unsupported bucket mode: {}", tsd.getBucketMode());
                }
            }
            if (null != tsd.isShowHeading()) builder.setShowHeading(tsd.isShowHeading());

            ITrackStyleDefinition defn = builder.build();

            if (logger.isDebugEnabled())
            {
                logger.debug("Successfully parsed track style definition {}", defn.getUniqueKey());
            }

            defnMap.put(defn.getUniqueKey(), defn);
        }

        return defnMap;
    }

    private static TrackStyleDefinitionsCollection processXML(InputStream stream) throws JAXBException
    {
        JAXBContext context = JAXBContext.newInstance(TrackStyleDefinitionsCollection.class.getPackage().getName());
        Unmarshaller unmarshaller = context.createUnmarshaller();

        JAXBElement<TrackStyleDefinitionsCollection> root = unmarshaller.unmarshal(new StreamSource(stream), TrackStyleDefinitionsCollection.class);
        return root.getValue();
    }

    /**
     * Constructs a new track style definition using strictly default values under the given context; there is no
     * implicit reach back to a parent track style definition
     *
     * @param family    Family
     * @param type      Type
     * @param uniqueKey Unique key, typically a UUID
     */
    public TrackStyleDefinitionBuilder(@Nonnull String family, @Nonnull String type, @Nonnull String uniqueKey)
    {
        this(family, type, uniqueKey, false);
    }

    /**
     * Constructs a new track style definition using strictly default values under the given context; there is no
     * implicit reach back to a parent track style definition.  A new unique key is generated using UUIDUtils.
     *
     * @param family            Family
     * @param type              Type
     * @param reachBackToParent true, if the default value for all properties should implicitly reach back to the parent
     *                          track style definition, or false if explicit default values should be assigned
     */
    public TrackStyleDefinitionBuilder(@Nonnull String family, @Nonnull String type, boolean reachBackToParent)
    {
        this(family, type, UUIDUtils.generateType1UUID().toString(), reachBackToParent);
    }

    /**
     * Constructs a new track style definition using strictly default values under the given context; there is no
     * implicit reach back to a parent track style definition
     *
     * @param family            Family
     * @param type              Type
     * @param uniqueKey         Unique key, typically a UUID
     * @param reachBackToParent true, if the default value for all properties should implicitly reach back to the parent
     *                          track style definition, or false if explicit default values should be assigned
     */
    public TrackStyleDefinitionBuilder(@Nonnull String family, @Nonnull String type, @Nonnull String uniqueKey, boolean reachBackToParent)
    {
        this.family = family;
        this.type = type;
        this.uniqueKey = uniqueKey;

        this.reachBackToParent = reachBackToParent;

        if (!reachBackToParent)
        {
            hasDefinedShowHeading = true;
            hasDefinedFadedOverTime = true;
            hasDefinedTimeThreshold = true;
            hasDefinedLinkGlyphSizeToTrackSize = true;
            hasDefinedGlyphConstantSize = true;
            hasDefinedLinkGlyphOpacityToTrackOpacity = true;
            hasDefinedLinkGlyphToHeading = true;
            hasDefinedSnapToSurface = true;
            hasDefinedTrackShape = true;
            hasDefinedLinkTrackShapeToHeading = true;
            hasDefinedTrackShapeConstantColor = true;
            hasDefinedTrackShapeConstantOpacity = true;
            hasDefinedTrackShapeConstantSize = true;
            hasDefinedGlyphResource = true;
            hasDefinedGlyphConstantOpacity = true;
            hasDefinedGlyphVerticalAlignment = true;
            hasDefinedGlyphVerticalOffset = true;
            hasDefinedElevationThreshold = true;
            hasDefinedGroupability = true;
            hasDefinedBucketMode = true;
            hasDefinedVisible = true;
        }
    }

    /**
     * Clones an existing track style definition, maintaining the same family and type but replacing the uniqueKey with
     * a new value.  Any implicit reach back to a parent track style definition for an attribute value is lost in the
     * clone.  Even if the source track style definition used reach back, values at the time of creation would be used
     * to construct the cloned track style definition.
     *
     * @param trackStyleDefinition Model track style definition to clone attribute definitions from
     * @param uniqueKey            New unique key to assign, typically a UUID
     */
    public TrackStyleDefinitionBuilder(ITrackStyleDefinition trackStyleDefinition, String uniqueKey)
    {
        this(trackStyleDefinition.getFamily(), trackStyleDefinition.getType(), uniqueKey, trackStyleDefinition);
    }

    /**
     * Clones an existing track style definition, fully replacing the model's family, type, and uniqueKey with the
     * new values specified; there is no implicit reach back to a parent track style definition. Even if the
     * source track style definition used reach back, values at the time of creation would be used to construct the
     * cloned track style definition.
     *
     * @param family               New family to assign
     * @param type                 New type to assign
     * @param uniqueKey            New unique key to assign, typically a UUID
     * @param trackStyleDefinition Model track style definition to clone attribute definitions from
     */
    public TrackStyleDefinitionBuilder(@Nonnull String family, @Nonnull String type, @Nonnull String uniqueKey,
                                       @Nonnull ITrackStyleDefinition trackStyleDefinition)
    {
        this(family, type, uniqueKey);

        cloneStyleDefinition(trackStyleDefinition);
    }

    private void cloneStyleDefinition(@Nonnull ITrackStyleDefinition trackStyleDefinition)
    {
        displayName = trackStyleDefinition.getDisplayName();
        visible = trackStyleDefinition.isVisible();

        trackShape = trackStyleDefinition.getTrackShape();
        trackShapeConstantColor = trackStyleDefinition.getTrackShapeConstantColor();
        trackShapeConstantOpacity = trackStyleDefinition.getTrackShapeConstantOpacity();
        trackShapeConstantSize = trackStyleDefinition.getTrackShapeConstantSize();
        linkTrackShapeToHeading = trackStyleDefinition.isTrackShapeLinkedToHeading();

        glyphResource = trackStyleDefinition.getGlyphResource();
        glyphAlignment = trackStyleDefinition.getGlyphVerticalAlignment();
        glyphVerticalOffset = trackStyleDefinition.getGlyphVerticalOffset();
        linkGlyphToHeading = trackStyleDefinition.isLinkGlyphToHeading();
        linkGlyphOpacityToTrackOpacity = trackStyleDefinition.isGlyphLinkedToTrackOpacity();
        linkGlyphSizeToTrackSize = trackStyleDefinition.isGlyphLinkedToTrackSize();
        glyphConstantSize = trackStyleDefinition.getGlyphConstantSize();
        glyphConstantOpacity = trackStyleDefinition.getGlyphConstantOpacity();

        snapToSurface = trackStyleDefinition.isSnappedToSurface();
        fadeOverTime = trackStyleDefinition.isFadedOverTime();
        elevationThreshold = trackStyleDefinition.getElevationThreshold();
        bucketMode = trackStyleDefinition.getBucketMode();
        timeThreshold = trackStyleDefinition.getTimeThreshold();
        groupability = trackStyleDefinition.getGroupability();
        showHeading = trackStyleDefinition.isShowingHeading();
    }

    public TrackStyleDefinitionBuilder setUserDefined(boolean isUserDefined)
    {
        this.isUserDefined = isUserDefined;
        return this;
    }

    public boolean isUserDefined()
    {
        return isUserDefined;
    }

    public TrackStyleDefinitionBuilder setDisplayName(@Nonnull String displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public TrackStyleDefinitionBuilder setVisible(boolean visible)
    {
        hasDefinedVisible = true;
        this.visible = visible;
        return this;
    }

    public TrackStyleDefinitionBuilder setTrackShapeConstantColor(Color constantColor)
    {
        hasDefinedTrackShapeConstantColor = true;
        trackShapeConstantColor = constantColor;
        return this;
    }

    public TrackStyleDefinitionBuilder setShowHeading(boolean showHeading)
    {
        hasDefinedShowHeading = true;
        this.showHeading = showHeading;
        return this;
    }

    public TrackStyleDefinitionBuilder setElevationThreshold(int elevationThreshold)
    {
        if (elevationThreshold < 0)
        {
            throw new IllegalArgumentException("Parameter 'elevationThreshold' must be >= 0");
        }
        this.elevationThreshold = elevationThreshold;
        hasDefinedElevationThreshold = true;
        return this;
    }

    public TrackStyleDefinitionBuilder setTrackShapeConstantSize(int trackShapeConstantSize)
    {
        if (trackShapeConstantSize < 1 || trackShapeConstantSize > 10)
        {
            throw new IllegalArgumentException("trackShapeConstantSize must be in the range 1 through 10, inclusive");
        }

        hasDefinedTrackShapeConstantSize = true;
        this.trackShapeConstantSize = trackShapeConstantSize;
        return this;
    }

    public TrackStyleDefinitionBuilder setTrackShapeConstantOpacity(float constantOpacity)
    {
        if (constantOpacity < 0.0f || constantOpacity > 1.0f)
        {
            throw new IllegalArgumentException("Opacity must be in the range 0.0 through 1.0, inclusive");
        }

        hasDefinedTrackShapeConstantOpacity = true;
        trackShapeConstantOpacity = constantOpacity;
        return this;
    }

    public TrackStyleDefinitionBuilder setTrackShape(TrackShape openGLShape)
    {
        hasDefinedTrackShape = true;
        trackShape = openGLShape;
        return this;
    }

    public TrackStyleDefinitionBuilder setLinkTrackShapeToHeading(boolean linkTrackShapeToHeading)
    {
        hasDefinedLinkTrackShapeToHeading = true;
        this.linkTrackShapeToHeading = linkTrackShapeToHeading;
        return this;
    }

    public TrackStyleDefinitionBuilder setGlyphResource(@Nonnull String glyphResource)
    {
        this.glyphResource = glyphResource;
        hasDefinedGlyphResource = true;
        return this;
    }

    public TrackStyleDefinitionBuilder setGlyphVerticalAlignment(VerticalAlignment glyphVerticalAlignment)
    {
        hasDefinedGlyphVerticalAlignment = true;
        glyphAlignment = glyphVerticalAlignment;
        return this;
    }

    public TrackStyleDefinitionBuilder setGlyphVerticalOffset(double glyphVerticalOffset)
    {
        hasDefinedGlyphVerticalOffset = true;
        this.glyphVerticalOffset = glyphVerticalOffset;
        return this;
    }

    public TrackStyleDefinitionBuilder setGlyphConstantOpacity(float glyphConstantOpacity)
    {
        if (glyphConstantOpacity < 0.0f || glyphConstantOpacity > 1.0f)
        {
            throw new IllegalArgumentException("Glyph opacity must be in the range 0.0 through 1.0, inclusive");
        }

        hasDefinedGlyphConstantOpacity = true;
        this.glyphConstantOpacity = glyphConstantOpacity;
        return this;
    }

    public TrackStyleDefinitionBuilder setLinkGlyphToHeading(boolean linkGlyphToHeading)
    {
        hasDefinedLinkGlyphToHeading = true;
        this.linkGlyphToHeading = linkGlyphToHeading;
        return this;
    }

    public TrackStyleDefinitionBuilder setLinkGlyphOpacityToTrackOpacity(boolean linkGlyphOpacityToTrackOpacity)
    {
        hasDefinedLinkGlyphOpacityToTrackOpacity = true;
        this.linkGlyphOpacityToTrackOpacity = linkGlyphOpacityToTrackOpacity;
        return this;
    }

    public TrackStyleDefinitionBuilder setLinkGlyphSizeToTrackSize(boolean linkGlyphSizeToTrackSize)
    {
        hasDefinedLinkGlyphSizeToTrackSize = true;
        this.linkGlyphSizeToTrackSize = linkGlyphSizeToTrackSize;
        return this;
    }

    public TrackStyleDefinitionBuilder setGlyphConstantSize(int glyphConstantSize)
    {
        if (glyphConstantSize < 1 || glyphConstantSize > 10)
        {
            throw new IllegalArgumentException("Size must be in the range 1 through 10, inclusive");
        }

        hasDefinedGlyphConstantSize = true;
        this.glyphConstantSize = glyphConstantSize;
        return this;
    }

    public TrackStyleDefinitionBuilder setSnapToSurface(boolean snapToSurface)
    {
        hasDefinedSnapToSurface = true;
        this.snapToSurface = snapToSurface;
        return this;
    }

    public TrackStyleDefinitionBuilder setFadedOverTime(boolean fadedOverTime)
    {
        hasDefinedFadedOverTime = true;
        fadeOverTime = fadedOverTime;
        return this;
    }

    public TrackStyleDefinitionBuilder setTimeThreshold(long fadeOverTimeDuration)
    {
        if (fadeOverTimeDuration < 0)
        {
            throw new IllegalArgumentException("Parameter 'fadeOverTimeDuration' must be >= 0");
        }

        hasDefinedTimeThreshold = true;
        timeThreshold = fadeOverTimeDuration;
        return this;
    }

    private TrackStyleDefinitionBuilder setBucketMode(BucketMode bucketMode)
    {
        hasDefinedBucketMode = true;
        this.bucketMode = bucketMode;
        return this;
    }

    public TrackStyleDefinitionBuilder setGroupability(Groupability groupability)
    {
        hasDefinedGroupability = true;
        this.groupability = groupability;
        return this;
    }

    public ITrackStyleDefinition build() throws SmartTrackBuilderException
    {
        if (null == displayName)
        {
            String err = "A display name must be set on a track style definition";
            logger.error(err);
            throw new SmartTrackBuilderException(err);
        }

        TrackStyleDefinition tsd = new TrackStyleDefinition(getFamily(), getType(), getUniqueKey(), isUserDefined());

        tsd.setDisplayName(displayName);

        if (!reachBackToParent)
        {
            // Need to assign default values to ensure that there is NO REACH BACK

            tsd.setShowHeading(!hasDefinedShowHeading || showHeading);
            tsd.setTrackShapeConstantColor((hasDefinedTrackShapeConstantColor ? trackShapeConstantColor : Color.WHITE));
            tsd.setTrackShape((hasDefinedTrackShape ? trackShape : TrackShape.Sphere));
            tsd.setTrackShapeConstantOpacity(hasDefinedTrackShapeConstantOpacity ? trackShapeConstantOpacity : 1.0f);
            tsd.setTrackShapeConstantSize(hasDefinedTrackShapeConstantSize ? trackShapeConstantSize : ISize.NORMAL);
            tsd.setBucketMode(hasDefinedBucketMode ? bucketMode : BucketMode.None);
            tsd.setGroupability(hasDefinedGroupability ? groupability : Groupability.SimilarOnly);
            tsd.setSnapToSurface(hasDefinedSnapToSurface && snapToSurface);
            tsd.setTimeThreshold(hasDefinedTimeThreshold ? timeThreshold : 0);
            tsd.setVisible(!hasDefinedVisible || visible);
            tsd.setElevationThreshold(hasDefinedElevationThreshold ? elevationThreshold : 0);
            tsd.setFadedOverTime(hasDefinedFadedOverTime && fadeOverTime);
            tsd.setGlyphConstantSize(hasDefinedGlyphConstantSize ? glyphConstantSize : ISize.NORMAL);
            tsd.setGlyphConstantOpacity(hasDefinedGlyphConstantOpacity ? glyphConstantOpacity : 1.0f);
            tsd.setGlyphResource(hasDefinedGlyphResource ? glyphResource : null);
            tsd.setGlyphVerticalAlignment(hasDefinedGlyphVerticalAlignment ? glyphAlignment : VerticalAlignment.DEFAULT);
            tsd.setGlyphVerticalOffset(hasDefinedGlyphVerticalOffset ? glyphVerticalOffset : 0.0);
            tsd.setLinkGlyphOpacityToTrackOpacity(hasDefinedLinkGlyphOpacityToTrackOpacity && linkGlyphOpacityToTrackOpacity);
            tsd.setLinkGlyphSizeToTrackSize(hasDefinedLinkGlyphSizeToTrackSize && linkGlyphSizeToTrackSize);
            tsd.setLinkGlyphToHeading(hasDefinedLinkGlyphToHeading && linkGlyphToHeading);
            tsd.setLinkTrackShapeToHeading(hasDefinedLinkTrackShapeToHeading && linkTrackShapeToHeading);
        } else
        {
            // Need to leave all properties null EXCEPT for those properties explicitly set on the builder

            if (hasDefinedShowHeading) tsd.setShowHeading(showHeading);
            if (hasDefinedBucketMode) tsd.setBucketMode(bucketMode);
            if (hasDefinedTrackShapeConstantColor) tsd.setTrackShapeConstantColor(trackShapeConstantColor);
            if (hasDefinedTrackShape) tsd.setTrackShape(trackShape);
            if (hasDefinedTrackShapeConstantOpacity) tsd.setTrackShapeConstantOpacity(trackShapeConstantOpacity);
            if (hasDefinedTrackShapeConstantSize) tsd.setTrackShapeConstantSize(trackShapeConstantSize);
            if (hasDefinedGroupability) tsd.setGroupability(groupability);
            if (hasDefinedSnapToSurface) tsd.setSnapToSurface(snapToSurface);
            if (hasDefinedTimeThreshold) tsd.setTimeThreshold(timeThreshold);
            if (hasDefinedVisible) tsd.setVisible(visible);
            if (hasDefinedElevationThreshold) tsd.setElevationThreshold(elevationThreshold);
            if (hasDefinedFadedOverTime) tsd.setFadedOverTime(fadeOverTime);
            if (hasDefinedGlyphConstantSize) tsd.setGlyphConstantSize(glyphConstantSize);
            if (hasDefinedGlyphConstantOpacity) tsd.setGlyphConstantOpacity(glyphConstantOpacity);
            if (hasDefinedGlyphResource) tsd.setGlyphResource(glyphResource);
            if (hasDefinedGlyphVerticalAlignment) tsd.setGlyphVerticalAlignment(glyphAlignment);
            if (hasDefinedGlyphVerticalOffset) tsd.setGlyphVerticalOffset(glyphVerticalOffset);
            if (hasDefinedLinkGlyphSizeToTrackSize) tsd.setLinkGlyphSizeToTrackSize(linkGlyphSizeToTrackSize);
            if (hasDefinedLinkGlyphToHeading) tsd.setLinkGlyphToHeading(linkGlyphToHeading);
            if (hasDefinedLinkTrackShapeToHeading) tsd.setLinkTrackShapeToHeading(linkTrackShapeToHeading);
            if (hasDefinedLinkGlyphOpacityToTrackOpacity)
            {
                tsd.setLinkGlyphOpacityToTrackOpacity(linkGlyphOpacityToTrackOpacity);
            }
        }

        return tsd;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedFadedOverTime()
    {
        return hasDefinedFadedOverTime;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedShowHeading()
    {
        return hasDefinedShowHeading;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedTimeThreshold()
    {
        return hasDefinedTimeThreshold;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedLinkGlyphSizeToTrackSize()
    {
        return hasDefinedLinkGlyphSizeToTrackSize;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedGlyphConstantSize()
    {
        return hasDefinedGlyphConstantSize;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedLinkGlyphOpacityToTrackOpacity()
    {
        return hasDefinedLinkGlyphOpacityToTrackOpacity;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedLinkGlyphToHeading()
    {
        return hasDefinedLinkGlyphToHeading;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedSnapToSurface()
    {
        return hasDefinedSnapToSurface;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedTrackShape()
    {
        return hasDefinedTrackShape;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedLinkTrackShapeToHeading()
    {
        return hasDefinedLinkTrackShapeToHeading;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedTrackShapeConstantColor()
    {
        return hasDefinedTrackShapeConstantColor;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedTrackShapeConstantOpacity()
    {
        return hasDefinedTrackShapeConstantOpacity;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedTrackShapeConstantSize()
    {
        return hasDefinedTrackShapeConstantSize;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedGlyphResource()
    {
        return hasDefinedGlyphResource;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedGlyphConstantOpacity()
    {
        return hasDefinedGlyphConstantOpacity;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedGlyphAlignment()
    {
        return hasDefinedGlyphVerticalAlignment;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedGlyphVerticalOffset()
    {
        return hasDefinedGlyphVerticalOffset;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedVisible()
    {
        return hasDefinedVisible;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedElevationThreshold()
    {
        return hasDefinedElevationThreshold;
    }

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    public boolean hasDefinedGroupability()
    {
        return hasDefinedGroupability;
    }

    public String getFamily()
    {
        return family;
    }

    public String getType()
    {
        return type;
    }

    public String getUniqueKey()
    {
        return uniqueKey;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public boolean isShowHeading()
    {
        return showHeading;
    }

    public Color getTrackShapeConstantColor()
    {
        return trackShapeConstantColor;
    }

    public float getTrackShapeConstantOpacity()
    {
        return trackShapeConstantOpacity;
    }

    public int getTrackShapeConstantSize()
    {
        return trackShapeConstantSize;
    }

    public boolean isLinkTrackShapeToHeading()
    {
        return linkTrackShapeToHeading;
    }

    public String getGlyphResource()
    {
        return glyphResource;
    }

    public VerticalAlignment getGlyphVerticalAlignment()
    {
        return glyphAlignment;
    }

    public double getGlyphVerticalOffset()
    {
        return glyphVerticalOffset;
    }

    public boolean isLinkGlyphToHeading()
    {
        return linkGlyphToHeading;
    }

    public boolean isLinkGlyphOpacityToTrackOpacity()
    {
        return linkGlyphOpacityToTrackOpacity;
    }

    public boolean isLinkGlyphSizeToTrackSize()
    {
        return linkGlyphSizeToTrackSize;
    }

    public int getGlyphConstantSize()
    {
        return glyphConstantSize;
    }

    public boolean isReachingBackToParent()
    {
        return reachBackToParent;
    }

    public boolean isSnapToSurface()
    {
        return snapToSurface;
    }

    public boolean isFadeOverTime()
    {
        return fadeOverTime;
    }

    public long getTimeThreshold()
    {
        return timeThreshold;
    }

    public TrackShape getTrackShape()
    {
        return trackShape;
    }

    public float getGlyphConstantOpacity()
    {
        return glyphConstantOpacity;
    }

    public double getElevationThreshold()
    {
        return elevationThreshold;
    }

    public Groupability getGroupability()
    {
        return groupability;
    }

    public BucketMode getBucketMode()
    {
        return bucketMode;
    }

    public boolean hasDefinedBucketMode()
    {
        return hasDefinedBucketMode;
    }

    // Quiet Weld warnings
    @SuppressWarnings("UnusedDeclaration")
    private TrackStyleDefinitionBuilder()
    {
    }
}
