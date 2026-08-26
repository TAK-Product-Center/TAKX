package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXTextField;

import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;

/**
 * Extension to {@link JXTextField} with convenience methods to prevent swing internal casts.
 * 
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class JXTextFieldExt extends JXTextField
{
    /**
     * @param documentFilter the document filter to apply to the underlying text field document
     * @throws IllegalStateException if the text field document is not an {@link AbstractDocument}
     */
    public void setDocumentFilter(@Nullable DocumentFilter documentFilter)
    {
        if (getDocument() instanceof AbstractDocument abstractDocument)
        {
            abstractDocument.setDocumentFilter(documentFilter);
        } else
        {
            throw new IllegalStateException("TextField document is not an instance of AbstractDocument");
        }
    }
}
