package gov.takx.core.view.gui.maps.exporters.dataset;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Popup menu containing location entry instructions.
 *
 */
public class LocationPopup extends JPopupMenu
{
    public LocationPopup()
    {
        add(new JMenuItem("<html>Valid Formats Include:<br>" +
                "Decimal Degrees e.g. 32.29, -64.77<br>" +
                "MGRS e.g. 20SLA 32709 74094"));
    }
}
