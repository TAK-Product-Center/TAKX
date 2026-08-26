package gov.takx.core.view.gui.connections;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.plugin.connection.NoSuchConnectionControllerException;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.actionlist.ActionList;
import gov.takx.commons.swing.actionlist.ActionListAction;
import gov.takx.commons.swing.entity.EntityColumn;
import gov.takx.commons.swing.entity.common.CommonEntityDefinition;
import gov.takx.platform.utils.ConnectionTemplateManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Component;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.takx.platform.ui.UIResources.DELETE_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.EXPORT_ACTION_BUTTON_ICON;

/**
 * {@link gov.takx.commons.swing.entity.EntityDefinition} implementation for {@link ConnectionInfo} instances. Populates the
 * name, icon, and actions columns.
 *
 * @since 5.8
 */
@PseudoBean
public class ConnectionEntityDefinition extends CommonEntityDefinition<ConnectionInfo>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String DELETE_POPUP_STRING_FORMAT = "<html>Delete the <b>%s<b>? connection</html>";

    private final Action deleteAction = new ActionListAction<ConnectionInfo>("Delete", DELETE_ACTION_BUTTON_ICON,
            e -> ActionPopupFactory
                    .yesNo(DELETE_POPUP_STRING_FORMAT.formatted(e.requireValue().getName()), (Component) e.getSource(), () -> removeButtonActionPerformed(e.requireValue()))
                    .show());

    private final Action exportAction = new ActionListAction<ConnectionInfo>("Export", EXPORT_ACTION_BUTTON_ICON,
            e -> export(List.of(e.requireValue()), (Component) e.getSource()));

    private final List<Action> actions = List.of(exportAction, deleteAction);

    @Inject
    private IConnectionManager connectionManager;

    final EntityColumn<ConnectionInfo, ConnectionState> stateColumn;

    /**
     * Construct a new entity definition for {@link ConnectionInfo} instances.
     */
    public ConnectionEntityDefinition()
    {
        stateColumn = EntityColumn.<ConnectionInfo, ConnectionState>builder().name("State").type(ConnectionState.class).value(ConnectionInfo::getState).cached(true).build();
        addColumns(stateColumn);
        addPropertyChangeMapping(List.of(ConnectionInfo.STATE), List.of(getSummaryColumn(), getIconColumn(), stateColumn));
        addPropertyChangeMapping(List.of(ConnectionInfo.NAME), List.of(getSummaryColumn()));
        addPropertyChangeMapping(List.of(ConnectionInfo.MESSAGE), List.of(getSummaryColumn()));
    }

    @Override
    protected @Nullable Icon getIcon(@NonNull ConnectionInfo connectionInfo)
    {
        return connectionInfo.getIcon();
    }

    @Override
    protected @Nullable String getName(@NonNull ConnectionInfo connectionInfo)
    {
        return connectionInfo.getName();
    }

    @Override
    protected @Nullable ActionList<ConnectionInfo> getActions(@NonNull ConnectionInfo connectionInfo)
    {
        return new ActionList<>(actions, connectionInfo);
    }

    @Override
    protected @Nullable String getDetailsHtml(@NonNull ConnectionInfo connectionInfo)
    {
        return connectionInfo.getState() + (connectionInfo.getMessage() != null ? ": " + connectionInfo.getMessage() : "");
    }

    private void removeButtonActionPerformed(@NonNull ConnectionInfo connectionInfo)
    {
        connectionManager.removeConnection(connectionInfo.getKey(), true);
    }

    /**
     * Export Connections Panel to a connection template. Use the {@link ConnectionTemplateManager} to do the JAXB
     * marshaling to the file.
     *
     * @param connections the connections to export
     * @param parent the parent component (for dialog construction)
     */
    public void export(@Nonnull Collection<ConnectionInfo> connections, @Nonnull Component parent)
    {
        Map<String, IConnectionPluginController> controllerMap = new HashMap<>();
        for (var connectionInfo : connections)
        {
            try
            {
                var controller = connectionManager.getControllerFromConnectionKey(connectionInfo.getKey());
                var pluginController = controller.getPluginConnectionController();
                if (pluginController.getRestorableParametersMap() != null)
                {
                    controllerMap.put(controller.getConnectionDisplayName(), pluginController);
                }
            } catch (NoSuchConnectionControllerException e)
            {
                logger.warn("Error looking up connection controller to save to template, key = {}", connectionInfo.getKey());
            }
        }

        if (!controllerMap.isEmpty())
        {
            JFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
            fileChooser.setDialogTitle("Export Connections to Template File");
            FileFilter xmlType = new FileNameExtensionFilter("Template File (.xml)", "xml");
            fileChooser.resetChoosableFileFilters();
            fileChooser.addChoosableFileFilter(xmlType);

            if (fileChooser.showSaveDialog(parent) == JOptionPane.OK_OPTION)
            {
                var file = fileChooser.getSelectedFile();
                var addSuffix = !file.getAbsolutePath().toLowerCase().endsWith(".xml");
                final var xmlFile = (addSuffix) ? new File(file.getAbsolutePath() + ".xml") : file;
                ConnectionTemplateManager.saveConnectionTemplate(controllerMap, xmlFile);
            }
        } else
        {
            AlwaysOnTopJOptionPane.showMessageDialog((connections.size() == 1 ? "Connection does not" : "No Connections") + " support template creation", "Connection Template", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
