/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton,
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
package org.csstudio.platform;

import org.csstudio.platform.internal.logging.CssLogListener;
import org.csstudio.platform.logging.CentralLogger;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Preferences;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Common super class for all CSS plugin classes.
 *
 * @author Alexander Will
 *
 */
public abstract class AbstractCssPlugin extends Plugin {
    /**
	 * Log listener that catches log events and redirects them to the central
	 * log service.
	 */
    final private ILogListener _logListener;

	/**
	 * Standard constructor.
	 */
	public AbstractCssPlugin() {
		_logListener = new CssLogListener();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void start(final BundleContext context) throws Exception {
		super.start(context);
		getLog().addLogListener(_logListener);
		doStart(context);
		CentralLogger.getInstance().getLogger(this).debug(
		    "Plugin with ID " + getPluginId() + " started"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Hook method that is called from
	 * {@link AbstractCssPlugin#start(BundleContext)}.
	 *
	 * @param context
	 *            the bundle context for this plug-in
	 * @exception Exception
	 *                if this plug-in did not start up properly
	 */
	protected abstract void doStart(final BundleContext context)
			throws Exception;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void stop(final BundleContext context) throws Exception {
		super.stop(context); // TODO Should't super.stop happen last?
		savePluginPreferences();
		doStop(context);
		CentralLogger.getInstance().getLogger(this).debug(
				"Plugin with ID " + getPluginId() + " stopped"); //$NON-NLS-1$ //$NON-NLS-2$
		getLog().removeLogListener(_logListener);
	}

	/**
	 * Hook method that is called from
	 * {@link AbstractCssPlugin#stop(BundleContext)}.
	 *
	 * @param context
	 *            the bundle context for this plug-in
	 * @exception Exception
	 *                if this method fails to shut down this plug-in
	 */
	protected abstract void doStop(final BundleContext context)
			throws Exception;

	/**
	 * Return the ID of this plugin.
	 *
	 * @return The ID of this plugin.
	 */
	public abstract String getPluginId();

	/**
	 * Returns the preference store for this plugin.
	 *
	 * @return the preferences for this plugin.
	 * @deprecated use the Eclipse Preferences Service instead.
	 */
	@Deprecated
	public Preferences getCssPluginPreferences() {
		return getPluginPreferences();
	}

    /**
     * Get the implementation of the service
     *
     * @param context
     * @param typeOfService
     * @return service implementation or null
     */
    @SuppressWarnings("unchecked")
    protected <T> T getService(final BundleContext context, final Class<T> typeOfService) {
        final ServiceReference reference = context.getServiceReference(typeOfService.getName());
        return (T) (reference == null ? null : context.getService(reference));
    }
}
