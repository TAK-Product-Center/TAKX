package gov.takx.api.globe;

/**
 */
public interface IProjectFavoritesManager extends IFavoritesManager
{
    IFavorite getDefault();

    IFavorite getLast();

    void setDefault(IFavorite bookmark);

    void setLast(IFavorite bookmark);
}
