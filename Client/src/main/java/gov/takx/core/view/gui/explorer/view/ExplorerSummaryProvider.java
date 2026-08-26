package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.explorer.IExplorable;
import gov.takx.commons.lang.ColorUtils;
import gov.takx.commons.swing.entity.SortFilterEntityTreeTableModel;
import gov.takx.commons.swing.entity.common.SummaryProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider for the summary column data in the explorer panel table. Adds extra features to the base class:
 * <ul>
 *     <li>Displaying the number of children and descendants of a given node. </li>
 *     <li>Returning custom text for unexpanded lazy nodes.</li>
 *     <li>Applying the Explorer text size scaling preference.</li>
 *     <li>Optionally embedding the icon into HTML.</li>
 * </ul>
 *
 * This class uses the tree model itself to calculate the values so that the totals actually
 * match what the user sees i.e. they do not include explorables that are hidden from the explorer. We use the technique of
 * embedding the icon into the HTML so that we can avoid having to create a custom JPanel renderer. This has the advantage
 * of allowing us to cache the entire contents of the cell, including the icon.
 *
 * @since 5.7
 */
@PseudoBean
public class ExplorerSummaryProvider extends SummaryProvider<IExplorable>
{
    private static final String NAME_HTML_FORMAT = "<span style='font-size:%s%%;color:%s'>%s</span>";
    private static final String TREE_HTML_FORMAT = """
            <table style='font-size:%s%%;color:%s' cellspacing='0' cellpadding='0'>
            <tr>
            <td>%s</td>
            <td>%s</td>
            </tr>
            </table>
            """;
    private static final String UNEXPANDED_LAZY_TEXT = "Lazy items not yet loaded";
    private static final Map<Icon, String> ICON_CACHE = new HashMap<>();
    private static BufferedImage ICON_CACHE_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    private final ExplorableEntityDefinition entityDefinition;
    private final LazyManager lazyManager;
    private final SortFilterEntityTreeTableModel<IExplorable> filteredExplorerModel;
    private boolean embedIcon;
    private int textScalingPercent = 100;

    /**
     * Construct a new instance using the given tree table model.
     *
     * @param lazyManager the manager for determining status of lazy nodes
     * @param filteredExplorerModel the filtered model, used for child counts
     * @param entityDefinition the entity definition
     */
    public ExplorerSummaryProvider(@Nonnull LazyManager lazyManager, @Nonnull SortFilterEntityTreeTableModel<IExplorable> filteredExplorerModel, @Nonnull ExplorableEntityDefinition entityDefinition)
    {
        super(entityDefinition);
        this.entityDefinition = entityDefinition;
        this.lazyManager = lazyManager;
        this.filteredExplorerModel = filteredExplorerModel;
    }

    @Nullable
    @Override
    public String createNameHtml(@Nonnull IExplorable entity)
    {
        var html = super.createNameHtml(entity);
        if (html == null) html = entity.getDisplayName();
        var color = entity.getDisplayNameColor() != null ? "#" + ColorUtils.colorToHexRGB(entity.getDisplayNameColor()) : "";
        html = NAME_HTML_FORMAT.formatted(textScalingPercent, color, html);
        return html;
    }

    /**
     * This implementation adds a child count to the details HTML. The calculation logic for this child count is not currently
     * settled, but for now: <p>
     * For non-lazy or expanded lazy nodes, it returns a count of the direct children, followed by a total descendants count. For
     * unexpanded lazy nodes it returns the {@link #UNEXPANDED_LAZY_TEXT} String.
     *
     * @param entity An explorable
     * @return The details HTML for the given explorable, <i>not</i> including {@code <html>} tags
     */
    @Nullable
    @Override
    public String createDetailsHtml(@Nonnull IExplorable entity)
    {
        if (ExplorerPanel.isTreeNodeType(entity, filteredExplorerModel))
        {
            String details;
            if (lazyManager.isLazyAndUnexpanded(entity))
            {
                details = UNEXPANDED_LAZY_TEXT;
            } else
            {
                var children = filteredExplorerModel.getChildCount(entity);

                details = children + " Item" + (children != 1 ? "s" : "");
                var descendants = filteredExplorerModel.getDescendantCount(entity);
                if (children > 0 && descendants != children)
                {
                    details += " (" + descendants + " Total)";
                }
            }
            return details;
        } else
        {
            return entity.getDetailsTextHtml();
        }
    }

    /**
     * @param entity the explorable to generate HTML for
     * @return the HTML to show in the explorer tree
     */
    public String getTreeHtml(@Nonnull IExplorable entity)
    {
        var html = super.createNameHtml(entity);
        if (html == null) html = entity.getDisplayName();
        var icon = embedIcon ? createIconHtml(entity) : "";
        var color = entity.getDisplayNameColor() != null ? "#" + ColorUtils.colorToHexRGB(entity.getDisplayNameColor()) : "";
        html = TREE_HTML_FORMAT.formatted(textScalingPercent, color, icon, html);
        return "<html>" + html + "</html>";
    }

    /**
     * @param embedIcon {@code true} to embed the explorable icon into the summary HTML block, otherwise {@code false}
     */
    public void setEmbedIcon(boolean embedIcon)
    {
        this.embedIcon = embedIcon;
    }

    /**
     * @param textScalingPercent the percent to scale the label text
     */
    public void setTextScalingPercent(int textScalingPercent)
    {
        this.textScalingPercent = textScalingPercent;
    }

    private String createIconHtml(@Nonnull IExplorable explorable)
    {
        if ((entityDefinition).getIcon(explorable) instanceof ImageIcon imageIcon)
        {
            var b64 = imageIconToBase64(imageIcon);
            if (b64 != null) return "<div style='padding-right:5px'><img src='data:image/png;base64," + b64 + "' /></div>";
        }
        return "";
    }

    private static String imageIconToBase64(ImageIcon icon)
    {
        return ICON_CACHE.computeIfAbsent(icon, k -> {
            Graphics2D g2d = null;
            try
            {
                var width = icon.getIconWidth();
                var height = icon.getIconHeight();
                if (ICON_CACHE_IMAGE.getWidth() < width || ICON_CACHE_IMAGE.getHeight() < height)
                {
                    ICON_CACHE_IMAGE = new BufferedImage(Math.max(ICON_CACHE_IMAGE.getWidth(), width), Math.max(ICON_CACHE_IMAGE.getHeight(), height), BufferedImage.TYPE_INT_ARGB);
                }
                var bufferedImage = ICON_CACHE_IMAGE.getSubimage(0, 0, width, height);
                g2d = bufferedImage.createGraphics();
                icon.paintIcon(null, g2d, 0, 0);
                var outputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", outputStream);
                var imageBytes = outputStream.toByteArray();
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, width, height);
                return Base64.getEncoder().encodeToString(imageBytes);
            } catch (IOException e)
            {
                return null;
            } finally
            {
                if (g2d != null) g2d.dispose();
            }
        });
    }
}
