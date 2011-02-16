/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.common.engine.model;

import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.ISeverity;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.TimestampFactory;
import org.csstudio.platform.data.ValueFactory;

/** Helper for creating test data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TestValueFactory
{
    final private static ISeverity severity = ValueFactory.createOKSeverity();
    final private static String status = "Test";
    final private static INumericMetaData meta_data = 
        ValueFactory.createNumericMetaData(0, 10, 2, 8, 1, 9, 2, "Eggs");

    public static IValue getDouble(double value)
    {
        final ITimestamp time = TimestampFactory.now();
    	return ValueFactory.createDoubleValue(time,
                severity, status, meta_data,
                IValue.Quality.Original,
                new double[] { value } );
    }
}
