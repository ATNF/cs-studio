package org.csstudio.platform.internal.simpledal.dal;

import org.csstudio.platform.internal.simpledal.converters.ConverterUtil;
import org.csstudio.platform.simpledal.ValueType;
import org.epics.css.dal.CharacteristicInfo;
import org.epics.css.dal.DataExchangeException;
import org.epics.css.dal.DynamicValueCondition;
import org.epics.css.dal.DynamicValueProperty;
import org.epics.css.dal.DynamicValueState;
import org.epics.css.dal.Timestamp;
import org.epics.css.dal.context.ConnectionEvent;
import org.epics.css.dal.context.LinkAdapter;

/**
 * Utility methods for accessing characteristics and dealing with DAL
 * properties.
 * 
 * @author Sven Wende
 * 
 */
public class EpicsUtil {

	/**
	 * Waits until DAL property is connected or timeout has elapsed
	 * 
	 * @param property
	 *            the DAL property
	 * @param timeout
	 *            the timeout to wait
	 * @return <code>true</code> if property was connected
	 */
	public static boolean waitTillConnected(DynamicValueProperty property, long timeout) {
		if (property == null) {
			return false;
		}
		if (property.isConnected()) {
			return true;
		}
		if (property.isConnectionFailed()) {
			return false;
		}

		LinkAdapter link = new LinkAdapter() {
			@Override
			public synchronized void connected(ConnectionEvent e) {
				notifyAll();
			}

			@Override
			public synchronized void connectionFailed(ConnectionEvent e) {
				notifyAll();
			}
		};

		synchronized (link) {
			property.addLinkListener(link);

			if (property.isConnected()) {
				property.removeLinkListener(link);
				return true;
			}
			if (property.isConnectionFailed()) {
				property.removeLinkListener(link);
				return false;
			}

			try {
				link.wait(timeout);
			} catch (Exception e) {
				e.printStackTrace();
			}

			property.removeLinkListener(link);
		}

		return property.isConnected();

	}

	/**
	 * Returns characteristic while properly converting values and
	 * characteristic names
	 * 
	 * @param charName
	 *            characteristic name
	 * @param property
	 *            DAL property
	 * @param valueType
	 *            SDS value type
	 * @return characteristic while properly converting values and
	 *         characteristic names
	 * @throws DataExchangeException
	 */
	public static final Object getCharacteristic(String charName, DynamicValueProperty property, ValueType valueType)
			throws DataExchangeException {
		if (charName.equals(DalConnector.C_SEVERITY_INFO.getName())) {
			return EpicsUtil.toEPICSFlavorSeverity(property.getCondition());
		}
		if (charName.equals(DalConnector.C_STATUS_INFO.getName())) {
			return EpicsUtil.extratStatus(property.getCondition());
		}
		if (charName.equals(DalConnector.C_TIMESTAMP_INFO.getName())) {
			return property.getCondition().getTimestamp();
		}
		Object value = property.getCharacteristic(charName);
		if (valueType != null) {
			return ConverterUtil.convert(value, valueType);
		}
		return value;

	}

	/**
	 * Returns EPICS favored status string for DAL condition.
	 * 
	 * @param cond
	 *            DAL condition
	 * @return EPICS favored status string for DAL condition
	 */
	public static final String extratStatus(DynamicValueCondition cond) {
		if (cond == null || cond.getDescription() == null) {
			return "N/A";
		}
		return cond.getDescription();
	}

	/**
	 * Converts DAL condition to EPICS favored severity string.
	 * 
	 * @param condition
	 *            DAL condition
	 * @return EPICS favored severity string
	 */
	public static final String toEPICSFlavorSeverity(DynamicValueCondition condition) {
		if (condition.isNormal()) {
			return DynamicValueState.NORMAL.toString();
		}
		if (condition.isWarning()) {
			return DynamicValueState.WARNING.toString();
		}
		if (condition.isAlarm()) {
			return DynamicValueState.ALARM.toString();
		}
		if (condition.isError()) {
			return DynamicValueState.ERROR.toString();
		}
		return "UNKNOWN";
	}

}
