
package org.csstudio.opibuilder.widgets.editparts;

import org.csstudio.opibuilder.properties.IWidgetPropertyChangeHandler;
import org.csstudio.opibuilder.util.OPIColor;
import org.csstudio.opibuilder.widgets.model.AbstractShapeModel;
import org.csstudio.opibuilder.widgets.model.RoundedRectangleModel;
import org.csstudio.swt.widgets.figures.RoundedRectangleFigure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;

/**The editpart of a rectangle widget.
 * @author Xihui Chen
 *
 */
public class RoundedRectangleEditpart extends AbstractShapeEditPart {

	

	@Override
	protected IFigure doCreateFigure() {
		RoundedRectangleFigure figure = new RoundedRectangleFigure();
		RoundedRectangleModel model = getWidgetModel();
		figure.setFill(model.getFillLevel());
		figure.setHorizontalFill(model.isHorizontalFill());
		figure.setTransparent(model.isTransparent());
		figure.setAntiAlias(model.isAntiAlias());
		figure.setCornerDimensions(new Dimension(model.getCornerWidth(), model.getCornerHeight()));
		figure.setLineColor(model.getLineColor());
		return figure;
	}	
	
	@Override
	public RoundedRectangleModel getWidgetModel() {
		return (RoundedRectangleModel)getModel();
	}
	

	@Override
	protected void registerPropertyChangeHandlers() {
		super.registerPropertyChangeHandlers();
		// fill
		IWidgetPropertyChangeHandler fillHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				RoundedRectangleFigure figure = (RoundedRectangleFigure) refreshableFigure;
				figure.setFill((Double) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractShapeModel.PROP_FILL_LEVEL, fillHandler);	
		
		// fill orientaion
		IWidgetPropertyChangeHandler fillOrientHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				RoundedRectangleFigure figure = (RoundedRectangleFigure) refreshableFigure;
				figure.setHorizontalFill((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractShapeModel.PROP_HORIZONTAL_FILL, fillOrientHandler);	
		
		// transparent
		IWidgetPropertyChangeHandler transparentHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				RoundedRectangleFigure figure = (RoundedRectangleFigure) refreshableFigure;
				figure.setTransparent((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(RoundedRectangleModel.PROP_TRANSPARENT, transparentHandler);	
		
		// anti alias
		IWidgetPropertyChangeHandler antiAliasHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				RoundedRectangleFigure figure = (RoundedRectangleFigure) refreshableFigure;
				figure.setAntiAlias((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractShapeModel.PROP_ANTIALIAS, antiAliasHandler);
		
		// line color
		IWidgetPropertyChangeHandler lineColorHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				((RoundedRectangleFigure)refreshableFigure).setLineColor(
						((OPIColor)newValue).getSWTColor());
				return true;
			}
		};
		setPropertyChangeHandler(AbstractShapeModel.PROP_LINE_COLOR,
				lineColorHandler);
		
		
		//corner width
		IWidgetPropertyChangeHandler cornerWidthHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				RoundedRectangleFigure figure = (RoundedRectangleFigure) refreshableFigure;
				figure.setCornerWidth((Integer)newValue);
				return true;
			}
		};
		setPropertyChangeHandler(RoundedRectangleModel.PROP_CORNER_WIDTH, cornerWidthHandler);	
	
		//corner height
		IWidgetPropertyChangeHandler cornerHeightHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				RoundedRectangleFigure figure = (RoundedRectangleFigure) refreshableFigure;
				figure.setCornerHeight((Integer)newValue);
				return true;
			}
		};
		setPropertyChangeHandler(RoundedRectangleModel.PROP_CORNER_HEIGHT, cornerHeightHandler);	
	
		
		
		
	}



}
