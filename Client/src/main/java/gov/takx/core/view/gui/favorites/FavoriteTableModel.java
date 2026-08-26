package gov.takx.core.view.gui.favorites;

import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IFavoritesManager;
import jakarta.annotation.Nonnull;

import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for maintaining {@link gov.takx.api.globe.IFavorite} objects in a table.
 *
 * @since 3.4
 */
public class FavoriteTableModel extends AbstractTableModel implements IDraggableTableModel
{
    private final List<IFavorite> favorites;
    private final IFavoritesManager favoritesManager;

    /**
     * @param favoritesManager The manager for the favorites contained in the model
     */
    public FavoriteTableModel(@Nonnull IFavoritesManager favoritesManager)
    {
        this.favoritesManager = favoritesManager;
        favorites = new ArrayList<>(favoritesManager.getBookmarks());
    }

    /**
     * Add a row with the provided favorite.
     * 
     * @param favorite The favorite to add
     */
    public void add(@Nonnull IFavorite favorite)
    {
        final int index = favorites.size();
        favorites.add(favorite);
        fireTableRowsInserted(index, index);
    }

    @Override
    public void moveRow(int sourceIndex, int destinationIndex)
    {
        int firstIndex;
        int lastIndex;

        // If the destination index is below the original index of the row, subtract one to account for removing
        // the row at the original location
        int newIndex = destinationIndex;
        if (destinationIndex > sourceIndex)
        {
            newIndex--;
            firstIndex = sourceIndex;
            lastIndex = destinationIndex;
        } else
        {
            firstIndex = destinationIndex;
            lastIndex = sourceIndex;
        }

        final IFavorite favorite = favorites.remove(sourceIndex);
        favorites.add(newIndex, favorite);

        fireTableRowsUpdated(firstIndex, lastIndex);

        // Notify the favorites manager of the change caused by the UI so the index can be updated
        favoritesManager.setIndex(favorite, newIndex);
    }

    /**
     * Remove the row containing the provided favorite.
     * 
     * @param favorite The favorite to remove
     */
    public void remove(@Nonnull IFavorite favorite)
    {
        final int index = favorites.indexOf(favorite);
        if (index >= 0 && favorites.remove(favorite))
        {
            fireTableRowsDeleted(index, index);
        }
    }

    @Override
    public int getRowCount()
    {
        return favorites.size();
    }

    @Override
    public int getColumnCount()
    {
        return 1;
    }

    @Override
    public IFavorite getValueAt(int rowIndex, int columnIndex)
    {
        return favorites.get(rowIndex);
    }
}
