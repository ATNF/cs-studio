package org.csstudio.diag.interconnectionServer.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.csstudio.platform.logging.CentralLogger;

public class SendCommandToIoc extends Thread {
	
	private String hostName = "locahost";
	private int port = 0;
	private String command = "NONE";
	private String statisticId = "NONE";
	private int id = 0;
	
	public SendCommandToIoc ( String hostName, int port, String command) {
		
		this.id = InterconnectionServer.getInstance().getSendCommandId();
		this.hostName = hostName;
		this.port = port;
		this.command = command;
		this.statisticId = hostName + ":" + port;
		this.start();
	}
	
	public SendCommandToIoc ( String statisticId, String command) {
		
		this.id = InterconnectionServer.getInstance().getSendCommandId();
		this.hostName = Statistic.getInstance().getContentObject(statisticId).getHost();
		this.port = Statistic.getInstance().getContentObject(statisticId).getPort();
		this.command = command;
		this.statisticId = statisticId;
		this.start();
	}
	
	public void run() {
		
		/*
		 * 
		 */
		byte[] preparedMessage = null; 
		byte[] buffer	=  new byte[1024];
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		String answer, answerMessage = null;
        
        preparedMessage = prepareMessage ( command, id);

        try
        {
        	socket = new DatagramSocket( );	// do NOT specify the port
            
            // DatagramPacket newPacket = new DatagramPacket(preparedMessage, preparedMessage.length, packet.getAddress(), packet.getPort());
            DatagramPacket newPacket = new DatagramPacket(preparedMessage, preparedMessage.length, InetAddress.getByName( hostName), PreferenceProperties.COMMAND_PORT_NUMBER);
            
            socket.send(newPacket);
            
            
			try {
				/*
	        	 * set timeout period to 10 seconds
	        	 */
				socket.setSoTimeout( PreferenceProperties.TIME_TO_GET_ANSWER_FROM_IOC_AFTER_COMMAND);

				packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
			} catch (InterruptedIOException ioe) {
				// TODO: handle exception
				ioe.printStackTrace();
			}            
			/*
             * check answer
             * for now we only check for the string 'DONE'
             */
			answerMessage = new String(packet.getData(), 0, packet.getLength());
			
			/*
			 * check whether this message contains the mandatory string: REPLY
			 * if not it's not a valid answer
			 */
			if ( answerMessage.contains("REPLY")) {
				answer = answerMessage.substring( answerMessage.indexOf("REPLY=")+6, answerMessage.length()-2);
			} else {
				answer = "invalid answer from IOC";
			}
			
			
			//System.out.println ("IOC: " + hostName + " is " + answer);
			
            if ( TagList.getInstance().getReplyType(answer) == TagList.REPLY_TYPE_DONE) {
            	/*
            	 * nothing to do
            	 */
            	CentralLogger.getInstance().info(this, "Command accepted by IOC: " + hostName + " command: " + command);
            } else if (TagList.getInstance().getReplyType(answer) == TagList.REPLY_TYPE_SELECTED) {
            	/*
            	 * did the select state change?
            	 */
            	if (!Statistic.getInstance().getContentObject(statisticId).isSelectState()) {
        			//remember we're selected
        			Statistic.getInstance().getContentObject(statisticId).setSelectState(true);
        			//create log message
        			CentralLogger.getInstance().warn(this, "IOC SELECTED this InterConnectionServer: " + hostName);
        			// send command to IOC - get ALL alarm states
        			new SendCommandToIoc( hostName, port, PreferenceProperties.COMMAND_SEND_ALL_ALARMS);
        			/*
        			 * TODO
        			 * send JMS message - we are selected
        			 */
        		}           	
            } else if (TagList.getInstance().getReplyType(answer) == TagList.REPLY_TYPE_NOT_SELECTED) {
            	/*
        		 * we are not selected any more
        		 * in case we were selected before - we'll have to create a JMS message
        		 */
            	
        		if ( Statistic.getInstance().getContentObject(statisticId).isSelectState()) {
        			//create log message
        			CentralLogger.getInstance().warn(this, "IOC DE-selected this InterConnectionServer: " + hostName);
        			/*
        			 * TODO
        			 * send JMS message - we are NOT selected
        			 */
        		}
        		//remember we're not selected any more
    			Statistic.getInstance().getContentObject(statisticId).setSelectState(false);
            }else {
            	CentralLogger.getInstance().info(this, "Command not accepted by IOC: " + hostName + " command: " + command + " answer: " + answer);
            }

        }
        catch ( /* UnknownHostException is a */ IOException e )
        {
          e.printStackTrace();
        }
        finally
        {
          if ( socket != null )
        	  socket.close(); 
        } 
		
	}
	
	public byte[] prepareMessage ( String command, int id) {
		String message = null;
		
		message = "COMMAND=" + command + ";" + "ID=" + id + ";";
		message = message + "\0";
        
        return message.getBytes();
	}

}
