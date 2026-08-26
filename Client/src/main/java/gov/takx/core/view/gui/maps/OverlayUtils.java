package gov.takx.core.view.gui.maps;

import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 */
public class OverlayUtils
{
    /**
     * Prompts the user if they want to use simple or advanced for overlay importing.
     * @param importFileType Overlay type to show in the prompt title.
     * @return {@code true} for advanced, {@code false} for simple, and {@code null} when the user cancelled the import.
     */
    @Nullable
    public static Boolean requestOverlayTypeFromUser(final String importFileType)
    {
        // 3D shapes have been detected, so present a confirm dialog to the user.
        String advanceImport = "Advanced Shape Import";
        String simpleImport = "Simple Overlay Import";
        String importQuestion = "How should the shape(s) be imported?";
        Object[] options = {advanceImport, simpleImport};

        int response = AlwaysOnTopJOptionPane.showOptionDialog(JFrame.getFrames()[0],
                "<html>" + importQuestion + "<br><br>" +

                        advanceImport + " -&gt; Shapes from imported files will be available to plugins for calculations " +
                        "(line-of-sight, occlusion analysis).<br><em>&#9;Note: This will consume more system resources</em><br><br>" +

                        simpleImport + " -&gt; Shapes from imported file will be for visualization only (imagery overlay)</html>",
                importFileType,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                options,
                options[0]);

        // We only act on explicit YES/NO responses, as closing the dialog via the "x" button should
        // cancel the import and not be handled as either of the import modes.
        if (response == JOptionPane.YES_OPTION)
        {
            return true;
        } else if (response == JOptionPane.NO_OPTION)
        {
            return false;
        }
        return null;
    }

    /**
     * Prompts the user if they want to use simple or advanced for overlay importing, and processes their choice.
     * @param controller Controller to pull the overlay type from.
     */
    public static void handleOverlayInput(@Nonnull AddOverlayController controller)
    {
        Boolean importAsInteractiveOverlay = requestOverlayTypeFromUser(controller.getType());
        if (importAsInteractiveOverlay != null)
        {
            controller.doImportFile(importAsInteractiveOverlay);
        }
    }
}
