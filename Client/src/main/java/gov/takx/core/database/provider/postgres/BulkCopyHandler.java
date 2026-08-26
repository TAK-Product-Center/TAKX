package gov.takx.core.database.provider.postgres;

import gov.takx.api.messages.IExtendedDataStructure;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.core.persist.PostgresCopyUtils;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.Encoding;
import org.postgresql.jdbc.PgConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

/**
 * Handler to process a bulk insert of RDMs into the DB.  This uses the PG-specific "COPY FROM" command
 * to rapidly insert data into the appropriate tables.
 *
 * @since 2.14
 */
public class BulkCopyHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ThreadLocal<StringBuilder> BUILDERS = ThreadLocal.withInitial(StringBuilder::new);

    private static final int BUFFER_SIZE = 128 * 1024;
    private static final ThreadLocal<char[]> BUFFERS = ThreadLocal.withInitial(() -> new char[BUFFER_SIZE]);

    /**
     * Perform a COPY of a collection of RDMs core data into the DB.
     *
     * @param connection Connection to use
     * @param rdms       RDMs to process
     * @return count of the number of RDMs inserted, -1 on error
     */
    public long doBulkInsertCoreData(PgConnection connection, Iterable<IPrePersistRaptorDataMessage> rdms)
    {
        final StringBuilder data = BUILDERS.get();
        data.setLength(0);

        try
        {
            PostgresCopyUtils.buildCopyData(data, rdms);

            final CopyManager copyManager = connection.getCopyAPI();

            final CopyIn copyIn = copyManager.copyIn(PostgresCopyUtils.RDM_COPY_SQL);
            final long count = doCopy(copyIn, data, connection.getEncoding());

            connection.commit();

            return count;
        } catch (Exception e)
        {
            String moKey = "";
            if (rdms.iterator().hasNext())
            {
                IPrePersistRaptorDataMessage rdm = rdms.iterator().next();
                moKey = " for MapObject \"" + rdm.getUniqueKey() + "\"";
            }
            logger.error("Failed to handle statement{}: " + PostgresCopyUtils.RDM_COPY_SQL, moKey, e);
            return -1;
        }
    }

    /**
     * Perform a COPY of a collection of RDMs structure data into the DB.
     *
     * @param connection Connection to use
     * @param structure  Structure being processed
     * @param rdms       RDMs to process
     * @return count of the number of RDMs inserted, -1 on error
     */
    public long doBulkInsertExtendedData(PgConnection connection, IStructure structure, Iterable<IPrePersistRaptorDataMessage> rdms)
    {
        final String copySql = structure.getBulkCopySql();
        final StringBuilder data = BUILDERS.get();
        data.setLength(0);

        try
        {
            final CopyManager copyManager = connection.getCopyAPI();

            for (IRaptorMessage message : rdms)
            {
                final IExtendedDataStructure dataObject = message.getDataObject();
                dataObject.buildCopyString(data, message.getIdAsInt());
            }

            final CopyIn copyIn = copyManager.copyIn(copySql);
            final long count = doCopy(copyIn, data, connection.getEncoding());

            connection.commit();

            return count;
        } catch (Exception e)
        {
            logger.error("Failed to handle statement: {}", copySql, e);
        }
        return -1;
    }

    /**
     * Perform a COPY operation.
     *
     * @param cp       The CopyIn operation
     * @param data     The data to send as input to the copy
     * @param encoding The string encoding for this connection
     * @return count of rows inserted
     */
    private long doCopy(CopyIn cp, StringBuilder data, Encoding encoding) throws SQLException, IOException
    {
        final char[] charBuf = BUFFERS.get();

        int len;
        try (StringBuilderReader reader = new StringBuilderReader(data))
        {
            while ((len = reader.read(charBuf)) >= 0)
            {
                if (len > 0)
                {
                    byte[] bytes = encoding.encode(new String(charBuf, 0, len));
                    cp.writeToCopy(bytes, 0, bytes.length);
                }
            }
            return cp.endCopy();
        } finally
        { // see to it that we do not leave the connection locked
            if (cp.isActive())
            {
                cp.cancelCopy();
            }
        }
    }

    /**
     * Specialized Reader to read from a StringBuilder without manufacturing an intermediate String object.
     */
    @SuppressWarnings({"SynchronizeOnNonFinalField", "NonPrivateFieldAccessedInSynchronizedContext", "FieldAccessedSynchronizedAndUnsynchronized"})
    private static final class StringBuilderReader extends Reader
    {
        private StringBuilder str;
        private final int length;
        private int next = 0;
        private int mark = 0;

        /**
         * Creates a new string reader.
         *
         * @param s String providing the character stream.
         */
        public StringBuilderReader(StringBuilder s)
        {
            str = s;
            length = s.length();
        }

        /**
         * Check to make sure that the stream has not been closed
         */
        private void ensureOpen() throws IOException
        {
            if (str == null)
            {
                throw new IOException("Stream closed");
            }
        }

        /**
         * Reads a single character.
         *
         * @return The character read, or -1 if the end of the stream has been
         * reached
         * @throws IOException If an I/O error occurs
         */
        @Override
        public int read() throws IOException
        {
            synchronized (lock)
            {
                ensureOpen();
                if (next >= length)
                {
                    return -1;
                }
                return str.charAt(next++);
            }
        }

        /**
         * Reads characters into a portion of an array.
         *
         * @param cbuf Destination buffer
         * @param off  Offset at which to start writing characters
         * @param len  Maximum number of characters to read
         * @return The number of characters read, or -1 if the end of the
         * stream has been reached
         * @throws IOException If an I/O error occurs
         */
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException
        {
            synchronized (lock)
            {
                ensureOpen();
                if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                        ((off + len) > cbuf.length) || ((off + len) < 0))
                {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0)
                {
                    return 0;
                }
                if (next >= length)
                {
                    return -1;
                }
                int n = Math.min(length - next, len);
                str.getChars(next, next + n, cbuf, off);
                next += n;
                return n;
            }
        }

        /**
         * Skips the specified number of characters in the stream. Returns
         * the number of characters that were skipped.
         * <p>
         * <p>The {@code ns} parameter may be negative, even though the
         * {@code skip} method of the {@link Reader} superclass throws
         * an exception in this case. Negative values of {@code ns} cause the
         * stream to skip backwards. Negative return values indicate a skip
         * backwards. It is not possible to skip backwards past the beginning of
         * the string.
         * <p>
         * <p>If the entire string has been read or skipped, then this method has
         * no effect and always returns 0.
         *
         * @throws IOException If an I/O error occurs
         */
        @Override
        public long skip(long ns) throws IOException
        {
            synchronized (lock)
            {
                ensureOpen();
                if (next >= length)
                {
                    return 0;
                }
                // Bound skip by beginning and end of the source
                long n = Math.min(length - next, ns);
                n = Math.max(-next, n);
                next += n;
                return n;
            }
        }

        /**
         * Tells whether this stream is ready to be read.
         *
         * @return True if the next read() is guaranteed not to block for input
         * @throws IOException If the stream is closed
         */
        @Override
        public boolean ready() throws IOException
        {
            synchronized (lock)
            {
                ensureOpen();
                return true;
            }
        }

        /**
         * Tells whether this stream supports the mark() operation, which it does.
         */
        @Override
        public boolean markSupported()
        {
            return true;
        }

        /**
         * Marks the present position in the stream.  Subsequent calls to reset()
         * will reposition the stream to this point.
         *
         * @param readAheadLimit Limit on the number of characters that may be
         *                       read while still preserving the mark.  Because
         *                       the stream's input comes from a string, there
         *                       is no actual limit, so this argument must not
         *                       be negative, but is otherwise ignored.
         * @throws IllegalArgumentException If {@code readAheadLimit < 0}
         * @throws IOException              If an I/O error occurs
         */
        @Override
        public void mark(int readAheadLimit) throws IOException
        {
            if (readAheadLimit < 0)
            {
                throw new IllegalArgumentException("Read-ahead limit < 0");
            }
            synchronized (lock)
            {
                ensureOpen();
                mark = next;
            }
        }

        /**
         * Resets the stream to the most recent mark, or to the beginning of the
         * string if it has never been marked.
         *
         * @throws IOException If an I/O error occurs
         */
        @Override
        public void reset() throws IOException
        {
            synchronized (lock)
            {
                ensureOpen();
                next = mark;
            }
        }

        /**
         * Closes the stream and releases any system resources associated with
         * it. Once the stream has been closed, further read(),
         * ready(), mark(), or reset() invocations will throw an IOException.
         * Closing a previously closed stream has no effect.
         */
        @Override
        public void close()
        {
            str = null;
        }
    }
}
