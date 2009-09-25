package org.csstudio.opibuilder.widgets.model;

import org.csstudio.opibuilder.properties.BooleanProperty;
import org.csstudio.opibuilder.properties.WidgetPropertyCategory;


/**
 * The widget model for LED.
 * @author Xihui Chen
 *
 */
public class LEDModel extends AbstractBoolWidgetModel {

	
	
	/** The ID of the effect 3D property. */
	public static final String PROP_EFFECT3D = "effect_3d"; //$NON-NLS-1$
	
	/** The ID of the square LED property. */
	public static final String PROP_SQUARE_LED = "square_led"; //$NON-NLS-1$
	
	
	/** The default value of the height property. */	
	private static final int DEFAULT_HEIGHT = 20;
	
	/** The default value of the width property. */
	private static final int DEFAULT_WIDTH = 20;
	
	
	public LEDModel() {
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}
	
	@Override
	protected void configureProperties() {
		super.configureProperties();
		
		addProperty(new BooleanProperty(PROP_EFFECT3D, "3D Effect", 
				WidgetPropertyCategory.Display, true));
		
		addProperty(new BooleanProperty(PROP_SQUARE_LED, "Square LED", 
				WidgetPropertyCategory.Display, false));
	}
	/**
	 * The ID of this widget model.
	 */
	public static final String ID = "org.csstudio.opibuilder.widgets.LED"; //$NON-NLS-1$	
	
	@Override
	public String getTypeID() {
		return ID;
	}

	/**
	 * @return true if the widget would be painted with 3D effect, false otherwise
	 */
	public boolean isEffect3D() {
		return (Boolean) getProperty(PROP_EFFECT3D).getPropertyValue();
	}
	
	/**
	 * @return true if the LED is square, false otherwise
	 */
	public boolean isSquareLED() {
		return (Boolean) getProperty(PROP_SQUARE_LED).getPropertyValue();
	}
}
