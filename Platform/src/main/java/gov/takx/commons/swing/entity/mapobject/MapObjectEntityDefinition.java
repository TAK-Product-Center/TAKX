package gov.takx.commons.swing.entity.mapobject;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.entity.common.CommonEntityDefinition;
import jakarta.annotation.Nonnull;

import javax.swing.Icon;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Entity definition for {@link IMapObject} types.
 *
 * @param <E> the entity type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class MapObjectEntityDefinition<E extends IMapObject> extends CommonEntityDefinition<E>
{
    /**
     * Construct a new instance, adding default property listeners.
     */
    public MapObjectEntityDefinition()
    {
        addPropertyChangeMapping(List.of(IMapObject.NAME), List.of(getSummaryColumn(), getNameColumn()));
        addPropertyChangeMapping(IMapObject.ICON, getIconColumn());
    }

    @Override
    protected Icon getIcon(@Nonnull E entity)
    {
        return entity.getSizedIcon(ImageUtils.MEDIUM_ICON_WIDTH);
    }

    @Override
    protected String getName(@Nonnull E entity)
    {
        return entity.getName();
    }

    @Override
    protected void addPropertyChangeListener(@Nonnull E entity, @Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
        entity.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    protected void removePropertyChangeListener(@Nonnull E entity, @Nonnull String propertyName, @Nonnull PropertyChangeListener propertyChangeListener)
    {
        entity.removePropertyChangeListener(propertyName, propertyChangeListener);
    }

    @Override
    protected void toggleVisibility(@Nonnull E entity)
    {
        entity.setVisible(!entity.isVisible());
    }

    @Override
    protected boolean isVisible(@Nonnull E entity)
    {
        return entity.isVisible();
    }
}
