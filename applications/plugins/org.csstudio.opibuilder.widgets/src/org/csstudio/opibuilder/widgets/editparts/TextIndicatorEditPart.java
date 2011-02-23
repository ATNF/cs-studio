package org.csstudio.opibuilder.widgets.editparts;

import org.csstudio.opibuilder.editparts.AbstractPVWidgetEditPart;
import org.csstudio.opibuilder.editparts.ExecutionMode;
import org.csstudio.opibuilder.model.AbstractPVWidgetModel;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.properties.IWidgetPropertyChangeHandler;
import org.csstudio.opibuilder.util.OPIFont;
import org.csstudio.opibuilder.widgets.model.LabelModel;
import org.csstudio.opibuilder.widgets.model.TextIndicatorModel;
import org.csstudio.opibuilder.widgets.model.TextIndicatorModel.FormatEnum;
import org.csstudio.platform.data.IDoubleValue;
import org.csstudio.platform.data.IEnumeratedValue;
import org.csstudio.platform.data.ILongValue;
import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.IValue.Format;
import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.csstudio.swt.widgets.figures.TextFigure;
import org.csstudio.swt.widgets.figures.TextFigure.H_ALIGN;
import org.csstudio.swt.widgets.figures.TextFigure.V_ALIGN;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.swt.widgets.Display;

/**The editor for text indicator widget.
 * @author Xihui Chen
 *
 */
public class TextIndicatorEditPart extends AbstractPVWidgetEditPart {

	public static final String HEX_PREFIX = "0x"; //$NON-NLS-1$
	
	private TextIndicatorModel widgetModel;
	private FormatEnum format;
	private boolean isAutoSize;
	private boolean isPrecisionFromDB;
	private boolean isShowUnits;
	private int precision;
	

	@Override
	protected IFigure doCreateFigure() {
		
		//Initialize frequently used variables.
		widgetModel = getWidgetModel();		
		format = widgetModel.getFormat();
		isAutoSize = widgetModel.isAutoSize();
		isPrecisionFromDB = widgetModel.isPrecisionFromDB();
		isShowUnits = widgetModel.isShowUnits();
		precision = widgetModel.getPrecision();
		
		
		TextFigure labelFigure = createTextFigure(); 
		labelFigure.setFont(CustomMediaFactory.getInstance().getFont(
				widgetModel.getFont().getFontData()));
		labelFigure.setOpaque(!widgetModel.isTransparent());
		labelFigure.setHorizontalAlignment(widgetModel.getHorizontalAlignment());
		labelFigure.setVerticalAlignment(widgetModel.getVerticalAlignment());
		return labelFigure;
	}
	
	protected TextFigure createTextFigure(){
		return new TextFigure(getExecutionMode() == ExecutionMode.RUN_MODE);
	}
	
	@Override
	protected void createEditPolicies() {
		super.createEditPolicies();
		if(getExecutionMode() == ExecutionMode.EDIT_MODE)
			installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new TextIndicatorDirectEditPolicy());		
	}
	
	@Override
	public void activate() {
		super.activate();
		((TextFigure)getFigure()).setText(getWidgetModel().getText());
		if(getWidgetModel().isAutoSize()){
			getWidgetModel().setSize(((TextFigure)figure).getAutoSizeDimension());
			figure.revalidate();
		}
	}
	
	@Override
	protected void registerPropertyChangeHandlers() {	
		
		IWidgetPropertyChangeHandler handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					final IFigure figure) {
				((TextFigure)figure).setText((String)newValue);
				
				if(isAutoSize){
					Display.getCurrent().timerExec(10, new Runnable() {					
						public void run() {						
								performAutoSize(figure);
						}
					});
				}
				return true;
			}
		};
		setPropertyChangeHandler(TextIndicatorModel.PROP_TEXT, handler);
		
		
		
		IWidgetPropertyChangeHandler fontHandler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					IFigure figure) {
				figure.setFont(CustomMediaFactory.getInstance().getFont(
						((OPIFont)newValue).getFontData()));
				return true;
			}
		};		
		setPropertyChangeHandler(LabelModel.PROP_FONT, fontHandler);
	
		
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					final IFigure figure) {
				Display.getCurrent().timerExec(10, new Runnable() {					
					public void run() {
						if(getWidgetModel().isAutoSize()){
							performAutoSize(figure);
							figure.revalidate();
						}
					}
				});
				
				return true;
			}
		};
		setPropertyChangeHandler(LabelModel.PROP_FONT, handler);		
		setPropertyChangeHandler(AbstractWidgetModel.PROP_BORDER_STYLE, handler);
		setPropertyChangeHandler(AbstractWidgetModel.PROP_BORDER_WIDTH, handler);
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					IFigure figure) {
				((TextFigure)figure).setOpaque(!(Boolean)newValue);
				return true;
			}
		};
		setPropertyChangeHandler(LabelModel.PROP_TRANSPARENT, handler);
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					IFigure figure) {
				isAutoSize = (Boolean)newValue;
				if((Boolean)newValue){
					performAutoSize(figure);
					figure.revalidate();
				}
				return true;
			}
		};
		setPropertyChangeHandler(LabelModel.PROP_AUTOSIZE, handler);
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					IFigure figure) {
				((TextFigure)figure).setHorizontalAlignment(H_ALIGN.values()[(Integer)newValue]);
				return true;
			}
		};
		setPropertyChangeHandler(LabelModel.PROP_ALIGN_H, handler);
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					IFigure figure) {
				((TextFigure)figure).setVerticalAlignment(V_ALIGN.values()[(Integer)newValue]);
				return true;
			}
		};
		setPropertyChangeHandler(LabelModel.PROP_ALIGN_V, handler);
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					final IFigure figure) {
				if(newValue == null)
					return false;				
				formatValue(newValue, AbstractPVWidgetModel.PROP_PVVALUE, figure);
				return false;
			}		
		};
		setPropertyChangeHandler(AbstractPVWidgetModel.PROP_PVVALUE, handler);		
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					final IFigure figure) {
				format = FormatEnum.values()[(Integer)newValue];
				formatValue(newValue, TextIndicatorModel.PROP_FORMAT_TYPE, figure);
				return true;
			}		
		};
		setPropertyChangeHandler(TextIndicatorModel.PROP_FORMAT_TYPE, handler);	
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					final IFigure figure) {
				precision = (Integer)newValue;
				formatValue(newValue, TextIndicatorModel.PROP_PRECISION, figure);
				return true;
			}		
		};
		setPropertyChangeHandler(TextIndicatorModel.PROP_PRECISION, handler);	
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					final IFigure figure) {
				isPrecisionFromDB = (Boolean)newValue;
				formatValue(newValue, TextIndicatorModel.PROP_PRECISION_FROM_DB, figure);
				return true;
			}		
		};
		setPropertyChangeHandler(TextIndicatorModel.PROP_PRECISION_FROM_DB, handler);	
		
		handler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					final IFigure figure) {
				isShowUnits = (Boolean)newValue;
				formatValue(newValue, TextIndicatorModel.PROP_SHOW_UNITS, figure);
				return true;
			}		
		};
		setPropertyChangeHandler(TextIndicatorModel.PROP_SHOW_UNITS, handler);	
		
	}
	
	@Override
	public TextIndicatorModel getWidgetModel() {
		return (TextIndicatorModel)getModel();
	}
	
	protected void performDirectEdit(){
		new LabelEditManager(this, new LabelCellEditorLocator((TextFigure)getFigure())).show();
	}
	
	@Override
	public void performRequest(Request request){
		if (getExecutionMode() == ExecutionMode.EDIT_MODE &&( 
				request.getType() == RequestConstants.REQ_DIRECT_EDIT || 
				request.getType() == RequestConstants.REQ_OPEN))
			performDirectEdit();
	}
	
	
	/**
	 * @param figure
	 */
	private void performAutoSize(IFigure figure) {
		getWidgetModel().setSize(((TextFigure)figure).getAutoSizeDimension());
	}
	
	/**
	 * @param newValue
	 * @return
	 */
	protected String formatValue(Object newValue, String propId, IFigure figure) {
		
		
		if(getExecutionMode() != ExecutionMode.RUN_MODE)
			return null;
		IValue value = null;
			

		int tempPrecision = precision;
		if(isPrecisionFromDB)
			tempPrecision = -1;
		
		if(propId.equals(AbstractPVWidgetModel.PROP_PVVALUE))
			value = (IValue)newValue;
		else
			value = getPVValue(AbstractPVWidgetModel.PROP_PVNAME);			
		
		String text;
		switch (format) {		
		case DECIAML:
			text = value.format(Format.Decimal, tempPrecision);
			break;
		case EXP:
			text = value.format(Format.Exponential, tempPrecision);
			break;
		case HEX:	
			if(value instanceof IDoubleValue)
				text = HEX_PREFIX + Integer.toHexString((int) ((IDoubleValue)value).getValue()).toUpperCase();
			else if(value instanceof ILongValue)
				text = HEX_PREFIX + Integer.toHexString((int) ((ILongValue)value).getValue()).toUpperCase();
			else if(value instanceof IEnumeratedValue)
				text = HEX_PREFIX + Integer.toHexString(((IEnumeratedValue)value).getValue()).toUpperCase();
			else
				text = value.format();			
			break;
		case HEX64:
			if(value instanceof IDoubleValue)
				text = HEX_PREFIX + Long.toHexString((long) ((IDoubleValue)value).getValue()).toUpperCase();
			else if(value instanceof ILongValue)
				text = HEX_PREFIX + Long.toHexString((long) ((ILongValue)value).getValue()).toUpperCase();
			else if(value instanceof IEnumeratedValue)
				text = HEX_PREFIX + Long.toHexString(((IEnumeratedValue)value).getValue()).toUpperCase();
			else
				text = value.format();			
			break;
		case STRING:			
			text = value.format(Format.String, tempPrecision);
			break;
		case DEFAULT:
		default:		
			text = value.format(Format.Default, tempPrecision);
			break;
		}		
		
		if(isShowUnits && value.getMetaData() instanceof INumericMetaData)
			text = text + " " + ((INumericMetaData)value.getMetaData()).getUnits(); //$NON-NLS-1$ //$NON-NLS-2$	
		
		//synchronize the property value without fire listeners.
		widgetModel.getProperty(
				TextIndicatorModel.PROP_TEXT).setPropertyValue(text, false);
		((TextFigure)figure).setText(text);		
		
		if(isAutoSize)
			performAutoSize(figure);
		
		return text;
	}

	@Override
	public String getValue() {
		return ((TextFigure)getFigure()).getText();
	}

	@Override
	public void setValue(Object value) {
		((TextFigure)getFigure()).setText(value.toString());
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class key) {
		if(key == TextFigure.class)
			return ((TextFigure)getFigure());

		return super.getAdapter(key);
	}
	
}
