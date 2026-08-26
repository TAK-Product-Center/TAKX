package gov.tak.core.map.layer.feature.datastore.memory;

import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.FeatureDefinition3;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;

import java.util.Objects;

/**
 * Internal class to store {@link com.atakmap.map.layer.feature.Feature} properties.
 *
 * @since 5.2
 */
final class Feature implements IDataStoreEntity, FeatureDefinition3
{
    private final Long id;
    private long version;
    private boolean visible;
    private final long featureSetId;
    private String name;
    private Geometry geometry;
    private Style style;
    private AttributeSet attributes;
    private AltitudeMode altitudeMode;
    private double extrude;
    private final long timestamp;
    private com.atakmap.map.layer.feature.Feature takFeature;

    /**
     * @param id           the unique id, see {@link com.atakmap.map.layer.feature.Feature#getId()}
     * @param version      the version, see {@link com.atakmap.map.layer.feature.Feature#getVersion()}
     * @param visible      the visible flag
     * @param featureSetId the unique id of the parent {@link FeatureSet}, see {@link com.atakmap.map.layer.feature.Feature#getFeatureSetId()}
     * @param name         the feature name, see {@link com.atakmap.map.layer.feature.Feature#getName()}
     * @param geometry     the geometry object, see {@link com.atakmap.map.layer.feature.Feature#getGeometry()}
     * @param style        the style object, see {@link com.atakmap.map.layer.feature.Feature#getStyle()}
     * @param attributes   the attribute set, see {@link com.atakmap.map.layer.feature.Feature#getAttributes()}
     * @param altitudeMode the altitude mode, see {@link com.atakmap.map.layer.feature.Feature#getAltitudeMode()}
     * @param extrude      the extrude value, see {@link com.atakmap.map.layer.feature.Feature#getExtrude()}
     * @param timestamp    the timestamp, see {@link com.atakmap.map.layer.feature.Feature#getTimestamp()}
     */
    Feature(Long id, long version, boolean visible, long featureSetId, String name, Geometry geometry, Style style, AttributeSet attributes, AltitudeMode altitudeMode, double extrude, long timestamp)
    {
        this.id = id;
        this.version = version;
        this.visible = visible;
        this.featureSetId = featureSetId;
        this.name = name;
        this.geometry = geometry;
        this.style = style;
        this.attributes = attributes;
        this.altitudeMode = altitudeMode;
        this.extrude = extrude;
        this.timestamp = timestamp;
    }

    @Override
    public AltitudeMode getAltitudeMode()
    {
        return altitudeMode;
    }

    /** @since 5.5.1 */
    public void setAltitudeMode(AltitudeMode altitudeMode)
    {
        this.altitudeMode = altitudeMode;
        version++;
        takFeature = null;
    }

    @Override
    public double getExtrude()
    {
        return extrude;
    }

    /** @since 5.5.1 */
    public void setExtrude(double extrude)
    {
        this.extrude = extrude;
        version++;
        takFeature = null;
    }

    @Override
    public String getName()
    {
        return name;
    }

    /** @since 5.5.1 */
    public void setName(String name)
    {
        this.name = name;
        version++;
        takFeature = null;
    }

    @Override
    public int getGeomCoding()
    {
        return GEOM_ATAK_GEOMETRY;
    }

    @Override
    public int getStyleCoding()
    {
        return STYLE_ATAK_STYLE;
    }

    @Override
    public AttributeSet getAttributes()
    {
        return attributes;
    }

    /** @since 5.5.1 */
    public void setAttributes(AttributeSet attributes)
    {
        this.attributes = new AttributeSet(attributes);
        version++;
        takFeature = null;
    }

    @Override
    public long getTimestamp()
    {
        return timestamp;
    }

    /**
     * @return the {@link Geometry} object for this Feature
     */
    @Override
    public Geometry getRawGeometry()
    {
        return geometry;
    }

    /** @since 5.5.1 */
    public void setGeometry(Geometry geometry)
    {
        this.geometry = geometry;
        version++;
        takFeature = null;
    }

    /**
     * @return the {@link Style} object for this Feature
     */
    @Override
    public Style getRawStyle()
    {
        return style;
    }

    /** @since 5.5.1 */
    public void setStyle(Style style)
    {
        this.style = style;
        version++;
        takFeature = null;
    }

    /**
     * {@inheritDoc}
     *
     * @return a new {@link com.atakmap.map.layer.feature.Feature} instance based on this definition.
     */
    @Override
    public synchronized com.atakmap.map.layer.feature.Feature get()
    {
        return takFeature != null
                ? takFeature
                : (takFeature = new com.atakmap.map.layer.feature.Feature(featureSetId, id, name, geometry, style, attributes, altitudeMode, extrude, timestamp, version));
    }

    @Override
    public Long id()
    {
        return id;
    }

    @Override
    public long version()
    {
        return version;
    }

    @Override
    public boolean visible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
        version++;
        takFeature = null;
    }

    public long featureSetId()
    {
        return featureSetId;
    }

    public String name()
    {
        return name;
    }

    public Geometry geometry()
    {
        return geometry;
    }

    public Style style()
    {
        return style;
    }

    public AttributeSet attributes()
    {
        return attributes;
    }

    public AltitudeMode altitudeMode()
    {
        return altitudeMode;
    }

    public double extrude()
    {
        return extrude;
    }

    public long timestamp()
    {
        return timestamp;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) return false;
        var that = (Feature) obj;
        return Objects.equals(id, that.id) &&
                version == that.version &&
                visible == that.visible &&
                featureSetId == that.featureSetId &&
                Objects.equals(name, that.name) &&
                Objects.equals(geometry, that.geometry) &&
                Objects.equals(style, that.style) &&
                Objects.equals(attributes, that.attributes) &&
                altitudeMode == that.altitudeMode &&
                Double.doubleToLongBits(extrude) == Double.doubleToLongBits(that.extrude) &&
                timestamp == that.timestamp;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, version, visible, featureSetId, name, geometry, style, attributes, altitudeMode, extrude, timestamp);
    }

    @Override
    public String toString()
    {
        return "Feature[" +
                "id=" + id + ", " +
                "version=" + version + ", " +
                "visible=" + visible + ", " +
                "featureSetId=" + featureSetId + ", " +
                "name=" + name + ", " +
                "geometry=" + geometry + ", " +
                "style=" + style + ", " +
                "attributes=" + attributes + ", " +
                "altitudeMode=" + altitudeMode + ", " +
                "extrude=" + extrude + ", " +
                "timestamp=" + timestamp + ']';
    }
}
