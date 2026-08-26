package gov.takx.core.mapobject.gui.widgets;

import gov.tak.api.widgets.IMapMenuWidget;
import gov.tak.api.widgets.IMapMenuWidgetController;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.api.widgets.factory.IMapMenuWidgetControllerSpi;
import gov.tak.platform.binding.PropertyInfo;
import gov.tak.platform.binding.PropertyValue;
import gov.tak.platform.widgets.AbstractWidgetController;

/**
 * @since 3.0
 */
public final class ContextMapMenuWidgetController extends AbstractWidgetController<ContextMenuModel> implements IMapMenuWidgetController
{

    public static final IMapMenuWidgetControllerSpi<ContextMenuModel> SPI = new IMapMenuWidgetControllerSpi<ContextMenuModel>()
    {
        @Override
        public IMapMenuWidgetController create(IMapWidget widget, ContextMenuModel model)
        {
            // ensured by the factory
            IMapMenuWidget menuWidget = (IMapMenuWidget) widget;
            return new ContextMapMenuWidgetController(menuWidget, model);
        }
    };

    ContextMapMenuWidgetController(IMapMenuWidget menuWidget, ContextMenuModel definition)
    {
        super(menuWidget, definition);

        //TODO-- more that can be done with this controller:
        //  * maintain layout with map changes
        //  * handling any dynamic changes to menu items, etc.
    }

    @Override
    protected PropertyValue resolvePropertyValue(PropertyInfo propertyInfo, ContextMenuModel definition)
    {
        // Called when a property value should refresh. For our purposes we just persist the initial value.
        return PropertyValue.of(this.widget.getPropertyValue(propertyInfo.getName()));
    }

    @Override
    public void onPropertyValueChanged(IMapWidget bindingObject, String propertyName, Object oldValue, Object value)
    {
        // called if we are listening to property value changes on the widget
    }

    @Override
    public IMapMenuWidget getMapMenuWidget()
    {
        return (IMapMenuWidget) this.widget;
    }
}
