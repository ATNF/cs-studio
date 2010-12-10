/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.alarmtree;

import org.csstudio.alarm.beast.Preferences;
import org.csstudio.alarm.beast.ui.AcknowledgeAction;
import org.csstudio.alarm.beast.ui.ConfigureItemAction;
import org.csstudio.alarm.beast.ui.MaintenanceModeAction;
import org.csstudio.alarm.beast.ui.UnAcknowledgeAction;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.csstudio.platform.logging.CentralLogger;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

/** Eclipse view that displays the alarm tree.
 *  @author Kay Kasemir
 */
public class AlarmTreeView extends ViewPart
{
    /** ID of the view a defined in plugin.xml */
    final public static String ID = "org.csstudio.alarm.beast.ui.alarmtree.View"; //$NON-NLS-1$

    private AlarmClientModel model;

    private GUI gui = null;

    /** {@inheritDoc} */
    @Override
    public void createPartControl(final Composite parent)
    {
        try
        {
            model = AlarmClientModel.getInstance();
        }
        catch (final Throwable ex)
        {   // Instead of actual GUI, create error message
            final String message =
                NLS.bind(Messages.CannotGetAlarmInfoFmt,
                        ex.getCause() != null
                        ? ex.getCause().getMessage()
                        : ex.getMessage());

            // Add to log, also display in text
            CentralLogger.getInstance().getLogger(this).error(message, ex);
            parent.setLayout(new FillLayout());
            new Text(parent, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI)
                .setText(message);
            return;
        }

        // Arrange for model to be released
        parent.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                model.release();
                model = null;
            }
        });

        // Have model, create GUI
        gui = new GUI(parent, model, getViewSite());

        final IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();
        if (model.isWriteAllowed())
        {
        	if (Preferences.isConfigSelectionAllowed())
        	{
        		toolbar.add(new SelectConfigurationAction(model));
        		toolbar.add(new Separator());
        	}
            toolbar.add(new MaintenanceModeAction(model));
            toolbar.add(new Separator());
            toolbar.add(new InfoAction(model));
            toolbar.add(new DebugAction(model));
            toolbar.add(new ConfigureItemAction(parent.getShell(), model, gui.getTreeViewer()));
            toolbar.add(new AcknowledgeAction(gui.getTreeViewer()));
            toolbar.add(new UnAcknowledgeAction(gui.getTreeViewer()));
            toolbar.add(new Separator());
        }
        toolbar.add(new CollapseAlarmTreeAction(gui));
        toolbar.add(new ExpandCurrentAlarmsAction(gui));

        // Inform workbench about currently selected alarm in tree viewer
        getSite().setSelectionProvider(gui.getTreeViewer());
    }

    /** {@inheritDoc} */
    @Override
    public void setFocus()
    {
        if (gui != null)
            gui.setFocus();
    }
}
