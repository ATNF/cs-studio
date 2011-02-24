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

package org.epics.css.dal.epics;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.Channel;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_STS_String;
import gov.aps.jca.dbr.LABELS;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import java.lang.reflect.Array;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.epics.css.dal.AccessType;
import org.epics.css.dal.CharacteristicInfo;
import org.epics.css.dal.DataExchangeException;
import org.epics.css.dal.DynamicValueCondition;
import org.epics.css.dal.DynamicValueState;
import org.epics.css.dal.EnumPropertyCharacteristics;
import org.epics.css.dal.NumericPropertyCharacteristics;
import org.epics.css.dal.PatternPropertyCharacteristics;
import org.epics.css.dal.PropertyCharacteristics;
import org.epics.css.dal.RemoteException;
import org.epics.css.dal.Request;
import org.epics.css.dal.ResponseListener;
import org.epics.css.dal.SequencePropertyCharacteristics;
import org.epics.css.dal.Timestamp;
import org.epics.css.dal.context.ConnectionState;
import org.epics.css.dal.impl.PropertyUtilities;
import org.epics.css.dal.impl.RequestImpl;
import org.epics.css.dal.impl.ResponseImpl;
import org.epics.css.dal.proxy.AbstractPropertyProxyImpl;
import org.epics.css.dal.proxy.DirectoryProxy;
import org.epics.css.dal.proxy.MonitorProxy;
import org.epics.css.dal.proxy.PropertyProxy;
import org.epics.css.dal.proxy.SyncPropertyProxy;
import org.epics.css.dal.simple.impl.DataUtil;
import org.epics.css.dal.spi.Plugs;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.util.BitCondition;

/**
 * Simulations implementations of proxy.
 * 
 * @author ikriznar
 * 
 */
public class PropertyProxyImpl<T> extends AbstractPropertyProxyImpl<T,EPICSPlug,MonitorProxyImpl<T>> implements
		PropertyProxy<T,EPICSPlug>, SyncPropertyProxy<T,EPICSPlug>, DirectoryProxy<EPICSPlug>,
		ConnectionListener, GetListener {
	
	/** C_CONDITION_WHEN_CLEARED characteristic for pattern channel */
	public static BitCondition[] patternWhenCleared = new BitCondition[] {
			BitCondition.UNUSED, BitCondition.UNUSED, BitCondition.UNUSED,
			BitCondition.UNUSED, BitCondition.UNUSED, BitCondition.UNUSED,
			BitCondition.UNUSED, BitCondition.UNUSED, BitCondition.UNUSED,
			BitCondition.UNUSED, BitCondition.UNUSED, BitCondition.UNUSED,
			BitCondition.UNUSED, BitCondition.UNUSED, BitCondition.UNUSED,
			BitCondition.UNUSED
		};

	/** C_CONDITION_WHEN_SET characteristic for pattern channel */
	public static BitCondition[] patternWhenSet = new BitCondition[] {
			BitCondition.OK, BitCondition.OK, BitCondition.OK, 
			BitCondition.OK, BitCondition.OK, BitCondition.OK, 
			BitCondition.OK, BitCondition.OK, BitCondition.OK,
			BitCondition.OK, BitCondition.OK, BitCondition.OK, 
			BitCondition.OK, BitCondition.OK, BitCondition.OK,
			BitCondition.OK
		};

	/** C_BIT_DESCRIPTION characteristic for pattern channel */
	public static String[] patternBitDescription = new String[] {
			"bit 0", "bit 1", "bit 2", "bit 3", "bit 4", "bit 5", "bit 6",
			"bit 7", "bit 8", "bit 9", "bit 10", "bit 11", "bit 12", "bit 13",
			"bit 14", "bit 15"
		};

	/** C_BIT_MASK characteristic for pattern channel */
	public static BitSet patternBitMask = new BitSet(16);
	{
		patternBitMask.set(0, 16);
	}

	protected Channel channel;

	protected String condDesc;

	protected DBRType type;
	
	protected Class<T> dataType;

	protected int elementCount;
	
	private ThreadPoolExecutor executor;
	
	private boolean initializeCharacteristicsRunning = false;
	
	// This task changes channel with INITIAL state to CONNECTION_FAILED.
	private class AbortConnectionRunnable implements Runnable {
		public void run() {
			ConnectionState cs = getConnectionState();
			if (cs == ConnectionState.CONNECTING) {
				//abortConnection = true;
				setConnectionState(ConnectionState.CONNECTION_FAILED);
			}
		}
	}
	private TimerTask abortConnectionTask = null;
	//private boolean abortConnection = false;
	
	/**
	 * Create a new proprty instance (channel).
	 * @param plug plug hosting this property.
	 * @param name name of the property.
	 * @param dataType java data type to work with.
	 * @param type channel type to work with.
	 * @throws RemoteException thrown on failure.
	 */
	public PropertyProxyImpl(EPICSPlug plug, String name, Class<T> dataType, DBRType type) throws RemoteException {
		super(name,plug);

		if (type.getValue() >= DBR_STS_String.TYPE.getValue())
			throw new IllegalArgumentException("type must be value-only type");
		
		synchronized (this) {
			this.type = type;
			this.dataType = dataType;
			setCondition(new DynamicValueCondition(EnumSet.of(DynamicValueState.LINK_NOT_AVAILABLE, DynamicValueState.NO_VALUE)));
			setConnectionState(ConnectionState.READY);
			setConnectionState(ConnectionState.CONNECTING);
			// create channel
			try {
				this.channel = plug.getContext().createChannel(name, this);
			} catch (Throwable th) {
				throw new RemoteException(this, "Failed create CA channel: "+PlugUtilities.toShortErrorReport(th), th);
			}
			abortConnectionTask = plug.schedule(new AbortConnectionRunnable(), Plugs.getInitialConnectionTimeout(plug.getConfiguration()), 0);
		}

		
	}
	
	/*
	 * @see org.epics.css.dal.proxy.AbstractProxyImpl#destroy()
	 */
	@Override
	public synchronized void destroy() {
		
		if (connectionStateMachine.isConnected()) {
			setConnectionState(ConnectionState.DISCONNECTING);
		}
		
		super.destroy();

		if (channel.getConnectionState() != Channel.CLOSED) { // FIXME workaround because CAJChannel.removeConnectionListener throws IllegalStateException: "Channel closed."
			try {
				channel.removeConnectionListener(this);
			} catch (IllegalStateException e) {
				// we ignore
			} catch (CAException e) {
				Logger.getLogger(this.getClass()).warn("Removing CA listener: "+PlugUtilities.toShortErrorReport(e), e);
			}
		}
		// destory channel
		channel.dispose();
		
		if (connectionStateMachine.getConnectionState()==ConnectionState.DISCONNECTING) {
			setConnectionState(ConnectionState.DISCONNECTED);
		}
		setConnectionState(ConnectionState.DESTROYED);
	}

	/*
	 * @see org.epics.css.dal.proxy.PropertyProxy#getValueAsync(org.epics.css.dal.ResponseListener)
	 */
	public Request<T> getValueAsync(ResponseListener<T> callback)
			throws DataExchangeException {
		GetRequest<T> r = new GetRequest<T>(this, callback);
		try {
			channel.get(type, channel.getElementCount(), r);
			plug.flushIO();
		} catch (Exception e) {
			r.addResponse(new ResponseImpl<T>(this, r, null, "value", false, e,
					getCondition(), null, true));
		}
		return r;
	}

	/*
	 * @see org.epics.css.dal.proxy.PropertyProxy#setValueAsync(T, org.epics.css.dal.ResponseListener)
	 */
	public Request<T> setValueAsync(T value, ResponseListener<T> callback)
			throws DataExchangeException {
		PutRequest<T> r = new PutRequest<T>(this, callback, value);
		try {
			Object o = PlugUtilities.toDBRValue(value, channel.getFieldType());
			if (channel instanceof CAJChannel) 
				((CAJChannel) channel).put(PlugUtilities.toDBRType(value.getClass()), Array.getLength(o), o, r);
			else {
				// TODO workaround until Channel supports put(DBRType, int, Object, PutListener)
				PlugUtilities.put(channel, o, r);
			}
			plug.flushIO();
		} catch (Exception e) {
			r.addResponse(new ResponseImpl<T>(this, r, value, "value", false, e,
					getCondition(), null, true));
		}
		return r;
	}

	/**
	 * Get listener implementation to implement sync. get.  
	 */
	private class GetListenerImpl implements GetListener {
		volatile GetEvent event = null;

		public synchronized void getCompleted(GetEvent ev) {
			event = ev;
			this.notifyAll();
		}
	}
	/**
	 * Connection listener implementation to implement sync. get.  
	 */
	private class ConnectionListenerImpl implements ConnectionListener {
		//volatile ConnectionEvent event= null;

		public synchronized void connectionChanged(ConnectionEvent arg0) {
			//event=arg0;
			this.notifyAll();
		}

	}
	
	/*
	 * @see org.epics.css.dal.proxy.SyncPropertyProxy#getValueSync()
	 */
	public T getValueSync() throws DataExchangeException {
		try 
		{

			GetListenerImpl listener = new GetListenerImpl();
	         synchronized (listener) {
				channel.get(type, channel.getElementCount(), listener);
				plug.flushIO();

				try {
					listener.wait((long) (plug.getTimeout() * 1000));
				} catch (InterruptedException e) {
					// noop
				}
			}

			final GetEvent event = listener.event;
			if (event == null)
				throw new TimeoutException("Get timeout.");

			// status check
			if (event.getStatus() != CAStatus.NORMAL)
				throw new CAStatusException(event.getStatus(), "Get failed.");

			// sanity check
			if (event.getDBR() == null)
				throw new DataExchangeException(this, "Get failed.");
			
			return toJavaValue(event.getDBR());
		} catch (CAException e) {
			throw new DataExchangeException(this, "Get failed: "+PlugUtilities.toShortErrorReport(e), e);
		} catch (TimeoutException e) {
			throw new DataExchangeException(this, "Get failed with timeout.", e);
		}
	}

	/*
	 * @see org.epics.css.dal.proxy.SyncPropertyProxy#setValueSync(java.lang.Object)
	 */
	public void setValueSync(Object value) throws DataExchangeException {
		try {
			Object o = PlugUtilities.toDBRValue(value, channel.getFieldType());
			if (channel instanceof CAJChannel) 
				((CAJChannel) channel).put(PlugUtilities.toDBRType(value.getClass()), Array.getLength(o), o);
			else {
				// TODO workaround until Channel supports put(DBRType, int, Object)
				PlugUtilities.put(channel, o);
			}
			// put does not affect on pendIO
			plug.flushIO();
		} catch (CAException e) {
			throw new DataExchangeException(this, "Set failed: "+PlugUtilities.toShortErrorReport(e), e);
		}
	}


	/*
	 * @see org.epics.css.dal.proxy.PropertyProxy#isSettable()
	 */
	public boolean isSettable() {
		return channel.getWriteAccess();
	}

	/*
	 * @see org.epics.css.dal.proxy.PropertyProxy#createMonitor(org.epics.css.dal.ResponseListener)
	 */
	public synchronized MonitorProxy createMonitor(ResponseListener<T> callback, Map<String,Object> param)
			throws RemoteException {

		if (getConnectionState() == ConnectionState.DESTROYED)
			throw new RemoteException(this, "Proxy destroyed.");
		try {
			MonitorProxyImpl<T> m = new MonitorProxyImpl<T>(plug, this, callback, param);
			return m;
		} catch (Throwable th) {
			throw new RemoteException(this, "Failed to create new monitor: "+PlugUtilities.toShortErrorReport(th), th);
		}
	}

	/**
	 * Characteristics async get listener.
	 * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event.GetEvent)
	 */
	public void getCompleted(GetEvent ev) {
		if (!connectionStateMachine.isConnected() 
				|| channel.getConnectionState()!= Channel.CONNECTED) 
		{
			/*
			 * It could happen that SimpleDAL broker does simple get and then destroys connection before CTRL_DBR request finishes.
			 * In this case CTRL_DBR has nothing to do any more.
			 */
			return;
		}
		if (ev.getStatus() == CAStatus.NORMAL)
			createCharacteristics(ev.getDBR());
		else if (ev.getDBR() == null) {
			recoverFromNullDbr();
		}
			
	}

	/**
	 * Creates default characteristics.
	 */
	protected void createDefaultCharacteristics() {
		synchronized (getCharacteristics()) {
			
			updateCharacteristic(PropertyCharacteristics.C_DESCRIPTION, "EPICS Channel '" + name + "'");
			updateCharacteristic(PropertyCharacteristics.C_DISPLAY_NAME, name);
			updateCharacteristic(PropertyCharacteristics.C_POSITION, new Double(0));
			updateCharacteristic(PropertyCharacteristics.C_PROPERTY_TYPE, "property");
			updateCharacteristic(NumericPropertyCharacteristics.C_SCALE_TYPE, "linear");

			updateCharacteristic(SequencePropertyCharacteristics.C_SEQUENCE_LENGTH, new Integer(elementCount));

			if (channel != null 
					&& channel.getConnectionState() == Channel.CONNECTED
					&& getConnectionState() == ConnectionState.CONNECTED) {
				
				try {

					DBRType ft= channel.getFieldType();
					updateCharacteristic("fieldType",ft);
					
					if (ft.isENUM()) {
						updateCharacteristic(NumericPropertyCharacteristics.C_RESOLUTION, 0xF);
					} else if (ft.isBYTE()) {
						updateCharacteristic(NumericPropertyCharacteristics.C_RESOLUTION, 0x8);
					} else if (ft.isSHORT()) {
						updateCharacteristic(NumericPropertyCharacteristics.C_RESOLUTION, 0xFF);
					} else {
						updateCharacteristic(NumericPropertyCharacteristics.C_RESOLUTION, 0xFFFF);
					}

					updateCharacteristic(PropertyCharacteristics.C_ACCESS_TYPE,AccessType.getAccess(channel.getReadAccess(),channel.getWriteAccess()));
					updateCharacteristic(PropertyCharacteristics.C_HOSTNAME, channel.getHostName());
					updateCharacteristic(EpicsPropertyCharacteristics.EPICS_NUMBER_OF_ELEMENTS, channel.getElementCount());

				} catch (IllegalStateException ex) {
					/*
					 * JCA channel was probably closed in the mean time, 
					 * nothing to do.
					 */

					updateCharacteristic(NumericPropertyCharacteristics.C_RESOLUTION, 0xFFFF);
					
					updateCharacteristic(PropertyCharacteristics.C_ACCESS_TYPE,AccessType.NONE);
					updateCharacteristic(PropertyCharacteristics.C_HOSTNAME,"unknown");
					updateCharacteristic(EpicsPropertyCharacteristics.EPICS_NUMBER_OF_ELEMENTS,1);
				}

			} else {

				updateCharacteristic(NumericPropertyCharacteristics.C_RESOLUTION, 0xFFFF);
				
				updateCharacteristic(PropertyCharacteristics.C_ACCESS_TYPE,AccessType.NONE);
				updateCharacteristic(PropertyCharacteristics.C_HOSTNAME,"unknown");
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_NUMBER_OF_ELEMENTS,1);
			}

			updateCharacteristic(PropertyCharacteristics.C_DATATYPE,PlugUtilities.getDataType(null));

			//characteristics.put(NumericPropertyCharacteristics.C_SCALE_TYPE, );

			updateCharacteristic(PatternPropertyCharacteristics.C_CONDITION_WHEN_SET, patternWhenSet);
			updateCharacteristic(PatternPropertyCharacteristics.C_CONDITION_WHEN_CLEARED, patternWhenCleared);

			updateCharacteristic(PatternPropertyCharacteristics.C_BIT_MASK, patternBitMask);
			updateCharacteristic(PatternPropertyCharacteristics.C_BIT_DESCRIPTIONS, patternBitDescription);
			
		}
	}
	
	/*private void abortInitalDBR() {
		synchronized (characteristics) {
			characteristics.notifyAll();
			initializeCharacteristicsRunning = false;
		}
	}*/

	/**
	 * Creates characteristics from given DBR.
	 * @param dbr DBR containign characteristics.
	 */
	protected void createCharacteristics(DBR dbr)
	{
		synchronized (getCharacteristics()) {
	
			if (channel ==null || channel.getConnectionState()!= Channel.CONNECTED) {
				/*
				 * It could happen that SimpleDAL broker does simple get and then destroys connection before CTRL_DBR request finishes.
				 * In this case CTRL_DBR has nothing to do any more.
				 */
				return;
			}

			System.out.println(">>> "+name+" Creating characteristics from DBR");

			if (dbr.isCTRL())
			{
				CTRL gr = (CTRL)dbr;
				updateCharacteristic(NumericPropertyCharacteristics.C_UNITS, gr.getUnits());
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_UNITS, gr.getUnits());
				
				// Integer -> Long needed here
				if (dbr.isINT())
				{
					updateCharacteristic(NumericPropertyCharacteristics.C_MINIMUM, new Long(gr.getLowerCtrlLimit().longValue()));
					updateCharacteristic(NumericPropertyCharacteristics.C_MAXIMUM, new Long(gr.getUpperCtrlLimit().longValue()));

					updateCharacteristic(NumericPropertyCharacteristics.C_GRAPH_MIN, new Long(gr.getLowerDispLimit().longValue()));
					updateCharacteristic(NumericPropertyCharacteristics.C_GRAPH_MAX, new Long(gr.getUpperDispLimit().longValue()));
					
					updateCharacteristic(NumericPropertyCharacteristics.C_WARNING_MIN, new Long(gr.getLowerWarningLimit().longValue()));
					updateCharacteristic(NumericPropertyCharacteristics.C_WARNING_MAX, new Long(gr.getUpperWarningLimit().longValue()));
					
					updateCharacteristic(NumericPropertyCharacteristics.C_ALARM_MIN, new Long(gr.getLowerAlarmLimit().longValue()));
					updateCharacteristic(NumericPropertyCharacteristics.C_ALARM_MAX, new Long(gr.getUpperAlarmLimit().longValue()));
					
										
				}
				else
				{
					updateCharacteristic(NumericPropertyCharacteristics.C_MINIMUM, gr.getLowerCtrlLimit());
					updateCharacteristic(NumericPropertyCharacteristics.C_MAXIMUM, gr.getUpperCtrlLimit());

					updateCharacteristic(NumericPropertyCharacteristics.C_GRAPH_MIN, gr.getLowerDispLimit());
					updateCharacteristic(NumericPropertyCharacteristics.C_GRAPH_MAX, gr.getUpperDispLimit());

					updateCharacteristic(NumericPropertyCharacteristics.C_WARNING_MIN, gr.getLowerWarningLimit());
					updateCharacteristic(NumericPropertyCharacteristics.C_WARNING_MAX, gr.getUpperWarningLimit());

					updateCharacteristic(NumericPropertyCharacteristics.C_ALARM_MIN, gr.getLowerAlarmLimit());
					updateCharacteristic(NumericPropertyCharacteristics.C_ALARM_MAX, gr.getUpperAlarmLimit());
				}
				
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_MIN, getCharacteristics().get(NumericPropertyCharacteristics.C_MINIMUM));
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_MAX, getCharacteristics().get(NumericPropertyCharacteristics.C_MAXIMUM));
				
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_OPR_MIN, getCharacteristics().get(NumericPropertyCharacteristics.C_GRAPH_MIN));
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_OPR_MAX, getCharacteristics().get(NumericPropertyCharacteristics.C_GRAPH_MAX));
				
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_WARNING_MAX, getCharacteristics().get(NumericPropertyCharacteristics.C_WARNING_MAX));
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_WARNING_MIN, getCharacteristics().get(NumericPropertyCharacteristics.C_WARNING_MIN));
				
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_ALARM_MAX, getCharacteristics().get(NumericPropertyCharacteristics.C_ALARM_MAX));
				updateCharacteristic(EpicsPropertyCharacteristics.EPICS_ALARM_MIN, getCharacteristics().get(NumericPropertyCharacteristics.C_ALARM_MIN));
				
//				int resolution = ((Number) characteristics.get(NumericPropertyCharacteristics.C_RESOLUTION)).intValue();
//				characteristics.put(CharacteristicInfo.C_META_DATA.getName(), DataUtil.createNumericMetaData(
//						gr.getLowerDispLimit().doubleValue(), gr.getUpperDispLimit().doubleValue(), 
//						gr.getLowerWarningLimit().doubleValue(), gr.getUpperWarningLimit().doubleValue(), 
//						gr.getLowerAlarmLimit().doubleValue(), gr.getUpperAlarmLimit().doubleValue(), 
//						resolution, gr.getUnits()));
				
			} else {
				updateCharacteristic(NumericPropertyCharacteristics.C_UNITS, "N/A");
			}
			
			if (dbr.isPRECSION())
			{
				short precision = ((PRECISION)dbr).getPrecision();
				updateCharacteristic(NumericPropertyCharacteristics.C_FORMAT, "%."  + precision + "f");
			}
			else if (dbr.isSTRING())
				updateCharacteristic(NumericPropertyCharacteristics.C_FORMAT, "%s");
			else
				updateCharacteristic(NumericPropertyCharacteristics.C_FORMAT, "%d");
			
			if (dbr.isLABELS())
			{
				String[] labels = ((LABELS)dbr).getLabels();
				updateCharacteristic(EnumPropertyCharacteristics.C_ENUM_DESCRIPTIONS, labels);
				updateCharacteristic(PatternPropertyCharacteristics.C_BIT_DESCRIPTIONS, labels);

				// create array of values (Long values)
				Object[] values = new Object[labels.length];
				for (int i = 0; i < values.length; i++)
					values[i] = new Long(i);
				
				updateCharacteristic(EnumPropertyCharacteristics.C_ENUM_VALUES, values);
				
//				updateCharacteristic(CharacteristicInfo.C_META_DATA.getName(), DataUtil.createEnumeratedMetaData(labels,values));

			}
			
			updateCharacteristic(PropertyCharacteristics.C_ACCESS_TYPE,channel != null ? AccessType.getAccess(channel.getReadAccess(),channel.getWriteAccess()) : AccessType.NONE);
			updateCharacteristic(PropertyCharacteristics.C_HOSTNAME,channel != null ? channel.getHostName() : "unknown");
			updateCharacteristic(EpicsPropertyCharacteristics.EPICS_NUMBER_OF_ELEMENTS, channel != null ? channel.getElementCount() : 1);
			updateCharacteristic(PropertyCharacteristics.C_DATATYPE,PlugUtilities.getDataType(dbr.getType()));
			
			DynamicValueCondition condition=null;
			if(dbr.isSTS()) {
				condition = deriveConditionWithDBR((STS)dbr);			
			}
			
			createSpecificCharacteristics(dbr);

			updateCharacteristic(CharacteristicInfo.C_META_DATA.getName(), DataUtil.createMetaData(getCharacteristics()));

			if (condition==null) {
				updateConditionWith(DynamicValueCondition.METADATA_AVAILABLE_MESSAGE, DynamicValueState.HAS_METADATA);
			} else {
				condition.getStates().add(DynamicValueState.HAS_METADATA);
				setCondition(condition);
			}
			
			System.out.println(">>> "+name+" characteristics from DBR "+getCharacteristics());

			getCharacteristics().notifyAll();
			initializeCharacteristicsRunning = false;
		}
	}
	
	protected void createSpecificCharacteristics(DBR dbr) {
		// specific proxy implementation may override this and provide own characteristic initialization
	}

	/**
	 * Initiate characteristics search.
	 */
	protected void initializeCharacteristics()
	{
		synchronized (getCharacteristics()) {
			if (!connectionStateMachine.isConnected() 
					|| channel.getConnectionState() != Channel.CONNECTED)
			{
				return;
			}
			
			System.out.println(">>> "+name+" initialize started");
			
			if (initializeCharacteristicsRunning) return;
			initializeCharacteristicsRunning = true;
	
			// convert to CTRL value
			characteristicsRequestTimestamp = System.currentTimeMillis();		
			try {
				elementCount = channel.getElementCount();
	
				createDefaultCharacteristics();
	
				final int CTRL_OFFSET = 28;
				DBRType ctrlType = DBRType.forValue(type.getValue() + CTRL_OFFSET);
				channel.get(ctrlType, 1, this);
				plug.flushIO();
			} catch (Throwable th) {
				if (!connectionStateMachine.isConnected() 
						|| channel.getConnectionState() != Channel.CONNECTED)
				{
					return;
				}
				createDefaultCharacteristics();
				updateConditionWith("Meta data request failed: "+PlugUtilities.toShortErrorReport(th), DynamicValueState.ERROR);
				synchronized (getCharacteristics()) {
					getCharacteristics().notifyAll();
				}
			}
		}
	}
	
	protected static final long CHARACTERISTICS_TIMEOUT = 5000;
	protected long characteristicsRequestTimestamp = System.currentTimeMillis();
	
	/*
	 * @see DirectoryProxy#getCharacteristicNames()
	 */
	public String[] getCharacteristicNames() throws DataExchangeException {
		synchronized (getCharacteristics())
		{
			// characteristics not initialized yet... wait
			if (getCharacteristics().size() == 0)
			{
				initializeCharacteristics();
				long timeToWait = CHARACTERISTICS_TIMEOUT - (System.currentTimeMillis() - characteristicsRequestTimestamp);
				if (timeToWait > 0)
				{
					try {
						getCharacteristics().wait(timeToWait);
					} catch (InterruptedException e) {
						// noop
					}
				}
				
			}
			
			// get names
			String[] names = new String[getCharacteristics().size()];
			getCharacteristics().keySet().toArray(names);
			return names;
		}
	}

	@Override
	protected Object processCharacteristicBeforeCache(Object value,
			String characteristicName) 
	{
		if (value!=null) {
			return value;
		}
		synchronized (getCharacteristics())
		{
			System.out.println(">>> "+name+" char "+characteristicName+" "+value+" process before size "+getCharacteristics().size());
			// characteristics not iniialized yet... wait
			if (getCharacteristics().size() == 0)
			{
				initializeCharacteristics();
				long timeToWait = CHARACTERISTICS_TIMEOUT +100 - (System.currentTimeMillis() - characteristicsRequestTimestamp);
				System.out.println(">>> "+name+" char "+characteristicName+" "+value+" process before wait "+timeToWait);
				if (timeToWait > 0)
				{
					try {
						getCharacteristics().wait(timeToWait);
					} catch (InterruptedException e) {
						// noop
					}
				}
				value= getCharacteristics().get(characteristicName);
			}
		}
		System.out.println(">>> "+name+" char "+characteristicName+" "+value+" process before wait done");
		return value;
	}
	
	@Override
	protected Object processCharacteristicAfterCache(Object value,
			String characteristicName) 
	{
		if (value==null && initializeCharacteristicsRunning) {
			System.out.println(">>> "+name+" char "+characteristicName+" "+value+" process after");
			synchronized (getCharacteristics()) {
				if (initializeCharacteristicsRunning) {
					long timeToWait = CHARACTERISTICS_TIMEOUT + 100 - (System.currentTimeMillis() - characteristicsRequestTimestamp);
					System.out.println(">>> "+name+" char "+characteristicName+" "+value+" process after wait "+timeToWait);
					if (timeToWait > 0)
					{
						try {
							getCharacteristics().wait(timeToWait);
						} catch (InterruptedException e) {
							// noop
						}
					}
					value= getCharacteristics().get(characteristicName);
				}
			}
		}
		System.out.println(">>> "+name+" char "+characteristicName+" "+value+" process after wait done");
		if (value == null && characteristicName.length() <= 4) {
			value = getCharacteristicFromField(characteristicName);
			if (value!=null) {
				synchronized (getCharacteristics()) {
					updateCharacteristic(characteristicName, value);
				}
			}
			System.out.println(">>> "+name+" char "+characteristicName+" "+value+" process after from field");
		}
		return value;
	}
	
	private Object getCharacteristicFromField(String characteristicName) {
		if (channel.getConnectionState() != Channel.CONNECTED)
			return null;
		
		GetListenerImpl listener = new GetListenerImpl();
        synchronized (listener) {
        	try {
        		CAJChannel ch=null;
        		ConnectionListenerImpl conn= new ConnectionListenerImpl();
        		synchronized (conn) {
    				ch = (CAJChannel)plug.getContext().createChannel(name+"."+characteristicName,conn);
    				if (ch.getConnectionState() != Channel.CONNECTED) {
	    				try {
	    					conn.wait((long) (plug.getTimeout() * 1000));
	    				} catch (InterruptedException e) {
	    					// noop
	    				}
    				}
				}
				ch.get(1, listener);
				plug.flushIO();
				try {
					listener.wait((long) (plug.getTimeout() * 1000));
				} catch (InterruptedException e) {
					// noop
				}
				ch.dispose();
			} catch (IllegalStateException e1) {
				Logger.getLogger(this.getClass()).warn("Characteristic failed.", e1);
			} catch (CAException e1) {
				Logger.getLogger(this.getClass()).warn("Characteristic failed: "+PlugUtilities.toShortErrorReport(e1), e1);
			}			
		}

		final GetEvent event = listener.event;
		if (event == null || event.getStatus() != CAStatus.NORMAL || event.getDBR() == null) {
			return null;
		}		
				
		return event.getDBR().getValue();
	}
	
	
	@Override
	protected void handleCharacteristicsReponses(final String[] characteristics,
			final ResponseListener<Object> callback, 
			final RequestImpl<Object> request) 
	{

		Runnable getCharsAsync = new Runnable () {

			public void run() {
				handleCharacteristicsReponsesSync(characteristics, callback, request);
			}
			
		};
		execute(getCharsAsync);
	}
	
	/**
	 * Convert DBR to Java value.
	 * @param dbr DBR to convert.
	 * @return converted Java value.
	 */
	public final T toJavaValue(DBR dbr) {
		return PlugUtilities.toJavaValue(dbr, dataType, channel.getFieldType());
	}
	
	/**
	 * Get CA channel.
	 * @return channel.
	 */
	protected Channel getChannel() {
		return channel;
	}

	/**
	 * Get DBR type (used to query data).
	 * @return DBR type.
	 */
	protected DBRType getType() {
		return type;
	}
	
	/**
	 * Update conditions.
	 * @param dbr status DBR.
	 */
	public void updateConditionWithDBR(DBR dbr) {
		if (!dbr.isSTS()) {
			return;
		}
		STS sts= (STS)dbr;
		DynamicValueCondition cond= deriveConditionWithDBR(sts);
		setCondition(cond);
		if (plug.isDbrUpdatesCharacteristics()) {
			synchronized (getCharacteristics()) {
				updateCharacteristic(
						CharacteristicInfo.C_SEVERITY.getName()
						,getLocalProxyCharacteristic(CharacteristicInfo.C_SEVERITY.getName()));
				updateCharacteristic(
						CharacteristicInfo.C_STATUS.getName()
						,getLocalProxyCharacteristic(CharacteristicInfo.C_STATUS.getName()));
				updateCharacteristic(
						CharacteristicInfo.C_TIMESTAMP.getName()
						,getLocalProxyCharacteristic(CharacteristicInfo.C_TIMESTAMP.getName()));
			}
		}
	}

	/**
	 * Creates copy of current condition condition .
	 * 
	 * @param dbr status DBR.
	 * @param notify if true the listeners will be notified about the change 
	 * 			of condition otherwise the condition will be created and returned
	 */
	private DynamicValueCondition deriveConditionWithDBR(STS dbr) {

		Status st = dbr.getStatus();
		Severity se = dbr.getSeverity();

		condDesc = st.getName();
		
		EnumSet<DynamicValueState> states = EnumSet.copyOf(getCondition().getStates());
		boolean change=false;
		
		if (se == Severity.NO_ALARM) {
			change |= states.add(DynamicValueState.NORMAL);
			change |= states.remove(DynamicValueState.WARNING);
			change |= states.remove(DynamicValueState.ALARM);
			change |= states.remove(DynamicValueState.ERROR);
		} else if (se == Severity.MINOR_ALARM) {
			change |= states.remove(DynamicValueState.NORMAL);
			change |= states.add(DynamicValueState.WARNING);
			change |= states.remove(DynamicValueState.ALARM);
			change |= states.remove(DynamicValueState.ERROR);
		} else if (se == Severity.MAJOR_ALARM) {
			change |= states.remove(DynamicValueState.NORMAL);
			change |= states.remove(DynamicValueState.WARNING);
			change |= states.add(DynamicValueState.ALARM);
			change |= states.remove(DynamicValueState.ERROR);
		} else if (se == Severity.INVALID_ALARM) {
			change |= states.remove(DynamicValueState.NORMAL);
			change |= states.remove(DynamicValueState.WARNING);
			change |= states.remove(DynamicValueState.ALARM);
			change |= states.add(DynamicValueState.ERROR);
		}
		
		if (!change && equal(condDesc, getCondition().getDescription())) {
			return getCondition();
		}

		Timestamp timestamp = null;
		//((TIME)dbr).getTimeStamp() != null - could happen
		if (dbr instanceof TIME && ((TIME)dbr).getTimeStamp() != null) {
			timestamp = PlugUtilities.convertTimestamp(((TIME) dbr).getTimeStamp());
		}

		return new DynamicValueCondition(states, timestamp, condDesc);
		
	}

	/*
	 * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
	 */
	public synchronized void connectionChanged(ConnectionEvent event) {
		if (abortConnectionTask != null) abortConnectionTask.cancel();
		// this prevented the proxy from ever connecting
//		if (abortConnection) return;
		
		Runnable connChangedRunnable = new Runnable () {

			public void run() {
//				 Maps JCA states to DAL states
				gov.aps.jca.Channel.ConnectionState c= channel.getConnectionState();
				if (c==null) {
					Logger.getLogger(PropertyProxyImpl.class).debug(PropertyProxyImpl.class.getName()+": JCA connection state for "+channel.getName()+" is NULL, connection event ignored!");
					return;
				} 
				if (c == gov.aps.jca.Channel.ConnectionState.CLOSED) {
					setConnectionState(ConnectionState.DESTROYED);
				} else if (c == gov.aps.jca.Channel.ConnectionState.CONNECTED) {
					setConnectionState(ConnectionState.CONNECTED);
					if (plug.isInitializeCharacteristicsOnConnect()) {
						synchronized (getCharacteristics()) {
							if (getCharacteristics().size() == 0) {
								initializeCharacteristics();
							}
						}
					}
				} else if (c == gov.aps.jca.Channel.ConnectionState.DISCONNECTED) {
					setConnectionState(ConnectionState.CONNECTION_LOST);
				} else if (c == gov.aps.jca.Channel.ConnectionState.NEVER_CONNECTED) {
					setConnectionState(ConnectionState.CONNECTING);
				}		
			}
			
		};
		
		if (getPlug().getMaxThreads() == 0) {
			execute(connChangedRunnable);
		}
		else if (!getExecutor().isShutdown()) {
			execute(connChangedRunnable);
		}
	}

	/*
	 * @see org.epics.css.dal.proxy.AbstractProxyImpl#setConnectionState(org.epics.css.dal.context.ConnectionState)
	 */
	@Override
	public void setConnectionState(ConnectionState s) {
		super.setConnectionState(s);
		if (s == ConnectionState.DESTROYED) {
			if (getPlug().getMaxThreads() != 0 && !getPlug().isUseCommonExecutor()) {
				getExecutor().shutdown();
		        try {
		            if (!getExecutor().awaitTermination(1, TimeUnit.SECONDS))
		                getExecutor().shutdownNow();
		        } catch (InterruptedException ie) {  }
			}
		}
	}

	/*
	 * @see org.epics.css.dal.proxy.DirectoryProxy#refresh()
	 */
	public void refresh() {
		initializeCharacteristics();
	}

	/**
	 * Executes a <code>Runnable</code>. The <code>Runnable</code> is run in the same thread if
	 * {@link EPICSPlug#PROPERTY_MAX_THREADS} is equal to 0. Otherwise it is delegated to the
	 * <code>Executor</code> ({@link #getExecutor()}).
	 * 
	 * @param r the <code>Runnable</code> to run
	 */
	protected void execute(Runnable r) {
		if (getPlug().getMaxThreads() > 0) {
			getExecutor().execute(r);
		}
		else {
			r.run();
		}
	}
	
	/**
	 * This method should be called only if {@link EPICSPlug#PROPERTY_MAX_THREADS} is
	 * a number greater than 0. 
	 * <p>
	 * If {@link EPICSPlug#PROPERTY_USE_COMMON_EXECUTOR} is set to <code>true</code> the 
	 * <code>Executor</code> from {@link EPICSPlug#getExecutor()} is returned. Otherwise
	 * a new </code>ThreadPoolExecutor</code> is created.
	 * </p>
	 * 
	 * @return the executor
	 * @throws IllegalStateException if maximum number of threads defined by {@link EPICSPlug}
	 * is equal to 0.
	 */
	public ThreadPoolExecutor getExecutor() {
		if (executor==null) {
			synchronized (this) {
				if (getPlug().getMaxThreads() == 0) throw new IllegalStateException("Maximum number of threads must be greater than 0.");
				if (getPlug().isUseCommonExecutor()) executor = getPlug().getExecutor();
				else {
					executor= new ThreadPoolExecutor(getPlug().getCoreThreads(),getPlug().getMaxThreads(),Long.MAX_VALUE, TimeUnit.NANOSECONDS,
			                new LinkedBlockingQueue<Runnable>());
					executor.prestartAllCoreThreads();
				}		
				executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {

					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
//						plug.getLogger().warn("ThreadPoolExecutor has rejected the execution of a runnable.");
					}
				});
			}
		}
		return executor;
	}
	
	private static boolean equal(String s1, String s2) {
		if (s1 == null || s2 == null) {
			if (s1 == s2) {
				return true;
			}
			return false;
		}
		return s1.equals(s2);
	}
	
	private boolean fallbackInProgress = false;
	private GetListener fallbackListener = new GetListener() {

		public void getCompleted(GetEvent ev) {
			DBR dbr = ev.getDBR();
			if (dbr == null) return;
			
			createCharacteristics(dbr);
			T defaultValue = PlugUtilities.defaultValue(dataType);
			
			if (isMonitorListCreated()) {
				synchronized (getMonitors()) {
					for (MonitorProxyImpl<T> monitor : getMonitors()) {
						monitor.addFallbackResponse(defaultValue);
					}
				}
			}
			
			fallbackInProgress = false;
		}
	};

	protected void recoverFromNullDbr() {
		synchronized (fallbackListener) {
			if (fallbackInProgress)	return;
			fallbackInProgress = true;
		}
		getExecutor().execute(new Runnable() {
			public void run() {
		
				try {
					getChannel().get(DBRType.CTRL_STRING, 1, fallbackListener);
					plug.flushIO();
				} catch (CAException e) {
//					plug.getLogger().warn("Recovery from null DBR failed.", e);
				}
				
			}
		});
	}
	
	@Override
	protected String getRemoteHostInfo() {
		if (channel!=null) {
			return channel.getHostName();
		}
		return super.getRemoteHostInfo();
	}
	
}
