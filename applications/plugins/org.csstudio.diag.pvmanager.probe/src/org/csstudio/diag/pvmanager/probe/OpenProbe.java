package org.csstudio.diag.pvmanager.probe;

import org.csstudio.csdata.ProcessVariableName;
import org.csstudio.diag.pvmanager.probe.views.PVManagerProbe;
import org.csstudio.ui.util.AdapterUtil;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command handler for opening probe on the current selection.
 * 
 * @author carcassi
 */
public class OpenProbe extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			// Retrieve the selection and the current page
			ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
					.getActivePage().getSelection();
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			ProcessVariableName[] pvs = AdapterUtil.convert(selection,
					ProcessVariableName.class);

			// If it's a single selection, open it in the single view instance
			if (pvs.length == 1) {
				PVManagerProbe probe = (PVManagerProbe) page
						.showView(PVManagerProbe.SINGLE_VIEW_ID);
				probe.setPVName(pvs[0].getProcessVariableName());

			// If it's a multiple selection, open a new multiple view
			// instance
			// for each element
			} else if (pvs.length > 1) {
				for (ProcessVariableName item : pvs) {
					PVManagerProbe probe = (PVManagerProbe) page.showView(
							PVManagerProbe.MULTIPLE_VIEW_ID,
							PVManagerProbe.createNewInstance(),
							IWorkbenchPage.VIEW_ACTIVATE);
					probe.setPVName(item.getProcessVariableName());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
