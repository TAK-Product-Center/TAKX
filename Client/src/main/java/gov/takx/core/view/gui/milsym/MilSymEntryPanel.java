package gov.takx.core.view.gui.milsym;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.symbology.ISymbolTable;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilSymIconBuilder;
import gov.takx.api.milsym.SymbolWrapper;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXLabel;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A panel that represents a single {@link ISymbolTable.Symbol} entry.
 * Shows a preview icon, the name of the symbol, and the summary if one is present for the symbol.
 * <p/>
 * The display and behaviour is adjusted slightly based on the context the panel is used in.
 * <br/>
 * For interactive button-like use cases use
 * {@link MilSymEntryPanel#MilSymEntryPanel(IMilStd2525Service, SymbolWrapper, Consumer, Consumer)}.
 * <br/>
 * For uses as cell displays such as in a {@link javax.swing.JTree} use
 * {@link MilSymEntryPanel#MilSymEntryPanel(IMilStd2525Service, SymbolWrapper, Consumer)}.
 *
 * @since 5.1
 */
public class MilSymEntryPanel extends JPanel
{
    private static final BufferedImage INFO_OUTLINE = UIResources.getSizedImage(PLATFORM_ICONS, "info_outline.png", new Dimension(32, 32), true);
    private static final PopupFactory POPUP_FACTORY = new PopupFactory();
    private final SymbolWrapper symbol;
    private final IMilStd2525Service milStd2525Service;
    private final Consumer<IMilSymIconBuilder> iconAdapter;
    private final boolean requireBlockedRendering;
    private BufferedImage fullRender;
    private JPanel fullRenderComponent;
    private JXLabel summaryComponent;
    private Popup fullRenderPopup;

    /**
     * New entry panel without mouse interactivity.
     * 
     * @param milStd2525Service MIL-STD-2525 service for for symbol rendering.
     * @param symbol Symbol this panel will represent.
     * @param iconAdapter Adapter to tweak icon parameters before rendering them.
     */
    public MilSymEntryPanel(@Nonnull IMilStd2525Service milStd2525Service, @Nonnull SymbolWrapper symbol,
                            @Nullable Consumer<IMilSymIconBuilder> iconAdapter)
    {
        this(milStd2525Service, symbol, iconAdapter, null);
    }

    /**
     * New entry panel with mouse interactivity ({@code onSelectEntryListener}).
     * 
     * @param milStd2525Service MIL-STD-2525 service to utilize for symbol rendering.
     * @param symbol Symbol this panel will represent.
     * @param iconAdapter Adapter to tweak icon parameters before rendering them.
     * @param onSelectEntryListener Action to run when clicked on.
     */
    public MilSymEntryPanel(@Nonnull IMilStd2525Service milStd2525Service, @Nonnull SymbolWrapper symbol,
                            @Nullable Consumer<IMilSymIconBuilder> iconAdapter,
                            @Nullable Consumer<ISymbolTable.Symbol> onSelectEntryListener)
    {
        this.milStd2525Service = milStd2525Service;
        this.symbol = symbol;
        this.iconAdapter = iconAdapter;

        // Rendering can take a lot of time, especially if a large number of entries are displayed at once.
        // In a context where the UI can self-update we will want to do this asynchronously.
        // However, in contexts where the UI's first rendered frame is taken as a snapshot and cannot be updated
        // such as with a tree cell renderer, then we'll need to ensure the render is blocked until the 2525 symbol
        // render is completed.
        requireBlockedRendering = onSelectEntryListener == null;

        // Initialize the icon preview and layout.
        initComponents();
        updateIcons();

        // In the contexts we have mouse interaction enabled, we want this panel to look and act more like a button.
        // When it's not interactive (Such as being a tree cell render preview) we'll have a bit more basic handling.
        Color borderColor = UIManager.getColor("Component.borderColor");
        if (onSelectEntryListener != null)
        {
            String symbolDisplay = symbol.getName() + "<br><i><span style=\"color: #777777\">(" + symbol.getContainingFolder().getName() + ")</span></i>";

            nameLabel.setVerticalAlignment(SwingConstants.CENTER);
            nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
            nameLabel.setText("<html><center>" + symbolDisplay + "</center></html>");

            // If a summary exists for the icon, set up the button to display it as a popup.
            String summary = symbol.getSummary();
            if (!StringUtils.isEmpty(summary))
            {
                summaryButton.setBorder(new MatteBorder(0, 1, 0, 0, borderColor));
                summaryButton.setIcon(new ImageIcon(INFO_OUTLINE));
                summaryButton.addActionListener(e -> showSummaryPopup());
            } else
            {
                summaryButton.setVisible(false);
            }

            // When the mouse 'hovers' over this panel, change the background color.
            // Also support showing the full scale symbol preview if 'CTRL' is pressed.
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    // Show full size preview if control is held.
                    if (e.isControlDown()) showFullRenderPopup();
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    if (fullRenderPopup != null && !isMouseOverRenderPopup(e))
                    {
                        fullRenderPopup.hide();
                        fullRenderPopup = null;
                    }
                }

                @Override
                public void mousePressed(MouseEvent e)
                {
                    onSelectEntryListener.accept(symbol);
                }
            });

            // Creating a padded border creates the illusion that this panel is similar to a button.
            setBorder(new CompoundBorder(
                    new EmptyBorder(5, 5, 5, 5),
                    new LineBorder(borderColor)));
            previewWrapper.setBorder(new MatteBorder(0, 0, 0, 1, borderColor));
            previewWrapper.setBackground(UIManager.getColor("Button.background"));
        } else
        {
            // When this is used as a cell-renderer item, we cannot ensure all the shown panels will have
            // the same UI coordinates/widths, so it makes the most sense to keep things left aligned.
            nameLabel.setVerticalAlignment(SwingConstants.CENTER);
            nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
            nameLabel.setText("<html>" + symbol.getName() + "</html>");

            // In the same breath, we don't want the default background.
            // It should pass through to whatever the base component the cell resides in is.
            setBackground(new Color(0, 0, 0, 0));

            // Improve contrast for darker previews
            previewWrapper.setBorder(new LineBorder(borderColor, 1));
            previewWrapper.setBackground(UIManager.getColor("Button.background"));

            // Not interactive, so a summary button wouldn't be clickable anyways.
            summaryButton.setVisible(false);
        }
    }

    @Override
    public void removeNotify()
    {
        super.removeNotify();

        // When the search updates, and we remove the mil-sym entry panel
        // any open full-sized render popups should be removed.
        if (fullRenderPopup != null)
        {
            fullRenderPopup.hide();
            fullRenderPopup = null;
        }
    }

    /**
     * @param color Color to apply to the name display label.
     */
    public void setLabelForegroundColor(@Nonnull Color color)
    {
        nameLabel.setForeground(color);
    }

    /**
     * Refresh the graphic preview/full-sized icons.
     */
    void updateIcons()
    {
        // Will trigger re-drawing the full render display.
        fullRenderComponent = null;

        // Update the preview, blocking if needed.
        if (requireBlockedRendering)
        {
            updatePreview(renderIcon());
        } else
        {
            CompletableFuture.supplyAsync(this::renderIcon).thenAccept(this::updatePreview);
        }

    }

    /**
     * Renders the current {@link #symbol} to a 32x32 image.
     * 
     * @return Rendered symbol.
     */
    @Nonnull
    private BufferedImage renderIcon()
    {
        BufferedImage scaledRender;
        if (iconAdapter != null)
        {
            IMilSymIconBuilder builder = milStd2525Service.newSymbolIconBuilder()
                    .setCode(symbol.getCode())
                    .setResolution(32);
            iconAdapter.accept(builder);
            scaledRender = builder.render();
        } else
        {
            scaledRender = milStd2525Service.renderSinglePointIcon(symbol, 32);
        }
        return scaledRender;
    }

    /**
     * @param image Image to set as preview.
     */
    @InvokeOnEDT
    private void updatePreview(@Nonnull BufferedImage image)
    {
        previewLabel.setIcon(new ImageIcon(image));
    }

    /**
     * Displays a popup showing the {@link ISymbolTable.Symbol#getSummary()} on top of where the {@link #summaryButton} is.
     */
    private void showSummaryPopup()
    {
        if (summaryComponent == null)
        {
            summaryComponent = new JXLabel(symbol.getSummary());
            summaryComponent.setLineWrap(true);
            summaryComponent.setMaxLineSpan(400);
            summaryComponent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }

        // Using a JPopupMenu so that it will auto-hide when the user clicks away, or presses escape.
        JPopupMenu summaryPopup = new JPopupMenu();
        summaryPopup.add(summaryComponent);
        summaryPopup.show(summaryButton, 0, 0);
    }

    /**
     * Displays a popup showing the full icon render on top of where the {@link #previewLabel} is.
     */
    private void showFullRenderPopup()
    {
        if (fullRenderComponent == null)
        {
            IMilSymIconBuilder builder = milStd2525Service.newSymbolIconBuilder()
                    .setCode(symbol.getCode())
                    .setResolution(256);
            if (iconAdapter != null) iconAdapter.accept(builder);
            fullRender = builder.render();

            JLabel label = new JLabel(new ImageIcon(fullRender));
            label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            fullRenderComponent = new JPanel();
            fullRenderComponent.setLayout(new BorderLayout());
            fullRenderComponent.setBackground(Color.darkGray);
            fullRenderComponent.setBorder(new LineBorder(UIManager.getColor("Component.infoForeground"), 1, false));
            fullRenderComponent.add(label, BorderLayout.CENTER);
            fullRenderComponent.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseExited(MouseEvent e)
                {
                    // Hide this popup when we mouse away from it.
                    fullRenderPopup.hide();
                    fullRenderPopup = null;
                }
            });
        }

        Point screenPos = previewLabel.getLocationOnScreen();
        fullRenderPopup = POPUP_FACTORY.getPopup(previewLabel, fullRenderComponent,
                Math.max(0, (int) (screenPos.getX() + previewLabel.getWidth() / 2) - fullRender.getWidth() / 2),
                Math.max(0, (int) (screenPos.getY() + previewLabel.getHeight() / 2) - fullRender.getHeight() / 2));
        fullRenderPopup.show();
    }

    private boolean isMouseOverRenderPopup(@Nonnull MouseEvent e)
    {
        Point px = fullRenderComponent.getLocationOnScreen();
        if (px != null)
        {
            Rectangle popupBounds = new Rectangle(px.x, px.y, fullRenderComponent.getWidth(), fullRenderComponent.getHeight());
            return popupBounds.contains(e.getLocationOnScreen());
        }
        return false;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        previewWrapper = new JPanel();
        previewLabel = new JLabel();
        nameLabel = new JLabel();
        summaryButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "[32dlu,default], $lcgap, default:grow, $lcgap, [32dlu,default]",
                "fill:[32dlu,default]"));

        //======== previewWrapper ========
        {
            previewWrapper.setName("previewWrapper");
            previewWrapper.setLayout(new BorderLayout());

            //---- previewLabel ----
            previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
            previewLabel.setName("previewLabel");
            previewWrapper.add(previewLabel, BorderLayout.CENTER);
        }
        add(previewWrapper, cc.xy(1, 1));

        //---- nameLabel ----
        nameLabel.setName("nameLabel");
        add(nameLabel, cc.xy(3, 1));

        //---- summaryButton ----
        summaryButton.setName("summaryButton");
        add(summaryButton, cc.xy(5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel previewWrapper;
    private JLabel previewLabel;
    private JLabel nameLabel;
    private JButton summaryButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
