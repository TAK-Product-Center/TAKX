package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.factories.Borders;
import gov.takx.api.explorer.ExplorerUtils;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.FormButtonBar;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.RowBorderHighlighter;
import gov.takx.core.explorer.common.CoreExplorerUtils;
import gov.takx.core.globe.GlobePlaybackIconMapObject;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A modal dialog that enables the user to select from a list of objects of various types including {@link IMapObject},
 * {@link IGlobeMapObject}, {@link IMapObjectExplorable}, and {@link IExplorable}.
 *
 * @since 5.1 (Significant redesign in 5.6)
 */
public class GlobeMapObjectSelectionDialog<T> extends AStandardDialog
{
    private final ActionEvent actionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null);
    private final List<ActionListener> actionListeners = new ArrayList<>();
    private final DefaultTableModel model;

    /**
     * Construct a new instance with no title using the given list of objects, allowing only a single object to be selected,
     * and make it visible.
     * 
     * @param objectList the object list
     * @since 5.1 (Generic type parameter in 5.6)
     */
    public GlobeMapObjectSelectionDialog(@Nonnull List<T> objectList)
    {
        this(null, objectList, false);
        setVisible(true);
    }

    /**
     * Construct a new instance with a title.
     *
     * @param title the dialog title
     * @param multiple {@code true} if multiple objects can be selected or {@code false} for single
     * @since 5.6
     */
    public GlobeMapObjectSelectionDialog(@Nullable String title, @Nullable List<T> objectList, boolean multiple)
    {
        initComponents();

        model = (DefaultTableModel) table.getModel();
        model.setColumnCount(1);
        model.setRowCount(0);

        table.setShowGrid(false);
        table.addHighlighter(new RowBorderHighlighter(HighlightPredicate.ALWAYS, new EmptyBorder(1, 1, 1, 1)));
        table.addHighlighter(new RowBorderHighlighter(HighlightPredicate.ROLLOVER_ROW, new MatteBorder(1, 1, 1, 1, UIResources.BORDER_HIGHLIGHT_COLOR)));
        table.getColumn(0).setCellRenderer(new Renderer());
        scrollPane.setColumnHeaderView(null);
        if (title != null) setTitle(title);
        if (!multiple)
        {
            buttonBar.setVisible(false);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(s -> fireListenersAndClose());
        } else
        {
            buttonBar.attachField(table, null);
        }

        if (objectList != null) objectList.forEach(item -> model.addRow(new Object[]{item}));
    }

    /**
     * @return the selected map object, or {@code null} if the window was closed before selection
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T getSelectedObject()
    {
        var index = table.getSelectionModel().getAnchorSelectionIndex();
        return index == -1 ? null : (T) model.getValueAt(index, 0);
    }

    /**
     * @param listener the listener to add
     * @since 5.6
     */
    public void addActionListener(@Nonnull ActionListener listener)
    {
        actionListeners.add(listener);
    }

    /**
     * @param listener the listener to remove
     * @since 5.6
     */
    @SuppressWarnings("unused")
    public void removeActionListener(@Nonnull ActionListener listener)
    {
        actionListeners.remove(listener);
    }

    private void fireListenersAndClose()
    {
        actionListeners.forEach(l -> l.actionPerformed(actionEvent));
        setVisible(false);
    }

    private void buttonBarApply(ActionEvent ignored)
    {
        fireListenersAndClose();
    }

    private static class Renderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column)
        {
            row = table.convertRowIndexToModel(row);
            var label = (JLabel) super.getTableCellRendererComponent(table, table, selected, focused, row, column);
            IMapObject mapObject = value instanceof IMapObject mo ? mo
                    : value instanceof IGlobeMapObject gmo ? gmo.getMapObject()
                    : value instanceof IMapObjectExplorable e ? e.getMapObject() : null;
            if (mapObject != null)
            {
                label.setIcon(mapObject.getSizedIcon(ImageUtils.SMALL_ICON_WIDTH));
                label.setText(getDisplayName(value));
                label.setToolTipText(mapObject.getKey());
            } else if (value instanceof IExplorable e)
            {
                label.setIcon(CoreExplorerUtils.iconForExplorable(e, ImageUtils.SMALL_ICON_WIDTH));
                label.setText(e.getDisplayName());
                label.setToolTipText(ExplorerUtils.createDefaultKey(e.getDisplayName(), e.getParentPath()));
            } else
            {
                label.setText("Unknown");
            }
            return label;
        }

        /**
         * @return Display name to use for the given table value, taking into account Playback markers
         * @since 5.7
         */
        private String getDisplayName(Object value)
        {
            if (value instanceof IMapObject mo) return mo.getName();
            if (value instanceof IMapObjectExplorable e) return e.getMapObject().getName();
            if (value instanceof IGlobeMapObject gmo)
            {
                final String name = gmo.getMapObject().getName();
                return value instanceof GlobePlaybackIconMapObject ? name + " (in playback)" : name;
            }
            return null;
        }
    }

    @SuppressWarnings("Convert2MethodRef")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPanel = new JPanel();
        scrollPane = new JScrollPane();
        table = new JXTable();
        buttonBar = new FormButtonBar();

        //======== this ========
        setTitle("Select an Object");
        setMinimumSize(new Dimension(150, 250));
        setMaximumSize(new Dimension(250, 250));
        setPreferredSize(new Dimension(250, 250));
        setModal(true);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPanel ========
        {
            dialogPanel.setBorder(Borders.DIALOG_BORDER);
            dialogPanel.setName("dialogPanel");
            dialogPanel.setLayout(new BorderLayout());

            //======== scrollPane ========
            {
                scrollPane.setName("scrollPane");

                //---- table ----
                table.setEditable(false);
                table.setRowHeight(32);
                table.setName("table");
                scrollPane.setViewportView(table);
            }
            dialogPanel.add(scrollPane, BorderLayout.CENTER);

            //---- buttonBar ----
            buttonBar.setApplyButtonVisible(false);
            buttonBar.setErrorLabelVisible(false);
            buttonBar.setDeleteButtonVisible(false);
            buttonBar.setName("buttonBar");
            buttonBar.addApplyListener(e -> buttonBarApply(e));
            dialogPanel.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JScrollPane scrollPane;
    private JXTable table;
    private FormButtonBar buttonBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
