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
package org.csstudio.sds.components.ui.internal.editparts;

import java.util.List;

import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.csstudio.sds.components.model.ActionButtonModel;
import org.csstudio.sds.components.ui.internal.figures.RefreshableActionButtonFigure;
import org.csstudio.sds.model.LabelModel;
import org.csstudio.sds.model.WidgetProperty;
import org.csstudio.sds.model.properties.actions.AbstractWidgetActionModel;
import org.csstudio.sds.ui.editparts.AbstractWidgetEditPart;
import org.csstudio.sds.ui.editparts.ExecutionMode;
import org.csstudio.sds.ui.editparts.IWidgetPropertyChangeHandler;
import org.csstudio.sds.ui.widgetactionhandler.WidgetActionHandlerService;
import org.eclipse.draw2d.ButtonModel;
import org.eclipse.draw2d.ChangeEvent;
import org.eclipse.draw2d.ChangeListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

/**
 * EditPart controller for the ActioButton widget. The controller mediates
 * between {@link ActionButtonModel} and {@link RefreshableActionButtonFigure}.
 * 
 * @author Sven Wende
 * 
 */
public final class ActionButtonEditPart extends AbstractWidgetEditPart {
    /**
     * The actual figure will be surrounded with a small frame that can be used to drag the figure
     * around (even if the cell editor is activated).
     */
    private static final int FRAME_WIDTH = 1;

    /**
     * The input field will be slightly brighter than the actual figure so it can be easily
     * recognized.
     */
    private static final int INPUT_FIELD_BRIGHTNESS = 10;
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IFigure doCreateFigure() {
		ActionButtonModel model = (ActionButtonModel) getWidgetModel();

		final RefreshableActionButtonFigure buttonFigure = new RefreshableActionButtonFigure();
		buttonFigure.setText(model.getLabel());
		buttonFigure.setFont(CustomMediaFactory.getInstance().getFont(
				model.getFont()));
		buttonFigure.setTextAlignment(model.getTextAlignment());
		buttonFigure.setEnabled(getExecutionMode().equals(
				ExecutionMode.RUN_MODE)
				&& model.isEnabled());
		buttonFigure.setStyle(model.isToggleButton());
		return buttonFigure;
	}

	/**
	 * Returns the Figure of this EditPart.
	 * 
	 * @return RefreshableActionButtonFigure The RefreshableActionButtonFigure
	 *         of this EditPart
	 */
	protected RefreshableActionButtonFigure getCastedFigure() {
		return (RefreshableActionButtonFigure) getFigure();
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public void performRequest(final Request req) {
        Object type = req.getType();

        // entering a value is only allowed in run mode and when the widget is
        // enabled
        if (type != null
                && (type.equals(RequestConstants.REQ_OPEN) || type
                        .equals(RequestConstants.REQ_DIRECT_EDIT))) {
            if (getExecutionMode() == ExecutionMode.RUN_MODE && getCastedModel().isEnabled()) {
                super.performRequest(req);
            } else if(getExecutionMode() == ExecutionMode.EDIT_MODE ){
                performEditTextValue();
            }
        }
        
    }
	
	/**
	 * Configures a listener for performing a {@link AbstractWidgetActionModel}.
	 * 
	 * @param figure
	 *            The figure of the widget
	 */
	private void configureButtonListener(
			final RefreshableActionButtonFigure figure) {
		figure.addChangeListener(new ChangeListener() {
			public void handleStateChanged(final ChangeEvent event) {
				final String propertyName = event.getPropertyName();
				CentralLogger.getInstance().debug(this, "ChangeEvent received, event.property=" + propertyName);

				// If the display is not in run mode or the button is not armed,
				// don't do anything.
				if (getExecutionMode() != ExecutionMode.RUN_MODE
						|| !figure.getModel().isArmed()) {
					return;
				}
				
				// If the button is a toggle button, the property that changes
				// when the toggle state changes is SELECTED_PROPERTY. For a
				// standard button, the PRESSED_PROPERTY is used, which reflects
				// mouse down/up events. The action index is then selected based
				// on whether the button is selected/deselected, or
				// pressed/released, respectively.
				// 
				// Note: we must use the same change listener because whether a
				// button is a toggle button can change dynamically. 
				int actionIndex = -1;
				final ActionButtonModel widget = (ActionButtonModel) getWidgetModel();
				if (widget.isToggleButton()) {
					if (ButtonModel.SELECTED_PROPERTY.equals(propertyName)) {
						if (figure.getModel().isSelected()) {
							actionIndex = widget.getChoosenPressedActionIndex();
							CentralLogger.getInstance().debug(this, "toggle=true, selected=true => using pressed action index: " + actionIndex);
						} else {
							actionIndex = widget.getChoosenReleasedActionIndex();
							CentralLogger.getInstance().debug(this, "toggle=true, selected=false => using released action index: " + actionIndex);
						}
					}
				} else {
					if (ButtonModel.PRESSED_PROPERTY.equals(propertyName)) {
						if (figure.getModel().isPressed()) {
							actionIndex = widget.getChoosenPressedActionIndex();
							CentralLogger.getInstance().debug(this, "toggle=false, pressed=true => using pressed action index: " + actionIndex);
						} else {
							actionIndex = widget.getChoosenReleasedActionIndex();
							CentralLogger.getInstance().debug(this, "toggle=false, pressed=false => using released action index: " + actionIndex);
						}
					}
				}
				
				List<AbstractWidgetActionModel> actions = widget.getActionData().getWidgetActions();

				// If an action should be used and there is only a single
				// action, use that action.
				if (actionIndex >= 0 && actions.size() == 1) {
					actionIndex = 0;
				}
				
				if (actionIndex >= 0 && actionIndex < actions.size()) {
					final AbstractWidgetActionModel action = actions.get(actionIndex);
					
					// The actual action can now be run asynchronously, because
					// all required data has been retrieved from the model.
					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							CentralLogger.getInstance().debug(this, "Performing widget action: " + action);
							WidgetActionHandlerService.getInstance().performAction(widget, action);
						}
					});
				}
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void registerPropertyChangeHandlers() {
		//
		RefreshableActionButtonFigure figure = getCastedFigure();
		this.configureButtonListener(figure);

		// label
		IWidgetPropertyChangeHandler labelHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue, final IFigure refreshableFigure) {
				RefreshableActionButtonFigure figure = getCastedFigure();
				figure.setText(newValue.toString());
				return true;
			}
		};
		setPropertyChangeHandler(ActionButtonModel.PROP_LABEL, labelHandler);
		// font
		IWidgetPropertyChangeHandler fontHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue, final IFigure refreshableFigure) {
				RefreshableActionButtonFigure figure = getCastedFigure();
				FontData fontData = (FontData) newValue;
				figure.setFont(CustomMediaFactory.getInstance().getFont(
						fontData.getName(), fontData.getHeight(),
						fontData.getStyle()));
				return true;
			}
		};
		setPropertyChangeHandler(LabelModel.PROP_FONT, fontHandler);

		// text alignment
		IWidgetPropertyChangeHandler alignmentHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue, final IFigure refreshableFigure) {
				RefreshableActionButtonFigure figure = (RefreshableActionButtonFigure) refreshableFigure;
				figure.setTextAlignment((Integer) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(ActionButtonModel.PROP_TEXT_ALIGNMENT,
				alignmentHandler);

		// button style
		IWidgetPropertyChangeHandler buttonStyleHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue, final IFigure refreshableFigure) {
				RefreshableActionButtonFigure figure = (RefreshableActionButtonFigure) refreshableFigure;
				figure.setStyle((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(ActionButtonModel.PROP_TOGGLE_BUTTON,
				buttonStyleHandler);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean forceDisabledInEditMode() {
		return true;
	}
	
	 private void performEditTextValue() {
	        CellEditor cellEditor = createCellEditor2();
	        locateCellEditor(cellEditor);
	        cellEditor.activate();
	        cellEditor.setFocus();
	    }

	    private CellEditor createCellEditor2() {
	        final CellEditor result = new TextCellEditor((Composite) getViewer().getControl());

	        // init cell editor...
	        String currentValue = "N/A"; //$NON-NLS-1$
	        WidgetProperty inputTextProperty = getWidgetModel().getProperty(
	                ActionButtonModel.PROP_LABEL);

	        if (inputTextProperty != null) {
	            currentValue = inputTextProperty.getPropertyValue().toString();
	        }

	        result.setValue(currentValue);
	        final Text text = (Text) result.getControl();
	        // input text
	        text.addKeyListener(new KeyAdapter() {
	            @Override
	            public void keyPressed(final KeyEvent e) {
	                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
	                    getWidgetModel().setPropertyValue(ActionButtonModel.PROP_LABEL,text.getText());
	                    result.deactivate();
	                    result.dispose();
	                } else if (e.keyCode == SWT.ESC) {
	                    result.deactivate();
	                    result.dispose();
	                }
	                
	            }

	        });
	        // get the chosen font
	        FontData fontData = (FontData) getWidgetModel().getProperty(ActionButtonModel.PROP_FONT)
	                .getPropertyValue();
	        Font font = CustomMediaFactory.getInstance().getFont(new FontData[] { fontData });

	        // get the chosen foreground color
	        Color foregroundColor = CustomMediaFactory.getInstance().getColor(
	                getWidgetModel().getForegroundColor());

	        // get the chosen background color
	        RGB backgroundRgb = getWidgetModel().getBackgroundColor();

	        int red = Math.min(backgroundRgb.red + INPUT_FIELD_BRIGHTNESS, 255);
	        int green = Math.min(backgroundRgb.green + INPUT_FIELD_BRIGHTNESS, 255);
	        int blue = Math.min(backgroundRgb.blue + INPUT_FIELD_BRIGHTNESS, 255);

	        Color backgroundColor = CustomMediaFactory.getInstance()
	                .getColor(new RGB(red, green, blue));

	        text.setForeground(foregroundColor);
	        text.setBackground(backgroundColor);
	        text.setFont(font);
	        text.selectAll();
	        
	        return result;
	    }
	    /**
	     * Locate the given cell editor .
	     * 
	     * @param cellEditor
	     *            A cell editor.
	     */
	    private void locateCellEditor(final CellEditor cellEditor) {
	        Rectangle rect = ActionButtonEditPart.this.figure.getBounds().getCopy();
	        rect.x = rect.x + FRAME_WIDTH;
	        rect.y = rect.y + FRAME_WIDTH;
	        rect.height = rect.height - (FRAME_WIDTH * 1);
	        rect.width = rect.width - (FRAME_WIDTH * 1);
	        getFigure().translateToAbsolute(rect);

	        cellEditor.getControl().setBounds(rect.x, rect.y, rect.width, rect.height);
	        cellEditor.getControl().setLayoutData(new GridData(SWT.CENTER));
	        cellEditor.getControl().setVisible(true);
	    }

}
