/*
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron,
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
package org.csstudio.utility.ldap;


import java.util.Dictionary;
import java.util.Hashtable;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.csstudio.platform.AbstractCssPlugin;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.utility.ldap.preference.LdapPreference;
import org.csstudio.utility.ldap.service.ILdapService;
import org.csstudio.utility.ldap.service.LdapServiceTracker;
import org.csstudio.utility.ldap.service.impl.LdapServiceImpl;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public final class LdapActivator extends AbstractCssPlugin {
    // The plug-in ID
    public static final String PLUGIN_ID = "org.csstudio.utility.ldap.service.impl";

    private static final Logger LOG = CentralLogger.getInstance().getLogger(LdapActivator.class);

    private static LdapActivator INSTANCE;

    private LdapServiceTracker _ldapServiceTracker;

    /**
     * Don't instantiate.
     * Called by framework.
     */
    public LdapActivator() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Activator " + PLUGIN_ID + " does already exist.");
        }
        INSTANCE = this; // Antipattern is required by the framework!
    }

    /* (non-Javadoc)
     * @see org.csstudio.platform.AbstractCssPlugin#doStart(org.osgi.framework.BundleContext)
     */
    @Override
    protected void doStart(@Nullable final BundleContext context) throws Exception {

        // TODO (jpenning) Hack: Find a better way to find out whether to use ldap
        final String ldapURL = getCssPluginPreferences().getString(LdapPreference.URL.getKeyAsString());
        final boolean useLDAP = ldapURL != null && ldapURL.length() > 5;

        if (useLDAP) {
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("service.vendor", "DESY");
            props.put("service.description", "LDAP service implementation");
            LOG.info("Register LDAP-Service");
            context.registerService(ILdapService.class.getName(), new LdapServiceImpl(), props);

            _ldapServiceTracker = new LdapServiceTracker(context);
            _ldapServiceTracker.open();
        } else {
            LOG.info("Do not register LDAP-Service");
        }


    }

    /* (non-Javadoc)
     * @see org.csstudio.platform.AbstractCssPlugin#doStop(org.osgi.framework.BundleContext)
     */
    @Override
    protected void doStop(@Nullable final BundleContext context) throws Exception {
        if (_ldapServiceTracker != null) {
            _ldapServiceTracker.close();
        }
    }

    /* (non-Javadoc)
     * @see org.csstudio.platform.AbstractCssPlugin#getPluginId()
     */
    @Override
    @Nonnull
    public String getPluginId() {
        return PLUGIN_ID;
    }

    /**
     * Returns the singleton instance.
     *
     * @return the instance
     */
    @Nonnull
    public static LdapActivator getDefault() {
        return INSTANCE;
    }

    /**
     * @return the LDAP service or null
     */
    @CheckForNull
    public ILdapService getLdapService() {
        return (ILdapService) _ldapServiceTracker.getService();
    }
}
