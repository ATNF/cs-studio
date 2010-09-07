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
 * $Id: ProfibusSensorService.java,v 1.2 2009/09/10 12:17:29 hrickens Exp $
 */
package org.csstudio.config.ioconfig.model.service;

import java.util.ArrayList;
import java.util.List;
import org.csstudio.config.ioconfig.model.Repository;
import org.csstudio.config.ioconfig.model.SensorsDBO;
import org.csstudio.dct.ISensorIdService;

/**
 * @author hrickens
 * @author $Author: hrickens $
 * @version $Revision: 1.2 $
 * @since 06.07.2009
 */
public class ProfibusSensorService implements ISensorIdService {

    /* (non-Javadoc)
     * @see org.csstudio.dct.SensorService#getSonsorId(java.lang.String, java.lang.String)
     */
    public String getSensorId(String ioName, String selection) {
        SensorsDBO loadSensors = Repository.loadSensor(ioName, selection);
        if(loadSensors==null) {
//            return null;
            return "$$$ NO Sensors ID found for IOName "+ioName+" $$$";
        }
        return loadSensors.getSensorID();
    }

    /* (non-Javadoc)
     * @see org.csstudio.dct.SensorService#getSonsorIds(java.lang.String)
     */
    public List<String> getSensorIds(String ioName) {
        List<SensorsDBO> loadSensors = Repository.loadSensors(ioName);
        List<String> sensorsIds = new ArrayList<String>(loadSensors.size());
        for (SensorsDBO sensors : loadSensors) {
            sensorsIds.add(sensors.getSensorID());
        }
        return sensorsIds;
    }

}
