/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.sidecar.config.yaml;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.cassandra.sidecar.common.DataObjectBuilder;
import org.apache.cassandra.sidecar.config.RestoreJobConfiguration;

/**
 * Configuration needed restore jobs restoring data from blob
 */
public class RestoreJobConfigurationImpl implements RestoreJobConfiguration
{
    private static final long MIN_RESTORE_JOB_TABLES_TTL_SECONDS = TimeUnit.DAYS.toSeconds(14);

    private static final long DEFAULT_JOB_DISCOVERY_ACTIVE_LOOP_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final long DEFAULT_JOB_DISCOVERY_IDLE_LOOP_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final int DEFAULT_JOB_DISCOVERY_MINIMUM_RECENCY_DAYS = 5;
    private static final int DEFAULT_PROCESS_MAX_CONCURRENCY = 20; // process at most 20 slices concurrently
    private static final long DEFAULT_RESTORE_JOB_TABLES_TTL_SECONDS = TimeUnit.DAYS.toSeconds(90);
    // A restore task is considered slow if it has been in the "active" list for 10 minutes.
    private static final long DEFAULT_RESTORE_JOB_SLOW_TASK_THRESHOLD_SECONDS = TimeUnit.MINUTES.toSeconds(10);
    // report once a minute
    private static final long DEFAULT_RESTORE_JOB_SLOW_TASK_REPORT_DELAY_SECONDS = TimeUnit.MINUTES.toSeconds(1);
    public static final long DEFAULT_RING_TOPOLOGY_REFRESH_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(1);

    @JsonProperty(value = "job_discovery_active_loop_delay_millis")
    protected final long jobDiscoveryActiveLoopDelayMillis;

    @JsonProperty(value = "job_discovery_idle_loop_delay_millis")
    protected final long jobDiscoveryIdleLoopDelayMillis;

    @JsonProperty(value = "job_discovery_minimum_recency_days")
    protected final int jobDiscoveryMinimumRecencyDays;

    @JsonProperty(value = "slice_process_max_concurrency")
    protected final int processMaxConcurrency;

    @JsonProperty(value = "restore_job_tables_ttl_seconds")
    protected final long restoreJobTablesTtlSeconds;

    @JsonProperty(value = "slow_task_threshold_seconds")
    protected final long slowTaskThresholdSeconds;

    @JsonProperty(value = "slow_task_report_delay_seconds")
    protected final long slowTaskReportDelaySeconds;

    @JsonProperty(value = "ring_topology_refresh_delay_millis")
    private final long ringTopologyRefreshDelayMillis;

    protected RestoreJobConfigurationImpl()
    {
        this(builder());
    }

    protected RestoreJobConfigurationImpl(Builder builder)
    {
        this.jobDiscoveryActiveLoopDelayMillis = builder.jobDiscoveryActiveLoopDelayMillis;
        this.jobDiscoveryIdleLoopDelayMillis = builder.jobDiscoveryIdleLoopDelayMillis;
        this.jobDiscoveryMinimumRecencyDays = builder.jobDiscoveryMinimumRecencyDays;
        this.processMaxConcurrency = builder.processMaxConcurrency;
        this.restoreJobTablesTtlSeconds = builder.restoreJobTablesTtlSeconds;
        this.slowTaskThresholdSeconds = builder.slowTaskThresholdSeconds;
        this.slowTaskReportDelaySeconds = builder.slowTaskReportDelaySeconds;
        this.ringTopologyRefreshDelayMillis = builder.ringTopologyRefreshDelayMillis;
        validate();
    }

    private void validate()
    {
        long ttl = restoreJobTablesTtlSeconds();
        if (ttl < MIN_RESTORE_JOB_TABLES_TTL_SECONDS)
        {
            throw new IllegalArgumentException("restoreJobTablesTtl cannot be less than "
                                               + MIN_RESTORE_JOB_TABLES_TTL_SECONDS);
        }
        if (TimeUnit.DAYS.toSeconds(jobDiscoveryMinimumRecencyDays()) >= ttl)
        {
            throw new IllegalArgumentException("JobDiscoveryMinimumRecencyDays (in seconds) cannot be greater than "
                                               + ttl);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = "job_discovery_active_loop_delay_millis")
    public long jobDiscoveryActiveLoopDelayMillis()
    {
        return jobDiscoveryActiveLoopDelayMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = "job_discovery_idle_loop_delay_millis")
    public long jobDiscoveryIdleLoopDelayMillis()
    {
        return jobDiscoveryActiveLoopDelayMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = "job_discovery_minimum_recency_days")
    public int jobDiscoveryMinimumRecencyDays()
    {
        return jobDiscoveryMinimumRecencyDays;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = "slice_process_max_concurrency")
    public int processMaxConcurrency()
    {
        return processMaxConcurrency;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = "restore_job_tables_ttl_seconds")
    public long restoreJobTablesTtlSeconds()
    {
        return restoreJobTablesTtlSeconds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = "slow_task_threshold_seconds")
    public long slowTaskThresholdSeconds()
    {
        return slowTaskThresholdSeconds;
    }

    @Override
    @JsonProperty(value = "slow_task_report_delay_seconds")
    public long slowTaskReportDelaySeconds()
    {
        return slowTaskReportDelaySeconds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = "ring_topology_refresh_delay_millis")
    public long ringTopologyRefreshDelayMillis()
    {
        return ringTopologyRefreshDelayMillis;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * {@code RestoreJobConfigurationImpl} builder static inner class.
     */
    public static class Builder implements DataObjectBuilder<Builder, RestoreJobConfigurationImpl>
    {
        private long slowTaskThresholdSeconds = DEFAULT_RESTORE_JOB_SLOW_TASK_THRESHOLD_SECONDS;
        private long slowTaskReportDelaySeconds = DEFAULT_RESTORE_JOB_SLOW_TASK_REPORT_DELAY_SECONDS;
        private long jobDiscoveryActiveLoopDelayMillis = DEFAULT_JOB_DISCOVERY_ACTIVE_LOOP_DELAY_MILLIS;
        private long jobDiscoveryIdleLoopDelayMillis = DEFAULT_JOB_DISCOVERY_IDLE_LOOP_DELAY_MILLIS;
        private int jobDiscoveryMinimumRecencyDays = DEFAULT_JOB_DISCOVERY_MINIMUM_RECENCY_DAYS;
        private int processMaxConcurrency = DEFAULT_PROCESS_MAX_CONCURRENCY;
        private long restoreJobTablesTtlSeconds = DEFAULT_RESTORE_JOB_TABLES_TTL_SECONDS;
        private long ringTopologyRefreshDelayMillis = DEFAULT_RING_TOPOLOGY_REFRESH_DELAY_MILLIS;

        protected Builder()
        {
        }

        @Override
        public RestoreJobConfigurationImpl.Builder self()
        {
            return this;
        }

        /**
         * Sets the {@code jobDiscoveryActiveLoopDelayMillis} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param jobDiscoveryActiveLoopDelayMillis the {@code jobDiscoveryActiveLoopDelayMillis} to set
         * @return a reference to this Builder
         */
        public Builder jobDiscoveryActiveLoopDelayMillis(long jobDiscoveryActiveLoopDelayMillis)
        {
            return update(b -> b.jobDiscoveryActiveLoopDelayMillis = jobDiscoveryActiveLoopDelayMillis);
        }

        /**
         * Sets the {@code jobDiscoveryIdleLoopDelayMillis} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param jobDiscoveryIdleLoopDelayMillis the {@code jobDiscoveryIdleLoopDelayMillis} to set
         * @return a reference to this Builder
         */
        public Builder jobDiscoveryIdleLoopDelayMillis(long jobDiscoveryIdleLoopDelayMillis)
        {
            return update(b -> b.jobDiscoveryIdleLoopDelayMillis = jobDiscoveryIdleLoopDelayMillis);
        }

        /**
         * Sets the {@code jobDiscoveryMinimumRecencyDays} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param jobDiscoveryMinimumRecencyDays the {@code jobDiscoveryMinimumRecencyDays} to set
         * @return a reference to this Builder
         */
        public Builder jobDiscoveryMinimumRecencyDays(int jobDiscoveryMinimumRecencyDays)
        {
            return update(b -> b.jobDiscoveryMinimumRecencyDays = jobDiscoveryMinimumRecencyDays);
        }

        /**
         * Sets the {@code processMaxConcurrency} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param processMaxConcurrency the {@code processMaxConcurrency} to set
         * @return a reference to this Builder
         */
        public Builder processMaxConcurrency(int processMaxConcurrency)
        {
            return update(b -> b.processMaxConcurrency = processMaxConcurrency);
        }

        /**
         * Sets the {@code restoreJobTablesTtlSeconds} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param restoreJobTablesTtlSeconds the {@code restoreJobTablesTtlSeconds} to set
         * @return a reference to this Builder
         */
        public Builder restoreJobTablesTtlSeconds(long restoreJobTablesTtlSeconds)
        {
            return update(b -> b.restoreJobTablesTtlSeconds = restoreJobTablesTtlSeconds);
        }

        /**
         * Sets the {@code slowTaskThresholdSeconds} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param slowTaskThresholdSeconds the {@code slowTaskThresholdSeconds} to set
         * @return a reference to this Builder
         */
        public Builder slowTaskThresholdSeconds(long slowTaskThresholdSeconds)
        {
            return update(b -> b.slowTaskThresholdSeconds = slowTaskThresholdSeconds);
        }

        /**
         * Sets the {@code slowTaskReportDelaySeconds} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param slowTaskReportDelaySeconds the {@code slowTaskReportDelaySeconds} to set
         * @return a reference to this Builder
         */
        public Builder slowTaskReportDelaySeconds(long slowTaskReportDelaySeconds)
        {
            return update(b -> b.slowTaskReportDelaySeconds = slowTaskReportDelaySeconds);
        }

        /**
         * Sets the {@code ringTopologyRefreshDelayMillis} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param ringTopologyRefreshDelayMillis the {@code ringTopologyRefreshDelayMillis} to set
         * @return a reference to this Builder
         */
        public Builder ringTopologyRefreshDelayMillis(long ringTopologyRefreshDelayMillis)
        {
            return update(b -> b.ringTopologyRefreshDelayMillis = ringTopologyRefreshDelayMillis);
        }

        @Override
        public RestoreJobConfigurationImpl build()
        {
            return new RestoreJobConfigurationImpl(this);
        }
    }
}
