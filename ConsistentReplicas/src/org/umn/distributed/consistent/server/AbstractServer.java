package org.umn.distributed.consistent.server;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.log4j.Logger;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Utils;

public abstract class AbstractServer implements TcpServerDelegate {
	protected Logger logger = Logger.getLogger(this.getClass());

	public enum STRATEGY {
		SEQUENTIAL, QUORUM
	}

	protected static final String WRITE_COMMAND = "WRITE";
	protected static final String READ_COMMAND = "READ";
	protected static final String READITEM_COMMAND = "RDITEM";
	protected static final String REGISTER_COMMAND = "REGISTER";
	protected static final String INVALID_COMMAND = "INVCOM";

	protected static final String HEARTBEAT_COMMAND = "PING";
	protected static final String COMMAND_SUCCESS = "SUCCESS";
	protected static final String COMMAND_FAILED = "FAILED";

	protected static final String COMMAND_PARAM_SEPARATOR = "-";
	private TCPServer tcpServer;
	protected int port;
	protected Machine myInfo;
	protected STRATEGY strategy;
	// TODO think if we can change this to list sorted in increasing order
	protected ConcurrentSkipListMap<Integer, Machine> knownClients = new ConcurrentSkipListMap<Integer, Machine>();

	protected AbstractServer(STRATEGY strategy, int port, int numTreads) {
		this.strategy = strategy;
		this.port = port;
		this.tcpServer = new TCPServer(this, numTreads);
	}

	public void start() throws Exception {
		try {
			this.port = this.tcpServer.startListening(this.port);
			// TODO: Add id once you get it form server
			myInfo = new Machine(Utils.getLocalServerIp(), this.port);
		} catch (IOException ioe) {
			this.stop();
			logger.error("Error starting tcp server", ioe);
			throw ioe;
		}
	}

	public abstract void startSpecific() throws Exception;

	public int getInternalPort() {
		return this.port;
	}

	public void stop() {
		this.tcpServer.stop();
	}

}
