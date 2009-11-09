/*
 * Copyright (c) 2007 Stiftung Deutsches Elektronen-Synchrotron,
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
/*
 * $Id$
 */
package org.csstudio.config.ioconfig.model.pbmodel.gsdParser;

import java.util.HashMap;

import org.csstudio.config.ioconfig.model.pbmodel.GSDFile;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 18.07.2008
 */
public final class GsdFactory {
    
    
    
    private static HashMap<Integer, GsdSlaveModel> _gsdSlaveModelMap = new HashMap<Integer, GsdSlaveModel>();

    /**
     * Default Constructor.
     */
    private GsdFactory(){
        
    }
    /**
     * Generate a GSD Master Model from Master GFSD File. 
     * @param gsdMasterFile The Master GSF File.
     * @return The {@link GsdMasterModel} from GSD File.
     */
    public static GsdMasterModel makeGsdMaster(final String gsdMasterFile){
        GSD2OBJ dataInstanz = new GSD2OBJ();
        
        GsdMasterModel masterModel = new GsdMasterModel();
        
        dataInstanz.setGeneralStruct(gsdMasterFile, masterModel);
        if (dataInstanz.setMasterStruct(gsdMasterFile, masterModel) != 0) {
            return null;
        }
        return masterModel;

    }

    /**
     * Generate a GSD Slave Model from Slave GFSD File. 
     * @param gsdFile The Slave GSF File.
     * @return The {@link GsdSlaveModel} from GSD File.
     */
    public static GsdSlaveModel makeGsdSlave(final GSDFile gsdFile){
        
        GsdSlaveModel slaveModel = _gsdSlaveModelMap.get(gsdFile.getId());
        if(slaveModel==null) {
            GSD2OBJ dataInstanz = new GSD2OBJ();
            
            slaveModel = new GsdSlaveModel();
    
            dataInstanz.setGeneralStruct(gsdFile.getGSDFile(), slaveModel);
            if (dataInstanz.setSlaveStruct(gsdFile.getGSDFile(), slaveModel) != 0) {
                return null;
            }
            GsdFileParser.parseSlave(gsdFile, slaveModel);
            _gsdSlaveModelMap.put(gsdFile.getId(), slaveModel);
        }        

        return slaveModel;
    }
}
