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

import java.beans.PropertyChangeEvent;
import java.util.Iterator;
import java.util.Map;

import org.epics.css.dal.DataExchangeException;
import org.epics.css.dal.DynamicValueCondition;
import org.epics.css.dal.DynamicValueEvent;
import org.epics.css.dal.DynamicValueListener;
import org.epics.css.dal.DynamicValueProperty;
import org.epics.css.dal.DynamicValueState;
import org.epics.css.dal.EventSystemListener;
import org.epics.css.dal.ExpertMonitor;
import org.epics.css.dal.RemoteException;
import org.epics.css.dal.Request;
import org.epics.css.dal.Response;
import org.epics.css.dal.ResponseEvent;
import org.epics.css.dal.ResponseListener;
import org.epics.css.dal.SimpleProperty;
import org.epics.css.dal.context.ConnectionEvent;
import org.epics.css.dal.context.ConnectionState;
import org.epics.css.dal.context.LinkListener;
import org.epics.css.dal.context.Linkable;
import org.epics.css.dal.context.PropertyContext;
import org.epics.css.dal.context.PropertyFamily;
import org.epics.css.dal.proxy.DirectoryProxy;
import org.epics.css.dal.proxy.MonitorProxy;
import org.epics.css.dal.proxy.PropertyProxy;
import org.epics.css.dal.proxy.Proxy;
import org.epics.css.dal.proxy.ProxyEvent;
import org.epics.css.dal.proxy.ProxyListener;
import org.epics.css.dal.simple.AnyData;
import org.epics.css.dal.simple.ChannelListener;
import org.epics.css.dal.simple.impl.ChannelListenerNotifier;
import org.epics.css.dal.simple.impl.DataUtil;

import com.cosylab.util.ListenerList;


/**
 * Glue code implementation of DynamicValueProperty.
 *
 * @author ikriznar
 *
 */
public class DynamicValuePropertyImpl<T> extends SimplePropertyImpl<T>
	implements DynamicValueProperty<T>
{
	protected final byte DVE_CONDITIONCHANGE = 0;
	protected final byte DVE_ERRORRESPONSE = 1;
	protected final byte DVE_TIMELAGSTARTS = 2;
	protected final byte DVE_TIMELAGSTOPS = 3;
	protected final byte DVE_TIMEOUTSTARTS = 4;
	protected final byte DVE_TIMEOUTSTOPS = 5;
	protected final byte DVE_VALUECHANGED = 6;
	protected final byte DVE_VALUEUPDATED = 7;

	protected PropertyContext propertyContext;
	protected ListenerList responseListeners = new ListenerList(ResponseListener.class);
	protected ResponseListener defaultResponseListener = new ResponseListener() {
			public void responseReceived(ResponseEvent event)
			{
				fireResponseReceived(event);
			}

			public void responseError(ResponseEvent event)
			{
				fireResponseError(event);
			}
		};
	protected ResponseListener<T> defaultValueResponseListener = new ResponseListener<T>() {
			public void responseReceived(ResponseEvent<T> event)
			{
				lastValueResponse = event.getResponse();
				fireResponseReceived(event);
			}

			public void responseError(ResponseEvent<T> event)
			{
				lastValueResponse = event.getResponse();
				fireResponseError(event);
			}
		};

	protected ListenerList linkListeners = new ListenerList(LinkListener.class);
	protected ProxyListener<T> proxyListener = new ProxyListener<T>() {
		public void connectionStateChange(ProxyEvent<Proxy> e) {
			setConnectionState(e.getConnectionState());
		}
		public void dynamicValueConditionChange(ProxyEvent<PropertyProxy<T>> e) {
			DynamicValueCondition oldCond = condition;
			condition = e.getCondition();
			checkAndFireConditionEvents(oldCond, condition);
		}
		public void characteristicsChange(PropertyChangeEvent e) {
			firePropertyChangeEvent(e);
		}
	};
	
	protected Request<?> lastRequest = null;
	protected Request<T> lastValueRequest = null;
	protected Response<?> lastResponse = null;
	protected Response<T> lastValueResponse = null;
	private int suspended = 0;
	protected ConnectionState connectionState = ConnectionState.INITIAL;

	private class ResponseForwarder<F extends Object> implements ResponseListener<F>
	{
		private boolean isValue=false;
		private ResponseListener<F> listener;

		/**
		         * Creates a new ResponseForwarder object.
		         *
		         * @param listener Response listener
		         */
		public ResponseForwarder(ResponseListener<F> listener)
		{
			this.listener = listener;
		}

		public ResponseForwarder(ResponseListener<F> listener, boolean value)
		{
			this.listener = listener;
			this.isValue=value;
		}

		/**
		 * Accepts responsReceived notifications
		 *
		 * @param event Response event
		 */
		public void responseReceived(ResponseEvent<F> event)
		{
			if (listener != null) {
				listener.responseReceived(event);
			}
			if (isValue) {
				lastValueResponse= (Response<T>)event.getResponse();
			}
			fireResponseReceived(event);
			if (event.isLast()) {
				listener=null;
			}
		}

		/**
		 * Accepts responseError notifications
		 *
		 * @param event Response event
		 */
		public void responseError(ResponseEvent<F> event)
		{
			if (listener != null) {
				listener.responseError(event);
			}
			if (isValue) {
				lastValueResponse= (Response<T>)event.getResponse();
			}
			fireResponseError(event);
			if (event.isLast()) {
				listener=null;
			}
		}
	}

	/**
	     * @param valClass
	     */
	public DynamicValuePropertyImpl(Class<T> valClass, String name,
	    PropertyContext propertyContext)
	{
		super(valClass, name);
		this.propertyContext = propertyContext;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.DynamicValueProperty#getParentContext()
	 */
	public PropertyContext getParentContext()
	{
		return propertyContext;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousAccess#getAsynchronous()
	 */
	public Request<T> getAsynchronous() throws DataExchangeException
	{
		return getAsynchronous(null);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousAccess#getAsynchronous(org.epics.css.dal.ResponseListener)
	 */
	public Request<T> getAsynchronous(ResponseListener<T> listener)
		throws DataExchangeException
	{
		if (proxy == null || proxy.getConnectionState() != ConnectionState.CONNECTED) {
			throw new DataExchangeException(this, "Proxy not connected");
		}
		
		    lastValueRequest = proxy.getValueAsync(listener == null
		            ? defaultValueResponseListener : new ResponseForwarder(listener,true));
		
		//lastValueRequest = proxy.getValueAsync(new ResponseForwarder(listener));
		lastRequest = lastValueRequest;

		return lastValueRequest;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousAccess#setAsynchronous(T)
	 */
	public Request<T> setAsynchronous(T value) throws DataExchangeException
	{
		return setAsynchronous(value, null);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousAccess#setAsynchronous(T, org.epics.css.dal.ResponseListener)
	 */
	public Request<T> setAsynchronous(T value, ResponseListener<T> listener)
		throws DataExchangeException
	{
		if (proxy == null || proxy.getConnectionState() != ConnectionState.CONNECTED) {
			throw new DataExchangeException(this, "Proxy not connected");
		}
		
		lastValueRequest = proxy.setValueAsync(value,
			    listener == null ? defaultResponseListener
			    : new ResponseForwarder(listener));
		lastRequest = lastValueRequest;

		return lastValueRequest;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousContext#addResponseListener(org.epics.css.dal.ResponseListener)
	 */
	public void addResponseListener(ResponseListener<?> l)
	{
		responseListeners.add(l);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousContext#removeResponseListener(org.epics.css.dal.ResponseListener)
	 */
	public void removeResponseListener(ResponseListener<?> l)
	{
		responseListeners.remove(l);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousContext#getResponseListeners()
	 */
	public ResponseListener<?>[] getResponseListeners()
	{
		return (ResponseListener<?>[])responseListeners.toArray(new ResponseListener[responseListeners
		    .size()]);
	}

	protected void fireResponseReceived(ResponseEvent event)
	{
		lastResponse = event.getResponse();

		Iterator<ResponseListener<?>> ite = responseListeners.iterator();

		while (ite.hasNext()) {
			ite.next().responseReceived(event);
		}
	}

	protected void fireResponseError(ResponseEvent event)
	{
		lastResponse = event.getResponse();

		Iterator<ResponseListener<?>> ite = responseListeners.iterator();

		while (ite.hasNext()) {
			ite.next().responseError(event);
		}
	}

	void updateLastValueCache(Response<T> r, boolean success, boolean change)
	{
		lastValueResponse = r;
		updateLastValueCache(r.getValue(), r.getTimestamp(), success, change);
	}

	/*void setLastResponse(Response<?> r)
	{
		lastResponse = r;
	}*/

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousContext#getLatestRequest()
	 */
	public Request<?> getLatestRequest()
	{
		return lastRequest;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousContext#getLatestResponse()
	 */
	public Response<?> getLatestResponse()
	{
		return lastResponse;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousContext#getLatestSuccess()
	 */
	public boolean getLatestSuccess()
	{
		return lastResponse == null ? true : lastResponse.success();
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousCharacteristicContext#getCharacteristicsAsynchronously(java.lang.String[])
	 */
	public Request<? extends Object> getCharacteristicsAsynchronously(String[] names)
		throws DataExchangeException
	{
		lastRequest = directoryProxy.getCharacteristics(names,
			    defaultResponseListener);

		return lastRequest;
	}
	
	public Request<? extends Object> getCharacteristicsAsynchronously(
			String[] names, ResponseListener<? extends Object> listener)
			throws DataExchangeException {
		lastRequest = directoryProxy.getCharacteristics(names, 
				listener == null ? defaultResponseListener : new ResponseForwarder(listener));
		
		return lastRequest;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.AsynchronousCharacteristicContext#getCharacteristicAsynchronously(java.lang.String)
	 */
	public Request<? extends Object> getCharacteristicAsynchronously(String name)
		throws DataExchangeException
	{
		lastRequest = directoryProxy.getCharacteristics(new String[]{ name },
			    defaultResponseListener);

		return lastRequest;
	}
	
	public Request<?> getCharacteristicAsynchronously(
			String name, ResponseListener<?> listener)
			throws DataExchangeException {

		lastRequest = directoryProxy.getCharacteristics(new String[]{ name }, 
				listener == null ? defaultResponseListener : new ResponseForwarder(listener));
		
		return lastRequest;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Updateable#getLatestValueRequest()
	 */
	public Request<T> getLatestValueRequest()
	{
		return lastValueRequest;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.Updateable#getLatestValueResponse()
	 */
	public Response<T> getLatestValueResponse()
	{
		return lastValueResponse;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#addLinkListener(org.epics.css.dal.context.LinkListener)
	 */
	public void addLinkListener(LinkListener<? extends Linkable> l)
	{
		linkListeners.add(l);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#isConnected()
	 */
	public boolean isConnected()
	{
		if (connectionState == ConnectionState.CONNECTED
		    || connectionState == ConnectionState.CONNECTION_LOST) {
			return true;
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#isDestroyed()
	 */
	public boolean isDestroyed()
	{
		return connectionState == ConnectionState.DESTROYED;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#isSuspended()
	 */
	public boolean isSuspended()
	{
		return suspended > 0;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#isConnectionAlive()
	 */
	public boolean isConnectionAlive()
	{
		return connectionState == ConnectionState.CONNECTED;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#isConnectionFailed()
	 */
	public boolean isConnectionFailed()
	{
		return connectionState == ConnectionState.CONNECTION_FAILED;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#refresh()
	 */
	public void refresh() throws RemoteException
	{
		directoryProxy.refresh();
		if ((proxy != directoryProxy) && (proxy instanceof DirectoryProxy)) ((DirectoryProxy)proxy).refresh();
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#removeLinkListener(org.epics.css.dal.context.LinkListener)
	 */
	public void removeLinkListener(LinkListener<? extends Linkable> l)
	{
		linkListeners.remove(l);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#resume()
	 */
	public void resume() throws RemoteException
	{
		if (suspended > 0) {
			suspended--;
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Linkable#suspend()
	 */
	public void suspend() throws RemoteException
	{
		suspended++;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Identifiable#isDebug()
	 */
	public boolean isDebug()
	{
		return false;
	}

	/**
	 * @return Returns the connectionState.
	 */
	public ConnectionState getConnectionState()
	{
		return connectionState;
	}

	/**
	 * @param connectionState The connectionState to set.
	 */
	protected void setConnectionState(ConnectionState connectionState)
	{
		if (this.connectionState == connectionState) {
			return;
		}

		this.connectionState = connectionState;

		LinkListener<Linkable>[] l = (LinkListener<Linkable>[])linkListeners.toArray();

		ConnectionEvent<Linkable> e = new ConnectionEvent<Linkable>(this, connectionState);

		if (connectionState == ConnectionState.CONNECTED) {
			
			if (hasDynamicValueListeners() && defaultMonitor==null) {
				getDefaultMonitor();
			}
			
			for(MonitorProxyWrapper<T, SimpleProperty<T>> mpw : monitors) {
				if (!mpw.isInitialized()) {
					try {
						MonitorProxy mp = proxy.createMonitor(mpw,mpw.getInitialParameters());
						mpw.initialize(mp);
					} catch (RemoteException e1) {
						// TODO mark mpw for removal from monitors?
						// if yes, it has to be done outside loop
					}
				}
			}
			
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].connected(e);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}else if (connectionState == ConnectionState.CONNECTION_FAILED) {
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].connectionFailed(e);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}	else if (connectionState == ConnectionState.CONNECTION_LOST) {
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].connectionLost(e);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} else if (connectionState == ConnectionState.DISCONNECTED) {
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].disconnected(e);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} else if (connectionState == ConnectionState.DESTROYED) {
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].destroyed(e);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.impl.SimplePropertyImpl#initialize(org.epics.css.dal.proxy.PropertyProxy, org.epics.css.dal.proxy.DirectoryProxy)
	 */
	@Override
	public void initialize(PropertyProxy<T> proxy, DirectoryProxy dirProxy)
	{
		if (this.proxy != null) {
			this.proxy.removeProxyListener(proxyListener);
			if (this.directoryProxy != null && this.directoryProxy != this.proxy) {
				this.directoryProxy.removeProxyListener(proxyListener);
			}
		} 

		super.initialize(proxy, dirProxy);

		if (proxy != null) {
			proxy.addProxyListener(proxyListener);
			if (dirProxy != null && dirProxy != proxy) {
				dirProxy.addProxyListener(proxyListener);
			}
		}
	}

	protected void fireDynamicValueEvent(byte type)
	{
		if (!hasDynamicValueListeners()) {
			return;
		}
		
		String msg = null;
		switch (type) {
		case DVE_CONDITIONCHANGE:
			msg = "Condition changed";break;
		case DVE_ERRORRESPONSE:
			msg = "Error response";break;
		case DVE_TIMELAGSTARTS:
			msg = "Timelag started";break;
		case DVE_TIMELAGSTOPS:
			msg = "Timelag stopped";break;
		case DVE_TIMEOUTSTARTS:
			msg = "Timeout started";break;
		case DVE_TIMEOUTSTOPS:
			msg = "Timeout stopped";break;
		case DVE_VALUECHANGED:
			msg = "Value changed";break;
		case DVE_VALUEUPDATED:
			msg = "Value updated";break;
		default:
			return;
		}
		DynamicValueEvent<T, DynamicValueProperty<T>> dve = new DynamicValueEvent<T, DynamicValueProperty<T>>(this,
			    this, lastValue, condition, null, "Condition changed");
		DynamicValueListener<T, DynamicValueProperty<T>>[] listen = new DynamicValueListener[getDvListeners()
			.size()];
		listen = (DynamicValueListener<T, DynamicValueProperty<T>>[])getDvListeners().toArray(listen);

		switch (type) {
		case DVE_CONDITIONCHANGE:
			for (int i = 0; i < listen.length; i++) {
				listen[i].conditionChange(dve);
			}
			break;
		case DVE_ERRORRESPONSE:
			for (int i = 0; i < listen.length; i++) {
				listen[i].errorResponse(dve);
			}
			break;
		case DVE_TIMELAGSTARTS:
			for (int i = 0; i < listen.length; i++) {
				listen[i].timelagStarts(dve);
			}
			break;
		case DVE_TIMELAGSTOPS:
			for (int i = 0; i < listen.length; i++) {
				listen[i].timelagStops(dve);
			}
			break;
		case DVE_TIMEOUTSTARTS:
			for (int i = 0; i < listen.length; i++) {
				listen[i].timeoutStarts(dve);
			}
			break;
		case DVE_TIMEOUTSTOPS:
			for (int i = 0; i < listen.length; i++) {
				listen[i].timeoutStops(dve);
			}
			break;
		case DVE_VALUECHANGED:
			for (int i = 0; i < listen.length; i++) {
				listen[i].valueChanged(dve);
			}
			break;
		case DVE_VALUEUPDATED:
			for (int i = 0; i < listen.length; i++) {
				listen[i].valueUpdated(dve);
			}
			break;
		default:
			return;
		}
	}
	
	protected void checkAndFireConditionEvents(DynamicValueCondition oldCond, DynamicValueCondition newCond){
		fireDynamicValueEvent(DVE_CONDITIONCHANGE);
		
		if (newCond == null) return;
		if (oldCond == null){
			if (newCond.containsStates(DynamicValueState.TIMELAG))	
				fireDynamicValueEvent(DVE_TIMELAGSTARTS);
			if (newCond.containsStates(DynamicValueState.TIMEOUT))	
				fireDynamicValueEvent(DVE_TIMEOUTSTARTS);
			if (newCond.containsStates(DynamicValueState.ERROR))	
				fireDynamicValueEvent(DVE_ERRORRESPONSE);			
		} else {
			
			//timelag checks
			if (newCond.containsStates(DynamicValueState.TIMELAG) && !oldCond.containsStates(DynamicValueState.TIMELAG))	
				fireDynamicValueEvent(DVE_TIMELAGSTARTS);
			else if (oldCond.containsStates(DynamicValueState.TIMELAG) && !condition.containsStates(DynamicValueState.TIMELAG))	
				fireDynamicValueEvent(DVE_TIMELAGSTOPS);
			
			//timeout checks
			if (newCond.containsStates(DynamicValueState.TIMEOUT) && !oldCond.containsStates(DynamicValueState.TIMEOUT))	
				fireDynamicValueEvent(DVE_TIMEOUTSTARTS);
			else if (oldCond.containsStates(DynamicValueState.TIMEOUT) && !condition.containsStates(DynamicValueState.TIMEOUT))	
				fireDynamicValueEvent(DVE_TIMEOUTSTOPS);
			
			//error checks
			if (newCond.containsStates(DynamicValueState.ERROR) && !oldCond.containsStates(DynamicValueState.ERROR))	
				fireDynamicValueEvent(DVE_ERRORRESPONSE);
			
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.impl.SimplePropertyImpl#addDynamicValueListener(org.epics.css.dal.DynamicValueListener)
	 */
	@Override
	public <P extends SimpleProperty<T>> void addDynamicValueListener(DynamicValueListener<T, P> l)
	{
		super.addDynamicValueListener(l);
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.DynamicValueProperty#getSupportedExpertMonitorParameters()
	 */
	public Map<String, Object> getSupportedExpertMonitorParameters()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.DynamicValueProperty#createNewExpertMonitor(org.epics.css.dal.DynamicValueListener, java.util.Map)
	 */
	public <E extends SimpleProperty<T>, M extends ExpertMonitor,DynamicValueMonitor> M createNewExpertMonitor(DynamicValueListener<T, E> listener,
	    Map<String, Object> parameters) throws RemoteException
	{
//		if (proxy == null) throw new IllegalStateException("Proxy is null");
		
		MonitorProxyWrapper<T, E> mpw = new MonitorProxyWrapper<T, E>((E) this, listener, parameters);
		
		if (proxy != null && isConnected()) {
			MonitorProxy mp = null;
			mp = proxy.createMonitor(mpw,parameters);
			mpw.initialize(mp);
		}
		
		synchronized (monitors) {
			monitors.add(mpw);
		}

		return (M)mpw;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.EventSystemContext#addEventSystemListener(org.epics.css.dal.EventSystemListener, java.util.Map)
	 */
	public void addEventSystemListener(
	    EventSystemListener<DynamicValueEvent<T, SimpleProperty<T>>> l, Map<String, Object> parameters)
		throws RemoteException
	{
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.EventSystemContext#addEventSystemListener(org.epics.css.dal.EventSystemListener)
	 */
	public void addEventSystemListener(EventSystemListener<DynamicValueEvent<T, SimpleProperty<T>>> l)
		throws RemoteException
	{
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.EventSystemContext#removeEventSystemListener(org.epics.css.dal.EventSystemListener, java.util.Map)
	 */
	public void removeEventSystemListener(
	    EventSystemListener<DynamicValueEvent<T, SimpleProperty<T>>> l, Map<String, Object> parameters)
	{
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.EventSystemContext#removeEventSystemListener(org.epics.css.dal.EventSystemListener)
	 */
	public void removeEventSystemListener(
	    EventSystemListener<DynamicValueEvent<T, SimpleProperty<T>>> l)
	{
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.EventSystemContext#getEventSystemListeners()
	 */
	public EventSystemListener<DynamicValueEvent<T, SimpleProperty<T>>>[] getEventSystemListeners()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.EventSystemContext#getSupportedEventSystemParameters()
	 */
	public Map<String, Object> getSupportedEventSystemParameters()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public AnyData getData() {
		return DataUtil.createAnyData(this);
	}
	
	public void setValueAsObject(Object new_value) throws RemoteException {
		setValue(DataUtil.castTo(new_value, getDataType()));
	}

	public String getStateInfo() {
		return getConnectionState().toString();
	}

	public boolean isRunning() {
		return this.isConnectionAlive();
	}

	public boolean isWriteAllowed() {
		return this.isSettable();
	}

	public void start() throws Exception {
		// TODO at the moment this method does nothing
		
	}

	public void startSync() throws Exception {
		// TODO at the moment this method does nothing
		
	}

	/* (non-Javadoc)
	 * @see org.epics.css.dal.simple.AnyDataChannel#getProperty()
	 */
	public DynamicValueProperty<?> getProperty() {
		return this;
	}

	public void stop() {
		setConnectionState(ConnectionState.DISCONNECTING);
		((PropertyFamily)propertyContext).destroy(this);
		releaseProxy(true);
		setConnectionState(ConnectionState.DISCONNECTED);
		setConnectionState(ConnectionState.DESTROYED);
	}
	
	@Override
	public Proxy[] releaseProxy(boolean destroy) {
		if (destroy) {
			setConnectionState(ConnectionState.DESTROYED);
			linkListeners.clear();
			responseListeners.clear();
		} else {
			setConnectionState(ConnectionState.DISCONNECTING);
		}
		if (this.proxy != null) {
			this.proxy.removeProxyListener(proxyListener);
			if (this.directoryProxy != null && this.directoryProxy != this.proxy) {
				this.directoryProxy.removeProxyListener(proxyListener);
			}
		}
		Proxy[] p= super.releaseProxy(destroy);
		if (!destroy) {
			setConnectionState(ConnectionState.DISCONNECTED);
		}
		return p;
	}

} /* __oOo__ */


/* __oOo__ */
