package org.csstudio.opibuilder.visualparts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**The cellEditor for macros property descriptor.
 * @author Xihui Chen
 *
 */
public class StringTableCellEditor extends AbstractDialogCellEditor {
	
	private List<String[]> data;

	private String[] columnTitles;
	
	public StringTableCellEditor(Composite parent, String title, String[] columnTitles) {
		super(parent, title);
		this.columnTitles = columnTitles;
	}

	@Override
	protected void openDialog(Shell parentShell, String dialogTitle) {
			
		StringTableEditDialog dialog = 
			new StringTableEditDialog(parentShell, data, dialogTitle, columnTitles);
		if(dialog.open() == Window.OK){
			data = dialog.getResult();			
		}
	}

	@Override
	protected boolean shouldFireChanges() {
		return data != null;
	}

	@Override
	protected Object doGetValue() {
		return data;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doSetValue(Object value) {
		if(value == null || !(value instanceof List))
			data = new ArrayList<String[]>();
		else
			data = (List<String[]>)value;
			
	}

}
