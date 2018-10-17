package com.mockserver.config;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@Getter
@Setter
@NoArgsConstructor
@ToString
public class MockConfig {

	private ServerConfig server;
	private MongoDBConfig db;
	private ZkConfig cluster;

}
