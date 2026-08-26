package gov.takx.core.mapobject.gui.widgets;

import gov.tak.api.commons.graphics.IIcon;
import gov.tak.api.widgets.IMapMenuButtonWidget;
import gov.tak.api.widgets.IMapMenuButtonWidget.OnButtonClickHandler;
import gov.tak.api.widgets.IMapMenuWidget;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.api.widgets.IWidgetBackground;
import gov.tak.api.widgets.factory.IMapMenuWidgetSpi;
import gov.tak.platform.commons.graphics.Icon;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.widgets.MapMenuButtonWidget;
import gov.tak.platform.widgets.MapMenuWidget;
import gov.tak.platform.widgets.WidgetBackground;
import gov.takx.core.globe.radial.NestedRadialMenuAction;
import gov.takx.core.globe.radial.RadialMenuAction;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;

import java.awt.event.ActionEvent;
import java.lang.invoke.MethodHandles;
import java.net.URI;

import static gov.tak.api.widgets.IMapWidget.OnClickListener;
import static gov.tak.platform.widgets.AbstractButtonWidget.STATE_DISABLED;
import static gov.tak.platform.widgets.AbstractButtonWidget.STATE_HOVERED;
import static gov.tak.platform.widgets.AbstractButtonWidget.STATE_PRESSED;
import static gov.tak.platform.widgets.AbstractButtonWidget.STATE_SELECTED;

/**
 * MapMenuButtonWidgetFactory SPI for handling IMapMenuWidget creation given a WidgetContextMenuModel
 *
 * @since 3.0
 */
public class ContextMenuMapMenuWidgetSpi implements IMapMenuWidgetSpi<ContextMenuModel>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final float SCALE = 0.75f;
    private IWidgetBackground widgetBackground = null;

    @Override
    public IMapMenuWidget create(ContextMenuModel contextModel)
    {
        MapMenuWidget widget = new MapMenuWidget();
        for (int i = 0; i < contextModel.getItemCount(); ++i)
        {
            if (contextModel.isSeparator(i)) continue;
            IMapMenuButtonWidget button = createMapMenuButtonWidget(contextModel, i);
            // Scale widget by adjusting width and radius
            button.setOrientation(button.getOrientationAngle(), button.getOrientationRadius() * SCALE);
            button.setButtonSize(button.getButtonSpan(), button.getButtonWidth() * SCALE);
            // Scale icons
            IIcon widgetIcon = button.getWidgetIcon();
            if (widgetIcon != null)
            {
                Icon icon = new Icon.Builder(widgetIcon)
                        .setSize((int) (widgetIcon.getWidth() * SCALE), (int) (widgetIcon.getHeight() * SCALE))
                        .setAnchor((int) (widgetIcon.getAnchorX() * SCALE), (int) (widgetIcon.getAnchorY() * SCALE))
                        .build();
                button.setWidgetIcon(icon);
            }
            ContextMenuModel submenuModel = contextModel.getSubmenu(i);
            if (submenuModel != null)
            {
                button.setSubmenu(create(submenuModel));
            }
            widget.addChildWidget(button);
        }
        return widget;
    }

    private IMapMenuButtonWidget createMapMenuButtonWidget(ContextMenuModel definition, int itemIndex)
    {
        final MapMenuButtonWidget menuButtonWidget = new MapMenuButtonWidget();
        final URI uri = definition.getIconURI(itemIndex);
        final String menuItemName = definition.getName(itemIndex);

        if (uri != null)
        {
            final Icon.Builder builder = new Icon.Builder();
            builder.setImageUri(0, uri.toString()).setAnchor(24, 24).setSize(48, 48);
            menuButtonWidget.setWidgetIcon(builder.build());
        } else
        {
            menuButtonWidget.setText(menuItemName);
        }

        menuButtonWidget.setName(menuItemName);
        menuButtonWidget.setLayoutWeight(1);
        menuButtonWidget.setWidgetBackground(getDefaultButtonBackground());

        final RadialMenuAction menuAction = definition.getMenuAction(itemIndex);

        if (menuAction != null)
        {
            menuButtonWidget.setSelectable(menuAction.hasState());
            menuButtonWidget.setState(menuAction.isSelected() ? STATE_SELECTED : 0);
            menuButtonWidget.setDisabled(!menuAction.isEnabled());

            // Handle a NestedMenu that has an action defined.
            //
            // Note: The takkernel will by default open the submenu on a single click if the wedge clicked has a submenu.
            //       It will also do so for a long press.  If the registered menu has an action defined, then we want to
            //       make a single click invoke that action instead of opening the submenu. This still leaves long press
            //       to open the submenu.

            if (menuAction instanceof NestedRadialMenuAction)
            {
                if (menuAction.hasActionListener())
                {
                    // setOnButtonClickHandler replaces the normal action of opening the submenu on click
                    menuButtonWidget.setOnButtonClickHandler(new MenuClickHandler(menuButtonWidget, menuAction, menuItemName));
                }
            } else
            {
                menuButtonWidget.addOnClickListener(new ButtonClickListener(menuButtonWidget, menuAction, menuItemName));
            }
        } else
        {
            menuButtonWidget.setDisabled(false);
        }

        return menuButtonWidget;
    }

    private synchronized IWidgetBackground getDefaultButtonBackground()
    {
        if (widgetBackground == null)
        {
            WidgetBackground.Builder builder = new WidgetBackground.Builder();

            builder.setColor(0, WidgetConstants.COLOR_BUTTON_DEFAULT);

            // highlighted
            builder.setColor(STATE_PRESSED, WidgetConstants.COLOR_BUTTON_HIGHLIGHTED);
            builder.setColor(STATE_PRESSED | STATE_HOVERED, WidgetConstants.COLOR_BUTTON_HIGHLIGHTED);
            builder.setColor(STATE_PRESSED | STATE_SELECTED, WidgetConstants.COLOR_BUTTON_HIGHLIGHTED);
            builder.setColor(STATE_HOVERED, WidgetConstants.COLOR_BUTTON_HIGHLIGHTED);

            // selected
            builder.setColor(STATE_SELECTED, WidgetConstants.COLOR_BUTTON_SELECTED);
            builder.setColor(STATE_PRESSED | STATE_SELECTED | STATE_HOVERED, WidgetConstants.COLOR_BUTTON_SELECTED);

            // disabled
            builder.setColor(STATE_DISABLED, WidgetConstants.COLOR_BUTTON_DISABLED);
            builder.setColor(STATE_DISABLED | STATE_PRESSED, WidgetConstants.COLOR_BUTTON_DISABLED);
            builder.setColor(STATE_DISABLED | STATE_PRESSED | STATE_HOVERED, WidgetConstants.COLOR_BUTTON_DISABLED);

            widgetBackground = builder.build();
        }
        return widgetBackground;
    }

    /**
     * @return A new {@link ActionEvent} with the given source and command name
     * @since 5.0
     */
    private static ActionEvent makeActionEvent(@Nonnull Object source, @Nonnull String menuItemName)
    {
        return new ActionEvent(source, ActionEvent.ACTION_PERFORMED, menuItemName, System.currentTimeMillis(), 0);
    }

    /**
     * Handler for submenu wedge that has an action defined.
     *
     * @param source Source to report
     * @param action Listener to invoke
     * @param menuItemName command/name for action
     * @since 5.0
     */
    private record MenuClickHandler(@Nonnull Object source, @Nonnull Action action, @Nonnull String menuItemName) implements OnButtonClickHandler
    {
        @Override
        public boolean isSupported(Object opaque)
        {
            return true;
        }

        @Override
        public void performAction(Object opaque)
        {
            action.actionPerformed(makeActionEvent(source, menuItemName));
        }
    }

    /**
     * Handler for normal wedge.
     *
     * @param source Source to report
     * @param action Action to invoke
     * @param menuItemName command/name for action
     * @since 5.0
     */
    private record ButtonClickListener(@Nonnull Object source, @Nonnull Action action, @Nonnull String menuItemName) implements OnClickListener
    {
        @Override
        public void onMapWidgetClick(IMapWidget widget, MotionEvent event)
        {
            action.actionPerformed(makeActionEvent(source, menuItemName));
        }
    }
}
