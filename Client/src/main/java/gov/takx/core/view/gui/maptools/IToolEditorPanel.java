package gov.takx.core.view.gui.maptools;

import gov.takx.api.ui.IResourceCleaner;

import javax.swing.JPanel;

/**
 * Contract for all map/draw tool editor panels.  Since JPanel isn't an interface, we need
 * a separate interface to support common handling of the panels (e.g., tracking them and
 * resource cleaning).
 *
 * @since 2.37
 */
public interface IToolEditorPanel extends IResourceCleaner
{
    JPanel getPanel();
}
