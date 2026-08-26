package gov.takx.dataservices.persist.upgraders;

import gov.takx.api.filesystem.VfsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Upgrade from DB version 24 to 25.  Changes include:
 * <ul>
 *     <li>Fix glyph resource paths if they are in VFS</li>
 * </ul>
 * @since 5.2
 */
public class UpgradeToV25 extends AUpgrader
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String VFS = "vfs:";
    private static final String VFS_GLYPHS = VFS + VfsConstants.GLYPHS_NAMESPACE + "/";

    @Override
    public void doUpgrade() throws UpgraderException
    {
        rewriteGlyphUrls();
    }

    /**
     * The way that glyph resources in VFS is specified has changed.  Previously, the {@code Glyphs} directory was
     * assumed, and thus not specified in the URL.  With 5.2, we have proper URL handling for the vfs protocol,
     * but it assumes that paths are relative to the VFS root, so we need to update any glyph resources that
     * use a vfs URL to add in the {@code Glyphs} parent directory.
     */
    @SuppressWarnings("SqlResolve")
    private void rewriteGlyphUrls()
    {
        final Map<Integer, String> rewrites = new HashMap<>();

        try
        {
            // Collect rows that need to be updated
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("select id, glyphResource from trackstyledefinition"))
            {
                while (rs.next())
                {
                    final int id = rs.getInt("id");
                    final String resource = rs.getString("glyphResource");

                    // If this resource refers to VFS, we need to add the glyphs namespace to the path if it is missing
                    if (resource != null && resource.startsWith(VFS) && !resource.startsWith(VFS_GLYPHS))
                    {
                        final String newResource = resource.replace(VFS, VFS_GLYPHS);
                        logger.debug("rewrite glyph: id {} glyph {} ==> {}", id, resource, newResource);
                        rewrites.put(id, newResource);
                    }
                }
            }

            // Perform the updates
            try (var statement = connection.prepareStatement("update trackstyledefinition set glyphresource=? where id=?"))
            {
                for (var rewrite : rewrites.entrySet())
                {
                    statement.setString(1, rewrite.getValue());
                    statement.setInt(2, rewrite.getKey());
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e)
        {
            throw new UpgraderException(e);
        }
    }
}
