package gov.tak.core.comms.roles;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.resources.IAssetManager;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.detail.Role;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.filesystem.VfsConstants;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * A service to manage handling and loading of {@link Role Roles}.
 * @since 5.6
 */
@ApplicationScoped
public class RolesManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String ROLES_DIR = "Roles";

    private final Map<String, String> rolePathNames = Map.of(
            "hq", "hq", "team lead", "teamlead", "sniper", "sniper", "k9", "k9",
            "rto", "rto", "medic", "medic", "forward observer", "forwardobserver");

    private final IVirtualFileSystemManager vfsManager;
    private final IAssetManager assetManager = new RolesAssetManager();
    private final List<Role> defaultRoles = new ArrayList<>();

    private gov.tak.api.cot.detail.RolesManager rolesManager;
    private File rolesDir;

    @Inject
    private RolesManager(@VFSManagerScope(VFSScope.Global) IVirtualFileSystemManager vfsManager)
    {
        this.vfsManager = vfsManager;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices event)
    {
        rolesDir = vfsManager.getRoot(new FilePath(VfsConstants.FILES_DIR, ROLES_DIR).toString());
        rolesManager = new gov.tak.api.cot.detail.RolesManager();
        loadDatabaseRoles();
        defaultRoles.addAll(getRoles());
        loadCustomRoles();
    }

    /**
     * Load the roles in from the Kernel SQLite db. This db may change, so load it every startup.
     * <p/>
     * Roles are global, so read only on startup.
     */
    private void loadDatabaseRoles()
    {
        DatabaseIface db = null;
        try (InputStream resourceAsStream = Role.class.getResourceAsStream("/assets/roles/roles.sqlite"))
        {
            if (resourceAsStream == null) throw new IOException("Exception opening the 'roles.sqlite' database");
            var rolesdb = File.createTempFile("tempRolesDB", ".sqlite"); // create a copy outside the JAR to use
            var rolesdbPath = rolesdb.toPath();
            rolesdb.deleteOnExit();
            Files.copy(resourceAsStream, rolesdbPath, StandardCopyOption.REPLACE_EXISTING);
            db = Databases.openDatabase(rolesdbPath.toAbsolutePath().toString(), true);
            try (CursorIface result = db.query("SELECT name, value FROM roles", null))
            {
                while (result.moveToNext())
                {
                    try
                    {
                        JSONObject rolesJson = new JSONObject(result.getString(1));
                        rolesManager.addRoles(Role.fromJSON(rolesJson));
                    } catch (Throwable t)
                    {
                        logger.error("Failed to parse [{}] roles JSON: {}", result.getString(0), result.getString(1), t);
                    }
                }
            }
        } catch (Throwable t)
        {
            logger.error("Failed to load roles database", t);
        } finally
        {
            if (db != null) db.close();
        }
    }

    /**
     * Load in roles from the JSON stored in VFS "Files/Roles" folder.
     * @see <a href="User+Roles">User Roles</a> for formatting.
     */
    private void loadCustomRoles()
    {
        if (!rolesDir.exists()) return;
        File[] roleFiles = rolesDir.listFiles();
        if (roleFiles == null) return;
        for (File f : roleFiles)
        {
            try (var strm = IOProviderFactory.getInputStream(f))
            {
                if (!importRoles(strm)) logger.error("Unable to import the User Roles file: {}", f.getName());
            } catch (IOException e)
            {
                logger.error("Unable to import the User Roles file: {}", f.getName(), e);
            }
        }
    }

    /**
     * Check if the provided {@link Role role} is one of the Kernel's "default" roles.
     * @param role The role to check
     * @return {@code true} if the role is a default role
     */
    public boolean isDefault(@Nonnull Role role)
    {
        return rolesManager.isDefault(role);
    }

    /**
     * @return an unmodifiable list of only the top-level {@link Role roles}.
     */
    @Nonnull
    public List<Role> getRoles()
    {
        return rolesManager.getRoles();
    }

    /**
     * Gets the {@link Role} with the provided name and abbreviation. This <em>IS</em> case-sensitive.
     * <p/>
     * @apiNote If the abbreviation is {@code null}, this will always return null.
     * @param name The name to search for
     * @param abbreviation The abbreviation to search for
     * @return The role with the provided name and abbreviation, or {@code null}
     */
    @Nullable
    public Role getRole(@Nonnull String name, @Nullable String abbreviation)
    {
        return rolesManager.getRole(name, abbreviation);
    }

    /**
     * Gets the {@link Role} with the provided marker's name and abbreviation, if possible.
     * <p/>
     * @apiNote If the marker abbreviation is {@code null} or does not have one, this will always return {@code null}.
     * @param marker The marker to get the Role from
     * @return The role of the provided marker, or {@code null}
     */
    @Nullable
    public Role getRole(@Nonnull Marker marker)
    {
        final String exRoleName = marker.getMetaString(StandardAttributeNames.CUSTOM_ROLE, "");
        final String roleName = marker.getMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, "");
        final String abbrev = marker.getMetaString(StandardAttributeNames.CUSTOM_ROLE_ABBREVIATION, "");
        return getRole(exRoleName.isBlank() ? roleName : exRoleName, abbrev);
    }

    /**
     * Remove a {@link Role} from the manager by matching the name. Implicitly removes the role's children. Does NOT
     * remove default roles.
     * @param name the top-level name of the roles
     * @return {@code true} if the remove was successful. Will always return {@code false} if the name is {@code null}.
     */
    public boolean removeRoles(@Nullable String name)
    {
        return rolesManager.removeRoles(name);
    }

    /**
     * Given search criteria, return the list of matching (case-insensitive) top-level {@link Role Roles}.
     * @param search the plaintext string used in a simple string contains search
     * @return the list of matching top-level roles, if any
     */
    @Nonnull
    public List<Role> search(@Nonnull String search)
    {
        return rolesManager.search(search);
    }

    /**
     * Add a {@link Role}.
     * @param r the role to be added. If the role name previously exists it is removed before the addition.
     */
    public void addRoles(@Nonnull Role r)
    {
        rolesManager.addRoles(r);
    }

    /**
     * Import {@link Role Roles} from an InputStream provided it's in the proper JSON format.
     * @param stream the stream containing the JSON
     * @return {@code true} if the import was successful
     * @see <a href="User+Roles">User Roles</a> for formatting.
     */
    public boolean importRoles(@Nonnull InputStream stream)
    {
        return rolesManager.importRoles(stream);
    }

    /**
     * Copy provided files into the VFS under the appropriate folder. These files should be appropriately formatted
     * JSON files. Auto-reloads this manager to incorporate the new roles, and will remove custom roles that do not exist
     * in the folder.
     * @param files The files to copy
     * @see <a href="User+Roles">User Roles</a> for formatting.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void importRoles(@Nonnull File... files)
    {
        if (files.length == 0) return;
        if (!rolesDir.exists()) rolesDir.mkdirs();
        for (var fileToImport : files)
        {
            String originalName = fileToImport.getName();
            final String safeName = FileUtils.sanitizeForFilename(originalName);
            if (!safeName.equals(originalName)) logger.warn("Invalid role file name '{}' changed to '{}'", originalName, safeName);

            try
            {
                Files.copy(fileToImport.toPath(), new File(rolesDir, safeName).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e)
            {
                logger.error("Failed to create filestream of JSON file for roles file: {}", fileToImport, e);
            }
        }
        reloadRoles();
    }

    /**
     * Remove all custom {@link Role Roles} and load the roles from the JSON files.
     * <p/>
     * This does not remove the kernel default roles, nor the database loaded roles. This <b><i>will</i></b> remove any roles that
     * are not in a JSON file in the VFS Roles directory. 
     */
    public void reloadRoles()
    {
        var nonDefaultRoleList = getRoles().stream().filter(r -> !defaultRoles.contains(r)).toList();
        for (Role r : nonDefaultRoleList) removeRoles(r.getName());
        loadCustomRoles();
    }

    /**
     * Get or create an icon for a given {@link Role} based on the image located at the provided URI.
     * <p/>
     * If the role is a "default" role, this will return the image located at the {@code baseIconUri}. If it's a custom
     * role, it will generate an image composed of the role's abbreviation centered on the image located at the {@code baseIconUri}.
     * @param role The role to create an icon for
     * @param baseIconURI The location of the base image to be used
     * @return An image with the abbreviation put on top of the base icon, if the abbreviation returns a valid Role
     */
    @Nonnull
    public BufferedImage createIcon(@Nonnull Role role, @Nonnull String baseIconURI)
    {
        return isDefault(role) ? UIResources.getImage(TAK_ASSETS_ICONS, baseIconURI) : MarshalManager.marshal(rolesManager.createIcon(role.getAbbreviation(), baseIconURI, assetManager), Bitmap.class, BufferedImage.class);
    }

    /**
     * Interrogate the marker to determine exactly what role icon path to use.
     * @param marker The {@link Marker} to query the attributes from
     * @return The path to the icon for the given {@link Role}
     */
    @Nonnull
    public String getIconPathForRole(@Nonnull Marker marker)
    {
        final String roleName = marker.getMetaString("atakRoleType", "Team Member").toLowerCase(Locale.ROOT);
        final Role role = rolesManager.getRole(marker.getMetaString(StandardAttributeNames.CUSTOM_ROLE, ""),
                marker.getMetaString(StandardAttributeNames.CUSTOM_ROLE_ABBREVIATION, ""));
        final String how = marker.getMetaString(StandardAttributeNames.HOW, "");
        String iconPath = role != null ? "roles/team" : "roles/" + rolePathNames.getOrDefault(roleName, "team");

        if (how.startsWith(CotConstants.HOW_HUMAN_PREFIX))
        {
            iconPath += "_human";
        } else if (how.startsWith(CotConstants.HOW_GPS_LOST_PREFIX))
        {
            iconPath += "_nogps";
        }

        iconPath += ".png";

        logger.debug("iconPath: {}, role {}, how {}", iconPath, roleName, how);
        return iconPath;
    }

    private static class RolesAssetManager implements IAssetManager
    {
        @Override
        public InputStream open(String path) throws IOException
        {
            return UIResources.getUrl(TAK_ASSETS_ICONS, path).openStream();
        }
    }
}
