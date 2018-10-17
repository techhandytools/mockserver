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
public class ServerConfig {

	private int port;
	private String logging;
	private int jettyContainerThreads;
	private int jettyAcceptors;
	private int jettyAcceptQueueSize;
	private int jettyHeaderBufferSize;
	private int maxRequestJournalEntries;

}
