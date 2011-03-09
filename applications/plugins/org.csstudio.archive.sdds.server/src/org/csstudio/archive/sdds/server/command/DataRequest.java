
/* 
 * Copyright (c) 2010 Stiftung Deutsches Elektronen-Synchrotron, 
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

package org.csstudio.archive.sdds.server.command;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.csstudio.archive.sdds.server.command.header.DataRequestHeader;
import org.csstudio.archive.sdds.server.conversion.SampleCtrl;
import org.csstudio.archive.sdds.server.data.DataCollector;
import org.csstudio.archive.sdds.server.data.DataCollectorException;
import org.csstudio.archive.sdds.server.data.EpicsRecordData;
import org.csstudio.archive.sdds.server.data.RecordDataCollection;
import org.csstudio.archive.sdds.server.util.IntegerValue;
import org.csstudio.archive.sdds.server.util.RawData;
import de.desy.aapi.AapiServerError;

/**
 * @author Markus Moeller
 *
 */
public class DataRequest extends ServerCommand {
    
    /** The data reader */
    private DataCollector dataCollector;

    /**
     * 
     * @throws ServerCommandException
     */
    public DataRequest() throws ServerCommandException {
        
        super();
        
        try {
            dataCollector = new DataCollector();
        } catch(DataCollectorException dce) {
            throw new ServerCommandException("Cannot create instance of DataCollector: " + dce.getMessage());
        }
    }

    /**
     * 
     */
    @Override
	public void execute(RawData buffer, RawData receivedValue, IntegerValue resultLength)
    throws ServerCommandException, CommandNotImplementedException {
        
        RecordDataCollection data = null;
        DataRequestHeader header = new DataRequestHeader(buffer.getData());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        double f;
        
        logger.debug(header.toString());
        
        if(header.isTimeDiffValid() == false) {
            
            receivedValue.setData(createErrorAnswer(AapiServerError.FROM_MORE_THEN_TO.getErrorNumber()));
            receivedValue.setErrorValue(AapiServerError.FROM_MORE_THEN_TO.getErrorNumber());
            logger.debug("ERROR: " + AapiServerError.FROM_MORE_THEN_TO.toString());
            return;
        }
        
        // TODO: Does it make sense to set a default number of samples instead of returning with an error?
        if(header.hasValidNumberofSamples() == false) {
            
            logger.warn(AapiServerError.BAD_MAX_NUM.toString());
            logger.warn("Using default: 1000");
            
            header.setMaxNumOfSamples(1000);
//            receivedValue.setData(createErrorAnswer(AAPI.AAPI.BAD_MAX_NUM));
//            receivedValue.setErrorValue(AAPI.AAPI.BAD_MAX_NUM);
//            logger.debug("ERROR: " + AAPI.AAPI.aapiServerSideErrorString[AAPI.AAPI.BAD_MAX_NUM]);
//            return;
        }

        try {
            // Number of PV's
            dos.writeInt(header.getPvNameSize());
            
            for(String name : header.getPvName()) {
                
            	data = dataCollector.readData(name, header);
                logger.debug("Number of samples: " + data.getNumberOfData());

                // TODO: Nicht vorhandene Daten abfangen und saubere Fehlermeldung zurueck liefern
                // Error
                dos.writeInt(0);
                
                // Type (6 = double)
                dos.writeInt(6);
                
                // Number of samples
                dos.writeInt(data.getNumberOfData());
                
                for(EpicsRecordData o : data.getData()) {
                    
                    dos.writeInt((int)o.getTime());
                    dos.writeInt((int)o.getNanoSeconds());
                    dos.writeInt((int)o.getStatus());
                    
                    // TODO: Handle ALL data types
					switch(o.getSddsType()) {
                        
                        case SDDS_DOUBLE:
                            
                            f = (Double)o.getValue();
                            dos.writeDouble(f);
                            
                            break;
                        
                        default:
                        	break;
                    }
                }
            }
            
            SampleCtrl sampleCtrl = data.getSampleCtrl();
            
            dos.writeInt(sampleCtrl.getPrecision());
            dos.writeDouble(sampleCtrl.getDisplayHigh());
            dos.writeDouble(sampleCtrl.getDisplayLow());
            dos.writeDouble(sampleCtrl.getHighAlarm());
            dos.writeDouble(sampleCtrl.getHighWarning());
            dos.writeDouble(sampleCtrl.getLowAlarm());
            dos.writeDouble(sampleCtrl.getLowWarning());
            dos.writeInt(sampleCtrl.getUnitsLength());
            dos.writeChars(sampleCtrl.getUnits());
            dos.write('\0');
            
            receivedValue.setData(baos.toByteArray());
            
        } catch(IOException ioe) {
            
            logger.error("[*** IOException ***]: " + ioe.getMessage());
        }
        finally {
            if(dos!=null) {
            	try{dos.close();}catch(Exception e) { /* Can be ignored */ }
            	dos = null;
            }
        }
        
        // throw new ServerCommandException(AAPI.AAPI.aapiServerSideErrorString[AAPI.AAPI.BAD_TIME], AAPI.AAPI.BAD_TIME);
    }
}
