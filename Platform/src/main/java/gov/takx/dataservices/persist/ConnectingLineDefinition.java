package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.platform.tracks.LineStyle;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostRemove;
import jakarta.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * @since 1.12
 */
@Internal
@Entity
@PseudoBean
public class ConnectingLineDefinition implements IConnectingLineDefinition, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unused") // set by hibernate
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String family;
    private String type;
    @Column(unique = true)
    private String uniqueKey;

    @ManyToOne(targetEntity = ConnectingLineDefinition.class)
    @JoinColumn(name = "parent_id")
    protected IConnectingLineDefinition parentDefinition;

    private String displayName;
    private Integer lineColorValue;
    private Boolean showDirectionalArrow;
    private Float lineOpacity;
    private Integer lineWidth;
    private LineStyle lineStyle;
    private Double elevationThreshold;
    private Boolean isUserDefined;

    @Transient
    @Inject
    protected ISmartTrackManager smartTrackManager;

    @Transient
    protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    @SuppressWarnings("unused") // For hibernate
    protected ConnectingLineDefinition()
    {
    }

    public ConnectingLineDefinition(String family, String type, String uniqueKey, boolean userDefined)
    {
        this.family = family;
        this.type = type;
        this.uniqueKey = uniqueKey;
        isUserDefined = userDefined;
    }

    @Override
    public IConnectingLineDefinition getParent()
    {
        return parentDefinition;
    }

    @Override
    public void setParent(IConnectingLineDefinition parentDefinition)
    {
        IConnectingLineDefinition oldValue = this.parentDefinition;

        if (null != this.parentDefinition)
        {
            // remove the old listener
            this.parentDefinition.removePropertyChangeListener(this);
        }

        this.parentDefinition = parentDefinition;

        if (null != this.parentDefinition)
        {
            this.parentDefinition.addPropertyChangeListener(this);
        }

        propertyChangeSupport.firePropertyChange(PARENT, oldValue, this.parentDefinition);
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
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();
        switch (propertyName)
        {
            case DIRECTIONAL_ARROW:
                if (null == showDirectionalArrow)
                {
                    propertyChangeSupport.firePropertyChange(DIRECTIONAL_ARROW, evt.getOldValue(), evt.getNewValue());
                }
                break;
            case DIRECTIONAL_ARROW + CLEARED:
                if (null == showDirectionalArrow)
                {
                    propertyChangeSupport.firePropertyChange(DIRECTIONAL_ARROW, null, showDirectionalArrow());
                }
                break;
            case LINE_COLOR:
                if (null == lineColorValue)
                {
                    propertyChangeSupport.firePropertyChange(LINE_COLOR, evt.getOldValue(), evt.getNewValue());
                }
                break;
            case LINE_COLOR + CLEARED:
                if (null == lineColorValue)
                {
                    propertyChangeSupport.firePropertyChange(LINE_COLOR, null, getLineColor());
                }
                break;
            case LINE_OPACITY:
                if (null == lineOpacity)
                {
                    propertyChangeSupport.firePropertyChange(LINE_OPACITY, evt.getOldValue(), evt.getNewValue());
                }
                break;
            case LINE_OPACITY + CLEARED:
                if (null == lineOpacity)
                {
                    propertyChangeSupport.firePropertyChange(LINE_OPACITY, null, getLineOpacity());
                }
                break;
            case LINE_WIDTH:
                if (null == lineWidth)
                {
                    propertyChangeSupport.firePropertyChange(LINE_WIDTH, evt.getOldValue(), evt.getNewValue());
                }
                break;
            case LINE_WIDTH + CLEARED:
                if (null == lineWidth)
                {
                    propertyChangeSupport.firePropertyChange(LINE_WIDTH, null, getLineWidth());
                }
                break;
            case LINE_STYLE:
                if (null == lineStyle)
                {
                    propertyChangeSupport.firePropertyChange(LINE_STYLE, evt.getOldValue(), evt.getNewValue());
                }
                break;
            case LINE_STYLE + CLEARED:
                if (null == lineStyle)
                {
                    propertyChangeSupport.firePropertyChange(LINE_STYLE, null, getLineStyle());
                }
                break;
            case ELEVATION_THRESHOLD:
                if (null == elevationThreshold)
                {
                    propertyChangeSupport.firePropertyChange(ELEVATION_THRESHOLD, evt.getOldValue(), evt.getNewValue());
                }
                break;
            case ELEVATION_THRESHOLD + CLEARED:
                if (null == elevationThreshold)
                {
                    propertyChangeSupport.firePropertyChange(ELEVATION_THRESHOLD, null, getElevationThreshold());
                }
                break;
            case PARENT:
                if (null == parentDefinition)
                {
                    propertyChangeSupport.firePropertyChange(PARENT, evt.getOldValue(), evt.getNewValue());
                }
                break;
            case PARENT + CLEARED:
                if (null == parentDefinition)
                {
                    propertyChangeSupport.firePropertyChange(PARENT, null, getParent());
                }
                break;
            default:
                logger.debug("Ignoring PCE: {}", evt);
                break;
        }
    }

    @Override
    public Color getLineColor()
    {
        Color value = null;

        if (!hasDefinedLineColor())
        {
            if (null != parentDefinition)
            {
                value = parentDefinition.getLineColor();
            } else
            {
                logger.warn("getLineColor: no value defined and no parent available!");
            }
        } else
        {
            value = new Color(lineColorValue, true);
        }

        return value;
    }

    @Override
    public void setLineColor(@Nonnull Color color)
    {
        Color oldValue = null != lineColorValue ? new Color(lineColorValue) : null;
        lineColorValue = color.getRGB();
        propertyChangeSupport.firePropertyChange(LINE_COLOR, oldValue, new Color(lineColorValue));
    }

    @Override
    public boolean hasDefinedLineColor()
    {
        return (null != lineColorValue);
    }

    @Override
    public void clearDefinedLineColor()
    {
        if (null != lineColorValue)
        {
            Color oldValue = new Color(lineColorValue);
            lineColorValue = null;
            propertyChangeSupport.firePropertyChange(LINE_COLOR + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(LINE_COLOR, oldValue, getLineColor());
        }
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String getUniqueKey()
    {
        return uniqueKey;
    }

    @Override
    public double getElevationThreshold()
    {
        double value = 0;

        if (!hasDefinedElevationThreshold())
        {
            if (null != parentDefinition)
            {
                value = parentDefinition.getElevationThreshold();
            } else
            {
                logger.warn("getElevationThreshold: no value defined and no parent available!");
            }
        } else
        {
            value = elevationThreshold;
        }

        return value;
    }

    @Override
    public void setElevationThreshold(double elevationThreshold)
    {
        Double oldValue = this.elevationThreshold;
        this.elevationThreshold = elevationThreshold;
        propertyChangeSupport.firePropertyChange(ELEVATION_THRESHOLD, oldValue, this.elevationThreshold);
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
    public String getFamily()
    {
        return family;
    }

    public void setFamily(String family)
    {
        this.family = family;
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public float getLineOpacity()
    {
        float value = 0;

        if (!hasDefinedLineOpacity())
        {
            if (null != parentDefinition)
            {
                value = parentDefinition.getLineOpacity();
            } else
            {
                logger.warn("getLineOpacity: no value defined and no parent available!");
            }
        } else
        {
            value = lineOpacity;
        }

        return value;
    }

    @Override
    public void setLineOpacity(float lineOpacity)
    {
        Float oldValue = this.lineOpacity;
        this.lineOpacity = lineOpacity;
        propertyChangeSupport.firePropertyChange(LINE_OPACITY, oldValue, this.lineOpacity);
    }

    @Override
    public boolean hasDefinedLineOpacity()
    {
        return (null != lineOpacity);
    }

    @Override
    public void clearDefinedLineOpacity()
    {
        if (null != lineOpacity)
        {
            float oldValue = lineOpacity;
            lineOpacity = null;
            propertyChangeSupport.firePropertyChange(LINE_OPACITY + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(LINE_OPACITY, oldValue, getLineOpacity());
        }
    }

    @Override
    public LineStyle getLineStyle()
    {
        LineStyle value = null;

        if (!hasDefinedLineStyle())
        {
            if (null != parentDefinition)
            {
                value = parentDefinition.getLineStyle();
            } else
            {
                logger.warn("getLineStyle: no value defined and no parent available!");
            }
        } else
        {
            value = lineStyle;
        }

        return value;
    }

    @Override
    public void setLineStyle(LineStyle lineStyle)
    {
        // CORE-3265: Only Solid is supported.
        if (lineStyle != LineStyle.None && lineStyle != LineStyle.Solid)
        {
            throw new UnsupportedOperationException(
                    "The only supported line styles are Solid and None. " +
                            "The other styles are deprecated and cannot be used.");
        }

        LineStyle oldValue = this.lineStyle;
        this.lineStyle = lineStyle;
        propertyChangeSupport.firePropertyChange(LINE_STYLE, oldValue, this.lineStyle);
    }

    @Override
    public boolean hasDefinedLineStyle()
    {
        return (null != lineStyle);
    }

    @Override
    public void clearDefinedLineStyle()
    {
        if (null != lineStyle)
        {
            LineStyle oldValue = lineStyle;
            lineStyle = null;
            propertyChangeSupport.firePropertyChange(LINE_STYLE + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(LINE_STYLE, oldValue, getLineStyle());
        }
    }

    @Override
    public int getLineWidth()
    {
        int value = 0;

        if (!hasDefinedLineWidth())
        {
            if (null != parentDefinition)
            {
                value = parentDefinition.getLineWidth();
            } else
            {
                logger.warn("getLineWidth: no value defined and no parent available!");
            }
        } else
        {
            value = lineWidth;
        }

        return value;
    }

    @Override
    public void setLineWidth(int lineWidth)
    {
        Integer oldValue = this.lineWidth;
        this.lineWidth = lineWidth;
        propertyChangeSupport.firePropertyChange(LINE_WIDTH, oldValue, this.lineWidth);
    }

    @Override
    public boolean hasDefinedLineWidth()
    {
        return (null != lineWidth);
    }

    @Override
    public void clearDefinedLineWidth()
    {
        if (null != lineWidth)
        {
            int oldValue = lineWidth;
            lineWidth = null;
            propertyChangeSupport.firePropertyChange(LINE_WIDTH + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(LINE_WIDTH, oldValue, getLineWidth());
        }
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
    public boolean showDirectionalArrow()
    {
        boolean value = false;

        if (!hasDefinedDirectionalArrow())
        {
            if (null != parentDefinition)
            {
                value = parentDefinition.showDirectionalArrow();
            } else
            {
                logger.warn("showDirectionalArrow: no value defined and no parent available!");
            }
        } else
        {
            value = showDirectionalArrow;
        }

        return value;
    }

    @Override
    public void setDirectionalArrow(boolean showDirectionalArrow)
    {
        Boolean oldValue = this.showDirectionalArrow;
        this.showDirectionalArrow = showDirectionalArrow;
        propertyChangeSupport.firePropertyChange(DIRECTIONAL_ARROW, oldValue, this.showDirectionalArrow);
    }

    @Override
    public boolean hasDefinedDirectionalArrow()
    {
        return (null != showDirectionalArrow);
    }

    @Override
    public void clearDefinedDirectionalArrow()
    {
        if (null != showDirectionalArrow)
        {
            boolean oldValue = showDirectionalArrow;
            showDirectionalArrow = null;
            propertyChangeSupport.firePropertyChange(DIRECTIONAL_ARROW + CLEARED, oldValue, null);
            propertyChangeSupport.firePropertyChange(DIRECTIONAL_ARROW, oldValue, showDirectionalArrow());
        }
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

    @PostRemove
    private void onDelete()
    {
        // Need to unregister as a PCL on our parent
        if (parentDefinition != null) parentDefinition.removePropertyChangeListener(this);
    }

    @PostLoad
    private void onLoad()
    {
        // Ok, this is sort of gross.  We have an issue with hibernate instantiating multiple instances of
        // CLD objects.  Normally, this would not be a problem, but since we rely heavily on property change
        // listeners, we have to ensure that we only have a single "master" instance in memory.
        // So, here, as the CLD is loaded and if it has a parent link, we need to replace that link with the
        // reference to the Global CLD maintained by the SmartTrackManager.  This ensures that when the Global
        // CLD is updated via the preferences panel, that the property change events are properly received by
        // this child's instance.

        // TODO: SmartTracks: We still have a problem where the references in the STM are potentially different
        // than those on the map objects.  We need to correlate these somehow and ensure that the values
        // returned from MapObject.getCLD() are the same references as managed by the STM.

        if (parentDefinition != null)
        {
            if (smartTrackManager != null)
            {
                final IConnectingLineDefinition globalCLD = smartTrackManager.getGlobalConnectingLineDefinition();
                if (!globalCLD.getId().equals(parentDefinition.getId()))
                {
                    logger.error("Parent CLD and Global CLD are not the same!");
                } else
                {
                    parentDefinition = globalCLD;
                    parentDefinition.addPropertyChangeListener(this);
                }
            } else
            {
                logger.debug("No services, no PCL");
            }
        }
    }
}
