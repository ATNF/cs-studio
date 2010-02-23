package org.csstudio.trends.databrowser.ui;

import java.util.List;

import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.model.IArchiveDataSource;
import org.csstudio.platform.model.IProcessVariable;
import org.csstudio.platform.ui.internal.dataexchange.ProcessVariableOrArchiveDataSourceDropTarget;
import org.csstudio.swt.xygraph.figures.Axis;
import org.csstudio.swt.xygraph.figures.IAxisListener;
import org.csstudio.swt.xygraph.figures.ToolbarArmedXYGraph;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.figures.XYGraphFlags;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.csstudio.swt.xygraph.undo.OperationsManager;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory;
import org.csstudio.trends.databrowser.Messages;
import org.csstudio.trends.databrowser.model.AxisConfig;
import org.csstudio.trends.databrowser.model.Model;
import org.csstudio.trends.databrowser.model.ModelItem;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.jface.action.IAction;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

/** Data Browser 'Plot' that displays the samples in a {@link Model}.
 *  <p>
 *  Underlying XYChart is a Draw2D Figure.
 *  Plot helps with linking that to an SWT Canvas.
 *   
 *  @author Kay Kasemir
 */
public class Plot
{
    /** Plot Listener */
    private PlotListener listener = null;
    
    /** {@link Display} used by this plot */
    final private Display display;

    /** Color registry, mapping {@link RGB} values to {@link Color} */
    final private ColorRegistry colormap;

    /** Font applied to axes */
    final private Font axis_font;

    /** Font applied to axes' titles */
    final private Font axis_title_font;
    
    /** Plot widget/figure */
    final private ToolbarArmedXYGraph plot;

    /** XYGraph inside <code>plot</code> */
    final private XYGraph xygraph;

    /** Button to enable/disable scrolling */
    final private ScrollButton scroll_button;
    
    /** Flag to suppress XYGraph events when the plot itself
     *  changes the time axis
     */
    private boolean plot_changes_timeaxis = false;

    /** Flag to suppress XYGraph events when the plot itself
     *  changes a value axis
     */
    private boolean plot_changes_valueaxis = false;

    private TimeConfigButton time_config_button;

    
    /** Initialize plot inside Canvas
     *  @param canvas Parent {@link Canvas}
     */
    public Plot(final Canvas canvas)
    {
        // Arrange for color disposal
        display = canvas.getDisplay();
        colormap = new ColorRegistry(canvas);

        // Use system font for axis labels
        axis_font = display.getSystemFont();
        
        // Use BOLD version for axis title
        final FontData fds[] = axis_font.getFontData();
        for (FontData fd : fds)
            fd.setStyle(SWT.BOLD);
        axis_title_font = XYGraphMediaFactory.getInstance().getFont(fds);
        
        // Create plot with basic configuration
        final LightweightSystem lws = new LightweightSystem(canvas);
        plot = new ToolbarArmedXYGraph(new XYGraph(),
                XYGraphFlags.SEPARATE_ZOOM | XYGraphFlags.STAGGER);
        xygraph = plot.getXYGraph();
        xygraph.setTransparent(false);
        
        scroll_button = new ScrollButton(xygraph.getOperationsManager());
        plot.addToolbarButton(scroll_button);
        
        time_config_button = new TimeConfigButton();
        plot.addToolbarButton(time_config_button);
        
        // Configure axes
        final Axis time_axis = xygraph.primaryXAxis;
        time_axis.setDateEnabled(true);
        time_axis.setTitle(Messages.Plot_TimeAxisName);
        time_axis.setFont(axis_font);
        time_axis.setTitleFont(axis_title_font);
        xygraph.primaryYAxis.setTitle(Messages.Plot_ValueAxisName);
        xygraph.primaryYAxis.setFont(axis_font);
        xygraph.primaryYAxis.setTitleFont(axis_title_font);
        
        // Forward time axis changes from the GUI to PlotListener
        // (Ignore changes from setTimeRange)
        time_axis.addListener(new IAxisListener()
        {
            public void axisRevalidated(final Axis axis)
            {
                // NOP
            }
            
            public void axisRangeChanged(final Axis axis, final Range old_range, final Range new_range)
            {   // Check that it's not caused by ourself, and a real change
                if (!plot_changes_timeaxis  &&
                    !old_range.equals(new_range)  &&
                    listener != null)
                    listener.timeAxisChanged((long)new_range.getLower(),
                                             (long)new_range.getUpper());
            }
        });
        
        xygraph.primaryYAxis.addListener(createValueAxisListener(0));
        
        lws.setContents(plot);
        
        // Allow drag & drop of names into the plot
//        new TextAsPVDropTarget(canvas)
//        {
//            @Override
//            public void handleDrop(final String name)
//            {
//                if (listener != null)
//                    listener.droppedPVName(name);
//            }
//        };
        new ProcessVariableOrArchiveDataSourceDropTarget(canvas)
        {
            
            @Override
            public void handleDrop(final IProcessVariable name,
                    final IArchiveDataSource archive, final DropTargetEvent event)
            {
                if (listener != null)
                    listener.droppedPVName(name.getName(), archive);
            }
            
            @Override
            public void handleDrop(final IArchiveDataSource archive, final DropTargetEvent event)
            {
                if (listener != null)
                    listener.droppedPVName(null, archive);
            }
            
            @Override
            public void handleDrop(final IProcessVariable name, final DropTargetEvent event)
            {
                handleDrop(name, null, event);
            }
            
            @Override
            public void handleDrop(final String name, final DropTargetEvent event)
            {
                if (listener != null)
                    listener.droppedName(name);
            }
        };
    }
    
    /** Add a listener (currently only one supported) */
    public void addListener(final PlotListener listener)
    {
        if (this.listener != null)
            throw new IllegalStateException();
        this.listener = listener;
        scroll_button.addPlotListener(listener);
        time_config_button.addPlotListener(listener);
    }
   
    /** @return Operations manager for undo/redo */
    public OperationsManager getOperationsManager()
    {
        return xygraph.getOperationsManager();
    }

    /** @return Action to show/hide the tool bar */
    public IAction getToggleToolbarAction()
    {
        return new ToggleToolbarAction(plot);
    }
    
    /** Remove all axes and traces */
    public void removeAll()
    {
        // Remove all traces
        int N = xygraph.getPlotArea().getTraceList().size();
        while (N > 0)
            xygraph.removeTrace(xygraph.getPlotArea().getTraceList().get(--N));
        // Now that Y axes are unused, remove all except for primary
        N = xygraph.getYAxisList().size();
        while (N > 1)
            xygraph.removeAxis(xygraph.getYAxisList().get(--N));
    }

    /** @param index Index of Y axis. If it doesn't exist, it will be created.
     *  @return Y Axis
     */
    private Axis getYAxis(final int index)
    {
        // Get Y Axis, creating new ones if needed
        final List<Axis> axes = xygraph.getYAxisList();
        while (axes.size() <= index)
        {
            final int new_axis_index = axes.size();
            final Axis axis = new Axis(NLS.bind(Messages.Plot_ValueAxisNameFMT, new_axis_index + 1), true);
            axis.setFont(axis_font);
            axis.setTitleFont(axis_title_font);
            xygraph.addAxis(axis);
            axis.addListener(createValueAxisListener(new_axis_index));
        }
        return axes.get(index);
    }
    
    /** Create value axis listener
     *  @param index Index of the axis, 0 ...
     *  @return IAxisListener
     */
    private IAxisListener createValueAxisListener(final int index)
    {
        return new IAxisListener()
        {
            public void axisRevalidated(Axis axis)
            {
                // NOP
            }
            
            public void axisRangeChanged(final Axis axis, final Range old_range, final Range new_range)
            {
                if (plot_changes_valueaxis ||
                    old_range.equals(new_range) ||
                    listener == null)
                    return;
                listener.valueAxisChanged(index, new_range.getLower(), new_range.getUpper());
            }
        };
    }

    /** Update configuration of axis
     *  @param index Axis index. Y axes will be created as needed.
     *  @param config Desired axis configuration
     */
    public void updateAxis(final int index, final AxisConfig config)
    {
        final Axis axis = getYAxis(index);
        axis.setTitle(config.getName());
        axis.setForegroundColor(colormap.getColor(config.getColor()));
        plot_changes_valueaxis = true;
        axis.setRange(config.getMin(), config.getMax());
        axis.setLogScale(config.isLogScale());
        axis.setAutoScale(config.isAutoScale());
        plot_changes_valueaxis = false;
    }

    /** Add a trace to the XYChart
     *  @param item ModelItem for which to add a trace
     */
    public void addTrace(final ModelItem item)
    {
        final Axis xaxis = xygraph.primaryXAxis;
        final Axis yaxis = getYAxis(item.getAxisIndex());
        final Trace trace = new Trace(item.getDisplayName(),
               xaxis, yaxis, item.getSamples());
        trace.setPointStyle(PointStyle.NONE);
        setTraceType(item, trace);
        trace.setTraceColor(colormap.getColor(item.getColor()));
        trace.setLineWidth(item.getLineWidth());
        xygraph.addTrace(trace);
    }

    /** Configure the XYGraph Trace's 
     *  @param item ModelItem whose Trace Type combines the basic line type
     *              and the error bar display settings
     *  @param trace Trace to configure
     */
    private void setTraceType(final ModelItem item, final Trace trace)
    {
        switch (item.getTraceType())
        {
        case AREA:
            // None of these seem to cause an immediate redraw, so
            // don't bother to check for changes
            trace.setTraceType(TraceType.STEP_HORIZONTALLY);
            trace.setPointStyle(PointStyle.NONE);
            trace.setErrorBarEnabled(true);
            trace.setDrawYErrorInArea(true);
            break;
        case ERROR_BARS:
            trace.setTraceType(TraceType.STEP_HORIZONTALLY);
            trace.setPointStyle(PointStyle.NONE);
            trace.setErrorBarEnabled(true);
            trace.setDrawYErrorInArea(false);
            break;
        case SINGLE_LINE:
            trace.setTraceType(TraceType.STEP_HORIZONTALLY);
            trace.setPointStyle(PointStyle.NONE);
            trace.setErrorBarEnabled(false);
            trace.setDrawYErrorInArea(false);
            break;
        case SQUARES:
            trace.setTraceType(TraceType.POINT);
            trace.setPointStyle(PointStyle.FILLED_SQUARE);
            trace.setPointSize(item.getLineWidth());
            trace.setErrorBarEnabled(false);
            trace.setDrawYErrorInArea(false);
            break;
        case CIRCLES:
            trace.setTraceType(TraceType.POINT);
            trace.setPointStyle(PointStyle.CIRCLE);
            trace.setPointSize(item.getLineWidth());
            trace.setErrorBarEnabled(false);
            trace.setDrawYErrorInArea(false);
            break;
        case CROSSES:
            trace.setTraceType(TraceType.POINT);
            trace.setPointStyle(PointStyle.XCROSS);
            trace.setPointSize(item.getLineWidth());
            trace.setErrorBarEnabled(false);
            trace.setDrawYErrorInArea(false);
            break;
        case DIAMONDS:
            trace.setTraceType(TraceType.POINT);
            trace.setPointStyle(PointStyle.FILLED_DIAMOND);
            trace.setPointSize(item.getLineWidth());
            trace.setErrorBarEnabled(false);
            trace.setDrawYErrorInArea(false);
            break;
        case TRIANGLES:
            trace.setTraceType(TraceType.POINT);
            trace.setPointStyle(PointStyle.FILLED_TRIANGLE);
            trace.setPointSize(item.getLineWidth());
            trace.setErrorBarEnabled(false);
            trace.setDrawYErrorInArea(false);
            break;
        }
    }

    /** Remove a trace from the XYChart
     *  @param item ModelItem to remove
     */
    public void removeTrace(final ModelItem item)
    {
        final Trace trace = findTrace(item);
        if (trace == null)
            throw new RuntimeException("No trace for " + item.getName()); //$NON-NLS-1$
        xygraph.removeTrace(trace);
    }

    /** Update the configuration of a trace from Model Item
     *  @param item Item that was previously added to the Plot
     */
    public void updateTrace(final ModelItem item)
    {
        final Trace trace = findTrace(item);
        if (trace == null)
            throw new RuntimeException("No trace for " + item.getName()); //$NON-NLS-1$
        // Update Trace with item's configuration
        if (!trace.getName().equals(item.getDisplayName()))
            trace.setName(item.getDisplayName());
        // These happen to not cause an immediate redraw, so
        // set even if no change
        trace.setTraceColor(colormap.getColor(item.getColor()));
        trace.setLineWidth(item.getLineWidth());
        setTraceType(item, trace);
        
        // Locate index of current Y Axis
        final Axis axis = trace.getYAxis();
        final List<Axis> yaxes = xygraph.getYAxisList();
        int axis_index = 0;
        for (/**/; axis_index < yaxes.size(); ++axis_index)
            if (axis == yaxes.get(axis_index))
                break;
        final int desired_axis = item.getAxisIndex();
        // Change to desired Y Axis?
        if (axis_index != desired_axis  &&  desired_axis < yaxes.size())
            trace.setYAxis(yaxes.get(desired_axis));
    }

    /** @param item ModelItem for which to locate the {@link Trace}
     *  @return Trace
     *  @throws RuntimeException on error
     */
    @SuppressWarnings("nls")
    private Trace findTrace(ModelItem item)
    {
        final List<Trace> traces = xygraph.getPlotArea().getTraceList();
        for (Trace trace : traces)
            if (trace.getDataProvider() == item.getSamples())
                return trace;
        throw new RuntimeException("Cannot locate trace for " + item);
    }

    /** Update plot to given time range.
     * 
     *  Can be called from any thread.
     *  @param start_ms Milliseconds since 1970 for start time
     *  @param end_ms ... end time
     */
    public void setTimeRange(final long start_ms, final long end_ms)
    {
        display.asyncExec(new Runnable()
        {
            public void run()
            {
                plot_changes_timeaxis = true;
                xygraph.primaryXAxis.setRange(start_ms, end_ms);
                plot_changes_timeaxis = false;
            }
        });
    }

    /** Update plot to given time range.
     *  @param start Start time
     *  @param end   End time
     */
    public void setTimeRange(final ITimestamp start, final ITimestamp end)
    {
        final double start_ms = start.toDouble()*1000;
        final double end_ms = end.toDouble()*1000;
        plot_changes_timeaxis = true;
        xygraph.primaryXAxis.setRange(start_ms, end_ms);
        plot_changes_timeaxis = false;
    }

    /** Update the scroll button 
     *  @param on <code>true</code> when scrolling is 'on'
     */
    public void updateScrollButton(final boolean scroll_on)
    {
        scroll_button.setButtonState(scroll_on);
    }

    /** Update Y axis auto-scale */
    public void updateAutoscale()
    {
        display.asyncExec(new Runnable()
        {
            public void run()
            {
                System.out.println("Autoscale?");
                for (Axis yaxis : xygraph.getYAxisList())
                    yaxis.performAutoScale(false);
            }
        });
    }
    
    /** Refresh the plot because the data has changed */
    public void redrawTraces()
    {
        display.asyncExec(new Runnable()
        {
            public void run()
            {
                for (Axis yaxis : xygraph.getYAxisList())
                    yaxis.performAutoScale(false);
                xygraph.revalidate();
            }
        });
    }

    /** @param color New background color */
    public void setBackgroundColor(final RGB color)
    {
        xygraph.getPlotArea().setBackgroundColor(colormap.getColor(color));
    }

    public XYGraph getXYGraph()
    {
        return xygraph;
    }
}
