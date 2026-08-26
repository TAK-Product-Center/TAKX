package gov.takx.core.kml;

import de.micromata.opengis.kml.v_2_2_0.BalloonStyle;
import de.micromata.opengis.kml.v_2_2_0.Data;
import de.micromata.opengis.kml.v_2_2_0.ExtendedData;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.SchemaData;
import de.micromata.opengis.kml.v_2_2_0.SimpleData;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles generation of, and entity reference expansion within, the balloon text for a KML Feature.  If a
 * Feature doesn't specify a balloon style, then a default balloon text will be generated for it.
 *
 * @since 5.5
 */
public class BalloonTextBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Pattern to match entity references
    private static final Pattern ENTITY_REF = Pattern.compile("\\$\\[(.*?)]");

    private static final String DATA_TABLE_START = "<p/><table border=\"1\" padding=\"1\">";

    // Map of entity reference field names to a function to get that field from a Feature
    private static final Map<String, Function<Feature, String>> FIELD_EXTRACTORS = Map.of(
            "address", Feature::getAddress,
            "description", Feature::getDescription,
            "id", Feature::getId,
            "geDirections", f -> "", // No support for driving directions
            "name", Feature::getName,
            "snippet", Feature::getSnippetd,
            "Snippet", f -> {
                final var snippet = f.getSnippet();
                return snippet == null ? "" : snippet.getValue();
            });

    private final Feature feature;
    private final BalloonStyle balloonStyle;
    private final KmlModel kmlModel;

    /**
     * @param kmlModel KML Model containing the feature to process
     * @param feature KML Feature to process
     * @param balloonStyle BalloonStyle to use, if {@code null} a default balloon text will be generated
     */
    public BalloonTextBuilder(@Nonnull KmlModel kmlModel, @Nonnull Feature feature, @Nullable BalloonStyle balloonStyle)
    {
        this.kmlModel = kmlModel;
        this.feature = feature;
        this.balloonStyle = balloonStyle;
    }

    /**
     * Get the balloon text for the configured feature.  If no balloon style can be found for the feature,
     * then a default style will be created which shows the name, description, and a table containing any extended
     * data it has defined.  Any field/data/schema references in the balloon text will have been expanded to their
     * appropriate value.
     *
     * @return balloon text
     */
    @Nonnull
    public String getBalloonText()
    {
        final String balloonStyleText = balloonStyle != null ? balloonStyle.getText() : null;
        logger.debug("style text: {}", balloonStyleText);

        // If we didn't get text from a style, then generate a default
        return expandEntityReferences(balloonStyleText != null ? balloonStyleText : buildDefaultBalloonText());
    }

    /**
     * @return the given balloon text with all entity references expanded
     */
    private String expandEntityReferences(String text)
    {
        final var sb = new StringBuilder();

        final var matcher = ENTITY_REF.matcher(text);
        while (matcher.find())
        {
            final String reference = matcher.group(1);
            final String resolvedValue = resolveEntityReference(reference);
            final String replacement = resolvedValue != null ? resolvedValue : reference;
            logger.debug("ref = '{}' => '{}'", reference, replacement);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resolve an entity reference. The reference can be in one of these forms:
     * <pre>
     *  FeatureField
     *  DataField
     *  DataField/displayName
     *  SchemaName/SchemaField
     *  SchemaName/SchemaField/displayName
     *  </pre>
     *
     * @param reference entity reference to resolve
     *
     * @return the replacement string for the entity, or {@code null} if no replacement can be found
     * @see <a href="https://developers.google.com/kml/documentation/extendeddata">the KML Extended Data spec</a>
     */
    @Nullable
    private String resolveEntityReference(String reference)
    {
        logger.debug("resolve '{}'", reference);

        // Process fields defined directly on the feature
        if (FIELD_EXTRACTORS.containsKey(reference)) return FIELD_EXTRACTORS.get(reference).apply(feature);

        // We have to have extended data in order to resolve any other reference
        final var extendedData = feature.getExtendedData();
        if (extendedData == null) return null;

        final boolean wantDisplayName = reference.endsWith("/displayName");
        if (wantDisplayName) reference = reference.substring(0, reference.length() - "/displayName".length());

        final var parts = reference.split("/");

        // If we have 1 part, then it is a Data reference, if 2, then it is a SchemaData reference
        if (parts.length == 1)
        {
            return expandDataReference(extendedData, parts[0], wantDisplayName);
        } else if (parts.length == 2)
        {
            return expandSchemaDataReference(extendedData, parts[0], parts[1], wantDisplayName);
        }

        return null; // malformed or unresolved reference
    }

    /**
     * @return the value of a referenced {@link Data} field, or {@code null}
     */
    @Nullable
    private String expandDataReference(ExtendedData extendedData, String dataFieldName, boolean wantDisplayName)
    {
        for (var data : extendedData.getData())
        {
            if (dataFieldName.equals(data.getName()))
            {
                if (wantDisplayName)
                {
                    final String displayName = data.getDisplayName();
                    return !StringUtils.isEmpty(displayName) ? displayName : data.getName();
                } else
                {
                    return data.getValue();
                }
            }
        }
        return null;
    }

    /**
     * @return the value of a referenced {@link SchemaData} field, or {@code null}
     */
    @Nullable
    private String expandSchemaDataReference(ExtendedData extendedData, String schemaName, String schemaFieldName, boolean wantDisplayName)
    {
        for (var schemaData : extendedData.getSchemaData())
        {
            final var schema = kmlModel.getSchema(schemaData.getSchemaUrl());
            if (schema == null || !schemaName.equals(schema.getName())) continue;

            // If we're looking for the "displayName", then we search the schema, otherwise the SimpleData on the Feature
            if (wantDisplayName)
            {
                // Search schema fields
                for (var simpleField : schema.getSimpleField())
                {
                    if (schemaFieldName.equals(simpleField.getName())) return simpleField.getDisplayName();
                }
            } else
            {
                // Search SimpleData fields
                for (SimpleData simpleData : schemaData.getSimpleData())
                {
                    if (schemaFieldName.equals(simpleData.getName())) return simpleData.getValue();
                }
            }
        }
        return null;
    }

    /**
     * @return the default balloon text for a feature, which includes the name, description, and a table of any extended data values
     */
    @Nonnull
    private String buildDefaultBalloonText()
    {
        final var sb = new StringBuilder();

        final String name = feature.getName();
        final String description = feature.getDescription();

        if (!StringUtils.isEmpty(name)) sb.append("<b>").append(name).append("</b>");

        if (!StringUtils.isEmpty(description))
        {
            // Some KML files don't include "https:" before an image link, like "<img src="//developers.google.com/kml/documentation/images/googleSample.png">"
            // So, fix those up.
            final String fixedDescription = description.replaceAll("<img src=\"//", "<img src=\"https://");
            sb.append("<br/>").append(fixedDescription);
        }

        final var extendedData = feature.getExtendedData();
        if (extendedData != null)
        {
            final var dataList = extendedData.getData();
            if (!dataList.isEmpty()) addDefaultData(sb, dataList);

            final var schemaDataList = extendedData.getSchemaData();
            if (!schemaDataList.isEmpty()) addDefaultSchemaData(sb, schemaDataList);
        }

        return sb.toString();
    }

    /**
     * Add a table of references to the given {@code dataList}.
     */
    private void addDefaultData(StringBuilder sb, List<Data> dataList)
    {
        sb.append(DATA_TABLE_START);
        dataList.forEach(data -> {
            final String name = data.getName();
            final String value = data.getValue();

            if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(value))
            {
                sb.append("<tr><td>$[").append(name).append("/displayName]</td><td>").append(StringEscapeUtils.escapeHtml4(value)).append("</td></tr>");
            }
        });
        sb.append("</table>");
    }

    /**
     * Add a table of references to the given {@code schemaDataList}.
     */
    private void addDefaultSchemaData(StringBuilder sb, List<SchemaData> schemaDataList)
    {
        sb.append(DATA_TABLE_START);

        schemaDataList.forEach(schemaData -> {
            final var schema = kmlModel.getSchema(schemaData.getSchemaUrl());
            final String schemaName = schema != null ? schema.getName() : null;
            final boolean schemaResolved = schema != null && !StringUtils.isEmpty(schemaName);

            schemaData.getSimpleData().forEach(simpleData -> {
                final String name = simpleData.getName();
                final String value = simpleData.getValue();

                if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(value))
                {
                    sb.append("<tr><td>");

                    // Insert the schema name, if the schema can be resolved. Otherwise, just use the data name.
                    if (schemaResolved)
                    {
                        sb.append("$[").append(schemaName).append("/").append(name).append("/displayName]");
                    } else
                    {
                        sb.append(name);
                    }

                    sb.append("</td><td>").append(StringEscapeUtils.escapeHtml4(value)).append("</td></tr>");
                }
            });
        });
        sb.append("</table>");
    }
}
