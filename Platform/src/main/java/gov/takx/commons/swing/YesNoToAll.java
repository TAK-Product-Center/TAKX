package gov.takx.commons.swing;

import java.util.Arrays;

/**
 */
public enum YesNoToAll
{
    YES, YES_TO_ALL, NO, NO_TO_ALL;

    private static final Object[] OPTIONS = new Object[]{"Yes", "Yes to All", "No", "No to All"};

    public static YesNoToAll getYesNoToAll(int value)
    {
        // There are places where we pass the result of a JOptionPane dialog directly in to this
        // method where the user clicks on the "X" of the dialog, which returns JOptionPane.CLOSED = -1;
        if (value < 0)
        {
            return NO_TO_ALL;
        }
        return values()[value];
    }

    public static Object getYesNoToAllDefaultOption()
    {
        return OPTIONS[0];
    }

    public static Object[] getYesNoToAllOptions()
    {
        return Arrays.copyOf(OPTIONS, OPTIONS.length);
    }

    public boolean isYes()
    {
        return this == YES || this == YES_TO_ALL;
    }

    public boolean isNo()
    {
        return this == NO || this == NO_TO_ALL;
    }

    public boolean isToAll()
    {
        return this == YES_TO_ALL || this == NO_TO_ALL;
    }
}
