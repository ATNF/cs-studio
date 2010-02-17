package org.csstudio.sds.components.ui.internal.editparts;

import org.csstudio.sds.components.model.ScaledSliderModel;
import org.csstudio.sds.components.ui.internal.figures.ScaledSliderFigure;
import org.csstudio.sds.ui.editparts.ExecutionMode;
import org.csstudio.sds.ui.editparts.IWidgetPropertyChangeHandler;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.graphics.RGB;

/**
 * EditPart controller for the scaled slider widget. The controller mediates between
 * {@link ScaledSliderModel} and {@link ScaledSliderFigure}.
 * 
 * @author Xihui Chen
 * 
 */
public final class ScaledSliderEditPart extends AbstractMarkedWidgetEditPart {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IFigure doCreateFigure() {
		final ScaledSliderModel model = (ScaledSliderModel) getWidgetModel();

		ScaledSliderFigure slider = new ScaledSliderFigure();
		
		initializeCommonFigureProperties(slider, model);		
		slider.setFillColor(getRgb(model.getFillColor()));
		slider.setEffect3D(model.isEffect3D());	
		slider.setFillBackgroundColor(getRgb(model.getFillbackgroundColor()));
		slider.setThumbColor(getRgb(model.getThumbColor()));
		slider.setHorizontal(model.isHorizontal());
		slider.setIncrement(model.getIncrement());
		slider.addSliderListener(new ScaledSliderFigure.IScaledSliderListener() {
			public void sliderValueChanged(final double newValue) {
				if (getExecutionMode() == ExecutionMode.RUN_MODE)
					model.getProperty(ScaledSliderModel.PROP_VALUE)
							.setManualValue(newValue);					
				
			}
		});		
		
		return slider;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void registerPropertyChangeHandlers() {
		registerCommonPropertyChangeHandlers();
		
		//fillColor
		IWidgetPropertyChangeHandler fillColorHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScaledSliderFigure slider = (ScaledSliderFigure) refreshableFigure;
				slider.setFillColor(getRgb((String) newValue));
				return true;
			}
		};
		setPropertyChangeHandler(ScaledSliderModel.PROP_FILL_COLOR, fillColorHandler);	
		
		//fillBackgroundColor
		IWidgetPropertyChangeHandler fillBackColorHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScaledSliderFigure slider = (ScaledSliderFigure) refreshableFigure;
				slider.setFillBackgroundColor(getRgb((String) newValue));
				return true;
			}
		};
		setPropertyChangeHandler(ScaledSliderModel.PROP_FILLBACKGROUND_COLOR, fillBackColorHandler);	
		
		//thumbColor
		IWidgetPropertyChangeHandler thumbColorHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScaledSliderFigure slider = (ScaledSliderFigure) refreshableFigure;
				slider.setThumbColor(getRgb((String) newValue));
				return true;
			}
		};
		setPropertyChangeHandler(ScaledSliderModel.PROP_THUMB_COLOR, thumbColorHandler);		
		
		//effect 3D
		IWidgetPropertyChangeHandler effect3DHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScaledSliderFigure slider = (ScaledSliderFigure) refreshableFigure;
				slider.setEffect3D((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(ScaledSliderModel.PROP_EFFECT3D, effect3DHandler);	
		
		
		//horizontal
		IWidgetPropertyChangeHandler horizontalHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScaledSliderFigure slider = (ScaledSliderFigure) refreshableFigure;
				slider.setHorizontal((Boolean) newValue);
				ScaledSliderModel model = (ScaledSliderModel)getModel();
				
				if((Boolean) newValue) //from vertical to horizontal
					model.setLocation(model.getX() - model.getHeight()/2 + model.getWidth()/2,
						model.getY() + model.getHeight()/2 - model.getWidth()/2);
				else  //from horizontal to vertical
					model.setLocation(model.getX() + model.getWidth()/2 - model.getHeight()/2,
						model.getY() - model.getWidth()/2 + model.getHeight()/2);					
				
				model.setSize(model.getHeight(), model.getWidth());
				
				return true;
			}
		};
		setPropertyChangeHandler(ScaledSliderModel.PROP_HORIZONTAL, horizontalHandler);	
		
		
		//enabled. WidgetBaseEditPart will force the widget as disabled in edit model,
		//which is not the case for the scaled slider		
		IWidgetPropertyChangeHandler enableHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScaledSliderFigure slider = (ScaledSliderFigure) refreshableFigure;
				
//				slider.setEnabled((Boolean) newValue);
				// 2009-07-21 KM: Changed to mkae the ScaledSlider only editable when the user has the permission for it
				slider.setEnabled(getWidgetModel().isAccesible());
				return true;
			}
		};
		setPropertyChangeHandler(ScaledSliderModel.PROP_ENABLED, enableHandler);	
		
		
		IWidgetPropertyChangeHandler incrementHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScaledSliderFigure slider = (ScaledSliderFigure) refreshableFigure;
				slider.setIncrement((Double)newValue);
				return true;
			}
		};
		setPropertyChangeHandler(ScaledSliderModel.PROP_INCREMENT, incrementHandler);
		
	}

}
