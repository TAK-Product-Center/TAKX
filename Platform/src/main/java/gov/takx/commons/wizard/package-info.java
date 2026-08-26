/**
 * Provides a general purpose wizard framework capable of complex branching logic.
 * <p>
 * A wizard is implemented as a set of {@link gov.takx.commons.wizard.IWizardPanelDescriptor}. Each has an unique
 * identifier, and each panel descriptor provides the identifiers of the next and previous panels. This identifier can be
 * hard coded if the wizard is linear, or selected based on user input, to implement a branching wizard. Many wizards
 * require only a linear sequence of panels, easily assembled using the {@link gov.takx.commons.wizard.WizardBuilder}.
 */
package gov.takx.commons.wizard;
