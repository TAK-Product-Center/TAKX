package gov.takx.core.tracks;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.tracks.IExtendedSmartTrackManager;
import gov.takx.api.tracks.ISize;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.platform.tracks.BucketMode;
import gov.takx.platform.tracks.Groupability;
import gov.takx.platform.tracks.TrackShape;
import gov.takx.platform.tracks.VerticalAlignment;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Implementation of {@link ITrackStylingContext}.  Handles attribute retrieval with
 * reach back to defined parent.  property changes in the parents are tracked and forwarded.  Changes in the
 * TSD assigned to a MapObject are tracked and any TSC involving that map object is properly adjusted to reference
 * the new TSD.
 * <p/>
 * Default track color on map object is unified into the getter and event hierarchy so consumers only
 * have to interact with this object instead of watching the map object as well.
 *
 * @since 1.12
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TrackStylingContext implements ITrackStylingContext
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ITrackStyleDefinition tsd;
    private final ITrackStylingContext parent;
    private IMapObject mapObject = null; // non-null IFF we are the direct context for a map object

    // The isVisible method can literally be called a million times when rendering a complex scene.  So, we
    // cache the visible value and then clear it whenever our TSD or parent TSC changes visibility.
    // And, due to the massive number of calls made, the Boolean.booleanValue method called for auto-unboxing
    // is actually taking 1% of our time.  So, use a second variable to manage state, avoiding the auto-boxing.
    private boolean cachedVisibleValueValid = false;
    private boolean cachedVisibleState;

    private boolean cachedGroupabilityValueValid = false;
    private Groupability cachedGroupabilityState;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    @Inject
    private IExtendedSmartTrackManager smartTrackManager;

    /**
     * Constructor for no parent using the given TSD for attribute values.  This constructor should only be used
     * to construct the context for the Global TSD.  Any properties not defined on the given TSD will use
     * default values when requested.
     *
     * @param tsd The track style definition to use for property value access
     */
    TrackStylingContext(ITrackStyleDefinition tsd)
    {
        this(tsd, null);
    }

    /**
     * Constructor for a given TSD and parent.  Property value access will use reach back into the given parent,
     * if specified.
     *
     * @param tsd    The track style definition to use for property value access
     * @param parent The parent context to use for reachback if the TSD doesn't specify a value.  May be {@code null}
     *               in which case no reach back is performed.
     */
    TrackStylingContext(ITrackStyleDefinition tsd, ITrackStylingContext parent)
    {
        this.tsd = tsd;
        this.parent = parent;

        tsd.addPropertyChangeListener(new TSDPropertyChangeListener());

        if (parent != null)
        {
            parent.addPropertyChangeListener(new ParentTSCPropertyChangeListener());
        }
    }

    /**
     * Set the map object. Only used when constructing a context directly on a map object.
     *
     * @param mapObject Our map object
     */
    void setMapObject(IMapObject mapObject)
    {
        this.mapObject = mapObject;

        // Now listen for changes in the MapObjects default track color and fire events on the context if needed
        mapObject.addPropertyChangeListener(IMapObject.DEFAULT_TRACK_COLOR, new ColorChangeListener(this));

        // And listen for replacement of the TSD on the MO so the TSC can be updated
        mapObject.addPropertyChangeListener(IMapObject.TRACK_STYLE, new TSDChangeListener(this));
    }

    @Override
    public ITrackStylingContext getParent()
    {
        return parent;
    }

    @Override
    public ITrackStyleDefinition getTrackStyleDefinition()
    {
        return tsd;
    }

    /**
     * Set a new Track Style Definition for this context.
     *
     * @param tsd New Track Style Definition to use for value access
     */
    private void setTrackStyleDefinition(ITrackStyleDefinition tsd)
    {
        ITrackStyleDefinition oldValue = this.tsd;
        this.tsd = tsd;
        tsd.addPropertyChangeListener(new TSDPropertyChangeListener());
        propertyChangeSupport.firePropertyChange(TRACK_STYLE_DEFINITION, oldValue, tsd);
    }

    @Override
    public boolean isFadedOverTime()
    {
        boolean value = false;

        if (!tsd.hasDefinedFadedOverTime())
        {
            if (null != parent)
            {
                value = parent.isFadedOverTime();
            } else
            {
                logger.warn("isFadedOverTime: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.isFadedOverTime();
        }

        return value;
    }

    @Override
    public boolean isShowingHeading()
    {
        boolean value = false;

        if (!tsd.hasDefinedShowHeading())
        {
            if (null != parent)
            {
                value = parent.isShowingHeading();
            } else
            {
                logger.warn("isShowingHeading: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.isShowingHeading();
        }

        return value;
    }

    @Override
    public long getTimeThreshold()
    {
        long value = 0;

        if (!tsd.hasDefinedTimeThreshold())
        {
            if (null != parent)
            {
                value = parent.getTimeThreshold();
            }
            // No warning here since a 0 result is normal
        } else
        {
            value = tsd.getTimeThreshold();
        }

        return value;
    }

    @Override
    public boolean hasTimeThreshold()
    {
        return getTimeThreshold() > 0;
    }

    @Override
    public boolean isGlyphLinkedToTrackSize()
    {
        boolean value = false;

        if (!tsd.hasDefinedLinkGlyphSizeToTrackSize())
        {
            if (null != parent)
            {
                value = parent.isGlyphLinkedToTrackSize();
            } else
            {
                logger.warn("isGlyphLinkedToTrackSize: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.isGlyphLinkedToTrackSize();
        }

        return value;
    }

    @Override
    public int getGlyphConstantSize()
    {
        int value = ISize.NORMAL;

        if (!tsd.hasDefinedGlyphConstantSize())
        {
            if (null != parent)
            {
                value = parent.getGlyphConstantSize();
            } else
            {
                logger.warn("getGlyphConstantSize: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.getGlyphConstantSize();
        }

        return value;
    }

    @Override
    public boolean isGlyphLinkedToTrackOpacity()
    {
        boolean value = true;

        if (!tsd.hasDefinedLinkGlyphOpacityToTrackOpacity())
        {
            if (null != parent)
            {
                value = parent.isGlyphLinkedToTrackOpacity();
            } else
            {
                logger.warn("isGlyphLinkedToTrackOpacity: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.isGlyphLinkedToTrackOpacity();
        }

        return value;
    }

    @Override
    public boolean isLinkGlyphToHeading()
    {
        boolean value = false;

        if (!tsd.hasDefinedLinkGlyphToHeading())
        {
            if (null != parent)
            {
                value = parent.isLinkGlyphToHeading();
            } else
            {
                logger.warn("isLinkGlyphToHeading: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.isLinkGlyphToHeading();
        }

        return value;
    }

    @Override
    public boolean isSnappedToSurface()
    {
        boolean value = false;

        if (!tsd.hasDefinedSnapToSurface())
        {
            if (null != parent)
            {
                value = parent.isSnappedToSurface();
            } else
            {
                logger.warn("isSnappedToSurface: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.isSnappedToSurface();
        }

        return value;
    }

    @Override
    public TrackShape getTrackShape()
    {
        TrackShape value = TrackShape.Sphere;

        if (!tsd.hasDefinedTrackShape())
        {
            if (null != parent)
            {
                value = parent.getTrackShape();
            } else
            {
                logger.warn("getTrackShape: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.getTrackShape();
        }

        return value;
    }

    @Override
    public boolean isTrackShapeLinkedToHeading()
    {
        boolean value = false;

        if (!tsd.hasDefinedLinkTrackShapeToHeading())
        {
            if (null != parent)
            {
                value = parent.isTrackShapeLinkedToHeading();
            } else
            {
                logger.warn("isTrackShapeLinkedToHeading: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.isTrackShapeLinkedToHeading();
        }

        return value;
    }

    @Override
    public Color getTrackShapeConstantColor()
    {
        Color value = null;

        if (!tsd.hasDefinedTrackShapeConstantColor())
        {
            if (null != parent)
            {
                value = parent.getTrackShapeConstantColor();
            }

            // If we are the context handling a map object, then default to the default track color on the map object
            if (value == null && mapObject != null)
            {
                value = mapObject.getDefaultTrackColor();
            }

            // No warning here since a null result is normal
        } else
        {
            value = tsd.getTrackShapeConstantColor();
        }

        return value;
    }

    @Override
    public float getTrackShapeConstantOpacity()
    {
        float value = 1.0f;

        if (!tsd.hasDefinedTrackShapeConstantOpacity())
        {
            if (null != parent)
            {
                value = parent.getTrackShapeConstantOpacity();
            } else
            {
                logger.warn("getTrackShapeConstantOpacity: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.getTrackShapeConstantOpacity();
        }

        return value;
    }

    @Override
    public int getTrackShapeConstantSize()
    {
        int value = ISize.NORMAL;

        if (!tsd.hasDefinedTrackShapeConstantSize())
        {
            if (null != parent)
            {
                value = parent.getTrackShapeConstantSize();
            } else
            {
                logger.warn("getTrackShapeConstantSize: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.getTrackShapeConstantSize();
        }

        return value;
    }

    @Override
    public String getGlyphResource()
    {
        String value = null;

        if (!tsd.hasDefinedGlyphResource())
        {
            if (null != parent)
            {
                value = parent.getGlyphResource();
            }
            // No warning here since a null result is normal
        } else
        {
            value = tsd.getGlyphResource();
        }

        return value;
    }

    @Override
    public float getGlyphConstantOpacity()
    {
        float value = 1.0f;

        if (!tsd.hasDefinedGlyphConstantOpacity())
        {
            if (null != parent)
            {
                value = parent.getGlyphConstantOpacity();
            } else
            {
                logger.warn("getGlyphConstantOpacity: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.getGlyphConstantOpacity();
        }

        return value;
    }

    @Override
    public VerticalAlignment getGlyphVerticalAlignment()
    {
        VerticalAlignment value;

        if (!tsd.hasDefinedGlyphVerticalAlignment())
        {
            if (null != parent)
            {
                value = parent.getGlyphVerticalAlignment();
            } else
            {
                logger.warn("getGlyphVerticalAlignment: no value defined and no parent available!");
                value = VerticalAlignment.DEFAULT;
            }
        } else
        {
            value = tsd.getGlyphVerticalAlignment();
        }

        return value;
    }

    @Override
    public double getGlyphVerticalOffset()
    {
        double value = 0;

        if (!tsd.hasDefinedGlyphVerticalOffset())
        {
            if (null != parent)
            {
                value = parent.getGlyphVerticalOffset();
            } else
            {
                logger.warn("getGlyphVerticalOffset: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.getGlyphVerticalOffset();
        }

        return value;
    }

    @Override
    public boolean isVisible()
    {
        if (cachedVisibleValueValid)
        {
            return cachedVisibleState;
        }

        boolean value = false;

        if (!tsd.hasDefinedVisible())
        {
            if (null != parent)
            {
                value = parent.isVisible();
            } else
            {
                logger.warn("isVisible: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.isVisible();
        }

        cachedVisibleState = value;
        cachedVisibleValueValid = true;

        return value;
    }

    @Override
    public double getElevationThreshold()
    {
        double value = 0;

        if (!tsd.hasDefinedElevationThreshold())
        {
            if (null != parent)
            {
                value = parent.getElevationThreshold();
            } else
            {
                logger.warn("getElevationThreshold: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.getElevationThreshold();
        }

        return value;
    }

    @Override
    public BucketMode getBucketMode()
    {
        BucketMode value = BucketMode.None;

        if (!tsd.hasDefinedBucketMode())
        {
            if (null != parent)
            {
                value = parent.getBucketMode();
            } else
            {
                logger.warn("getBucketMode: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.getBucketMode();
        }

        return value;
    }

    @Override
    public Groupability getGroupability()
    {
        if (cachedGroupabilityValueValid)
        {
            return cachedGroupabilityState;
        }

        // TODO: SmartTracks: Is this the correct default?
        Groupability value = Groupability.SimilarOnly;

        if (!tsd.hasDefinedGroupability())
        {
            if (null != parent)
            {
                value = parent.getGroupability();
            } else
            {
                logger.warn("getGroupability: no value defined and no parent available!");
            }
        } else
        {
            value = tsd.getGroupability();
        }

        cachedGroupabilityState = value;
        cachedGroupabilityValueValid = true;

        return value;
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

    private class TSDPropertyChangeListener implements PropertyChangeListener
    {
        /**
         * When events are fired on our direct TSD, then we just forward them on to our listeners.
         *
         * @param evt A PropertyChangeEvent object describing the event source
         *            and the property that has changed.
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            // CORE-2907:
            //    All of the boolean flags PCE.newValue will be null to ensure they event is fired
            //    We need to actually determine the proper new value here and replace it in the event

            final String propertyName = evt.getPropertyName();
            Object newValue = evt.getNewValue();

            // NOTE: We also need to clear our cache control flags if this is VISIBILITY or GROUPABILITY

            switch (propertyName)
            {
                case FADED_OVER_TIME -> newValue = isFadedOverTime();
                case GLYPH_LINKED_TRACK_HEADING -> newValue = isLinkGlyphToHeading();
                case GLYPH_LINKED_TRACK_OPACITY -> newValue = isGlyphLinkedToTrackOpacity();
                case GLYPH_LINKED_TRACK_SIZE -> newValue = isGlyphLinkedToTrackSize();
                case GROUPABILITY ->
                {
                    cachedGroupabilityValueValid = false;
                    newValue = getGroupability();
                }
                case SHOW_HEADING -> newValue = isShowingHeading();
                case SNAP_TO_SURFACE -> newValue = isSnappedToSurface();
                case TRACK_SHAPE_TRACK_HEADING -> newValue = isTrackShapeLinkedToHeading();
                case VISIBILITY ->
                {
                    cachedVisibleValueValid = false;
                    newValue = isVisible();
                }
            }

            // Need to construct a new event with the same data, but with this TSC as a source
            PropertyChangeEvent newEvt =
                    new PropertyChangeEvent(TrackStylingContext.this, propertyName, evt.getOldValue(), newValue);
            newEvt.setPropagationId(evt.getPropagationId());

            propertyChangeSupport.firePropertyChange(newEvt);
        }
    }

    /**
     * Internal class to listen for replacement of the TSD assigned to a given map object and replace the reference
     * in the TSC.
     */
    private class TSDChangeListener implements PropertyChangeListener
    {
        private final TrackStylingContext tsc;

        /**
         * Construct a change handler on the specified context.
         *
         * @param tsc Context to manage
         */
        public TSDChangeListener(TrackStylingContext tsc)
        {
            this.tsc = tsc;
        }

        /**
         * Handle a change in the TSD assigned to the map object in a styling context.  This happens as
         * the result of a call to {@link IIconMapObject#setTrackStyleDefinition(ITrackStyleDefinition)}
         * not on modifications to the TSD itself.
         *
         * @param evt A PropertyChangeEvent object describing the event source
         *            and the property that has changed.
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Handle TSD change on MO: {}", evt.getSource());
            }

            if (evt.getPropertyName().equals(IMapObject.TRACK_STYLE))
            {
                // Replace the TSD
                ITrackStyleDefinition newTsd = (ITrackStyleDefinition) evt.getNewValue();

                // If someone actually cleared the TSD on the map object, then we create an empty TSD to use
                if (newTsd == null)
                {
                    newTsd = smartTrackManager.createDeferringTSD((IMapObject) evt.getSource());
                }

                final ITrackStyleDefinition oldTsd = (ITrackStyleDefinition) evt.getOldValue();
                if (oldTsd != null) oldTsd.removePropertyChangeListener(this);

                tsc.setTrackStyleDefinition(newTsd);
            } else
            {
                logger.warn("Unhandled property change on MapObject: {}", evt.getPropertyName());
            }
        }
    }

    /**
     * Internal static class to handle track color changes on the map object in the inheritance hierarchy.  If the
     * map object default track color changes, then, if the TSC doesn't have an override, we need to fire a
     * PCE on track shape color so that the TSC can be the sole place a consumer needs to listen for color change
     * events.
     */
    private static class ColorChangeListener implements PropertyChangeListener
    {
        private final TrackStylingContext tsc;

        /**
         * Construct a listener for the given TSC.
         *
         * @param tsc TSC to manage
         */
        public ColorChangeListener(TrackStylingContext tsc)
        {
            this.tsc = tsc;
        }

        /**
         * Handle a change in the default track color of a map object.  If the TSC we're managing doesn't have
         * a color setting, then fire a property change on the TSC so we can unify the color handling into the
         * context.
         *
         * @param evt Event to process
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (evt.getPropertyName().equals(IMapObject.DEFAULT_TRACK_COLOR))
            {
                // If our TSC's direct tsd has no color setting and it's parent has no color setting, this this
                // new color setting needs to fire a TRACK_SHAPE_COLOR event since the MO's default track color is in play

                // Note: we know we only have to check these 2 levels since we only apply to MO level TSCs

                final boolean hasColor = tsc.tsd.hasDefinedTrackShapeConstantColor();
                final boolean parentHasColor = tsc.getParent().getTrackStyleDefinition().hasDefinedTrackShapeConstantColor();
                if (!hasColor && !parentHasColor)
                {
                    tsc.propertyChangeSupport.firePropertyChange(TRACK_SHAPE_COLOR, evt.getOldValue(), evt.getNewValue());
                }
            } else
            {
                logger.error("Unhandled property change: {}", evt.getPropertyName());
            }
        }
    }

    /**
     * Internal class to handle event propagation from parent TSCs.  It handles a parents change events on
     * properties and on property clears.
     */
    private class ParentTSCPropertyChangeListener implements PropertyChangeListener
    {
        /**
         * Handle a property change in our parent TSD.  If the main TSD doesn't define a value for the property
         * that changed, then fire an event indicating a property change (since due to inheritance, it may have).
         *
         * @param evt A PropertyChangeEvent object describing the event source
         *            and the property that has changed.
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            switch (evt.getPropertyName())
            {
                case FADED_OVER_TIME:
                    if (!tsd.hasDefinedFadedOverTime())
                    {
                        propertyChangeSupport.firePropertyChange(FADED_OVER_TIME, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case FADED_OVER_TIME + CLEARED:
                    if (!tsd.hasDefinedFadedOverTime())
                    {
                        propertyChangeSupport.firePropertyChange(FADED_OVER_TIME, evt.getOldValue(), isFadedOverTime());
                    }
                    break;
                case TIME_THRESHOLD:
                    if (!tsd.hasDefinedTimeThreshold())
                    {
                        propertyChangeSupport.firePropertyChange(TIME_THRESHOLD, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case TIME_THRESHOLD + CLEARED:
                    if (!tsd.hasDefinedTimeThreshold())
                    {
                        propertyChangeSupport.firePropertyChange(TIME_THRESHOLD, evt.getOldValue(), getTimeThreshold());
                    }
                    break;
                case GLYPH_LINKED_TRACK_SIZE:
                    if (!tsd.hasDefinedLinkGlyphSizeToTrackSize())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_SIZE, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case GLYPH_LINKED_TRACK_SIZE + CLEARED:
                    if (!tsd.hasDefinedLinkGlyphSizeToTrackSize())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_SIZE, evt.getOldValue(), isGlyphLinkedToTrackSize());
                    }
                    break;
                case GLYPH_LINKED_TRACK_OPACITY:
                    if (!tsd.hasDefinedLinkGlyphOpacityToTrackOpacity())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_OPACITY, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case GLYPH_LINKED_TRACK_OPACITY + CLEARED:
                    if (!tsd.hasDefinedLinkGlyphOpacityToTrackOpacity())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_OPACITY, evt.getOldValue(), isGlyphLinkedToTrackOpacity());
                    }
                    break;
                case GLYPH_LINKED_TRACK_HEADING:
                    if (!tsd.hasDefinedLinkGlyphToHeading())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_HEADING, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case GLYPH_LINKED_TRACK_HEADING + CLEARED:
                    if (!tsd.hasDefinedLinkGlyphToHeading())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_LINKED_TRACK_HEADING, evt.getOldValue(), isLinkGlyphToHeading());
                    }
                    break;
                case GLYPH_RESOURCE:
                    if (!tsd.hasDefinedGlyphResource())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_RESOURCE, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case GLYPH_RESOURCE + CLEARED:
                    if (!tsd.hasDefinedGlyphResource())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_RESOURCE, evt.getOldValue(), getGlyphResource());
                    }
                    break;
                case GLYPH_CONSTANT_SIZE:
                    if (!tsd.hasDefinedGlyphConstantSize())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_SIZE, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case GLYPH_CONSTANT_SIZE + CLEARED:
                    if (!tsd.hasDefinedGlyphConstantSize())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_SIZE, evt.getOldValue(), getGlyphConstantSize());
                    }
                    break;
                case GLYPH_CONSTANT_OPACITY:
                    if (!tsd.hasDefinedGlyphConstantOpacity())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_OPACITY, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case GLYPH_CONSTANT_OPACITY + CLEARED:
                    if (!tsd.hasDefinedGlyphConstantOpacity())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_CONSTANT_OPACITY, evt.getOldValue(), getGlyphConstantOpacity());
                    }
                    break;
                case GLYPH_VERTICAL_ALIGNMENT:
                    if (!tsd.hasDefinedGlyphVerticalAlignment())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_ALIGNMENT, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case GLYPH_VERTICAL_ALIGNMENT + CLEARED:
                    if (!tsd.hasDefinedGlyphVerticalAlignment())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_ALIGNMENT, evt.getOldValue(), getGlyphVerticalAlignment());
                    }
                    break;
                case GLYPH_VERTICAL_OFFSET:
                    if (!tsd.hasDefinedGlyphVerticalOffset())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_OFFSET, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case GLYPH_VERTICAL_OFFSET + CLEARED:
                    if (!tsd.hasDefinedGlyphVerticalOffset())
                    {
                        propertyChangeSupport.firePropertyChange(GLYPH_VERTICAL_OFFSET, evt.getOldValue(), getGlyphVerticalOffset());
                    }
                    break;
                case SNAP_TO_SURFACE:
                    if (!tsd.hasDefinedSnapToSurface())
                    {
                        propertyChangeSupport.firePropertyChange(SNAP_TO_SURFACE, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case SNAP_TO_SURFACE + CLEARED:
                    if (!tsd.hasDefinedSnapToSurface())
                    {
                        propertyChangeSupport.firePropertyChange(SNAP_TO_SURFACE, evt.getOldValue(), isSnappedToSurface());
                    }
                    break;
                case TRACK_SHAPE:
                    if (!tsd.hasDefinedTrackShape())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case TRACK_SHAPE + CLEARED:
                    if (!tsd.hasDefinedTrackShape())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE, evt.getOldValue(), getTrackShape());
                    }
                    break;
                case TRACK_SHAPE_COLOR:
                    if (!tsd.hasDefinedTrackShapeConstantColor())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_COLOR, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case TRACK_SHAPE_COLOR + CLEARED:
                    if (!tsd.hasDefinedTrackShapeConstantColor())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_COLOR, evt.getOldValue(), getTrackShapeConstantColor());
                    }
                    break;
                case TRACK_SHAPE_OPACITY:
                    if (!tsd.hasDefinedTrackShapeConstantOpacity())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_OPACITY, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case TRACK_SHAPE_OPACITY + CLEARED:
                    if (!tsd.hasDefinedTrackShapeConstantOpacity())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_OPACITY, evt.getOldValue(), getTrackShapeConstantOpacity());
                    }
                    break;
                case TRACK_SHAPE_SIZE:
                    if (!tsd.hasDefinedTrackShapeConstantSize())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_SIZE, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case TRACK_SHAPE_SIZE + CLEARED:
                    if (!tsd.hasDefinedTrackShapeConstantSize())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_SIZE, evt.getOldValue(), getTrackShapeConstantSize());
                    }
                    break;
                case TRACK_SHAPE_TRACK_HEADING:
                    if (!tsd.hasDefinedLinkTrackShapeToHeading())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_TRACK_HEADING, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case TRACK_SHAPE_TRACK_HEADING + CLEARED:
                    if (!tsd.hasDefinedLinkTrackShapeToHeading())
                    {
                        propertyChangeSupport.firePropertyChange(TRACK_SHAPE_TRACK_HEADING, evt.getOldValue(), isTrackShapeLinkedToHeading());
                    }
                    break;
                case VISIBILITY:
                    if (!tsd.hasDefinedVisible())
                    {
                        cachedVisibleValueValid = false; // Clear our cached value
                        propertyChangeSupport.firePropertyChange(VISIBILITY, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case VISIBILITY + CLEARED:
                    if (!tsd.hasDefinedVisible())
                    {
                        cachedVisibleValueValid = false; // Clear our cached value
                        propertyChangeSupport.firePropertyChange(VISIBILITY, evt.getOldValue(), isVisible());
                    }
                    break;
                case ELEVATION_THRESHOLD:
                    if (!tsd.hasDefinedElevationThreshold())
                    {
                        propertyChangeSupport.firePropertyChange(ELEVATION_THRESHOLD, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case ELEVATION_THRESHOLD + CLEARED:
                    if (!tsd.hasDefinedElevationThreshold())
                    {
                        propertyChangeSupport.firePropertyChange(ELEVATION_THRESHOLD, evt.getOldValue(), getElevationThreshold());
                    }
                    break;
                case BUCKET_MODE:
                    if (!tsd.hasDefinedBucketMode())
                    {
                        propertyChangeSupport.firePropertyChange(BUCKET_MODE, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case BUCKET_MODE + CLEARED:
                    if (!tsd.hasDefinedBucketMode())
                    {
                        propertyChangeSupport.firePropertyChange(BUCKET_MODE, evt.getOldValue(), getBucketMode());
                    }
                    break;
                case GROUPABILITY:
                    if (!tsd.hasDefinedGroupability())
                    {
                        cachedGroupabilityValueValid = false; // Clear our cached value
                        propertyChangeSupport.firePropertyChange(GROUPABILITY, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case GROUPABILITY + CLEARED:
                    if (!tsd.hasDefinedGroupability())
                    {
                        cachedGroupabilityValueValid = false; // Clear our cached value
                        propertyChangeSupport.firePropertyChange(GROUPABILITY, evt.getOldValue(), getGroupability());
                    }
                    break;
                case SHOW_HEADING:
                    if (!tsd.hasDefinedShowHeading())
                    {
                        propertyChangeSupport.firePropertyChange(SHOW_HEADING, evt.getOldValue(), evt.getNewValue());
                    }
                    break;
                case SHOW_HEADING + CLEARED:
                    if (!tsd.hasDefinedShowHeading())
                    {
                        propertyChangeSupport.firePropertyChange(SHOW_HEADING, evt.getOldValue(), isShowingHeading());
                    }
                    break;
            }
        }
    }
}
