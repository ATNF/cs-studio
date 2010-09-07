package org.csstudio.opibuilder.scriptUtil;

import org.csstudio.apputil.ui.elog.ElogDialog;
import org.csstudio.logbook.ILogbook;
import org.csstudio.opibuilder.actions.SendToElogAction;
import org.csstudio.opibuilder.editparts.AbstractBaseEditPart;
import org.csstudio.opibuilder.runmode.RunModeService;
import org.csstudio.opibuilder.runmode.RunModeService.TargetWindow;
import org.csstudio.opibuilder.util.MacrosInput;
import org.csstudio.opibuilder.util.ResourceUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;

/**The utility class to facilitate Javascript programming.
 * @author Xihui Chen
 *
 */
public class ScriptUtil {
	
	/**Open an OPI.
	 * @param widgetController the widgetController to which the script is attached. 
	 * @param relative_path the path of the OPI relative to the Display file of the widgetContoller. 
	 * @param newWindow true if it will be opened in a new window. false if in a new tab.
	 * @param macrosInput the macrosInput. null if no macros needed.
	 */
	public final static void openOPI(AbstractBaseEditPart widgetController, 
			String relative_path, boolean newWindow, MacrosInput macrosInput){
		IPath  path = ResourceUtil.buildAbsolutePath(
				widgetController.getWidgetModel(), ResourceUtil.getPathFromString(relative_path));
		RunModeService.getInstance().runOPI(path, 
				newWindow ? TargetWindow.NEW_WINDOW : TargetWindow.SAME_WINDOW, null, macrosInput);
	}
			
	/**Pop up an Elog dialog to make an Elog entry.
	 * @param filePath path of a file to attach or null. 
	 * It could be either a local file system file path 
	 * or a workspace file path. File types that the logbook support depend on 
	 * implementation but should include *.gif, *.jpg: File will be attached 
	 * as image.
	 */
	public final static void makeElogEntry(final String filePath){
		if(!SendToElogAction.isElogAvailable()){
			 MessageDialog.openError(null, "Error", "No Elog support is available.");
			 return;
		}
		 // Display dialog, create entry
        try
        {	String systemFilePath;
	            IPath path = ResourceUtil.getPathFromString(filePath);
	            try {
		            // try workspace
		  			IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(
		     					path, false);
		        	if (r!= null && r instanceof IFile) {			
		            		systemFilePath = ((IFile)r).getLocation().toOSString();
		            }else
		            	throw new Exception();
	           	} catch (Exception e) {
	            	systemFilePath = filePath;
	        }
	        final String finalfilePath = systemFilePath;
            final ElogDialog dialog =
                new ElogDialog(null, "Send To Logbook",
                        "Elog Entry from BOY",
                        "See attached image",
                        finalfilePath)
            {
                @Override
                public void makeElogEntry(final String logbook_name, final String user,
                        final String password, final String title, final String body)
                        throws Exception
                {
                    final ILogbook logbook = getLogbook_factory()
                                        .connect(logbook_name, user, password);
                    try
                    {	
	                
                        logbook.createEntry(title, body, finalfilePath);
                    }
                    finally
                    {
                        logbook.close();
                    }
                }
                
            };
            dialog.open();
        }
        catch (Exception ex)
        {
            MessageDialog.openError(null, "Error", ex.getMessage());
        }
	}
	
}
