/*
 * Copyright (c) 2010 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.archive.common.service.mysqlimpl.dao;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.csstudio.archive.common.service.ArchiveConnectionException;
import org.csstudio.platform.logging.CentralLogger;

import com.google.common.collect.Lists;

/**
 * Persistence layer worker for batched statements.
 * Intended to be scheduled periodically and if necessary on demand
 * (when the queue is getting big or the contained statements reach the max allowed packet size).
 *
 * @author bknerr
 * @since 08.02.2011
 */
public class PersistDataWorker implements Runnable {

    private static final Logger LOG =
            CentralLogger.getInstance().getLogger(PersistDataWorker.class);

    private final String _name;
    private final long _period;

    private final List<String> _batchedStatements;
    private final BlockingQueue<String> _queuedStatements;
    /**
     * Constructor.
     * @param sqlStatements
     */
    public PersistDataWorker(@Nonnull final String name,
                             @Nonnull final BlockingQueue<String> sqlStatements,
                             @Nonnull final long period) {
        _name = name;
        _period = period;

        _queuedStatements = sqlStatements;
        _batchedStatements = Lists.newLinkedList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOG.info("RUN: " + _name);

        Statement sqlStmt = null;
        Connection connection = null;
        try {
            connection = ArchiveDaoManager.INSTANCE.getConnection();
            sqlStmt = connection.createStatement();

            while (_queuedStatements.peek() != null) {
                final String queuedStmt = _queuedStatements.poll();
                _batchedStatements.add(queuedStmt);
                LOG.info(queuedStmt.length() + "\t\t" + queuedStmt.getBytes().length);
                sqlStmt.addBatch(queuedStmt);
            }
            sqlStmt.executeBatch();

        } catch (final ArchiveConnectionException se) {
            ArchiveDaoManager.WORKER_LOG.error("Batched update failed. Drain unpersisted statements to file system.");
            ArchiveDaoManager.INSTANCE.rescueData(_batchedStatements);
        } catch (final BatchUpdateException be) {
            ArchiveDaoManager.WORKER_LOG.error("Batched update failed. Drain unpersisted statements to file system.");
            processFailedBatch(_batchedStatements, be);
        } catch (final SQLException se) {
            ArchiveDaoManager.WORKER_LOG.error("Batched update failed. Statement was already closed or driver does not support batched statements.");
            ArchiveDaoManager.INSTANCE.rescueData(_batchedStatements);
        } finally {
            _batchedStatements.clear();
            closeStatement(sqlStmt);
            try {
                finalizeWorker();
            } catch (final ArchiveConnectionException e) {
                LOG.warn("Connection retrieval for close failed on termination of worker");
            } catch (final SQLException e) {
                LOG.warn("Closing of connection failed on termination of worker");
            }
        }
    }

    /**
     * Called in finally block at the end of the {@link PersistDataWorker#run()} method.
     * Can be overridden for instance to close the thread's own connection.
     *
     * @throws SQLException
     * @throws ArchiveConnectionException
     */
    protected void finalizeWorker() throws SQLException, ArchiveConnectionException {
        // Empty
    }

    private static void processFailedBatch(@Nonnull final List<String> batchedStatements,
                                           @Nonnull final BatchUpdateException be) {
        // NOT all statements have been successfully executed! (Depends on RDBM)
        final int[] updateCounts = be.getUpdateCounts();
        if (updateCounts.length == batchedStatements.size()) {
            // All statements have been tried executed, look for the failed ones
            final List<String> failedStmts = findFailedStatements(updateCounts, batchedStatements);
            ArchiveDaoManager.INSTANCE.rescueData(failedStmts);
        } else {
            // Not all statements have been tried to be executed - safe only the failed ones
            ArchiveDaoManager.INSTANCE.rescueData(batchedStatements.subList(updateCounts.length, batchedStatements.size()));
        }
    }

    private static void closeStatement(@CheckForNull final Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (final SQLException e) {
                ArchiveDaoManager.WORKER_LOG.warn("Closing of statemend failed.");
            }
        }
    }

    @Nonnull
    private static List<String> findFailedStatements(@Nonnull final int[] updateCounts,
                                                     @Nonnull final List<String> allStmts) {
        final List<String> failedStmts = Lists.newLinkedList();
        for (int j = 0; j < updateCounts.length; j++) {
            if (updateCounts[j] == Statement.EXECUTE_FAILED) {
                failedStmts.add(allStmts.get(j));
            }
        }
        return failedStmts;
    }

    @Nonnull
    public String getName() {
        return _name;
    }

    public long getPeriod() {
        return _period;
    }
}
