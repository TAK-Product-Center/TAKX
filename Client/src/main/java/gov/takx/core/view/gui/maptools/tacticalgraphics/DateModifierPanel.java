package gov.takx.core.view.gui.maptools.tacticalgraphics;

import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.border.Border;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Displays an editor for modifiers with date values.
 *
 * @since 5.1
 */
public class DateModifierPanel extends AModifierPanel<DateTimePicker>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // The persisted date time format is ddHHmmssZMMMyy, splitting the day from the month and year, so we'll just use
    // the same format as ATAK when displaying the date in the date chooser
    private static final DateTimeFormatter DATE_FORMATTER_DISPLAY_ONLY = new DateTimeFormatterBuilder().appendPattern("dd MMM yyyy").toFormatter();

    /**
     * @param modifierId The modifier id
     * @param name The modifier name
     * @param valueSupplier The supplier that retrieves the currently persisted modifier value
     * @param valueChangeListener Listener that's notified when the editor value changes
     * @param commitConsumer Consumer that handles request to persist the editor's value. Pass the modifier id and
     *                       editor value to the consumer when the value should be committed.
     */
    public DateModifierPanel(@Nonnull String modifierId, @Nonnull String name, @Nonnull Supplier<String> valueSupplier,
                             @Nonnull Consumer<String> valueChangeListener, @Nonnull BiConsumer<String, String> commitConsumer)
    {
        super(modifierId, name, new DateTimePicker(getDatePickerSettings(), getTimePickerSettings()), valueSupplier,
                commitConsumer, editor -> getDateTimeString(editor.getDateTimePermissive()));

        refresh();

        // Clean up the date and time text field borders so they match the rest of the UI
        final Border border = editor.getDatePicker().getComponentToggleCalendarButton().getBorder();
        editor.getDatePicker().getComponentDateTextField().setBorder(border);
        editor.getTimePicker().getComponentTimeTextField().setBorder(border);

        editor.addDateTimeChangeListener(e -> notifyListener(valueChangeListener));

        // This notifies the listener when the user modifies the date text with the keyboard
        editor.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                notifyListener(valueChangeListener);
            }
        });
    }

    @Override
    protected void refresh()
    {
        final String value = valueSupplier.get();

        editor.setDateTimePermissive(StringUtils.isEmpty(value)
                ? null
                : LocalDateTime.parse(value, SymbolUtilities.getNatoDateTimeFormatter()));
    }

    @Override
    public void commit()
    {
        super.commit();

        refresh(); // ensure both the date and time match the persisted value
    }

    @Override
    protected void setModifierComponentEnabled(boolean enabled)
    {
        editor.setEnabled(enabled);
    }

    /**
     * Notifies the listener if the data is valid and the value has changed.
     *
     * @param valueChangeListener The listener to notify
     */
    private void notifyListener(@Nonnull Consumer<String> valueChangeListener)
    {
        try
        {
            valueChangeListener.accept(getDateTimeString(editor.getDateTimePermissive()));
        } catch (UnsupportedTemporalTypeException e)
        {
            logger.error("Error parsing date/time", e);
        }
    }

    /**
     * @return custom date picker settings
     */
    private static DatePickerSettings getDatePickerSettings()
    {
        final DatePickerSettings settings = new DatePickerSettings();

        setColors(DatePickerSettings.DateArea.values(), settings::setColor);

        final Color background = new JLabel().getBackground();
        settings.setColorBackgroundWeekdayLabels(background, true);
        settings.setColorBackgroundWeekNumberLabels(background, true);

        settings.setFormatForDatesCommonEra(DATE_FORMATTER_DISPLAY_ONLY);
        settings.setAllowKeyboardEditing(false);

        return settings;
    }

    /**
     * @return custom time picker settings
     */
    private static TimePickerSettings getTimePickerSettings()
    {
        final TimePickerSettings settings = new TimePickerSettings();

        setColors(TimePickerSettings.TimeArea.values(), settings::setColor);

        settings.generatePotentialMenuTimes(TimePickerSettings.TimeIncrement.FifteenMinutes, null, null);

        return settings;
    }

    /**
     * Sets date/time area colors so they match the application.
     *
     * @param componentAreas The areas to update
     * @param colorSetter The consumer that will set the area color
     * @param <T> The date or time component area enum
     */
    private static <T extends Enum<T>> void setColors(@Nonnull T[] componentAreas,
                                                      @Nonnull BiConsumer<T, Color> colorSetter)
    {
        final JLabel label = new JLabel();

        final Color background = label.getBackground();
        final Color selectedBackground = Color.gray;
        final Color foreground = label.getForeground();

        for (T componentArea : componentAreas)
        {
            final String areaName = componentArea.name();

            if (areaName.contains("Background")) // check this first; some names contain "text" and "background"
            {
                colorSetter.accept(componentArea, background);
            } else if (areaName.contains("Selected"))
            {
                colorSetter.accept(componentArea, selectedBackground);
            } else if (areaName.contains("Text"))
            {
                colorSetter.accept(componentArea, foreground);
            } else
            {
                colorSetter.accept(componentArea, background);
            }
        }
    }

    /**
     * @param localDateTime the date/time to convert to a String
     * @return the String representation of the date/time, or an empty String if if the date/time is null (unset)
     */
    private static String getDateTimeString(@Nullable LocalDateTime localDateTime)
    {
        return localDateTime != null
                ? SymbolUtilities.getNatoDateTimeFormatter().format(localDateTime.atZone(ZoneId.systemDefault())) // todo: is this correct?
                : "";
    }
}
