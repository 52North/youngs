/*
 * Copyright 2015-2018 52°North Initiative for Geospatial Open Source
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.n52.youngs.api.Report;
import org.n52.youngs.control.Runner;
import org.n52.youngs.exception.MappingError;
import org.n52.youngs.exception.SinkError;
import org.n52.youngs.harvest.Source;
import org.n52.youngs.harvest.SourceException;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.ReportImpl;
import org.n52.youngs.load.Sink;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.postprocess.PostProcessor;
import org.n52.youngs.transform.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A harvesting runner that downloads a fixed number of records at a time from
 * the source, transforms these, and loads them into the sink.
 *
 * Everything happens within one thread and consequtively when the method "load"
 * is called.
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

    private final boolean testRun = false;

    private long startPosition = 1;
    private PostProcessor postProcessor;

    public SingleThreadBulkRunner() {
        //
    }

    public SingleThreadBulkRunner setBulkSize(long bulkSize) {
        this.bulkSize = bulkSize;
        return this;
    }

    public SingleThreadBulkRunner setStartPosition(long startPosition) {
        this.startPosition = startPosition;
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
    public Runner postTransformProcess(PostProcessor postProcessor) {
        this.postProcessor = postProcessor;
        log.debug("Saved postProcessor, waiting for load() to be called...", source);
        return this;
    }

    @Override
    public Report load(final Sink sink) {
        this.sink = sink;
        Objects.nonNull(source);
        Objects.nonNull(mapper);
        Objects.nonNull(this.sink);

        log.info("Starting harvest from {} to {} with {}", source, this.sink, mapper);
        Report report = new ReportImpl();

        try {
            boolean prepareSink = sink.prepare(mapper.getMapper());
            if (!prepareSink) {
                String msg = "The sink could not be prepared. Stopping load, please check the logs.";
                log.error(msg);
                report.addMessage(msg);
                return report;
            }
        } catch (SinkError e) {
            log.error("Problem preparing sink", e);
            report.addMessage(String.format("Problem preparing sink: %s", e.getMessage()));
            return report;
        }

        final Stopwatch timer = Stopwatch.createStarted();
        long pageStart = startPosition;
        long count = source.getRecordCount();
        final long limit = recordsLimit == Long.MAX_VALUE ? count : Math.min(recordsLimit + startPosition, count);

        final Stopwatch sourceTimer = Stopwatch.createUnstarted();
        final Stopwatch mappingTimer = Stopwatch.createUnstarted();
        final Stopwatch sinkTimer = Stopwatch.createUnstarted();
        final Stopwatch currentBulkTimer = Stopwatch.createUnstarted();
        double bulkTimeAvg = 0d;
        long runNumber = 0;

        while (pageStart <= limit) {
            currentBulkTimer.start();

            long recordsLeft = limit - pageStart + 1;
            long size = Math.min(recordsLeft, bulkSize);
            if (size <= 0) {
                break;
            }
            log.info("### [{}] Requesting {} records from {} starting at {}, last requested record will be {} ###",
                    runNumber, size, source.getEndpoint(), pageStart, limit);

            try {
                sourceTimer.start();
                Collection<SourceRecord> records = source.getRecords(pageStart, size, report);
                sourceTimer.stop();

                log.debug("Mapping {} retrieved records.", records.size());
                mappingTimer.start();
                List<SinkRecord> mappedRecords = records.stream()
                        .map(record -> {
                            try {
                                SinkRecord r = mapper.map(record);
                                if (this.postProcessor != null) {
                                    return this.postProcessor.process(r);
                                }
                                return r;
                            } catch (MappingError e) {
                                report.addFailedRecord(record.toString(), "Problem during mapping: " + e.getMessage());
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                mappingTimer.stop();

                log.debug("Storing {} mapped records.", mappedRecords.size());
                if (!testRun) {
                    sinkTimer.start();
                    mappedRecords.forEach(record -> {
                        try {
                            boolean result = sink.store(record);
                            if (result) {
                                report.addSuccessfulRecord(record.getId());
                            } else {
                                report.addFailedRecord(record.getId(), "see sink log");
                            }
                        } catch (SinkError e) {
                            log.warn("Problem during mapping: ", e);
                            report.addFailedRecord(record.toString(), "Problem during mapping: " + e.getMessage());
                        }
                    });
                    sinkTimer.stop();
                } else {
                    log.info("TESTRUN, created documents are:\n{}", Arrays.toString(mappedRecords.toArray()));
                }

            } catch (SourceException | RuntimeException e) {
                if (sourceTimer.isRunning()) {
                    sourceTimer.stop();
                }
                if (mappingTimer.isRunning()) {
                    mappingTimer.stop();
                }
                if (sinkTimer.isRunning()) {
                    sinkTimer.stop();
                }

                String msg = String.format("Problem processing records %s to %s: %s", pageStart, pageStart + size, e.getMessage());
                log.error(msg, e);
                report.addMessage(msg);
            }

            pageStart += bulkSize;

            currentBulkTimer.stop();
            bulkTimeAvg = ((bulkTimeAvg * runNumber) + currentBulkTimer.elapsed(TimeUnit.SECONDS)) / (runNumber + 1);
            updateAndLog(runNumber, (runNumber + 1) * bulkSize, currentBulkTimer.elapsed(TimeUnit.SECONDS), bulkTimeAvg);
            currentBulkTimer.reset();

            runNumber++;
        }

        timer.stop();
        log.info("Completed harvesting for {} ({} failed) of {} records in {} minutes",
                report.getNumberOfRecordsAdded(),
                report.getNumberOfRecordsFailed(),
                source.getRecordCount(),
                timer.elapsed(TimeUnit.MINUTES));
        log.info("Time spent (minutes): source={}, mapping={}, sink={}", sourceTimer.elapsed(TimeUnit.MINUTES),
                mappingTimer.elapsed(TimeUnit.MINUTES), sinkTimer.elapsed(TimeUnit.MINUTES));

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

    private void updateAndLog(long run, long pageStart, long bulkSeconds, double bulkAverageSeconds) {
        double percentageTask = (double) pageStart / this.recordsLimit * 100;
        this.completedPercentage = Optional.of(percentageTask);
        log.info("### [{}] Completed {}% of task in {} seconds (avg: {} seconds) ###",
                run,
                String.format("%1$,.2f", getCompletedPercentage()),
                bulkSeconds,
                String.format("%1$,.2f", bulkAverageSeconds));
    }

}
