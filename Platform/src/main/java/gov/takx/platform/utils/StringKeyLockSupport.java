package gov.takx.platform.utils;

import com.google.common.util.concurrent.Striped;

import java.util.concurrent.locks.Lock;

/**
 * Support methods for locking operations pertaining to a given String (that operate as a selector for the lock).
 * This allows a section of code to lock on an arbitrary string value, such as a Structure hash or a Map Entity key.
 * <p>
 * We use a {@code Striped<Lock>} from Guava to manage access to a pool of locks selected using the string key value.
 * <p>
 * Note: I originally just used String.intern() method, but I saw spurious failures and my reading doesn't
 * guarantee that String.intern is itself thread safe.
 *
 * @since 2.9
 */
public class StringKeyLockSupport
{
    // High concurrency locks using Strings as the key
    private final Striped<Lock> hashLocks = Striped.lock(128);

    /**
     * Get the lock object associated with the given key.
     *
     * @param key The key for which to obtain the lock object
     * @return The lock object, the lock has not been acquired
     */
    public Lock getLockForKey(String key)
    {
        return hashLocks.get(key);
    }

    /**
     * Acquire a lock according to the given key.
     * <p>
     * It is the responsibility of the caller to call {@link Lock#unlock()} on the returned lock at an
     * appropriate future time.
     *
     * @param key The key for which to acquire the lock
     * @return The acquired lock ({@link Lock#lock()} will have already been called (i.e., the lock is already acquired)
     */
    public Lock lock(String key)
    {
        final Lock lock = getLockForKey(key);
        lock.lock();

        return lock;
    }
}
