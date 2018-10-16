package com.mockserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.mockserver.cluster.impl.CuratorZKManagerImpl;
import com.mockserver.config.MockConfig;
import com.mockserver.db.MongoDAO;
import com.mockserver.util.MongoDBMappingsSource;
import com.mockserver.util.StubMappingsUtil;

public class MockServer {

	static final Logger logger = LoggerFactory.getLogger(MockServer.class);

	public static void main(String[] args) {
		new MockServer().run(args[0]);
	}

	/**
	 * To start the mock server based on the configuration
	 *
	 * @param configFile
	 */
	public void run(String configFile) {

		logger.info("Starting the mock server application");

		//Loading the config for the application
		MockConfig mockConfig = loadConfig(configFile);

		//Initializing the mongo db
		MongoDAO mongoDAO = new MongoDAO(mockConfig.getDb());

		//Starting the ZooKeeper
		SharedCount sharedCount = startZooKeeper(mockConfig);

		//Starting the Wire mock server
		WireMockServer wireMockServer = startServer(mockConfig, mongoDAO, sharedCount);
		Runtime.getRuntime().addShutdownHook(new Thread(wireMockServer::shutdown));

		initStubsMapping(wireMockServer, mongoDAO, sharedCount);
		wireMockServer.start();
		logger.info("Server started");
	}

	/**
	 * To load the configuration of the application
	 *
	 * @param configFile
	 * @return
	 */
	private MockConfig loadConfig(final String configFile) {
		MockConfig config = null;

		try (InputStream inputStream = Files.newInputStream(Paths.get(configFile))) {
			logger.info("Loading the configuration....");
			config = new Yaml().loadAs(inputStream, MockConfig.class);
		} catch (IOException error) {
			logger.error("The exception occurred in run method is :: " + error.getMessage());
			error.printStackTrace();
		}
		return config;
	}

	/**
	 * To start the zoo keeper based on the application config
	 *
	 * @param config
	 * @return
	 */
	private SharedCount startZooKeeper(final MockConfig config) {
		logger.info("Starting the zookeeper.....");
		CuratorFramework zClient = new CuratorZKManagerImpl().createAndStartClient(config.getCluster());
		SharedCount sharedCount = new SharedCount(zClient, "/wiremock/sync" + config.getDb().getCollection(), 0);
		try {
			sharedCount.start();
			logger.info("Connected to zookeeper");
		} catch (Exception error) {
			logger.error("Failed to connect to zookeeper", error.getMessage());
			error.printStackTrace();
		}
		return sharedCount;
	}

	/**
	 * To start the wire mock server using the configuration of the application,
	 * mongo and zoo keeper
	 *
	 * @param config
	 * @param mongoDAO
	 * @param sharedCount
	 * @return
	 */
	private WireMockServer startServer(final MockConfig config, final MongoDAO mongoDAO, final SharedCount sharedCount) {
		WireMockConfiguration.wireMockConfig();
		return new WireMockServer(
				WireMockConfiguration
						.options()
						.notifier(new Slf4jNotifier(false))
						.port(config.getServer().getPort())
						.extensions(new ResponseTemplateTransformer(true))
						.containerThreads(config.getServer().getJettyContainerThreads())
						.jettyAcceptors(config.getServer().getJettyAcceptors())
						.jettyAcceptQueueSize(config.getServer().getJettyAcceptQueueSize())
						.jettyHeaderBufferSize(config.getServer().getJettyHeaderBufferSize())
						.disableRequestJournal()
						.mappingSource(new MongoDBMappingsSource(mongoDAO, sharedCount))
						.usingFilesUnderDirectory("./ui/")
		);
	}

	/**
	 * To initialize the health check stub and load the stub mapping util
	 *
	 * @param wireMockServer
	 * @param mongoDAO
	 * @param sharedCount
	 */
	private void initStubsMapping(final WireMockServer wireMockServer, final MongoDAO mongoDAO, final SharedCount sharedCount) {
		// Don't remove /heath_check stub as it will lead to deployment failure.
		wireMockServer.addStubMapping(
				WireMock.request(
						String.valueOf(RequestMethod.ANY),
						WireMock.urlMatching("/health_check")
				).willReturn(WireMock.aResponse()
						.withStatus(200)
						.withBody(""))
						.build()
		);

		new StubMappingsUtil(wireMockServer, mongoDAO, sharedCount);
	}

}
