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

import org.csstudio.platform.data.ILongValue;
import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.ISeverity;
import org.csstudio.platform.data.ITimestamp;

/** Implementation of {@link ILongValue}.
 *  @see ILongValue
 *  @author Kay Kasemir, Xihui Chen
 */
public class LongValue extends Value implements ILongValue
{
	private final long values[];
	
    /** Constructor from pieces. */
	public LongValue(final ITimestamp time, final ISeverity severity,
                     final String status, final INumericMetaData meta_data,
                     final Quality quality, final long values[])
	{
		super(time, severity, status, meta_data, quality);
		this.values = values;
	}

    /** {@inheritDoc} */
	public final long[] getValues()
	{	return values;	}

    /** {@inheritDoc} */
    public final long getValue()
	{	return values[0];	}
	
    /** {@inheritDoc} */
	@Override
	public final String format(final Format how, int precision)
	{
		// Any value at all?
		if (!getSeverity().hasValue())
			return Messages.NoValue;

		final StringBuffer buf = new StringBuffer();
		if (how == Format.Exponential)
		{
			// Is there a better way to get this silly format?
			NumberFormat fmt;
			StringBuffer pattern = new StringBuffer(10);
			pattern.append("0."); //$NON-NLS-1$
			for (int i = 0; i < precision; ++i)
				pattern.append('0');
			pattern.append("E0"); //$NON-NLS-1$
			fmt = new DecimalFormat(pattern.toString());
			buf.append(fmt.format(values[0]));
			for (int i = 1; i < values.length; i++)
			{
				buf.append(Messages.ArrayElementSeparator);
				buf.append(buf.append(fmt.format(values[i])));
			}
		}
		else if (how == Format.String)
		{   // Format array elements as characters
			for (int i = 0; i < values.length; i++)
				buf.append(getDisplayChar((char) values[i]));
		}
		else
		{
			buf.append(values[0]);
			for (int i = 1; i < values.length; i++)
			{
				buf.append(Messages.ArrayElementSeparator);
				buf.append(values[i]);
				if (i >= MAX_FORMAT_VALUE_COUNT)
				{
					buf.append(Messages.ArrayElementSeparator);
					buf.append("..."); //$NON-NLS-1$
					break;
				}
			}
		}
		return buf.toString();
	}
	
    /** {@inheritDoc} */
	@Override
	public final boolean equals(final Object obj)
	{
		if (! (obj instanceof LongValue))
			return false;
		final LongValue rhs = (LongValue) obj;
		if (rhs.values.length != values.length)
			return false;
		for (int i=0; i<values.length; ++i)
			if (rhs.values[i] != values[i])
				return false;
		return super.equals(obj);
	}

    /** {@inheritDoc} */
	@Override
	public final int hashCode()
	{
		int h = super.hashCode();
		for (int i=0; i<values.length; ++i)
			h += values[i];
		return h;
	}
}
