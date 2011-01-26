package org.csstudio.opibuilder.properties;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.csstudio.opibuilder.editparts.ExecutionMode;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jdom.Element;

/**The base widget property class for all kinds of widget property.
 * @author Xihui Chen
 *
 */
public abstract class AbstractWidgetProperty {
	
	protected String prop_id;
	
	protected String description; 
	
	private PropertyChangeSupport pcsDelegate;
	
	private PropertyDescriptor propertyDescriptor;
	
	protected Object propertyValue;
	
	protected Object defaultValue;
	
	protected WidgetPropertyCategory category;
	
	protected boolean visibleInPropSheet;
	
	protected ExecutionMode executionMode = ExecutionMode.EDIT_MODE;
	
	protected AbstractWidgetModel widgetModel;
	
	/**Widget Property Constructor
	 * @param prop_id the property id which should be unique in a widget model.
	 * @param description the description of the property,
	 * which will be shown as the property name in property sheet.
	 * @param category the category of the widget.
	 * @param defaultValue the default value when the widget is first created. It cannot be null.
	 */
	public AbstractWidgetProperty(String prop_id, String description,
			WidgetPropertyCategory category, Object defaultValue) {
		this.prop_id = prop_id;
		this.description = description;
		this.category = category;
		this.visibleInPropSheet = true;
		this.defaultValue = defaultValue;
		this.propertyValue = defaultValue;
		pcsDelegate = new PropertyChangeSupport(this);	
	}
	
	public synchronized final void addPropertyChangeListener(PropertyChangeListener listener){
		if(listener == null){
			return;
		}
		pcsDelegate.addPropertyChangeListener(listener);
	}
	
	/**Check if the requestNewValue is convertible or legal.
	 * @param value the value to be checked.
	 * @return The value after being checked. It might be coerced if the requestValue 
	 * is illegal. return null if it is not convertible or illegal.
	 */
	public abstract Object checkValue(final Object value);
	
	public final void firePropertyChange(final Object oldValue, final Object newValue){
		if(pcsDelegate.hasListeners(prop_id))
			pcsDelegate.firePropertyChange(prop_id, oldValue, newValue);
	}
	
	public final WidgetPropertyCategory getCategory() {
		return category;
	}

	public final Object getDefaultValue() {
		return defaultValue;
	}
	
	public boolean isDefaultValue(){
		return defaultValue.equals(propertyValue);
	}
	
	public final String getDescription() {
		return description;
	}

	public final IPropertyDescriptor getPropertyDescriptor() {
		if(propertyDescriptor == null)
			createPropertyDescriptor(visibleInPropSheet);
		return propertyDescriptor;
	}

	public final String getPropertyID() {
		return prop_id;
	}

	public Object getPropertyValue() {
		return propertyValue;
	}

	/**Get the formatted value to be displayed in property sheet. 
	 * @return 
	 */
	//public abstract Object getFormattedPropertyValue();
	
	public final boolean isVisibleInPropSheet() {
		return visibleInPropSheet;
	}

	public final void removeAllPropertyChangeListeners(){
		for(PropertyChangeListener l : pcsDelegate.getPropertyChangeListeners()){
			//if(l instanceof WidgetPropertyChangeListener)
			//	((WidgetPropertyChangeListener) l).removeAllHandlers();
			pcsDelegate.removePropertyChangeListener(l);
		}
	}
	
	public final PropertyChangeListener[] getAllPropertyChangeListeners(){
		return pcsDelegate.getPropertyChangeListeners();
	}
	
	public final void removePropertyChangeListener(PropertyChangeListener listener){
		if(listener instanceof WidgetPropertyChangeListener)
			((WidgetPropertyChangeListener) listener).removeAllHandlers();
		pcsDelegate.removePropertyChangeListener(listener);
	}

	public final void setCategory(WidgetPropertyCategory category) {
		this.category = category;
	}

	public final void setDescription(String description) {		
		this.description = description;
		createPropertyDescriptor(visibleInPropSheet);
	}

	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**Set property value and fire the listeners on the property.
	 * @param value
	 */
	public void setPropertyValue(Object value) {
		//do conversion and legally check
		Object newValue = checkValue(value);
		if(newValue == null || newValue.equals(propertyValue))
			return;
		Object oldValue= propertyValue;
		propertyValue = newValue;
		firePropertyChange(oldValue, newValue);				
	}
	
	/**Set property value and fire the listeners on the property.oldValue will
	 * be set as null. 
	 * @param value
	 */
	public void setPropertyValue_IgnoreOldValue(Object value) {
		//do conversion and legally check
		Object newValue = checkValue(value);
		if(newValue == null || newValue.equals(propertyValue))
			return;
		propertyValue = newValue;
		firePropertyChange(null, newValue);				
	}
		
	/**Set the property value.
	 * @param value the value to be set.
	 * @param fire true if listeners should be fired regardless the old value. 
	 * If false, only set the property value without firing listeners.
	 */
	public void setPropertyValue(Object value, boolean fire){
		if(fire){
			//do conversion and legally check
			Object newValue = checkValue(value);
			if(newValue == null)
				return;
			propertyValue = newValue;
			firePropertyChange(null, newValue);
				
		}else{
			Object newValue = checkValue(value);
			if(newValue == null || newValue.equals(propertyValue))
				return;
			propertyValue = newValue;
		}
	}
	
	/**
	 * @param visibleInPropSheet
	 * @return true if visibility changed.
	 */
	public final boolean setVisibleInPropSheet(boolean visibleInPropSheet) {
		if(visibleInPropSheet == this.visibleInPropSheet)
			return false;
		createPropertyDescriptor(visibleInPropSheet);
		this.visibleInPropSheet = visibleInPropSheet;
		return true;
	}
	
	private void createPropertyDescriptor(final boolean visibleInPropSheet){
		if(visibleInPropSheet){
			propertyDescriptor = createPropertyDescriptor();
			propertyDescriptor.setCategory(category.toString());
		}
		else
			propertyDescriptor = null;
	}
	
	/**
	 * Create the {@link IPropertyDescriptor}
	 */
	protected abstract PropertyDescriptor createPropertyDescriptor();

	/**
	 * Write the property value into a XML element.
	 * @param propElement
	 */
	public abstract void writeToXML(Element propElement);

	
	/**Read the property value from a XML element.
	 * @param propElement
	 * @return
	 */
	public abstract Object readValueFromXML(Element propElement) throws Exception;
	
	public void setWidgetModel(AbstractWidgetModel widgetModel) {
		this.widgetModel = widgetModel;
	}
	
	public void setExecutionMode(ExecutionMode executionMode) {
		this.executionMode = executionMode;
	}
	
	
	public ExecutionMode getExecutionMode() {
		return executionMode;
	}
	
	@Override
	public String toString() {
		return widgetModel.getName() + " : " + prop_id;
	}
	
}
