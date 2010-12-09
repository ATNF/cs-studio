/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.opibuilder.model.AbstractContainerModel;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.util.MacrosInput;
import org.csstudio.opibuilder.widgetActions.ActionsInput;
import org.csstudio.opibuilder.widgetActions.OpenDisplayAction;
import org.csstudio.opibuilder.widgets.model.MenuButtonModel;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.RelatedDisplayItem;
import org.csstudio.utility.adlparser.fileParser.widgets.RelatedDisplay;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.RGB;

/**
 * Convert MEDMs related display to BOYs MenuButton 
 * 
 * @author John Hammonds, Argonne National Laboratory
 *
 */
public class RelatedDisplay2Model extends AbstractADL2Model {
	MenuButtonModel menuModel = new MenuButtonModel();

	public RelatedDisplay2Model(ADLWidget adlWidget, RGB[] colorMap,
			AbstractContainerModel parentModel) {
		super(adlWidget, colorMap, parentModel);
		parentModel.addChild(menuModel, true);
		processWidget(adlWidget);
	}

	public RelatedDisplay2Model(RGB[] colorMap) {
		super(colorMap);
	}

	/**
	 * @param adlWidget
	 */
	public void processWidget(ADLWidget adlWidget) {
		RelatedDisplay rdWidget = new RelatedDisplay(adlWidget);
		if (rdWidget != null) {
			setADLObjectProps(rdWidget, menuModel);
			setADLDynamicAttributeProps(rdWidget, menuModel);
			if (rdWidget.isForeColorDefined()) {
				menuModel.setForegroundColor(this.colorMap[rdWidget
						.getForegroundColor()]);
			}
			if (rdWidget.isBackColorDefined()) {
				menuModel.setBackgroundColor(this.colorMap[rdWidget
						.getBackgroundColor()]);
			}
			RelatedDisplayItem[] rdDisplays = rdWidget.getRelatedDisplayItems();
			if (rdDisplays.length > 0) {
				ActionsInput ai = menuModel.getActionsInput();
				for (int ii = 0; ii < rdDisplays.length; ii++) {
					if (!(rdDisplays[ii].getFileName().replaceAll("\"", "")
							.equals(""))) {
						OpenDisplayAction odAction = createOpenDisplayAction(rdDisplays[ii]);
						ai.addAction(odAction);
					}
				}
			}
		}
		String label = rdWidget.getLabel();
		if (label != null) {
			if (label.startsWith("-")) { // leading "-" was used to flag not
											// using the icon. Just don't use
											// the icon and throw this away
				label = label.substring(1);
			}
		}
		menuModel.setPropertyValue(MenuButtonModel.PROP_LABEL, label);
	}

	/**
	 * @param rdDisplays
	 * @param ii
	 * @return
	 */
	public OpenDisplayAction createOpenDisplayAction(
			RelatedDisplayItem rdDisplay) {
		OpenDisplayAction odAction = new OpenDisplayAction();

		// Try to add the filename to the PROP_PATH
		IPath fPath = new Path(rdDisplay.getFileName().replaceAll("\"", "")
				.replace(".adl", ".opi"));
		System.out.println("Related display file: "
				+ rdDisplay.getFileName().replace(".adl", ".opi"));
		odAction.setPropertyValue(OpenDisplayAction.PROP_PATH, fPath);

		// Try to add macros
		addMacrosToOpenDisplayAction(rdDisplay, odAction);
		if (rdDisplay.getLabel() != null) {
			odAction.setPropertyValue(OpenDisplayAction.PROP_DESCRIPTION,
					rdDisplay.getLabel().replaceAll("\"", ""));
		}
		if ((rdDisplay.getPolicy() != null)) { // policy is present
			if (rdDisplay.getPolicy().equals("replace display")) { // replace
																	// the
																	// display
				odAction.setPropertyValue(OpenDisplayAction.PROP_REPLACE, true);
			} else { // don't replace the display
				odAction.setPropertyValue(OpenDisplayAction.PROP_REPLACE, false);
			}
		} else { // policy not present go to default
			odAction.setPropertyValue(OpenDisplayAction.PROP_REPLACE, false); // don't
																				// replace
																				// the
																				// display
		}
		return odAction;
	}

	/**
	 * @param rdDisplays
	 * @param ii
	 * @param odAction
	 */
	public void addMacrosToOpenDisplayAction(RelatedDisplayItem rdDisplay,
			OpenDisplayAction odAction) {
		if (rdDisplay.getArgs() != null && !rdDisplay.getArgs().isEmpty()) {
			String args = rdDisplay.getArgs().replaceAll("\"", "");
			MacrosInput macIn = makeMacros(args);
			odAction.setPropertyValue(OpenDisplayAction.PROP_MACROS, macIn);
		}
	}

	/**
	 * @param args
	 * @return
	 */
	public MacrosInput makeMacros(String args) {
		String resArgs = removeParentMacros(args);
		String argsIn = "true, " + resArgs;
		MacrosInput macIn = null;
		try {
			macIn = MacrosInput.recoverFromString(argsIn);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return macIn;
	}

	/**
	 * Remove parent macros (i.e. P=$(P))from the list.  We can now pass parent Macros.
	 * @param args
	 * @return
	 */
	public String removeParentMacros(String args) {
		String[] argList = args.split(",");
		StringBuffer strBuff = new StringBuffer();
		for (int ii = 0; ii < argList.length; ii++) {
			String[] argParts = argList[ii].split("=");
			if (!argParts[1].replaceAll(" ", "").equals(
					"$(" + argParts[0].trim() + ")")) {
				if (strBuff.length() != 0)
					strBuff.append(", ");
				strBuff.append(argList[ii]);
			}
		}
		String resArgs = strBuff.toString();
		return resArgs;
	}

	@Override
	public AbstractWidgetModel getWidgetModel() {
		return menuModel;
	}

	public void cleanup() {
		menuModel = null;
	}
}
