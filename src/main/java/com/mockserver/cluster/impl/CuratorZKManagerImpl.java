package com.mockserver.cluster.impl;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mockserver.config.ZkConfig;

public class CuratorZKManagerImpl {

	private static final Logger logger = LoggerFactory.getLogger(CuratorZKManagerImpl.class);

	/**
	 * To create a client for zoo keeper
	 *
	 * @param zkConfig
	 * @return
	 */
	public CuratorFramework createClient(final ZkConfig zkConfig) {
		logger.info("Creating Curator client with the config :: " + zkConfig);
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(zkConfig.getBaseSleepTimeMills(), zkConfig.getMaxRetires());
		logger.info("Retry Policy initialized and creating a new client");
		return CuratorFrameworkFactory.newClient(zkConfig.getHost(), zkConfig.getSessionTimeout(), zkConfig.getConnectionTimeout(), retryPolicy);
	}

	/**
	 * To create and start the zoo keeper client
	 *
	 * @param zkConfig
	 * @return
	 */
	public CuratorFramework createAndStartClient(final ZkConfig zkConfig) {
		CuratorFramework client = createClient(zkConfig);
		logger.info("Starting the Curator client with the config :: " + zkConfig);
		client.start();
		logger.info("Started the Curator client");
		return client;
	}
}
