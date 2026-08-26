package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.mapobject.CancelableMapObjectSelectionListener;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ADD_ICON;

/**
 * A swing field that holds an {@link IMapObject} selection and allows the user to populate by either selecting from a list,
 * selecting from the globe, or by creating a new object (creating a new object requires further integration). When populated
 * it displays the name and icon in a JLabel. When empty it displays the configurable String: {@link #setEmptyText(String)}.
 * To the right of the label are buttons that trigger selection operations. The globe and list dialog prompt can be
 * configured via {@link #setSelectPrompt(String)}
 *
 * @since 5.6
 * @param <T> the type of map objects expected in this field
 */
public class MapObjectField<T extends IMapObject> extends JPanel implements IResourceCleaner
{
    private static final ImageIcon GLOBE_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "globe.png");
    private static final ImageIcon SEARCH_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "search.png");

    private final ActionEvent changedEvent = new ActionEvent(this, 0, null);
    private final List<String> PROPERTIES = List.of(IMapObject.NAME, IMapObject.ICON);
    private final ListenerManager listenerManager = new ListenerManager();
    private final Font labelFont;
    private final Font labelFontEmpty;
    private T selected;
    private String emptyText = "No Object Selected";
    private String selectPrompt = "Select an object.";
    private Predicate<IMapObject> filter;
    private boolean editable = true;
    private boolean hideDialog = true;
    private boolean iconVisible = true;
    private boolean nameVisible = true;

    /**
     * Constructs a new component with no value.
     */
    public MapObjectField()
    {
        // TODO DCW TAKX-4207 Consider using the MapItem interface for this class. See ticket for info.
        initComponents();
        labelFont = objectLabel.getFont();
        labelFontEmpty = new Font(labelFont.getName(), labelFont.getStyle() | Font.ITALIC, labelFont.getSize());
        globeButton.setIcon(GLOBE_ICON);
        searchButton.setIcon(SEARCH_ICON);
        newButton.setIcon(ADD_ICON);
        setSelectedItem(null);
    }

    /**
     * @return {@code true} if the value of the field can be changed, otherwise {@code false}
     */
    public boolean isEditable()
    {
        return editable;
    }

    /**
     * @param editable {@code true} if the value of the field should be editable, otherwise {@code false}
     */
    public void setEditable(boolean editable)
    {
        if (editable != this.editable)
        {
            this.editable = editable;
            FormLayoutUtils.hideOrRestoreColumn(globeButton, !editable);
            FormLayoutUtils.hideOrRestoreColumn(searchButton, !editable);
            FormLayoutUtils.hideOrRestoreColumn(newButton, !editable);
        }
    }

    /**
     * @return the predicate that filters selected globe objects
     */
    @Nullable
    public Predicate<IMapObject> getFilter()
    {
        return filter;
    }

    /**
     * @param filter a filter to apply to selections from the globe
     */
    public void setFilter(@Nullable Predicate<IMapObject> filter)
    {
        this.filter = filter;
    }

    /**
     * @param visible {@code true} if the 'new' button should be visible, otherwise {@code false}
     */
    public void setNewButtonVisible(boolean visible)
    {
        newButton.setVisible(visible);
    }

    /**
     * @return {@code true} if the 'new' button is visible, otherwise {@code false}
     */
    @SuppressWarnings("unused")
    public boolean isNewButtonVisible()
    {
        return newButton.isVisible();
    }

    /**
     * @param visible {@code true} if the icon should be visible, otherwise {@code false}
     * @since 5.7
     */
    public void setIconVisible(boolean visible)
    {
        iconVisible = visible;
        refresh();
    }

    /**
     * @return {@code true} if the icon is visible, otherwise {@code false}
     * @since 5.7
     */
    @SuppressWarnings("unused")
    public boolean isIconVisible()
    {
        return iconVisible;
    }

    /**
     * @param visible {@code true} if the name should be visible, otherwise {@code false}
     * @since 5.7
     */
    public void setNameVisible(boolean visible)
    {
        nameVisible = visible;
        refresh();
    }

    /**
     * @return {@code true} if the name is visible, otherwise {@code false}
     * @since 5.7
     */
    @SuppressWarnings("unused")
    public boolean isNameVisible()
    {
        return nameVisible;
    }

    /**
     * @return the text to display when the field is empty
     */
    @SuppressWarnings("unused")
    @Nullable
    public String getEmptyText()
    {
        return emptyText;
    }

    /**
     * @param emptyText the text to display when the field is empty
     */
    public void setEmptyText(@Nullable String emptyText)
    {
        this.emptyText = emptyText;
        if (selected == null)
        {
            objectLabel.setText(emptyText);
        }
    }

    /**
     * @return the prompt to display on the globe when the user is selecting
     */
    @SuppressWarnings("unused")
    @Nullable
    public String getSelectPrompt()
    {
        return selectPrompt;
    }

    /**
     * @param selectPrompt the prompt to display on the globe when the user is selecting
     */
    public void setSelectPrompt(@Nullable String selectPrompt)
    {
        this.selectPrompt = selectPrompt;
    }

    /**
     * @return {@code true} if the parent {@link JDialog} should be hidden during globe selection, otherwise {@code false}
     */
    @SuppressWarnings("unused")
    public boolean isHideDialog()
    {
        return hideDialog;
    }

    /**
     * @param hideDialog {@code true} if the parent {@link JDialog} should be hidden during globe selection, otherwise {@code false}
     */
    @SuppressWarnings("unused")
    public void setHideDialog(boolean hideDialog)
    {
        this.hideDialog = hideDialog;
    }

    /**
     * @param listener a listener to receive events when the user selects an object
     */
    public void addActionListener(@Nonnull ActionListener listener)
    {
        listenerList.add(ActionListener.class, listener);
    }

    /**
     * @param listener a listener to add
     */
    public void addNewObjectActionListener(@Nonnull NewObjectActionListener listener)
    {
        listenerList.add(NewObjectActionListener.class, listener);
    }

    /**
     * @param listener a listener to remove
     */
    @SuppressWarnings("unused")
    public void removeNewObjectActionListener(@Nonnull NewObjectActionListener listener)
    {
        listenerList.remove(NewObjectActionListener.class, listener);
    }

    /**
     * @return the selected explorable
     */
    @Nullable
    public T getSelectedItem()
    {
        return selected;
    }

    /**
     * @param selected the explorable to select
     */
    public void setSelectedItem(@Nullable T selected)
    {
        if (this.selected == selected) return;
        listenerManager.unregisterAll();
        this.selected = selected;
        refresh();
        if (selected != null) listenerManager.addPropertyChangeListener(selected, PROPERTIES, ignored -> refresh());
        Stream.of(listenerList.getListeners(ActionListener.class)).forEach(l -> l.actionPerformed(changedEvent));
    }

    @Override
    public void releaseResources()
    {
        listenerManager.unregisterAll();
    }

    private void refresh()
    {
        var empty = selected == null;
        objectLabel.setFont(empty ? labelFontEmpty : labelFont);
        objectLabel.setText(empty ? emptyText : nameVisible ? selected.getName() : "");
        objectLabel.setIcon(!iconVisible || empty ? null : selected.getSizedIcon(ImageUtils.SMALL_ICON_WIDTH));
        objectLabel.setEnabled(!empty);
    }

    private boolean testFilter(IMapObject object)
    {
        return filter == null || filter.test(object);
    }

    private void searchButtonActionPerformed()
    {
        var dialog = new MapObjectFieldListDialog<T>(this::testFilter, selectPrompt);
        dialog.addActionListener(this::searchDialogActionPerformed);
        dialog.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void searchDialogActionPerformed(ActionEvent event)
    {
        var dialog = (MapObjectFieldListDialog<T>) event.getSource();
        var selectedItem = dialog.getSelectedItem();
        if (selectedItem != null)
        {
            setSelectedItem(selectedItem);
        }
    }

    @SuppressWarnings("unchecked")
    private void selectButtonActionPerformed(ActionEvent ignored)
    {
        var dialogToHide = hideDialog ? (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, this) : null;
        var modal = dialogToHide != null && dialogToHide.isModal();
        if (dialogToHide != null)
        {
            dialogToHide.setVisible(false);
        }
        new CancelableMapObjectSelectionListener(selected -> {
            if (dialogToHide != null)
            {
                dialogToHide.setVisible(true);
                dialogToHide.setModal(modal);
            }
            if (selected != null)
            {
                setSelectedItem((T) selected.get(0));
            }
        }, this::testFilter, selectPrompt).register();
    }

    private void newButtonActionPerformed(ActionEvent e)
    {
        Stream.of(listenerList.getListeners(NewObjectActionListener.class)).forEach(l -> l.actionPerformed(e));
    }

    /**
     * An extension of {@link ActionListener} necessary so that we can add the listeners via JFD.
     */
    @FunctionalInterface
    public interface NewObjectActionListener extends ActionListener
    {
        @Override
        void actionPerformed(@Nonnull ActionEvent e);
    }

    @SuppressWarnings({"Convert2MethodRef", "RedundantExplicitVariableType"})
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        objectLabel = new JLabel();
        globeButton = new JButton();
        searchButton = new JButton();
        newButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow, $ugap, [default,100dlu], 2*($lcgap, default)",
                "default"));

        //---- objectLabel ----
        objectLabel.setEnabled(false);
        objectLabel.setName("objectLabel");
        add(objectLabel, cc.xy(1, 1));

        //---- globeButton ----
        globeButton.setToolTipText("Select on Globe");
        globeButton.setName("globeButton");
        globeButton.addActionListener(e -> selectButtonActionPerformed(e));
        add(globeButton, cc.xy(3, 1));

        //---- searchButton ----
        searchButton.setToolTipText("Search by Name");
        searchButton.setName("searchButton");
        searchButton.addActionListener(e -> searchButtonActionPerformed());
        add(searchButton, cc.xy(5, 1));

        //---- newButton ----
        newButton.setToolTipText("Create New");
        newButton.setName("newButton");
        newButton.addActionListener(e -> newButtonActionPerformed(e));
        add(newButton, cc.xy(7, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    JLabel objectLabel;
    private JButton globeButton;
    private JButton searchButton;
    JButton newButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
