/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui;

import org.csstudio.alarm.beast.AlarmTreePV;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelListener;
import org.csstudio.platform.security.SecurityFacade;
import org.csstudio.platform.ui.security.AbstractUserDependentAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;

/** Action to control the "maintenance" mode
 *  @author Kay Kasemir
 */
public class MaintenanceModeAction extends AbstractUserDependentAction
{
    /** Images for button icon */
    private static ImageDescriptor image_on = null, image_off = null;
    
    /** Model who's Mode we control */
    final private AlarmClientModel model;

    /** Initialize
     *  @param model Model who's Mode we control
     */
    public MaintenanceModeAction(final AlarmClientModel model)
    {
        super(Messages.MaintenanceMode, AuthIDs.CONFIGURE, false);
        getIcons();
        this.model = model;
        
        //authorization
        setEnabledWithoutAuthorization(true);
        setEnabled(SecurityFacade.getInstance().canExecute(AuthIDs.CONFIGURE, false));
        
        // Reflect mode of model right now, then monitor for mode changes
        reflectModelMode(model.inMaintenanceMode());
        model.addListener(new AlarmClientModelListener()
        {
            public void serverModeUpdate(final AlarmClientModel model, final boolean maintenance_mode)
            {
                Display.getDefault().asyncExec(new Runnable()
                {
                    public void run()
                    {
                        reflectModelMode(maintenance_mode);
                    }
                });
            }

            public void serverTimeout(AlarmClientModel model) { /* Ignore */ }
            public void newAlarmTree(AlarmClientModel model) { /* Ignore */ }
            public void newAlarmState(AlarmClientModel model, AlarmTreePV pv) { /* Ignore */ }
        });
    }
    
    /** Assert that icons are loaded */
    private static void getIcons()
    {
        if (image_off != null)
        	return;
        image_on = Activator.getImageDescriptor("icons/maintenance_act.gif"); //$NON-NLS-1$
        image_off = Activator.getImageDescriptor("icons/operate.gif"); //$NON-NLS-1$
    }
    
    /** Update button show show current model mode */
    private void reflectModelMode(final boolean maintenance_mode)
    {
        setImageDescriptor(maintenance_mode ? image_on : image_off);
        setToolTipText(maintenance_mode ? Messages.MaintenanceModeTT : Messages.NormalModeTT);
    }

    @Override
    protected void doWork()
    {
        if (model.inMaintenanceMode())
        {
            if (!MessageDialog.openConfirm(null, Messages.MaintenanceMode,
                                           Messages.MaintenanceModeDisableMsg))
                return;
            model.requestMaintenanceMode(false);
        }
        else
        {
            if (!MessageDialog.openConfirm(null, Messages.MaintenanceMode,
                                           Messages.MaintenanceModeEnableMsg))
                return;
            model.requestMaintenanceMode(true);
        }
    }
}
