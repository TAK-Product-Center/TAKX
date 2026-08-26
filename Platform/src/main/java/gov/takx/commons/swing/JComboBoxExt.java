package gov.takx.commons.swing;

import jakarta.annotation.Nullable;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

import java.util.Vector;

/**
 * Extension of {@link JComboBox} with convenience features, specifically returning the typed value to prevent unnecessary casts.
 * 
 * @param <E> the model type
 * @since 5.6
 */
public class JComboBoxExt<E> extends JComboBox<E>
{
    /**
     * Construct a new instance using the given model.
     * 
     * @param aModel the model
     */
    @SuppressWarnings("unused")
    public JComboBoxExt(ComboBoxModel<E> aModel)
    {
        super(aModel);
    }

    /**
     * Construct a new instance using the given items.
     * 
     * @param items the items to fill the default model
     */
    public JComboBoxExt(E[] items)
    {
        super(items);
    }

    /**
     * Construct a new instance using the given items.
     * 
     * @param items the items to fill the default model
     */
    @SuppressWarnings("unused")
    public JComboBoxExt(Vector<E> items)
    {
        super(items);
    }

    /**
     * Construct a new instance with an empty model.
     */
    @SuppressWarnings("unused")
    public JComboBoxExt()
    {
    }

    /**
     * @return the selected item, cast to the generic type {@code <E>}
     * @throws ClassCastException if the field is editable and the value is not of the generic type
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public E getTypedSelectedItem()
    {
        return (E) getSelectedItem();
    }
}
