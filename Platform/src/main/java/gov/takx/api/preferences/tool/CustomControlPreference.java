package gov.takx.api.preferences.tool;

import jakarta.annotation.Nonnull;

import javax.swing.JComponent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Defines a String {@link gov.takx.platform.preferences.PreferenceEntry} that will be controlled by a custom JComponent.
 *
 * @since 5.0
 */
public class CustomControlPreference extends AToolPreference
{
    @Nonnull
    private final JComponent control;
    @Nonnull
    private final Supplier<String> valueSupplier;
    @Nonnull
    private final Consumer<String> valueRestorer;

    /**
     * @param id The preference id. This id will be combined with the tool uid to form a unique preference entry id.
     *           See {@link IToolPreference#getId()}.
     * @param title The title that will be displayed in the tool preference list
     * @param description The description that will be displayed in the tool preference list
     * @param defaultValue The initial value to use if no value has been stored
     */
    public CustomControlPreference(@Nonnull String id, @Nonnull String title, @Nonnull String description,
                                   @Nonnull String defaultValue, @Nonnull JComponent control,
                                   @Nonnull Supplier<String> valueSupplier, @Nonnull Consumer<String> valueRestorer)
    {
        super(id, title, description, defaultValue);

        this.control = control;
        this.valueSupplier = valueSupplier;
        this.valueRestorer = valueRestorer;
    }

    /**
     * @return The component that will be used to control the preference value
     */
    @Nonnull
    public JComponent getControl()
    {
        return control;
    }

    /**
     * @return A supplier that provides the control component's current value in the form of a String
     */
    @Nonnull
    public Supplier<String> getValueSupplier()
    {
        return valueSupplier;
    }

    /**
     * @return A consumer that accepts the preference's value in the form of a String and updates the control Component
     * with the value
     */
    @Nonnull
    public Consumer<String> getValueRestorer()
    {
        return valueRestorer;
    }
}