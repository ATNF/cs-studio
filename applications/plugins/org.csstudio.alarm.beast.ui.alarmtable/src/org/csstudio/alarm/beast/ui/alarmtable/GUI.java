/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.alarmtable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.csstudio.alarm.beast.AlarmTreeItem;
import org.csstudio.alarm.beast.AlarmTreePV;
import org.csstudio.alarm.beast.ui.AlarmPVDragSource;
import org.csstudio.alarm.beast.ui.AlarmPerspectiveAction;
import org.csstudio.alarm.beast.ui.ConfigureItemAction;
import org.csstudio.alarm.beast.ui.ContextMenuHelper;
import org.csstudio.alarm.beast.ui.Messages;
import org.csstudio.alarm.beast.ui.SeverityColorProvider;
import org.csstudio.alarm.beast.ui.alarmtable.AlarmTableLabelProvider.ColumnInfo;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelListener;
import org.csstudio.apputil.text.RegExHelper;
import org.csstudio.platform.ui.swt.AutoSizeColumn;
import org.csstudio.platform.ui.swt.AutoSizeColumnAction;
import org.csstudio.platform.ui.swt.AutoSizeControlListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPartSite;

/** Alarm table GUI
 *  @author Kay Kasemir
 */
public class GUI implements AlarmClientModelListener
{
    final private Display display;

    /** Model with all the alarm information */
    final private AlarmClientModel model;

    /** TableViewer for active alarms */
    private TableViewer active_table_viewer;

    /** TableViewer for acknowledged alarms */
    private TableViewer acknowledged_table_viewer;

    /** PV selection filter text box */
    private Text filter;

    /** PV un-select button */
    private Button unselect;

    private SeverityColorProvider color_provider;

    /** Is something displayed in <code>error_message</code>? */
    private volatile boolean have_error_message = false;

    /** Error message (no server...) */
    private Label error_message;

    /** GUI updates are throttled to reduce flicker */
    final private GUIUpdateThrottle gui_update =
        new GUIUpdateThrottle(Preferences.getInitialMillis(),
                              Preferences.getSuppressionMillis())
    {
        @Override
        protected void fire()
        {
            if (display.isDisposed())
                return;
            display.asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    //System.out.println("GUI Update");
                    final Table act_table = active_table_viewer.getTable();
                    if (act_table.isDisposed())
                        return;
                    // Don't use TableViewer.setInput(), it causes flicker on Linux!
                    // active_table_viewer.setInput(model.getActiveAlarms());
                    // acknowledged_table_viewer.setInput(model.getAcknowledgedAlarms());
                    //
                    // Instead, tell ModelInstanceProvider about the data,
                    // which then updates the table with setItemCount(), refresh(),
                    // as that happens to not flicker.
                    ((AlarmTableContentProvider)
                        active_table_viewer.getContentProvider()).setAlarms(model.getActiveAlarms());
                    ((AlarmTableContentProvider)
                        acknowledged_table_viewer.getContentProvider()).setAlarms(model.getAcknowledgedAlarms());
                }
            });
        }
    };

    /** Initialize GUI
     *  @param parent Parent widget
     *  @param model Alarm model
     *  @param site Workbench site or <code>null</code>
     */
    public GUI(final Composite parent, final AlarmClientModel model,
            final IWorkbenchPartSite site)
    {
        display = parent.getDisplay();
        this.model = model;
        createComponents(parent);

        if (!model.isServerAlive())
            setErrorMessage(Messages.WaitingForServer);
        // Subscribe to model updates, arrange to un-subscribe
        model.addListener(this);
        parent.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                model.removeListener(GUI.this);
                gui_update.dispose();
            }
        });

        connectContextMenu(active_table_viewer, site);
        connectContextMenu(acknowledged_table_viewer, site);
        // Allow 'drag' of alarm info as text
        new AlarmPVDragSource(active_table_viewer.getTable(), getSelectedAlarms());
        new AlarmPVDragSource(acknowledged_table_viewer.getTable(), getSelectedAckAlarms());
    }

    /** @return Provider for selected active alarms */
    public ISelectionProvider getSelectedAlarms()
    {
        return active_table_viewer;
    }

    /** @return Provider for selected acknowledged alarms */
    public ISelectionProvider getSelectedAckAlarms()
    {
        return acknowledged_table_viewer;
    }

    /** Create GUI elements
     *  @param parent Parent widget
     */
    private void createComponents(final Composite parent)
    {
        parent.setLayout(new FillLayout());
        SashForm sash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
        sash.setLayout(new FillLayout());

        color_provider = new SeverityColorProvider(parent);

        addActiveAlarmSashElement(sash);
        addAcknowledgedAlarmSashElement(sash);

        sash.setWeights(new int[] { 80, 20 });

        // Update selection in active & ack'ed alarm table
        // in response to filter changes
        filter.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e)
            {
                final String filter_text = filter.getText().trim();
                selectFilteredPVs(filter_text, active_table_viewer);
                selectFilteredPVs(filter_text, acknowledged_table_viewer);
            }
        });
        // Clear filter, un-select all items
        unselect.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                filter.setText(""); //$NON-NLS-1$
                active_table_viewer.setSelection(null, true);
                acknowledged_table_viewer.setSelection(null, true);
            }
        });

        gui_update.start();
    }

    /** Add the sash element for active alarms
     *  @param sash SashForm
     */
    private void addActiveAlarmSashElement(final SashForm sash)
    {
        final Composite box = new Composite(sash, SWT.BORDER);
        final GridLayout layout = new GridLayout();
        layout.numColumns = 5;
        box.setLayout(layout);

        GridData gd;

        // Current Alarms {Error}   Select: ___ filter ___ [X]
        Label l = new Label(box, 0);
        l.setText(Messages.CurrentAlarms);
        l.setLayoutData(new GridData());

        error_message = new Label(box, 0);
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        gd.grabExcessHorizontalSpace = true;
        error_message.setLayoutData(gd);

        l = new Label(box, 0);
        l.setText(Messages.Filter);
        l.setLayoutData(new GridData());

        filter = new Text(box, SWT.BORDER);
        filter.setToolTipText(Messages.FilterTT);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        filter.setLayoutData(gd);

        unselect = new Button(box, SWT.PUSH);
        unselect.setText(Messages.Unselect);
        unselect.setToolTipText(Messages.UnselectTT);
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        unselect.setLayoutData(gd);

        // Table w/ active alarms
        active_table_viewer = createAlarmTable(box);
        active_table_viewer.setInput(null);
        ((AlarmTableContentProvider)
            active_table_viewer.getContentProvider()).setAlarms(model.getActiveAlarms());
    }

    /** Add the sash element for acknowledged alarms
     *  @param sash SashForm
     */
    private void addAcknowledgedAlarmSashElement(final SashForm sash)
    {
        final Composite box = new Composite(sash, SWT.BORDER);
        box.setLayout(new GridLayout());

        // Ack'ed alarms
        Label l = new Label(box, 0);
        l.setText(Messages.AcknowledgedAlarms);
        l.setLayoutData(new GridData());

        // Table w/ ack'ed alarms
        acknowledged_table_viewer = createAlarmTable(box);
        acknowledged_table_viewer.setInput(null);
        ((AlarmTableContentProvider)
            acknowledged_table_viewer.getContentProvider()).setAlarms(model.getAcknowledgedAlarms());
    }

    /** Select PVs in table that match filter expression
     *  @param filter_text Filter expression ('vac', 'amp*trip')
     *  @param table_viewer Table in which to select PVs
     */
    private void selectFilteredPVs(final String filter_text,
                                   final TableViewer table_viewer)
    {
        final Pattern pattern =
            Pattern.compile(RegExHelper.fullRegexFromGlob(filter_text),
                            Pattern.CASE_INSENSITIVE);
        final AlarmTreePV pvs[] =
            ((AlarmTableContentProvider) table_viewer.getContentProvider()).getAlarms();
        final ArrayList<AlarmTreePV> selected =
            new ArrayList<AlarmTreePV>();
        for (AlarmTreePV pv : pvs)
        {
            if (pattern.matcher(pv.getName()).matches()  ||
                pattern.matcher(pv.getDescription()).matches())
                selected.add(pv);
        }
        table_viewer.setSelection(new StructuredSelection(selected), true);
    }

    /** Create a table viewer for displaying alarms
     *  @param parent Parent widget, uses GridLayout
     *  @return TableViewer, still needs input
     */
    private TableViewer createAlarmTable(final Composite parent)
    {
        final GridLayout layout = (GridLayout) parent.getLayout();
        final TableViewer table_viewer = new TableViewer(parent,
                SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);

        // Some tweaks to the underlying table widget
        final Table table = table_viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gd = new GridData();
        gd.horizontalSpan = layout.numColumns;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        table.setLayoutData(gd);

        ColumnViewerToolTipSupport.enableFor(table_viewer, ToolTip.NO_RECREATE);

        // Connect TableViewer to the Model: Provide content from model...
        table_viewer.setContentProvider(new AlarmTableContentProvider());

        // Create the columns of the table, using a fixed initial width.
        for (AlarmTableLabelProvider.ColumnInfo col_info
                                : AlarmTableLabelProvider.ColumnInfo.values())
        {
            final TableViewerColumn view_col =
                AutoSizeColumn.make(table_viewer, col_info.getTitle(),
                        col_info.getMinWidth(), col_info.getWeight());
            // Tell column how to display the model elements
            view_col.setLabelProvider(new AlarmTableLabelProvider(table,
                                                   color_provider, col_info));
            final TableColumn table_col = view_col.getColumn();

            final AlarmColumnSortingSelector sel_listener =
                new AlarmColumnSortingSelector(table_viewer, table_col, col_info);
            table_col.addSelectionListener(sel_listener);
            // Sort on severity right away
            if (col_info == ColumnInfo.SEVERITY)
                sel_listener.widgetSelected(null);
        }
        // Logically we would add the AutoSizeControlListener() here to
        // auto-size the columns, but we need it later to create the
        // context menu action, so that's done in connectContextMenu()
        return table_viewer;
    }

    /** Add context menu to tree
     *  @param table_viewer TableViewer to which to add the menu
     *  @param site Workbench site or <code>null</code>
     */
    private void connectContextMenu(final TableViewer table_viewer,
            final IWorkbenchPartSite site)
    {
        final Table table = table_viewer.getTable();

        final Action auto_size =
            new AutoSizeColumnAction(new AutoSizeControlListener(table, true));

        final MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener()
        {
            // Dynamically build menu based on current selection
            @Override
            @SuppressWarnings("unchecked")
            public void menuAboutToShow(IMenuManager manager)
            {
                final Shell shell = table.getShell();
                final List<AlarmTreeItem> items =
                    ((IStructuredSelection)table_viewer.getSelection()).toList();
                new ContextMenuHelper(manager, shell, items, model.isWriteAllowed());
                manager.add(new Separator());
                // Placeholder for CSS PV contributions
                manager.add(new GroupMarker("additions")); //$NON-NLS-1$
                manager.add(new Separator());
                // Add edit items
                if (items.size() == 1 && model.isWriteAllowed())
                {
                    final AlarmTreeItem item = items.get(0);
                    manager.add(new ConfigureItemAction(shell, model, item));
                }
                manager.add(auto_size);
                manager.add(new AlarmPerspectiveAction());
            }
        });
        table.setMenu(manager.createContextMenu(table));

        // Allow extensions to add to the context menu
        if (site != null)
            site.registerContextMenu(manager, table_viewer);
    }

    /** Set or clear error message.
     *  Setting an error message also disables the GUI.
     *  <p>
     *  OK to call multiple times or after disposal.
     *  @param error Error message or <code>null</code> to clear error
     */
    public void setErrorMessage(final String error)
    {
        final Table act_table = active_table_viewer.getTable();
        if (act_table.isDisposed())
            return;
        if (error == null)
        {
            if (! have_error_message)
                return; // msg already cleared, GUI already enabled
            error_message.setText(""); //$NON-NLS-1$
            error_message.setBackground(null);
            act_table.setEnabled(true);
            acknowledged_table_viewer.getTable().setEnabled(true);
            have_error_message = false;
        }
        else
        {   // Update the message
            error_message.setText(error);
            error_message.setBackground(display.getSystemColor(SWT.COLOR_MAGENTA));
            error_message.getParent().layout();
            if (have_error_message)
                return; // GUI already disabled
            act_table.setEnabled(false);
            acknowledged_table_viewer.getTable().setEnabled(false);
            have_error_message = true;
        }
    }

    // @see AlarmClientModelListener
    @Override
    public void serverModeUpdate(AlarmClientModel model, boolean maintenanceMode)
    {
        // Ignored
    }

    // @see AlarmClientModelListener
    @Override
    public void serverTimeout(final AlarmClientModel model)
    {
        display.asyncExec(new Runnable()
        {
            @Override
            public void run()
            {
                setErrorMessage(Messages.ServerTimeout);
            }
        });
    }

    // For now, the table responds to any changes with a full update
    // @see AlarmClientModelListener
    @Override
    public void newAlarmConfiguration(final AlarmClientModel model)
    {
        gui_update.trigger();
    }

    // @see AlarmClientModelListener
    @Override
    public void newAlarmState(final AlarmClientModel model,
            final AlarmTreePV pv)
    {
        gui_update.trigger();
        if (model.isServerAlive() && have_error_message)
        {   // Clear error message now that we have info from the alarm server
            display.asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    setErrorMessage(null);
                }
            });
        }
    }
}
