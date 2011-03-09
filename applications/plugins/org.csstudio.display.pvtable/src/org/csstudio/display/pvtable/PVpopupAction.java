/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pvtable;

import org.csstudio.display.pvtable.model.PVListModel;
import org.csstudio.display.pvtable.ui.editor.PVTableEditor;
import org.csstudio.platform.model.IProcessVariable;
import org.csstudio.platform.ui.internal.dataexchange.ProcessVariablePopupAction;

/** Another application sent us a PV name via its popup menu.
 *  @author Kay Kasemir
 */
public class PVpopupAction extends ProcessVariablePopupAction
{    
    @Override
    public void handlePVs(IProcessVariable pv_names[])
    {   
    	PVTableEditor editor = PVTableEditor.createPVTableEditor();
    	if (editor == null)
    		return;
        PVListModel model = editor.getModel();
        for (IProcessVariable pv : pv_names)
            model.addPV(pv.getName());
    }
}
