/*
 * Copyright (c) 2011 Stiftung Deutsches Elektronen-Synchrotron,
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
package org.csstudio.archive.common.engine.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.csstudio.archive.common.engine.ArchiveEnginePreference;
import org.csstudio.archive.common.engine.service.IServiceProvider;
import org.csstudio.archive.common.service.ArchiveServiceException;
import org.csstudio.archive.common.service.IArchiveEngineFacade;
import org.csstudio.archive.common.service.engine.ArchiveEngineId;
import org.csstudio.archive.common.service.sample.IArchiveSample;
import org.csstudio.archive.common.service.util.DataRescueException;
import org.csstudio.domain.desy.calc.CumulativeAverageCache;
import org.csstudio.domain.desy.system.ISystemVariable;
import org.csstudio.domain.desy.time.StopWatch;
import org.csstudio.domain.desy.time.StopWatch.RunningStopWatch;
import org.csstudio.domain.desy.time.TimeInstant;
import org.csstudio.domain.desy.time.TimeInstant.TimeInstantBuilder;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.service.osgi.OsgiServiceUnavailableException;

import com.google.common.collect.Lists;

/**
 * The independent worker to write/submit the samples to the persistence layer.
 *
 * @author bknerr
 * @since 14.02.2011
 */
final class WriteWorker implements Runnable {

    private static final Logger WORKER_LOG =
            CentralLogger.getInstance().getLogger(WriteWorker.class);

    private final String _name;
    private final Collection<ArchiveChannel<Object, ISystemVariable<Object>>> _channels;

    private final long _periodInMS;
    /** Average number of values per write run */
    private final CumulativeAverageCache _avgWriteCount = new CumulativeAverageCache();
    /** Average duration of write run */
    private final CumulativeAverageCache _avgWriteDurationInMS = new CumulativeAverageCache();


    private final IServiceProvider _provider;

    private final ArchiveEngineId _engineId;

    private TimeInstant _lastTimeWrite;

    /**
     * Constructor.
     */
    public WriteWorker(@Nonnull final ArchiveEngineId engineId,
                       @Nonnull final IServiceProvider provider,
                       @Nonnull final String name,
                       @Nonnull final Collection<ArchiveChannel<Object, ISystemVariable<Object>>> channels,
                       final long periodInMS) {
        _engineId = engineId;
        _provider = provider;
        _name = name;
        _channels = channels;
        _periodInMS = periodInMS;

        WORKER_LOG.info(_name + " created with period " + periodInMS + "ms");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        WORKER_LOG.info("RUN: " + TimeInstantBuilder.fromNow().formatted());

        List<IArchiveSample<Object, ISystemVariable<Object>>> samples = Collections.emptyList();
        try {
            final RunningStopWatch watch = StopWatch.start();

            samples = collectSamplesFromBuffers(_channels);

            final long written = writeSamples(_provider,  samples);

            _lastTimeWrite = TimeInstantBuilder.fromNow();

            updateEngineHeartBeat(_provider, _engineId, _lastTimeWrite);

            final long durationInMS = watch.getElapsedTimeInMillis();
            if (durationInMS >= _periodInMS) {
                // FIXME (bknerr) : this won't work, stupid
                //_writeExec.enhanceWriterThroughput(this);
            }

            _avgWriteCount.accumulate(Double.valueOf(written));
            _avgWriteDurationInMS.accumulate(Double.valueOf(durationInMS));
        } catch (final ArchiveServiceException e) {
            WORKER_LOG.error("Exception within service impl. Data rescue should be handled there.", e);
        }
    }

    private void updateEngineHeartBeat(@Nonnull final IServiceProvider provider,
                                       @Nonnull final ArchiveEngineId engineId,
                                       @Nonnull final TimeInstant lastTimeWrite)
                                       throws ArchiveServiceException {
        try {
            final IArchiveEngineFacade engineFacade  = provider.getEngineFacade();
            engineFacade.updateEngineIsAlive(engineId, lastTimeWrite);
        } catch (final OsgiServiceUnavailableException e) {
            WORKER_LOG.error("Service unavailable. Engine heartbeat for engine " + engineId + " not written.", e);
        }

    }

    private long writeSamples(@Nonnull final IServiceProvider provider,
                              @Nonnull final List<IArchiveSample<Object, ISystemVariable<Object>>> samples)
                              throws ArchiveServiceException {

        try {
            final IArchiveEngineFacade service = provider.getEngineFacade();
            service.writeSamples(samples);
        } catch (final OsgiServiceUnavailableException e) {
            try {
                ArchiveEngineSampleRescuer.with(samples).to(ArchiveEnginePreference.DATA_RESCUE_DIR.getValue()).rescue();
            } catch (final DataRescueException e1) {
                WORKER_LOG.error("Data rescue to file system failed!:" + e1.getMessage());
                throw new ArchiveServiceException("Data rescue failed.", e1);
            }
        }

        return samples.size();
    }

    @Nonnull
    private LinkedList<IArchiveSample<Object, ISystemVariable<Object>>>
    collectSamplesFromBuffers(@Nonnull final Collection<ArchiveChannel<Object, ISystemVariable<Object>>> channels) {

        final LinkedList<IArchiveSample<Object, ISystemVariable<Object>>> allSamples = Lists.newLinkedList();

        for (final ArchiveChannel<Object, ISystemVariable<Object>> channel : channels) {
            final SampleBuffer<Object, ISystemVariable<Object>, IArchiveSample<Object, ISystemVariable<Object>>> buffer =
                channel.getSampleBuffer();

            buffer.updateStats();
            buffer.drainTo(allSamples);
        }
        return allSamples;
    }

    public long getPeriodInMS() {
        return _periodInMS;
    }

    @Nonnull
    protected CumulativeAverageCache getAvgWriteCount() {
        return _avgWriteCount;
    }

    @Nonnull
    protected CumulativeAverageCache getAvgWriteDurationInMS() {
        return _avgWriteDurationInMS;
    }

    @CheckForNull
    protected TimeInstant getLastTimeWrite() {
        return _lastTimeWrite;
    }

    public void clear() {
        _avgWriteCount.clear();
        _avgWriteDurationInMS.clear();
        _lastTimeWrite = null;
    }
}
