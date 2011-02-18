/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser.opiwidget;

import org.csstudio.apputil.ui.dialog.ErrorDetailDialog;
import org.csstudio.opibuilder.util.ResourceUtil;
import org.csstudio.trends.databrowser.editor.DataBrowserEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;

/** Action for context menu object contribution that opens
 *  the full Data Browser for the model in the Data Browser widget
 *  @author Kay Kasemir
 */
public class OpenDataBrowserAction extends DataBrowserWidgetAction
{
    /** Open Data Browser */
    @Override
    protected void doRun(final IWorkbenchPage page, final DataBrowserWidgedEditPart edit_part)
    {
        // In run mode, we always seem to receive the absolute path.
        // In edit mode, a relative path it not resolved
        // unless it's first converted to the absolute path.
        IPath filename = edit_part.getWidgetModel().getExpandedFilename();
        if(!filename.isAbsolute())
            filename = ResourceUtil.buildAbsolutePath(edit_part.getWidgetModel(), filename);
        try
        {
            IFile input = ResourceUtil.getIFileFromIPath(filename);
            IDE.openEditor(page, input, DataBrowserEditor.ID, true);
        }
        catch (Exception ex)
        {
            new ErrorDetailDialog(page.getActivePart().getSite().getShell(),
                Messages.Error,
                NLS.bind(Messages.OpenDataBrowserErrorFmt, filename.toString()),
                NLS.bind(Messages.ErrorDetailFmt, filename.toString(), ex.getMessage())).open();
        }
    }
}
