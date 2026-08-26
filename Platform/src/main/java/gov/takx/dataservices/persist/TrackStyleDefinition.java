package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.tracks.ISize;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.platform.tracks.BucketMode;
import gov.takx.platform.tracks.Groupability;
import gov.takx.platform.tracks.TrackShape;
import gov.takx.platform.tracks.VerticalAlignment;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

/**
 * @since 1.12
 */
@Internal
@Entity
public class TrackStyleDefinition implements ITrackStyleDefinition
{
    @SuppressWarnings("unused") // set by hibernate
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String family;
    private String type;
    @Column(unique = true)
    private String uniqueKey;

    private String displayName;
    private Boolean visible;

    private Integer trackShapeConstantColor;

    private Float trackShapeConstantOpacity;
    private Integer trackShapeConstantSize;
    private Boolean linkTrackShapeToHeading;
    private String glyphResource;
    private VerticalAlignment glyphVerticalAlignment;
    private Double glyphVerticalOffset;
    private Boolean linkGlyphToHeading;
    private Boolean linkGlyphOpacityToTrackOpacity;
    private Boolean linkGlyphSizeToTrackSize;
    private Integer glyphConstantSize;
    private Boolean snapToSurface;
    private Boolean fadedOverTimeFlag;
    private Long timeThreshold;
    private TrackShape trackShape;
    private Float glyphConstantOpacity;
    private Double elevationThreshold;
    private Groupability groupability;
    private BucketMode bucketMode;
    private Boolean isUserDefined;
    private Boolean showHeading;

    @Transient
    protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public TrackStyleDefinition()
    {
    }

    public TrackStyleDefinition(String family, String type, String uniqueKey, boolean userDefined)
    {
        this.family = family;
        this.type = type;
        this.uniqueKey = uniqueKey;
        isUserDefined = userDefined;
    }

    @Override
    public boolean isFadedOverTime()
    {
        return hasDefinedFadedOverTime() ? fadedOverTimeFlag : false;
    }

    @Override
    public long getTimeThreshold()
    {
        return hasDefinedTimeThreshold() ? timeThreshold : 0;
    }

    @Override
    public void setTimeThreshold(long threshold)
    {
        if (threshold < 0)
        {
            throw new IllegalArgumentException("Parameter 'threshold' must not be less than zero");
        }

        Long oldValue = timeThreshold;
        timeThreshold = threshold;
        propertyChangeSupport.firePropertyChange(TIME_THRESHOLD, oldValue, timeThreshold);
    }

    @Override
    public boolean hasTimeThreshold()
    {
        return getTimeThreshold() > 0;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public void setDisplayName(@Nonnull String displayName)
    {
        String oldValue = this.displayName;
        this.displayName = displayName;
        propertyChangeSupport.firePropertyChange(DISPLAY_NAME, oldValue, this.displayName);
    }

    @Override
    public boolean isVisible()
    {
        return hasDefinedVisible() ? visible : true;
    }

    @Override
    public VerticalAlignment getGlyphVerticalAlignment()
    {
        return hasDefinedGlyphVerticalAlignment() ? glyphVerticalAlignment : VerticalAlignment.DEFAULT;
    }

    @Override
    public double getGlyphVerticalOffset()
    {
        return hasDefinedGlyphVerticalOffset() ? glyphVerticalOffset : 0.0;
    }

    @Override
    public double getElevationThreshold()
    {
        return hasDefinedElevationThreshold() ? elevationThreshold : 0;
    }

    @Override
    public Groupability getGroupability()
    {
        // TODO: SmartTracks: Is this the correct default?
        return hasDefinedGroupability() ? groupability : Groupability.SimilarOnly;
    }

    @Override
    public void setVisible(boolean isVisible)
    {
        Boolean oldValue = visible;
        visible = isVisible;
        propertyChangeSupport.firePropertyChange(VISIBILITY, oldValue, visible);
    }

    public void setUniqueKey(String uniqueKey)
    {
        this.uniqueKey = uniqueKey;
    }

    @Override
    public void setTrackShape(TrackShape trackShape)
    {
        TrackShape oldValue = this.trackShape;
        this.trackShape = trackShape;
        propertyChangeSupport.firePropertyChange(TRACK_SHAPE, oldValue, this.trackShape);
    }

    @Override
    public void setSnapToSurface(boolean snapToSurface)
    {
        Boolean oldValue = this.snapToSurface;
        this.snapToSurface = snapToSurface;
        propertyChangeSupport.firePropertyChange(SNAP_TO_SURFACE, oldValue, this.snapToSurface);
    }

    @Override
    public void setLinkTrackShapeToHeading(boolean linkTrackShapeToHeading)
    {
        Boolean oldValue = this.linkTrackShapeToHeading;
        this.linkTrackShapeToHeading = linkTrackShapeToHeading;
        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_TRACK_HEADING, oldValue, this.linkTrackShapeToHeading);
    }

    @Override
    public void setGlyphResource(String glyphResource)
    {
        String oldValue = this.glyphResource;
        this.glyphResource = glyphResource;
        propertyChangeSupport.firePropertyChange(GLYPH_RESOURCE, oldValue, this.glyphResource);
    }

    @Override
    public void setGroupability(Groupability groupability)
    {
        Groupability oldValue = this.groupability;
        this.groupability = groupability;
        propertyChangeSupport.firePropertyChange(GROUPABILITY, oldValue, this.groupability);
    }

    @Override
    public void setBucketMode(@Nonnull BucketMode bucketMode)
    {
        BucketMode oldValue = this.bucketMode;
        this.bucketMode = bucketMode;
        propertyChangeSupport.firePropertyChange(BUCKET_MODE, oldValue, this.bucketMode);
    }

    @Override
    public BucketMode getBucketMode()
    {
        return hasDefinedBucketMode() ? bucketMode : BucketMode.None;
    }

    @Override
    public void setTrackShapeConstantOpacity(float trackShapeConstantOpacity)
    {
        if (trackShapeConstantOpacity < 0.0f || trackShapeConstantOpacity > 1.0f)
        {
            throw new IllegalArgumentException("Opacity must be in the range 0.0 through 1.0, inclusive");
        }

        Float oldValue = this.trackShapeConstantOpacity;
        this.trackShapeConstantOpacity = trackShapeConstantOpacity;
        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_OPACITY, oldValue, this.trackShapeConstantOpacity);
    }

    @Override
    public void setTrackShapeConstantSize(int trackShapeConstantSize)
    {
        if (trackShapeConstantSize < 1 || trackShapeConstantSize > 10)
        {
            throw new IllegalArgumentException("Parameter 'trackShapeConstantSize' must be between 1 and 10");
        }

        Integer oldValue = this.trackShapeConstantSize;
        this.trackShapeConstantSize = trackShapeConstantSize;
        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_SIZE, oldValue, this.trackShapeConstantSize);
    }

    @Override
    public boolean isGlyphLinkedToTrackSize()
    {
        return hasDefinedLinkGlyphSizeToTrackSize() ? linkGlyphSizeToTrackSize : false;
    }

    @Override
    public boolean isGlyphLinkedToTrackOpacity()
    {
        return hasDefinedLinkGlyphOpacityToTrackOpacity() ? linkGlyphOpacityToTrackOpacity : true;
    }

    @Override
    public boolean isLinkGlyphToHeading()
    {
        return hasDefinedLinkGlyphToHeading() ? linkGlyphToHeading : false;
    }

    @Override
    public void setLinkGlyphToHeading(boolean linkGlyphToHeading)
    {
        Boolean oldValue = this.linkGlyphToHeading;
        this.linkGlyphToHeading = linkGlyphToHeading;
        propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_HEADING, oldValue, this.linkGlyphToHeading);
    }

    @Override
    public void setLinkGlyphOpacityToTrackOpacity(boolean linkGlyphOpacityToTrackOpacity)
    {
        Boolean oldValue = this.linkGlyphOpacityToTrackOpacity;
        this.linkGlyphOpacityToTrackOpacity = linkGlyphOpacityToTrackOpacity;
        propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_OPACITY, oldValue, this.linkGlyphOpacityToTrackOpacity);
    }

    @Override
    public void setLinkGlyphSizeToTrackSize(boolean linkGlyphSizeToTrackSize)
    {
        Boolean oldValue = this.linkGlyphSizeToTrackSize;
        this.linkGlyphSizeToTrackSize = linkGlyphSizeToTrackSize;
        propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_SIZE, oldValue, this.linkGlyphSizeToTrackSize);
    }

    @Override
    public void setGlyphConstantSize(int constantGlyphSize)
    {
        if (constantGlyphSize < 1 || constantGlyphSize > 10)
        {
            throw new IllegalArgumentException("Size must be in the range 1 through 10, inclusive");
        }

        Integer oldValue = glyphConstantSize;
        glyphConstantSize = constantGlyphSize;
        propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_SIZE, oldValue, glyphConstantSize);
    }

    @Override
    public boolean isSnappedToSurface()
    {
        return hasDefinedSnapToSurface() ? snapToSurface : true;
    }

    @Override
    public void setFadedOverTime(boolean fadedOverTimeFlag)
    {
        Boolean oldValue = this.fadedOverTimeFlag;
        this.fadedOverTimeFlag = fadedOverTimeFlag;
        propertyChangeSupport.firePropertyChange(FADED_OVER_TIME, oldValue, this.fadedOverTimeFlag);
    }

    @Override
    public void setShowHeading(boolean showHeading)
    {
        Boolean oldValue = this.showHeading;
        this.showHeading = showHeading;
        propertyChangeSupport.firePropertyChange(SHOW_HEADING, oldValue, this.showHeading);
    }

    @Override
    public boolean isShowingHeading()
    {
        return hasDefinedShowHeading() ? showHeading : false;
    }

    @Override
    public boolean hasDefinedShowHeading()
    {
        return (null != showHeading);
    }

    @Override
    public TrackShape getTrackShape()
    {
        return hasDefinedTrackShape() ? trackShape : TrackShape.Sphere;
    }

    @Override
    public boolean isTrackShapeLinkedToHeading()
    {
        return hasDefinedLinkTrackShapeToHeading() ? linkTrackShapeToHeading : false;
    }

    @Override
    public int getGlyphConstantSize()
    {
        return hasDefinedGlyphConstantSize() ? glyphConstantSize : ISize.NORMAL;
    }

    @Override
    public Color getTrackShapeConstantColor()
    {
        return hasDefinedTrackShapeConstantColor() ? new Color(trackShapeConstantColor, true) : null;
    }

    @Override
    public float getTrackShapeConstantOpacity()
    {
        return hasDefinedTrackShapeConstantOpacity() ? trackShapeConstantOpacity : 1.0f;
    }

    @Override
    public int getTrackShapeConstantSize()
    {
        return hasDefinedTrackShapeConstantSize() ? trackShapeConstantSize : ISize.NORMAL;
    }

    @Override
    public String getGlyphResource()
    {
        return hasDefinedGlyphResource() ? glyphResource : null;
    }

    @Override
    public float getGlyphConstantOpacity()
    {
        return hasDefinedGlyphConstantOpacity() ? glyphConstantOpacity : 1.0f;
    }

    @Override
    public boolean isUserDefined()
    {
        return isUserDefined != null && isUserDefined;
    }

    public void setUserDefined(boolean userDefined)
    {
        isUserDefined = userDefined;
    }

    @Override
    public void setTrackShapeConstantColor(Color constantColorValue)
    {
        if (constantColorValue != null &&
                (null == trackShapeConstantColor || !trackShapeConstantColor.equals(constantColorValue.getRGB())))
        {
            Color oldValue = null;
            if (null != trackShapeConstantColor) oldValue = new Color(trackShapeConstantColor);
            trackShapeConstantColor = constantColorValue.getRGB();
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE_COLOR, oldValue, new Color(trackShapeConstantColor));
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackStyleDefinition that = (TrackStyleDefinition) o;

        return family.equals(that.family) && type.equals(that.type) && uniqueKey.equals(that.uniqueKey);
    }

    @Override
    public int hashCode()
    {
        int result = family.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + uniqueKey.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return family + ":" + type + ":" + uniqueKey;
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public String getFamily()
    {
        return family;
    }

    public void setFamily(String family)
    {
        this.family = family;
    }

    @Override
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public String getUniqueKey()
    {
        return uniqueKey;
    }

    @Override
    public void setGlyphConstantOpacity(float glyphOpacity)
    {
        if (glyphOpacity < 0.0f || glyphOpacity > 1.0f)
        {
            throw new IllegalArgumentException("Opacity must be in the range 0.0 through 1.0, inclusive");
        }

        Float oldValue = glyphConstantOpacity;
        glyphConstantOpacity = glyphOpacity;
        propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_OPACITY, oldValue, glyphConstantOpacity);
    }

    @Override
    public void setGlyphVerticalAlignment(VerticalAlignment glyphVerticalAlignment)
    {
        VerticalAlignment oldValue = this.glyphVerticalAlignment;
        this.glyphVerticalAlignment = glyphVerticalAlignment;
        propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_ALIGNMENT, oldValue, this.glyphVerticalAlignment);
    }

    @Override
    public void setGlyphVerticalOffset(double offset)
    {
        Double oldValue = glyphVerticalOffset;
        glyphVerticalOffset = offset;
        propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_OFFSET, oldValue, glyphVerticalOffset);
    }

    @Override
    public void setElevationThreshold(double elevationThreshold)
    {
        if (elevationThreshold < 0)
        {
            throw new IllegalArgumentException("Parameter 'elevationThreshold' must be >= 0");
        }

        Double oldValue = this.elevationThreshold;
        this.elevationThreshold = elevationThreshold;
        propertyChangeSupport.firePropertyChange(ELEVATION_THRESHOLD, oldValue, this.elevationThreshold);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties)
    {
        if (properties.isEmpty()) throw new IllegalArgumentException("Property list for listener must be non-empty");
        for (String property : properties)
        {
            addPropertyChangeListener(listener, property);
        }
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull String property)
    {
        propertyChangeSupport.addPropertyChangeListener(property, listener);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties)
    {
        if (properties.isEmpty()) throw new IllegalArgumentException("Property list for listener must be non-empty");
        for (String property : properties)
        {
            removePropertyChangeListener(listener, property);
        }
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull String property)
    {
        propertyChangeSupport.removePropertyChangeListener(property, listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public boolean hasDefinedFadedOverTime()
    {
        return (null != fadedOverTimeFlag);
    }

    @Override
    public void clearDefinedFadedOverTime()
    {
        if (null != fadedOverTimeFlag)
        {
            boolean oldValue = fadedOverTimeFlag;
            fadedOverTimeFlag = null;
            propertyChangeSupport.firePropertyChange(FADED_OVER_TIME + CLEARED, oldValue, null);
            // Can't use new value here since it defaults to false when the field is not set.
            // So, the case of clearing a setting of false would fail to fire a change event
            propertyChangeSupport.firePropertyChange(FADED_OVER_TIME, oldValue, null);
        }
    }

    @Override
    public boolean hasDefinedTimeThreshold()
    {
        return (null != timeThreshold);
    }

    @Override
    public void clearDefinedTimeThreshold()
    {
        if (null != timeThreshold)
        {
            long oldValue = timeThreshold;
            timeThreshold = null;
            propertyChangeSupport.firePropertyChange(TIME_THRESHOLD + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(TIME_THRESHOLD, oldValue, getTimeThreshold());
        }
    }

    @Override
    public boolean hasDefinedLinkGlyphSizeToTrackSize()
    {
        return (null != linkGlyphSizeToTrackSize);
    }

    @Override
    public void clearDefinedBucketMode()
    {
        if (null != bucketMode)
        {
            BucketMode oldValue = bucketMode;
            bucketMode = null;
            propertyChangeSupport.firePropertyChange(BUCKET_MODE + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(BUCKET_MODE, oldValue, getBucketMode());
        }
    }

    @Override
    public boolean hasDefinedBucketMode()
    {
        return (null != bucketMode);
    }

    @Override
    public void clearDefinedLinkGlyphSizeToTrackSize()
    {
        if (null != linkGlyphSizeToTrackSize)
        {
            boolean oldValue = linkGlyphSizeToTrackSize;
            linkGlyphSizeToTrackSize = null;
            propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_SIZE + CLEARED, oldValue, null);
            // Can't use new value here since it defaults to false when the field is not set.
            // So, the case of clearing a setting of false would fail to fire a change event
            propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_SIZE, oldValue, null);
        }
    }

    @Override
    public boolean hasDefinedGlyphConstantSize()
    {
        return (null != glyphConstantSize);
    }

    @Override
    public void clearDefinedGlyphConstantSize()
    {
        if (null != glyphConstantSize)
        {
            int oldValue = glyphConstantSize;
            glyphConstantSize = null;
            propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_SIZE + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_SIZE, oldValue, getGlyphConstantSize());
        }
    }

    @Override
    public boolean hasDefinedLinkGlyphOpacityToTrackOpacity()
    {
        return (null != linkGlyphOpacityToTrackOpacity);
    }

    @Override
    public void clearDefinedLinkGlyphOpacityToTrackOpacity()
    {
        if (null != linkGlyphOpacityToTrackOpacity)
        {
            boolean oldValue = linkGlyphOpacityToTrackOpacity;
            linkGlyphOpacityToTrackOpacity = null;
            propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_OPACITY + CLEARED, oldValue, null);
            // Can't use new value here since it defaults to false when the field is not set.
            // So, the case of clearing a setting of false would fail to fire a change event
            propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_OPACITY, oldValue, null);
        }
    }

    @Override
    public boolean hasDefinedLinkGlyphToHeading()
    {
        return (null != linkGlyphToHeading);
    }

    @Override
    public void clearDefinedLinkGlyphToHeading()
    {
        if (null != linkGlyphToHeading)
        {
            boolean oldValue = linkGlyphToHeading;
            linkGlyphToHeading = null;
            propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_HEADING + CLEARED, oldValue, null);
            // Can't use new value here since it defaults to false when the field is not set.
            // So, the case of clearing a setting of false would fail to fire a change event
            propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_HEADING, oldValue, null);
        }
    }

    @Override
    public boolean hasDefinedSnapToSurface()
    {
        return (null != snapToSurface);
    }

    @Override
    public void clearDefinedSnapToSurface()
    {
        if (null != snapToSurface)
        {
            boolean oldValue = snapToSurface;
            snapToSurface = null;
            propertyChangeSupport.firePropertyChange(SNAP_TO_SURFACE + CLEARED, oldValue, null);
            // Can't use new value here since it defaults to false when the field is not set.
            // So, the case of clearing a setting of false would fail to fire a change event
            propertyChangeSupport.firePropertyChange(SNAP_TO_SURFACE, oldValue, null);
        }
    }

    @Override
    public boolean hasDefinedTrackShape()
    {
        return (null != trackShape);
    }

    @Override
    public void clearDefinedTrackShape()
    {
        if (null != trackShape)
        {
            TrackShape oldValue = trackShape;
            trackShape = null;
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE, oldValue, getTrackShape());
        }
    }

    @Override
    public boolean hasDefinedLinkTrackShapeToHeading()
    {
        return (null != linkTrackShapeToHeading);
    }

    @Override
    public void clearDefinedLinkTrackShapeToHeading()
    {
        if (null != linkTrackShapeToHeading)
        {
            boolean oldValue = linkTrackShapeToHeading;
            linkTrackShapeToHeading = null;
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE_TRACK_HEADING + CLEARED, oldValue, null);
            // Can't use new value here since it defaults to false when the field is not set.
            // So, the case of clearing a setting of false would fail to fire a change event
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE_TRACK_HEADING, oldValue, null);
        }
    }

    @Override
    public boolean hasDefinedTrackShapeConstantColor()
    {
        return (null != trackShapeConstantColor);
    }

    @Override
    public void clearDefinedTrackShapeConstantColor()
    {
        if (null != trackShapeConstantColor)
        {
            Color oldValue = new Color(trackShapeConstantColor);
            trackShapeConstantColor = null;
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE_COLOR + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE_COLOR, oldValue, getTrackShapeConstantColor());
        }
    }

    @Override
    public boolean hasDefinedTrackShapeConstantOpacity()
    {
        return (null != trackShapeConstantOpacity);
    }

    @Override
    public void clearDefinedTrackShapeConstantOpacity()
    {
        if (null != trackShapeConstantOpacity)
        {
            float oldValue = trackShapeConstantOpacity;
            trackShapeConstantOpacity = null;
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE_OPACITY + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE_OPACITY, oldValue, getTrackShapeConstantOpacity());
        }
    }

    @Override
    public boolean hasDefinedTrackShapeConstantSize()
    {
        return (null != trackShapeConstantSize);
    }

    @Override
    public void clearDefinedTrackShapeConstantSize()
    {
        if (null != trackShapeConstantSize)
        {
            int oldValue = trackShapeConstantSize;
            trackShapeConstantSize = null;
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE_SIZE + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(TRACK_SHAPE_SIZE, oldValue, getTrackShapeConstantSize());
        }
    }

    @Override
    public boolean hasDefinedGlyphResource()
    {
        return (null != glyphResource);
    }

    @Override
    public void clearDefinedGlyphResource()
    {
        if (null != glyphResource)
        {
            String oldValue = glyphResource;
            glyphResource = null;
            propertyChangeSupport.firePropertyChange(GLYPH_RESOURCE + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(GLYPH_RESOURCE, oldValue, getGlyphResource());
        }
    }

    @Override
    public boolean hasDefinedGlyphConstantOpacity()
    {
        return (null != glyphConstantOpacity);
    }

    @Override
    public void clearDefinedGlyphConstantOpacity()
    {
        if (null != glyphConstantOpacity)
        {
            Float oldValue = glyphConstantOpacity;
            glyphConstantOpacity = null;
            propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_OPACITY + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_OPACITY, oldValue, getGlyphConstantOpacity());
        }
    }

    @Override
    public boolean hasDefinedGlyphVerticalAlignment()
    {
        return (null != glyphVerticalAlignment);
    }

    @Override
    public void clearDefinedGlyphVerticalAlignment()
    {
        if (null != glyphVerticalAlignment)
        {
            VerticalAlignment oldValue = glyphVerticalAlignment;
            glyphVerticalAlignment = null;
            propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_ALIGNMENT + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_ALIGNMENT, oldValue, getGlyphVerticalAlignment());
        }
    }

    @Override
    public boolean hasDefinedGlyphVerticalOffset()
    {
        return (null != glyphVerticalOffset);
    }

    @Override
    public void clearDefinedGlyphVerticalOffset()
    {
        if (null != glyphVerticalOffset)
        {
            Double oldValue = glyphVerticalOffset;
            glyphVerticalOffset = null;
            propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_OFFSET + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_OFFSET, oldValue, getGlyphVerticalOffset());
        }
    }

    @Override
    public boolean hasDefinedVisible()
    {
        return (null != visible);
    }

    @Override
    public void clearDefinedVisible()
    {
        if (null != visible)
        {
            boolean oldValue = visible;
            visible = null;
            propertyChangeSupport.firePropertyChange(VISIBILITY + CLEARED, oldValue, null);
            // Can't use new value here since it defaults to false when the field is not set.
            // So, the case of clearing a setting of false would fail to fire a change event
            propertyChangeSupport.firePropertyChange(VISIBILITY, oldValue, null);
        }
    }

    @Override
    public void clearDefinedShowHeading()
    {
        if (null != showHeading)
        {
            boolean oldValue = showHeading;
            showHeading = null;
            propertyChangeSupport.firePropertyChange(SHOW_HEADING + CLEARED, oldValue, null);
            // Can't use new value here since it defaults to false when the field is not set.
            // So, the case of clearing a setting of false would fail to fire a change event
            propertyChangeSupport.firePropertyChange(SHOW_HEADING, oldValue, null);
        }
    }

    @Override
    public boolean hasDefinedElevationThreshold()
    {
        return (null != elevationThreshold);
    }

    @Override
    public void clearDefinedElevationThreshold()
    {
        if (null != elevationThreshold)
        {
            Double oldValue = elevationThreshold;
            elevationThreshold = null;
            propertyChangeSupport.firePropertyChange(ELEVATION_THRESHOLD + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(ELEVATION_THRESHOLD, oldValue, getElevationThreshold());
        }
    }

    @Override
    public boolean hasDefinedGroupability()
    {
        return (null != groupability);
    }

    @Override
    public void clearDefinedGroupability()
    {
        if (null != groupability)
        {
            Groupability oldValue = groupability;
            groupability = null;
            propertyChangeSupport.firePropertyChange(GROUPABILITY + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(GROUPABILITY, oldValue, getGroupability());
        }
    }
}
