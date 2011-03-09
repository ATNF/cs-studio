/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.globalclientmodel;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.alarm.beast.AlarmTreePath;
import org.csstudio.alarm.beast.Preferences;
import org.csstudio.alarm.beast.SQL;
import org.csstudio.alarm.beast.SeverityLevel;
import org.csstudio.alarm.beast.client.AlarmTreeRoot;
import org.csstudio.data.values.ITimestamp;
import org.csstudio.data.values.TimestampFactory;
import org.csstudio.platform.utility.rdb.RDBUtil;

/** Helper for reading currently active global alarms from RDB
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class GlobalAlarmReader
{
    final private RDBUtil rdb;
    final private SQL sql;

    /** Initialize */
    public GlobalAlarmReader() throws Exception
    {
        final String url = Preferences.getRDB_Url();
        final String user = Preferences.getRDB_User();
        final String password = Preferences.getRDB_Password();

        rdb = RDBUtil.connect(url, user, password, false);
        sql = new SQL(rdb);
    }

    /** Must be called to release resources */
    public void close()
    {
        rdb.close();
    }

    /** Read currently active global alarms from RDB
     *  @return Roots of all alarm trees with active global alarms
     *  @throws Exception on RDB access error
     */
    public List<AlarmTreeRoot> readGlobalAlarms() throws Exception
    {
        final List<AlarmTreeRoot> alarms = new ArrayList<AlarmTreeRoot>();

        final PreparedStatement statement = rdb.getConnection().prepareStatement(sql.sel_global_alarm_pvs);
        final PreparedStatement path_statement = rdb.getConnection().prepareStatement(sql.sel_item_by_id);
        try
        {
            statement.setBoolean(1, true);
            final ResultSet result = statement.executeQuery();
            while (result.next())
            {
                // Get info for PV in global alarm
                final int parent_id = result.getInt(1);
                final String name = result.getString(2);
                final SeverityLevel severity = SeverityLevel.parse(result.getString(3));
                final String status = result.getString(4);
                // Not used: final String value = result.getString(5);
                final Timestamp sql_time = result.getTimestamp(6);
                ITimestamp alarm_time;
                if (result.wasNull())
                    alarm_time = null;
                else
                    alarm_time = TimestampFactory.fromSQLTimestamp(sql_time);

                // Get path to PV
                final String path = getPath(path_statement, parent_id);
                final String full_path = AlarmTreePath.makePath(path, name);

                // Create GlobalAlarm
                final GlobalAlarm alarm = GlobalAlarm.fromPath(alarms, full_path, severity, status, alarm_time);

                // Get guidance etc.
                alarm.completeGuiInfo(rdb, sql);
            }
        }
        finally
        {
            path_statement.close();
            statement.close();
        }

        return alarms;
    }

    /** Read path to a PV
     *  @param path_statement Prepared statement that's used to read info
     *  @param id ID of parent, descending down to root from there
     *  @return Path from alarm tree root to the item with <code>id</code>
     *  @throws Exception on RDB error
     */
    private String getPath(final PreparedStatement path_statement, int id) throws Exception
    {
        final List<String> path_items = new ArrayList<String>();
        boolean have_parent;
        do
        {
            path_statement.setInt(1, id);
            final ResultSet result = path_statement.executeQuery();
            if (! result.next())
                throw new Exception("Cannot locate item with ID " + id);
            // Looping tree towards root, so add new element to _start_ of path
            path_items.add(0, result.getString(2));
            id = result.getInt(1);
            have_parent = ! result.wasNull();
            result.close();
        }
        while (have_parent);
        // Concatenate elements to path
        final int length = path_items.size();
        return AlarmTreePath.makePath(path_items.toArray(new String[length]), length);
    }
}
