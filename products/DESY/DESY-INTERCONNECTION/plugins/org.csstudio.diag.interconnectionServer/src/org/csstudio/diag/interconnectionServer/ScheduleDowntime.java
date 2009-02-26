/*
 * Copyright (c) 2009 Stiftung Deutsches Elektronen-Synchrotron,
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

package org.csstudio.diag.interconnectionServer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.csstudio.diag.interconnectionServer.preferences.PreferenceConstants;
import org.csstudio.diag.interconnectionServer.server.IocConnection;
import org.csstudio.diag.interconnectionServer.server.IocConnectionManager;
import org.csstudio.platform.libs.dcf.actions.IAction;
import org.eclipse.core.runtime.Platform;

/**
 * Remote management action which schedules a downtime for all IOCs. This is
 * a temporary solution for testing purposes.
 * 
 * @author Joerg Rathlev
 */
public class ScheduleDowntime implements IAction {

	/**
	 * {@inheritDoc}
	 */
	public Object run(Object param) {
		if (!(param instanceof Map)) {
			return null;
		}
		
		@SuppressWarnings("unchecked")
		Map<String, String> params = (Map<String, String>) param;
		String iocName = params.get("IOC");
		
		if (iocName.contains("|")) {
			iocName = iocName.substring(0, iocName.indexOf('|'));
		}
		if (iocName.length() == 0) {
			return "Invalid IOC";
		}
		
		int dataPort = Integer.parseInt(Platform.getPreferencesService().getString(Activator.getDefault().getPluginId(),
				PreferenceConstants.DATA_PORT_NUMBER, "", null));
		IocConnection iocConnection = IocConnectionManager.getInstance().getIocConnection(iocName, dataPort);
		
		String durationParam = params.get("Duration (seconds)");
		long duration = Long.parseLong(durationParam);
		
		iocConnection.scheduleDowntime(duration, TimeUnit.SECONDS);
		
		return "Downtime successfully scheduled.";
	}

}
