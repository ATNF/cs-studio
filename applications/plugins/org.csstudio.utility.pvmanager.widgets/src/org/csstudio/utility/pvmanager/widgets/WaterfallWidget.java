package org.csstudio.utility.pvmanager.widgets;

import org.csstudio.utility.pvmanager.ui.SWTUtil;
import org.eclipse.swt.widgets.Composite;
import org.epics.pvmanager.PV;
import org.epics.pvmanager.PVManager;
import org.epics.pvmanager.PVValueChangeListener;
import org.epics.pvmanager.data.VImage;
import org.epics.pvmanager.extra.WaterfallPlotParameters;

import static org.epics.pvmanager.extra.ExpressionLanguage.*;
import static org.epics.pvmanager.data.ExpressionLanguage.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Label;
import com.swtdesigner.ResourceManager;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

/**
 * A widget that connects to an array and display a waterfall plot based on it.
 * 
 * @author carcassi
 */
public class WaterfallWidget extends Composite {
	
	private VImageDisplay imageDisplay;
	private WaterfallPlotParameters parameters = new WaterfallPlotParameters();
	private CLabel errorLabel;
	private Label errorImage;

	/**
	 * Creates a new widget.
	 * 
	 * @param parent the parent
	 * @param style the style
	 */
	public WaterfallWidget(Composite parent, int style) {
		super(parent, style);
		setLayout(new FormLayout());
		
		imageDisplay = new VImageDisplay(this);
		imageDisplay.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 3) {
					WaterfallParametersDialog dialog = new WaterfallParametersDialog(getShell(), SWT.NORMAL);
					Point position = new Point(e.x, e.y);
					position = getDisplay().map(WaterfallWidget.this, null, position);
					WaterfallPlotParameters newParameters = dialog.open(parameters, position.x, position.y);
					if (newParameters != null) {
						parameters = newParameters;
						reconnect();
					}
				}
			}
		});
		imageDisplay.setStretched(true);
		GridLayout gl_imageDisplay = new GridLayout(2, false);
		gl_imageDisplay.marginWidth = 0;
		gl_imageDisplay.marginHeight = 0;
		imageDisplay.setLayout(gl_imageDisplay);
		FormData fd_imageDisplay = new FormData();
		fd_imageDisplay.bottom = new FormAttachment(100);
		fd_imageDisplay.right = new FormAttachment(100);
		fd_imageDisplay.top = new FormAttachment(0);
		fd_imageDisplay.left = new FormAttachment(0);
		imageDisplay.setLayoutData(fd_imageDisplay);
		
		errorImage = new Label(imageDisplay, SWT.NONE);
		errorImage.setImage(ResourceManager.getPluginImage("org.eclipse.ui", "/icons/full/obj16/warn_tsk.gif"));
		errorImage.setVisible(false);
		
		errorLabel = new CLabel(imageDisplay, SWT.NONE);
		GridData gd_errorLabel = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_errorLabel.widthHint = 221;
		errorLabel.setLayoutData(gd_errorLabel);
		errorLabel.setText("");
		errorLabel.setVisible(false);
	}
	
	// The pv name for connection
	private String pvName;
	// The pv created by pvmanager
	private PV<VImage> pv;
	
	/**
	 * The pv name to connect to.
	 * 
	 * @return the current property value
	 */
	public String getPvName() {
		return pvName;
	}
	
	/**
	 * Changes the pv name to connect to. Triggers a reconnection.
	 * 
	 * @param pvName the new property value
	 */
	public void setPvName(String pvName) {
		// Guard from double calls
		if (this.pvName != null && this.pvName.equals(pvName)) {
			return;
		}
		
		this.pvName = pvName;
		reconnect();
	}
	
	// Displays the last error generated
	private void setLastError(Exception ex) {
		if (!isDisposed()) {
			if (ex == null) {
				errorImage.setVisible(false);
				errorLabel.setVisible(false);
			} else {
				errorImage.setVisible(true);
				errorLabel.setVisible(true);
				errorLabel.setToolTipText(ex.getMessage());
				errorLabel.setText(ex.getMessage());
			}
		}
	}
	
	// Reconnects the pv
	private void reconnect() {
		// First de-allocate current pv if any
		if (pv != null) {
			pv.close();
			pv = null;
		}
		
		// Clean up old image if present
		imageDisplay.setVImage(null);
		
		if (pvName != null) {
			pv = PVManager.read(waterfallPlotOf(vDoubleArray(pvName), parameters))
				.andNotify(SWTUtil.onSWTThread()).atHz(30);
			pv.addPVValueChangeListener(new PVValueChangeListener() {
				
				@Override
				public void pvValueChanged() {
					setLastError(pv.lastException());
					imageDisplay.setVImage(pv.getValue());
				}
			});
		}
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
