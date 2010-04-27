package org.csstudio.archive;

import org.apache.log4j.Logger;
import org.csstudio.platform.AbstractCssPlugin;
import org.csstudio.platform.logging.CentralLogger;
import org.osgi.framework.BundleContext;

/** The activator class controls the plug-in life cycle
 *  @author Jan Hatje
 *  @author Albert Kagarmanov
 *  @author Kay Kasemir
 */
public class Activator extends AbstractCssPlugin
{
	/** The plug-in ID */
	public static final String ID = "org.csstudio.archive"; //$NON-NLS-1$

	/**
	 * The shared instance
	 */
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	    plugin = this;
	}

    /* @see org.csstudio.platform.AbstractCssPlugin#getPluginId() */
    @Override
    public String getPluginId()
    {   return ID;  }

	/* @see org.csstudio.platform.AbstractCssPlugin#doStart(org.osgi.framework.BundleContext)
     */
    @Override
    protected void doStart(final BundleContext context) throws Exception
    {
        // NOP
    }

    /* @see org.csstudio.platform.AbstractCssPlugin#doStop(org.osgi.framework.BundleContext)
     */
    @Override
    protected void doStop(final BundleContext context) throws Exception
    {
        plugin = null;
    }

	/**
	 *  @return shared instance
	 */
	public static Activator getDefault() {
	    return plugin;
	}

    /** @return Log4j Logger */
    public static Logger getLogger()
    {
        return CentralLogger.getInstance().getLogger(plugin);
    }
}
