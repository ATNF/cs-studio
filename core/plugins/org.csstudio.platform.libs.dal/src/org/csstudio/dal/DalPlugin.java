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
 package org.csstudio.dal;

import org.eclipse.core.runtime.Plugin;
import org.epics.css.dal.CharacteristicInfo;
import org.epics.css.dal.impl.DefaultApplicationContext;
import org.epics.css.dal.simple.SimpleDALBroker;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DalPlugin extends Plugin {
	
	{
		CharacteristicInfo.registerCharacteristicInfo(CharacteristicInfo.C_SEVERITY_INFO);
		CharacteristicInfo.registerCharacteristicInfo(CharacteristicInfo.C_TIMESTAMP_INFO);
		CharacteristicInfo.registerCharacteristicInfo(CharacteristicInfo.C_STATUS_INFO);
	}

	/**
	 * The ID of this plugin.
	 */
	public static final String ID = "org.csstudio.platform.libs.dal"; //$NON-NLS-1$

	/**
	 * The ID of the <code>plugs</code> extension point.
	 */
	public static final String EXTPOINT_PLUGS = ID + ".plugs"; //$NON-NLS-1$

	/**
	 * The shared instance.
	 */
	private static DalPlugin plugin;
	
	private static DefaultApplicationContext applicationContext;
	
	private static SimpleDALBroker broker;

	/**
	 * The constructor
	 */
	public DalPlugin() {
		plugin = this;
		applicationContext = new CssApplicationContext("CSS");
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static DalPlugin getDefault() {
		return plugin;
	}

	public SimpleDALBroker getSimpleDALBroker() {
		if (broker == null) {
			broker = SimpleDALBroker.newInstance(applicationContext);
		}
		return broker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		applicationContext.destroy();
		super.stop(context);
	}

}
