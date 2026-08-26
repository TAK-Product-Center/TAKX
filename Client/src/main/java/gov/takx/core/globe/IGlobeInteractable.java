package gov.takx.core.globe;

import gov.takx.api.globe.IHighlightable;

/**
 */
public interface IGlobeInteractable extends IHighlightable
{
    boolean isShowToolTip();

    void setShowToolTip(boolean showToolTip);
}
