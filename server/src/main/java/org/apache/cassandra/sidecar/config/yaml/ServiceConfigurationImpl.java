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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.cassandra.sidecar.common.DataObjectBuilder;
import org.apache.cassandra.sidecar.config.CdcConfiguration;
import org.apache.cassandra.sidecar.config.JmxConfiguration;
import org.apache.cassandra.sidecar.config.SSTableImportConfiguration;
import org.apache.cassandra.sidecar.config.SSTableSnapshotConfiguration;
import org.apache.cassandra.sidecar.config.SSTableUploadConfiguration;
import org.apache.cassandra.sidecar.config.SchemaKeyspaceConfiguration;
import org.apache.cassandra.sidecar.config.ServiceConfiguration;
import org.apache.cassandra.sidecar.config.ThrottleConfiguration;
import org.apache.cassandra.sidecar.config.TrafficShapingConfiguration;
import org.apache.cassandra.sidecar.config.WorkerPoolConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for the Sidecar Service and configuration of the REST endpoints in the service
 */
public class ServiceConfigurationImpl implements ServiceConfiguration
{
    public static final String HOST_PROPERTY = "host";
    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final String PORT_PROPERTY = "port";
    public static final int DEFAULT_PORT = 9043;
    public static final String REQUEST_IDLE_TIMEOUT_MILLIS_PROPERTY = "request_idle_timeout_millis";
    public static final int DEFAULT_REQUEST_IDLE_TIMEOUT_MILLIS = 300000;
    public static final String REQUEST_TIMEOUT_MILLIS_PROPERTY = "request_timeout_millis";
    public static final long DEFAULT_REQUEST_TIMEOUT_MILLIS = 300000L;
    public static final String TCP_KEEP_ALIVE_PROPERTY = "tcp_keep_alive";
    public static final boolean DEFAULT_TCP_KEEP_ALIVE = false;
    public static final String ACCEPT_BACKLOG_PROPERTY = "accept_backlog";
    public static final int DEFAULT_ACCEPT_BACKLOG = 1024;
    public static final String ALLOWABLE_SKEW_IN_MINUTES_PROPERTY = "allowable_time_skew_in_minutes";
    public static final int DEFAULT_ALLOWABLE_SKEW_IN_MINUTES = 60;
    private static final String SERVER_VERTICLE_INSTANCES_PROPERTY = "server_verticle_instances";
    private static final int DEFAULT_SERVER_VERTICLE_INSTANCES = 1;
    public static final String THROTTLE_PROPERTY = "throttle";
    public static final String SSTABLE_UPLOAD_PROPERTY = "sstable_upload";
    public static final String SSTABLE_IMPORT_PROPERTY = "sstable_import";
    public static final String SSTABLE_SNAPSHOT_PROPERTY = "sstable_snapshot";
    public static final String WORKER_POOLS_PROPERTY = "worker_pools";
    private static final String JMX_PROPERTY = "jmx";
    private static final String TRAFFIC_SHAPING_PROPERTY = "traffic_shaping";
    private static final String SCHEMA = "schema";
    private static final String CDC = "cdc";
    protected static final Map<String, WorkerPoolConfiguration> DEFAULT_WORKER_POOLS_CONFIGURATION
    = Collections.unmodifiableMap(new HashMap<String, WorkerPoolConfiguration>()
    {{
        put(SERVICE_POOL, new WorkerPoolConfigurationImpl("sidecar-worker-pool", 20,
                                                          TimeUnit.SECONDS.toMillis(60)));

        put(INTERNAL_POOL, new WorkerPoolConfigurationImpl("sidecar-internal-worker-pool", 20,
                                                           TimeUnit.MINUTES.toMillis(15)));
    }});


    @JsonProperty(value = HOST_PROPERTY, defaultValue = DEFAULT_HOST)
    protected final String host;

    @JsonProperty(value = PORT_PROPERTY, defaultValue = DEFAULT_PORT + "")
    protected final int port;

    @JsonProperty(value = REQUEST_IDLE_TIMEOUT_MILLIS_PROPERTY, defaultValue = DEFAULT_REQUEST_IDLE_TIMEOUT_MILLIS + "")
    protected final int requestIdleTimeoutMillis;

    @JsonProperty(value = REQUEST_TIMEOUT_MILLIS_PROPERTY, defaultValue = DEFAULT_REQUEST_TIMEOUT_MILLIS + "")
    protected final long requestTimeoutMillis;

    @JsonProperty(value = TCP_KEEP_ALIVE_PROPERTY, defaultValue = DEFAULT_TCP_KEEP_ALIVE + "")
    protected final boolean tcpKeepAlive;

    @JsonProperty(value = ACCEPT_BACKLOG_PROPERTY, defaultValue = DEFAULT_ACCEPT_BACKLOG + "")
    protected final int acceptBacklog;

    @JsonProperty(value = ALLOWABLE_SKEW_IN_MINUTES_PROPERTY, defaultValue = DEFAULT_ALLOWABLE_SKEW_IN_MINUTES + "")
    protected final int allowableSkewInMinutes;

    @JsonProperty(value = SERVER_VERTICLE_INSTANCES_PROPERTY, defaultValue = DEFAULT_SERVER_VERTICLE_INSTANCES + "")
    protected final int serverVerticleInstances;

    @JsonProperty(value = THROTTLE_PROPERTY)
    protected final ThrottleConfiguration throttleConfiguration;

    @JsonProperty(value = SSTABLE_UPLOAD_PROPERTY)
    protected final SSTableUploadConfiguration sstableUploadConfiguration;

    @JsonProperty(value = SSTABLE_IMPORT_PROPERTY)
    protected final SSTableImportConfiguration sstableImportConfiguration;

    @JsonProperty(value = SSTABLE_SNAPSHOT_PROPERTY)
    protected final SSTableSnapshotConfiguration sstableSnapshotConfiguration;

    @JsonProperty(value = WORKER_POOLS_PROPERTY)
    protected final Map<String, ? extends WorkerPoolConfiguration> workerPoolsConfiguration;

    @JsonProperty(value = JMX_PROPERTY)
    protected final JmxConfiguration jmxConfiguration;

    @JsonProperty(value = TRAFFIC_SHAPING_PROPERTY)
    protected final TrafficShapingConfiguration trafficShapingConfiguration;

    @JsonProperty(value = SCHEMA)
    protected final SchemaKeyspaceConfiguration schemaKeyspaceConfiguration;

    @JsonProperty(value = CDC)
    protected final CdcConfiguration cdcConfiguration;

    /**
     * Constructs a new {@link ServiceConfigurationImpl} with the default values
     */
    public ServiceConfigurationImpl()
    {
        this(builder());
    }

    /**
     * Constructs a new {@link ServiceConfigurationImpl} with the configured {@link Builder}
     *
     * @param builder the builder object
     */
    protected ServiceConfigurationImpl(Builder builder)
    {
        host = builder.host;
        port = builder.port;
        requestIdleTimeoutMillis = builder.requestIdleTimeoutMillis;
        requestTimeoutMillis = builder.requestTimeoutMillis;
        tcpKeepAlive = builder.tcpKeepAlive;
        acceptBacklog = builder.acceptBacklog;
        allowableSkewInMinutes = builder.allowableSkewInMinutes;
        serverVerticleInstances = builder.serverVerticleInstances;
        throttleConfiguration = builder.throttleConfiguration;
        sstableUploadConfiguration = builder.sstableUploadConfiguration;
        sstableImportConfiguration = builder.sstableImportConfiguration;
        sstableSnapshotConfiguration = builder.sstableSnapshotConfiguration;
        workerPoolsConfiguration = builder.workerPoolsConfiguration;
        jmxConfiguration = builder.jmxConfiguration;
        trafficShapingConfiguration = builder.trafficShapingConfiguration;
        schemaKeyspaceConfiguration = builder.schemaKeyspaceConfiguration;
        cdcConfiguration = builder.cdcConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = HOST_PROPERTY)
    public String host()
    {
        return host;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = PORT_PROPERTY)
    public int port()
    {
        return port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = REQUEST_IDLE_TIMEOUT_MILLIS_PROPERTY)
    public int requestIdleTimeoutMillis()
    {
        return requestIdleTimeoutMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = REQUEST_TIMEOUT_MILLIS_PROPERTY)
    public long requestTimeoutMillis()
    {
        return requestTimeoutMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = TCP_KEEP_ALIVE_PROPERTY)
    public boolean tcpKeepAlive()
    {
        return tcpKeepAlive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = ACCEPT_BACKLOG_PROPERTY)
    public int acceptBacklog()
    {
        return acceptBacklog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = ALLOWABLE_SKEW_IN_MINUTES_PROPERTY)
    public int allowableSkewInMinutes()
    {
        return allowableSkewInMinutes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = SERVER_VERTICLE_INSTANCES_PROPERTY)
    public int serverVerticleInstances()
    {
        return serverVerticleInstances;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = THROTTLE_PROPERTY)
    public ThrottleConfiguration throttleConfiguration()
    {
        return throttleConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = SSTABLE_UPLOAD_PROPERTY)
    public SSTableUploadConfiguration sstableUploadConfiguration()
    {
        return sstableUploadConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = SSTABLE_IMPORT_PROPERTY)
    public SSTableImportConfiguration sstableImportConfiguration()
    {
        return sstableImportConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = SSTABLE_SNAPSHOT_PROPERTY)
    public SSTableSnapshotConfiguration sstableSnapshotConfiguration()
    {
        return sstableSnapshotConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = WORKER_POOLS_PROPERTY)
    public Map<String, ? extends WorkerPoolConfiguration> workerPoolsConfiguration()
    {
        return workerPoolsConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = JMX_PROPERTY)
    public JmxConfiguration jmxConfiguration()
    {
        return jmxConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = TRAFFIC_SHAPING_PROPERTY)
    public TrafficShapingConfiguration trafficShapingConfiguration()
    {
        return trafficShapingConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty(value = SCHEMA)
    public SchemaKeyspaceConfiguration schemaKeyspaceConfiguration()
    {
        return schemaKeyspaceConfiguration;
    }

    /**
     * @return the configuration for cdc
     */
    @Override
    @JsonProperty(value = CDC)
    @Nullable
    public CdcConfiguration cdcConfiguration()
    {
        return cdcConfiguration;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * {@code ServiceConfigurationImpl} builder static inner class.
     */
    public static class Builder implements DataObjectBuilder<Builder, ServiceConfigurationImpl>
    {
        protected String host = DEFAULT_HOST;
        protected int port = DEFAULT_PORT;
        protected int requestIdleTimeoutMillis = DEFAULT_REQUEST_IDLE_TIMEOUT_MILLIS;
        protected long requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MILLIS;
        protected boolean tcpKeepAlive = DEFAULT_TCP_KEEP_ALIVE;
        protected int acceptBacklog = DEFAULT_ACCEPT_BACKLOG;
        protected int allowableSkewInMinutes = DEFAULT_ALLOWABLE_SKEW_IN_MINUTES;
        protected int serverVerticleInstances = DEFAULT_SERVER_VERTICLE_INSTANCES;
        protected ThrottleConfiguration throttleConfiguration = new ThrottleConfigurationImpl();
        protected SSTableUploadConfiguration sstableUploadConfiguration = new SSTableUploadConfigurationImpl();
        protected SSTableImportConfiguration sstableImportConfiguration = new SSTableImportConfigurationImpl();
        protected SSTableSnapshotConfiguration sstableSnapshotConfiguration = new SSTableSnapshotConfigurationImpl();
        protected Map<String, ? extends WorkerPoolConfiguration> workerPoolsConfiguration =
        DEFAULT_WORKER_POOLS_CONFIGURATION;
        protected JmxConfiguration jmxConfiguration = new JmxConfigurationImpl();
        protected TrafficShapingConfiguration trafficShapingConfiguration = new TrafficShapingConfigurationImpl();
        protected SchemaKeyspaceConfiguration schemaKeyspaceConfiguration = new SchemaKeyspaceConfigurationImpl();
        protected CdcConfiguration cdcConfiguration = new CdcConfigurationImpl();

        private Builder()
        {
        }

        @Override
        public Builder self()
        {
            return this;
        }

        /**
         * Sets the {@code host} and returns a reference to this Builder enabling method chaining.
         *
         * @param host the {@code host} to set
         * @return a reference to this Builder
         */
        public Builder host(String host)
        {
            return update(b -> b.host = host);
        }

        /**
         * Sets the {@code port} and returns a reference to this Builder enabling method chaining.
         *
         * @param port the {@code port} to set
         * @return a reference to this Builder
         */
        public Builder port(int port)
        {
            return update(b -> b.port = port);
        }

        /**
         * Sets the {@code requestIdleTimeoutMillis} and returns a reference to this Builder enabling method chaining.
         *
         * @param requestIdleTimeoutMillis the {@code requestIdleTimeoutMillis} to set
         * @return a reference to this Builder
         */
        public Builder requestIdleTimeoutMillis(int requestIdleTimeoutMillis)
        {
            return update(b -> b.requestIdleTimeoutMillis = requestIdleTimeoutMillis);
        }

        /**
         * Sets the {@code requestTimeoutMillis} and returns a reference to this Builder enabling method chaining.
         *
         * @param requestTimeoutMillis the {@code requestTimeoutMillis} to set
         * @return a reference to this Builder
         */
        public Builder requestTimeoutMillis(long requestTimeoutMillis)
        {
            return update(b -> b.requestTimeoutMillis = requestTimeoutMillis);
        }

        /**
         * Sets the {@code tcpKeepAlive} and returns a reference to this Builder enabling method chaining.
         *
         * @param tcpKeepAlive the {@code tcpKeepAlive} to set
         * @return a reference to this Builder
         */
        public Builder tcpKeepAlive(boolean tcpKeepAlive)
        {
            return update(b -> b.tcpKeepAlive = tcpKeepAlive);
        }

        /**
         * Sets the {@code acceptBacklog} and returns a reference to this Builder enabling method chaining.
         *
         * @param acceptBacklog the {@code acceptBacklog} to set
         * @return a reference to this Builder
         */
        public Builder acceptBacklog(int acceptBacklog)
        {
            return update(b -> b.acceptBacklog = acceptBacklog);
        }

        /**
         * Sets the {@code allowableSkewInMinutes} and returns a reference to this Builder enabling method chaining.
         *
         * @param allowableSkewInMinutes the {@code allowableSkewInMinutes} to set
         * @return a reference to this Builder
         */
        public Builder allowableSkewInMinutes(int allowableSkewInMinutes)
        {
            return update(b -> b.allowableSkewInMinutes = allowableSkewInMinutes);
        }

        /**
         * Sets the {@code serverVerticleInstances} and returns a reference to this Builder enabling method chaining.
         *
         * @param serverVerticleInstances the {@code serverVerticleInstances} to set
         * @return a reference to this Builder
         */
        public Builder serverVerticleInstances(int serverVerticleInstances)
        {
            return update(b -> b.serverVerticleInstances = serverVerticleInstances);
        }

        /**
         * Sets the {@code throttleConfiguration} and returns a reference to this Builder enabling method chaining.
         *
         * @param throttleConfiguration the {@code throttleConfiguration} to set
         * @return a reference to this Builder
         */
        public Builder throttleConfiguration(ThrottleConfiguration throttleConfiguration)
        {
            return update(b -> b.throttleConfiguration = throttleConfiguration);
        }

        /**
         * Sets the {@code sstableUploadConfiguration} and returns a reference to this Builder enabling method chaining.
         *
         * @param sstableUploadConfiguration the {@code sstableUploadConfiguration} to set
         * @return a reference to this Builder
         */
        public Builder sstableUploadConfiguration(SSTableUploadConfiguration sstableUploadConfiguration)
        {
            return update(b -> b.sstableUploadConfiguration = sstableUploadConfiguration);
        }

        /**
         * Sets the {@code sstableImportConfiguration} and returns a reference to this Builder enabling method chaining.
         *
         * @param sstableImportConfiguration the {@code sstableImportConfiguration} to set
         * @return a reference to this Builder
         */
        public Builder sstableImportConfiguration(SSTableImportConfiguration sstableImportConfiguration)
        {
            return update(b -> b.sstableImportConfiguration = sstableImportConfiguration);
        }

        /**
         * Sets the {@code sstableSnapshotConfiguration} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param sstableSnapshotConfiguration the {@code sstableSnapshotConfiguration} to set
         * @return a reference to this Builder
         */
        public Builder sstableSnapshotConfiguration(SSTableSnapshotConfiguration sstableSnapshotConfiguration)
        {
            return update(b -> b.sstableSnapshotConfiguration = sstableSnapshotConfiguration);
        }

        /**
         * Sets the {@code workerPoolsConfiguration} and returns a reference to this Builder enabling method chaining.
         *
         * @param workerPoolsConfiguration the {@code workerPoolsConfiguration} to set
         * @return a reference to this Builder
         */
        public Builder workerPoolsConfiguration(Map<String, ? extends WorkerPoolConfiguration> workerPoolsConfiguration)
        {
            return update(b -> b.workerPoolsConfiguration = workerPoolsConfiguration);
        }

        /**
         * Sets the {@code jmxConfiguration} and returns a reference to this Builder enabling method chaining.
         *
         * @param jmxConfiguration the {@code jmxConfiguration} to set
         * @return a reference to this Builder
         */
        public Builder jmxConfiguration(JmxConfiguration jmxConfiguration)
        {
            return update(b -> b.jmxConfiguration = jmxConfiguration);
        }

        /**
         * Sets the {@code trafficShapingConfiguration} and returns a reference to this Builder enabling method
         * chaining.
         *
         * @param trafficShapingConfiguration the {@code trafficShapingConfiguration} to set
         * @return a reference to this Builder
         */
        public Builder trafficShapingConfiguration(TrafficShapingConfiguration trafficShapingConfiguration)
        {
            return update(b -> b.trafficShapingConfiguration = trafficShapingConfiguration);
        }

        /**
         * Sets the {@code schemaKeyspaceConfiguration} and returns a reference to this Builder enabling method
         * chaining.
         *
         * @param schemaKeyspaceConfiguration the {@code schemaKeyspaceConfiguration} to set
         * @return a reference to this Builder
         */
        public Builder schemaKeyspaceConfiguration(SchemaKeyspaceConfiguration schemaKeyspaceConfiguration)
        {
            return update(b -> b.schemaKeyspaceConfiguration = schemaKeyspaceConfiguration);
        }

        /**
         * Set the {@code cdcConfiguration} and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param configuration th {@code cdcConfiguration} to set
         * @return a reference to the Builder
         */
        public Builder cdcConfiguration(CdcConfiguration configuration)
        {
            return update(b -> b.cdcConfiguration = configuration);
        }

        /**
         * Returns a {@code ServiceConfigurationImpl} built from the parameters previously set.
         *
         * @return a {@code ServiceConfigurationImpl} built with parameters of this
         * {@code ServiceConfigurationImpl.Builder}
         */
        @Override
        public ServiceConfigurationImpl build()
        {
            return new ServiceConfigurationImpl(this);
        }
    }
}
