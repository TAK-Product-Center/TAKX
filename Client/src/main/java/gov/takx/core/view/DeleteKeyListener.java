package gov.takx.core.view;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.core.mapobject.gui.DeleteAction;
import jakarta.inject.Inject;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 */
@PseudoBean
public class DeleteKeyListener extends KeyAdapter
{
    @Inject
    private IMapObjectManager mapObjectManager;

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_DELETE)
        {
            IMapObject selectedMO = mapObjectManager.getSelectedMapObject();
            if (selectedMO != null)
            {
                DeleteAction da = new DeleteAction("Delete", selectedMO);
                da.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "Delete"));
            }
        }
    }
}
