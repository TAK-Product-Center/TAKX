package gov.takx.commons.swing;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.7
 */
class CombinedDocumentListenerTest
{
    @Test
    public void test()
    {
        var textField = new JTextField();
        var eventHolder = new MutableObject<DocumentEvent>();
        var listener = (CombinedDocumentListener) eventHolder::setValue;
        textField.getDocument().addDocumentListener(listener);
        textField.setText("text");
        assertThat(eventHolder.getValue()).isNotNull();
        assertThat(eventHolder.getValue().getType()).isEqualTo(DocumentEvent.EventType.INSERT);
        textField.setText(null);
        assertThat(eventHolder.getValue()).isNotNull();
        assertThat(eventHolder.getValue().getType()).isEqualTo(DocumentEvent.EventType.REMOVE);
    }
}
