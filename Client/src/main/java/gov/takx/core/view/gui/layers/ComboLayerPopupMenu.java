package gov.takx.core.view.gui.layers;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.view.gui.StandardDialogManager;
import gov.takx.core.view.gui.maps.AddImageryOverlayDialog;
import gov.takx.core.view.gui.maps.AddKMLOverlayDialog;
import gov.takx.core.view.gui.maps.AddShapeOverlayDialog;
import gov.takx.core.view.gui.maps.ConnectToImageryServerDialog;
import gov.takx.core.view.gui.maps.LoadElevationFilesDialog;
import gov.takx.core.view.gui.maps.LoadImageryFilesDialog;
import gov.takx.core.view.gui.maps.LoadModelFilesDialog;
import jakarta.inject.Inject;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A contextual pop-up menu for the Add button in the Layer Panel.  Layer panel holds a single instance reference to
 * this class and must specify a combo group in order to display this menu.  The combo group defines which menu items
 * will be displayed.
 */
@PseudoBean
public class ComboLayerPopupMenu extends JPopupMenu
{
    @Inject
    private StandardDialogManager standardDialogManager;

    public ComboLayerPopupMenu()
    {
        //set up overlay action listeners
        JMenuItem addImageryOverlayItem = new JMenuItem("Add Imagery Overlay");
        addImageryOverlayItem.addActionListener(e -> AddImageryOverlayDialog.showInstance());

        JMenuItem addKMLOverlayItem = new JMenuItem("Add KML/KMZ Overlay");
        addKMLOverlayItem.addActionListener(e -> AddKMLOverlayDialog.showInstance());

        JMenuItem addSHPOverlayItem = new JMenuItem("Add SHP Overlay");
        addSHPOverlayItem.addActionListener(e -> AddShapeOverlayDialog.showInstance());

        // Set up 3D model action listeners
        JMenuItem loadModelFilesItem = new JMenuItem("Load Model Files");
        loadModelFilesItem.addActionListener(e -> standardDialogManager.showDialog(LoadModelFilesDialog.class));

        // Set up imagery action listeners
        JMenuItem loadImageryFilesItem = new JMenuItem("Load Imagery Files");
        loadImageryFilesItem.addActionListener(e -> standardDialogManager.showDialog(LoadImageryFilesDialog.class));

        JMenuItem connectToImageryServerItem = new JMenuItem("Connect to Imagery Server");
        connectToImageryServerItem.addActionListener(e -> standardDialogManager.showDialog(ConnectToImageryServerDialog.class));

        // Set up elevation action listener
        JMenuItem loadElevationFilesItem = new JMenuItem("Load Elevation Files");
        loadElevationFilesItem.addActionListener(e -> standardDialogManager.showDialog(LoadElevationFilesDialog.class));

        add(connectToImageryServerItem);
        add(loadImageryFilesItem);
        add(loadModelFilesItem);
        add(loadElevationFilesItem);
        add(addImageryOverlayItem);
        add(addKMLOverlayItem);
        add(addSHPOverlayItem);
    }
}
