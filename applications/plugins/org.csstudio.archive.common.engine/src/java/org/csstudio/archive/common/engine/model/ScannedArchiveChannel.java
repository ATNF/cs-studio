/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.common.engine.model;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.csstudio.apputil.time.PeriodFormat;
import org.csstudio.platform.data.IDoubleValue;
import org.csstudio.platform.data.IEnumeratedValue;
import org.csstudio.platform.data.ILongValue;
import org.csstudio.platform.data.IStringValue;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.logging.CentralLogger;

/** An ArchiveChannel that stores value in a periodic scan.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScannedArchiveChannel extends ArchiveChannel implements Runnable
{
    /** Scan period in seconds */
    final private double scan_period;
    final private int max_repeats;
    private int repeats = 0;
    private Logger log;
    
    /** @see ArchiveChannel#ArchiveChannel(String, int, IValue) */
    public ScannedArchiveChannel(final String name,
                                 Enablement enablement, final int buffer_capacity,
                                 final IValue last_archived_value,
                                 final double scan_period,
                                 final int max_repeats) throws Exception
    {
        super(name, enablement, buffer_capacity, last_archived_value);
        this.scan_period = scan_period;
        this.max_repeats = max_repeats;
        log = CentralLogger.getInstance().getLogger(this);
        if (! log.isDebugEnabled())
            log = null;
    }
    
    /** @return Scan period in seconds */
    final public double getPeriod()
    {
        return scan_period;
    }

    @Override
    public String getMechanism()
    {
        return PeriodFormat.formatSeconds(scan_period) + " scan, max. "
            + max_repeats + " repeats";
    }

    // Just for debugging...
    @Override
    protected boolean handleNewValue(final IValue value)
    {
        final boolean written = super.handleNewValue(value);
        if (! written  &&   log != null)
            log.debug(getName() + ": cached " + value);
        return written;
    }

    /** Invoked by periodic scanner.
     *  Try to add the most recent value to the archive.
     *  Skip repeated values, unless we exceed the max. repeat count.
     */
    final public void run()
    {
        if (! isEnabled())
            return;
        final IValue value;
        synchronized (this)
        {   // Have anything?
            if (most_recent_value == null)
            {
                if (log != null)
                    log.debug(getName() + " scan: No data");
                return;
            }
            // Is it a new value?
            if (isMatchingValue(last_archived_value, most_recent_value))
            {
                ++repeats ;
                if (repeats < max_repeats)
                {
                    if (log != null)
                        log.debug(getName() + " skips  " + most_recent_value + ": repeat " + repeats);
                    return;
                }
                // No new value, but we'd like to write a sample every once in a while
                value = ValueButcher.transformTimestampToNow(most_recent_value);
                if (log != null)
                    log.debug(getName() + " writes " + most_recent_value + " as " + value.getTime());
                if (value == null)
                {
                    CentralLogger.getInstance().getLogger(this).error("Channel " + getName()
                                    + ": Cannot handle value type "
                                    + most_recent_value.getClass().getName());
                    return;
                }
            }
            else
            {   // It's a new value, so we should be able to write it
                // "as is"
                value = most_recent_value;
                if (log != null)
                    log.debug(getName() + " writes " + value);
            }
            // New value, or exceeded repeats
            repeats = 0;
        }
        // unlocked, should have 'value'
        addValueToBuffer(value);
    }

    /** Check if values match in status, severity, and value. Time is ignored.
     *  @param val1 One value
     *  @param val2 Other value
     *  @return <code>true</code> if they match
     */
    private boolean isMatchingValue(final IValue val1, final IValue val2)
    {
        // Compare data type and value
        if (val1 instanceof IDoubleValue)
        {
            if (! (val2 instanceof IDoubleValue))
                return false;
            final double v1[] = ((IDoubleValue) val1).getValues();
            final double v2[] = ((IDoubleValue) val2).getValues();
            if (!Arrays.equals(v1, v2))
                return false;
        }
        else if (val1 instanceof IEnumeratedValue)
        {
            if (! (val2 instanceof IEnumeratedValue))
                return false;
            final int[] v1 = ((IEnumeratedValue) val1).getValues();
            final int[] v2 = ((IEnumeratedValue) val2).getValues();
            if (!Arrays.equals(v1, v2))
                return false;            
        }
        else if (val1 instanceof ILongValue)
        {
            if (! (val2 instanceof ILongValue))
                return false;
            final long[] v1 = ((ILongValue) val1).getValues();
            final long[] v2 = ((ILongValue) val2).getValues();
            if (!Arrays.equals(v1, v2))
                return false;
        }
        else if (val1 instanceof IStringValue)
        {
            if (! (val2 instanceof IStringValue))
                return false;
            final String[] v1 = ((IStringValue) val1).getValues();
            final String[] v2 = ((IStringValue) val2).getValues();
            if (!Arrays.equals(v1, v2))
                return false;
        }
        else
            return false; // Assume that unknown type differs in value
        // Compare severity, status
        if (!val1.getSeverity().toString().equals(val2.getSeverity().toString()))
            return false;
        if (!val1.getStatus().equals(val2.getStatus()))
            return false;
        return true;
    }
}
