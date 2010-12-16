/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.clientmodel;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.csstudio.alarm.beast.AlarmTreePath;
import org.csstudio.alarm.beast.Preferences;
import org.csstudio.alarm.beast.SeverityLevel;
import org.csstudio.alarm.beast.client.AlarmConfiguration;
import org.csstudio.alarm.beast.client.AlarmTreeItem;
import org.csstudio.alarm.beast.client.AlarmTreePV;
import org.csstudio.alarm.beast.client.AlarmTreeRoot;
import org.csstudio.alarm.beast.client.GDCDataStructure;
import org.csstudio.alarm.beast.ui.Messages;
import org.csstudio.apputil.time.BenchmarkTimer;
import org.csstudio.platform.logging.CentralLogger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

/** Model of alarm information for client applications.
 *  <p>
 *  Obtains alarm configuration (PVs, their hierarchy, guidance, status, ...)
 *  from RDB, then monitors JMS for changes,
 *  sends out acknowledgments or changes to JMS,
 *  signals listeneres about updates, ...
 *
 *  @author Kay Kasemir, Xihui Chen
 */
public class AlarmClientModel
{
    /** Singleton instance */
    private static volatile AlarmClientModel instance = null;

    /** Reference count for instance */
    private volatile int references = 0;

    /** Name of this configuration, i.e. root element of the configuration tree */
	private String config_name;

    /** Names of all available configuration, i.e. all root elements */
    private String config_names[] = new String[0];

	/** Have we recently heard from the server? */
    private volatile boolean server_alive = false;

    /** Do we think the server is in maintenance mode? */
    private volatile boolean maintenance_mode = false;

    /** Connection to configuration/state snapshot.
     *  <br><b>SYNC:</b> Access needs to synchronize on <code>this</code>
     */
    private AlarmConfiguration config;

    /** JMS Connection to server: alarm updates, send acknowledgment.
     *  May be <code>null</code> when we (re-)read the configuration.
     *
     *  SYNC: on communicator_lock for access.
     */
    private AlarmClientCommunicator communicator = null;

    /** Lock for <code>communicator</code> */
    final private Object communicator_lock = new Object();

	/** Root of the alarm tree.
     *  <br><b>SYNC:</b> Access needs to synchronize on <code>this</code>
     *  Usually this would be the same as config.getConfigTree(),
     *  but initially and after errors it will be a pseudo-alarm-tree
     *  that shows error messages
     */
    private AlarmTreeRoot config_tree;

    /** Array of items which are currently in alarm
     *  <br><b>SYNC:</b> Access needs to synchronize on <code>this</code>
     */
    private ArrayList<AlarmTreePV> active_alarms = new ArrayList<AlarmTreePV>();

    /** Array of items which are in alarm but acknowledged
     *  <br><b>SYNC:</b> Access needs to synchronize on <code>this</code>
     */
    private ArrayList<AlarmTreePV> acknowledged_alarms = new ArrayList<AlarmTreePV>();

    /** Listeners who registered for notifications */
    final private CopyOnWriteArrayList<AlarmClientModelListener> listeners =
        new CopyOnWriteArrayList<AlarmClientModelListener>();

    /** Send events? */
    private boolean notify_listeners = true;

    /** @return <code>true</code> for read-only model */
    final private boolean allow_write = ! Preferences.isReadOnly();

	/** Initialize client model */
    private AlarmClientModel() throws Exception
    {
    	config_name = Preferences.getAlarmTreeRoot();

        // Initial dummy alarm info
        createPseudoAlarmTree(Messages.AlarmClientModel_NotInitialized);

        new ReadConfigJob(this).schedule();
    }

    /** Obtain the shared instance.
     *  <p>
     *  Increments the reference count.
     *  @see #release()
     *  @return Alarm client model instance
     *  @throws Exception on error
     */
    public static AlarmClientModel getInstance() throws Exception
    {
        if (instance == null)
            instance = new AlarmClientModel();
        ++instance.references;
        return instance;
    }

    /** Release the 'instance' */
    private static void releaseInstance()
    {
    	instance = null;
    }

    /** Must be called to release model when no longer used.
     *  <p>
     *  Based on reference count, model is closed when last
     *  user releases it.
     */
    public void release()
    {
        synchronized (acknowledged_alarms)
        {
            --references;
            if (references > 0)
                return;
        }
        try
        {
            CentralLogger.getInstance().getLogger(this)
                .debug("AlarmClientModel closed."); //$NON-NLS-1$
            // Don't lock the model while closing the communicator
            // because communicator could right now be in a model
            // update which in turn already locks the model -> deadlock
            synchronized (communicator_lock)
            {
                if (communicator != null)
                {
                	communicator.stop();
                	communicator = null;
                }
            }
            synchronized (this)
            {
                if (config != null)
                {
                    config.close();
                    config = null;
                }
            }
        }
        catch (Exception ex)
        {
            CentralLogger.getInstance().getLogger(this).warn(ex);
        }
        releaseInstance();
    }

    /** List all configuration 'root' element names, i.e. names
     *  of all possible configurations, including the current one.
     *  @return Array of 'root' elements.
     *          May be empty but non-<code>null</code> while model is still reading from RDB.
     */
    public synchronized String[] getConfigurationNames()
    {
    	return config_names;
    }

    /** @return Name of this configuration, i.e. name of its root element */
    public synchronized String getConfigurationName()
    {
    	return config_name;
    }

    /** Load a new alarm configuration.
     *  Ignored if the 'new' configuration name matches the current name.
     *
     *  @param new_root_name Name of configuration to load
     *  @param listener Listener that's notified when done
     */
    public void setConfigurationName(final String new_root_name,
    		final AlarmClientModelConfigListener listener)
    {
    	synchronized (this)
        {
        	if (new_root_name.equals(config_name))
        		return;

        	// Update config. name
        	config_name = new_root_name;
        }

    	// Update GUI with 'empty' model
        createPseudoAlarmTree(Messages.AlarmClientModel_NotInitialized);
        fireNewConfig();

    	// Clear JMS communicator because it uses topics of the old config. name
        synchronized (communicator_lock)
        {
	    	if (communicator != null)
	    	{
		    	// Close old communicator
		    	communicator.stop();
		    	communicator = null;
	    	}
        }

        // Load new configuration:
        // Create new JMS communicator, read from RDB, fire events, ...
    	new ReadConfigJob(this, listener).schedule();
    }

    /** @return <code>true</code> if model allows write access
     *          (acknowledge, update config)
     */
    public boolean isWriteAllowed()
    {
        return allow_write;
    }

    /** @param listener Listener to add */
    public void addListener(final AlarmClientModelListener listener)
    {
        listeners.add(listener);
    }

    /** @param listener Listener to remove */
    public void removeListener(final AlarmClientModelListener listener)
    {
        listeners.remove(listener);
    }

    /** Read alarm configuration.
     *  May be invoked from ReadConfigJob.
     *  @param monitor Progress monitor (has not been called)
     */
    @SuppressWarnings("nls")
    void readConfiguration(final IProgressMonitor monitor)
    {
        final BenchmarkTimer timer = new BenchmarkTimer();
        monitor.beginTask(Messages.AlarmClientModel_ReadingConfiguration, IProgressMonitor.UNKNOWN);

        // While we read the RDB, new alarms could arrive.
        // To avoid missing them, we assert that we are connected to JMS.
        // Check if we need to create a NEW communicator
        final AlarmClientCommunicator comm;
        synchronized (communicator_lock)
        {
	        if (communicator == null)
	        {   // New communicator will queue received events until we
		        // read the whole configuration.
		    	try
		    	{
			    	communicator = new AlarmClientCommunicator(this);
			        communicator.start();
		    	}
		    	catch (Exception ex)
		    	{
		    		CentralLogger.getInstance().getLogger(this).error("Cannot start AlarmClientCommunicator", ex); //$NON-NLS-1$
		    		return;
		    	}
	        }
	        else
	        {	// Switch existing communicator to queue mode
	        	communicator.setQueueMode(true);
	        }
	        comm = communicator;
        }
        // Wait for JMS connection
        int wait = 0;
        while (!comm.isConnected())
        {
            monitor.subTask(NLS.bind(Messages.AlarmClientModel_WaitingForJMSFmt, ++wait));
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            if (monitor.isCanceled())
            {
                monitor.done();
                return;
            }
        }

        // Read RDB
        monitor.subTask(Messages.AlarmClientModel_ReadingRDB);
        try
        {
            synchronized (this)
            {
                // Prevent a flurry of events while items with alarms are added
                notify_listeners = false;
                if (config != null)
                    config.close();
                active_alarms.clear();
                acknowledged_alarms.clear();
                config = null;
                // Note config_tree stays as it was...
            }
            // When reading config, create the root element
            // that links to the model instead of the default AlarmTreeRoot
            final AlarmConfiguration new_config =
                new AlarmConfiguration(Preferences.getRDB_Url(),Preferences.getRDB_User(),
                					   Preferences.getRDB_Password(),
                                       getConfigurationName(),false)
            {
                @Override
                protected AlarmTreeRoot createAlarmTreeRoot(int id,
                        String root_name)
                {
                    return new AlarmClientModelRoot(id, root_name,
                                                    AlarmClientModel.this);
                }
            };
            // Read names of available configurations
            final String new_root_names[] = new_config.listConfigurations();
            // Update model with newly received data
            synchronized (this)
            {
                config = new_config;
                config_tree = config.getAlarmTree();
                config_names = new_root_names;
                // active_alarms & acknowledged_alarms already populated
                // because fireNewAlarmState() was called while building
                // the alarm tree
            }
        }
        catch (Exception ex)
        {
            CentralLogger.getInstance().getLogger(this).error(ex);
            createPseudoAlarmTree("Alarm RDB Error: " + ex.getMessage()); //$NON-NLS-1$
        }
        // Info about performance
        timer.stop();
        final Logger logger = CentralLogger.getInstance().getLogger(this);
        if (logger.isInfoEnabled())
        {
            final int count;
            final int pv_count;
        	synchronized (this)
            {
                count = config_tree.getElementCount();
                pv_count = config_tree.getPVCount();
            }
            logger.info(String.format(
                "Read %d alarm tree items, %d PVs in %.2f seconds: %.1f items/sec, %.1f PVs/sec\n",
                count, pv_count, timer.getSeconds(),
                count / timer.getSeconds(),
                pv_count/timer.getSeconds()));
        }

        // After we received configuration, handle updates that might
        // have accumulated.
        comm.setQueueMode(false);
        // Re-enable events, send a single notification.
        synchronized (this)
        {
            notify_listeners = true;
        }
        fireNewConfig();
        monitor.done();
    }

    /** @return Name of JMS server or some text that indicates
     *          disconnected state. For information, not to determine
     *          exact connection state.
     */
    public String getJMSServerInfo()
    {
        synchronized (communicator_lock)
        {
        	if (communicator != null)
        		return communicator.toString();
        }
        return "No Communicator"; //$NON-NLS-1$
    }

    /** Invoked by AlarmClientCommunicator whenever an 'IDLE'
     *  message was received from the server
     *  @param maintenance_mode
     */
    public void updateServerState(boolean maintenance_mode)
    {
        // Tell GUI that there is a server.
        if (! server_alive)
        {
            server_alive = true;
            fireNewAlarmState(null);
        }
        // Change in maintenance mode?
        if (this.maintenance_mode  != maintenance_mode)
        {
            this.maintenance_mode = maintenance_mode;
            fireModeUpdate();
        }
    }

    /** @return <code>true</code> if we received updates from server,
     *          <code>false</code> after server communication timeout
     */
    public boolean isServerAlive()
    {
        return server_alive;
    }

    /** @return <code>true</code> if we assume server is in maintenance mode,
     *          <code>false</code> for 'normal' mode.
     */
    public boolean inMaintenanceMode()
    {
        return maintenance_mode;
    }

    /** Send request to enable/disable maintenance mode to alarm server
     *  @param maintenance <code>true</code> to enable
     */
    public void requestMaintenanceMode(final boolean maintenance)
    {
        synchronized (communicator_lock)
        {
        	if (allow_write  &&  communicator != null)
        		communicator.requestMaintenanceMode(maintenance);
        }
    }

    /** @return root of the alarm tree configuration */
    synchronized public AlarmTreeRoot getConfigTree()
    {
        return config_tree;
    }

    /** Get the currently active alarms.
     *  <p>
     *  @return Array of active alarms. May be empty, but not <code>null</code>.
     */
    synchronized public AlarmTreePV[] getActiveAlarms()
    {
        final AlarmTreePV array[] = new AlarmTreePV[active_alarms.size()];
        return active_alarms.toArray(array);
    }

    /** Get the acknowledged alarms: Still in alarm, but ack'ed.
     *  <p>
     *  @return Array of active alarms. May be empty, but not <code>null</code>.
     */
    synchronized public AlarmTreePV[] getAcknowledgedAlarms()
    {
        final AlarmTreePV array[] = new AlarmTreePV[acknowledged_alarms.size()];
        return acknowledged_alarms.toArray(array);
    }

    /** Add a component to the model and RDB
     *  @param root_or_component Root or Component under which to add the component
     *  @param name Name of the new component
     *  @throws Exception on error
     */
    public void addComponent(final AlarmTreeItem root_or_component,
            final String name) throws Exception
    {
        if (! allow_write)
            return;
        synchronized (this)
        {
            if (config == null)
                return;
            config.addComponent(root_or_component, name);
        }
        synchronized (communicator_lock)
        {
        	if (communicator != null)
        		communicator.sendConfigUpdate(AlarmTreePath.makePath(root_or_component.getPathName(), name));
        }
    }

    /** Add a PV to the model and config storage (RDB)
     *  @param component Component under which to add the PV
     *  @param name Name of the new PV
     *  @throws Exception on error
     */
    public void addPV(final AlarmTreeItem component,
                      final String name) throws Exception
    {
        if (! allow_write)
            return;
        synchronized (this)
        {
            if (config == null)
                return;
            config.addPV(component, name);
        }
        // Notify via JMS, then add to local model in response to notification.
        synchronized (communicator_lock)
        {
        	if (communicator != null)
                communicator.sendConfigUpdate(
                        AlarmTreePath.makePath(component.getPathName(), name));
        }
    }

    /** Change an items configuration in RDB.
     *  @param item Item to configure (which already exists, won't be created)
     *  @param guidance Guidance strings
     *  @param displays Related displays
     *  @param commands Commands
     *  @throws Exception on error
     */
    public void configureItem(final AlarmTreeItem item,
            final GDCDataStructure guidance[], final GDCDataStructure displays[],
            final GDCDataStructure commands[]) throws Exception
    {
        if (! allow_write)
            return;
        synchronized (this)
        {
            if (config == null)
                return;
            config.configureItem(item, guidance, displays, commands);
        }
        synchronized (communicator_lock)
        {
        	if (communicator != null)
        		communicator.sendConfigUpdate(item.getPathName());
        }
    }

    /** Change a PV's configuration in RDB.
     *  @param pv PV
     *  @param description Description
     *  @param enabled Are alarms enabled?
     *  @param annunciate Annunciate or not?
     *  @param latch Latch highest alarms?
     *  @param delay Alarm delay [seconds]
     *  @param count Count of severity != OK within delay to detect as alarm
     *  @param filter Filter expression for enablement
     *  @param guidance Guidance strings
     *  @param displays Related displays
     *  @param commands Commands
     *  @throws Exception on error
     */
    public void configurePV(final AlarmTreePV pv, final String description,
        final boolean enabled, final boolean annunciate, final boolean latch,
        final int delay, final int count, final String filter,
        final GDCDataStructure guidance[], final GDCDataStructure displays[],
        final GDCDataStructure commands[]) throws Exception
    {
        if (! allow_write)
            return;
        synchronized (this)
        {
            if (config == null)
                return;
            config.configurePV(pv, description, enabled, annunciate,
                    latch, delay, count, filter,
                    guidance, displays, commands);
        }
        synchronized (communicator_lock)
        {
        	if (communicator != null)
        		communicator.sendConfigUpdate(pv.getPathName());
        }
    }

    /** Change item's name
     *  @param item Item to change
     *  @param new_name New name for the item
     *  @throws Exception on error
     */
    public void rename(final AlarmTreeItem item, final String new_name) throws Exception
    {
        if (! allow_write)
            return;
        synchronized (this)
        {
            if (config == null)
                return;
            config.rename(item, new_name);
        }
        synchronized (communicator_lock)
        {
        	if (communicator != null)
        		communicator.sendConfigUpdate(null);
        }
    }

    /** Change item's location in alarm configuration hierarchy.
     *  This does not actually change the item's position in the in-memory
     *  model. It does change the RDB configuration and trigger an update,
     *  which the client should then receive and consequently re-load the
     *  whole model.
     *  @param item Item to move
     *  @param new_path New path for the item
     *  @throws Exception on error
     */
    public void move(final AlarmTreeItem item, final String new_path) throws Exception
    {
        if (! allow_write)
            return;
        synchronized (this)
        {
            if (config == null)
                return;
            config.move(item, new_path);
        }
        synchronized (communicator_lock)
        {
        	if (communicator != null)
        		communicator.sendConfigUpdate(null);
        }
    }

    /** Create new PV by copying existing PV
     *  @param pv Existing PV
     *  @param new_path_and_pv Complete path, including PV name, of PV-to-create
     *  @throws Exception on error in new path, duplicate PV name, error while
     *          adding new PV
     */
    @SuppressWarnings("nls")
    public void duplicatePV(final AlarmTreePV pv, final String new_path_and_pv) throws Exception
    {
        if (! allow_write)
            return;
        synchronized (this)
        {
            if (config == null)
                return;
            // Determine path and name of new PV
            final String new_pieces[] = AlarmTreePath.splitPath(new_path_and_pv);
            final int new_pieces_len = new_pieces.length;
            // Need at least "area/pv"
            if (new_pieces_len < 2)
                throw new Exception("New path too short");
            final String new_path =
                AlarmTreePath.makePath(new_pieces, new_pieces_len - 1);
            final String new_name = new_pieces[new_pieces_len - 1];
            if (new_name.equals(pv.getName()))
                throw new Exception("New PV name must differ from existing PV name");

            // Locate parent item
            final AlarmTreeItem new_parent =
                config.getAlarmTree().getItemByPath(new_path);
            if (new_parent == null)
                throw new Exception("Cannot locate parent entry: " + new_path);
            if (new_parent instanceof AlarmTreePV)
                throw new Exception("Parent entry has wrong type: " + new_path);

            // Add new PV
            final AlarmTreePV new_pv =
                config.addPV(new_parent, new_name);
            // Update configuration of new PV to match duplicated PV
            config.configurePV(new_pv, pv.getDescription(), pv.isEnabled(),
                    pv.isAnnunciating(), pv.isLatching(), pv.getDelay(),
                    pv.getCount(), pv.getFilter(), pv.getGuidance(),
                    pv.getDisplays(), pv.getCommands());
        }
        // This will trigger an update the configuration of new_pv
        // in this model as well as other alarm system listeners
        synchronized (communicator_lock)
        {
        	if (communicator != null)
        		communicator.sendConfigUpdate(new_path_and_pv);
        }
    }

    /** Remove item and all sub-items from alarm tree.
     *  @param item Item to remove
     *  @throws Exception on error
     */
    public void remove(final AlarmTreeItem item) throws Exception
    {
        if (! allow_write)
            return;
        synchronized (this)
        {
            if (config == null)
                return;
            config.remove(item);
        }
        synchronized (communicator_lock)
        {
        	if (communicator != null)
        		communicator.sendConfigUpdate(null);
        }
    }

    /** Update the configuration of a model item
     *  @param path Path name of the added/removed/changed item or null
     */
    public void readConfig(final String path) throws Exception
    {
        final AlarmTreeItem item = getConfigTree().getItemByPath(path);

        if (item == null  ||  !(item instanceof AlarmTreePV))
        {   // Not a known PV? Update the whole config.
            new ReadConfigJob(this).schedule();
            return;
        }
        // Update a known PV
        final AlarmTreePV pv = (AlarmTreePV) item;
        synchronized (this)
        {
            if (config == null)
                return;
            config.readPVConfig(pv);
        }
        // This could change the alarm tree after a PV was disabled or enabled.
        // Maximizing the severity would also fireNewAlarmState
        final AlarmTreeItem parent = pv.getClientParent();
        if (parent != null)
            parent.maximizeSeverity(pv);
        else
            fireNewAlarmState(pv);
    }

	/** Update the enablement of a PV in model.
     *  <p>
     *  Called by AlarmUpdateCommunicator, i.e. from JMS thread.
     *
     *  @param name PV name
     *  @param enabled Enabled?
     */
    @SuppressWarnings("nls")
    public void updateEnablement(final String name, final boolean enabled)
    {
        final AlarmTreePV pv;
        synchronized (this)
        {
            pv = findPV(name);
            if (pv == null)
            {
                CentralLogger.getInstance().getLogger(this).error(
                        "Received enablement (" + Boolean.toString(enabled) +
                        ") for unknown PV " + name);
                return;
            }
            pv.setEnabled(enabled);
        }
        // This could change the alarm tree after a PV was disabled or enabled.
        // Maximizing the severity would also fireNewAlarmState
        final AlarmTreeItem parent = pv.getClientParent();
        if (parent != null)
            parent.maximizeSeverity(pv);
        else
            fireNewAlarmState(pv);
	}

	/** Update the state of a PV in model.
     *  <p>
     *  Called by AlarmUpdateCommunicator, i.e. from JMS thread.
     *
     *  @param into Alarm update info
     */
    void updatePV(final AlarmUpdateInfo info)
    {
        // Update should contain PV name
        String name = info.getNameOrPath();
        if (AlarmTreePath.isPath(name))
            name = AlarmTreePath.getName(name);
        synchronized (this)
        {
            server_alive = true;
            final AlarmTreePV pv = findPV(name);
            if (pv != null)
            {
                pv.setAlarmState(info.getCurrentSeverity(), info.getCurrentMessage(),
                        info.getSeverity(), info.getMessage(),
                        info.getValue(), info.getTimestamp());
                return;
            }
        }
        // Can this result in out-of-memory?!
        // First glance: No, since we just log & return.
        // Is there a memory leak in the logger?
        // The update comes from JMS, and the logger may also
        // send info to JMS. Is that a problem?
        // Moved this outside the lock in case that makes a difference.
        CentralLogger.getInstance().getLogger(this).error(
                "Received update for unknown PV " + name); //$NON-NLS-1$
    }

    /** Locate PV by name
     *  @param name Name of PV to locate. May be <code>null</code>.
     *  @return PV or <code>null</code> when not found
     */
    private synchronized AlarmTreePV findPV(final String name)
    {
        if (config == null)
            return null;
        return config.findPV(name);
    }

    /** Ask alarm server to acknowledge alarm.
     *  @param pv PV to acknowledge
     *  @param acknowledge Acknowledge, or un-acknowledge?
     */
    public void acknowledge(final AlarmTreePV pv, final boolean acknowledge)
    {
    	synchronized (communicator_lock)
        {
            if (allow_write  &&  communicator != null)
                communicator.requestAcknowledgement(pv, acknowledge);
        }
    }

    /** Create a pseudo alarm tree for the purpose of displaying a message
     *  @param info Info that will show as dummy alarm tree item
     *  @return Pseudo alarm tree
     */
    private synchronized void createPseudoAlarmTree(final String info)
    {
        config_tree = new AlarmTreeRoot("Pseudo", -1); //$NON-NLS-1$
        new AlarmTreeItem(config_tree, info, 0);
        active_alarms.clear();
        acknowledged_alarms.clear();
    }

    /** Send debug trigger to alarm server */
    public void triggerDebug()
    {
        synchronized (communicator_lock)
        {
        	if (communicator != null)
        		communicator.triggerDebugAction();
        }
    }

    /** Inform listeners about server timeout */
    void fireServerTimeout()
    {
        server_alive = false;
        for (AlarmClientModelListener listener : listeners)
        {
            try
            {
                listener.serverTimeout(this);
            }
            catch (Throwable ex)
            {
                CentralLogger.getInstance().getLogger(this).error(ex);
            }
        }
    }

    /** Inform listeners that server is OK and in which mode */
    private void fireModeUpdate()
    {
        for (AlarmClientModelListener listener : listeners)
        {
            try
            {
                listener.serverModeUpdate(this, maintenance_mode);
            }
            catch (Throwable ex)
            {
                CentralLogger.getInstance().getLogger(this).error(ex);
            }
        }
    }

    /** Inform listeners about overall change to alarm tree configuration:
     *  Items added, removed.
     */
    private void fireNewConfig()
    {
        for (AlarmClientModelListener listener : listeners)
        {
            try
            {
                listener.newAlarmConfiguration(this);
            }
            catch (Throwable ex)
            {
                CentralLogger.getInstance().getLogger(this).error(ex);
            }
        }
    }

    /** Inform listeners about change in alarm state.
     *  <p>
     *  Typically, this is invoked with the PV that changed state.
     *  May be called with a <code>null</code> PV
     *  to indicate that messages were received after a server timeout.
     *  @param pv PV that might have changed the alarm state or <code>null</code>
     */
    void fireNewAlarmState(final AlarmTreePV pv)
    {
        if (pv != null)
        {
            synchronized (this)
            {
                active_alarms.remove(pv);
                acknowledged_alarms.remove(pv);
                final SeverityLevel severity = pv.getSeverity();
                if (severity.ordinal() > 0)
                {
                    if (severity.isActive())
                        active_alarms.add(pv);
                    else
                        acknowledged_alarms.add(pv);
                }
                if (!notify_listeners )
                    return;
            }
        }
        for (AlarmClientModelListener listener : listeners)
        {
            try
            {
                listener.newAlarmState(this, pv);
            }
            catch (Throwable ex)
            {
                CentralLogger.getInstance().getLogger(this).error(ex);
            }
        }
    }

    /** @return Debug string */
    @Override
    public String toString()
    {
        return "AlarmClientModel: " + config_tree; //$NON-NLS-1$
    }

    /** Dump debug information */
    @SuppressWarnings("nls")
    public synchronized void dump()
    {
        System.out.println("== AlarmClientModel ==");
        config_tree.dump(System.out);
        System.out.println("= Active alarms =");
        for (AlarmTreePV pv : active_alarms)
            System.out.println(pv.toString());
        System.out.println("= Acknowledged alarms =");
        for (AlarmTreePV pv : acknowledged_alarms)
            System.out.println(pv.toString());
    }
}
