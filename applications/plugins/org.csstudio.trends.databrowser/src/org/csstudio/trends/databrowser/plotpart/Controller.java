package org.csstudio.trends.databrowser.plotpart;

import java.util.ArrayList;

import org.csstudio.apputil.time.RelativeTime;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.TimestampFactory;
import org.csstudio.platform.model.IArchiveDataSource;
import org.csstudio.platform.model.IProcessVariable;
import org.csstudio.platform.ui.internal.dataexchange.ProcessVariableOrArchiveDataSourceDropTarget;
import org.csstudio.swt.chart.Chart;
import org.csstudio.swt.chart.ChartListener;
import org.csstudio.swt.chart.Trace;
import org.csstudio.swt.chart.axes.Marker;
import org.csstudio.swt.chart.axes.XAxis;
import org.csstudio.swt.chart.axes.YAxis;
import org.csstudio.swt.chart.axes.YAxisListener;
import org.csstudio.trends.databrowser.Plugin;
import org.csstudio.trends.databrowser.model.IModelItem;
import org.csstudio.trends.databrowser.model.IPVModelItem;
import org.csstudio.trends.databrowser.model.MarkerInfo;
import org.csstudio.trends.databrowser.model.Model;
import org.csstudio.trends.databrowser.model.ModelListener;
import org.eclipse.swt.dnd.DropTargetEvent;

/** Data Browser Controller: Creates model, UI and handles everything between them.
 *  @author Kay Kasemir
 */ 
public class Controller implements ArchiveFetchJobListener
{
    final private static boolean debug_scroll = false;
    /** The model */
    final private Model model;
    /** GUI for the model. */
    final private BrowserUI gui;
    /** Shortcut to chart inside gui. */
    final private Chart chart;
    private ScannerAndScroller scanner_scroller;
    private boolean controller_changes_xaxis = false;
    private boolean controller_changes_yaxes = false;
    private boolean controller_changes_model = false;
    private boolean controller_changes_model_times = false;
    
    /** Start time of the plot used for scrolling, like "-10 minutes" */
    private String scroll_start_specification = null;
    
    /** Scan the PVs, and possibly redraw. */
    final private ScannerAndScrollerListener scanner_scroller_listener =
        new ScannerAndScrollerListener()
    {
        @SuppressWarnings("nls")
        public void scan(boolean with_redraw)
        {
            // 'Scan' the PVs
            model.scan();

            // Done, no scroll or redraw?
            if (!with_redraw  ||  !chart.isVisible())
                return;
            
            // Need to redraw, so update formulas
            model.updateFormulas();

            if (!model.isScrollEnabled())
            {   // redraw w/o scroll
                chart.redrawTraces();
                return;
            }
            // Scroll by updating the model's time range.
            // The plot should listen to the model and adjust its x axis.
            // But we listen as well, so avoid infinite loop.
            controller_changes_xaxis = true;
            try
            {
                if (scroll_start_specification == null)
                {   // Compute (relative) start time spec
                    double low = model.getStartTime().toDouble();
                    double high = model.getEndTime().toDouble();
                    setScrollStart(high - low);
                }
                else
                {
                    // Only update when really changed...
                    if (model.getEndSpecification().equals(RelativeTime.NOW)
                       && model.getStartSpecification().equals(scroll_start_specification))
                    {
                        model.updateStartEndTime();
                		gui.setTimeRange(model.getStartTime(), model.getEndTime());
                    }
                    else
                    {
                        if (debug_scroll)
                            System.out.println("Scroll: Update start "
                                               + scroll_start_specification);
                        controller_changes_model_times = true;
                        try
                        {
                        	model.setTimeSpecifications(scroll_start_specification,
                        			RelativeTime.NOW, true);
                        }
                        finally
                        {
                        	controller_changes_model_times = false;
                        }
                        // Looks like scrolling was just turned on, and we might jump
                        // from some old time range to 'now', so we better get new data.
                        getArchivedData(null);
                    }
                }
            }
            catch (Exception ex)
            {
                Plugin.getLogger().error("Cannot scroll", ex);
            }
            finally
            {
                controller_changes_xaxis = false;
            }
            // redraw is implied in the x axis update
        }
    };
    
    /** React to model changes by updating the chart,
     *  and possibly getting new archive data.
     */
    final private ModelListener model_listener = new ModelListener()
    {
        public void plotColorsChangedChanged()
        {
        	chart.setPlotBackground(model.getPlotBackground());
        	chart.setPlotForeground(model.getPlotForeground());
        	chart.setPlotGrid(model.getPlotGrid());
        	gui.redraw();
		}
        
        public void markersChanged()
        {
            // Ignore
        }

        public void timeSpecificationsChanged()
        {
        	if (controller_changes_model_times)
        		return;
            // Invalidate any scroll start spec that we might have
            scroll_start_specification = null;
            gui.updateScrollPauseButton(model.isScrollEnabled());
        }
        
        public void timeRangeChanged()
        {
            if (controller_changes_model_times ||
                controller_changes_xaxis)
        		return;
        	// Adjust the x axis to the "current" model time range
        	controller_changes_xaxis = true;
        	try
        	{
        		gui.setTimeRange(model.getStartTime(), model.getEndTime());
        		getArchivedData(null);
        	}
        	finally
        	{
        		controller_changes_xaxis = false;
        	}
        }
        
        public void samplingChanged()
        {
            if (scanner_scroller != null)
                scanner_scroller.periodsChanged();
        }

        public void entriesChanged()
        { 
            handleChangedModelEntries();
            getArchivedData(null);
        }

        public void entryAdded(IModelItem new_item)
        { 
            addToDisplay(new_item);
            getArchivedData(new_item);
        }

        public void entryConfigChanged(final IModelItem item)
        {   // Avoid infinite loops if we are changing the model ourselves
            //
            if (controller_changes_model)
                return;
            removeFromDisplay(item);
            if (removeUnusedAxes())
                handleChangedModelEntries();
            else
                addToDisplay(item);
        }

        public void entryMetaDataChanged(IModelItem item)
        {   // Display the new units on the Y Axis
            removeFromDisplay(item);
            addToDisplay(item);
        }
        
        public void entryArchivesChanged(IModelItem item)
        {   getArchivedData(item); }
        
        public void entryRemoved(IModelItem removed_item)
        {   
            removeFromDisplay(removed_item);
            if (removeUnusedAxes())
                handleChangedModelEntries();
        }
    };

    /** React to chart changes by updating the model. */
    final private ChartListener chart_listener = new ChartListener()
    {
        public void aboutToZoomOrPan(final String description)
        {
            gui.addCurrentZoomToUndoStack(description);
        }

        public void changedXAxis(final XAxis xaxis)
        {
            // Did the controller cause this?
            if (controller_changes_xaxis)
                return;
            // This is a user-driven pan or zoom.
            // Update the time range of the model.
            final double x0 = xaxis.getLowValue();
            final double x1 = xaxis.getHighValue();
            // Is the end close enough to 'now' to use relative times?
            final double range = x1 - x0;
            final double now = TimestampFactory.now().toDouble();
            // When scrolling, and not close to 'now', disable scroll
            // to prevent scrolling out of the selected range.
            // Criteria: End is within 10% of 'now' 
            final boolean close_to_now = Math.abs(x1 - now) < 0.1*range;
            try
            {
                controller_changes_model_times = true;
                if (close_to_now)
                {   // Set relative start/end times
                    setScrollStart(range);
                    model.setTimeSpecifications(scroll_start_specification,
                                                RelativeTime.NOW, true);
                }
                else
                {   // Set absolute start/end times
                    final ITimestamp start = TimestampFactory.fromDouble(x0);
                    final ITimestamp end = TimestampFactory.fromDouble(x1);
                    model.setTimeSpecifications(start.toString(), end.toString(),
                                                false);
                }
                gui.updateScrollPauseButton(model.isScrollEnabled());
            }
            catch (Exception ex)
            {
                Plugin.getLogger().error("Cannot update model time range", ex); //$NON-NLS-1$
            }
            finally
            {
                controller_changes_model_times = false;
            }
            // Trigger archive retrieval for new time range
            getArchivedData(null);
        }

        public void changedYAxis(final YAxisListener.Aspect what, final YAxis yaxis)
        {
            // Avoid infinite loop: We are changing the axes, so ignore.
            if (controller_changes_yaxes)
                return;
            switch (what)
            {
            case RANGE:
                // Range was changed interactively, update the model
                controller_changes_model = true;
                try
                {
	                int axis_index = chart.getYAxisIndex(yaxis);
	                Controller.this.model.setAxisLimits(axis_index,
	                                yaxis.getLowValue(),
	                                yaxis.getHighValue());
                }
                finally
                {
                	controller_changes_model = false;
                }
                break;
            case MARKER:
                // Update model with marker info.
                {
                    final int y_count = chart.getNumYAxes();
                    final MarkerInfo markers[][] = new MarkerInfo[y_count][];
                    for (int y=0; y<y_count; ++y)
                    {
                        final Marker[] ymarks = chart.getYAxis(y).getMarkers();
                        markers[y] = new MarkerInfo[ymarks.length];
                        for (int m=0; m<ymarks.length; ++m)
                        {
                            final Marker ymark = ymarks[m];
                            markers[y][m] = new MarkerInfo(ymark.getPosition(),
                                    ymark.getValue(), ymark.getText());
                        }
                    }
                    model.setMarkers(markers);
                }
                break;
            case LABEL:
            case SELECTION:
                // NOP
            }
        }

        public void pointSelected(final int x, final int y)
        { /* NOP */ }
    };
    
    /** List of currently active background jobs.
     *  When the last one finishes, we might adjust the chart
     *  to show all newly received samples.
     */
    private ArrayList<ArchiveFetchJob> jobs = new ArrayList<ArchiveFetchJob>();
    
    /** Construct controller.
     *  @param parent Parent widget (shell) under which the UI is created.
     */
    public Controller(Model model, BrowserUI gui, boolean allow_drop)
    {
        this.model = model;
        this.gui = gui;
        chart = gui.getInteractiveChart().getChart();    
    	chart.setPlotBackground(model.getPlotBackground());
        chart.addListener(chart_listener);
        model.addListener(model_listener);
        
        // Initialize GUI with the current model content.
        model_listener.entriesChanged();
        
        // Allow PV drops into the chart?
        if (allow_drop)
            new ProcessVariableOrArchiveDataSourceDropTarget(chart)
            {
                /** {@inheritDoc} */
                @Override
                public void handleDrop(IProcessVariable name, DropTargetEvent event)
                {   // Add item to axis (or new axis) with default archives
                    YAxis yaxis = chart.getYAxisAtScreenPoint(event.x, event.y);
                    IModelItem item = nameDropped(name.getName(), yaxis);
                    if (item instanceof IPVModelItem)
                    {
                        IPVModelItem pv_item = (IPVModelItem) item;
                        pv_item.useDefaultArchiveDataSources();
                    }
                }

                /** {@inheritDoc} */
                @Override
                public void handleDrop(IArchiveDataSource archive, DropTargetEvent event)
                {   // Add archive to model (all items)
                    Controller.this.model.addArchiveDataSource(archive);
                }

                /** {@inheritDoc} */
                @Override
                public void handleDrop(IProcessVariable name, IArchiveDataSource archive, DropTargetEvent event)
                { 
                    if (name.getName().length() <= 0)
                        return;
                    // Add item with source
                    YAxis yaxis = chart.getYAxisAtScreenPoint(event.x, event.y);
                    IModelItem item = nameDropped(name.getName(), yaxis);
                    if (item instanceof IPVModelItem)
                        ((IPVModelItem) item).addArchiveDataSource(archive);
                }

                @Override
                public void handleDrop(final String name, final DropTargetEvent event)
                {
                    if (name.length() <= 0)
                        return;
                    // Add item with source
                    YAxis yaxis = chart.getYAxisAtScreenPoint(event.x, event.y);
                    IModelItem item = nameDropped(name, yaxis);
                    if (item instanceof IPVModelItem)
                    {
                        IPVModelItem pv_item = (IPVModelItem) item;
                        pv_item.useDefaultArchiveDataSources();
                    }
                }
            };
    }
    
    /** Must be invoked for cleanup. */
    public void dispose()
    {
        // scanner_scroller stops when the chart is disposed
        chart.removeListener(chart_listener);
        model.removeListener(model_listener);
        model.stop();
        // Stop all pending jobs
        synchronized (jobs)
        {
            for (ArchiveFetchJob job : jobs)
                job.cancel();
            jobs.clear();
        }
    }
    
    /** Set the scroll_start_specification string for the given seconds */
    @SuppressWarnings("nls")
    private void setScrollStart(final double range_in_seconds)
    {
        // Use RelativeTime to normalize the seconds into hours, minutes, ...
        RelativeTime start = new RelativeTime(-range_in_seconds);
        scroll_start_specification = start.toString();
        if (debug_scroll)
            System.out.println("Scroll: New start " + scroll_start_specification);
    }
    
    /** Private handler for ..DropTarget interface */
    private IModelItem nameDropped(String name, YAxis yaxis)
    {
        // Catch duplicate PVs before the following code
        // creates new Y axis...
        if (model.findItem(name) != null)
            return null;
        if (yaxis == null)
        {   // If there is only one axis, and it's empty, use it:
            if (chart.getNumYAxes() == 1  &&
                chart.getYAxis(0).getNumTraces() < 1)
                yaxis = chart.getYAxis(0);
            else
                // Create new axis
                yaxis = chart.addYAxis(name);
        }
        return model.addPV(name, chart.getYAxisIndex(yaxis));
        // Model should now invoke entryAdded(), where we add it to the chart..
    }

    /** Re-populate all chart items because of overall model changes */
    private void handleChangedModelEntries()
    {
        // Avoid infinite loops if we are changing the model ourselves
        if (controller_changes_model)
            return;
        // Clear chart by removing all the traces
        while (chart.getNumTraces() > 0)
            chart.removeTrace(0);
        // .. and all but the first Y Axis.
        while (chart.getNumYAxes() > 1)
            chart.removeYAxis(chart.getNumYAxes()-1); // del. last axis
        // Add model data.
        for (int i=0; i<model.getNumItems(); ++i)
            addToDisplay(model.getItem(i));

        // Update markers in chart based on model.
        // Most of the time, the GUI will handle the markers
        // and model will track.
        // This code should mostly get invoked after the initial
        // Model load from a config file.
        // Set controller_changes_yaxes flag to avoid loops
        // when we update chart, then chart sends events to update
        // model which in turn ...
        controller_changes_yaxes = true;
        final MarkerInfo[][] markers = model.getMarkers();
        for (int y=0; y<markers.length; ++y)
        {
            if (y >= chart.getNumYAxes())
                continue;
            final YAxis axis = chart.getYAxis(y);
            axis.removeMarkers();
            for (MarkerInfo marker : markers[y])
                axis.addMarker(marker.toMarker());
        }
        controller_changes_yaxes = false;
    }
    
    /** Connect a model item to the display by adding it to the chart. */
    private void addToDisplay(final IModelItem new_item)
    {
        // Avoid infinite loops if we are changing the model ourselves
        if (controller_changes_model)
            return;
        // In case the item is invisible, don't actually add it to the plot...
        if (new_item.isVisible())
        {
            final int yaxis_index = new_item.getAxisIndex();
            // Assert that the chart has the requested Y Axis.
            while (yaxis_index >= chart.getNumYAxes())
                chart.addYAxis();
            // Add new model entry to the chart.
            // For the trace name, we use the item name plus its units.
            // Remember this when later trying to locate an item
            // by its trace name!
            final Trace trace = chart.addTrace(getTraceName(new_item),
                                         new_item.getSamples(),
                                         new_item.getColor(),
                                         new_item.getLineWidth(),
                                         yaxis_index,
                                         new_item.getTraceType());
            // Set initial axis range from model
            controller_changes_yaxes = true;
            try
            {
	            final YAxis yaxis = trace.getYAxis();
	            yaxis.setValueRange(new_item.getAxisLow(), new_item.getAxisHigh());
	            // Do we need to change the axis type?
	            if (new_item.isAxisVisible() != yaxis.isVisible())
	                yaxis.setVisible(new_item.isAxisVisible());
	            if (new_item.getLogScale() != yaxis.isLogarithmic())
	                yaxis.setLogarithmic(new_item.getLogScale());
	            if (new_item.getAutoScale() != yaxis.getAutoScale())
	                yaxis.setAutoScale(new_item.getAutoScale());
            }
            finally
            {
            	controller_changes_yaxes = false;
            }
        }
        // Model already running?
        if (model.isRunning())
            return;
        // else: Start the model
        model.start();
        scanner_scroller = new ScannerAndScroller(gui, model,
                                                  scanner_scroller_listener);
    }

    /** @return a trace name (item name plus units) for an item. */
    private String getTraceName(IModelItem item)
    {
        final String units = item.getUnits();
        if (units.length() > 0)
            return item.getDisplayName() + Messages.UnitMarkerStart
                                    + units + Messages.UnitMarkerEnd;
        // else
        return item.getDisplayName();
    }
    
    /** @return the item name for a trace. */
    private String getModelItemName(Trace trace)
    {
        // If there are units, chop them off
        String trace_name = trace.getName();
        int i = trace_name.indexOf(Messages.UnitMarkerStart);
        if (i < 0)
            return trace_name;
        return trace_name.substring(0, i);
    }
    
    private void removeFromDisplay(IModelItem removed_item)
    {
        for (int i=0; i<chart.getNumTraces(); ++i)
            if (getModelItemName(chart.getTrace(i))
                    .equals(removed_item.getDisplayName()))
            {
                chart.removeTrace(i);
                return;
            }
    }
    
    /** Remove unused axes
     *  @return <code>true</code> if this rearranged entries
     */
    private boolean removeUnusedAxes()
    {
        boolean anything_changed = false;
        if (model.getNumItems() > 0)
        {
            // Fill empty axis by moving items 'down'
            controller_changes_model = true;
            
            // Highest requested axis index
            int max_axis = getMaxAxisIndex();
            for (int y=0; y<max_axis;  ++y)
            {
                while (isAxisEmpty(y))
                    for (int y2=y+1; y2<=max_axis;  ++y2)
                        anything_changed |= moveItems(y2, y2-1);
                max_axis = getMaxAxisIndex();
            }
            controller_changes_model = false;
        }

        // Remove empty axes at end of list
        for (int y = chart.getNumYAxes()-1; y > 0; --y)
            if (chart.getYAxis(y).getNumTraces() < 1)
                chart.removeYAxis(y); // Drop empty axis
            else
                break; // Done, found used axis
        return anything_changed;
    }
    
    /** @return Highest uses axis index */
    private int getMaxAxisIndex()
    {
        int max_axis = 0;
        for (int i=0; i<model.getNumItems(); ++i)
            max_axis = Math.max(max_axis, model.getItem(i).getAxisIndex());
        return max_axis;
    }

    /** Any model items on this axis?
     *  @param y Axis index
     *  @return true if no model items use that axis
     */
    private boolean isAxisEmpty(final int y)
    {
        for (int i=0; i<model.getNumItems(); ++i)
            if (model.getItem(i).getAxisIndex() == y)
                return false;
        return true;
    }

    /** Move items from one axis to another
     *  @param old_y Original axis index
     *  @param new_y New axis index
     *  @return <code>true</code> if this rearranged other entries
     */
    private boolean moveItems(final int old_y, final int new_y)
    {
        boolean anything_changed = false;
        for (int i=0; i<model.getNumItems(); ++i)
        {
            final IModelItem item = model.getItem(i);
            if (item.getAxisIndex() == old_y)
            {
                item.setAxisIndex(new_y);
                anything_changed = true;
            }
        }
        return anything_changed;
    }

    /** Get data from archive for given model item,
     *  if it's an <code>IPVModelItem</code>,
     *  or all if <code>item==null</code>.
     */
    private void getArchivedData(final IModelItem item)
    {
        final ITimestamp start = model.getStartTime();
        final ITimestamp end = model.getEndTime();
        
        // System.out.println("Get archived data: " + start + " ... " + end);
        
        if (item == null)
        {
            // Request new data for all items because the overall time
            // range changed. First stop ongoing requests which are likely
            // for a previous, now-wrong time range
            synchronized (jobs)
            {
                for (ArchiveFetchJob job : jobs)
                {
                    // System.out.println("All new request cancels " + job);
                    job.cancel();
                }
                jobs.clear();
            }
            for (int i=0; i<model.getNumItems(); ++i)
            { 
                final IModelItem model_item = model.getItem(i);
                if (model_item instanceof IPVModelItem)
                    getArchivedData((IPVModelItem)model_item, start, end);
            }
        }
        else if (item instanceof IPVModelItem)
        {
            // Cancel jobs that are already running for this item
            synchronized (jobs)
            {
                for (int i=0; i<jobs.size(); ++i)
                {
                    final ArchiveFetchJob job = jobs.get(i);
                    if (job.getItem() != item)
                        continue;
                    // System.out.println("Request for " + item.getName() + " cancels " + job);
                    job.cancel();
                    jobs.remove(job);
                }
            }
            getArchivedData((IPVModelItem) item, start, end);
        }
    }

    /** Get data from archive for given model item and time range. */
    private void getArchivedData(final IPVModelItem item,
                                 final ITimestamp start,
                                 final ITimestamp end)
    {
        // Anything to fetch at all?
        if (item.getArchiveDataSources().length < 1)
            return;
        // Start background job to get data from archive
    	final ArchiveFetchJob job =
    	    new ArchiveFetchJob(chart.getShell(), item, start, end, this);
    	synchronized (jobs)
        {
            jobs.add(job);
        }
        job.schedule();
    }
    
    /** @see ArchiveFetchJobListener */
    public void fetchCompleted(final ArchiveFetchJob job)
    {
        synchronized (jobs)
        {
            jobs.remove(job);
            if (!jobs.isEmpty())
                return;
        }
        // This call could already happen after the chart was disposed...
        if (chart.isDisposed())
            return;
        // Then we switch to the UI thread, which could again run to the issue
        chart.getDisplay().asyncExec(new Runnable()
        {
            public void run()
            {
                if (chart.isDisposed())
                    return;
                switch(model.getArchiveRescale())
                {
                case AUTOZOOM:
                    chart.autozoom();
                    break;
                case STAGGER:
                    chart.stagger();
                    break;
                default:
                }
            }
        });
    }
}
