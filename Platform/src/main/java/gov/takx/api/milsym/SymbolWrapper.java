package gov.takx.api.milsym;

import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ShapeType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper of a symbol with its containing folder.
 *
 * @since 5.1
 */
public class SymbolWrapper implements ISymbolTable.Symbol, Comparable<SymbolWrapper>
{
    private final List<ISymbolTable.Folder> folderPath;
    private final ISymbolTable.Symbol symbol;

    /**
     * @param folderPath The complete path of folders up to this symbol.
     * @param symbol The symbol entry.
     */
    public SymbolWrapper(@Nonnull List<ISymbolTable.Folder> folderPath, @Nonnull ISymbolTable.Symbol symbol)
    {
        this.folderPath = Collections.unmodifiableList(folderPath);
        this.symbol = symbol;
    }

    /**
     * @return String representation of {@link #getFolderPath()} and the {@link ISymbolTable.Symbol#getName()}.
     */
    @Nonnull
    public String getFullPath()
    {
        // Do note that 'ISymbolTable.Symbol#getFullName()' is not actually the full path in the default implementation,
        // but only the parent folder name and the symbol name.
        return folderPath.stream()
                .map(ISymbolTable.Entry::getName)
                .collect(Collectors.joining(" / "))
                + " / " + symbol.getName();
    }

    /**
     * @return The complete path of folders up to this symbol.
     */
    @Nonnull
    public List<ISymbolTable.Folder> getFolderPath()
    {
        return folderPath;
    }

    /**
     * @return The symbol entry.
     */
    @Nonnull
    public ISymbolTable.Symbol getSymbol()
    {
        return symbol;
    }

    /**
     * @param type Shape type to check.
     * @return {@code true} when the wrapped symbol's content mask includes the given shape type.
     * Always {@code true} if the passed in shape type is {@code null}.
     */
    public boolean supportsShape(@Nullable ShapeType type)
    {
        return type == null || symbol.getContentMask().contains(type);
    }

    @Nonnull
    @Override
    public String getFullName()
    {
        // See note in full-path for why we do not delegate to the full name method here.
        return getFullPath();
    }

    @Nonnull
    @Override
    public String getCode()
    {
        return symbol.getCode();
    }

    @Nullable
    @Override
    public String getSummary()
    {
        return symbol.getSummary();
    }

    @Nullable
    @Override
    public Drawable getPreviewDrawable(int color)
    {
        return symbol.getPreviewDrawable(color);
    }

    @Nonnull
    @Override
    public String getName()
    {
        return symbol.getName();
    }

    @Nonnull
    @Override
    public EnumSet<ShapeType> getContentMask()
    {
        return symbol.getContentMask();
    }

    /**
     * @return The folder containing this symbol. The last item in the {@link #getFolderPath()}.
     */
    @Nonnull
    public ISymbolTable.Folder getContainingFolder()
    {
        return folderPath.get(folderPath.size() - 1);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolWrapper wrapper = (SymbolWrapper) o;

        // The kernel implementations of folder/symbol do not implement hashCode or equals
        // so, we'll base equality strictly off the symbol code.
        return symbol.getCode().equals(wrapper.symbol.getCode());
    }

    @Override
    public int hashCode()
    {
        // Same reasoning as 'equals(Object)'
        return symbol.getCode().hashCode();
    }

    @Override
    public String toString()
    {
        return symbol.getFullName();
    }

    @Override
    public int compareTo(@Nonnull SymbolWrapper o)
    {
        return getFullPath().compareTo(o.getFullPath());
    }
}