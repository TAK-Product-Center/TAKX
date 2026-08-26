package gov.takx.core.milsym;

import gov.tak.api.symbology.ISymbolTable;
import gov.takx.api.milsym.ISymbolSelection;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Implementation of {@link ISymbolSelection}.
 * 
 * @since 5.1
 */
public class SymbolSelection implements ISymbolSelection
{
    private final ISymbolTable.Symbol symbol;
    private final String symbolCode;

    /**
     * @param symbol Selected symbol.
     * @param symbolCode Symbol code. May differ from {@link ISymbolTable.Symbol#getCode()} due to inclusion of modifiers.
     */
    public SymbolSelection(@Nonnull ISymbolTable.Symbol symbol, @Nonnull String symbolCode)
    {
        this.symbol = symbol;
        this.symbolCode = symbolCode;
    }

    @Nonnull
    @Override
    public ISymbolTable.Symbol getSymbolEntry()
    {
        return symbol;
    }

    @Nonnull
    @Override
    public String getSymbolCode()
    {
        return symbolCode;
    }

    @Override
    public String toString()
    {
        return getSymbolCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolSelection that = (SymbolSelection) o;
        return Objects.equals(symbolCode, that.symbolCode);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(symbolCode);
    }
}
