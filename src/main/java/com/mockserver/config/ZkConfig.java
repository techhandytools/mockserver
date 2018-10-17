package com.mockserver.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ZkConfig {

	private String host;
	private int baseSleepTimeMills;
	private int maxRetires;
	private int waitTimeSeconds;
	private int sessionTimeout;
	private int connectionTimeout;


}
