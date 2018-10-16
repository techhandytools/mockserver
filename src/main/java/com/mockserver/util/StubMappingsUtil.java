package com.mockserver.util;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.curator.framework.recipes.shared.SharedCountListener;
import org.apache.curator.framework.recipes.shared.SharedCountReader;
import org.apache.curator.framework.state.ConnectionState;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.mockserver.db.MongoDAO;

/**
 * This class provides the utility for Stub mappings
 */
public class StubMappingsUtil {

	static final Logger logger = LoggerFactory.getLogger(StubMappingsUtil.class);

	private WireMockServer wireMockServer;
	private MongoDAO mongoDAO;

	/**
	 * Initializing the configuration for the stub mapping utils
	 *
	 * @param wireMockServer
	 * @param mongoDAO
	 * @param sharedCount
	 */
	public StubMappingsUtil(WireMockServer wireMockServer, MongoDAO mongoDAO, SharedCount sharedCount) {

		this.wireMockServer = wireMockServer;
		this.mongoDAO = mongoDAO;
		sharedCount.addListener(new SharedCountListener() {

			@Override
			public void countHasChanged(SharedCountReader sharedCountReader, int newCount) throws Exception {

				if (newCount == 1) {
					reloadMapping();
					sharedCount.setCount(0);
				}
			}

			@Override
			public void stateChanged(CuratorFramework client, ConnectionState newState) {

			}
		});

	}

	/**
	 * Resetting the stub mappings
	 */
	private void resetMappings() {
		logger.info("Resetting the stub mappings");
		wireMockServer.resetMappings();
	}

	/**
	 * Reloading the stub mappings
	 */
	private void reloadMapping() {
		logger.info("Reloading the stub mappings");
		resetMappings();
		addHealthCheckMapping();

		wireMockServer.loadMappingsUsing(stubMappings -> {
			List<Document> readDoc = mongoDAO.readAll();

			for (Document doc : readDoc) {
				StubMapping mapping = StubMapping.buildFrom(doc.toJson());
				mapping.setDirty(false);
				stubMappings.addMapping(mapping);
			}
		});
	}

	/**
	 * Adding health check in the stub mappings
	 */
	private void addHealthCheckMapping() {
		logger.info("Adding health check in the stub mappings");
		wireMockServer.addStubMapping(
				WireMock.request(
						String.valueOf(RequestMethod.ANY),
						WireMock.urlMatching("/health_check")
				).willReturn(WireMock.aResponse()
						.withStatus(200)
						.withBody(""))
						.build()
		);
	}
}
