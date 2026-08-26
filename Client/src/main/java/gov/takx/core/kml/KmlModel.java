package gov.takx.core.kml;

import com.atakmap.io.ZipVirtualFile;
import com.atakmap.map.layer.feature.FeatureSet;
import de.micromata.opengis.kml.v_2_2_0.BalloonStyle;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.ExtendedData;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LabelStyle;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.ListStyle;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.NamespaceFilterHandler;
import de.micromata.opengis.kml.v_2_2_0.PhotoOverlay;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.PolyStyle;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.Schema;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.StyleMap;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;
import de.micromata.opengis.kml.v_2_2_0.StyleState;
import de.micromata.opengis.kml.v_2_2_0.SubStyle;
import de.micromata.opengis.kml.v_2_2_0.gx.MultiTrack;
import de.micromata.opengis.kml.v_2_2_0.gx.Track;
import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Model representing a parsed KML/KMZ file.
 *
 * @see Kml
 * @since 5.5
 */
public final class KmlModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static
    {
        // KML URI missing from the JAK library
        NamespaceFilterHandler.addKml2SpecUri("http://earth.google.com/kml/2.2");
    }

    private File sourceFile;
    private final Kml kml;
    private final Document kmlDocument;
    private final List<Long> featureSetIds = new ArrayList<>();

    /**
     * @param sourceFile Source file from which the given {@code kml} object was created
     * @param kml Parsed form of the kml from the source file
     */
    private KmlModel(@Nonnull File sourceFile, @Nonnull Kml kml)
    {
        this.sourceFile = sourceFile;
        this.kml = kml;
        kmlDocument = kml.getFeature() instanceof Document document ? document : null;
    }

    /**
     * @return The source file from which this model was built, may be either a KML or KMZ file
     */
    @Nonnull
    public File sourceFile()
    {
        return sourceFile;
    }

    /**
     * Set the source file location for this model.  This is used to properly update the location of the "source",
     * when the original source is copied into VFS after the original has been parsed into this model.
     *
     * @param sourceFile New source file location
     */
    public void setSourceFile(File sourceFile)
    {
        this.sourceFile = sourceFile;
    }

    /**
     * @return The underlying JAK {@link Kml} model
     */
    @Nonnull
    public Kml kml()
    {
        return kml;
    }

    /**
     * @return {@code true} if the source document is a KMZ file
     */
    public boolean isKmz()
    {
        return sourceFile.getName().toLowerCase().endsWith(".kmz");
    }

    /**
     * @return The display name to use for this model, either the name property of the main {@link Document} if it
     *         specifies one, or the name of the source file.
     * @see #sourceFile()
     */
    @Nonnull
    public String displayName()
    {
        final String documentName = kmlDocument != null ? kmlDocument.getName() : null;
        return documentName != null ? documentName : sourceFile.getName();
    }

    /**
     * @return The list of {@link FeatureSet} ids that were created for this model
     */
    @Nonnull
    public List<Long> featureSetIds()
    {
        return Collections.unmodifiableList(featureSetIds);
    }

    /**
     * Add a {@link FeatureSet} id to our list.  We track the feature set ids so we can
     * easily remove all the TME features associated with this model when it is removed.
     */
    public void addFeatureSetId(long id)
    {
        featureSetIds.add(id);
    }

    /**
     * Visit all nodes of a given type within the model.
     *
     * @param type Type of node to visit
     * @param visitor Visitor to invoke on matching features
     * @param <T> Inferred type of feature
     */
    public <T extends Feature> void visitFeaturesOfType(@Nonnull Class<T> type, @Nonnull Consumer<T> visitor)
    {
        visitFeaturesOfType(type, visitor, List.of(kml.getFeature()));
    }

    /**
     * Add an attachment to a feature.
     *
     * @param feature Feature to add the attachment to
     * @param attachment Attachment to add
     * @see Feature#addToFeatureSimpleExtension(Object)
     */
    public void addAttachment(@Nonnull Feature feature, @Nonnull Object attachment)
    {
        feature.addToFeatureSimpleExtension(attachment);
    }

    /**
     * Get an attachment of the given type on the given feature.
     *
     * @param feature Feature to process
     * @param type Type of attachment to get
     * @return Attachment or {@code null} if no attachment of the given type is found
     * @param <T> Inferred attachment type
     */
    @Nullable
    public <T> T getAttachment(@Nonnull Feature feature, @Nonnull Class<T> type)
    {
        return feature.getFeatureSimpleExtension().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Visit the attachment of the given type on the given feature.
     *
     * @param feature Feature to process
     * @param type Type of attachment to find
     * @param visitor Visitor to invoke if the attachment is found
     * @param <T> Inferred attachment type
     */
    public <T> void visitAttachment(@Nonnull Feature feature, @Nonnull Class<T> type, @Nonnull Consumer<T> visitor)
    {
        final T attachment = getAttachment(feature, type);
        if (attachment != null) visitor.accept(attachment);
    }

    /**
     * Visit all nodes of a given type within the given feature list, recursing into any containers.
     *
     * @param type Type of node to visit
     * @param visitor Visitor to invoke on matching features
     * @param features List of features to process
     * @param <T> Inferred type of feature
     */
    private <T extends Feature> void visitFeaturesOfType(@Nonnull Class<T> type, @Nonnull Consumer<T> visitor, @Nonnull List<Feature> features)
    {
        for (Feature feature : features)
        {
            if (type.isInstance(feature))
            {
                visitor.accept(type.cast(feature));
            } else if (feature instanceof Document document)
            {
                visitFeaturesOfType(type, visitor, document.getFeature());
            } else if (feature instanceof Folder folder)
            {
                visitFeaturesOfType(type, visitor, folder.getFeature());
            }
        }
    }

    /**
     * Determine if the given KML feature has an associated geometry.
     *
     * @param feature Feature of interest
     * @return {@code true} if it has a non-empty geometry or bounding box
     */
    public boolean hasGeometry(@Nonnull Feature feature)
    {
        if (feature instanceof Placemark placemark)
        {
            return geometryIsNotEmpty(placemark.getGeometry());
        } else if (feature instanceof GroundOverlay groundOverlay)
        {
            return groundOverlay.getLatLonBox() != null;
        } else if (feature instanceof PhotoOverlay photoOverlay)
        {
            return geometryIsNotEmpty(photoOverlay.getPoint());
        }
        return false;
    }

    /**
     * Get the geometry for a given Placemark, expanding any {@link MultiGeometry}.
     *
     * @param placemark Placemark of interest
     * @return List of {@link Geometry} elements
     */
    public List<Geometry> getGeometry(@Nonnull Placemark placemark)
    {
        final List<Geometry> geometries = new ArrayList<>();

        expandGeometry(placemark.getGeometry(), geometries);
        return geometries;
    }

    /**
     * Add the given geometry to the {@code geometries} list, expanding any {@link MultiGeometry} instances recursively.
     *
     * @param geometry Initial geometry to process
     * @param geometries List to which to add discrete geometries
     */
    public void expandGeometry(@Nonnull Geometry geometry, @Nonnull List<Geometry> geometries)
    {
        if (geometry instanceof MultiGeometry multiGeometry)
        {
            multiGeometry.getGeometry().forEach(geom -> expandGeometry(geom, geometries));
        } else
        {
            geometries.add(geometry);
        }
    }

    /**
     * Sadly, the subclasses of {@link Geometry} contain the methods to get the coordinates of the geometry,
     * so we have to test types, and cast to obtain them.
     *
     * @return {@code true} if the given geometry is not empty
     */
    public boolean geometryIsNotEmpty(@Nullable Geometry geometry)
    {
        if (geometry == null) return false;

        if (geometry instanceof Point point)
        {
            return !point.getCoordinates().isEmpty();
        } else if (geometry instanceof Polygon polygon)
        {
            final var boundary = polygon.getOuterBoundaryIs();
            if (boundary == null) return false;

            final var linearRing = boundary.getLinearRing();
            if (linearRing == null) return false;

            return !linearRing.getCoordinates().isEmpty();
        } else if (geometry instanceof LineString lineString)
        {
            return !lineString.getCoordinates().isEmpty();
        } else if (geometry instanceof LinearRing linearRing)
        {
            return !linearRing.getCoordinates().isEmpty();
        } else if (geometry instanceof Track track)
        {
            return !track.getCoord().isEmpty();
        } else if (geometry instanceof MultiTrack multiTrack)
        {
            for (var track : multiTrack.getTrack())
            {
                if (!track.getCoord().isEmpty()) return true;
            }
        } else if (geometry instanceof MultiGeometry multiGeometry)
        {
            for (var geom : multiGeometry.getGeometry())
            {
                if (geometryIsNotEmpty(geom)) return true;
            }
        }
        return false;
    }

    /**
     * Find the requested style type for the given feature.  This may be directly defined on the feature, or referenced
     * via a style URL.
     *
     * @param feature Feature to process
     * @param <T> Inferred sub-style type
     * @return Applicable {@link SubStyle}, or {@code null} if no such style can be found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends SubStyle> T findStyleOfType(@Nonnull Feature feature, @Nonnull Class<T> styleType)
    {
        final String styleUrl = feature.getStyleUrl();
        if (styleUrl != null)
        {
            final var styleSelector = getStyleSelector(styleUrl);
            if (styleSelector != null) return (T) findStyleOfType(styleSelector, styleType);
        } else
        {
            for (var styleSelector : feature.getStyleSelector())
            {
                final var balloonStyle = findStyleOfType(styleSelector, styleType);
                if (balloonStyle != null) return (T) balloonStyle;
            }
        }
        return null;
    }

    /**
     * @return The appropriate sub-style extracted from the given {@code styleSelector}
     */
    @Nullable
    private <T extends SubStyle> SubStyle findStyleOfType(@Nonnull StyleSelector styleSelector, Class<T> styleType)
    {
        if (styleSelector instanceof Style style)
        {
            // Each style type has to be obtained by a distinct getter method
            if (styleType == BalloonStyle.class) return style.getBalloonStyle();
            if (styleType == IconStyle.class) return style.getIconStyle();
            if (styleType == ListStyle.class) return style.getListStyle();
            if (styleType == LabelStyle.class) return style.getLabelStyle();
            if (styleType == LineStyle.class) return style.getLineStyle();
            if (styleType == PolyStyle.class) return style.getPolyStyle();
        } else if (styleSelector instanceof StyleMap styleMap)
        {
            for (var pair : styleMap.getPair())
            {
                // We only support NORMAL style, parity with ATAK/WTK
                if (StyleState.NORMAL == pair.getKey())
                {
                    final String styleUrl = pair.getStyleUrl();
                    if (styleUrl != null)
                    {
                        final var pairStyleSelector = getStyleSelector(styleUrl);
                        if (pairStyleSelector != null) return findStyleOfType(pairStyleSelector, styleType);
                    } else
                    {
                        return findStyleOfType(pair.getStyleSelector(), styleType);
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param schemaUrl Schema reference in the form {@code #<schemaId>}, or {@code null}
     * @return The {@link Schema} referenced by the given url, or {@code null} if the given url is {@code null} or the
     *         referenced schema is not found
     * @see Document#getSchema()
     */
    @Nullable
    public Schema getSchema(@Nullable String schemaUrl)
    {
        final String schemaId = schemaUrl != null && schemaUrl.length() > 1 ? schemaUrl.substring(1) : null;

        return kmlDocument != null && schemaId != null
                ? kmlDocument.getSchema().stream().filter(s -> schemaId.equals(s.getId())).findFirst().orElse(null)
                : null;
    }

    /**
     * @param styleUrl Style reference in the form {@code #<styleId>}, or {@code null}
     * @return The {@link StyleSelector} referenced by the given url, or {@code null} if the given url is {@code null}
     *         or the referenced style selector is not found
     * @see Document#getStyleSelector()
     */
    @Nullable
    public StyleSelector getStyleSelector(@Nullable String styleUrl)
    {
        final String styleId = styleUrl != null && styleUrl.length() > 1 ? styleUrl.substring(1) : null;

        return kmlDocument != null && styleId != null
                ? kmlDocument.getStyleSelector().stream().filter(s -> styleId.equals(s.getId())).findFirst().orElse(null)
                : null;
    }

    /**
     * Parse a given file (either a KML or KMZ) and produce a {@link KmlModel}.
     *
     * @param file File to parse
     * @return new model
     * @throws IOException If parsing fails for any reason
     */
    @UseClientClassLoader
    @Nonnull
    public static KmlModel parseModel(@Nonnull File file) throws IOException
    {
        logger.debug("Parse KML: {}", file);
        final Kml kml;
        try
        {
            // If it's a KMZ, then find and open the doc.kml entry within the zip file
            if (StringUtils.endsWithIgnoreCase(file.getName(), "kmz"))
            {
                var zipFile = new ZipVirtualFile(file);
                final var doc = findKmlDoc(zipFile);
                if (doc == null)
                {
                    final String message = "KMZ File '" + file + "' does not contain a .kml file";
                    logger.warn(message);
                    throw new IOException(message);
                }

                try (var is = doc.openStream())
                {
                    kml = Kml.unmarshal(is);
                }
            } else
            {
                try (var is = Files.newInputStream(file.toPath()))
                {
                    kml = Kml.unmarshal(is);
                }
            }
        } catch (IOException e)
        {
            throw e;
        } catch (Exception e)
        {
            throw new IOException("Failed to parse '" + file + "'", e);
        }

        if (logger.isDebugEnabled() && kml.getFeature() instanceof Document document) dumpDocument(document);

        return new KmlModel(file, kml);
    }

    /**
     * Find the KML document within the KMZ to process.  Default to {@code doc.kml}, and if that is not found,
     * look for the first file with a name ending in {@code .kml}.
     */
    @Nullable
    private static ZipVirtualFile findKmlDoc(@Nonnull ZipVirtualFile zipFile) throws IOException
    {
        final var docFiles = zipFile.listFiles((dir, name) -> "doc.kml".equals(name));
        if (docFiles != null && docFiles.length > 0) return (ZipVirtualFile) docFiles[0];

        final var kmlFiles = zipFile.listFiles((dir, name) -> name.endsWith(".kml"));
        if (kmlFiles != null && kmlFiles.length > 0) return (ZipVirtualFile) kmlFiles[0];
        return null;
    }

    private static void dumpDocument(Document document)
    {
        logger.debug("DOC: id {}, name '{}', description '{}'", document.getId(), document.getName(), document.getDescription());

        dumpSchemas(document.getSchema());
        document.getFeature().forEach(feature -> {
            final String description = feature.getDescription() != null ? feature.getDescription().trim() : null;
            logger.debug("   feature {}: id {}, name '{}', description '{}'", feature.getClass().getSimpleName(), feature.getId(), feature.getName(), description);
            if (feature instanceof Folder folder) dumpFolder(folder, "   ");
        });
    }

    private static void dumpSchemas(List<Schema> schemas)
    {
        schemas.forEach(schema -> {
            logger.debug("Schema id='{}' name='{}'", schema.getId(), schema.getName());
            schema.getSimpleField().forEach(field -> logger.debug("   field name='{}', type={}, displayName='{}'", field.getName(), field.getType(), field.getDisplayName()));
        });
    }

    private static void dumpFolder(Folder folder, String prefix)
    {
        int count = 0;
        for (var feature : folder.getFeature())
        {
            final String description = feature.getDescription() != null ? feature.getDescription().trim() : null;
            logger.debug("{}feature {}: id {}, name '{}', description '{}'", prefix, feature.getClass().getSimpleName(), feature.getId(), feature.getName(), description);
            if (feature instanceof Folder folder2) dumpFolder(folder2, prefix + "   ");
            dumpExtendedData(feature.getExtendedData());
            if (++count > 4) break;
        }
    }

    private static void dumpExtendedData(ExtendedData extendedData)
    {
        if (extendedData == null) return;
        extendedData.getData().forEach(d -> logger.debug("      DATA: {} = {}", d.getDisplayName(), d.getValue()));
        extendedData.getSchemaData().forEach(schemaData -> schemaData.getSimpleData().forEach(sd -> logger.debug("      SIMPLE_DATA: {} = {}", sd.getName(), sd.getValue())));
    }
}
