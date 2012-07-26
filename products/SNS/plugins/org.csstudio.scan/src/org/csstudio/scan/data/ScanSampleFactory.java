/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.data;

import java.util.Date;

/** Factory for {@link ScanSample} instances
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanSampleFactory
{
    /** Create ScanSample for plain number or text value
	 *  @param timestamp Time stamp
     *  @param serial Serial to identify when the sample was taken
	 *  @param value Value (Number, String)
	 *  @return {@link ScanSample}
	 *  @throws IllegalArgumentException if the value type is not handled
	 */
	public static ScanSample createSample(final Date timestamp,
	        final long serial, final Object value) throws IllegalArgumentException
	{
		if (value instanceof Number)
			return new NumberScanSample(timestamp, serial, (Number)value);
		throw new IllegalArgumentException("Sample of type " + value.getClass().getName() + " is not handled");
	}
}
