package gov.takx.core.view.gui.preferences;

import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.commons.swing.SliderControlPanel;
import gov.takx.commons.swing.TextComponentResizeHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Collection of factory methods to assist in the creation of UI components for editing/displaying preference values.
 *
 * @since 5.4
 */
public class PreferencesComponentFactory
{
    // Handler per preference page (actually the main scroll pane of that page)
    private static final Map<JScrollPane, TextComponentResizeHandler> resizeHandlers = new HashMap<>();

    /**
     * @return Text field editor control
     */
    public static IEditorComponent makeTextEditor()
    {
        return new TextEditor();
    }

    /**
     * @return Integer slider editor control with the given min/max
     */
    public static IEditorComponent makeSliderEditor(int min, int max)
    {
        return new SliderEditor(min, max, null);
    }

    /**
     * @return Integer slider editor control with the given min/max
     */
    public static IEditorComponent makeSliderEditor(int min, int max, @Nullable Float scaleFactor)
    {
        return new SliderEditor(min, max, scaleFactor);
    }

    /**
     * @return Integer spinner editor control with min value of 1, no max value, and a step of 1
     */
    public static IEditorComponent makeIntSpinnerEditor()
    {
        return makeIntSpinnerEditor(1, null, 1);
    }

    /**
     * @return Integer spinner editor control
     */
    public static IEditorComponent makeIntSpinnerEditor(@Nullable Integer min, @Nullable Integer max, int step)
    {
        //noinspection DataFlowIssue - SpinnerNumberModel allows null min/max
        final var spinnerEditor = new SpinnerEditor(new SpinnerNumberModel((Number) min, min, max, step));

        // hide the comma in the numeric value
        spinnerEditor.setEditor(new JSpinner.NumberEditor(spinnerEditor, "#"));

        return spinnerEditor;
    }

    /**
     * @return Integer spinner editor control, preconfigured for a port number
     */
    public static IEditorComponent makePortNumberEditor()
    {
        return makeIntSpinnerEditor(1, 65_535, 1);
    }

    /**
     * @param items List of values for the combobox
     * @return ComboBox editor control
     */
    public static IEditorComponent makeComboBox(@Nonnull List<String> items)
    {
        return new StringComboBoxEditor(items);
    }

    /**
     * @param enumClass Enum class from which to get the value list
     * @return Combobox editor control
     */
    public static <T extends Enum<?>> IEditorComponent makeComboBox(@Nonnull Class<T> enumClass)
    {
        return makeComboBox(enumClass, false, null);
    }

    /**
     * @param enumClass Enum class from which to get the value list
     * @param subset Subset of full enumeration to include in the combobox
     * @return Combobox editor control
     */
    public static <T extends Enum<?>> IEditorComponent makeComboBox(@Nonnull Class<T> enumClass, @Nullable List<T> subset)
    {
        return makeComboBox(enumClass, false, subset);
    }

    /**
     * @param enumClass Enum class from which to get the value list
     * @param useToString Pass {@code true} to use the {@code toString} value of the enum to match
     * @param subset Subset of full enumeration to include in the combobox
     * @return Combobox editor control
     */
    public static <T extends Enum<?>> IEditorComponent makeComboBox(@Nonnull Class<T> enumClass, boolean useToString, @Nullable List<T> subset)
    {
        return new EnumComboBoxEditor(enumClass, useToString, subset);
    }

    /**
     * Configure a text area so that it can be used as a display that wraps lines and resizes as its parent resizes.
     *
     * @param textArea to configure
     * @param parent Component to monitor for size changes
     */
    public static JTextArea configureResizingTextArea(@Nonnull JTextArea textArea, @Nonnull JScrollPane parent)
    {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setBorder(null);

        resizeHandlers.computeIfAbsent(parent, k -> new TextComponentResizeHandler(parent)).addTextComponent(textArea);
        return textArea;
    }

    /**
     * Editor for an integer value using a slider.
     */
    private static class SliderEditor extends SliderControlPanel implements IEditorComponent
    {
        private final Float scaleFactor;

        /**
         * @param min minimum value
         * @param max maximum value
         * @param scaleFactor If not {@code null}, then the value stored in the preferences is scaled by this value.
         *                    This is used to handle cases where a floating point value is stored in preferences, but we
         *                    want to use a slider (which only operates on integers) as the editor.
         */
        SliderEditor(int min, int max, @Nullable Float scaleFactor)
        {
            super(min, max, e -> {
            });
            this.scaleFactor = scaleFactor;
        }

        @Override
        public void updateFromPreference(@Nonnull IPreferenceEntry entry)
        {
            setValue(getScaledValue(entry));
        }

        @Override
        public void applyToPreference(@Nonnull IPreferenceEntry entry)
        {
            if (scaleFactor != null)
            {
                entry.putAndSave(getValue() / scaleFactor);
            } else
            {
                entry.putAndSave(getValue());
            }
        }

        @Override
        public String getDisplayValue(@Nonnull IPreferenceEntry entry)
        {
            return String.valueOf(getScaledValue(entry));
        }

        private int getScaledValue(IPreferenceEntry entry)
        {
            return scaleFactor != null ? (int) (entry.getDoubleValue() * scaleFactor) : entry.getIntegerValue();
        }
    }

    /**
     * Editor for a numeric value using a spinner.
     */
    private static class SpinnerEditor extends JSpinner implements IEditorComponent
    {
        SpinnerEditor(@Nonnull SpinnerNumberModel model)
        {
            super(model);
        }

        @Override
        public void updateFromPreference(@Nonnull IPreferenceEntry entry)
        {
            setValue(entry.getIntegerValue());
        }

        @Override
        public void applyToPreference(@Nonnull IPreferenceEntry entry)
        {
            entry.putAndSave((int) getValue());
        }
    }

    /**
     * Editor for text values.
     */
    private static class TextEditor extends JTextField implements IEditorComponent
    {
        @Override
        public void updateFromPreference(@Nonnull IPreferenceEntry entry)
        {
            setText(entry.getStringValue());
        }

        @Override
        public void applyToPreference(@Nonnull IPreferenceEntry entry)
        {
            entry.putAndSave(getText());
        }
    }

    /**
     * ComboBox editor that takes a given list to supply the values.
     */
    private static class StringComboBoxEditor extends JComboBox<String> implements IEditorComponent
    {
        StringComboBoxEditor(List<String> items)
        {
            setModel(new DefaultComboBoxModel<>(new Vector<>(items)));
        }

        @Override
        public void updateFromPreference(@Nonnull IPreferenceEntry entry)
        {
            setSelectedItem(entry.getStringValue());
        }

        @Override
        public void applyToPreference(@Nonnull IPreferenceEntry entry)
        {
            entry.putAndSave((String) getSelectedItem());
        }
    }

    /**
     * ComboBox editor that takes an enum to supply the values.
     */
    private static class EnumComboBoxEditor extends JComboBox<Enum<?>> implements IEditorComponent
    {
        private final Class<? extends Enum<?>> enumClass;
        private final boolean useToString;

        /**
         * @param enumClass Enumeration to pull values from
         * @param useToString Pass {@code true} to use the {@code toString} value of the enum to match
         * @param subset Subset of full enumeration to include in the combobox
         * @param <T> Inferred enum type
         */
        <T extends Enum<?>> EnumComboBoxEditor(@Nonnull Class<T> enumClass, boolean useToString, @Nullable List<T> subset)
        {
            this.enumClass = enumClass;
            this.useToString = useToString;

            final var list = subset != null ? subset : Arrays.asList(enumClass.getEnumConstants());
            setModel(new DefaultComboBoxModel<>(new Vector<>(list)));
        }

        @Override
        public void updateFromPreference(@Nonnull IPreferenceEntry entry)
        {
            final Enum<?> theEnum;

            if (useToString)
            {
                final String stringValue = entry.getStringValue();
                theEnum = Arrays.stream(enumClass.getEnumConstants())
                        .filter(e -> stringValue.equals(e.toString()))
                        .findFirst().orElse(null);
            } else
            {
                theEnum = entry.getValueAs(enumClass);
            }
            setSelectedItem(theEnum);
        }

        @Override
        public void applyToPreference(@Nonnull IPreferenceEntry entry)
        {
            final var selectedItem = getItemAt(getSelectedIndex());
            final String value = selectedItem == null ? "" : useToString ? selectedItem.toString() : selectedItem.name();

            entry.putAndSave(value);
        }

        @Override
        public String getDisplayValue(@Nonnull IPreferenceEntry entry)
        {
            return entry.getValueAs(enumClass).toString();
        }
    }

    private PreferencesComponentFactory()
    {
    }
}
