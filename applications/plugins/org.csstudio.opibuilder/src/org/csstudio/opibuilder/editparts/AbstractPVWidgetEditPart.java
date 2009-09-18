package org.csstudio.opibuilder.editparts;

import java.util.HashMap;
import java.util.Map;

import org.csstudio.opibuilder.model.AbstractPVWidgetModel;
import org.csstudio.opibuilder.properties.IWidgetPropertyChangeHandler;
import org.csstudio.opibuilder.properties.PVValueProperty;
import org.csstudio.opibuilder.properties.StringProperty;
import org.csstudio.opibuilder.util.AlarmColorScheme;
import org.csstudio.opibuilder.util.ResourceUtil;
import org.csstudio.opibuilder.util.UIBundlingThread;
import org.csstudio.opibuilder.visualparts.BorderFactory;
import org.csstudio.opibuilder.visualparts.BorderStyle;
import org.csstudio.platform.data.ISeverity;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.model.IProcessVariable;
import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.PVFactory;
import org.csstudio.utility.pv.PVListener;
import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;

/**The abstract edit part for all PV armed widgets. 
 * Widgets inheritate this class will have the CSS context menu on it.
 * @author Xihui Chen
 *
 */
public abstract class AbstractPVWidgetEditPart extends AbstractWidgetEditPart 
	implements IProcessVariable{
	
	private static RGB DISCONNECTED_COLOR = new RGB(255, 0, 255);
	
	private Map<String, PV> pvMap = new HashMap<String, PV>(); 
	private Map<String, PVListener> pvListenerMap = new HashMap<String, PVListener>();
	private Map<String, Boolean> pvConnectedStatusMap = new HashMap<String, Boolean>();
	
	private boolean connected = true;
	private boolean preEnableState;
	private Border preBorder;
	private String currentDisconnectPVName = "";

	private String controlPVPropId = null;
	
	private Border saveBorder;
	private Color saveForeColor, saveBackColor;
	private interface AlarmSeverity extends ISeverity{
		public void copy(ISeverity severity);
	}
	private AlarmSeverity lastAlarmSeverity = new AlarmSeverity(){
		
		boolean isOK = true;
		boolean isMajor = false;
		boolean isMinor = false;
		boolean isInvalid = false;
		
		public void copy(ISeverity severity){
			isOK = severity.isOK();
			isMajor = severity.isMajor();
			isMinor = severity.isMinor();
			isInvalid = severity.isInvalid();
		}
		
		public boolean hasValue() {
			return false;
		}
		public boolean isInvalid() {
			return isInvalid;
		}
		public boolean isMajor() {
			return isMajor;
		}
		public boolean isMinor() {
			return isMinor;
		}
		public boolean isOK() {
			return isOK;
		}		
	}; 
	
	
	private void markWidgetAsDisconnected(final String pvName){
		if(!connected)
			return;
		connected = false;
		
		preBorder = figure.getBorder();
		
		UIBundlingThread.getInstance().addRunnable(new Runnable(){
			public void run() {
				boolean allDisconnected = true;
				for(boolean c : pvConnectedStatusMap.values()){
					allDisconnected &= !c;
				}
				preEnableState = figure.isEnabled();
				figure.setEnabled(preEnableState && !allDisconnected);
				figure.setBorder(BorderFactory.createBorder(
						BorderStyle.TITLE_BAR, 1, DISCONNECTED_COLOR, 
						allDisconnected ? "Disconnected" : pvName + ": Disconnected"));
				currentDisconnectPVName = pvName;
				figure.repaint();
			}
		});
		
		
	}
	
	private void widgetConnectionRecovered(final String pvName){
		if(connected)
			return;
		connected = true;
		UIBundlingThread.getInstance().addRunnable(new Runnable(){
			public void run() {
				boolean allConnected = true;
				String nextDisconnecteName = ""; //$NON-NLS-1$
				
				for(String s : pvConnectedStatusMap.keySet()){
					boolean c = pvConnectedStatusMap.get(s);
					allConnected &= c;
					if(!c)
						nextDisconnecteName = s;					
				}
				figure.setEnabled(preEnableState);
				if(allConnected)
					figure.setBorder(preBorder);
				else if(currentDisconnectPVName.equals(pvName) || currentDisconnectPVName.equals("")){ //$NON-NLS-1$
					figure.setBorder(BorderFactory.createBorder(
						BorderStyle.TITLE_BAR, 1, DISCONNECTED_COLOR, 
						nextDisconnecteName + " : Disconnected"));
					currentDisconnectPVName = nextDisconnecteName;
				}
				figure.repaint();
			}
		});		
	}
	
	@Override
	public void activate() {
		if(!isActive()){
			super.activate();
						
			if(getExecutionMode() == ExecutionMode.RUN_MODE){
				pvMap.clear();	
				pvConnectedStatusMap.clear();
				saveFigureOKStatus(getFigure());
				final Map<StringProperty, PVValueProperty> pvValueMap = getWidgetModel().getPVMap();
				
				for(final StringProperty sp : pvValueMap.keySet()){
					
					if(sp.getPropertyValue() == null || 
							sp.getPropertyValue().equals("")) 
						continue;
					
					try {
						PV pv = PVFactory.createPV((String) sp.getPropertyValue());
						pvConnectedStatusMap.put(pv.getName(), false);
						if(controlPVPropId != null && controlPVPropId.equals((String)sp.getPropertyID())
								&& !pv.isWriteAllowed()){
							UIBundlingThread.getInstance().addRunnable(new Runnable(){
								public void run() {
									figure.setCursor(ResourceUtil.CURSOR_NO);
									figure.setEnabled(false);
									preEnableState = false;									
								}
							});							
						}
						
						PVListener pvListener = new PVListener(){
							public void pvDisconnected(PV pv) {
								pvConnectedStatusMap.put(pv.getName(), false);
								markWidgetAsDisconnected(pv.getName());
							}

							public synchronized void pvValueUpdate(PV pv) {
								if(pv == null)
									return;
								Boolean connected = pvConnectedStatusMap.get(pv.getName());
								if(connected != null && !connected){
									pvConnectedStatusMap.put(pv.getName(), true);
									widgetConnectionRecovered(pv.getName());
								}
								pvValueMap.get(sp).setPropertyValue(pv.getValue());		
								
							}							
						};
						
						pv.addListener(pvListener);						
						pvMap.put(sp.getPropertyID(), pv);
						pvListenerMap.put(sp.getPropertyID(), pvListener);
					} catch (Exception e) {
						pvConnectedStatusMap.put((String) sp.getPropertyValue(), false);
						markWidgetAsDisconnected((String) sp.getPropertyValue());
						CentralLogger.getInstance().error(this, "Unable to connect to PV:" +
								(String)sp.getPropertyValue());
					}					
				}
				if(!pvMap.isEmpty())
					markWidgetAsDisconnected("");
				doActivate();
				
				//the pv should be started at the last minute
				for(PV pv : pvMap.values()){
					try {
						pv.start();
					} catch (Exception e) {
						pvConnectedStatusMap.put(pv.getName(), false);
						markWidgetAsDisconnected(pv.getName());
						CentralLogger.getInstance().error(this, "Unable to connect to PV:" +
								pv.getName());
					}
				}
			}
		}
	}
	
	/**
	 * Subclass should do the activate things in this method.
	 */
	protected void doActivate() {		
	}
	
	@Override
	protected void registerBasePropertyChangeHandlers() {
		super.registerBasePropertyChangeHandlers();
		// value
		IWidgetPropertyChangeHandler valueHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure figure) {
				AbstractPVWidgetModel model = getWidgetModel();
				if(!model.isBorderAlarmSensitve() && !model.isBackColorAlarmSensitve() &&
						!model.isForeColorAlarmSensitve())
					return false;			
				ISeverity severity = ((IValue)newValue).getSeverity();				
				if(severity.isOK() && lastAlarmSeverity.isOK())
					return false;
				else if(severity.isOK() && !lastAlarmSeverity.isOK()){					
					lastAlarmSeverity.copy(severity);
					restoreFigureToOKStatus(figure);
					return true;
				}
				if(severity.isMajor() && lastAlarmSeverity.isMajor())
					return false;
				if(severity.isMinor() && lastAlarmSeverity.isMinor())
					return false;
				if(severity.isInvalid() && lastAlarmSeverity.isInvalid())
					return false;
				
				if(lastAlarmSeverity.isOK()){
					saveFigureOKStatus(figure);
				}
				
				RGB alarmColor; 
				if(severity.isMajor()){
					alarmColor = AlarmColorScheme.getMajorColor();					
				}else if(severity.isMinor()){
					alarmColor = AlarmColorScheme.getMinorColor();
				}else{
					alarmColor = AlarmColorScheme.getInValidColor();
				}			
								
				if(model.isBorderAlarmSensitve()){
					figure.setBorder(BorderFactory.createBorder(BorderStyle.LINE, 2, alarmColor, ""));
				}
				if(model.isBackColorAlarmSensitve()){
					figure.setBackgroundColor(CustomMediaFactory.getInstance().getColor(alarmColor));
				}
				if(model.isForeColorAlarmSensitve()){
					figure.setForegroundColor(CustomMediaFactory.getInstance().getColor(alarmColor));
				}				
				lastAlarmSeverity.copy(severity);
				return true;
			}

			
		};
		setPropertyChangeHandler(AbstractPVWidgetModel.PROP_PVVALUE, valueHandler);
	}


	private void restoreFigureToOKStatus(IFigure figure) {
		figure.setBorder(saveBorder);
		figure.setBackgroundColor(saveBackColor);
		figure.setForegroundColor(saveForeColor);
	}
	private void saveFigureOKStatus(IFigure figure) {
		saveBorder = figure.getBorder();
		saveForeColor = figure.getForegroundColor();
		saveBackColor = figure.getBackgroundColor();
	}
	
	/**
	 * Subclass should do the deActivate things in this method.
	 */
	protected void doDeActivate() {		
	}
	
	@Override
	public void deactivate() {
		if(isActive()){	
			doDeActivate();
			for(PV pv : pvMap.values())				
				pv.stop();
			
			for(String pvPropID : pvListenerMap.keySet()){
				pvMap.get(pvPropID).removeListener(pvListenerMap.get(pvPropID));
			}
			
			pvMap.clear();
			pvListenerMap.clear();
			pvConnectedStatusMap.clear();
			super.deactivate();
		}
	}
	
	public String getName() {		
		 if(getWidgetModel().getPVMap().isEmpty())
			 return "";
		return (String)((StringProperty)getWidgetModel().getPVMap().keySet().toArray()[0])
			.getPropertyValue();
	}
	
	public String getTypeId() {
		return TYPE_ID;
	}
	
	/**Set PV to given value. Should accept Double, Double[], Integer, String, maybe more.
	 * @param pvPropId
	 * @param value
	 */
	protected void setPVValue(String pvPropId, Object value){
		final PV pv = pvMap.get(pvPropId);
		if(pv != null){
			try {
				pv.setValue(value);
			} catch (final Exception e) {
				UIBundlingThread.getInstance().addRunnable(new Runnable(){
					public void run() {
						MessageBox mb = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								SWT.ICON_ERROR | SWT.OK);
						mb.setMessage("Failed to write PV:" + pv.getName() + "\n" + e.getMessage());
						mb.setText("PV write error");
						mb.open();
					}
				});
				
				
			}
		}
	}
	
	/**Get the pv.
	 * @param pvPropId
	 * @return the corresponding pv for the pvPropId. null if the pv desn't exist.
	 */
	protected PV getPV(String pvPropId){
		return pvMap.get(pvPropId);
	}
	
	protected IValue getPVValue(String pvPropId){
		final PV pv = pvMap.get(pvPropId);
		if(pv != null){
			return pv.getValue();
		}
		return null;
	}
	
	protected void markAsControlPV(String pvPropId){
		controlPVPropId  = pvPropId;
	}
	
	@Override
	public AbstractPVWidgetModel getWidgetModel() {
		return (AbstractPVWidgetModel)getModel();
	}

}
