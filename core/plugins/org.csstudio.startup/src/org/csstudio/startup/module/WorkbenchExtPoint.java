package org.csstudio.startup.module;

import java.util.Map;

import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;

/**
 * <code>RunWorkbenchExtPoint</code> defines the actions that are executed during
 * the workbench startup. Only one extension point of this type is allowed per product.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public interface WorkbenchExtPoint extends CSSStartupExtensionPoint {

	/** The name of this extension point */
	public static final String NAME = "org.csstudio.startup.module.workbench"; //$NON-NLS-1$
	
	/**
	 * Is called just before the workbench is started. This method can run additional
	 * services or sets up whatever is needed for proper running of the workbench.
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
	public Object beforeWorkbenchCreation(Display display, IApplicationContext context, Map<String,Object> parameters) throws Exception;
		
	/**
	 * Is called just after the workbench is created. This method can run additional
	 * services or destroys/sets up whatever it has for proper application closing.
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
	public Object afterWorkbenchCreation(Display display, IApplicationContext context, Map<String, Object> parameters) throws Exception;
	
	/**
	 * Runs the workbench. The implementation should create the workbench
	 * with the appropriate advisors and run it. 
	 * 
	 * @param display the display of the application
	 * @param context the application's context
	 * @param parameters contains additional parameters, which can define
	 * 			some special behaviour during the execution of this method (the keys
	 * 			are parameters names and the values are parameters values)
	 * 
	 * @return the exit code, which defines how the application exited
	 * 
	 * @throws Exception if an error occurred during the operation
	 */
	public Object runWorkbench(Display display, IApplicationContext context, Map<String, Object> parameters) throws Exception;
}
