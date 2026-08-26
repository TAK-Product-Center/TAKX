package gov.takx.core.view.gui.files;

import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.commons.file.FileUtils;

import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * An Action that will "launch" a collection of files when invoked.
 *
 * @see FileUtils#launchFile
 */
public class LaunchAction extends AbstractAction
{
    private final Supplier<List<IFileMapObject>> filesSupplier;

    /**
     * @param filesSupplier Supplier to provide the list of files to process when the action is invoked
     * @since 3.0
     */
    public LaunchAction(Supplier<List<IFileMapObject>> filesSupplier)
    {
        super("Launch");

        this.filesSupplier = filesSupplier;
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        filesSupplier.get().stream()
                .map(file -> file.getVfsFile().getFile())
                .filter(Objects::nonNull)
                .forEach(FileUtils::launchFile);
    }
}
