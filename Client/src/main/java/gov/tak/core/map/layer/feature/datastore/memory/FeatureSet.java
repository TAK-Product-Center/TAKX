package gov.tak.core.map.layer.feature.datastore.memory;

import java.util.Objects;

/**
 * Internal class for storing {@link com.atakmap.map.layer.feature.FeatureSet} properties.
 *
 * @since 5.2 (converted to class in 5.5.1)
 */
final class FeatureSet implements IDataStoreEntity
{
    private final Long id;
    private final String provider;
    private final String type;

    private boolean visible;
    private String name;
    private double minGsd;
    private double maxGsd;
    private long version;
    private com.atakmap.map.layer.feature.FeatureSet takFeatureSet;

    /**
     * @param id       the unique id
     * @param version  the version
     * @param visible  {@code true} if visible, otherwise {@code false}
     * @param provider the provider
     * @param type     the type
     * @param name     the name
     * @param minGsd   the minimum GSD resolution
     * @param maxGsd   the maximum GSD resolution
     */
    FeatureSet(Long id, long version, boolean visible, String provider, String type, String name, double minGsd, double maxGsd, com.atakmap.map.layer.feature.FeatureSet takFeatureSet)
    {
        this.id = id;
        this.version = version;
        this.visible = visible;
        this.provider = provider;
        this.type = type;
        this.name = name;
        this.minGsd = minGsd;
        this.maxGsd = maxGsd;
        this.takFeatureSet = takFeatureSet;
    }

    /**
     * Constructs a new instance, copying properties from the given {@link com.atakmap.map.layer.feature.FeatureSet}.
     *
     * @param id the new id
     * @param takFeatureSet the feature set to copy
     */
    FeatureSet(Long id, com.atakmap.map.layer.feature.FeatureSet takFeatureSet)
    {
        this(id, takFeatureSet.getVersion(), true, takFeatureSet.getProvider(), takFeatureSet.getType(), takFeatureSet.getName(), takFeatureSet.getMinResolution(), takFeatureSet.getMaxResolution(), takFeatureSet);
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
        takFeatureSet = null;
    }

    public String provider()
    {
        return provider;
    }

    public String type()
    {
        return type;
    }

    public String name()
    {
        return name;
    }

    /** @since 5.5.1 */
    public void setName(String name)
    {
        this.name = name;
        version++;
        takFeatureSet = null;
    }

    public double minGsd()
    {
        return minGsd;
    }

    /** @since 5.5.1 */
    public void setMinGsd(double minGsd)
    {
        this.minGsd = minGsd;
        version++;
        takFeatureSet = null;
    }

    public double maxGsd()
    {
        return maxGsd;
    }

    /** @since 5.5.1 */
    public void setMaxGsd(double maxGsd)
    {
        this.maxGsd = maxGsd;
        version++;
        takFeatureSet = null;
    }

    public synchronized com.atakmap.map.layer.feature.FeatureSet get()
    {
        return takFeatureSet != null
                ? takFeatureSet
                : (takFeatureSet = new com.atakmap.map.layer.feature.FeatureSet(id, provider, type, name, minGsd, maxGsd, version));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) return false;
        var that = (FeatureSet) obj;
        return Objects.equals(id, that.id) &&
                version == that.version &&
                visible == that.visible &&
                Objects.equals(provider, that.provider) &&
                Objects.equals(type, that.type) &&
                Objects.equals(name, that.name) &&
                Double.doubleToLongBits(minGsd) == Double.doubleToLongBits(that.minGsd) &&
                Double.doubleToLongBits(maxGsd) == Double.doubleToLongBits(that.maxGsd) &&
                Objects.equals(takFeatureSet, that.takFeatureSet);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, version, visible, provider, type, name, minGsd, maxGsd, takFeatureSet);
    }

    @Override
    public String toString()
    {
        return "FeatureSet[" +
                "id=" + id + ", " +
                "version=" + version + ", " +
                "visible=" + visible + ", " +
                "provider=" + provider + ", " +
                "type=" + type + ", " +
                "name=" + name + ", " +
                "minGsd=" + minGsd + ", " +
                "maxGsd=" + maxGsd + ", " +
                "takFeatureSet=" + takFeatureSet + ']';
    }
}
