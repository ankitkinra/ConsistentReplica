package org.umn.distributed.consistent.server;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Utils;

public abstract class AbstractServer implements TcpServerDelegate{
	private Logger logger = Logger.getLogger(this.getClass());

	private TCPServer tcpServer;
	protected int port;
	protected Machine myInfo;
	
	protected AbstractServer(int port, int numTreads) {
		this.port = port;
		this.tcpServer = new TCPServer(this, numTreads);
	}

	public void start() throws Exception {
		try {
			this.port = this.tcpServer.startListening(this.port);
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
