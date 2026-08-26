package gov.takx.api.globe;

import gov.tak.platform.widgets.LinearLayoutWidget;

/**
 * Abstract base implementation for widgets. Developers of widgets are encouraged to extend this
 * base class.
 * @since 5.0
 */
public abstract class AWidget extends LinearLayoutWidget implements IWidget
{
    @Override
    public abstract void releaseResources();
}
