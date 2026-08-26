package gov.takx.core.mapobject.gui;

import com.google.common.base.Predicates;
import com.jgoodies.forms.factories.Borders;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.commons.swing.FormButtonBar;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXTable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.EventListenerList;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A dialog that presents the user with a list of map objects to select from. The primary use of this dialog is by the
 * {@link MapObjectField} to allow users to select the explorable from a List. This component uses a {@link JXTable} rather
 * than a {@link javax.swing.JList} so as not to preclude the use of sorting, filtering, and multi-column presentation.
 *
 * @since 5.6
 */
@PseudoBean
public class MapObjectFieldListDialog<T extends IMapObject> extends AStandardDialog
{
    private final ActionEvent actionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null);
    private final EventListenerList listenerList = new EventListenerList();
    private final DefaultTableModel model;
    private final Predicate<IMapObject> filter;

    @Inject
    private IMapObjectManager mapObjectManager;

    /**
     * Create a new dialog instance with a title and filtering the map objects by the given filter.
     *
     * @param filter optional predicate to filter explorables
     * @param title title of dialog
     */
    public MapObjectFieldListDialog(@Nullable Predicate<IMapObject> filter, @Nullable String title)
    {
        initComponents();
        this.filter = filter;
        model = (DefaultTableModel) table.getModel();
        model.setColumnCount(1);
        model.setRowCount(0);
        table.setShowGrid(false);
        table.putClientProperty(JXTable.USE_DTCR_COLORMEMORY_HACK, Boolean.FALSE);

        table.getColumn(0).setCellRenderer(new Renderer());
        scrollPane.setColumnHeaderView(null);
        if (title != null) setTitle(title);

        buttonBar.attachField(table, null);
    }

    @PostConstruct
    private void postConstruct()
    {
        mapObjectManager.getMapObjects().stream().filter(IMapObject::isSelectable).filter(filter != null ? filter : Predicates.alwaysTrue()).forEach(item -> model.addRow(new Object[]{item}));
    }

    /**
     * @return the List of items selected by the user
     */
    @SuppressWarnings({"unused", "unchecked"})
    @Nonnull
    public List<T> getSelectedItems()
    {
        return IntStream.of(table.getSelectionModel().getSelectedIndices()).mapToObj(i -> (T) model.getValueAt(i, 0)).toList();
    }

    /**
     * @return the first item selected by the user, or {@code null} if nothing was selected
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T getSelectedItem()
    {
        var index = table.getSelectionModel().getAnchorSelectionIndex();
        return index == -1 ? null : (T) model.getValueAt(index, 0);
    }

    /**
     * @param listener the listener to add
     */
    public void addActionListener(@Nonnull ActionListener listener)
    {
        listenerList.add(ActionListener.class, listener);
    }

    /**
     * @param listener the listener to remove
     */
    @SuppressWarnings("unused")
    public void removeActionListener(@Nonnull ActionListener listener)
    {
        listenerList.remove(ActionListener.class, listener);
    }

    /**
     * See {@link JXTable#getSelectionMode()}
     *
     * @return the selection mode
     */
    @SuppressWarnings("unused")
    public int getSelectionMode()
    {
        return table.getSelectionMode();
    }

    /**
     * See {@link JTable#setSelectionMode(int)}.
     *
     * @param mode the selection mode
     */
    public void setSelectionMode(int mode)
    {
        table.setSelectionMode(mode);
    }

    private void buttonBarApply(ActionEvent ignored)
    {
        Stream.of(listenerList.getListeners(ActionListener.class)).forEach(l -> l.actionPerformed(actionEvent));
    }

    private static class Renderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean selected,
                                                       boolean focused,
                                                       int row,
                                                       int column)
        {
            row = table.convertRowIndexToModel(row);
            var label = (JLabel) super.getTableCellRendererComponent(table, table, selected, focused, row, column);
            var model = table.getModel();
            var object = (IMapObject) model.getValueAt(row, 0);
            label.setIcon(object.getSizedIcon(ImageUtils.SMALL_ICON_WIDTH));
            label.setText(object.getName());
            label.setToolTipText(object.getUid());
            return label;
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
