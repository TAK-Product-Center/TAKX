package gov.takx.core.view.gui.milsym;

import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ShapeType;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilSymIconBuilder;
import gov.takx.api.milsym.SymbolWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Wrapper for {@link MilSymTreePanel} that swaps out an instance of each based on
 * a new selection of a {@link ShapeType}. It's way more optimal to have 7 trees
 * and swap between them than attempt to get the swing model to update a single
 * tree and correctly remember which paths are expanded and what scroll position was.
 *
 * @since 5.1
 */
public class MilSymTreeDelegatePanel extends AMilSymFilterablePanel
{
    private final Map<ShapeType, MilSymTreePanel> shapeTrees = new HashMap<>();
    private String initialSymbol;

    /**
     * @param milStd2525Service MIL-STD-2525 service to query for symbols.
     * @param onSelectEntry Action to run when clicked on.
     */
    protected MilSymTreeDelegatePanel(@Nonnull IMilStd2525Service milStd2525Service,
                                      @Nonnull Consumer<ISymbolTable.Symbol> onSelectEntry)
    {
        super(milStd2525Service, onSelectEntry);

        setLayout(new BorderLayout());
    }

    /**
     * @param shapeType Shape type to display tree for.
     */
    public synchronized void setShape(@Nullable ShapeType shapeType)
    {
        removeAll();

        // Swap out the display.
        MilSymTreePanel display = shapeTrees.computeIfAbsent(shapeType, t -> new MilSymTreePanel(milStd2525Service, initialSymbol, onSelectEntry));
        add(display, BorderLayout.CENTER);
    }

    /**
     * @param initialSymbol The initial symbol to show as being selected. Can be {@code null} to start at the root of the selection tree.
     */
    public void setInitialSymbol(@Nullable String initialSymbol)
    {
        this.initialSymbol = initialSymbol;
        shapeTrees.values().forEach(tree -> tree.setSelectedSymbolPath(initialSymbol));
    }

    @Override
    public void setIconAdapter(@Nonnull Consumer<IMilSymIconBuilder> adapter)
    {
        for (MilSymTreePanel delegate : shapeTrees.values())
        {
            delegate.setIconAdapter(adapter);
        }
    }

    @Override
    protected void updateSearch(@Nonnull List<SymbolWrapper> symbolWrappers)
    {
        for (MilSymTreePanel delegate : shapeTrees.values())
        {
            delegate.updateSearch(symbolWrappers);
        }
    }
}
