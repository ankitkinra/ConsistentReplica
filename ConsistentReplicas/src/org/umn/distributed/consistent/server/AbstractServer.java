package org.umn.distributed.consistent.server;

import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Utils;

public abstract class AbstractServer implements TcpServerDelegate{
	protected Logger logger = Logger.getLogger(this.getClass());

	public enum STRATEGY {
		SEQUENTIAL, QUORUM,
	}
	
	private TCPServer tcpServer;
	protected int port;
	protected Machine myInfo;
	protected STRATEGY strategy;

	// TODO think if we can change this to list sorted in increasing order
	private TreeMap<Integer, Machine> knownClients = new TreeMap<Integer, Machine>();

	protected AbstractServer(STRATEGY strategy, int port, int numTreads) {
		this.strategy = strategy;
		this.port = port;
		this.tcpServer = new TCPServer(this, numTreads);
	}

	public void start() throws Exception {
		try {
			this.port = this.tcpServer.startListening(this.port);
			//TODO: Add id once you get it form server
			myInfo = new Machine(Utils.getLocalServerIp(), this.port);
		}
		catch(IOException ioe) {
			this.stop();
			logger.error("Error starting tcp server", ioe);
			throw ioe;
		}
	}
	
	public void stop() {
		this.tcpServer.stop();
	}

}
