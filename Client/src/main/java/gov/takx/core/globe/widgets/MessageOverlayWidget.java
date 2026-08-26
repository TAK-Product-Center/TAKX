package gov.takx.core.globe.widgets;

import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.platform.widgets.TextWidget;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.globemessage.HideGlobeMessageEvent;
import gov.takx.api.event.globemessage.ShowGlobeMessageEvent;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.platform.globe.GlobeTextUtils;
import gov.takx.platform.ui.FontUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;

import java.awt.Color;

/**
 * Widget for displaying a message over the top of the globe (e.g. "Click map to place points").
 * @see ShowGlobeMessageEvent
 * @see HideGlobeMessageEvent
 * @since 5.2
 */
@PseudoBean
public class MessageOverlayWidget extends AWidget
{
    private static final Font FONT = new Font(GlobeTextUtils.DEFAULT_GLOBE_FONT.getName(), Font.Style.Normal, FontUtils.DEFAULT_FONT_SIZE);
    private static final Color TEXT_COLOR = Color.GREEN;

    private final IGlobeDelegate globe;
    private final TextWidget textWidget;

    /**
     * @param globe Target globe to show messages on.
     */
    public MessageOverlayWidget(@Nonnull IGlobeDelegate globe)
    {
        this.globe = globe;

        textWidget = new TextWidget("", new TextFormat(FONT, 0));
        textWidget.setBackground(TextWidget.TRANSLUCENT_BLACK);
        textWidget.setColor(TEXT_COLOR.getRGB());
        textWidget.setPadding(5);
        textWidget.setMargins(0, 5, 0, 0);

        setTouchable(false);
        addChildWidget(textWidget);
        setName("Message Overlay Widget");
    }

    @Override
    public boolean showInWidgetList()
    {
        // We do not want to allow users to disable overlay messages.
        return false;
    }

    @Override
    public void releaseResources()
    {
        // no-op
    }

    private void onShowGlobeMessageEvent(@Observes ShowGlobeMessageEvent event)
    {
        final IGlobeDelegate globe = event.getGlobe();
        if (globe == null || globe == this.globe)
        {
            updateMessage(event.getMessageText());
        }
    }

    private void onHideGlobeMessageEvent(@Observes HideGlobeMessageEvent event)
    {
        final IGlobeDelegate globe = event.getGlobe();
        if (globe == null || globe == this.globe)
        {
            updateMessage(null);
        }
    }

    @InvokeOnEDT
    private void updateMessage(@Nullable String message)
    {
        textWidget.setText(message);
        textWidget.setVisible(message != null);
    }
}
