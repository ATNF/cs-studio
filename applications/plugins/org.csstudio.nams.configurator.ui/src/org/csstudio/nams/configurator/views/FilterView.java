package org.csstudio.nams.configurator.views;

import org.csstudio.nams.configurator.beans.IConfigurationBean;

public class FilterView extends AbstractNamsView {

	public static final String ID = "org.csstudio.nams.configurator.filter";

	@Override
	protected IConfigurationBean[] getTableContent() {
		return configurationBeanService.getFilterBeans();
	}

}
