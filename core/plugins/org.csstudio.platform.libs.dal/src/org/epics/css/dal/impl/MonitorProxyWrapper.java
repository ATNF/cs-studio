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

package org.epics.css.dal.impl;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import org.epics.css.dal.DataExchangeException;
import org.epics.css.dal.DynamicValueCondition;
import org.epics.css.dal.DynamicValueEvent;
import org.epics.css.dal.DynamicValueListener;
import org.epics.css.dal.DynamicValueMonitor;
import org.epics.css.dal.ExpertMonitor;
import org.epics.css.dal.RemoteException;
import org.epics.css.dal.Response;
import org.epics.css.dal.ResponseEvent;
import org.epics.css.dal.ResponseListener;
import org.epics.css.dal.SimpleProperty;
import org.epics.css.dal.Suspendable;
import org.epics.css.dal.proxy.MonitorProxy;

import com.cosylab.util.ListenerList;


// TODO type safety
/**
 * Wrapper clas for <code>MonitorProxy</code>.
 *
 */
public class MonitorProxyWrapper<T, P extends SimpleProperty<T>> implements ResponseListener<T>,
	ExpertMonitor, DynamicValueMonitor, Suspendable
{
	private DynamicValueListener<T, P> dvl;
	private ListenerList dvls;
	private MonitorProxy proxy;
	private ListenerList plistners = new ListenerList(PropertyChangeListener.class);
	private P property;
	private Object lastValue = null;
	private DynamicValueCondition lastCondition;
	private int suspendCount;

	/**
	 * Creates a new MonitorProxyWrapper object.
	 *
	 * @param property paretn property
	 * @param listener listener to events
	 */
	public MonitorProxyWrapper(P property,
	    DynamicValueListener<T,P> listener)
	{
		dvl = listener;
		this.property = property;
	}

	/**
	 * Creates a new MonitorProxyWrapper object.
	 *
	 * @param property paretn property
	 * @param listeners listeners to events
	 */
	public MonitorProxyWrapper(P property, ListenerList listeners)
	{
		dvls = listeners;
		this.property = property;
	}

	/**
	 * Initializes the proxy wrapper
	 *
	 * @param p Monitor proxy
	 */
	public void initialize(MonitorProxy p)
	{
		proxy = p;
	}

	/**
	 * Returns <code>true</code> if this <code>MonitorProxyWrapper</code> is
	 * initialized.
	 * @return <code>true</code> if already initialized and <code>false</code>
	 * otherwise
	 */
	public boolean isInitialized() {
		return proxy != null;
	}
	
	private final static String ERROR_MESSAGE = "Error response";

	/* (non-Javadoc)
	 * @see org.epics.css.dal.ResponseListener#responseError(org.epics.css.dal.ResponseEvent)
	 */
	public void responseError(ResponseEvent<T> event)
	{
		Response<T> response = event.getResponse();
		if (property instanceof DynamicValuePropertyImpl) {
			((DynamicValuePropertyImpl<T>)property).updateLastValueCache(response, false, false);
		} else if (property instanceof SimplePropertyImpl) {
			((SimplePropertyImpl<T>)property).updateLastValueCache(null, response.getTimestamp(), false, false);
		}

		lastCondition = response.getCondition();

		// TODO better message
		DynamicValueEvent<T, P> dynamicEvent = new DynamicValueEvent<T,P>(
				property,property, response.getValue(), lastCondition,
			    response.getTimestamp(), ERROR_MESSAGE, response.getError());

		if (dvl != null) {
			dvl.errorResponse(dynamicEvent);
		} else if (dvls != null) {
			DynamicValueListener<T,P>[] dvlArray = (DynamicValueListener<T,P>[])dvls
				.toArray();

			for (int i = 0; i < dvlArray.length; i++) {
				dvlArray[i].errorResponse(dynamicEvent);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.ResponseListener#responseReceived(org.epics.css.dal.ResponseEvent)
	 */
	public void responseReceived(ResponseEvent<T> event)
	{
		Response<T> response = event.getResponse();
		T value = response.getValue();

		// TODO can value be null?
		boolean valueChanged = !value.equals(lastValue);
		lastValue = value;

		if (property instanceof DynamicValuePropertyImpl) {
			((DynamicValuePropertyImpl<T>)property).updateLastValueCache(response, true, valueChanged);
		} else if (property instanceof SimplePropertyImpl) {
			((SimplePropertyImpl<T>)property).updateLastValueCache(value, response.getTimestamp(), true, valueChanged);
		}



		lastCondition = response.getCondition();

		// TODO better message
		DynamicValueEvent<T,P> dynamicEvent = new DynamicValueEvent<T,P>(property,
			    property, value, lastCondition, response.getTimestamp(), null);

		if (dvl != null) {
			if (valueChanged) {
				dvl.valueChanged(dynamicEvent);
			} else {
				dvl.valueUpdated(dynamicEvent);
			}
		} else if (dvls != null) {
			DynamicValueListener<T,P>[] dvlArray = (DynamicValueListener<T,P>[])dvls
				.toArray();

			for (int i = 0; i < dvlArray.length; i++) {
				if (valueChanged) {
					dvlArray[i].valueChanged(dynamicEvent);
				} else {
					dvlArray[i].valueUpdated(dynamicEvent);
				}
			}
		}
	}

	/**
	 * Returns <code>true</code> if there is a timelag from actual event delivered
	 *
	 * @return <code>true</code> if timelag
	 */
	public boolean isTimelag()
	{
		return lastCondition != null ? lastCondition.isTimelag() : false;
	}

	/**
	 * Returns <code>true</code> if there is a timeout from actual event delivered
	 *
	 * @return <code>true</code> if timelag
	 */
	public boolean isTimeout()
	{
		return lastCondition != null ? lastCondition.isTimeout() : false;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.DynamicValueMonitor#getDefaultTimerTrigger()
	 */
	public long getDefaultTimerTrigger() throws DataExchangeException
	{
		return proxy.getDefaultTimerTrigger();
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.DynamicValueMonitor#getTimerTrigger()
	 */
	public long getTimerTrigger() throws DataExchangeException
	{
		return proxy.getTimerTrigger();
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.DynamicValueMonitor#isDefault()
	 */
	public boolean isDefault()
	{
		return proxy.isDefault();
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.DynamicValueMonitor#isHeartbeat()
	 */
	public boolean isHeartbeat()
	{
		return proxy.isHeartbeat();
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.DynamicValueMonitor#setHeartbeat(boolean)
	 */
	public void setHeartbeat(boolean heartbeat)
		throws DataExchangeException, UnsupportedOperationException
	{
		proxy.setHeartbeat(heartbeat);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.DynamicValueMonitor#setTimerTrigger(long)
	 */
	public void setTimerTrigger(long trigger)
		throws DataExchangeException, UnsupportedOperationException
	{
		proxy.setTimerTrigger(trigger);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.CharacteristicContext#addPropertyChangeListener(java.beans.PropertyChangeListener)
	 */
	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		plistners.add(l);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.CharacteristicContext#getCharacteristic(java.lang.String)
	 */
	public Object getCharacteristic(String name) throws DataExchangeException
	{
		if (C_DEFAULT_TIMER_TRIGGER.equals(name)) {
			return proxy.getDefaultTimerTrigger();
		}

		if (C_HEARTBEAT.equals(name)) {
			return proxy.isHeartbeat();
		}

		if (C_TIMER_TRIGGER.equals(name)) {
			return proxy.getTimerTrigger();
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.CharacteristicContext#getCharacteristicNames()
	 */
	public String[] getCharacteristicNames() throws DataExchangeException
	{
		return new String[]{
			C_DEFAULT_TIMER_TRIGGER, C_HEARTBEAT, C_TIMER_TRIGGER
		};
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.CharacteristicContext#getCharacteristics(java.lang.String[])
	 */
	public Map<String, Object> getCharacteristics(String[] names) throws DataExchangeException
	{
		Map<String, Object> m = new HashMap<String, Object>(names.length);

		for (int i = 0; i < names.length; i++) {
			m.put(names[i], getCharacteristic(names[i]));
		}

		return m;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.CharacteristicContext#getPropertyChangeListeners()
	 */
	public PropertyChangeListener[] getPropertyChangeListeners()
	{
		return (PropertyChangeListener[])plistners.toArray(new PropertyChangeListener[plistners
		    .size()]);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.CharacteristicContext#removePropertyChangeListener(java.beans.PropertyChangeListener)
	 */
	public void removePropertyChangeListener(PropertyChangeListener l)
	{
		plistners.remove(l);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.SimpleMonitor#destroy()
	 */
	public void destroy()
	{
		// TODO: if this is default monitor for property, throw exception
		// TODO: ike: so what
		///if (dvls != null) {
		//	return;
		//}

		((SimplePropertyImpl)property).removeMonitor(this);
		if (proxy != null) {
			proxy.destroy();
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.SimpleMonitor#isDestroyed()
	 */
	public boolean isDestroyed()
	{
		return proxy.isDestroyed();
	}

	public void suspend() {
		if (!(proxy instanceof Suspendable)) throw new UnsupportedOperationException("Monitor proxy doesn't support suspend/resume operations!");
		if (suspendCount == 0) ((Suspendable)proxy).suspend();
		suspendCount++;
	}

	public void resume() {
		if (!(proxy instanceof Suspendable)) throw new UnsupportedOperationException("Monitor proxy doesn't support suspend/resume operations!");
		if (suspendCount == 1) ((Suspendable)proxy).resume();
		if (suspendCount > 0) suspendCount--;;
	}

	public boolean isSuspended() {
		if (!(proxy instanceof Suspendable)) throw new UnsupportedOperationException("Monitor proxy doesn't support suspend/resume operations!");
		return suspendCount > 0;
	}

	public MonitorProxy releaseProxy(boolean destroy) {
		if (destroy) {
			destroy();
			dvls=null;
			dvl=null;
			plistners.clear();
		}
		MonitorProxy p= proxy;
		if (proxy != null) {
			proxy.destroy();
		}
		return p;
	}
	
	public Map<String, Object> getParameters() {
		if (proxy instanceof ExpertMonitor) {
			return ((ExpertMonitor)proxy).getParameters();
		}
		return null;
	}
	
	public void setParameters(Map<String, Object> param) throws RemoteException {
		if (proxy instanceof ExpertMonitor) {
			((ExpertMonitor)proxy).setParameters(param);
		}
	}

}

/* __oOo__ */
