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
package org.csstudio.alarm.table.jms;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.csstudio.alarm.table.JmsLogsPlugin;
import org.csstudio.alarm.table.dataModel.BasicMessage;
import org.csstudio.alarm.table.dataModel.MessageList;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.utility.jms.sharedconnection.IMessageListenerSession;
import org.csstudio.platform.utility.jms.sharedconnection.SharedJmsConnections;

/**
 * This class handles the receiver jms connection (via the shared jsm connection
 * in css platform) New messages were added to the model (_messageList).
 * 
 * @author jhatje
 * 
 */
public class JmsMessageReceiver implements MessageListener {

    /**
     * List of messages displayed in the table.
     */
    MessageList _messageList;

    /**
     * JMS Session for the listener
     */
    IMessageListenerSession _listenerSession;

    /**
     * A new message is received. Add it to the model.
     */
    public void onMessage(final Message message) {
        if (message == null) {
            JmsLogsPlugin.logError("Message == null");
        }
        try {
            if (message instanceof TextMessage) {
                JmsLogsPlugin.logError("received message is not a map message");
            } else if (message instanceof MapMessage) {
                final MapMessage mm = (MapMessage) message;
                CentralLogger.getInstance().debug(
                        this,
                        "Received map message: EVENTTIME: "
                                + mm.getString("EVENTTIME")
                                + " NAME: " + mm.getString("NAME")
                                + " ACK: " + mm.getString("ACK"));
                Map<String, String> messageProperties = readMapMessageProperties(mm);
                _messageList.addMessage(new BasicMessage(messageProperties));
            } else {
                JmsLogsPlugin.logError("received message is an unknown type");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JmsLogsPlugin.logException("JMS error: ", e); //$NON-NLS-1$
        }
    }

    /**
     * Read properties from a {@link MapMessage} and put them into a Map.
     * 
     * @param mm
     * @return message properties in a Map
     * @throws JMSException 
     */
    private Map<String, String> readMapMessageProperties(MapMessage mm) throws JMSException {
        Map<String, String> messageProperties = new HashMap<String, String>();
        Enumeration<String> mapNames = mm.getMapNames();
        while (mapNames.hasMoreElements()) {
            String key = (String) mapNames.nextElement();
            messageProperties.put(key.toUpperCase(), mm.getString(key));
        }
        return messageProperties;
    }

    /**
     * Start jms message listener. If there is a previous session active (the
     * user has edited the topics) it will be closed and a new session is
     * created.
     * 
     * @param _deafultTopicSet
     *            JMS topics to be monitored
     */
    public void initializeJMSConnection(List<String> list, MessageList messageList) {
        _messageList = messageList;
        String[] topicList = null;
        if ((list == null) || (list.size() == 0)) {
            CentralLogger.getInstance().error(this,
                    "Could not initialize JMS Listener. JMS topics == NULL!");
        } else {
            topicList = list.toArray(new String[0]);
        }
        try {
            if ((_listenerSession != null) && (_listenerSession.isActive())) {
                _listenerSession.close();
                _listenerSession = null;
            }
            _listenerSession = SharedJmsConnections.startMessageListener(this,
                    topicList, Session.AUTO_ACKNOWLEDGE);
            CentralLogger.getInstance()
                    .info(
                            this,
                            "Initialize JMS connection with topics: "
                                    + list);
        } catch (JMSException e) {
            CentralLogger.getInstance().error(this,
                    "JMS Connection error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            CentralLogger.getInstance().error(
                    this,
                    "JMS Connection error, invalid arguments: "
                            + e.getMessage());
        }
    }

    /**
     * Stop the jms connection.
     */
    public void stopJMSConnection() {
        if (_listenerSession != null) {
            _listenerSession.close();
        }
    }
}
