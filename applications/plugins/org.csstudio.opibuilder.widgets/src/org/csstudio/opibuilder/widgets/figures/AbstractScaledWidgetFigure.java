package org.csstudio.opibuilder.widgets.figures;


import org.csstudio.opibuilder.widgets.model.AbstractScaledWidgetModel;
import org.csstudio.swt.xygraph.linearscale.AbstractScale;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Base figure for a widget based on {@link AbstractScaledWidgetModel}.
 * 
 * @author Xihui Chen
 *
 */
public abstract class AbstractScaledWidgetFigure extends Figure {

	protected AbstractScale scale;
	
	protected boolean transparent;
	
	protected double value;
	
	protected double minimum;	
	
	protected double maximum;
	
	protected double majorTickMarkStepHint;
	
	protected boolean showMinorTicks;
	
	protected boolean showScale;
	
	protected boolean logScale;	
	

	@Override
	public boolean isOpaque() {
		return false;
	}
	/**
	 * {@inheritDoc}
	 */
	public void paintFigure(final Graphics graphics) {		
		if (!transparent) {
			graphics.setBackgroundColor(this.getBackgroundColor());
			Rectangle bounds = this.getBounds().getCopy();
			bounds.crop(this.getInsets());
			graphics.fillRectangle(bounds);
		}
		super.paintFigure(graphics);
	}
	
	/**
	 * @param value the value to set
	 */
	public void setValue(final double value) {
		this.value = 
			Math.max(scale.getRange().getLower(), Math.min(scale.getRange().getUpper(), value));
	}
	
	/**
	 * set the range of the scale
	 * @param min
	 * @param max
	 */
	public void setRange(final double min, final double max) {
		this.minimum = min;
		this.maximum = max;
		scale.setRange(new Range(min, max));
	}
	
	/**
	 * @param majorTickMarkStepHint the majorTickMarkStepHint to set
	 */
	public void setMajorTickMarkStepHint(double majorTickMarkStepHint) {
		this.majorTickMarkStepHint = majorTickMarkStepHint;
		scale.setMajorTickMarkStepHint((int) majorTickMarkStepHint);
	}

	/**
	 * @param showMinorTicks the showMinorTicks to set
	 */
	public void setShowMinorTicks(final boolean showMinorTicks) {
		this.showMinorTicks = showMinorTicks;
		scale.setMinorTicksVisible(showMinorTicks);
	}

	
	/**
	 * @param showScale the showScale to set
	 */
	public void setShowScale(final boolean showScale) {
		this.showScale = showScale;
		scale.setVisible(showScale);
	}

	/**
	 * @param logScale the logScale to set
	 */
	public void setLogScale(final boolean logScale) {
		this.logScale = logScale;
		scale.setLogScale(logScale);
		scale.setRange(new Range(minimum, maximum));
	}	

	/**
	 * Sets, if this widget should have a transparent background.
	 * @param transparent
	 * 				The new value for the transparent property
	 */
	public void setTransparent(final boolean transparent) {
		this.transparent = transparent;
	}	

}
