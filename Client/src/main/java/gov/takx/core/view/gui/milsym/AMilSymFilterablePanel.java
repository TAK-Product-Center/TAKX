package gov.takx.core.view.gui.milsym;

import gov.tak.api.symbology.ISymbolTable;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilSymIconBuilder;
import gov.takx.api.milsym.SymbolWrapper;
import jakarta.annotation.Nonnull;

import javax.swing.JPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Common base for 2525 panels that support filtering which symbols should be displayed and how to display them.
 *
 * @since 5.1
 */
public abstract class AMilSymFilterablePanel extends JPanel
{
    protected final IMilStd2525Service milStd2525Service;
    protected final Consumer<ISymbolTable.Symbol> onSelectEntry;
    protected Consumer<IMilSymIconBuilder> adapter;

    /**
     * @param milStd2525Service MIL-STD-2525 service to query for symbols.
     * @param onSelectEntry Action to run when clicked on.
     */
    protected AMilSymFilterablePanel(@Nonnull IMilStd2525Service milStd2525Service,
                                     @Nonnull Consumer<ISymbolTable.Symbol> onSelectEntry)
    {
        this.milStd2525Service = milStd2525Service;
        this.onSelectEntry = onSelectEntry;
    }

    /**
     * @param adapter Icon adapter to use for this panel's displayed symbols.
     */
    public void setIconAdapter(@Nonnull Consumer<IMilSymIconBuilder> adapter)
    {
        this.adapter = adapter;
    }

    /**
     * Apply a given filter for symbols.
     * @param filter Filter to apply.
     */
    public void applyNewFilter(@Nonnull Predicate<SymbolWrapper> filter)
    {
        List<SymbolWrapper> symbols = new ArrayList<>();
        milStd2525Service.traverse(wrapper -> {
            // Skip symbols with no supported shape types. They're unusable.
            if (wrapper.getSymbol().getContentMask().isEmpty()) return;

            // Apply user-provided filter.
            if (filter.test(wrapper)) symbols.add(wrapper);
        });
        updateSearch(symbols);
    }

    /**
     * Called by {@link #applyNewFilter(Predicate)} upon completion, with the symbols matching the filter.
     * 
     * @param symbolWrappers List of symbols to display after the filter has been applied.
     */
    protected abstract void updateSearch(@Nonnull List<SymbolWrapper> symbolWrappers);
}
