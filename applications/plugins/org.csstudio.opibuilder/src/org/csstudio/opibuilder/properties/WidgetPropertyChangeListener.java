package org.csstudio.opibuilder.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.opibuilder.datadefinition.WidgetIgnorableUITask;
import org.csstudio.opibuilder.editparts.AbstractBaseEditPart;
import org.csstudio.opibuilder.util.GUIRefreshThread;
import org.eclipse.draw2d.IFigure;

/**
 * The listener on widget property change.
 * 
 * @author Sven Wende (class of same name in SDS)
 * @author Xihui Chen 
 *
 */
public class WidgetPropertyChangeListener implements PropertyChangeListener {

	private AbstractBaseEditPart editpart;
	private AbstractWidgetProperty widgetProperty;
	private List<IWidgetPropertyChangeHandler> handlers;
	
	/**Constructor.
	 * @param editpart backlint to the editpart, which uses this listener.
	 */
	public WidgetPropertyChangeListener(AbstractBaseEditPart editpart,
			AbstractWidgetProperty property) {
		this.editpart = editpart;
		this.widgetProperty = property;
		handlers = new ArrayList<IWidgetPropertyChangeHandler>();
	}
	
	public void propertyChange(final PropertyChangeEvent evt) {
		Runnable runnable = new Runnable() {			
			public void run() {
				if(editpart == null || !editpart.isActive()){
					return;
				}
				for(IWidgetPropertyChangeHandler h : handlers) {
					IFigure figure = editpart.getFigure();
					boolean repaint = h.handleChange(
							evt.getOldValue(), evt.getNewValue(), figure);
					if(repaint)
						figure.repaint();
				}
			}
		};		
		WidgetIgnorableUITask task = new WidgetIgnorableUITask(widgetProperty, runnable);
		
		GUIRefreshThread.getInstance().addIgnorableTask(task);
	}
	
	/**Add handler, which is informed when a property changed.
	 * @param handler
	 */
	public void addHandler(final IWidgetPropertyChangeHandler handler) {
		assert handler != null;
		handlers.add(handler);
	}
	
	public void removeAllHandlers(){
		handlers.clear();
	}

}
