package gov.takx.core.view.gui.files;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.actionlist.ActionList;
import gov.takx.commons.swing.actionlist.ActionListAction;
import gov.takx.commons.swing.entity.EntityColumn;
import gov.takx.commons.swing.entity.mapobject.MapObjectEntityDefinition;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.io.FilenameUtils;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Component;
import java.io.File;
import java.util.List;

import static gov.takx.api.coordinatesystem.CoordinateSystem.getLocationString;
import static gov.takx.platform.ui.UIResources.DELETE_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.EXPORT_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.GOTO_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.LAUNCH_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.OPEN_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.SETTINGS_ACTION_BUTTON_ICON;

/**
 * Entity Definition for {@link IFileMapObject}.
 * 
 * @since 5.8
 */
@PseudoBean
public class FileEntityDefinition extends MapObjectEntityDefinition<IFileMapObject>
{
    private static final String DETAILS_STRING_FORMAT = "%s<br>%s";
    private static final String DELETE_POPUP_STRING_FORMAT = "<html>Delete File <b>%s<b>?</html>";

    @Inject
    private MapObjectManager mapObjectManager;
    @Inject
    private IGlobeManager globeManager;
    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;
    private IVirtualFileSystem fileSystem;

    @SuppressWarnings("DataFlowIssue")
    private final Action gotoAction = new ActionListAction<IFileMapObject>("Go To", GOTO_ACTION_BUTTON_ICON, mo -> globeManager.getLastFocusedGlobe().goTo(mo.getLocation()), null);
    private final Action launchAction = new ActionListAction<IFileMapObject>("Launch", LAUNCH_ACTION_BUTTON_ICON, mo -> FileUtils.launchFile(mo.getVfsFile().getFile()), null);
    private final Action exportAction = new ActionListAction<>("Export", EXPORT_ACTION_BUTTON_ICON, this::export, null);
    private final Action configureAction = new ActionListAction<IFileMapObject>("Configure", SETTINGS_ACTION_BUTTON_ICON, mo -> mo.getController().showConfigurationDialog(), null);
    private final Action openAction = new ActionListAction<IFileMapObject>("Open", OPEN_ACTION_BUTTON_ICON, mo -> mo.getController().showPluginDialog(), null);
    private final Action deleteAction = new ActionListAction<IFileMapObject>("Delete", DELETE_ACTION_BUTTON_ICON,
            e -> ActionPopupFactory.yesNo(
                    DELETE_POPUP_STRING_FORMAT.formatted(e.requireValue().getName()),
                    (Component) e.getSource(),
                    () -> mapObjectManager.deleteMapObject(e.requireValue())).show());

    // Only show a subset of actions on the row. Show all in the popup menu.
    private final List<Action> rowActions = List.of(gotoAction, exportAction, deleteAction);
    private final List<Action> popupActions = List.of(launchAction, exportAction, openAction, configureAction, gotoAction, deleteAction);

    final EntityColumn<IFileMapObject, String> typeColumn;
    final EntityColumn<IFileMapObject, Long> sizeColumn;

    /**
     * Construct a new instance with standard file columns.
     */
    public FileEntityDefinition()
    {
        typeColumn = EntityColumn.<IFileMapObject, String>builder().name("Type").type(String.class).value(fmo -> FilenameUtils.getExtension(fmo.getName())).cached(true).build();
        sizeColumn = EntityColumn.<IFileMapObject, Long>builder().name("Size").type(Long.class).value(fmo -> fmo.getVfsFile().length()).cached(true).build();
        addColumns(typeColumn, sizeColumn);
        addPropertyChangeMapping(List.of(IMapObject.LOCATION), List.of(getSummaryColumn()));
    }

    @PostConstruct
    private void postConstruct()
    {
        fileSystem = projectVFSManager.getVirtualFileSystemForAttachments();
    }

    @Override
    @Nullable
    protected String getName(@Nonnull IFileMapObject fileMapObject)
    {
        return fileMapObject.getVfsFile().getFileName();
    }

    @Override
    @Nullable
    protected String getDetailsHtml(@Nonnull IFileMapObject fileMapObject)
    {
        var location = fileMapObject.getLocation().isValid()
                ? getLocationString(fileMapObject.getLocation(), UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class))
                : "Not on Globe";
        //noinspection DataFlowIssue size will never be null
        var size = FileUtils.formatFileSize(getData().getValue(fileMapObject, sizeColumn));
        return DETAILS_STRING_FORMAT.formatted(size, location);
    }

    @Override
    @Nullable
    protected ActionList<IFileMapObject> getActions(@Nonnull IFileMapObject fileMapObject)
    {
        gotoAction.setEnabled(fileMapObject.getLocation().isValid());
        return new ActionList<>(rowActions, fileMapObject);
    }

    @Nullable
    @Override
    protected ActionList<IFileMapObject> getPopupActions(@Nonnull IFileMapObject fileMapObject)
    {
        gotoAction.setEnabled(fileMapObject.getLocation().isValid());
        return new ActionList<>(popupActions, fileMapObject);
    }

    private void export(@Nonnull IFileMapObject mo)
    {
        var vfsFile = mo.getVfsFile();
        if (vfsFile.exists())
        {
            var chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
            chooser.setDialogTitle("Save File");

            var ext = FileUtils.getExtension(vfsFile.getFile());
            if (null != ext)
            {
                chooser.resetChoosableFileFilters();
                chooser.setFileFilter(new FileNameExtensionFilter("*." + ext, ext));
            }

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
            {
                var targetFile = chooser.getSelectedFile();
                if (null != ext && FileUtils.getExtension(targetFile) == null)
                {
                    targetFile = new File(targetFile.getParent(), targetFile.getName() + "." + ext);
                }
                new ExportDialog(mo, fileSystem, targetFile).setVisible(true);
            }
        } else
        {
            AlwaysOnTopJOptionPane.showErrorDialogLater("Can't find file: " + vfsFile.getFileName() + ".  Very possibly it has been removed from the project.", "Error Opening File");
        }
    }
}
