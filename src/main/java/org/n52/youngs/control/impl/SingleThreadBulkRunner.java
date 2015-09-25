/*
 * Copyright 2015-2015 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.youngs.control.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.n52.youngs.api.Record;
import org.n52.youngs.api.Report;
import org.n52.youngs.control.Runner;
import org.n52.youngs.harvest.Source;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.ReportImpl;
import org.n52.youngs.load.Sink;
import org.n52.youngs.transform.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A harvesting runner that downloads a fixed number of records at a time from the source, transforms these, and loads them into the sink.
 *
 * Everything happens within one thread and consequtively when the method "load" is called.
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class SingleThreadBulkRunner implements Runner {

    private static final Logger log = LoggerFactory.getLogger(SingleThreadBulkRunner.class);

    private static final long DEFAULT_BULK_SIZE = 10;

    private long bulkSize = DEFAULT_BULK_SIZE;

    private Source source;

    private Mapper mapper;

    private long recordsLimit = Long.MAX_VALUE;

    private Optional<Double> completedPercentage = Optional.empty();

    private Sink sink;

    private final boolean testRun = true;

    public SingleThreadBulkRunner() {
        //
    }

    public SingleThreadBulkRunner setBulkSize(long bulkSize) {
        this.bulkSize = bulkSize;
        return this;
    }

    public SingleThreadBulkRunner setRecordsLimit(long recordsLimit) {
        this.recordsLimit = recordsLimit;
        return this;
    }

    @Override
    public SingleThreadBulkRunner harvest(final Source source) {
        this.source = source;
        log.debug("Saved source, waiting for load() to be called...", source);
        return this;
    }

    @Override
    public SingleThreadBulkRunner transform(final Mapper mapper) {
        this.mapper = mapper;
        log.debug("Saved mapper, waiting for load() to be called...", source);
        return this;
    }

    @Override
    public Report load(final Sink sink) {
        this.sink = sink;
        Objects.nonNull(source);
        Objects.nonNull(mapper);
        Objects.nonNull(sink);

        log.info("Starting harvest from {} to {} with {}", source, sink, mapper);
        Report report = new ReportImpl();

        boolean prepareSink = sink.prepare(mapper.getMapper());
        if(!prepareSink) {
            String msg = "The sink could not be prepared. Stopping load, please check the logs.";
            log.error(msg);
            report.addMessage(msg);
            return report;
        }

        final Stopwatch timer = Stopwatch.createStarted();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        long counter = 1;

        while (counter <= Math.min(recordsLimit, source.getRecordCount())) {
            long recordsLeft = source.getRecordCount() - counter;
            long size = Math.min(recordsLeft, bulkSize);

            log.info("Requesting {} of {} records from {} starting at {}", size, source.getRecordCount(), source.getEndpoint(), counter);
            Collection<Record> records = source.getRecords(counter, size);
            log.debug("Mapping {} retrieved records.", records.size());

            List<Record> mappedRecords = records.stream()
                    .filter(r -> r instanceof SourceRecord)
                    .map(r -> (SourceRecord) r)
                    .map(mapper::map)
                    .collect(Collectors.toList());

            if (!testRun) {
                sink.store(mappedRecords);
            } else {
                log.info("TESTRUN, created documents are:\n{}", Arrays.toString(mappedRecords.toArray()));
            }

            counter += bulkSize;
            updateCompletedPercentage(counter);
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.error("during shut down of map executor", e);
        }

        timer.stop();
        log.info("Completed harvesting for {} of {} records in {} seconds",
                counter,
                source.getRecordCount(),
                timer.elapsed(TimeUnit.SECONDS));

        return report;
    }

    @Override
    public double getCompletedPercentage() {
        return this.completedPercentage.orElse(Double.NEGATIVE_INFINITY);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("source", source)
                .add("mapper", mapper)
                .add("sink", sink).toString();
    }

    private void updateCompletedPercentage(long counter) {
        double percentage = (double) counter / source.getRecordCount();
        this.completedPercentage = Optional.of(percentage);
        log.info("Completed {}%", String.format("%1$,.5f", this.completedPercentage.get()));
    }

}
