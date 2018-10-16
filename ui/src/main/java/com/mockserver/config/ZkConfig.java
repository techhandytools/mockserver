package com.mockserver.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ZkConfig {

	private String host;
	private int baseSleepTimeMills;
	private int maxRetires;
	private int waitTimeSeconds;
	private int sessionTimeout;
	private int connectionTimeout;


}
