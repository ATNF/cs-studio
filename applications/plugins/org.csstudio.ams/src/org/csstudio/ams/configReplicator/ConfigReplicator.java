
/* 
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron, 
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

 package org.csstudio.ams.configReplicator;

import java.sql.Connection;
import java.sql.SQLException;

import org.csstudio.ams.AmsConstants;
import org.csstudio.ams.ExitException;
import org.csstudio.ams.Log;
import org.csstudio.ams.dbAccess.configdb.CommonConjunctionFilterConditionDAO;
import org.csstudio.ams.dbAccess.configdb.FilterActionDAO;
import org.csstudio.ams.dbAccess.configdb.FilterActionTypeDAO;
import org.csstudio.ams.dbAccess.configdb.FilterCondFilterCondDAO;
import org.csstudio.ams.dbAccess.configdb.FilterCondJunctionDAO;
import org.csstudio.ams.dbAccess.configdb.FilterCondNegationDAO;
import org.csstudio.ams.dbAccess.configdb.FilterConditionArrayStringDAO;
import org.csstudio.ams.dbAccess.configdb.FilterConditionArrayStringValuesDAO;
import org.csstudio.ams.dbAccess.configdb.FilterConditionDAO;
import org.csstudio.ams.dbAccess.configdb.FilterConditionProcessVariableDAO;
import org.csstudio.ams.dbAccess.configdb.FilterConditionStringDAO;
import org.csstudio.ams.dbAccess.configdb.FilterConditionTimeBasedDAO;
import org.csstudio.ams.dbAccess.configdb.FilterConditionTypeDAO;
import org.csstudio.ams.dbAccess.configdb.FilterDAO;
import org.csstudio.ams.dbAccess.configdb.FilterFilterActionDAO;
import org.csstudio.ams.dbAccess.configdb.FilterFilterConditionDAO;
import org.csstudio.ams.dbAccess.configdb.FlagDAO;
import org.csstudio.ams.dbAccess.configdb.TopicDAO;
import org.csstudio.ams.dbAccess.configdb.UserDAO;
import org.csstudio.ams.dbAccess.configdb.UserGroupDAO;
import org.csstudio.ams.dbAccess.configdb.UserGroupUserDAO;

public class ConfigReplicator implements AmsConstants
{
	/**
	 * Copying configuration from one database to another.
	 */
	public static void replicateConfiguration(Connection masterDB, Connection localDB)
												throws Exception
	{
		try
		{
			Log.log(Log.INFO, "Start Configuration Replicating.");
			
			masterDB.setAutoCommit(false);
			if (!FlagDAO.bUpdateFlag(masterDB, FLG_BUP, FLAGVALUE_RPLCFG_IDLE, FLAGVALUE_RPLCFG_DIST_SYNC))
				throw new ExitException("replicateConfiguration start: could not update " + FLG_BUP 
						+ " from + " + FLAGVALUE_RPLCFG_IDLE + " to " + FLAGVALUE_RPLCFG_DIST_SYNC
						, EXITERR_BUP_UPDATEFLAG_START);

			Log.log(Log.INFO, "Start deleting local Configuration.");			
			// ADDED: Markus Moeller 06.08.2008
			FilterCondJunctionDAO.removeAll(localDB);			
			FilterCondNegationDAO.removeAll(localDB);
			FilterCondFilterCondDAO.removeAll(localDB);
			
			FilterConditionTypeDAO.removeAll(localDB);
			FilterConditionDAO.removeAll(localDB);
			FilterConditionStringDAO.removeAll(localDB);
			FilterConditionArrayStringDAO.removeAll(localDB);
			FilterConditionArrayStringValuesDAO.removeAll(localDB);
			FilterConditionProcessVariableDAO.removeAll(localDB);
			CommonConjunctionFilterConditionDAO.removeAll(localDB);
			
			FilterConditionTimeBasedDAO.removeAll(localDB);
			FilterDAO.removeAll(localDB);
			FilterFilterConditionDAO.removeAll(localDB);
			TopicDAO.removeAll(localDB);
			FilterActionTypeDAO.removeAll(localDB);
			
			FilterActionDAO.removeAll(localDB);
			FilterFilterActionDAO.removeAll(localDB);
			UserDAO.removeAll(localDB);
			UserGroupDAO.removeAll(localDB);
			UserGroupUserDAO.removeAll(localDB);
	
			Log.log(Log.INFO, "Start copying master Configuration.");
			FilterConditionTypeDAO.copyFilterConditionType(masterDB, localDB);
			FilterConditionDAO.copyFilterCondition(masterDB, localDB);
			FilterConditionStringDAO.copyFilterConditionString(masterDB, localDB);
			FilterConditionArrayStringDAO.copyFilterConditionArrayString(masterDB, localDB);
			FilterConditionArrayStringValuesDAO.copyFilterConditionArrayStringValues(masterDB, localDB);
			FilterConditionProcessVariableDAO.copy(masterDB, localDB);
			CommonConjunctionFilterConditionDAO.copy(masterDB, localDB);            
            // ADDED: Markus Moeller 2008-08-06
            FilterCondJunctionDAO.copyFilterCondJunction(masterDB, localDB);
            FilterCondNegationDAO.copyFilterCondNegation(masterDB, localDB);
            FilterCondFilterCondDAO.copyFilterCondFilterCond(masterDB, localDB);
            
			FilterConditionTimeBasedDAO.copyFilterConditionTimeBased(masterDB, localDB);
			FilterDAO.copyFilter(masterDB, localDB);
			FilterFilterConditionDAO.copyFilterFilterCondition(masterDB, localDB);
			TopicDAO.copyTopic(masterDB, localDB);
			FilterActionTypeDAO.copyFilterActionType(masterDB, localDB);
			
			FilterActionDAO.copyFilterAction(masterDB, localDB);
			FilterFilterActionDAO.copyFilterFilterAction(masterDB, localDB);
			UserDAO.copyUser(masterDB, localDB);
			UserGroupDAO.copyUserGroup(masterDB, localDB);
			UserGroupUserDAO.copyUserGroupUser(masterDB, localDB);
			
			Log.log(Log.INFO, "Replicating Configuration finished.");
			
			if (!FlagDAO.bUpdateFlag(masterDB, FLG_BUP, FLAGVALUE_RPLCFG_DIST_SYNC, FLAGVALUE_RPLCFG_IDLE))
				throw new ExitException("replicateConfiguration end: could not update " + FLG_BUP 
						+ " from + " + FLAGVALUE_RPLCFG_DIST_SYNC + " to " + FLAGVALUE_RPLCFG_IDLE
						, EXITERR_BUP_UPDATEFLAG_END);

			masterDB.commit();
		}
		catch (Exception ex)
		{
			try
			{
				masterDB.rollback();
			}
			catch(Exception e)
			{
				Log.log(Log.WARN, "rollback failed.", e);
			}

			Log.log(Log.FATAL, "replicateConfiguration failed.", ex);
			
			throw ex;
		}
		finally 
		{
			try
			{
				masterDB.setAutoCommit(true);
			}
			catch(Exception e){}
		}
		// All O.K.
	}
	
	/**
	 * Makes a backup of given database (copying data to 'syn'-tables).
	 */
	public static boolean backupMasterDbBeforeSync(Connection masterDB) throws SQLException
	{
		boolean bReturnValue = false;
		try
		{
			masterDB.setAutoCommit(false);
			if (FlagDAO.bUpdateFlag(masterDB, FLG_BUP, FLAGVALUE_RPLCFG_IDLE, FLAGVALUE_RPLCFG_CONF_SYNC))
			{
				Log.log(Log.INFO, "Start MasterDB-Backup before Replicating Configuration.");
		
				Log.log(Log.INFO, "Start deleting MasterDB-Backup Configuration.");
				FilterConditionTypeDAO.removeAllBackupFromMasterDB(masterDB);
				FilterConditionDAO.removeAllBackupFromMasterDB(masterDB);
				FilterConditionStringDAO.removeAllBackupFromMasterDB(masterDB);
				FilterConditionArrayStringDAO.removeAllBackupFromMasterDB(masterDB);
				FilterConditionArrayStringValuesDAO.removeAllBackupFromMasterDB(masterDB);
				FilterConditionProcessVariableDAO.removeAllFromBackup(masterDB);
				CommonConjunctionFilterConditionDAO.removeAllFromBackup(masterDB);

				FilterConditionTimeBasedDAO.removeAllBackupFromMasterDB(masterDB);
				FilterDAO.removeAllBackupFromMasterDB(masterDB);
				FilterFilterConditionDAO.removeAllBackupFromMasterDB(masterDB);
				TopicDAO.removeAllBackupFromMasterDB(masterDB);
				FilterActionTypeDAO.removeAllBackupFromMasterDB(masterDB);
				
				FilterActionDAO.removeAllBackupFromMasterDB(masterDB);
				FilterFilterActionDAO.removeAllBackupFromMasterDB(masterDB);
				UserDAO.removeAllBackupFromMasterDB(masterDB);
				UserGroupDAO.removeAllBackupFromMasterDB(masterDB);
				UserGroupUserDAO.removeAllBackupFromMasterDB(masterDB);
		
				Log.log(Log.INFO, "Start backuping MasterDB Configuration.");
				FilterConditionTypeDAO.backupFilterConditionType(masterDB);
				FilterConditionDAO.backupFilterCondition(masterDB);
				FilterConditionStringDAO.backupFilterConditionString(masterDB);
				FilterConditionArrayStringDAO.backupFilterConditionArrayString(masterDB);
				FilterConditionArrayStringValuesDAO.backupFilterConditionArrayStringValues(masterDB);
				FilterConditionProcessVariableDAO.backup(masterDB);
				CommonConjunctionFilterConditionDAO.backup(masterDB);

				FilterConditionTimeBasedDAO.backupFilterConditionTimeBased(masterDB);
				FilterDAO.backupFilter(masterDB);
				FilterFilterConditionDAO.backupFilterFilterCondition(masterDB);	
				TopicDAO.backupTopic(masterDB);
				FilterActionTypeDAO.backupFilterActionType(masterDB);
				
				FilterActionDAO.backupFilterAction(masterDB);
				FilterFilterActionDAO.backupFilterFilterAction(masterDB);
				UserDAO.backupUser(masterDB);
				UserGroupDAO.backupUserGroup(masterDB);
				UserGroupUserDAO.backupUserGroupUser(masterDB);
				
				Log.log(Log.INFO, "MasterDB-Backup finished.");
				
				if (!FlagDAO.bUpdateFlag(masterDB, FLG_BUP, FLAGVALUE_RPLCFG_CONF_SYNC, FLAGVALUE_RPLCFG_IDLE))
					Log.log(Log.FATAL, "freeing bUpdateReplFlag failed");
				else
					bReturnValue = true;
				
				if (bReturnValue)
					masterDB.commit();
				else
					masterDB.rollback();
			}
		}
		catch (SQLException ex)
		{
			try{
				masterDB.rollback();
			}catch(Exception e)
			{
				Log.log(Log.WARN, "MasterDB-Backup rollback failed.", e);
			}

			Log.log(Log.FATAL, "MasterDB-Backup synch failed.", ex);
			throw ex;
		}
		finally 
		{
			try{
				masterDB.setAutoCommit(true);
			}catch(Exception e){}
		}
		return bReturnValue;
	}
}