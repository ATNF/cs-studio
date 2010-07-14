package org.csstudio.utility.pv.epics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.csstudio.platform.data.IDoubleValue;
import org.csstudio.platform.data.IEnumeratedMetaData;
import org.csstudio.platform.data.IEnumeratedValue;
import org.csstudio.platform.data.ILongValue;
import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.IValue;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.PVListener;
import org.junit.Ignore;
import org.junit.Test;

/** Test of the PV interface for a hardcoded implementation
 *  so that it can run as a unit test without Eclipse plugin loader.
 *  <p>
 *  These tests require the soft-IOC database from lib/test.db.
 *  <p>
 *  When using the JNI CA libs, one also needs ((DY)LD_LIBRARY_)PATH.
 *  <p>
 *  Results (Linux Laptop, local soft-IOC):
 *  <pre>
 *  Setup                        Tests   Connection Time     Free Memory
 *  JNI, DirectEventDispatcher     OK        <20 sec         5.8 MB
 *  JNI, QueuedEventDispatcher     OK        >32 sec         6.8 MB
 *  CAJ                            OK        10..140 sec     11.7 MB
 *  </pre>
 *  @author Kay Kasemir
 *
 *  FIXME (kasemir) : commented sysos (showstopper for org.csstudio.testsuite) - use assertions anyway
 */
@SuppressWarnings("nls")
public class EPICS_V3_PV_Test
{

    /** Get a PV.
     *
     *  <b>This is where the implementation is hard-coded!</b>
     *
     *  @return PV
     */
    static private PV getPV(final String name)
    {
        PVContext.use_pure_java = false;
        System.setProperty("gov.aps.jca.jni.ThreadSafeContext.event_dispatcher",
                           "gov.aps.jca.event.DirectEventDispatcher");
        //                   "gov.aps.jca.event.QueuedEventDispatcher");
        return new EPICS_V3_PV(name);
    }

    /** Update counter for TestListener */
    private final AtomicInteger updates = new AtomicInteger();

    /** Listener that dumps info and counts updates */
    class TestListener implements PVListener
    {
        private final String name;

        TestListener(final String name)
        {
            this.name = name;
        }

        public void pvValueUpdate(final PV pv)
        {
            updates.addAndGet(1);
            final IValue v = pv.getValue();
            ////System.out.println(name + ": " + pv.getName() + ", " + v.getTime() + " " + v);
        }

        public void pvDisconnected(final PV pv)
        {
            ////System.out.println(name + ": " + pv.getName() + " disconnected");
        }
    }

    @Test
    public void testSinglePVStartStop() throws Exception
    {
        final PV pv = getPV("fred");
        pv.addListener(new TestListener("A"));

        ////System.out.println("Checking monitors from single PV...");
        pv.start();
        int wait = 10;
        while (wait > 0)
        {
            if (updates.get() > 2) {
                break;
            }
            Thread.sleep(1000);
            --wait;
        }
        // Did we get anything?
        assertTrue(updates.get() > 2);
        pv.stop();
        ////System.out.println("Asserting that monitors stop...");
        final int old = updates.get();
        wait = 10;
        while (wait > 0)
        {
            Thread.sleep(1000);
            assertEquals("updates should stop", old, updates.get());
            --wait;
        }
    }

    @Test
    public void testMetaData() throws Exception
    {
        final PV pv = getPV("fred");
        pv.addListener(new TestListener("A"));
        pv.start();
        try
        {
            int wait = 10;
            while (wait > 0)
            {
                if (updates.get() > 2) {
                    break;
                }
                Thread.sleep(1000);
                --wait;
            }
            // Did we get anything?
            assertTrue(updates.get() > 2);
            // Meta info as expected?
            final INumericMetaData meta = (INumericMetaData)pv.getValue().getMetaData();
            assertEquals("furlong", meta.getUnits());
            assertEquals(4, meta.getPrecision());
        }
        finally
        {
            pv.stop();
        }
    }


    @Test
    public void testLong() throws Exception
    {
        final PV pva = getPV("long_fred");

        pva.start();

        threadSleepWithMaxTimeOut(pva);

        assertTrue(pva.isConnected());
        final IValue value = pva.getValue();
        ////System.out.println("'long' PV value: " + value);
        assertTrue(value instanceof ILongValue);

        pva.stop();
    }

    @Test
    public void testMultiplePVs() throws Exception
    {
        final PV pva = getPV("fred");
        final PV pvb = getPV("janet");

        pva.addListener(new TestListener("A"));
        pvb.addListener(new TestListener("B"));

        updates.set(0);
        pva.start();
        pvb.start();
        int wait = 10;
        while (wait > 0)
        {
            if (updates.get() > 4) {
                break;
            }
            Thread.sleep(1000);
            --wait;
        }
        assertTrue(updates.get() > 4);
        pvb.stop();
        pva.stop();
    }

    @Test
    public void testDuplicatePVs() throws Exception
    {
        final PV pva = getPV("fred");
        final PV pvb = getPV("fred");

        pva.addListener(new TestListener("A"));
        pvb.addListener(new TestListener("B"));

        updates.set(0);
        pva.start();
        pvb.start();
        int wait = 10;
        while (wait > 0)
        {
            if (updates.get() > 4) {
                break;
            }
            Thread.sleep(1000);
            --wait;
        }
        assertTrue(updates.get() > 4);
        pvb.stop();
        Thread.sleep(4000);
        pva.stop();
    }

    @Test
    public void testEnum() throws Exception
    {
        PV pva = getPV("fred.SCAN");

        pva.start();

        threadSleepWithMaxTimeOut(pva);

        assertTrue(pva.isConnected());
        assertTrue(pva.getValue() instanceof IEnumeratedValue);
        IEnumeratedValue e = (IEnumeratedValue) pva.getValue();
        assertEquals(6, e.getValue());
        assertEquals("1 second (6)", e.format());

        pva.stop();

        pva = getPV("enum");

        pva.start();

        threadSleepWithMaxTimeOut(pva);

        assertTrue(pva.isConnected());
        assertTrue(pva.getValue() instanceof IEnumeratedValue);
        e = (IEnumeratedValue) pva.getValue();
        assertEquals(1, e.getValue());
        assertEquals("one (1)", e.format());
        assertTrue(e.getMetaData() instanceof IEnumeratedMetaData);
        final IEnumeratedMetaData meta = (IEnumeratedMetaData) e.getMetaData();
        assertEquals(4, meta.getStates().length);
        assertEquals("zero", meta.getStates()[0]);

        pva.stop();
    }


    @Test
    public void testDblWaveform() throws Exception
    {
        final PV pva = getPV("hist");

        pva.start();

        threadSleepWithMaxTimeOut(pva);
        assertTrue(pva.isConnected());
        final IValue value = pva.getValue();
        assertTrue(value instanceof IDoubleValue);
        final double dbl[] = ((IDoubleValue) value).getValues();
        assertEquals(50, dbl.length);
        ////System.out.println(value);

        pva.stop();
    }

    @Test
    public void testLongWaveform() throws Exception
    {
        final PV pva = getPV("longs");

        pva.start();
        threadSleepWithMaxTimeOut(pva);
        assertTrue(pva.isConnected());
        final IValue value = pva.getValue();
        assertTrue(value instanceof ILongValue);
        final long longs[] = ((ILongValue) value).getValues();
        assertEquals(50, longs.length);
        ////System.out.println(value);

        pva.stop();
    }

    /**
     * @param pva
     * @throws InterruptedException
     */
    private void threadSleepWithMaxTimeOut(final PV pva) throws InterruptedException {
        final int maxTime = 1000;
        int duration = 0;
        final int interval = 100;

        while (!pva.isConnected()) {
            Thread.sleep(interval);

            duration += interval;
            if (duration >= maxTime) {
                Assert.fail("PVA is not connected after " + duration + " millis!");
                break;
            }
        }
    }

    @Ignore
    @Test
    public void testManyConnections() throws Exception
    {
        final int PV_Count = 10000;

        ////System.out.println("Creating " + PV_Count + " PVs...");
        final PV pvs[] = new PV[PV_Count];
        for (int i=0; i<PV_Count; ++i) {
            pvs[i] = getPV("ramp" + (i+1));
        }

        ////System.out.println("Starting " + PV_Count + " PVs...");
        final long start = System.currentTimeMillis();
        for (int i=0; i<PV_Count; ++i) {
            pvs[i].start();
        }
        int test = 0;
        int duration = 0;
        final int interval = 100;
        final int maxTime = 2000;
        while (true)
        {
            int connected = 0;
            int disconnected = 0;
            for (int i=0; i < PV_Count; ++i)
            {
                if (pvs[i].isConnected()) {
                    ++connected;
                } else {
                    ++disconnected;
                }
            }
            ++test;
            if (test >= 10)
            {
                ////System.out.format("%d out of %d disconnected\n", disconnected, PV_Count);
                test = 0;
            }
            if ((disconnected > 0) && (duration < maxTime)) {
                duration += interval;
                Thread.sleep(interval);
            } else {
                Assert.fail("Either max time is reached (duration >= " + maxTime +
                            ") or disconnected is greater 0.");
                break;
            }
        }
        // Time and ...
        final long time = System.currentTimeMillis() - start;
        ////System.out.println("Time to connect " + PV_Count + " channels: " + time/1000.0 +  " sec");
        // Memory Info
        final double MB = 1024.0*1024.0;
        final Runtime runtime = Runtime.getRuntime();
        final double freeMB = runtime.freeMemory()/MB;
        final double totalMB = runtime.totalMemory()/MB;
        ////System.out.format("Memory: %.2f of %.2f MB = %.0f %% free\n", freeMB, totalMB, freeMB/totalMB*100.0);
        ////System.out.println("Stopping " + PV_Count + " PVs...");
        for (int i=0; i<PV_Count; ++i)
        {
            pvs[i].stop();
            pvs[i] = null;
        }
        ////System.out.println("Done.");
    }

    @Test
    public void testMultipleRuns() throws Exception
    {
        // Disable INFO and DEBUG messages
        final Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.ERROR);
    	for (int run=0; run < 10; ++run) {
            testManyConnections();
        }
	}
}
