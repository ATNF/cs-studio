package org.csstudio.config.ioconfig.model;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.config.ioconfig.model.pbmodel.ChannelDBO;
import org.csstudio.config.ioconfig.model.pbmodel.ChannelStructureDBO;
import org.csstudio.config.ioconfig.model.pbmodel.GSDFileDBO;
import org.csstudio.config.ioconfig.model.pbmodel.GSDModuleDBO;
import org.csstudio.config.ioconfig.model.pbmodel.MasterDBO;
import org.csstudio.config.ioconfig.model.pbmodel.ModuleDBO;
import org.csstudio.config.ioconfig.model.pbmodel.ModuleChannelPrototypeDBO;
import org.csstudio.config.ioconfig.model.pbmodel.ProfibusSubnetDBO;
import org.csstudio.config.ioconfig.model.pbmodel.SlaveDBO;
import org.csstudio.platform.AbstractCssPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractCssPlugin{

	/**
	 *  The plug-in ID.
	 */
	public static final String PLUGIN_ID = "org.csstudio.config.ioconfig.model";

	/**
	 *  The shared instance
	 */
    private static Activator plugin;

    private List<Class<?>> _classes;

    /**
     * The constructor
     */
    public Activator() {
        plugin = this;
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doStart(final BundleContext context) throws Exception {
        _classes = new ArrayList<Class<?>>();
        _classes.add(NodeImageDBO.class);
        _classes.add(ChannelDBO.class);
        _classes.add(ChannelStructureDBO.class);
        _classes.add(ModuleDBO.class);
        _classes.add(SlaveDBO.class);
        _classes.add(MasterDBO.class);
        _classes.add(ProfibusSubnetDBO.class);
        _classes.add(GSDModuleDBO.class);
        _classes.add(IocDBO.class);
        _classes.add(FacilityDBO.class);
        _classes.add(AbstractNodeDBO.class);
        _classes.add(GSDFileDBO.class);
        _classes.add(ModuleChannelPrototypeDBO.class);
        _classes.add(DocumentDBO.class);
        _classes.add(SearchNodeDBO.class);
        _classes.add(SensorsDBO.class);
        _classes.add(PV2IONameMatcherModelDBO.class);
        HibernateManager.addClasses(_classes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doStop(final BundleContext context) throws Exception {
        HibernateManager.removeClasses(_classes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

}
