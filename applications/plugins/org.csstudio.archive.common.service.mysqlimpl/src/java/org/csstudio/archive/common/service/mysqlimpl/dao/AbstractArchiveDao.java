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

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.csstudio.archive.common.service.ArchiveConnectionException;
import org.csstudio.platform.logging.CentralLogger;


/**
 * Abstract implementation of an archive DAO.
 *
 * @author bknerr
 * @since 10.11.2010
 */
public abstract class AbstractArchiveDao {

    private static final Logger LOG =
        CentralLogger.getInstance().getLogger(AbstractArchiveDao.class);


    /**
     * Constructor.
     */
    public AbstractArchiveDao() { }

    /**
     * Returns the current connection for the dao implementation and its subclasses.
     * @return the connection
     * @throws ArchiveConnectionException
     */
    @Nonnull
    protected Connection getConnection() throws ArchiveConnectionException {
        return ArchiveDaoManager.INSTANCE.getConnection();
    }

    @Nonnull
    protected ArchiveDaoManager getDaoMgr() {
        return ArchiveDaoManager.INSTANCE;
    }
    @Nonnull
    protected PersistEngineDataManager getEngineMgr() {
        return PersistEngineDataManager.INSTANCE;
    }

    /**
     * Tries to close the passed statement and logs the given message on closing error.
     * @param stmt
     * @param logMsg
     */
    protected void closeStatement(@CheckForNull final Statement stmt, @Nonnull final String logMsg) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (final SQLException e) {
                LOG.warn(logMsg);
            }
        }
    }

    protected void handleExceptions(@Nonnull final String msg,
                                    @Nonnull final Exception inE) throws ArchiveDaoException {
        try {
            throw inE;
        } catch (final SQLException e) {
            throw new ArchiveDaoException("SQL: " + msg, e);
        } catch (final ArchiveConnectionException e) {
            throw new ArchiveDaoException("Connection: " + msg, e);
        } catch (final ClassNotFoundException e) {
            throw new ArchiveDaoException("Class not found: " + msg, e);
        } catch (final MalformedURLException e) {
            throw new ArchiveDaoException("Malformed URL: " + msg, e);
        } catch (final Exception re) {
            throw new ArchiveDaoException("Unknown: ", re);
        }
    }
}
