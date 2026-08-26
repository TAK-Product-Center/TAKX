package gov.takx.dataservices.persist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.takx.api.annotation.Internal;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.MediaKey;
import gov.takx.api.media.MediaProperties;
import gov.takx.api.media.factory.AMediaBuilder;
import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Transient;
import org.hibernate.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a named handle for an audio, video, or image file or stream
 *
 * @since 2.8
 */
@SuppressWarnings("JpaDataSourceORMInspection")
@Internal
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // The default strategy but adding for clarification
@DiscriminatorColumn(
        name = "mediaType",
        discriminatorType = DiscriminatorType.STRING)
public abstract class AMedia<M extends IMedia, B extends AMediaBuilder<M, B>> implements IMedia
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String keyOfSource;

    @Column(length = Length.LONG32)
    private URI sourceUri;
    private String thumbnailURL;
    private Long lastModified;
    private Long fileSize;
    private String info;
    private Long time;
    private Integer color;
    @Embedded
    private RaptorLocation location;

    @Column(length = Length.LONG32)
    private String jsonMetaData;

    @Column(length = Length.LONG32)
    private String jsonOptions;

    @Transient
    protected final PrioritizedPropertyChangeSupport propertyChangeSupport = new PrioritizedPropertyChangeSupport(this);

    @Transient
    private Map<String, String> metaData;

    @Transient
    private Map<String, String> options;

    private String format;

    // Used across all media so make them static
    private static final TypeReference<HashMap<String, String>> typeRef = new TypeReference<>()
    {
    };

    private static final ObjectMapper mapper = new ObjectMapper();

    @Transient
    private B mediaBuilder;

    /**
     * No-arg constructor needed for hibernate
     */
    protected AMedia()
    {
    }

    protected AMedia(@Nonnull B mediaBuilder)
    {
        this.mediaBuilder = mediaBuilder;

        name = mediaBuilder.getName();
        keyOfSource = mediaBuilder.getKeyOfSource();
        sourceUri = mediaBuilder.getSourceUri();

        if (sourceUri == null) throw new IllegalArgumentException("'sourceUri' must not be null");

        info = mediaBuilder.getInfo();
        thumbnailURL = mediaBuilder.getThumbnailURL();
        // Should be a better way to do this than a cast
        location = (RaptorLocation) mediaBuilder.getLocation();

        // Try to extract the last modified and size from the file
        if (!isFileUri(sourceUri) || !extractFileData(new File(sourceUri)))
        {
            lastModified = mediaBuilder.getLastModified();
            fileSize = mediaBuilder.getFileSize();
        }

        metaData = mediaBuilder.getMetaData();
        jsonMetaData = packJson(metaData);
    }

    /**
     * Determine if the given URI represents a local file.
     * @since 5.0
     */
    protected boolean isFileUri(URI uri)
    {
        return uri != null && uri.getScheme().equals("file");
    }

    /**
     * @return Our media builder
     * @since 3.0
     */
    @Nullable
    protected B getMediaBuilder()
    {
        return mediaBuilder;
    }

    /**
     * Populate the file size and last modified fields from a local file.
     */
    private boolean extractFileData(File file)
    {
        if (file.exists())
        {
            final Path path = file.toPath();
            try
            {
                setFileSize(Files.size(path));
                setLastModified(Files.getLastModifiedTime(path).toMillis());
                return true;
            } catch (IOException e)
            {
                logger.warn("Can't read attributes of local file {}", path);
            }
        }
        return false;
    }

    protected void setName(String name)
    {
        this.name = name;
    }

    @Override
    @Nonnull
    public String getName()
    {
        return name;
    }

    @Override
    @Nonnull
    public String getKeyOfSource()
    {
        return keyOfSource;
    }

    @Override
    @Nonnull
    public String getKey()
    {
        return MediaKey.makeKey(keyOfSource, name);
    }

    @Override
    @Nullable
    public URI getSourceUri()
    {
        return sourceUri;
    }

    @Override
    public void setSourceUri(@Nonnull URI sourceUri)
    {
        if (isFileUri(sourceUri))
        {
            final File file = new File(sourceUri);
            if (!file.exists()) throw new IllegalArgumentException("File must exist: " + file);

            extractFileData(file);
        } else
        {
            setFileSize(null);
            setLastModified(null);
        }

        propertyChangeSupport.updateAndFire(MediaProperties.Common.SOURCE_URI, this.sourceUri, sourceUri, () -> this.sourceUri = sourceUri);
    }

    @Override
    @Nullable
    public String getLocalFilePath()
    {
        return isFileUri(sourceUri) ? new File(sourceUri).getPath() : null;
    }

    @Override
    @Nullable
    public String getThumbnailURL()
    {
        return thumbnailURL;
    }

    @Override
    public void setThumbnailURL(@Nullable String thumbnailURL)
    {
        if (thumbnailURL != null)
        {
            try
            {
                //noinspection ResultOfObjectAllocationIgnored
                new URL(thumbnailURL);
            } catch (MalformedURLException e)
            {
                throw new IllegalArgumentException("Malformed URL", e);
            }
        }

        propertyChangeSupport.updateAndFire(MediaProperties.Common.THUMBNAIL_URL, this.thumbnailURL, thumbnailURL,
                () -> this.thumbnailURL = thumbnailURL);
    }

    @Override
    @Nullable
    public Long getLastModified()
    {
        return lastModified;
    }

    @Override
    public void setLastModified(@Nullable Long lastModified)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Common.LAST_MODIFIED, this.lastModified, lastModified,
                () -> this.lastModified = lastModified);
    }

    @Override
    @Nullable
    public Long getFileSize()
    {
        return fileSize;
    }

    @Override
    public void setFileSize(@Nullable Long fileSize)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Common.FILE_SIZE, this.fileSize, fileSize,
                () -> this.fileSize = fileSize);
    }

    @Override
    @Nullable
    public String getInfo()
    {
        return info;
    }

    @Override
    public void setInfo(@Nullable String info)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Common.INFO, this.info, info, () -> this.info = info);
    }

    @Override
    public void setLocation(@Nullable ILocation location)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Common.LOCATION, this.location, location,
                () -> this.location = (RaptorLocation) location);
    }

    @Override
    @Nullable
    public ILocation getLocation()
    {
        return location;
    }

    @Override
    public void setTime(@Nullable Long time)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Common.TIME, this.time, time,
                () -> this.time = time);
    }

    @Override
    @Nullable
    public Long getTime()
    {
        return time;
    }

    @Override
    public void setColor(@Nullable Color color)
    {
        final Integer newColorInt = color != null ? color.getRGB() : null;
        final Color oldColor = this.color != null ? new Color(this.color) : null;

        propertyChangeSupport.updateAndFire(MediaProperties.Common.COLOR, oldColor, color, () -> this.color = newColorInt);
    }

    @Override
    @Nullable
    public Color getColor()
    {
        return color == null ? null : new Color(color);
    }

    // Used by JPA
    @SuppressWarnings("unused")
    private void setJsonMetaData(String jsonMetaData)
    {
        this.jsonMetaData = jsonMetaData;
    }

    // Used by JPA
    @SuppressWarnings("unused")
    private String getJsonMetaData()
    {
        return jsonMetaData;
    }

    /**
     * Set the json representation for the options. Used by JPA for persistence.
     *
     * @param jsonOptions The json string representation for the options.
     * @since 5.0.0
     */
    @SuppressWarnings("unused")
    private void setJsonOptions(String jsonOptions)
    {
        this.jsonOptions = jsonOptions;
    }

    /**
     * Get the json for of the options. Used by jpa for persistence.
     *
     * @return The json representation of the options.
     * @since 5.0.0
     */
    @SuppressWarnings("unused")
    private String getJsonOptions()
    {
        return jsonOptions;
    }

    /**
     * Convert json representation into a Map<String, String>
     *
     * @param json The json string to be decoded
     * @return A map extracted from the json.
     * @since 5.0.0
     */
    private Map<String, String> unpackJson(String json)
    {
        Map<String, String> data = null;
        try
        {
            data = mapper.readValue(json, typeRef);
        } catch (IOException e)
        {
            logger.warn("Error loading media data", e);
        }
        return data;
    }

    /**
     * Pack a map into a json string for storage.
     *
     * @param map The map to be packed into json.
     * @return A String representing the map or null if the map is null.
     * @since 5.0.0
     */
    private String packJson(Map<String, String> map)
    {
        String json = null;
        if (map != null)
        {
            try
            {
                json = mapper.writeValueAsString(map);
            } catch (IOException e)
            {
                logger.error("Unable to save media data {} for {}", map, name);
            }
        }
        return json;
    }

    @Transient
    @Override
    @Nullable
    public Map<String, String> getMetaData()
    {
        // Load the metadata map since it's not persisted
        if (metaData == null && jsonMetaData != null)
        {
            metaData = unpackJson(jsonMetaData);
        }
        return metaData != null ? Collections.unmodifiableMap(metaData) : null;
    }

    @Transient
    @Override
    public void setMetaData(@Nullable Map<String, String> metaData)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Common.META_DATA, this.metaData, metaData,
                () -> this.metaData = metaData != null ? Map.copyOf(metaData) : null);

        jsonMetaData = packJson(metaData);
    }

    @Transient
    @Override
    public Map<String, String> getOptions()
    {
        if (options == null && jsonOptions != null)
        {
            options = unpackJson(jsonOptions);
        }
        return options;
    }

    @Transient
    @Override
    @SuppressWarnings("unchecked")
    public void setOptions(Map<String, String> options)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Common.OPTIONS, this.options, options,
                () -> this.options = options != null ? Map.copyOf(options) : null);

        jsonOptions = packJson(options);
    }

    @Override
    public String getFormat()
    {
        return format;
    }

    @Override
    public void setFormat(String format)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Common.FORMAT, this.format, format,
                () -> this.format = format);
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener propertyChangeListener)
    {
        propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener propertyChangeListener)
    {
        propertyChangeSupport.addPropertyChangeListener(checkPropertyName(property), propertyChangeListener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener propertyChangeListener)
    {
        propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener propertyChangeListener)
    {
        propertyChangeSupport.removePropertyChangeListener(checkPropertyName(property), propertyChangeListener);
    }

    private String checkPropertyName(String property)
    {
        if (!MediaProperties.ALL.contains(property)) throw new IllegalArgumentException("Invalid property " + property);
        return property;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof AMedia<?, ?> aMedia)) return false;

        if (!name.equals(aMedia.name)) return false;
        return keyOfSource.equals(aMedia.keyOfSource);
    }

    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + keyOfSource.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        // Used to display media in UIs
        return name;
    }
}
