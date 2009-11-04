package org.csstudio.opibuilder.widgets.editparts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.opibuilder.editparts.AbstractPVWidgetEditPart;
import org.csstudio.opibuilder.editparts.ExecutionMode;
import org.csstudio.opibuilder.model.AbstractPVWidgetModel;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.properties.IWidgetPropertyChangeHandler;
import org.csstudio.opibuilder.util.OPIFont;
import org.csstudio.opibuilder.widgets.figures.ComboFigure;
import org.csstudio.opibuilder.widgets.model.ComboModel;
import org.csstudio.platform.data.IEnumeratedMetaData;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.ValueUtil;
import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.PVListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * 
 * @author Xihui Chen
 * 
 */
public final class ComboEditPart extends AbstractPVWidgetEditPart {

	private PVListener loadItemsFromPVListener;

	private IEnumeratedMetaData meta = null;
	
	private Combo combo;
	private SelectionListener comboSelectionListener;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IFigure doCreateFigure() {
		final ComboModel model = getWidgetModel();
		updatePropSheet(model.isItemsFromPV());		
		ComboFigure comboFigure = new ComboFigure((Composite) getViewer().getControl());
		comboFigure.setParentModel(getWidgetModel().getParent());
		comboFigure.setRunMode(getExecutionMode() == ExecutionMode.RUN_MODE);
		comboFigure.setFont(CustomMediaFactory.getInstance().getFont(
						model.getFont().getFontData()));
		combo = comboFigure.getCombo();
		//select the combo when mouse down
		combo.addMouseListener(new org.eclipse.swt.events.MouseAdapter(){
			@Override
			public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
				getViewer().select(ComboEditPart.this);
			}
		});
		//update tooltip
		combo.addMouseMoveListener(new MouseMoveListener(){
			public void mouseMove(org.eclipse.swt.events.MouseEvent e) {
				combo.setToolTipText(getWidgetModel().getTooltip());
			}
		});
		
		List<String> items = getWidgetModel().getItems();
		
		updateCombo(items);
		
		markAsControlPV(AbstractPVWidgetModel.PROP_PVNAME);

		//hook the context menu to combo
		combo.setMenu(getViewer().getContextMenu().createContextMenu(getViewer().getControl()));

		return comboFigure;
	}

	/**
	 * @param items
	 */
	private void updateCombo(List<String> items) {
		if(items !=null && getExecutionMode() == ExecutionMode.RUN_MODE){
			combo.removeAll();		
			
			for(String item : items){
				combo.add(item);
			}
			
			//write value to pv if pv name is not empty
			if(getWidgetModel().getPVName().trim().length() > 0){				
				if(comboSelectionListener !=null)				
					combo.removeSelectionListener(comboSelectionListener);
				comboSelectionListener = new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							setPVValue(AbstractPVWidgetModel.PROP_PVNAME, combo.getText());						
						}
				};
				combo.addSelectionListener(comboSelectionListener);		
			}
		
		}
	}

	@Override
	public ComboModel getWidgetModel() {
		return (ComboModel)getModel();
	}
	
	@Override
	protected void doActivate() {
		super.doActivate();
		//load items from PV
		if(getExecutionMode() == ExecutionMode.RUN_MODE){
			if(getWidgetModel().isItemsFromPV()){
				PV pv = getPV(AbstractPVWidgetModel.PROP_PVNAME);
				if(pv != null){	
					loadItemsFromPVListener = new PVListener() {					
						public void pvValueUpdate(PV pv) {
							IValue value = pv.getValue();
							if (value != null && value.getMetaData() instanceof IEnumeratedMetaData){
								IEnumeratedMetaData new_meta = (IEnumeratedMetaData)value.getMetaData();
								if(meta  == null || !meta.equals(new_meta)){
									meta = new_meta;
									List<String> itemsFromPV = new ArrayList<String>();
									for(String writeValue : meta.getStates()){										
										itemsFromPV.add(writeValue);
									}
									getWidgetModel().setPropertyValue(
											ComboModel.PROP_ITEMS, itemsFromPV);
								}
							}
						}					
						public void pvDisconnected(PV pv) {}
					};
					pv.addListener(loadItemsFromPVListener);				
				}
			}
		}
	}
	
	@Override
	protected void doDeActivate() {
		super.doDeActivate();
		if(getWidgetModel().isItemsFromPV()){
			PV pv = getPV(AbstractPVWidgetModel.PROP_PVNAME);
			if(pv != null){	
				pv.removeListener(loadItemsFromPVListener);
			}
		}
		((ComboFigure)getFigure()).dispose();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void registerPropertyChangeHandlers() {
		autoSizeWidget((ComboFigure) getFigure());
		// PV_Value
		IWidgetPropertyChangeHandler pvhandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue, final IFigure refreshableFigure) {
				if(newValue != null && newValue instanceof IValue){
					String stringValue = ValueUtil.getString((IValue)newValue);					
					combo.setText(stringValue);
					if(getWidgetModel().isBorderAlarmSensitve())
							autoSizeWidget((ComboFigure) refreshableFigure);
				}
					
				return true;
			}
		};
		setPropertyChangeHandler(ComboModel.PROP_PVVALUE, pvhandler);
		
		// Items
		IWidgetPropertyChangeHandler itemsHandler = new IWidgetPropertyChangeHandler() {
			@SuppressWarnings("unchecked")
			public boolean handleChange(final Object oldValue,
					final Object newValue, final IFigure refreshableFigure) {
				if(newValue != null && newValue instanceof List){
					updateCombo((List<String>)newValue);
					if(getWidgetModel().isItemsFromPV())
						combo.setText(ValueUtil.getString(getPVValue(AbstractPVWidgetModel.PROP_PVNAME)));
				}
				return true;
			}
		};
		setPropertyChangeHandler(ComboModel.PROP_ITEMS, itemsHandler);
		
		
		
		// font
		IWidgetPropertyChangeHandler fontHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue, final IFigure refreshableFigure) {
				FontData fontData = ((OPIFont)newValue).getFontData();
				refreshableFigure.setFont(CustomMediaFactory.getInstance().getFont(
						fontData.getName(), fontData.getHeight(),
						fontData.getStyle()));
				return true;
			}
		};
		setPropertyChangeHandler(ComboModel.PROP_FONT, fontHandler);
		
		final IWidgetPropertyChangeHandler handler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue, final IFigure refreshableFigure) {				
				updatePropSheet((Boolean) newValue);
				return false;
			}			
		};
		getWidgetModel().getProperty(ComboModel.PROP_ITEMS_FROM_PV).
			addPropertyChangeListener(new PropertyChangeListener(){
				public void propertyChange(PropertyChangeEvent evt) {
					handler.handleChange(evt.getOldValue(), evt.getNewValue(), getFigure());
				}
		});

		
		//size change handlers--always apply the default height
		IWidgetPropertyChangeHandler handle = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue, final Object newValue,
					final IFigure figure) {				
				autoSizeWidget((ComboFigure)figure);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractWidgetModel.PROP_WIDTH, handle);
		setPropertyChangeHandler(AbstractWidgetModel.PROP_HEIGHT, handle);
		setPropertyChangeHandler(AbstractWidgetModel.PROP_BORDER_STYLE, handle);	
		setPropertyChangeHandler(AbstractWidgetModel.PROP_BORDER_WIDTH, handle);	
		setPropertyChangeHandler(ComboModel.PROP_FONT, handle);
	}
	
		/**
		* @param actionsFromPV
		*/
	private void updatePropSheet(final boolean itemsFromPV) {
		getWidgetModel().setPropertyVisible(
				ComboModel.PROP_ITEMS, !itemsFromPV);	
	}

	private void autoSizeWidget(ComboFigure comboFigure) {		
		Dimension d = comboFigure.getAutoSizeDimension();
		getWidgetModel().setSize(getWidgetModel().getWidth(), d.height);
	}

}
