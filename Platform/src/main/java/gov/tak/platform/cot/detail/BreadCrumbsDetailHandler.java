package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.breadcrumb.IBreadcrumbManager;

/**
 * Handle the {@code <__bread_crumbs/>} detail.  This detail has a single attribute, {@code enabled} indicating if
 * the map item's breadcrumbs should be enabled (started) or disabled (stopped).
 *
 * @since 5.6
 * @implNote This handler simply sets an attribute value, {@link StandardAttributeNames#BREADCRUMBS_ENABLED}, on the map item.
 * The {@link IBreadcrumbManager BreadcrumbManager} reacts to a change in that attribute value to toggle the breadcrumbs.
 */
class BreadCrumbsDetailHandler extends CotDetailHandler
{
    private static final String BREAD_CRUMBS_DETAIL = "__bread_crumbs";
    private static final String ENABLED = "enabled";

    BreadCrumbsDetailHandler()
    {
        super(BREAD_CRUMBS_DETAIL);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        return false;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final String enabledString = detail.getAttribute(ENABLED);
        final boolean enable = "1".equals(enabledString) || Boolean.parseBoolean(enabledString);

        // It's too early to call the BreadcrumbManager directly, so store the value temporarily
        item.setMetaBoolean(StandardAttributeNames.BREADCRUMBS_ENABLED, enable);

        return ImportResult.SUCCESS;
    }
}
