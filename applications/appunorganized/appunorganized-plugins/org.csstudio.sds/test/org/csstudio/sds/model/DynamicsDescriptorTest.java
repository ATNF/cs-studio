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
package org.csstudio.sds.model;

import static org.junit.Assert.assertEquals;

import org.csstudio.sds.internal.rules.ParameterDescriptor;
import org.junit.Test;

/**
 * Test case for class {@link DynamicsDescriptor}.
 *
 * @author Alexander Will, Stefan Hofer
 * @version $Revision: 1.3 $
 *
 */
public final class DynamicsDescriptorTest {

    /**
     * Test method for {@link org.csstudio.sds.model.DynamicsDescriptor}.
     */
    @Test
    public void testElementConfiguration() {
        final String descriptorId = "testId"; //$NON-NLS-1$
        final String channel = "kryo/pump"; //$NON-NLS-1$

        DynamicsDescriptor ec = new DynamicsDescriptor(descriptorId);
        assertEquals(descriptorId, ec.getRuleId());

        ec.addInputChannel(new ParameterDescriptor(
                channel));
        final ParameterDescriptor[] inputChannels = ec.getInputChannels();

        assertEquals(1, inputChannels.length);
        assertEquals(channel, inputChannels[0].getChannel());
    }

}
