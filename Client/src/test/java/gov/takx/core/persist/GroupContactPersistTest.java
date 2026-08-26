package gov.takx.core.persist;

import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GroupContactPersist}. These tests <em>do not</em> utilize a real DB, instead relying on mocks
 * and interaction verifications.
 *
 * @since 3.2
 */
@ExtendWith(BlacksmithExtension.class)
public class GroupContactPersistTest
{
    @Mocked
    private IPersistServiceProvider persistServiceProvider;

    @Mocked
    private GroupContactManager groupContactManager;

    private GroupContactPersist groupContactPersist;

    static
    {
        MockStrategy strategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes)
                .withMockStrategy(strategy));
    }

    @BeforeEach
    public void setup()
    {
        IPersistService persistService = mock(IPersistService.class);
        when(persistServiceProvider.getPersistService()).thenReturn(persistService);
        when(persistService.query(anyString(), any())).thenReturn(Collections.emptyList());

        groupContactPersist = new GroupContactPersist(persistServiceProvider, groupContactManager);
    }

    @Test
    public void persistGroupContact_ThrowsNullPointerException_WhenPassedNullContact()
    {
        //noinspection ConstantConditions
        assertThatNullPointerException()
                .isThrownBy(() -> groupContactPersist.persistGroupContact(null));
    }

    @Test
    public void queryAllGroupContacts_ReturnsNonNullSet()
    {
        assertThat(groupContactPersist.findAllGroupContacts()).isNotNull();
    }

    @Test
    public void deleteGroupContact_ThrowsNullPointerException_WhenPassedNullContact()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> groupContactPersist.deleteGroupContact(null));
    }
}
