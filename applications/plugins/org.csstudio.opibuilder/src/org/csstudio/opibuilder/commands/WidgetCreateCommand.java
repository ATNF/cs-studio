package org.csstudio.opibuilder.commands;

import java.util.HashMap;
import java.util.Map;

import org.csstudio.opibuilder.model.AbstractContainerModel;
import org.csstudio.opibuilder.model.AbstractLayoutModel;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;

/**The command to add a widget to a container.
 * @author Xihui Chen
 *
 */
public class WidgetCreateCommand extends Command {
	
	private AbstractWidgetModel newWidget;
	
	private final AbstractContainerModel container;
	
	private Rectangle bounds;

	private boolean append;
	
	private Rectangle oldBounds;
	
	private int index = -1;
	

	/**
	 * @param newWidget The new Widget to be added.
	 * @param container the parent.
	 * @param bounds the bounds for the new widget
	 */
	public WidgetCreateCommand(AbstractWidgetModel newWidget, AbstractContainerModel
			container, Rectangle bounds, boolean append) {
		this.newWidget = newWidget;
		this.container  = container;
		this.bounds = bounds;
		this.append = append;
		setLabel("create widget");
	}
		
	@Override
	public boolean canExecute() {
		return newWidget != null && container != null;
	}
	
	@Override
	public void execute() {
		oldBounds = newWidget.getBounds();
		redo();
	}
	
	@Override
	public void redo() {
		if(newWidget instanceof AbstractLayoutModel && container.getLayoutWidget() != null){
			MessageDialog.openError(null, "Creating widget failed", 
					"There is already a layout widget in the container. " +
					"Please delete it before you can add a new layout widget.");
			return;
		}
		if(bounds != null){
			newWidget.setLocation(bounds.x, bounds.y);
			if (bounds.width > 0 && bounds.height > 0)
			newWidget.setSize(bounds.width, bounds.height);
		}
		boolean autoName = false; 
		for(AbstractWidgetModel child :container.getChildren()){
			if(child.getName().equals(newWidget.getName()))
				autoName = true;
		}
		if(autoName){
			Map<String, Integer> typeIDMap = new HashMap<String, Integer>();
			for(AbstractWidgetModel child : container.getChildren()){
				if(typeIDMap.containsKey(child.getTypeID()))
					typeIDMap.put(child.getTypeID(), typeIDMap.get(child.getTypeID())+1);
				else
					typeIDMap.put(child.getTypeID(), 0);
			}
			
			newWidget.setName(newWidget.getType() + "_" 	//$NON-NLS-1$
					+ (typeIDMap.get(newWidget.getTypeID())==null?
							0 : typeIDMap.get(newWidget.getTypeID()) + 1)); 
		}
		container.addChild(index, newWidget);
		container.selectWidget(newWidget, append);
	}
	
	@Override
	public void undo() {
		newWidget.setBounds(oldBounds);
		container.removeChild(newWidget);
	}

	public void setIndex(int index) {
		this.index = index;
	}

}
