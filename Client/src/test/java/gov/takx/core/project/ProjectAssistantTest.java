package gov.takx.core.project;

import gov.takx.api.persist.IDatabaseProvider;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.project.IProjectMetadata;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ProjectAssistantTest extends AIntegrationTest
{
    private static final String PROJECT_NAME = ProjectAssistantTest.class.getSimpleName();

    @Test
    public void parseProjectFileTest() throws IOException
    {
        // Install some values into the project preferences
        final long now = System.currentTimeMillis();
        ProjectPreferenceEntry.CREATION_TIME.put(now);
        ProjectPreferenceEntry.START_DATE.put(now + 10);
        ProjectPreferenceEntry.END_DATE.put(now + 20);
        ProjectPreferenceEntry.NOTES.put("some notes");
        ProjectPreferenceEntry.DESCRIPTION.put("some description");
        ProjectPreferenceEntry.LOCATION.put("very special location");
        ProjectPreferenceEntry.UUID.put(UUID.randomUUID().toString());

        projectManager.getCurrentOpenProject().saveProjectPreferences();

        // Pull out values that we don't control
        String size = ProjectPreferenceEntry.SIZE.getStringValue();
        String vfs = ProjectPreferenceEntry.VFS.getStringValue();

        // And parse the projx file and verify that all the values match
        final String projectFilePath = projectManager.getProjectFilePath(PROJECT_NAME);
        IProjectMetadata pmd = new ProjectAssistant().parseProjectFile(new File(projectFilePath));
        IDatabaseProvider dbProvider = getBean(IPersistServiceProvider.class).getPersistService().getDatabaseProvider();

        assertThat(pmd).isNotNull();
        assertThat(pmd.getProjectName()).isEqualTo(PROJECT_NAME);
        assertThat(pmd.isPackage()).isFalse();
        assertThat(pmd.getPlatformVersion().toString()).isEqualTo(programEnvironment.getPlatformVersionString());
        assertThat(pmd.getMapObjects()).isEmpty();
        assertThat(pmd.getCreationDate()).isEqualTo(now);
        assertThat(pmd.getStartDate()).isEqualTo(now + 10);
        assertThat(pmd.getEndDate()).isEqualTo(now + 20);
        assertThat(pmd.getSize()).isEqualTo(size);
        assertThat(pmd.getVfsSize()).isEqualTo(vfs);
        assertThat(pmd.getDescription()).isEqualTo("some description");
        assertThat(pmd.getLocation()).isEqualTo("very special location");
        assertThat(pmd.getNotes()).isEqualTo("some notes");
        assertThat(pmd.getWmsOfflineMode()).isTrue();
        assertThat(pmd.getDBType()).isEqualTo(dbProvider.getType());
    }
}
