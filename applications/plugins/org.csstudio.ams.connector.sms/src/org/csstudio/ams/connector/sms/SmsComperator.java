
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
 */

package org.csstudio.ams.connector.sms;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author Markus Moeller
 *
 */
public class SmsComperator implements Comparator<Sms>, Serializable
{
    /** Generated serial version id */
    private static final long serialVersionUID = 2782893510854286259L;
    
    /**
     * Compares the timestamp and the priority of the SMS.
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int oldCompare(Sms sms0, Sms sms1)
    {
        int resultTimestamp;
        int resultPriority;
        int resultId;
        int result;
        
        resultTimestamp = Long.signum(sms0.getSmsTimestamp() - sms1.getSmsTimestamp());
        resultPriority = Integer.signum(sms0.getPriority() - sms1.getPriority());
        resultId = Long.signum(sms0.getId() - sms1.getId());
        
        if(resultPriority == 1)
        {
            result = resultPriority;
        }
        else if(resultTimestamp == 0)
        {
            result = resultId;
        }
        else
        {
            result = resultTimestamp;
        }
        
        return result;
    }
    
    public int compare(Sms sms0, Sms sms1)
    {
        int resultTimestamp;
        int resultPriority;
        int result;
        
        resultTimestamp = Long.signum(sms0.getSmsTimestamp() - sms1.getSmsTimestamp());
        resultPriority = Integer.signum(sms0.getPriority() - sms1.getPriority());
        
        if(sms0.getPriority() < sms1.getPriority())
        {
            result = resultPriority;
        }
        else if(sms0.getPriority() == sms1.getPriority())
        {
            result = resultTimestamp;
        }
        else
        {
            result = resultPriority;
        }
        
        return result;
    }
}
