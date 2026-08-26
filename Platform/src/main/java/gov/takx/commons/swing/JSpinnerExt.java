package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import java.awt.event.FocusListener;

/**
 * Extension of JSpinner that provides convenience methods. Specifically, setting and getting typed values. To prevent
 * confusing issues, most methods can ONLY be used if the component has the correct model or editor, and will throw
 * exceptions if it does not.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class JSpinnerExt extends JSpinner
{
    /**
     * @return the spinner number model
     * @throws IllegalStateException if the spinner model is not an instance of {@link SpinnerNumberModel}
     */
    @Nonnull
    public SpinnerNumberModel getNumberModel()
    {
        var model = getModel();
        if (model instanceof SpinnerNumberModel nm) return nm;
        throw new IllegalStateException("Model is not a SpinnerNumberModel");
    }

    /**
     * @return the spinner value as a Number
     * @throws IllegalStateException if the spinner model is not an instance of {@link SpinnerNumberModel}
     */
    @Nonnull
    public Number getValueNumber()
    {
        return getNumberModel().getNumber();
    }

    /**
     * @return the spinner value as an {@code int}
     * @throws IllegalStateException if the spinner model is not an instance of {@link SpinnerNumberModel}
     */
    public int getValueInt()
    {
        return getValueNumber().intValue();
    }

    /**
     * @return the spinner value as a {@code long}
     * @throws IllegalStateException if the spinner model is not an instance of {@link SpinnerNumberModel}
     */
    public long getValueLong()
    {
        return getValueNumber().longValue();
    }

    /**
     * @return the spinner value as an {@code double}
     * @throws IllegalStateException if the spinner model is not an instance of {@link SpinnerNumberModel}
     */
    public double getValueDouble()
    {
        return getValueNumber().doubleValue();
    }

    /**
     * @return the spinner value as an {@code float}
     * @throws IllegalStateException if the spinner model is not an instance of {@link SpinnerNumberModel}
     */
    public float getValueFloat()
    {
        return getValueNumber().floatValue();
    }

    /**
     * @param value the value to set on the spinner
     */
    public void setValueDouble(double value)
    {
        setValue(value);
    }

    /**
     * @param value the value to set on the spinner
     */
    public void setValueInt(int value)
    {
        setValue(value);
    }

    /**
     * @param listener a focus listener to add to the editor text field
     * @throws IllegalStateException if the spinner model is not an instance of {@link DefaultEditor}
     */
    @Override
    public void addFocusListener(@Nonnull FocusListener listener)
    {
        if (getEditor() instanceof DefaultEditor editor)
        {
            editor.getTextField().addFocusListener(listener);
        } else
        {
            throw new IllegalStateException("Editor is not a JSpinner.DefaultEditor");
        }
    }

    /**
     * @param listener a focus listener to remove from the editor text field
     * @throws IllegalStateException if the spinner model is not an instance of {@link DefaultEditor}
     */
    @Override
    public synchronized void removeFocusListener(@Nonnull FocusListener listener)
    {
        if (getEditor() instanceof DefaultEditor editor)
        {
            editor.getTextField().removeFocusListener(listener);
        } else
        {
            throw new IllegalStateException("Editor is not a JSpinner.DefaultEditor");
        }
    }
}
