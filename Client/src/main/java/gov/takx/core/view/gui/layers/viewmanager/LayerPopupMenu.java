package gov.takx.core.view.gui.layers.viewmanager;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerGroup;
import gov.takx.core.globe.layers.SurfaceImageLayer;
import gov.takx.core.view.gui.layers.DefaultLayerUtils;
import gov.takx.core.view.gui.maps.importers.kml.KMLImportController;
import jakarta.inject.Inject;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;

/**
 */
@PseudoBean
public class LayerPopupMenu
{
    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private KMLImportController kmlImportController;

    @Inject
    private DefaultLayerUtils defaultLayerUtils;

    @Inject
    private LayerViewManager layerViewManager;

    public void show(Component parent, IRaptorLayer layer, Point p)
    {
        if (layer == null) return;

        final LayerGroup layerGroup = layer.getLayerGroup();
        final JPopupMenu menu = new JPopupMenu();

        // Add the "Go To" action (if "go to" is possible).
        if (layerViewManager.isGoToEnabled()) menu.add(new GoToLayerAction());

        if (layerGroup == LayerGroup.SURFACE_IMAGERY)
        {
            // Surface image layers may provide undo capability.  Add an undo action if necessary.
            final var layerManager = globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager();
            final var surfaceImageLayer = (SurfaceImageLayer) layerManager.getGlobeLayer(layer);
            if (surfaceImageLayer.isUndoAvailable()) menu.add(new UndoResizeAction(surfaceImageLayer));

        } else if (layerGroup == LayerGroup.IMAGERY || layerGroup == LayerGroup.ELEVATION)
        {
            // Ordinary imagery and elevation layers can be made default.
            menu.add(new DefaultLayerMenuItem(parent, layer));
        }

        if (!layerViewManager.getSelectedLayers().isEmpty()) menu.add(new ShowHideSelectedAction());
        if (layerViewManager.isDeleteEnabled()) menu.add(new DeleteSelectedAction());

        // Show the popup menu if it has any Action menu items.
        if (menu.getComponentCount() > 0) menu.show(parent, p.x, p.y);
    }

    /**
     * Action to undo a resize operation on a surface image.
     * @since 3.5
     */
    private static class UndoResizeAction extends AbstractAction
    {
        private final SurfaceImageLayer surfaceImageLayer;

        public UndoResizeAction(SurfaceImageLayer surfaceImageLayer)
        {
            super("Undo Movement");
            this.surfaceImageLayer = surfaceImageLayer;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            surfaceImageLayer.undoUserResize();
        }
    }

    /**
     * Action for Go To Layer
     */
    private class GoToLayerAction extends AbstractAction
    {
        public GoToLayerAction()
        {
            super("Go To");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            layerViewManager.goToSelectedLayer();
        }
    }

    /**
     * Menu item to wrap a default layer action.
     */
    private class DefaultLayerMenuItem extends JCheckBoxMenuItem
    {
        private final IRaptorLayer layer;
        private final Component parentComponent;

        public DefaultLayerMenuItem(Component parentComponent, IRaptorLayer layer)
        {
            super(new DefaultLayerAction());

            this.parentComponent = parentComponent;
            this.layer = layer;
            setToolTipText("Make layer(s) default layer(s) that will be installed every time a new project is opened.");
            setSelected(defaultLayerUtils.isDefaultLayer(layer));
        }
    }

    /**
     * Action to set/unset a Layer as default.
     */
    private class DefaultLayerAction extends AbstractAction
    {
        public DefaultLayerAction()
        {
            super("Default Layer");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            //Make sure we have the proper source.
            if (e.getSource() instanceof DefaultLayerMenuItem defaultLayerMenuItem)
            {
                final IRaptorLayer layer = defaultLayerMenuItem.layer;
                final boolean isDefaultLayer = defaultLayerUtils.isDefaultLayer(layer);

                if (isDefaultLayer)
                {
                    defaultLayerUtils.removeDefaultLayer(layer);
                } else
                {
                    defaultLayerUtils.addDefaultLayer(layer);
                }

                // Repaint the abstract layers table so that the default imagery layers icon gets updated properly.
                defaultLayerMenuItem.parentComponent.repaint();
            }
        }
    }

    private class DeleteSelectedAction extends AbstractAction
    {
        public DeleteSelectedAction()
        {
            super("Delete Selected");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            layerViewManager.deleteSelectedLayers();
        }
    }

    private class ShowHideSelectedAction extends AbstractAction
    {
        private final boolean showOrHide;

        public ShowHideSelectedAction()
        {
            showOrHide = !layerViewManager.getSelectedLayers().stream().allMatch(IRaptorLayer::isVisible);
            putValue(NAME, showOrHide ? "Show Selected" : "Hide Selected");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            layerViewManager.getSelectedLayers().forEach(layer -> layer.setVisible(showOrHide));
        }
    }
}
