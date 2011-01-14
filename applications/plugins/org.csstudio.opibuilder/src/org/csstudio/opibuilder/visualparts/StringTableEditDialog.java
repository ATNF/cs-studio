package org.csstudio.opibuilder.visualparts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.csstudio.platform.ui.swt.stringtable.StringTableEditor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**The dialog for editing String Table.
 * @author Xihui Chen
 *
 */
public class StringTableEditDialog extends Dialog {
	
	private String title;	
	private String[] columnTitles;
	private List<String[]> contents;
	
	private StringTableEditor tableEditor;

	public StringTableEditDialog(Shell parentShell, List<String[]> inputData, 
			String dialogTitle, String[] columnTitles) {
		super(parentShell);
		this.title = dialogTitle;
		this.columnTitles = columnTitles;
		this.contents = new ArrayList<String[]>();
		for(String[] item : inputData){
			this.contents.add(item);
		}
		// Allow resize
        setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		// Table editor should stretch to fill the dialog space, but
		// at least on OS X, it has some minimum size below which it
		// doesn't properly shrink.
		int[] columnWidths = new int[columnTitles.length];
		Arrays.fill(columnWidths, 150);
		tableEditor = new StringTableEditor(container,columnTitles, 
				null, contents, null, columnWidths);
		tableEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));	
	
		return container;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configureShell(final Shell shell) {
		super.configureShell(shell);
		if (title != null) {
			shell.setText(title);
		}
	}

	public List<String[]> getResult() {		
		return contents;
	}
	
	@Override
	protected void okPressed() {
		tableEditor.forceFocus();
		super.okPressed();
	}
}
