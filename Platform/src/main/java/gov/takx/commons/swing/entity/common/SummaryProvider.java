package gov.takx.commons.swing.entity.common;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.lang.ColorUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.UIManager;

import java.awt.Color;

/**
 * A provider for "Summary" column data for {@link CommonEntityDefinition} instances. By default, combines the entity name
 * and details block into a single HTML block.
 *
 * @param <E> the type of entities contained in the corresponding entity model
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class SummaryProvider<E>
{
    private static final String DETAILS_COLOR_HEX = ColorUtils.colorToHexRGB(UIManager.getColor("Label.disabledForeground") != null ? UIManager.getColor("Label.disabledForeground") : Color.gray);

    private final CommonEntityDefinition<E> entityDefinition;

    /**
     * Construct a new renderer, using the given model to create the text for entities.
     *
     * @param entityDefinition the entity model
     */
    public SummaryProvider(@Nonnull CommonEntityDefinition<E> entityDefinition)
    {
        this.entityDefinition = entityDefinition;
    }

    /**
     * @param entity the model object to create name HTML for
     * @return the block of HTML containing the name, <i>not</i> including {@code <html>} tags
     */
    @Nullable
    public String createNameHtml(@Nonnull E entity)
    {
        return entityDefinition.getName(entity);
    }

    /**
     * Creates the HTML string used for the summary column. By default, this is the value from {@link #createNameHtml(Object)}
     * appended with the value from {@link #createDetailsHtml(Object)}, and wrapped in an HTML tag.
     *
     * @param entity the model object to create summary column HTML for
     * @return the summary column HTML for the given object, including {@code <html>} tags
     */
    @Nullable
    public String createSummaryHtml(@Nonnull E entity)
    {
        var nameHtml = createNameHtml(entity);
        var detailsHtml = createDetailsHtml(entity);
        var summaryHtml = nameHtml != null ? (nameHtml + (detailsHtml != null ? "<br/><span style='color:#" + DETAILS_COLOR_HEX + ";'>" + detailsHtml + "</span>" : "")) : detailsHtml;
        // We use nowrap because table row heights don't adapt well to dynamic wrapping
        return summaryHtml != null ? "<html><div style='white-space:nowrap'>" + summaryHtml + "</div></html>" : null;
    }

    /**
     * Creates the detail HTML string for a model object.
     *
     * @param entity the model object to create details HTML for
     * @return the details HTML for the given object, <i>not</i> including {@code <html>} tags
     */
    @Nullable
    public String createDetailsHtml(@Nonnull E entity)
    {
        return entityDefinition.getDetailsHtml(entity);
    }
}
