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
public class MongoDBConfig {

	private String host;
	private int port;
	private String database;
	private String username;
	private String password;
	private String collection;

}
