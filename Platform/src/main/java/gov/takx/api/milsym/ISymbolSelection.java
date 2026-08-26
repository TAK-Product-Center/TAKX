package gov.takx.api.milsym;

import gov.tak.api.symbology.ISymbolTable;
import jakarta.annotation.Nonnull;

/**
 * Outline of a selection result from a {@link ISymbolChooser}.
 * 
 * @since 5.1
 */
public interface ISymbolSelection
{
    /**
     * @return Selected symbol.
     */
    @Nonnull
    ISymbolTable.Symbol getSymbolEntry();

    /**
     * @return Symbol code. May differ from {@link ISymbolTable.Symbol#getCode()} due to inclusion of modifiers.
     */
    @Nonnull
    String getSymbolCode();
}
