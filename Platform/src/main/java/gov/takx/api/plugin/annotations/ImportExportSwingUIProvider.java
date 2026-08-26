package gov.takx.api.plugin.annotations;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.api.ui.IImportExportSwingUIProvider;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark the class that provides the Swing UI for a given Import Export plugin wizard.
 * The class on which this annotation is used must implement {@link IImportExportSwingUIProvider}.
 * <p>
 * There must be exactly one constructor, and it can optionally have the following argument which will be supplied
 * automatically when the object is instantiated.
 * <ul>
 * <li>{@link IImportExportWizard}, the import/export wizard for which this UI provider is being created</li>
 * </ul>
 *
 * @since 2.13
 */
@Stereotype
@PseudoBean
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImportExportSwingUIProvider
{
    /**
     * This value specifies the Wizard class for which this is the UI provider.
     *
     * @return The class of the Wizard for which this class provides the Swing UI
     */
    Class<? extends IImportExportWizard> forWizard();
}
