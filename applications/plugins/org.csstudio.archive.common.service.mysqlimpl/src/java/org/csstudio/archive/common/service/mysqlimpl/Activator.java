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
package org.csstudio.archive.common.service.mysqlimpl;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.csstudio.archive.common.service.IArchiveEngineFacade;
import org.csstudio.archive.common.service.IArchiveReaderFacade;
import org.csstudio.archive.common.service.mysqlimpl.channel.ArchiveChannelDaoImpl;
import org.csstudio.archive.common.service.mysqlimpl.channel.IArchiveChannelDao;
import org.csstudio.archive.common.service.mysqlimpl.dao.ArchiveDaoManager;
import org.csstudio.archive.common.service.mysqlimpl.engine.ArchiveEngineDaoImpl;
import org.csstudio.archive.common.service.mysqlimpl.engine.IArchiveEngineDao;
import org.csstudio.archive.common.service.mysqlimpl.sample.ArchiveSampleDaoImpl;
import org.csstudio.archive.common.service.mysqlimpl.sample.IArchiveSampleDao;
import org.csstudio.platform.logging.CentralLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;

/**
 * Activator.
 *
 * @author bknerr
 * @since 22.11.2010
 */
public class Activator implements BundleActivator {
    /**
     * The id of this Java plug-in (value <code>{@value}</code> as defined in MANIFEST.MF.
     */
    public static final String PLUGIN_ID = "org.csstudio.archive.common.service.mysqlimpl";

    private static final Logger LOG = CentralLogger.getInstance().getLogger(Activator.class);

    private static Activator INSTANCE;

    /**
     * Don't instantiate.
     * Called by framework.
     */
    public Activator() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Activator " + PLUGIN_ID + " does already exist.");
        }
        INSTANCE = this; // Antipattern is required by the framework!
    }

    /**
     * Returns the singleton instance.
     *
     * @return the instance
     */
    @Nonnull
    public static Activator getDefault() {
        return INSTANCE;
    }

    private static class MySQLArchiveServiceImplModule extends AbstractModule {
        /**
         * Constructor.
         */
        public MySQLArchiveServiceImplModule() {
            // EMPTY
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void configure() {
            bind(IArchiveEngineDao.class).to(ArchiveEngineDaoImpl.class).in(Scopes.SINGLETON);
            bind(IArchiveSampleDao.class).to(ArchiveSampleDaoImpl.class).in(Scopes.SINGLETON);
            bind(IArchiveChannelDao.class).to(ArchiveChannelDaoImpl.class).in(Scopes.SINGLETON);
        }
    }

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
    public void start(@Nonnull final BundleContext context) throws Exception {

	    final Injector injector = Guice.createInjector(new MySQLArchiveServiceImplModule());
	    final MySQLArchiveEngineServiceImpl engineServiceImpl =
	        injector.getInstance(MySQLArchiveEngineServiceImpl.class);

        final Dictionary<String, Object> propsCfg = new Hashtable<String, Object>();
        propsCfg.put("service.vendor", "DESY");
        propsCfg.put("service.description", "MySQL archive engine service implementation");
        LOG.info("Register MySQL archive engine service");

        context.registerService(IArchiveEngineFacade.class.getName(),
                                engineServiceImpl,
                                propsCfg);


        final Dictionary<String, Object> propsRd = new Hashtable<String, Object>();
        propsRd.put("service.vendor", "DESY");
        propsRd.put("service.description", "MySQL archive reader service implementation");
        LOG.info("Register MySQL archive reader service");

        context.registerService(IArchiveReaderFacade.class.getName(),
                                MySQLArchiveReaderServiceImpl.INSTANCE,
                                propsRd);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
    public void stop(@Nonnull final BundleContext bundleContext) throws Exception {

	    // Services are automatically unregistered

	    ArchiveDaoManager.INSTANCE.disconnect();
	}
}
