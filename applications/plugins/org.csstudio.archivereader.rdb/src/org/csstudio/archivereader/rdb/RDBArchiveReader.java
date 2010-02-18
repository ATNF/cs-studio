package org.csstudio.archivereader.rdb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import org.csstudio.archivereader.ArchiveInfo;
import org.csstudio.archivereader.ArchiveReader;
import org.csstudio.archivereader.Severity;
import org.csstudio.archivereader.ValueIterator;
import org.csstudio.platform.data.ISeverity;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.utility.rdb.RDBUtil;
import org.csstudio.platform.utility.rdb.RDBUtil.Dialect;

/** ArchiveServer for RDB data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBArchiveReader implements ArchiveReader
{
    /** Oracle error code for canceled statements */
    final private static String ORACLE_CANCELLATION = "ORA-01013"; //$NON-NLS-1$

    /** Oracle error code "error occurred at recursive SQL level ...: */
    final private static String ORACLE_RECURSIVE_ERROR = "ORA-00604"; //$NON-NLS-1$
    
    final private String url;
    final private String user;
    final private int password;
    final private String stored_procedure;
    final private RDBUtil rdb;
    final private SQL sql;

    /** Map of status IDs to Status strings */
    final private HashMap<Integer, String> stati;

    /** Map of severity IDs to Severities */
    final private HashMap<Integer, ISeverity> severities;
    
    /** List of statements to cancel in cancel() */
    private ArrayList<Statement> cancellable_statements =
        new ArrayList<Statement>();

    /** Initialize
     *  @param url Database URL
     *  @param user .. user
     *  @param password .. password
     *  @param stored_procedure Stored procedure or "" for client-side optimization
     *  @throws Exception on error
     */
    public RDBArchiveReader(final String url, final String user,
            final String password, final String stored_procedure)
        throws Exception
    {
        this.url = url;
        this.user = user;
        this.password = (password == null) ? 0 : password.length();
        rdb = RDBUtil.connect(url, user, password, false);
        // Ignore the stored procedure for MySQL
        if (rdb.getDialect() == Dialect.MySQL)
            this.stored_procedure = "";
        else
            this.stored_procedure = stored_procedure;
        String schema = Preferences.getSchema();
        if (schema.length() > 0)
            schema = schema + ".";
        sql = new SQL(rdb.getDialect(), schema);
        stati = getStatusValues();
        severities = getSeverityValues();
    }

    /** @return Map of all status ID/Text mappings
     *  @throws Exception on error
     */
    private HashMap<Integer, String> getStatusValues() throws Exception
    {
        final HashMap<Integer, String> stati = new HashMap<Integer, String>();
        final Statement statement = rdb.getConnection().createStatement();
        statement.setFetchSize(100);
        try
        {
            final ResultSet result = statement.executeQuery(sql.sel_stati);
            while (result.next())
                stati.put(result.getInt(1), result.getString(2));
            return stati;
        }
        finally
        {
            statement.close();
        }
    }
    
    /** @return Map of all severity ID/ISeverity mappings 
     *  @throws Exception on error
     */
    private HashMap<Integer, ISeverity> getSeverityValues() throws Exception
    {
        final HashMap<Integer, ISeverity> severities = new HashMap<Integer, ISeverity>();
        final Statement statement = rdb.getConnection().createStatement();
        statement.setFetchSize(100);
        try
        {
            final ResultSet result = statement.executeQuery(sql.sel_severities);
            while (result.next())
            {
                final ISeverity severity = new Severity(result.getString(2));
                severities.put(result.getInt(1), severity);
            }
            return severities;
        }
        finally
        {
            statement.close();
        }
    }

    /** @return RDBUtil with connection to RDB */
    RDBUtil getRDB()
    {
        return rdb;
    }

    /** @return SQL statements */
    SQL getSQL()
    {
        return sql;
    }

    /** @param status_id Numeric status ID
     *  @return Status string for ID
     */
    String getStatus(int status_id)
    {
        final String status = stati.get(status_id);
        if (status == null)
            return "<" + status_id + ">";
        return status;
    }

    /** @param severity_id Numeric severity ID
     *  @return ISeverity for ID
     */
    ISeverity getSeverity(int severity_id)
    {
        final ISeverity severity = severities.get(severity_id);
        if (severity == null)
            return new Severity("<" + severity_id + ">");
        return severity;
    }

    /** {@inheritDoc} */
    public String getServerName()
    {
        return "RDB";
    }

    /** {@inheritDoc} */
    public String getURL()
    {
        return url;
    }

    /** {@inheritDoc} */
    public String getDescription()
    {
        return "RDB Archive V" + getVersion() + " (" + rdb.getDialect() + ")\n" +
        	   "User: " + user + "\n" +
        	   "Password: " + password + " characters";
    }

    /** {@inheritDoc} */
    public int getVersion()
    {
        return 2;
    }

    /** {@inheritDoc} */
    public ArchiveInfo[] getArchiveInfos()
    {
        return new ArchiveInfo[]
        {
            new ArchiveInfo("rdb", rdb.getDialect().toString(), 1)
        };
    }
    
    /** {@inheritDoc} */
    public String[] getNamesByPattern(final int key, final String glob_pattern) throws Exception
    {
        // Escape underscores because they are SQL patterns
        String sql_pattern = glob_pattern.replace("_", "\\_");
        // Glob '?' -> SQL '_'
        sql_pattern = sql_pattern.replace('?', '_');
        // Glob '*' -> SQL '%'
        sql_pattern = sql_pattern.replace('*', '%');
        return perform_search(sql_pattern, sql.channel_sel_by_like);
    }

    /** {@inheritDoc} */
    public String[] getNamesByRegExp(final int key, final String reg_exp) throws Exception
    {
        return perform_search(reg_exp, sql.channel_sel_by_reg_exp);
    }
    
    /** Perform channel search by name pattern
     *  @param pattern Pattern
     *  @param sql_query SQL query that can handle the pattern
     *  @return Channel names
     *  @throws Exception on error
     */
    private String[] perform_search(final String pattern, final String sql_query) throws Exception
    {
        final ArrayList<String> names = new ArrayList<String>();
        final PreparedStatement statement = rdb.getConnection().prepareStatement(sql_query);
        addForCancellation(statement);
        try
        {
            statement.setString(1, pattern);
            final ResultSet result = statement.executeQuery();
            while (result.next())
                names.add(result.getString(1));
        }
        catch (Exception ex)
        {
            if (ex.getMessage().startsWith("ORA-01013"))
            {
                // Ignore ORA-01013: user requested cancel of current operation
            }
            else
                throw ex;
        }
        finally
        {
            removeFromCancellation(statement);
            statement.close();
        }
        return names.toArray(new String[names.size()]);
    }

    /** {@inheritDoc} */
    public ValueIterator getRawValues(final int key, final String name,
            final ITimestamp start, final ITimestamp end) throws Exception
    {
        final int channel_id = getChannelID(name);
        return new RawSampleIterator(this, channel_id, start, end);
    }

    /** {@inheritDoc} */
    public ValueIterator getOptimizedValues(final int key, final String name,
            final ITimestamp start, final ITimestamp end, int count) throws Exception
    {
        if (count <= 0)
            throw new Exception("Count must be positive");
        if (stored_procedure.length() > 0)
        {
            final int channel_id = getChannelID(name);
            return new StoredProcedureValueIterator(this, stored_procedure, channel_id, start, end, count);
        }
        // Else: Fetch raw data and perform averaging
        final ValueIterator raw_data = getRawValues(key, name, start, end);
        final double seconds = (end.toDouble() - start.toDouble()) / count;
        return new AveragedValueIterator(raw_data, seconds);
    }
    
    /** @param name Channel name
     *  @return Numeric channel ID
     *  @throws Exception on error
     */
    private int getChannelID(final String name) throws Exception
    {
        final PreparedStatement statement =
            rdb.getConnection().prepareStatement(sql.channel_sel_by_name);
        try
        {
            statement.setString(1, name);
            final ResultSet result = statement.executeQuery();
            if (!result.next())
                throw new Exception("Unknown channel name '" + name + "'");
            return result.getInt(1);
        }
        finally
        {
            statement.close();
        }
    }

    /** Add a statement to the list of statements-to-cancel in cancel()
     *  @param statement Statement to cancel
     *  @see #cancel()
     */
    void addForCancellation(final Statement statement)
    {
        synchronized (cancellable_statements)
        {
            cancellable_statements.add(statement);
        }
    }

    /** Remove a statement to the list of statements-to-cancel in cancel()
     *  @param statement Statement that should no longer be cancelled
     *  @see #cancel()
     */
    void removeFromCancellation(final Statement statement)
    {
        synchronized (cancellable_statements)
        {
            cancellable_statements.remove(statement);
        }
    }

    /** Check if an exception indicates Oracle operation was canceled,
     *  i.e. this program requested the operation to abort
     *  @param ex Exception (Throwable) to test
     *  @return <code>true</code> if it looks like the result of cancellation.
     */
    public static boolean isCancellation(final Throwable ex)
    {
        final String message = ex.getMessage();
        if (message == null)
            return false;
        if (message.startsWith(ORACLE_CANCELLATION))
            return true;
        if (message.startsWith(ORACLE_RECURSIVE_ERROR))
            return isCancellation(ex.getCause());
        return false;
    }

    /** Cancel an ongoing RDB query.
     *  Not supported by all queries.
     */
    public void cancel()
    {
        synchronized (cancellable_statements)
        {
            for (Statement statement : cancellable_statements)
            {
                try
                {
                    // Note that
                    //    statement.getConnection().close()
                    // does NOT stop an ongoing Oracle query!
                    // Only this seems to do it:
                    statement.cancel();
                }
                catch (Exception ex)
                {
                    CentralLogger.getInstance().getLogger(this).
                        info("Attempt to cancel statment", ex); //$NON-NLS-1$
                }
            }
        }
    }
    
    /** {@inheritDoc} */
    public void close()
    {
        cancel();
        rdb.close();
    }
}
