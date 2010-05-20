package org.csstudio.diag.interconnectionServer.server;
/*
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchroton,
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.utility.ldap.engine.Engine;

/**
 * Helper class for local LDAP support.
 *
 * @author Matthias Clausen
 * @author Bastian Knerr
 */
public enum LdapSupport {

    // Modern singleton pattern with synchronization and serialization safety for free.
    INSTANCE;

	private LdapSupport() {
		// EMPTY
	}


	/**
	 *
	 * @param ipAddress
	 * @param ipName
	 * @param ldapIocName
	 * @return 1. Param = logicalIocName; 2. Param = ldapIocName
	 */
	public String[] getLogicalIocName ( final String ipAddress, final String ipName) {

		final String[] stringReturnArray = new String[2];
		String logicalIocName, ldapIocName = null;

		/*
		 * error handling
		 */
		if ( ipAddress.length() < 8) {
			/*
			 * can't be a valid IP address
			 */
			return new String[]{"invalid logical address","invalid ldap name"};
		}

		ldapIocName = Engine.getInstance().getLogicalNameFromIPAdr(ipAddress);
		System.out.println("LdapSupport:  ldapIocName = " + ldapIocName);
		if ( ldapIocName != null) {
			/*
			 * fortunately a valid name was found
			 * the string returned looks like: econ=iocName, ....
			 * make sure the string is a valid LDAP address - must contain "econ"
			 */
			if ( ldapIocName.contains("econ") ) {
				logicalIocName = ldapIocName.substring( ldapIocName.indexOf("econ=")+5,ldapIocName.indexOf(","));
				System.out.println("logicalIocName = " + logicalIocName);
				stringReturnArray[0] = logicalIocName;
				stringReturnArray[1] = ldapIocName;
				return stringReturnArray;
			} else {
				System.out.println("ldapIocName = " + ldapIocName);
				stringReturnArray[0] = ldapIocName;
				stringReturnArray[1] = ldapIocName;
				return stringReturnArray;
			}
		} else {
			CentralLogger.getInstance().warn(this,
					"No logical name configured in LDAP for IOC: " +
					ipName + " [" + ipAddress + "]");
			/*
			 * in the meantime ...
			 */

			if ( ipAddress.equals("131.169.112.56")) {
				stringReturnArray[0] = stringReturnArray[1] = "mkk10KVB1";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.146")) {
				stringReturnArray[0] = stringReturnArray[1] =  "mthKryoStand";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.155")) {
				stringReturnArray[0] = stringReturnArray[1] =  "ttfKryoCMTB";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.68")) {
				stringReturnArray[0] = stringReturnArray[1] =  "utilityIOC";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.80")) {
				stringReturnArray[0] = stringReturnArray[1] =  "ttfKryoLinac";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.52")) {
				stringReturnArray[0] = stringReturnArray[1] =  "krykWetter";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.108")) {
				stringReturnArray[0] = stringReturnArray[1] =  "ttfKryoLinac";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.104")) {
				stringReturnArray[0] = stringReturnArray[1] =  "ttfKryoSK47a";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.54")) {
				stringReturnArray[0] = stringReturnArray[1] =  "ttfKryoCB";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.68")) {
				stringReturnArray[0] = stringReturnArray[1] =  "utilityIOC";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.144")) {
				stringReturnArray[0] = stringReturnArray[1] =  "heraKryoFel";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.109")) {
				stringReturnArray[0] = stringReturnArray[1] =  "ttfKryoVC2";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.178")) {
				stringReturnArray[0] = stringReturnArray[1] =  "mthKryoStand";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.225")) {
				stringReturnArray[0] = stringReturnArray[1] =  "ttfDiagLinac";
				return stringReturnArray;
			} else if ( ipAddress.equals("131.169.112.101")) {
				stringReturnArray[0] = stringReturnArray[1] =  "ttfKryoFV";
				return stringReturnArray;
			} else {
				return new String[]{"~" + ipName + "~","~" + ipName + "~"};
			}
		}



		/*
		 * es fehlen: 131.169.112.178 und 131.169.112.108
		 *
		 *epicsGPFC01       mkk10KVA1       : Keine Datei Y:\directoryServer\mkk10KVA1.BootLine.dat gefunden
epicsGPFC02       mkk10KVB1       131.169.112.56
epicsGPFC03       mkk10KVC1       131.169.112.69
epicsGPFC04       mkk10KVC2       131.169.112.87
epicsGPFC05       mkk10KV6A       131.169.112.153
epicsGPFC06       mkk10KV2B       131.169.112.154
epicsGPFC07       mkk10KV3B       131.169.112.157
epicsPC21         mthKryoStand    131.169.112.146
epicsPC24         wienerVME       131.169.112.150
epicsPC25         ttfKryoCMTB     131.169.112.155
epicsPC26         ttfKryoXCB      131.169.112.170
epicsPPC02        mkkPPC02        131.169.112.224
epicsPPC11        mkkSender       : Keine Datei Y:\directoryServer\mkkSender.BootLine.dat gefunden
epicsPPC12        ttfKryoSK47a    131.169.112.104
epicsPPC13        mkkPPC03        : Keine Datei Y:\directoryServer\mkkPPC03.BootLine.dat gefunden
epicsPPC14        ttfKryoVC2      131.169.112.109
epicsPPC18        mkkModbus       131.169.113.52
epicsPPC19        ttfKryoVC1      131.169.113.53
epicsVME00        utilityIOC      131.169.112.68
epicsVME01        mkkTempPuls     : Keine Datei Y:\directoryServer\mkkTempPuls.BootLine.dat gefunden
epicsVME02        kryoCta         131.169.112.94
epicsVME04        ttfKryoLinac    131.169.112.80
epicsVME08        analyze         131.169.112.228
epicsVME11        heraKryoKoMag   131.169.112.92
epicsVME12        modulator       : Keine Datei Y:\directoryServer\modulator.BootLine.dat gefunden
epicsVME14        ttfDiagLinac    131.169.112.225
epicsVME15        mhf-irm-a       : Keine Datei Y:\directoryServer\mhf-irm-a.BootLine.dat gefunden
epicsVME16        mkkKlima3       131.169.112.227
epicsVME17        mkkPowStatC_B   131.169.112.176
epicsVME18        mkk-irm-b       131.169.112.177
epicsVME20        krykWetter      131.169.112.52
epicsVME22        ttfKryoCB       131.169.112.54
epicsVME27        heraKryoRefmag  : Keine Datei Y:\directoryServer\heraKryoRefmag.BootLine.dat gefunden
epicsVME28        heraKryoCavity  : Keine Datei Y:\directoryServer\heraKryoCavity.BootLine.dat gefunden
epicsVME29        tineDataSrv     131.169.112.229
epicsVME34        mkkKlima2       131.169.112.138
epicsVME35        heraKryoFel     131.169.112.144
epicsVME36        mkkKlima1       131.169.112.145
epicsVME37        mkk-irm-a       131.169.112.114
epicsVME40        ttfKryoFV       131.169.112.101
epicsVME62        mkkPowStatC_A   131.169.112.142
epicsVME62.irm-c  mkk-irm-c       : Keine Datei Y:\directoryServer\mkk-irm-c.BootLine.dat gefunden
		 */
	}

	public void setAllRecordsToConnected ( final String ldapIocName) {
		/*
		 * just a convenience method
		 */

        final String status = "ONLINE";
        final String severity = "NO_ALARM";

		CentralLogger.getInstance().debug(this,"IocChangeState: setAllRecordsToConnected");
		setAllRecordsInLdapServerAndJms ( ldapIocName, status, severity);

	}

	public void setAllRecordsToDisconnected ( final String ldapIocName) {
		/*
		 * just a convenience method
		 */
        final String status = "DISCONNECTED";
        final String severity = "INVALID";
		CentralLogger.getInstance().debug(this,"IocChangeState: setAllRecordsToDisconnected");
		setAllRecordsInLdapServerAndJms ( ldapIocName, status, severity);

	}

	/*
	 * method is synchronized: In case several IOCs disconnect the threads to enter the changes in the LDAP server will be
	 * started. But they will write in parallel to the LDAP server - but in sequence!
	 * This will (partly) avoid congestion on the send queue in addLdapWriteRequest()
	 */
	synchronized private void setAllRecordsInLdapServerAndJms (final String ldapIocName, final String status, final String severity) {
	    String logicalIocName = ldapIocName;
		/*
		 * find all records belonging to the IOC: logicalIocName
		 * -> search for econ = logicalIocName
		 * -> create list for all eren (record) entries
		 *
		 * for each eren entry set the epicsAlarmStatus to 'OFFLINE' and the epicsAlarmTimeStamp to the actual time
		 */

		//
		// create time stamp written to epicsAlarmTimeStamp
		// this is a copy from the class ClientRequest
		//
		final SimpleDateFormat sdf = new SimpleDateFormat( PreferenceProperties.JMS_DATE_FORMAT);
        final java.util.Date currentDate = new java.util.Date();
        final String eventTime = sdf.format(currentDate);

		final ArrayList<String> allRecordList = Engine.getInstance().getAllRecordsOfIOC(ldapIocName, severity, status, eventTime);

        if(logicalIocName==null){
            return;
        }else if(logicalIocName.contains("=")){
            logicalIocName  = logicalIocName.split("[=,]")[1];
        }
        for (final String channelName : allRecordList) {
            CentralLogger.getInstance().debug(this, "Found Channelname: "+channelName);
            if(channelName!=null){
            	/*
            	 * set values in LDAP and create JMS message
            	 */
                setSingleChannel(channelName, status ,severity, eventTime, logicalIocName);
            }
        }
    }


	private void setSingleChannel ( String channelName, final String status, final String severity, final String eventTime, final String logicalIocName) {
        if(channelName==null){
        	CentralLogger.getInstance().error(this, "no channel name set");
            return;
        }
        /*
         * TODO:
         * addLdapWriteRequest does NOT support the usage of the full qualifies LDAP string
         * So we remove it - for now until it's supported
         */
        if(channelName.contains("=")){
        	channelName  = channelName.split("[=,]")[1];
        }

        if(severity!=null){
            Engine.getInstance().addLdapWriteRequest( "epicsAlarmSeverity", channelName, severity);
            CentralLogger.getInstance().debug(this, "Set SEVERITY: " + severity + " for channel: " + channelName);
        }
        if(status!=null){
            Engine.getInstance().addLdapWriteRequest( "epicsAlarmStatus", channelName, status);
        }
        if(eventTime!=null){
            Engine.getInstance().addLdapWriteRequest( "epicsAlarmTimeStamp", channelName, eventTime);
        }
        /*
         * up to this point the channelName is still the LDAP address
         * eren=alarmTest:RAMPA_calc,econ=Bernds_Test_IOC,ecom=EPICS-IOC,efan=TEST,ou=epicsControls
         * we need to extract the record name from this LDAP string
         */
        if(channelName.contains("=")){
        	channelName  = channelName.split("[=,]")[1];
        }

		JmsMessage.getInstance().sendMessage ( JmsMessage.JMS_MESSAGE_TYPE_ALARM,
				JmsMessage.MESSAGE_TYPE_STATUS, 									// type
				channelName,														// name
				null, 																// value
				severity, 															// severity
				status, 															// status
				logicalIocName, 													// host
				null, 																// facility
				"alarm set by IC-Server");																// howTo
	}
}
