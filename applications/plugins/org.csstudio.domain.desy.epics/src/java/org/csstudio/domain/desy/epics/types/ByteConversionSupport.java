/*
 * Copyright (c) 2011 Stiftung Deutsches Elektronen-Synchrotron,
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
package org.csstudio.domain.desy.epics.types;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.csstudio.domain.desy.epics.alarm.EpicsGraphicsData;
import org.csstudio.domain.desy.epics.alarm.EpicsMetaData;
import org.csstudio.domain.desy.types.Limits;
import org.csstudio.domain.desy.typesupport.TypeSupportException;
import org.csstudio.platform.data.IMetaData;
import org.csstudio.platform.data.INumericMetaData;

final class ByteConversionSupport extends EpicsIMetaDataTypeSupport<Byte> {

    @Nonnull
    private Byte toByte(final double d) {
        return Byte.valueOf(Double.valueOf(d).byteValue());
    }
    /**
     * Constructor.
     */
    public ByteConversionSupport() {
        super(Byte.class);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    protected EpicsMetaData convertToMetaData(@Nonnull final IMetaData data) throws TypeSupportException {
        final INumericMetaData numData = checkAndConvert(data, Byte.class);
        final EpicsGraphicsData<Byte> gr =
            new EpicsGraphicsData<Byte>(Limits.<Byte>create(toByte(numData.getAlarmLow()),
                                                            toByte(numData.getAlarmHigh())),
                                        Limits.<Byte>create(toByte(numData.getWarnLow()),
                                                            toByte(numData.getWarnHigh())),
                                        Limits.<Byte>create(toByte(numData.getDisplayLow()),
                                                            toByte(numData.getDisplayHigh())));
        return new EpicsMetaData(null, gr, null, null);
    }
}