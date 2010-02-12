/* 
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton, 
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND 
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR 
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. 
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, 
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION, 
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY 
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.sds.components.model;

import org.csstudio.sds.model.AbstractWidgetModel;
import org.csstudio.sds.model.WidgetPropertyCategory;
import org.csstudio.sds.model.optionEnums.TextAlignmentEnum;
import org.csstudio.sds.model.properties.ArrayOptionProperty;
import org.csstudio.sds.model.properties.BooleanProperty;
import org.csstudio.sds.model.properties.FontProperty;
import org.csstudio.sds.model.properties.IntegerProperty;
import org.csstudio.sds.model.properties.StringProperty;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;

/**
 * An action button widget model.
 * 
 * @author Sven Wende
 * @version $Revision$
 * 
 */
public final class ActionButtonModel extends AbstractWidgetModel {
	/**
	 * The ID of the label property.
	 */
	public static final String PROP_LABEL = "label"; //$NON-NLS-1$
	/**
	 * The ID of the font property.
	 */
	public static final String PROP_FONT = "font"; //$NON-NLS-1$
	/**
	 * The ID of the text alignment property.
	 */
	public static final String PROP_TEXT_ALIGNMENT = "textAlignment"; //$NON-NLS-1$
	/**
	 * The ID of the ActionData property.
	 */
	public static final String PROP_ACTION_PRESSED_INDEX = "action_pressed_index"; //$NON-NLS-1$
	/**
	 * The ID of the ActionData property.
	 */
	public static final String PROP_ACTION_RELEASED_INDEX = "action_released_index"; //$NON-NLS-1$
	/**
	 * The ID of the ToggelButton property.
	 */
	public static final String PROP_TOGGLE_BUTTON= "toggleButton"; //$NON-NLS-1$
	
	/**
	 * The ID of this widget model.
	 */
	public static final String ID = "org.csstudio.sds.components.ActionButton"; //$NON-NLS-1$

	/**
	 * The default value of the height property.
	 */
	private static final int DEFAULT_HEIGHT = 20;

	/**
	 * The default value of the width property.
	 */
	private static final int DEFAULT_WIDTH = 80;
	
	/**
	 * The default value of the Button style.  
	 */
    private static final boolean DEFAULT_TOGGLE_BUTTON = false;

	/**
	 * Standard constructor.
	 */
	public ActionButtonModel() {
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTypeID() {
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configureProperties() {
		addProperty(PROP_LABEL, new StringProperty("Label Text",
				WidgetPropertyCategory.Display, "")); //$NON-NLS-1$
		addProperty(PROP_FONT, new FontProperty("Font",
				WidgetPropertyCategory.Display, new FontData(
						"Arial", 8, SWT.NONE))); //$NON-NLS-1$
		addProperty(PROP_TEXT_ALIGNMENT, new ArrayOptionProperty("Text Alignment", 
				WidgetPropertyCategory.Display, TextAlignmentEnum.getDisplayNames() ,TextAlignmentEnum.CENTER.getIndex()));
		addProperty(PROP_ACTION_RELEASED_INDEX, new IntegerProperty("Action Index (released)",
				WidgetPropertyCategory.Behaviour, 0, -1, Integer.MAX_VALUE));
		addProperty(PROP_ACTION_PRESSED_INDEX, new IntegerProperty("Action Index (pressed)",
				WidgetPropertyCategory.Behaviour, -1, -1, Integer.MAX_VALUE));
		addProperty(PROP_TOGGLE_BUTTON, new BooleanProperty("Toggle Button",
		        WidgetPropertyCategory.Behaviour,DEFAULT_TOGGLE_BUTTON));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void markPropertiesAsInvisible() {
		this.markPropertyAsInvisible(PROP_BORDER_COLOR);
		this.markPropertyAsInvisible(PROP_BORDER_STYLE);
		this.markPropertyAsInvisible(PROP_BORDER_WIDTH);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getDefaultToolTip() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(createTooltipParameter(PROP_ALIASES)+"\n");
		buffer.append(createTooltipParameter(PROP_ACTIONDATA)+"\n");
		buffer.append("Performed Action: ");
		buffer.append(createTooltipParameter(PROP_ACTION_PRESSED_INDEX));
		buffer.append(createTooltipParameter(PROP_ACTION_RELEASED_INDEX));
		return buffer.toString();
	}

	/**
	 * Return the index of the selected WidgetAction from the ActionData.
	 * The Action is running when the button is released.
	 * @return The index
	 */
	public int getChoosenReleasedActionIndex() {
		return (Integer) getProperty(PROP_ACTION_RELEASED_INDEX).getPropertyValue();
	}
	
	/**
	 * Return the index of the selected WidgetAction from the ActionData.
	 * The Action is running when the button is pressed.
	 * @return The index
	 */
	public int getChoosenPressedActionIndex() {
		return (Integer) getProperty(PROP_ACTION_PRESSED_INDEX).getPropertyValue();
	}

	/**
	 * Return the label text.
	 * 
	 * @return The label text.
	 */
	public String getLabel() {
		return (String) getProperty(PROP_LABEL).getPropertyValue();
	}

	/**
	 * Return the label font.
	 * 
	 * @return The label font.
	 */
	public FontData getFont() {
		return (FontData) getProperty(PROP_FONT).getPropertyValue();
	}
	
	/**
	 * Returns the alignment for the text.
	 * @return int 
	 * 			0 = Center, 1 = Top, 2 = Bottom, 3 = Left, 4 = Right
	 */
	public int getTextAlignment() {
		return (Integer) getProperty(PROP_TEXT_ALIGNMENT).getPropertyValue();
	}

	/**
	 * Returns whether the button is a toggle button.
	 *  @return false = Push, true=Toggle
	 */
	public boolean isToggleButton(){
	    return (Boolean)getProperty(PROP_TOGGLE_BUTTON).getPropertyValue();
	}
}
