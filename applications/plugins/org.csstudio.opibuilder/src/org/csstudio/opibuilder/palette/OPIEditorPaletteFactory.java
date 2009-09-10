package org.csstudio.opibuilder.palette;

import java.util.List;
import java.util.Map;

import org.csstudio.opibuilder.feedback.IGraphicalFeedbackFactory;
import org.csstudio.opibuilder.util.WidgetDescriptor;
import org.csstudio.opibuilder.util.WidgetsService;
import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.jface.resource.ImageDescriptor;

/**The factory help to create the palette.
 * @author Xihui Chen
 *
 */
public class OPIEditorPaletteFactory {

	public static PaletteRoot createPalette(){
		PaletteRoot palette = new PaletteRoot();
		createPaletteContents(palette);
		return palette;
	}
	
	private static void createPaletteContents(PaletteRoot palette){
		Map<String, List<String>> categoriesMap = 
			WidgetsService.getInstance().getAllCategoriesMap();
		for(String category : categoriesMap.keySet()){
			PaletteDrawer categoryDrawer = new PaletteDrawer(category);
			for(String typeId : categoriesMap.get(category)){
				WidgetDescriptor widgetDescriptor = 
					WidgetsService.getInstance().getWidgetDescriptor(typeId);
				ImageDescriptor icon = CustomMediaFactory.getInstance().
					getImageDescriptorFromPlugin(
							widgetDescriptor.getPluginId(), widgetDescriptor.getIconPath());
				CombinedTemplateCreationEntry widgetEntry = new CombinedTemplateCreationEntry(
					widgetDescriptor.getName(), 
					widgetDescriptor.getDescription(),
					new WidgetCreationFactory(widgetDescriptor), icon, icon);
				
				IGraphicalFeedbackFactory feedbackFactory = 
					WidgetsService.getInstance().getWidgetFeedbackFactory(
							widgetDescriptor.getTypeID());
				if( feedbackFactory != null){
					widgetEntry.setToolClass(feedbackFactory.getCreationTool());
				}				
				categoryDrawer.add(widgetEntry);
			}
			palette.add(categoryDrawer);			
		}		
	}
	
}
