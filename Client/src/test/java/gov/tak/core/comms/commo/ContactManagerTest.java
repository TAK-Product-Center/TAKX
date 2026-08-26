package gov.tak.core.comms.commo;

import com.atakmap.commoncommo.Contact;
import gov.takx.test.AContainerTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Validate {@link ContactManager} functionality.
 *
 * @since 3.0
 */
public class ContactManagerTest extends AContainerTest
{
    private final ContactManager contactManager;

    private final Contact contactA;
    private final Contact contactB;
    private final Contact contactC;

    public ContactManagerTest() throws Exception
    {
        contactManager = getBean(ContactManager.class);

        contactA = makeContact("A");
        contactB = makeContact("B");
        contactC = makeContact("C");
    }

    @Test
    public void test_contactAdded_on_null_throws()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> contactManager.contactAdded(null));
    }

    @Test
    public void test_contactRemoved_on_null_throws()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> contactManager.contactRemoved(null));
    }

    @Test
    public void test_findContact_on_null_uid_throws()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> contactManager.findContactByUid(null));
    }

    @Test
    public void test_findContact_on_missing_contact_returns_null()
    {
        assertThat(contactManager.findContactByUid("A")).isNull();
    }

    @Test
    public void test_findContact_returns_correct_contact()
    {
        contactManager.contactAdded(contactA);
        contactManager.contactAdded(contactB);
        contactManager.contactAdded(contactC);

        assertThat(contactManager.findContactByUid("A")).isSameAs(contactA);
        assertThat(contactManager.findContactByUid("B")).isSameAs(contactB);
        assertThat(contactManager.findContactByUid("C")).isSameAs(contactC);
    }

    @Test
    public void test_findContact_after_removeContact_returns_null()
    {
        contactManager.contactAdded(contactA);
        contactManager.contactAdded(contactB);
        contactManager.contactAdded(contactC);

        contactManager.contactRemoved(contactA);
        contactManager.contactRemoved(contactB);
        contactManager.contactRemoved(contactC);

        assertThat(contactManager.findContactByUid("A")).isNull();
        assertThat(contactManager.findContactByUid("B")).isNull();
        assertThat(contactManager.findContactByUid("C")).isNull();
    }

    /**
     * Construct a Contact with the given UID.  Have to use reflection since the constructor is package private.
     *
     * @param uid UID of contact
     * @return Contact
     */
    private Contact makeContact(String uid) throws Exception
    {
        final Constructor<Contact> constructor = Contact.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(uid);
    }
}
