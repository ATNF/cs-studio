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

package org.epics.css.dal.proxy;

import org.epics.css.dal.DataExchangeException;
import org.epics.css.dal.DynamicValueCondition;
import org.epics.css.dal.RemoteException;
import org.epics.css.dal.Request;
import org.epics.css.dal.ResponseListener;
import org.epics.css.dal.simple.RemoteInfo;


/**
 * Interface encapsulate communication to remote property object.
 *
 * @author Blaz Hostnik
 */
public interface PropertyProxy<T> extends Proxy
{
    /**
     * Asynchronously gets remote property value.
     *
     * @param callback The callback that receives the response
     *
     * @return a Request, which identifies incoming responses.
     *
     * @throws DataExchangeException if remote operation fails
     */
    public Request<T> getValueAsync(ResponseListener<T> callback)
    throws DataExchangeException;
    
    /**
     * Asynchronously sets value on remote property.
     *
     * @param value new value.
     * @param callback The callback that receives the response.
     *
     * @return a Request, which identifies incoming responses.
     *
     * @throws DataExchangeException if remote operation fails
     */
    public Request<T> setValueAsync(T value, ResponseListener<T> callback)
    throws DataExchangeException;
    
    /**
     * Returns whether the value can be set on remote object presented
     * by this proxy.
     *
     * @return <code>true</code> if value can be set.
     */
    public boolean isSettable();
    
    /**
     * Creates new value subscription and returns monitor proxy, which
     * controls the subscription.
     *
     * @param callback The callback that receives the response.
     * @param changeType TODO
     *
     * @return monitor proxy, which controls the subscription.
     *
     * @throws RemoteException if operation fails
     */
    public MonitorProxy createMonitor(ResponseListener<T> callback, RemoteInfo.ChangeType changeType) throws RemoteException;
    
    /**
     * Returns remote condition of this dynamic value representation.
     *
     * @return remote condition
     */
    public DynamicValueCondition getCondition();
}

/* __oOo__ */
