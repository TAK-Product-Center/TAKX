package gov.takx.core.view.gui.breadcrumb;

import gov.tak.api.maps.Marker;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.commons.swing.ImageUtils;
import jakarta.inject.Inject;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import java.awt.Component;

/**
 * Renders a {@link Marker}, displaying its name, icon and track count.
 *
 * @since 5.4
 */
@PseudoBean
class MarkerEntryRenderer extends DefaultListCellRenderer
{
    private final MarkerEntryPanel markerEntryPanel = new MarkerEntryPanel();

    @Inject
    private IBreadcrumbManager breadcrumbManager;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value != null)
        {
            final var markerMapObject = (Marker) value;
            markerEntryPanel.update(markerMapObject.getSizedIcon(ImageUtils.DEFAULT_ICON_WIDTH),
                    markerMapObject.getName(), breadcrumbManager.getTrackCount(markerMapObject.getUid()));
        }

        return markerEntryPanel;
    }
}
