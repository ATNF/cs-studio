/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.alarmtree;

import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.eclipse.jface.action.Action;

/** Action to trigger debug on server
 *  @author Kay Kasemir
 */
public class DebugAction extends Action
{
    final private AlarmClientModel model;

    @SuppressWarnings("nls")
    public DebugAction(final AlarmClientModel model)
    {
        super("Debug", Activator.getImageDescriptor("icons/debug.gif"));
        setToolTipText("Send debug trigger to alarm server");
        this.model = model;
    }

    @Override
    public void run()
    {
        model.triggerDebug();
    }
}
