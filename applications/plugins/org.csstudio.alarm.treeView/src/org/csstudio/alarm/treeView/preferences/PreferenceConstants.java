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
 package org.csstudio.alarm.treeView.preferences;

import org.apache.log4j.Logger;
import org.csstudio.alarm.treeView.AlarmTreePlugin;
import org.csstudio.platform.logging.CentralLogger;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

/**
 * Constant definitions for plug-in preferences.
 */
public final class PreferenceConstants {
	private static final Logger LOG = CentralLogger.getInstance().getLogger(PreferenceConstants.class);

	/**
	 * Private constructor.
	 */
	private PreferenceConstants() {
	    // EMPTY
	}

	/**
	 * A preference that stores the URL for the primary JMS server.
	 */
	public static final String JMS_URL_PRIMARY = "jmsurl";

	/**
	 * A preference that stores the URL for the secondary JMS server.
	 */
	public static final String JMS_URL_SECONDARY = "jms.url.2";

	/**
	 * A preference that stores the name or names of the JMS topics to connect
	 * to.
	 */
	public static final String JMS_TOPICS = "jms.queue";

	/**
	 * A preference that stores the facility names that should be displayed
	 * in the tree.
	 */
	public static final String FACILITIES = "NODE";

    public static String[] retrieveFacilityNames() {
        final IPreferencesService prefs = Platform.getPreferencesService();
        final String facilitiesPref = prefs.getString(AlarmTreePlugin.PLUGIN_ID,
                                                      FACILITIES, "", null);
        String[] facilityNames;
        if (facilitiesPref.equals("")) {
            facilityNames = new String[0];
        } else {
            facilityNames = facilitiesPref.split(";");
        }

        if (facilityNames.length == 0) {
            LOG.debug("No facility names selected, using TEST facility.");
            facilityNames = new String[] { "TEST" };
        }
        return facilityNames;
    }
}
