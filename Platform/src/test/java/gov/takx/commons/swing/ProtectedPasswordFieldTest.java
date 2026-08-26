package gov.takx.commons.swing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import java.awt.event.FocusEvent;

/**
 * @since 5.1
 */
public class ProtectedPasswordFieldTest
{

    @Test
    public void testFocus() throws BadLocationException
    {
        String value = "value";

        ProtectedPasswordField field = new ProtectedPasswordField();

        // After setting text programmatically the password should be set but the parent text value should be the placeholder
        field.setText("value");
        Assertions.assertThat(field.getText()).isEmpty();
        Assertions.assertThat(String.valueOf(field.getPassword())).isEqualTo(value);

    }

    @Test
    public void testInput() throws BadLocationException
    {
        String value = "value";

        ProtectedPasswordField field = new ProtectedPasswordField();

        // After setting text programmatically the password should be set but the parent text value should be empty
        field.setText("value");
        Assertions.assertThat(field.getText()).isEmpty();
        Assertions.assertThat(String.valueOf(field.getPassword())).isEqualTo(value);

        String uservalue = "userValue";
        Document doc = field.getDocument();
        if (doc instanceof AbstractDocument)
        {
            ((AbstractDocument) doc).replace(0, doc.getLength(), uservalue, null);
        } else
        {
            doc.remove(0, doc.getLength());
            doc.insertString(0, uservalue, null);
        }

        //after user input the protection should be disabled
        field.focusLost(new FocusEvent(field, FocusEvent.FOCUS_LOST));
        Assertions.assertThat(field.getText()).isEqualTo(uservalue);
        Assertions.assertThat(String.valueOf(field.getPassword())).isEqualTo(uservalue);
    }

    @Test
    public void testClear() throws BadLocationException
    {
        String value = "value";

        ProtectedPasswordField field = new ProtectedPasswordField();

        // After setting text programmatically the password should be set but the parent text value should be the placeholder
        field.setText("value");
        Assertions.assertThat(field.getText()).isEmpty();
        Assertions.assertThat(String.valueOf(field.getPassword())).isEqualTo(value);

        field.getClearButton().doClick();

        Assertions.assertThat(field.getText()).isEmpty();
        Assertions.assertThat(String.valueOf(field.getPassword())).isEmpty();
    }
}