package org.csstudio.opibuilder.widgets.editparts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.ParseException;

import org.csstudio.opibuilder.commands.SetWidgetPropertyCommand;
import org.csstudio.opibuilder.editparts.ExecutionMode;
import org.csstudio.opibuilder.model.AbstractPVWidgetModel;
import org.csstudio.opibuilder.properties.IWidgetPropertyChangeHandler;
import org.csstudio.opibuilder.util.ConsoleService;
import org.csstudio.opibuilder.widgets.model.LabelModel;
import org.csstudio.opibuilder.widgets.model.TextInputModel;
import org.csstudio.opibuilder.widgets.model.TextIndicatorModel.FormatEnum;
import org.csstudio.platform.data.IDoubleValue;
import org.csstudio.platform.data.IEnumeratedValue;
import org.csstudio.platform.data.ILongValue;
import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.IStringValue;
import org.csstudio.platform.data.IValue;
import org.csstudio.swt.widgets.datadefinition.IManualStringValueChangeListener;
import org.csstudio.swt.widgets.figures.TextFigure;
import org.csstudio.swt.widgets.figures.TextInputFigure;
import org.csstudio.swt.widgets.figures.TextInputFigure.FileReturnPart;
import org.csstudio.swt.widgets.figures.TextInputFigure.FileSource;
import org.csstudio.swt.widgets.figures.TextInputFigure.SelectorType;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.PVListener;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.osgi.util.NLS;

/**The editpart for text input widget.)
 * @author Xihui Chen
 *
 */
public class TextInputEditpart extends TextIndicatorEditPart {

	private PVListener pvLoadLimitsListener;
	private INumericMetaData meta = null;
	
	@Override
	public TextInputModel getWidgetModel() {
		return (TextInputModel)getModel();
	}
	
	@Override
	protected IFigure doCreateFigure() {
		TextInputFigure textInputFigure = (TextInputFigure)super.doCreateFigure();
		textInputFigure.setSelectorType(getWidgetModel().getSelectorType());
		textInputFigure.setDateTimeFormat(getWidgetModel().getDateTimeFormat());
		textInputFigure.setFileSource(getWidgetModel().getFileSource());
		textInputFigure.setFileReturnPart(getWidgetModel().getFileReturnPart());
		
		textInputFigure
				.addManualValueChangeListener(new IManualStringValueChangeListener() {

					public void manualValueChanged(String newValue) {
						if(getExecutionMode() == ExecutionMode.RUN_MODE){
							setPVValue(TextInputModel.PROP_PVNAME, newValue);
							getWidgetModel().setPropertyValue(
								TextInputModel.PROP_TEXT, newValue, false);
						}else{
							getViewer().getEditDomain().getCommandStack().execute(
									new SetWidgetPropertyCommand(
											getWidgetModel(), TextInputModel.PROP_TEXT, newValue));
						}
					}
				});
		
		return textInputFigure;
	}
	
	
	@Override
	protected TextFigure createTextFigure() {
		return new TextInputFigure(getExecutionMode() == ExecutionMode.RUN_MODE);
	}
	
	@Override
	protected void createEditPolicies() {
		super.createEditPolicies();
		installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new TextIndicatorDirectEditPolicy());		
	}
	
	@Override
	public void activate() {
		markAsControlPV(AbstractPVWidgetModel.PROP_PVNAME, AbstractPVWidgetModel.PROP_PVVALUE);
		super.activate();
	}
	
	@Override
	protected void doActivate() {	
		super.doActivate();
		registerLoadLimitsListener();
	}


	/**
	 * 
	 */
	private void registerLoadLimitsListener() {
		if(getExecutionMode() == ExecutionMode.RUN_MODE){
			final TextInputModel model = getWidgetModel();
			if(model.isLimitsFromPV()){
				PV pv = getPV(AbstractPVWidgetModel.PROP_PVNAME);
				if(pv != null){	
					if(pvLoadLimitsListener == null)
						pvLoadLimitsListener = new PVListener() {				
							public void pvValueUpdate(PV pv) {
								IValue value = pv.getValue();
								if (value != null && value.getMetaData() instanceof INumericMetaData){
									INumericMetaData new_meta = (INumericMetaData)value.getMetaData();
									if(meta == null || !meta.equals(new_meta)){
										meta = new_meta;
										model.setPropertyValue(TextInputModel.PROP_MAX,	meta.getDisplayHigh());
										model.setPropertyValue(TextInputModel.PROP_MIN,	meta.getDisplayLow());								
									}
								}
							}					
							public void pvDisconnected(PV pv) {}
						};
					pv.addListener(pvLoadLimitsListener);				
				}
			}
		}
	}
	
	@Override
	protected void registerPropertyChangeHandlers() {
		super.registerPropertyChangeHandlers();
		if(getExecutionMode() == ExecutionMode.RUN_MODE){
			removeAllPropertyChangeHandlers(LabelModel.PROP_TEXT);
			IWidgetPropertyChangeHandler handler = new IWidgetPropertyChangeHandler(){
				public boolean handleChange(Object oldValue, Object newValue,
						IFigure figure) {
					String text = (String)newValue;
					//((TextFigure)figure).setText(text);		
					
					try {
						setPVValue(AbstractPVWidgetModel.PROP_PVNAME, parseString(text));
					} catch (Exception e) {
						String msg = NLS.bind("Failed to write value to PV {0}: illegal input : {1} \n",
								getPV(AbstractPVWidgetModel.PROP_PVNAME).getName(), text) + e.toString();
						ConsoleService.getInstance().writeError(msg);
					}					
					return false;
				}
			};			
			setPropertyChangeHandler(LabelModel.PROP_TEXT, handler);
		}
		
		IWidgetPropertyChangeHandler pvNameHandler = new IWidgetPropertyChangeHandler() {
			
			public boolean handleChange(Object oldValue, Object newValue, IFigure figure) {
				registerLoadLimitsListener();
				return false;
			}
		};		
		setPropertyChangeHandler(AbstractPVWidgetModel.PROP_PVNAME, pvNameHandler);
		
		IWidgetPropertyChangeHandler dateTimeFormatHandler = new IWidgetPropertyChangeHandler() {
			
			public boolean handleChange(Object oldValue, Object newValue, IFigure figure) {
				((TextInputFigure)figure).setDateTimeFormat((String)newValue);
				return false;
			}
		};
		setPropertyChangeHandler(TextInputModel.PROP_DATETIME_FORMAT, dateTimeFormatHandler);
		
		IWidgetPropertyChangeHandler fileSourceHandler = new IWidgetPropertyChangeHandler() {
			
			public boolean handleChange(Object oldValue, Object newValue, IFigure figure) {
				((TextInputFigure)figure).setFileSource(FileSource.values()[(Integer)newValue]);
				return false;
			}
		};
		setPropertyChangeHandler(TextInputModel.PROP_FILE_SOURCE, fileSourceHandler);
		
		IWidgetPropertyChangeHandler fileReturnPartHandler = new IWidgetPropertyChangeHandler() {
			
			public boolean handleChange(Object oldValue, Object newValue, IFigure figure) {
				((TextInputFigure)figure).setFileReturnPart(FileReturnPart.values()[(Integer)newValue]);
				return false;
			}
		};
		setPropertyChangeHandler(TextInputModel.PROP_FILE_RETURN_PART, fileReturnPartHandler);
		
		
		getWidgetModel().getProperty(TextInputModel.PROP_SELECTOR_TYPE).
			addPropertyChangeListener(new PropertyChangeListener() {
			
				public void propertyChange(PropertyChangeEvent evt) {
					SelectorType selectorType = SelectorType.values()[(Integer)evt.getNewValue()];
					((TextInputFigure)figure).setSelectorType(selectorType);
					switch (selectorType) {
					case NONE:
						getWidgetModel().setPropertyVisible(TextInputModel.PROP_DATETIME_FORMAT, false);
						getWidgetModel().setPropertyVisible(TextInputModel.PROP_FILE_RETURN_PART, false);
						getWidgetModel().setPropertyVisible(TextInputModel.PROP_FILE_SOURCE, false);						
						break;
					case DATETIME:
						getWidgetModel().setPropertyVisible(TextInputModel.PROP_DATETIME_FORMAT, true);
						getWidgetModel().setPropertyVisible(TextInputModel.PROP_FILE_RETURN_PART, false);
						getWidgetModel().setPropertyVisible(TextInputModel.PROP_FILE_SOURCE, false);
						break;
					case FILE:
						getWidgetModel().setPropertyVisible(TextInputModel.PROP_DATETIME_FORMAT, false);
						getWidgetModel().setPropertyVisible(TextInputModel.PROP_FILE_RETURN_PART, true);
						getWidgetModel().setPropertyVisible(TextInputModel.PROP_FILE_SOURCE, true);
						break;
					default:
						break;
					}					
				}
		});
		
		
		
	
	}
	
	@Override
	protected void doDeActivate() {
		super.doDeActivate();
		if(getWidgetModel().isLimitsFromPV()){
			PV pv = getPV(AbstractPVWidgetModel.PROP_PVNAME);
			if(pv != null && pvLoadLimitsListener != null){	
				pv.removeListener(pvLoadLimitsListener);
			}
		}
		
	}
	
	@Override
	public void performRequest(Request request){
		if (getFigure().isEnabled() && (request.getType() == RequestConstants.REQ_DIRECT_EDIT || 
				request.getType() == RequestConstants.REQ_OPEN))
			performDirectEdit();
	}
	
	protected void performDirectEdit(){
		new LabelEditManager(this, 
				new LabelCellEditorLocator((Figure)getFigure()), false).show();
	}
	
	@Override
	protected int getUpdateSuppressTime() {
		return -1;
	}
	
	/**Parse string to a value according PV value type and format
	 * @param text
	 * @return value
	 * @throws ParseException 
	 */
	private Object parseString(final String text) throws ParseException{
		IValue pvValue = getPVValue(AbstractPVWidgetModel.PROP_PVNAME);
		FormatEnum formatEnum = getWidgetModel().getFormat();

		if(pvValue == null || pvValue instanceof IStringValue){
			return text;
		}
		
		if(pvValue instanceof IDoubleValue){
			switch (formatEnum) {
			case HEX:
			case HEX64:
				return parseHEX(text, true);
			case STRING:
				if(((IDoubleValue)pvValue).getValues().length > 1){
					return parseCharArray(text);
				}
			case DECIAML:
			case DEFAULT:
			case EXP:			
			default:
				return parseDouble(text, true);
			}
		}
		
		if(pvValue instanceof ILongValue){
			switch (formatEnum) {
			case HEX:
			case HEX64:
				return parseHEX(text, true);
			case STRING:
				if(((ILongValue)pvValue).getValues().length > 1){
					return parseCharArray(text);
				}
			case DECIAML:
			case DEFAULT:
			case EXP:			
			default:
				return parseDouble(text, true);
			}
		}
		
		if(pvValue instanceof IEnumeratedValue){
			switch (formatEnum) {
			case HEX:
			case HEX64:
				return parseHEX(text, true);
			case STRING:
				return text;
			case DEFAULT:				
			case DECIAML:			
			case EXP:			
			default:
				return parseDouble(text, true);
			}
		}
		
		return text;
		
	}

	private Integer[] parseCharArray(final String text) {
		Integer[] iString = new Integer[text.length()];
		char[] textChars = text.toCharArray();
		
		for (int ii = 0; ii< text.length(); ii++){
			iString[ii] = Integer.valueOf(textChars[ii]);
		}
		return iString;
	}
	
	private Object parseDouble(final String text, final boolean coerce) throws ParseException {
		DecimalFormat format = new DecimalFormat();

		double value = format.parse(text).doubleValue();
		if (coerce) {
			double min = getWidgetModel().getMinimum();
			double max = getWidgetModel().getMaximum();
			if(value<min){
				value = min;
			}else if(value>max)
				value=max;			
		}
		return value;

	}
	
	private Object parseHEX(final String text, final boolean coerce) {
		String valueText = text.trim();
		if (text.startsWith(TextIndicatorEditPart.HEX_PREFIX)) {
			valueText = text.substring(2);
		}
		if(valueText.contains(" ")){  //$NON-NLS-1$
			valueText = valueText.substring(0, valueText.indexOf(' '));  
		}
		long i = Long.parseLong(valueText, 16);
		if (coerce) {			
			double min = getWidgetModel().getMinimum();
			double max = getWidgetModel().getMaximum();
			if(i<min){
				i=(long) min;
			}else if(i>max)
				i=(long) max;		
		}
		return (int)i;  //EPICS_V3_PV doesn't support Long

	}
	
	@Override
	protected String formatValue(Object newValue, String propId, IFigure figure) {
		String text = super.formatValue(newValue, propId, figure);
		getWidgetModel().setPropertyValue(TextInputModel.PROP_TEXT, text, false);
		return text;
		
	}
	
}
