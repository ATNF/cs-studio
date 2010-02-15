package org.csstudio.archivereader.rdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import oracle.jdbc.OracleTypes;

import org.csstudio.archivereader.ArchiveInfo;
import org.csstudio.archivereader.ArchiveReader;
import org.csstudio.archivereader.ValueIterator;
import org.csstudio.platform.data.IMetaData;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.TimestampFactory;
import org.csstudio.platform.utility.rdb.RDBUtil;
import org.csstudio.platform.utility.rdb.TimeWarp;
import org.junit.Ignore;
import org.junit.Test;

/** JUnit test of the RDBArchiveServer
 *  <p>
 *  Will only work when suitable archived data is available.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBArchiveReaderTest
{
    final private static String URL = "jdbc:oracle:thin:@//172.31.73.122:1521/prod";
    final private static String USER = "sns_reports";
    final private static String PASSWORD = "sns";
    final private static String SCALAR_NAME =
//        "CCL_LLRF:IOC1:Load";
        "RFQ_Vac:IG_2:P";
    final private static String WAVEFORM_NAME = "PPS_SM:SF:Emi_PM";
    final private static String STORED_PROCEDURE = "chan_arch_sns.archive_reader_pkg";
    
    final private static double TIMERANGE_SECONDS = 60*60*24*14;
    final private static int BUCKETS = 50;
    
    final private static boolean dump = false;
    
    /** Basic connection */
    @Ignore
    @Test
    public void testBasicInfo() throws Exception
    {
        final ArchiveReader archive = new RDBArchiveReader(
                URL, USER, PASSWORD, STORED_PROCEDURE);
        try
        {
            assertEquals("RDB", archive.getServerName());
            System.out.println(archive.getDescription());
            for (ArchiveInfo arch : archive.getArchiveInfos())
                System.out.println(arch);
        }
        finally
        {
            archive.close();
        }
    }

    /** Locate channels by pattern */
    @Ignore
    @Test
    public void testChannelByPattern() throws Exception
    {
        final ArchiveReader archive = new RDBArchiveReader(
                URL, USER, PASSWORD, STORED_PROCEDURE);
        try
        {
            System.out.println("Channels matching a pattern:");
            final String names[] = archive.getNamesByPattern(1, "CCL_LLRF:IOC?:Load");
            for (String name : names)
                System.out.println(name);
            assertEquals(4, names.length);
        }
        finally
        {
            archive.close();
        }
    }

    /** Locate channels by pattern */
    @Ignore
    @Test
    public void testChannelByRegExp() throws Exception
    {
        final ArchiveReader archive = new RDBArchiveReader(
                URL, USER, PASSWORD, STORED_PROCEDURE);
        try
        {
            System.out.println("Channels matching a regular expression, aborted");
            scheduleCancellation(archive, 1.0);
            String names[] = archive.getNamesByRegExp(1, "CCL_LLRF:IOC.:Load");
            assertEquals(0, names.length);
            
            System.out.println("Channels matching a regular expression:");
            names = archive.getNamesByRegExp(1, "CCL_LLRF:IOC.:Load");
            
            for (String name : names)
                System.out.println(name);
            assertEquals(4, names.length);
        }
        finally
        {
            archive.close();
        }
    }
    
    /** Get raw data for scalar */
    @Test
    @Ignore
    public void testRawScalarData() throws Exception
    {
        final ArchiveReader archive = new RDBArchiveReader(
                URL, USER, PASSWORD, STORED_PROCEDURE);
        try
        {
            System.out.println("Raw samples for " + SCALAR_NAME + ":");
            final ITimestamp end = TimestampFactory.now();
            final ITimestamp start = TimestampFactory.fromDouble(end.toDouble() - TIMERANGE_SECONDS);
            final ValueIterator values = archive.getRawValues(0, SCALAR_NAME, start, end);
            
            if (dump)
            {
                IMetaData meta = null;
                while (values.hasNext())
                {
                    IValue value = values.next();
                    System.out.println(value);
                    if (meta == null)
                        meta = value.getMetaData();
                }
                values.close();
                System.out.println("Meta data: " + meta);
            }
            else
            {
                int count = 0;
                while (values.hasNext())
                {
                    assertNotNull(values.next());
                    ++count;
                }
                System.out.println(count + " samples");
            }
        }
        finally
        {
            archive.close();
        }
    }

    /** Get raw data for waveform */
    @Ignore
    @Test
    public void testRawWaveformData() throws Exception
    {
        final ArchiveReader archive = new RDBArchiveReader(
                URL, USER, PASSWORD, STORED_PROCEDURE);
        try
        {
            System.out.println("Raw samples for " + WAVEFORM_NAME + ":");
            final ITimestamp end = TimestampFactory.now();
            final ITimestamp start = TimestampFactory.fromDouble(end.toDouble() - 60*60*0.5); // 0.5 hours
            
            // Waveform readout seems to take about 30 seconds, cancel in the middle
            scheduleCancellation(archive, 10.0);
            final ValueIterator values = archive.getRawValues(0, WAVEFORM_NAME, start, end);
            IMetaData meta = null;
            while (values.hasNext())
            {
                IValue value = values.next();
                System.out.println(value);
                if (meta == null)
                    meta = value.getMetaData();
            }
            values.close();
            System.out.println("Meta data: " + meta);
        }
        finally
        {
            archive.close();
        }
    }
    
    /** Get optimized data for scalar */
    @Test
    @Ignore
    public void testJavaOptimizedScalarData() throws Exception
    {
        System.out.println("Optimized samples for " + SCALAR_NAME + ":");
        System.out.println("-- Java implementation --");

        final ArchiveReader archive = new RDBArchiveReader(
                URL, USER, PASSWORD, "");
        try
        {
            final ITimestamp end = TimestampFactory.now();
            final ITimestamp start = TimestampFactory.fromDouble(end.toDouble() - TIMERANGE_SECONDS);
            final ValueIterator values = archive.getOptimizedValues(0, SCALAR_NAME, start, end, BUCKETS);
            IMetaData meta = null;
            while (values.hasNext())
            {
                IValue value = values.next();
                System.out.println(value);
                if (meta == null)
                    meta = value.getMetaData();
            }
            values.close();
            System.out.println("Meta data: " + meta);
        }
        finally
        {
            archive.close();
        }
    }

    /** Get optimized data for scalar */
    @Test
    @Ignore
    public void testStoredProcedureOptimizedScalarData() throws Exception
    {
        System.out.println("Optimized samples for " + SCALAR_NAME + ":");
        System.out.println("-- Based on stored procedure --");
        final ArchiveReader archive = new RDBArchiveReader(URL, USER, PASSWORD, STORED_PROCEDURE);
        try
        {
            System.out.println("Optimized samples for " + SCALAR_NAME + ":");
            final ITimestamp end = TimestampFactory.now();
            final ITimestamp start = TimestampFactory.fromDouble(end.toDouble() - TIMERANGE_SECONDS);
            final ValueIterator values = archive.getOptimizedValues(0, SCALAR_NAME, start, end, BUCKETS);
            IMetaData meta = null;
            while (values.hasNext())
            {
                IValue value = values.next();
                System.out.println(value);
                if (meta == null)
                    meta = value.getMetaData();
            }
            values.close();
            System.out.println("Meta data: " + meta);
        }
        finally
        {
            archive.close();
        }
    }

    /** Schedule a call to 'cancel()'
     *  @param archive ArchiveReader to cance
     *  @param seconds Seconds until cancellation
     */
    private void scheduleCancellation(final ArchiveReader archive, final double seconds)
    {
        new Timer("CancellationTest").schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                System.out.println("Cancelling ongoing requests!");
                archive.cancel();
            }
        }, 2000);
    }
    
    /** Directly call the stored procedure */
    @Test
    @Ignore
    public void testStoredProcedureDirectly() throws Exception
    {
        final ITimestamp end = TimestampFactory.now();
        final ITimestamp start = TimestampFactory.fromDouble(end.toDouble() - TIMERANGE_SECONDS);
        
        RDBUtil rdb = RDBUtil.connect(URL, USER, PASSWORD, false);
        
        // Get channel id
        final PreparedStatement stmt = rdb.getConnection().prepareStatement("SELECT channel_id FROM chan_arch.channel WHERE name=?");
        stmt.setString(1, SCALAR_NAME);
        ResultSet result = stmt.executeQuery();
        assertTrue(result.next());
        final int channel_id = result.getInt(1);
        System.out.println(SCALAR_NAME + " ID = " + channel_id);
        
        // Call stored procedure
        // Jeff's
//        CallableStatement statement = rdb.getConnection().prepareCall(
//                 "begin ? := chan_arch_sns.sample_aggregation_pkg.get_brower_data(?, '01/28/2010 00:00:00:000000', '02/04/2010 00:00:00:000000', ?); end;");
//        statement.registerOutParameter(1, OracleTypes.CURSOR);
//        statement.setInt(2, channel_id);
//        statement.setInt(3, BUCKETS);

        // Mine
        CallableStatement statement = rdb.getConnection().prepareCall(
            "{ ? = call chan_arch_sns.archive_reader_pkg.get_browser_data(?, ?, ?, ?) }");
        statement.registerOutParameter(1, OracleTypes.CURSOR);
        statement.setInt(2, channel_id);
        statement.setTimestamp(3, TimeWarp.getSQLTimestamp(start));
        statement.setTimestamp(4, TimeWarp.getSQLTimestamp(end));
        statement.setInt(5, BUCKETS);
        
        statement.setFetchSize(10000);
        final long bench_start = System.currentTimeMillis();
        statement.execute();
        result = (ResultSet) statement.getObject(1);
        final long bench_lap1 = System.currentTimeMillis();
        if (dump)
        {
            while (result.next())
            {
                final ResultSetMetaData meta = result.getMetaData();
                final int N = meta.getColumnCount();
                for (int i=1; i<=N; ++i)
                {
                    if (i > 1)
                        System.out.print(", ");
                    if (meta.getColumnName(i).equals("SMPL_TIME"))
                        System.out.print(meta.getColumnName(i) + ": " + TimeWarp.getCSSTimestamp(result.getTimestamp(i)));
                    else
                        System.out.print(meta.getColumnName(i) + ": " + result.getString(i));
                }
                System.out.println();
            }
        }
        else
        {
            int count = 0;
            while (result.next())
            {
                @SuppressWarnings("unused")
                double value = result.getDouble(4);
                ++count;
            }
            System.out.println(count + " samples");
        }
        final long bench_lap2 = System.currentTimeMillis();
        final double secs_query = (bench_lap1 - bench_start) / 1000.0;
        final double secs_total = (bench_lap2 - bench_start) / 1000.0;
        System.out.println("Query: " + secs_query);
        System.out.println("Total: " + secs_total);
    }

    /** Directly call the stored procedure */
    @Test
    public void testSQL() throws Exception
    {
        final SimpleDateFormat parser = new SimpleDateFormat("yyyy/MM/dd");
        
        final RDBUtil rdb = RDBUtil.connect(URL, USER, PASSWORD, false);
        final PreparedStatement statement = rdb.getConnection().prepareStatement(
            "SELECT smpl_time, severity_id, status_id, num_val, float_val, str_val" +
            " FROM chan_arch.sample" +
            " WHERE channel_id=?" +
            "   AND smpl_time BETWEEN ? AND ?"
            // + "   ORDER BY smpl_time"
            );
        statement.setInt(1, 58418);
        statement.setTimestamp(2, new Timestamp(parser.parse("2009/07/01").getTime()));
        statement.setTimestamp(3, new Timestamp(parser.parse("2009/11/01").getTime()));
        statement.setFetchSize(100000);
        final long bench_start = System.currentTimeMillis();
        final ResultSet result = statement.executeQuery();
        // - Network sniffer:
        // Client -> Oracle: SELECT ....
        // Oracle -> Client: Result contains columns time SMPL_TIME, int SEVERITY_ID, ... 
        // Client -> Oracle: OK, send me the data
        // - then a long pause, like 180 seconds.
        // - No network traffic, client waits in executeQuery(), until suddenly
        // Oracle -> Client: data
        // Oracle -> Client: data
        // Oracle -> Client: data
        // Client -> Oracle: Acknowledge
        // Oracle -> Client: data
        // Oracle -> Client: data
        // Oracle -> Client: data
        // Client -> Oracle: Acknowledge
        // - about 4000 network packages with data in maybe 4 seconds
        // - executeQuery() returns
        final long bench_lap1 = System.currentTimeMillis();
        // While fetching the rows of data, most calls to next()
        // do not cause any network traffic.
        // With setFetchSize(10), the default, there is another request for data
        // about every 10 rows. With setFetchSize(100000) there is no network
        // traffic after about 100000 rows, so maybe that fetch size is too big
        // and the JDBC library uses its own idea of how many rows to transfer
        // in a network packet.
        // These bursts data read over the network triggered by next()
        // take almost no time compared to the initial executeQuery(),
        // but a bigger setFetchSize() reduces the number of network requests
        // and is of course faster.
        int count = 0;
        Timestamp last = null;
        while (result.next())
        {
            @SuppressWarnings("unused")
            final Timestamp time = result.getTimestamp(1);
            if (last != null &&  time.before(last))
                System.out.println("Time error!");
            last = time;
            ++count;
        }
        final long bench_lap2 = System.currentTimeMillis();
        final double secs_query = (bench_lap1 - bench_start) / 1000.0;
        final double secs_total = (bench_lap2 - bench_start) / 1000.0;
        System.out.println(count + " samples");
        System.out.println("Query: " + secs_query);
        System.out.println("Total: " + secs_total);
    }
}
