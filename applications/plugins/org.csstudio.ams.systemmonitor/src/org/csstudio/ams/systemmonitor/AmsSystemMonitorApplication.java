
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
 *
 */

package org.csstudio.ams.systemmonitor;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.log4j.Logger;
import org.csstudio.ams.systemmonitor.check.AmsSystemCheck;
import org.csstudio.ams.systemmonitor.check.CheckResult;
import org.csstudio.ams.systemmonitor.check.SmsConnectorCheck;
import org.csstudio.ams.systemmonitor.database.DatabaseHelper;
import org.csstudio.ams.systemmonitor.internal.PreferenceKeys;
import org.csstudio.ams.systemmonitor.status.MonitorStatusHandler;
import org.csstudio.ams.systemmonitor.util.CommonMailer;
import org.csstudio.platform.logging.CentralLogger;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * @author Markus Moeller
 *
 */
public class AmsSystemMonitorApplication implements IApplication
{
    /** Status handler for the AMS system check */
    private MonitorStatusHandler amsStatusHandler;
    
    /** Status handler for the SmsConnector check */
    private MonitorStatusHandler modemStatusHandler;

    /** Status handler for the AmsSystemMonitor */
    private MonitorStatusHandler monitorStatusHandler;

    /** Class that does the check of the AMS */
    private AmsSystemCheck amsSystemCheck;

    /** Class that does the check of the SMS connector */
    private SmsConnectorCheck smsConnectorCheck;

    /** Simple version information */
    private VersionInfo version;
    
    /** A nice logger */
    private Logger logger;
    
    private int allowedTimeout;
    
    /** Flag that indicates whether or not the application should run */
    private boolean running;

    public AmsSystemMonitorApplication()
    {
        logger = CentralLogger.getInstance().getLogger(this);

        // Retrieve the check interval
        IPreferencesService pref = Platform.getPreferencesService();

        allowedTimeout = pref.getInt(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_ALLOWED_TIMEOUT_COUNT, 2, null);
        logger.info("Number of allowed timeouts: " + allowedTimeout);
        
        monitorStatusHandler = new MonitorStatusHandler("AmsMonitor Status", "amsMonitorStatus.ser", allowedTimeout);
        amsStatusHandler = new MonitorStatusHandler("AMS Status", "amsStatus.ser", allowedTimeout);
        
        long checkInterval = pref.getLong(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_SMS_CHECK_INTERVAL, -1, null);
        if(checkInterval > 0)
        {
            // Assume minutes and convert it to ms
            checkInterval *= 60000;
        }
        else
        {
            logger.warn("Modem check interval '" + checkInterval + "' is invalid. Using default: 20 minutes");
            checkInterval = 1200000;
        }
        
        modemStatusHandler = new MonitorStatusHandler("SmsConnector Status", "modemStatus.ser", checkInterval, allowedTimeout);

        version = new VersionInfo();
        running = true;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
     */
    public Object start(IApplicationContext context) throws Exception
    {
        logger.info("AmsSystemMonitor started [" + version.toString() + "]");
        
        monitorStatusHandler.beginCurrentCheck();
        try
        {
            amsSystemCheck = new AmsSystemCheck("AmsSystemCheckSender", "AmsSystemCheckReceiver", "AmsSystemCheck");
            smsConnectorCheck = new SmsConnectorCheck("AmsSmsConnectorSender", "AmsSmsConnectorReceiver", "SmsConnectorCheck");
            
            monitorStatusHandler.setSmsSent(false);
            monitorStatusHandler.setCurrentStatus(CheckResult.OK);
            if((monitorStatusHandler.getPreviousStatus() == CheckResult.ERROR) && (monitorStatusHandler.isPriviousSmsSent() == true))
            {
                sendErrorSms("AmsSystemMonitor switched from " + CheckResult.ERROR + " to OK.");
            }

            monitorStatusHandler.resetErrorFlag();
        }
        catch(AmsSystemMonitorException asme)
        {
            logger.error("[*** AmsSystemMonitorException ***]: " + asme.getMessage());
            monitorStatusHandler.setCurrentStatus(CheckResult.ERROR);
            if(monitorStatusHandler.sendErrorSms())
            {
                sendErrorSms("AmsSystemMonitor could not initialize JMS. HOWTO: http://elogbook.desy.de:8080/eLogbook/HowTo?ACTION=VIEW&VALUE=64");
                monitorStatusHandler.setSmsSent(true);
            }
            
            monitorStatusHandler.stopCurrentCheck();
            return IApplication.EXIT_OK;
        }
        
        monitorStatusHandler.stopCurrentCheck();
        
        while(running)
        {
            this.checkSystem();
            
            // If the first check was not successful, we need not to do the second check
            if(amsStatusHandler.getCurrentStatus() != CheckResult.OK)
            {
                // Force a modem check
                modemStatusHandler.forceNextCheck();
                modemStatusHandler.storeStatus();
                
                break;
            }
            
            this.checkSmsConnector();
            
            // Just one time
            running = false;
        }
        
        logger.info("AmsSystemMonitor is stopping...");

        return IApplication.EXIT_OK;
    }

    public void checkSystem()
    {
        // First check: Message to topic ALARM produces a message to topic T_AMS_SYSTEM_MONITOR
        // Checks the DecisionDepartment, MessageMinder, Distributor
        // This check will _always_ be done!!!!
        if(amsStatusHandler.doNextCheck())
        {
            try
            {
                // Create a new current check status with the current timestamp and
                // a copy of the status flags.
                monitorStatusHandler.beginCurrentCheck();
                amsStatusHandler.beginCurrentCheck();
                
                amsSystemCheck.doCheck(amsStatusHandler.getCurrentStatusEntry());
                logger.info("AMS alarm chain is working.");
    
                amsStatusHandler.setCurrentStatus(CheckResult.OK);
                amsStatusHandler.setSmsSent(false);
                
                if(((amsStatusHandler.getPreviousStatus() == CheckResult.ERROR)
                 || (amsStatusHandler.getPreviousStatus() == CheckResult.TIMEOUT))
                 && (amsStatusHandler.isPriviousSmsSent() == true))
                {
                    sendErrorSms("AMS switched from " + CheckResult.ERROR + " to OK.");
                }
                
                amsStatusHandler.resetErrorFlag();
                
                monitorStatusHandler.setCurrentStatus(CheckResult.OK);
                monitorStatusHandler.setSmsSent(false);
                if((monitorStatusHandler.getPreviousStatus() == CheckResult.ERROR) && (monitorStatusHandler.isPriviousSmsSent() == true))
                {
                    sendErrorSms("AmsSystemMonitor switched from " + CheckResult.ERROR + " to OK.");
                }
                
                monitorStatusHandler.resetErrorFlag();
            }
            catch(AmsSystemMonitorException asme)
            {
                if(asme.getErrorCode() == AmsSystemMonitorException.ERROR_CODE_TIMEOUT)
                {
                    logger.warn("Timeout!");
    
                    // Set current status TIMEOUT
                    amsStatusHandler.setCurrentStatus(CheckResult.TIMEOUT);
                    
                    logger.info("Number of timeouts: " + amsStatusHandler.getNumberOfTimeouts());
                    if(amsStatusHandler.getNumberOfTimeouts() > allowedTimeout)
                    {
                        amsStatusHandler.setCurrentStatus(CheckResult.ERROR);
                        amsStatusHandler.setErrorStatusSet(true);
                    }
                    
                    if(amsStatusHandler.sendErrorSms())
                    {
                        sendErrorSms("AMS does NOT respond to the current check. HOWTO: http://elogbook.desy.de:8080/eLogbook/HowTo?ACTION=VIEW&VALUE=64");
                        amsStatusHandler.setSmsSent(true);
                    }
                    else
                    {
                        logger.info("AmsSystemMonitor does not send a SMS yet.");
                    }
                    
                    // No effect here because the check of the AMS system will _always_ be done!
                    amsStatusHandler.forceNextCheck();
                }
                else if(asme.getErrorCode() == AmsSystemMonitorException.ERROR_CODE_SYSTEM_MONITOR)
                {
                    logger.warn("AmsSystemMonitor does not work properly.");
    
                    monitorStatusHandler.setCurrentStatus(CheckResult.ERROR);
                    if(monitorStatusHandler.sendErrorSms())
                    {
                        sendErrorSms("AmsSystemMonitor could not send JMS check message. HOWTO: http://elogbook.desy.de:8080/eLogbook/HowTo?ACTION=VIEW&VALUE=64");
                        monitorStatusHandler.setSmsSent(true);
                    }
                    else
                    {
                        logger.info("AmsSystemMonitor does not send a SMS yet.");
                    }
                    
                    // No effect here
                    monitorStatusHandler.forceNextCheck();
                }
            }
        
            amsStatusHandler.stopCurrentCheck();
            monitorStatusHandler.stopCurrentCheck();
        }
    }

    public void checkSmsConnector()
    {
        if(modemStatusHandler.doNextCheck())
        {
            try
            {
                // Second check: Message to topic ALARM produces a message for the SmsConnector
                // Checks the modems the SmsConnector uses
                monitorStatusHandler.beginCurrentCheck();
                modemStatusHandler.beginCurrentCheck();
                
                smsConnectorCheck.doCheck(modemStatusHandler.getCurrentStatusEntry());
                
                modemStatusHandler.setCurrentStatus(CheckResult.OK);
                modemStatusHandler.setSmsSent(false);
                
                if(((modemStatusHandler.getPreviousStatus() == CheckResult.ERROR)
                 || (modemStatusHandler.getPreviousStatus() == CheckResult.TIMEOUT))
                 && (modemStatusHandler.isPriviousSmsSent() == true))
                {
                    sendErrorSms("SmsConnector switched from " + CheckResult.ERROR + " to OK. All modems are working.");
                }
                else if((modemStatusHandler.getPreviousStatus() == CheckResult.WARN) && (modemStatusHandler.isPriviousSmsSent() == true))
                {
                    if((modemStatusHandler.isErrorStatusSet()))
                    {
                        sendErrorSms("SmsConnector switched from " + CheckResult.WARN + " to OK. All modems are working.");
                    }
                    else
                    {
                        sendWarnMail("SmsConnector switched from " + CheckResult.WARN + " to OK. All modems are working.");
                    }
                }
                
                modemStatusHandler.resetErrorFlag();
                modemStatusHandler.resetForcedCheck();
                
                monitorStatusHandler.setCurrentStatus(CheckResult.OK);
                monitorStatusHandler.setSmsSent(false);
                if((monitorStatusHandler.getPreviousStatus() == CheckResult.ERROR) && (monitorStatusHandler.isPriviousSmsSent() == true))
                {
                    sendErrorSms("AmsSystemMonitor switched from " + CheckResult.ERROR + " to OK.");
                }
                
                monitorStatusHandler.resetErrorFlag();
                
                logger.info("GSM modem(s) is(are) working.");
            }
            catch(AmsSystemMonitorException asme)
            {
                if(asme.getErrorCode() == AmsSystemMonitorException.ERROR_CODE_SYSTEM_MONITOR)
                {
                    logger.warn("AmsSystemMonitor does not work properly.");
                    
                    monitorStatusHandler.setCurrentStatus(CheckResult.ERROR);
                    if(monitorStatusHandler.sendErrorSms())
                    {
                        sendErrorSms("AmsSystemMonitor could not send JMS check message. HOWTO: http://elogbook.desy.de:8080/eLogbook/HowTo?ACTION=VIEW&VALUE=64");
                        monitorStatusHandler.setSmsSent(true);
                    }
                    else
                    {
                        logger.info("AmsSystemMonitor does not send a SMS yet.");
                    }
                    
                    // No effect here
                    monitorStatusHandler.forceNextCheck();
                }
                else if(asme.getErrorCode() == AmsSystemMonitorException.ERROR_CODE_TIMEOUT)
                {

                    logger.warn("Timeout!");
                    modemStatusHandler.setCurrentStatus(CheckResult.TIMEOUT);
                    
                    logger.info("Number of timeouts: " + modemStatusHandler.getNumberOfTimeouts());
                    if(modemStatusHandler.getNumberOfTimeouts() > allowedTimeout)
                    {
                        modemStatusHandler.setCurrentStatus(CheckResult.ERROR);
                        modemStatusHandler.setErrorStatusSet(true);
                    }
                    
                    if(modemStatusHandler.sendErrorSms())
                    {
                        sendErrorSms("SmsConnector does NOT respond to the current check: " + asme.getMessage() + " HOWTO: http://elogbook.desy.de:8080/eLogbook/HowTo?ACTION=VIEW&VALUE=64");
                        modemStatusHandler.setSmsSent(true);
                    }
                    else
                    {
                        logger.info("AmsSystemMonitor does not send a SMS yet.");
                    }
                    
                    modemStatusHandler.forceNextCheck();
                }
                else if(asme.getErrorCode() == AmsSystemMonitorException.ERROR_CODE_SMS_CONNECTOR_ERROR)
                {
                    logger.warn("SmsConnector does not work properly.");
                    modemStatusHandler.setCurrentStatus(CheckResult.ERROR);
                    
                    if(modemStatusHandler.sendErrorSms())
                    {
                        sendErrorSms("SmsConnector responds a modem problem: " + asme.getMessage() + " HOWTO: http://elogbook.desy.de:8080/eLogbook/HowTo?ACTION=VIEW&VALUE=64");
                        modemStatusHandler.setSmsSent(true);
                    }
                    else
                    {
                        logger.info("AmsSystemMonitor does not send a SMS yet.");
                    }
                    
                    modemStatusHandler.forceNextCheck();
                }
                else if(asme.getErrorCode() == AmsSystemMonitorException.ERROR_CODE_SMS_CONNECTOR_WARN)
                {
                    logger.warn("SmsConnector does not work properly.");
    
                    modemStatusHandler.setCurrentStatus(CheckResult.WARN);
                    
                    if(modemStatusHandler.getPreviousStatus() == CheckResult.ERROR)
                    {
                        sendErrorSms("SmsConnector switched from " + CheckResult.ERROR + " to WARN: " + asme.getMessage() + " HOWTO: http://elogbook.desy.de:8080/eLogbook/HowTo?ACTION=VIEW&VALUE=64");
                        modemStatusHandler.setSmsSent(true);
                    }
                    
                    if(modemStatusHandler.sendWarnMail())
                    {
                        sendWarnMail("SmsConnector responds a modem problem: " + asme.getMessage() + " HOWTO: http://elogbook.desy.de:8080/eLogbook/HowTo?ACTION=VIEW&VALUE=64");
                        modemStatusHandler.setSmsSent(true);
                    }
                    else
                    {
                        logger.info("AmsSystemMonitor does not send a warn mail yet.");
                    }
                    
                    modemStatusHandler.forceNextCheck();
                }
            }

            modemStatusHandler.stopCurrentCheck();
            monitorStatusHandler.stopCurrentCheck();
        }
        else
        {
            logger.info("No modem check now.");
        }
    }

    // TODO: Auslagern in eine Helferklasse
    public boolean sendWarnMail(String text)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String[] list = null;
        String to = "";
        boolean success = false;
        
        success = true;
        
        logger.debug("sendWarnMail(): " + text);

        text = text + " [" + dateFormat.format(Calendar.getInstance().getTime()) + "]";
        
        IPreferencesService pref = Platform.getPreferencesService();
        
        String server = pref.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_MAIL_SERVER, "", null);
        String from = pref.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_MAIL_SENDER, "", null);
        String subject = pref.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_MAIL_SUBJECT, "", null);
        String amsGroup = pref.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_AMS_GROUP, "", null);
        
        list = DatabaseHelper.getEMailAddresses(amsGroup);
        if(list != null)
        {
            for(int i = 0;i < list.length;i++)
            {
                to = to + list[i] + ",";
                logger.info("Mail to: " + list[i]);
            }
            
            to = to.trim();
            if(to.endsWith(","))
            {
                to = to.substring(0, to.length() - 1);
            }
            
            success = CommonMailer.sendMail(server, from, to, subject, text);
        }
        else
        {
            // We do not have any mail address. Use the emergency number from the preferences.
            to = pref.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_SMS_EMERGENCY_MAIL, "", null);
            logger.info("Mail to: " + to);
            success = CommonMailer.sendMail(server, from, to, subject, text);
        }
                
        return success;
    }
    
    // TODO: Auslagern in eine Helferklasse
    public boolean sendErrorSms(String text)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String[] list = null;
        String to = "";
        boolean success = false;
        
        success = true;
        
        logger.debug("sendErrorSms(): " + text);
        text = text + " [" + dateFormat.format(Calendar.getInstance().getTime()) + "]";

        IPreferencesService prefs = Platform.getPreferencesService();

        String server = prefs.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_MAIL_SERVER, "", null);
        String from = prefs.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_MAIL_SENDER, "", null);
        String domainPart = prefs.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_MAIL_DOMAIN_PART, "", null);
        String localPart = prefs.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_MAIL_LOCAL_PART, "", null);
        String subject = prefs.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_MAIL_SUBJECT, "", null);
        String amsGroup = prefs.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_AMS_GROUP, "", null);
        
        // The mail should be send to the SMS Gateway (localPart = sms/${NUMBER})
        if(localPart.indexOf("${NUMBER}") != -1)
        {
            list = DatabaseHelper.getPhoneNumbers(amsGroup);
            if(list != null)
            {
                for(int i = 0;i < list.length;i++)
                {
                    list[i] = localPart.replaceAll("\\$\\{NUMBER\\}", list[i]) + "@" + domainPart;
                    logger.info("SMS to: " + list[i]);
                }
                
                success = (CommonMailer.sendMultiMail(server, from, list, subject, text) == 0);
            }
            else
            {
                // We do not have any phone number. Use the emergency number from the preferences.
                String emergency = prefs.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_SMS_EMERGENCY_NUMBER, "", null);
                to = localPart.replaceAll("\\$\\{NUMBER\\}", emergency) + "@" + domainPart;
                logger.info("SMS to: " + to);
                
                success = CommonMailer.sendMail(server, from, to, subject, text);
            }
        }
        else 
        {
            to = localPart + "@" + domainPart;
            logger.info("SMS to: " + to);
            
            success = CommonMailer.sendMail(server, from, to, subject, text);
        }
        
        // Maybe we want to send the Alarm SMS a second time using the Old Alarm System (via James)
        String temp = prefs.getString(AmsSystemMonitorActivator.PLUGIN_ID, PreferenceKeys.P_SMS_USE_OAS, "", null);
        boolean useOas = false;
        
        useOas = Boolean.parseBoolean(temp);
        if(useOas)
        {
            list = DatabaseHelper.getPhoneNumbers(amsGroup);
            if(list != null)
            {
                for(int i = 0;i < list.length;i++)
                {
                    list[i] = "N:" + list[i] + " " + text;
                    logger.info("SMS to: " + list[i]);
                }
                
                success = (CommonMailer.sendMultiMail(server, from, "sms@krykmail.desy.de", list, text) == 0);
            }
        }
        
        return success;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.equinox.app.IApplication#stop()
     */
    public void stop()
    {
    }
}
