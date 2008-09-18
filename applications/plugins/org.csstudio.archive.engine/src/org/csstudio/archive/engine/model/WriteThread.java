package org.csstudio.archive.engine.model;

import java.util.ArrayList;

import org.csstudio.apputil.time.BenchmarkTimer;
import org.csstudio.archive.engine.Activator;
import org.csstudio.archive.rdb.ChannelConfig;
import org.csstudio.archive.rdb.RDBArchive;
import org.csstudio.platform.data.IValue;
import org.csstudio.util.stats.Average;

/** Thread that writes values from multiple <code>SampleBuffer</code>s
 *  to an <code>RDBArchiveServer</code>.
 *  <p>
 *  When there are write errors, it sets the sample buffer error state
 *  and tries to reconnect to the database and write again until successful.
 *  Since the Oracle batch mechanism doesn't tell us what exactly failed
 *  in a batch, all the samples that were part of the batch might
 *  be lost.
 *  The channels that add samples to the sample buffer supposedly notice
 *  the error condition and add a special indicator once we recover.
 *  
 *  @author Kay Kasemir
 */
public class WriteThread implements Runnable
{
    /** Minimum write period [seconds] */
    private static final double MIN_WRITE_PERIOD = 5.0;

    /** Server to which this thread writes. */
    final private RDBArchive archive;
    
    /** All the sample buffers this thread writes. */
    final private ArrayList<SampleBuffer> buffers =
        new ArrayList<SampleBuffer>();
    
    /** Flag that tells the write thread to run or quit. */
    private boolean do_run;
    
    /** Synchronization block for waiting.
     *  Signaled in stop().
     */
    private Object wait_block = new Object();
    
    /** Delay between write runs. */
    private long millisec_delay = 5000;
    
    /** Number of values to place into one batch */
    private int batch_size = 500;
    
    /** Average number of values per write run */
    private Average write_count = new Average();
    
    /** Average duration of write run */
    private Average write_time = new Average();

    /** Thread the executes this.run() */
    private Thread thread;
    
    /** Construct thread for writing to server
     *  @param archive RDB to write to
     */
    public WriteThread(final RDBArchive archive)
    {
        this.archive = archive;
    }

    /** Add a channel's buffer that this thread reads */
    public void addChannel(final ArchiveChannel channel)
    {
        addSampleBuffer(channel.getSampleBuffer());
    }

    /** Add a sample buffer that this thread reads */
    void addSampleBuffer(final SampleBuffer buffer)
    {
        buffers.add(buffer);
    }
    
    /** Start the write thread.
     *  @param write_period Period between writes in seconds
     *  @param batch_size Number of values to batch
     */
    @SuppressWarnings("nls")
    public void start(double write_period, int batch_size)
    {
        if (write_period < MIN_WRITE_PERIOD)
        {
            Activator.getLogger().warn("Adjusting write period from "
                    + write_period + " to " + MIN_WRITE_PERIOD);
            write_period = MIN_WRITE_PERIOD;
        }
        millisec_delay = (int)(1000.0 * write_period);
        this.batch_size = batch_size;
        thread = new Thread(this, "WriteThread");
        thread.start();
    }

    /** Reset statistics */
    public void reset()
    {
        write_count.reset();
        write_time.reset();
    }

    /** Ask the write thread to stop ASAP. */
    private void stop()
    {
        do_run = false;
        synchronized (wait_block)
        {
            wait_block.notify();
        }
    }
    
    /** @return Average number of values per write run */
    public double getWriteCount()
    {
        return write_count.get();
    }
    
    /** @return  Average duration of write run in seconds */
    public double getWriteDuration()
    {
        return write_time.get();
    }
    
    /** 'Main loop' of the write thread.
     *  <p>
     *  Writes all values out, then waits.
     *  The idea is that waiting a little for values
     *  to accumulate actually helps, because then
     *  we may write a few values per channel,
     *  so the effort for locating a channel's ID
     *  and the batching actually pays off.
     *  <p>
     *  Since the wait time can be considerable (30 seconds?),
     *  we wait on a semaphore (wait_block), which
     *  can be notified in stop() to cause an ASAP exit.
     */
    @SuppressWarnings("nls")
    public void run()
    {
        Activator.getLogger().info("WriteThread starts");
        final BenchmarkTimer timer = new BenchmarkTimer();
        boolean write_error = false;
        do_run = true;
        while (do_run)
        {
            long delay;
            try
            {
                // If there was an error before...
                if (write_error)
                {   // .. try to reconnect
                    archive.reconnect();
                    // If we get here, all is OK so far ...
                    write_error = false;
                    // .. and we continue to write.
                }
                timer.start();
                // In case of a network problem, we can hang in here
                // for a long time...
                final long written = write();
                timer.stop();
                write_count.update(written);
                write_time.update(timer.getSeconds());
                // How much of the scheduled delay is left after write()?
                delay = millisec_delay - timer.getMilliseconds();
            }
            catch (Exception ex)
            {   // Error in write() or the preceding reconnect()...
                Activator.getLogger().error("Error, will try to reconnect", ex);
                // Use max. delay
                delay = millisec_delay;
                write_error = true;
            }
            SampleBuffer.setErrorState(write_error);
            // See if there's any time left to wait,
            // or if we already used all that time in the last 'write'
            if (delay > 0)
            {
                synchronized (wait_block)
                {
                    try
                    {
                        wait_block.wait(delay);
                    }
                    catch (InterruptedException ex)
                    {
                        Activator.getLogger().error("Interrupted wait", ex);
                    }
                }
            }
        }
        Activator.getLogger().info("WriteThread exists");
    }

    /** Stop the write thread, performing a final write. */
    public void shutdown() throws Exception
    {
        // Stop the thread
        stop();
        // Wait for it to end
        thread.join();
        // Then write once more.
        // Errors in this last write are passed up.
        write();
    }
    
    /** Write right now until all sample buffers are empty
     *  @return number of samples written
     */
    private long write() throws Exception
    {
        int total_count = 0;
        int count = 0;
        for (SampleBuffer buffer : buffers)
        {
            // Update max buffer length etc. before we start to remove samples
            buffer.updateStats();
            // Write samples for one channel
            final String name = buffer.getChannelName();
            final ChannelConfig channel = archive.createChannel(name);
            IValue sample = buffer.remove();
            while (sample != null)
            {   // Write one value
                channel.batchSample(sample);
                // Note: count across different sample buffers!
                ++count;
                if (count > batch_size)
                {
                    total_count += count;
                    count = 0;
                    archive.commitBatch();
                }
                // next
                sample = buffer.remove();
            }
        }
        // Flush remaining samples (less than batch_size)
        archive.commitBatch();
        total_count += count;
        return total_count;
    }
}
