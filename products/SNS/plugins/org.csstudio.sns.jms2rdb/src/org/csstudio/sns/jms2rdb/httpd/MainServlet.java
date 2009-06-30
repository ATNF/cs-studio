package org.csstudio.sns.jms2rdb.httpd;

import java.util.Dictionary;
import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.MapMessage;

import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.sns.jms2rdb.Activator;
import org.csstudio.sns.jms2rdb.LogClientThread;
import org.osgi.framework.Constants;

/** Servlet to display overall status of JMS Log Tool.
 *  @author Kay Kasemir
 *  reviewed by Katia Danilova 08/20/08
 */
@SuppressWarnings("nls")
public class MainServlet extends AbstractServlet
{
    /** Required by Serialize interface */
    private static final long serialVersionUID = 1L;
    
    final private LogClientThread log_client_thread;

    private final String app_name;

    @SuppressWarnings("unchecked")
    public MainServlet(final LogClientThread log_client_thread)
    {
        this.log_client_thread = log_client_thread;
        
        final Dictionary<String, String> headers =
            Activator.getInstance().getBundle().getHeaders();
        app_name = headers.get(Constants.BUNDLE_NAME) + " " +
                   headers.get(Constants.BUNDLE_VERSION);
    }
    
    /** Create status page */
    @SuppressWarnings("unchecked")
	@Override
    protected void fillBody(final HTMLWriter html)
    {
        html.h1(app_name);
        
        html.h2("Message Count: " + log_client_thread.getMessageCount());
        
        html.openTable(1, new String [] { "Last Error" });
        html.tableLine(new String[] { log_client_thread.getLastError() });
        html.closeTable();

        final MapMessage map = log_client_thread.getLastMessage();
        if (map == null)
            return;
        
        html.openTable(2, new String [] { "Last JMS Message" });
		try
		{	// Dump all properties of last message
	        Enumeration<String> props;
			props = map.getMapNames();
	        while (props.hasMoreElements())
	        {
	        	final String prop = props.nextElement();
		        html.tableLine(new String []
		        {
		        		prop, map.getString(prop)
		        });
	        }
		}
		catch (JMSException e)
		{
			CentralLogger.getInstance().getLogger(this).warn(e);
	        html.tableLine(new String []
	        {
        		"Error", e.getMessage()
	        });
		}
        html.closeTable();
    }
}
