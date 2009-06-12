package org.csstudio.startup.module;

import java.util.Map;

import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;

/**
 * <code>ServicesStartupExtPoint</code> defines how the services and which services
 * should be started when the application is loading.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public interface ServicesStartupExtPoint extends CSSStartupExtensionPoint {

	/** The name of this extension point */
	public static final String NAME = "org.csstudio.startup.module.services"; //$NON-NLS-1$
	
	/**
	 * Starts all services that need to be started. In principle this method should
	 * load the {@link ServiceProxy}s and start the ones that need to be started.
	 * Other services can also be started by implementing this extension point.
	 * 
	 * @param display the display of the application
	 * @param context the application's context
	 * @param parameters contains additional parameters, which can define
	 * 			some special behaviour during the execution of this method (the keys
	 * 			are parameters names and the values are parameters values)
	 * 
	 * @return the exit code if something happened which requires to exit or restart 
	 * 			application or null if everything is alright
	 * @throws Exception if an error occurred during the operation
	 */
	public Object startServices(Display display, IApplicationContext context, Map<String, Object> parameters) throws Exception;
	
}
