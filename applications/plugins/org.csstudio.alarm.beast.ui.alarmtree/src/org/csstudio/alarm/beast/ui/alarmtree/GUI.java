/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.alarmtree;

import java.util.List;

import org.csstudio.alarm.beast.client.AlarmTreeItem;
import org.csstudio.alarm.beast.client.AlarmTreePV;
import org.csstudio.alarm.beast.client.AlarmTreePosition;
import org.csstudio.alarm.beast.client.AlarmTreeRoot;
import org.csstudio.alarm.beast.ui.AlarmPVDragSource;
import org.csstudio.alarm.beast.ui.ContextMenuHelper;
import org.csstudio.alarm.beast.ui.Messages;
import org.csstudio.alarm.beast.ui.actions.AddComponentAction;
import org.csstudio.alarm.beast.ui.actions.AlarmPerspectiveAction;
import org.csstudio.alarm.beast.ui.actions.ConfigureItemAction;
import org.csstudio.alarm.beast.ui.actions.DuplicatePVAction;
import org.csstudio.alarm.beast.ui.actions.MoveItemAction;
import org.csstudio.alarm.beast.ui.actions.RemoveComponentAction;
import org.csstudio.alarm.beast.ui.actions.RenameItemAction;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelListener;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;

/** GUI for the alarm tree viewer
 *  @author Kay Kasemir
 */
public class GUI implements AlarmClientModelListener
{
    /** Model for this GUI */
    final private AlarmClientModel model;

    /** Error message.
     *  @see #setErrorMessage(String)
     */
    private Label error_message;

    /** Tree */
    private TreeViewer tree_viewer;

    /** Show only alarms, or all items? */
    private boolean show_only_alarms;

    /** Initialize GUI
     *  @param parent SWT parent
     *  @param model AlarmClientModel to display in GUI
     *  @param site Workbench site or <code>null</code>
     */
    public GUI(final Composite parent, final AlarmClientModel model,
            final IWorkbenchPartSite site)
    {
        this.model = model;
        createGUI(parent);

        if (model.isServerAlive())
            setErrorMessage(null);
        else
            setErrorMessage(Messages.WaitingForServer);

        // Subscribe to model updates, arrange to un-subscribe
        model.addListener(this);
        parent.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                model.removeListener(GUI.this);
            }
        });

        connectContextMenu(site);
        // Allow 'drag' of alarm info as text
        new AlarmPVDragSource(tree_viewer.getTree(), tree_viewer);
    }

    /** Create the GUI elements */
    private void createGUI(final Composite parent)
    {
        parent.setLayout(new FormLayout());

        // Error label in top-right
        error_message = new Label(parent, 0);
        error_message.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_MAGENTA));
        FormData fd = new FormData();
        fd.top = new FormAttachment(0, 0);
        fd.right = new FormAttachment(100, 0);
        error_message.setLayoutData(fd);

        // Tree below the error label, filling the rest
        tree_viewer = new TreeViewer(parent,
                SWT.VIRTUAL | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL |
                SWT.BORDER | SWT.FULL_SELECTION);
        final Tree tree = tree_viewer.getTree();
        fd = new FormData();
        fd.top = new FormAttachment(error_message);
        fd.left = new FormAttachment(0, 0);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        tree.setLayoutData(fd);

        // Connect tree viewer to data model
        tree_viewer.setUseHashlookup(true);
        tree_viewer.setContentProvider(new AlarmTreeContentProvider(this));
        tree_viewer.setLabelProvider(new AlarmTreeLabelProvider(tree));
        tree_viewer.setInput(model.getConfigTree());

        ColumnViewerToolTipSupport.enableFor(tree_viewer);
    }

    /** Set or clear error message.
     *  Setting an error message also disables the GUI.
     *  <p>
     *  OK to call multiple times or after disposal.
     *  @param error Error message or <code>null</code> to clear error
     */
    public void setErrorMessage(final String error)
    {
        if (error_message.isDisposed())
            return;
        final Tree tree = tree_viewer.getTree();
        if (error == null)
        {
            if (!error_message.getVisible())
                return; // msg already hidden
            // Hide error message and unlink from layout
            error_message.setVisible(false);
            final FormData fd = (FormData) tree.getLayoutData();
            fd.top = new FormAttachment(0, 0);
            tree.getParent().layout();
        }
        else
        {   // Update the message
            error_message.setText(error);
            if (!error_message.getVisible())
            {   // Show error message and link to layout
                error_message.setVisible(true);
                final FormData fd = (FormData) tree.getLayoutData();
                fd.top = new FormAttachment(error_message);
            }
            error_message.getParent().layout();
        }
    }

    /** Add context menu to tree
     *  @param site Workbench site or <code>null</code>
     */
    private void connectContextMenu(final IWorkbenchPartSite site)
    {
        final Tree tree = tree_viewer.getTree();
        final MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener()
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }
        });
        tree.setMenu(manager.createContextMenu(tree));

        // Allow extensions to add to the context menu
        if (site != null)
        {
            site.registerContextMenu(manager, tree_viewer);
            site.setSelectionProvider(tree_viewer);
        }
    }

    /** Invoked by the manager of the context menu when menu
     *  is about to show.
     *  Fills context menu with guidance and related displays
     *  for currently selected items.
     *  @param manager Menu manager
     */
    @SuppressWarnings("unchecked")
    private void fillContextMenu(final IMenuManager manager)
    {
        final Shell shell = tree_viewer.getTree().getShell();
        final List<AlarmTreeItem> items =
            ((IStructuredSelection)tree_viewer.getSelection()).toList();

        new ContextMenuHelper(manager, shell, items, model.isWriteAllowed());
        manager.add(new Separator());
		if(model.isWriteAllowed())
		{
	        // Add edit items
		    if (items.size() <= 0)
		    {
		        // Use the 'root' element as the parent
                manager.add(new AddComponentAction(shell, model, model.getConfigTree()));
		    }
		    else if (items.size() == 1)
	        {
	            final AlarmTreeItem item = items.get(0);
	            // Allow configuration of single item

		        manager.add(new ConfigureItemAction(shell, model, item));
		        manager.add(new Separator());
		        // Allow addition of items to all but PVs (leafs of tree)
		        if (! (item instanceof AlarmTreePV))
		        	manager.add(new AddComponentAction(shell, model, item));
		        manager.add(new RenameItemAction(shell, model, item));

		        if (items.get(0).getPosition() == AlarmTreePosition.PV)
	                  manager.add(new DuplicatePVAction(shell, model,
	                                                    (AlarmTreePV)items.get(0)));
	        }
	        if (items.size() >= 1)
	        {   // Allow removal of one or more selected items
	            manager.add(new MoveItemAction(shell, model, items));
	            manager.add(new RemoveComponentAction(shell, model, items));
	        }
		}
        manager.add(new Separator());
        manager.add(new AlarmPerspectiveAction());
        manager.add(new Separator());
        manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    /** Set focus to desired element in GUI */
    public void setFocus()
    {
        tree_viewer.getTree().setFocus();
    }

    /** Collapse the alarm tree */
    public void collapse()
    {
        tree_viewer.collapseAll();
        tree_viewer.refresh(false);
    }

    /** @return <code>true</code> if we only show alarms,
     *          <code>false</code> if we show the whole configuration
     * @return
     */
    public boolean getAlarmDisplayMode()
    {
        return show_only_alarms;
    }

    /** @param only_alarms Show only alarms? */
    public void setAlarmDisplayMode(boolean only_alarms)
    {
        show_only_alarms = only_alarms;
        tree_viewer.refresh();
        if (show_only_alarms)
            tree_viewer.expandAll();
    }

    // @see AlarmClientModelListener
    @Override
    public void serverModeUpdate(AlarmClientModel model, boolean maintenanceMode)
    {
        // Ignored
    }

    /** Server connection timeout
     *  @see AlarmClientModelListener
     */
    @Override
    public void serverTimeout(final AlarmClientModel model)
    {
        Display.getDefault().asyncExec(new Runnable()
        {
            @Override
            public void run()
            {
                setErrorMessage(Messages.ServerTimeout);
            }
        });
    }

    /** Model changed, redo the whole tree
     *  @see AlarmClientModelListener
     */
    @Override
    public void newAlarmConfiguration(final AlarmClientModel model)
    {
        final AlarmTreeRoot config = model.getConfigTree();
        Display.getDefault().asyncExec(new Runnable()
        {
            @Override
            public void run()
            {
                final Tree tree = tree_viewer.getTree();
                if (tree.isDisposed())
                    return;
                // Puzzling. After switching to ws=cocoa for OS X,
                // the tree would stay blank until either waiting a long time,
                // or switching to another window, opening a dialog etc.
                // triggers a refresh.
                // What seems to work is the combination of manual setRedraw(false, true)
                // and a tree_viewer.refresh().
                tree.setRedraw(false);
                tree_viewer.setInput(config);
                tree_viewer.refresh();
                tree.setRedraw(true);
            }
        });
    }

    /** Alarm state changed, refresh the display
     *  @see AlarmClientModelListener
     */
    @Override
    public void newAlarmState(final AlarmClientModel model,
            final AlarmTreePV pv)
    {
        Display.getDefault().asyncExec(new Runnable()
        {
            @Override
            public void run()
            {
                final Tree tree = tree_viewer.getTree();
                if (tree.isDisposed())
                    return;
                if (model.isServerAlive())
                    setErrorMessage(null);
                // Refresh to indicate new state
                if (pv != null)
                    tree_viewer.refresh();
            }
        });
    }

    /** Acknowledge currently selected alarms */
    @SuppressWarnings("unchecked")
    public void acknowledgeSelectedAlarms()
    {
        final List<AlarmTreeItem> items =
            ((IStructuredSelection)tree_viewer.getSelection()).toList();
        for (AlarmTreeItem item : items)
            if (item instanceof AlarmTreePV)
                ((AlarmTreePV)item).acknowledge(true);
    }

    /** Un-acknowledge currently selected alarms */
    @SuppressWarnings("unchecked")
    public void unacknowledgeSelectedAlarms()
    {
        final List<AlarmTreeItem> items =
            ((IStructuredSelection)tree_viewer.getSelection()).toList();
        for (AlarmTreeItem item : items)
            if (item instanceof AlarmTreePV)
                ((AlarmTreePV)item).acknowledge(false);
    }

    public TreeViewer getTreeViewer()
    {
        return tree_viewer;
    }
}
