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

package org.apache.cassandra.sidecar.server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.SidecarRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.NettyOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.Match;
import io.vertx.ext.dropwizard.MatchType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ChainAuthHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.apache.cassandra.sidecar.acl.authentication.AuthenticationHandlerFactory;
import org.apache.cassandra.sidecar.acl.authentication.AuthenticationHandlerFactoryRegistry;
import org.apache.cassandra.sidecar.acl.authentication.MutualTlsAuthenticationHandlerFactory;
import org.apache.cassandra.sidecar.adapters.base.CassandraFactory;
import org.apache.cassandra.sidecar.adapters.cassandra41.Cassandra41Factory;
import org.apache.cassandra.sidecar.cluster.CQLSessionProviderImpl;
import org.apache.cassandra.sidecar.cluster.CassandraAdapterDelegate;
import org.apache.cassandra.sidecar.cluster.InstancesConfig;
import org.apache.cassandra.sidecar.cluster.InstancesConfigImpl;
import org.apache.cassandra.sidecar.cluster.instance.InstanceMetadata;
import org.apache.cassandra.sidecar.cluster.instance.InstanceMetadataImpl;
import org.apache.cassandra.sidecar.cluster.locator.CachedLocalTokenRanges;
import org.apache.cassandra.sidecar.cluster.locator.LocalTokenRangesProvider;
import org.apache.cassandra.sidecar.common.ApiEndpointsV1;
import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.JmxClient;
import org.apache.cassandra.sidecar.common.server.dns.DnsResolver;
import org.apache.cassandra.sidecar.common.server.utils.DriverUtils;
import org.apache.cassandra.sidecar.common.server.utils.SidecarVersionProvider;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.config.AccessControlConfiguration;
import org.apache.cassandra.sidecar.config.CassandraInputValidationConfiguration;
import org.apache.cassandra.sidecar.config.FileSystemOptionsConfiguration;
import org.apache.cassandra.sidecar.config.InstanceConfiguration;
import org.apache.cassandra.sidecar.config.JmxConfiguration;
import org.apache.cassandra.sidecar.config.ParameterizedClassConfiguration;
import org.apache.cassandra.sidecar.config.ServiceConfiguration;
import org.apache.cassandra.sidecar.config.SidecarConfiguration;
import org.apache.cassandra.sidecar.config.VertxConfiguration;
import org.apache.cassandra.sidecar.config.VertxMetricsConfiguration;
import org.apache.cassandra.sidecar.config.yaml.SidecarConfigurationImpl;
import org.apache.cassandra.sidecar.db.schema.RestoreJobsSchema;
import org.apache.cassandra.sidecar.db.schema.RestoreRangesSchema;
import org.apache.cassandra.sidecar.db.schema.RestoreSlicesSchema;
import org.apache.cassandra.sidecar.db.schema.SidecarInternalKeyspace;
import org.apache.cassandra.sidecar.db.schema.SidecarSchema;
import org.apache.cassandra.sidecar.db.schema.SystemAuthSchema;
import org.apache.cassandra.sidecar.exceptions.ConfigurationException;
import org.apache.cassandra.sidecar.logging.SidecarLoggerHandler;
import org.apache.cassandra.sidecar.metrics.MetricRegistryFactory;
import org.apache.cassandra.sidecar.metrics.SchemaMetrics;
import org.apache.cassandra.sidecar.metrics.SidecarMetrics;
import org.apache.cassandra.sidecar.metrics.SidecarMetricsImpl;
import org.apache.cassandra.sidecar.metrics.instance.InstanceHealthMetrics;
import org.apache.cassandra.sidecar.routes.CassandraHealthHandler;
import org.apache.cassandra.sidecar.routes.ConnectedClientStatsHandler;
import org.apache.cassandra.sidecar.routes.DiskSpaceProtectionHandler;
import org.apache.cassandra.sidecar.routes.FileStreamHandler;
import org.apache.cassandra.sidecar.routes.GossipInfoHandler;
import org.apache.cassandra.sidecar.routes.JsonErrorHandler;
import org.apache.cassandra.sidecar.routes.RingHandler;
import org.apache.cassandra.sidecar.routes.RoutingOrder;
import org.apache.cassandra.sidecar.routes.SchemaHandler;
import org.apache.cassandra.sidecar.routes.StreamSSTableComponentHandler;
import org.apache.cassandra.sidecar.routes.TimeSkewHandler;
import org.apache.cassandra.sidecar.routes.TokenRangeReplicaMapHandler;
import org.apache.cassandra.sidecar.routes.cassandra.NodeSettingsHandler;
import org.apache.cassandra.sidecar.routes.cdc.ListCdcDirHandler;
import org.apache.cassandra.sidecar.routes.cdc.StreamCdcSegmentHandler;
import org.apache.cassandra.sidecar.routes.restore.AbortRestoreJobHandler;
import org.apache.cassandra.sidecar.routes.restore.CreateRestoreJobHandler;
import org.apache.cassandra.sidecar.routes.restore.CreateRestoreSliceHandler;
import org.apache.cassandra.sidecar.routes.restore.RestoreJobProgressHandler;
import org.apache.cassandra.sidecar.routes.restore.RestoreJobSummaryHandler;
import org.apache.cassandra.sidecar.routes.restore.RestoreRequestValidationHandler;
import org.apache.cassandra.sidecar.routes.restore.UpdateRestoreJobHandler;
import org.apache.cassandra.sidecar.routes.snapshots.ClearSnapshotHandler;
import org.apache.cassandra.sidecar.routes.snapshots.CreateSnapshotHandler;
import org.apache.cassandra.sidecar.routes.snapshots.ListSnapshotHandler;
import org.apache.cassandra.sidecar.routes.sstableuploads.SSTableCleanupHandler;
import org.apache.cassandra.sidecar.routes.sstableuploads.SSTableImportHandler;
import org.apache.cassandra.sidecar.routes.sstableuploads.SSTableUploadHandler;
import org.apache.cassandra.sidecar.routes.validations.ValidateTableExistenceHandler;
import org.apache.cassandra.sidecar.utils.CassandraVersionProvider;
import org.apache.cassandra.sidecar.utils.DigestAlgorithmProvider;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.apache.cassandra.sidecar.utils.JdkMd5DigestProvider;
import org.apache.cassandra.sidecar.utils.TimeProvider;
import org.apache.cassandra.sidecar.utils.XXHash32Provider;

import static org.apache.cassandra.sidecar.common.ApiEndpointsV1.API_V1_ALL_ROUTES;
import static org.apache.cassandra.sidecar.common.server.utils.ByteUtils.bytesToHumanReadableBinaryPrefix;
import static org.apache.cassandra.sidecar.server.SidecarServerEvents.ON_SERVER_STOP;

/**
 * Provides main binding for more complex Guice dependencies
 */
public class MainModule extends AbstractModule
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MainModule.class);
    public static final Map<String, String> OK_STATUS = Collections.singletonMap("status", "OK");
    public static final Map<String, String> NOT_OK_STATUS = Collections.singletonMap("status", "NOT_OK");

    protected final Path confPath;

    /**
     * Constructs the Guice main module to run Cassandra Sidecar
     */
    public MainModule()
    {
        confPath = null;
    }

    /**
     * Constructs the Guice main module with the configured yaml {@code confPath} to run Cassandra Sidecar
     *
     * @param confPath the path to the yaml configuration file
     */
    public MainModule(Path confPath)
    {
        this.confPath = confPath;
    }

    @Provides
    @Singleton
    public Vertx vertx(SidecarConfiguration sidecarConfiguration, MetricRegistryFactory metricRegistryFactory)
    {
        VertxMetricsConfiguration metricsConfig = sidecarConfiguration.metricsConfiguration().vertxConfiguration();
        Match serverRouteMatch = new Match().setValue(API_V1_ALL_ROUTES).setType(MatchType.REGEX);
        DropwizardMetricsOptions dropwizardMetricsOptions
        = new DropwizardMetricsOptions().setEnabled(metricsConfig.enabled())
                                        .setJmxEnabled(metricsConfig.exposeViaJMX())
                                        .setJmxDomain(metricsConfig.jmxDomainName())
                                        .setMetricRegistry(metricRegistryFactory.getOrCreate())
                                        // Monitor all V1 endpoints.
                                        // Additional filtering is done by configuring yaml fields 'metrics.include|exclude'
                                        .addMonitoredHttpServerRoute(serverRouteMatch);

        VertxOptions vertxOptions = new VertxOptions().setMetricsOptions(dropwizardMetricsOptions);
        VertxConfiguration vertxConfiguration = sidecarConfiguration.vertxConfiguration();
        FileSystemOptionsConfiguration fsOptions = vertxConfiguration != null ? vertxConfiguration.filesystemOptionsConfiguration() : null;

        if (fsOptions != null)
        {
            vertxOptions.setFileSystemOptions(new FileSystemOptions()
                                              .setClassPathResolvingEnabled(fsOptions.classpathResolvingEnabled())
                                              .setFileCacheDir(fsOptions.fileCacheDir())
                                              .setFileCachingEnabled(fsOptions.fileCachingEnabled()));
        }

        return Vertx.vertx(vertxOptions);
    }

    @Provides
    @Singleton
    public AuthenticationHandlerFactoryRegistry authNHandlerFactoryRegistry(MutualTlsAuthenticationHandlerFactory mTLSAuthHandlerFactory)
    {
        AuthenticationHandlerFactoryRegistry registry = new AuthenticationHandlerFactoryRegistry();
        registry.register(mTLSAuthHandlerFactory);
        return registry;
    }

    @Provides
    @Singleton
    public ChainAuthHandler chainAuthHandler(Vertx vertx,
                                             SidecarConfiguration sidecarConfiguration,
                                             AuthenticationHandlerFactoryRegistry registry) throws ConfigurationException
    {
        AccessControlConfiguration accessControlConfiguration = sidecarConfiguration.accessControlConfiguration();
        List<ParameterizedClassConfiguration> authList = accessControlConfiguration.authenticatorsConfiguration();
        if (!accessControlConfiguration.enabled())
        {
            return ChainAuthHandler.any();
        }

        if (authList == null || authList.isEmpty())
        {
            LOGGER.error("Access control was enabled, but there are no configured authenticators");
            throw new ConfigurationException("Invalid access control configuration. There are no configured authenticators");
        }

        ChainAuthHandler chainAuthHandler = ChainAuthHandler.any();
        for (ParameterizedClassConfiguration config : authList)
        {
            AuthenticationHandlerFactory factory = registry.getFactory(config.className());

            if (factory == null)
            {
                throw new RuntimeException(String.format("Implementation for class %s has not been registered",
                                                         config.className()));
            }
            chainAuthHandler.add(factory.create(vertx, accessControlConfiguration, config.namedParameters()));
        }
        return chainAuthHandler;
    }

    @Provides
    @Singleton
    public Router vertxRouter(Vertx vertx,
                              SidecarConfiguration sidecarConfiguration,
                              ChainAuthHandler chainAuthHandler,
                              ServiceConfiguration conf,
                              CassandraHealthHandler cassandraHealthHandler,
                              StreamSSTableComponentHandler streamSSTableComponentHandler,
                              FileStreamHandler fileStreamHandler,
                              ClearSnapshotHandler clearSnapshotHandler,
                              CreateSnapshotHandler createSnapshotHandler,
                              ListSnapshotHandler listSnapshotHandler,
                              SchemaHandler schemaHandler,
                              RingHandler ringHandler,
                              TokenRangeReplicaMapHandler tokenRangeHandler,
                              LoggerHandler loggerHandler,
                              GossipInfoHandler gossipInfoHandler,
                              TimeSkewHandler timeSkewHandler,
                              NodeSettingsHandler nodeSettingsHandler,
                              SSTableUploadHandler ssTableUploadHandler,
                              SSTableImportHandler ssTableImportHandler,
                              SSTableCleanupHandler ssTableCleanupHandler,
                              StreamCdcSegmentHandler streamCdcSegmentHandler,
                              ListCdcDirHandler listCdcDirHandler,
                              RestoreRequestValidationHandler validateRestoreJobRequest,
                              DiskSpaceProtectionHandler diskSpaceProtection,
                              ValidateTableExistenceHandler validateTableExistence,
                              CreateRestoreJobHandler createRestoreJobHandler,
                              RestoreJobSummaryHandler restoreJobSummaryHandler,
                              UpdateRestoreJobHandler updateRestoreJobHandler,
                              AbortRestoreJobHandler abortRestoreJobHandler,
                              CreateRestoreSliceHandler createRestoreSliceHandler,
                              RestoreJobProgressHandler restoreJobProgressHandler,
                              ConnectedClientStatsHandler connectedClientStatsHandler,
                              ErrorHandler errorHandler)
    {
        Router router = Router.router(vertx);
        router.route()
              .order(RoutingOrder.HIGHEST.order)
              .handler(loggerHandler)
              .handler(TimeoutHandler.create(conf.requestTimeoutMillis(),
                                             HttpResponseStatus.REQUEST_TIMEOUT.code()));

        // chain authentication before all requests
        if (sidecarConfiguration.accessControlConfiguration().enabled())
        {
            router.route().order(RoutingOrder.HIGHEST.order).handler(chainAuthHandler);
        }

        router.route()
              .path(ApiEndpointsV1.API + "/*")
              .order(RoutingOrder.HIGHEST.order)
              .failureHandler(errorHandler);

        // Docs index.html page
        StaticHandler docs = StaticHandler.create("docs");
        router.route()
              .path("/docs/*")
              .handler(docs);

        // Add custom routers
        // Provides a simple REST endpoint to determine if Sidecar is available
        router.get(ApiEndpointsV1.HEALTH_ROUTE)
              .handler(context -> context.json(OK_STATUS));

        // Backwards compatibility for the Cassandra health endpoint
        //noinspection deprecation
        router.get(ApiEndpointsV1.CASSANDRA_HEALTH_ROUTE)
              .handler(cassandraHealthHandler);

        router.get(ApiEndpointsV1.CASSANDRA_NATIVE_HEALTH_ROUTE)
              .handler(cassandraHealthHandler);

        router.get(ApiEndpointsV1.CASSANDRA_JMX_HEALTH_ROUTE)
              .handler(cassandraHealthHandler);

        //noinspection deprecation
        router.get(ApiEndpointsV1.DEPRECATED_COMPONENTS_ROUTE)
              .handler(streamSSTableComponentHandler)
              .handler(fileStreamHandler);

        router.get(ApiEndpointsV1.COMPONENTS_ROUTE)
              .handler(streamSSTableComponentHandler)
              .handler(fileStreamHandler);

        // Support for routes that want to stream SStable index components
        router.get(ApiEndpointsV1.COMPONENTS_WITH_SECONDARY_INDEX_ROUTE_SUPPORT)
              .handler(streamSSTableComponentHandler)
              .handler(fileStreamHandler);

        //noinspection deprecation
        router.get(ApiEndpointsV1.DEPRECATED_SNAPSHOTS_ROUTE)
              .handler(listSnapshotHandler);

        router.get(ApiEndpointsV1.SNAPSHOTS_ROUTE)
              .handler(listSnapshotHandler);

        router.delete(ApiEndpointsV1.SNAPSHOTS_ROUTE)
              // Leverage the validateTableExistence. Currently, JMX does not validate for non-existent keyspace.
              // Additionally, the current JMX implementation to clear snapshots does not support passing a table
              // as a parameter.
              .handler(validateTableExistence)
              .handler(clearSnapshotHandler);

        router.put(ApiEndpointsV1.SNAPSHOTS_ROUTE)
              .handler(createSnapshotHandler);

        //noinspection deprecation
        router.get(ApiEndpointsV1.DEPRECATED_ALL_KEYSPACES_SCHEMA_ROUTE)
              .handler(schemaHandler);

        router.get(ApiEndpointsV1.ALL_KEYSPACES_SCHEMA_ROUTE)
              .handler(schemaHandler);

        //noinspection deprecation
        router.get(ApiEndpointsV1.DEPRECATED_KEYSPACE_SCHEMA_ROUTE)
              .handler(schemaHandler);

        router.get(ApiEndpointsV1.KEYSPACE_SCHEMA_ROUTE)
              .handler(schemaHandler);

        router.get(ApiEndpointsV1.RING_ROUTE)
              .handler(ringHandler);

        router.get(ApiEndpointsV1.CONNECTED_CLIENT_STATS_ROUTE)
              .handler(connectedClientStatsHandler);

        router.get(ApiEndpointsV1.RING_ROUTE_PER_KEYSPACE)
              .handler(ringHandler);

        router.put(ApiEndpointsV1.SSTABLE_UPLOAD_ROUTE)
              .handler(ssTableUploadHandler);

        router.get(ApiEndpointsV1.KEYSPACE_TOKEN_MAPPING_ROUTE)
              .handler(tokenRangeHandler);

        router.put(ApiEndpointsV1.SSTABLE_IMPORT_ROUTE)
              .handler(ssTableImportHandler);

        router.delete(ApiEndpointsV1.SSTABLE_CLEANUP_ROUTE)
              .handler(ssTableCleanupHandler);

        router.get(ApiEndpointsV1.GOSSIP_INFO_ROUTE)
              .handler(gossipInfoHandler);

        router.get(ApiEndpointsV1.TIME_SKEW_ROUTE)
              .handler(timeSkewHandler);

        router.get(ApiEndpointsV1.NODE_SETTINGS_ROUTE)
              .handler(nodeSettingsHandler);

        router.post(ApiEndpointsV1.CREATE_RESTORE_JOB_ROUTE)
              .handler(BodyHandler.create())
              .handler(validateTableExistence)
              .handler(validateRestoreJobRequest)
              .handler(createRestoreJobHandler);

        router.post(ApiEndpointsV1.RESTORE_JOB_SLICES_ROUTE)
              .handler(BodyHandler.create())
              .handler(diskSpaceProtection) // reject creating slice if short of disk space
              .handler(validateTableExistence)
              .handler(validateRestoreJobRequest)
              .handler(createRestoreSliceHandler);

        router.get(ApiEndpointsV1.RESTORE_JOB_ROUTE)
              .handler(validateTableExistence)
              .handler(validateRestoreJobRequest)
              .handler(restoreJobSummaryHandler);

        router.patch(ApiEndpointsV1.RESTORE_JOB_ROUTE)
              .handler(BodyHandler.create())
              .handler(validateTableExistence)
              .handler(validateRestoreJobRequest)
              .handler(updateRestoreJobHandler);

        router.post(ApiEndpointsV1.ABORT_RESTORE_JOB_ROUTE)
              .handler(BodyHandler.create())
              .handler(validateTableExistence)
              .handler(validateRestoreJobRequest)
              .handler(abortRestoreJobHandler);

        router.get(ApiEndpointsV1.RESTORE_JOB_PROGRESS_ROUTE)
              .handler(validateTableExistence)
              .handler(validateRestoreJobRequest)
              .handler(restoreJobProgressHandler);

        // CDC APIs
        router.get(ApiEndpointsV1.LIST_CDC_SEGMENTS_ROUTE)
              .handler(listCdcDirHandler);
        router.get(ApiEndpointsV1.STREAM_CDC_SEGMENTS_ROUTE)
              .handler(streamCdcSegmentHandler);

        return router;
    }

    @Provides
    @Singleton
    public SidecarConfiguration sidecarConfiguration() throws IOException
    {
        if (confPath == null)
        {
            throw new NullPointerException("the YAML configuration path for Sidecar has not been defined.");
        }
        return SidecarConfigurationImpl.readYamlConfiguration(confPath);
    }

    @Provides
    @Singleton
    public ServiceConfiguration serviceConfiguration(SidecarConfiguration sidecarConfiguration)
    {
        return sidecarConfiguration.serviceConfiguration();
    }

    @Provides
    @Singleton
    public CassandraInputValidationConfiguration validationConfiguration(SidecarConfiguration configuration)
    {
        return configuration.cassandraInputValidationConfiguration();
    }

    @Provides
    @Singleton
    public CQLSessionProvider cqlSessionProvider(Vertx vertx, SidecarConfiguration sidecarConfiguration,
                                                 DriverUtils driverUtils)
    {
        CQLSessionProviderImpl cqlSessionProvider = new CQLSessionProviderImpl(sidecarConfiguration,
                                                                               NettyOptions.DEFAULT_INSTANCE,
                                                                               driverUtils);
        vertx.eventBus().localConsumer(ON_SERVER_STOP.address(), message -> cqlSessionProvider.close());
        return cqlSessionProvider;
    }

    @Provides
    @Singleton
    public DriverUtils driverUtils()
    {
        return new DriverUtils();
    }

    @Provides
    @Singleton
    public InstancesConfig instancesConfig(Vertx vertx,
                                           SidecarConfiguration configuration,
                                           CassandraVersionProvider cassandraVersionProvider,
                                           SidecarVersionProvider sidecarVersionProvider,
                                           DnsResolver dnsResolver,
                                           CQLSessionProvider cqlSessionProvider,
                                           DriverUtils driverUtils,
                                           MetricRegistryFactory registryProvider)
    {
        List<InstanceMetadata> instanceMetadataList =
        configuration.cassandraInstances()
                     .stream()
                     .map(cassandraInstance -> {
                         JmxConfiguration jmxConfiguration = configuration.serviceConfiguration().jmxConfiguration();
                         return buildInstanceMetadata(vertx,
                                                      cassandraInstance,
                                                      cassandraVersionProvider,
                                                      sidecarVersionProvider.sidecarVersion(),
                                                      jmxConfiguration,
                                                      cqlSessionProvider,
                                                      driverUtils,
                                                      registryProvider);
                     })
                     .collect(Collectors.toList());

        return new InstancesConfigImpl(instanceMetadataList, dnsResolver);
    }

    @Provides
    @Singleton
    public CassandraVersionProvider cassandraVersionProvider(DnsResolver dnsResolver, DriverUtils driverUtils)
    {
        return new CassandraVersionProvider.Builder()
               .add(new CassandraFactory(dnsResolver, driverUtils))
               .add(new Cassandra41Factory(dnsResolver, driverUtils))
               .build();
    }

    @Provides
    @Singleton
    @Named("StreamRequestRateLimiter")
    public SidecarRateLimiter streamRequestRateLimiter(ServiceConfiguration config)
    {
        long permitsPerSecond = config.throttleConfiguration().rateLimitStreamRequestsPerSecond();
        LOGGER.info("Configuring streamRequestRateLimiter. rateLimitStreamRequestsPerSecond={}",
                    permitsPerSecond);
        return SidecarRateLimiter.create(permitsPerSecond);
    }

    @Provides
    @Singleton
    @Named("IngressFileRateLimiter")
    public SidecarRateLimiter ingressFileRateLimiter(ServiceConfiguration config)
    {
        long bytesPerSecond = config.trafficShapingConfiguration()
                                    .inboundGlobalFileBandwidthBytesPerSecond();
        LOGGER.info("Configuring ingressFileRateLimiter. inboundGlobalFileBandwidth={}/s " +
                    "rawInboundGlobalFileBandwidth={} B/s", bytesToHumanReadableBinaryPrefix(bytesPerSecond),
                    bytesPerSecond);
        return SidecarRateLimiter.create(bytesPerSecond);
    }

    @Provides
    @Singleton
    public LoggerHandler loggerHandler()
    {
        return SidecarLoggerHandler.create(LoggerHandler.create());
    }

    @Provides
    @Singleton
    public TimeProvider timeProvider()
    {
        return TimeProvider.DEFAULT_TIME_PROVIDER;
    }

    @Provides
    @Singleton
    public ErrorHandler errorHandler()
    {
        return new JsonErrorHandler();
    }

    @Provides
    @Singleton
    public DnsResolver dnsResolver()
    {
        return DnsResolver.DEFAULT;
    }

    @Provides
    @Singleton
    public SidecarVersionProvider sidecarVersionProvider()
    {
        return new SidecarVersionProvider("/sidecar.version");
    }

    @Provides
    @Singleton
    public RestoreJobsSchema restoreJobsSchema(SidecarConfiguration configuration)
    {
        return new RestoreJobsSchema(configuration.serviceConfiguration()
                                                  .schemaKeyspaceConfiguration(),
                                     configuration.restoreJobConfiguration()
                                                  .restoreJobTablesTtlSeconds());
    }

    @Provides
    @Singleton
    public RestoreSlicesSchema restoreSlicesSchema(SidecarConfiguration configuration)
    {
        return new RestoreSlicesSchema(configuration.serviceConfiguration()
                                                    .schemaKeyspaceConfiguration(),
                                       configuration.restoreJobConfiguration()
                                                    .restoreJobTablesTtlSeconds());
    }

    @Provides
    @Singleton
    public RestoreRangesSchema restoreJobProgressSchema(SidecarConfiguration configuration)
    {
        return new RestoreRangesSchema(configuration.serviceConfiguration()
                                                    .schemaKeyspaceConfiguration(),
                                       configuration.restoreJobConfiguration()
                                                    .restoreJobTablesTtlSeconds());
    }

    @Provides
    @Singleton
    public SidecarSchema sidecarSchema(Vertx vertx,
                                       ExecutorPools executorPools,
                                       SidecarConfiguration configuration,
                                       CQLSessionProvider cqlSessionProvider,
                                       RestoreJobsSchema restoreJobsSchema,
                                       RestoreSlicesSchema restoreSlicesSchema,
                                       RestoreRangesSchema restoreRangesSchema,
                                       SystemAuthSchema systemAuthSchema,
                                       SidecarMetrics metrics)
    {
        SidecarInternalKeyspace sidecarInternalKeyspace = new SidecarInternalKeyspace(configuration);
        // register table schema when enabled
        sidecarInternalKeyspace.registerTableSchema(restoreJobsSchema);
        sidecarInternalKeyspace.registerTableSchema(restoreSlicesSchema);
        sidecarInternalKeyspace.registerTableSchema(restoreRangesSchema);
        sidecarInternalKeyspace.registerTableSchema(systemAuthSchema);
        SchemaMetrics schemaMetrics = metrics.server().schema();
        return new SidecarSchema(vertx, executorPools, configuration,
                                 sidecarInternalKeyspace, cqlSessionProvider, schemaMetrics);
    }

    @Provides
    @Singleton
    public SidecarMetrics metrics(MetricRegistryFactory registryFactory, InstanceMetadataFetcher metadataFetcher)
    {
        return new SidecarMetricsImpl(registryFactory, metadataFetcher);
    }

    /**
     * The provided hasher is used in {@link org.apache.cassandra.sidecar.restore.RestoreJobUtil}
     */
    @Provides
    @Singleton
    @Named("xxhash32")
    public DigestAlgorithmProvider xxHash32Provider()
    {
        return new XXHash32Provider();
    }

    @Provides
    @Singleton
    @Named("md5")
    public DigestAlgorithmProvider md5Provider()
    {
        return new JdkMd5DigestProvider();
    }

    @Provides
    @Singleton
    public LocalTokenRangesProvider localTokenRangesProvider(InstancesConfig instancesConfig, DnsResolver dnsResolver)
    {
        return new CachedLocalTokenRanges(instancesConfig, dnsResolver);
    }

    /**
     * Builds the {@link InstanceMetadata} from the {@link InstanceConfiguration},
     * a provided {@code  versionProvider}, and {@code healthCheckFrequencyMillis}.
     *
     * @param vertx             the vertx instance
     * @param cassandraInstance the cassandra instance configuration
     * @param versionProvider   a Cassandra version provider
     * @param sidecarVersion    the version of the Sidecar from the current binary
     * @param jmxConfiguration  the configuration for the JMX Client
     * @param session           the CQL Session provider
     * @param registryFactory   factory for creating cassandra instance specific registry
     * @return the build instance metadata object
     */
    private static InstanceMetadata buildInstanceMetadata(Vertx vertx,
                                                          InstanceConfiguration cassandraInstance,
                                                          CassandraVersionProvider versionProvider,
                                                          String sidecarVersion,
                                                          JmxConfiguration jmxConfiguration,
                                                          CQLSessionProvider session,
                                                          DriverUtils driverUtils,
                                                          MetricRegistryFactory registryFactory)
    {
        String host = cassandraInstance.host();
        int port = cassandraInstance.port();

        JmxClient jmxClient = JmxClient.builder()
                                       .host(cassandraInstance.jmxHost())
                                       .port(cassandraInstance.jmxPort())
                                       .role(cassandraInstance.jmxRole())
                                       .password(cassandraInstance.jmxRolePassword())
                                       .enableSsl(cassandraInstance.jmxSslEnabled())
                                       .connectionMaxRetries(jmxConfiguration.maxRetries())
                                       .connectionRetryDelayMillis(jmxConfiguration.retryDelayMillis())
                                       .build();
        MetricRegistry instanceSpecificRegistry = registryFactory.getOrCreate(cassandraInstance.id());
        CassandraAdapterDelegate delegate = new CassandraAdapterDelegate(vertx,
                                                                         cassandraInstance.id(),
                                                                         versionProvider,
                                                                         session,
                                                                         jmxClient,
                                                                         driverUtils,
                                                                         sidecarVersion,
                                                                         host,
                                                                         port,
                                                                         new InstanceHealthMetrics(instanceSpecificRegistry));
        return InstanceMetadataImpl.builder()
                                   .id(cassandraInstance.id())
                                   .host(host)
                                   .port(port)
                                   .dataDirs(cassandraInstance.dataDirs())
                                   .stagingDir(cassandraInstance.stagingDir())
                                   .cdcDir(cassandraInstance.cdcDir())
                                   .delegate(delegate)
                                   .metricRegistry(instanceSpecificRegistry)
                                   .build();
    }
}
