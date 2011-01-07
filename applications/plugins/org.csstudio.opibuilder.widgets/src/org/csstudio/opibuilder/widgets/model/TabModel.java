package org.csstudio.opibuilder.widgets.model;

import org.csstudio.opibuilder.model.AbstractContainerModel;
import org.csstudio.opibuilder.properties.ColorProperty;
import org.csstudio.opibuilder.properties.FilePathProperty;
import org.csstudio.opibuilder.properties.FontProperty;
import org.csstudio.opibuilder.properties.IntegerProperty;
import org.csstudio.opibuilder.properties.StringProperty;
import org.csstudio.opibuilder.properties.WidgetPropertyCategory;
import org.csstudio.opibuilder.util.MediaService;
import org.csstudio.opibuilder.util.ResourceUtil;
import org.csstudio.opibuilder.widgets.editparts.TabItem;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.RGB;

/**The model for a tab widget.
 * @author Xihui Chen
 *
 */
public class TabModel extends AbstractContainerModel {

	public enum TabProperty{
		TITLE("title", "Title"),		
		FONT("font", "Font"),
		FORECOLOR("foreground_color", "Foreground Color"),
		BACKCOLOR("background_color", "Background Color"),
		ICON_PATH("icon_path", "Icon Path");
		
		
		public String propIDPre;
		public String description;
		
		private TabProperty(String propertyIDPrefix, String description) {
			this.propIDPre = propertyIDPrefix;
			this.description = description;
		}
		
		@Override
		public String toString() {
			return description;
		}
	}	

	public interface ITabItemHandler{
		
		public void addTab(int index, TabItem tabItem);
		
		public void removeTab(int index);
	}
	
	public static final String PROP_TAB_COUNT = "tab_count"; //$NON-NLS-1$
	
	private static final RGB DEFAULT_TAB_FORECOLOR = new RGB(0,0,0);
	
	private static final RGB DEFAULT_TAB_BACKCOLOR = new RGB(255,255,255);
	
	private static final String DEFAULT_TAB_FONT = MediaService.DEFAULT_FONT; //$NON-NLS-1$

	public static final int MAX_TABS_AMOUNT = 20;
	
	private static final String[] FILE_EXTENSIONS = new String[] {"jpg", "jpeg", "gif", "bmp", "png"};

	/**
	 * The ID of this widget model.
	 */
	public static final String ID = "org.csstudio.opibuilder.widgets.tab"; //$NON-NLS-1$	
	
	private ITabItemHandler tabItemHandler;
	
	public TabModel() {
		setSize(300, 200);
	}
	
	
	@Override
	protected void configureProperties() {
		addProperty(new IntegerProperty(PROP_TAB_COUNT, "Tab Count",
				WidgetPropertyCategory.Behavior, 1, 1, MAX_TABS_AMOUNT));	
		addTabsProperties();		
	}
	
	private void addTabsProperties(){
		for(int i=0; i < MAX_TABS_AMOUNT; i++){
			for(TabProperty tabProperty : TabProperty.values())
				addTabProperty(tabProperty, i);
		}
	}

	private void addTabProperty(TabProperty tabProperty, final int tabIndex){		
		String propID = makeTabPropID(tabProperty.propIDPre, tabIndex);
		
		WidgetPropertyCategory category;
		category = new WidgetPropertyCategory(){
			@Override
			public String toString() {
				return NLS.bind("Tab {0}",tabIndex);
			}
		};
		
		switch (tabProperty) {
		case TITLE:
			addProperty(new StringProperty(propID, tabProperty.toString(), category, category.toString()));
			break;
		case FONT:
			addProperty(new FontProperty(propID, tabProperty.toString(), category, DEFAULT_TAB_FONT));
			break;
		case FORECOLOR:
			addProperty(new ColorProperty(propID, tabProperty.toString(), category, DEFAULT_TAB_FORECOLOR));
			break;
		case BACKCOLOR:
			addProperty(new ColorProperty(propID, tabProperty.toString(), category, DEFAULT_TAB_BACKCOLOR));
			break;	
		case ICON_PATH:
			addProperty(new FilePathProperty(propID, tabProperty.toString(), 
					category, null, FILE_EXTENSIONS));
			break;
		default:
			break;
		}
	}
	
	public static String makeTabPropID(String propIDPre, int index){
		return "tab_" + index + "_" + propIDPre; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	/**Get the property value of a tabItem.
	 * @param index index of the tab item.
	 * @param tabProperty the property.
	 * @return the value of the property.
	 */
	public Object getTabPropertyValue(int index, TabProperty tabProperty){
		return getPropertyValue(makeTabPropID(tabProperty.propIDPre, index));
	}

	
	/**
	 * @return The number of tabs.
	 */
	public int getTabsAmount() {
		return (Integer) getProperty(PROP_TAB_COUNT).getPropertyValue();
	}
	
	public IPath toAbsolutePath(IPath path){
		IPath absolutePath = path;
		if(absolutePath != null && !absolutePath.isEmpty() && !absolutePath.isAbsolute())
			absolutePath = ResourceUtil.buildAbsolutePath(this, absolutePath);
		return absolutePath;
	}
	
	
	/**Add a TabItem to the index;
	 * @param index
	 * @param tabItem
	 */
	public void addTab(int index, TabItem tabItem){			
		if(tabItemHandler != null){
			tabItemHandler.addTab(index, tabItem);
		}
	}
	
	/**Remove a tab.
	 * @param index
	 */
	public  void removeTab(int index){
		if(tabItemHandler != null){
			tabItemHandler.removeTab(index);
		}
	}


	/**
	 * @param tabItemHandler the tabItemHandler to set
	 */
	public void setTabItemHandler(ITabItemHandler tabItemHandler) {
		this.tabItemHandler = tabItemHandler;
	}


	/**
	 * @return the tabItemHandler
	 */
	public ITabItemHandler getTabItemHandler() {
		return tabItemHandler;
	}
	
	@Override
	public boolean isChildrenOperationAllowable() {
		return false;
	}
}
