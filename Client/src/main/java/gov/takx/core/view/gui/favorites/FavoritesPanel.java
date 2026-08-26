package gov.takx.core.view.gui.favorites;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IFavoritesListener;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IProjectFavoritesManager;
import gov.takx.api.globe.IUserFavoritesManager;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@PseudoBean(singletonObserver = true)
public class FavoritesPanel extends JPanel
{
    @Inject
    private IProjectFavoritesManager projectFavoritesManager;

    @Inject
    private IUserFavoritesManager userFavoritesManager;

    @Inject
    private IGlobeManager globeManager;

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::repaintTables);

    private AddFavoriteDialog addFavoriteDialog;
    private EditFavoriteDialog editFavoriteDialog;

    private IFavoritesListener userFavoritesListener;
    private IFavoritesListener projectFavoritesListener;
    private FavoriteTableModel userFavoritesTableModel;
    private FavoriteTableModel projectFavoritesTableModel;

    public FavoritesPanel()
    {
        initComponents();

        // Prepare icons.
        buttonAddUser.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);
        buttonRemove.setIcon(UIResources.REMOVE_ACTION_BUTTON_ICON);
        editButton.setIcon(UIResources.EDIT_ACTION_BUTTON_ICON);
        gotoButton.setIcon(UIResources.GOTO_ACTION_BUTTON_ICON);
    }

    @PostConstruct
    private void postConstruct()
    {
        userFavoritesTableModel = new FavoriteTableModel(userFavoritesManager);
        initTable(userFavoritesTable, userFavoritesTableModel, true);
        userFavoritesListener = new FavoritesListener(userFavoritesTableModel);
        userFavoritesManager.addListener(userFavoritesListener);

        projectFavoritesTableModel = new FavoriteTableModel(projectFavoritesManager);
        initTable(projectFavoritesTable, projectFavoritesTableModel, false);
        projectFavoritesListener = new FavoritesListener(projectFavoritesTableModel);
        projectFavoritesManager.addListener(projectFavoritesListener);
    }

    /**
     * Dispose of our resources and cleanup.
     *
     * @since 2.37
     */
    void dispose()
    {
        userFavoritesManager.removeListener(userFavoritesListener);
        projectFavoritesManager.removeListener(projectFavoritesListener);
        clear();
    }

    /**
     * Repaint the tables when the coordinate system changes.
     *
     * @since 2.35
     */
    private void repaintTables(CoordinateSystem coordinateSystem)
    {
        userFavoritesTable.repaint();
        projectFavoritesTable.repaint();
    }

    /**
     * Initialize the provided favorites table.
     *
     * @param favoritesTable The table to initialize
     * @param tableModel The model for the table
     * @param isUserFavoriteTable Indicates if the table is for storing user favorites
     */
    private void initTable(JTable favoritesTable, FavoriteTableModel tableModel, boolean isUserFavoriteTable)
    {
        favoritesTable.setModel(tableModel);
        favoritesTable.setTransferHandler(new TableRowTransferHandler(favoritesTable));
        favoritesTable.setTableHeader(null);
        favoritesTable.getSelectionModel().addListSelectionListener(new FavoritesTableSelectionListener());
        favoritesTable.setDefaultRenderer(Object.class, new FavoritesTableCellRenderer(isUserFavoriteTable));
    }

    private void buttonAddFavoriteActionPerformed()
    {
        if (addFavoriteDialog != null) addFavoriteDialog.dispose();
        addFavoriteDialog = new AddFavoriteDialog();
        addFavoriteDialog.setVisible(true);
    }

    private void buttonRemoveActionPerformed()
    {
        removeFavorite(getSelectedFavoriteAndType());
    }

    private FavoriteAndType getSelectedFavoriteAndType()
    {
        if (userFavoritesTable.getSelectedRowCount() > 0)
        {
            return new FavoriteAndType(userFavoritesTableModel.getValueAt(userFavoritesTable.getSelectedRow(), 0), true);
        } else if (projectFavoritesTable.getSelectedRowCount() > 0)
        {
            return new FavoriteAndType(projectFavoritesTableModel.getValueAt(projectFavoritesTable.getSelectedRow(), 0), false);
        } else
        {
            return null;
        }
    }

    private void favoritesTableMouseClicked(MouseEvent e)
    {
        if (e.getClickCount() == 2)
        {
            gotoFavorite(getSelectedFavoriteAndType());
        }
    }

    /**
     * Remove a favorite.
     *
     * @param favoriteAndType Favorite to remove
     */
    void removeFavorite(FavoriteAndType favoriteAndType)
    {
        if (favoriteAndType == null) return;

        final IFavorite favorite = favoriteAndType.favorite();
        final boolean isUserFavorite = favoriteAndType.isUserFavorite();
        if (JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete " + favorite.getDescription() + "?", "Delete " + (isUserFavorite ? "User" : "Project") + " favorite?",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
        {
            if (isUserFavorite)
            {
                userFavoritesManager.remove(favorite);
            } else
            {
                projectFavoritesManager.remove(favorite);
            }
        }
    }

    void gotoFavorite(FavoriteAndType favoriteAndType)
    {
        if (favoriteAndType == null) return;
        globeManager.getLastFocusedGlobe().goTo(favoriteAndType.favorite());
    }

    private void gotoButtonActionPerformed()
    {
        gotoFavorite(getSelectedFavoriteAndType());
    }

    public void clear()
    {
        coordinateSystemMonitor.dispose();
    }

    private void editButtonActionPerformed()
    {
        final FavoriteAndType favoriteAndType = getSelectedFavoriteAndType();

        if (favoriteAndType == null) return;

        if (editFavoriteDialog != null) editFavoriteDialog.dispose();
        editFavoriteDialog = new EditFavoriteDialog(favoriteAndType.favorite(), favoriteAndType.isUserFavorite());
        editFavoriteDialog.setVisible(true);
    }

    private void favoritesTableKeyPressed(KeyEvent e)
    {
        if (e.getKeyChar() == KeyEvent.VK_DELETE)
        {
            removeFavorite(getSelectedFavoriteAndType());
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panelButtons = new JPanel();
        buttonAddUser = new JButton();
        buttonRemove = new JButton();
        editButton = new JButton();
        gotoButton = new JButton();
        var favoritesPanel = new JPanel();
        var favoritesScrollPane = new JScrollPane();
        userFavoritesTable = new JTable();
        var projectFavoritesScrollPane = new JScrollPane();
        projectFavoritesTable = new JTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, fill:default:grow"));

        //======== panelButtons ========
        {
            panelButtons.setName("panelButtons");
            panelButtons.setLayout(new FormLayout(
                    "3*(default, $lcgap), default",
                    "fill:default"));

            //---- buttonAddUser ----
            buttonAddUser.setText("Add");
            buttonAddUser.setIconTextGap(1);
            buttonAddUser.setVerticalTextPosition(SwingConstants.BOTTOM);
            buttonAddUser.setToolTipText("Add New User Favorite");
            buttonAddUser.setHorizontalTextPosition(SwingConstants.CENTER);
            buttonAddUser.setName("buttonAddUser");
            buttonAddUser.addActionListener(e -> buttonAddFavoriteActionPerformed());
            panelButtons.add(buttonAddUser, cc.xy(1, 1));

            //---- buttonRemove ----
            buttonRemove.setText("Delete");
            buttonRemove.setToolTipText("Delete Selected Favorite");
            buttonRemove.setEnabled(false);
            buttonRemove.setIconTextGap(1);
            buttonRemove.setHorizontalTextPosition(SwingConstants.CENTER);
            buttonRemove.setVerticalTextPosition(SwingConstants.BOTTOM);
            buttonRemove.setName("buttonRemove");
            buttonRemove.addActionListener(e -> buttonRemoveActionPerformed());
            panelButtons.add(buttonRemove, cc.xy(3, 1));

            //---- editButton ----
            editButton.setText("Edit");
            editButton.setToolTipText("Edit the selected Project Favorite");
            editButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            editButton.setIconTextGap(1);
            editButton.setHorizontalTextPosition(SwingConstants.CENTER);
            editButton.setEnabled(false);
            editButton.setName("editButton");
            editButton.addActionListener(e -> editButtonActionPerformed());
            panelButtons.add(editButton, cc.xy(5, 1));

            //---- gotoButton ----
            gotoButton.setText("Go To");
            gotoButton.setToolTipText("Add New Project Favorite");
            gotoButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            gotoButton.setIconTextGap(1);
            gotoButton.setHorizontalTextPosition(SwingConstants.CENTER);
            gotoButton.setEnabled(false);
            gotoButton.setName("gotoButton");
            gotoButton.addActionListener(e -> gotoButtonActionPerformed());
            panelButtons.add(gotoButton, cc.xy(7, 1));
        }
        add(panelButtons, cc.xy(1, 1));

        //======== favoritesPanel ========
        {
            favoritesPanel.setName("favoritesPanel");
            favoritesPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow, $lgap, fill:default:grow"));

            //======== favoritesScrollPane ========
            {
                favoritesScrollPane.setBorder(new TitledBorder("User Favorites"));
                favoritesScrollPane.setName("favoritesScrollPane");

                //---- userFavoritesTable ----
                userFavoritesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                userFavoritesTable.setPreferredScrollableViewportSize(new Dimension(400, 100));
                userFavoritesTable.setDragEnabled(true);
                userFavoritesTable.setDropMode(DropMode.INSERT_ROWS);
                userFavoritesTable.setName("userFavoritesTable");
                userFavoritesTable.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        favoritesTableMouseClicked(e);
                    }
                });
                userFavoritesTable.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyPressed(KeyEvent e)
                    {
                        favoritesTableKeyPressed(e);
                    }
                });
                favoritesScrollPane.setViewportView(userFavoritesTable);
            }
            favoritesPanel.add(favoritesScrollPane, cc.xy(1, 1));

            //======== projectFavoritesScrollPane ========
            {
                projectFavoritesScrollPane.setBorder(new TitledBorder("Project Favorites"));
                projectFavoritesScrollPane.setName("projectFavoritesScrollPane");

                //---- projectFavoritesTable ----
                projectFavoritesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                projectFavoritesTable.setPreferredScrollableViewportSize(new Dimension(400, 100));
                projectFavoritesTable.setDragEnabled(true);
                projectFavoritesTable.setDropMode(DropMode.INSERT_ROWS);
                projectFavoritesTable.setName("projectFavoritesTable");
                projectFavoritesTable.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        favoritesTableMouseClicked(e);
                    }
                });
                projectFavoritesTable.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyPressed(KeyEvent e)
                    {
                        favoritesTableKeyPressed(e);
                    }
                });
                projectFavoritesScrollPane.setViewportView(projectFavoritesTable);
            }
            favoritesPanel.add(projectFavoritesScrollPane, cc.xy(1, 3));
        }
        add(favoritesPanel, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton buttonAddUser;
    private JButton buttonRemove;
    private JButton editButton;
    private JButton gotoButton;
    private JTable userFavoritesTable;
    private JTable projectFavoritesTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * Listener to handle favorite events for the provided table model.
     *
     * @since 2.37
     */
    private record FavoritesListener(FavoriteTableModel tableModel) implements IFavoritesListener
    {
        @Override
        public void onFavoriteAdded(IFavorite favorite)
        {
            tableModel.add(favorite);
        }

        @Override
        public void onFavoriteRemoved(IFavorite favorite)
        {
            tableModel.remove(favorite);
        }

        @Override
        public void onFavoriteChanged(IFavorite favorite, int index)
        {
            tableModel.fireTableRowsUpdated(index, index);
        }
    }

    /**
     * Cell renderer for a favorites table.
     */
    private static class FavoritesTableCellRenderer extends DefaultTableCellRenderer
    {
        private static final Icon USER_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "favorite_user.png");
        private static final Icon PROJECT_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "favorite_project.png");
        private final Icon entryIcon;
        private final boolean isUserFavoriteTable;
        private FavoriteTableEntry entry;
        private boolean rowHeightSet;

        /**
         * @param isUserFavoriteTable Indicates if the table is for storing user favorites
         */
        public FavoritesTableCellRenderer(boolean isUserFavoriteTable)
        {
            this.isUserFavoriteTable = isUserFavoriteTable;
            entryIcon = isUserFavoriteTable ? USER_ICON : PROJECT_ICON;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (entry != null)
            {
                if (!rowHeightSet)
                {
                    int rowHeight = Math.max(table.getRowHeight(row), entry.getPreferredSize().height) + 4;
                    table.setRowHeight(rowHeight);
                    rowHeightSet = true;
                }

                entry.setBackground(getBackground());
                entry.setForegroundForChildComponents(getForeground());
            }

            return entry != null ? entry : this;
        }

        @Override
        protected void setValue(Object value)
        {
            entry = new FavoriteTableEntry((IFavorite) value, isUserFavoriteTable, entryIcon);
        }
    }

    private static boolean working; // Can't have a static member in an inner class, so declare it here

    private class FavoritesTableSelectionListener implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (!e.getValueIsAdjusting())
            {
                if (!working)
                {
                    working = true;

                    // Deselect any selections in the other table
                    if (e.getSource() == userFavoritesTable.getSelectionModel())
                    {
                        projectFavoritesTable.clearSelection();
                    } else
                    {
                        userFavoritesTable.clearSelection();
                    }

                    // Enable/disable the buttons depending if a favorite is or isn't selected
                    final boolean enabled = (userFavoritesTable.getSelectedRowCount() + projectFavoritesTable.getSelectedRowCount()) > 0;
                    buttonRemove.setEnabled(enabled);
                    gotoButton.setEnabled(enabled);
                    editButton.setEnabled(enabled);

                    working = false;
                }
            }
        }
    }

    /**
     * Record to identify a favorite and its type.
     *
     * @since 3.4 (converted to an internal record)
     */
    record FavoriteAndType(IFavorite favorite, boolean isUserFavorite)
    {
    }
}
