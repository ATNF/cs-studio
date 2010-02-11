package org.csstudio.archivereader.rdb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.csstudio.archivereader.Severity;
import org.csstudio.archivereader.ValueIterator;
import org.csstudio.platform.data.IDoubleValue;
import org.csstudio.platform.data.IEnumeratedMetaData;
import org.csstudio.platform.data.IEnumeratedValue;
import org.csstudio.platform.data.ILongValue;
import org.csstudio.platform.data.IMetaData;
import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.ISeverity;
import org.csstudio.platform.data.IStringValue;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.ValueFactory;
import org.csstudio.platform.data.IValue.Quality;
import org.csstudio.platform.utility.rdb.TimeWarp;
import org.csstudio.platform.utility.rdb.RDBUtil.Dialect;

/** Base for ValueIterators that read from the RDB
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class AbstractRDBValueIterator  implements ValueIterator
{
    /** Oracle error code for cancelled statements */
    final protected static String ORACLE_CANCELLATION = "ORA-01013"; //$NON-NLS-1$

    /** Special Severity that's INVALID without a value */
    private static ISeverity no_value_severity = null;

    /** Numeric meta data used as default if nothing else is known */
    private static INumericMetaData default_numeric_meta = null;
    
    final protected RDBArchiveReader reader;
    final protected int channel_id;
    
    protected IMetaData meta = null;
    
    /** SELECT ... for the array samples. */
    private PreparedStatement sel_array_samples = null;
    
    /** For performance reasons, we remember the fact that we
     *  found (or didn't find) array samples.
     *  <p>
     *  Initially: Assume there are array samples.
     *  Once a scalar is identified, we stick with scalars.
     *  The obvious drawback: If only a few samples are array
     *  samples, we're likely to ignore them.
     */
    private boolean data_is_scalar = false;

    /** @param reader RDBArchiveReader
     *  @param channel_id ID of channel
     *  @throws Exception on error
     */
    AbstractRDBValueIterator(final RDBArchiveReader reader,
            final int channel_id) throws Exception
    {
        this.reader = reader;
        this.channel_id = channel_id;
        try
        {
            this.meta = determineMetaData();
        }
        catch (final Exception ex)
        {
            // Set iterator to empty
            close();
            final String message = ex.getMessage();
            if (message != null  &&  message.startsWith(ORACLE_CANCELLATION))
            {
                // Ignore
            }
            else
                throw ex;
        }
    }
    
    /** @return Meta data information for the channel or <code>null</code>
     *  @throws Exception on error
     */
    private IMetaData determineMetaData() throws Exception
    {
        // Try numeric meta data
        PreparedStatement statement =
            reader.getRDB().getConnection().prepareStatement(reader.getSQL().numeric_meta_sel_by_channel);
        try
        {
            statement.setInt(1, channel_id);
            final ResultSet result = statement.executeQuery();
            if (result.next())
                return ValueFactory.createNumericMetaData(
                        result.getDouble(1), result.getDouble(2), // display range
                        result.getDouble(3), result.getDouble(4), // warn range
                        result.getDouble(5), result.getDouble(6), // alarm range
                        result.getInt(7), result.getString(8));   // prev, units
        }
        finally
        {
            statement.close();
        }
        
        // Try enumerated meta data
        ArrayList<String> enums = null;
        statement = reader.getRDB().getConnection().prepareStatement(
                                reader.getSQL().enum_sel_num_val_by_channel);
        try
        {
            statement.setInt(1, channel_id);
            final ResultSet result = statement.executeQuery();
            if (result.next())
            {
                enums = new ArrayList<String>();
                do
                {
                    final int id = result.getInt(1);
                    final String val = result.getString(2);
                    // Expect vals for ids 0, 1, 2, ...
                    if (id != enums.size())
                        throw new Exception("Enum IDs for channel with ID "
                                + channel_id + " not in sequential order");
                    enums.add(val);
                }
                while (result.next());
            }
        }
        finally
        {
            statement.close();
        }
        // Anything found?
        if (enums == null  ||  enums.size() <= 0)
            return null; // Nothing found
        // Convert to plain array, then IEnumeratedMetaData
        return ValueFactory.createEnumeratedMetaData(enums.toArray(new String[enums.size()])); 
    }



    /** @return Some default numeric meta data */
    protected INumericMetaData getDefaultNumericMeta()
    {
        if (default_numeric_meta == null)
            default_numeric_meta =
                ValueFactory.createNumericMetaData(0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 2, ""); //$NON-NLS-1$
        return default_numeric_meta;
    }

    /** Extract value from SQL result
     *  @param result ResultSet that must contain contain time, severity, ..., value
     *  @return IValue Decoded IValue
     *  @throws Exception on error
     */
    protected IValue decodeSampleTableValue(final ResultSet result) throws Exception
    {
        // Get time stamp
        final Timestamp stamp = result.getTimestamp(1);
        // Oracle has nanoseconds in TIMESTAMP, MySQL in separate column 
        if (reader.getRDB().getDialect() == Dialect.MySQL)
            stamp.setNanos(result.getInt(7));
        final ITimestamp time = TimeWarp.getCSSTimestamp(stamp);
        
        // Get severity/status
        final String status = reader.getStatus(result.getInt(3));
        final ISeverity severity = filterSeverity(reader.getSeverity(result.getInt(2)), status);
        
        // Determine the value type
        // Try double
        final double dbl0 = result.getDouble(5);
        if (! result.wasNull())
        {
            // Is it an error to have enumeration strings for double samples?
            // In here, we handle it by returning enumeration samples,
            // because the meta data would be wrong for double values.
            if (meta instanceof IEnumeratedMetaData)
                return ValueFactory.createEnumeratedValue(time, severity, status,
                        (IEnumeratedMetaData) meta, IValue.Quality.Original,
                        new int [] { (int) dbl0 });
            // Double data. Get array elements - if any.
            final double data[] = readArrayElements(severity, stamp, dbl0);
            if (meta instanceof INumericMetaData)
                return ValueFactory.createDoubleValue(time, severity, status,
                        (INumericMetaData)meta, IValue.Quality.Original, data);
            // Make some meta data up
            return ValueFactory.createDoubleValue(time, severity, status,
                    getDefaultNumericMeta(), IValue.Quality.Original, data);
        }
        
        // Try integer
        final int num = result.getInt(4);
        if (! result.wasNull())
        {   // Enumerated integer?
            if (meta instanceof IEnumeratedMetaData)
                return ValueFactory.createEnumeratedValue(time, severity, status,
                        (IEnumeratedMetaData) meta, IValue.Quality.Original,
                        new int [] { num });
            // Fall back to plain (long) integer
            if (meta instanceof INumericMetaData)
                return ValueFactory.createLongValue(time, severity, status,
                        (INumericMetaData)meta, IValue.Quality.Original,
                        new long [] { num });
            // Numeric, but not meta data
            return ValueFactory.createLongValue(time, severity, status,
                    getDefaultNumericMeta(), IValue.Quality.Original,
                    new long [] { num });
        }
        
        // Default to string
        final String txt = result.getString(6);
        return ValueFactory.createStringValue(time, severity, status,
                IValue.Quality.Original, new String [] { txt });
    }

    /** @param severity Original severity
     *  @param status Status text
     *  @return If the severity/status indicate that there is no actual value,
     *          provide the special 'no value' severity
     */
    protected ISeverity filterSeverity(final ISeverity severity, final String status)
    {
        // Hard-coded knowledge:
        // When the severity is INVALID and the status indicates
        // that the archive is off or channel was disconnected,
        // we use the special INVALID severity that marks a sample
        // without a value.
        if (severity.isInvalid() &&
             (status.equalsIgnoreCase("Archive_Off") ||
              status.equalsIgnoreCase("Disconnected") ||
              status.equalsIgnoreCase("Write_Error")))
            return getNoValueSeverity();
        return severity;
    }

    /** @return Special Severity that's INVALID without a value */
    private static ISeverity getNoValueSeverity()
    {
        // Lazy init.
        if (no_value_severity  == null)
            no_value_severity = new ISeverity()
        {
            public boolean hasValue()  { return false; }
            public boolean isInvalid() { return true;  }
            public boolean isMajor()   { return false; }
            public boolean isMinor()   { return false; }
            public boolean isOK()      { return false; }
            @Override
            public String toString()   { return Severity.Level.INVALID.toString(); }
        };
        return no_value_severity;
    }

    /** Create new value with specific time stamp
     *  @param value Original Value
     *  @param time Desired time stamp
     *  @return New value with given time stamp
     */
    protected IValue changeTimestamp(final IValue value,
            final ITimestamp time)
    {
        final ISeverity severity = value.getSeverity();
        final String status = value.getStatus();
        final Quality quality = value.getQuality();
        final IMetaData meta = value.getMetaData();
        if (value instanceof IDoubleValue)
            return ValueFactory.createDoubleValue(time , severity, status,
                            (INumericMetaData)meta, quality,
                            ((IDoubleValue)value).getValues());
        else if (value instanceof ILongValue)
            return ValueFactory.createLongValue(time, severity, status,
                            (INumericMetaData)meta, quality,
                            ((ILongValue)value).getValues());
        else if (value instanceof IEnumeratedValue)
            return ValueFactory.createEnumeratedValue(time, severity, status,
                            (IEnumeratedMetaData)meta, quality,
                            ((IEnumeratedValue)value).getValues());
        else if (value instanceof IStringValue)
            return ValueFactory.createStringValue(time, severity, status,
                            quality, ((IStringValue)value).getValues());
        // Else: Log unknown data type as text
        return ValueFactory.createStringValue(time, severity, status,
                quality, new String[] { value.toString() });
    }

    
    /** Given the time and first element of the  sample, see if there
     *  are more array elements.
     *  @return Array with given element and maybe more.
     */
    private double[] readArrayElements(final ISeverity severity,
            final Timestamp stamp,
            final double dbl0) throws Exception
    {
        // For performance reasons, we only look for array data
        // until we hit a scalar sample.
        if (data_is_scalar)
            return new double [] { dbl0 };
        
        // See if there are more array elements
        if (sel_array_samples == null)   // Lazy initialization
            sel_array_samples = reader.getRDB().getConnection().prepareStatement(
                    reader.getSQL().sample_sel_array_vals);
        sel_array_samples.setInt(1, channel_id);
        sel_array_samples.setTimestamp(2, stamp);
        // MySQL keeps nanoseconds in designated column, not TIMESTAMP
        if (reader.getRDB().getDialect() == Dialect.MySQL)
            sel_array_samples.setInt(3, stamp.getNanos());
        
        // Assemble array of unknown size in ArrayList ....
        final ArrayList<Double> vals = new ArrayList<Double>();
        reader.addForCancellation(sel_array_samples);
        try
        {
            final ResultSet res = sel_array_samples.executeQuery();
            vals.add(new Double(dbl0));
            while (res.next())
                vals.add(res.getDouble(1));
            res.close();
        }
        finally
        {
            reader.removeFromCancellation(sel_array_samples);
        }
        
        // Convert to plain double array
        final int N = vals.size();
        final double ret[] = new double[N];
        for (int i = 0; i < N; i++)
            ret[i] = vals.get(i).doubleValue();
        // Check if it's in fact just a scalar, and a valid one
        if (N == 1  &&  !severity.isInvalid())
        {   // Found a perfect non-array sample:
            // Assume that the data is scalar, skip the array check from now on
            data_is_scalar = true;
            closeArraySampleSel();
        }
        return ret;
    }

    /** @param result ResultSet positioned on row to dump to console
     *  @throws Exception on error
     */
    protected void dumpResultSet(final ResultSet result) throws Exception
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
    
    /** Close the select statement for array samples. */
    private void closeArraySampleSel()
    {
        if (sel_array_samples != null)
        {
            try
            {
                sel_array_samples.close();
            }
            catch (Exception ex)
            {
                // Ignore
            }
            sel_array_samples = null;
        }
    }
    
    /** Release all database resources.
     *  OK to call more than once.
     */
    public void close()
    {
        closeArraySampleSel();
    }
}
