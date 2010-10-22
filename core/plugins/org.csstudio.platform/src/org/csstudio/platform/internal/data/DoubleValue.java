/* 
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron, 
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
 package org.csstudio.platform.internal.data;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.csstudio.platform.data.IDoubleValue;
import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.ISeverity;
import org.csstudio.platform.data.ITimestamp;

/** Implementation of {@link IDoubleValue}.
 *  @author Kay Kasemir, Xihui Chen
 */
public class DoubleValue extends Value implements IDoubleValue
{
	/** Map of NumberFormats by precision.
	 * 
	 *  JProfiler tests showed that about _half_ of the string formatting
	 *  is spent in creating the suitable NumberFormat,
	 *  so they are cached for re-use.
	 *  The key is the 'precision', where a precision >= 0 means decimal format,
	 *  and a precision < 0 means exponential notation.
	 *  
	 *  Access must sync on the hash, then sync on the format while using it.
	 */
	final private static Map<Integer, NumberFormat> fmt_cache = new HashMap<Integer, NumberFormat>();

	/** The values. */
	final private double values[];
	
    /** Constructor from pieces. */
	public DoubleValue(final ITimestamp time, final ISeverity severity,
                       final String status, final INumericMetaData meta_data,
                       final Quality quality,
                       final double values[])
	{
		super(time, severity, status, meta_data, quality);
		this.values = values;
	}

    /** {@inheritDoc} */
	final public double[] getValues()
	{	return values;	}

    /** {@inheritDoc} */
	final public double getValue()
	{	return values[0];	}
	
    /** {@inheritDoc} 
     * <br> 
     * If precision is less than zero, the precision from meta data will be used.
     */
	@Override
    public String format(final Format how, int precision)
	{
	    // Any value at all?
	    if (!getSeverity().hasValue())
	        return Messages.NoValue;

	    final StringBuilder buf = new StringBuilder();

	    if (how == Format.String)
	    {   // Handle array elements as characters
	    	for (int i = 0; i<values.length; i++)
			{
				final char c = getDisplayChar((char) values[i]);
				if (c == 0)
					break;
				buf.append(c);
			}
	    	return buf.toString();
	    }

	    // Show array elements as numbers
		
		NumberFormat fmt;
        if (how == Format.Exponential)
        {
        	// Assert positive precision
        	precision = Math.abs(precision);
        	synchronized (fmt_cache)
            {
            	// Exponential notation itentified as 'negative' precision in cached
        		fmt = fmt_cache.get(-precision);
        		if (fmt == null)
        		{	// Is there a better way to get this silly format?
	            	final StringBuffer pattern = new StringBuffer(10);
	                pattern.append("0."); //$NON-NLS-1$
	                for (int i=0; i<precision; ++i)
	                    pattern.append('0');
	                pattern.append("E0"); //$NON-NLS-1$
	                fmt = new DecimalFormat(pattern.toString());
	                fmt_cache.put(-precision, fmt);
        		}
            }
        }
        else
        {
            // For the default format, or when requested via <0 prec.,
            // use the precision from meta data
            if (how == Format.Default  ||  precision < 0)
            {
                final INumericMetaData num_meta = (INumericMetaData)getMetaData();
                // Should have numeric meta data, but in case of errors
                // that might be null.
                if (num_meta != null)
                    precision = num_meta.getPrecision();
            }
            // Hack: If default format precision is 0, assume nobody configured
            // it properly, and fall back to Double.toString
            if (how == Format.Default  &&  precision == 0)
            {
                fmt = null;
            }
            else
            {
            	synchronized (fmt_cache)
                {
                	fmt = fmt_cache.get(precision);
                	if (fmt == null)
                	{
    	                fmt = NumberFormat.getNumberInstance();
    	                fmt.setMinimumFractionDigits(precision);
    	                fmt.setMaximumFractionDigits(precision);
    	                fmt_cache.put(precision, fmt);
                	}
                }
            }
        }
        buf.append(formatDouble(fmt, values[0]));
        for (int i = 1; i < values.length; i++)
        {            	
        	buf.append(Messages.ArrayElementSeparator);
            buf.append(formatDouble(fmt, values[i]));
            if(i >= MAX_FORMAT_VALUE_COUNT){
            	buf.append(Messages.ArrayElementSeparator);
            	buf.append("..."); //$NON-NLS-1$
            	break;
            }
        }
		return buf.toString();
	}

	/** @param fmt NumberFormat or <code>null</code> to use Double.toString
	 *  @param d Number to format
	 *  @return String
	 */
    private String formatDouble(final NumberFormat fmt, double d)
    {
        if (Double.isInfinite(d))
            return Messages.Infinite;
        if (Double.isNaN(d))
            return Messages.NaN;
        if (fmt == null)
            return Double.toString(d);
        synchronized (fmt)
        {
            return fmt.format(d);
        }
    }
	
    /** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj)
	{
		if (! (obj instanceof DoubleValue))
			return false;
		final DoubleValue rhs = (DoubleValue) obj;
		if (rhs.values.length != values.length)
			return false;
        // Compare individual values, using the hint from
		// page 33 of the "Effective Java" book to handle
		// NaN and Infinity
        for (int i=0; i<values.length; ++i)
            if (Double.doubleToLongBits(values[i]) !=
                Double.doubleToLongBits(rhs.values[i]))
                return false;
		return super.equals(obj);
	}

    /** {@inheritDoc} */
	@Override
	public int hashCode()
	{
		int h = super.hashCode();
		for (int i=0; i<values.length; ++i)
			h += values[i];
		return h;
	}
}
