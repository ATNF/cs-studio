package org.csstudio.diag.pvutil.gui;


import org.csstudio.diag.pvutil.model.PVUtilModel;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** Standalone "main" test of the PVUtilDataAPI and the GUI */
public class TestMain
{
    //private static final String URL = "jdbc:oracle:thin:sns_reports/sns@//snsdev3.sns.ornl.gov:1521/devl";
    //private static final String URL = "jdbc:oracle:thin:sns_reports/sns@//snsdb1.sns.ornl.gov/prod";
    @Test
	public void test() throws Exception
    {
        // Initialize SWT
        Display display = Display.getCurrent();
        Shell shell = new Shell(display);
        shell.setText("PV Utility");
        
        PVUtilModel control = new PVUtilModel (); 
        new GUI(shell, control);

        shell.pack();
        shell.open();
        
        while (!shell.isDisposed())
        {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
    }
}
