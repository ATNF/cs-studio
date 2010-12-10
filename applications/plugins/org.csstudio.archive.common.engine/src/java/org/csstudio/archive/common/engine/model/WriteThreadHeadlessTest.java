/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.common.engine.model;


import java.util.HashMap;
import java.util.Map;

import org.csstudio.apputil.test.TestProperties;
import org.csstudio.archive.common.engine.RDBArchiveEnginePreferences;
import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.ISeverity;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.TimestampFactory;
import org.csstudio.platform.data.ValueFactory;
import org.junit.Test;

/** [Headless] JUnit write thread tests, writing from a queue with fake samples.
 *  @author Kay Kasemir
 */
public class WriteThreadHeadlessTest
{
    @SuppressWarnings("nls")
    @Test(timeout=20000)
    public void testWriteThread() throws Exception
    {
    	// Get test configuration
    	final TestProperties settings = new TestProperties();
    	final String url = settings.getString("archive_rdb_url");
    	if (url == null)
    	{
    		System.out.println("Skipping, no archive test settings");
    		return;
    	}
    	final String user = settings.getString("archive_rdb_user");
    	final String password = settings.getString("archive_rdb_password");
    	final String channel = settings.getString("archive_write_channel");
    	if (channel == null)
    	{
    		System.out.println("Skipping, no name for write_channel");
    		return;
    	}
		System.out.println("Writing samples for channel " + channel);

    	// Setup buffer
        final SampleBuffer buffer = new SampleBuffer(channel, 1000);

        // Connect writer to the service with the given prefs
        final Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(RDBArchiveEnginePreferences.URL, url);
        prefs.put(RDBArchiveEnginePreferences.USER, user);
        prefs.put(RDBArchiveEnginePreferences.PASSWORD, password);

        final WriteThread writer = new WriteThread(prefs);
        writer.addSampleBuffer(buffer);

        // Trigger thread to write
        writer.start(5.0, 500);

        // Add some samples
        final long seconds = TimestampFactory.now().seconds();
        final ISeverity severity = ValueFactory.createOKSeverity();
        final String status = "Test";
        final INumericMetaData meta_data =
            ValueFactory.createNumericMetaData(0, 10, 2, 8, 1, 9, 2, "Eggs");
        for (int i=0; i<10; ++i)
        {
            final ITimestamp time = TimestampFactory.createTimestamp(seconds, i);
            buffer.add(ValueFactory.createDoubleValue(time,
                            severity, status, meta_data,
                            IValue.Quality.Original,
                            new double[] { i } ));
            Thread.sleep(1);
        }

        // Wait for the thread to write all the samples
        while (buffer.size() > 0) {
            Thread.sleep(500);
        }
        writer.shutdown();

        // Show stats
        System.out.println(buffer);
    }
}
