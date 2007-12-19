package org.csstudio.display.pvtable.ui;

import java.util.Iterator;

import org.csstudio.apputil.ui.swt.AutoSizeColumn;
import org.csstudio.apputil.ui.swt.AutoSizeControlListener;
import org.csstudio.display.pvtable.Messages;
import org.csstudio.display.pvtable.Plugin;
import org.csstudio.display.pvtable.model.PVListEntry;
import org.csstudio.display.pvtable.model.PVListModel;
import org.csstudio.display.pvtable.model.PVListModelListener;
import org.csstudio.platform.model.IProcessVariable;
import org.csstudio.platform.ui.internal.dataexchange.ProcessVariableDragSource;
import org.csstudio.platform.ui.internal.dataexchange.ProcessVariableDropTarget;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;

/** Creates an Eclipse TableViewer hooked to a PVListModel.
 * 
 *  @author Kay Kasemir
 */
public class PVTableViewerHelper
{
    public final static String empty_row = Messages.EmptyRowMarker;
    
    private TableViewer table_viewer;
    private PVListModel pv_list;
    private PVListModelListener listener;

    private TableViewerUpdate table_viewer_update;
    
    private Action config_action, add_action, delete_action;
    private Action start_stop_action, snapshot_action, restore_action;
    private Action cut_action, copy_action, paste_action;
    private Clipboard clipboard;

    /** Helper for the asyn. update of the PV values in the table. */
    class TableViewerUpdate implements Runnable
    {
        // Now simply redraws the whole table.
        // Tried to redraw only changed entries, even only
        // those table columns that actually need a redraw.
        // Didn't help, on the contrary: It leads to many more checks,
        // and in the end, each column gets redrawn.
        // Only possible optimization: If all the properties sent to
        // update() are isLabelProperty(..)== false, in which case
        // nothing gets redrawn at all.
        
        //private String properties[];
        //private PVListEntry entry;
        
        public TableViewerUpdate()
        {
            //properties = new String[3];
            //properties[0] = PVTableHelper.properties[PVTableHelper.TIME];
            //properties[1] = PVTableHelper.properties[PVTableHelper.VALUE];
            //properties[2] = PVTableHelper.properties[PVTableHelper.READBACK_VALUE];
        }

        //public void setPVListEntry(PVListEntry entry)
        //{  this.entry = entry; }

        public void run()
        {   // Update table, unless this is a 'late' event
            // and we are already stopped.
            if (table_viewer.getTable().isDisposed())
                return;
            /*
            if (entry != null)
            {   // Update single item
                if (entry.isDisposed())
                    return;
                //table_viewer.update(entry, properties);
                table_viewer.update(entry, null);
            }
            else */
            // Update whole table
            table_viewer.refresh();
        }
    }
    
    /** Creates a TableViewer with context menu etc.
     *  for the PVListModel
     *  @param parent
     *  @param pv_list The PV list model to show in the table.
     */
    public PVTableViewerHelper(IWorkbenchPartSite site, Composite parent, PVListModel pv_list)
    {
        this.pv_list = pv_list;
        this.table_viewer_update = new TableViewerUpdate();
        Table table = new Table(parent,
                SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION
                | SWT.VIRTUAL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        for (int i = 0; i < PVTableHelper.properties.length; i++)
                AutoSizeColumn.make(table,
                        PVTableHelper.properties[i],
                        PVTableHelper.sizes[i],
                        PVTableHelper.weights[i]);
        // Configure table to auto-size the columns
        new AutoSizeControlListener(parent, table);
    
        table_viewer = new TableViewer(table);
        // Enable hashmap for resolving 'PVListEntry' to associated SWT widget.
        table_viewer.setUseHashlookup(true);
        table_viewer.setLabelProvider(new PVTableLabelProvider(table, pv_list));
        table_viewer.setContentProvider(
                new PVTableLazyContentProvider(table_viewer, pv_list));
        setTableViewerItemCount();
        
        // Allow editing.
        new PVTableCellModifier(table_viewer, pv_list);
        
        // Create Actions
        config_action = new ConfigAction(pv_list);
        add_action = new AddAction(this);
        delete_action = new DeleteAction(this);
        start_stop_action = new StartStopAction(pv_list);
        snapshot_action = new SnapshotAction(pv_list);
        restore_action = new RestoreAction(pv_list);
        cut_action = new CutAction(this);
        copy_action = new CopyAction(this);
        paste_action = new PasteAction(this);
        makeContextMenu(site);

        // Drag and drop
        new ProcessVariableDragSource(table_viewer.getTable(), table_viewer);
        new ProcessVariableDropTarget(table_viewer.getTable())
        {
            @Override
            public void handleDrop(IProcessVariable name,
                                   DropTargetEvent event)
            {
                getPVListModel().addPV(name.getName());
            }
        };
        
        // React to model changes
        listener = new PVListModelListener()
        {
            public void runstateChanged(boolean isRunning)
            { /* NOP */ }

            public void entriesChanged()
            {
                setTableViewerItemCount();
                table_viewer.refresh();
            }

            public void entryAdded(PVListEntry entry)
            {   entriesChanged(); }

            public void entryRemoved(PVListEntry entry)
            {   entriesChanged(); }

            public void valuesUpdated()
            {   // Update the whole table.
                // Note that this event arrives from a non-GUI thread!
                Display.getDefault().syncExec(table_viewer_update);
            }
        };
        pv_list.addModelListener(listener);
    }

    /** Update the item count. */
    private void setTableViewerItemCount()
    {
        // The +1 triggers the display of a final 'add new PV' row.
        // But that doesn't work when the model is running:
        // Any edit action is abruptly stopped when
        // new values get displayed.
        // Needs further though, for now disabled.
        table_viewer.setItemCount(pv_list.getEntryCount() /* + 1 */);
    }

    /** Connect actions to the global 'Edit' menu. */
    public void hookGlobalActions(IActionBars bars)
    {   // See plugin book p. 289
        bars.setGlobalActionHandler(ActionFactory.CUT.getId(), cut_action);
        bars.setGlobalActionHandler(ActionFactory.COPY.getId(), copy_action);
        bars.setGlobalActionHandler(ActionFactory.PASTE.getId(), paste_action);
        bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), delete_action);
    }
    
    /** @return The clipboard.
     *  See plugin book p. 290 and dispose()
     */
    public Clipboard getClipboard()
    {
        if (clipboard == null)
            clipboard = new Clipboard(Display.getCurrent());
        return clipboard;
    }

    public void dispose()
    {
        if (pv_list.isDisposed())
            System.err.println("PVTableViewerHelper was disposed after its PVListModel!"); //$NON-NLS-1$
        pv_list.removeModelListener(listener);
        pv_list.stop();
        if (clipboard != null)
        {
            clipboard.dispose();
            clipboard = null;
        }
    }
    
    /** @return Returns the TableViewer. */
    public TableViewer getTableViewer()
    {   return table_viewer;  }
    
    /** @return Returns the PVListModel. */
    public PVListModel getPVListModel()
    {   return pv_list;  }

    private void makeContextMenu(IWorkbenchPartSite site)
    {
        // See Plug-ins book p. 285
        MenuManager manager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        manager.add(add_action);
        manager.add(config_action);
        manager.add(cut_action);
        manager.add(copy_action);
        manager.add(paste_action);
        manager.add(delete_action);
        manager.add(new Separator());
        manager.add(start_stop_action);
        manager.add(snapshot_action);
        manager.add(restore_action);
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        Menu menu = manager.createContextMenu(table_viewer.getControl());
        table_viewer.getControl().setMenu(menu);
        site.registerContextMenu(manager, table_viewer);
    }

    /** @return Returns the PVs which are currently selected in the table. */
    @SuppressWarnings("unchecked")
    public PVListEntry[] getSelectedEntries()
    {
        final IStructuredSelection selection =
            (IStructuredSelection) table_viewer.getSelection();
        if (selection.isEmpty())
            return null;
        final int num = selection.size();
        final PVListEntry entries[] = new PVListEntry[num];
        int i = 0;
        for (Iterator iter = selection.iterator(); iter.hasNext();)
        {
            PVListEntry entry = (PVListEntry) iter.next();
            entries[i++] = entry;
            if (i > num)
            {
                Plugin.getLogger().error("Selection grew beyond " + num); //$NON-NLS-1$
                return null;
            }
        }
        return entries;
    }
}
