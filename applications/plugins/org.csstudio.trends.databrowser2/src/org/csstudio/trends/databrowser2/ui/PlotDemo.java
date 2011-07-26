/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser2.ui;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.csstudio.csdata.ProcessVariable;
import org.csstudio.data.values.ITimestamp;
import org.csstudio.data.values.IValue;
import org.csstudio.data.values.TimestampFactory;
import org.csstudio.trends.databrowser2.model.ArchiveDataSource;
import org.csstudio.trends.databrowser2.model.ModelItem;
import org.csstudio.trends.databrowser2.model.PlotSampleArray;
import org.csstudio.trends.databrowser2.model.PlotSamples;
import org.csstudio.trends.databrowser2.model.TestSampleBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** (Headless) JUnit Plug-in demo of Plot
 *
 *  Simply displays the plot. Static data, no controller.
 *
 *  Must run as plug-in test to load XY Graph icons etc.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlotDemo
{
    private boolean run = true;
    private volatile boolean scroll = true;
    private ITimestamp start_time, end_time;
    
    final private PlotListener listener = new PlotListener()
    {
        @Override
        public void scrollRequested(final boolean enable_scrolling)
        {
        	System.out.println("Scroll enabled: " + enable_scrolling);
        	scroll = enable_scrolling;
        }

        @Override
        public void timeConfigRequested()
        {
        	System.out.println("Time Config requested");
        }

        @Override
        public void timeAxisChanged(final long start_ms, final long end_ms)
        {
        	start_time = TimestampFactory.fromMillisecs(start_ms);
        	end_time = TimestampFactory.fromMillisecs(end_ms);
        	System.out.println("Time axis: " + start_time + " ... " + end_time);
        }

        @Override
        public void valueAxisChanged(final int index, final double lower, final double upper)
        {
            System.out.println("Value axis " + index + ": " + lower + " ... " + upper);
        }

        @Override
        public void droppedName(final String name)
        {
            System.out.println("Name dropped: " + name);
        }

        @Override
        public void droppedPVName(final ProcessVariable name, final ArchiveDataSource archive)
        {
            System.out.println("PV Name dropped: " + name);
        }
    };

    private void createGUI(final Composite parent)
    {
    	final Display display = parent.getDisplay();
        final GridLayout layout = new GridLayout(1, false);
        parent.setLayout(layout);

        // Canvas that holds the graph
        final Canvas plot_box = new Canvas(parent, 0);
        plot_box.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, layout.numColumns, 1));

        // [Done] button to end demo
        final Button ok = new Button(parent, SWT.PUSH);
        ok.setText("Done");
        ok.setLayoutData(new GridData(SWT.RIGHT, 0, true, false));
        ok.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(final SelectionEvent e)
            {
                run = false;
            }
        });

        final Plot plot = Plot.forCanvas(plot_box);
        plot.addListener(listener);

        // Create demo samples
        final ArrayList<IValue> values = new ArrayList<IValue>();
        for (int i=1; i<10; ++i) {
            values.add(TestSampleBuilder.makeValue(i));
        }
        values.add(TestSampleBuilder.makeError(15, "Disconnected"));
        // Single value. Line should continue until the following 'disconnect'.
        values.add(TestSampleBuilder.makeValue(17));
        values.add(TestSampleBuilder.makeError(18, "Disconnected"));

        for (int i=20; i<30; ++i) {
            values.add(TestSampleBuilder.makeValue(i));
        }

        final PlotSampleArray samples = new PlotSampleArray();
        samples.set("Demo", values);

        // Add item with demo samples
        final ModelItem item = new ModelItem("Demo")
        {
            @Override
            public PlotSamples getSamples()
            {
                return samples;
            }

            @Override
            public void write(final PrintWriter writer)
            {
                // NOP
            }
        };
        item.setColor(new RGB(0, 0, 255));
        plot.addTrace(item);

        start_time = samples.getSample(0).getValue().getTime();
        end_time = samples.getSample(samples.getSize()-1).getValue().getTime();
        plot.setTimeRange(start_time, end_time);
        
        new Thread(new Runnable()
        {
			@Override
            public void run()
            {
				while (true)
				{
					try
					{
						Thread.sleep(1000);
					}
					catch (Exception ex)
					{
						return;
					}
					if (scroll)
					{
						start_time = TimestampFactory.createTimestamp(start_time.seconds() + 1, start_time.nanoseconds());
						end_time = TimestampFactory.createTimestamp(end_time.seconds() + 1, end_time.nanoseconds());
						display.syncExec(new Runnable()
						{
							@Override
		                    public void run()
		                    {
						        plot.setTimeRange(start_time, end_time);
		                    }
						});
					}
				}
            }
        }, "Scroller").start();
    }

    @Test
    public void plotDemo()
    {
        final Shell shell = new Shell();
        shell.setSize(600, 500);

        createGUI(shell);
        shell.open();

        final Display display = Display.getDefault();
        while (run  &&  !shell.isDisposed())
        {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }
}
