package gov.takx.module;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.FilterVirtualFileVisitor;
import org.jboss.vfs.util.SuffixMatchFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * File finder to locate deployments (jar files) below a given directory in the VFS.
 *
 * @since 2.36
 */
public class VfsFileFinder
{
    /**
     * A finder that locates just jar files in the scanned directory.
     */
    public static final VfsFileFinder JAR_FINDER = new VfsFileFinder(new SuffixMatchFilter(".jar"));

    /**
     * A plugin finder used during testing.  Currently, matches NO files.  This might need to change if we ever
     * resurrect test support for running on Postgres.
     */
    public static final VfsFileFinder TEST_PLUGIN_FINDER = new VfsFileFinder(file -> false);

    // For an historical note, the previous test filter loaded the known DB plugins, and looked like this:
    //
    //     DB_PLUGIN_FILTER = new IncludeFileNameVirtualFileFilter("plugin.(hypersonic|postgres).*.jar");

    private final VirtualFileFilter filter;

    public VfsFileFinder(VirtualFileFilter filter)
    {
        this.filter = filter;
    }

    /**
     * Find all files directly in the given directory that pass the configured filter.
     *
     * @param dir VFS directory to search, no subdirectories are processed, may be {@code null}
     * @return List of located files, empty list if {@code null} directory was passed in
     */
    @Nonnull
    public List<VirtualFile> find(@Nullable VirtualFile dir) throws IOException
    {
        if (dir == null) return Collections.emptyList();

        final var visitor = new FilterVirtualFileVisitor(filter, VisitorAttributes.LEAVES_ONLY);
        dir.visit(visitor);

        return visitor.getMatched();
    }
}
