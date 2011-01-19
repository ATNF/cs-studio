package org.csstudio.utility.pvmanager.widgets;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.epics.pvmanager.extra.WaterfallPlotParameters;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.layout.GridLayout;

public class WaterfallParametersDialog extends Dialog {

	protected WaterfallPlotParameters result;
	protected Shell shell;
	
	private Button btnMetadata;
	private Button btnAutoRange;
	private Button btnManual;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public WaterfallParametersDialog(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
		createContents();
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public WaterfallPlotParameters open(WaterfallPlotParameters oldParameters, int x, int y) {
		if (oldParameters.isAdaptiveRange()) {
			btnAutoRange.setSelection(true);
			btnMetadata.setSelection(false);
		} else {
			btnAutoRange.setSelection(false);
			btnMetadata.setSelection(true);
		}
		
		shell.open();
		shell.layout();
		shell.setBounds(Math.min(x, shell.getDisplay().getClientArea().width - shell.getBounds().width),
				Math.min(y, shell.getDisplay().getClientArea().height - shell.getBounds().height),
				shell.getBounds().width, shell.getBounds().height);
		
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}
	
	private void prepareResult() {
		result = new WaterfallPlotParameters();
		if (btnAutoRange.getSelection()) {
			result = result.withAdaptiveRange(true);
		}
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), SWT.APPLICATION_MODAL);
		shell.setSize(283, 182);
		shell.setText(getText());
		shell.setLayout(new FormLayout());
		
		Button btnCancel = new Button(shell, SWT.NONE);
		FormData fd_btnCancel = new FormData();
		fd_btnCancel.bottom = new FormAttachment(100, -10);
		fd_btnCancel.right = new FormAttachment(100, -10);
		btnCancel.setLayoutData(fd_btnCancel);
		btnCancel.setText("Cancel");
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				result = null;
				shell.close();
			}
		});
		
		Button btnApply = new Button(shell, SWT.NONE);
		FormData fd_btnApply = new FormData();
		fd_btnApply.top = new FormAttachment(btnCancel, 0, SWT.TOP);
		fd_btnApply.right = new FormAttachment(btnCancel, -6);
		btnApply.setLayoutData(fd_btnApply);
		btnApply.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				prepareResult();
				shell.close();
			}
		});
		btnApply.setText("Apply");
		shell.setDefaultButton(btnApply);
		
		Group grpRange = new Group(shell, SWT.NONE);
		grpRange.setText("Range:");
		grpRange.setLayout(new GridLayout(1, false));
		FormData fd_grpRange = new FormData();
		fd_grpRange.top = new FormAttachment(0, 10);
		fd_grpRange.right = new FormAttachment(btnCancel, 0, SWT.RIGHT);
		fd_grpRange.left = new FormAttachment(0, 10);
		fd_grpRange.bottom = new FormAttachment(0, 113);
		grpRange.setLayoutData(fd_grpRange);
		
		btnMetadata = new Button(grpRange, SWT.RADIO);
		btnMetadata.setSelection(true);
		btnMetadata.setText("Metadata");
		
		btnAutoRange = new Button(grpRange, SWT.RADIO);
		btnAutoRange.setText("Auto");
		
		btnManual = new Button(grpRange, SWT.RADIO);
		btnManual.setEnabled(false);
		btnManual.setText("Manual:");

	}
}
