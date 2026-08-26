package gov.takx.commons.swing;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.6
 */
class FormButtonBarTest
{
    private FormButtonBar buttonBar;

    @BeforeEach
    public void beforeEach()
    {
        buttonBar = new FormButtonBar();
    }

    @Test
    public void testClean()
    {
        JComboBox<String> field = new JComboBox<>(new String[]{"a", "b"});
        buttonBar.attachField(field, object -> null);
        assertThat(buttonBar.isClean()).isTrue();
        field.setSelectedIndex(0);
        assertThat(buttonBar.isClean()).isFalse();
    }

    @Test
    public void testError()
    {
        JComboBox<String> field = new JComboBox<>(new String[]{"a", "b"});
        String errorText = "error";
        buttonBar.attachField(field, object -> errorText);
        field.setSelectedIndex(0);
        assertThat(buttonBar.isError()).isTrue();
        assertThat(buttonBar.getError()).isEqualTo(errorText);
    }

    @Test
    public void testApply()
    {
        JComboBox<String> field = new JComboBox<>(new String[]{"a", "b"});
        buttonBar.attachField(field, null);
        field.setSelectedIndex(0);
        MutableBoolean applied = new MutableBoolean(false);

        FormButtonBar.ApplyListener al = ignored -> applied.setTrue();
        buttonBar.addApplyListener(al);
        buttonBar.getApplyButton().doClick();
        assertThat(applied.getValue()).isTrue();
        buttonBar.removeApplyListener(al);
        assertThat(List.of(buttonBar.getListeners(FormButtonBar.ApplyListener.class))).doesNotContain(al);
    }

    @Test
    public void testCancel()
    {
        MutableBoolean closed = new MutableBoolean(false);
        FormButtonBar.CloseListener cl = ignored -> closed.setTrue();
        buttonBar.addCloseListener(cl);
        buttonBar.getCancelButton().doClick();
        assertThat(closed.getValue()).isTrue();
        buttonBar.removeCloseListener(cl);
        assertThat(List.of(buttonBar.getListeners(FormButtonBar.CloseListener.class))).doesNotContain(cl);
    }

    /**
     * @since 5.7
     */
    @Test
    public void testDelete()
    {
        MutableBoolean clicked = new MutableBoolean(false);
        FormButtonBar.DeleteListener listener = ignored -> clicked.setTrue();
        buttonBar.addDeleteListener(listener);
        buttonBar.getDeleteButton().doClick();
        assertThat(clicked.getValue()).isTrue();
        buttonBar.removeDeleteListener(listener);
        assertThat(List.of(buttonBar.getListeners(FormButtonBar.DeleteListener.class))).doesNotContain(listener);
    }

    /**
     * @since 5.7
     */
    @Test
    public void testTextField()
    {
        var errorValue = "error value";
        var errorMessage = "error message";
        var field = new JTextField();
        buttonBar.attachField(field, event -> field.getText().equals(errorValue) ? errorMessage : null);
        field.setText("");
        assertThat(buttonBar.isError()).isFalse();
        field.setText(errorValue);
        assertThat(buttonBar.isError()).isTrue();
        assertThat(buttonBar.getError()).isEqualTo(errorMessage);
    }
}
