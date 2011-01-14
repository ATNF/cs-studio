/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.diag.pvfields.view;

import org.csstudio.apputil.ui.swt.ComboHistoryHelper;
import org.csstudio.diag.pvfields.Activator;
import org.csstudio.diag.pvfields.gui.GUI;
import org.csstudio.diag.pvfields.model.PVFieldsModel;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.model.IProcessVariable;
import org.csstudio.platform.ui.internal.dataexchange.ProcessVariableDropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class PVFieldsView  extends ViewPart 
{
    final public static String ID = PVFieldsView.class.getName();
    //private static final String URL = "jdbc:oracle:thin:sns_reports/sns@//snsdev3.sns.ornl.gov:1521/devl";
    public static final String URL = "jdbc:oracle:thin:sns_reports/sns@//snsdb1.sns.ornl.gov/prod"; //$NON-NLS-1$
    /** Memento tag */
    private static final String PV_TAG = "pv"; //$NON-NLS-1$
    final public static String PV_LIST_TAG = "pv_list"; //$NON-NLS-1$
    private static final String FIELD_TAG = "field"; //$NON-NLS-1$
    final public static String FIELD_LIST_TAG = "field_list"; //$NON-NLS-1$
    
    private PVFieldsModel model = null;
    private GUI gui;
    private IMemento memento;
	private ComboViewer cbo_name;
    private ComboHistoryHelper pv_name_helper;
	private ComboViewer field_value;
    private ComboHistoryHelper field_value_helper;
    
    public PVFieldsView()
    {
    	try
    	{
    		model = new PVFieldsModel();
    	}
    	catch (Exception ex)
        {
        	CentralLogger.getInstance().getLogger(this).error(ex);
        }
    	
    }
    /** ViewPart interface, keep the memento. */
    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException
    {
        super.init(site, memento);
        this.memento = memento;
    }
    
   
    /** ViewPart interface, persist state */
    @Override
    public void saveState(IMemento memento)
    {
        super.saveState(memento);
        memento.putString(PV_TAG, gui.getPVName());
        memento.putString(FIELD_TAG, gui.getFieldValue());
    }

    @Override
    public void createPartControl(Composite parent)
    {
        if (model == null)
        {
            new Label(parent, 0).setText("Cannot initialize"); //$NON-NLS-1$
            return;
        }
        gui = new GUI(parent, model);
        cbo_name = gui.getPVViewer();
        field_value = gui.getFieldViewer();
        
        // Allow Eclipse to listen to PV selection changes
        final TableViewer fields_viewer = gui.getFieldsTable();
        getSite().setSelectionProvider(fields_viewer);
 
        pv_name_helper = new ComboHistoryHelper(
                Activator.getDefault().getDialogSettings(),
                PV_LIST_TAG, cbo_name.getCombo(),10,true)
		{
		    @Override
		    public void newSelection(String pv_name)
		    { 
		        setPVName(pv_name);   
		        pv_name_helper.addEntry(pv_name);
		    }
		};
		
		field_value_helper = new ComboHistoryHelper(
                Activator.getDefault().getDialogSettings(),
                FIELD_LIST_TAG, field_value.getCombo(),10,true)
		{
		    @Override
		    public void newSelection(String field_value)
		    { 
		        field_value_helper.addEntry(field_value);
		    }
		};
		
		
		if (memento != null)
        {
            String pv_name = memento.getString(PV_TAG);
            String field = memento.getString(FIELD_TAG);
            if (field != null  &&  field.length() > 0 )
                field_value.getCombo().setText(field);
            if (pv_name != null  &&  pv_name.length() > 0)
                setPVName(pv_name);
        }

		
        // Stop the press when we're no more
        cbo_name.getCombo().addDisposeListener(new DisposeListener()
        {
            public void widgetDisposed(DisposeEvent e)
            {
                pv_name_helper.saveSettings();
            	field_value_helper.saveSettings();
            }
        });

        final ComboViewer cbo_name = gui.getPVViewer();

        pv_name_helper.loadSettings();
        field_value_helper.loadSettings();
        
        
        // Enable 'Drop' on to combo box (entry box)
        new ProcessVariableDropTarget(cbo_name.getCombo())
        {
            @Override
            public void handleDrop(IProcessVariable name,
                                   DropTargetEvent event)
            {
                setPVName(name.getName());
            }
        };

        final Table fields_table = gui.getFieldsTable().getTable();
        
        // Enable 'Drop' on to table.
        new ProcessVariableDropTarget(fields_table)
        {
            @Override
            public void handleDrop(IProcessVariable name,
                                   DropTargetEvent event)
            {
                setPVName(name.getName());
             }
        };

        // Add empty context menu so that other CSS apps can
        // add themselves to it
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        Menu menu = menuMgr.createContextMenu(fields_viewer.getControl());
        fields_viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, fields_viewer);
    }
    
        /** Create or re-display a probe view with the given PV name.
         *  <p>
         *  Invoked by the PVpopupAction.
         *
         *  @param pv_name The PV to 'probe'
         *  @return Returns <code>true</code> when successful.
         */
        public static boolean activateWithPV(IProcessVariable pv_name)
        {
            try
            {
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                IWorkbenchPage page = window.getActivePage();
                PVFieldsView pvFields = (PVFieldsView) page.showView(PVFieldsView.ID);
                pvFields.setPVName(pv_name.getName());
                return true;
            }
            catch (Exception e)
            {
            	CentralLogger.getInstance().getLogger(new PVFieldsView()).error("Exception", e);
            }
            return false;
        }

       
        // ViewPart interface
        @Override
        public void setFocus()
        {
        	if (gui != null)
        		gui.getPVViewer().getCombo().setFocus();
        }

        private void setPVName(final String pv_name)
        {
        	String field = gui.getFieldValue().trim();
            field_value_helper.addEntry(field);
            if (field.length() <= 0)
            	field = null; 
        	gui.setPVName(pv_name,field);
            pv_name_helper.addEntry(pv_name);
        }
}
