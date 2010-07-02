package org.csstudio.opibuilder.widgets.model;


import org.csstudio.opibuilder.model.AbstractPVWidgetModel;
import org.csstudio.opibuilder.properties.BooleanProperty;
import org.csstudio.opibuilder.properties.ColorProperty;
import org.csstudio.opibuilder.properties.ComboProperty;
import org.csstudio.opibuilder.properties.DoubleProperty;
import org.csstudio.opibuilder.properties.IntegerProperty;
import org.csstudio.opibuilder.properties.WidgetPropertyCategory;
import org.csstudio.opibuilder.util.OPIColor;
import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

/**
 * The abstract widget model for all shape based widgets.
 * @author Xihui Chen
 */
public abstract class AbstractShapeModel extends AbstractPVWidgetModel {
	
	/**The line sytle:
	 * 0: Solid,
	 * 1: Dash,
	 * 2: Dot,
	 * 3: DashDot,
	 * 4: DashDotDot.
	 * @author Xihui Chen
	 *
	 */
	public enum LineStyle {
		SOLID("Solid", SWT.LINE_SOLID),
		DASH("Dash", SWT.LINE_DASH),
		DOT("Dot", SWT.LINE_DOT),
		DASH_DOT("DashDot", SWT.LINE_DASHDOT),
		Dash_DOTDOT("DashDotDot", SWT.LINE_DASHDOTDOT);
				
		String description;
		int style;
		LineStyle(String description, int style){
			this.description = description;
			this.style = style;
		}
		
		/**
		 * @return SWT line style {SWT.LINE_SOLID,
			SWT.LINE_DASH, SWT.LINE_DOT, SWT.LINE_DASHDOT, SWT.LINE_DASHDOTDOT }
		 */
		public int getStyle() {
			return style;
		}
		
		@Override
		public String toString() {
			return description;
		}
		
		public static String[] stringValues(){
			String[] sv = new String[values().length];
			int i=0;
			for(LineStyle p : values())
				sv[i++] = p.toString();
			return sv;
		}
	}

	
	
	
	/**
	 * Width of the line.
	 */
	public static final String PROP_LINE_WIDTH = "line_width";//$NON-NLS-1$
	
	/**
	 * Style of the line.
	 */
	public static final String PROP_LINE_STYLE = "line_style";//$NON-NLS-1$
	
	/**
	 * Color of the line.
	 */
	public static final String PROP_LINE_COLOR = "line_color";//$NON-NLS-1$
	
	/**
	 * The widget can be filled with foreground color if this is not zero. 
	 * It must be a value between 0 to 100.
	 */
	public static final String PROP_FILL_LEVEL = "fill_level"; //$NON-NLS-1$
	
	/**
	 * True if fill direction is horizontal.
	 */
	public static final String PROP_HORIZONTAL_FILL = "horizontal_fill"; //$NON-NLS-1$
	/**
	 * True if anti alias is enabled for the figure.
	 */
	public static final String PROP_ANTIALIAS = "anti_alias"; //$NON-NLS-1$
	
	/** True if background is transparent. */
	public static final String PROP_TRANSPARENT = "transparent";	

	private static final RGB DEFAULT_LINE_COLOR = CustomMediaFactory.COLOR_PURPLE;
	
	
	public AbstractShapeModel() {
		setBackgroundColor(CustomMediaFactory.COLOR_BLUE);
		setForegroundColor(CustomMediaFactory.COLOR_RED);
		setPropertyValue(PROP_BORDER_ALARMSENSITIVE, false);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configureProperties() {
		addProperty(new IntegerProperty(PROP_LINE_WIDTH, "Line Width",
				WidgetPropertyCategory.Display, 0, 0, 100));
		addProperty(new ComboProperty(PROP_LINE_STYLE, "Line Style",
				WidgetPropertyCategory.Display, LineStyle.stringValues(), 0));
		addProperty(new ColorProperty(PROP_LINE_COLOR, "Line Color",
				WidgetPropertyCategory.Display, DEFAULT_LINE_COLOR));
		addProperty(new DoubleProperty(PROP_FILL_LEVEL, "Fill Level",
				WidgetPropertyCategory.Display, 0.0, 0.0, 100.0));
		addProperty(new BooleanProperty(PROP_HORIZONTAL_FILL, "Horizontal Fill", 
				WidgetPropertyCategory.Display, true));
		addProperty(new BooleanProperty(PROP_ANTIALIAS, "Anti Alias", 
				WidgetPropertyCategory.Display, true));
		addProperty(new BooleanProperty(PROP_TRANSPARENT, "Transparent",
				WidgetPropertyCategory.Display, false));

	}
	

	
	/**
	 * @return true if the graphics's antiAlias is on.
	 */
	public final boolean isAntiAlias(){
		return (Boolean)getCastedPropertyValue(PROP_ANTIALIAS);
	}
	
	/**
	 * Returns the fill grade.
	 * 
	 * @return the fill grade
	 */
	public final double getFillLevel() {
		return (Double) getProperty(PROP_FILL_LEVEL).getPropertyValue();
	}
	
	/**set the fill level
	 * @param value 
	 */
	public final void setFillLevel(final double value){
		setPropertyValue(PROP_FILL_LEVEL, value);
	}
	
	public boolean isHorizontalFill(){
		return (Boolean)getCastedPropertyValue(PROP_HORIZONTAL_FILL);
	}
	
	public void setHoizontalFill(boolean value){
		setPropertyValue(PROP_HORIZONTAL_FILL, value);
	}
	
	/**
	 * Gets the width of the line.
	 * @return int
	 * 				The width of the line
	 */
	public int getLineWidth() {
		return (Integer) getProperty(PROP_LINE_WIDTH).getPropertyValue();
	}
	
	public void setLineWidth(int width){
		setPropertyValue(PROP_LINE_WIDTH, width);
	}
	
	/**
	 * @param style the integer value corresponding to {@link LineStyle}
	 */
	public void setLineStyle(int style){
		setPropertyValue(PROP_LINE_STYLE, style);
	}
	
	
	/**
	 * Gets the style of the line.
	 * @return int
	 * 				The style of the line
	 */
	public int getLineStyle() {
		return LineStyle.values()[(Integer) getProperty(PROP_LINE_STYLE).
		                          getPropertyValue()].getStyle();
	}
	
	public RGB getLineColor(){
		return ((OPIColor)getPropertyValue(PROP_LINE_COLOR)).getRGBValue();
	}
	
	/**
	 * Returns, if this widget should have a transparent background.
	 * @return boolean
	 * 				True, if it should have a transparent background, false otherwise
	 */
	public boolean isTransparent() {
		return (Boolean) getProperty(PROP_TRANSPARENT).getPropertyValue();
	}
	
	
	public void setTransparent(boolean value){
		setPropertyValue(PROP_TRANSPARENT, value);
	}

}
